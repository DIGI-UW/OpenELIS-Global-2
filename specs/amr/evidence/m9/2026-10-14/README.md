# M9 audit evidence — 14 October 2026

Baseline evidence for the [workflow audit](../../../milestone-9-validation.md).
Candidate: `9471be12ba997998bd3ddf64db69551b6e8f8aa0`.
All case data was synthetic and created through the local application scenario
service. Identifiers refer to that local database snapshot, not portable fixtures.
Use fresh scenario data when reproducing on another stack.

Screenshots preserve the observed application/reference rendering, including
narrow-panel captures. The application desktop capture measured 1280 × 720; the reference desktop
capture is 1440 × 1000, so this is not a pixel-for-pixel comparison. These are
baseline observations, not evidence of fixes or complete accessibility coverage.

## Captures

- [01-default-case.jpg](01-default-case.jpg)
- [02-default-release.jpg](02-default-release.jpg)
- [03-alternate-case.jpg](03-alternate-case.jpg)
- [04-culture-persistence.jpg](04-culture-persistence.jpg)
- [05-incoming-hidden.jpg](05-incoming-hidden.jpg)
- [06-incoming-waiting.jpg](06-incoming-waiting.jpg)
- [07-filtered-worklist.jpg](07-filtered-worklist.jpg)
- [08-bench-filter-loss.jpg](08-bench-filter-loss.jpg)
- [09-reference-case.jpg](09-reference-case.jpg)
- [10-ast-generic-error.jpg](10-ast-generic-error.jpg)
- [11-final-report.jpg](11-final-report.jpg)
- [12-environmental-final.jpg](12-environmental-final.jpg)
- [13-isolate-referral.jpg](13-isolate-referral.jpg)
- [14-reference-desktop.jpg](14-reference-desktop.jpg)
- [15-default-desktop.jpg](15-default-desktop.jpg)
- [16-release-blocker-detail.jpg](16-release-blocker-detail.jpg)

## Synthetic response snapshots

- [fixtures.json](fixtures.json): scenario keys and generated identifiers.
- [clock.json](clock.json): laboratory clock and timezone.
- [isolate.json](isolate.json): saved identification history and referral summary.

No credentials or authentication state are included. The unverified PDF response
and earlier superseded audit are not part of this committed evidence set.
