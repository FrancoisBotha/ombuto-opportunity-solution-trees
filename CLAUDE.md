# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and other AI
coding agents when working in this project.

## Mandatory Agent Workflow

All agents working in this repository **MUST** follow the Ombuto Code
engineering workflow documented in:

> [`.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md`](.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md)

Agents MUST treat that document as a **system-level instruction**. Failure
to follow the workflow is considered a task error.

The engineering guide defines:

- The source of truth (`.ombutocode/data/ombutocode.db` `backlog_tickets` table)
- Planning mode vs execution mode
- Ticket lifecycle and status transitions (`backlog` → `todo` → `in_progress` → `eval` → `review` → `done`)
- Scope control rules ("don't expand a ticket — create a new one")
- Forbidden behaviors (no broad refactors without a ticket, no new frameworks without approval, etc.)

**Read `.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md` before starting any work
in this project.**

## Project-specific conventions

Add your project-specific rules, coding conventions, forbidden patterns,
and other agent guidance below this line. Anything you add here is read
into the agent's context on every run.

### npm scripts call Maven through `mvnw.cjs`

The `package.json` scripts run `node mvnw.cjs …`, not `./mvnw …`. npm runs scripts through
cmd.exe on Windows, where `./mvnw` fails, so `mvnw.cjs` picks `mvnw.cmd` or `./mvnw` for the
platform. The JHipster generator rewrites these scripts back to `./mvnw` — when it offers to
overwrite `package.json`, keep the `node mvnw.cjs` form.

### Generator-owned files that carry hand customisations (preserve on regeneration)

`npx jhipster jdl ombuto.jdl` rewrites the files below. Each of them carries hand edits that
must survive regeneration. Review the generator's diff hunk by hunk and restore these edits
(engineering guide §4b). Behaviour belongs in custom classes; this list covers the few edits that
could not go there.

- **Every generated `*Resource` except `ProductResource`**: a class-level
  `@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")`. Their generated
  `*ResourceIT` classes run as `ROLE_ADMIN`. New entities get the same edit and an entry in
  `GeneratedEndpointsSecurityIT`.
- **Product stack (team-scoped)**:
  - `ProductResource`, `ProductService`.
  - `ProductServiceImpl`: `TeamAccessService` checks. `sortOrder` is server-owned (appended on
    create, kept on PUT/PATCH) and read under the `TreeStructureLock`. Save, update and partial
    update take the lock (both teams on a team change), and partial update never mutates the
    current `Team`. Delete goes through `TreeNodeCascadeService`.
  - `ProductRepository` (`findAllByTeamId*`, max/sortOrder queries), `ProductDTO` (no
    `@NotNull` on the server-set `createdDate` / `sortOrder`), `ProductResourceIT` (team-access
    version plus sortOrder).
  - `entities/product/product-update.vue`, `product-update.component.ts` and
    `product-update.component.spec.ts`: Sort Order is read-only with a hint and not required.
- **`ExceptionTranslator`**: handlers for `NodeWriteRuleException`, `DataIntegrityViolation` and
  any constraint violation in the cause chain (409 `error.dataintegrity`), unique violations
  (409 `error.duplicate`), lock and concurrency failures (409 `error.concurrencyFailure`), and
  generic details with no SQL or Java `toString`. Also its tests `ExceptionTranslatorIT` and
  `ExceptionTranslatorTestController`.
- **`TeamMemberRepository`** and **`UserRepository`**: added queries.
- **`config/ApplicationProperties.java`**: the `seed` property. **`config/application-dev.yml`**:
  `application.seed.enabled: true`, and the Liquibase `contexts: dev` (without `faker`).
- **`config/liquibase/master.xml`**: the custom includes
  `20260918110000_add_unique_constraint_team_member.xml`,
  `20260919120000_add_index_node_history_node.xml` and
  `20260921120000_link_001_delete_placeholder_node_links.xml`.
- **Frontend shell**:
  - `router/entities.ts`: every entity route has `authorities: [Authority.ADMIN]`.
  - `router/pages.ts`: the Teams and OST routes.
  - `app.vue`: the `full-bleed` mode; `app.component.ts`: the `fullBleed` computed (route
    `meta.fullBleed`) that `app.vue` reads.
  - `core/jhi-navbar/jhi-navbar.vue`.
  - `core/jhi-footer/jhi-footer.vue`: the "Opportunity Solution Tree" footer text.
  - `core/home/home.vue`, `home.component.ts` and `home.component.spec.ts`: Trees and Teams
    links for everyone, Static Data cards for admins only.
- **`core/sidebar-menu/sidebar-menu.component.ts`** (custom, but it holds the generator's
  `jhipster-needle-add-entity-to-menu`): the generator inserts new entities there. Move them
  into the admin-only Static Data group.
- **Branding** (`src/main/webapp/`): `index.html` (title, theme colour, SVG icon),
  `content/scss/global.scss` (brand styles), `content/css/loading.css` (the seed logo),
  `manifest.webapp` (name, colours, icons) and `favicon.ico`.
- **Keycloak**: `src/main/docker/keycloak.yml` (mounts `keycloak-themes/ost`) and
  `src/main/docker/realm-config/jhipster-realm.json` (display name and `loginTheme: "ost"`).
- **`pom.xml`**: description, project URL and the Apache 2.0 `<licenses>` block.
- **`.gitattributes`**: `* text=auto eol=lf`.
- **Not generated, but part of the customised shell** (the generator has no template for them, so
  it never rewrites them; listed so they are not mistaken for generator output):
  `content/scss/va-sidemenu.scss` (the `.full-bleed` layout), `content/scss/va-navbar.scss` and
  `core/sidebar-menu/sidebar-menu.vue`.
- **Build and test config**:
  - `package.json`: the `node mvnw.cjs` scripts (see above) and the OST dependencies.
  - `vite.config.ts`: `optimizeDeps.include` for the OST packages, guarded by
    `app/ost/vite-optimize-deps.spec.ts`; and `/mcp` in the dev-server `proxy` list so the
    Connect an agent page's endpoint (`window.location.origin + /mcp`) reaches the backend in
    development, guarded by `app/connect-agent/connect-agent.component.spec.ts`.
  - `playwright.config.ts`: the `cleanup` teardown project.
  - `src/main/webapp/app/test-setup.ts`: the localStorage / sessionStorage shim.
  - `eslint.config.ts`: `.ombutocode` and `docs/` ignored.

The Tree Builder rewrote the entity Liquibase changelogs in place (pre-production). An old dev
database fails checksum validation: delete `target/h2db/`.

### `@vue-flow/core` is pinned at exactly 1.48.2

`pinRendered` (`app/ost/canvas/canvas-model.ts`) keeps a node that is being renamed or has its
`+` menu open rendered by setting its `dragging` flag, which Vue Flow's visibility culling reads
as "keep this node". That is internal behaviour, not API. When bumping `@vue-flow/core`, re-run
the `ost-perf` and `ost-canvas-edit` Playwright specs before merging.
