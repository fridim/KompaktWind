#!/usr/bin/env python3
"""Upsert named OSM wind-sport spots into the bundled catalog.

Usage:
    python3 scripts/update-osm-catalog.py --dry-run
    python3 scripts/update-osm-catalog.py

Defaults to the Occitanie bbox. Only OSM rows are touched; other bundled rows
are preserved. Unnamed spots get a hintName from the nearest named sport
facility/building, beach/water, or place feature in OSM.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import math
import os
import pathlib
import sys
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
SPOTS = ROOT / "spots"
INDEX = SPOTS / "spots.index.json"
MANIFEST = SPOTS / "manifest.json"
OVERPASS_URL = os.environ.get("OVERPASS_URL", "https://overpass-api.de/api/interpreter")
USER_AGENT = "KompaktWind-catalog-updater/1.0 (https://github.com/fridim/KompaktWind)"
OCCITANIE_BBOX = (42.31, 2.91, 43.61, 4.70)

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
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response).get("elements", [])


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
    return {
        "id": f"osm-{element['type']}-{element['id']}",
        "name": name,
        "shortName": name,
        "altNames": [],
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
        "hintName": "",
        "hintKind": "",
        "hintOsm": "",
    }


def hints_for(records: list[dict]) -> None:
    if not records:
        return
    pad = 0.06
    south = min(record["lat"] for record in records) - pad
    west = min(record["lon"] for record in records) - pad
    north = max(record["lat"] for record in records) + pad
    east = max(record["lon"] for record in records) + pad
    query = f"""[out:json][timeout:120];
(
  nwr["name"]["natural"~"^(beach|bay)$"]({south},{west},{north},{east});
  nwr["name"]["natural"="water"]({south},{west},{north},{east});
  nwr["name"]["place"~"^(city|town|village|hamlet|suburb|neighbourhood|locality)$"]({south},{west},{north},{east});
  nwr["name"]["tourism"~"^(attraction|viewpoint)$"]({south},{west},{north},{east});
  nwr["name"]["leisure"~"^(marina|nature_reserve|beach_resort)$"]({south},{west},{north},{east});
  nwr["name"]["sport"~"(^|;)(kitesurfing|windsurfing)(;|$)"]({south},{west},{north},{east});
);
out bb tags;"""
    request = urllib.request.Request(
        OVERPASS_URL,
        data=urllib.parse.urlencode({"data": query}).encode(),
        headers={"User-Agent": USER_AGENT},
    )
    try:
        with urllib.request.urlopen(request, timeout=150) as response:
            elements = json.load(response).get("elements", [])
    except Exception as error:
        print(f"  ! hint lookup failed: {error}")
        return

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

    def usable_hint(tags: dict) -> bool:
        name = tags.get("name", "").lower()
        if any(word in name for word in ("port de plaisance", "port de pêche", "marina", "bassin", "jet d'eau", "fontaine", "piscine")):
            return False
        if tags.get("natural") == "water":
            if tags.get("water") in {"sea", "ocean", "fountain", "basin", "wastewater", "swimming_pool"}:
                return False
            if tags.get("leisure") == "marina":
                return False
        return True

    def distance(element: dict, lat: float, lon: float) -> float:
        bounds = element.get("bounds")
        if bounds:
            south, west, north, east = (
                bounds["minlat"], bounds["minlon"], bounds["maxlat"], bounds["maxlon"]
            )
            d_lat = max(south - lat, 0.0, lat - north)
            d_lon = max(west - lon, 0.0, lon - east)
            return math.hypot(
                d_lat * 111_000,
                d_lon * 111_000 * math.cos(math.radians(lat)),
            )
        other_lat = element.get("lat")
        other_lon = element.get("lon")
        if other_lat is None or other_lon is None:
            return float("inf")
        d_lat = (other_lat - lat) * 111_000
        d_lon = (other_lon - lon) * 111_000 * math.cos(math.radians(lat))
        return math.hypot(d_lat, d_lon)

    usable = [
        element for element in elements
        if element.get("tags", {}).get("name") and usable_hint(element["tags"])
    ]
    for record in records:
        facilities = []
        candidates = []
        for element in usable:
            distance_m = distance(element, record["lat"], record["lon"])
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
        "--bbox", default=",".join(str(value) for value in OCCITANIE_BBOX),
        help="south,west,north,east",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    bbox = tuple(float(value) for value in args.bbox.split(","))
    if len(bbox) != 4:
        parser.error("--bbox needs south,west,north,east")

    updated = dt.date.today().isoformat()
    existing = load_records()
    elements = overpass(bbox)
    candidates = [record for element in elements if (record := record_for(element, updated))]
    unnamed = [record for record in candidates if not record["name"]]
    hints_for(unnamed)
    added = [record for record in candidates if record["id"] not in existing]
    changed = [record for record in candidates if record["id"] in existing]

    print(f"OSM elements: {len(elements)}; usable: {len(candidates)}")
    print(f"new: {len(added)}; existing updated: {len(changed)}")
    for record in sorted(added, key=lambda item: (item.get("name") or item.get("hintName") or "").casefold()):
        print(f"  + {record['id']} {record.get('name') or record.get('hintName')} ({record['tile']})")
    for record in sorted(changed, key=lambda item: (item.get("name") or item.get("hintName") or "").casefold()):
        print(f"  ~ {record['id']} {record.get('name') or record.get('hintName')} ({record['tile']})")

    if args.dry_run:
        return 0

    merged = dict(existing)
    for record in candidates:
        previous = merged.get(record["id"])
        if previous:
            record["shortName"] = previous.get("shortName") or record["shortName"]
            record["altNames"] = previous.get("altNames", [])
            record["windDirections"] = previous.get("windDirections", [])
            record["avoidWindDirections"] = previous.get("avoidWindDirections", [])
            record["description"] = previous.get("description", "")
            record["country"] = previous.get("country")
            record["region"] = previous.get("region")
            if not record["name"] and not record["hintName"]:
                record["hintName"] = previous.get("hintName", "")
                record["hintKind"] = previous.get("hintKind", "")
                record["hintOsm"] = previous.get("hintOsm", "")
        merged[record["id"]] = record

    manifest = json.loads(MANIFEST.read_text())
    write_catalog(merged, int(manifest.get("version", 0)) + 1, updated)
    print(f"wrote {len(merged)} spots")
    return 0


if __name__ == "__main__":
    sys.exit(main())
