# RBAC — open items from OGC-384

Work identified during the OGC-384 review (PR #3443) that is **not** implemented
on that branch. Each file is standalone; nothing here blocks the others.

| Task | Severity | Decision (2026-09-21) |
| --- | --- | --- |
| [T1 — ungated @Service classes](t1-ungated-service-classes.md) | HIGH | **Blocks #3443.** All 41 gated or justified before merge. |
| [T2 — SystemInitFlag bypass](t2-systeminitflag-bypass.md) | HIGH | **Gates accept `ROLE_SYSTEM`.** Delete the flag; daemon identity satisfies gates at one central expression handler. |
| [T3 — interceptor fails open](t3-interceptor-fails-open.md) | HIGH | **Restore deny-by-default.** Seed `system_module_url` rows first, then flip. |
| [T4 — Global Admin by mutable name](t4-global-admin-identity.md) | MEDIUM | Not yet decided — self-contained, can follow. |
| [T5 — verify the 339 gates](t5-verify-gates-e2e.md) | HIGH | In progress: rebase → push → get `Build + Test` to complete. |

**Merge gate**: T1 blocks #3443. T2 and T3 are decided but their
implementation may land separately — confirm with reviewers before merging
without them.

## Shared background

PR #3443 moves authorization from `@PreAuthorize("hasRole(...)")` on controllers
to `@PreAuthorize("hasAuthority('PRIV_*')")` on **service interfaces**, enforced
by `ServicePrivilegeCoverageTest` and the `S011c` build gate.

Two consequences run through every task below:

- `ModuleAuthenticationInterceptor` **fails open** for unmapped `/rest` paths, so
  a service gate is frequently the *only* control on an endpoint — there is no
  second layer to catch a miss.
- `ServicePrivilegeCoverageTest` scans **interfaces only**. A `@Service` class
  with no interface is invisible to it (T1).
