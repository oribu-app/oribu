# HobbiesVault — CLAUDE.md

Native Android app (Kotlin + Jetpack Compose) for personal tracking of games, manga, webtoons, series, movies and books. No server of its own, no account. All data stays locally on the device.

> The project was migrated from Flutter to native Kotlin/Compose. The old Flutter version is archived at `../Hobbies-Backup` (outside this repository) and serves only as historical reference — don't edit it or treat it as a source of truth.

---

## Stack and main dependencies

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Local database | Room (`androidx.room`) |
| Build | Gradle Kotlin DSL (`build.gradle.kts`), plugins via `libs.versions.toml`, KSP for codegen |
| Navigation | Compose Navigation (`MainNavGraph.kt`, `Routes.kt`) |
| Serialization | Gson |
| Background tasks | WorkManager (`CacheUpdateWorker.kt`) |
| HTTP | (check the client used in `*Service.kt`, e.g. OkHttp/Retrofit) |

---

## Folder structure

```
app/src/main/kotlin/com/hobbiesvault/
  MainActivity.kt
  HobbiesVaultApp.kt

  data/
    db/
      AppDatabase.kt              # @Database Room, migrations, schemaVersion
      DB.kt                       # Singleton/DI access to the database
      dao/
        MediaItemDao.kt
        MediaDetailsCacheDao.kt
        FilmeListaDao.kt
        GameCacheDao.kt
        SerieEpisodioDao.kt
      entity/
        MediaItemEntity.kt
        MediaDetailsCacheEntity.kt
        FilmeListaEntity.kt
        GameCacheEntity.kt
        SerieEpisodioEntity.kt
    repository/
      MediaRepository.kt          # MediaItem CRUD
      MediaCacheRepository.kt     # Read/write of the JSON cache in the database

  model/
    MediaItem.kt
    MediaType.kt
    MediaStatus.kt
    GameConsole.kt
    ApiSearchResult.kt

  service/
    ApiServices.kt          # Central registry of all API services
    Secrets.kt              # Loads secrets.json from app/src/main/assets at runtime
    TmdbService.kt          # TMDB — movies and series
    IgdbService.kt          # IGDB — games
    HltbService.kt          # HowLongToBeat — game playtime
    ItadService.kt          # IsThereAnyDeal — game prices
    GameSearchService.kt
    GameCacheService.kt
    GameDatasetImporter.kt
    AniListService.kt       # AniList (GraphQL) — main source for manga/webtoons + progress sync
    MangaDexService.kt      # MangaDex — search/detail fallback and latest chapter count for ongoing series
    MangaSearchService.kt   # Orchestrates AniList + MangaDex
    GoogleBooksService.kt   # Google Books — books
    OpenLibraryService.kt   # Open Library — books (fallback)
    BookSearchService.kt    # Orchestrates Google Books + Open Library
    SteamService.kt         # Steam Web API — library and achievements
    PsnService.kt           # PSN — trophies (public, no auth)
    MediaCacheService.kt    # Orchestrates the cache: fetch + comparison + persistence

  worker/
    CacheUpdateWorker.kt    # Daily cache-refresh task (WorkManager)

  ui/
    theme/
      AppTheme.kt
      Color.kt              # Colors by media type, platforms and app themes
    navigation/
      Routes.kt
      MainNavGraph.kt
    components/
      SharedComponents.kt
    screens/
      HomeScreen.kt
      SearchScreen.kt
      HistoryScreen.kt
      StatsScreen.kt
      CalendarScreen.kt
      SettingsScreen.kt

      games/
        GamesScreen.kt
        AddGameScreen.kt
        GameDetailScreen.kt
      films/
        FilmsScreen.kt
        AddFilmScreen.kt
        FilmDetailScreen.kt
      series/
        SeriesScreen.kt
        AddSeriesScreen.kt
        SeriesDetailScreen.kt
      manga/
        MangaScreen.kt
        AddMangaScreen.kt
        MangaDetailScreen.kt
      books/
        BooksScreen.kt
        AddBookScreen.kt
        BookDetailScreen.kt

assets/
  data/                      # static datasets (e.g. game import)
  trofeus/                   # PSN trophy icons/data
```

---

## Database (Room)

**Current schema: v10** (see `AppDatabase.kt` for the full migration history)

Main entities:

### `MediaItemEntity` (table `media_items`)
Main fields: `id`, `tipo`, `titulo`, `status`, `nota`, `comentario`, `capaUrl`, `dataAdicaoMs`, `dataConclusaoMs`, `favorito`, `idExterno`, `fonteApi`.

Type-specific fields:
- **Game:** `console`, `horasJogadasMinutos`, `conquistasDesbloqueadas`, `conquistasTotal`, `trofeusOuro`, `trofeusPrata`, `trofeusBronze`, `trofeuPlatina`, `desenvolvedor`
- **Manga/Book/Series:** `progressoAtual`, `progressoTotal`
- **Movie/Series:** `streamingPlataforma`
- **All:** `dataLancamentoMs`, `genero`
- Fields added in the most recent migrations (v7-v10): `dataInicioLeituraMs`, `dataReleituraMs`, `dataConclusaoHistoriaMs`, `dataConclusaoExtrasMs`, `dataConclusaoPlatinaMs`

### `MediaDetailsCacheEntity` (table `media_details_cache`)
Cache of the full API details per item. Fields: `mediaItemId`, `dadosJson` (full JSON), `ultimaVerificacaoMs`.

### `FilmeListaEntity` / list-item table (`filme_listas`, `filme_lista_itens`)
Custom movie lists.

### `GameCacheEntity` (table `game_cache`)
Cache of game metadata (name, GB/IGDB ids, cover, genres, platforms etc.).

### `SerieEpisodioEntity` (table `serie_episodios_assistidos`)
Tracks watched episodes per series (season, episode, date).

### Access via the DB singleton

```kotlin
import com.hobbiesvault.data.db.DB

// Reading/writing items — via MediaRepository / DAOs
mediaRepository.salvar(item)
mediaRepository.atualizar(item)
mediaRepository.deletar(id)
mediaRepository.porTipo(MediaType.MOVIE)

// Details cache — via MediaCacheRepository
mediaCacheRepository.carregar(mediaItemId)
mediaCacheRepository.salvar(mediaItemId, dadosJson)
mediaCacheRepository.deletar(mediaItemId)
```

### ⚠️ Cardinal rule: existing migrations are IMMUTABLE

**Never edit an already-existing migration — only add a new one.** Each
`Migration` is a historical fact: on upgrade, Room only runs the migrations
between the installed `version` and the new one — it never re-applies the old
ones. Editing an already-published migration only changes the result for a
fresh install, and diverges from anyone who already has the app with that
schema applied. Any schema change is always a **new** `Migration`, never an
edit to an existing one.

### After any change to entities/DAOs

Increment `version` in `@Database` (`AppDatabase.kt`) and add a new `Migration` in the companion object following the existing pattern (`MIGRATION_N_N+1`), registering it when `Room.databaseBuilder` is created. There's no manual build_runner/codegen — Room uses KSP automatically during the Gradle build.

---

## Main models

### MediaType
```kotlin
enum class MediaType(val label: String, val dbValue: String) {
    GAME, MANGA, WEBTOON, SERIES, MOVIE, BOOK
}
```
`dbValue` keeps the original Portuguese names (`jogo`, `manga`, `webtoon`, `serie`, `filme`, `livro`) for compatibility with the schema migrated from Flutter/Drift.

### MediaStatus
```kotlin
enum class MediaStatus(val label: String, val dbValue: String) {
    // Games
    COMPLETED, FINISHED, PLAYING, REPLAYING, PLATINUM,
    // Movies
    WATCHED, WATCHING, REWATCHING,
    // Series
    CONCLUDED, HISTORY, WAITING_EPISODES,
    // Manga/Books
    READ, READING, REREADING, ON_HOLD,
    // All
    QUEUED, DROPPED, WAITING_RELEASE
}
```

Per type/platform list methods (`MediaStatus` companion object):
- `forSteam()`, `forPlayStation()`, `forNintendo()`, `forOtherGames()` — variations per game platform
- `forMovie()` → [WATCHED, REWATCHING, QUEUED, WAITING_RELEASE]
- `forSeries()` / `forSeriesAdd()` → [WATCHING, REWATCHING, QUEUED, HISTORY]
- `forManga()` → [READING, REREADING, ON_HOLD, READ, QUEUED, WAITING_RELEASE]
- `forMangaAdd()` → [READING, REREADING, QUEUED]
- `forBook()` → [READING, REREADING, READ, QUEUED, DROPPED]
- `forBookAdd()` → [READING, REREADING, QUEUED]

### MediaItem
Kotlin data class — use `.copy()` for immutable updates (equivalent to Flutter's `copyWith()`). The `idExterno` and `fonteApi` fields identify the origin in the API.

---

## Cache and data refresh

### Mandatory pattern on every detail screen

Load the cache from the database immediately (sync/local) and trigger a background check via `MediaCacheService`, without blocking the UI — equivalent to the `_carregarCache()` + `doubleCheck()` pattern from the Flutter version.

### When adding an item to the library
Call `MediaCacheService` to populate the cache immediately after saving the item to the database (`MediaRepository`), using the id generated by Room.

### Daily routine (WorkManager)
`CacheUpdateWorker` runs periodically and triggers the refresh of every item via `MediaCacheService`:
1. Iterates over every item with `idExterno`
2. Fetches the matching API by type
3. Compares against the current cache (hash/JSON comparison)
4. Only persists if something changed
5. Checks for status changes in series (Ended/Cancelled → moves to History)

**When changing any detail screen:** verify that the JSON generated in `MediaCacheService` reflects the newly displayed fields.

### Manga progress sync via AniList
If `anilist_username` is set in `secrets.json`, every cache update for a manga/webtoon added via AniList (`apiSource == "anilist"`) queries the user's public AniList list (`AniListService.getUserProgress`) and only advances `currentProgress` when the remote value is greater than the local one — it never regresses a more recent manual edit. No OAuth required (same static-credential logic already used by Steam/PSN); only works if the user's AniList profile isn't private.

---

## APIs per media type

| Type | Main service | Fallback |
|---|---|---|
| Movies | TMDB (`TmdbService`) | — |
| Series | TMDB (`TmdbService`) | — |
| Games | IGDB (`IgdbService`, requires a Twitch token); HLTB (`HltbService`) and ITAD (`ItadService`) for supplementary data | Prefix search / local dataset (`GameDatasetImporter`) |
| Manga/Webtoons | AniList (`AniListService`, GraphQL) | MangaDex (`MangaDexService`) — only triggered when AniList returns no results (see `MangaSearchService.kt`); also provides the latest chapter count for ongoing series via the `/aggregate` endpoint |
| Books | Google Books (`GoogleBooksService`) | Open Library (`OpenLibraryService`) |

### Availability checked before use
```kotlin
if (!secrets.tmdbConfigurado) { /* show error */ }
if (!secrets.igdbConfigurado) { /* show error */ }
if (!secrets.steamConfigurado) { /* show error */ }
if (!secrets.itadConfigurado) { /* show error */ }
```
Availability flags live in `Secrets` (`app/src/main/kotlin/com/hobbiesvault/service/Secrets.kt`), loaded once (singleton) from `secrets.json`.

### secrets.json (`app/src/main/assets/secrets.json`, never commit)
```json
{
  "tmdb_bearer_token": "eyJhbGci...",
  "igdb_client_id": "abc123",
  "igdb_client_secret": "xyz789",
  "google_books_api_key": "AIza...",
  "anilist_client_id": "12345",
  "anilist_username": "your_anilist_username",
  "steam_api_key": "ABCD1234",
  "steam_id": "76561198XXXXXXXXX",
  "itad_api_key": "..."
}
```

---

## Routes (Compose Navigation)

Defined in `Routes.kt` and wired in `MainNavGraph.kt`.

| Route | Screen |
|---|---|
| `home` | HomeScreen |
| `games` | GamesScreen |
| `games/add` | AddGameScreen |
| `games/detail` | GameDetailScreen |
| `films` | FilmsScreen |
| `films/add` | AddFilmScreen |
| `films/detail` | FilmDetailScreen |
| `series` | SeriesScreen |
| `series/add` | AddSeriesScreen |
| `series/detail` | SeriesDetailScreen |
| `manga` | MangaScreen |
| `manga/add` | AddMangaScreen |
| `manga/detail` | MangaDetailScreen |
| `books` | BooksScreen |
| `books/add` | AddBookScreen |
| `books/detail` | BookDetailScreen |
| `search` | SearchScreen |
| `settings` | SettingsScreen |
| `history` | HistoryScreen |
| `stats` | StatsScreen |
| `calendar` | CalendarScreen |

Passing an object between screens: follow the existing NavGraph pattern (savedStateHandle / route arguments), check `MainNavGraph.kt` for the mechanism in use before adding new routes with complex parameters.

---

## Colors per media type (Color.kt)

```kotlin
ColorJogo        // #7B1FA2 (purple)
ColorManga       // #E91E63 (pink)
ColorWebtoon     // #00BCD4 (cyan)
ColorSerie       // #1976D2 (blue)
ColorFilme       // #FF6F00 (orange)
ColorLivro       // #388E3C (green)
ColorSteam       // #1B2838
ColorPlayStation // #00439C
ColorNintendo    // #E4000F
ColorXbox        // #107C10
```

`Color.kt` also defines `appThemes`: a list of `AppThemeDefinition` (themes named Neko, Tako, Yin, Doki, Oceano, Meia-noite) with seeds and background/surface colors for dark/light.

---

## Code conventions

### Naming and null-safety (Kotlin)

- `UpperCamelCase` for classes, enums and objects; `lowerCamelCase` for
  variables, functions and parameters. `_privateMember` isn't a Kotlin
  convention — just use `private`.
- Prefer explicit types and `val` (immutable) over `var`. Avoid `!!` (the
  not-null assertion); handle nulls via `?.`, `?:` or an explicit `if`/`when`
  — only use `!!` when there's no real alternative.
- Prefer `data class` for models and `.copy()` for immutable updates (never
  mutate a `var` field on an already-persisted model).
- Prefer coroutines/`Flow` over callbacks; avoid global mutable state.

### Comments

- A comment exists to explain the **why**, never the **what** — names and
  types already say what the code does. The `commitCount` comment in
  `app/build.gradle.kts` (explaining the fallback outside a git repo) is a
  good example; a `// saves the item to the database` above
  `repository.salvar(item)` would not be.
- No file header ("what this file is") and no section banners
  (`// ==== Foo ====`) — the file path and the first declaration already say
  that.
- No comment beats a bad comment — if there's nothing non-obvious to explain,
  don't write one.

### Lists and grids
Follow the visual pattern from the Flutter version: `LazyVerticalGrid` with 3 columns and a portrait cover ratio (~0.56), unless the current Compose implementation already diverges — check the list `*Screen.kt` files before assuming.

### Grid cards
Portrait cover + title below. No status, rating or any other info on the card. Status and details live on the detail screen.

### Persistence on detail screens
Always persist via `MediaRepository`/`MediaCacheRepository` (never just local in-memory state). When removing an item, also delete the matching cache (`MediaCacheRepository.deletar(id)`).

### Duplicate detection in search
Search must identify items whose `idExterno` already exists in the library (same pattern as the Flutter version's `existingIds`) and flag duplicates in the results UI.

---

## Series-specific logic

Series get automatic status transitions based on TMDB:

- **TMDB status `Ended`/`Cancelled`** → moves to `MediaStatus.HISTORY`
- **TMDB status `Returning Series`** with no confirmed next season → moves to `WAITING_RELEASE`

Checks happen at:
1. `SeriesScreen` initialization — validates series in `WAITING_RELEASE`
2. When marking the last watched episode on the detail screen
3. `CacheUpdateWorker`'s daily routine — checks every series

---

## Useful commands

```bash
# Debug build
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

---

## Definition of done

Before reporting a task as complete, follow this order:

1. **Lint** — `./gradlew lintKotlin` (the same check CI runs in
   `.github/workflows/lint.yml`).
2. **Tests** — `./gradlew test`.
3. **Schema migration, if the change touched an entity/DAO** — check the
   migration cardinal rule above (new `Migration`, never edit an existing
   one) and that the `@Database` `version` was incremented.
4. **Manual verification** — install (`./gradlew installDebug`) and exercise
   the changed flow on a device/emulator before reporting it done, especially
   for detail screens (the cache-first + `doubleCheck()` pattern described
   above).
5. **CHANGELOG**, if the change is user-visible — use the project's
   simplified format (`Additions`/`Changes`/`Fixes`/`Other`) in the
   `[Unreleased]` section of `CHANGELOG.md`.

If lint or tests fail, fix and repeat from step 1 — don't skip steps or
report the task done with a red gate.

---

## What NOT to do

- Don't commit `secrets.json` (`app/src/main/assets/secrets.json`)
- Don't fetch from an API directly in detail screens (Composables) — use `MediaCacheService`
- Don't change the serializers in `MediaCacheService` without reflecting the new fields on the screens
- Don't forget to call `mediaCacheRepository.deletar(id)` when removing an item from the library
- Don't treat the Flutter project at `../Hobbies-Backup` as active code — it's only historical reference for the previous version
