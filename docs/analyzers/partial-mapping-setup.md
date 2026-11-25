# Analyzer setup with partial mappings

Each analyzer has its own mapping of its profile's tests and answers to the
local catalog. A new analyzer starts from the defaults the resolver finds by
exact standard-code match against the active catalog (test LOINC, answer code);
the Analyzer Types page shows the same defaults, read-only. Nothing is shared,
so editing one analyzer's mapping never changes another's, and every row records
whether it is a default or an operator edit.

An operator reviews and confirms the analyzer's mapping before it is applied. A
confirmed revision can contain unresolved rows: those rows are not verified or
excluded, and they do not prevent the analyzer from connecting. Each unresolved
row keeps the reason it could not be resolved (no match, ambiguous, or
incompatible), and the resolver never chooses between two usable candidates.
Existing choices are never overwritten when setup is reopened.

## A catalog change affects only the relevant results

For a previously confirmed configuration, an inactive test or answer choice
holds only observations that use that mapping. Other valid observations continue
to ordinary review. The original value and source payload remain available for
correction and retry. Restoring the catalog entry or applying a corrected,
confirmed mapping recovers the saved observation in place.

The mapping editor may still show the overall configuration as needing
attention; that status does not make every incoming observation invalid. The
selected profile, mapping revision, recorded review and control-recognition
configuration must still match. A newly saved but unconfirmed replacement does
not inherit a previous revision's confirmation.

## Controls that report an answer

A control that reports an answer instead of a number (a positive or negative
cartridge) is judged against the Test Catalog QC target for its test and control
level, with a lot's own target taking precedence. OpenELIS records PASS when the
mapped answer is the expected one and FAIL otherwise; a FAIL enters corrective
action like any other QC rule violation. A control whose test and level have no
expected answer is held on the review page with a link to set the target;
applying the analyzer's mapping afterwards retries it.

## Failed runs

A run that produced no result (ERROR, NO RESULT) arrives with no value and is
held; it is never a patient result. The review row shows what the instrument
reported and its own note on the run. **Dismiss as failed run** records both as
an internal note on the one test the run was for, removes the row and leaves the
test waiting for the repeat. It is refused when the run matches no single test.

## Correct and recover held results

1. Open the analyzer's results in **Analyzer Results**. A held row shows its
   original code and value; follow its **Review analyzer mapping** link.
   Original test codes, values and source context are retained.
2. In the analyzer's mapping, select the correct local test and answer choices.
   Unrecognized codes and values observed in held traffic also appear in the
   editor. If no choices exist for a selected test, check the test selection and
   its catalog configuration. **Save mapping** lists every row that changes
   before it writes the new revision. Excluding a row and then un-excluding it
   restores its earlier choices.
3. Save and confirm the mapping. Remaining unresolved rows may stay unresolved;
   their incoming results will remain held.
4. Choose **Apply mappings and retry held results** to put the confirmed
   revision in force for that analyzer and retry its held rows in place. The
   setup **Verify → Continue to Connect** action does the same for an analyzer
   being configured. Saving or confirming a mapping alone does not change the
   revision an analyzer is running.
5. Check Analyzer Results again. Resolved rows become available for the usual
   review. Unresolved rows remain held. Applying a mapping does not accept
   results into a patient's clinical record.

For an **Awaiting specimen** row, choose one of the displayed specimen types and
accept it in Analyzer Results. The row stays staged if no valid specimen can be
selected; a held mapping result cannot be accepted through that choice. Results
received together that add new tests to an order share one specimen: choose a
type every one of them can use, or they all stay staged. When their tests have
no specimen type in common in the test catalog, there is nothing to choose; they
stay staged until the catalog gives those tests a common specimen type.

Use **Undelivered analyzer results → Retry** for messages still queued in
Bridge. That is separate from mapping recovery: a message already accepted by OE
has a delivery receipt, and resending it intentionally does not create new work
or duplicates.

**Do not receive** means intentionally omit that code/value from clinical
staging. It is not a substitute for leaving an uncertain mapping unresolved.
Previously held rows are retained if the new mapping excludes them; historical
observations are not deleted.
