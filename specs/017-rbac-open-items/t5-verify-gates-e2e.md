# T5 — Actually verify the 339 privilege gates

**Severity**: HIGH (assurance gap, not a known defect)
**Effort**: small to run, unknown to fix what it finds

## Problem

PR #3443 gates 339 service methods across 78 services. **Every one of those
privilege choices was made from reading method signatures and callers — none has
been exercised against a running application.**

Local verification covers compile, `spotless`, `S011c`, `T045` and the ~34
non-Docker security tests. It cannot cover the Testcontainers suites or E2E,
because there is no Docker in the dev environment used for this work.

The exposure is asymmetric and both directions are bad:

- **Gate too tight** → a legitimate role is locked out. This already happened
  twice: `getUserTestSections` on `PRIV_RESULT_VIEW` 403'd Reception on
  `GET /rest/menu` and took out 52 E2E specs; `userInRole` on a privilege no role
  held would have done the same.
- **Gate too loose** → an endpoint is open. This also already happened:
  `MicroAstAnalyzerEventService.receive()` was left completely ungated when its
  controller guard was removed.

Highest-risk area: **microbiology**, ~108 methods on `micro:view` / `micro:bench`
/ `micro:supervise` — privileges invented for this PR, with read/write boundaries
inferred from method names.

## Blockers

- `Build + Test` has not completed on the current head (`f0dbe54fe`); only one
  check-run exists. The last full run (`f63082a23`) ended at 50 errors, ~37 of
  which are addressed in the two commits since. **~13 were never isolated.**
- The PR is `mergeable: false` / `dirty` and behind develop, so CI will not give
  a clean signal until it is rebased.

## Definition of done

1. Rebase onto develop, push, and get `Build + Test` to **complete** — that alone
   identifies the remaining ~13 errors.
2. Get a green E2E run. The `core 2/2` shard is the one that exercises the
   non-admin personas (`rbac_reception`, `rbac_results`, `rbac_validation`).
3. Walk the real workflows per role, at minimum: order entry → result entry →
   validation → release, plus one full microbiology culture/AST case as both a
   bench user and a supervisor.
4. Any gate that denies a role which previously had access is a **regression**,
   not a test-fixture problem — fix the gate or add the grant in `012-004`.

## Standing caution

`SelfIdentityMethodsUngatedTest` asserts that a privilege is *held by some role*.
It does **not** assert that the privilege is the *right* one for the method.
`ServicePrivilegeCoverageTest` only asserts an annotation is *present*. Neither
test can catch a plausible-but-wrong privilege — only running the workflow can.
