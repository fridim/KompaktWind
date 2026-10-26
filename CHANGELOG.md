# Changelog

## v0.1.2

Compared with v0.1.1, generated from `jj log -r v0.1.1..@` and
`jj diff --from v0.1.1 --to @`.

### Added

- Live station layer: OpenWindMap and METAR/AWC (with IEM fallback), nearby
  stations, station detail with 24 h history, manual refresh, and polite caching.
- OSM spot catalog: 166 spots (161 named + 5 with nearby-feature hints),
  searchable from Add Spot, Near Me, and the map picker; spot info and
  add-to-favorites.
- Private JSON spot imports: Settings → Spot locations, app-private storage;
  raw import files are excluded from Android backup, with validation and delete
  support. Saved spot metadata may still be included in Android backup.
- Forecast preview for the whole spot list: time presets, nearest-hour
  wind/gust values, and the existing wind colour/unit display.
- Selectable forecast models/sources, marine/wave display options, configurable
  cache TTL, and stale-cache fallback.
- Offline coastline map with multiple detail levels, pan/zoom, and the Add Spot
  map picker.
- Screenshot capture script with live-data waits and a full 3×3 README gallery.
- OSM catalog updater with clustered nearest-feature hints, point-in-polygon
  where geometry is available, compass qualifiers, a review CSV, and curated
  hint overrides.
- CI and tag-driven signed APK/AAB release automation, with signer pinning,
  APK/AAB signature verification, and GitHub Release assets.

### Changed

- Edge-to-edge layout with status/navigation bar insets and matching icon
  contrast.
- Wind colour mode: higher-contrast foregrounds, no extra bold in colour mode,
  and two-character wind/gust alignment in both modes.
- Spot list long-press actions now share the row with the name and info button;
  delete uses an X icon.
- The live-station strip now has previous/next triangle buttons and wraps around
  the nearby stations.
- Increased the app text scale by 10% and enlarged the forecast preset labels.
- Settings “Sources” renamed to “Spot locations”; language, units, marine
  display, cache TTL, dark screen, and colour mode controls.
- Bundled catalog is now OSM-only; 166 spots, manifest v16.
- Database schema v4 with sort-order migration.

### Fixed

- Android startup crash caused by a regular-expression flag unsupported by ICU.
- Map canvas drawing over the Add Spot tabs.
- Forecast HTTP error classification and optional marine failure logging.
- Live-station cache concurrency and stale-data handling.
- Recomputation cost in Near Me and Add Spot search.
- Unnamed OSM spots now display a meaningful nearby-feature hint instead of a
  blank name.
- Private-import source links are restricted to HTTPS, and release signing is
  isolated from the build job and checked against the release certificate.

### Tests

- 97 unit tests covering parsing, mapping, repositories, migrations, catalog
  logic, forecast preview, and formatting.
