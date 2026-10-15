# Black Cat Global

A separate, open-source Android app for finding worldwide legal TV broadcasters and authorised streaming services showing Sunderland AFC matches.

## Features

- Manual fixture search only — no background polling.
- Search links for Sunderland AFC announcements, Live Soccer TV, Premier League coverage, Google and Bing.
- Optional match date, competition and region/country query fields.
- Prioritises official sources and reminds users to verify listings.
- Does not host streams, bypass paywalls, or evade geographic restrictions.

## Build

Open this repository in Android Studio, or run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds a debug APK on pushes and manual workflow dispatch. The APK is uploaded as an Actions artifact.

## Important limitation

The app opens search results from external providers. Search rankings and listing availability depend on those providers; the app does not claim that every country or broadcaster has been exhaustively checked. Always confirm match-specific rights with the broadcaster.

## Licence

MIT. See [LICENSE](LICENSE).
