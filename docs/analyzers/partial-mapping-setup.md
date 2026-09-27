# Analyzer setup with partial mappings

Analyzer Types still require an operator to review and confirm mappings. A
confirmed revision can contain unresolved rows: those rows are not verified or
excluded, and they do not prevent the analyzer from connecting.
Connection/profile readiness and review of the exact mapping revision still
apply.

New shared mappings use profile LOINC defaults when the active local catalog has
a unique compatible match. Repeated LOINCs require a unique profile
code/alias/name hint to distinguish the candidates. Categorical answers are
filled only when their labels match uniquely (ignoring case and whitespace). No
vendor-specific synonyms or positive/negative equivalences are guessed. Existing
saved choices are never overwritten when another analyzer is created or setup is
reopened.

## Correct and recover held results

1. Open **Analyzer Results → Import issues** and follow the mapping link for an
   unresolved result. Original test codes, values and source context are
   retained.
2. In **Analyzer Types**, select the correct local test and answer choices.
   Unrecognized codes and values observed in held traffic also appear in the
   editor. If no choices exist for a selected test, check the test selection and
   its catalog configuration.
3. Save and confirm the mapping. Remaining unresolved rows may stay unresolved;
   their incoming results will remain held.
4. For each analyzer that should use this revision, open setup, review
   **Verify**, and choose **Continue to Connect**. This explicitly adopts the
   revision and retries that analyzer's held mapping results in place. Saving or
   confirming a shared mapping alone does not change a running analyzer's
   selected revision.
5. Check Analyzer Results again. Resolved rows become available for the usual
   review. Unresolved rows remain held. Adoption does not accept results into a
   patient's clinical record.

Use **Undelivered results → Retry** for messages still queued in Bridge. That is
separate from mapping recovery: a message already accepted by OE has a delivery
receipt, and resending it intentionally does not create new work or duplicates.

**Exclude** means intentionally omit that code/value from clinical staging. It
is not a substitute for leaving an uncertain mapping unresolved. Previously held
rows are retained if the new mapping excludes them; this change does not delete
historical observations.

This hotfix does not change the Bridge profile/FHIR contract or complete the
separate work to make incoming clinical concepts fully independent of analyzer
codes.
