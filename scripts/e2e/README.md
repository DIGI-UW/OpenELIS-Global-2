# E2E CI helper scripts

Shared bash entrypoints used by `.github/workflows/e2e-*.yml` so bootstrap and
readiness checks stay consistent between fork and non-fork paths.

| Script                                   | Purpose                                                                         |
| ---------------------------------------- | ------------------------------------------------------------------------------- |
| `pull-retag-e2e-images.sh`               | Pull GHCR-staged images and retag to local compose names (`E2E_IMAGE_MAP_FILE`) |
| `wait-for-openelis-login.sh`             | Block until `projects/analyzer-harness/scripts/verify-login.sh` succeeds        |
| `wait-for-analyzer-harness-readiness.sh` | Login gate + bridge health + ASTM simulator health (harness jobs)               |
