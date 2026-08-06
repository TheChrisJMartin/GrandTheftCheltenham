package uk.co.cheltenham.gtc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/map")
public class MapController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/tiles")
    public ResponseEntity<Map<String, Object>> getTile(
            @RequestParam int z,
            @RequestParam int x,
            @RequestParam int y) {

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT render_payload FROM map_tiles WHERE zoom_level = ? AND x = ? AND y = ?",
                z, x, y);

        if (!rows.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "z", z, "x", x, "y", y,
                    "payload", rows.get(0).get("render_payload")
            ));
        }

        Map<String, Object> placeholder = new LinkedHashMap<>();
        placeholder.put("roads", List.of());
        placeholder.put("terrain", "urban");
        placeholder.put("note", "No real tile data yet – run map import (F16-0002)");
        return ResponseEntity.ok(Map.of(
                "z", z, "x", x, "y", y,
                "payload", placeholder
        ));
    }

    @GetMapping("/road-name")
    public ResponseEntity<Map<String, Object>> roadName(
            @RequestParam double x,
            @RequestParam double y) {

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT r.name, r.highway_type, ST_Distance(s.geom, ST_SetSRID(ST_MakePoint(?, ?), 3857)) AS dist
                FROM road_segments s
                JOIN roads r ON r.id = s.road_id
                WHERE r.name IS NOT NULL
                ORDER BY s.geom <-> ST_SetSRID(ST_MakePoint(?, ?), 3857)
                LIMIT 1
                """, x, y, x, y);

        if (rows.isEmpty()) {
            return ResponseEntity.ok(Map.of("name", null, "found", false));
        }
        Map<String, Object> row = rows.get(0);
        return ResponseEntity.ok(Map.of(
                "name", row.get("name"),
                "highwayType", row.get("highway_type"),
                "distanceM", row.get("dist"),
                "found", true
        ));
    }

    @GetMapping("/graph/nearest")
    public ResponseEntity<Map<String, Object>> nearestEdge(
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam(defaultValue = "30") double radius) {

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.id AS edge_id, e.length_m, e.max_speed_kph,
                       r.name, r.highway_type
                FROM road_graph_edges e
                JOIN road_segments s ON s.id = e.segment_id
                JOIN roads r ON r.id = s.road_id
                WHERE ST_DWithin(s.geom, ST_SetSRID(ST_MakePoint(?, ?), 3857), ?)
                ORDER BY s.geom <-> ST_SetSRID(ST_MakePoint(?, ?), 3857)
                LIMIT 1
                """, x, y, radius, x, y);

        if (rows.isEmpty()) {
            return ResponseEntity.ok(Map.of("found", false));
        }
        return ResponseEntity.ok(Map.of("found", true, "edge", rows.get(0)));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Long roadCount = jdbc.queryForObject("SELECT COUNT(*) FROM roads", Long.class);
        Long segmentCount = jdbc.queryForObject("SELECT COUNT(*) FROM road_segments", Long.class);
        Long tileCount = jdbc.queryForObject("SELECT COUNT(*) FROM map_tiles", Long.class);
        Long locCount = jdbc.queryForObject("SELECT COUNT(*) FROM locations", Long.class);
        return Map.of(
                "roads", roadCount != null ? roadCount : 0,
                "segments", segmentCount != null ? segmentCount : 0,
                "tiles", tileCount != null ? tileCount : 0,
                "locations", locCount != null ? locCount : 0,
                "ready", (roadCount != null && roadCount > 0)
        );
    }
}
