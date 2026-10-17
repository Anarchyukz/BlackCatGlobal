# Black Cat Global

A separate, open-source Android app for finding legal worldwide TV coverage of Sunderland AFC.

## Current features

- Shows the next Sunderland fixture it can identify from public football TV guides.
- Checks TV Guide UK, Where's The Match, Live Soccer TV and EntertainmentBox when it refreshes; existing sources are retained rather than added twice.
- Attempts to extract the fixture, kickoff details and broadcaster names from each guide's public page.
- Merges channel names across guide pages and Google/Bing results, removing case and common naming-alias duplicates.
- Includes one-tap Google and Bing searches for worldwide, country-by-country listings using the detected fixture.
- No fixture form, no manual opponent entry, no background polling, and no streams hosted by the app.

## Important limitations

There is no single free, official worldwide TV-rights API that reliably lists every broadcaster for every football match. This version reads public guide pages where accessible and provides worldwide search links as a fallback. Guide websites can change their page structure or block automated requests, so a channel name may be missing even when the website itself has the answer. Always open the source and confirm the match, country and current rights.

The app is for finding authorised broadcasters only. It does not host streams or attempt to bypass paywalls or geographic restrictions.

## Build

Open this repository in Android Studio, or run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds a debug APK on pushes and manual workflow dispatch. The APK is uploaded as an Actions artifact.

## Licence

MIT. See [LICENSE](LICENSE).
