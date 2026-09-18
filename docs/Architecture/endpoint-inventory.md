# REST endpoint inventory

Source of truth: every `@RestController` under `com.opportunity.tree.web.rest`.
This inventory backs TEAMS-004 (NFR-001, NFR-002): every generated CRUD endpoint
whose entity is team-owned must either be restricted to `ROLE_ADMIN` at the
controller or route through `TeamAccessService` in the service layer.

## Legend

- **Owner scope** — how the entity is owned:
  - `team-owned` — belongs to a team (directly or transitively via Product /
    Outcome / Opportunity / Solution).
  - `platform` — global (users, authorities, auth/session plumbing).
- **Protection** — what enforces authorisation:
  - `ROLE_ADMIN` — locked at the controller with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.
  - `TeamAccessService` — every read/write in the service goes through
    `TeamAccessService` (non-members get 403 on writes and 404 on reads,
    without existence disclosure — NFR-002).
  - `authenticated` — any signed-in user; behaviour is intentionally public to
    all authenticated users.
  - `permitAll` / role from `SecurityConfiguration` — filter chain rule.

## Team-owned entities

| Controller                | Base path                | Verbs                                                  | Owner scope                          | Protection                                                                                                                                                        |
| ------------------------- | ------------------------ | ------------------------------------------------------ | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TeamResource`            | `/api/teams`             | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned                           | `ROLE_ADMIN`                                                                                                                                                      |
| `TeamMemberResource`      | `/api/team-members`      | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned                           | `ROLE_ADMIN`                                                                                                                                                      |
| `ProductResource`         | `/api/products`          | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned                           | `TeamAccessService` (via `ProductServiceImpl`; list is filtered to the caller's teams, single read/writes go through `requireReadProduct` / `requireEditProduct`) |
| `OutcomeResource`         | `/api/outcomes`          | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Product)             | POST, PUT, PATCH: `TeamAccessService` (via `OutcomeServiceImpl`, `requireEditOutcome`); GET, GET/{id}, DELETE: `ROLE_ADMIN`                                       |
| `OpportunityResource`     | `/api/opportunities`     | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Outcome)             | POST, PUT, PATCH: `TeamAccessService` (via `OpportunityServiceImpl`, `requireEditOpportunity`); GET, GET/{id}, DELETE: `ROLE_ADMIN`                               |
| `OpportunityLinkResource` | `/api/opportunity-links` | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Opportunity)         | `ROLE_ADMIN`                                                                                                                                                      |
| `SolutionResource`        | `/api/solutions`         | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Opportunity)         | POST, PUT, PATCH: `TeamAccessService` (via `SolutionServiceImpl`, `requireEditSolution`); GET, GET/{id}, DELETE: `ROLE_ADMIN`                                     |
| `SolutionLinkResource`    | `/api/solution-links`    | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Solution)            | `ROLE_ADMIN`                                                                                                                                                      |
| `AssumptionResource`      | `/api/assumptions`       | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Solution)            | `ROLE_ADMIN`                                                                                                                                                      |
| `ExperimentResource`      | `/api/experiments`       | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Assumption)          | `ROLE_ADMIN`                                                                                                                                                      |
| `InterviewResource`       | `/api/interviews`        | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Product)             | `ROLE_ADMIN`                                                                                                                                                      |
| `CommentResource`         | `/api/comments`          | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via node)                | `ROLE_ADMIN`                                                                                                                                                      |
| `TagResource`             | `/api/tags`              | GET, GET/{id}, POST, PUT/{id}, PATCH/{id}, DELETE/{id} | team-owned (via Solution/Assumption) | `ROLE_ADMIN`                                                                                                                                                      |

## Team-scoped façades (built on `TeamAccessService`)

Introduced by TEAMS-002 / TEAMS-003. These remain open to authenticated users;
authorisation is enforced per call inside the service.

| Controller               | Path                           | Notes                                                                                                                                    |
| ------------------------ | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `TeamManagementResource` | `/api/team-management/**`      | Create team / list-my-teams / member add-remove-role / user-search. All flows delegate to `TeamManagementService` → `TeamAccessService`. |
| `TeamProductResource`    | `/api/teams/{teamId}/products` | Lists a team's products (including archived); non-members get 403 via `teamAccessService.requireReadTeam`.                               |

## Platform endpoints (not team-owned)

| Controller           | Path(s)                                     | Verbs             | Protection                                                                                                                                                                                                                                                                             |
| -------------------- | ------------------------------------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AccountResource`    | `/api/account`, `/api/authenticate`         | GET               | `authenticated` (SecurityConfiguration `authz` rules; `/api/authenticate` and `/api/auth-info` are `permitAll`)                                                                                                                                                                        |
| `AuthInfoResource`   | `/api/auth-info`                            | GET               | `permitAll`                                                                                                                                                                                                                                                                            |
| `AuthorityResource`  | `/api/authorities`, `/api/authorities/{id}` | GET, POST, DELETE | Restricted by SecurityConfiguration — falls under `/api/admin/**`? No, `/api/authorities/**` matches `/api/**` (`authenticated`). The controller itself does not add `@PreAuthorize`; the authorities catalogue is inherently a platform concern and is treated as authenticated-only. |
| `PublicUserResource` | `/api/users`                                | GET               | `authenticated` — exposes the user directory the team-member picker needs.                                                                                                                                                                                                             |
| `LogoutResource`     | `/api/logout`                               | POST              | `authenticated`                                                                                                                                                                                                                                                                        |

## Front-end alignment

The generated JHipster CRUD screens for team-owned entities (Team, TeamMember,
Product, Outcome, Opportunity, OpportunityLink, Solution, SolutionLink,
Assumption, Experiment, Interview, Comment, Tag) are gated on `ROLE_ADMIN` in
the router (`src/main/webapp/app/router/entities.ts`) and hidden from the nav
bar (`src/main/webapp/app/core/jhi-navbar/jhi-navbar.vue`) so a non-admin user
is never led to a URL that now returns 403.

Team members still reach their teams and products through the team-scoped
façades (TEAMS-002 / TEAMS-003 endpoints and the future Epic 2 tree editor).
