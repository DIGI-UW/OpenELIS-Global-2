# PRFlow Phase 1 scripts

Bash helpers used by `.github/workflows/prflow-*.yml`. They only call the GitHub API (`gh`) — safe for `pull_request_target`.

## Local smoke test

```bash
export REPO="DIGI-UW/OpenELIS-Global-2"   # or your fork
gh auth status

# Digest (read-only, prints markdown)
TOP_N=5 bash scripts/prflow/digest.sh

# Classify one PR
bash scripts/prflow/classify.sh 3432

# Dependabot / behind (posts comments — use a test PR on your fork)
# bash scripts/prflow/dependabot-ready.sh <pr>
# bash scripts/prflow/behind-nudge.sh <pr>
```

## Repository variable (optional)

Set **`PRFLOW_DIGEST_ISSUE`** (repo variable) to an issue number to append/update a daily digest comment. If unset, digest only appears in the Actions job summary.
