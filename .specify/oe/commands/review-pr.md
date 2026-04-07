# Review PR

Perform a deep, guided, layer-by-layer Pull Request review and remediation
session on OpenELIS Global 2.

## User Input

```text
$ARGUMENTS
```

Provide the GitHub Pull Request number as the argument to begin (e.g.,
`/review-pr 3326`). You can optionally pass the `--boot` flag (e.g.,
`/review-pr 3326 --boot`) if you want the environment immediately spun up upon
checkout.

## The Interactive Layer-by-Layer Rule

**WARNING TO AI:** You MUST perform this review iteratively. You CANNOT jump
ahead and try to audit the testing layer if the structural layer is broken. You
MUST halt after each Layer phase, present your targeted findings to the user,
and explicitly ask for permission to either **auto-remediate the layer's issues
locally** OR **proceed to the next layer**.

## Workflow

### Phase 0: Pre-Flight Context Loading

1. Run `gh pr checkout <PR_NUMBER>`.
2. Extract the PR Body/Description, and if linked to a SpecKit milestone branch
   (e.g., `feat/004-astm-m1`), read the corresponding `specs/.../spec.md`.
3. **Checkpoint:** Present the macro intent of the PR. Ask the user: _"Are we
   aligned on the intent? Shall we proceed to Layer 1 (Structural Audit)?"_
4. _Wait for Human._

### Layer 1: Structural & Dependency Audit

1. Audit the touched files for absolute OpenELIS non-negotiables:
   - Does it introduce Spring Boot dependencies? (REJECT)
   - Does it use `org.junit.jupiter.*` (JUnit 5)? (MUST BE JUNIT 4)
   - Are there hardcoded string texts in the frontend instead of
     `intl.formatMessage`? (REPLACE)
2. **Checkpoint:** List violations. Ask the user: _"Should I auto-remediate
   these structure issues locally, or proceed to Layer 2 (Architecture)?"_
3. _Wait for Human._

### Layer 2: Architectural Audit (The 5-Layer Pattern)

1. Verify the code conforms to the 5-Layer Pattern:
   - **Controllers:** Must be stateless with NO class variables. Must NOT
     contain `@Transactional`.
   - **Services:** Must contain transaction boundaries. Must use `JOIN FETCH`
     queries to avoid Lazy Initialization.
   - **DAOs/Valueholders:** Schemas must match Liquibase XML constraints.
2. **Checkpoint:** Present architectural drift analysis. Ask the user: _"Would
   you like me to restructure this logic locally, or skip to Layer 3
   (UI/Testing)?"_
3. _Wait for Human._

### Layer 3: UI & Carbon Pattern Audit (Phase bypassable if purely backend)

1. Ensure the frontend changes utilize `@carbon/react` and official `$spacing-*`
   tokens, eradicating Tailwind or custom CSS.
2. **Checkpoint:** Present UI findings. Ask the user for permission to align the
   styling to Carbon before proceeding.
3. _Wait for Human._

### Layer 4: Verification, Run-Gate, & Testing

1. Execute compiling boundaries (`mvn clean install -DskipTests`) or test
   subsets to ensure the branch fundamentally functions.
2. Detect if changes inherently require testing updates. Target JUnit 4
   execution against the modified classes, and verify Playwright tests if
   required.
3. **Checkpoint:** Present test verification health. Ask the user: _"Testing
   analyzed. Should I write missing JUnit4/Playwright tests for these bounds, or
   proceed to drafting the final PR review?"_
4. _Wait for Human._

### Final Phase: PR Conclusion & Review Posting

1. Prepare a `commit` summarizing any local remediations accomplished during the
   wizard phases, and prompt the user to let you push it.
2. Render a formatted PR Review summarizing your collaborative audit findings.
3. **Checkpoint:** Ask the user: _"Review drafted. Approve, Request Changes, or
   exit to terminal?"_
4. If approved: `gh pr review <PR_NUMBER> --approve -b "<REVIEW_BODY>"` (or
   `--request-changes`).

## Safety

- Do NOT merge a PR unconditionally.
- Remember the AI is the Consultant. The Human drives the boundaries.
