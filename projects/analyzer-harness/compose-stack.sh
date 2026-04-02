#!/usr/bin/env bash
# Shared compose layering for analyzer harness local and CI/parity flows.
#
# Contract: DB service `db.openelis.org` uses container_name `openelisglobal-database`
# (see docker-compose.base.yml). Bridge import dir on host:
# `projects/analyzer-harness/volume/analyzer-imports`.

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"

HARNESS_BASE_COMPOSE="$HARNESS_DIR/docker-compose.base.yml"
HARNESS_LOCAL_COMPOSE="$HARNESS_DIR/docker-compose.dev.yml"
HARNESS_ANALYZER_COMPOSE="$HARNESS_DIR/docker-compose.analyzer-test.yml"
HARNESS_LETSENCRYPT_COMPOSE="$HARNESS_DIR/docker-compose.letsencrypt.yml"
CI_BUILD_COMPOSE="$REPO_ROOT/build.docker-compose.yml"
CI_HARNESS_COMPOSE="$REPO_ROOT/.github/ci/ci.analyzer-harness.yml"

compose_args_local() {
  local include_letsencrypt="${1:-true}"
  local -a args=(
    -f "$HARNESS_LOCAL_COMPOSE"
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
