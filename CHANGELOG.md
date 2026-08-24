# Changelog

All notable changes to this project will be documented in this file.

The format is a simplified version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/):
- `Additions` - New features
- `Changes` - Behaviour/visual changes
- `Fixes` - Bugfixes
- `Other` - Technical changes/updates

## [Unreleased]

### Additions
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
