#!/usr/bin/env python3
"""
F16-0002 rapid prototype: build a drive graph for the Cheltenham 10-mile bbox
using OSMnx and optionally export GeoPackage / GraphML.

Requires: pip install osmnx geopandas

Usage:
  python scripts/import_map_osmnx.py
  python scripts/import_map_osmnx.py --bbox -2.32 51.74 -1.84 52.06
"""

import argparse
import sys

def main():
    parser = argparse.ArgumentParser(description="Cheltenham drive graph via OSMnx")
    parser.add_argument("--bbox", nargs=4, type=float,
                        default=[-2.32, 51.74, -1.84, 52.06],
                        metavar=("WEST", "SOUTH", "EAST", "NORTH"),
                        help="Bounding box (lon/lat)")
    parser.add_argument("--out", default="cheltenham_drive.gpkg")
    args = parser.parse_args()

    try:
        import osmnx as ox
    except ImportError:
        print("OSMnx not installed. Run: pip install osmnx geopandas", file=sys.stderr)
        print("Falling back to documentation-only mode.", file=sys.stderr)
        print("See docs/DATABASE_SETUP.md for the full Geofabrik + osmium production path.")
        return 1

    ox.settings.use_cache = True
    print(f"Downloading drive network for bbox {args.bbox} …")
    G = ox.graph_from_bbox(args.bbox, network_type="drive", simplify=True)
    print(f"Nodes: {len(G.nodes)}, Edges: {len(G.edges)}")

    G_proj = ox.project_graph(G, to_crs="EPSG:3857")
    G_proj = ox.routing.add_edge_speeds(G_proj)
    G_proj = ox.routing.add_edge_travel_times(G_proj)

    ox.save_graph_geopackage(G_proj, args.out)
    print(f"Wrote {args.out}")
    print("Next: load into PostGIS (roads / road_segments / junctions / road_graph_edges).")
    return 0

if __name__ == "__main__":
    sys.exit(main())
