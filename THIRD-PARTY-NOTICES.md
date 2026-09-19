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

### Inter — font under the SIL Open Font License 1.1

The Inter typeface, bundled through `@fontsource/inter`, is licensed under the
[SIL Open Font License, Version 1.1](https://openfontlicense.org/) (OFL-1.1); the Fontsource
packaging is MIT. Copyright: _© 2016 The Inter Project Authors
([github.com/rsms/inter](https://github.com/rsms/inter))._ The OFL allows the font to be used,
embedded, bundled and redistributed with any software, provided that the font files are not sold
on their own, that this copyright and licence notice accompany them, and that modified versions are
not published under the reserved font name. The font files are shipped unmodified. The full licence
text is in `node_modules/@fontsource/inter/LICENSE` and at the link above.

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

## Licence summary

**Frontend** — 148 packages in the web client's production dependency tree:

- MIT: 124
- ISC: 11
- Apache-2.0: 6
- BSD-3-Clause: 3
- OFL-1.1: 1
- (CC-BY-4.0 AND MIT): 1
- BSD-2-Clause: 1
- 0BSD: 1

**Backend** — 173 runtime libraries in the production build:

- Apache-2.0: 150
- BSD-3-Clause (EDL-1.0): 3
- EPL-2.0 OR GPL-2.0-with-classpath-exception: 3
- MIT: 3
- EPL-2.0 OR LGPL-2.1: 2
- BSD-3-Clause (EDL-1.0) OR EPL-2.0 OR GPL-2.0-with-classpath-exception: 2
- BSD-3-Clause: 2
- EPL-1.0 OR MPL-2.0: 1
- BSD-3-Clause (EDL-1.0) OR EPL-2.0: 1
- CDDL-1.1 OR GPL-2.0-with-classpath-exception: 1
- EPL-2.0: 1
- BSD-2-Clause OR CC0-1.0: 1
- CC0-1.0: 1
- FSL-1.1-ALv2: 1
- BSD-2-Clause: 1

## Frontend packages

Packages marked **direct** are declared in `package.json`; the rest are pulled in by them.

| Package                                                                                                    | Version | Licence             |        |
| ---------------------------------------------------------------------------------------------------------- | ------- | ------------------- | ------ |
| [@babel/generator](https://babel.dev/docs/en/next/babel-generator)                                         | 7.29.8  | MIT                 |        |
| [@babel/helper-string-parser](https://babel.dev/docs/en/next/babel-helper-string-parser)                   | 7.29.7  | MIT                 |        |
| @babel/helper-validator-identifier                                                                         | 7.29.7  | MIT                 |        |
| [@babel/parser](https://babel.dev/docs/en/next/babel-parser)                                               | 7.29.8  | MIT                 |        |
| [@babel/types](https://babel.dev/docs/en/next/babel-types)                                                 | 7.29.8  | MIT                 |        |
| [@fontsource/inter](https://fontsource.org/fonts/inter)                                                    | 5.3.0   | OFL-1.1             | direct |
| [@fortawesome/fontawesome-common-types](https://fontawesome.com)                                           | 7.2.0   | MIT                 |        |
| [@fortawesome/fontawesome-svg-core](https://fontawesome.com)                                               | 7.2.0   | MIT                 | direct |
| [@fortawesome/free-solid-svg-icons](https://fontawesome.com)                                               | 7.2.0   | (CC-BY-4.0 AND MIT) | direct |
| [@fortawesome/vue-fontawesome](https://github.com/FortAwesome/vue-fontawesome)                             | 3.1.3   | MIT                 | direct |
| [@jridgewell/gen-mapping](https://github.com/jridgewell/sourcemaps/tree/main/packages/gen-mapping)         | 0.3.13  | MIT                 |        |
| [@jridgewell/remapping](https://github.com/jridgewell/sourcemaps/tree/main/packages/remapping)             | 2.3.5   | MIT                 |        |
| @jridgewell/resolve-uri                                                                                    | 3.1.2   | MIT                 |        |
| [@jridgewell/sourcemap-codec](https://github.com/jridgewell/sourcemaps/tree/main/packages/sourcemap-codec) | 1.6.0   | MIT                 |        |
| [@jridgewell/trace-mapping](https://github.com/jridgewell/sourcemaps/tree/main/packages/trace-mapping)     | 0.3.31  | MIT                 |        |
| @phosphor-icons/vue                                                                                        | 2.2.1   | MIT                 | direct |
| @popperjs/core                                                                                             | 2.11.8  | MIT                 |        |
| [@stomp/rx-stomp](https://github.com/stomp-js/rx-stomp#readme)                                             | 2.3.0   | Apache-2.0          | direct |
| [@stomp/stompjs](https://github.com/stomp-js/stompjs#readme)                                               | 7.3.0   | Apache-2.0          |        |
| [@types/web-bluetooth](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/web-bluetooth) | 0.0.21  | MIT                 |        |
| [@types/web-bluetooth](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/web-bluetooth) | 0.0.20  | MIT                 |        |
| [@vue-flow/core](https://vueflow.dev)                                                                      | 1.48.2  | MIT                 | direct |
| [@vue-macros/common](https://vue-macros.dev)                                                               | 3.1.4   | MIT                 |        |
| [@vue/compiler-core](https://github.com/vuejs/core/tree/main/packages/compiler-core#readme)                | 3.5.30  | MIT                 |        |
| [@vue/compiler-dom](https://github.com/vuejs/core/tree/main/packages/compiler-dom#readme)                  | 3.5.30  | MIT                 |        |
| [@vue/compiler-sfc](https://github.com/vuejs/core/tree/main/packages/compiler-sfc#readme)                  | 3.5.30  | MIT                 |        |
| [@vue/compiler-ssr](https://github.com/vuejs/core/tree/main/packages/compiler-ssr#readme)                  | 3.5.30  | MIT                 |        |
| @vue/devtools-api                                                                                          | 7.7.10  | MIT                 |        |
| @vue/devtools-api                                                                                          | 8.2.1   | MIT                 |        |
| @vue/devtools-kit                                                                                          | 7.7.10  | MIT                 |        |
| @vue/devtools-kit                                                                                          | 8.2.1   | MIT                 |        |
| @vue/devtools-shared                                                                                       | 7.7.10  | MIT                 |        |
| @vue/devtools-shared                                                                                       | 8.2.1   | MIT                 |        |
| [@vue/reactivity](https://github.com/vuejs/core/tree/main/packages/reactivity#readme)                      | 3.5.30  | MIT                 |        |
| [@vue/runtime-core](https://github.com/vuejs/core/tree/main/packages/runtime-core#readme)                  | 3.5.30  | MIT                 |        |
| [@vue/runtime-dom](https://github.com/vuejs/core/tree/main/packages/runtime-dom#readme)                    | 3.5.30  | MIT                 |        |
| [@vue/server-renderer](https://github.com/vuejs/core/tree/main/packages/server-renderer#readme)            | 3.5.30  | MIT                 |        |
| [@vue/shared](https://github.com/vuejs/core/tree/main/packages/shared#readme)                              | 3.5.30  | MIT                 |        |
| @vuelidate/core                                                                                            | 2.0.3   | MIT                 | direct |
| @vuelidate/validators                                                                                      | 2.0.4   | MIT                 | direct |
| [@vueuse/core](https://github.com/vueuse/vueuse#readme)                                                    | 10.11.1 | MIT                 | direct |
| [@vueuse/core](https://github.com/vueuse/vueuse#readme)                                                    | 14.2.1  | MIT                 | direct |
| [@vueuse/metadata](https://github.com/vueuse/vueuse/tree/main/packages/metadata#readme)                    | 10.11.1 | MIT                 |        |
| [@vueuse/metadata](https://github.com/vueuse/vueuse/tree/main/packages/metadata#readme)                    | 14.2.1  | MIT                 |        |
| [@vueuse/shared](https://github.com/vueuse/vueuse/tree/main/packages/shared#readme)                        | 10.11.1 | MIT                 |        |
| [@vueuse/shared](https://github.com/vueuse/vueuse/tree/main/packages/shared#readme)                        | 14.2.1  | MIT                 |        |
| [acorn](https://github.com/acornjs/acorn)                                                                  | 8.18.0  | MIT                 |        |
| [ast-kit](https://github.com/sxzz/ast-kit#readme)                                                          | 2.2.0   | MIT                 |        |
| [ast-walker-scope](https://github.com/sxzz/ast-walker-scope#readme)                                        | 0.8.3   | MIT                 |        |
| [asynckit](https://github.com/alexindigo/asynckit#readme)                                                  | 0.4.0   | MIT                 |        |
| [axios](https://axios-http.com)                                                                            | 1.13.6  | MIT                 | direct |
| [birpc](https://github.com/antfu-collective/birpc#readme)                                                  | 2.9.0   | MIT                 |        |
| [bootstrap](https://getbootstrap.com/)                                                                     | 5.3.7   | MIT                 | direct |
| [bootstrap-vue-next](https://github.com/bootstrap-vue-next/bootstrap-vue-next)                             | 0.43.8  | MIT                 | direct |
| [bootswatch](https://bootswatch.com)                                                                       | 5.3.8   | MIT                 | direct |
| [call-bind-apply-helpers](https://github.com/ljharb/call-bind-apply-helpers#readme)                        | 1.0.2   | MIT                 |        |
| [chokidar](https://github.com/paulmillr/chokidar)                                                          | 5.0.0   | MIT                 |        |
| [combined-stream](https://github.com/felixge/node-combined-stream)                                         | 1.0.8   | MIT                 |        |
| confbox                                                                                                    | 0.3.1   | MIT                 |        |
| confbox                                                                                                    | 0.1.8   | MIT                 |        |
| [copy-anything](https://github.com/mesqueeb/copy-anything#readme)                                          | 4.1.0   | MIT                 |        |
| csstype                                                                                                    | 3.2.3   | MIT                 |        |
| [d3-color](https://d3js.org/d3-color/)                                                                     | 3.1.0   | ISC                 |        |
| [d3-dispatch](https://d3js.org/d3-dispatch/)                                                               | 3.0.1   | ISC                 |        |
| [d3-drag](https://d3js.org/d3-drag/)                                                                       | 3.0.0   | ISC                 |        |
| [d3-ease](https://d3js.org/d3-ease/)                                                                       | 3.0.1   | BSD-3-Clause        |        |
| [d3-interpolate](https://d3js.org/d3-interpolate/)                                                         | 3.0.1   | ISC                 |        |
| [d3-selection](https://d3js.org/d3-selection/)                                                             | 3.0.0   | ISC                 |        |
| [d3-timer](https://d3js.org/d3-timer/)                                                                     | 3.0.1   | ISC                 |        |
| [d3-transition](https://d3js.org/d3-transition/)                                                           | 3.0.1   | ISC                 |        |
| [d3-zoom](https://d3js.org/d3-zoom/)                                                                       | 3.0.0   | ISC                 |        |
| [dayjs](https://day.js.org)                                                                                | 1.11.19 | MIT                 | direct |
| debug                                                                                                      | 3.2.7   | MIT                 |        |
| [delayed-stream](https://github.com/felixge/node-delayed-stream)                                           | 1.0.0   | MIT                 |        |
| [dunder-proto](https://github.com/es-shims/dunder-proto#readme)                                            | 1.0.1   | MIT                 |        |
| entities                                                                                                   | 7.0.1   | BSD-2-Clause        |        |
| [es-define-property](https://github.com/ljharb/es-define-property#readme)                                  | 1.0.1   | MIT                 |        |
| [es-errors](https://github.com/ljharb/es-errors#readme)                                                    | 1.3.0   | MIT                 |        |
| [es-object-atoms](https://github.com/ljharb/es-object-atoms#readme)                                        | 1.1.2   | MIT                 |        |
| [es-set-tostringtag](https://github.com/es-shims/es-set-tostringtag#readme)                                | 2.1.0   | MIT                 |        |
| estree-walker                                                                                              | 2.0.2   | MIT                 |        |
| [eventsource](http://github.com/EventSource/eventsource)                                                   | 2.0.2   | MIT                 |        |
| exsolve                                                                                                    | 1.1.1   | MIT                 |        |
| [faye-websocket](https://github.com/faye/faye-websocket-node)                                              | 0.11.4  | Apache-2.0          |        |
| [fdir](https://github.com/thecodrr/fdir#readme)                                                            | 6.5.0   | MIT                 |        |
| [follow-redirects](https://github.com/follow-redirects/follow-redirects)                                   | 1.16.0  | MIT                 |        |
| form-data                                                                                                  | 4.0.6   | MIT                 |        |
| [function-bind](https://github.com/Raynos/function-bind)                                                   | 1.1.2   | MIT                 |        |
| [get-intrinsic](https://github.com/ljharb/get-intrinsic#readme)                                            | 1.3.0   | MIT                 |        |
| [get-proto](https://github.com/ljharb/get-proto#readme)                                                    | 1.0.1   | MIT                 |        |
| [gopd](https://github.com/ljharb/gopd#readme)                                                              | 1.2.0   | MIT                 |        |
| [has-symbols](https://github.com/ljharb/has-symbols#readme)                                                | 1.1.0   | MIT                 |        |
| [has-tostringtag](https://github.com/inspect-js/has-tostringtag#readme)                                    | 1.0.2   | MIT                 |        |
| [hasown](https://github.com/inspect-js/hasOwn#readme)                                                      | 2.0.4   | MIT                 |        |
| hookable                                                                                                   | 5.5.3   | MIT                 |        |
| http-parser-js                                                                                             | 0.5.10  | MIT                 |        |
| inherits                                                                                                   | 2.0.4   | ISC                 |        |
| js-cookie                                                                                                  | 3.0.5   | MIT                 | direct |
| [jsesc](https://mths.be/jsesc)                                                                             | 3.1.0   | MIT                 |        |
| [json5](http://json5.org/)                                                                                 | 2.2.3   | MIT                 |        |
| [local-pkg](https://github.com/antfu-collective/local-pkg#readme)                                          | 1.2.1   | MIT                 |        |
| magic-string                                                                                               | 0.30.21 | MIT                 |        |
| [magic-string-ast](https://github.com/sxzz/magic-string-ast#readme)                                        | 1.0.3   | MIT                 |        |
| [math-intrinsics](https://github.com/es-shims/math-intrinsics#readme)                                      | 1.1.0   | MIT                 |        |
| mime-db                                                                                                    | 1.52.0  | MIT                 |        |
| mime-types                                                                                                 | 2.1.35  | MIT                 |        |
| [mitt](https://github.com/developit/mitt)                                                                  | 3.0.1   | MIT                 |        |
| mlly                                                                                                       | 1.8.2   | MIT                 |        |
| ms                                                                                                         | 2.1.3   | MIT                 |        |
| muggle-string                                                                                              | 0.4.1   | MIT                 |        |
| nanoid                                                                                                     | 3.3.19  | MIT                 |        |
| pathe                                                                                                      | 2.0.3   | MIT                 |        |
| perfect-debounce                                                                                           | 1.0.0   | MIT                 |        |
| perfect-debounce                                                                                           | 2.1.0   | MIT                 |        |
| picocolors                                                                                                 | 1.1.1   | ISC                 |        |
| [picomatch](https://github.com/micromatch/picomatch)                                                       | 4.0.7   | MIT                 |        |
| [pinia](https://pinia.vuejs.org)                                                                           | 3.0.4   | MIT                 | direct |
| pkg-types                                                                                                  | 1.3.1   | MIT                 |        |
| pkg-types                                                                                                  | 2.3.3   | MIT                 |        |
| [postcss](https://postcss.org/)                                                                            | 8.5.28  | MIT                 |        |
| [proxy-from-env](https://github.com/Rob--W/proxy-from-env#readme)                                          | 1.1.0   | MIT                 |        |
| [quansync](https://github.com/quansync-dev/quansync#readme)                                                | 0.2.11  | MIT                 |        |
| [querystringify](https://github.com/unshiftio/querystringify)                                              | 2.2.0   | MIT                 |        |
| [readdirp](https://github.com/paulmillr/readdirp)                                                          | 5.1.1   | MIT                 |        |
| [requires-port](https://github.com/unshiftio/requires-port)                                                | 1.0.0   | MIT                 |        |
| [rfdc](https://github.com/davidmarkclements/rfdc#readme)                                                   | 1.4.1   | MIT                 |        |
| [rxjs](https://rxjs.dev)                                                                                   | 7.8.2   | Apache-2.0          | direct |
| [safe-buffer](https://github.com/feross/safe-buffer)                                                       | 5.2.1   | MIT                 |        |
| scule                                                                                                      | 1.3.0   | MIT                 |        |
| [sockjs-client](http://sockjs.org)                                                                         | 1.6.1   | MIT                 | direct |
| [source-map-js](https://github.com/7rulnik/source-map-js)                                                  | 1.2.1   | BSD-3-Clause        |        |
| [speakingurl](http://pid.github.io/speakingurl/)                                                           | 14.0.1  | BSD-3-Clause        |        |
| superjson                                                                                                  | 2.2.6   | MIT                 |        |
| [tinyglobby](https://superchupu.dev/tinyglobby)                                                            | 0.2.15  | MIT                 |        |
| [tslib](https://www.typescriptlang.org/)                                                                   | 2.8.1   | 0BSD                |        |
| ufo                                                                                                        | 1.6.4   | MIT                 |        |
| [unplugin](https://unplugin.unjs.io)                                                                       | 3.4.0   | MIT                 |        |
| [unplugin-utils](https://github.com/sxzz/unplugin-utils#readme)                                            | 0.3.2   | MIT                 |        |
| url-parse                                                                                                  | 1.5.10  | MIT                 |        |
| uuid                                                                                                       | 11.1.1  | MIT                 |        |
| [vue](https://github.com/vuejs/core/tree/main/packages/vue#readme)                                         | 3.5.30  | MIT                 | direct |
| vue-demi                                                                                                   | 0.14.10 | MIT                 |        |
| vue-demi                                                                                                   | 0.13.11 | MIT                 |        |
| [vue-router](https://router.vuejs.org)                                                                     | 5.0.3   | MIT                 | direct |
| [webpack-virtual-modules](https://github.com/sysgears/webpack-virtual-modules#readme)                      | 0.6.2   | MIT                 |        |
| [websocket-driver](https://github.com/faye/websocket-driver-node)                                          | 0.7.5   | Apache-2.0          |        |
| [websocket-extensions](http://github.com/faye/websocket-extensions-node)                                   | 0.1.4   | Apache-2.0          |        |
| [yaml](https://eemeli.org/yaml/)                                                                           | 2.8.2   | ISC                 |        |

## Backend libraries

Where a library is offered under several licences, this project uses it under the first one listed.

| Library                                                                                                                                                    | Coordinates                                                            | Version       | Licence                                                               |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------- | --------------------------------------------------------------------- |
| [Logback Classic Module](http://logback.qos.ch/logback-classic)                                                                                            | `ch.qos.logback:logback-classic`                                       | 1.5.32        | EPL-2.0 OR LGPL-2.1                                                   |
| [Logback Core Module](http://logback.qos.ch/logback-core)                                                                                                  | `ch.qos.logback:logback-core`                                          | 1.5.32        | EPL-2.0 OR LGPL-2.1                                                   |
| [High Performance Primitive Collections](https://github.com/carrotsearch/hppc)                                                                             | `com.carrotsearch:hppc`                                                | 0.9.1         | Apache-2.0                                                            |
| [ClassMate](https://github.com/FasterXML/java-classmate)                                                                                                   | `com.fasterxml:classmate`                                              | 1.7.3         | Apache-2.0                                                            |
| [Jackson-annotations](https://github.com/FasterXML/jackson)                                                                                                | `com.fasterxml.jackson.core:jackson-annotations`                       | 2.20          | Apache-2.0                                                            |
| [Jackson-core](https://github.com/FasterXML/jackson-core)                                                                                                  | `com.fasterxml.jackson.core:jackson-core`                              | 2.20.2        | Apache-2.0                                                            |
| [jackson-databind](https://github.com/FasterXML/jackson)                                                                                                   | `com.fasterxml.jackson.core:jackson-databind`                          | 2.20.2        | Apache-2.0                                                            |
| [Jackson-dataformat-YAML](https://github.com/FasterXML/jackson-dataformats-text)                                                                           | `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`             | 2.20.2        | Apache-2.0                                                            |
| [Jackson datatype: HPPC](https://github.com/FasterXML/jackson-datatypes-collections)                                                                       | `com.fasterxml.jackson.datatype:jackson-datatype-hppc`                 | 2.20.2        | Apache-2.0                                                            |
| [Jackson datatype: jdk8](https://github.com/FasterXML/jackson-modules-java8/jackson-datatype-jdk8)                                                         | `com.fasterxml.jackson.datatype:jackson-datatype-jdk8`                 | 2.20.2        | Apache-2.0                                                            |
| [Jackson datatype: JSR310](https://github.com/FasterXML/jackson-modules-java8/jackson-datatype-jsr310)                                                     | `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`               | 2.20.2        | Apache-2.0                                                            |
| [Jackson-module-parameter-names](https://github.com/FasterXML/jackson-modules-java8/jackson-module-parameter-names)                                        | `com.fasterxml.jackson.module:jackson-module-parameter-names`          | 2.20.2        | Apache-2.0                                                            |
| [Caffeine cache](https://github.com/ben-manes/caffeine)                                                                                                    | `com.github.ben-manes.caffeine:caffeine`                               | 3.2.3         | Apache-2.0                                                            |
| [JCIP Annotations under Apache License](http://stephenc.github.com/jcip-annotations)                                                                       | `com.github.stephenc.jcip:jcip-annotations`                            | 1.0-1         | Apache-2.0                                                            |
| [error-prone annotations](https://errorprone.info/error_prone_annotations)                                                                                 | `com.google.errorprone:error_prone_annotations`                        | 2.43.0        | Apache-2.0                                                            |
| [H2 Database Engine](https://h2database.com)                                                                                                               | `com.h2database:h2`                                                    | 2.4.240       | EPL-1.0 OR MPL-2.0                                                    |
| [Nimbus Content Type](https://bitbucket.org/connect2id/nimbus-content-type)                                                                                | `com.nimbusds:content-type`                                            | 2.3           | Apache-2.0                                                            |
| [Nimbus LangTag](https://bitbucket.org/connect2id/nimbus-language-tags)                                                                                    | `com.nimbusds:lang-tag`                                                | 1.7           | Apache-2.0                                                            |
| [Nimbus JOSE+JWT](https://bitbucket.org/connect2id/nimbus-jose-jwt)                                                                                        | `com.nimbusds:nimbus-jose-jwt`                                         | 10.4          | Apache-2.0                                                            |
| [OAuth 2.0 SDK with OpenID Connect extensions](https://bitbucket.org/connect2id/oauth-2.0-sdk-with-openid-connect-extensions)                              | `com.nimbusds:oauth2-oidc-sdk`                                         | 11.26.1       | Apache-2.0                                                            |
| [opencsv](http://opencsv.sf.net)                                                                                                                           | `com.opencsv:opencsv`                                                  | 5.12.0        | Apache-2.0                                                            |
| [HikariCP](https://github.com/brettwooldridge/HikariCP)                                                                                                    | `com.zaxxer:HikariCP`                                                  | 7.0.2         | Apache-2.0                                                            |
| [Apache Commons IO](https://commons.apache.org/proper/commons-io/)                                                                                         | `commons-io:commons-io`                                                | 2.20.0        | Apache-2.0                                                            |
| [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/)                                                                               | `commons-logging:commons-logging`                                      | 1.3.5         | Apache-2.0                                                            |
| [micrometer-commons](https://github.com/micrometer-metrics/micrometer)                                                                                     | `io.micrometer:micrometer-commons`                                     | 1.16.3        | Apache-2.0                                                            |
| [micrometer-core](https://github.com/micrometer-metrics/micrometer)                                                                                        | `io.micrometer:micrometer-core`                                        | 1.16.3        | Apache-2.0                                                            |
| [micrometer-jakarta9](https://github.com/micrometer-metrics/micrometer)                                                                                    | `io.micrometer:micrometer-jakarta9`                                    | 1.16.3        | Apache-2.0                                                            |
| [micrometer-observation](https://github.com/micrometer-metrics/micrometer)                                                                                 | `io.micrometer:micrometer-observation`                                 | 1.16.3        | Apache-2.0                                                            |
| [micrometer-registry-prometheus-simpleclient](https://github.com/micrometer-metrics/micrometer)                                                            | `io.micrometer:micrometer-registry-prometheus-simpleclient`            | 1.16.3        | Apache-2.0                                                            |
| [Prometheus Java Simpleclient](http://github.com/prometheus/client_java/simpleclient)                                                                      | `io.prometheus:simpleclient`                                           | 0.16.0        | Apache-2.0                                                            |
| [Prometheus Java Simpleclient Common](http://github.com/prometheus/client_java/simpleclient_common)                                                        | `io.prometheus:simpleclient_common`                                    | 0.16.0        | Apache-2.0                                                            |
| [Prometheus Java Span Context Supplier - Common](http://github.com/prometheus/client_java/simpleclient_tracer/simpleclient_tracer_common)                  | `io.prometheus:simpleclient_tracer_common`                             | 0.16.0        | Apache-2.0                                                            |
| [Prometheus Java Span Context Supplier - OpenTelemetry](http://github.com/prometheus/client_java/simpleclient_tracer/simpleclient_tracer_otel)             | `io.prometheus:simpleclient_tracer_otel`                               | 0.16.0        | Apache-2.0                                                            |
| [Prometheus Java Span Context Supplier - OpenTelemetry Agent](http://github.com/prometheus/client_java/simpleclient_tracer/simpleclient_tracer_otel_agent) | `io.prometheus:simpleclient_tracer_otel_agent`                         | 0.16.0        | Apache-2.0                                                            |
| [swagger-annotations-jakarta](https://github.com/swagger-api/swagger-core/modules/swagger-annotations-jakarta)                                             | `io.swagger.core.v3:swagger-annotations-jakarta`                       | 2.2.43        | Apache-2.0                                                            |
| [swagger-core-jakarta](https://github.com/swagger-api/swagger-core/modules/swagger-core-jakarta)                                                           | `io.swagger.core.v3:swagger-core-jakarta`                              | 2.2.43        | Apache-2.0                                                            |
| [swagger-models-jakarta](https://github.com/swagger-api/swagger-core/modules/swagger-models-jakarta)                                                       | `io.swagger.core.v3:swagger-models-jakarta`                            | 2.2.43        | Apache-2.0                                                            |
| [Jakarta Activation API](https://github.com/jakartaee/jaf-api)                                                                                             | `jakarta.activation:jakarta.activation-api`                            | 2.1.4         | BSD-3-Clause (EDL-1.0)                                                |
| [Jakarta Annotations API](https://projects.eclipse.org/projects/ee4j.ca)                                                                                   | `jakarta.annotation:jakarta.annotation-api`                            | 3.0.0         | EPL-2.0 OR GPL-2.0-with-classpath-exception                           |
| [Jakarta Dependency Injection](https://github.com/eclipse-ee4j/injection-api)                                                                              | `jakarta.inject:jakarta.inject-api`                                    | 2.0.1         | Apache-2.0                                                            |
| [Jakarta Mail API](https://projects.eclipse.org/projects/ee4j/jakarta.mail-api)                                                                            | `jakarta.mail:jakarta.mail-api`                                        | 2.1.5         | BSD-3-Clause (EDL-1.0) OR EPL-2.0 OR GPL-2.0-with-classpath-exception |
| [Jakarta Persistence API](https://github.com/jakartaee/persistence)                                                                                        | `jakarta.persistence:jakarta.persistence-api`                          | 3.2.0         | BSD-3-Clause (EDL-1.0) OR EPL-2.0                                     |
| [Jakarta Servlet](https://projects.eclipse.org/projects/ee4j.servlet)                                                                                      | `jakarta.servlet:jakarta.servlet-api`                                  | 6.1.0         | EPL-2.0 OR GPL-2.0-with-classpath-exception                           |
| [jakarta.transaction API](https://projects.eclipse.org/projects/ee4j.jta)                                                                                  | `jakarta.transaction:jakarta.transaction-api`                          | 2.0.1         | EPL-2.0 OR GPL-2.0-with-classpath-exception                           |
| [Jakarta Validation API](https://beanvalidation.org)                                                                                                       | `jakarta.validation:jakarta.validation-api`                            | 3.1.1         | Apache-2.0                                                            |
| [Jakarta XML Binding API](https://github.com/jakartaee/jaxb-api/jakarta.xml.bind-api)                                                                      | `jakarta.xml.bind:jakarta.xml.bind-api`                                | 4.0.4         | BSD-3-Clause (EDL-1.0)                                                |
| [JSR107 API and SPI](https://github.com/jsr107/jsr107spec)                                                                                                 | `javax.cache:cache-api`                                                | 1.1.1         | Apache-2.0                                                            |
| [jaxb-api](https://github.com/javaee/jaxb-spec/jaxb-api)                                                                                                   | `javax.xml.bind:jaxb-api`                                              | 2.3.1         | CDDL-1.1 OR GPL-2.0-with-classpath-exception                          |
| [Byte Buddy (without dependencies)](https://bytebuddy.net/byte-buddy)                                                                                      | `net.bytebuddy:byte-buddy`                                             | 1.17.8        | Apache-2.0                                                            |
| [ASM based accessors helper used by json-smart](https://urielch.github.io/)                                                                                | `net.minidev:accessors-smart`                                          | 2.6.0         | Apache-2.0                                                            |
| [JSON Small and Fast Parser](https://urielch.github.io/)                                                                                                   | `net.minidev:json-smart`                                               | 2.6.0         | Apache-2.0                                                            |
| [ANTLR 4 Runtime](https://www.antlr.org/antlr4-runtime/)                                                                                                   | `org.antlr:antlr4-runtime`                                             | 4.13.2        | BSD-3-Clause                                                          |
| [Apache Commons Collections](https://commons.apache.org/proper/commons-collections/)                                                                       | `org.apache.commons:commons-collections4`                              | 4.5.0         | Apache-2.0                                                            |
| [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)                                                                                     | `org.apache.commons:commons-lang3`                                     | 3.19.0        | Apache-2.0                                                            |
| [Apache Commons Text](https://commons.apache.org/proper/commons-text)                                                                                      | `org.apache.commons:commons-text`                                      | 1.14.0        | Apache-2.0                                                            |
| [Apache Log4j API](https://logging.apache.org/log4j/2.x/)                                                                                                  | `org.apache.logging.log4j:log4j-api`                                   | 2.25.3        | Apache-2.0                                                            |
| [Log4j API to SLF4J Adapter](https://logging.apache.org/log4j/2.x/)                                                                                        | `org.apache.logging.log4j:log4j-to-slf4j`                              | 2.25.3        | Apache-2.0                                                            |
| [tomcat-embed-core](https://tomcat.apache.org/)                                                                                                            | `org.apache.tomcat.embed:tomcat-embed-core`                            | 11.0.18       | Apache-2.0                                                            |
| [tomcat-embed-el](https://tomcat.apache.org/)                                                                                                              | `org.apache.tomcat.embed:tomcat-embed-el`                              | 11.0.18       | Apache-2.0                                                            |
| [tomcat-embed-websocket](https://tomcat.apache.org/)                                                                                                       | `org.apache.tomcat.embed:tomcat-embed-websocket`                       | 11.0.18       | Apache-2.0                                                            |
| [AspectJ Weaver](https://www.eclipse.org/aspectj/)                                                                                                         | `org.aspectj:aspectjweaver`                                            | 1.9.25.1      | EPL-2.0                                                               |
| [attoparser](https://www.attoparser.org)                                                                                                                   | `org.attoparser:attoparser`                                            | 2.0.7.RELEASE | Apache-2.0                                                            |
| [Checker Qual](https://checkerframework.org/)                                                                                                              | `org.checkerframework:checker-qual`                                    | 3.52.0        | MIT                                                                   |
| [Angus Activation Registries](https://github.com/eclipse-ee4j/angus-activation/angus-activation)                                                           | `org.eclipse.angus:angus-activation`                                   | 2.0.3         | BSD-3-Clause (EDL-1.0)                                                |
| [Angus Mail Provider](http://eclipse-ee4j.github.io/angus-mail/angus-mail)                                                                                 | `org.eclipse.angus:angus-mail`                                         | 2.0.5         | BSD-3-Clause (EDL-1.0) OR EPL-2.0 OR GPL-2.0-with-classpath-exception |
| [Ehcache](http://ehcache.org)                                                                                                                              | `org.ehcache:ehcache`                                                  | 3.11.1        | Apache-2.0                                                            |
| [HdrHistogram](http://hdrhistogram.github.io/HdrHistogram/)                                                                                                | `org.hdrhistogram:HdrHistogram`                                        | 2.2.2         | BSD-2-Clause OR CC0-1.0                                               |
| [Hibernate Models](https://github.com/hibernate/hibernate-models)                                                                                          | `org.hibernate.models:hibernate-models`                                | 1.0.1         | Apache-2.0                                                            |
| [Hibernate ORM - hibernate-core](https://hibernate.org/orm)                                                                                                | `org.hibernate.orm:hibernate-core`                                     | 7.2.4.Final   | Apache-2.0                                                            |
| [Hibernate ORM - hibernate-jcache](https://hibernate.org/orm)                                                                                              | `org.hibernate.orm:hibernate-jcache`                                   | 7.2.4.Final   | Apache-2.0                                                            |
| [Hibernate Validator Engine](https://hibernate.org/validator)                                                                                              | `org.hibernate.validator:hibernate-validator`                          | 9.0.1.Final   | Apache-2.0                                                            |
| [JBoss Logging 3](https://www.jboss.org)                                                                                                                   | `org.jboss.logging:jboss-logging`                                      | 3.6.2.Final   | Apache-2.0                                                            |
| [JSpecify annotations](http://jspecify.org/)                                                                                                               | `org.jspecify:jspecify`                                                | 1.0.0         | Apache-2.0                                                            |
| [LatencyUtils](http://latencyutils.github.io/LatencyUtils/)                                                                                                | `org.latencyutils:LatencyUtils`                                        | 2.0.3         | CC0-1.0                                                               |
| [Liquibase](http://www.liquibase.com)                                                                                                                      | `org.liquibase:liquibase-core`                                         | 5.0.1         | FSL-1.1-ALv2                                                          |
| [MapStruct Core](https://mapstruct.org/mapstruct/)                                                                                                         | `org.mapstruct:mapstruct`                                              | 1.6.3         | Apache-2.0                                                            |
| [asm](http://asm.ow2.io/)                                                                                                                                  | `org.ow2.asm:asm`                                                      | 9.7.1         | BSD-3-Clause                                                          |
| [PostgreSQL JDBC Driver](https://jdbc.postgresql.org)                                                                                                      | `org.postgresql:postgresql`                                            | 42.7.10       | BSD-2-Clause                                                          |
| [JUL to SLF4J bridge](http://www.slf4j.org)                                                                                                                | `org.slf4j:jul-to-slf4j`                                               | 2.0.17        | MIT                                                                   |
| [SLF4J API Module](http://www.slf4j.org)                                                                                                                   | `org.slf4j:slf4j-api`                                                  | 2.0.17        | MIT                                                                   |
| [springdoc-openapi-starter-common](https://springdoc.org/springdoc-openapi-starter-common/)                                                                | `org.springdoc:springdoc-openapi-starter-common`                       | 3.0.2         | Apache-2.0                                                            |
| [springdoc-openapi-starter-webmvc-api](https://springdoc.org/springdoc-openapi-starter-webmvc-api/)                                                        | `org.springdoc:springdoc-openapi-starter-webmvc-api`                   | 3.0.2         | Apache-2.0                                                            |
| [Spring AOP](https://github.com/spring-projects/spring-framework)                                                                                          | `org.springframework:spring-aop`                                       | 7.0.5         | Apache-2.0                                                            |
| [Spring Aspects](https://github.com/spring-projects/spring-framework)                                                                                      | `org.springframework:spring-aspects`                                   | 7.0.5         | Apache-2.0                                                            |
| [Spring Beans](https://github.com/spring-projects/spring-framework)                                                                                        | `org.springframework:spring-beans`                                     | 7.0.5         | Apache-2.0                                                            |
| [Spring Context](https://github.com/spring-projects/spring-framework)                                                                                      | `org.springframework:spring-context`                                   | 7.0.5         | Apache-2.0                                                            |
| [Spring Context Support](https://github.com/spring-projects/spring-framework)                                                                              | `org.springframework:spring-context-support`                           | 7.0.5         | Apache-2.0                                                            |
| [Spring Core](https://github.com/spring-projects/spring-framework)                                                                                         | `org.springframework:spring-core`                                      | 7.0.5         | Apache-2.0                                                            |
| [Spring Expression Language (SpEL)](https://github.com/spring-projects/spring-framework)                                                                   | `org.springframework:spring-expression`                                | 7.0.5         | Apache-2.0                                                            |
| [Spring JDBC](https://github.com/spring-projects/spring-framework)                                                                                         | `org.springframework:spring-jdbc`                                      | 7.0.5         | Apache-2.0                                                            |
| [Spring Messaging](https://github.com/spring-projects/spring-framework)                                                                                    | `org.springframework:spring-messaging`                                 | 7.0.5         | Apache-2.0                                                            |
| [Spring Object/Relational Mapping](https://github.com/spring-projects/spring-framework)                                                                    | `org.springframework:spring-orm`                                       | 7.0.5         | Apache-2.0                                                            |
| [Spring Transaction](https://github.com/spring-projects/spring-framework)                                                                                  | `org.springframework:spring-tx`                                        | 7.0.5         | Apache-2.0                                                            |
| [Spring Web](https://github.com/spring-projects/spring-framework)                                                                                          | `org.springframework:spring-web`                                       | 7.0.5         | Apache-2.0                                                            |
| [Spring Web MVC](https://github.com/spring-projects/spring-framework)                                                                                      | `org.springframework:spring-webmvc`                                    | 7.0.5         | Apache-2.0                                                            |
| [Spring WebSocket](https://github.com/spring-projects/spring-framework)                                                                                    | `org.springframework:spring-websocket`                                 | 7.0.5         | Apache-2.0                                                            |
| [spring-boot](https://spring.io/projects/spring-boot)                                                                                                      | `org.springframework.boot:spring-boot`                                 | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-actuator](https://spring.io/projects/spring-boot)                                                                                             | `org.springframework.boot:spring-boot-actuator`                        | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-actuator-autoconfigure](https://spring.io/projects/spring-boot)                                                                               | `org.springframework.boot:spring-boot-actuator-autoconfigure`          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-autoconfigure](https://spring.io/projects/spring-boot)                                                                                        | `org.springframework.boot:spring-boot-autoconfigure`                   | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-cache](https://spring.io/projects/spring-boot)                                                                                                | `org.springframework.boot:spring-boot-cache`                           | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-data-commons](https://spring.io/projects/spring-boot)                                                                                         | `org.springframework.boot:spring-boot-data-commons`                    | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-data-jpa](https://spring.io/projects/spring-boot)                                                                                             | `org.springframework.boot:spring-boot-data-jpa`                        | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-h2console](https://spring.io/projects/spring-boot)                                                                                            | `org.springframework.boot:spring-boot-h2console`                       | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-health](https://spring.io/projects/spring-boot)                                                                                               | `org.springframework.boot:spring-boot-health`                          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-hibernate](https://spring.io/projects/spring-boot)                                                                                            | `org.springframework.boot:spring-boot-hibernate`                       | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-http-client](https://spring.io/projects/spring-boot)                                                                                          | `org.springframework.boot:spring-boot-http-client`                     | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-http-converter](https://spring.io/projects/spring-boot)                                                                                       | `org.springframework.boot:spring-boot-http-converter`                  | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-jackson](https://spring.io/projects/spring-boot)                                                                                              | `org.springframework.boot:spring-boot-jackson`                         | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-jackson2](https://spring.io/projects/spring-boot)                                                                                             | `org.springframework.boot:spring-boot-jackson2`                        | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-jdbc](https://spring.io/projects/spring-boot)                                                                                                 | `org.springframework.boot:spring-boot-jdbc`                            | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-jpa](https://spring.io/projects/spring-boot)                                                                                                  | `org.springframework.boot:spring-boot-jpa`                             | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-liquibase](https://spring.io/projects/spring-boot)                                                                                            | `org.springframework.boot:spring-boot-liquibase`                       | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-mail](https://spring.io/projects/spring-boot)                                                                                                 | `org.springframework.boot:spring-boot-mail`                            | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-micrometer-metrics](https://spring.io/projects/spring-boot)                                                                                   | `org.springframework.boot:spring-boot-micrometer-metrics`              | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-micrometer-observation](https://spring.io/projects/spring-boot)                                                                               | `org.springframework.boot:spring-boot-micrometer-observation`          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-persistence](https://spring.io/projects/spring-boot)                                                                                          | `org.springframework.boot:spring-boot-persistence`                     | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-restclient](https://spring.io/projects/spring-boot)                                                                                           | `org.springframework.boot:spring-boot-restclient`                      | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-security](https://spring.io/projects/spring-boot)                                                                                             | `org.springframework.boot:spring-boot-security`                        | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-security-oauth2-client](https://spring.io/projects/spring-boot)                                                                               | `org.springframework.boot:spring-boot-security-oauth2-client`          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-security-oauth2-resource-server](https://spring.io/projects/spring-boot)                                                                      | `org.springframework.boot:spring-boot-security-oauth2-resource-server` | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-servlet](https://spring.io/projects/spring-boot)                                                                                              | `org.springframework.boot:spring-boot-servlet`                         | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-sql](https://spring.io/projects/spring-boot)                                                                                                  | `org.springframework.boot:spring-boot-sql`                             | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter](https://spring.io/projects/spring-boot)                                                                                              | `org.springframework.boot:spring-boot-starter`                         | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-actuator](https://spring.io/projects/spring-boot)                                                                                     | `org.springframework.boot:spring-boot-starter-actuator`                | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-aspectj](https://spring.io/projects/spring-boot)                                                                                      | `org.springframework.boot:spring-boot-starter-aspectj`                 | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-cache](https://spring.io/projects/spring-boot)                                                                                        | `org.springframework.boot:spring-boot-starter-cache`                   | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-data-jpa](https://spring.io/projects/spring-boot)                                                                                     | `org.springframework.boot:spring-boot-starter-data-jpa`                | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-jackson](https://spring.io/projects/spring-boot)                                                                                      | `org.springframework.boot:spring-boot-starter-jackson`                 | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-jdbc](https://spring.io/projects/spring-boot)                                                                                         | `org.springframework.boot:spring-boot-starter-jdbc`                    | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-liquibase](https://spring.io/projects/spring-boot)                                                                                    | `org.springframework.boot:spring-boot-starter-liquibase`               | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-logging](https://spring.io/projects/spring-boot)                                                                                      | `org.springframework.boot:spring-boot-starter-logging`                 | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-mail](https://spring.io/projects/spring-boot)                                                                                         | `org.springframework.boot:spring-boot-starter-mail`                    | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-micrometer-metrics](https://spring.io/projects/spring-boot)                                                                           | `org.springframework.boot:spring-boot-starter-micrometer-metrics`      | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-oauth2-client](https://spring.io/projects/spring-boot)                                                                                | `org.springframework.boot:spring-boot-starter-oauth2-client`           | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-oauth2-resource-server](https://spring.io/projects/spring-boot)                                                                       | `org.springframework.boot:spring-boot-starter-oauth2-resource-server`  | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-restclient](https://spring.io/projects/spring-boot)                                                                                   | `org.springframework.boot:spring-boot-starter-restclient`              | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-security](https://spring.io/projects/spring-boot)                                                                                     | `org.springframework.boot:spring-boot-starter-security`                | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-thymeleaf](https://spring.io/projects/spring-boot)                                                                                    | `org.springframework.boot:spring-boot-starter-thymeleaf`               | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-tomcat](https://spring.io/projects/spring-boot)                                                                                       | `org.springframework.boot:spring-boot-starter-tomcat`                  | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-tomcat-runtime](https://spring.io/projects/spring-boot)                                                                               | `org.springframework.boot:spring-boot-starter-tomcat-runtime`          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-validation](https://spring.io/projects/spring-boot)                                                                                   | `org.springframework.boot:spring-boot-starter-validation`              | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-web](https://spring.io/projects/spring-boot)                                                                                          | `org.springframework.boot:spring-boot-starter-web`                     | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-webmvc](https://spring.io/projects/spring-boot)                                                                                       | `org.springframework.boot:spring-boot-starter-webmvc`                  | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-starter-websocket](https://spring.io/projects/spring-boot)                                                                                    | `org.springframework.boot:spring-boot-starter-websocket`               | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-thymeleaf](https://spring.io/projects/spring-boot)                                                                                            | `org.springframework.boot:spring-boot-thymeleaf`                       | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-tomcat](https://spring.io/projects/spring-boot)                                                                                               | `org.springframework.boot:spring-boot-tomcat`                          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-transaction](https://spring.io/projects/spring-boot)                                                                                          | `org.springframework.boot:spring-boot-transaction`                     | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-validation](https://spring.io/projects/spring-boot)                                                                                           | `org.springframework.boot:spring-boot-validation`                      | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-web-server](https://spring.io/projects/spring-boot)                                                                                           | `org.springframework.boot:spring-boot-web-server`                      | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-webmvc](https://spring.io/projects/spring-boot)                                                                                               | `org.springframework.boot:spring-boot-webmvc`                          | 4.0.3         | Apache-2.0                                                            |
| [spring-boot-websocket](https://spring.io/projects/spring-boot)                                                                                            | `org.springframework.boot:spring-boot-websocket`                       | 4.0.3         | Apache-2.0                                                            |
| [Spring Data Core](https://spring.io/projects/spring-data)                                                                                                 | `org.springframework.data:spring-data-commons`                         | 4.0.3         | Apache-2.0                                                            |
| [Spring Data JPA](https://projects.spring.io/spring-data-jpa)                                                                                              | `org.springframework.data:spring-data-jpa`                             | 4.0.3         | Apache-2.0                                                            |
| [spring-security-config](https://spring.io/projects/spring-security)                                                                                       | `org.springframework.security:spring-security-config`                  | 7.0.3         | Apache-2.0                                                            |
| [spring-security-core](https://spring.io/projects/spring-security)                                                                                         | `org.springframework.security:spring-security-core`                    | 7.0.3         | Apache-2.0                                                            |
| [spring-security-crypto](https://spring.io/projects/spring-security)                                                                                       | `org.springframework.security:spring-security-crypto`                  | 7.0.3         | Apache-2.0                                                            |
| [spring-security-data](https://spring.io/projects/spring-security)                                                                                         | `org.springframework.security:spring-security-data`                    | 7.0.3         | Apache-2.0                                                            |
| [spring-security-messaging](https://spring.io/projects/spring-security)                                                                                    | `org.springframework.security:spring-security-messaging`               | 7.0.3         | Apache-2.0                                                            |
| [spring-security-oauth2-client](https://spring.io/projects/spring-security)                                                                                | `org.springframework.security:spring-security-oauth2-client`           | 7.0.3         | Apache-2.0                                                            |
| [spring-security-oauth2-core](https://spring.io/projects/spring-security)                                                                                  | `org.springframework.security:spring-security-oauth2-core`             | 7.0.3         | Apache-2.0                                                            |
| [spring-security-oauth2-jose](https://spring.io/projects/spring-security)                                                                                  | `org.springframework.security:spring-security-oauth2-jose`             | 7.0.3         | Apache-2.0                                                            |
| [spring-security-oauth2-resource-server](https://spring.io/projects/spring-security)                                                                       | `org.springframework.security:spring-security-oauth2-resource-server`  | 7.0.3         | Apache-2.0                                                            |
| [spring-security-web](https://spring.io/projects/spring-security)                                                                                          | `org.springframework.security:spring-security-web`                     | 7.0.3         | Apache-2.0                                                            |
| [thymeleaf](http://www.thymeleaf.org/thymeleaf-lib/thymeleaf)                                                                                              | `org.thymeleaf:thymeleaf`                                              | 3.1.3.RELEASE | Apache-2.0                                                            |
| [thymeleaf-spring6](http://www.thymeleaf.org/thymeleaf-lib/thymeleaf-spring6)                                                                              | `org.thymeleaf:thymeleaf-spring6`                                      | 3.1.3.RELEASE | Apache-2.0                                                            |
| [unbescape](http://www.unbescape.org)                                                                                                                      | `org.unbescape:unbescape`                                              | 1.1.6.RELEASE | Apache-2.0                                                            |
| [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml)                                                                                                     | `org.yaml:snakeyaml`                                                   | 2.5           | Apache-2.0                                                            |
| [JHipster server-side framework](https://github.com/jhipster/jhipster-bom/)                                                                                | `tech.jhipster:jhipster-framework`                                     | 9.0.0         | Apache-2.0                                                            |
| [Jackson-core](https://github.com/FasterXML/jackson-core)                                                                                                  | `tools.jackson.core:jackson-core`                                      | 3.0.4         | Apache-2.0                                                            |
| [jackson-databind](https://github.com/FasterXML/jackson)                                                                                                   | `tools.jackson.core:jackson-databind`                                  | 3.0.4         | Apache-2.0                                                            |
| [Jackson-datatype-hibernate7](https://github.com/FasterXML/jackson-datatype-hibernate)                                                                     | `tools.jackson.datatype:jackson-datatype-hibernate7`                   | 3.0.4         | Apache-2.0                                                            |
| [Jackson module: JAXB Annotations (javax.xml.bind)](https://github.com/FasterXML/jackson-modules-base)                                                     | `tools.jackson.module:jackson-module-jaxb-annotations`                 | 3.0.4         | Apache-2.0                                                            |
