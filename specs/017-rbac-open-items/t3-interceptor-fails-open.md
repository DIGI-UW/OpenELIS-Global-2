# T3 — Settle the interceptor fail-open, and correct the PR description

**Severity**: HIGH (as a documentation/assurance problem)
**Effort**: small for the text; larger if deny-by-default is restored

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

## DECIDED — option (b): restore deny-by-default

Chosen 2026-09-21. Seed the missing `system_module_url` rows, flip line 122 back
to `false`, and rewrite the integration test that currently codifies fail-open.

Sequencing matters: **seed the rows first and prove the non-admin personas still
load their screens**, then flip the flag. The revert in `b2381dddf` happened
because the flip landed without the rows. The endpoints known to have broken are
menu, configuration-properties, home-dashboard/metrics and user-test-sections —
treat that list as a starting point, not as complete.

This also downgrades T1 from "only control" to "defence in depth", though T1 is
still blocking (see its own file).

## Definition of done

~~Pick one:~~ (decided above — (b))

**(a) Accept fail-open** — edit the PR description to say so, add a comment at
the `return true` in `hasPermissionForUrl()` (line 122) explaining the revert and pointing
at `b2381dddf`, and treat 100% service-gate coverage (T1) as a merge blocker.

**(b) Restore deny-by-default** — seed `system_module_url` rows for the
infrastructure endpoints that broke, then flip the line-122 `return true` back to `false` and
update `ModuleAuthenticationInterceptorIntegrationTest`, whose three tests
currently assert `assertTrue(allowed)` and so **codify** fail-open.

Either way the PR description must stop asserting a closed gap.

## Related, already fixed

The interceptor returned **401** for authenticated-but-unauthorized requests
while `SecurityConfig`'s `AccessDeniedHandler` returned **403** for the same
condition. Corrected to 403 in an earlier commit on this branch; noted here so
the two are not re-diverged.
