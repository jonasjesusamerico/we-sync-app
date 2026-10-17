# 001 — Login and sign-up screens

- **Status:** Draft
- **Author:** jonasjesusamerico
- **Date:** 2026-10-16

## Context

The app is still the Android Studio template: there is no way to authenticate, so no feature that depends on
the API (purchases, categories, payment methods) can be built. Every resource route of the API requires
`Authorization: Bearer <access_token>`, so login is the first real feature.

The API already exposes everything needed (`../we-sync-api/internal/auth`, routes in
`internal/router/router.go`). This spec covers the client side only; no API change is required.

## Goals

- A user can create an account with e-mail and password.
- A user can sign in and reach a (placeholder) authenticated home screen.
- The session survives app restarts until the refresh token expires or is rejected.
- Tokens are handled according to the security rules in `CLAUDE.md`.

## Non-goals

- Logout UI (belongs to the future home/settings spec; the API endpoint is mapped below for reference).
- Password reset, e-mail verification, social login, biometrics — the API has no support for them.
- Profile screen or showing the user's e-mail on the home (there is no "current user" endpoint; see gaps).
- Real home content (purchases list etc.); the home is a placeholder destination.

## API contract consumed (source of truth: `../we-sync-api`)

Base URL: `/api/v1`. JSON in `snake_case`. Every error uses the envelope
`{"code": "<CODE>", "message": "<text>"}`. Messages are English and intended for developers — the app maps
`status` + `code` to its own pt-BR strings and must not show `message` verbatim.

### `POST /auth/register`

Request:

```json
{ "email": "user@example.com", "password": "********" }
```

Response — `201 Created` (**no tokens**, the user is not signed in):

```json
{ "id": 1, "email": "user@example.com" }
```

| Status | Code | When |
|--------|------|------|
| 400 | `BAD_REQUEST` | Malformed JSON (client bug) |
| 409 | `CONFLICT` | E-mail already registered (`"email already registered"`) |
| 422 | `VALIDATION_ERROR` | `"email is invalid"` or `"password must be between 8 and 72 bytes"` |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected server failure |

### `POST /auth/login`

Request: same shape as register.

Response — `200 OK`:

```json
{ "access_token": "<jwt>", "refresh_token": "<opaque>", "token_type": "Bearer" }
```

| Status | Code | When |
|--------|------|------|
| 400 | `BAD_REQUEST` | Malformed JSON |
| 401 | `UNAUTHORIZED` | Unknown e-mail **or** wrong password — same response by design (`"invalid credentials"`) |
| 422 | `VALIDATION_ERROR` | E-mail or password empty |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected server failure |

### `POST /auth/refresh`

Request: `{ "refresh_token": "<opaque>" }` → `200 OK` with a **new** token pair (same shape as login).

- **Rotation:** the refresh token sent is revoked and replaced. The new `refresh_token` must be stored
  before anything else; reusing the old one returns `401`.
- `401 UNAUTHORIZED` when the token is unknown, revoked or expired. `400` when it is empty.

### `POST /auth/logout` (reference only)

Request: `{ "refresh_token": "<opaque>" }` → `204 No Content`. Unknown/revoked tokens also return `204`.

### Server-side rules the app must know

| Rule | Value / behavior | Evidence |
|------|------------------|----------|
| E-mail normalization | Trimmed and lower-cased by the server before storing/comparing | `normalizeEmail` in `service.go` |
| E-mail validation (register) | Non-empty, ≤ 254 chars, matches `^[^@\s]+@[^@\s]+\.[^@\s]+$` | `validateEmail` |
| Password (register) | 8 to 72 **bytes** in UTF-8 (not characters — accents/emoji count more than 1) | `validatePassword` |
| Password (login) | Only "non-empty" is checked | `Login` |
| Access token lifetime | Default 15 min, configurable (`JWT_ACCESS_TOKEN_DURATION`, max 24h) | `internal/config/env_properties.go` |
| Refresh token lifetime | Default 7 days, configurable (`JWT_REFRESH_TOKEN_DURATION`, max 30 days) | same |
| Expired/invalid access token | `401 UNAUTHORIZED` (`"missing authentication token"` / `"invalid authentication token"`) | `internal/middleware/jwt.go` |
| Login latency | bcrypt cost 12, queued per CPU — login/register can take noticeably longer under load | `service.go` |

The app must not hardcode token lifetimes; it reacts to `401` instead.

### API gaps (do not work around in the client)

- No `GET /me` (current user) endpoint: the app cannot show who is signed in without storing the e-mail
  typed at login.
- Register does not return tokens: signing in after sign-up needs a second call to `/auth/login`.
- No rate limiting on `/auth/login` in the API or nginx (`nginx.conf`) — server-side concern, noted only.

## Functional requirements

### Screens and navigation

1. **Login** — fields: e-mail, password; actions: "Entrar", link "Criar conta".
2. **Sign-up** — fields: e-mail, password, password confirmation; actions: "Criar conta", link back to login.
3. **Home (placeholder)** — authenticated start destination.

Start destination: if a refresh token is stored, the app tries to restore the session (see rules) and goes
to Home on success; otherwise it opens Login. Back from Home does not return to Login/Sign-up (they are
removed from the back stack after authentication).

### Data

| Field | Screen | Required | Client-side rule (mirrors the API) |
|-------|--------|----------|------------------------------------|
| E-mail | Login, Sign-up | Yes | Trimmed; on sign-up must match the API pattern and be ≤ 254 chars |
| Password | Login, Sign-up | Yes | Login: non-empty. Sign-up: 8–72 bytes UTF-8 |
| Password confirmation | Sign-up | Yes | Must equal password (client-only rule) |

Client validation is a convenience; the API is the authority and its `422` must still be handled.

### Business rules

- **BR-1:** Submit is disabled while a request is in flight (no double submit); a progress indicator is shown.
- **BR-2:** Field errors are shown inline on the field; request errors (401, 409, network, 5xx) as a
  message on the screen. Typed values are kept after an error, except that password fields are not
  persisted across process death.
- **BR-3:** After a successful sign-up the app signs the user in automatically by calling `/auth/login`
  with the same credentials (pending confirmation — see open questions). If that login fails, the user
  lands on Login with the e-mail pre-filled and a message that the account was created.
- **BR-4:** Session restore at startup calls `/auth/refresh` with the stored refresh token, stores the new
  pair and goes to Home. On `401` the stored tokens are deleted and Login is shown. On network failure the
  stored tokens are **kept** and the user sees a retry option (the session is not lost because of being offline).
- **BR-5:** Refresh calls are serialized: at most one in flight, because rotation makes a second concurrent
  call with the same token fail with `401` and sign the user out.
- **BR-6:** When an authenticated call returns `401`, the app refreshes once and retries the call once;
  if the refresh returns `401`, tokens are deleted and the user is sent to Login.
- **BR-7:** Fields support Android autofill / password managers (e-mail and password hints, new-password on
  sign-up).

### Error mapping

| Situation | Suggested copy (pt-BR, `strings.xml`) |
|-----------|----------------------------------------|
| Login `401` | "E-mail ou senha inválidos." (never say which one) |
| Sign-up `409` | "Este e-mail já está cadastrado." with action to go to Login |
| `422` e-mail | "Informe um e-mail válido." |
| `422` password | "A senha deve ter entre 8 e 72 caracteres." |
| Passwords differ | "As senhas não conferem." |
| No connection / timeout | "Sem conexão com o servidor. Tente novamente." |
| `5xx` / unexpected / `400` | "Algo deu errado. Tente novamente." |

### Security

- Access and refresh tokens are stored only in encrypted storage (never plain `SharedPreferences`), excluded
  from Auto Backup / device transfer, and never logged, shown in UI or sent to analytics.
- Passwords are never logged or persisted; the request/response body of `/auth/*` is not logged even in debug.
- HTTP (cleartext) is allowed only in the debug build and only for the local dev host
  (`10.0.2.2` / localhost) via `network_security_config`; release uses HTTPS only.
- The API base URL comes from build configuration, not from source code literals.

## Acceptance criteria

- **AC-1:** With no stored session, the app opens on Login.
- **AC-2:** Valid credentials → tokens stored securely → Home; Back from Home closes the app instead of
  returning to Login.
- **AC-3:** Wrong password and unknown e-mail show the same message ("E-mail ou senha inválidos.").
- **AC-4:** Empty fields on Login show inline errors and send no request.
- **AC-5:** Sign-up with a valid e-mail and 8–72 byte password creates the account and the user reaches
  Home (per BR-3).
- **AC-6:** Sign-up with an e-mail already registered shows the `409` message and offers to go to Login.
- **AC-7:** Sign-up rejects locally: invalid e-mail, password < 8 or > 72 bytes, mismatched confirmation —
  without calling the API. A `422` from the API is still mapped to the field error.
- **AC-8:** While a request is in flight the submit button is disabled and a progress indicator is visible.
- **AC-9:** No connection / timeout / `5xx` shows the generic message and the user can retry; the app does
  not crash and no request runs on the main thread.
- **AC-10:** Restarting the app with a valid refresh token goes straight to Home and stores the rotated token.
- **AC-11:** Restarting the app with an expired/revoked refresh token deletes the stored tokens and shows Login.
- **AC-12:** Restarting the app offline with a stored session does not delete the tokens and offers retry.
- **AC-13:** Two concurrent `401`s trigger a single refresh (BR-5/BR-6).
- **AC-14:** No token or password appears in Logcat, UI messages or backups; release build rejects cleartext HTTP.

## Open questions

- [ ] BR-3: auto sign-in after sign-up, or return to Login with the e-mail pre-filled?
- [ ] Password confirmation field on sign-up — keep it (current proposal) or use only a show/hide toggle?
- [ ] Offline start with a stored session (BR-4): retry screen only, or allow entering Home in a read-only/offline state? (No offline data exists yet, so retry is the proposal.)
- [ ] Should the API add a `GET /me` and login rate limiting? Out of scope here; to be opened in `we-sync-api` if wanted.
- [ ] Architecture choices for the plan phase (still undecided in `CLAUDE.md`): HTTP client, JSON
      serialization, ViewModel/coroutines, DI, encrypted token storage. To be proposed and confirmed in `plan.md`.

## Changelog

- 2026-10-16 — Created.
