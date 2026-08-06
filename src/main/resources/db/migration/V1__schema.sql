-- Grand Theft Cheltenham – core spatial schema (F16-0002)
-- EPSG:3857 (Web Mercator) for tile alignment

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS roads (
  id            BIGSERIAL PRIMARY KEY,
  osm_id        BIGINT UNIQUE,
  name          TEXT,
  highway_type  TEXT NOT NULL,
  lanes         SMALLINT DEFAULT 2,
  width_m       REAL,
  max_speed_kph SMALLINT,
  oneway        BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS road_segments (
  id            BIGSERIAL PRIMARY KEY,
  road_id       BIGINT NOT NULL REFERENCES roads(id) ON DELETE CASCADE,
  geom          GEOMETRY(LineString, 3857) NOT NULL,
  length_m      REAL NOT NULL,
  oneway        BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS road_segments_geom_idx ON road_segments USING GIST (geom);

CREATE TABLE IF NOT EXISTS junctions (
  id            BIGSERIAL PRIMARY KEY,
  geom          GEOMETRY(Point, 3857) NOT NULL
);
CREATE INDEX IF NOT EXISTS junctions_geom_idx ON junctions USING GIST (geom);

CREATE TABLE IF NOT EXISTS road_graph_edges (
  id                BIGSERIAL PRIMARY KEY,
  from_junction_id  BIGINT NOT NULL REFERENCES junctions(id),
  to_junction_id    BIGINT NOT NULL REFERENCES junctions(id),
  segment_id        BIGINT NOT NULL REFERENCES road_segments(id),
  length_m          REAL NOT NULL,
  max_speed_kph     SMALLINT,
  cost              REAL
);
CREATE INDEX IF NOT EXISTS road_graph_edges_from_idx ON road_graph_edges (from_junction_id);
CREATE INDEX IF NOT EXISTS road_graph_edges_to_idx ON road_graph_edges (to_junction_id);

CREATE TABLE IF NOT EXISTS map_tiles (
  zoom_level    SMALLINT NOT NULL,
  x             INTEGER  NOT NULL,
  y             INTEGER  NOT NULL,
  render_payload JSONB   NOT NULL,
  PRIMARY KEY (zoom_level, x, y)
);

CREATE TABLE IF NOT EXISTS locations (
  id            BIGSERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  category      TEXT NOT NULL,
  geom          GEOMETRY(Point, 3857) NOT NULL,
  metadata      JSONB DEFAULT '{}'
);
CREATE INDEX IF NOT EXISTS locations_geom_idx ON locations USING GIST (geom);
CREATE INDEX IF NOT EXISTS locations_category_idx ON locations (category);

CREATE TABLE IF NOT EXISTS vehicle_models (
  id                TEXT PRIMARY KEY,
  display_name      TEXT NOT NULL,
  year_from         SMALLINT,
  year_to           SMALLINT,
  body_style        TEXT,
  drivetrain        TEXT,
  mass_kg           REAL NOT NULL,
  power_hp          REAL NOT NULL,
  top_speed_kmh     REAL NOT NULL,
  accel_0_100_s     REAL,
  drag_coeff        REAL DEFAULT 0.35,
  frontal_area_m2   REAL DEFAULT 2.0,
  wheelbase_m       REAL DEFAULT 2.6,
  steering_sensitivity REAL DEFAULT 1.0,
  grip_multiplier   REAL DEFAULT 1.0,
  drift_threshold   REAL DEFAULT 0.9,
  colour            TEXT,
  category          TEXT DEFAULT 'car',
  subcategory       TEXT,
  tags              TEXT[],
  is_police         BOOLEAN DEFAULT FALSE,
  physics_profile   JSONB DEFAULT '{}',
  damage_profile    JSONB DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS driving_samples (
  id              BIGSERIAL PRIMARY KEY,
  session_id      UUID NOT NULL,
  sampled_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  x               DOUBLE PRECISION,
  y               DOUBLE PRECISION,
  heading         REAL,
  speed_ms        REAL,
  accel_long      REAL,
  accel_lat       REAL,
  lateral_g       REAL,
  road_id         BIGINT,
  edge_id         BIGINT
);
CREATE INDEX IF NOT EXISTS driving_samples_session_idx ON driving_samples (session_id);
CREATE INDEX IF NOT EXISTS driving_samples_time_idx ON driving_samples (sampled_at);

CREATE TABLE IF NOT EXISTS missions (
  id                TEXT PRIMARY KEY,
  name              TEXT NOT NULL,
  description       TEXT,
  type              TEXT DEFAULT 'drive',
  start_location_id BIGINT REFERENCES locations(id),
  end_location_id   BIGINT REFERENCES locations(id),
  time_limit_seconds INTEGER,
  max_damage_percent REAL,
  required_vehicle_category TEXT[],
  reward_cash       INTEGER DEFAULT 0,
  reward_score      INTEGER DEFAULT 0,
  unlock_flags      TEXT[],
  difficulty        SMALLINT DEFAULT 1,
  metadata          JSONB DEFAULT '{}'
);
