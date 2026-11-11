# ModuleAuthenticationInterceptor Fail-Closed Rollout

## Scope

This document defines the separate follow-up pass to harden
`ModuleAuthenticationInterceptor` so unmapped `/rest/**` and `/Provider/**`
routes do not default to authenticated allow.

This is intentionally split from the immediate controller `@PreAuthorize`
remediation because it has higher regression risk.

## Current Risk

- `ModuleAuthenticationInterceptor.hasPermissionForUrl(...)` currently returns
  `true` for unmapped REST and provider URLs when the user is authenticated.
- Any missing `SystemModuleUrl` mapping behaves as fail-open, which can bypass
  module-based URL restrictions if method security is absent.

## Rollout Strategy

### Phase 1: Inventory and Impact Analysis

1. Enumerate active request mappings under `/rest/**` and `/Provider/**`.
2. Enumerate current `SystemModuleUrl` mappings in the target environment.
3. Produce a diff:
   - mapped endpoints
   - unmapped endpoints currently relying on fallback allow
4. Classify unmapped endpoints:
   - intentionally public/open
   - authenticated but role-gated by `@PreAuthorize`
   - unaudited/high-risk

### Phase 2: Controlled Fail-Closed Toggle

1. Introduce a configuration flag for interceptor behavior:
   - `security.moduleUrl.failOpen` (default `true` for compatibility)
2. When disabled (`false`), unmapped REST/provider routes return deny unless
   explicitly allowlisted.
3. Keep a narrow allowlist for known public routes already expected by
   `SecurityConfig`.

### Phase 3: Staging Validation

1. Enable fail-closed in staging only.
2. Run focused smoke tests for:
   - login/session flows
   - known public endpoints
   - Tier 1 admin endpoints now protected with `@PreAuthorize`
   - analyzer bridge/runtime paths that were carve-outs in Phase 1
3. Resolve missing `SystemModuleUrl` rows or explicit allowlist entries before
   production rollout.

### Phase 4: Production Rollout

1. Enable fail-closed with rollback switch kept available.
2. Monitor authorization denials and endpoint error rates.
3. Remove temporary allowlist entries once DB mappings and method security are
   complete.

## Required Test Coverage

- Unit tests for `hasPermissionForUrl(...)`:
  - mapped allow
  - mapped deny
  - unmapped allow when `failOpen=true`
  - unmapped deny when `failOpen=false`
- Integration-level smoke tests proving:
  - open endpoints remain reachable
  - authenticated users cannot reach unmapped protected routes
  - mapped admin routes remain accessible with correct roles

## Exit Criteria

- Fail-closed mode runs in staging and production without breaking known valid
  routes.
- No Tier 1 admin endpoint depends on fallback allow behavior.
- Remaining unmapped endpoints are either intentionally open (documented) or
  formally mapped.
