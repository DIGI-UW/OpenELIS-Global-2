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

The remaining phases of the module system add to this page: the runtime
(discovery, class loading, child contexts, migrations, permissions, menus), the
frontend shell and the packaging and installation steps.
