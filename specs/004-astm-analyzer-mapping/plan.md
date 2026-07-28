# Feature 004 Implementation Plan

## Current Milestone

Mapping configuration is delivered through
[OGC-1054](../OGC-1054-analyzer-qc-config/plan.md):

1. profile-applied test mappings;
2. pending analyzer-code review;
3. catalog-bound qualitative mappings;
4. fingerprinted mapping verification and audit;
5. activation-readiness integration;
6. Carbon mapping UI inside the four-step analyzer setup flow.

No new mapping persistence table is introduced for qualitative mappings or
verification metadata.

## Next Milestone

Create a separate spec and PR for result import and Results/Validation v4:

1. specify the analyzer target/component identity contract;
2. add failing bridge/OpenELIS contract tests;
3. add failing result-processing service tests;
4. implement stable component-code mapping with primary-component default;
5. integrate held-result resolution and reprocessing;
6. add a UI user story for visible imported-result validation.

The next milestone may require bridge work only if contract evidence proves the
current transport drops required identity.
