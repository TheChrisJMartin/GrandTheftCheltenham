# Grand Theft Cheltenham – Database Setup

**Feature:** F16-0002 Cheltenham Map Import & Road Graph  
**Version:** 0.12.1

## Requirements

- PostgreSQL 14+ (tested on 16)
- PostGIS 3.x extension

## Quick start (local / Docker-friendly)

```bash
# Create role & database
sudo -u postgres psql -c "CREATE USER gtc WITH PASSWORD 'gtcpass';"
sudo -u postgres psql -c "CREATE DATABASE grandtheftcheltenham OWNER gtc;"
sudo -u postgres psql -d grandtheftcheltenham -c "CREATE EXTENSION postgis;"

# Apply schema + vehicle seed
psql -U gtc -d grandtheftcheltenham -f src/main/resources/db/migration/V1__schema.sql
psql -U gtc -d grandtheftcheltenham -f src/main/resources/db/migration/V2__vehicle_catalogue.sql
```

Or as the `postgres` superuser:

```bash
psql -d grandtheftcheltenham -f src/main/resources/db/migration/V1__schema.sql
psql -d grandtheftcheltenham -f src/main/resources/db/migration/V2__vehicle_catalogue.sql
```

## Connection settings (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/grandtheftcheltenham
    username: gtc
    password: gtcpass
```

## Schema overview

| Table              | Purpose                                      | Feature   |
|--------------------|----------------------------------------------|-----------|
| `roads`            | Named highway objects from OSM               | F16-0002  |
| `road_segments`    | LineString geometry (EPSG:3857)              | F16-0002  |
| `junctions`        | Graph nodes                                  | F16-0002  |
| `road_graph_edges` | Directed edges with cost / speed             | F16-0002  |
| `map_tiles`        | Pre-rendered JSON payloads for Canvas (z14–18) | F16-0002 |
| `locations`        | Bars, landmarks, POIs, police stations       | F16-0009  |
| `vehicle_models`   | Physics profiles (cars + utility/emergency)  | F16-0015 / F16-0018 |
| `driving_samples`  | Telemetry ring-buffer / analytics            | F16-0007  |
| `missions`         | Mission definitions                          | F16-0008 / F16-0016 |

All geometry uses **EPSG:3857** (Web Mercator) so it lines up with standard map tiles.

## Map import pipeline (production path)

1. Download Geofabrik Gloucestershire extract:
   ```bash
   wget -N https://download.geofabrik.de/europe/great-britain/england/gloucestershire-latest.osm.pbf
   ```

2. Clip to ~10-mile radius of Cheltenham (padded bbox):
   ```bash
   osmium extract --bbox -2.32,51.74,-1.84,52.06 \
     gloucestershire-latest.osm.pbf \
     -o cheltenham-10mi.osm.pbf --strategy=smart
   ```

3. Keep only drivable highways:
   ```bash
   osmium tags-filter cheltenham-10mi.osm.pbf \
     w/highway=motorway,trunk,primary,secondary,tertiary,unclassified,residential,service,living_street,motorway_link,trunk_link,primary_link,secondary_link,tertiary_link \
     -o cheltenham-roads.osm.pbf
   ```

4. Load into PostGIS (custom Java importer or osm2pgsql flex style – scripts/ to be expanded).

5. Build junctions by snapping segment endpoints (~1 m tolerance).

6. Generate directed `road_graph_edges`.

7. Produce simplified tiles into `map_tiles` for zooms 14–18.

## Rapid prototype path (OSMnx)

```python
import osmnx as ox
ox.settings.use_cache = True
bbox = (-2.32, 51.74, -1.84, 52.06)
G = ox.graph_from_bbox(bbox, network_type="drive", simplify=True)
G_proj = ox.project_graph(G, to_crs="EPSG:3857")
G_proj = ox.routing.add_edge_speeds(G_proj)
ox.save_graph_geopackage(G_proj, "cheltenham_drive.gpkg")
```

## Verification

```sql
SELECT COUNT(*) FROM roads;
SELECT COUNT(*) FROM vehicle_models;
SELECT PostGIS_Version();
SELECT * FROM vehicle_models WHERE id = 'vauxhall_astra_mk1_brown';
```

## Notes

- The application uses `ddl-auto: validate` – schema must be applied out-of-band (or via Flyway/Liquibase in a later release).
- No real OSM data is shipped in the WAR; the map import is an operational step after deployment.
- Placeholder tiles are returned by `/api/map/tiles` until the importer has run.
