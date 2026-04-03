# E2E CI Operator Model

This guide explains trust boundaries, PR-facing checkpoints, and the
repo/operator settings required for the split E2E workflow model.

## Workflow Boundary Model

- `03 - E2E` is build-only. It produces transient `e2e-cache` images for
  non-forks and emits build-context artifacts for downstream orchestrators.
- `E2E / Tests` is the non-fork `workflow_run` wrapper. It owns the stable
  `03 Checkpoint - E2E` status context and keeps the detailed job graph behind
  that single PR-facing contract.
- `03 - E2E - Fork PR` is the privileged `pull_request_target` executor. It owns
  the authoritative fork-only status context `03 Checkpoint - E2E - Fork PR`.
- `Publish Images` is post-merge only (`push`/`release`) and gated on the
  successful `03 Checkpoint - E2E` status.

## What Changes Apply Pre-Merge vs Post-Merge

Pre-merge (picked up from checked-out PR payload):

- Scripts and test code executed in job steps
- Compose files and fixture SQL loaded by tests
- Submodule pointers in checked-out PR refs

Requires default-branch merge (controls `workflow_run` orchestration):

- Workflow topology and wrapper/job wiring
- Job-level permission model
- Cross-workflow artifact plumbing
- Trigger conditions for `workflow_run` pipelines

## Local Reusable Workflow Resolution

- `uses: ./.github/workflows/<file>.yml` resolves from the checked-out commit in
  that job.
- In `workflow_run` contexts, orchestration behavior still depends on default
  branch workflow definitions; this is why merge validation is required.

## Fork vs Non-Fork Safety Model

- Non-fork PRs: `03 - E2E` publishes GHCR cache directly, then `E2E / Tests`
  consumes those artifacts and reports `03 Checkpoint - E2E`.
- Fork PRs: `03 - E2E` records `fork-fallback`, `E2E / Tests` reports
  `03 Checkpoint - E2E` as `not_applicable`, and `03 Checkpoint - E2E - Fork PR`
  becomes the authoritative executor after maintainer approval.
- Synthetic checkpoint reporting uses commit statuses, not checks, so the PR
  rows can link directly to the authoritative workflow graphs.
- Do not publish a check and a status with the same checkpoint name; GitHub can
  treat both as required if branch protection references that shared name.

## Branch Protection Contract

- Preferred operator rollout for this topology:
  - require `03 Checkpoint - E2E`
  - require `03 Checkpoint - E2E - Fork PR`
- Expected behavior:
  - non-fork PRs: `03 Checkpoint - E2E` does real work,
    `03 Checkpoint - E2E - Fork PR` returns `not_applicable`
  - fork PRs: `03 Checkpoint - E2E - Fork PR` does real work,
    `03 Checkpoint - E2E` returns `not_applicable`
- Manual GitHub ruleset updates are the safe rollout path when CLI automation is
  unreliable.

## CI Auth Contract

Use this contract in all E2E test executors:

- `TEST_USER: ${{ vars.TEST_USER || 'admin' }}`
- `TEST_PASS: ${{ vars.TEST_PASS || 'adminADMIN!' }}`

No E2E PR executor should require `secrets.TEST_PASS`.

## Publish Environment Contract

- DockerHub publish credentials are scoped to environment `publish`.
- Environment branch policy must restrict deployments to `develop`.

## GitHub Configuration — Automation First

Run from repo root with sufficient admin permissions:

```bash
# Ensure GH CLI is authenticated for repo admin APIs
gh auth status

# Repository-level CI vars
gh variable set TEST_USER --body "admin" --repo DIGI-UW/OpenELIS-Global-2
gh variable set TEST_PASS --body "adminADMIN!" --repo DIGI-UW/OpenELIS-Global-2

# Optional: DockerHub username as repo variable
gh variable set DOCKERHUB_USERNAME --body "<dockerhub-username>" --repo DIGI-UW/OpenELIS-Global-2

# Create or update environment "publish"
gh api --method PUT repos/DIGI-UW/OpenELIS-Global-2/environments/publish

# Enable custom deployment branch policies on publish environment
printf '%s' '{"deployment_branch_policy":{"protected_branches":false,"custom_branch_policies":true}}' \
  | gh api --method PUT repos/DIGI-UW/OpenELIS-Global-2/environments/publish --input -

# Restrict publish environment to develop branch
gh api --method POST repos/DIGI-UW/OpenELIS-Global-2/environments/publish/deployment-branch-policies \
  -f name=develop \
  -f type=branch
```

Environment secrets are repo-admin controlled and may require dedicated
commands/permissions:

```bash
# Example for environment-scoped secret (requires admin + proper GH CLI setup)
gh secret set DOCKERHUB_TOKEN --env publish --repo DIGI-UW/OpenELIS-Global-2
```

## Manual Fallback (If API/Permissions Block Automation)

1. Open repo settings: `Settings` -> `Environments` -> `publish`.
2. Create environment `publish` if missing.
3. Under deployment branches, allow only selected branches and set `develop`.
4. Add environment secret `DOCKERHUB_TOKEN`.
5. Confirm repo variables:
   - `TEST_USER=admin`
   - `TEST_PASS=adminADMIN!`
   - `DOCKERHUB_USERNAME=<dockerhub-username>`

## Verification Checklist

```bash
# Confirm repo variables exist
gh variable list --repo DIGI-UW/OpenELIS-Global-2

# Confirm publish environment exists and branch policy is applied
gh api repos/DIGI-UW/OpenELIS-Global-2/environments/publish
gh api repos/DIGI-UW/OpenELIS-Global-2/environments/publish/deployment-branch-policies

# Confirm relevant workflows are present in current branch
ls .github/workflows/e2e-*.yml .github/workflows/publish-images.yml
```
