-- Landscape features for Canvas renderer (buildings, parks, water)
CREATE TABLE IF NOT EXISTS map_features (
  id BIGSERIAL PRIMARY KEY,
  osm_id BIGINT,
  kind TEXT NOT NULL,
  name TEXT,
  geom GEOMETRY(Geometry, 3857) NOT NULL,
  props JSONB DEFAULT '{}'
);
CREATE INDEX IF NOT EXISTS map_features_geom_idx ON map_features USING GIST (geom);
CREATE INDEX IF NOT EXISTS map_features_kind_idx ON map_features (kind);
CREATE UNIQUE INDEX IF NOT EXISTS map_features_osm_kind_uidx ON map_features (osm_id, kind) WHERE osm_id IS NOT NULL;
