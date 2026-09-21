# T3 — Settle the interceptor fail-open, and correct the PR description

**Severity**: HIGH (as a documentation/assurance problem)
**Effort**: small for the text; **re-architecture, not a task,** if deny-by-default is restored

## Problem

`ModuleAuthenticationInterceptor.hasPermissionForUrl()` returns `true` for any
`/rest`, `/api`, `/Provider`, `/dbImage` or `/logging` path with no
`system_module_url` row — i.e. **unmapped REST endpoints are allowed through for
any authenticated user**.

Deny-by-default was implemented in `acc3b5408` and **reverted in `b2381dddf`**
after live testing showed it broke non-admins on unmapped infrastructure
endpoints (menu, configuration-properties, home-dashboard, user-test-sections).
The revert is defensible. Two things about it are not:

1. **The PR description still claims the opposite.** It reads:

   > Close auto-allow gap in ModuleAuthenticationInterceptor (REST paths now
   > return HTTP 401 JSON when denied)

   and lists `ModuleAuthenticationInterceptorTest (auto-allow gap closed)`.
   A reviewer reading this believes a control exists that does not. PR #3443
   also removes ~80 controller `hasRole('ADMIN')` guards *on the stated premise
   that this interceptor plus service gates cover them*.

2. **Nothing records fail-open as the accepted design**, so the next person to
   touch it has no way to know the revert was deliberate.

This is why T1 matters so much: with the interceptor failing open, a missing
service gate is not a defence-in-depth gap — it is the whole control.

## DECISION SUPERSEDED — measured 2026-09-21

An earlier decision here chose **(b) restore deny-by-default**, on the
assumption that the four endpoints named in the revert commit were roughly the
whole gap and could be seeded. Measurement contradicts that assumption:

| | count |
|---|---|
| distinct `/rest` + `/api` endpoint paths across 343 controllers | **838** |
| distinct `url_path` values seeded into `system_module_url` | 55 total |
| …of those, rows matching a `/rest` or `/api` path | **9** |

Method: endpoint paths are class-level `@RequestMapping` prefixes composed with
each method-level mapping, over `*Controller*.java` under `src/main/java`; seed
rows are distinct `name="url_path"` values across all Liquibase changelogs
(there are no raw-SQL inserts into that table). Counts are approximate at the
margins — annotation-parsing edge cases move them by single digits — but the
order of magnitude is not in question: **838 against 9.**

The nine that exist are `/rest/GenericSampleOrder`,
`/rest/GenericSampleOrder/import`, `/rest/GenericSampleOrder/validate`,
`/rest/admin/vector/manual-entry-fields`, `/rest/alerts`, `/rest/eqa`,
`/rest/qc`, `/rest/reports/vector-surveillance/manual-entry` and
`/rest/reports/vector-surveillance/manual-entry/submit`.

So flipping line 122 back to `false` denies roughly **829 endpoints** for every
non-admin. The four endpoints in the revert commit were not the gap; they were
the first four a tester happened to hit.

**Why the original flip looked fine before it was reverted:** admins never
reach the module check at all.

```java
protected boolean hasPermission(Errors errors, HttpServletRequest request, String path) {
    if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
        return hasPermissionForUrl(request, USE_PARAMETERS, path) || userModuleService.isUserAdmin(request);
```

`isUserAdmin(request)` short-circuits the whole thing. Any verification done as
an admin — which is how the flip was tested — cannot observe the breakage. That
is the same trap that produced the revert, and re-running (b) without seeding
all ~830 rows first walks straight back into it.

Seeding ~830 `system_module_url` rows is not a task inside this PR. It is a
module-model migration: every row needs a `system_module` to point at, the
module names have to line up with what `getPermittedForms()` returns per role,
and getting any single one wrong locks out a persona with no failing test to
catch it. It also cannot be verified from this branch without a full stack and
one login per persona.

**Therefore: (a) for this PR, (b) as separate work.** Accept fail-open here,
document it, and keep T1 (100% service-gate coverage) as the merge blocker —
which is where the real enforcement now lives. Restoring deny-by-default is
tracked as follow-on work with the ~830-row seeding as its actual scope.

## Definition of done — for this PR

- [x] Comment at the `return true` in `hasPermissionForUrl()` explaining the
      revert and naming the endpoints that broke. Present at
      `ModuleAuthenticationInterceptor.java:113-120`.
- [ ] PR description stops asserting the gap is closed. It currently claims
      "REST paths now return HTTP 401 JSON when denied" and cites
      `ModuleAuthenticationInterceptorTest (auto-allow gap closed)`. Both must
      go, replaced with a statement that unmapped REST paths defer to
      `@PreAuthorize` and that this is deliberate.
- [ ] T1 treated as a merge blocker, not a nice-to-have, since it is now the
      only enforcement for ~829 endpoints.

`ModuleAuthenticationInterceptorIntegrationTest`'s three `assertTrue(allowed)`
tests are **correct as they stand** under (a) — they codify the accepted
behaviour. They only need rewriting if (b) is later done.

## Follow-on work (not this PR)

Restore deny-by-default. Real scope:

1. Enumerate all ~838 `/rest`+`/api` paths and map each to a `system_module`.
2. Seed `system_module_url` rows for all of them.
3. Prove each non-admin persona still loads its screens — **as that persona,
   never as an admin**, because `isUserAdmin()` masks the failure.
4. Only then flip line 122 to `false` and rewrite the three integration tests.

Step 3 is the one that was skipped in `acc3b5408`.

## Related, already fixed

The interceptor returned **401** for authenticated-but-unauthorized requests
while `SecurityConfig`'s `AccessDeniedHandler` returned **403** for the same
condition. Corrected to 403 in an earlier commit on this branch; noted here so
the two are not re-diverged.
