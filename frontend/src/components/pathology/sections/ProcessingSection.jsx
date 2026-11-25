import React from "react";
import { InlineNotification, Stack, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import StageEventList from "./StageEventList";

// The column the run reference is stored in.
const RUN_REFERENCE_MAX_LENGTH = 64;

/**
 * FR-5.1: the processor run a case went through, recorded as free text on
 * the case's entry into processing, because OpenELIS has no register of
 * processor runs to pick one from yet, and the section says so.
 *
 * The reference belongs to the stage entry and is written once: the server
 * says whether it can still be recorded, which is only while the case stands
 * at processing on an entry that holds none yet, and while the case stands
 * there the field says so, and that a wrong one is put right by returning
 * the case and sending it to processing again, which opens a new entry. A
 * case that reached processing before entries were kept has no entry, and
 * the field says how to record one rather than inventing an entry to hold
 * it. The entries themselves are listed read-only below, each with its run.
 */
const ProcessingSection = ({ caseInfo, updateCase, readOnly }) => {
  const intl = useIntl();

  const processingEvents = (caseInfo.stageEvents ?? []).filter(
    (event) => event.stage === "PROCESSING",
  );
  const atProcessing = caseInfo.status === "PROCESSING";
  const hasOpenEntry = processingEvents.some((event) => !event.completedAt);
  const missingEntry = atProcessing && !hasOpenEntry;
  const editable = !readOnly && Boolean(caseInfo.runReferenceEditable);

  return (
    <Stack gap={6}>
      <InlineNotification
        kind="warning"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: "pathology.banner.runReferenceInterimTitle",
        })}
        subtitle={intl.formatMessage({
          id: "pathology.banner.runReferenceInterim",
        })}
      />
      <div className="pathology-case-view__field-group">
        <TextInput
          id="runReference"
          labelText={intl.formatMessage({ id: "pathology.label.runReference" })}
          maxLength={RUN_REFERENCE_MAX_LENGTH}
          value={caseInfo.runReference ?? ""}
          disabled={missingEntry}
          readOnly={!editable && !missingEntry}
          helperText={
            atProcessing
              ? intl.formatMessage({
                  id: missingEntry
                    ? "pathology.locked.noStageEvent"
                    : "pathology.label.runReferenceOnce",
                })
              : undefined
          }
          onChange={(e) =>
            editable && updateCase({ runReference: e.target.value })
          }
        />
      </div>
      {processingEvents.length > 0 && (
        <StageEventList
          events={processingEvents}
          ariaLabelKey="pathology.stage.processing"
          showStage={false}
          showRunReference
        />
      )}
    </Stack>
  );
};

export default ProcessingSection;
