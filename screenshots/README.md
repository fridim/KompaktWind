# Screenshots

Captured from a Mudita Kompakt (480x800) with English UI and the current app
settings. The sequence is driven over adb and saved here by:

```bash
./scripts/capture-screenshots.sh
```

Requirements:

- Device connected and authorized (`adb devices` shows `device`)
- Device unlocked and awake
- App/UI language set to English (the script matches English labels)
- App installed (`adb install -r app/build/outputs/apk/debug/app-debug.apk`)
- At least one saved spot with nearby live stations

`ANDROID_SERIAL` and `ADB` can be set to target a specific device/tool.
The script opens the saved spot named `Wellington` by default and pans the map
to the Golfe du Lion before selecting Saint-Eugène (Frontignan); set `FORECAST_SPOT` to
use another saved spot for the forecast/live screenshots. The map camera
defaults offshore in the Golfe du Lion (`42.9, 3.8`); override it with
`MAP_CAMERA_LAT`/`MAP_CAMERA_LON`, and the selected spot with `MAP_SPOT_LAT`/
`MAP_SPOT_LON`.

Files:

| File | Screen |
|---|---|
| `01-spots.png` | Saved spots |
| `02-spot-actions.png` | Long-press actions |
| `03-forecast.png` | 7-day forecast with live station |
| `04-forecast-picker.png` | Forecast presets |
| `05-spots-forecast.png` | Spots with predicted wind |
| `06-live.png` | Live stations |
| `07-add-spot-map.png` | Add spot map, Golfe du Lion spot selected |
| `08-settings.png` | Settings |
| `09-station-detail.png` | Live station detail (wind/gust) |
| `10-color.png` | Optional color wind values (color-display preview) |
| `11-tides.png` | La Baule forecast with the Tides column enabled (large tidal range) |
