# Modules: extending OpenELIS without a core pull request

A module is one JAR that OpenELIS discovers at start-up. It can bring its own
database tables, Spring services, REST endpoints, permissions, menu entries,
pages and dashboard cards. A site installs it by copying the JAR into a folder
and restarting the webapp.

**A module reuses the core, on the backend and the frontend: anything the core
already has, a module uses instead of duplicating.** See
[Reusing the core's services and components](#3-reusing-the-cores-services-and-components).

**Example module:**
[DIGI-UW/openelis-module-specimen-followup](https://github.com/DIGI-UW/openelis-module-specimen-followup),
the Specimen Follow-up & Action Tracker. Every feature on this page is used
there, and the code samples below are taken from it. Start a new module by
copying it.

The architecture and its reasoning are in
[specs/plans/module-system-architecture.md](../specs/plans/module-system-architecture.md);
the decisions are in [ADR 0001](adr/0001-module-system.md).

## Contents

1. [Quick start: run the example module](#1-quick-start-run-the-example-module)
2. [What a module can contribute](#2-what-a-module-can-contribute)
3. [Reusing the core's services and components](#3-reusing-the-cores-services-and-components)
4. [Create a module step by step](#4-create-a-module-step-by-step)
5. [Extension slots](#5-extension-slots)
6. [Configuration, events and backend extension points](#6-configuration-events-and-backend-extension-points)
7. [Develop on the dev stack](#7-develop-on-the-dev-stack)
8. [Test, build and CI](#8-test-build-and-ci)
9. [Deploy and operate](#9-deploy-and-operate)
10. [Reference](#10-reference)
11. [Troubleshooting](#11-troubleshooting)

## 1. Quick start: run the example module

Check out the core and the example module next to each other, then start the dev
stack **from the core checkout** with the module's folder:

```bash
git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git
git clone https://github.com/DIGI-UW/openelis-module-specimen-followup.git
cd OpenELIS-Global-2
bash scripts/setup-workspace.sh                      # once per clone
scripts/dev-stack up --setup light --module ../openelis-module-specimen-followup
scripts/dev-stack url                                # open this address
```

Log in as `admin` / `adminADMIN!`. You now see:

| Where                    | What the module added                                                                |
| ------------------------ | ------------------------------------------------------------------------------------ |
| Home dashboard           | An "Open follow-ups" card (an [extension slot](#5-extension-slots) contribution)     |
| Menu, Order              | Specimen Follow-up, with Create Follow-up and View Follow-ups                        |
| `/m/specimen-followup/…` | The module's pages, loaded from its own Vite dev server with hot reload              |
| Admin, Modules           | The module, its version and its state (`STARTED`)                                    |
| Role Management          | Two new permissions, `mod.specimen-followup.view` and `mod.specimen-followup.manage` |

Edit a file under the module's `frontend/src` and the browser updates at once.
After a Java, Liquibase or manifest change, run the same `up --module` command
again: it rebuilds the JAR and restarts the webapp. See
[Develop on the dev stack](#7-develop-on-the-dev-stack) for every command.

## 2. What a module can contribute

There are two kinds of module, set by `kind` in the manifest:

| `kind`     | May declare                                                                                                                  | Compiles against                                    |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| `module`   | Everything below                                                                                                             | `openelis-spi`, `openelis-module-api`, core classes |
| `provider` | Backend extension points, beans, configuration and permissions only: no tables, entities, controllers, menu entries or pages | `openelis-spi`, `openelis-module-api`               |

What a full module can add, where it is declared, and where the example does it:

| Contribution                           | Declared in the manifest          | Code in the module                                  | In the example module                                    |
| -------------------------------------- | --------------------------------- | --------------------------------------------------- | -------------------------------------------------------- |
| Database tables                        | `backend.liquibase`               | `liquibase/base.xml` and changelog files            | `src/main/resources/liquibase/`                          |
| JPA entities                           | `backend.entities`                | `@Entity` classes, usually `ModuleAuditedEntity`    | `domain/FollowUp.java`                                   |
| Services                               | `backend.basePackages`            | `@Service` classes that inject core services        | `service/FollowUpService.java`, `CoreLookupService.java` |
| REST API                               | `backend.basePackages`            | `@RestController` under `/rest/modules/<id>/`       | `web/FollowUpRestController.java`                        |
| Start-up logic                         | `backend.activator`               | A class implementing `OpenElisModule`               | `SpecimenFollowUpModule.java`                            |
| Permissions                            | `permissions[]`                   | `@PreAuthorize("hasAuthority('mod.<id>.<key>')")`   | `view`, `manage`                                         |
| Menu entries                           | `menu[]`                          | Nothing                                             | Order, Specimen Follow-up                                |
| Pages                                  | `frontend.routes[]`               | React components exposed by the module's Vite build | `frontend/src/pages/`                                    |
| Dashboard cards and other slot content | `frontend.slots[]`                | React components exposed by the Vite build          | `frontend/src/slots/FollowUpDashboardCard.tsx`           |
| Translations                           | `frontend.messages`               | `messages/<locale>.json` with `mod.<id>.*` keys     | `frontend/public/messages/en.json`                       |
| Settings                               | `configuration.properties[]`      | `ModuleContext.configuration()`, `useModuleConfig`  | `defaultDueDays`                                         |
| Reports                                | `backend.extensions[]` (`report`) | A `ReportProvider`                                  | Not used                                                 |
| Reactions to core events               | Nothing                           | `@EventListener` on an `openelis-spi` event         | Not used                                                 |

A module never changes a core table, a core page or a core bean. It references
core data by id, reads it through core services, and adds UI only through its
own pages, its menu entries and the [extension slots](#5-extension-slots) the
core offers.

## 3. Reusing the core's services and components

> **Rule:** if the core already has a service, component, hook or helper for
> what your module needs, use the core's. Never copy core code into a module,
> and never re-implement something the core already does.

A module runs inside OpenELIS, so it calls the core's own code directly, on the
backend and on the frontend:

| Side     | What a module reuses                                                                                                                                                                | How                                                                                                                     |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Backend  | Every core Spring service (patients, samples, sample items, results, users, statuses, reference data, ...), the core database and its transactions, security, roles and permissions | Inject the service through the constructor, as any core class does; or `context.coreService(...)` from the activator    |
| Frontend | The core's UI components, its API client, hooks, contexts and the module SDK (session, permissions, notifications, configuration)                                                   | `import ... from "openelis_core/components"` (and `/api`, `/hooks`, `/contexts`, `/sdk`), loaded from the running shell |

### Backend: use the core's services

The module's Spring context is a child of the core's, so any core service can be
injected. The example module never reads a core table: it asks the core's
services for patients, specimens, statuses and users
(`service/CoreLookupService.java`, shortened):

```java
@Service
@Transactional(readOnly = true)
public class CoreLookupService {

    private final PatientService patientService;
    private final SampleHumanService sampleHumanService;
    private final SampleItemService sampleItemService;
    private final TypeOfSampleService typeOfSampleService;
    private final SystemUserService systemUserService;
    private final IStatusService statusService;

    public CoreLookupService(PatientService patientService, SampleHumanService sampleHumanService,
            SampleItemService sampleItemService, TypeOfSampleService typeOfSampleService,
            SystemUserService systemUserService, IStatusService statusService) {
        this.patientService = patientService;
        this.sampleHumanService = sampleHumanService;
        this.sampleItemService = sampleItemService;
        this.typeOfSampleService = typeOfSampleService;
        this.systemUserService = systemUserService;
        this.statusService = statusService;
    }

    /** A patient as the core shows it: names, gender, birth date and ids come from PatientService. */
    public Optional<PatientSummary> patient(String patientId) {
        Patient patient = patientService.getData(patientId);
        if (patient == null || patient.getId() == null) {
            return Optional.empty();
        }
        return Optional.of(new PatientSummary(patient.getId(), patientService.getFirstName(patient),
                patientService.getLastName(patient), patientService.getGender(patient),
                patientService.getBirthdayForDisplay(patient), patientService.getNationalId(patient),
                patientService.getSubjectNumber(patient)));
    }

    /** A patient's specimens, with their sample types and status, from the core's sample services. */
    public List<SpecimenSummary> specimensOfPatient(String patientId) {
        List<SpecimenSummary> specimens = new ArrayList<>();
        for (Sample sample : sampleHumanService.getSamplesForPatient(patientId)) {
            List<String> types = new ArrayList<>();
            for (SampleItem item : sampleItemService.getSampleItemsBySampleId(sample.getId())) {
                types.add(typeOfSampleService.getNameForTypeOfSampleId(sampleItemService.getTypeOfSampleId(item)));
            }
            String status = statusService.getStatusNameFromId(sample.getStatusId());
            specimens.add(new SpecimenSummary(sample.getId(), sample.getAccessionNumber(), status, types));
        }
        return specimens;
    }

    /** Active staff for the assignee list, from SystemUserService. */
    public List<UserSummary> activeUsers() {
        List<UserSummary> users = new ArrayList<>();
        for (SystemUser user : systemUserService.getAllSystemUsers()) {
            if ("Y".equalsIgnoreCase(user.getIsActive())) {
                users.add(new UserSummary(user.getId(), user.getLoginName(), user.getDisplayName()));
            }
        }
        return users;
    }
}
```

The module's own tables hold only what the module adds: a follow-up stores the
core patient's and sample's **ids** and reads everything else from the core when
it shows them. Outside Spring-managed classes, the activator reaches a core
service through its context:

```java
@Override
public void onStart(ModuleContext context) {
    SystemUserService users = context.coreService(SystemUserService.class);
    context.log().info("active users: " + users.getAllSystemUsers().size());
}
```

The core's security applies too: a module's REST handlers are guarded by
`@PreAuthorize("hasAuthority('mod.<id>.<key>')")`, its permissions appear in
Role Management next to the core's, and `@Transactional` joins the core's
transactions.

### Frontend: use the core's components and SDK

The module's pages import the core's own components through the `openelis_core`
remote, so they look and behave exactly like core pages. The example's create
page (`frontend/src/pages/CreateFollowUpPage.tsx`, shortened) uses the core's
patient search, breadcrumb, date picker, notifications and API client:

```tsx
import { PageBreadCrumb, SearchPatientForm } from "openelis_core/components";
import {
  AlertDialog,
  CustomDatePicker,
  NotificationKinds,
} from "openelis_core/components";
import {
  useModuleConfig,
  useNotifications,
  useOpenElisApi,
} from "openelis_core/sdk";

export default function CreateFollowUpPage() {
  const api = useOpenElisApi(); // the core's API client: base path, session, CSRF
  const notifications = useNotifications()!; // the core's notification system (always there inside the shell)
  const config = useModuleConfig("specimen-followup"); // the module's own settings

  const save = async (payload: FollowUpRequest) => {
    const response = await api.post<FollowUp>(
      "/rest/modules/specimen-followup/followups",
      payload
    );
    notifications.addNotification({
      kind: NotificationKinds.success,
      title: "Follow-up saved",
    });
    notifications.setNotificationVisible(true);
  };

  return (
    <>
      {notifications.notificationVisible && <AlertDialog />}
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "mod.specimen-followup.create.title",
            link: "/m/specimen-followup/create",
          },
        ]}
      />
      {/* The core's patient search, the same one order entry uses */}
      <SearchPatientForm
        getSelectedPatient={selectPatient}
        idPrefix="followup"
      />
      {/* The core's date picker, in the site's date format */}
      <CustomDatePicker
        id="followup-due-date"
        labelText="Due date"
        onChange={setDueDate}
      />
    </>
  );
}
```

Other pages reuse the rest of the surface the same way:

```tsx
import {
  usePermission,
  useSession,
  useConfigurationProperties,
} from "openelis_core/sdk";

const canManage = usePermission("mod.specimen-followup.manage"); // the core's permission check
const { userSessionDetails } = useSession(); // the signed-in user
const siteConfig = useConfigurationProperties(); // the site's settings, e.g. date format
```

The full list of what `openelis_core` exposes is in the
[frontend reference](#frontend-reference).

### TypeScript and the module SDK

`openelis_core/*` exists only at runtime, after the shell registers the remote
in the browser, so on its own a module build cannot check the names it imports:
a misspelt export builds green and fails in the browser. The module SDK,
`@openelis/module-sdk` in `frontend/packages/module-sdk` of the core, closes
that gap. It carries:

- `types/openelis_core.d.ts`: the five entry points as ambient modules, so
  `tsc --noEmit` checks every `openelis_core/*` import by name and props without
  changing the import paths.
- `build.js` (typed by `build.d.ts`): `openelisModule()`, the whole Vite
  configuration of a module derived from its manifest (see
  [Step 9: pages](#step-9-pages)).
- `shared.js` (typed by `shared.d.ts`): `sharedForModule()` (the shell's shared
  singletons with `import: false`) and `coreRemote()` for the module's Vite
  configuration, so the shared list is written once, in the core.
- `tsconfig.base.json`: the compiler options a module extends. It sets
  `preserveSymlinks`: the SDK is installed as a link into the core checkout,
  which has no `node_modules` on a module's CI, so without it the declarations
  could not find React's types there and every component prop would quietly turn
  into `any`; with it they resolve from the module's own `node_modules`, locally
  and on CI alike.
- `testing/`: one test double per entry point, each with its declarations
  (`testing/<entry>.d.ts`: the entry point's surface plus the double's test
  controls), and `openelisCoreAliases`, the Vitest alias list that renders a
  page against the doubles instead of the shell.
- Its `version` is the frontend contract version: the shell reads it into
  `FRONTEND_CONTRACT_VERSION`, the backend keeps `ModuleFrontendContract` in
  step with it (a core test fails otherwise), and a module's
  `requires.frontendContract` range is matched against it. Versions so far:
  - `1.0.0`: the shared singletons (React 17, React Router 5, react-intl 5,
    Carbon, TanStack Query 4) and the five `openelis_core` entry points.
  - `1.1.0` (2026-11-17): the shell provides the Module Federation runtime
    (`provideExternalRuntime`) and shares `react/jsx-runtime`; a module built
    with the SDK's preset ships neither (its runtime chunk goes from about 19 kB
    gzipped to under 1 kB) and declares `"frontendContract": "^1.1"`. A `1.0`
    shell lists such a module as not loadable with that reason instead of
    failing when its pages load; a module built against `1.0` still runs on a
    `1.1` shell.

The package is not published yet (ADR 0001, decision 1): a module installs it
from the core checkout it targets. The example module keeps the core at `./core`
(its CI checks it out there, at `CORE_REF`; `scripts/dev-stack up --module`
links it there before `npm ci`; by hand, `ln -s <core checkout> core`) and
declares

```json
"devDependencies": {
  "@openelis/module-sdk": "file:../core/frontend/packages/module-sdk",
  "@types/react": "^17.0.0",
  "@types/react-dom": "^17.0.0",
  "@types/react-router-dom": "^5.3.3",
  "typescript": "^6.0.0"
}
```

react-intl 5 declares an optional peer on TypeScript 4, so the module's
`frontend/.npmrc` sets `legacy-peer-deps=true` (the core's own install needs the
same flag); without it `npm ci` refuses TypeScript 6 and the Maven build stops.
`tsconfig.json` extends the base and `npm run typecheck` (`tsc --noEmit`) runs
in the `test` phase of `mvn verify`, next to the Vitest run, so a wrong import
or prop fails the build before the JAR is made. The example is TypeScript
throughout: its pages, slot component, API helpers (`src/followUpApi.ts`, with
types that mirror the backend's DTOs), tests and `vite.config.ts`, with
`noImplicitAny` on. A `.jsx` page still builds, but `tsc` checks neither its
`openelis_core/*` imports nor the code inside it (the base sets
`checkJs: false`), so a misspelt import there fails only in the browser: write
pages as `.tsx`.

```json
{
  "extends": "@openelis/module-sdk/tsconfig.base.json",
  "compilerOptions": {
    "noImplicitAny": true
  },
  "include": ["src", "vite.config.ts"]
}
```

A page test imports nothing from the shell: the aliases hand it the doubles, and
`openelisCoreDedupe` (the shared libraries) goes into `resolve.dedupe`, so the
doubles, which live in the core checkout, use the module's React rather than the
core's (two Reacts in one test is the "Invalid hook call" error). `apiResponses`
and `apiCalls` from `@openelis/module-sdk/testing/sdk.jsx` script and record the
API a page calls, `testPatients` in `testing/components.jsx` feeds the patient
search, and the contexts in `testing/contexts.js` default to an administrator
session with an empty configuration. The doubles are typed, so a `.ts` test
scripts them without casts:

```tsx
apiResponses["/rest/modules/specimen-followup/followups/7"] = (
  endpoint,
  body,
  method
) => (method === "DELETE" ? 204 : followUp);
```

The example tests every page this way (`src/pages/*.test.tsx`): a small
`src/test/renderPage.tsx` mounts the page inside a router at its route, with the
module's English or French messages and a notification context that records what
the page announces, and each test drives the page as a user would and asserts
what the API received. Carbon's counted `TextArea` needs `ResizeObserver`, which
jsdom lacks; the example's `src/setupTests.ts` provides an empty one.

The federation plugin shares exact keys only. Every library a module expects
from the shell must be in the shared list under that exact name; a module that
imports an unlisted subpath bundles its own copy, and a context read from that
copy is `null` in the shell with no build warning.

### What reuse gives you

- **Same look and behaviour:** module pages render the very components the core
  renders, with the same session, translations and theme.
- **Core fixes reach modules without a rebuild:** a module runs the services and
  renders the components of the OpenELIS version it is installed in.
- **Nothing to keep in sync:** there is no second copy of patient search, sample
  lookups or date pickers to maintain.

### When something is not reusable yet

| Need                                                 | Do                                                                                                                                                                                                        |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A core service exists but lacks the method you need  | Add the method to the core service in a core pull request. Do not call the core's DAOs or query its tables from the module                                                                                |
| A core component exists but `openelis_core` lacks it | Add one export line to `frontend/src/module-exports/components.js` (or `hooks.js`, `api.js`, ...) in a core pull request, then import it in the module. Do not copy the component's files into the module |
| Nothing in the core does it                          | Build it in the module. If other modules or the core will need it too, propose it to the core instead                                                                                                     |

Adding to the frontend surface is a frontend contract minor version.

**The boundaries that keep the core in charge:** a module uses core services but
cannot replace them (the core never sees module beans, even `@Primary` ones); it
references core data by id and reads it through services, never changing a core
table; and it imports only what `frontend/src/module-exports/` exposes, not
arbitrary core frontend files.

<a id="3-create-a-module-step-by-step"></a>

## 4. Create a module step by step

### Step 1: copy the example and choose an id

Copy the example repository and pick a module **id**: lower case, starting with
a letter, letters, digits and hyphens, 3 to 41 characters, for example
`lab-notes`. Every namespace the module owns derives from it. Replace
`specimen-followup` (and `specimen_followup`, `followup`) in these places:

| Place                                      | Example value for `lab-notes`                          |
| ------------------------------------------ | ------------------------------------------------------ |
| Manifest `id`                              | `lab-notes`                                            |
| Java package                               | `org.openelisglobal.module.labnotes`                   |
| `pom.xml` `module.id` and `artifactId`     | `lab-notes`, `openelis-module-lab-notes`               |
| `frontend/src/…Api.js` `MODULE_ID`         | `lab-notes`                                            |
| Message keys                               | `mod.lab-notes.*`                                      |
| Liquibase folder, changeset ids and author | `liquibase/lab-notes/`, `lab-notes-001-…`, `lab-notes` |
| Table names                                | `mod_lab_notes_*`                                      |
| REST prefix                                | `/rest/modules/lab-notes/`                             |
| Page routes                                | `/m/lab-notes/…`                                       |
| Menu element ids                           | `menu_mod_lab_notes`, `menu_mod_lab_notes_*`           |
| Permissions in code                        | `mod.lab-notes.view`, `mod.lab-notes.manage`           |

The core refuses a module that breaks one of these namespaces; see
[Namespace rules](#namespace-rules).

### Step 2: the Maven project

The module depends on three artifacts from the core, all in `provided` scope;
none is ever packaged inside the module JAR:

| Artifact                                              | Contents                                                                                                       | Manifest requirement |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- | -------------------- |
| `org.openelisglobal:openelis-module-api`              | `OpenElisModule`, `ModuleContext`, `ModuleConfiguration`, `ModuleAuditedEntity`, the manifest model and schema | `requires.coreApi`   |
| `org.openelisglobal:openelis-spi`                     | Extension points (`ReportProvider`) and core events (`SampleCreatedEvent`)                                     | `requires.spi`       |
| `org.openelisglobal:openelisglobal:<version>:classes` | The application's own classes (services, DAOs, value holders), attached from the WAR build                     | `requires.openelis`  |

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  <openelis.version>3.4.0-SNAPSHOT</openelis.version>
  <openelis.module-api.version>1.0.0-SNAPSHOT</openelis.module-api.version>
  <openelis.spi.version>1.0.0-SNAPSHOT</openelis.spi.version>
  <module.id>lab-notes</module.id>
</properties>

<dependencies>
  <dependency>
    <groupId>org.openelisglobal</groupId>
    <artifactId>openelis-module-api</artifactId>
    <version>${openelis.module-api.version}</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.openelisglobal</groupId>
    <artifactId>openelis-spi</artifactId>
    <version>${openelis.spi.version}</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.openelisglobal</groupId>
    <artifactId>openelisglobal</artifactId>
    <version>${openelis.version}</version>
    <classifier>classes</classifier>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.openelisglobal</groupId>
    <artifactId>openelis-module-test</artifactId>
    <version>${openelis.module-api.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

The `classes` artifact brings the WAR's own dependencies in `provided` scope, so
Spring, Hibernate and Jackson arrive at the versions the core really ships. A
library the module adds in `compile` scope is module-only and travels in `lib/`
inside the JAR; the example has none, so it carries no copy step. Add this
execution with the first such library:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <version>3.8.1</version>
  <executions>
    <execution>
      <id>bundle module libraries</id>
      <phase>prepare-package</phase>
      <goals>
        <goal>copy-dependencies</goal>
      </goals>
      <configuration>
        <outputDirectory>${project.build.outputDirectory}/lib</outputDirectory>
        <includeScope>runtime</includeScope>
        <excludeScope>provided</excludeScope>
      </configuration>
    </execution>
  </executions>
</plugin>
```

The example's `pom.xml` also runs the frontend build through
`frontend-maven-plugin` (`npm ci`, `npm run build`, and `npm test` in the test
phase; `-DskipFrontend=true` skips it), and names the JAR
`<module.id>-<version>.jar`. Copy those parts as they are.

### Step 3: the manifest

`src/main/resources/META-INF/openelis-module.json` describes the module. This is
the example's manifest; every field is explained in the
[manifest reference](#manifest-reference).

```json
{
  "schemaVersion": 1,
  "id": "specimen-followup",
  "kind": "module",
  "name": "Specimen Follow-up & Action Tracker",
  "version": "1.0.0",
  "description": "Tracks follow-up actions on existing patients' specimens.",
  "vendor": {
    "name": "OpenELIS Global",
    "url": "https://github.com/DIGI-UW/openelis-module-specimen-followup"
  },
  "requires": {
    "openelis": ">=3.4.0",
    "coreApi": "^1.0",
    "frontendContract": "^1.1"
  },
  "backend": {
    "basePackages": ["org.openelisglobal.module.followup"],
    "activator": "org.openelisglobal.module.followup.SpecimenFollowUpModule",
    "entities": ["org.openelisglobal.module.followup.domain.FollowUp"],
    "liquibase": "liquibase/base.xml"
  },
  "permissions": [
    {
      "key": "view",
      "description": "Open the specimen follow-up pages and read follow-ups",
      "defaultRoles": ["Results", "Reception", "Validation"]
    },
    {
      "key": "manage",
      "description": "Create, update and delete specimen follow-ups",
      "defaultRoles": ["Results", "Reception"]
    }
  ],
  "menu": [
    {
      "elementId": "menu_mod_specimen_followup",
      "parent": "menu_sample",
      "displayKey": "mod.specimen-followup.menu.title",
      "presentationOrder": 95,
      "permission": "view"
    },
    {
      "elementId": "menu_mod_specimen_followup_list",
      "parent": "menu_mod_specimen_followup",
      "displayKey": "mod.specimen-followup.menu.list",
      "actionURL": "/m/specimen-followup/followups",
      "presentationOrder": 2,
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
      },
      {
        "path": "/m/specimen-followup/followups/:followUpId",
        "expose": "./FollowUpDetailPage",
        "permission": "view"
      }
    ],
    "slots": [
      {
        "slot": "home.dashboard.cards",
        "expose": "./FollowUpDashboardCard",
        "permission": "view",
        "order": 10
      }
    ]
  },
  "configuration": {
    "properties": [
      {
        "key": "defaultDueDays",
        "type": "integer",
        "default": 3,
        "description": "Days from today proposed as the due date of a new follow-up",
        "exposeToFrontend": true
      }
    ]
  }
}
```

### Step 4: the activator

The class named in `backend.activator` implements `OpenElisModule`. All four
methods are optional:

```java
public class SpecimenFollowUpModule implements OpenElisModule {

    static final String DEFAULT_DUE_DAYS = "defaultDueDays";

    @Override
    public void validate(ModuleContext context) {
        // Throws ModuleValidationException, which refuses the start, when the setting is out of range.
        context.configuration().requireInteger(DEFAULT_DUE_DAYS, 0, 365);
    }

    @Override
    public void onStart(ModuleContext context) {
        context.log().info("started " + context.descriptor().version());
    }

    @Override
    public void onUpgrade(ModuleContext context, String fromVersion, String toVersion) {
        context.log().info("upgraded from " + fromVersion + " to " + toVersion);
    }
}
```

| Method                         | Runs                                                                                              |
| ------------------------------ | ------------------------------------------------------------------------------------------------- |
| `validate(context)`            | After the module's Spring context is ready; throw `ModuleValidationException` to refuse the start |
| `onStart(context)`             | Once everything is registered; must return within `modules.startTimeoutSeconds` (60)              |
| `onUpgrade(context, from, to)` | After the migrations, when the JAR's version differs from the installed one                       |
| `onStop(context)`              | At shutdown                                                                                       |

`ModuleContext` gives the module its descriptor, its configuration, any core
service by type, a JDBC template on the core datasource, a private data
directory and a logger; see [ModuleContext](#modulecontext).

### Step 5: tables, entities and repositories

**Liquibase.** The manifest names one master changelog, `liquibase/base.xml`,
which includes the module's changelog files in order, the way the core's
`base.xml` files do:

```xml
<databaseChangeLog ...>
  <include relativeToChangelogFile="true" file="specimen-followup/changelog.xml"/>
  <include relativeToChangelogFile="true" file="specimen-followup/002-follow-up-indexes.xml"/>
</databaseChangeLog>
```

```xml
<changeSet id="specimen-followup-001-follow-up-table" author="specimen-followup">
  <preConditions onFail="MARK_RAN">
    <not><tableExists schemaName="clinlims" tableName="mod_specimen_followup_record"/></not>
  </preConditions>
  <createSequence schemaName="clinlims" sequenceName="mod_specimen_followup_record_seq"/>
  <createTable schemaName="clinlims" tableName="mod_specimen_followup_record">
    <column name="id" type="BIGINT"><constraints primaryKey="true" nullable="false"/></column>
    <column name="patient_id" type="VARCHAR(20)"><constraints nullable="false"/></column>
    <!-- the four audit columns ModuleAuditedEntity maps -->
    <column name="created_by_user_id" type="INTEGER"/>
    <column name="created_at" type="TIMESTAMP" defaultValueComputed="now()"><constraints nullable="false"/></column>
    <column name="sys_user_id" type="INTEGER"/>
    <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="now()"><constraints nullable="false"/></column>
  </createTable>
</changeSet>
```

Rules the core enforces before running anything:

- Every changeset id starts with `<id>-`, and its author is the module id.
- Tables live in the `clinlims` schema and start with `mod_<id_>_` (hyphens
  become underscores).
- To change the schema, add a new changelog file and include it last in
  `base.xml`. Never move or rename a file that has run on a site: its path is
  part of every changeset's identity.

**Entities.** Extend `ModuleAuditedEntity` to get the core's audit columns
(`created_by_user_id`, `created_at`, `sys_user_id`, `lastupdated`); the
timestamps are set automatically, the service sets the user ids:

```java
@Entity
@Table(name = "mod_specimen_followup_record", schema = "clinlims")
public class FollowUp extends ModuleAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mod_specimen_followup_record_seq")
    @SequenceGenerator(name = "mod_specimen_followup_record_seq", schema = "clinlims",
            sequenceName = "mod_specimen_followup_record_seq", allocationSize = 1)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;   // a core patient, referenced by id
    ...
}
```

List every entity class in `backend.entities`; they join the core persistence
unit (same session factory, transactions and schema).

**Repositories.** Inject the core's `EntityManager`:

```java
@Repository
public class FollowUpRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public FollowUp save(FollowUp followUp) {
        if (followUp.getId() == null) {
            entityManager.persist(followUp);
            return followUp;
        }
        return entityManager.merge(followUp);
    }
}
```

### Step 6: services that use the core

Classes under `backend.basePackages` are scanned into the module's own Spring
context, whose parent is the core's. Inject any core service through the
constructor; `@Transactional` joins the core transaction manager:

```java
@Service
@Transactional(readOnly = true)
public class CoreLookupService {

    private final PatientService patientService;
    private final SampleService sampleService;

    public CoreLookupService(PatientService patientService, SampleService sampleService) {
        this.patientService = patientService;
        this.sampleService = sampleService;
    }
}
```

The core never sees module beans, so a module cannot replace a core bean, even
with `@Primary`.

### Step 7: the REST API

Every controller maps under `/rest/modules/<id>/`, and every handler carries a
`@PreAuthorize` on one of the module's permissions. A controller mapped anywhere
else fails the module.

```java
@RestController
@RequestMapping("/rest/modules/specimen-followup")
public class FollowUpRestController {

    static final String VIEW = "hasAuthority('mod.specimen-followup.view')";
    static final String MANAGE = "hasAuthority('mod.specimen-followup.manage')";

    @PostMapping(value = "/followups", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(MANAGE)
    public ResponseEntity<FollowUpView> create(@RequestBody FollowUpRequest request,
            HttpServletRequest httpRequest) {
        FollowUpView created = followUpService.create(request, getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** The signed-in user's id as the core keeps it for the request or the session. */
    static String getSysUserId(HttpServletRequest request) {
        UserSessionData data = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        if (data == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                data = (UserSessionData) session.getAttribute(IActionConstants.USER_SESSION_DATA);
            }
        }
        return data == null ? null : String.valueOf(data.getSystemUserId());
    }
}
```

The browser reaches it at `/api/OpenELIS-Global/rest/modules/<id>/…`; the
frontend client adds that base path for you.

### Step 8: permissions and menu entries

**Permissions.** The manifest declares the bare key; the core names it
`mod.<id>.<key>`:

| In the manifest | In `@PreAuthorize`, `usePermission` and Role Management |
| --------------- | ------------------------------------------------------- |
| `"key": "view"` | `mod.specimen-followup.view`                            |

On first install, the core grants each permission to its `defaultRoles`; Global
Administrator always holds every module permission. Sites change the grants in
Role Management afterwards.

**Menu entries.** Each entry needs an `elementId` (`menu_mod_<id_>` or
`menu_mod_<id_>_*`), a `parent`, and a `displayKey` from the module's messages:

- `parent` is the `elementId` of an existing core menu entry, such as
  `menu_sample` (Order), or of an earlier entry of the same module. The core
  menu's element ids are listed by `GET /api/OpenELIS-Global/rest/menu`.
- `actionURL` is one of the module's page routes (`/m/<id>/…`); leave it out for
  a folder that only holds children.
- `permission` hides the entry from users without that permission.
- `presentationOrder` places it among its siblings (default 100).

Module menu entries live in memory: Menu Configuration shows them locked, and
who sees one is decided by its permission in Role Management.

### Step 9: pages

A module's frontend is a Vite build with `@module-federation/vite`. It exposes
one React component per page and per slot contribution, imports the core's
components through the `openelis_core` remote, and shares the shell's React,
router, react-intl, Carbon and query client instead of bundling copies.

**`frontend/vite.config.ts`** (from the example). The whole configuration is the
module SDK's build preset, derived from the manifest (see
[TypeScript and the module SDK](#typescript-and-the-module-sdk)):

```ts
import { openelisModule } from "@openelis/module-sdk/build";

export default openelisModule({
  test: { setupFiles: ["./src/setupTests.ts"] },
});
```

`openelisModule()` reads `../src/main/resources/META-INF/openelis-module.json`
and derives:

- the remote name, the id with hyphens replaced (`specimen_followup`);
- the exposes, one per `frontend.routes[].expose` and `frontend.slots[].expose`:
  `./Name` is the first of `src/pages/Name.jsx`, `.tsx`, `.js` and `.ts` that
  exists for a route, and the same under `src/slots/` for a slot contribution
  (the example's are `.tsx`). The manifest is the only list; a name without a
  file fails `vite build` with the expose and the paths tried, and the core's
  contract test (`ModuleContractTest`) checks the built entry exposes every
  manifest name with a chunk in the bundle;
- the shell as the `openelis_core` remote and the shell's shared singletons with
  `import: false`, so the module bundles no copy of React, its JSX runtime, the
  router, react-intl, Carbon or the query client, and `externalRuntime`, so it
  bundles no copy of the Module Federation runtime either: the shell provides
  both from frontend contract `1.1`, which the manifest's
  `requires.frontendContract` range states, and a `1.0` shell lists the module
  as not loadable with that reason instead of failing when a page loads;
- `build.outDir`, straight into the Maven output
  (`../target/classes/META-INF/resources/modules/<id>`), the dev server on port
  5174 for the shell's dev override, `resolve.dedupe` and the test aliases for
  the SDK's doubles.

Options, all optional: `exposes` (file overrides by expose name, for a component
that does not follow the convention), `pages` and `slots` (directories),
`manifest` (path), `coreEntry`, `outDir`, `port`, and `build`, `server`,
`resolve`, `test` (merged over the defaults) and `plugins` (appended).

**Routes.** Each `frontend.routes[]` entry maps a path under `/m/<id>/` to an
exposed component and names a `permission` (or a `role`): there are no anonymous
module pages. A path may carry parameters, read with `useParams()`:

```json
{
  "path": "/m/specimen-followup/followups/:followUpId",
  "expose": "./FollowUpDetailPage",
  "permission": "view"
}
```

**A page component:**

```tsx
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { FormattedMessage } from "react-intl";
import { PageBreadCrumb } from "openelis_core/components";
import { useOpenElisApi, usePermission } from "openelis_core/sdk";
import type { FollowUp } from "../followUpApi";

export default function FollowUpDetailPage() {
  const { followUpId } = useParams<{ followUpId: string }>();
  const api = useOpenElisApi();
  const canManage = usePermission("mod.specimen-followup.manage");
  const [followUp, setFollowUp] = useState<FollowUp | null>(null);

  useEffect(() => {
    api
      .get<FollowUp>(`/rest/modules/specimen-followup/followups/${followUpId}`)
      .then(setFollowUp);
  }, [api, followUpId]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />
      <h2>
        <FormattedMessage id="mod.specimen-followup.detail.title" />
      </h2>
      {canManage && <button>…</button>}
    </>
  );
}
```

Pages get no module id from the shell; keep it in a constant, as the example's
`followUpApi.ts` does (`export const MODULE_ID = "specimen-followup"`), next to
the types of what its API sends and answers.

**Calling the backend.** `useOpenElisApi()` returns `{ get, post, put, del }`.
It adds the base path, the session cookie, the locale and the CSRF token:

| Call                     | Resolves with                                           |
| ------------------------ | ------------------------------------------------------- |
| `get(endpoint, signal?)` | The parsed body; rejects on a non-2xx answer            |
| `post(endpoint, body)`   | The parsed body; a refused request carries `statusCode` |
| `put(endpoint, body)`    | The parsed body; a refused request carries `statusCode` |
| `del(endpoint)`          | The HTTP status                                         |

**Messages.** Put the translations in `frontend/public/messages/<locale>.json`
(at least `en.json`); every key starts with `mod.<id>.`:

```json
{
  "mod.specimen-followup.menu.title": "Specimen Follow-up",
  "mod.specimen-followup.card.title": "Open follow-ups"
}
```

The backend sends each module's messages with the module registry, so menu
labels exist before the pages load: the shell asks for its active locale
(`/rest/module-registry?locale=fr-MG`; without the parameter the request's
`Accept-Language`, else English) and the registry carries that tag's bundle, the
base language's and the English one, not every bundle the module ships
(`locales` names those). A header locale switch reads the registry again; in
development the shell also reads the files from the module's dev server (see
[Develop on the dev stack](#7-develop-on-the-dev-stack)). For each key the shell
takes the exact locale's text, then the base language's, then the English one,
so a partial translation shows English for the keys it lacks rather than raw
ids; a menu entry whose key has no message in any bundle stays hidden rather
than show the key. The core's own translations load the same way: only the
active locale's bundle is downloaded, so a module adds no locale files to the
first page load. Add new English keys only; other languages follow the core's
translation process.

**The core surface.** Import core components, API helpers, hooks and contexts
from `openelis_core/*`; the full list is in the
[frontend reference](#frontend-reference). Everything there runs the shell's own
instance, so module pages look and behave like core pages.

<a id="4-extension-slots"></a>

## 5. Extension slots

An extension slot is a place in a **core** page where modules may add UI. The
core page renders `<ExtensionSlot name="…" context={…} />`; every started module
that declares a contribution to that slot renders there. This is the only way a
module adds UI inside a core page.

### The slot catalogue

| Slot                     | Where it renders                                                                                                                                                                                                  | Props your component receives        | Rendered when                 |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ----------------------------- |
| `home.dashboard.cards`   | Home dashboard, after the core tiles (`frontend/src/components/home/Dashboard.tsx`)                                                                                                                               | `user` (the session details), `slot` | No dashboard tile is selected |
| `patient.header.actions` | Patient header, before the patient id tags (`frontend/src/components/common/PatientHeader.jsx`), used by order modify, the pathology, cytology and IHC cases, program cases, the results viewer and patient merge | `patientId` (a string), `slot`       | The header shows a patient    |

The catalogue is exported as `SLOTS` from
`frontend/src/modules/ExtensionSlot.jsx`. Both slots are experimental: their
props may still change in a frontend contract minor.

### Contribute to a slot

1. Write the component. It receives the slot's context keys as props, plus
   `slot` (the slot name). This is the example's dashboard card:

   ```tsx
   import React, { useEffect, useState } from "react";
   import { ClickableTile } from "@carbon/react";
   import { useHistory } from "react-router-dom";
   import { useIntl } from "react-intl";
   import { useOpenElisApi } from "openelis_core/sdk";
   import { ROUTES, endpoints } from "../followUpApi";
   import type { FollowUpPage } from "../followUpApi";

   export default function FollowUpDashboardCard() {
     const intl = useIntl();
     const history = useHistory();
     const api = useOpenElisApi();
     const [open, setOpen] = useState<number | null>(null);

     useEffect(() => {
       let cancelled = false;
       api
         .get<FollowUpPage>(
           endpoints.followUps({ status: "OPEN", page: 0, size: 1 })
         )
         .then((page) => !cancelled && setOpen(page?.total ?? 0))
         .catch(() => !cancelled && setOpen(0));
       return () => {
         cancelled = true;
       };
     }, [api]);

     return (
       <ClickableTile
         className="dashboard-tile"
         onClick={() => history.push(`${ROUTES.list}?status=OPEN`)}
       >
         <h5 className="dashboard-tile__title">
           {intl.formatMessage({ id: "mod.specimen-followup.card.title" })}
         </h5>
         <p className="dashboard-tile__value">{open === null ? "…" : open}</p>
       </ClickableTile>
     );
   }
   ```

   A `patient.header.actions` contribution reads the patient from its props:

   ```tsx
   import React from "react";
   import { Button } from "@carbon/react";
   import { useHistory } from "react-router-dom";

   export default function NewFollowUpAction({
     patientId,
   }: {
     patientId: string;
   }) {
     const history = useHistory();
     return (
       <Button
         size="sm"
         kind="ghost"
         onClick={() =>
           history.push(`/m/specimen-followup/create?patientId=${patientId}`)
         }
       >
         New follow-up
       </Button>
     );
   }
   ```

2. Nothing to add in `vite.config.ts`: the build preset exposes
   `src/slots/FollowUpDashboardCard.tsx` under the manifest's name once the next
   step declares it (a declared name without a file fails the build).

3. Declare it in the manifest's `frontend.slots`:

   ```json
   "slots": [
     { "slot": "home.dashboard.cards", "expose": "./FollowUpDashboardCard", "permission": "view", "order": 10 }
   ]
   ```

### How the shell renders contributions

| Rule          | Behaviour                                                                                                                                                                                    |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Which modules | Only `STARTED` modules whose `requires.frontendContract` accepts the shell's contract (`1.1.0`)                                                                                              |
| Order         | Ascending `order` across all modules (default 100)                                                                                                                                           |
| Permission    | With a `permission`, only users holding `mod.<id>.<permission>` see the contribution. The manifest names a key the module declared; it is checked at install                                 |
| Failure       | Each contribution renders in its own error boundary. One that fails to load or throws renders **nothing** (no notice in a slot) and the page carries on; the error is in the browser console |
| Loading       | No spinner; the contribution appears when its code has loaded (one attempt; a failure renders nothing, see Failure above)                                                                    |
| Slot name     | **Not checked**: a misspelled `slot` is accepted at install and simply renders nowhere. Copy the name from the catalogue                                                                     |

### Add a new slot to the core

A slot is a contract between the core and every module, so it is added by a core
pull request. The `home.dashboard.cards` slot is the model.

**Step 1: render the slot in the core page.** The dashboard does it in
`frontend/src/components/home/Dashboard.tsx`:

```jsx
import ExtensionSlot from "../../modules/ExtensionSlot";

<ExtensionSlot
  name="home.dashboard.cards"
  context={{ user: userSessionDetails }}
/>;
```

A new slot, for example a toolbar on the results page:

```jsx
<ExtensionSlot
  name="results.toolbar.actions"
  context={{ accessionNumber, testSectionId }}
/>
```

| Prop      | Rule                                                                                                                                                          |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`    | `area.place`, unique; modules write exactly this in their manifest                                                                                            |
| `context` | Each key becomes a prop of every contribution. Pass plain values (ids, strings, numbers), never component state or callbacks: the props are a public contract |
| `wrapper` | Optional component that wraps the contributions, for example a grid or a button set                                                                           |

The slot renders nothing when no module contributes, so the page looks exactly
as before on a site without modules.

**Step 2: add it to the catalogue.** Add the slot and its props to `SLOTS` in
`frontend/src/modules/ExtensionSlot.jsx`, and a row to
[the slot catalogue](#the-slot-catalogue) on this page:

```js
export const SLOTS = {
  "home.dashboard.cards": "{ user }",
  "patient.header.actions": "{ patientId }",
  "results.toolbar.actions": "{ accessionNumber, testSectionId }",
};
```

**Step 3: test it.** In `frontend/src/modules/ExtensionSlot.test.jsx` or the
page's own test, check that a contribution renders with the slot's props and
that the page is unchanged without contributions.

**Step 4: version it.** A new slot is a frontend contract minor: existing
modules keep loading. Changing or removing a slot's props is a major, after
which modules declare the new `requires.frontendContract`.

Modules then contribute as described in
[Contribute to a slot](#contribute-to-a-slot), naming the new slot:

```json
"slots": [{ "slot": "results.toolbar.actions", "expose": "./MyAction", "permission": "view" }]
```

<a id="5-configuration-events-and-backend-extension-points"></a>

## 6. Configuration, events and backend extension points

### Configuration

Declare settings in `configuration.properties` with a `type` (`string`,
`integer`, `boolean`, `url`) and a `default` matching it. A site overrides one
with the property `modules.<id>.<key>` in any of the application's property
sources: the mounted properties directory, `/run/secrets` or the environment.

```json
"configuration": {
  "properties": [
    { "key": "defaultDueDays", "type": "integer", "default": 3, "exposeToFrontend": true }
  ]
}
```

| Read it from | How                                                                                                                                                   |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend      | `context.configuration().requireInteger("defaultDueDays", 0, 365)`, `getString`, `getBoolean`, `get`; see [ModuleConfiguration](#moduleconfiguration) |
| Frontend     | `useModuleConfig("specimen-followup").defaultDueDays`; only `exposeToFrontend` properties, and **always as strings** (`"3"`)                          |

### Core events

The core publishes `openelis-spi` events into every started module, inside the
publishing transaction. Today there is one: `SampleCreatedEvent`, when an order
with its samples is saved.

```java
@Component
public class NewSampleListener {

    @EventListener
    public void onSampleCreated(SampleCreatedEvent event) {
        // event.getSampleId(), event.getAccessionNumber(), event.getPatientId() (null without a patient),
        // event.getCreatedAt()
    }
}
```

A listener runs inside the order's transaction: an exception thrown here reaches
the code that saved the order. Use `@TransactionalEventListener` for work that
should run after the commit. Core Spring events (`ContextRefreshedEvent` and
others) never reach a module.

### Backend extension points

A provider, or a full module, can implement an `openelis-spi` extension point
and declare it in `backend.extensions`. Today there is one point, `report`:

```java
public class TurnaroundReport implements ReportProvider {

    @Override
    public Set<String> reportIds() {
        return Set.of("labNotesTurnaround");
    }

    @Override
    public ReportOutput run(ReportRequest request) throws ReportException {
        byte[] pdf = render(request.getParameter("from"), request.getParameter("to"), request.getLocale());
        return ReportOutput.pdf(pdf, "turnaround.pdf");
    }
}
```

```json
"requires": { "openelis": ">=3.4.0", "coreApi": "^1.0", "spi": "^1.0" },
"backend": {
  "extensions": [{ "spi": "report", "class": "org.openelisglobal.module.labnotes.TurnaroundReport", "order": 100 }]
}
```

Report ids are unique across modules and may never be the id of a report the
core ships. `run` executes inside the core's transaction and must not open its
own.

### Add a new backend extension point

A backend extension point is a hook where module code takes part in core logic,
such as producing a report. Like a slot, it is added by a core pull request;
`report` is the model.

**Step 1: define the interface in `openelis-spi`.** Put it, with its own value
types, in a new package under
`module-contracts/openelis-spi/src/main/java/org/openelisglobal/spi/`, and
export the package in that artifact's `module-info.java`:

```java
package org.openelisglobal.spi.label;

public interface LabelProvider extends ExtensionPoint {
    Set<String> labelTypes();

    LabelOutput render(LabelRequest request) throws LabelException;
}
```

```java
module org.openelisglobal.spi {
    exports org.openelisglobal.spi;
    exports org.openelisglobal.spi.event;
    exports org.openelisglobal.spi.report;
    exports org.openelisglobal.spi.label;
}
```

Keep it narrow: every method and value type becomes a public contract. Use plain
value types, never core entities or services.

**Step 2: register it in the core.**

| File                                                                      | Change                                                                          |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `src/main/java/org/openelisglobal/module/runtime/ExtensionPoints.java`    | `register(new Definition("label", LabelProvider.class, Cardinality.MULTIPLE));` |
| `src/main/java/org/openelisglobal/module/manifest/ManifestValidator.java` | Add `"label"` to `KNOWN_EXTENSION_POINTS`, so manifests may declare it          |

`Cardinality.MULTIPLE` lets every installed module contribute (ordered by each
declaration's `order`). `Cardinality.SINGLE` allows one active provider: when
several modules provide it, the site chooses with
`spi.label.provider=<module id>`, and the core refuses to start until it does.

**Step 3: consult it where the core does the work.** Ask the module registry for
the providers at the seam in core code. The report factory is the model
(`ReportImplementationFactory.moduleReportCreator`):

```java
ModuleRegistry registry = SpringContext.getBean(ModuleRegistry.class);

// MULTIPLE: every provider, in order
List<LabelProvider> providers = registry.extensions().providers(LabelProvider.class);

// SINGLE: the one the site selected (spi.label.provider), if any
Optional<LabelProvider> provider = registry.extensions()
        .activeProvider(LabelProvider.class, Optional.ofNullable(selectedModuleId));
```

Decide and document at the seam whether the core's own behaviour or the module's
wins, and whether the provider runs inside the caller's transaction (`report`
providers do, and must not open their own).

**Step 4: version and document it.** A new extension point is an `openelis-spi`
minor version: bump it in `module-contracts`, and add the point to
[Backend extension points](#backend-extension-points) on this page with an
example. Modules then implement it and declare it:

```json
"requires": { "openelis": ">=3.4.0", "spi": "^1.1" },
"backend": {
  "extensions": [{ "spi": "label", "class": "org.openelisglobal.module.mylabels.MyLabels" }]
}
```

**Slot or extension point?** Use a slot when modules should add UI to a core
screen, and a backend extension point when they should change or extend what the
core computes or produces.

<a id="6-develop-on-the-dev-stack"></a>

## 7. Develop on the dev stack

Run every command from the **core checkout**. `--module` takes the module's
source folder (any folder with a `pom.xml` and the manifest); repeat it for
several modules.

| Task                                                | Command or action                                                                       |
| --------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Start the stack with the module                     | `scripts/dev-stack up --module ../my-module`                                            |
| Start it without FHIR, the Bridge and the simulator | `scripts/dev-stack up --setup light --module ../my-module`                              |
| Print the address to open                           | `scripts/dev-stack url`                                                                 |
| Change a page or a slot component                   | Save the file; the browser updates (the module's Vite dev server serves the pages)      |
| Change a translation (`frontend/public/messages/`)  | Save the file; the open page shows the new text within a second, no reload              |
| Change Java code, Liquibase files or the manifest   | Run the same `up --module …` command again; it rebuilds the JAR and restarts the webapp |
| Follow the webapp log                               | `scripts/dev-stack logs -f oe.openelis.org` (the module logs as `[module:<id>]`)        |
| Read the module dev server log                      | `.dev-stack/modules/<id>-vite.log` in the core checkout                                 |
| Serve the pages built into the JAR instead          | `scripts/dev-stack up` without `--module`; the JAR stays installed                      |
| Remove the module                                   | Delete `volume/modules/<id>.jar` in the core checkout and run `scripts/dev-stack up`    |
| Stop everything                                     | `scripts/dev-stack down`                                                                |

What `up --module` does, every run:

1. Builds the core with `mvn clean install`, which installs the artifacts the
   module compiles against, and the module test kit.
2. Builds the module JAR (`mvn package`, pages included) and installs it as
   `volume/modules/<id>.jar`, replacing any other copy of the module.
3. Starts the module's Vite dev server (`npm run dev`) and routes the shell's
   requests for the module's pages and message bundles to it.
4. Restarts the webapp, which migrates the module's tables and starts it.

Outside the dev stack,
`localStorage["oe.module-override.<id>"] = "http://localhost:5174/remoteEntry.js"`
points a development shell at a module's dev server, pages and message bundles
alike.

**Translations while you work.** The registry reads a module's bundles from the
installed JAR, so on their own they change only with a rebuilt JAR. A
development build of the shell also finds each module's dev server (the dev
stack's redirect of the module's `messages/en.json`, or the override above) and
re-reads the active locale's bundle files from it about once a second while the
page is visible, as conditional requests; each file it reads replaces the
registry's copy of that language. A saved `fr.json` therefore shows on the open
page without a reload, a key deleted from it falls back to English as it would
in the rebuilt JAR, and a file saved half-edited keeps its last good copy with a
console warning. A module served from its JAR is asked once and left alone, and
a production build of the shell does none of this: it reads the bundles from the
registry only.

<a id="7-test-build-and-ci"></a>

## 8. Test, build and CI

**Contract test.** Extend `ModuleContractTest` from
`org.openelisglobal:openelis-module-test`. It runs the core's own checks at
build time, so a mistake fails your build instead of the site's start-up:

```java
public class SpecimenFollowUpContractTest extends ModuleContractTest {

    @Test
    public void declaresItsPagesAndMenu() {
        assertEquals("specimen-followup", manifest().getId());
    }
}
```

| Inherited check                                       | Fails when                                                                                                          |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `manifestIsValidForTheCore`                           | The manifest breaks the schema or a namespace rule                                                                  |
| `declaredClassesExistAndPlayTheirRole`                | The activator, an entity or an extension class is missing or wrong                                                  |
| `changelogExistsAndItsChangeSetsBelongToTheModule`    | A changelog file is missing, or a changeset id or author is not the module's                                        |
| `everyControllerMapsUnderTheModulePrefixAndIsGuarded` | A controller maps outside `/rest/modules/<id>/` or a handler lacks `@PreAuthorize`                                  |
| `frontendBundleShipsItsEntryAndOwnsItsMessageKeys`    | The bundle has no remote entry, or an English key lacks the `mod.<id>.` prefix (skipped with `-DskipFrontend=true`) |

Test services and controllers the usual way, with the core services mocked; the
example has `FollowUpServiceTest`, `CoreLookupServiceTest` and a MockMvc
`FollowUpRestControllerTest`.

**Build.**

```bash
mvn verify                      # backend tests, frontend build and tests, the JAR
mvn verify -DskipFrontend=true  # backend only
```

The JAR, `target/<id>-<version>.jar`, carries the manifest, the classes, the
changelogs, the prebuilt pages and any module-only libraries. Outside the dev
stack, install the core artifacts once per core commit, from the core checkout:

```bash
mvn -q -f dataexport/pom.xml install -DskipTests -Dmaven.test.skip=true
mvn -q -f module-contracts/pom.xml install -DskipTests
mvn -q install -DskipTests -Dmaven.test.skip=true
mvn -q -f module-test/pom.xml install -DskipTests
```

**CI.** The example's `.github/workflows/ci.yml` checks out the core named by
the `CORE_REPOSITORY` and `CORE_REF` repository variables into `./core`,
installs the core artifacts (cached per core commit), runs `mvn -B verify` and
uploads the JAR as a build artifact. Copy it as it is. The frontend's
`@openelis/module-sdk` dependency points at
`../core/frontend/packages/module-sdk`, so the same checkout serves the SDK
install; locally, `scripts/dev-stack up --module` creates that `core` link.

<a id="8-deploy-and-operate"></a>

## 9. Deploy and operate

Install a module on a site by copying its JAR into the modules directory and
restarting the webapp:

| Deployment                  | Modules directory                  | Restart                                  |
| --------------------------- | ---------------------------------- | ---------------------------------------- |
| openelis-docker             | `configs/modules/`                 | `docker compose restart oe.openelis.org` |
| This repository's compose   | `volume/modules/`                  | `docker compose restart oe.openelis.org` |
| Inside the webapp container | `/var/lib/openelis-global/modules` | Restart the container                    |

Then check the webapp log for `[module:<id>] STARTED`, open Admin, Modules, and
grant the module's permissions in Role Management.

| Operation   | Steps                                                                                                                                                                                        |
| ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Upgrade     | Replace the JAR (never two versions of one module side by side) and restart; new changesets and `onUpgrade` run                                                                              |
| Downgrade   | Refused by default; set `modules.<id>.allowDowngrade=true` to allow it                                                                                                                       |
| Disable     | Create `<jar>.disabled` next to the JAR, or set `modules.<id>.enabled=false` (or list the id in `modules.disabled`), and restart                                                             |
| Remove      | Delete the JAR and restart. Tables and permission rows stay (the data belongs to the site); menu entries and pages disappear                                                                 |
| Strict mode | Put a `modules.lock` in the modules directory with one `id@version sha256` line per allowed JAR; any other JAR is `INVALID`                                                                  |
| Inspect     | Admin, Modules; `GET /rest/module-registry` (every user) lists modules with state and reason; `GET /rest/module-registry/{id}` (administrators) adds the JAR, digest, libraries and warnings |

<a id="9-reference"></a>

## 10. Reference

### Manifest reference

Top level (`schemaVersion`, `id`, `kind`, `name`, `version`, `requires` are
required):

| Field           | Type and rule                                     |
| --------------- | ------------------------------------------------- |
| `schemaVersion` | `1`                                               |
| `id`            | `^[a-z][a-z0-9-]{2,40}$`                          |
| `kind`          | `module` or `provider`                            |
| `name`          | 1 to 120 characters                               |
| `version`       | Semantic version, such as `1.0.0` or `1.1.0-rc.1` |
| `description`   | Text                                              |
| `vendor`        | `{ name, url, contact }`                          |
| `conflicts`     | Ids of modules that cannot run alongside this one |

`requires`:

| Field              | Rule                                                                                         |
| ------------------ | -------------------------------------------------------------------------------------------- |
| `openelis`         | Mandatory range with a lower bound (`>=3.4.0`, `^3.4`); `3.4.0-SNAPSHOT` satisfies `>=3.4.0` |
| `coreApi`          | Range of `openelis-module-api`, such as `^1.0`                                               |
| `spi`              | Range of `openelis-spi`; mandatory with `backend.extensions`                                 |
| `frontendContract` | Range of the shell's contract; mandatory with `frontend`                                     |
| `modules`          | `[{ id, version }]`: other modules this one needs; their classes are visible to this one     |

Ranges use npm syntax (`>=`, `<`, `^`, `~`, `||`).

`backend`:

| Field          | Rule                                                                        |
| -------------- | --------------------------------------------------------------------------- |
| `basePackages` | Packages scanned for Spring beans, under `org.openelisglobal.module.<name>` |
| `activator`    | Class implementing `OpenElisModule`                                         |
| `entities`     | JPA entity classes                                                          |
| `liquibase`    | Master changelog under `liquibase/`, normally `liquibase/base.xml`          |
| `extensions`   | `[{ spi, class, order }]`; `spi` is `report` today; `order` defaults to 100 |

`permissions[]`: `key` (required, `^[a-z][a-zA-Z0-9.-]*$`, unique),
`description`, `defaultRoles`.

`menu[]`: `elementId`, `parent`, `displayKey` (required), `toolTipKey`,
`actionURL` (under `/m/<id>/`), `presentationOrder` (default 100), `icon`,
`permission` (a declared key).

`frontend`:

| Field      | Rule                                                                                                         |
| ---------- | ------------------------------------------------------------------------------------------------------------ |
| `entry`    | Relative path of the remote entry, default `remoteEntry.js`                                                  |
| `messages` | Path pattern containing `{locale}`, default `messages/{locale}.json`                                         |
| `routes[]` | `path` (under `/m/<id>/`, unique), `expose` (`./Name`), `exact` (default `true`), and `permission` or `role` |
| `slots[]`  | `slot` (a catalogue name), `expose` (`./Name`), `order` (default 100), `permission` (a declared key)         |

`configuration.properties[]`: `key` (required), `type` (`string`, `integer`,
`boolean`, `url`; default `string`), `default` (must match the type; a `url`
starts with `http://` or `https://`), `description`, `exposeToFrontend`.

No other fields are accepted anywhere in the manifest.

### Namespace rules

With `<id_>` meaning the id with hyphens replaced by underscores:

| Namespace        | Rule                                                                                                                        |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Permission keys  | `mod.<id>.<key>`; the manifest declares `<key>` only                                                                        |
| Message keys     | `mod.<id>.*`; a menu `displayKey` must use this prefix                                                                      |
| Menu element ids | `menu_mod_<id_>` or `menu_mod_<id_>_*`                                                                                      |
| REST paths       | `/rest/modules/<id>/**`; a controller mapped elsewhere fails the module                                                     |
| Page routes      | `/m/<id>/**`; every route names a permission or a role                                                                      |
| Tables           | `mod_<id_>_*` in `clinlims`; changeset ids start with `<id>-` and the author is `<id>`                                      |
| Java packages    | Under `org.openelisglobal.module.<name>`; the segments `api`, `manifest`, `runtime`, `web` and `testkit` belong to the core |
| Report ids       | Unique across modules and never a report the core ships                                                                     |

### ModuleContext

| Method                               | Returns                                                                                         |
| ------------------------------------ | ----------------------------------------------------------------------------------------------- |
| `descriptor()`                       | `ModuleDescriptor`: `id()`, `name()`, `version()`, `kind()`, `state()`, `jarPath()`, `sha256()` |
| `configuration()`                    | The module's settings, see below                                                                |
| `coreService(Class<T> type)`         | A core bean by type                                                                             |
| `jdbc()`                             | `NamedParameterJdbcTemplate` on the core datasource                                             |
| `dataDirectory()`                    | `/var/lib/openelis-global/modules-data/<id>`                                                    |
| `runAsSystem(Runnable)`              | Runs start-up writes as the system user                                                         |
| `withModuleClassLoader(Callable<T>)` | Runs code with the module's class loader as context loader                                      |
| `log()`                              | `info`, `warn`, `error`; lines carry `[module:<id>]`                                            |

### ModuleConfiguration

| Method                               | Behaviour                                                        |
| ------------------------------------ | ---------------------------------------------------------------- |
| `get(key)`                           | `Optional<String>`                                               |
| `getString(key, fallback)`           | The value or the fallback                                        |
| `getInteger(key)`, `getBoolean(key)` | `Optional`; a malformed value throws `ModuleValidationException` |
| `require(key)`                       | The value; throws when missing                                   |
| `requireInteger(key, min, max)`      | The value; throws when missing or out of range                   |
| `asMap()`                            | Every setting                                                    |

### Frontend reference

| Import                     | Gives a module                                                                                                                                                                                                                                                                                                                      |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `openelis_core/components` | `SearchPatientForm`, `PatientHeader`, `CustomDatePicker`, `CustomTimePicker`, `CustomLabNumberInput`, `CustomSelect`, `CustomTextInput`, `CustomCheckBox`, `AutoComplete`, `PageBreadCrumb`, `ConfirmPopup`, `AlertDialog`, `NotificationKinds`, `Spinner`, `PageTitle`, `RequiredMarker`, `RouteErrorBoundary`, `ServerPageArrows` |
| `openelis_core/api`        | `getFromOpenElisServer`, `fetchFromOpenElisServer`, `postToOpenElisServer`, `postToOpenElisServerJsonResponse`, `putToOpenElisServer`, `putToOpenElisServerJsonResponse`, `deleteFromOpenElisServer`, `hasRole`, `hasPermission`, `serverQuery`, `useServerData`, `config`                                                          |
| `openelis_core/hooks`      | `useInAppNavigation`, `useUrlFilters`, `useActiveLocales`, `useDomains`                                                                                                                                                                                                                                                             |
| `openelis_core/contexts`   | `UserSessionDetailsContext`, `ConfigurationContext`, `NotificationContext`                                                                                                                                                                                                                                                          |
| `openelis_core/sdk`        | The hooks below, `ExtensionSlot`, `ModuleErrorBoundary`, `FRONTEND_CONTRACT_VERSION`                                                                                                                                                                                                                                                |

| `openelis_core/sdk` hook       | Returns                                                                                                                          |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| `useSession()`                 | `{ userSessionDetails, errorLoadingSessionDetails, isCheckingLogin, logout, refresh }`                                           |
| `usePermission(key)`           | `true` when the user holds the full key, such as `mod.specimen-followup.manage`                                                  |
| `useRole(role)`                | `true` when the user has the role                                                                                                |
| `useConfigurationProperties()` | The core's configuration properties (site date format and others)                                                                |
| `useNotifications()`           | `{ addNotification, removeNotification, notifications, setNotificationVisible, … }`; kinds `info`, `error`, `success`, `warning` |
| `useOpenElisApi()`             | `{ get, post, put, del }`, see [Step 9](#step-9-pages)                                                                           |
| `useModuleConfig(moduleId)`    | The module's `exposeToFrontend` settings, as strings                                                                             |
| `useModule(moduleId)`          | The module's registry entry, or `undefined`                                                                                      |

The shell shares `react`, `react/jsx-runtime`, `react-dom`, `react-router-dom`,
`react-intl`, `@carbon/react`, `@carbon/icons-react` and `@tanstack/react-query`
(`sharedSingletons` in `frontend/packages/module-sdk/shared.js`, which
`frontend/module-federation.config.js` imports). The frontend contract version
is the module SDK's package version, `1.1.0`: additions to this surface are
minors; removals, prop changes and a shared library's major version (React 18
will be one) are majors. The surface is typed in
`frontend/packages/module-sdk/types/openelis_core.d.ts`; a core test
(`sdkDeclaresEveryExport.test.js`) keeps those declarations, the test doubles'
and the build preset's in step with what the files really export.

### How the runtime loads a module

1. **Discovery.** At start-up the registry lists `*.jar` in
   `/var/lib/openelis-global/modules` (skipping a JAR with a `<jar>.disabled`
   marker), reads each manifest, computes its SHA-256 and validates it. Two JARs
   with one id stop start-up, naming both files.
2. **Switches and compatibility.** `modules.disabled` and
   `modules.<id>.enabled=false` mark a module `DISABLED`. A module whose
   `requires` the running versions do not satisfy, or with a missing dependency,
   a conflict or a dependency cycle, is `INCOMPATIBLE`. Skipped modules are
   reported and the core starts.
3. **Class loading.** Each module gets one parent-first class loader over its
   JAR and its extracted `lib/*.jar` (cached under
   `modules-cache/<id>/<sha256>/`). A class the core ships always resolves to
   the core's copy; a bundled library the core also ships is reported as
   shadowed.
4. **Migrations.** After the core changelog, each module's master changelog runs
   from the module JAR. The installed version is recorded in
   `clinlims.module_state`. A failing changeset stops start-up, naming the
   module.
5. **Entities.** The declared entities join the core persistence unit.
6. **Spring context.** Each module gets its own context whose parent is the
   core's; `basePackages` are scanned and `@Transactional` and `@PreAuthorize`
   apply.
7. **Contributions.** Extensions are registered, controllers join the core
   request mapping, permissions become `system_module` rows (first install
   grants `defaultRoles`), menu entries join the menu in memory.
8. **Start.** `validate`, then `onStart`. The module is `STARTED`.
9. **Frontend files.** The shell requests `/rest/module-registry` as soon as it
   mounts, beside its session check (both carry the same cookie), keeps the
   answer once the session reports a signed-in user, and repeats a read that was
   refused before sign-in. The registry's `assetBase` names the started module's
   files under the running JAR's digest, `/modules/<id>/<sha16>/`, served from
   the JAR. Every file there, `remoteEntry.js` and the message files included,
   is immutable for a year, so a warm browser sends no request at all for a
   module; a new JAR is a new `assetBase`, so the next load fetches the new
   files, and a request for any other digest answers 404, so an old URL can
   never cache new bytes. The plain form `/modules/<id>/<path>` stays for one
   release with the entry and the message files revalidated on every load. Both
   forms carry a weak `ETag` that names the digest, answered without reading the
   JAR. Tomcat's authenticator adds its own `Cache-Control: private` header to
   every response (the `web.xml` security constraints), so a browser sees
   `private` beside the controller's directives: it caches the digest files as
   told; a shared proxy cache does not store them. Text responses are compressed
   by Tomcat's connector (`tomcat/oe_server.xml`); no proxy setting is needed.
   Chunk URLs inside a built module resolve relative to `remoteEntry.js`, so no
   module is rebuilt for the digest.

Any failure after validation stops the application with the module named and the
two ways out: the `<jar>.disabled` marker or `modules.<id>.enabled=false`, then
a restart.

| State          | Meaning                                                                          |
| -------------- | -------------------------------------------------------------------------------- |
| `STARTED`      | Running                                                                          |
| `MIGRATED`     | Tables migrated; the start did not complete                                      |
| `DISABLED`     | Switched off by a marker file or a setting; the core runs without it             |
| `INVALID`      | The manifest or the JAR breaks a rule; the reason is in the registry and the log |
| `INCOMPATIBLE` | Its `requires` do not match this core, or a dependency is missing or conflicting |
| `FAILED`       | Failed after validation; the core stopped (see above)                            |

<a id="10-troubleshooting"></a>

## 11. Troubleshooting

| Symptom                                                                                             | Cause and fix                                                                                                                                                                                                                                                              |
| --------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Admin, Modules shows `INVALID`                                                                      | The reason names the broken rule (namespace, schema, missing class). Run the module's contract test; it reports the same problems at build time                                                                                                                            |
| Admin, Modules shows `INCOMPATIBLE`                                                                 | `requires.openelis`, `coreApi` or `spi` does not match this core, or a dependency is missing; rebuild against it or widen the range. A `frontendContract` mismatch is the shell's check: the module stays `STARTED` and its row carries the reason                         |
| The webapp stops at start-up naming the module                                                      | A changeset, `validate` or `onStart` failed; the log has the cause. Add `<jar>.disabled` next to the JAR to start without it                                                                                                                                               |
| The menu entry does not show                                                                        | The user lacks the entry's permission, or its `displayKey` has no English message                                                                                                                                                                                          |
| The page shows "Module unavailable"                                                                 | The remote entry did not load: the dev server stopped (rerun `up --module`), or the JAR has no pages (build without `-DskipFrontend`)                                                                                                                                      |
| `tsc` fails on CI only with "Parameter implicitly has an 'any' type" on a core component's callback | The module's `tsconfig.json` overrides `preserveSymlinks` (or does not extend the SDK's base): the SDK's declarations then look for React's types next to the core, which has none on CI. Extend `@openelis/module-sdk/tsconfig.base.json` and keep its `preserveSymlinks` |
| `vite build` fails with "the manifest names an expose the frontend does not have"                   | A `routes[].expose` or `slots[].expose` has no `src/pages/<Name>.*` or `src/slots/<Name>.*` file: the page was renamed or the manifest has a typo; the message lists the paths tried. A component elsewhere takes an `exposes` override in `openelisModule()`              |
| A slot contribution does not show                                                                   | The `slot` name is misspelled (not checked), the user lacks its permission, or it failed (see the browser console)                                                                                                                                                         |
| A REST call answers 403                                                                             | The user's role does not hold the handler's permission; grant it in Role Management                                                                                                                                                                                        |
| A REST call answers 404                                                                             | The controller is not under `backend.basePackages`, or the path lacks `/rest/modules/<id>/`                                                                                                                                                                                |
| The log refuses a changeset of the module                                                           | Every changeset id must start with `<id>-` and its author must be `<id>`                                                                                                                                                                                                   |
| Module files arrive uncompressed                                                                    | The deployment runs its own `server.xml`; copy the connector's `compression` attributes from `tomcat/oe_server.xml`                                                                                                                                                        |
| A module page shows the previous JAR's pages                                                        | The browser never does after a restart: `assetBase` carries the new JAR's digest. A proxy cache keyed on the old URL, or a tab left open on the old entry, does; reload the page                                                                                           |
| `/modules/<id>/<digest>/…` answers 404                                                              | That digest is not the running JAR's: the JAR was replaced, or the URL was copied from another installation; read `assetBase` from `/rest/module-registry`                                                                                                                 |
| `mvn` cannot find `openelisglobal:classes`                                                          | The core artifacts are not installed; run `scripts/dev-stack up --module …` or the four install commands in [Build](#8-test-build-and-ci)                                                                                                                                  |
