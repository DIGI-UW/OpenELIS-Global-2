# Address PR Comments

When the user invokes `/address-pr-comments` (optionally with a PR number),
perform a structured, interactive workflow to triage and address all review
comments on a pull request. The workflow:

- **Fetches** all inline review comments, general comments, and thread
  resolution status using GitHub's REST and GraphQL APIs
- **Classifies** each unresolved comment into a triage category (blocking, bug,
  suggestion, question, nitpick, praise, outdated, out-of-scope) with LLM
  reasoning, then presents the triage table for user override
- **Runs a QA session** per comment in priority order, where the user decides:
  fix, defer, reply, skip, or mark as already addressed
- **Batches actions**: formats and commits code changes, posts reply comments,
  resolves threads, and optionally posts a PR summary comment

This command is intentionally interactive: every action (commit, reply, resolve)
requires explicit user confirmation.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/address-pr-comments` → auto-detect PR for current branch
- `/address-pr-comments 2767` → target PR #2767
- `/address-pr-comments --unresolved-only` → skip already-resolved threads
- `/address-pr-comments --author ibacher` → filter to comments by a specific
  reviewer
- `/address-pr-comments --category blocking,bug` → filter to specific categories
  after classification

If any inputs are unclear, ask a single concise question and continue with safe
defaults.

## Safety Rules (non-negotiable)

- **Never resolve a thread without posting a reply first.** Every resolution
  needs an audit trail — even if just "Fixed in [commit ref]."
- **Never force-push.** Changes are committed normally; the user pushes when
  ready.
- **Never auto-resolve without per-comment confirmation.** Each thread gets its
  own "Resolve this thread? [y/n]" prompt during the QA session.
- **Never post replies without user confirmation.** All drafted replies are
  shown for review before posting. The user can edit any reply before it is
  sent.
- **Never auto-commit.** Stage changes and present them for user confirmation.
  The user writes or approves the commit message.
- **Respect the reviewer.** Draft replies must be professional, reference
  specific commits or reasoning, and never be dismissive. Follow Google
  Engineering Practices: clarify the code itself (rename, restructure, add
  comments), not just explain in the review tool.

## Workflow

### 0) Preflight (gather facts, no changes)

Run these and summarize the results:

- `git rev-parse --show-toplevel` (verify repo root)
- `git status --porcelain` (warn on uncommitted changes — suggest stashing)
- `git branch --show-current`

Determine the PR number:

- If provided as argument, use it directly
- Otherwise, auto-detect: `gh pr view --json number -q '.number'`
- If no PR exists for the current branch, stop and ask for a PR number

Fetch PR metadata:

```bash
gh pr view {num} --json number,title,body,baseRefName,headRefName,author,reviews,reviewDecision
```

Fetch all comment data using **four separate API calls** (this is required
because `gh pr view --comments` does NOT return inline review comments — known
limitation, gh CLI issue #5788):

**1. Inline review comments** (attached to specific code lines):

```bash
gh api repos/{owner}/{repo}/pulls/{num}/comments --paginate \
  --jq '.[] | {id, node_id, body, path, line, start_line, side, diff_hunk, user: .user.login, in_reply_to_id, pull_request_review_id, created_at}'
```

Note: `node_id` is the stable GraphQL identifier — used later to correlate REST
comments with GraphQL threads deterministically.

**2. Review envelopes** (APPROVE / REQUEST_CHANGES / COMMENT verdicts):

```bash
gh api repos/{owner}/{repo}/pulls/{num}/reviews \
  --jq '.[] | {id, state, body, user: .user.login, submitted_at}'
```

**3. Thread resolution status** (GraphQL — REST API has no resolution concept):

```bash
gh api graphql -f query='
  query($owner: String!, $repo: String!, $num: Int!) {
    repository(owner: $owner, name: $repo) {
      pullRequest(number: $num) {
        reviewThreads(first: 100) {
          pageInfo { hasNextPage endCursor }
          nodes {
            id
            isResolved
            isOutdated
            path
            line
            comments(first: 50) {
              nodes { id databaseId body author { login } createdAt }
            }
          }
        }
      }
    }
  }
' -f owner="{owner}" -f repo="{repo}" -F num={num}
```

Note: `databaseId` on comment nodes matches the REST `id` field — this is how
REST comments are correlated to GraphQL threads deterministically (see Merge
Data below). If `pageInfo.hasNextPage` is true, paginate with
`after: <endCursor>` to fetch all threads. For most PRs (<100 threads), a single
call suffices.

**4. General conversation comments** (PR-level, not attached to code):

```bash
gh api repos/{owner}/{repo}/issues/{num}/comments \
  --jq '.[] | {id, body, user: .user.login, created_at}'
```

**Merge data**: Correlate REST inline comments with GraphQL threads using stable
identifiers: match each REST comment's `id` (numeric) against the `databaseId`
field on GraphQL `PullRequestReviewComment` nodes within each thread. This gives
both the full comment content (REST) AND resolution status (GraphQL) for each
thread deterministically — avoid matching by `path` + `line` + body, which is
fragile when lines shift or bodies are edited.

**Build thread model**: Group comments into threads using `in_reply_to_id`. A
thread is: one top-level comment + zero or more replies, with resolution status
from GraphQL. Track each comment's **source type** (inline review comment vs
general conversation comment) — this determines which API endpoints to use for
replies and reactions later.

Report the preflight summary:

```
PR #2767: feat(011): Madagascar Analyzer Integration
Author:   pmanko
Target:   develop
Decision: CHANGES_REQUESTED (by ibacher)
Comments: 30 inline + 4 general = 34 total
Threads:  20 unresolved, 10 resolved, 4 outdated
Reviewers: copilot-pull-request-reviewer (20), ibacher (10), pmanko (4)
```

### 1) Classification & Triage

For each **unresolved** thread, the LLM reads:

- Author and author association (MEMBER, COLLABORATOR, etc.)
- File path and line number
- The diff hunk context (provided by GitHub in `diff_hunk` field)
- The full comment body (including any GitHub suggestion blocks)
- All replies in the thread
- Whether the thread is marked `isOutdated` by GitHub (code has changed since
  comment was made)

The LLM classifies each thread into one of these categories:

| Category         | Blocking? | Typical Action                   |
| ---------------- | --------- | -------------------------------- |
| **blocking**     | Yes       | Must-fix code change             |
| **bug**          | Yes       | Fix the defect                   |
| **suggestion**   | No        | Consider adopting                |
| **question**     | Maybe     | Reply with clarification         |
| **nitpick**      | No        | Fix if trivial                   |
| **praise**       | No        | Acknowledge (thumbs-up reaction) |
| **outdated**     | No        | Verify addressed, resolve        |
| **out-of-scope** | No        | Defer to follow-up issue         |

If a comment contains GitHub suggestion syntax (` ```suggestion ` blocks), note
"Has suggestion" in the triage table — this context is shown during the QA
session to help the user decide how to address it.

If GitHub marks a thread as `isOutdated`, default-classify as "outdated" (user
can override).

Present the triage table:

```
| #  | Category    | Author  | File:Line                        | Preview             | Notes      |
|----|-------------|---------|----------------------------------|---------------------|------------|
| 1  | blocking    | ibacher | AnalyzerRestController.java:42   | "Missing auth..."   |            |
| 2  | suggestion  | ibacher | MappingPanel.jsx:79              | "Consider using..."  | Suggestion |
| 3  | nitpick     | copilot | AnalyzerForm.jsx:156             | "Unused import"      |            |
| 4  | outdated    | copilot | Login.js:23                      | "console.log..."     | Outdated   |
...
```

Ask: "Review classifications. Override any? (e.g., '#3 → suggestion', or 'looks
good' to proceed)"

Apply any overrides, then proceed to the QA session.

### 2) QA Session (interactive, per-comment)

Process comments in priority order: blocking → bug → question → suggestion →
nitpick → outdated → out-of-scope → praise.

For each unresolved thread, display full context:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1/20] BLOCKING — ibacher on AnalyzerRestController.java:42
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

"The @Transactional annotation should be on the service, not the controller.
This violates the layered architecture principle."

┌─ Diff Context ──────────────────────────────────┐
│ (diff hunk from GitHub shown here)              │
└─────────────────────────────────────────────────┘

[If thread has replies, show them indented]
[If comment has suggestion block, show the suggested change]

How do you want to handle this?
  [f] Fix — make the code change now
  [d] Defer — create a follow-up GitHub issue
  [r] Reply only — respond without code change
  [s] Skip — come back to this later
  [a] Already fixed — mark as resolved with commit reference
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

After user selects:

**Fix [f]**:

1. Read the relevant file at the indicated line
2. Make the code change (the user guides what change to make)
3. Stage the changed file
4. Draft a reply: "Fixed — [brief description]. See upcoming commit."
5. Ask: "Resolve this thread? [y/n]"

**Defer [d]**:

1. Create a GitHub issue via `gh issue create` with:
   - Title referencing the review comment
   - Body containing: comment text, file:line, diff context, link back to PR
2. Draft a reply: "Tracked in #{issue_number} — will address in a follow-up PR."
3. Ask: "Resolve this thread? [y/n]"

**Reply [r]**:

1. Ask the user what they want to convey (explain, push back, ask question)
2. Draft a professional reply based on their input
3. Show the draft for user review/edit
4. Ask: "Resolve this thread? [y/n]" (choose No if awaiting reviewer response)

**Skip [s]**:

1. Move to the next comment. No resolve prompt.
2. Skipped comments are revisited at the end of the session.

**Already fixed [a]**:

1. Ask which commit addressed it, or search recent branch history for changes to
   the same file:line
2. Draft a reply: "Addressed in [commit ref] — [brief description]."
3. Ask: "Resolve this thread? [y/n]"

**For praise comments**: Offer to add a thumbs-up reaction, using the correct
endpoint based on comment type:

- **Inline review comments**:

  ```bash
  gh api repos/{owner}/{repo}/pulls/comments/{comment_id}/reactions \
    -f content="+1"
  ```

- **General conversation comments**:

  ```bash
  gh api repos/{owner}/{repo}/issues/comments/{comment_id}/reactions \
    -f content="+1"
  ```

Record each decision:
`{thread_id, category, action, reply_draft, resolve_yn, files_changed}`.

### 3) Batch Actions

After all comments are processed (or user types "done" to exit early):

**Display session summary:**

```
Fixed:          8 comments (code changes staged)
Deferred:       2 comments (issues #123, #124 created)
Replied:        5 comments (reply drafts ready)
Skipped:        3 comments
Already fixed:  2 comments

Threads marked for resolution: 12
Threads left unresolved: 8
```

**Execute in order:**

**Step 1 — Format and commit** (only if code changes were staged):

- Run `mvn spotless:apply` (backend formatting)
- Run `cd frontend && npm run format && cd ..` (frontend formatting)
- Show `git diff --staged --stat` for review
- Draft commit message: `fix: address review comments from <reviewer(s)>`
  - Include brief summary of what was changed
  - Reference PR number
- User confirms or edits the commit message
- Create the commit

**Step 2 — Post reply comments** (batch):

Show all drafted replies in a numbered list:

```
Reply drafts:
  1. AnalyzerRestController.java:42 -> "Fixed — moved @Transactional to..."
  2. MappingPanel.jsx:79 -> "Good suggestion — adopted with a slight..."
  3. AnalyzerForm.jsx:156 -> "Tracked in #123 — will address in..."
  ...

Post all 15 replies? [y / n / edit #N]
```

- **y**: Post each reply using the correct endpoint based on comment type:
  - **Inline review comments**: reply via
    `gh api --method POST repos/{o}/{r}/pulls/{n}/comments/{id}/replies -f body="..."`
  - **General conversation comments**: post a new comment on the PR quoting or
    linking the original, via
    `gh api --method POST repos/{o}/{r}/issues/{n}/comments -f body="> [original comment link]\n\n..."`
- **edit #N**: Show reply N for editing, then re-display the list
- **n**: Skip posting (user will post manually)

Report success/failure for each reply.

**Step 3 — Resolve threads**:

For threads marked for resolution during the QA session:

```
Resolving 12 threads...
```

Execute GraphQL mutation for each:

```bash
gh api graphql -f query='
  mutation($id: ID!) {
    resolveReviewThread(input: {threadId: $id}) {
      thread { id isResolved }
    }
  }
' -f id="{thread_node_id}"
```

**Important**: Only resolve threads whose reply was successfully posted in
Step 2. If a reply failed to post, skip that thread's resolution and warn:
`"Skipping resolution for [file:line] — reply failed to post (safety rule: never resolve without a reply)."`
Re-confirm with the user before retrying the failed reply.

Report: "Resolved N/M threads successfully." (where N excludes threads with
failed replies)

**Step 4 — Post PR summary comment** (optional):

Draft a summary comment like:

```
Addressed 17/20 review comments:
- 8 fixed (code changes in [commit ref])
- 5 replied with explanations
- 2 deferred to issues (#123, #124)
- 2 confirmed already addressed

Remaining: 3 skipped (will revisit)

Ready for re-review.
```

Ask: "Post this summary comment on the PR? [y/n/edit]"

If confirmed, post via:

```bash
gh pr comment {num} --body "..."
```

### 4) Post-Session Report

Print a final summary:

```
## PR Comment Resolution — PR #2767

| Category     | Count | Action                     |
|--------------|-------|----------------------------|
| blocking     |     3 | 3 fixed                    |
| bug          |     1 | 1 fixed                    |
| suggestion   |     5 | 3 adopted, 2 explained     |
| question     |     2 | 2 replied                  |
| nitpick      |     6 | 4 fixed, 2 skipped         |
| outdated     |     2 | 2 resolved                 |
| out-of-scope |     1 | 1 deferred -> #123         |

Commit:    abc1234 "fix: address review comments from ibacher"
Replies:   15 posted
Resolved:  12/20 threads
Remaining: 8 (3 skipped, 2 deferred, 3 awaiting response)

Next steps:
  - Push: git push
  - Skipped comments can be revisited by running /address-pr-comments again
```

## Troubleshooting

### "gh pr view" finds no PR

If the current branch has no open PR, the command asks for a PR number. You can
also pass it directly: `/address-pr-comments 2767`.

### Large PRs (100+ comments)

For PRs with many comments, use filters to focus:

- `/address-pr-comments --author ibacher` — only human reviewer comments
- `/address-pr-comments --category blocking,bug` — only blocking/bug comments
- `/address-pr-comments --unresolved-only` — skip already-resolved threads

### GraphQL permission errors

Thread resolution requires repository write access. If `resolveReviewThread`
fails with a permission error, ensure your `gh` token has the "repo" scope
(classic PAT) or "Pull requests: Read and write" (fine-grained PAT).

### Rate limiting

The command makes multiple API calls during preflight. If you hit rate limits,
the command will report the error and suggest waiting. For large PRs, the
`--paginate` flag on `gh api` handles pagination automatically.
