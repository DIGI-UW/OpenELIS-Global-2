# RBAC — open items from OGC-384

Work identified during the OGC-384 review (PR #3443) that is **not** implemented
on that branch. Each file is standalone; nothing here blocks the others.

| Task | Severity | Decision (2026-09-21) |
| --- | --- | --- |
| [T1 — ungated @Service classes](t1-ungated-service-classes.md) | HIGH | **DONE.** 16 gated, 25 exempted with justification; regression test added. |
| [T2 — SystemInitFlag bypass](t2-systeminitflag-bypass.md) | HIGH | **Partly done.** Gates accept `ROLE_SYSTEM`; scheduler on daemon identity. Flag can't be deleted — 6 sites escalate a *live user*, not a daemon. |
| [T3 — interceptor fails open](t3-interceptor-fails-open.md) | HIGH | **Re-scoped.** Accept fail-open + fix the PR text; deny-by-default is follow-on (838 endpoints vs 9 seeded rows). |
| [T4 — Global Admin by mutable name](t4-global-admin-identity.md) | MEDIUM | Not yet decided — self-contained, can follow. |
| [T5 — verify the 339 gates](t5-verify-gates-e2e.md) | HIGH | **CI failures root-caused and fixed locally** (Mockito copies `@PreAuthorize` onto mocks; fixtures predated RBAC; 4 controllers re-labelled denials). Pushing for a full run. |
| [T6 — inherited CRUD ungated](t6-inherited-crud-ungated.md) | HIGH | **New, undecided.** 109 method-gated services extending `BaseObjectService` inherit ungated `insert/update/delete`. `AlertService` fixed; ratchet in place. Needs a fix-shape decision. |

**Merge gate**: T1 is done and is now the *primary* control — T3's measurement
showed the interceptor cannot be closed in this PR, so service gates are the only
enforcement on ~829 REST endpoints. T2 is decided and in progress; T3's remaining
in-PR work is the PR-description correction.

## Shared background

PR #3443 moves authorization from `@PreAuthorize("hasRole(...)")` on controllers
to `@PreAuthorize("hasAuthority('PRIV_*')")` on **service interfaces**, enforced
by `ServicePrivilegeCoverageTest` and the `S011c` build gate.

Two consequences run through every task below:

- `ModuleAuthenticationInterceptor` **fails open** for unmapped `/rest` paths, so
  a service gate is frequently the *only* control on an endpoint — there is no
  second layer to catch a miss. Measured: **838** distinct `/rest`+`/api` paths,
  **9** with `system_module_url` rows. This is accepted design, not an oversight
  (T3); admins bypass the check entirely via `isUserAdmin()`, which is why the
  original deny-by-default flip passed admin testing and still broke non-admins.
- `ServicePrivilegeCoverageTest` scans **interfaces only**. A `@Service` class
  with no interface is invisible to it (T1). It also checks that an annotation is
  *present*, not what it *covers*: a method-gated interface extending
  `BaseObjectService` inherits ungated CRUD (T6).
