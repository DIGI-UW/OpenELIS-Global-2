# Audit Branch

When the user invokes `/audit-branch` (optionally with arguments), perform a
tiered audit of the current branch diff against its PR target (or `develop`),
identifying LLM-assisted development artifacts, code quality issues, and
constitution violations.

This command produces a **structured report** with findings grouped by severity
and detection tier. It is designed to run **before PR submission** to catch
patterns that traditional linters miss — verbose comments, debug remnants,
placeholder code, leaked task references, and architectural violations.

Key capabilities:

- **Tier 1 (Mechanical)**: Regex-based pattern matching against added lines in
  the diff — fast, tuned for low false positives; findings still require quick
  human review
- **Tier 2 (Heuristic)**: Structural analysis requiring context —
  comment-to-code adjacency, import usage, scope relevance
- **Tier 3 (Semantic)**: LLM reasoning over diff chunks with constitution rules
  as context — deepest analysis, opt-in via `--deep`
- **Auto-fix**: Optional per-category fix for mechanical (Tier 1) findings with
  user confirmation
- **CI-ready**: Exit codes and severity thresholds for pipeline integration

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/audit-branch` → Full Tier 1+2 audit of current branch vs PR target
- `/audit-branch --deep` → Include Tier 3 semantic analysis
- `/audit-branch --tier 1` → Mechanical scan only (fastest)
- `/audit-branch --fix` → After report, offer to auto-fix Tier 1 findings
- `/audit-branch --threshold {CRITICAL|HIGH|MEDIUM|LOW}` → Set minimum severity
  for exit code failure (default: HIGH)
- `/audit-branch --since <ref>` → Audit only changes since a specific ref
  (narrows diff scope, but full-branch is recommended)

Flags can be combined: `/audit-branch --deep --fix --threshold MEDIUM`

If any inputs are unclear, ask a single concise question and continue with safe
defaults.

## Safety Rules (non-negotiable)

- **Read-only by default.** The audit NEVER modifies files unless `--fix` is
  passed AND the user confirms each fix category.
- **Never auto-commit.** Even with `--fix`, stage changes but do NOT create a
  commit. The user decides when and how to commit.
- **Never auto-fix Tier 2 or Tier 3 findings.** Only Tier 1 mechanical patterns
  are eligible for auto-fix.
- **Scan the diff only by default.** Findings should normally be limited to
  lines ADDED in the branch diff. For explicitly documented whole-file checks
  (e.g., M10/i18n), you MAY scan entire files, but clearly label any issues that
  predate the current branch as "pre-existing (out of scope for this branch)."
- **Report all findings honestly.** Do not suppress, downplay, or editorialize
  findings. Present them with severity, location, matched text, and a brief
  suggested action.

## Workflow

### 0) Preflight (gather facts, no changes)

Run these and summarize the results:

- `git rev-parse --show-toplevel` (verify repo root)
- `git status --porcelain` (warn on uncommitted changes — include them in scope
  note but don't block)
- `git branch --show-current` (if detached, ask user which branch to audit)
- Detect PR target:
  - `gh pr view --json baseRefName,number,title` (handle non-zero exit status
    gracefully if no PR exists for the current branch)
  - If no PR exists, fall back to `develop` (or `main` if `develop` doesn't
    exist)
- Compute diff scope:
  - `git diff <target>...HEAD --stat`
  - Count: total files changed, insertions, deletions
- Categorize changed files into buckets:
  - **backend-java**: `src/main/java/**/*.java` (exclude test)
  - **backend-test**: `src/test/java/**/*.java`
  - **frontend-jsx**: `frontend/src/**/*.{jsx,tsx}`
  - **frontend-js**: `frontend/src/**/*.{js,ts}` (exclude test)
  - **frontend-test**: `frontend/src/**/*.test.*`, `cypress/**`
  - **config**: `*.xml`, `*.yml`, `*.yaml`, `*.properties`, `*.json` (non-i18n)
  - **i18n**: `frontend/src/languages/*.json`
  - **docs**: `*.md`
  - **other**: everything else

Report preflight summary:

```
Branch:   <current-branch>
Target:   <target> (PR #NNN / fallback)
Files:    N changed (N backend, N frontend, N docs, N other)
Lines:    +NNNN / -NNNN
Tiers:    1+2 (add --deep for Tier 3)
```

### 1) Mechanical Scan (Tier 1)

Extract added lines per file from `git diff <target>...HEAD`. For each file,
track original line numbers (from the `@@` hunk headers) so findings reference
actual file positions, not diff positions.

Run each detection rule against added lines only. Skip test files for rules that
target production code (M1, M2, M5, M8, M9). Apply M1 debug-statement detection
to test files separately with a note that test `System.out` should use logger or
assertions.

#### Detection Rules — Tier 1

| ID  | Name                                | Pattern (applied to added lines)                                                                                                                                                 | Severity | Fixable |
| --- | ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------- |
| M1  | Debug statements (prod)             | `console\.(log\|warn\|error)\(` in non-test JS/JSX/TS/TSX; `System\.out\.print` in non-test Java                                                                                 | MEDIUM   | Yes     |
| M1t | Debug statements (test)             | `System\.out\.print` in test Java (suggest logger/assertion)                                                                                                                     | LOW      | No      |
| M2  | Unresolved TODOs                    | `//\s*TODO:?\s` or `//\s*FIXME:?\s` in non-test source                                                                                                                           | MEDIUM   | No      |
| M3  | Task reference tags                 | `Task Reference:\s*T\d+` in javadoc/comments; `\(T\d{2,4}[a-z]?\)` inline                                                                                                        | MEDIUM   | Yes     |
| M4  | Hedging language                    | `should work\|hopefully\|might need\|this assumes\|for now\|temporary fix\|quick implementation` inside comments (lines starting with `//` or `*`)                               | LOW      | No      |
| M5  | Placeholder/stub comments           | `placeholder\|simplified.*(check\|implementation\|parser)\|in practice.*would\|actual implementation would\|would need specific` in comments                                     | HIGH     | No      |
| M6  | Merge conflict markers              | `^[<>=]{7}`                                                                                                                                                                      | CRITICAL | No      |
| M7  | Commented-out code                  | 3+ consecutive lines matching `^\s*//\s*(if\|for\|while\|return\|try\|catch\|else\|switch\|case\|import\|public\|private\|protected\|const\|let\|var\|function)`                 | LOW      | Yes     |
| M8  | Hardcoded English in REST responses | `\.put\(\s*"(message\|error\|detail)",\s*"[A-Z]` in Java controller files                                                                                                        | MEDIUM   | No      |
| M9  | Swallowed exceptions                | `catch\s*\([^)]*\)\s*\{` where the catch body (next 1-2 lines) contains only a comment or is empty before `}`                                                                    | HIGH     | No      |
| M10 | i18n duplicate keys                 | Run Python duplicate-key detector on ALL files in `frontend/src/languages/*.json` (not just diff — duplicates can pre-exist). Reuse the pattern from the i18n-check CI workflow. | CRITICAL | No      |

For each finding, record:
`{rule_id, severity, file_path, line_number, matched_text (trimmed to 120 chars), suggested_action}`.

### 2) Heuristic Analysis (Tier 2)

These rules require structural context beyond simple pattern matching. Parse
diff hunks to understand relationships between lines.

#### Detection Rules — Tier 2

| ID  | Name                            | Detection Method                                                                                                                                                                                                                                                                                                                  | Severity |
| --- | ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| H1  | Narrating comments              | A comment line matching `//\s*(Parse\|Build\|Convert\|Apply\|Get\|Set\|Create\|Retrieve\|Calculate\|Check\|Verify\|Call\|Perform\|Trigger\|Return\|Initialize\|Update\|Delete\|Save\|Load\|Send\|Fetch)` immediately followed by a single code statement that does exactly what the comment says. Both lines must be in the diff. | LOW      |
| H2  | Verbose javadoc                 | A `/** ... */` block spanning >5 added lines that documents a method spanning <5 added lines in the same hunk. Especially flag javadoc containing "Handles CRUD", "Provides business logic", "REST Controller for".                                                                                                               | LOW      |
| H3  | Unused imports in diff          | An `import` statement added in the diff where the imported symbol (last segment of the import path) does NOT appear in any other added line in the same file. NOTE: This is a heuristic — the symbol may be used in unchanged code. Flag as LOW confidence if unsure.                                                             | MEDIUM   |
| H4  | Scope creep indicator           | Compare the set of changed file paths against keywords from the PR title and description (from `gh pr view`). Files in directories with no keyword overlap are potential scope creep. Report the list but do not assign severity — this is informational.                                                                         | INFO     |
| H5  | Excessive documentation         | New `.md` files added in the diff that are >500 lines, OR new `.md` files in paths containing `research/`, `plans/`, or names matching `*-report.md`, `*-analysis.md`, `SETUP-REPORT.md`.                                                                                                                                         | LOW      |
| H6  | Single-use abstraction          | An `interface` or `abstract class` definition added in the diff that has exactly 1 `implements`/`extends` reference in the entire diff.                                                                                                                                                                                           | LOW      |
| H7  | Over-documented private methods | A `private` method added in the diff that has a javadoc block. Private methods rarely need javadoc — method name should be self-documenting.                                                                                                                                                                                      | LOW      |

### 3) Semantic Review (Tier 3 — only if `--deep`)

If `--deep` was NOT passed, skip this section and print:
`Tier 3 (semantic) skipped — add --deep to enable.`

If `--deep` was passed:

Load the constitution from `.specify/memory/constitution.md` and use it as
evaluation context. Group diff chunks by file category and analyze:

| ID  | Name                               | What to Evaluate                                                                                                                                                                                                                      | Severity |
| --- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| S1  | Constitution: layered architecture | `@Transactional` on methods in `*/controller/*.java` files (MUST be in services only). Controllers calling DAO classes directly (look for DAO type references). `createNativeQuery` or `nativeQuery=true` in service/controller code. | CRITICAL |
| S2  | Constitution: Carbon Design System | Import statements referencing `bootstrap`, `tailwind`, `@mui`, `material-ui`, `antd` in frontend files. CSS/SCSS with hardcoded color hex values instead of Carbon tokens.                                                            | CRITICAL |
| S3  | Constitution: internationalization | JSX string literals that appear to be user-facing text (>2 words, not a CSS class, not a prop name) not wrapped in `<FormattedMessage>` or `intl.formatMessage()`.                                                                    | HIGH     |
| S4  | Hallucinated API usage             | Method calls on Spring/Java/React libraries that don't match known APIs. Cross-reference against project dependency versions in `pom.xml` and `package.json`.                                                                         | HIGH     |
| S5  | Copy-paste duplication             | Two or more code blocks in the diff (>5 lines each) with >80% structural similarity.                                                                                                                                                  | MEDIUM   |
| S6  | Inconsistent patterns              | New code that uses different conventions than existing code in the same file (naming style, error handling approach, response format).                                                                                                | MEDIUM   |
| S7  | Over-engineering                   | Unnecessary abstraction layers, feature flags for non-configurable behavior, wrapper classes that add no logic.                                                                                                                       | MEDIUM   |
| S8  | Security anti-patterns             | Hardcoded credentials or API keys, missing input validation on controller parameters, weak cryptographic choices (MD5, SHA1 for security purposes), SQL injection vectors.                                                            | CRITICAL |

### 4) Report

#### Inline Summary (always printed to chat)

```markdown
## Audit Report — <branch> vs <target>

**Scope:** N files, +NNNN/-NNNN lines | Tiers: 1+2 [+3 if --deep]

| Severity | Count | Fixable |
| -------- | ----- | ------- |
| CRITICAL | N     | N       |
| HIGH     | N     | N       |
| MEDIUM   | N     | N       |
| LOW      | N     | N       |
| INFO     | N     | —       |

### Top Findings (up to 10, highest severity first)

1. **[M6] CRITICAL** Merge conflict marker: `file.java:42` `<<<<<<< HEAD` →
   Resolve merge conflict before proceeding

2. **[M5] HIGH** Placeholder comment: `ServiceImpl.java:130`
   `// This is a placeholder - actual implementation would...` → Implement the
   actual logic or remove the placeholder

...

### Clean Checks ✓

- No merge conflict markers (M6)
- No i18n duplicate keys (M10)
- No hardcoded secrets detected (S8)

Full report: `.specify/audit-reports/<branch>-<timestamp>.md`
```

#### Detail File

Write the full report to
`.specify/audit-reports/<branch-sanitized>-<timestamp>.md` where:

- `<branch-sanitized>` replaces `/` with `-` in the branch name
- `<timestamp>` is `YYYYMMDDTHHMMSS` format

The detail file contains:

- Header with branch, target, timestamp, tiers run
- Full findings grouped by severity (CRITICAL → HIGH → MEDIUM → LOW → INFO),
  then by rule ID within each group
- Each finding: rule ID, name, severity, `file:line`, matched text (full line),
  suggested action
- Summary statistics table: rule ID → count
- "Clean bill of health" section listing rules with zero findings

#### Exit Code Behavior

After the report, evaluate the exit code:

- Count findings at or above the `--threshold` severity (default: HIGH)
- If count > 0: report `EXIT 1 — N finding(s) at or above <threshold> severity`
- If count == 0: report `EXIT 0 — clean at <threshold> threshold`

This is informational in interactive mode. In CI, the exit code would gate the
pipeline.

### 5) Optional Auto-Fix (only if `--fix`)

If `--fix` was NOT passed, skip this section.

If `--fix` was passed and Tier 1 fixable findings exist:

For each fixable rule category (M1, M3, M7), present:

```
Fix M1 (debug statements)? 5 instances found.
  Examples:
    frontend/src/components/analyzers/MappingPanel.jsx:79  console.warn(...)
    frontend/src/components/analyzers/AnalyzerForm.jsx:156 console.warn(...)
  [y]es / [n]o / [p]review all
```

- **yes**: Apply the fix (remove the line or the specific statement), then
  report what was changed
- **no**: Skip this category
- **preview**: Show every edit that would be made, then ask again

Fix strategies per rule:

- **M1 (debug statements)**: Remove the entire line containing the debug call.
  If the debug call spans multiple lines, remove all of them.
- **M3 (task references)**: Remove `* Task Reference: T###` lines from javadoc
  blocks. Remove `(T###)` from inline comments (preserve the rest of the
  comment).
- **M7 (commented-out code)**: Remove consecutive blocks of commented-out code.

After all fixes are applied:

- Run `mvn spotless:apply` (backend formatting)
- Run `cd frontend && npm run format` (frontend formatting)
- Stage the changed files with `git add <specific-files>`
- Report: "Fixed N instances across M files. Changes staged but NOT committed."

## Troubleshooting

### Large diffs (>10K added lines)

For very large diffs, Tier 2 heuristic analysis may be slow. Consider:

- `/audit-branch --tier 1` for a quick mechanical scan
- Focus on HIGH and CRITICAL findings first

### False positives

Some heuristic rules (H1 narrating comments, H3 unused imports) may produce
false positives. The report marks these as LOW severity intentionally. Review
them but don't treat them as blocking.

### No PR found

If `gh pr view` fails (no PR exists for the branch), the command falls back to
diffing against `develop`. If `develop` doesn't exist, it tries `main`. If
neither exists, it asks the user to specify a target ref.
