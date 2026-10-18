# 001 — Login and sign-up screens: plan

- **Spec:** [spec.md](spec.md)
- **Status:** Approved (dependencies confirmed 2026-10-16; deviations in [tasks.md](tasks.md#notes))

## Approach

Single Activity (`MainActivity` + Navigation Component, already in place) with four fragments:
`SessionGateFragment` (start destination), `LoginFragment`, `SignUpFragment`, `HomeFragment` (placeholder).
Each screen with logic has a `ViewModel` exposing one `StateFlow<UiState>`; network and disk run in coroutines
off the main thread.

Networking is Retrofit over OkHttp. OkHttp's `Authenticator` is the standard hook for "on 401, refresh once and
retry" (BR-6) and keeps refresh logic out of every call site. Tokens are encrypted with an AES-GCM key held in the
Android Keystore and stored in a private `SharedPreferences` file; no extra library is needed for that.

Dependencies are wired by hand in an `AppContainer` owned by a custom `Application` — the same manual
composition the API uses in `cmd/api/main.go`. There are too few objects to justify a DI framework.

This plan also sets the base architecture for the next features (package per feature, HTTP client,
ViewModel/coroutines, manual DI, token storage). `CLAUDE.md` is updated in the same change.

## New dependencies (to confirm)

All declared in `gradle/libs.versions.toml`, using the latest stable version compatible with AGP 9.4.1 /
compileSdk 37 at implementation time. Exact versions are recorded in the catalog, not here.

| Dependency | Scope | Why | Alternative rejected |
|------------|-------|-----|----------------------|
| `com.squareup.okhttp3:okhttp` | impl | HTTP client; `Authenticator` for refresh, timeouts | `HttpURLConnection`: no refresh hook, more hand-written code |
| `com.squareup.retrofit2:retrofit` + `converter-kotlinx-serialization` | impl | Typed `suspend` endpoints; reused by purchases/categories later | Ktor client: larger, nothing extra needed here |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `org.jetbrains.kotlin.plugin.serialization` | impl / plugin | JSON without reflection; R8-friendly; `@SerialName` for `snake_case` | Gson: ignores Kotlin nullability; Moshi: needs KSP or kotlin-reflect |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | impl | Async work off the main thread | — |
| `androidx.lifecycle:lifecycle-viewmodel-ktx`, `lifecycle-runtime-ktx` | impl | `ViewModel`, `viewModelScope`, `repeatOnLifecycle` | — |
| `androidx.fragment:fragment-ktx` | impl | `by viewModels { factory }` | — |
| `com.squareup.okhttp3:mockwebserver` | test | Real HTTP round-trips in JVM tests (error envelope, refresh concurrency) | Mocking Retrofit interfaces: would not cover the `Authenticator` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | test | Deterministic ViewModel tests | — |

Not added: Hilt/Koin, `security-crypto` (`EncryptedSharedPreferences` is deprecated), DataStore,
OkHttp `logging-interceptor` (the spec forbids logging `/auth/*` bodies), `core-splashscreen`.

## Affected code

Package root `com.github.jonasjesusamerico.we_sync_app` (abbreviated `…`).

| Path | Change |
|------|--------|
| `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` | New dependencies and serialization plugin; `buildConfig = true`; `API_BASE_URL` per build type |
| `app/src/main/AndroidManifest.xml` | `android:name=".WeSyncApplication"`, `INTERNET` permission, `networkSecurityConfig` |
| `app/src/main/res/xml/network_security_config.xml` | New: cleartext not permitted |
| `app/src/debug/res/xml/network_security_config.xml` | New: cleartext allowed only for `10.0.2.2` and `localhost` |
| `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Exclude the token prefs file from cloud backup and device transfer |
| `…/WeSyncApplication.kt`, `…/AppContainer.kt` | New: builds the OkHttp clients, Retrofit, `TokenStore`, `AuthRepository` once |
| `…/core/network/ApiError.kt` | New: error envelope `{code, message}` + parsing of an error response into a typed failure |
| `…/auth/AuthApi.kt` | New: Retrofit interface for `register`, `login`, `refresh` + DTOs (`@SerialName` snake_case) |
| `…/auth/AuthRepository.kt` | New: `login`, `register` (= register + login, D-1), `restoreSession`; returns `AuthResult` (sealed: `Success`, `InvalidCredentials`, `EmailTaken`, `InvalidEmail`, `InvalidPassword`, `Network`, `Unexpected`) |
| `…/auth/CredentialsValidator.kt` | New: pure Kotlin rules mirroring the API (trim, regex, ≤254 chars, 8–72 UTF-8 bytes, confirmation) |
| `…/auth/TokenStore.kt` | New: interface `readRefreshToken()/saveRefreshToken()/clear()` — only the refresh token is persisted |
| `…/auth/KeystoreTokenStore.kt` | New: AES-256-GCM key in `AndroidKeyStore`; ciphertext + IV in private prefs; decrypt failure = no session (also covers a restored backup whose key is gone) |
| `…/auth/TokenRefresher.kt` | New: holds the current access token in memory; single synchronized `refresh(staleRefreshToken)`; if the stored token already changed, returns the current one without calling the API (BR-5) |
| `…/auth/TokenAuthenticator.kt`, `AuthInterceptor.kt` | New: adds `Bearer` header; on 401 refreshes once and retries once, gives up if the request was already retried (BR-6). Installed only on the authenticated client; `/auth/*` uses a client without them |
| `…/auth/SessionEvents` (in `AppContainer`) | New: `SharedFlow` emitting "signed out" when refresh returns 401, so `MainActivity` navigates to Login |
| `…/auth/ui/SessionGateFragment.kt` + `ViewModel` | New: start destination; progress → Home / Login / retry (BR-4, D-3) |
| `…/auth/ui/LoginFragment.kt` + `ViewModel` | New |
| `…/auth/ui/SignUpFragment.kt` + `ViewModel` | New |
| `…/home/HomeFragment.kt` | New placeholder |
| `res/layout/fragment_session_gate.xml`, `fragment_login.xml`, `fragment_sign_up.xml`, `fragment_home.xml` | New; `TextInputLayout` with `endIconMode="password_toggle"` (D-2), `autofillHints` `emailAddress`/`password`/`newPassword` (BR-7), `saveEnabled="false"` on password fields (BR-2) |
| `res/navigation/nav_graph.xml` | Replaced: new destinations; navigation to Home uses `popUpTo` the graph root, `inclusive` (AC-2) |
| `res/values/strings.xml` | New pt-BR strings (spec "Error mapping") |
| `MainActivity.kt`, `activity_main.xml`, `content_main.xml`, `menu_main.xml` | Template FAB/menu removed; observes `SessionEvents` |
| `FirstFragment`, `SecondFragment`, their layouts, `Example*Test` | Removed (template placeholders) |
| `app/src/main/keepRules/rules.keep` | Keep rules only if R8 breaks serialization/Retrofit in release (verified with `assembleRelease`) |
| `CLAUDE.md` | Stack, code map (package per feature), "Estado atual" updated |

## Base URL and build configuration

- Debug: `BuildConfig.API_BASE_URL` = `http://10.0.2.2:8080/api/v1/`, overridable by `api.baseUrl` in
  `local.properties` (physical device on the LAN).
- Release: read from the `api.baseUrl.release` property; the build fails if it is missing or not `https://`.
  No production URL is committed.

## API contract changes

None. Gaps already listed in the spec (no `GET /me`, no login rate limiting) remain open.

## Security and data isolation

- Tokens exist in memory only inside `TokenStore`/`TokenRefresher` and the HTTP headers; never in UI state,
  `SavedStateHandle`, logs or exception messages shown to the user.
- `KeystoreTokenStore` key: AES/GCM/NoPadding, 256-bit, non-exportable. Prefs file excluded from backups.
- Login error never reveals which field is wrong (AC-3): `401` → `InvalidCredentials`.
- `422` is mapped to a field by the API message prefix (`email…` / `password…`); unknown messages fall back to
  the generic error. See risks.
- Release cannot do cleartext HTTP (main `network_security_config`), debug only to the local hosts.

## Observability

No logging of requests/responses or tokens. Unexpected errors in debug may log the exception class and HTTP
status only.

## Test strategy

JVM tests (`./gradlew testDebugUnitTest`) cover all logic; `MockWebServer` serves the real API envelopes.
`TokenStore` is replaced by an in-memory fake on the JVM.

| AC | Level | Test |
|----|-------|------|
| AC-1, AC-10, AC-11, AC-12 | `SessionGateViewModel` + `AuthRepository` (MockWebServer) | no token → Login; refresh 200 → Home and rotated refresh token saved; 401 → tokens cleared → Login; I/O error → tokens kept → Retry |
| AC-2 | `AuthRepository` + `LoginViewModel` | 200 saves pair and emits authenticated; back stack via `popUpTo` checked manually |
| AC-3 | `AuthRepository` | 401 → `InvalidCredentials` (single type for both cases) |
| AC-4, AC-7 | `CredentialsValidator`, ViewModels | empty fields, bad e-mail, 7/8/72/73 bytes, multibyte chars, mismatch → no request sent (MockWebServer request count 0) |
| AC-5, AC-6 | `AuthRepository` | 201 then login 200 → `Success`; 201 then login failure → account-created result; 409 → `EmailTaken`; 422 messages → field errors |
| AC-8 | ViewModels | state is `loading` while request pending; second submit ignored |
| AC-9 | `AuthRepository` | timeout / closed connection / 500 / 400 → `Network` / `Unexpected` |
| AC-13 | `TokenAuthenticator` + `TokenRefresher` (MockWebServer) | two parallel calls get 401 → exactly one `/auth/refresh` request; both retried with the new token; refresh 401 → `SessionEvents` emits signed out |
| AC-14 | `KeystoreTokenStore` instrumented test (`connectedDebugAndroidTest`); config review | encrypt/decrypt round-trip, clear; manual check of Logcat and of `assembleRelease` network config |

Manual check on emulator against `docker compose up` of the API: AC-1 to AC-12 end to end.
Instrumented tests need an emulator; if none is available at implementation time, it will be reported as not run.

Done when: `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug` pass (release with a dummy
`https://` URL property).

## Risks and alternatives considered

- **Kotlin serialization plugin version** must match the Kotlin version bundled with AGP 9.4.1 (built-in Kotlin).
  Verified at the first build; if it cannot be aligned, fall back to Moshi with KSP (new decision, asked first).
- **Outdated template libraries** (`core-ktx 1.10.1`, `appcompat 1.6.1`, ...): new lifecycle/fragment versions
  may force upgrades. If so, only the minimum bump needed is done and reported; a general upgrade stays a
  separate task.
- **Mapping 422 by message text** couples the app to English API messages. Accepted because it only selects the
  field; a change in the API message degrades to the generic error, not a crash. A machine-readable field in the
  API error would remove this (suggestion for the API).
- **Refresh on every app start** rotates the token each launch (one extra DB write in the API). Accepted: there
  is no `GET /me` to validate the session otherwise.
- **Keystore key invalidation** (rare OEM bugs, user clears credentials) means a lost session, not a crash:
  decrypt failure → clear → Login.
- **Only the refresh token is persisted;** the access token lives in memory (`TokenRefresher`). Every process
  start already refreshes (BR-4), so persisting the access token would add exposure without saving a call.
- **Considered:** Jetpack Compose (rejected — project uses Views by rule), Hilt (overkill for ~6 objects),
  `EncryptedSharedPreferences` (deprecated).
