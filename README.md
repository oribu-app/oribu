<div align="center">

<a href="https://github.com/oribu-app/oribu-app">
    <img src="./.github/readme-images/app-icon-round.png" alt="Oribu logo" height="200px" width="200px" />
</a>

# Oribu

</div>

<div align="center">

A free and open source personal media tracker for games, manga, webtoons, series, movies and books

[![CI](https://github.com/oribu-app/oribu-app/actions/workflows/build_push.yml/badge.svg?labelColor=27303D)](https://github.com/oribu-app/oribu-app/actions/workflows/build_push.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/oribu-app/oribu-app?labelColor=27303D&color=0877d2)](/LICENSE)

## Download

[![Oribu Stable](https://img.shields.io/github/v/release/oribu-app/oribu-app?maxAge=3600&label=Stable&labelColor=06599d&color=043b69&filter=v*)](https://github.com/oribu-app/oribu-app/releases)
[![Oribu Nightly](https://img.shields.io/github/v/release/oribu-app/oribu-nightly?maxAge=3600&label=Nightly&labelColor=2c2c47&color=1c1c39&include_prereleases)](https://github.com/oribu-app/oribu-nightly/releases)

*Requires Android 8.0 or higher.*

## About

Oribu is a native Android app (Kotlin + Jetpack Compose) for keeping track of everything you play,
read and watch — games, manga, webtoons, series, movies and books — in one place.

There's no account, no server, no sync with a backend of any kind: every entry is stored locally
on your device with Room. The app talks directly to public metadata APIs (TMDB, IGDB, AniList,
MangaDex, Google Books, Steam, PSN...) just to fetch covers, details and progress, never to store
or share your data.

The project started as a Flutter app (HobbiesVault) and was later fully rewritten in native
Kotlin/Compose.

## Features

<div align="left">

* Track **games**, **manga/webtoons**, **series**, **movies** and **books** in a single library,
  each with its own statuses (playing, watching, reading, queued, dropped, platinum...).
* Rich per-type details: playtime and trophies/achievements for games, watched episodes for
  series, read chapters for manga, streaming platform for movies/series, ratings and reviews.
* Automatic metadata lookup and cover art from:
  * [TMDB](https://www.themoviedb.org/) for movies and series
  * [IGDB](https://www.igdb.com/), [HowLongToBeat](https://howlongtobeat.com/) and
    [IsThereAnyDeal](https://isthereanydeal.com/) for games
  * [AniList](https://anilist.co/) and [MangaDex](https://mangadex.org/) for manga/webtoons
  * [Google Books](https://books.google.com/) and [Open Library](https://openlibrary.org/) for books
  * [Steam](https://store.steampowered.com/) and [PSN](https://www.playstation.com/) for
    library/trophy sync
* Automatic series status tracking (moves to History when a show ends/is cancelled, flags
  "waiting for release" when there's no confirmed next season).
* Daily background refresh of cached metadata, with change detection so it only writes when
  something actually changed.
* Home, calendar, history and statistics screens to see your activity over time.
* Everything stored locally — no account, no cloud sync, no telemetry.

</div>

## Tech stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Local database | Room |
| Build | Gradle Kotlin DSL, version catalog (`libs.versions.toml`), KSP |
| Navigation | Compose Navigation |
| Background work | WorkManager |
| Networking | OkHttp |
| Images | Coil |

## Building

1. Clone the repo and open it in Android Studio (or use the Gradle wrapper directly).
2. Create `app/src/main/assets/secrets.json` with your own API credentials — see
   [`CLAUDE.md`](CLAUDE.md) for the expected keys. This file is gitignored and required only for
   the integrations you actually want to use; the app degrades gracefully when a given API isn't
   configured.
3. Build and run:

   ```bash
   ./gradlew installDebug   # build and install a debug build on a connected device/emulator
   ./gradlew assembleRelease
   ./gradlew test
   ```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you
would like to change. See [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md) for commit message
conventions, branch strategy and everything else you need to get set up.

<div align="left">

<details><summary>Issues</summary>

**Before reporting a new issue, take a look at the [changelog](https://github.com/oribu-app/oribu-app/releases) and the already opened [issues](https://github.com/oribu-app/oribu-app/issues).**

</details>

<details><summary>Bugs</summary>

* Include the app version (**Settings → About**).
  * Nightly version numbers match the total commit count shown on the main page.
* Include steps to reproduce (if not obvious from the description).
* Include a screenshot (if it helps).
* Don't group unrelated reports into a single issue.

</details>

<details><summary>Feature requests</summary>

* Write a detailed issue explaining what it should do and how.
* Avoid writing just "like X app does".
* Include a screenshot/mockup if it helps.

</details>

</div>

### Credits

Thank you to all the people who have contributed!

<a href="https://github.com/oribu-app/oribu-app/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=oribu-app/oribu-app" alt="Oribu contributors" title="Oribu contributors"/>
</a>

### Disclaimer

The developer(s) of this application are not affiliated with any of the content/metadata
providers it integrates with (TMDB, IGDB, AniList, MangaDex, Google Books, Open Library, Steam,
PSN, HowLongToBeat, IsThereAnyDeal), and this application hosts zero content of its own.

### License

<pre>
Copyright © 2026 Thiago Rocha

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
</div>
