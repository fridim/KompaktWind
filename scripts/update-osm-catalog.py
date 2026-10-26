#!/usr/bin/env python3
"""Regenerate OSM wind-sport spots in a bbox of the bundled catalog.

Usage:
    python3 scripts/update-osm-catalog.py --dry-run
    python3 scripts/update-osm-catalog.py

Defaults to the Golfe du Lion / eastern Occitanie bbox. OSM rows inside the
bbox are replaced by the query result; rows outside are preserved. Pass a world
bbox for a full global rebuild. Unnamed spots get a hintName from OSM name tags
or the nearest named sport facility/building, beach/water, or place feature.
Suggestions are written to spots/unnamed-hints.csv; edit its override column or
spots/hint-overrides.json to apply curated names.
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import math
import os
import pathlib
import sys
import time
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
SPOTS = ROOT / "spots"
INDEX = SPOTS / "spots.index.json"
MANIFEST = SPOTS / "manifest.json"
OVERPASS_URL = os.environ.get("OVERPASS_URL", "https://overpass-api.de/api/interpreter")
USER_AGENT = "KompaktWind-catalog-updater/1.0 (https://github.com/fridim/KompaktWind)"
# Golfe du Lion / eastern Occitanie; not the whole administrative region.
DEFAULT_BBOX = (42.31, 2.91, 43.61, 4.70)

SPORT_ACTIVITIES = {"kitesurfing", "windsurfing"}
FACILITY_LEISURE = {"sports_centre", "water_sports", "swimming_pool", "water_park"}
FACILITY_AMENITY = {
    "school", "college", "university", "surf_school", "boat_rental",
    "diving_centre", "restaurant", "cafe", "bar", "parking", "fuel",
    "community_centre",
}
FACILITY_TOURISM = {"hotel", "guest_house", "apartment", "motel", "hostel", "water_sports"}


def overpass(bbox: tuple[float, float, float, float]) -> list[dict]:
    south, west, north, east = bbox
    query = f"""[out:json][timeout:90];
(
  nwr["sport"~"(^|;)(kitesurfing|windsurfing)(;|$)"]({south},{west},{north},{east});
);
out center tags;"""
    request = urllib.request.Request(
        OVERPASS_URL,
        data=urllib.parse.urlencode({"data": query}).encode(),
        headers={"User-Agent": USER_AGENT},
    )
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                return json.load(response).get("elements", [])
        except Exception:
            if attempt == 2:
                raise
            time.sleep(3)


def sport_activities(tags: dict) -> list[str]:
    values = [value.strip().lower() for value in tags.get("sport", "").split(";")]
    return sorted({value for value in values if value in SPORT_ACTIVITIES})


def has_any(tags: dict, key: str, values: set[str]) -> bool:
    return any(value in values for value in tags.get(key, "").split(";") if value)


def is_facility(tags: dict) -> bool:
    if tags.get("shop") or tags.get("office") or tags.get("club") or tags.get("building"):
        return True
    if has_any(tags, "leisure", FACILITY_LEISURE):
        return True
    if has_any(tags, "amenity", FACILITY_AMENITY):
        return True
    if has_any(tags, "tourism", FACILITY_TOURISM):
        return True
    return "school" in tags.get("name", "").lower()


def tile_for(lat: float, lon: float) -> str:
    return f"{math.floor(lat)}_{math.floor(lon)}"


def record_for(element: dict, updated: str) -> dict | None:
    tags = element.get("tags", {})
    name = tags.get("name", "").strip()
    activities = sport_activities(tags)
    lat = element.get("lat") or element.get("center", {}).get("lat")
    lon = element.get("lon") or element.get("center", {}).get("lon")
    if not activities or lat is None or lon is None or is_facility(tags):
        return None
    url = f"https://www.openstreetmap.org/{element['type']}/{element['id']}"
    alt_names = []
    for key in ("alt_name", "official_name", "loc_name", "old_name"):
        for value in tags.get(key, "").split(";"):
            value = value.strip()
            if value and value != name and value not in alt_names:
                alt_names.append(value)
    short_name = tags.get("short_name", "").strip() if name else ""
    hint_name = ""
    hint_kind = ""
    if not name:
        for key in ("short_name", "alt_name", "official_name", "loc_name", "old_name", "name:fr"):
            value = tags.get(key, "").strip()
            if value:
                hint_name, hint_kind = value, f"osm:{key}"
                break
        if not hint_name:
            for key, value in tags.items():
                if key.startswith("name:") and value.strip():
                    hint_name, hint_kind = value.strip(), f"osm:{key}"
                    break
    return {
        "id": f"osm-{element['type']}-{element['id']}",
        "name": name,
        "shortName": short_name or name,
        "altNames": alt_names,
        "lat": float(lat),
        "lon": float(lon),
        "country": None,
        "region": None,
        "description": "",
        "windDirections": [],
        "avoidWindDirections": [],
        "activities": activities,
        "source": "openstreetmap.org",
        "sourceUrl": url,
        "sources": [{"name": "openstreetmap.org", "url": url}],
        "tile": tile_for(float(lat), float(lon)),
        "updated": updated,
        "hintName": hint_name,
        "hintKind": hint_kind,
        "hintOsm": "",
    }


HINT_CLUSTER_DEGREES = 0.5
HINT_PAD_DEGREES = 0.06
HINT_OVERRIDES = SPOTS / "hint-overrides.json"
HINTS_CSV = SPOTS / "unnamed-hints.csv"
DIRECTIONS = ("N", "NE", "E", "SE", "S", "SW", "W", "NW")


def hints_for(records: list[dict]) -> None:
    if not records:
        return
    groups: dict[tuple[int, int], list[dict]] = {}
    for record in records:
        key = (
            math.floor(record["lat"] / HINT_CLUSTER_DEGREES),
            math.floor(record["lon"] / HINT_CLUSTER_DEGREES),
        )
        groups.setdefault(key, []).append(record)

    print(f"hint clusters: {len(groups)} for {len(records)} unnamed spots")
    for index, group in enumerate(groups.values()):
        if index:
            time.sleep(1)
        south = min(record["lat"] for record in group) - HINT_PAD_DEGREES
        west = min(record["lon"] for record in group) - HINT_PAD_DEGREES
        north = max(record["lat"] for record in group) + HINT_PAD_DEGREES
        east = max(record["lon"] for record in group) + HINT_PAD_DEGREES
        query = f"""[out:json][timeout:90];
(
  nwr["name"]["natural"="beach"]({south},{west},{north},{east});
  nwr["name"]["natural"="water"]({south},{west},{north},{east});
  nwr["name"]["place"~"^(city|town|village|hamlet|suburb|neighbourhood|locality)$"]({south},{west},{north},{east});
  nwr["name"]["tourism"~"^(attraction|viewpoint)$"]({south},{west},{north},{east});
  nwr["name"]["leisure"~"^(marina|nature_reserve|beach_resort)$"]({south},{west},{north},{east});
  nwr["name"]["sport"~"(^|;)(kitesurfing|windsurfing)(;|$)"]({south},{west},{north},{east});
);
out geom tags;"""
        request = urllib.request.Request(
            OVERPASS_URL,
            data=urllib.parse.urlencode({"data": query}).encode(),
            headers={"User-Agent": USER_AGENT},
        )
        elements = None
        for attempt in range(2):
            try:
                with urllib.request.urlopen(request, timeout=120) as response:
                    elements = json.load(response).get("elements", [])
                break
            except Exception as error:
                if attempt:
                    print(f"  ! hint lookup failed for cluster {index + 1}: {error}")
                else:
                    time.sleep(2)
        if elements is None:
            continue
        apply_hints(group, elements)


def bbox_distance(element: dict, lat: float, lon: float) -> float:
    bounds = element.get("bounds")
    if not bounds:
        other_lat = element.get("lat")
        other_lon = element.get("lon")
        if other_lat is None or other_lon is None:
            return float("inf")
        d_lat = (other_lat - lat) * 111_000
        d_lon = (other_lon - lon) * 111_000 * math.cos(math.radians(lat))
        return math.hypot(d_lat, d_lon)
    south, west, north, east = (
        bounds["minlat"], bounds["minlon"], bounds["maxlat"], bounds["maxlon"]
    )
    d_lat = max(south - lat, 0.0, lat - north)
    d_lon = max(west - lon, 0.0, lon - east)
    return math.hypot(d_lat * 111_000, d_lon * 111_000 * math.cos(math.radians(lat)))


def segment_distance(lat: float, lon: float, a: dict, b: dict) -> float:
    kx = 111_000 * math.cos(math.radians(lat))
    ax = (a["lon"] - lon) * kx
    ay = (a["lat"] - lat) * 111_000
    bx = (b["lon"] - lon) * kx
    by = (b["lat"] - lat) * 111_000
    dx = bx - ax
    dy = by - ay
    if dx == 0 and dy == 0:
        return math.hypot(ax, ay)
    t = -(ax * dx + ay * dy) / (dx * dx + dy * dy)
    t = max(0.0, min(1.0, t))
    return math.hypot(ax + t * dx, ay + t * dy)


def point_in_polygon(lat: float, lon: float, points: list[tuple[float, float]]) -> bool:
    inside = False
    j = len(points) - 1
    for i, (x_i, y_i) in enumerate(points):
        x_j, y_j = points[j]
        if (y_i > lat) != (y_j > lat):
            x_cross = (x_j - x_i) * (lat - y_i) / (y_j - y_i) + x_i
            if lon < x_cross:
                inside = not inside
        j = i
    return inside


def geometry_distance(element: dict, lat: float, lon: float) -> float:
    geometry = element.get("geometry")
    if geometry:
        points = [
            (point["lon"], point["lat"])
            for point in geometry
            if point.get("lat") is not None and point.get("lon") is not None
        ]
        if len(points) >= 2:
            if len(points) >= 4 and points[0] == points[-1] and point_in_polygon(lat, lon, points):
                return 0.0
            return min(
                segment_distance(lat, lon, {"lat": b[1], "lon": b[0]}, {"lat": a[1], "lon": a[0]})
                for a, b in zip(points, points[1:])
            )
    return bbox_distance(element, lat, lon)


def element_center(element: dict) -> tuple[float | None, float | None]:
    if element.get("type") == "node":
        return element.get("lat"), element.get("lon")
    geometry = element.get("geometry")
    if geometry:
        points = [point for point in geometry if point.get("lat") is not None and point.get("lon") is not None]
        if points:
            return (
                sum(point["lat"] for point in points) / len(points),
                sum(point["lon"] for point in points) / len(points),
            )
    bounds = element.get("bounds")
    if bounds:
        return (
            (bounds["minlat"] + bounds["maxlat"]) / 2,
            (bounds["minlon"] + bounds["maxlon"]) / 2,
        )
    return None, None


def bearing_direction(lat1: float, lon1: float, lat2: float, lon2: float) -> str:
    d_lon = (lon2 - lon1) * math.cos(math.radians((lat1 + lat2) / 2))
    angle = (math.degrees(math.atan2(d_lon, lat2 - lat1)) + 360) % 360
    return DIRECTIONS[int((angle + 22.5) // 45) % 8]


def apply_hints(records: list[dict], elements: list[dict]) -> None:
    def bbox_km(element: dict) -> float:
        bounds = element.get("bounds")
        if not bounds:
            return 0.0
        d_lat = (bounds["maxlat"] - bounds["minlat"]) * 111
        d_lon = (bounds["maxlon"] - bounds["minlon"]) * 111 * math.cos(
            math.radians((bounds["minlat"] + bounds["maxlat"]) / 2)
        )
        return math.hypot(d_lat, d_lon)

    def radius(tags: dict) -> float:
        natural = tags.get("natural")
        if natural in {"beach", "bay"}:
            return 2500
        if natural == "water":
            return 1500
        if tags.get("place"):
            return 3000
        return 500

    def is_sport_facility(tags: dict) -> bool:
        if not sport_activities(tags):
            return False
        if tags.get("shop") or tags.get("office"):
            return False
        return bool(
            tags.get("building")
            or tags.get("club")
            or tags.get("leisure") in {"sports_centre"}
            or tags.get("amenity") in {"surf_school", "sailing_school", "school", "college", "university"}
        )

    def usable_hint(tags: dict, element: dict) -> bool:
        name = tags.get("name", "").lower()
        if any(word in name for word in ("port de plaisance", "port de pêche", "marina", "bassin", "jet d'eau", "fontaine", "piscine")):
            return False
        if tags.get("natural") == "water":
            if tags.get("water") in {"sea", "ocean", "fountain", "basin", "wastewater", "swimming_pool"}:
                return False
            if tags.get("leisure") == "marina":
                return False
            if bbox_km(element) > 50:
                return False
        return True

    usable = [
        element for element in elements
        if element.get("tags", {}).get("name") and usable_hint(element["tags"], element)
    ]
    for record in records:
        facilities = []
        candidates = []
        for element in usable:
            distance_m = geometry_distance(element, record["lat"], record["lon"])
            if is_sport_facility(element["tags"]) and distance_m <= 600:
                facilities.append((distance_m, element))
            if distance_m <= radius(element["tags"]):
                candidates.append((distance_m, element))
        if facilities:
            _, winner = min(facilities, key=lambda item: item[0])
        elif candidates:
            _, winner = min(candidates, key=lambda item: item[0])
        else:
            continue
        tags = winner["tags"]
        record["hintName"] = tags["name"].strip()
        record["hintKind"] = (
            f"natural={tags['natural']}" if tags.get("natural")
            else f"place={tags['place']}" if tags.get("place")
            else f"tourism={tags['tourism']}" if tags.get("tourism")
            else f"leisure={tags['leisure']}" if tags.get("leisure")
            else f"amenity={tags['amenity']}" if tags.get("amenity")
            else f"club={tags['club']}" if tags.get("club")
            else f"building={tags['building']}"
        )
        record["hintOsm"] = f"{winner['type']}/{winner['id']}"
        center_lat, center_lon = element_center(winner)
        record["_hintLat"] = center_lat
        record["_hintLon"] = center_lon


def qualify_hints(records: list[dict]) -> None:
    groups: dict[str, list[dict]] = {}
    for record in records:
        if record.get("hintOsm"):
            groups.setdefault(record["hintOsm"], []).append(record)
    for group in groups.values():
        if len(group) < 2:
            continue
        for record in group:
            center_lat = record.get("_hintLat")
            center_lon = record.get("_hintLon")
            if center_lat is None or center_lon is None:
                continue
            record["hintName"] = (
                f"{record['hintName']} "
                f"({bearing_direction(center_lat, center_lon, record['lat'], record['lon'])})"
            )


def load_overrides() -> dict[str, str]:
    overrides: dict[str, str] = {}
    if HINT_OVERRIDES.exists():
        try:
            data = json.loads(HINT_OVERRIDES.read_text())
            overrides.update({str(key): str(value) for key, value in data.items()})
        except Exception as error:
            print(f"  ! could not read {HINT_OVERRIDES}: {error}")
    if HINTS_CSV.exists():
        try:
            with HINTS_CSV.open(newline="", encoding="utf-8") as handle:
                for row in csv.DictReader(handle):
                    value = (row.get("override") or "").strip()
                    osm_id = (row.get("osm_id") or "").strip()
                    if osm_id and value:
                        overrides[osm_id] = value
        except Exception as error:
            print(f"  ! could not read {HINTS_CSV}: {error}")
    return overrides


def csv_safe(value: object) -> str:
    text = str(value)
    if text and text[0] in ("=", "+", "-", "@"):
        return "'" + text
    return text


def write_hints_csv(records: list[dict], overrides: dict[str, str]) -> None:
    rows = []
    for record in records:
        if record.get("name"):
            continue
        rows.append({
            "osm_id": record["id"],
            "name": csv_safe(record.get("name", "")),
            "lat": record["lat"],
            "lon": record["lon"],
            "sport": ";".join(record.get("activities", [])),
            "hint_name": csv_safe(record.get("hintName", "")),
            "hint_kind": csv_safe(record.get("hintKind", "")),
            "hint_osm": csv_safe(record.get("hintOsm", "")),
            "override": csv_safe(overrides.get(record["id"], "")),
        })
    rows.sort(key=lambda row: (row["hint_name"], row["osm_id"]))
    with HINTS_CSV.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=[
            "osm_id", "name", "lat", "lon", "sport",
            "hint_name", "hint_kind", "hint_osm", "override",
        ])
        writer.writeheader()
        writer.writerows(rows)
    print(f"hints report: {HINTS_CSV} ({len(rows)} rows)")


def load_records() -> dict[str, dict]:
    records: dict[str, dict] = {}
    for path in sorted(SPOTS.glob("spots.*.json")):
        if path.name == INDEX.name:
            continue
        for record in json.loads(path.read_text()):
            records[record["id"]] = record
    return records


def index_record(record: dict) -> dict:
    out = {
        key: value
        for key, value in record.items()
        if value not in (None, "", [], {})
    }
    out["name"] = record.get("name", "")
    return out


def write_catalog(records: dict[str, dict], version: int, updated: str) -> None:
    records = {
        key: {name: value for name, value in record.items() if not name.startswith("_")}
        for key, record in records.items()
    }
    by_tile: dict[str, list[dict]] = {}
    for record in records.values():
        record["tile"] = tile_for(record["lat"], record["lon"])
        by_tile.setdefault(record["tile"], []).append(record)

    for path in SPOTS.glob("spots.*.json"):
        if path.name != INDEX.name:
            path.unlink()
    for tile, tile_records in by_tile.items():
        tile_records.sort(key=lambda item: (item.get("name") or item.get("hintName") or "").casefold())
        (SPOTS / f"spots.{tile}.json").write_text(
            json.dumps(tile_records, indent=2, ensure_ascii=False) + "\n"
        )

    ordered = sorted(
        records.values(),
        key=lambda item: (item.get("name") or item.get("hintName") or "").casefold(),
    )
    INDEX.write_text(
        json.dumps([index_record(item) for item in ordered], indent=2, ensure_ascii=False) + "\n"
    )
    MANIFEST.write_text(
        json.dumps(
            {"version": version, "generatedAt": updated, "spots": len(records)},
            indent=2,
        )
        + "\n"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--bbox", default=",".join(str(value) for value in DEFAULT_BBOX),
        help="south,west,north,east (default: Golfe du Lion / eastern Occitanie)",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    bbox = tuple(float(value) for value in args.bbox.split(","))
    if len(bbox) != 4:
        parser.error("--bbox needs south,west,north,east")

    updated = dt.date.today().isoformat()
    south, west, north, east = bbox
    existing = load_records()
    outside = {
        spot_id: record for spot_id, record in existing.items()
        if not (south <= record["lat"] <= north and west <= record["lon"] <= east)
    }
    removed = len(existing) - len(outside)
    elements = overpass(bbox)
    candidates = [record for element in elements if (record := record_for(element, updated))]
    unnamed = [record for record in candidates if not record["name"] and not record["hintName"]]
    hints_for(unnamed)
    qualify_hints([record for record in candidates if not record["name"]])
    overrides = load_overrides()
    for record in candidates:
        if record["id"] in overrides:
            record["hintName"] = overrides[record["id"]]
            record["hintKind"] = "override"
            record["hintOsm"] = ""
    print(f"OSM elements: {len(elements)}; usable: {len(candidates)}")
    print(f"replacing {removed} rows inside the bbox")
    for record in sorted(candidates, key=lambda item: (item.get("name") or item.get("hintName") or "").casefold()):
        print(f"  * {record['id']} {record.get('name') or record.get('hintName')} ({record['tile']})")

    if args.dry_run:
        return 0

    write_hints_csv(candidates, overrides)
    merged = dict(outside)
    for record in candidates:
        merged[record["id"]] = record

    manifest = json.loads(MANIFEST.read_text())
    write_catalog(merged, int(manifest.get("version", 0)) + 1, updated)
    print(f"wrote {len(merged)} spots")
    return 0


if __name__ == "__main__":
    sys.exit(main())
