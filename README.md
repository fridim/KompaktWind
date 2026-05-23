# KompaktWind

KompaktWind is a focused weather forecast app for outdoor sports: wingfoil,
kitesurf, windsurf, sailing, … targeting the Mudita Kompakt e-ink. Open-Meteo as
the default free data source.

## Screenshots

<table>
<tr>
    <td>
        <img width="480" height="800" alt="Screenshot_20260523-234100" src="https://github.com/user-attachments/assets/851fa52c-c1df-496c-b283-475fc094a0d9" />
      </td>
      <td>
        <img width="480" height="800" alt="Screenshot_20260523-233637" src="https://github.com/user-attachments/assets/de86c128-0d32-4014-ba96-16ac4fb58788" />
      </td>
      <td>
<img width="480" height="800" alt="Screenshot_20260523-233645" src="https://github.com/user-attachments/assets/396421d1-74d0-465a-950a-6da3f5cd2e1a" />
      </td>
</tr>
    <tr>
    <td><img width="480" height="800" alt="Screenshot_20260523-204846" src="https://github.com/user-attachments/assets/5545ba93-9060-440d-8dae-b09188d7507a" /></td></tr>
</table>


## Features

- Saved spots (search by place name or enter coordinates manually)
- 7-day hourly forecast in a single dense table with sticky day headers
- Wind speed + gust, direction, air temperature, precipitation
- For coastal spots: wave height/period and water temperature
- Offline-friendly: cached forecasts shown with a clear age stamp
- Configurable units (m/s, km/h, kn, mph; °C, °F)

## Build

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Min SDK 28 (Android 9). Target SDK 35.

## Architecture

- Single-module Android app
- Kotlin + Jetpack Compose + Mudita Mindful Design (MMD) 1.0.0
- MVVM + StateFlow
- Retrofit + OkHttp + kotlinx-serialization
- Room for spots + forecast cache, DataStore for preferences

## Attribution

Weather data: [Open-Meteo](https://open-meteo.com), free non-commercial license.
Place-name search uses Open-Meteo's geocoding API.

## License

MIT. See [LICENSE](LICENSE).
