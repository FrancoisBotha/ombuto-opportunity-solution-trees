# Keycloak Identity-Provider Brokering — Deployer Guide

This guide is for the person deploying Ombuto OST who wants their organisation
to sign in with a corporate identity provider (Google Workspace, Microsoft
Entra ID / Azure AD, Okta, a company SAML IdP, etc.) instead of — or in
addition to — the local Keycloak accounts shipped in the dev realm
(`src/main/docker/realm-config/jhipster-realm.json`).

Ombuto OST does not talk to the corporate IdP directly. It only trusts one
issuer: the app's Keycloak realm. Keycloak takes on the role of an **identity
broker** — the app authenticates against Keycloak, Keycloak redirects the user
to the corporate IdP, and Keycloak issues its own OIDC token back to the app.
Local Keycloak accounts keep working alongside the brokered IdP, which lets
you keep an admin escape hatch and lets contractors sign in without a
corporate directory account.

Covers FR-008 (users can sign in through a company IdP brokered by Keycloak
or a local Keycloak account) and NFR-003 (the OIDC client secret and the
database password are supplied by environment variables; nothing beyond the
dev `secret-samples` profile is committed).

---

## 1. Architecture at a glance

```
Browser ──1──▶ Ombuto OST (Spring Boot)
                     │
                     └─2 OIDC auth-code redirect──▶ Keycloak realm
                                                       │
                                                       ├─ local user? sign in here
                                                       │
                                                       └─3 IdP brokering──▶ Corporate IdP
                                                                              (OIDC or SAML)
                                                       ◀────────────────── ID token / SAML assertion
                     ◀───4 Keycloak-issued ID token ───┘
                     │
                     └─5 First-login user sync into the app's USER table
```

1. The browser hits a protected route in the app.
2. Spring Security redirects to Keycloak (`spring.security.oauth2.client.provider.oidc.issuer-uri`).
3. Keycloak presents its login page. Local Keycloak users authenticate here.
   For a brokered IdP the user clicks the corporate IdP button and Keycloak
   redirects them to the corporate IdP; the corporate IdP redirects back to
   Keycloak with an assertion.
4. Keycloak issues its own ID token to the app. The app only ever validates
   Keycloak-issued tokens — the corporate IdP's issuer is invisible to the app.
5. On the user's first sign-in JHipster's `AccountResource` /
   `UserService.getUserFromAuthentication()` inserts the account into the
   app's `jhi_user` table using the token's `sub`, `preferred_username`,
   `email`, `given_name`, `family_name` and `groups` claims. Subsequent
   sign-ins update the row.

Consequences:

- The app is IdP-agnostic. Adding, replacing or removing a brokered IdP is a
  pure Keycloak change and does not touch the app or its database.
- **Only users who have signed in at least once appear in the app database**
  (see [Data Model Impact](../Epics/epic_01_TEAMS_AND_SCOPED_ACCESS.md#7-data-model-impact)),
  which is why team owners can only add users to a team _after_ those users
  have signed in once. This also means you cannot pre-provision a team's
  members ahead of their first login.
- Local Keycloak accounts and brokered IdP accounts co-exist. A user who
  authenticates via a brokered IdP is still a Keycloak account (in the realm
  configured for that IdP), and the local `admin` / `user` accounts keep
  working even after brokering is enabled.

---

## 2. Prerequisites

- A Keycloak realm you control. The dev realm shipped in
  `src/main/docker/realm-config/jhipster-realm.json` is fine for local
  testing. In production you will start from that realm export, rename it
  and rotate every secret (see [Section 6](#6-secrets-managed-outside-git)).
- The realm already has the `web_app` OIDC client the app authenticates as.
  Keep it a **confidential** client with `Standard Flow` enabled; the
  client secret is what the app carries in
  `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET`.
- A `roles` client scope mapped into the ID token / access token, containing
  the JHipster `ROLE_USER` and `ROLE_ADMIN` roles. Users signing in through
  a brokered IdP will need at least `ROLE_USER` — see
  [Section 5](#5-role-assignment).
- Whichever protocol your corporate IdP speaks (OIDC or SAML 2.0), plus the
  discovery URL / metadata XML and — for OIDC — a client-id/secret pair the
  IdP has issued for Keycloak.

---

## 3. Add an OIDC identity provider

Log in to the Keycloak admin console as a realm admin, select the app's
realm, and open **Identity providers → Add provider → OpenID Connect v1.0**.

1. **Alias:** short, URL-safe identifier (e.g. `google`, `entra`,
   `okta-corp`). This becomes part of the redirect URI you register with the
   corporate IdP:
   `https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint`.
2. **Display name:** what the user sees on the Keycloak login page
   (e.g. "Sign in with Google").
3. Under **OpenID Connect settings**:
   - Prefer **Use discovery endpoint** and paste the corporate IdP's
     `.well-known/openid-configuration` URL. Keycloak fills in the auth,
     token, JWKS and end-session URLs for you.
   - **Client authentication:** `Client secret sent as post` (or JWT signed
     with a private key if the corporate IdP requires it).
   - **Client ID / Client secret:** the credentials the corporate IdP issued
     for this Keycloak-as-relying-party registration. **Never** commit these
     to this repository — Keycloak stores them in its own database.
   - **Default scopes:** `openid profile email`.
   - **Sync mode:** `force` if you want Keycloak to refresh the linked user's
     first / last name and email from the IdP on every login;
     `import` to only sync on the first login.
4. Register the redirect URI Keycloak just displayed
   (`https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint`) with
   the corporate IdP as an allowed redirect URL for its client.
5. Save. The provider now appears as a button on the Keycloak login page.

Optional but recommended:

- **First login flow:** the default `first broker login` flow prompts the
  brokered user to confirm the account. If the corporate IdP is trusted,
  switch it to a flow that auto-links by email so the first login is
  seamless.
- **Trust email:** enable this if the corporate IdP verifies email addresses
  itself — otherwise Keycloak treats the email as unverified and the app's
  first-login sync creates the user with `activated=false` behaviour that
  depends on your JHipster configuration.
- **Domain restriction:** for Google / Entra, use the built-in identity
  provider mapper `Hardcoded Attribute` or `Advanced Claim to Role Mapper`
  to reject users whose `hd` / `tid` claim does not match your organisation.

---

## 4. Add a SAML identity provider

Same admin console path, but pick **Identity providers → Add provider →
SAML v2.0**.

1. **Alias:** as above.
2. **Import from URL / file:** paste the corporate IdP's SAML metadata URL
   or upload the metadata XML. Keycloak fills in the SSO URL, single logout
   URL and signing certificate.
3. Keep **Want AuthnRequests signed** enabled and configure the signing key
   the corporate IdP expects.
4. Under **Mappers**, add:
   - `Attribute Importer` mapping the assertion's `email`, `firstName`,
     `lastName` attributes onto Keycloak user attributes (`email`,
     `firstName`, `lastName`).
   - `SAML Attribute to Role` mappers if the assertion carries group / role
     data that should become `ROLE_USER` (or `ROLE_ADMIN`) in Keycloak.
5. Give the corporate IdP the Keycloak SP metadata Keycloak displays at
   `https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint/descriptor`.

---

## 5. Role assignment

Everyone who reaches the app needs the JHipster `ROLE_USER` role in
Keycloak (`ROLE_ADMIN` for admin users). For a brokered IdP you have three
options:

- **Default role in the realm.** Under _Realm settings → User registration →
  Default roles_, add `ROLE_USER`. Every user Keycloak creates through
  brokering will have it.
- **Identity provider mapper.** On the IdP → _Mappers_ tab, add a
  `Hardcoded Role` mapper granting `ROLE_USER` to every user that logs in
  via that IdP.
- **Group / claim mapping.** For OIDC IdPs, map an `hd` / `groups` /
  `wids` claim to `ROLE_ADMIN`; for SAML, map a group attribute. This lets
  the corporate IdP govern who becomes an admin without editing Keycloak
  per-user.

Verify the token: sign in through Keycloak, open the app, and confirm the
`GET /api/account` response lists the expected `authorities`.

---

## 6. Secrets managed outside git

Two secrets never live in the repository. They are read from the environment
by Spring Boot's property binder.

| Secret                               | Env var                                                         | Spring property it binds to                                     | Sample file (committed, all placeholders empty)                                                        |
| ------------------------------------ | --------------------------------------------------------------- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| OIDC client secret for `web_app`     | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET` | `spring.security.oauth2.client.registration.oidc.client-secret` | `src/main/resources/config/application-secret-samples.yml` (`client-secret: web_app` — dev value only) |
| Datasource password (Postgres, prod) | `SPRING_DATASOURCE_PASSWORD`                                    | `spring.datasource.password`                                    | `src/main/resources/config/application-secret-samples.yml` (`password:` — blank)                       |

Standard operator setup:

```bash
# Read these values from your secrets manager (Vault, AWS Secrets Manager,
# GCP Secret Manager, Kubernetes Secret) at deploy time — never from a file
# on the pod.
export SPRING_PROFILES_ACTIVE=prod
export SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI=https://sso.example.com/realms/ombuto
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_ID=web_app
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET='***'
export SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.internal:5432/opportunitySolutionTree
export SPRING_DATASOURCE_USERNAME=opportunitySolutionTree
export SPRING_DATASOURCE_PASSWORD='***'
```

Anywhere else — `application-*.yml`, Dockerfiles, Helm charts, CI scripts —
these two secrets must remain absent or blank. The `secret-samples` profile
exists as a template, not a runtime configuration; production runs never
activate it.

**Rules of thumb:**

- Do not commit the OIDC client secret. `application-secret-samples.yml`
  contains `client-secret: web_app` on purpose — it matches the dev realm
  export, is only useful against the local dev Keycloak on
  `http://localhost:9080/realms/jhipster`, and it is not the production
  secret. Rotate `web_app` when you clone the realm for production.
- Do not commit the database password. Every committed `spring.datasource.password`
  value in this repository is blank.
- Any test-fixture password (e.g. `TestSecurityConfiguration#clientSecret("client-secret")`
  in `src/test/java`) is a stub used to satisfy the Spring Security
  autoconfigurer inside the test context; those are not real secrets and do
  not appear in a running deployment.

If you ever change what belongs on this list, update this section — the
per-ticket secrets check (Section 8) grep is written against exactly it.

---

## 7. Local Keycloak dev accounts

The dev realm at `src/main/docker/realm-config/jhipster-realm.json` seeds two
local Keycloak accounts:

| Login   | Password | Realm roles               |
| ------- | -------- | ------------------------- |
| `admin` | `admin`  | `ROLE_ADMIN`, `ROLE_USER` |
| `user`  | `user`   | `ROLE_USER`               |

They keep working after you add a brokered IdP — Keycloak's login screen
lists both the local username/password form _and_ every configured IdP
button. The Playwright e2e test at
`src/test/javascript/playwright/teams-collaboration.spec.ts` signs in as
both `admin` and `user` and covers the "local Keycloak accounts still work"
acceptance criterion (Epic AC 6, ticket AC 4).

Bringing the stack up locally:

```bash
docker compose -f src/main/docker/keycloak.yml up -d   # Keycloak on :9080
./mvnw                                                  # Spring Boot on :8080
npm start                                               # Vite dev server on :9000
```

Then sign in via [http://localhost:9000](http://localhost:9000) → **Sign in**;
the browser will bounce through Keycloak and back.

---

## 8. Secrets check (per ticket)

The per-ticket check is a repository-wide grep, run against
[`src/`](../../src) minus the two allowlisted locations:

- `src/main/docker/realm-config/` — the exported dev realm containing the
  local `admin` and `user` credentials.
- `src/main/resources/config/application-secret-samples.yml` — the
  documented `secret-samples` profile.

Command:

```bash
# From the repo root, on Git Bash / WSL / macOS / Linux.
grep -rEn 'client-secret|password:' src/ \
  | grep -v 'src/main/docker/realm-config/' \
  | grep -v 'src/main/resources/config/application-secret-samples.yml' \
  | grep -Ev 'key-store-password|# password:'
```

For the TEAMS-008 sweep on 2026-09-19 this returns only
`src/test/java/com/opportunity/tree/config/TestSecurityConfiguration.java`
with `clientSecret("client-secret")` — a Spring Security test stub, not a
credential — and empty `password:` entries in
`src/main/docker/grafana/provisioning/datasources/datasource.yml`.
Both are safe. Any _new_ line that turns up here either has to be moved out
of the repository (into env vars / secrets manager) or documented as a
non-secret with a comment explaining why, and a follow-up ticket raised if
neither is possible.

---

## 9. Running the two-user e2e test

See [`docs/Test Strategy/test-strategy.md`](../Test%20Strategy/test-strategy.md)
sections 3 and 8 for the full E2E toolchain.

Quick recipe:

```bash
# 1. Bring the three processes up.
docker compose -f src/main/docker/keycloak.yml up -d
./mvnw                       # in one terminal, backend on :8080
npm start                    # in another terminal, Vite on :9000

# 2. Run every Playwright spec (the standard e2e command from the test strategy).
npm run e2e

# 3. Or just the two-user collaboration test (fast iteration).
npx playwright test src/test/javascript/playwright/teams-collaboration.spec.ts
```

The test signs in through Keycloak twice — once as the seeded `admin`
account (the team owner) and once as the seeded `user` account (the viewer
and, in the sibling scenario, the non-member). It covers:

- Local Keycloak login for both users, verifying that local accounts still
  work (Epic AC 6, ticket AC 4).
- Owner creates a team, adds the other user as viewer, adds a product;
  viewer sees the team on _My teams_ with a viewer badge, sees the
  product, and sees no edit controls (ticket AC 5).
- A user who is not a member does not see the team on _My teams_, gets
  the forbidden banner when navigating to `/teams/<id>` in the UI, and
  gets HTTP 403 from `GET /api/team-management/teams/<id>` (ticket AC 6,
  Epic AC 2).

If you want to point the test at a different Keycloak or use different
credentials, override at the shell:

```bash
export E2E_BASE_URL=http://localhost:9000
export E2E_ADMIN_USERNAME=admin
export E2E_ADMIN_PASSWORD=admin
export E2E_USER_USERNAME=user
export E2E_USER_PASSWORD=user
npx playwright test src/test/javascript/playwright/teams-collaboration.spec.ts
```

The test always signs users in against the app's own Keycloak realm — a
brokered corporate IdP is not exercised here because a real corporate IdP
is not available in CI.

---

## 10. Production checklist

Before enabling a brokered IdP for real users:

- [ ] Cloned the dev realm export into a production realm and rotated the
      `web_app` client secret (and any other client secret in the export).
- [ ] Removed or disabled the local `admin` / `user` accounts, or reset
      their passwords to something only the operator knows and stored the
      new password in the secrets manager as the break-glass credential.
- [ ] `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET` and
      `SPRING_DATASOURCE_PASSWORD` set from the secrets manager, not from a
      file in git or a plain env-file on the host.
- [ ] Keycloak reachable at the URL the app's `issuer-uri` points at over
      HTTPS.
- [ ] Redirect URIs on the corporate IdP registration match the Keycloak
      realm URL exactly (including the trailing `/endpoint`).
- [ ] End-to-end sanity: sign in through the brokered IdP → confirm the
      app creates a `jhi_user` row with the expected login / email → add
      the user to a team → confirm they see it on _My teams_.
