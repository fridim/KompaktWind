#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${OUT:-$ROOT/screenshots}"
ADB="${ADB:-adb}"
PKG="com.kompaktwind"
ACTIVITY="$PKG/.MainActivity"

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
    adb_ shell input tap $coords
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
restore() {
    if [[ -n "$original_timeout" && "$original_timeout" != "null" ]]; then
        adb_ shell settings put system screen_off_timeout "$original_timeout" >/dev/null 2>&1 || true
    fi
}
trap restore EXIT

adb_ shell settings put system screen_off_timeout 600000
adb_ shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb_ shell am force-stop "$PKG"
adb_ shell am start -n "$ACTIVITY" >/dev/null
wait_for text "Favorites" 20
sleep 1

echo "Capturing screenshots to $OUT"
shot 01-spots.png

spot="$(first_spot_text)"
if [[ -z "$spot" ]]; then
    echo "No saved spots found. Add at least one spot before capturing." >&2
    exit 1
fi

spot_coords="$(ui_coords text "$spot")"
adb_ shell input swipe $spot_coords $spot_coords 900
wait_for desc "Up" 5
shot 02-spot-actions.png

tap text "$spot"
wait_for desc "Back" 10
wait_for text "H" 15
sleep 1
shot 03-forecast.png

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
sleep 1
shot 06-live.png

station="$(first_station_text)"
tap text "$station"
wait_for desc "Back" 10
sleep 2
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
shot 07-add-spot-map.png
adb_ shell input keyevent KEYCODE_BACK
wait_for text "Favorites" 10
sleep 1

tap text "Settings"
wait_for text "Language" 10
sleep 1
shot 08-settings.png

echo "Done."
