#!/usr/bin/env bash
# Shared compose layering for analyzer harness local and CI/parity flows.
#
# Contract: DB service `db.openelis.org` uses container_name `openelisglobal-database`
# (see compose.yaml). Bridge import dir on host:
# `projects/analyzer-harness/volume/analyzer-imports`.
#
# After the Docker modernization (chore/docker-compose-modernization), the
# harness base + dev were merged into a single `compose.yaml`. Analyzer E2E
# services live in `compose.harness.yaml` under the `harness` profile.

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"

HARNESS_BASE_COMPOSE="$HARNESS_DIR/compose.yaml"
HARNESS_ANALYZER_COMPOSE="$HARNESS_DIR/compose.harness.yaml"
HARNESS_LETSENCRYPT_COMPOSE="$HARNESS_DIR/compose.letsencrypt.yaml"
CI_BUILD_COMPOSE="$REPO_ROOT/compose.build.yaml"
CI_HARNESS_COMPOSE="$REPO_ROOT/.github/ci/ci.analyzer-harness.yml"

# Emit compose args for local dev / CI-parity runs. Callers should also pass
# `--profile harness` to activate the analyzer services.
compose_args_local() {
  local include_letsencrypt="${1:-true}"
  local -a args=(
    -f "$HARNESS_BASE_COMPOSE"
    -f "$HARNESS_ANALYZER_COMPOSE"
  )

  if [[ "$include_letsencrypt" == "true" ]]; then
    args+=(-f "$HARNESS_LETSENCRYPT_COMPOSE")
  fi

  printf '%s\n' "${args[@]}"
}

compose_args_ci() {
  local -a args=(
    -f "$CI_BUILD_COMPOSE"
    -f "$HARNESS_BASE_COMPOSE"
    -f "$CI_HARNESS_COMPOSE"
  )

  printf '%s\n' "${args[@]}"
}
