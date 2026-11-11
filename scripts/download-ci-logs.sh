#!/usr/bin/env bash
#
# Download CI logs for a PR or branch to .cursor/ci-logs/
#
# Usage:
#   ./scripts/download-ci-logs.sh --pr 123
#   ./scripts/download-ci-logs.sh --branch feat/my-feature
#   ./scripts/download-ci-logs.sh --run-id 12345678
#   ./scripts/download-ci-logs.sh --pr 123 --workflow backend.yml
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

usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Download CI logs for a PR or branch.

Options:
  --pr <number>       PR number to get logs for
  --branch <name>     Branch whose newest commit's checks to get logs for
  --run-id <id>       Download a specific run by ID (skips PR/branch lookup)
  --workflow <name>   Filter to specific workflow (e.g., backend.yml, frontend.yml, e2e-playwright.yml)
  --failed            Only download failed runs
  --list              List available runs without downloading
  -h, --help          Show this help

Examples:
  $(basename "$0") --pr 123
  $(basename "$0") --branch develop --workflow e2e-playwright.yml
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

# Runs come from the commit's statusCheckRollup, the checks GitHub shows on it.
# Listing runs by branch shows something else: runs started by workflow_run
# (E2E / Tests) and Dependabot are filed under develop's newest commit,
# whichever PR they test, and a PR branch's own listing has no E2E / Tests run.
REPO="$(get_repo_info)"
if [[ -n "$PR" ]]; then
    log_info "Looking up PR #$PR..."
    REF=$(gh pr view "$PR" --json headRefOid -q '.headRefOid' 2>/dev/null) || {
        log_error "Could not find PR #$PR"
        exit 1
    }
    IDENTIFIER="pr-$PR"
else
    REF="$BRANCH"
    IDENTIFIER="branch-${BRANCH//\//-}"
fi

log_info "Reading the checks GitHub shows on $REF..."
# A commit re-run many times carries more than 100 contexts, so every page is
# read before the newest run of each check is chosen.
PAGES=$(gh api graphql --paginate -F owner="${REPO%%/*}" -F name="${REPO#*/}" -F ref="$REF" -f query='
query($owner: String!, $name: String!, $ref: String!, $endCursor: String) {
  repository(owner: $owner, name: $name) {
    object(expression: $ref) {
      ... on Commit {
        statusCheckRollup {
          contexts(first: 100, after: $endCursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
              __typename
              ... on CheckRun { name status conclusion startedAt detailsUrl
                checkSuite { workflowRun { workflow { name resourcePath } } } }
              ... on StatusContext { context state createdAt targetUrl }
            }
          }
        }
      }
    }
  }
}' 2>/dev/null) || {
    log_error "Failed to read the checks on $REF"
    exit 1
}
# STALE is a check GitHub no longer trusts and shows as pending, as gh pr checks does.
RUNS_JSON=$(printf '%s' "$PAGES" | jq -s '
    def pending: test("^(pending|queued|in_progress|expected|waiting|requested|stale)$");
    def failed: test("^(failure|error|cancelled|timed_out|action_required|startup_failure)$");
    [.[] | (.data.repository.object.statusCheckRollup.contexts.nodes // [])[]
     | if .__typename == "CheckRun" then
         {url: (.detailsUrl // ""),
          workflowName: (.checkSuite.workflowRun.workflow.name // .name),
          workflowPath: (.checkSuite.workflowRun.workflow.resourcePath // ""),
          checkName: .name,
          result: ((if (.conclusion // "") == "" then .status else .conclusion end) | ascii_downcase),
          createdAt: (.startedAt // "")}
       else
         {url: (.targetUrl // ""), workflowName: .context, workflowPath: "", checkName: .context,
          result: (.state | ascii_downcase), createdAt: (.createdAt // "")}
       end
     | . + {databaseId: ([.url | match("/actions/runs/([0-9]+)").captures[0].string][0])}
     | select(.databaseId != null)]
    # A re-run leaves the earlier run of a check on the commit; GitHub shows
    # only the newest run of each check name.
    | group_by([.workflowName, .checkName]) | map(max_by(.createdAt))
    | group_by(.databaseId)
    | map({databaseId: (.[0].databaseId | tonumber),
           workflowName: ((map(select(.workflowPath != "")) + .)[0].workflowName),
           workflowPath: (map(.workflowPath) | max),
           createdAt: (map(.createdAt) | min),
           status: (if any(.result | pending) then "in_progress" else "completed" end),
           conclusion: (if any(.result | failed) then "failure"
                        elif any(.result | pending) then null
                        else "success" end)})')

if [[ -n "$WORKFLOW" ]]; then
    RUNS_JSON=$(echo "$RUNS_JSON" | jq --arg w "$WORKFLOW" \
        '[.[] | select(.workflowName == $w or (.workflowPath | endswith("/" + $w)))]')
fi

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
