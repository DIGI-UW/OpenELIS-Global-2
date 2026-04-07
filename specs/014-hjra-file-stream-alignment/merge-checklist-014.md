# 014 merge checklist reference (submodules + OpenELIS #3103)

This checklist is retained as historical reference for the original 014
multi-repository merge chain. On the current `fix/013-hl7-test-connection`
branch, it should be read as evidence of how the work was previously staged, not
as the authoritative description of current branch/PR state.

**Merge readiness gate** — do not merge a PR until **both** are true on **latest
`HEAD`**:

1. **Comments:** All review threads resolved (fix) or dismissed with a short
   reason.
2. **CI:** All **required** checks green (re-run after every push).

## Order (re-check gate before each merge)

| Step | PR                                                                        | Target    | CI to verify (typical)                                                     |
| ---- | ------------------------------------------------------------------------- | --------- | -------------------------------------------------------------------------- |
| 1    | [Bridge #22](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/22) | `develop` | Run Tests, Maven Tests                                                     |
| 2    | [Bridge #23](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/23) | `develop` | Same (after rebase onto post-#22 `develop`)                                |
| 3    | [Mock #21](https://github.com/DIGI-UW/analyzer-mock-server/pull/21)       | `main`    | Tests, Docker Publish                                                      |
| 4    | [Plugins #69](https://github.com/DIGI-UW/openelisglobal-plugins/pull/69)  | `develop` | CI Build; confirm `develop` after merge if needed                          |
| 5    | [OpenELIS #3103](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3103)  | `develop` | Backend + E2E required (incl. Analyzer Harness **Required** / checkpoints) |

After steps 1–4: bump submodule SHAs on #3103 branch, rebase on `develop`,
re-apply gate on #3103.

**Out of scope for this chain:** plugins #68 (ASTM feature branch).
