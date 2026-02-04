#!/usr/bin/env bash
#
# Download CI logs for a PR or branch to .cursor/ci-logs/
#
# Usage:
#   ./scripts/download-ci-logs.sh --pr 123
#   ./scripts/download-ci-logs.sh --branch feat/my-feature
#   ./scripts/download-ci-logs.sh --run-id 12345678
#   ./scripts/download-ci-logs.sh --pr 123 --workflow ci.yml
#   ./scripts/download-ci-logs.sh --pr 123 --failed
#   ./scripts/download-ci-logs.sh --pr 123 --list
#
# Requires: gh CLI (authenticated)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Defaults
PR=""
BRANCH=""
RUN_ID=""
WORKFLOW=""
FAILED_ONLY=false
LIST_ONLY=false
LIMIT=10

usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Download CI logs for a PR or branch.

Options:
  --pr <number>       PR number to get logs for
  --branch <name>     Branch name to get logs for
  --run-id <id>       Download a specific run by ID (skips PR/branch lookup)
  --workflow <name>   Filter to specific workflow (e.g., ci.yml, frontend-qa.yml)
  --failed            Only download failed runs
  --list              List available runs without downloading
  --limit <n>         Max runs to list/check (default: 10)
  -h, --help          Show this help

Examples:
  $(basename "$0") --pr 123
  $(basename "$0") --branch develop --workflow ci.yml
  $(basename "$0") --pr 123 --failed --list
  $(basename "$0") --run-id 12345678901
EOF
    exit 0
}

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --pr)
            PR="$2"
            shift 2
            ;;
        --branch)
            BRANCH="$2"
            shift 2
            ;;
        --run-id)
            RUN_ID="$2"
            shift 2
            ;;
        --workflow)
            WORKFLOW="$2"
            shift 2
            ;;
        --failed)
            FAILED_ONLY=true
            shift
            ;;
        --list)
            LIST_ONLY=true
            shift
            ;;
        --limit)
            LIMIT="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Validation
if [[ -z "$PR" && -z "$BRANCH" && -z "$RUN_ID" ]]; then
    log_error "Must specify --pr, --branch, or --run-id"
    usage
fi

# Find repo root (where .cursor is)
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
LOGS_BASE_DIR="$REPO_ROOT/.cursor/ci-logs"
mkdir -p "$LOGS_BASE_DIR"

# Get repo info for API calls
get_repo_info() {
    gh repo view --json nameWithOwner -q '.nameWithOwner' 2>/dev/null
}

# Function to download logs for a single run
download_run_logs() {
    local run_id="$1"
    local output_dir="$2"
    local workflow_name="${3:-run}"

    mkdir -p "$output_dir"

    local summary_file="$output_dir/summary.txt"
    local zip_file="$output_dir/logs.zip"

    # Get run summary (always useful)
    gh run view "$run_id" > "$summary_file" 2>/dev/null || true

    # Get repo name for API call
    local repo
    repo=$(get_repo_info)
    if [[ -z "$repo" ]]; then
        log_warn "Could not determine repo, skipping API log download"
        return 0
    fi

    # Download logs via API (returns ZIP file)
    if gh api "repos/$repo/actions/runs/$run_id/logs" > "$zip_file" 2>/dev/null; then
        # Extract the ZIP
        if unzip -q "$zip_file" -d "$output_dir" 2>/dev/null; then
            rm -f "$zip_file"
            log_info "  Extracted $(find "$output_dir" -name "*.txt" -type f | wc -l) log files"
        else
            log_warn "  Failed to extract logs ZIP"
            rm -f "$zip_file"
        fi
    else
        log_warn "  Logs not available via API (may have expired)"
        rm -f "$zip_file"
    fi

    # Check if we got anything useful
    if [[ -f "$summary_file" ]]; then
        return 0
    else
        return 1
    fi
}

# If run-id specified, download directly
if [[ -n "$RUN_ID" ]]; then
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    OUTPUT_DIR="$LOGS_BASE_DIR/run-$RUN_ID-$TIMESTAMP"

    log_info "Downloading logs for run $RUN_ID..."

    if download_run_logs "$RUN_ID" "$OUTPUT_DIR"; then
        log_success "Logs downloaded to: $OUTPUT_DIR"
        echo ""
        echo "Contents:"
        ls -la "$OUTPUT_DIR"
    else
        log_error "Failed to download logs for run $RUN_ID"
        rmdir "$OUTPUT_DIR" 2>/dev/null || true
        exit 1
    fi
    exit 0
fi

# Build gh run list command
GH_ARGS=(run list --limit "$LIMIT" --json "databaseId,name,status,conclusion,headBranch,event,createdAt,workflowName")

if [[ -n "$PR" ]]; then
    # Get the branch name for this PR
    log_info "Looking up PR #$PR..."
    PR_BRANCH=$(gh pr view "$PR" --json headRefName -q '.headRefName' 2>/dev/null) || {
        log_error "Could not find PR #$PR"
        exit 1
    }
    log_info "PR #$PR is on branch: $PR_BRANCH"
    GH_ARGS+=(--branch "$PR_BRANCH")
    IDENTIFIER="pr-$PR"
elif [[ -n "$BRANCH" ]]; then
    GH_ARGS+=(--branch "$BRANCH")
    IDENTIFIER="branch-${BRANCH//\//-}"
fi

if [[ -n "$WORKFLOW" ]]; then
    GH_ARGS+=(--workflow "$WORKFLOW")
fi

# Fetch runs
log_info "Fetching workflow runs..."
RUNS_JSON=$(gh "${GH_ARGS[@]}" 2>/dev/null) || {
    log_error "Failed to list runs"
    exit 1
}

# Filter to failed only if requested
if [[ "$FAILED_ONLY" == true ]]; then
    RUNS_JSON=$(echo "$RUNS_JSON" | jq '[.[] | select(.conclusion == "failure")]')
fi

RUN_COUNT=$(echo "$RUNS_JSON" | jq 'length')

if [[ "$RUN_COUNT" -eq 0 ]]; then
    log_warn "No workflow runs found matching criteria"
    exit 0
fi

# List mode - just show runs
if [[ "$LIST_ONLY" == true ]]; then
    echo ""
    echo "Available runs:"
    echo "==============="
    echo "$RUNS_JSON" | jq -r '.[] | "[\(.databaseId)] \(.workflowName) - \(.conclusion // .status) (\(.createdAt | split("T")[0]))"'
    echo ""
    echo "To download a specific run:"
    echo "  $(basename "$0") --run-id <id>"
    exit 0
fi

# Get the latest run per workflow (deduplicate by workflowName)
LATEST_RUNS=$(echo "$RUNS_JSON" | jq -r '
    group_by(.workflowName) |
    map(sort_by(.createdAt) | reverse | .[0]) |
    .[]
')

LATEST_COUNT=$(echo "$RUNS_JSON" | jq 'group_by(.workflowName) | length')

log_info "Found $LATEST_COUNT unique workflow(s) to download"

# Create output directory
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="$LOGS_BASE_DIR/$IDENTIFIER-$TIMESTAMP"
mkdir -p "$OUTPUT_DIR"

# Download each workflow's latest run
echo "$RUNS_JSON" | jq -c 'group_by(.workflowName) | map(sort_by(.createdAt) | reverse | .[0]) | .[]' | while read -r run; do
    RUN_ID=$(echo "$run" | jq -r '.databaseId')
    WORKFLOW_NAME=$(echo "$run" | jq -r '.workflowName')
    CONCLUSION=$(echo "$run" | jq -r '.conclusion // .status')

    # Sanitize workflow name for directory
    SAFE_NAME=$(echo "$WORKFLOW_NAME" | tr ' /' '_-' | tr -cd '[:alnum:]_-')
    RUN_DIR="$OUTPUT_DIR/$SAFE_NAME-$RUN_ID"

    log_info "Downloading: $WORKFLOW_NAME (run $RUN_ID, $CONCLUSION)..."

    if download_run_logs "$RUN_ID" "$RUN_DIR" "$WORKFLOW_NAME"; then
        log_success "  → $RUN_DIR"
    else
        log_warn "  → Failed to download logs (may have expired)"
        rmdir "$RUN_DIR" 2>/dev/null || true
    fi
done

echo ""
log_success "Logs downloaded to: $OUTPUT_DIR"
echo ""

# Show directory tree
echo "Contents:"
find "$OUTPUT_DIR" -type d -mindepth 1 -maxdepth 2 2>/dev/null | sort | while read -r d; do
    DEPTH=$(echo "$d" | sed "s|$OUTPUT_DIR||" | tr -cd '/' | wc -c)
    DIRNAME=$(basename "$d")
    if [[ $DEPTH -eq 1 ]]; then
        echo "  $DIRNAME/"
    else
        echo "      $DIRNAME/"
    fi
done

# Count total log files
TOTAL_FILES=$(find "$OUTPUT_DIR" -name "*.txt" -type f 2>/dev/null | wc -l)
TOTAL_SIZE=$(du -sh "$OUTPUT_DIR" 2>/dev/null | cut -f1)
echo ""
echo "  Total: $TOTAL_FILES log files ($TOTAL_SIZE)"

echo ""
echo "Quick commands:"
echo "  # View summaries"
echo "  cat '$OUTPUT_DIR'/*/summary.txt"
echo ""
echo "  # View failed steps"
echo "  cat '$OUTPUT_DIR'/*/failed-steps.txt 2>/dev/null"
echo ""
echo "  # View specific job logs"
echo "  ls '$OUTPUT_DIR'/*/jobs/"
echo ""
echo "  # Search for errors"
echo "  grep -rn 'ERROR\\|FAILED\\|Exception' '$OUTPUT_DIR'/*/jobs/"
