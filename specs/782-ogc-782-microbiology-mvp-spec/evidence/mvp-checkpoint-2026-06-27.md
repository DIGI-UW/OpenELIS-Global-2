# OGC-782 Microbiology MVP Checkpoint Evidence

Generated on 2026-06-27 from the local worktree dev stack.

## Headline Result

The OGC-782 MVP happy path passed with browser evidence: a seeded bacteriology
case opened in the workbench, setup was recorded, a significant isolate was
created, manual AST was started/read/overridden/reviewed, readiness changed to
final-release ready, and the final report was released.

## Validation Summary

| Check | Result | Why it matters |
|---|---|---|
| Backend focused M7 tests | Passed | Verifies release services, WHONET readiness, architecture, ORM mapping, and readiness blockers. |
| Frontend focused M7 tests | 3 files / 5 tests passed | Verifies readiness blocker rendering, release action behavior, case refresh, and AST-triggered readiness refresh. |
| Playwright registration | Passed for `core-demo` and `core-demo-video` | Confirms the MVP demo spec is correctly wired to the intended projects. |
| Playwright MVP demo | Passed on `core-demo` | Proves the complete browser user story against the running stack. |
| Playwright MVP video | Passed on `core-demo-video` | Produces reviewable video evidence of the MVP happy path. |
| Code-qa evidence bundle | Generated 7 screenshots and 1 MP4 | Packages the browser proof without committing binary media. |

## Media Artifacts

Do not commit these binary artifacts. They are local proof artifacts for PR
attachment or manual sharing.

- Video: `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/test-results/demo-core-ogc-782-microbio-3f6cc-ual-AST-override-and-review-core-demo-video/video.webm`
- MP4 bundle copy: `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27/videos/demo-core-ogc-782-microbio-3f6cc-ual-AST-override-and-review-core-demo-video.mp4`
- Bundle zip: `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/specs/782-ogc-782-microbiology-mvp-spec/evidence/mvp-checkpoint-2026-06-27.zip`

Screenshots:

- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-case-opened.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-setup-recorded.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-isolate-created.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-ast-reading.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-ast-overridden.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-ast-reviewed-ready.png`
- `/Users/pmanko/.codex/worktrees/1c9d/OpenELIS-Global-2/frontend/e2e-evidence/ogc-782-final-released.png`

## Known Console Noise

The passing Playwright runs still report pre-existing React Intl missing-message
errors for unrelated analyzer and navigation labels such as analyzer names,
`sideNav.title.vectorIdentification`, and `vectorReport.title`. The MVP run did
not show the release-panel DOM nesting or unmounted-state warnings after the M7
fix.
