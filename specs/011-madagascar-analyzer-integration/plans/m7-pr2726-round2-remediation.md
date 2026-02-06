# PR #2726 Round 2 — New Comments Remediation

All 6 items decided as **Fix** in answer session.

## Decision Summary

| #   | Comment                                    | Decision |
| --- | ------------------------------------------ | -------- |
| 1   | AnalyzerForm timer cleanup on modal close  | **Fix**  |
| 2   | Centralize auth() helper (8 specs)         | **Fix**  |
| 3   | waitForBackendReady self-signed TLS        | **Fix**  |
| 4   | Cypress credentials — fail in CI only      | **Fix**  |
| 5   | frontend-qa.yml vars/secrets with defaults | **Fix**  |
| 6   | MappingActivationModal.css var fallbacks   | **Fix**  |

## Implementation Details

### 1. AnalyzerForm timer cleanup

- Clear `closeTimeoutRef` when `open` transitions to `false`, not just on
  unmount
- Add `open` to cleanup effect deps or clear in onClose handler

### 2. Centralize auth() helper

- Add `Cypress.getBasicAuth` in commands.js
- Update 8 analyzer specs to use it

### 3. waitForBackendReady TLS + drain

- Add `rejectUnauthorized: false` for localhost HTTPS
- Call `res.resume()` to drain response

### 4. Cypress credentials — fail in CI only

- When CI: require env vars, throw if missing
- Local: keep fallbacks

### 5. frontend-qa.yml

- Use `vars.TEST_USER || 'admin'` and `secrets.TEST_PASS || 'adminADMIN!'`

### 6. MappingActivationModal.css

- Add fallbacks: `var(--cds-support-warning, #f1c21b)`,
  `var(--cds-border-subtle, #e0e0e0)`
