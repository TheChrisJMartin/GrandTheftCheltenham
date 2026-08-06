# Grand Theft Cheltenham

Top-down GTA1/GTA2-style browser driving game set in a **10-mile radius of Cheltenham town centre**.  
Delivered as a Tomcat-compatible WAR with real OpenStreetMap-derived roads, physics, missions and admin tooling.

**Current version:** 0.12.1  
**Epic:** E016

## Features implemented in this release

| ID        | Title                                      | Status       |
|-----------|--------------------------------------------|--------------|
| F16-0001  | Grand Theft Cheltenham (concept)           | Implemented  |
| F16-0002  | Cheltenham Map Import & Road Graph         | Partial – schema + APIs + docs |
| F16-0013  | Client Bootstrap & Game Loop               | Implemented (skeleton) |
| F16-0015  | Vehicle Catalogue & Physics Profiles       | Implemented (data + API) |

Blocked features remain blocked pending the foundation above.

## Quick start

### 1. Database

See [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md).

```bash
# After PostgreSQL + PostGIS are running:
psql -U gtc -d grandtheftcheltenham -f src/main/resources/db/migration/V1__schema.sql
psql -U gtc -d grandtheftcheltenham -f src/main/resources/db/migration/V2__vehicle_catalogue.sql
```

### 2. Build the WAR

```bash
mvn clean package -DskipTests
# → target/grand-theft-cheltenham.war
```

### 3. Run (embedded or external Tomcat)

```bash
# Embedded (dev)
mvn spring-boot:run

# Or drop the WAR into Tomcat webapps/
```

Open http://localhost:8080/ and click **Start Driving**.

### Controls

| Key        | Action     |
|------------|------------|
| ↑ / W      | Throttle   |
| ↓ / S      | Brake      |
| ← / A      | Steer left |
| → / D      | Steer right|

## API surface (current)

- `GET /api/health`
- `GET /api/map/status`
- `GET /api/map/tiles?z=&x=&y=`
- `GET /api/map/road-name?x=&y=`
- `GET /api/map/graph/nearest?x=&y=&radius=`
- `GET /api/vehicles`
- `GET /api/vehicles/{id}`
- `GET /api/vehicles/catalogue/summary`

## Project layout

```
src/main/java/uk/co/cheltenham/gtc/   Spring Boot app + REST controllers
src/main/resources/static/            Client (HTML + Canvas game loop)
src/main/resources/db/migration/      SQL schema & vehicle seed
docs/                                 Database & operational docs
scripts/                              Map import helpers (expanding)
```

## Map data

Real Cheltenham roads are **not** bundled. After deployment, run the Geofabrik + osmium pipeline described in `docs/DATABASE_SETUP.md` (or the OSMnx prototype) and load the resulting graph into PostGIS. Until then the client shows a placeholder grid and the map APIs return empty/placeholder payloads.

## Licence / notes

Internal development build for the Garruga / Cheltenham project.
