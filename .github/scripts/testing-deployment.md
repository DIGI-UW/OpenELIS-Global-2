`Publish Images` deploys tested `develop` image digests using the VM's
`/home/ubuntu/openelis-docker/docker-compose.yml`. It preserves the site's
`.env` and volumes, rejects superseded commits and competing stacks on ports
80/443, and verifies running images and JSON application health.

Configure access with `TESTING_VM_SSH_KEY`, `DEPLOY_HOST`, `TESTING_VM_USER`,
`DEPLOY_PORT`, and `DEPLOY_PATH`. `DOCKERHUB_USERNAME` controls the image
namespace. Optional readiness variables are:

| Variable                     | Default                                                          |
| ---------------------------- | ---------------------------------------------------------------- |
| `TESTING_READINESS_URL`      | `https://testing.openelis-global.org/api/OpenELIS-Global/health` |
| `TESTING_READINESS_TIMEOUT`  | `300` seconds                                                    |
| `TESTING_READINESS_JSON_KEY` | `status` (supports dotted nested keys)                           |
| `TESTING_READINESS_EXPECTED` | `"UP"` (JSON-encoded value)                                      |

Diagnostics include Compose status, logs for all five services, readiness, and
the previous image selection. Successful deployment records
`.openelis-ci/target.json`. Rollback is manual because older images may be
incompatible with applied database migrations. Images and orphan services are
retained.

If an interrupted update leaves `.openelis-ci/server.env.backup`, restore `.env`
from that private backup, verify the configuration, and remove the backup before
retrying. New upstream environment defaults require explicit site configuration.

The host lock covers this deployment script only. Do not run the harness
deployer or the infrastructure repository's manual deployment workflow on the
same VM.

Run the focused checks from the repository root:

```sh
node --test .github/scripts/publish-checkpoints.test.cjs
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
```

The Python tests require PyYAML and use temporary localhost HTTP servers. Docker
operations are mocked; the tests do not deploy to the testing VM.
