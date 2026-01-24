# E2E Test Failure Analysis
Generated: Fri Jan 23 22:57:34 UTC 2026

## Summary

- Error logs found: 2
- Screenshots found: 0

## Error Logs Analysis

### chunk_0_to_2_20260123_224711.log

No failures found in this log.

**Tests that ran:**
```
  Running:  login.cy.js                                                                     (1 of 3)
  Running:  home.cy.js                                                                      (2 of 3)
```

### chunk_3_to_7_20260123_225243.log

**Failures detected:**
```
  1 failing
     CypressError: Timed out retrying after 3050ms: `cy.click()` failed because this element:
     earch Patient -- Search patient By Date Of Birth (failed).png                                  
  1 failing
     AssertionError: Timed out retrying after 3000ms: Expected to find element: `#referralReasonId_0_1`, but never found it.
     der Entity -- Select sample type (failed).png                                                  
    ✖  2 of 5 failed (40%)                      04:17       48       40        2        -        6  
```

**Tests that ran:**
```
  Running:  AdminE2E/providerManagement.cy.js                                               (1 of 5)
  Running:  patientEntry.cy.js                                                              (2 of 5)
  Running:  orderEntity.cy.js                                                               (3 of 5)
  Running:  AdminE2E/barcode.cy.js                                                          (4 of 5)
  Running:  AdminE2E/batchTestReassignmentandCancelation.cy.js                              (5 of 5)
```

## Screenshot Analysis

No screenshots found.

## Common Failure Patterns

- Timeout errors: cypress/e2e-reports/chunk_0_to_2_20260123_224711.log:0
cypress/e2e-reports/chunk_3_to_7_20260123_225243.log:0
0
- Element not found: cypress/e2e-reports/chunk_0_to_2_20260123_224711.log:0
cypress/e2e-reports/chunk_3_to_7_20260123_225243.log:0
0
- Network errors: cypress/e2e-reports/chunk_0_to_2_20260123_224711.log:0
cypress/e2e-reports/chunk_3_to_7_20260123_225243.log:0
0

## Proposed Solutions

## Next Steps

1. Review screenshots manually to understand UI state at failure
2. Check browser console logs in test output for JavaScript errors
3. Verify test data fixtures are loaded correctly
4. Run individual failing tests with: `npx cypress run --spec <test-file>`
5. Enable video recording for debugging: Set `video: true` in cypress.config.js

