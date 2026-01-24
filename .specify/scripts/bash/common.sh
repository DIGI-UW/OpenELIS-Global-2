#!/usr/bin/env bash
# Common functions and variables for all scripts

# Get repository root, with fallback for non-git repositories
get_repo_root() {
    if git rev-parse --show-toplevel >/dev/null 2>&1; then
        git rev-parse --show-toplevel
    else
        # Fall back to script location for non-git repos
        local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        (cd "$script_dir/../../.." && pwd)
    fi
}

# Get current branch, with fallback for non-git repositories
get_current_branch() {
    # First check if SPECIFY_FEATURE environment variable is set
    if [[ -n "${SPECIFY_FEATURE:-}" ]]; then
        echo "$SPECIFY_FEATURE"
        return
    fi

    # Then check git if available
    if git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
        git rev-parse --abbrev-ref HEAD
        return
    fi

    # For non-git repos, try to find the latest feature directory
    local repo_root=$(get_repo_root)
    local specs_dir="$repo_root/specs"

    if [[ -d "$specs_dir" ]]; then
        local latest_feature=""
        local highest=0

        for dir in "$specs_dir"/*; do
            if [[ -d "$dir" ]]; then
                local dirname=$(basename "$dir")
                if [[ "$dirname" =~ ^([0-9]{3})- ]]; then
                    local number=${BASH_REMATCH[1]}
                    number=$((10#$number))
                    if [[ "$number" -gt "$highest" ]]; then
                        highest=$number
                        latest_feature=$dirname
                    fi
                fi
            fi
        done

        if [[ -n "$latest_feature" ]]; then
            echo "$latest_feature"
            return
        fi
    fi

    echo "main"  # Final fallback
}

# Check if we have git available
has_git() {
    git rev-parse --show-toplevel >/dev/null 2>&1
}

check_feature_branch() {
    local branch="$1"
    local has_git_repo="$2"

    # For non-git repos, we can't enforce branch naming but still provide output
    if [[ "$has_git_repo" != "true" ]]; then
        echo "[specify] Warning: Git repository not detected; skipped branch validation" >&2
        return 0
    fi

    # Support flexible branch patterns (Constitution Principle IX):
    #
    # Legacy format (still supported):
    # - 001-feature-name
    # - fix/001-feature-name
    # - hotfix/001-bug-fix
    #
    # New Principle IX format:
    # - spec/OGC-009-sidenav or spec/009-sidenav
    # - feat/OGC-009-sidenav
    # - feat/OGC-009-sidenav/m1-core (milestone branch)
    # - hotfix/OGC-123-fix-login
    # - fix/OGC-456-null-check
    #
    # Pattern matches:
    # 1. NNN- at start or after / (legacy)
    # 2. OGC-NNN- or similar Jira prefix after / (new)
    # 3. spec/, feat/, fix/, hotfix/ prefixes with issue ID
    if [[ "$branch" =~ (^|/)[0-9]{3}- ]] || \
       [[ "$branch" =~ ^(spec|feat|fix|hotfix)/[A-Z]+-[0-9]+- ]] || \
       [[ "$branch" =~ ^(spec|feat|fix|hotfix)/[0-9]{3}- ]]; then
        return 0
    fi

    echo "ERROR: Not on a feature branch. Current branch: $branch" >&2
    echo "Feature branches should match one of these patterns:" >&2
    echo "" >&2
    echo "  Legacy format:" >&2
    echo "    - 001-feature-name" >&2
    echo "    - fix/001-feature-name" >&2
    echo "" >&2
    echo "  Principle IX format (Jira: OGC-###, GitHub: ###):" >&2
    echo "    - spec/OGC-009-sidenav or spec/009-sidenav" >&2
    echo "    - feat/OGC-009-sidenav" >&2
    echo "    - feat/OGC-009-sidenav/m1-core (milestone)" >&2
    echo "    - hotfix/OGC-123-fix-login" >&2
    echo "    - fix/OGC-456-null-check" >&2
    return 1
}

get_feature_dir() { echo "$1/specs/$2"; }

# Find feature directory by numeric prefix instead of exact branch match
# This allows multiple branches to work on the same spec (e.g., 004-fix-bug, 004-add-feature)
#
# Supports (Constitution Principle IX):
# - Legacy: 001-feature, fix/001-feature, hotfix/001-bug
# - New: spec/OGC-009-sidenav, feat/OGC-009-sidenav, feat/OGC-009-sidenav/m1-core
find_feature_dir_by_prefix() {
    local repo_root="$1"
    local branch_name="$2"
    local specs_dir="$repo_root/specs"
    local prefix=""

    # Extract numeric prefix from branch
    # Priority order for pattern matching:
    #
    # 1. Principle IX Jira format: spec/OGC-009-sidenav, feat/OGC-009-sidenav/m1-core
    #    Extract "009" from "OGC-009"
    if [[ "$branch_name" =~ ^(spec|feat|fix|hotfix)/[A-Z]+-([0-9]+)- ]]; then
        prefix="${BASH_REMATCH[2]}"
        # Pad to 3 digits if needed (009, not 9)
        prefix=$(printf "%03d" "$((10#$prefix))")
    # 2. Principle IX GitHub format: spec/009-sidenav, feat/009-sidenav/m1-core
    elif [[ "$branch_name" =~ ^(spec|feat|fix|hotfix)/([0-9]{3})- ]]; then
        prefix="${BASH_REMATCH[2]}"
    # 3. Legacy format: 004-whatever, fix/004-whatever
    elif [[ "$branch_name" =~ (^|/)([0-9]{3})- ]]; then
        prefix="${BASH_REMATCH[2]}"
    fi

    # If no prefix found, fall back to exact match
    if [[ -z "$prefix" ]]; then
        echo "$specs_dir/$branch_name"
        return
    fi

    # Search for directories in specs/ that start with this prefix
    local matches=()
    if [[ -d "$specs_dir" ]]; then
        # Support both numeric-prefixed folders (e.g., 150-foo) and Jira-style folders (e.g., OGC-150-foo)
        for dir in "$specs_dir"/"$prefix"-* "$specs_dir"/OGC-"$prefix"-*; do
            if [[ -d "$dir" ]]; then
                matches+=("$(basename "$dir")")
            fi
        done
    fi

    # Handle results
    if [[ ${#matches[@]} -eq 0 ]]; then
        # No match found - return the branch name path (will fail later with clear error)
        echo "$specs_dir/$branch_name"
    elif [[ ${#matches[@]} -eq 1 ]]; then
        # Exactly one match - perfect!
        echo "$specs_dir/${matches[0]}"
    else
        # Multiple matches - this shouldn't happen with proper naming convention
        echo "ERROR: Multiple spec directories found with prefix '$prefix': ${matches[*]}" >&2
        echo "Please ensure only one spec directory exists per numeric prefix." >&2
        echo "$specs_dir/$branch_name"  # Return something to avoid breaking the script
    fi
}

get_feature_paths() {
    local repo_root=$(get_repo_root)
    local current_branch=$(get_current_branch)
    local has_git_repo="false"

    if has_git; then
        has_git_repo="true"
    fi

    # Use prefix-based lookup to support multiple branches per spec
    local feature_dir=$(find_feature_dir_by_prefix "$repo_root" "$current_branch")

    cat <<EOF
REPO_ROOT='$repo_root'
CURRENT_BRANCH='$current_branch'
HAS_GIT='$has_git_repo'
FEATURE_DIR='$feature_dir'
FEATURE_SPEC='$feature_dir/spec.md'
IMPL_PLAN='$feature_dir/plan.md'
TASKS='$feature_dir/tasks.md'
RESEARCH='$feature_dir/research.md'
DATA_MODEL='$feature_dir/data-model.md'
QUICKSTART='$feature_dir/quickstart.md'
CONTRACTS_DIR='$feature_dir/contracts'
EOF
}

check_file() { [[ -f "$1" ]] && echo "  ✓ $2" || echo "  ✗ $2"; }
check_dir() { [[ -d "$1" && -n $(ls -A "$1" 2>/dev/null) ]] && echo "  ✓ $2" || echo "  ✗ $2"; }
