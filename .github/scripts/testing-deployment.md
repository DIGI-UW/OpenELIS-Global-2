`Publish images / Deploy testing` deploys each tested `develop` commit to the
testing VM using that commit's own `docker-compose.yml` and
`docker-compose.analyzers.yml`, with the five application images pinned to their
published digests.

## Deploy now

In GitHub Actions, open **Publish images / Deploy testing**, choose **Run
workflow**, select **develop**, and run it. The action selects the latest build
for the current `develop` commit, requires its backend and end-to-end checks,
and uses the same publication and deployment jobs as automatic deployment.
It reuses the built images; it does not compile the application again. A missing
or unsuccessful build stops the action, and a newer commit arriving before
container startup stops an obsolete deployment.

Automatic deployments continue after tested pushes to `develop`. Retrying a
commit reuses its existing release directory without replacing files mounted
by running containers.

## Server configuration

The job builds `deploy-bundle.tgz` (both compose files, `volume/`, the analyzer
harness catalog and seed scripts, and the Bridge profiles at the submodule
pin). On the VM, `deploy-published-testing.py` unpacks it into
`<site>/releases/<sha>/` and runs Compose as project `openelis-testing`, so
named volumes persist across releases. The site directory (`TESTING_SITE_PATH`,
default `/home/ubuntu/openelis-testing`) holds what belongs to the host:

- `.env` (required): passed as the Compose env file.
- `docker-compose.site.yml` (optional): applied after the release's files,
  for certificates, proxy configuration and other host-specific settings.
  Compose resolves its relative paths against the release, so use absolute
  paths.
- `lucene/`: the search index, linked into every release.
- `configuration/backend/`: writable catalog files, linked into every release.
  The harness catalog is copied here only when the directory does not exist;
  subsequent deploys preserve uploaded and edited files. The webapp entrypoint
  grants its Tomcat group write access.
- `.openelis-ci/`: the image override and `target.json`.

After the application reports ready, the deploy seeds the default analyzers
(`seed-analyzers.sh --ensure-connections --no-mock-network --activate`). Only
newly created priority connections are activated. An existing shared mapping
is reused only when already confirmed; it is never rewritten or confirmed by
a deployment. Existing connections retain their configuration and activation
state. If setup was interrupted, complete that connection's setup in OpenELIS
before retrying; deployment does not guess whether an inactive connection was
intentionally disabled.

The deploy then sends one GeneXpert result through the mock with an accession
derived from the run ID. The deployment is ready only when that result appears
in OpenELIS. If testers have disabled the GeneXpert connection or changed its
listener from port 9600,
the check fails and leaves their settings intact; restore that connection in
OpenELIS when it is ready to receive the deployment check.
The deploy refuses superseded commits and ports 80/443 owned by any other
Compose project. After success it keeps the current and previous release and
removes unused images.

Configure access with `TESTING_VM_SSH_KEY`, `DEPLOY_HOST`, `TESTING_VM_USER`,
`DEPLOY_PORT`, and `TESTING_SITE_PATH`. `DOCKERHUB_USERNAME` controls the image
namespace.

The site's `.env` also configures the API account used by the Bridge, seeding,
and delivery verification. Set `TEST_USER` and `TEST_PASS` to an existing
OpenELIS account. If unset, `OE_ADMIN_USERNAME` and `OE_ADMIN_PASSWORD` are used,
then the standard testing defaults. These settings do not change the account's
password in OpenELIS. `ASTM_SIMULATOR_HTTP_PORT` (default `8085`) sets both the
mock's loopback port and the delivery check's destination. The deployer uses
Compose's environment parser for these values and passes credentials to the
seed subprocess through its environment, not command arguments.

Optional readiness variables are:

| Variable                     | Default                                                          |
| ---------------------------- | ---------------------------------------------------------------- |
| `TESTING_READINESS_URL`      | `https://testing.openelis-global.org/api/OpenELIS-Global/health` |
| `TESTING_READINESS_TIMEOUT`  | `300` seconds                                                    |
| `TESTING_READINESS_JSON_KEY` | `status` (supports dotted nested keys)                           |
| `TESTING_READINESS_EXPECTED` | `"UP"` (JSON-encoded value)                                      |

Diagnostics include Compose status, logs for the application, Bridge and mock,
readiness, and the previous image selection. Rollback is manual because older
images may be incompatible with applied database migrations.

Run the focused checks from the repository root:

```sh
node --test .github/scripts/publish-checkpoints.test.cjs
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
```

The Python tests require PyYAML and use temporary localhost HTTP servers. Docker
operations are mocked; the tests do not deploy to the testing VM.
`test_analyzer_overlay.py` needs network access to list the Bridge and mock
release tags.
