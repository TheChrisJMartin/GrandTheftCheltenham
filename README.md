# Grand Theft Cheltenham

Top-down GTA1/GTA2-style browser driving game set in a **10-mile radius of Cheltenham town centre**.  
Delivered as a Tomcat-compatible WAR with real OpenStreetMap-derived roads, physics, missions and admin tooling.

**Current version:** 0.12.1  
**Epic:** E016  
**Live:** https://games.donotpassgo.co.uk/gtc/

## Progress (2026-08-06)

| ID | Title | Status |
|----|-------|--------|
| F16-0001 | Concept / scope | Implemented |
| F16-0002 | Map import & road graph | Implemented (live Overpass + PostGIS cache) |
| F16-0003 | Map rendering (Canvas layers) | In progress – roads, buildings, parks, water |
| F16-0013 | Client bootstrap & game loop | Implemented |
| F16-0015 | Vehicle catalogue (UK 1995–2010) | Implemented |
| F16-0004+ | Full physics, missions, police, etc. | Still planned / blocked |

### Working in the client
- Fixed-timestep drive loop, WASD / arrows
- Speed in **mph**, smoothed steering
- Speed-based camera zoom (thumb-sized car when slow, pulls back at speed)
- Live OSM fetch → PostGIS (`roads`, `road_segments`, `map_features`)
- Layered render: terrain → parks/buildings/water → road casing/fill → centre lines → car
- Road-name toasts

## Quick start

### Database
See [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md).

```bash
psql -d grandtheftcheltenham -f src/main/resources/db/migration/V1__schema.sql
psql -d grandtheftcheltenham -f src/main/resources/db/migration/V2__vehicle_catalogue.sql
psql -d grandtheftcheltenham -f src/main/resources/db/migration/V3__map_features.sql
```

### Build
```bash
mvn clean package -DskipTests
# → target/grand-theft-cheltenham.war
```

Deploy to Tomcat 10+ (context e.g. `/gtc`). Server needs outbound HTTPS to `overpass-api.de` for live map import.

### Controls
| Key | Action |
|-----|--------|
| ↑ / W | Throttle |
| ↓ / S | Brake |
| ← / A | Steer left |
| → / D | Steer right |

## API surface
- `GET /api/health`
- `GET /api/map/status`
- `GET /api/map/roads/nearby?x=&y=&radius=`
- `GET /api/map/features/nearby?x=&y=&radius=`
- `POST /api/map/fetch-around?x=&y=&radius=` — Overpass → DB cache
- `GET /api/map/road-name?x=&y=`
- `GET /api/vehicles`, `/api/vehicles/{id}`, `/api/vehicles/catalogue/summary`

## Licence
Internal development build for the Garruga / Cheltenham project.
