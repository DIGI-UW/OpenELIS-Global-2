# OpenELIS Global 2

OpenELIS Global is open enterprise-level laboratory information system software
tailored for public health laboratories. OpenELIS is used at a national scale in
a variety of settings, from small general hospital labs, all the way up to
national reference labs, and all sizes in between.

Thousands of users use OpenELIS daily to make their laboratory jobs easier by
automating work plans, importing results from clinical analyzers, and supporting
complex workflows like pathology and cytology, reducing turnaround times, and
increasing result accuracy for better patient care.

OpenELIS Global meets all relevant ISO and SLIPTA requirements for the
accreditation of labs.

OpenELIS adheres to the strictest of security standards to keep your data safe
and supports fully featured, standards-based interoperability to make it easy to
receive lab orders and send results to other systems

Please vist our [website](http://www.openelis-global.org/) for more information.

You can find more information on how to set up OpenELIS at our
[docs page](http://docs.openelis-global.org/)

### CI Status

[![01 - Backend Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/backend.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/backend.yml)
![Coverage](https://raw.githubusercontent.com/DIGI-UW/OpenELIS-Global-2/refs/heads/gh-pages/badges/jacoco.svg)

[![02 - Frontend Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/frontend.yml)

[![03 - Playwright Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-playwright.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-playwright.yml)

[![04 - Cypress Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-cypress-deprecated.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-cypress-deprecated.yml)

[![E2E Wrapper Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-tests.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/e2e-tests.yml)

[![Installer Packaging Status](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml/badge.svg)](https://github.com/DIGI-UW/OpenELIS-Global-2/actions/workflows/build-installer.yml)

### CI Architecture

For the current fork/non-fork E2E validation design, artifact contracts, and
checkpoint/status model, see
[`specs/plans/ci-e2e-architecture-spec.md`](specs/plans/ci-e2e-architecture-spec.md).

For operational troubleshooting of the E2E wrapper and downstream execution, see
[`specs/plans/e2e-ci-operator-model.md`](specs/plans/e2e-ci-operator-model.md).

### Contributing

We welcome community contributions to help improve OpenELIS Global!

1. Read our
   [Dev Environment Setup Instructions](https://uwdigi.atlassian.net/wiki/spaces/OG/pages/240844805/Dev+Environment+Setup+Instructions)
   on the project wiki.
2. Check out our [CONTRIBUTING guide](./CONTRIBUTING.md) for detailed
   contribution practices and [pull request tips](PULL_REQUEST_TIPS.md).
3. To report a **security vulnerability**, follow [SECURITY.md](./SECURITY.md)
   (private reporting — not public issues).

### Requirements

1. You need to install [Docker](https://docs.docker.com/engine/install/) and
   [Docker compose](https://docs.docker.com/compose/install/)

1. For development , you need to install [Java](https://openjdk.org/install/) 21

### For Offline Installation Using the OpenELIS Global2 Installer

Download the OpenELIS Global Installer for each Release from the
[Release Assets](https://github.com/DIGI-UW/OpenELIS-Global-2/releases)

see full
[installation instructions](https://uwdigi.atlassian.net/wiki/x/EoBIDg#Downloaded-Installer-Offline-Setup)
for Offline Installation

### For running OpenELIS Global2 in Docker with default Settings out of the Box

see [OpenELIS-Docker setup](https://github.com/DIGI-UW/openelis-docker)

### For Running OpenELIS Global2 from Source Code

Development has one supported startup path. From the root of any clone or Git
worktree, run:

```bash
scripts/dev-stack up
```

The command initializes the required submodules, uses Java 21, builds the local
WAR and analyzer components, and starts the complete OpenELIS + analyzer harness.
Its Compose project, containers, images, networks, ports, and volumes are derived
from the worktree path, so multiple worktrees can run concurrently.
The harness analyzer scenarios are created idempotently through authenticated
application services after login readiness; startup never seeds the database
directly. Use `--no-scenarios` only when testing an intentionally empty system.

Useful commands:

```bash
scripts/dev-stack status
scripts/dev-stack url
eval "$(scripts/dev-stack env)"  # configure Playwright for this worktree
scripts/dev-stack logs -f oe.openelis.org
scripts/dev-stack down
scripts/dev-stack down --volumes --yes  # explicit data reset
```

Local development needs no configuration: `.env` is created from `.env.example`,
the proxy binds random loopback ports, and `scripts/dev-stack url` prints the
browser URL. Frontend source remains hot-reloaded. Re-run `scripts/dev-stack up`
after backend or analyzer component changes.

The published development frontend dependency image is reused when
`package.json`, `package-lock.json`, and `frontend/Dockerfile` match `develop`;
worktree source is still mounted for hot reload. If any of those inputs differ,
the command automatically builds an isolated frontend image. Set
`DEV_STACK_BUILD_FRONTEND=true` only to force that rebuild.

For a domain-enabled development server, set a real `LETSENCRYPT_DOMAIN` and
`LETSENCRYPT_EMAIL` in `.env`, then run the same `scripts/dev-stack up` command.
It binds ports 80/443, renders the named nginx hosts, and uses the existing
Let's Encrypt HTTP-01 flow. DNS for both the primary domain and
`bridge.<domain>` must resolve to the server. Port and bind overrides are listed
in `.env.example` for hosts that already have an external router; those hosts
should terminate TLS at that router and set `DEV_STACK_TLS=self-signed` for the
private upstream.

Do not invoke the development Compose layers directly. CI, release, and packaged
installation commands remain separate operational interfaces.

#### The Instances can be accessed at

| Instance     |                   URL                   | credentials (user : password) |
| ------------ | :-------------------------------------: | ----------------------------: |
| Legacy UI    | `<scripts/dev-stack url>/api/OpenELIS-Global/` | admin: adminADMIN! |
| New React UI | output of `scripts/dev-stack url`              | admin: adminADMIN! |

**Note:** If your browser indicates that the website is not secure after
accessing any of these links, simply follow these steps:

1. Scroll down on the warning page.
2. Click on the "Advanced" button.
3. Finally, click on "Proceed to https://localhost" to access the development
   environment.

#### Formating the Source code after making changes

1.  After making UI changes to the [frontend](./frontend/) directory , run the
    formatter to properly format the Frontend code

        cd frontend
        npm run format

2.  After making changes to the [backend](./src/) directory, run the formatter
    to properly format the Java code

        mvn spotless:apply

#### To ensure your code passes the same checks as the CI pipeline

**Recommended: Use the CI check scripts** (replicates exact CI workflow):

```bash
# Run backend CI checks (formatting + build + tests)
./scripts/run-ci-checks.sh

# Run frontend CI checks (formatting + unit tests + E2E tests)
./scripts/run-frontend-ci-checks.sh

# Run both (full CI simulation)
./scripts/run-ci-checks.sh && ./scripts/run-frontend-ci-checks.sh
```

**Options:**

- `--skip-submodules`: Skip submodule build (faster, for quick checks)
- `--skip-tests`: Skip tests (formatting only)
- `--skip-e2e`: Skip E2E tests (frontend only)

**Manual commands** (if you prefer to run steps individually):

1.  Run Code Formatting Check (Backend). This command checks code formatting and
    performs validation similar to the CI

        mvn spotless:check

1.  Run Build Check (Backend). This command builds the project similar to CI

        mvn clean install -Dspotless.check.skip=true

1.  To run Individual Integration Test

         mvn verify -Dit.test=<packageName>.<TestClassName>

    **DBUnit test data note:** DB-backed integration tests typically load DBUnit
    Flat XML datasets from `src/test/resources/testdata/` via
    `executeDataSetWithStateManagement("testdata/<file>.xml")`. Prefer datasets
    over inline SQL setup/cleanup to avoid test data pollution.

1.  Run Frontend Formatting, Build, and E2E Test Checks similar to CI

    > **Note:** Frontend checks will only pass successfully if your development
    > environment is properly set up and running without issues.

        cd frontend/ # from project directory
        npm install
        npm run build
        npm run cy:run # this will run e2e testing same CI

### AI-Assisted Development (SpecKit)

This project uses [GitHub SpecKit](https://github.com/github/spec-kit) for
Spec-Driven Development (SDD). AI coding agents can use slash commands to create
specifications, plans, and tasks.

**Available Commands:**

- `/speckit.specify` - Create feature specification
- `/speckit.plan` - Generate implementation plan
- `/speckit.tasks` - Generate task breakdown
- `/speckit.implement` - Execute implementation
- `/speckit.analyze` - Validate consistency

**Reference Documentation:**

- **AGENTS.md** - Comprehensive guide for AI coding agents
- **Constitution**: `.specify/memory/constitution.md` - Governance principles
- **Feature Example**: `specs/001-sample-storage/` - Complete SDD example

### Testing Resources

For comprehensive testing guidance, see:

- **Testing Roadmap**: `.specify/guides/testing-roadmap.md` - Complete testing
  guide for both agents and humans
- **Test Templates**: `.specify/templates/testing/` - Standardized test
  templates
- **AGENTS.md**: Testing Strategy section - Overview of testing approach
- **Test Data Strategy**: `.specify/guides/test-data-strategy.md` - Unified test
  data management guide

### Test Data Setup

For E2E testing, integration testing, and manual testing, load test fixtures:

```bash
# Basic usage (loads and verifies automatically)
./src/test/resources/load-test-fixtures.sh --profile=core

# Harness fixture lane (includes HARN-* lane data)
./src/test/resources/load-test-fixtures.sh --profile=harness

# Reset database before loading (clean state)
./src/test/resources/load-test-fixtures.sh --profile=core --reset

# Load without verification (faster)
./src/test/resources/load-test-fixtures.sh --profile=core --no-verify
```

**Note**: The unified loader script provides dependency checks, verification,
and reset capabilities. See
[Test Data Strategy Guide](.specify/guides/test-data-strategy.md) for details.

### Pull request guidelines

Please follow the [pull request tips](PULL_REQUEST_TIPS.md) in order to make
life easy for the code reviewers by having a well defined and clean pull
request.

### code of conduct

Please see our [Contributor Code of Conduct](./CODE_OF_CONDUCT.md)
