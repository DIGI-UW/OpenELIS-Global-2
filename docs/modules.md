# Modules: extending OpenELIS without a core pull request

A module is one JAR, dropped into `/var/lib/openelis-global/modules`, that
OpenELIS discovers at start-up. It describes itself in a manifest,
`META-INF/openelis-module.json`, and can bring its own database tables, Spring
services, REST endpoints, permissions, navigation entries and pages. It reuses
the core's services and components instead of copying them.

This page is the reference for module authors. The architecture and its
reasoning are in
[specs/plans/module-system-architecture.md](../specs/plans/module-system-architecture.md);
the decisions are in [ADR 0001](adr/0001-module-system.md).

## Two shapes of module

| `kind`     | What it may declare                                                                                                                                               | Compiles against                                                           |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `provider` | Implementations of extension points, beans, configuration, permissions. No entities, no changelog, no controllers, no menu, no frontend.                          | `openelis-spi`, `openelis-module-api`                                      |
| `module`   | Everything: beans, REST controllers under `/rest/modules/<id>/`, extensions, JPA entities, a Liquibase changelog, permissions, menu entries, a prebuilt frontend. | `openelis-spi`, `openelis-module-api`, `openelis-core` (the WAR's classes) |

## The three contracts

All three are built from this repository and installed into the local Maven
repository by `mvn -f module-contracts/pom.xml install` (the build hooks run it
before the WAR, like `dataexport`). A module declares them in `provided` scope;
none of them is ever packaged inside a module JAR.

| Artifact                                              | Contents                                                                                                                                           | Version                                                  |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `org.openelisglobal:openelis-spi`                     | `ExtensionPoint` and the extension points (`report` today) with their value types; the events the core publishes to modules (`SampleCreatedEvent`) | Its own semver; `requires.spi` in the manifest           |
| `org.openelisglobal:openelis-module-api`              | `OpenElisModule` (lifecycle), `ModuleContext`, `ModuleDescriptor`, the manifest model, the manifest JSON schema, `ModuleAuditedEntity`             | Its own semver; `requires.coreApi`                       |
| `org.openelisglobal:openelisglobal:<version>:classes` | The application's own classes (services, DAOs, value holders), attached from the WAR build                                                         | The application version; `requires.openelis` (mandatory) |

A provider references only the first two, so it keeps loading on every core
release that ships the same SPI major. A full module that injects core services
takes on the application's release cadence and declares the lowest version it
was built and tested against in `requires.openelis`.

```xml
<dependency>
  <groupId>org.openelisglobal</groupId>
  <artifactId>openelis-module-api</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>org.openelisglobal</groupId>
  <artifactId>openelisglobal</artifactId>
  <version>3.4.0-SNAPSHOT</version>
  <classifier>classes</classifier>
  <scope>provided</scope>
</dependency>
```

The `classes` artifact carries the WAR's dependencies transitively, so Spring,
Hibernate, Jackson and the rest arrive in `provided` scope at the versions the
core really ships. Anything a module adds in `compile` scope is a module-only
library and is packaged under `lib/` inside the JAR.

## The manifest

`META-INF/openelis-module.json` is validated against the schema shipped in
`openelis-module-api` (`META-INF/openelis-module.schema.json`) and then against
the rules below. A manifest with any problem makes the module `INVALID`; the
problems are logged and shown by the registry, and the core starts without the
module.

```json
{
  "schemaVersion": 1,
  "id": "specimen-followup",
  "kind": "module",
  "name": "Specimen Follow-up & Action Tracker",
  "version": "1.0.0",
  "requires": {
    "openelis": ">=3.4.0",
    "coreApi": "^1.0",
    "frontendContract": "^1.0"
  },
  "backend": {
    "basePackages": ["org.openelisglobal.module.followup"],
    "activator": "org.openelisglobal.module.followup.SpecimenFollowUpModule",
    "entities": ["org.openelisglobal.module.followup.domain.FollowUp"],
    "liquibase": "liquibase/specimen-followup/changelog.xml"
  },
  "permissions": [
    {
      "key": "view",
      "description": "Open the follow-up pages",
      "defaultRoles": ["Results"]
    },
    { "key": "manage", "description": "Create, update and delete follow-ups" }
  ],
  "menu": [
    {
      "elementId": "menu_mod_specimen_followup",
      "parent": "menu_sample",
      "displayKey": "mod.specimen-followup.menu.title",
      "actionURL": "/m/specimen-followup/followups",
      "presentationOrder": 90,
      "permission": "view"
    }
  ],
  "frontend": {
    "entry": "remoteEntry.js",
    "messages": "messages/{locale}.json",
    "routes": [
      {
        "path": "/m/specimen-followup/followups",
        "expose": "./FollowUpListPage",
        "permission": "view"
      }
    ]
  }
}
```

### Rules the registry enforces

Every namespace the module owns derives from `id` (lower-case, starts with a
letter, letters, digits and hyphens, 3 to 41 characters). With `<id_>` meaning
the id with hyphens replaced by underscores:

| Namespace        | Rule                                                                                                                                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Permission keys  | `mod.<id>.<key>`; the manifest declares `<key>` only                                                                                                                                                         |
| Message keys     | `mod.<id>.*`; a menu `displayKey` must use this prefix                                                                                                                                                       |
| Menu element ids | `menu_mod_<id_>` or `menu_mod_<id_>_*`                                                                                                                                                                       |
| REST paths       | `/rest/modules/<id>/**`; a controller mapped elsewhere fails the module                                                                                                                                      |
| Report ids       | unique across modules and never the id of a report the core ships; a module cannot take over a built-in report                                                                                               |
| Routes           | `/m/<id>/**` only; every route names a permission or a role                                                                                                                                                  |
| Tables           | `mod_<id_>_*` in the `clinlims` schema; changeset ids start with `<id>-` with author `<id>`, the changelog lives under `liquibase/<id>/`                                                                     |
| Java packages    | `backend.basePackages`, the activator, the entities and the extension classes under `org.openelisglobal.module.<name>`; `api`, `manifest`, `runtime`, `web` and `testkit` belong to the core and are refused |

Other rules:

- `requires.openelis` is mandatory and must carry a lower bound (`>=3.4.0`,
  `^3.4`, `>=3.4.0 <4`). The running version is compared with its pre-release
  label dropped, so `3.4.0-SNAPSHOT` satisfies `>=3.4.0`.
- `requires.spi` is mandatory when `backend.extensions` is declared;
  `requires.frontendContract` when `frontend` is declared.
- A provider may not declare `entities`, `liquibase`, `menu` or `frontend`, and
  must declare at least one extension.
- A menu entry's `permission`, a route's `permission` and a slot's `permission`
  must be one of the module's own declared keys.
- A configuration property's `default` must match its `type` (`string`,
  `integer`, `boolean`, `url`).

Ranges use npm syntax (`>=`, `<`, `^`, `~`, `||`), parsed by semver4j on the
server and `semver` in the browser.

## Lifecycle

The class named in `backend.activator` implements `OpenElisModule`:

```java
public class SpecimenFollowUpModule implements OpenElisModule {
    @Override
    public void validate(ModuleContext context) {
        context.configuration().requireInteger("dueDays", 1, 365);
    }

    @Override
    public void onStart(ModuleContext context) {
        context.log().info("started");
    }
}
```

`validate` runs after the module's Spring context is refreshed and may throw
`ModuleValidationException` to refuse to start; `onStart` runs once everything
is registered; `onUpgrade(context, from, to)` runs after the migrations when the
JAR's version differs from the recorded one; `onStop` runs at shutdown.
`ModuleContext` gives the module its descriptor, its typed configuration
(`modules.<id>.<key>` properties), any core service by type, a JDBC template on
the core datasource, a private data directory, a way to run start-up writes as
the system user, and a logger.

## Build layout

```
my-module/
├── pom.xml                                   provided: openelis-module-api, openelis-spi, openelisglobal:classes
├── src/main/resources/META-INF/openelis-module.json
├── src/main/resources/liquibase/my-module/changelog.xml
├── src/main/java/org/openelisglobal/module/<name>/...  activator, entities, services, controllers
├── frontend/                                 the pages, built into src/main/resources/META-INF/resources/modules/<id>/
└── src/test/java/...                         tests
```

## How the runtime loads a module

1. **Discovery.** At start-up the registry lists `*.jar` in
   `/var/lib/openelis-global/modules` (a `<jar>.disabled` marker skips a JAR),
   reads each manifest, computes its SHA-256 and validates it. Two JARs with one
   id stop start-up naming both files.
2. **Switches and compatibility.** `modules.disabled` (comma-separated ids) and
   `modules.<id>.enabled=false` mark a module `DISABLED`. A module whose
   `requires.openelis`, `requires.coreApi` or `requires.spi` the running
   versions do not satisfy, a missing or wrong-version dependency, a conflict or
   a dependency cycle mark it `INCOMPATIBLE`. Skipped modules are reported by
   `GET /rest/module-registry` and in the log, and the core starts.
3. **Class loading.** Each remaining module gets one parent-first class loader
   over its JAR and its extracted `lib/*.jar` (cached under
   `modules-cache/<id>/<sha256>/`). A class the core ships always resolves to
   the core's copy; a bundled library the core also ships is reported as
   shadowed. Modules named in `requires.modules` are visible, searched after the
   core. The activator, the entities and the extensions must load from the
   module and implement their contracts, or the module is `INVALID`.
4. **Migrations.** After the core changelog and before the entity manager
   factory, each `VALIDATED` module's changelog runs through the Liquibase API
   with the module's class loader. Every changeset id must start with `<id>-`
   and carry the module id as author (refused before anything runs). The
   installed version is recorded in `clinlims.module_state`; a JAR older than
   the recorded version is refused unless `modules.<id>.allowDowngrade=true`. A
   failing changeset stops start-up naming the module, as a failing core
   changeset does.
5. **Entities.** The declared entity classes join the core persistence unit
   (same session factory, transactions and `clinlims` schema).
6. **Child context.** Once the root context is refreshed, each `MIGRATED` module
   gets its own Spring context whose parent is the core's: the module's
   `basePackages` are scanned, the activator and the extensions become beans,
   `@Transactional` joins the core transaction manager and `@PreAuthorize` is
   enforced. A module injects any core service; the core never sees module
   beans, so a module `@Primary` bean cannot replace a core bean.
7. **Contributions.** Declared extensions are registered with the extension
   registry (the report factory consults it first). Every `@RestController` of
   the module is registered into the core request mapping; a mapping outside
   `/rest/modules/<id>/` fails the module. Permission keys become
   `system_module` rows named `mod.<id>.<key>` (first install also grants the
   `defaultRoles`), menu entries join the menu tree in memory (Menu Management
   refuses to edit them; who sees one is decided by its permission in Role
   Management), and a menu entry with an `actionURL` and a `permission` gets a
   `system_module_url` row so the menu hides it from users who lack the key.
8. **Start.** `validate` then `onStart` run with the module's class loader as
   context loader; `onStart` must return within `modules.startTimeoutSeconds`
   (60). The module is `STARTED`; `module_state` records the start.

Any failure after validation (migration, context refresh, a controller outside
the prefix, `validate`, `onStart`) stops the application with the module named
and the two ways to recover: the `<jar>.disabled` marker file or
`modules.<id>.enabled=false`, then a restart.

### Events

Spring events never flow from the core context into a module's. The core
publishes `openelis-spi` events (`SampleCreatedEvent` when an order with its
samples is saved) and the runtime forwards them into every started module,
inside the publishing transaction, where ordinary `@EventListener` and
`@TransactionalEventListener` methods receive them. A module's own events do
reach the core's listeners; its lifecycle events (`ContextRefreshedEvent`) do
not, so core start-up listeners run once.

### Configuration

Module settings are `modules.<id>.<key>` properties in the application's
property sources (the mounted properties directory, `/run/secrets`, the
environment), typed and defaulted by the manifest's `configuration.properties`;
`ModuleContext.configuration()` reads them. Properties marked `exposeToFrontend`
are served by `GET /rest/module-registry/{id}/config`.

### Operations

| Operation   | Steps                                                                                                                                                                                |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Install     | Copy the JAR into the mounted modules directory (`volume/modules` in this repository's compose files, `configs/modules` in `openelis-docker`), restart the webapp.                   |
| Upgrade     | Replace the JAR (never two versions of one id), restart; migrations and `onUpgrade` run.                                                                                             |
| Disable     | Create `<jar>.disabled` next to the JAR, or set `modules.<id>.enabled=false`, restart.                                                                                               |
| Remove      | Delete the JAR, restart. Tables and permission rows stay (the data belongs to the site); menu entries and routes vanish.                                                             |
| Strict mode | Put a `modules.lock` in the modules directory with one `id@version sha256` line per allowed JAR; unlisted or changed JARs are `INVALID`.                                             |
| Inspect     | `GET /rest/module-registry` (any user) lists every module with its state and reason; `GET /rest/module-registry/{id}` (administrators) adds the JAR, digest, libraries and warnings. |
| State table | `clinlims.module_state` keeps the last outcome per module id: MIGRATED, STARTED or FAILED from a start, DISABLED, INVALID or INCOMPATIBLE when discovery set the module aside.       |

The frontend shell and the packaging steps are described in the next sections as
those phases land.
