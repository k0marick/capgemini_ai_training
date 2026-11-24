# ADR-002: Server-side HTTP session authentication with OAuth2/OIDC

- Status: Proposed

## Context
Users sign in with Google (OIDC) or GitHub (OAuth 2.0). The session must survive refresh, support logout, and authenticate WebSocket handshakes. Single backend instance for MVP.

## Decision
Spring Security `oauth2Login` (Authorization Code + PKCE, state). After login, the app stores an authenticated principal in a server-side HTTP session. Browser holds only an `HttpOnly`, `SameSite=Lax` (and `Secure` outside `dev`) `JSESSIONID`. CSRF via `X-XSRF-TOKEN`. The app never stores provider tokens in the browser.

## Alternatives
- **JWT access/refresh tokens**: needs refresh logic, revocation strategy, and token storage decisions in the SPA; WebSocket needs a separate token mechanism.
- **Keycloak / external IdP**: explicitly excluded; not justified for MVP.
- **Spring Session + MySQL/Redis store**: not needed for one instance; Redis is excluded.

## Consequences
- Logout is immediate (invalidate session); sockets closed via `SessionDestroyedEvent`.
- WebSocket reuses handshake authentication.
- No refresh-token logic.
- Multi-instance deployment would require sticky sessions or a shared session store (future ADR).
- Session fixation, state, and OIDC `iss`/`aud`/signature validation handled by Spring Security.
