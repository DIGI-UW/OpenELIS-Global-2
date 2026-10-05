#!/usr/bin/env bash
# Keep Docker API clients (including Java tests) on the CLI's selected endpoint.
configure_docker_environment() {
  docker info >/dev/null
  if [[ -n "${DOCKER_CONTEXT:-}" ]]; then
    DOCKER_HOST="$(docker context inspect "$DOCKER_CONTEXT" --format '{{.Endpoints.docker.Host}}')" || return
  elif [[ -z "${DOCKER_HOST:-}" ]]; then
    DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')" || return
  fi
  export DOCKER_HOST
}
