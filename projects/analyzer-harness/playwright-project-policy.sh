#!/usr/bin/env bash

resolve_harness_playwright_project() {
  local mode="$1"
  local requested_project="${2:-}"

  if [[ -n "$requested_project" ]]; then
    validate_harness_playwright_project "$requested_project"
    return
  fi
  if [[ "$mode" == "video" ]]; then
    echo "harness-demo-video"
  else
    echo "harness-demo"
  fi
}

validate_harness_playwright_project() {
  local project="$1"
  case "$project" in
    harness-foundational|harness-demo|harness-demo-video)
      echo "$project"
      ;;
    *)
      echo "ERROR: unsupported project '$project' (expected harness-foundational, harness-demo, or harness-demo-video)" >&2
      return 2
      ;;
  esac
}
