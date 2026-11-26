import React from "react";
import { InlineNotification, Stack } from "@carbon/react";
import { useIntl } from "react-intl";
import RunReferenceField from "./RunReferenceField";
import StageEventList from "./StageEventList";
import { stageEntries } from "../pathologySections";

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

  const processingEvents = stageEntries(caseInfo, "PROCESSING");

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
      <RunReferenceField
        id="runReference"
        stage="PROCESSING"
        caseInfo={caseInfo}
        value={caseInfo.runReference}
        recordable={caseInfo.runReferenceEditable}
        readOnly={readOnly}
        labelKey="pathology.label.runReference"
        onceKey="pathology.label.runReferenceOnce"
        missingKey="pathology.locked.noStageEvent"
        onChange={(runReference) => updateCase({ runReference })}
      />
      {processingEvents.length > 0 && (
        <StageEventList
          events={processingEvents}
          ariaLabelKey="pathology.stage.processing"
          showStage={false}
          runReferenceKey="pathology.label.runReference"
        />
      )}
    </Stack>
  );
};

export default ProcessingSection;
