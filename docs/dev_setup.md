# Development, CI validation, and deployment

Choose the mode that matches your work. Source development and candidate CI
build the checkout; published deployment pulls an existing image set.

## Work on source

Install Git, Python, a working Docker CLI with Compose, Java 21, Maven, curl,
and gettext (`envsubst`). Frontend build tools run inside Docker; install a
supported Node version when running browser tests or frontend tools on the host.
The package lockfiles and Dockerfiles own dependency versions.

From the repository root:

```bash
bash scripts/setup-workspace.sh
scripts/dev-stack doctor
scripts/dev-stack up
scripts/dev-stack url
```

`up` builds OpenELIS, the frontend development stage, Bridge, and the analyzer
mock from the checkout and its pinned submodules. Application image names,
containers, networks, ports, and data volumes belong to this worktree. Docker
and Maven caches are reused. Infrastructure images may be pulled; published
application images never replace source builds.

Frontend edits hot reload. Re-run `up` after changing backend source, pinned
submodules, frontend dependencies, or build configuration. `--skip-build` is
only for reusing this worktree's already-built images and WAR.

```bash
scripts/dev-stack status
scripts/dev-stack logs -f oe.openelis.org
eval "$(scripts/dev-stack env)"
(cd frontend && npm run pw:test -- --project=core-app <spec-path>)
scripts/dev-stack down
scripts/dev-stack down --volumes --yes  # explicit reset of this worktree's data
```

Localhost uses random loopback ports and self-signed certificates. Use the
printed URL, not an assumed port. For a public development server, follow
[development HTTPS setup](LETSENCRYPT_SETUP.md).

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

Follow
[the installation instructions](../README.md#for-offline-installation-using-the-openelis-global2-installer)
and [release guidance](../RELEASES.md) to prepare the deployment configuration.
The deployment files are `docker-compose.yml` and, when analyzers are needed,
`docker-compose.analyzers.yml`. Use coherent version or digest pins.

```bash
docker compose -f docker-compose.yml -f docker-compose.analyzers.yml pull
docker compose -f docker-compose.yml -f docker-compose.analyzers.yml up -d --no-build
```

This mode does not compile application source or mount a locally built WAR. An
unavailable image must fail visibly. Mutable `develop` images are for explicit
development/demo deployments; release verification uses fixed images.

## Diagnose a source build failure

Read the first actual compiler or dependency error. A successful cached download
can report zero transferred bytes; that alone does not prove an empty archive.
If Maven explicitly reports an empty or damaged JAR, identify that artifact and
recover only the affected cache entry. Do not prune all Docker or Maven caches
as routine setup. Automatic Bridge cache repair is separate follow-up work.
