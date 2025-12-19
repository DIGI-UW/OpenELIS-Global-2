# Pull Request Guidelines

Before submitting a pull request, please ensure you follow these important
guidelines:

## 1. GitHub Issue Requirements

- Ensure you have a GitHub issue for your changes (except for trivial typo
  fixes)
- Check existing issues to avoid duplicates
- Wait for issue assessment and confirmation before starting work
- Assign the issue to yourself when you start working to prevent duplicate
  efforts

## 2. Branch Naming

- Branch name should reference issue number (e.g., issue-123)
- Create new branches from updated develop:
  ```bash
  git checkout develop
  git checkout -b issue-123
  ```

## 3. Pull Request References

- PR title must include issue number (e.g., "issue-123: Improve error handling")
- Include issue link in PR description
- Title should clearly summarize the changes

## 4. Target Branch

- Always create PRs against the develop branch unless specified otherwise
- Reviewers will handle backporting to other branches if needed

## 5. Keep Updated

- Pull latest changes before submitting PR:
  ```bash
  git pull --rebase upstream develop
  ```
- Run this daily when working on longer tickets

## 6. Formatting the Source Code

### Automatic Frontend Formatting (Pre-commit Hook)

Frontend code is **automatically formatted** when you commit changes. The pre-commit hook formats **only the files you've staged**, not the entire codebase.

**One-time setup (required after cloning):**
```bash
cd frontend
npm install
```

This installs Husky and lint-staged, which enable the automatic formatting.

**How it works:**
- When you run `git commit`, the pre-commit hook automatically runs
- Only your **staged frontend files** (`.js`, `.jsx`, `.ts`, `.tsx`, `.json`, `.css`, `.scss`, `.md`) are formatted
- Formatted files are automatically re-staged
- The commit proceeds automatically

**Manual formatting (if needed):**
If you want to format all frontend files manually:
```bash
cd frontend
npm run format
```

### Backend Formatting (Manual)

After making changes to the [backend](./src/) directory, run the formatter to properly format the Java code:

```bash
mvn spotless:apply
```

**Note:** Backend pre-commit hooks are not yet implemented. Always run `mvn spotless:apply` before committing backend changes.

## 7. Code Conventions

- Follow project coding conventions
- Configure IDE according to project guidelines
- Review your diff carefully before committing

## 8. Clean Pull Requests

- Keep changes focused and related to the issue at hand
- Avoid mixing unrelated changes in a single PR
- Example of what NOT to do:
  ```
  // Bad: Single PR with unrelated changes
  - Refactor utils class
  - Improve UI responsiveness in multiple components
  - Fix typos in documentation
  ```
- Instead, split into separate PRs:
  ```
  // Good: Separate PRs for each concern
  PR #1: "Issue-123: Refactor utils class"
  PR #2: "Issue-124: Improve UI component responsiveness"
  PR #3: "Issue-125: Fix documentation typos"
  ```
- Consider squashing commits for bug fixes and small features
- Don't worry about squashing review-related commits
- Final squash will be handled during merge

## 9. Descriptive Messages

- Use meaningful PR descriptions
- Include issue number and purpose
- When in doubt, use the issue summary

## 10. Review Process

- Request review from appropriate team members
- Add PR URL as comment on the issue
- Address review comments promptly

## 11. UI Changes

- Attach screenshots to PR description or comments
- Include before/after images for visual changes
- For web apps, include preview links if possible

## 12. Single Pull Request

- Maintain one PR per issue
- Push new commits to same branch to update PR
- Only create new PR if original cannot be modified

## 13. Replacing Pull Requests

- Close old PR with explanation if creating new one
- Reference new PR in closing comment

## 14. Abandoning Work

- Unassign yourself from issue if stopping work
- Document useful findings in issue comments
- Close any open PRs with explanation

## 15. Build Verification

- Check GitHub Actions build status
- Investigate failures using "Details" link
- Run `mvn clean install` locally before pushing

Remember to review the "Using Git" documentation, particularly the "Submit the
code" section, before creating pull requests.
