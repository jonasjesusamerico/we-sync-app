# 001 — Login and sign-up screens: tasks

- **Spec:** [spec.md](spec.md)
- **Plan:** [plan.md](plan.md)

Each task is small, independently verifiable and references the acceptance criteria it covers.

- [x] T1 — Dependencies in `libs.versions.toml`, serialization plugin, `buildConfig`, `API_BASE_URL` per build type, release URL check (AC-14)
- [x] T2 — Manifest: `INTERNET`, `Application` class, network security config main/debug, backup exclusions (AC-14)
- [x] T3 — `core/network/ApiError` + `auth/AuthApi` DTOs (all)
- [x] T4 — `CredentialsValidator` + tests (AC-4, AC-7)
- [x] T5 — `TokenStore`, `KeystoreTokenStore` + instrumented test (AC-14)
- [x] T6 — `TokenRefresher`, `AuthInterceptor`, `TokenAuthenticator` + MockWebServer tests (AC-10, AC-11, AC-12, AC-13)
- [x] T7 — `AuthRepository` + MockWebServer tests (AC-2, AC-3, AC-5, AC-6, AC-7, AC-9)
- [x] T8 — `AppContainer`, `WeSyncApplication`
- [x] T9 — ViewModels (gate, login, sign-up) + tests (AC-1, AC-4, AC-5, AC-8, AC-10, AC-11, AC-12)
- [x] T10 — Layouts, strings pt-BR, fragments, nav graph, `MainActivity`; remove template placeholders (AC-1, AC-2, AC-6)
- [x] T11 — Update `CLAUDE.md` (stack, code map, state, commands)
- [x] T12 — Run `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug`; instrumented tests if a device is available
  (JVM suite and lint pass; `KeystoreTokenStoreTest` written but **not run** — no device/emulator available)
- [ ] T13 — Manual end-to-end check on emulator against the local API (AC-1 to AC-12)

## Notes

Decisions taken during implementation that deviate from the plan.

- Versions pinned to libraries compiled with Kotlin ≤ 2.2.x (AGP 9.4.1 bundles Kotlin 2.2.10): okhttp 5.3.2,
  kotlinx-serialization 1.9.0 (plugin 2.2.10), coroutines 1.10.2, lifecycle 2.10.0, fragment-ktx 1.8.9,
  retrofit 3.0.0. Lint reports newer versions; upgrading them requires a newer bundled Kotlin.
- `AuthRepository` is an interface with `RemoteAuthRepository` as implementation, so ViewModel tests use a fake
  (same port/adapter split as the API's `Repository` / `PostgresRepository`).
- The "signed out" event lives in `TokenRefresher.signedOut` instead of a separate `SessionEvents` object.
- `TokenStoreException`: a Keystore failure while saving becomes `AuthResult.Unexpected` / `RefreshResult.Failed`
  instead of crashing; a failure while reading is treated as "no session".
- Physical device in debug: cleartext is allowed only for `10.0.2.2` and `localhost`, so use `adb reverse` +
  `api.baseUrl=http://localhost:8080/api/v1/` instead of a LAN IP.
- Sign-up `409`: the "Já tenho conta" button (always visible) is the action to go to Login, pre-filling the e-mail;
  the message points to it.
- Login `422` (only "email and password are required") is shown as "required" on the field; the validator already
  prevents it.
