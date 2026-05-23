# KompaktWind

KompaktWind is a focused weather forecast app for the Mudita Kompakt e-ink phone,
targeting outdoor sports (sailing, surfing, paragliding). It uses the
Mudita Mindful Design (MMD) library for an e-ink-optimized UI and a pluggable
weather-provider layer with Open-Meteo as the default free data source.

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
