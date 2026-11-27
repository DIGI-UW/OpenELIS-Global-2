import React from "react";
import { TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

// The column a run reference is stored in, for every stage that keeps one.
const RUN_REFERENCE_MAX_LENGTH = 64;

/**
 * A reference written once on the case's entry into a stage: the processor
 * run at processing (FR-5.1), the coverslipping batch at coverslipping
 * (FR-8.4). The server says whether it can still be recorded, which is only
 * while the case stands at the stage on an entry that holds none yet, and
 * while the case stands there the field says so. A case that reached the
 * stage before entries were kept has none to record it on, and the field
 * says how to get one rather than inventing it. `held` holds the field for
 * a moment, while a request is out.
 */
const RunReferenceField = ({
  id,
  stage,
  caseInfo,
  value,
  recordable,
  readOnly,
  held = false,
  labelKey,
  onceKey,
  missingKey,
  onChange,
}) => {
  const intl = useIntl();
  const atStage = caseInfo.status === stage;
  const hasOpenEntry = (caseInfo.stageEvents ?? []).some(
    (event) => event.stage === stage && !event.completedAt,
  );
  const missingEntry = atStage && !hasOpenEntry;
  const editable = !readOnly && !held && Boolean(recordable);

  return (
    <div className="pathology-case-view__field-group">
      <TextInput
        id={id}
        labelText={intl.formatMessage({ id: labelKey })}
        maxLength={RUN_REFERENCE_MAX_LENGTH}
        value={value ?? ""}
        disabled={missingEntry}
        readOnly={!editable && !missingEntry}
        helperText={
          atStage
            ? intl.formatMessage({ id: missingEntry ? missingKey : onceKey })
            : undefined
        }
        onChange={(e) => editable && onChange(e.target.value)}
      />
    </div>
  );
};

export default RunReferenceField;
