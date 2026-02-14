# E2E Semantics Guardrail Checklist (M3)

This checklist prevents accidental downgrade from true E2E to mocked integration
tests during migration.

## Guardrails

- [ ] Test runs against real application routes (no component-only harness).
- [ ] Test uses real backend APIs for core workflow behavior (no mutation stubs
      that bypass backend effects).
- [ ] Authentication uses configured setup/session flow
      (`playwright/tests/auth.setup.ts` storage state).
- [ ] Test data is provisioned through approved fixture/database workflows.
- [ ] Scenario remains valid when run in CI with default retries and artifact
      capture.

## Anti-Patterns to Avoid

- Replacing business-critical API calls with mocks while still labeling as E2E.
- Asserting only mocked response payload without validating UI state transition.
- Relying on unstable local-only seed state that CI cannot reproduce.
