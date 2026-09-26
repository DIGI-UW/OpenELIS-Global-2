`Publish Images` deploys each tested `develop` commit to the testing VM using
that commit's own `docker-compose.yml` and `docker-compose.analyzers.yml`, with
the five application images pinned to their published digests.

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
- `.openelis-ci/`: the image override and `target.json`.

After the application reports ready, the deploy seeds the default analyzers
(`seed-analyzers.sh --ensure-connections --no-mock-network --activate`), then
sends one GeneXpert result through the mock with an accession derived from the
run ID. The deployment is ready only when that result appears in OpenELIS.
The deploy refuses superseded commits and ports 80/443 owned by any other
Compose project. After success it keeps the current and previous release and
removes unused images.

Configure access with `TESTING_VM_SSH_KEY`, `DEPLOY_HOST`, `TESTING_VM_USER`,
`DEPLOY_PORT`, and `TESTING_SITE_PATH`. `DOCKERHUB_USERNAME` controls the image
namespace. Optional readiness variables are:

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
