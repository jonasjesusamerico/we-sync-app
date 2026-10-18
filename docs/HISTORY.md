# History

Chronological record of every change to this repository: what changed, why, and how to undo it.
Newest entries at the bottom. The repository has **no commits yet** (decision of 2026-10-17: history is kept
in this file), so reverting means undoing the listed files by hand — the "Revert" line says what to remove or
restore. Detailed rationale lives in the linked specs (`docs/specs/`).

Format of each entry: date — title; Why; Changed (files); Revert; Verified.

---

## 2026-10-16 — Spec 001: login and sign-up (specification)

- **Why:** first real feature; every API resource needs authentication.
- **Changed:** `docs/specs/001-auth-screens/spec.md` (created, then open questions resolved: auto sign-in after
  sign-up, password confirmation + show/hide, no offline mode). `CLAUDE.md`: code map entry for `docs/specs/`.
- **Revert:** delete `docs/specs/001-auth-screens/`.
- **Verified:** documentation only.

## 2026-10-16 — Spec 001: plan and tasks

- **Why:** define architecture and confirm new dependencies before coding.
- **Changed:** `docs/specs/001-auth-screens/plan.md`, `tasks.md`.
- **Revert:** delete both files.
- **Verified:** documentation only.

## 2026-10-16 — Base architecture + login, sign-up and session restore (spec 001 T1–T12)

- **Why:** implement spec 001.
- **Changed:**
  - Build: `gradle/libs.versions.toml` (okhttp 5.3.2, retrofit 3.0.0, kotlinx-serialization 1.9.0 + plugin 2.2.10,
    coroutines 1.10.2, lifecycle 2.10.0, fragment-ktx 1.8.9, mockwebserver3, coroutines-test), `build.gradle.kts`,
    `app/build.gradle.kts` (`buildConfig`, `API_BASE_URL` per build type, release URL check).
  - Manifest/security: `AndroidManifest.xml` (`INTERNET`, `WeSyncApplication`, network security config),
    `res/xml/network_security_config.xml`, `app/src/debug/res/xml/network_security_config.xml`,
    `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.
  - Code: `WeSyncApplication.kt`, `AppContainer.kt`, `core/network/ApiError.kt`, `auth/*` (API, repository,
    validator, Keystore token store, refresher, interceptor, authenticator), `auth/ui/*` (gate, login, sign-up),
    `home/HomeFragment.kt`, `MainActivity.kt`.
  - Resources: layouts `fragment_session_gate|login|sign_up|home.xml`, `activity_main.xml`, `nav_graph.xml`,
    `strings.xml`, `dimens.xml`.
  - Removed template placeholders: `FirstFragment`, `SecondFragment`, their layouts, `menu_main.xml`,
    `values-land|w600dp|w1240dp/dimens.xml`, `Example*Test`.
  - Tests: `app/src/test/.../auth/**`, `app/src/androidTest/.../auth/KeystoreTokenStoreTest.kt`.
  - `CLAUDE.md`: stack, code map, API usage, commands.
- **Revert:** restore the Android Studio "Basic Views Activity" template files and remove the listed ones;
  remove the dependencies from the catalog.
- **Verified:** `testDebugUnitTest` (40 tests), `lintDebug`, `assembleDebug`, `assembleRelease` passed.
  Instrumented test and manual check not run (no device).

## 2026-10-17 — Debug API URL on the LAN (192.168.68.59)

- **Why:** run the app on a physical device against the API running in Docker on the dev notebook.
- **Changed:** `local.properties` (not versioned): `api.baseUrl=http://192.168.68.59:8080/api/v1/`;
  `app/src/debug/res/xml/network_security_config.xml`: cleartext allowed for `192.168.68.59`; `CLAUDE.md`,
  spec 001 `tasks.md` notes.
- **Revert:** remove the `api.baseUrl` line from `local.properties` and the `192.168.68.59` domain entry.
- **Verified:** `assembleDebug`; `BuildConfig.API_BASE_URL` checked; API health-check reachable from the notebook.

## 2026-10-17 — Design system

- **Why:** consistent visual language inspired by a reference screen (navy header, light background, white cards,
  bold navy amounts).
- **Changed:** `res/values/colors.xml` (semantic `ws_*`), `res/values-night/colors.xml` (new),
  `res/values/styles.xml` (new: typography, shapes, components), `res/values/themes.xml` (Material 3 mapping),
  `res/values/dimens.xml` (spacing/radius tokens), `res/color/ws_navigation_item.xml` (new); removed
  `values-night/themes.xml`, `values-v23/themes.xml`. Screens restyled: `fragment_login|sign_up|home.xml`,
  `activity_main.xml`, `MainActivity.kt` (brand band behind the status bar), `auth/ui/AuthMessage.kt`
  (error/info colors), `strings.xml` (headlines). Docs: `docs/design-system.md` (new), `CLAUDE.md`.
- **Revert:** restore the previous `themes.xml`/`colors.xml`, delete the new files, restore the previous layouts.
- **Verified:** `testDebugUnitTest`, `lintDebug`, `assembleDebug`. Visual result **not** seen (emulator unavailable).

## 2026-10-17 — Logout (spec 001 amendment, T14)

- **Why:** let the user sign out to sign in again or create another account.
- **Changed:** `auth/AuthApi.kt` (`logout`), `auth/TokenRefresher.kt` (`clearSession`), `auth/AuthRepository.kt`
  (`logout`: local first, API revocation in background), `AppContainer.kt` (`applicationScope`),
  `home/HomeViewModel.kt` (new), `home/HomeFragment.kt` ("Sair" + confirmation), `res/menu/menu_home.xml`,
  `res/drawable/ic_logout.xml`, `strings.xml`; `auth/ui/FragmentExtensions.kt` moved to `core/ui/`.
  Tests: `RemoteAuthRepositoryTest`, `TokenRefresherTest`, `home/HomeViewModelTest.kt`. Docs: spec 001 (BR-8,
  AC-15–17), plan amendment, tasks T14, `CLAUDE.md`.
- **Revert:** remove the logout method/UI/menu/icon/strings and their tests; move the helpers back to `auth/ui`.
- **Verified:** `testDebugUnitTest` (45 tests), `lintDebug`, `assembleDebug`.

## 2026-10-17 — Spec 002: purchase entry (specification, plan, tasks)

- **Why:** first data entry feature: "+" button on Home with a creation menu; "Lançamento" form.
- **Changed:** `docs/specs/002-purchase-entry/spec.md`, `plan.md`, `tasks.md` (Draft); this file (created, with
  the entries above reconstructed from the work log); `CLAUDE.md` (rule: every change gets an entry here).
- **Revert:** delete `docs/specs/002-purchase-entry/`.
- **Verified:** documentation only. Implementation waits for plan confirmation.

## 2026-10-17 — Purchase entry: "+" menu and "Novo lançamento" form (spec 002 T1–T11)

- **Why:** first data entry feature, plan confirmed by the user.
- **Changed:**
  - Code (new): `core/network/Serializers.kt`, `core/network/ApiOutcome.kt`, `category/CategoryApi.kt`,
    `paymentmethod/PaymentMethodApi.kt`, `purchase/PurchaseApi.kt`, `purchase/MoneyInput.kt`,
    `purchase/PurchaseValidator.kt`, `purchase/PurchaseRepository.kt`, `purchase/ui/PurchaseFormViewModel.kt`,
    `purchase/ui/PurchaseFormFragment.kt`, `purchase/ui/MoneyTextWatcher.kt`, `purchase/ui/PurchaseFormText.kt`,
    `home/CreateMenuBottomSheet.kt`.
  - Code (changed): `AppContainer.kt` (authenticated Retrofit, `purchaseRepository`), `home/HomeFragment.kt` (FAB,
    "Lançamento salvo." Snackbar), `MainActivity.kt` (signed-out redirect from any authenticated destination).
  - Resources: `layout/fragment_home.xml` (FAB), new `layout/fragment_purchase_form.xml`,
    `layout/bottom_sheet_create_menu.xml`, `layout/item_create_option.xml`, `layout/item_dropdown.xml`,
    `drawable/ic_add.xml`, `drawable/ic_receipt.xml`, `drawable/ic_calendar.xml`, `drawable/bg_option_icon.xml`;
    `navigation/nav_graph.xml` (`createMenuSheet` dialog, `purchaseFormFragment`); `values/strings.xml`;
    `values/styles.xml` (`Widget.WeSync.TextInputLayout.Dropdown`).
  - Tests (new): `core/network/SerializersTest`, `purchase/MoneyInputTest`, `purchase/PurchaseValidatorTest`,
    `purchase/RemotePurchaseRepositoryTest`, `purchase/ui/PurchaseFormViewModelTest`.
  - Docs: spec 002 (In progress), plan (Approved), tasks (T1–T11 + notes), `docs/design-system.md` (dropdown,
    segmented control, option row, bottom sheet, patterns), `CLAUDE.md` (code map).
- **Revert:** delete the new files above; in `HomeFragment.kt`, `fragment_home.xml`, `nav_graph.xml`,
  `AppContainer.kt`, `MainActivity.kt`, `strings.xml` and `styles.xml` remove the parts added here
  (FAB/menu/form destinations, authenticated Retrofit and `purchaseRepository`, the new strings and the dropdown
  style); restore the `MainActivity` redirect condition to `currentDestination?.id == R.id.homeFragment`.
- **Verified:** `testDebugUnitTest` (72 tests, 0 failures), `lintDebug` (only "newer version" warnings),
  `assembleDebug`, `assembleRelease`. **Not run:** on a device/emulator (T12).

## 2026-10-17 — Spec 003: dashboard and navigation drawer (specification, plan, tasks)

- **Why:** see the recorded entries and navigate between sections as screens are added.
- **Changed:** `docs/specs/003-dashboard-drawer/spec.md`, `plan.md`, `tasks.md` (Draft), with decisions D-1 to D-3
  taken with the user (dashboard content, drawer shows only existing screens, "Sair" only in the drawer).
- **Revert:** delete `docs/specs/003-dashboard-drawer/`.
- **Verified:** documentation only. Implementation waits for plan confirmation.

## 2026-10-17 — Dashboard and navigation drawer (spec 003 T1–T10)

- **Why:** see the month's numbers and navigate between sections; plan confirmed by the user.
- **Changed:**
  - Build: `gradle/libs.versions.toml`, `app/build.gradle.kts` — declared `androidx.drawerlayout:drawerlayout:1.1.1`
    and `androidx.recyclerview:recyclerview:1.1.0` (already resolved through Material; no new download).
  - Code (new): `purchase/MonthSummary.kt`, `home/DashboardViewModel.kt`, `auth/ui/SessionViewModel.kt`,
    `core/ui/drawer/DrawerMenu.kt`, `core/ui/drawer/DrawerAdapter.kt`.
  - Code (changed): `purchase/PurchaseApi.kt` (`list`, `PurchaseDto`, `PurchasePageDto`, `PurchaseStatus.CANCELED`),
    `purchase/PurchaseRepository.kt` (`listMonth`, `PAGE_SIZE`, `MAX_PAGES`), `home/HomeFragment.kt` (dashboard;
    logout menu removed), `MainActivity.kt` (drawer, lock per destination, Back closes drawer, logout).
  - Code (removed): `home/HomeViewModel.kt` (logout moved to `SessionViewModel`).
  - Resources (new): `layout/drawer_header.xml`, `item_drawer_section.xml`, `item_drawer_child.xml`,
    `item_category_total.xml`, `item_recent_purchase.xml`, `drawable/bg_drawer_item.xml`, `ic_home.xml`,
    `ic_account.xml`, `ic_chevron.xml`, `ic_chevron_left.xml`, `ic_chevron_right.xml`.
  - Resources (changed): `layout/activity_main.xml` (`DrawerLayout`), `layout/fragment_home.xml` (dashboard),
    `navigation/nav_graph.xml` (`action_global_purchaseForm`), `values/strings.xml`, `values/styles.xml`
    (`Widget.WeSync.DrawerItem*`), `values/dimens.xml` (`fab_clearance`, `drawer_width`).
  - Resources (removed): `menu/menu_home.xml`, `drawable/ic_logout.xml`.
  - Tests: new `purchase/MonthSummaryTest`, `purchase/PurchaseFixtures`, `home/DashboardViewModelTest`,
    `core/ui/drawer/DrawerMenuTest`, `auth/ui/SessionViewModelTest`; `RemotePurchaseRepositoryTest` (+listMonth);
    `FakePurchaseRepository` (+listMonth); removed `home/HomeViewModelTest`.
  - Docs: spec 003 (In progress), plan (Approved), tasks (T1–T10 + notes), spec 001 changelog (logout location),
    `docs/design-system.md` (drawer, bars, entry row, dashboard pattern), `CLAUDE.md` (code map).
- **Revert:** restore the previous `HomeFragment.kt`/`fragment_home.xml` (placeholder + logout menu, from the
  2026-10-17 logout and purchase entries), `HomeViewModel.kt`, `menu_home.xml`, `ic_logout.xml` and the
  `activity_main.xml`/`MainActivity.kt` without drawer; delete the new files; remove the two catalog entries.
- **Verified:** `testDebugUnitTest` (89 tests, 0 failures), `lintDebug` (only version warnings), `assembleDebug`,
  `assembleRelease`. **Not run:** on a device/emulator (T11).

## 2026-10-17 — Backend improvement roadmap (in the API repository)

- **Why:** list what the API must change to serve the app correctly, instead of working around it in the client.
- **Changed:** `../we-sync-api/docs/specs/004-app-support-roadmap/spec.md` (Draft) — amount and description
  validation, auth rate limiting, monthly summary endpoint, `field` in validation errors, idempotency on purchase
  creation, `GET /me`, refresh token reuse detection; grouped in 5 phases. No code changed in either repository.
- **Revert:** delete that folder in the API repository.
- **Verified:** documentation only; findings checked against the API code (`internal/purchase/service.go`,
  migration `000005`, `.claude/rules/security.md`).

## 2026-10-17 — API: purchase input validation (roadmap phase 1, in the API repository)

- **Why:** the API returned `500` for amounts above `99999999.99`, rounded extra decimals silently and accepted
  blank descriptions; the app validated these client-side only.
- **Changed (API repository):** `internal/purchase/service.go` (`validateRequest`: amount ≤ `99999999.99` and
  ≤ 2 decimals; description trimmed, not blank, ≤ 255 characters), `internal/purchase/service_test.go`;
  docs `docs/specs/005-purchase-input-validation/` (spec, plan, tasks), roadmap 004 changelog. No change in the app:
  the new `422` messages start with the field name, which `RemotePurchaseRepository` already maps.
- **Revert:** restore `validateRequest` and remove the three new tests in the API; delete spec 005.
- **Verified (API):** `gofmt -l` empty, `go vet ./...`, `go build ./...`, `go test ./...` passed. Not run against
  PostgreSQL / docker compose.

## 2026-10-18 — Implementation subagents (app and API)

- **Why:** delegate execution of approved spec tasks to a dedicated Claude Code subagent per repository, keeping
  the main session for planning and review.
- **Changed:** `.claude/agents/app-implementer.md` (created), `CLAUDE.md` (code map line for `.claude/agents/`).
  In the API repository: `.claude/agents/api-implementer.md` and a `CLAUDE.md` line.
- **Revert:** delete `.claude/agents/` and the `CLAUDE.md` line (in both repositories).
- **Verified:** configuration/documentation only; agents not exercised yet.

## 2026-10-18 — Implementation subagents: performance and concurrency guidance

- **Why:** the API runs on tight CPU/memory/connection limits (`../we-sync-api/docker-compose.yml`); the agents must
  treat request economy, complexity and race conditions as requirements of every task.
- **Changed:** `.claude/agents/app-implementer.md` (new "Performance and concurrency" section, review checklist and
  report item). In the API repository: `.claude/agents/api-implementer.md` (same, plus mandatory reading of
  `reliability-and-performance.md` and `persistence.md`).
- **Revert:** remove those sections from both agent files.
- **Verified:** configuration/documentation only; agents not exercised yet.

## 2026-10-18 — Implementation subagents: tasks come from we-sync-requirements

- **Why:** functional specifications and implementation tasks are now produced by the `business-analyst` agent in the
  sibling repository `../we-sync-requirements` (`docs/requirements/NNN-*/05-plano-implementacao.md`).
- **Changed:** `.claude/agents/app-implementer.md` (description; input step reads the requirements activity and gates on
  phase `Aprovado`/`Planejado`; progress tracked as task status + `evidencias.md` there; hard limit: only task status
  and evidence may be written in that repository; `docs/specs/` kept as legacy source). In the API repository:
  `.claude/agents/api-implementer.md` (same changes).
- **Revert:** restore the previous "Identify the input", implementation loop step 4–5 and hard limits in both agent files.
- **Verified:** configuration/documentation only; agents not exercised yet.

## 2026-10-18 — Local specs moved to we-sync-requirements (legacy)

- **Why:** requirements and tasks now live only in `../we-sync-requirements`; the old specs are kept there as history.
- **Changed:** `docs/specs/` (001–003) moved to `../we-sync-requirements/docs/legacy/we-sync-app/`.
  `.claude/agents/app-implementer.md`: legacy local spec source removed; tasks only from
  `../we-sync-requirements/docs/requirements/`; writing in `docs/legacy/` forbidden. `CLAUDE.md`: "Estado atual" link
  and code map line. In the API repository: `docs/specs/` moved to `docs/legacy/we-sync-api/`, same agent changes,
  `CLAUDE.md` workflow paragraph, link in `docs/requirements/purchase-entry.md`. Older entries in this file still
  cite `docs/specs/` (not rewritten).
- **Revert:** move `../we-sync-requirements/docs/legacy/we-sync-app/` back to `docs/specs/` (and the API folder back to
  `../we-sync-api/docs/specs/`), then undo the agent and `CLAUDE.md` edits.
- **Verified:** documentation/configuration only; `grep` for leftover `docs/specs` references.
