# T2 — Remove the SystemInitFlag authorization bypass

**Severity**: HIGH — disables all authorization on a thread
**Effort**: medium, but **blocked on a design decision**

## Problem

`SystemAwareSecurityExpressionRoot` short-circuits `hasAuthority`, `hasRole`,
`hasPermission` **and** `isAuthenticated` to `true` whenever the
`SystemInitFlag` ThreadLocal is set. It is not "the scheduler gets the
privileges it needs" — it is "authorization is off on this thread".

`SchedulerConfig:87` wires `SystemContextTaskDecorator.systemContext()` onto a
pooled 10-thread `ThreadPoolTaskScheduler`, so the flag is set for the duration
of every `@Scheduled` task.

The blast radius is wider than the scheduler. `FhirApiWorkFlowServiceImpl
.pollForRemoteTasks()` is `@Scheduled` and calls `processWorkflow(...)` as a
plain `this.` call, so the `@Async` proxy is bypassed and the whole FHIR
import/transform workflow runs on the flagged thread with every gate disabled.

## Why it is still here

Both reviewers asked for it to go:

- **@ibacher**: returning `true` for an unauthenticated caller is "quite
  dangerous"; the class is probably dispensable entirely.
- **@mozzy11**: develop merged #3356, which provides a sanctioned system identity
  (`DaemonAuthenticationToken` via `DaemonContextExecutor`), superseding this
  whole mechanism.

**The open decision**: `DaemonAuthenticationToken` carries only `ROLE_SYSTEM`, so
it satisfies **no** `hasAuthority('PRIV_*')` gate. Swapping the flag for the
daemon token as-is would make daemonised startup and scheduled writers fail.
Someone has to choose:

- **(a)** grant the daemon token an explicit `PRIV_*` set (auditable, verbose), or
- **(b)** teach the gates to accept `ROLE_SYSTEM` (concise, blunter).

Either is defensible. A flag that makes every check return `true` is not.

## DECIDED — option (b): the gates accept `ROLE_SYSTEM`

Chosen 2026-09-21. Keep `DaemonAuthenticationToken` as-is (`ROLE_SYSTEM` only)
and teach method security to pass for it, rather than enumerating a `PRIV_*` set
per job.

Implementation note: do this in **one place** — a custom
`MethodSecurityExpressionHandler`/expression root that returns true for
`ROLE_SYSTEM` — not by editing 339 `@PreAuthorize` expressions. That keeps the
daemon's reach greppable and revocable at a single point.

Accepted tradeoff: this is still "the daemon may do anything". The improvement
over `SystemInitFlag` is that it is an *authenticated identity* evaluated by
Spring Security, not a ThreadLocal that also makes `isAuthenticated()` lie, and
it cannot leak to a request thread via a pooled executor.

## Implementation status — 2026-09-21

**Done (option (b) landed):**

- `SystemAwareSecurityExpressionRoot` now satisfies gates for a caller holding
  `ROLE_SYSTEM`, at the single interception point, without touching any of the
  `@PreAuthorize` expressions. `isSystemInitiated()` is the one predicate every
  check routes through.
- `SchedulerConfig` runs `@Scheduled` work under
  `DaemonContextExecutor.executeAsDaemon(...)` — the daemon **identity** —
  instead of `SystemContextTaskDecorator.systemContext()`. Scheduled writes now
  attribute to the daemon system user rather than to nobody.
- The two `@Scheduled` importers (`ProviderImportServiceImpl`,
  `OrganizationImportServiceImpl`) no longer scope the flag by hand; they inherit
  the daemon identity from the scheduler thread.
- 13 unit tests on the expression root, inversion-verified: disabling the
  `hasSystemRole()` term fails exactly the two tests that assert the daemon route
  and leaves the four negative tests green.

**Ordering gotcha, found the hard way:** the daemon check must run *after*
`SystemInitFlag.isSet()`, not before. Spring's delegate
`getAuthentication()` **throws** `AuthenticationCredentialsNotFoundException`
when no SecurityContext exists, so probing for the daemon role first broke every
`@PostConstruct` that calls a gated service (whole test contexts failed to load
on `testServiceImpl`). `hasSystemRole()` also catches that exception rather than
letting it escape.

## NOT done — the flag cannot be deleted yet

The original definition of done ("`grep -rn SystemInitFlag src/main/java` finds
nothing") is **not reachable by swapping in the daemon token**, because the 15
call sites are not all the same kind of thing:

| Kind | Sites | Can `ROLE_SYSTEM` replace the flag? |
|---|---|---|
| **A — genuinely daemon-initiated** | 2 scheduled importers, `ConfigurationListenerServiceImpl` (`@Async` cache rebuild) | **Yes** — done for the two importers |
| **B — startup, no SecurityContext exists at all** | `SystemInitBeanPostProcessor`, `ConfigurationInitializationService.reloadAtStartup`, `ResultsLoadUtility` `@PostConstruct` | **No** — nothing can install a token before the context exists |
| **C — running on a live user's request thread** | `LoginPageController` `/session`, `CustomFormAuthenticationSuccessHandler`, `UserContextHolder.resolveSystemUser` (every audited write), `UserServiceImpl.getUserTestSections`, `DisplayListService` cold-cache build, `FhirTransformServiceImpl` | **No** — the thread carries the *user's* token, not a daemon token |

Kind C is the part that matters and the part the decision did not account for.
There the flag is not standing in for a missing identity — it is **escalating a
real, logged-in user past gates they do not hold**, so that an admin-scoped
internal read (`PRIV_USER_ROLE_VIEW`, `PRIV_ROLE_VIEW`, `PRIV_TEST_CONFIGURE`, …)
does not deny an ordinary user's own screen. Handing those threads a daemon token
would be strictly worse: it would swap the user's identity out from under an
audited write.

The real fix for Kind C is not a different bypass — it is that those internal
reads are gated at the wrong level. A self-identity read ("my own test sections",
"my own user record") should not require the admin privilege that governs reading
*anyone's*. That is a gating-model change across several services, with the
existing `SelfIdentityMethodsUngatedTest` as the place to assert it.

## Remaining definition of done

- [x] Gates accept `ROLE_SYSTEM` at one interception point.
- [x] Scheduler runs under the daemon identity.
- [ ] Kind A: `ConfigurationListenerServiceImpl` moved to `executeAsDaemon`.
- [ ] Kind C: split self-identity reads from admin-scoped reads so the six
      request-thread call sites can drop the flag. **This is the security-relevant
      half of T2 and is not done.**
- [ ] Kind B: keep the flag, but narrow it to the startup window only and rename
      it so it cannot be mistaken for a general-purpose override.
- [ ] A test asserts a scheduled task **cannot** reach a gated method it has no
      privilege for. Not yet written — with option (b) the daemon passes *every*
      gate, so this test can only assert the boundary (`denyAll`, and that a
      non-daemon authenticated caller is refused), which is what the new
      `ordinaryUser_isNotTreatedAsSystem` /
      `similarlyNamedAuthority_isNotTreatedAsSystem` /
      `denyAll_staysDenied_forRoleSystem` tests cover.
- [ ] `RequiredByAlertScheduler:90` calls `resolveAlert` (needs
      `PRIV_ALERT_MANAGE`) — now covered by the daemon identity via the
      scheduler, but unverified end to end.
