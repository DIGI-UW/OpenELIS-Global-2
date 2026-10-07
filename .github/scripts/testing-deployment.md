`Publish images / Deploy testing` deploys each tested `develop` commit to the
testing VM using that commit's own `docker-compose.yml` and
`docker-compose.analyzers.yml`, with the five application images pinned to their
published digests.

## Deploy now

In GitHub Actions, open **Publish images / Deploy testing**, choose **Run
workflow**, select **develop**, and run it. The action selects the latest build
for the current `develop` commit, requires its backend, frontend and end-to-end
checks, and uses the same publication and deployment jobs as automatic
deployment. It reuses the built images; it does not compile the application
again. A missing or unsuccessful build stops the action, and a newer commit
arriving before container startup stops an obsolete deployment.

Automatic deployments continue after tested pushes to `develop`. Retrying a
commit reuses its existing release directory without replacing files mounted by
running containers.

## Server configuration

The job builds `deploy-bundle.tgz` (both compose files, `volume/`, the analyzer
seed script and the Bridge profiles at the submodule pin). The analyzer
harness's molecular clinical CSVs are test configuration and are not packaged in
the application image. On the VM, `deploy-published-testing.py` unpacks it into
`<site>/releases/<sha>/` and runs Compose as project `openelis-testing`, so
named volumes persist across releases. The site directory (`TESTING_SITE_PATH`,
default `/home/ubuntu/openelis-testing`) holds what belongs to the host:

- `.env` (required): passed as the Compose env file.
- `docker-compose.site.yml` (optional): applied after the release's files, for
  certificates, proxy configuration and other host-specific settings. Compose
  resolves its relative paths against the release, so use absolute paths.
- `lucene/`: the search index, linked into every release.
- `configuration/backend/`: writable catalog files, linked into every release.
  New sites leave this directory empty and load the application's ordinary
  bundled catalog. Clinical tests needed by a site must be supplied through its
  supported catalog configuration. Subsequent deploys preserve uploaded and
  edited files; existing overrides are not removed automatically. The webapp
  entrypoint grants its Tomcat group write access.
- `.openelis-ci/`: the image override and `target.json`.

After the application reports ready, the deploy creates missing default
analyzers (`seed-analyzers.sh --ensure-connections --no-mock-network`) in Setup.
Each analyzer has its own mapping, and the deploy never selects, excludes or
confirms mapping rows or activates a connection. Existing connections retain
their configuration and activation state. On the first deployment, the delivery
check that follows stops because the GeneXpert connection is not active yet:
confirm its mapping **and activate it** in OpenELIS, then retry deployment.
`--ensure-connections` preserves existing connections, including inactive ones.
Deployment does not guess whether a connection was intentionally disabled.

The deploy then sends one GeneXpert result through the mock with an accession
derived from the run ID. The deployment smoke passes when that result appears in
OpenELIS staging; it does not prove clinical mapping, acceptance, or saved
patient result readback. The smoke message uses the shared ASTM listener at port
12001 and the seeded instrument system name `OE2-TEST-GENEXPERT`. When upgrading
an existing test connection, set that instrument system name in its connection
screen before retrying deployment. If testers have disabled the connection or
changed that sender identity, the check fails and leaves their settings intact;
restore that connection in OpenELIS when it is ready to receive the deployment
check. The deploy refuses superseded commits and ports 80/443 owned by any other
Compose project. After success it keeps the current and previous release and
removes unused images.

Configure access with `TESTING_VM_SSH_KEY`, `DEPLOY_HOST`, `TESTING_VM_USER`,
`DEPLOY_PORT`, and `TESTING_SITE_PATH`. `DOCKERHUB_USERNAME` controls the image
namespace.

The site's `.env` also configures the API account used by the Bridge, seeding,
and delivery verification. Set `TEST_USER` and `TEST_PASS` to an existing
OpenELIS account. If unset, `OE_ADMIN_USERNAME` and `OE_ADMIN_PASSWORD` are
used, then the standard testing defaults. These settings do not change the
account's password in OpenELIS. `ASTM_SIMULATOR_HTTP_PORT` (default `8085`) sets
both the mock's loopback port and the delivery check's destination. The deployer
uses Compose's environment parser for these values and passes credentials to the
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

### Reviewing a site that used the former harness catalog

Before an upgrade, inspect `configuration/backend/` on the host. A filesystem
CSV overrides classpath defaults for its entire domain, so changing those files
can change a site's clinical catalog. Older releases shipped these files:

- `dictionaries/analyzer-result-options.csv`
- `sample-types/harness-samples.csv` and
  `sample-types/molecular-sample-types.csv`
- `test-results/harness-test-results.csv`
- `test-sections/harness-sections.csv` and
  `test-sections/molecular-sections.csv`
- `tests/harness-tests.csv` and `tests/molecular-tests.csv`

Compare their contents with the prior deployed release and the site's approved
catalog. Retain intentional site edits and uploaded catalogs. Do not remove a
file merely because a harness once used its name: this deployment does not ship
the harness molecular CSVs as a replacement. If any CSV remains in a domain,
that domain still uses filesystem configuration rather than bundled defaults.
Apply deliberate corrections through the supported catalog workflow and verify
the resulting test identities and analyzer bindings.

Removing a CSV does not delete existing database records or reconcile duplicate
COVID tests. Inspect the resulting catalog and analyzer bindings; resolve any
existing duplicates through the supported catalog workflow before claiming
upgrade success. Do not delete clinical history or repair it with SQL. Run the
stock-default Playwright checks against the upgraded server, then verify native
analyzer delivery and clinical result readback. A clean-install pass does not
prove this populated upgrade.
