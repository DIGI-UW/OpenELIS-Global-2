# Security Hardening Roadmap (v1)

**Date:** 2026-03-16  
**Scope:** Initial triage + execution plan for vulnerabilities found in full-codebase and recent merged PR review.  
**Goal:** Create a shared decision framework and first implementation sequence before code remediation PRs.

---

## 1) Executive Summary

This roadmap prioritizes security risks that can cause:
- unauthorized analyzer result injection or routing tampering,
- exposure/reuse of credentials from source control,
- web request forgery on authenticated sessions,
- internal error disclosure to external clients.

The highest-risk item is analyzer ingress trust-by-header behavior on open endpoints.  
The roadmap assumes we should reduce reliance on external network controls and enforce stronger application-layer guarantees.

---

## 2) Triage Model (for discussion and reprioritization)

Each issue is scored 1-5 on:
- **Exploitability** (ease of abuse),
- **Impact** (clinical/business blast radius),
- **Exposure** (internet/internal surface),
- **Confidence** (evidence strength).

`Priority Score = Exploitability + Impact + Exposure + Confidence`  
Bands:
- **P0**: 17-20 (urgent hardening now)
- **P1**: 13-16 (next sprint)
- **P2**: 9-12 (scheduled hardening)
- **P3**: <=8 (backlog/watch)

> Decision checkpoint: if operational constraints prevent immediate app-layer controls, we keep compensating controls as explicit temporary exceptions with expiry dates.

---

## 3) Initial Triage Table

| ID | Finding | Current Risk | Score | Priority | Notes |
|---|---|---:|---:|---|---|
| SEC-001 | Analyzer ingress spoofing risk via open `/analyzer/astm` + `/analyzer/hl7` and trusted headers (`X-Source-*`, `X-Analyzer-Id`) | Critical | 19 | P0 | Recent PRs expanded deterministic header routing; app trusts infrastructure boundary. |
| SEC-002 | Hardcoded credentials in backup/import scripts/templates | High | 16 | P1 | Includes upload/FTP credentials and analyzer-side credentials in repo files. |
| SEC-003 | CSRF ignored for broad `/rest/**` paths with session/cookie auth in same stack | Medium | 14 | P1 | Needs endpoint-level threat split and safer defaults. |
| SEC-004 | Analyzer endpoints can return parser/plugin error details to clients | Medium | 12 | P2 | Can expose internals useful for probing. |
| SEC-005 | Weak default `encryption.general.password=dev` fallback | Low-Medium | 10 | P2 | Depends on deployment hygiene; should still be hardened. |

---

## 4) Challenge to Existing Assumptions

### Assumption A: “Internal Docker network/firewall makes header trust safe.”
**Challenge:** This is brittle under misconfiguration, lateral movement, ingress proxy mistakes, and non-prod deployments.  
**Direction:** Treat network controls as defense-in-depth; enforce authenticity and authorization in app layer too.

### Assumption B: “Legacy scripts with credentials are low risk.”
**Challenge:** Secret reuse and copy-forward into real environments is common; source exposure persists.  
**Direction:** Eliminate cleartext secrets from repo paths and enforce secret scanning gates.

### Assumption C: “Broad CSRF ignore is necessary for all REST APIs.”
**Challenge:** Not all REST endpoints are true stateless-token APIs.  
**Direction:** Split API classes by auth mechanism and require explicit CSRF policy per class.

---

## 5) Phased Execution Plan

## Phase 0 (P0, immediate containment)
1. **Document temporary trust boundary contract** for analyzer ingress (source identity, allowed network paths, expected headers).
2. **Add deploy-time guardrails checklist** (ingress ACLs, mTLS or signed-auth between bridge and OpenELIS, deny direct external access).
3. **Create architecture decision record (ADR)** for app-layer verification requirement (short-term and target-state).

**Exit criteria:**
- Security owner signoff on temporary controls and expiry date.
- Production and staging hardening checklist completed.

## Phase 1 (P0/P1 code hardening)
1. **Analyzer ingress authentication hardening**
   - Add verifiable bridge-to-app trust mechanism (preferred: mTLS or signed HMAC header with nonce/timestamp replay protection).
   - Reject unauthenticated analyzer ingest requests regardless of header values.
   - Remove/replace `userId = "1"` fallback with explicit service principal and auditable attribution.
2. **Endpoint exposure tightening**
   - Re-evaluate `OPEN_PAGES` entries for analyzer endpoints.
   - Restrict by path + auth + source controls.

**Exit criteria:**
- Spoofed header replay test fails.
- Unauthenticated ingest attempts denied.
- Audit logs identify service principal and source.

## Phase 2 (P1 secret hygiene + CSRF boundary corrections)
1. **Secret cleanup**
   - Remove hardcoded credentials from scripts/templates.
   - Replace with environment/secret manager references.
   - Rotate known leaked credentials.
2. **CSRF policy hardening**
   - Inventory `/rest/**` endpoints by auth mode.
   - Narrow CSRF ignore list to stateless-token endpoints only.
   - Add integration tests for cookie-auth mutating requests.

**Exit criteria:**
- No cleartext operational credentials in repository.
- CSRF tests pass for session-auth mutating endpoints.

## Phase 3 (P2 robustness and observability)
1. **Error response sanitization**
   - Standardize generic client messages for analyzer ingest.
   - Keep detailed diagnostics server-side only.
2. **Configuration hardening**
   - Remove insecure default encryption password fallback or fail fast when unset in non-dev.
3. **Monitoring + detection**
   - Alert on ingest auth failures, unusual source drift, repeated malformed payloads.

**Exit criteria:**
- External responses contain no stack/plugin internals.
- Security telemetry dashboards include ingest/auth anomalies.

---

## 6) Work Breakdown (first implementation slices)

### Slice A (small, high-value)
- Ingest request authenticator middleware/filter + signature validation (or mTLS assertion check).
- Unit tests for pass/fail cases (tampered header, stale timestamp, missing signature).

### Slice B
- Remove hardcoded script secrets; update docs/examples to env var placeholders.
- Add repo secret scan workflow/check.

### Slice C
- CSRF policy refactor for targeted endpoints.
- Integration tests for protected session-auth API actions.

### Slice D
- Error contract cleanup for analyzer upload/ingest controllers.
- Logging policy update for sensitive fields.

---

## 7) Proposed PR Sequence

1. **PR-SEC-01:** Analyzer ingress trust hardening (authentication + audit identity)  
2. **PR-SEC-02:** Credential de-hardcoding + rotation playbook + CI secret scan  
3. **PR-SEC-03:** CSRF boundary correction + tests  
4. **PR-SEC-04:** Error sanitization + secure defaults cleanup

Each PR should include:
- threat statement,
- before/after abuse test evidence,
- rollback plan,
- operational runbook updates.

---

## 8) Open Decisions for Team Triage

1. Preferred ingress trust mechanism: **mTLS** vs **signed header** vs both?
2. Should analyzer ingest remain on open paths with strong auth, or move under authenticated route groups?
3. Secret management baseline: environment variables only vs managed secret store requirement?
4. CSRF policy target: default-on with explicit opt-out, or split app by stateless API boundary?

---

## 9) Definition of Done (program-level)

- All P0/P1 items remediated and validated with abuse-case tests.
- No hardcoded operational credentials in repository.
- Ingest endpoints require verifiable request authenticity.
- Security controls documented with ownership and monitoring.
- Residual risks accepted explicitly with expiry + owner.

