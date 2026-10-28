# Development, CI validation, and deployment

For everyday development, use `scripts/dev-stack`. Run commands from the
repository root of the clone or worktree you are editing.

A normal clone is fully supported. Separate checkout directories get separate
containers, ports and data; creating a Git worktree is not required.

| Purpose                              | Entry point                                   | What runs                                                                |
| ------------------------------------ | --------------------------------------------- | ------------------------------------------------------------------------ |
| Edit and try the application locally | `scripts/dev-stack up`                        | Current source, frontend hot reload, persistent worktree data            |
| Check a committed change against CI  | `scripts/run-ci-checks.sh`                    | Committed source and recorded submodule pins, isolated test environments |
| Run a released application           | [Published deployment](#run-published-images) | Published application images                                             |

The development stack retains HTTPS, secure session cookies and reverse-proxy
routing, but it is not a production deployment or proof that CI passes.

## Work on source

Install Git, Python, a working Docker CLI with Compose, Java 21, Maven, curl,
gettext (`envsubst`), and [mkcert](https://github.com/FiloSottile/mkcert). On
macOS, install mkcert with `brew install mkcert`; other platforms should follow
its installation instructions. Frontend build tools run inside Docker; install a
supported Node version when running browser tests or frontend tools on the host.
The package lockfiles and Dockerfiles own dependency versions.

Once per clone or worktree:

```bash
bash scripts/setup-workspace.sh
```

Start development:

```bash
scripts/dev-stack up
```

Use the URL printed by `up`. Retrieve it later with `scripts/dev-stack url`.
Localhost works with the generated `.env`; no domain or port settings are
needed. `scripts/dev-stack doctor` is an optional prerequisite diagnostic; `up`
already performs those checks.

On the first start on a machine, `up` explains and runs the local certificate
trust setup before building. Complete the administrator prompt in your terminal
if requested. This trusts this machine's development certificate authority in
the operating-system trust store. Later starts and other worktrees reuse that
trust. If authentication is canceled or unavailable, startup stops before the
build and explains how to retry.

`up` never builds container images unless asked. It builds the OpenELIS WAR from
the checkout and runs it, together with the frontend `src` and `public`
directories, in the published development images named in
`projects/analyzer-harness/docker-compose.dev.yml` and
`docker-compose.worktree.yml` (`itechuw/openelis-global-2-dev`, `-frontend-dev`,
`-analyzer-bridge` and `-analyzer-mock`, tag `develop`), which it pulls on each
start. `DEV_STACK_IMAGE_NAMESPACE` and `DEV_STACK_IMAGE_TAG` select other
published images.

Run `up --build` to build those images from the checkout instead. That is only
needed when a change lives inside an image rather than in the mounted WAR or
frontend source: `Dockerfile.dev`, the frontend `Dockerfile`, frontend
dependencies (`package.json`, `package-lock.json`) or build configuration
(`vite.config.ts`, `index.html`, `tsconfig.json`), or Bridge and mock changes in
`tools/<submodule>`. Without `--build`, `up` prints a warning when the frontend
files, a Bridge or mock checkout, or `Dockerfile.dev`, `install/` or
`tomcat/client-certificates` differ from what the published images were built
from, and keeps running the published images. Application containers, networks,
ports, and data volumes belong to this worktree. Docker and Maven caches are
reused.

`up` initializes an empty submodule at the commit the parent repository records.
It never moves, resets or refuses a populated one. For each submodule at another
commit or with uncommitted changes, `up` prints a line such as
`tools/openelis-analyzer-bridge: checked out a4a22a4 (recorded pin b20f5db), with uncommitted changes`.
`dataexport` changes are always built into the WAR; Bridge and mock changes run
with `up --build`. `scripts/run-ci-checks.sh` builds the recorded submodule
commits instead.

Frontend edits hot reload. Re-run `up` after changing backend source. Re-run
`up --build` after changing frontend dependencies, image build configuration or
Bridge and mock source. `--skip-build` reuses this checkout's WAR and the images
the last `up` ran (published or built); it neither pulls nor builds.

```bash
scripts/dev-stack status
scripts/dev-stack logs -f oe.openelis.org
eval "$(scripts/dev-stack env)"
(cd frontend && npm run pw:test -- --project=core-app <spec-path>)
scripts/dev-stack down
scripts/dev-stack down --volumes --yes  # explicit reset of this worktree's data
```

Localhost uses random loopback ports and a locally trusted certificate. Keep the
development authority's private key local; never share or commit it. Restart a
browser that was already open if it caches the old trust. Use a browser that
reads the operating-system trust store; separate browser-specific certificate
stores are not configured by the launcher.

`up` generates a certificate covering `localhost`, `127.0.0.1`, and `::1`,
stores its leaf key under ignored `.dev-stack/tls/`, and installs only that leaf
certificate/key in this worktree's proxy. It verifies HTTPS without ignoring
certificate errors. The trust setup announces its scope and delegates
administrator authentication to mkcert. For an unattended first start, prepare
trust interactively with `TRUST_STORES=system mkcert -install` first.
`DEV_STACK_TLS=self-signed` remains an explicit option for headless fixtures; it
is not the default for interactive development. Backend and FHIR direct ports
retain their internal service certificates; use the proxy URL for browser
access.

Use the printed URL, not an assumed port. For a public development server,
follow [development HTTPS setup](LETSENCRYPT_SETUP.md).

Configuration templates are copied during bootstrap. Existing local properties,
database initialization files and passwords are preserved; nginx and menu
configuration are refreshed. Review local overrides when upstream defaults
change rather than deleting the data volume to refresh configuration.

## Validate a committed candidate

```bash
scripts/run-ci-checks.sh --list-jobs
scripts/run-ci-checks.sh --plan
scripts/run-ci-checks.sh --artifact-dir /tmp/openelis-local-ci
```

Run this after each push while GitHub CI runs. The runner validates one
committed revision in isolated checkouts, starts backend, frontend and E2E lanes
in parallel, and retains logs and browser reports. Each E2E job gets a fresh
isolated database and reuses the candidate images. Use `--base REF` when the PR
base is not `develop`.

A failed job stops its dependent steps, while independent jobs still run. Each
job keeps its own log and result; the overall command fails if any required job
fails or does not run.

The runner selects the CI Node versions with `fnm`, or accepts an explicitly
configured Node installation. It uses the selected Docker context for the Java
test database too. Docker runtime names do not change the test commands.

Select one or more jobs with repeatable `--job NAME` arguments; each job
includes its necessary setup. A selected-job result is partial validation. The
default runs all listed code checks, including existing agent-asset validation;
it does not publish images or manage GitHub checkpoints. GitHub-only
publication, artifact uploads and checkpoint reporting still require their
GitHub results. Neither a failed lane nor a check that did not run is a pass.

## Native builds

Java, Maven and Node can also run directly on the host. The development launcher
does not replace the native build tools or the deployable WAR:

```bash
scripts/run-java21 mvn -f dataexport/pom.xml install -DskipTests -Dmaven.test.skip=true
scripts/run-java21 mvn package -DskipTests -Dmaven.test.skip=true
cd frontend
npm ci --legacy-peer-deps
npm run build
```

The backend artifact is `target/OpenELIS-Global.war`; frontend output is
`frontend/dist`. Native runtime installation still needs the application's
database, servlet-container, FHIR, certificate and property configuration. The
Docker launcher is the supported automated full-stack setup; choosing a native
installation does not change application ownership or configuration.

## Run published images

For a lab deployment, use
[the released default distribution](https://github.com/DIGI-UW/openelis-docker/releases)
and its tagged README. Run the following in that distribution directory after
configuring credentials:

```bash
docker compose pull
docker compose up -d --no-build
docker compose ps
```

OE2's root Compose files remain inputs to build/test and testing-server
deployment. They do not define a second operator installation path. Development
images require an explicit development/testing choice; a production distribution
uses the tested release manifest's digests and fails if an image is unavailable.

The versioned `openelis-global-2-certs` image contains the initializer and
tools, with no installation keys. Docker Compose starts it before dependent
services. An empty installation gets its own TLS identity and independent
`install/encryption.key`; the webapp loads that key unless an explicit
`OE_ENCRYPTION_PASSWORD` is retained. Restart validates and preserves the
complete security volume. Missing material or incorrect passwords stop
initialization. Back up the database, configuration and all security volumes
together.

Existing installations use the separate `certs --upgrade-existing` command with
their effective encryption password supplied through the environment. It adopts
site-owned certificates or replaces recognized shared TLS certificates while
preserving the effective encryption password. It does not scan or convert
database values. See the
[installer's adoption procedure](https://github.com/DIGI-UW/openelis-docker#installation-keys-and-existing-installations).
Moving existing encrypted values to a different encryption key remains the
separate [one-time encryption upgrade](upgrades/installation-encryption.md).

A paired Analyzer Bridge pinned the certificate OpenELIS served when they
paired, so it stops delivering results when the key is replaced; undelivered
results wait in its outbox. Once, after the first start on the new key:

1. Restart the Bridge (`docker compose restart openelis-analyzer-bridge`). A
   Bridge given the stack's truststore, as `docker-compose.analyzers.yml` does,
   then trusts the new certificate through it and delivers the waiting results.
2. If it still cannot reach OpenELIS (its health check stays down), it has no
   truststore: [pair it again](analyzers/bridge-pairing.md#pairing-again) with a
   new pairing code.

A development or harness stack runs the Bridge with its `dev` profile, which
serves the stack keystore itself, so OpenELIS's pin on the Bridge breaks too:
pair it again with a new code.

## Diagnose a source build failure

Read the first actual compiler or dependency error. A successful cached download
can report zero transferred bytes; that alone does not prove an empty archive.
If Maven explicitly reports an empty or damaged JAR, identify that artifact and
recover only the affected cache entry. Do not prune all Docker or Maven caches
as routine setup. Automatic Bridge cache repair is separate follow-up work.

### Install-owned TLS stores

The published Compose stack generates a private TLS key on first start and
reuses it on later starts. Explicit adoption replaces both historical shared
certgen certificate subjects (DIGI-UW and I-TECH-UW), including a shared key
previously exported into the install directory. Genuine site keys, separate
proxy certificates, and extra truststore entries are preserved.

Set `SSL_KEYSTORE_PASSWORD` and `SSL_TRUSTSTORE_PASSWORD` consistently in
`.env`. Compose passes these to certificate generation, Tomcat, FHIR, Bridge,
and the OpenELIS Spring properties. Defaults remain `kspass` and `tspass`;
custom passwords are supported. Changing the variables does not re-encrypt
existing stores: the script preserves an unreadable store and fails certificate
setup before dependent services start. Keep the passwords that were used to
create existing stores when upgrading.
