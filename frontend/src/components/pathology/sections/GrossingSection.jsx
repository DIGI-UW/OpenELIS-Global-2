import React, { useState } from "react";
import {
  Checkbox,
  ComboBox,
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
  tissueTypeItems,
  unsavedPosition,
} from "../pathologyRows";
import "../pathologyCaseView.scss";

/**
 * FR-3: what the bench did with the specimen at grossing: the macroscopic
 * description, whether it needs decalcifying, the cassettes cut from it and
 * the tissue each holds, and who worked it. The decalcification flag stops
 * being editable once the stored case is past that stage.
 * A saved cassette is named by the server and only ever deactivated (S-10.4).
 */
const GrossingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  technicianUsers,
  tissueTypes,
  decalcificationFlagFrozen,
  dirty,
  saving,
  onDeactivate,
}) => {
  const intl = useIntl();
  const [showDeactivated, setShowDeactivated] = useState(false);

  const blocks = caseInfo.blocks ?? [];

  const { patchRow, removeRow, addRow } = rowListEditors("blocks", updateCase);
  // The list comes from a dictionary category each laboratory fills itself,
  // so an empty one is a configuration gap the bench is told about rather
  // than a picker that silently offers nothing. Until the list has arrived
  // there is nothing to pick and nothing yet to say about it.
  const tissueTypesUnknown = !Array.isArray(tissueTypes);
  const noTissueTypes = !tissueTypesUnknown && tissueTypes.length === 0;

  return (
    <Stack gap={6}>
      <TextArea
        id="grossExam"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({ id: "pathology.label.grossExam" })}
        value={caseInfo.grossExam ?? ""}
        onChange={(e) => updateCase({ grossExam: e.target.value })}
      />
      <Checkbox
        id="decalcificationRequired"
        disabled={readOnly || decalcificationFlagFrozen}
        labelText={intl.formatMessage({
          id: "pathology.label.decalcificationRequired",
        })}
        checked={Boolean(caseInfo.decalcificationRequired)}
        onChange={(_, { checked }) =>
          updateCase({ decalcificationRequired: checked })
        }
        // Carbon renders the helper text under the box but does not point the
        // box at it, so the hint carries its own id and the box names it.
        helperText={
          decalcificationFlagFrozen ? (
            <span id="decalcificationRequiredHint">
              {intl.formatMessage({
                id: "pathology.locked.decalcificationPassed",
              })}
            </span>
          ) : undefined
        }
        aria-describedby={
          decalcificationFlagFrozen ? "decalcificationRequiredHint" : undefined
        }
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
            {({ hiddenName, disabled }) => {
              const tissueItems = tissueTypeItems(
                tissueTypes,
                block,
                caseInfo.tissueTypeNames,
              );
              return (
                <>
                  <div className="pathology-case-view__row-field">
                    <TextInput
                      id={"blockLocation" + index}
                      disabled={disabled}
                      labelText={
                        <>
                          {intl.formatMessage({
                            id: "pathology.label.location",
                          })}
                          {hiddenName}
                        </>
                      }
                      value={block.location ?? ""}
                      onChange={(e) =>
                        patchRow(index, { location: e.target.value })
                      }
                    />
                  </div>
                  <div className="pathology-case-view__row-field">
                    <ComboBox
                      id={"blockTissueType" + index}
                      size="md"
                      titleText={
                        <>
                          {intl.formatMessage({
                            id: "pathology.label.tissueType",
                          })}
                          {hiddenName}
                        </>
                      }
                      placeholder={intl.formatMessage({ id: "common.select" })}
                      items={tissueItems}
                      itemToString={(item) => item?.value ?? ""}
                      selectedItem={
                        tissueItems.find(
                          (tissue) =>
                            String(tissue.id) === String(block.tissueTypeId),
                        ) ?? null
                      }
                      onChange={({ selectedItem }) =>
                        patchRow(index, {
                          tissueTypeId: selectedItem ? selectedItem.id : null,
                        })
                      }
                      disabled={disabled || tissueTypesUnknown || noTissueTypes}
                      helperText={
                        // A row locked for its own reasons says nothing about
                        // the list, which is not what keeps it locked.
                        noTissueTypes && !disabled
                          ? intl.formatMessage({
                              id: "pathology.empty.noTissueTypes",
                            })
                          : undefined
                      }
                    />
                  </div>
                </>
              );
            }}
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
