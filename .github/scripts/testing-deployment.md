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
seed script, shared harness catalog initializer, molecular catalog configuration
and the Bridge profiles at the submodule pin). On the VM,
`deploy-published-testing.py` unpacks it into `<site>/releases/<sha>/` and runs
Compose as project `openelis-testing`, so named volumes persist across releases.
The site directory (`TESTING_SITE_PATH`, default
`/home/ubuntu/openelis-testing`) holds what belongs to the host:

- `.env` (required): passed as the Compose env file.
- `docker-compose.site.yml` (optional): applied after the release's files, for
  certificates, proxy configuration and other host-specific settings. Compose
  resolves its relative paths against the release, so use absolute paths.
- `lucene/`: the search index, linked into every release.
- `configuration/backend/`: writable catalog files, linked into every release.
  The shared `harness-catalog-init` service copies the same molecular tests and
  result configuration used in core development and CI, before OE2 starts. Other
  catalog domains use bundled application defaults. Ordinary deployments
  preserve edited files. The nightly reset restores the versioned baseline.
- `.openelis-ci/`: the image override and `target.json`.

After the application reports ready, the deploy creates missing default
analyzers (`seed-analyzers.sh --ensure-connections --no-mock-network`). It never
selects, excludes or confirms mapping rows. Existing connections retain their
configuration and activation state. New connections await the normal lab-facing
review and activation workflow, just as in `scripts/dev-stack`.

CI owns new-analyzer creation, default-mapping and clinical result workflow
validation. Deployment checks image identity and application readiness, then
initializes the harness connections. It does not send synthetic traffic through
a tester's existing analyzer or require testers to approve mappings before a
deployment can finish. The deploy refuses superseded commits and ports 80/443
owned by any other Compose project. After success it keeps the current and
previous release and removes unused images.

Configure access with `TESTING_VM_SSH_KEY`, `DEPLOY_HOST`, `TESTING_VM_USER`,
`DEPLOY_PORT`, and `TESTING_SITE_PATH`. `DOCKERHUB_USERNAME` controls the image
namespace.

The site's `.env` also configures the API account used by the Bridge, seeding,
and connection initialization. Set `TEST_USER` and `TEST_PASS` to an existing
OpenELIS account. If unset, `OE_ADMIN_USERNAME` and `OE_ADMIN_PASSWORD` are
used, then the standard testing defaults. These settings do not change the
account's password in OpenELIS. `ASTM_SIMULATOR_HTTP_PORT` (default `8085`) sets
the mock's loopback port. The deployer uses Compose's environment parser for
these values and passes credentials to the seed subprocess through its
environment, not command arguments.

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

## Nightly clean baseline

After a successful deployment, the workflow installs the repository-owned reset
tools and systemd units. `openelis-testing-reset.timer` runs daily at **18:00
America/Los_Angeles** (6 p.m. Pacific, including daylight-saving changes).
`Persistent=false` avoids a surprise daytime reset after a missed run or reboot.
Ordinary deployments preserve data and never invoke the reset.

The reset uses the recorded deployed release and every container's actual image
ID. It does not fetch develop, pull new images, rebuild or change the software
version. It shares `/tmp/openelis-testing-deploy.lock` with deployment. It
removes only the testing project's application/FHIR database, Bridge state and
queues, analyzer-import files and reporting data, and clears the site's catalog
overrides and search index. Certificates, keys, `.env` and host configuration
remain. The shared harness initializer restores catalog configuration, normal
application startup populates the database, and the existing setup API script
creates fresh analyzers. No SQL fixtures, mapping repairs or manufactured
confirmations run.

The baseline contains the application's initial reference/demo data and four
analyzer instances: GeneXpert, FluoroCycler, QuantStudio 5 and QuantStudio 7.
Fresh instances await ordinary review and activation in the UI. Testing uses
published application images, the pinned Bridge/mock, and shared harness catalog
initialization. Source mounts, CI-only fixture services and development-only
scenario endpoints are not required for manual analyzer use.

Visit `/testing-baseline/` for the deployed version, last successful reset and
latest reset outcome. JSON is available at `/testing-baseline/status.json`. Full
errors remain in `journalctl -u openelis-testing-reset.service`, not in the
public page. Failed resets are reported as failed, and old staging-delivery
proof is removed because it refers to discarded data.

On the testing VM (replace the site path if configured differently):

```sh
python3 /usr/local/lib/openelis-testing/reset-testing.py plan /home/ubuntu/openelis-testing
python3 /usr/local/lib/openelis-testing/reset-testing.py reset /home/ubuntu/openelis-testing --yes
python3 /usr/local/lib/openelis-testing/reset-testing.py status /home/ubuntu/openelis-testing
python3 /usr/local/lib/openelis-testing/reset-testing.py skip-next /home/ubuntu/openelis-testing
systemctl list-timers openelis-testing-reset.timer
```

`skip-next` retains a reproduction for one night; manual resets still work.
Disable the timer with
`sudo systemctl disable --now openelis-testing-reset.timer` for a longer pause.
A concurrent deployment/reset is rejected before destructive work; no second
reset is queued. The workflow needs passwordless `sudo` for installing these
testing-only units. Installation itself never clears data.
