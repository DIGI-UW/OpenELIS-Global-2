# OGC-788 M2 Automated Validation - 2026-08-05

## Results

| Check                                                              | Result                                                                   |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| Focused JUnit 4, controller, integration, ORM, and Liquibase tests | 26 passed                                                                |
| Text-macro frontend Vitest/RTL                                     | 21 passed across 9 files                                                 |
| Foundational Playwright                                            | 3 passed including authentication setup                                  |
| Desktop/mobile accessibility                                       | 6 passed and 1 intentional mobile keyboard skip; no WCAG 2.1 AA findings |
| Standard demo video                                                | 2 passed including authentication setup; 45.64-second H.264/yuv420p MP4  |

The focused browser journey creates its phrases through the authenticated REST
controller and service. It uses generated persisted identifiers and no SQL,
DAO bypass, fixed database identifiers, forced clicks, or arbitrary waits.

## Behavior Proved

- The service emits deterministic CSV ordered by canonical code, with escaping,
  contexts, active state, and provenance but no database or audit identifiers.
- Bulk operations load and validate the complete selection before mutation,
  reject more than 100 rows, attribute changes to the authenticated actor, and
  restrict irreversible removal to local phrases.
- The Carbon table renders batch controls only while a selection exists. This
  removes hidden focusable controls and mobile overflow while retaining named
  confirmation and cancellation behavior.
- Canonical query state survives reload and is unchanged by selection,
  confirmation, cancellation, export, and completed bulk actions.
- The Playwright journey exercises deactivation, reload, export, reactivation,
  and audited local deletion through user-visible Carbon controls.

## Test Boundaries

- JUnit service tests own ordering, CSV quoting, caps, atomic validation, local
  deletion rules, and actor attribution.
- Controller tests own query binding, media headers, authorization, and request
  actor propagation.
- Integration tests prove persistence and audit behavior through the real
  service/database boundary.
- Focused component tests cover selection, conditional batch rendering, named
  confirmation, cancellation, download lifecycle, and canonical URL state.
- Shared Playwright helpers own authenticated setup, Carbon row selection,
  confirmations, downloads, and observable response/UI readiness.

## Negative Proof

The initial always-mounted Carbon batch toolbar failed the accessibility scan
because focusable controls lived under `aria-hidden`, and it overflowed the
mobile table. Conditional rendering of the shared batch action fixed both the
WCAG and viewport assertions. The tests therefore fail on the regression they
claim to prevent.

## Known Baseline Noise

The alternate-port local Vite server reports development HMR WebSocket noise
and one unnamed asset 404. Feature requests and all focused journeys pass.
A direct one-off ESLint invocation also reports repository/configuration noise,
including Vitest globals and existing effect-state patterns; no lint result is
claimed as a milestone pass.
