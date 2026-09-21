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

## Definition of done

- `SystemInitFlag`, `SystemInitBeanPostProcessor`,
  `SystemAwareSecurityExpressionRoot`, `SystemAwareMethodSecurityExpressionHandler`
  and `SystemContextTaskDecorator` are deleted.
- Scheduler and startup writers run under `DaemonContextExecutor.executeAsDaemon(...)`.
- The 14 call sites that currently scope the flag by hand (`SystemInitFlag.enter()`,
  across 18 files referencing `SystemInitFlag`)
  are converted — `grep -rn SystemInitFlag src/main/java` finds nothing.
- `RequiredByAlertScheduler:90` calls `resolveAlert`, which needs
  `PRIV_ALERT_MANAGE`; whatever replaces the bypass must cover it.
- A test asserts a scheduled task **cannot** reach a gated method it has no
  privilege for.
