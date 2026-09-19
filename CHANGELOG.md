# Changelog

All notable changes to this project will be documented in this file.

The format is a simplified version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/):
- `Additions` - New features
- `Changes` - Behaviour/visual changes
- `Fixes` - Bugfixes
- `Other` - Technical changes/updates

## [Unreleased]

### Additions
- Settings > General: a "Locale" section with Language (System default/English/Português) and
  Date format (System default/Day-Month-Year/Month-Day-Year/ISO) pickers. Language is applied
  app-wide via `AppCompatDelegate.setApplicationLocales`. The whole app's UI has been converted
  to `strings.xml` (English default, `values-pt-rBR` for Portuguese) — English is now the
  project's primary language throughout. Fixed status/category labels (`MediaStatus`,
  `GameConsole`) and the manga hiatus-tracking status (`serializationStatus`, which doubles as a
  state-transition key, not just display text) stay in English regardless of the language
  picker, same as the other enum-style labels; API genre translations (books/games/manga) still
  follow the language picker.
- Settings > Aparência: "Seguir tema do sistema" switch (replicates Rokku's real toggle — turning
  it off pins the theme to whatever the system's current brightness is, and picking a light/dark
  theme swatch also forces that mode explicitly, same as picking a swatch in Rokku), "Modo Escuro
  com preto absoluto" (pure black backgrounds), and "Usar cor do tema nas categorias"
  (jogos/mangás/séries/filmes/livros follow the selected theme's color instead of their own fixed
  accent). Theme catalog replaced with Rokku's real 8 followable + 2 single-mode palettes (exact
  names/colors from `Themes.kt`/`themes.xml`), instead of an invented palette.
- Settings > Aparência > "Página de detalhes" ("Details page" in Rokku's `SettingsAppearanceController.kt`):
  "Cores baseadas na capa" ("Theme buttons based on cover") extracts an accent color from each
  item's cover (`androidx.palette`, same heuristic as Rokku's `Palette.getBestColor()`) and, with
  "Estilo de cor da capa" ("Cover theme style"), generates a full color scheme from it via the
  `materialkolor` library's 9 styles (Tonal Spot, Neutral, Vibrant, Expressive, Rainbow, Fruit
  Salad, Monochrome, Fidelity, Content) or a Legacy mode that only swaps the accent color — same
  options as Rokku, but applied to all 5 detail screens (games/manga/series/movies/books) instead
  of manga only.
- Settings > Plataformas: choose which game platforms show up as filters in Jogos (all enabled by default).
- "Notas" (free-text notes) available from the "..." menu on every detail screen.
- Swipe left/right between the 5 hobby tabs.
- Books: star rating (0-5), a review section, and a Citações (quotes) section.
- Games: Preços section (current ITAD deals) with a real price-history chart (ITAD `games/history/v2`).
- Games: DLCs/Expansões and Recomendações sections, sourced from IGDB.
- Games: Jogatinas section — manual playthrough log (title, dates, hours, notes). Steam/PSN can't be
  auto-synced here since neither API exposes per-session data, only aggregate totals.
- Films: "Adicionar à lista" from the "..." menu (existing list or new).
- Series: toggle to show/hide "Séries relacionadas" from the "..." menu.
- Real launcher icon (dark background, gold kanji), with distinct debug (gray) and nightly (orange)
  variants so side-installed builds are easy to tell apart on the home screen.
- First-launch onboarding flow, in Rokku's mold: a fixed rocket-icon header, a rounded card with
  the current step, and a single full-width button to advance (no "back" button — the system
  back gesture/key steps back instead). Steps: theme (color mode + a live phone-mockup preview
  per palette, also used in Settings > Aparência), a storage folder picker (saved for future
  use), optional notification/battery optimization permission prompts, and a step to configure
  API keys for TMDB, IGDB, Google Books, Steam and ITAD with a "test connection" check per
  service — also available afterwards from Settings > Integrações, which is now editable instead
  of read-only.
- Sobre screen, in Rokku's mold: in-app update checking against GitHub Releases (stable and
  nightly channels), downloading and prompting to install the new APK, release notes link, a
  build-time row, a "Versão" row that copies debug info to the clipboard, an "Ajude a traduzir"
  row (not yet linked — pending Weblate setup), and an open-source licenses screen (via the
  AboutLibraries plugin). Also checks silently once a day and posts a notification when a new
  version is found.
- The "..." menu (Configurações, Status, Histórico, Sobre, Ajuda) is now the same on every hobby
  list screen (Jogos, Filmes, Séries, Mangás, Livros), not just Home.

### Changes
- Removed "Barra de ferramentas expandida" from Settings > Aparência — it was a persisted
  preference no screen ever read, so it did nothing.
- All Settings screens (hub, Aparência, Geral, Notificações, Plataformas, Dados, Integrações) now
  share the same building blocks (`SettingsScaffold`, `SettingsSectionHeader`, `SettingsSwitchRow`,
  `SettingsClickRow` — see `docs/design-system-settings.md`) instead of each screen having its own
  variant of the top bar/rows; Notificações, Plataformas, Dados and Integrações previously still
  used a plain default `TopAppBar` and default `ListItem`/`Switch` styling instead of the
  restyled "Rokku mold" look the hub/Aparência/Geral already had.
- Settings hub and Aparência screen restyled in Rokku's mold: large in-body title, borderless
  search field, flat category rows (icon + label only, no chevrons).
- Bottom bar reordered to Jogos, Filmes, Séries, Mangás, Livros.
- Overflow ("...") menus now dim the background and open anchored under the top bar, matching Rokku's style.
- All hobby list screens open Settings through a "..." menu instead of a direct shortcut icon.
- Larger, tighter tabs on the hobby list screens.
- Standardized rounded corners on remaining flat-cornered boxes across detail screens.
- Games/Manga "Informações"/"Datas" now render as individual cards instead of one shared box.
- Manga: added "Início da leitura" date, relabeled publication dates, moved Sinônimos to the end of the page.
- Series/Manga/Books status menus now show a checkmark on the current status.
- Series/Manga/Books progress bars: flat ends instead of the rounded "dot" look.
- Books: "Progresso" renamed to "Histórico de Leitura"; publication date relabeled "Publicação desta edição".
- All search fields now have a clear (X) button.
- Films/Series: cast/crew now render as a horizontal avatar list (same pattern as Manga), section order
  changed (Sinopse → Gêneros → Onde Assistir → Elenco → Equipe), streaming options in two columns past 2.
- CI: `gradlew` executable bit and a machine-specific `gradle-daemon-jvm.properties` were breaking every
  build since the initial commit — fixed, unrelated to any app code.

### Fixes
- Settings > Aparência: the Sistema/Claro/Escuro segmented selector didn't reliably switch to
  Light — replaced with the "Seguir tema do sistema" switch described above. Also removed a
  leftover race in `AppThemeController`/`AppLocaleController` initial load (`.collect` on the
  DataStore Flow could re-overwrite a freshly-picked value with a stale re-emission; now a
  one-shot `.first()` read).
- Settings > Aparência screen didn't match Rokku's actual spacing/typography: title was bold
  (Rokku's isn't), the gap under the back arrow was too small (should be 52dp, from
  `main_activity.xml`'s `big_title`), "Tema claro"/"Tema escuro" weren't the right weight, the gap
  between "Tema do aplicativo" and "Tema claro" was too big (should be 4dp), and the theme-swatch
  mockup didn't match `theme_item.xml`'s real dimensions/colors (wrong size, top bar tinted with
  the accent color instead of `colorSurface`, bottom bar recolored per theme when it should stay
  neutral). All of `SettingsScreen`/`SettingsAppearanceScreen`/`SettingsGeneralScreen` and the
  previously unconverted `SettingsNotificações`/`Plataformas`/`Dados`/`Integrações` screens now
  share the same `SettingsScaffold`/`SettingsSectionHeader`/`SettingsSwitchRow`/`SettingsClickRow`
  building blocks (`docs/design-system-settings.md`).
- Steam platform badge text was unreadable (near-black on a dark badge); now white.
- Update download from Sobre could freeze mid-download with the screen locked (Doze/App Standby
  killing the plain background worker) and never report success or error; now runs as a foreground
  service and times out instead of hanging.
- "Procurar por atualizações" could report the app as up to date even when a newer nightly
  existed: every nightly tag pointed at the same static commit in oribu-nightly, so the
  GitHub API's release ordering (and the tag dates shown on GitHub) were unreliable. Nightly
  tags now point at a freshly-dated commit per build, and the in-app check picks the release
  with the highest build number instead of trusting the API's order.
- The "toque para instalar" notification after a successful update download could disappear
  before it could be tapped: it reused the same notification ID as the foreground download
  service, which Android cancels when the service stops. Now uses its own ID.
- Installing a downloaded nightly update always failed ("conflito com um pacote já existente"):
  nightly builds were signed with an auto-generated debug keystore that CI regenerates from
  scratch on every run, so each nightly had a different signature than the one before it.
  Nightly/qa builds now sign with a fixed, low-stakes keystore committed to the repo, so
  updates install cleanly over the previous nightly.
