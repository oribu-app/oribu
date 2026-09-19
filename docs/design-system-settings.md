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
- Top bar: back arrow only, no title, `containerColor`/`scrolledContainerColor` =
  `MaterialTheme.colorScheme.background` (no visible seam between bar and body).
- Screen title: rendered in the body, `headlineLarge` + bold, `padding(horizontal = 20.dp,
  vertical = 12.dp)`.

## Rows
- `SettingsSectionHeader(title, tinted = false)` — small label, no card/divider.
  `bodyMedium`/secondary color normally; `tinted = true` for the one header that names what the
  whole screen is about (`titleMedium`/primary/bold — e.g. "App theme").
- `SettingsSwitchRow(title, subtitle?, checked, onCheckedChange)` — non-bold `bodyLarge` title,
  `bodyMedium` subtitle at 70% alpha, `padding(horizontal = 16.dp, vertical = 12.dp)`, whole row
  clickable (not just the switch).
- `SettingsClickRow(title, subtitle?, onClick, minHeight = 56.dp, titleColor?, leadingIcon?,
  trailing?)` — same typography; pass `minHeight = 64.dp` for a top-level category row with a
  leading icon (matches the Settings hub); pass `titleColor` for a destructive action (e.g.
  `MaterialTheme.colorScheme.error` for "Delete all data").
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
