# Biorepository QC Requirement Gap Matrix

| Requirement Area                   | Current Implementation                                         | Gap                                                | Implemented in this change                                                                                        |
| ---------------------------------- | -------------------------------------------------------------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Periodic random QC selection       | Manual sample selection on QC page                             | No server-generated random QC round                | Added `generate-round` API and UI action to generate `N boxes x M samples`                                        |
| QC round tracking                  | QC records exist per sample                                    | No round/batch linkage                             | Added `qc_batch_id` on QC inspection and request payload support                                                  |
| Expected coordinate snapshot       | QC checklist includes correct-position flag                    | No expected coordinate snapshot at inspection time | Added `expected_coordinate_snapshot` field for traceable expected location context                                |
| Failure taxonomy coverage          | Existing discrepancy enums (missing, damaged, misplaced, etc.) | Missing explicit client categories                 | Added new discrepancy types (wrong sample, empty slot registered occupied, labeling error, box/rack misplacement) |
| Server-side QC fail validation     | UI prompts discrepancy fields                                  | Backend accepted incomplete failed records         | Enforced server-side requirement for discrepancy type, corrective action, and remarks on failed QC                |
| Corrective action audit linkage    | Chain-of-custody exists, storage metadata update exists        | No direct QC corrective action endpoint            | Added QC corrective action API that updates coordinate metadata and logs custody action details                   |
| Location/technician failure trends | Basic QC metrics and discrepancy counts                        | No failure trends by freezer/rack/technician       | Added dashboard APIs for QC failure trends and problematic location history                                       |
| Escalation signals                 | No threshold evaluator                                         | No batch/location alerting                         | Added 5% batch fail signal, repeated-location failures, and critical missing sample indicators                    |
| Dashboard visibility for trends    | Existing overview and detailed tabs                            | No UI for QC trend/escalation metrics              | Added trend and escalation sections to reporting tabs                                                             |
| PDF export fidelity                | PDF endpoint returned JSON bytes                               | Invalid PDF output despite PDF content type        | Implemented real PDF generation for dashboard and audit trail exports                                             |

## Notes

- Changes are additive and backward-compatible where possible.
- Existing QC page workflow is retained and extended; no major UI rewrite was
  introduced.
- Existing audit/storage primitives are reused instead of creating duplicate
  subsystems.
