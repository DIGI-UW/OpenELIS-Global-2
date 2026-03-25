# Real-Device Validation Checklist Template

Use this checklist to capture release evidence for analyzer bidirectional
validation against a real device.

## Metadata

- Feature/Stream:
- Analyzer model:
- Analyzer serial/device ID:
- Site/Lab:
- OpenELIS version/branch:
- Profile/version:
- Validator:
- Validation date:

## Preconditions

- [ ] Test environment is stable and network routing is confirmed.
- [ ] Analyzer profile is applied with final M3 settings.
- [ ] Required test samples/orders are prepared and traceable.
- [ ] Time synchronization is confirmed between OpenELIS and analyzer.
- [ ] Logging is enabled for bridge and analyzer import components.

## Pathway Gate 1: Results PUSH (Analyzer -> OpenELIS)

- [ ] Analyzer sends ASTM result payload to OpenELIS bridge.
- [ ] OpenELIS ingests payload without protocol/frame errors.
- [ ] Test mappings resolve and expected analyses are updated.
- [ ] QC and warning behavior matches expected rules.
- [ ] Final sample/analysis status is correct.

Evidence:

- Raw ASTM message capture:
- OpenELIS import logs:
- Database/API verification snapshot:
- UI/API result verification:

## Pathway Gate 2: Orders PULL (Analyzer queries OpenELIS)

- [ ] Analyzer initiates Q-record query to OpenELIS.
- [ ] OpenELIS returns protocol-compliant response (P/O or P/O/R as required).
- [ ] Returned order content matches pending tests for target accession.
- [ ] Analyzer accepts response without protocol rejection.

Evidence:

- Query request payload:
- OpenELIS responder payload:
- Analyzer acknowledgement/log:
- Order parity check notes:

## Pathway Gate 3: Orders PUSH (OpenELIS -> Analyzer)

- [ ] OpenELIS `send-order` endpoint invoked with target accession.
- [ ] Outbound ASTM H/P/O/L message content is valid.
- [ ] Analyzer receives and accepts order message.
- [ ] Transmission/ACK path completes without retries/failures.

Evidence:

- API request/response:
- Outbound payload capture:
- Analyzer receive log/screenshot:
- Error/retry metrics (if any):

## Pathway Gate 4: Results PULL (OpenELIS -> Analyzer)

- [ ] OpenELIS `query-results` endpoint sends H+Q request.
- [ ] Analyzer returns P/O/R segments with result values.
- [ ] OpenELIS ingests returned values through standard mapping flow.
- [ ] Imported result count and persisted values are verified.

Evidence:

- API request/response:
- Query payload and analyzer response capture:
- Import pipeline logs:
- Persisted result verification:

## Regression and Safety Checks

- [ ] Existing results-push harness regression remains green.
- [ ] No duplicate result imports after repeated queries.
- [ ] No cross-analyzer contamination of profile behavior.
- [ ] No RBAC violations for bidirectional endpoints.

## Sign-Off

- [ ] All four pathway gates passed.
- [ ] Evidence package is complete and archived.
- [ ] Release recommendation recorded.

Approver:

Date:
