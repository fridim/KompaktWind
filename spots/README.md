# Spot catalog

Static, sharded spot data for KompaktWind. Current snapshot: **166 spots**.

Sources:
- OpenStreetMap: 166 `sport=kitesurfing` / `sport=windsurfing` elements
  (161 named + 5 unnamed with a nearby-feature hint name)
  that are actual outdoor spots, imported from the Overpass API snapshot of
  2026-10-24. Ways/relations use their computed center. Shops, offices,
  schools, clubs, sports centres, hotels and other facilities are excluded;
  beaches, bays, slipways, swimming areas, attractions and weather-station
  spots are kept.

- `manifest.json`: catalog version; bump `version` whenever any data changes.
- `spots.index.json`: lightweight list of every spot for discovery/search.
- `spots.<floor(lat)>_<floor(lon)>.json`: full records for 1° tiles.
- `tile` in the index tells the app which shard to fetch for details.

OSM-sourced rows can be refreshed (upserted, without touching other bundled
rows) with `python3 scripts/update-osm-catalog.py`; pass `--dry-run` first.
Unnamed spots keep `name` empty and get a `hintName` from the nearest named
sport facility/building (kite/windsurf), then beach/water/place feature in OSM;
the app displays that hint.
The Gradle `copySpotCatalog` task copies these JSON files into the app assets at
build time. Catalog updates ship with app releases; there is no runtime
network refresh. After editing any catalog file, bump `manifest.json`.

OpenStreetMap data is © OpenStreetMap contributors and licensed under ODbL 1.0.
The resulting catalog database is distributed under ODbL 1.0; see
https://www.openstreetmap.org/copyright and `LICENSE`.

OSM snapshot query (then keep only actual spots; see the filter rules above):
```
[out:json][timeout:120];
(
  node["sport"~"^(kitesurfing|windsurfing)$"];
  way["sport"~"^(kitesurfing|windsurfing)$"];
  relation["sport"~"^(kitesurfing|windsurfing)$"];
);
out tags center;
```

The OFB/N2Glisse datasets are **not included yet**: data.gouv.fr lists them as
metadata-only harvest stubs with no downloadable resources and
`license: notspecified`, and data.ofb.fr is currently returning 503/504. Add
them only after the files and an explicit licence are available.

Descriptions and supported wind directions are intentionally blank unless we
have permission to reuse them. The schema supports `description`,
`shortName`, `windDirections`, `avoidWindDirections`, `activities`,
`sourceUrl`, and `altNames`. `shortName` is a generated display fallback for
long names. Wind directions are only included when they are explicitly stated
on the source page. Merged OSM entries keep the OSM name(s) in `altNames`; OSM
`name:*`, `alt_name`, `official_name`, and similar tags are also preserved.

Attribution:
- OpenStreetMap: © OpenStreetMap contributors, ODbL 1.0
