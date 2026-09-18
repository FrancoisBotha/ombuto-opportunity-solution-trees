# Third-Party Notices

Ombuto Opportunity Solution Tree is licensed under the [Apache License 2.0](LICENSE). It is built
on the work of many other projects. This file lists the third-party software that is distributed
with the application — the runtime libraries of the production server build and the production
dependency tree of the web client — together with the licence each is used under.

Build-time and test-only tooling (Vite, Vitest, Playwright, ESLint, Prettier, JUnit, Testcontainers,
the JHipster generator itself, and so on) is not distributed with the application and is not listed.

> Everything below the "Licence summary" heading is **generated**. Do not edit it by hand —
> change `scripts/third-party-notices.header.md` for the prose, then run
> `node scripts/generate-third-party-notices.cjs` whenever dependencies change.

## Acknowledgements

- **[JHipster](https://www.jhipster.tech/)** (Apache-2.0) generated the application scaffold, the
  entity CRUD layers and their tests from `ombuto.jdl`. The generated code is part of this project
  and carries this project's licence.
- **[Spring Boot](https://spring.io/projects/spring-boot)** and the wider Spring portfolio
  (Apache-2.0) provide the server framework, security, data access and WebSocket messaging.
- **[Hibernate ORM](https://hibernate.org/orm/)**, **[MapStruct](https://mapstruct.org/)**,
  **[Ehcache](https://www.ehcache.org/)** and **[Jackson](https://github.com/FasterXML/jackson)**
  (Apache-2.0) handle persistence, mapping, caching and JSON.
- **[Vue](https://vuejs.org/)**, **[Pinia](https://pinia.vuejs.org/)**,
  **[Vue Router](https://router.vuejs.org/)** and
  **[BootstrapVueNext](https://bootstrap-vue-next.github.io/bootstrap-vue-next/)** (MIT), with
  **[Bootstrap](https://getbootstrap.com/)** and **[Bootswatch](https://bootswatch.com/)** (MIT),
  make up the user interface.
- **[Keycloak](https://www.keycloak.org/)** (Apache-2.0) is the identity provider used in
  development and recommended for deployment. It runs as a separate service and is not bundled.
- **[PostgreSQL](https://www.postgresql.org/)** (PostgreSQL Licence) is the production database. It
  runs as a separate service; only its JDBC driver (BSD-2-Clause) is bundled.

The Opportunity Solution Tree is a product discovery technique created by **Teresa Torres** and
described in her book _Continuous Discovery Habits_ and at
[producttalk.org](https://www.producttalk.org/). This project is an independent implementation of
the technique and is not affiliated with, or endorsed by, Teresa Torres or Product Talk.

## Licences that need particular attention

Most dependencies are under the Apache-2.0, MIT or BSD licences, which ask only that their
copyright and licence notices are preserved. The following are different.

### Font Awesome Free — icons under CC BY 4.0

The icons from `@fortawesome/free-solid-svg-icons` are licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), which requires attribution; the
accompanying code is MIT. Attribution: _Icons by [Font Awesome](https://fontawesome.com/) — Font
Awesome Free, © Fonticons, Inc., [licence](https://fontawesome.com/license/free)._ The icon files
also carry this attribution in embedded comments, which must not be stripped.

### Liquibase — source-available, not open source

`org.liquibase:liquibase-core` 5.x is published under the
[Functional Source License 1.1 (Apache 2.0 future licence)](https://fsl.software/). This is **not**
an OSI-approved open-source licence. It permits use, modification and redistribution for any
purpose **except a "Competing Use"** — offering a product or service that competes with Liquibase
itself. Each release converts to Apache-2.0 two years after publication.

Using Liquibase to manage this application's database schema is a permitted use. Anyone
redistributing or building on this project should confirm their own use also is. Projects that
need a fully open-source dependency tree can pin Liquibase 4.x (Apache-2.0).

### Weak-copyleft libraries (EPL, LGPL, MPL, CDDL)

Logback, AspectJ, the Jakarta EE APIs and a few others are under file-level ("weak") copyleft
licences, in most cases dual-licensed. They are used here **unmodified, as libraries**, which these
licences allow without affecting the licence of this project's own code. If you modify the source
of one of these libraries and distribute the result, you must publish those modifications under
that library's licence.

**H2 Database** (EPL-1.0 OR MPL-2.0) is the database for local development and automated tests;
production uses PostgreSQL. H2 is nevertheless present in the production build, because the
`spring-boot-h2console` starter that JHipster includes depends on it, so it is listed below.
