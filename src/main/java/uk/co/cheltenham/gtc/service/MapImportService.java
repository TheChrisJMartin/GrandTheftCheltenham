package uk.co.cheltenham.gtc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** F16-0002 live Overpass fetch + PostGIS cache for roads and landscape features. */
@Service
public class MapImportService {

    private static final Logger log = LoggerFactory.getLogger(MapImportService.class);
    private static final String OVERPASS = "https://overpass-api.de/api/interpreter";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public MapImportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        ensureFeatureTable();
    }

    private void ensureFeatureTable() {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS map_features (
                  id BIGSERIAL PRIMARY KEY,
                  osm_id BIGINT,
                  kind TEXT NOT NULL,
                  name TEXT,
                  geom GEOMETRY(Geometry, 3857) NOT NULL,
                  props JSONB DEFAULT '{}'
                )
                """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS map_features_geom_idx ON map_features USING GIST (geom)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS map_features_kind_idx ON map_features (kind)");
        } catch (Exception e) {
            log.warn("map_features ensure: {}", e.toString());
        }
    }

    public static double[] mercatorToLonLat(double x, double y) {
        double lon = x / 20037508.34 * 180.0;
        double lat = y / 20037508.34 * 180.0;
        lat = 180.0 / Math.PI * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[]{lon, lat};
    }

    public static double[] lonLatToMercator(double lon, double lat) {
        double x = lon * 20037508.34 / 180.0;
        double y = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * 20037508.34 / 180.0;
        return new double[]{x, y};
    }

    public Map<String, Object> fetchAndCacheAround(double x, double y, double radiusM) {
        double[] ll = mercatorToLonLat(x, y);
        double lon = ll[0], lat = ll[1];
        double dLat = radiusM / 111320.0;
        double dLon = radiusM / (111320.0 * Math.cos(Math.toRadians(lat)));
        double south = lat - dLat, north = lat + dLat;
        double west = lon - dLon, east = lon + dLon;

        String query = """
                [out:json][timeout:55];
                (
                  way[\"highway\"~\"^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|living_street|motorway_link|trunk_link|primary_link|secondary_link|tertiary_link)$\"](%f,%f,%f,%f);
                  way[\"building\"](%f,%f,%f,%f);
                  way[\"leisure\"=\"park\"](%f,%f,%f,%f);
                  way[\"landuse\"~\"^(grass|recreation_ground|forest|meadow|cemetery)$\"](%f,%f,%f,%f);
                  way[\"natural\"~\"^(water|wood)$\"](%f,%f,%f,%f);
                  relation[\"natural\"=\"water\"](%f,%f,%f,%f);
                );
                out body geom;
                """.formatted(
                south, west, north, east,
                south, west, north, east,
                south, west, north, east,
                south, west, north, east,
                south, west, north, east,
                south, west, north, east
        );

        try {
            String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OVERPASS))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "GrandTheftCheltenham/0.12.1")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                return Map.of("ok", false, "error", "Overpass HTTP " + resp.statusCode());
            }
            return ingestOverpassJson(resp.body());
        } catch (Exception e) {
            log.warn("Overpass fetch failed: {}", e.toString());
            return Map.of("ok", false, "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public Map<String, Object> ingestOverpassJson(String json) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        JsonNode elements = root.get("elements");
        if (elements == null || !elements.isArray()) {
            return Map.of("ok", false, "error", "no elements");
        }
        int ways = 0, segments = 0, features = 0, skipped = 0;
        for (JsonNode el : elements) {
            String type = el.path("type").asText();
            if (!"way".equals(type) && !"relation".equals(type)) continue;
            JsonNode tags = el.path("tags");
            JsonNode geom = el.get("geometry");
            if (geom == null || !geom.isArray() || geom.size() < 2) { skipped++; continue; }
            if (tags.has("highway")) {
                int[] r = ingestRoad(el, tags, geom);
                ways += r[0]; segments += r[1]; skipped += r[2];
            } else {
                String kind = classifyFeature(tags);
                if (kind == null) { skipped++; continue; }
                if (ingestFeature(el.path("id").asLong(), kind, tags.path("name").asText(null), geom)) features++;
                else skipped++;
            }
        }
        return Map.of("ok", true, "waysUpserted", ways, "segmentsWritten", segments, "featuresWritten", features, "skipped", skipped);
    }

    private String classifyFeature(JsonNode tags) {
        if (tags.has("building")) return "building";
        if ("park".equals(tags.path("leisure").asText())) return "park";
        String landuse = tags.path("landuse").asText("");
        if ("grass".equals(landuse) || "meadow".equals(landuse) || "recreation_ground".equals(landuse)) return "grass";
        if ("forest".equals(landuse) || "wood".equals(tags.path("natural").asText())) return "wood";
        if ("cemetery".equals(landuse)) return "cemetery";
        if ("water".equals(tags.path("natural").asText())) return "water";
        return null;
    }

    private boolean ingestFeature(long osmId, String kind, String name, JsonNode geom) {
        StringBuilder wkt = new StringBuilder("POLYGON((");
        boolean first = true;
        double firstX = 0, firstY = 0;
        int n = 0;
        for (JsonNode pt : geom) {
            double[] m = lonLatToMercator(pt.path("lon").asDouble(), pt.path("lat").asDouble());
            if (!first) wkt.append(", ");
            else { firstX = m[0]; firstY = m[1]; }
            wkt.append(m[0]).append(" ").append(m[1]);
            first = false; n++;
        }
        if (n < 3) return false;
        wkt.append(", ").append(firstX).append(" ").append(firstY).append("))");
        try {
            jdbc.update("DELETE FROM map_features WHERE osm_id = ? AND kind = ?", osmId, kind);
            jdbc.update("INSERT INTO map_features (osm_id, kind, name, geom) VALUES (?, ?, ?, ST_GeomFromText(?, 3857))", osmId, kind, name, wkt.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int[] ingestRoad(JsonNode el, JsonNode tags, JsonNode geom) {
        int ways = 0, segments = 0, skipped = 0;
        long osmId = el.path("id").asLong();
        String name = tags.path("name").asText(null);
        String highway = tags.path("highway").asText("residential");
        boolean oneway = "yes".equalsIgnoreCase(tags.path("oneway").asText()) || "true".equalsIgnoreCase(tags.path("oneway").asText());
        int maxSpeed = parseSpeed(tags.path("maxspeed").asText(null));
        Long roadId = jdbc.query("SELECT id FROM roads WHERE osm_id = ?", rs -> rs.next() ? rs.getLong(1) : null, osmId);
        if (roadId == null) {
            roadId = jdbc.queryForObject("INSERT INTO roads (osm_id, name, highway_type, oneway, max_speed_kph) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, osmId, name, highway, oneway, maxSpeed > 0 ? maxSpeed : null);
            ways++;
        } else {
            jdbc.update("UPDATE roads SET name = COALESCE(?, name), highway_type = ?, oneway = ?, max_speed_kph = COALESCE(?, max_speed_kph) WHERE id = ?", name, highway, oneway, maxSpeed > 0 ? maxSpeed : null, roadId);
        }
        jdbc.update("DELETE FROM road_graph_edges WHERE segment_id IN (SELECT id FROM road_segments WHERE road_id = ?)", roadId);
        jdbc.update("DELETE FROM road_segments WHERE road_id = ?", roadId);
        StringBuilder wkt = new StringBuilder("LINESTRING(");
        double length = 0, prevX = 0, prevY = 0;
        boolean first = true;
        for (JsonNode pt : geom) {
            double[] m = lonLatToMercator(pt.path("lon").asDouble(), pt.path("lat").asDouble());
            if (!first) { length += Math.hypot(m[0] - prevX, m[1] - prevY); wkt.append(", "); }
            wkt.append(m[0]).append(" ").append(m[1]);
            prevX = m[0]; prevY = m[1]; first = false;
        }
        wkt.append(")");
        if (length < 1) return new int[]{ways, segments, skipped + 1};
        Long segId = jdbc.queryForObject("INSERT INTO road_segments (road_id, geom, length_m, oneway) VALUES (?, ST_GeomFromText(?, 3857), ?, ?) RETURNING id", Long.class, roadId, wkt.toString(), length, oneway);
        segments++;
        Long fromJ = ensureJunction(wktStart(wkt.toString()));
        Long toJ = ensureJunction(wktEnd(wkt.toString()));
        if (fromJ != null && toJ != null && segId != null) {
            jdbc.update("INSERT INTO road_graph_edges (from_junction_id, to_junction_id, segment_id, length_m, max_speed_kph, cost) VALUES (?, ?, ?, ?, ?, ?)", fromJ, toJ, segId, length, maxSpeed > 0 ? maxSpeed : 50, length);
            if (!oneway) jdbc.update("INSERT INTO road_graph_edges (from_junction_id, to_junction_id, segment_id, length_m, max_speed_kph, cost) VALUES (?, ?, ?, ?, ?, ?)", toJ, fromJ, segId, length, maxSpeed > 0 ? maxSpeed : 50, length);
        }
        return new int[]{ways, segments, skipped};
    }

    private Long ensureJunction(double[] xy) {
        if (xy == null) return null;
        List<Long> existing = jdbc.query("SELECT id FROM junctions WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(?, ?), 3857), 1.5) ORDER BY geom <-> ST_SetSRID(ST_MakePoint(?, ?), 3857) LIMIT 1", (rs, i) -> rs.getLong(1), xy[0], xy[1], xy[0], xy[1]);
        if (!existing.isEmpty()) return existing.get(0);
        return jdbc.queryForObject("INSERT INTO junctions (geom) VALUES (ST_SetSRID(ST_MakePoint(?, ?), 3857)) RETURNING id", Long.class, xy[0], xy[1]);
    }

    private static double[] wktStart(String wkt) {
        int a = wkt.indexOf('('); int b = wkt.indexOf(',');
        if (a < 0 || b < 0) return null;
        String[] p = wkt.substring(a + 1, b).trim().split("\\s+");
        return new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1])};
    }

    private static double[] wktEnd(String wkt) {
        int a = wkt.lastIndexOf(','); int b = wkt.lastIndexOf(')');
        if (a < 0 || b < 0) return null;
        String[] p = wkt.substring(a + 1, b).trim().split("\\s+");
        return new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1])};
    }

    private static int parseSpeed(String s) {
        if (s == null || s.isBlank()) return 0;
        s = s.replace("mph", "").replace("km/h", "").replace("kmh", "").trim();
        try {
            int v = (int) Double.parseDouble(s.split("\\s+")[0]);
            if (v > 0 && v <= 70 && !s.contains("km")) return (int) Math.round(v * 1.60934);
            return v;
        } catch (Exception e) { return 0; }
    }

    public List<Map<String, Object>> nearbyRoads(double x, double y, double radiusM, int limit) {
        return jdbc.queryForList("SELECT s.id AS segment_id, r.name, r.highway_type, r.oneway, s.length_m, ST_AsGeoJSON(s.geom) AS geojson FROM road_segments s JOIN roads r ON r.id = s.road_id WHERE ST_DWithin(s.geom, ST_SetSRID(ST_MakePoint(?, ?), 3857), ?) ORDER BY s.geom <-> ST_SetSRID(ST_MakePoint(?, ?), 3857) LIMIT ?", x, y, radiusM, x, y, limit);
    }

    public List<Map<String, Object>> nearbyFeatures(double x, double y, double radiusM, int limit) {
        try {
            return jdbc.queryForList("SELECT id, kind, name, ST_AsGeoJSON(geom) AS geojson FROM map_features WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(?, ?), 3857), ?) ORDER BY geom <-> ST_SetSRID(ST_MakePoint(?, ?), 3857) LIMIT ?", x, y, radiusM, x, y, limit);
        } catch (Exception e) { return List.of(); }
    }
}
