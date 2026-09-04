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
    harness-demo|harness-mvp|harness-demo-video)
      echo "$project"
      ;;
    *)
      echo "ERROR: unsupported project '$project' (expected harness-demo, harness-mvp, or harness-demo-video)" >&2
      return 2
      ;;
  esac
}

assert_harness_project_has_specs() {
  local repo_root="$1"
  local project="$2"
  local spec_dir

  if [[ "$project" == "harness-mvp" || "$project" == "harness-demo-video" ]]; then
    local final_mvp_spec="$repo_root/frontend/playwright/tests/demo/harness/ogc-1054-analyzer-mvp.spec.ts"
    if [[ ! -f "$final_mvp_spec" ]]; then
      echo "ERROR: project '$project' has no final analyzer MVP demo spec" >&2
      return 2
    fi
    return
  fi

  case "$project" in
    harness-demo)
      spec_dir="$repo_root/frontend/playwright/tests/demo/harness"
      ;;
    *)
      validate_harness_playwright_project "$project" >/dev/null
      return
      ;;
  esac

  if ! find "$spec_dir" -type f -name '*.spec.ts' -print -quit 2>/dev/null | grep -q .; then
    echo "ERROR: project '$project' has no analyzer demo specs" >&2
    return 2
  fi
}
