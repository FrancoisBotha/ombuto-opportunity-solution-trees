# Ombuto Opportunity Solution Tree

A collaborative web application for product teams that practise **continuous discovery**. Each team
keeps one living [Opportunity Solution Tree](https://www.producttalk.org/opportunity-solution-trees/):
its products at the top, the outcomes each product is chasing, the customer opportunities behind
those outcomes, the solutions being considered, and the assumptions and evidence used to test them.

It replaces trees drawn in whiteboard tools — which have no structure, go stale within weeks, and
are invisible outside the team that drew them — with a structured, shared, always-current model that
links to the tickets and pages where the work actually happens.

> **Status: active development.** The core team tree workflow and read-only MCP access are
> available, including meeting transcript capture and reading. Interviews and leadership reporting
> still have planned work. Transcript backup coverage and epic closeout remain outstanding.
> See the [roadmap](#roadmap). Not yet ready for production use.

## Who it is for

- **Product trios** — product manager, designer and tech lead — who interview customers most weeks
  and need somewhere to record what they learned, turn it into opportunities, and track which
  solutions and assumptions they are testing.
- **Heads of product** and similar leaders, for whom a cross-team read-only overview is planned.

## Features

| Capability                                                                                                                                       | State     |
| ------------------------------------------------------------------------------------------------------------------------------------------------ | --------- |
| **Teams and products** — create teams, manage members and products, and use owner, editor and viewer roles                                       | Available |
| **SSO sign-in** — OAuth2 / OpenID Connect through Keycloak, with a guide for brokering company identity providers                                | Available |
| **Visual tree editor** — build product, outcome, nested opportunity, solution, assumption and evidence branches; edit details and open questions | Available |
| **Tree rearranging** — move or reorder nodes by dragging or with keyboard controls; collapse branches individually or all at once                | Available |
| **Assumption tracking** — record status, confidence and owner; review assumptions in the experiment tracker                                      | Available |
| **Live collaboration** — changes to the shared tree and node comments appear in other open sessions                                              | Available |
| **Node discussion** — open chat from Detail or a canvas chip; replies are not threaded                                                           | Available |
| **Node links** — add named URLs, including Jira and Confluence links, to tree nodes                                                              | Available |
| **MCP server** — read-only, team-scoped tools for trees, nodes, labels, interviews, search, discussions and transcripts; connect through OAuth   | Available |
| **Admin backup and restore** — export supported data; meeting transcripts are not yet covered                                                    | Available |
| **Interviews as evidence** — interview records and opportunity links exist, but the planned team-facing workflow is unfinished                   | Partial   |
| **Leadership overview** — read-only view across all teams                                                                                        | Planned   |
| **Meeting transcripts** — paste/upload, read, edit and delete from a dedicated tab                                                               | Available |

## Using the tree

- **Canvas:** the active node has a bright outline and tinted background. Collapse all / Expand
  all beside Search nodes applies to the current product view and fits the result to the canvas.
  Collapse preferences are saved locally per user and team.
- **Detail:** edit fields, notes and labels, navigate children, or open the Chat dialog with its
  message-count button. The full-page node detail view also has a discussion section.
- **Transcripts:** select a non-product node and open its Transcripts tab. Paste text or upload
  a `.txt`, `.vtt` or `.srt` file, review the parsed text, then save. Editors can edit/delete;
  viewers can read. Counts appear on nodes and tabs. Audio/video transcription is not included.
- **Solution statuses:** CANDIDATE, EXPLORING, DEVREADY, BUILDING, SHIPPED and DROPPED.
- **Help:** use Help in the navbar for searchable guidance without leaving the current page.

## Connecting an agent

Open **Connect an agent** below Teams in the sidebar, or visit `/connect-agent`. The page has
copyable endpoint and client configuration blocks that follow the current color theme, plus
OAuth sign-in instructions. MCP is read-only and enforces team membership. Node and tree reads
include labels and the current status, including EXPLORING and DEVREADY. `list_transcripts`
returns metadata; `get_transcript` returns a transcript's body. UI layout and collapse preferences
do not change the MCP data contract.

## Backup scope and current limitation

The admin backup includes the existing tree entities, statuses, labels and their node links,
comments, interviews, history and team membership references. **Meeting transcripts are currently
missing from the archive and restore implementation. Do not rely on this export as a complete
backup for transcript-bearing trees.** Existing transcript foreign keys may also prevent restore
from clearing node data. Transcript coverage needs to be added and verified before that workflow
can be considered complete.

Browser preferences (including collapse state) and Keycloak accounts/configuration are outside
the application archive and need separate handling. Restore expects referenced user identities
to exist. New solution status values require an application version that recognizes them.

## Application version

The navbar displays the version from `package.json`, for example `v0.1.0`.
The Vite development server adds `-dev` (`v0.1.0-dev`); production builds omit it.
An explicit `APP_VERSION` environment variable overrides the package version at build time.
Maven builds already supply their project version from `pom.xml` through this override,
including `-SNAPSHOT` for unreleased backend builds.

Use semantic versions: patch for fixes, minor for features, major for breaking changes.
To prepare a release, run `npm version patch --no-git-tag-version` (or `minor` / `major`)
to update `package.json` and `package-lock.json`, and set the top-level project version in
`pom.xml` to the same release version. Commit those files together. For subsequent backend
development, use the next version with `-SNAPSHOT`. Restart the Vite dev server after changing
the package version; deployed assets need a rebuild to pick up a new version.

## Tech stack

|               |                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------- |
| **Backend**   | Java 21, Spring Boot 4 (Spring MVC, Spring Security, Spring Data JPA), Hibernate, MapStruct       |
| **Frontend**  | Vue 3, TypeScript, Pinia, Vue Router, BootstrapVueNext, Vite                                      |
| **Real-time** | STOMP over WebSocket (Spring's simple broker)                                                     |
| **Database**  | PostgreSQL in production, H2 on disk for development, Liquibase migrations                        |
| **Auth**      | OAuth2 / OIDC with Keycloak                                                                       |
| **Tests**     | JUnit 5, ArchUnit, Cucumber and Testcontainers on the backend; Vitest and Playwright on the front |
| **Scaffold**  | [JHipster 9](https://www.jhipster.tech/) monolith, generated from [`ombuto.jdl`](ombuto.jdl)      |

The full design is in [`docs/Architecture/Architecture.md`](docs/Architecture/Architecture.md).

## Getting started

### Prerequisites

- **Java 21** or newer
- **Node.js 24.14** or newer
- **Docker** — Keycloak and PostgreSQL run as containers

### Run it locally

```bash
npm install          # frontend dependencies
./mvnw               # backend on http://localhost:8080
npm start            # frontend dev server on http://localhost:9000  (second terminal)
```

`./mvnw` starts the Keycloak and PostgreSQL containers from `src/main/docker/services.yml` for you
(Spring Boot's Docker Compose support), creates the H2 development database under `target/h2db/`,
and seeds sample data (`DevDataSeeder`): four teams, and the full prototype tree in Team Jupiter. On Windows use `mvnw.cmd` from PowerShell, or `./mvnw` from Git Bash.

Open **http://localhost:9000** and sign in with one of the development accounts:

| User    | Password | Roles                     |
| ------- | -------- | ------------------------- |
| `admin` | `admin`  | `ROLE_ADMIN`, `ROLE_USER` |
| `user`  | `user`   | `ROLE_USER`               |

The Keycloak admin console is at http://localhost:9080 (`admin` / `admin`). To sign users in through
a company identity provider, see
[`docs/Architecture/keycloak-idp-brokering.md`](docs/Architecture/keycloak-idp-brokering.md).

> A user can only be added to a team after they have signed in once — that first login is what
> creates their record in the application.

### Troubleshooting

- **New controls return 404 despite appearing in the UI** — rebuild/restart the backend after
  pulling changes. The Vite frontend updates independently; an older Java process can be missing
  the new endpoints.

- **`Bind for 127.0.0.1:9080 failed: port is already allocated`** — another Keycloak container is
  holding the port. `docker ps -a --filter name=keycloak`, then remove any that is not
  `opportunitysolutiontree-keycloak-1`.
- **Liquibase checksum errors on startup** — the development database predates a schema change
  (the Tree Builder rewrote the entity changelogs in place). Stop the app and delete `target/h2db/`.
- **Frontend tests fail with `localStorage.clear is not a function`** — Node 25 or newer; run them
  with `NODE_OPTIONS=--no-experimental-webstorage`.

## Testing

```bash
npm run backend:unit:test -- -Pprod   # all backend tests against PostgreSQL (needs Docker)
npm test                              # lint + all frontend unit tests
npm run e2e                           # Playwright end-to-end tests (needs the app running)
```

How to run a single test, what each test layer covers, and the known pitfalls are all in
[`docs/Test Strategy/test-strategy.md`](docs/Test%20Strategy/test-strategy.md).

## Building for production

```bash
./mvnw -Pprod clean verify                     # executable jar in target/
java -jar target/*.jar

npm run java:docker                            # or a Docker image, built with Jib
docker compose -f src/main/docker/app.yml up   # app + PostgreSQL + Keycloak
```

Production needs a PostgreSQL database and an OIDC provider; configure them through the standard
Spring properties (`SPRING_DATASOURCE_URL`, `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI`,
and so on). The [JHipster production guide](https://www.jhipster.tech/production/) covers the rest.

## Project layout

```
ombuto.jdl                  Data model — the single source of truth for entities
.jhipster/                  Entity configuration generated from the JDL
src/main/java/              Spring Boot application (com.opportunity.tree)
src/main/webapp/app/        Vue application
  entities/                   generated CRUD screens
  teams/, ost/, connect-agent/  hand-written product features
src/main/resources/config/  Spring configuration and Liquibase changelogs
src/main/docker/            Compose files for Keycloak, PostgreSQL, the app, monitoring
src/test/java/              JUnit, ArchUnit and Cucumber tests
src/test/javascript/        Playwright end-to-end tests
docs/                       PRD, architecture, epics, test strategy, use cases
scripts/                    Repository maintenance scripts
.ombutocode/                Ombuto Code workbench (see below)
```

## How this project is developed

The application is planned and built with **Ombuto Code**, an agent-driven engineering workbench
vendored under [`.ombutocode/`](.ombutocode/). Work flows from the
[PRD](docs/Product%20Requirements%20Document/PRD.md) to [epics](docs/Epics/) to small tickets, which
coding agents implement, test and evaluate before a human reviews them.
[`GettingStarted.md`](GettingStarted.md) explains the workbench.

Two rules matter to anyone contributing, human or agent:

1. **Follow the engineering guide** —
   [`.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md`](.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md)
   defines the ticket lifecycle and scope rules.
2. **The data model changes only through the JDL.** Edit [`ombuto.jdl`](ombuto.jdl), then run
   `npx jhipster jdl ombuto.jdl`, so the entity, API, UI, migration and generated tests stay in
   step. Never hand-edit a generated entity to change the model.

## Roadmap

| Epic | Scope                                                                          | State                                                |
| ---- | ------------------------------------------------------------------------------ | ---------------------------------------------------- |
| 1    | [Teams & Scoped Access](docs/Epics/epic_01_TEAMS_AND_SCOPED_ACCESS.md)         | Available                                            |
| 2    | [Tree Editor Core](docs/Epics/epic_02_TREE_EDITOR_CORE.md)                     | Available                                            |
| 3    | [Tree Rearranging](docs/Epics/epic_03_TREE_REARRANGING.md)                     | Available                                            |
| 4    | [Assumptions & Experiments](docs/Epics/epic_04_ASSUMPTIONS_AND_EXPERIMENTS.md) | Partial; core assumptions shipped through Epic 11    |
| 5    | [Real-Time Collaboration](docs/Epics/epic_05_REALTIME_COLLABORATION.md)        | Available; a reliability follow-up remains           |
| 6    | [Node Comments](docs/Epics/epic_06_THREADED_COMMENTS.md)                       | Available as flat chat-style comments                |
| 7    | [Interviews as Evidence](docs/Epics/epic_07_INTERVIEWS_AS_EVIDENCE.md)         | Partial                                              |
| 8    | [Jira & Confluence Links](docs/Epics/epic_08_JIRA_AND_CONFLUENCE_LINKS.md)     | Available; provider inference is still being refined |
| 9    | [Leadership Overview](docs/Epics/epic_09_LEADERSHIP_OVERVIEW.md)               | Planned                                              |
| 10   | [MCP Server](docs/Epics/epic_10_MCP_SERVER.md)                                 | Available; tool coverage is still expanding          |
| 11   | [OST Tree Builder](docs/Epics/epic_11_OST_TREE_BUILDER.md)                     | Available                                            |
| 12   | [Meeting Transcripts](docs/Epics/epic_12_MEETING_TRANSCRIPTS.md)               | Core available; backup coverage and closeout pending |

## Documentation

- [Product Requirements](docs/Product%20Requirements%20Document/PRD.md) — what it is and who it is for
- [Architecture](docs/Architecture/Architecture.md) — design, stack and deployment
- [Endpoint inventory](docs/Architecture/endpoint-inventory.md) — every REST endpoint and how it is protected
- [Test strategy](docs/Test%20Strategy/test-strategy.md) — how to run and write tests
- [Functional](docs/Functional%20Requirements/FunctionalRequirements.md) and
  [non-functional](docs/Non-Functional%20Requirements/NonFunctionalRequirements.md) requirements
- [JHipster 9 documentation](https://www.jhipster.tech/documentation-archive/v9.0.0) — for everything the scaffold provides

## Licence

Copyright 2026 Francois Botha. Licensed under the [Apache License, Version 2.0](LICENSE).

This project stands on a great deal of other people's work.
[`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md) lists every library distributed with the
application and the licence it is used under, including the few that need particular attention
(Font Awesome's CC BY 4.0 icons, and Liquibase 5, which is source-available rather than open source).

## Acknowledgements

The Opportunity Solution Tree is a product discovery technique created by **Teresa Torres**,
described in _Continuous Discovery Habits_ and at [producttalk.org](https://www.producttalk.org/).
This project is an independent implementation and is not affiliated with, or endorsed by, Teresa
Torres or Product Talk.

The application scaffold was generated by [JHipster](https://www.jhipster.tech/).
