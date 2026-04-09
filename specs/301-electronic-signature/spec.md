# Feature Specification: 21 CFR Part 11 Electronic Signatures

**Jira Epic**: [OGC-539](https://uwdigi.atlassian.net/browse/OGC-539) **Primary
Ticket**: [OGC-301](https://uwdigi.atlassian.net/browse/OGC-301) **Feature
Branch**: `feature/electronic-signature` **PR**:
[#3288](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3288) **Status**: In
Review **Madagascar Requirement**: DU-11-03

## Authoritative Artifacts

- **FRS**:
  [electronic-signature.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/system/electronic-signature.md)
  (Casey, 2026-02-09, 30KB, 16 sections)
- **Mockup**:
  [electronic-signature.jsx](https://github.com/DIGI-UW/openelis-work/blob/main/designs/system/electronic-signature.jsx)
  (Casey, 2026-02-09, 44KB)
- **Gallery**:
  https://digi-uw.github.io/openelis-work/#/system/electronic-signature

## Scope (this PR)

Core e-signature infrastructure: database schema, backend
service/DAO/controller, frontend modal/button/hook, integration + E2E tests.
Implements the three-step ceremony per 21 CFR Part 11:

1. **First-use certification** (SS 11.100(c)) - one-time legal acknowledgment
2. **Full authentication** (SS 11.200(a)) - username + password for first
   signature in session
3. **Password-only signing** (SS 11.200(a)(1)(i)) - subsequent signatures in
   same session

### What's included

- Liquibase migrations: `electronic_signature` + `esig_first_use_certification`
  tables with immutability trigger, sequences, indexes
- JPA entities: `ElectronicSignature`, `EsigFirstUseCertification`,
  `SignatureMeaning`, `AuthMethod`
- Service layer: signing, certification, session tracking, credential
  verification
- REST controller: 8 endpoints at `/rest/esig/`
- Frontend: `ESignatureModal`, `ESignatureButton`, `useESign` hook, `api.js`
- Feature toggle: `electronicSignatureEnabled` site_information setting
  (default: false)
- Configurable session timeout: `esigSessionTimeoutMinutes` (default: 30 min)
- Tests: 37 integration tests + 18 Playwright E2E tests

### What's deferred (follow-up tickets under OGC-539)

| Ticket  | Item                                               | FRS Ref    |
| ------- | -------------------------------------------------- | ---------- |
| OGC-540 | Record hash at signing time (tamper-evidence)      | SS 11.70   |
| OGC-541 | Lockout controls (5 attempts / 30 min)             | FRS SS 13  |
| OGC-542 | Audit log query endpoint                           | FRS SS 12  |
| OGC-543 | Role-based access (3 dedicated roles)              | FRS SS 4   |
| OGC-544 | Configurable/versioned certification text          | FRS SS 6   |
| OGC-545 | UI polish (MeaningBadge, PasswordInput, cert flow) | Mockup     |
| OGC-546 | Batch signing with atomic rollback                 | FRS SS 8.4 |
| OGC-302 | Report-level signature display                     | FRS SS 16  |
| OGC-343 | Multi-level validation pipeline                    | FRS SS 4   |

## Key Decisions

1. **Session timeout: 30 min** (configurable). The PR originally used 8 hours,
   which conflates web session with signing session. Industry standard for 21
   CFR Part 11 is 15-30 minutes of inactivity.

2. **Default enabled: false**. Per FRS SS 13. Sites must explicitly opt-in.

3. **Username validation**: The username entered during the signing ceremony is
   validated against the authenticated session principal. This is part of the
   two-component identification per SS 11.200(a) and prevents impersonation.

4. **Roles deferred**: FRS defines RESULT_ENTRY_SIGN, RESULT_VALIDATE_SIGN,
   ESIG_ADMIN roles. Current implementation uses hasRole('ADMIN') for admin
   endpoints only. Full role model deferred to OGC-543.

5. **In-memory session storage accepted**: Research confirms this is acceptable
   for signing session state as long as loss triggers re-authentication (which
   it does). All signature events and audit data are persisted to the database
   immediately.

## Related Tickets

- OGC-302: Display signatures on printed reports (Herbert Yiga)
- OGC-343: Multi-level validation pipeline (Reagan Meant)
- OGC-291: Validation Page Update Step 2 (blocked by OGC-343)
