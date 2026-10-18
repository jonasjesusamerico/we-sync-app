# we-sync design system

Visual language of the app: a navy brand header, a light neutral background, white cards, bold navy money
values and a brighter blue for actions. Implemented with Material 3 (Views XML) — no extra library.

Source files:

| File | Content |
|------|---------|
| `app/src/main/res/values/colors.xml` | Semantic colors `ws_*` (light) |
| `app/src/main/res/values-night/colors.xml` | Same names, dark values |
| `app/src/main/res/values/dimens.xml` | Spacing, radius and component sizes |
| `app/src/main/res/values/styles.xml` | Typography (`TextAppearance.WeSync.*`), shapes, component styles (`Widget.WeSync.*`) |
| `app/src/main/res/values/themes.xml` | Maps everything onto Material 3 theme attributes |

## Rules

- No hex colors, literal `dp`/`sp` or ad-hoc styles in layouts/code. Use theme attributes
  (`?attr/colorPrimary`, `?attr/textAppearanceBodyLarge`), the `ws_*` colors for brand roles, and `@dimen/space_*`.
- Standard components (`MaterialButton`, `TextInputLayout`, `MaterialCardView`, `MaterialToolbar`, FAB,
  `BottomNavigationView`) are styled by the theme — do not set `style` unless you want a variant.
- Money values always use `TextAppearance.WeSync.Amount*`.
- Every new color needs a light and a dark value (both `colors.xml` files).
- Change the design system here first, then in the resources, in the same change.

## Colors

| Token | Light | Dark | Use |
|-------|-------|------|-----|
| `ws_brand` / `ws_on_brand` | `#1F3F80` / `#FFFFFF` | `#16264A` / `#E6EAF5` | Header band (toolbar, behind the status bar) |
| `ws_amount` | `#1F3F80` | `#B8CBF7` | Money values, headlines |
| `ws_primary` / `ws_on_primary` | `#2F5BB7` / `#FFFFFF` | `#9DB6F2` / `#0B1D46` | Buttons, FAB, focused fields (`colorPrimary`) |
| `ws_primary_container` / `ws_on_primary_container` | `#E6EDFA` / `#1F3F80` | `#233866` / `#DCE5FB` | Selected states (navigation pill, segmented selected) |
| `ws_background` | `#F6F7FB` | `#0F131C` | Screen background |
| `ws_surface` | `#FFFFFF` | `#171C27` | Cards, fields |
| `ws_surface_variant` | `#EEF1F7` | `#232A38` | Tracks, subtle blocks |
| `ws_on_surface` / `ws_on_surface_variant` | `#1B2130` / `#5E6577` | `#E4E7EF` / `#A6ADBD` | Primary / secondary text |
| `ws_outline` / `ws_outline_variant` | `#C9CFDC` / `#E1E5EE` | `#4A5266` / `#2A3141` | Field borders / dividers and card strokes |
| `ws_error` | `#C62828` | `#FF8A80` | Errors |
| `ws_positive` / `ws_negative` | `#2E7D32` / `#C62828` | `#81C784` / `#FF8A80` | Income / expense |
| `ws_highlight` / `ws_on_highlight` / `ws_highlight_accent` | `#33353D` / `#FFFFFF` / `#E9B949` | `#262830` / `#F2F2F4` / `#E9B949` | Dark highlight card with gold accent |

## Typography

System font (Roboto); hierarchy by size and weight.

| Style | Theme attribute | Size / weight | Use |
|-------|-----------------|---------------|-----|
| `TextAppearance.WeSync.Amount` | — | 32sp bold, `ws_amount` | Main money value |
| `TextAppearance.WeSync.Amount.Medium` / `.Small` | — | 24sp / 16sp bold | Secondary values |
| `TextAppearance.WeSync.Headline` | `textAppearanceHeadlineSmall` | 26sp bold, `ws_amount` | Screen headline |
| `TextAppearance.WeSync.Title` | `textAppearanceTitleLarge` | 20sp medium | Toolbar title |
| `TextAppearance.WeSync.Section` | `textAppearanceTitleMedium` | 18sp regular | Section label ("Próximos gastos") |
| `TextAppearance.WeSync.Body` | `textAppearanceBodyLarge` | 16sp | Body text |
| `TextAppearance.WeSync.BodySecondary` | `textAppearanceBodyMedium` | 14sp, secondary color | Subtitles, messages |
| `TextAppearance.WeSync.Caption` | `textAppearanceBodySmall` | 13sp, secondary color | Dates, hints |
| `TextAppearance.WeSync.Label` | `textAppearanceLabelLarge` | 15sp medium | Buttons |

## Spacing and shape

- Spacing (4dp grid): `space_4`, `space_8`, `space_12`, `space_16`, `space_24`, `space_32`, `space_48`.
  Screen padding: `screen_padding` (24dp). Between fields: 16dp. Between blocks: 24dp.
- Radius: `radius_small` 8dp, `radius_medium` 12dp (buttons, fields), `radius_large` 16dp (cards, FAB).
- Buttons: 52dp tall (`button_height`). Cards: outlined, no elevation.

## Components

| Style | Applied by theme | Notes |
|-------|------------------|-------|
| `Widget.WeSync.AppBarLayout`, `Widget.WeSync.Toolbar` | yes | Brand band; `MainActivity` extends it behind the status bar |
| `Widget.WeSync.Button` | yes | Filled primary action |
| `Widget.WeSync.Button.Outlined` | yes (`materialButtonOutlinedStyle`) | Secondary action |
| `Widget.WeSync.Button.Text` | yes (`borderlessButtonStyle`) | Tertiary / link-like action (set explicitly) |
| `Widget.WeSync.TextInputLayout` | yes | Outlined field on surface |
| `Widget.WeSync.Card` | yes | White card, thin stroke, 16dp padding |
| `Widget.WeSync.Card.Highlight` | no | Dark highlight card; use `ws_on_highlight` text and `ws_highlight_accent` icon |
| `Widget.WeSync.FloatingActionButton` | yes | Rounded square, primary color |
| `Widget.WeSync.BottomNavigation` | yes | Selected item on a `ws_primary_container` pill |
| `Widget.WeSync.TextInputLayout.Dropdown` | no (set it) | Exposed dropdown with the field look; items use `layout/item_dropdown` |
| Segmented control | — | `MaterialButtonToggleGroup` (`singleSelection`, `selectionRequired`) of `?attr/materialButtonOutlinedStyle` buttons with `layout_weight=1`; selected = `ws_primary_container` |
| Option row | — | `layout/item_create_option`: 48dp icon on `drawable/bg_option_icon`, title (BodyLarge) + subtitle (BodyMedium); used in bottom sheets |
| Bottom sheet | default M3 | `BottomSheetDialogFragment` with `BottomSheetDragHandleView`, title in `textAppearanceTitleMedium`, then option rows |

| Navigation drawer | — | `DrawerLayout` + panel (`activity_main`): `drawer_header` on `ws_brand`, rows `item_drawer_section` / `item_drawer_child` (`Widget.WeSync.DrawerItem`, current screen on a `ws_primary_container` pill via `bg_drawer_item`), chevron rotates 180° when expanded |
| Category bar | — | `layout/item_category_total`: determinate `LinearProgressIndicator`, `colorPrimary` on `colorSurfaceVariant`, 8dp thick |
| Entry row | — | `layout/item_recent_purchase`: date (Caption), description + category, amount (`Amount.Small`), "A pagar" in `colorPrimary` |

Patterns:

- **Dashboard:** month selector (chevrons in `colorPrimary` + `TitleMedium` month), summary card (Section label,
  `Amount`, two `Amount.Small` columns split by a `colorOutlineVariant` divider), then titled cards per block.
  Loading/empty/error states replace the content; scrolling content uses `fab_clearance` bottom padding.

- **Create action:** FAB at the bottom-right (`layout_gravity="bottom|end"`, `space_16` margin) opening a bottom
  sheet of option rows.
- **Money field:** `TextAppearance.WeSync.Amount.Medium`, cents-based mask (`purchase/MoneyInput`).
- **Confirmation after save:** Snackbar anchored above the FAB.

Not built yet (add when a screen needs it): full entries list screen.
