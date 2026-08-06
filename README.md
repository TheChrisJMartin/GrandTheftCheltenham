# Grand Theft Cheltenham

Top-down GTA1/GTA2-style browser driving game set in a **10-mile radius of Cheltenham town centre**.
Delivered as a Tomcat-compatible WAR with real OpenStreetMap-derived roads.

**Version:** 0.12.1  
**Epic:** E016  
**Live:** https://games.donotpassgo.co.uk/gtc/

## Progress

| Feature | Status |
|---------|--------|
| F16-0002 Map import (Overpass → PostGIS) | Implemented |
| F16-0003 Canvas map rendering | Implemented (roads, buildings, parks, water) |
| F16-0013 Client bootstrap & game loop | Implemented (fixed 40 FPS render, 60 Hz physics) |
| F16-0015 Vehicle catalogue | Implemented |
| Schema auto-migrate on WAR boot | Implemented (`SchemaBootstrap`) |
| UK left-lane drive, reverse, multipoint turns | Implemented |
| Town-centre spawn (Promenade) | Implemented |

## Build

```bash
mvn clean package -DskipTests
# → target/grand-theft-cheltenham.war
```

Deploy to Tomcat 10+ (context `/gtc`). Needs PostgreSQL/PostGIS and outbound HTTPS to Overpass.

SQL under `src/main/resources/db/migration/` is applied automatically on startup (V1 schema, V2 vehicles, V3 map_features).

## Controls

| Key | Action |
|-----|--------|
| W / ↑ | Accelerate |
| S / ↓ | Brake, then reverse |
| A / ← | Steer left |
| D / → | Steer right |

## API

- `GET /api/health`
- `GET /api/map/status`
- `GET /api/map/roads/nearby`, `/api/map/features/nearby`
- `POST /api/map/fetch-around`
- `GET /api/map/road-name`
- `GET /api/vehicles`, `/api/vehicles/{id}`
