#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${OUT:-$ROOT/screenshots}"
ADB="${ADB:-adb}"
PKG="com.kompaktwind"
ACTIVITY="$PKG/.MainActivity"
FORECAST_SPOT="${FORECAST_SPOT:-Wellington}"
MAP_CAMERA_LAT="${MAP_CAMERA_LAT:-42.9}"
MAP_CAMERA_LON="${MAP_CAMERA_LON:-3.8}"
MAP_SPOT_LAT="${MAP_SPOT_LAT:-43.4512055}"
MAP_SPOT_LON="${MAP_SPOT_LON:-3.809512}"

ADB_ARGS=("$ADB")
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    ADB_ARGS+=(-s "$ANDROID_SERIAL")
fi

adb_() {
    "${ADB_ARGS[@]}" "$@"
}

dump_ui() {
    local xml=""
    for _ in 1 2 3; do
        adb_ shell uiautomator dump /sdcard/kompaktwind-ui.xml >/dev/null 2>&1 || true
        xml="$(adb_ exec-out cat /sdcard/kompaktwind-ui.xml 2>/dev/null || true)"
        if [[ "$xml" == *"</hierarchy>"* ]]; then
            printf '%s' "$xml"
            return 0
        fi
        sleep 0.5
    done
    printf '%s' "$xml"
}

ui_coords() {
    local mode="$1" value="$2"
    dump_ui | python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

mode, value = sys.argv[1], sys.argv[2]
root = ET.fromstring(sys.stdin.read())
contains = mode.endswith("_contains")
desc = mode.startswith("desc")

for node in root.iter("node"):
    haystack = node.get("content-desc", "") if desc else node.get("text", "")
    matches = (value in haystack) if contains else (haystack == value)
    if matches:
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = (int(part) for part in match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
' "$mode" "$value"
}

tap() {
    local coords
    coords="$(ui_coords "$1" "$2")"
    if [[ -z "$coords" ]]; then
        echo "Not found on screen: $1 '$2'" >&2
        exit 1
    fi
    adb_ shell input tap "$coords"
}

wait_for() {
    local mode="$1" value="$2" timeout="${3:-20}"
    local deadline=$((SECONDS + timeout))
    while ((SECONDS < deadline)); do
        if [[ -n "$(ui_coords "$mode" "$value")" ]]; then
            return 0
        fi
        sleep 1
    done
    echo "Timed out waiting for $mode '$value'" >&2
    return 1
}

ui_dark_checked() {
    dump_ui | python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

root = ET.fromstring(sys.stdin.read())
for node in root.iter("node"):
    if node.get("checkable") == "true":
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if match and int(match.group(2)) < 350:
            print("true" if node.get("checked") == "true" else "false")
            break
'
}

open_settings() {
    tap desc "Settings"
    wait_for text "Language" 10
}

set_wind_colors() {
    local desired="$1" current
    adb_ shell input swipe 240 300 240 700 400
    sleep 1
    wait_for text "Wind colors" 10
    if [[ -n "$(ui_coords text "$desired")" ]]; then
        return
    fi
    if [[ "$desired" == "Color" ]]; then
        current="Monochrome"
    else
        current="Color"
    fi
    tap text "$current"
    wait_for text "$desired" 5
    tap text "$desired"
    sleep 1
}

set_dark_mode() {
    local desired="$1" checked
    adb_ shell input swipe 240 300 240 700 400
    sleep 1
    adb_ shell input swipe 240 650 240 250 400
    sleep 1
    wait_for text "Dark screen (white on black)" 10
    checked="$(ui_dark_checked)"
    if [[ "$checked" != "$desired" ]]; then
        adb_ shell input tap 424 256
        sleep 1
        checked="$(ui_dark_checked)"
        if [[ "$checked" != "$desired" ]]; then
            adb_ shell input tap 424 256
            sleep 1
        fi
    fi
}

wait_no_text() {
    local value="$1" timeout="${2:-20}"
    local deadline=$((SECONDS + timeout))
    while ((SECONDS < deadline)); do
        if ! dump_ui | grep -Fq "text=\"$value\""; then
            return 0
        fi
        sleep 1
    done
    return 0
}

first_spot_text() {
    dump_ui | python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

root = ET.fromstring(sys.stdin.read())
ignore = {"Favorites", "Near me", "Settings", "Forecast", "Add spot", "Live"}
for node in root.iter("node"):
    text = node.get("text", "").strip()
    if not text or text in ignore:
        continue
    if re.fullmatch(r"[-+0-9.,° ]+", text):
        continue
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if match and int(match.group(2)) >= 120:
        print(text)
        break
'
}


map_camera() {
    dump_ui | python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

root = ET.fromstring(sys.stdin.read())
for node in root.iter("node"):
    text = (node.get("text") or "").strip()
    match = re.fullmatch(r"(-?\d+\.\d+),\s*(-?\d+\.\d+)", text)
    if match:
        print(match.group(1), match.group(2))
        break
'
}

first_station_text() {
    dump_ui | python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

root = ET.fromstring(sys.stdin.read())
ignore = {"Live stations", "Nearest stations"}
for node in root.iter("node"):
    text = node.get("text", "").strip()
    if not text or text in ignore:
        continue
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if match and int(match.group(2)) >= 180:
        print(text)
        break
'
}

shot() {
    adb_ shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    sleep 0.3
    adb_ exec-out screencap -p > "$OUT/$1"
    if [[ ! -s "$OUT/$1" ]]; then
        echo "Screenshot is empty: $1" >&2
        exit 1
    fi
    echo "  captured $1"
}

require_device() {
    if [[ "$(adb_ get-state 2>/dev/null || true)" != "device" ]]; then
        echo "No adb device connected. Connect the Kompakt and authorize USB debugging." >&2
        exit 1
    fi
}

require_unlocked() {
    if adb_ shell dumpsys trust 2>/dev/null | grep -q "deviceLocked=1"; then
        echo "The device is locked. Unlock it first and keep it awake." >&2
        exit 1
    fi
}

require_device
require_unlocked

mkdir -p "$OUT"

original_timeout="$(adb_ shell settings get system screen_off_timeout | tr -d '\r')"
TIDES_WAS_ON=0
TIDES_TOUCHED=0
restore() {
    if [[ -n "$original_timeout" && "$original_timeout" != "null" ]]; then
        adb_ shell settings put system screen_off_timeout "$original_timeout" >/dev/null 2>&1 || true
    fi
    if [[ "$TIDES_TOUCHED" == "1" ]]; then
        local tide_visible=0 coords
        if [[ -n "$(ui_coords text "Tide" 2>/dev/null || true)" ]]; then
            tide_visible=1
        fi
        if [[ "$tide_visible" != "$TIDES_WAS_ON" ]]; then
            coords="$(ui_coords text "Tides" 2>/dev/null || true)"
            if [[ -n "$coords" ]]; then
                adb_ shell input tap "$coords" >/dev/null 2>&1 || true
            fi
        fi
    fi
}
trap restore EXIT

adb_ shell settings put system screen_off_timeout 600000
adb_ shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb_ shell am force-stop "$PKG"
adb_ shell am start -n "$ACTIVITY" >/dev/null
sleep 2
wait_for text "Favorites" 20
sleep 1

open_settings
set_wind_colors Monochrome
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
sleep 1

echo "Capturing screenshots to $OUT"
shot 01-spots.png

spot="$FORECAST_SPOT"
if [[ -z "$(ui_coords text "$spot")" ]]; then
    echo "Saved spot '$spot' not found. Set FORECAST_SPOT to an existing spot." >&2
    exit 1
fi

spot_coords="$(ui_coords text "$spot")"
adb_ shell input swipe "$spot_coords" "$spot_coords" 900
wait_for desc "Up" 5
shot 02-spot-actions.png

tap text "$spot"
wait_for desc "Back" 10
wait_for text "H" 15
wait_for text_contains "Live ·" 45
wait_for text "Tides" 15
if [[ -n "$(ui_coords text "Tide")" ]]; then
    TIDES_WAS_ON=1
    TIDES_TOUCHED=1
    tap text "Tides"
    wait_for text "Sea" 10
fi
sleep 1
shot 03-forecast.png

TIDES_TOUCHED=1
tap text "Tides"
wait_for text "Tide" 10
sleep 1
shot 11-tides.png
# Leave tides off for the remaining screenshots; the initial state is restored
# after the color-mode forecast capture.
tap text "Tides"
wait_for text "Sea" 10
sleep 1

adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
sleep 1

tap desc "Forecast"
wait_for text "Forecast in" 10
shot 04-forecast-picker.png
tap text "+6 h"
wait_for text_contains "Forecast ·" 10
wait_no_text "…" 20
sleep 1
shot 05-spots-forecast.png
tap desc "Close forecast"
sleep 1

tap text "$spot"
wait_for desc "Back" 10
sleep 3
if [[ -n "$(ui_coords text_contains "Live ·")" ]]; then
    tap text_contains "Live ·"
else
    tap text_contains "Loading live"
fi
wait_for text "Live stations" 15
wait_for text "Nearest stations" 45
sleep 1
shot 06-live.png

station="$(first_station_text)"
tap text "$station"
wait_for desc "Back" 10
wait_for text_contains "Observed" 20
sleep 1
shot 09-station-detail.png

adb_ shell input keyevent KEYCODE_BACK
wait_for text "Nearest stations" 10
adb_ shell input keyevent KEYCODE_BACK
wait_for desc "Back" 10
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
sleep 1

tap desc "Add spot"
wait_for text "Search" 10
tap text "Map"
sleep 2
# Pan to the Golfe du Lion and select a known spot.
for _ in 1 2 3 4 5; do
    read -r cam_lat cam_lon < <(map_camera)
    read -r cross_x cross_y < <(ui_coords text "+")
    if [[ -z "${cam_lat:-}" || -z "${cross_x:-}" ]]; then
        echo "Could not read the map camera position" >&2
        exit 1
    fi
    if python3 -c "import sys; sys.exit(0 if abs($cam_lat-$MAP_CAMERA_LAT)<0.12 and abs($cam_lon-$MAP_CAMERA_LON)<0.12 else 1)"; then
        break
    fi
    read -r dx dy < <(python3 - "$cam_lat" "$cam_lon" "$MAP_CAMERA_LAT" "$MAP_CAMERA_LON" <<'PY'
import math
import sys

cam_lat, cam_lon, target_lat, target_lon = map(float, sys.argv[1:])
ppd = 256.0 * 2.0 ** 6.5 / 360.0
cos_lat = max(0.2, math.cos(math.radians(cam_lat)))
dx = max(-250, min(250, (target_lon - cam_lon) * ppd * cos_lat))
dy = max(-250, min(250, (cam_lat - target_lat) * ppd))
print(round(dx), round(dy))
PY
)
    adb_ shell input swipe "$cross_x" "$cross_y" "$((cross_x - dx))" "$((cross_y - dy))" 300
    sleep 1
done
read -r cam_lat cam_lon < <(map_camera)
read -r cross_x cross_y < <(ui_coords text "+")
read -r tap_x tap_y < <(python3 - "$cam_lat" "$cam_lon" "$cross_x" "$cross_y" "$MAP_SPOT_LAT" "$MAP_SPOT_LON" <<'PY'
import math
import sys

cam_lat, cam_lon, cross_x, cross_y, target_lat, target_lon = map(float, sys.argv[1:])
ppd = 256.0 * 2.0 ** 6.5 / 360.0
cos_lat = max(0.2, math.cos(math.radians(cam_lat)))
x = cross_x + (target_lon - cam_lon) * ppd * cos_lat
y = cross_y - (target_lat - cam_lat) * ppd
print(round(x), round(y))
PY
)
adb_ shell input tap "$tap_x" "$tap_y"
wait_for text_contains "Known spots" 15
sleep 1
shot 07-add-spot-map.png
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
sleep 1

tap text "Settings"
wait_for text "Language" 10
sleep 1
shot 08-settings.png

set_wind_colors Color
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
tap text "$FORECAST_SPOT"
wait_for desc "Back" 10
wait_for text "H" 15
wait_for text_contains "Live ·" 45
sleep 1
shot 10-color.png
if [[ "$TIDES_WAS_ON" == "1" ]]; then
    tap text "Tides"
    wait_for text "Tide" 10
fi
TIDES_TOUCHED=0

# Restore the default light/monochrome display for future runs.
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
open_settings
set_wind_colors Monochrome
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10

echo "Done."
