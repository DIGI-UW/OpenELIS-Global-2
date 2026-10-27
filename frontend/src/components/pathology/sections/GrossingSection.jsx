import React, { useState } from "react";
import {
  Heading,
  Section,
  Select,
  SelectItem,
  Stack,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import IdentifiedRow, { ListToolbar } from "../IdentifiedRow";
import {
  cassetteStateBadge,
  countedRows,
  deactivatedCount,
  labelStreamUrl,
  rowListEditors,
  unsavedPosition,
} from "../pathologyRows";
import "../pathologyCaseView.scss";

/**
 * FR-3: what the bench did with the specimen at grossing: the macroscopic
 * description, the cassettes cut from it, and who worked it.
 * A saved cassette is named by the server and only ever deactivated (S-10.4).
 */
const GrossingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  technicianUsers,
  dirty,
  saving,
  onDeactivate,
}) => {
  const intl = useIntl();
  const [showDeactivated, setShowDeactivated] = useState(false);

  const blocks = caseInfo.blocks ?? [];
  const { patchRow, removeRow, addRow } = rowListEditors("blocks", updateCase);

  return (
    <Stack gap={6}>
      <TextArea
        id="grossExam"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({ id: "pathology.label.grossexam" })}
        value={caseInfo.grossExam ?? ""}
        onChange={(e) => updateCase({ grossExam: e.target.value })}
      />
      <div>
        <Section>
          <Heading className="pathology-case-view__heading">
            <FormattedMessage id="pathology.label.cassettes" />
          </Heading>
        </Section>
        {blocks.map((block, index) => (
          <IdentifiedRow
            key={block.id ?? block.clientKey}
            kind="block"
            row={block}
            position={unsavedPosition(blocks, index)}
            readOnly={readOnly}
            showDeactivated={showDeactivated}
            deactivateLocked={dirty || saving}
            deactivateLockedReason={intl.formatMessage({
              id: "pathology.locked.saveBeforeDeactivate",
            })}
            stateBadge={cassetteStateBadge(block)}
            onRemove={() => removeRow(index)}
            onDeactivate={onDeactivate}
          >
            {({ objectName, disabled }) => (
              <div className="pathology-case-view__row-field">
                <TextInput
                  id={"blockLocation" + index}
                  disabled={disabled}
                  labelText={
                    <>
                      {intl.formatMessage({ id: "pathology.label.location" })}
                      <span className="cds--visually-hidden">
                        {" " + objectName}
                      </span>
                    </>
                  }
                  value={block.location ?? ""}
                  onChange={(e) =>
                    patchRow(index, { location: e.target.value })
                  }
                />
              </div>
            )}
          </IdentifiedRow>
        ))}
        <ListToolbar
          addLabelKey="pathology.action.addCassette"
          onAdd={() => addRow({ location: "" })}
          addDisabled={readOnly}
          printLabelKey="pathology.action.printCassetteLabels"
          printUrl={labelStreamUrl(
            config.serverBaseUrl,
            "block",
            caseInfo.labNumber,
          )}
          canPrint={countedRows(blocks).length > 0}
          deactivatedCount={deactivatedCount(blocks)}
          showDeactivated={showDeactivated}
          onToggleDeactivated={setShowDeactivated}
          toggleId="showDeactivatedCassettes"
        />
      </div>
      <div className="pathology-case-view__field-group">
        <Select
          id="assignedTechnician"
          name="assignedTechnician"
          disabled={readOnly}
          labelText={intl.formatMessage({
            id: "label.button.select.technician",
          })}
          value={caseInfo.assignedTechnicianId}
          onChange={(event) =>
            updateCase({ assignedTechnicianId: event.target.value })
          }
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "common.select" })}
          />
          {technicianUsers.map((user, index) => (
            <SelectItem key={index} text={user.value} value={user.id} />
          ))}
        </Select>
      </div>
    </Stack>
  );
};

export default GrossingSection;
