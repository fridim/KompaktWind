# Screenshots

Captured from a Mudita Kompakt (480x800) with English UI and the current app
settings. The sequence is driven over adb and saved here by:

```bash
./scripts/capture-screenshots.sh
```

Requirements:

- Device connected and authorized (`adb devices` shows `device`)
- Device unlocked and awake
- App installed (`adb install -r app/build/outputs/apk/debug/app-debug.apk`)
- At least one saved spot

`ANDROID_SERIAL` and `ADB` can be set to target a specific device/tool.

Files:

| File | Screen |
|---|---|
| `01-spots.png` | Saved spots |
| `02-spot-actions.png` | Long-press actions |
| `03-forecast.png` | 7-day forecast |
| `04-forecast-picker.png` | Forecast presets |
| `05-spots-forecast.png` | Spots with predicted wind |
| `06-live.png` | Live stations |
| `07-add-spot-map.png` | Add spot, map tab |
| `08-settings.png` | Settings |
| `09-station-detail.png` | Live station detail (wind/gust) |
