# Settings design system

Oribu's Settings screens are modeled after Rokku, but Rokku's actual Settings UI
(`SettingsAppearanceController.kt` and friends, legacy AndroidX Preference) has no bespoke
styling of its own — it's stock Material Preference-library defaults plus one custom switch
widget. There is nothing to literally port. Instead, this doc fixes Oribu's *own* conventions —
inspired by that stock look — as the standard every Settings screen (Compose) follows, so new
screens (including ones modeled after other Rokku features later) don't each invent their own
spacing/typography.

Building blocks live in `ui/screens/settings/SettingsComponents.kt`. Use them instead of ad hoc
`Scaffold`/`TopAppBar`/`ListItem`/`Switch` calls in any new or touched Settings screen.

## Screen shell — `SettingsScaffold(title, navController) { padding -> ... }`
- Top bar: a plain 56dp-tall `Row` (back arrow, then `actions` at the end), not M3's `TopAppBar` —
  that component defaults to 64dp, and pinning it to Rokku's real 56dp Toolbar height
  (`?attr/actionBarSize`) with a bare `Modifier.height(56.dp)` doesn't shrink its internal
  navigation-icon slot to fit: the back button kept its old internal position and overflowed past
  the smaller box (same class of bug the search field's `TextField` had — see below). Content color
  pinned to `MaterialTheme.colorScheme.onSurface` via `CompositionLocalProvider(LocalContentColor
  provides ...)` so the back arrow and the `actions` icons (e.g. the hub's "?" help button) render
  the same full-strength color Rokku's real toolbar uses, instead of M3's default two-tone
  navigation-vs-action icon colors. `Modifier.statusBarsPadding()` replaces what `TopAppBar` was
  handling automatically via `windowInsets`.
- Screen title: rendered in the body, `headlineLarge` + bold, `padding(horizontal = 20.dp,
  vertical = 12.dp)`.
- Settings hub search field (`SettingsScreen.kt` only, not a shared component): hand-rolled from
  `BasicTextField` in a `Row` (icon, placeholder/field, clear button), not M3's `TextField` —
  that component defaults to a 56dp-tall box via padding baked into its own decoration, and a
  `Modifier.height(48.dp)` forced on top of it doesn't reflow that internal padding, it just clips
  the icon/placeholder against the smaller box (this actually shipped once — the hand-rolled
  version, sized with `heightIn(min = 48.dp)`, matches Rokku's real `MiniSearchView` search row
  without clipping anything).

## Rows
- `SettingsSectionHeader(title, tinted = false)` — small label, no card/divider.
  `bodyMedium`/secondary color normally; `tinted = true` for the one header that names what the
  whole screen is about (`titleMedium`/primary/bold — e.g. "App theme").
- `SettingsSwitchRow(title, subtitle?, checked, onCheckedChange)` — non-bold `bodyLarge` title,
  `bodyMedium` subtitle at 70% alpha, `padding(horizontal = 16.dp, vertical = 12.dp)`, whole row
  clickable (not just the switch).
- `SettingsClickRow(title, subtitle?, onClick, minHeight = 56.dp, titleColor?, leadingIcon?,
  trailing?)` — same typography; pass `titleColor` for a destructive action (e.g.
  `MaterialTheme.colorScheme.error` for "Delete all data"). The Settings hub's own top-level
  category rows (icon + title) do NOT use this component — see `SettingsCategoryRow` in
  `SettingsScreen.kt`, which ports the real `preference_material.xml`/`image_frame.xml` constants
  from `androidx.preference:preference` (the library Rokku's `conductor-support-preference` builds
  on): 16dp row padding, a 56dp-wide icon frame, and 16dp top/bottom padding around the title text.
  Height comes from that text padding, not a fixed value, so it grows with the system font size the
  way Rokku's real Preference row does. The title also sets `platformStyle =
  PlatformTextStyle(includeFontPadding = true)` with `lineHeight = TextUnit.Unspecified` — Compose
  drops Android's legacy per-font leading by default, but the real Preference row's title is a
  plain `TextView` that keeps it, so without opting back in Compose measures the same 16dp/56dp/16dp
  spacing shorter than Rokku's real row (a text-metrics gap, not a spacing one).
- `HorizontalDivider(Modifier.padding(vertical = 8.dp))` between logical groups within one screen.

## Typography rule
Never bold a row's own title or subtitle — this is the one rule ported directly from Rokku/stock
Material Preference (no custom text styling there at all). Bold is reserved for the screen's own
big title and a `tinted` section header, nothing else in a settings row.

## Applies to
Settings hub (`SettingsScreen.kt`), Aparência, Geral, Notificações, Plataformas, Dados,
Integrações — all converted to these components. Scope is Settings only for now; other parts of
the app (detail screens, lists, cards) are not covered and shouldn't be assumed to follow the
same rules — their bold/emphasis usage is deliberate info hierarchy (prices, ratings, titles),
not an instance of this same inconsistency.
