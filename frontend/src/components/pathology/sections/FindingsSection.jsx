import React from "react";
import {
  Checkbox,
  FilterableMultiSelect,
  InlineNotification,
  Stack,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import MacroTextArea from "../../common/textMacro/MacroTextArea";
import ConclusionPicker from "../ConclusionPicker";
import { formIsCritical } from "../pathologySections";
import SelectionChips from "../SelectionChips";
import "../pathologyCaseView.scss";

const NARRATIVE_MAX = 255;

const itemName = (item) => (item ? item.value : "");

/**
 * The picker's own items in place of the stored ones, matched by id: Carbon
 * compares selections by value, and a stored item read with other fields
 * would otherwise never show as ticked. A stored item the list no longer
 * holds is kept as it is, so the next pick does not drop it.
 */
const selectedFrom = (items, stored) =>
  (stored ?? []).map(
    (chosen) =>
      (items ?? []).find((item) => String(item.id) === String(chosen.id)) ??
      chosen,
  );

/**
 * FR-12 to FR-13: what the pathologist read under the microscope, the
 * techniques that produced it, the coded and narrative conclusions, and the
 * decisions that close the reading: refer for immunohistochemistry, and flag
 * the case as a critical result. Signing out is done from the action bar.
 *
 * From Ready for Pathologist on, the macroscopic description is completed
 * here and not under Grossing, so the case holds one such field at a time.
 *
 * The pickers are controlled by the case, so a removed chip unticks its item
 * and a discard puts back what was stored. The checkboxes are controlled for
 * the same reason: an uncontrolled box keeps the tick a discard has just
 * thrown away.
 */
const FindingsSection = ({
  caseInfo,
  updateCase,
  readOnly,
  techniques,
  immunoHistoChemistryTests,
  formVersion,
  canAddDiagnosis,
}) => {
  const intl = useIntl();

  const removeFrom = (listKey, id) =>
    updateCase((prev) => ({
      [listKey]: (prev[listKey] ?? []).filter(
        (item) => String(item.id) !== String(id),
      ),
    }));

  const chips = (listKey, testId, fallbackFocusId) => (
    <SelectionChips
      items={caseInfo[listKey] ?? []}
      label={itemName}
      onRemove={(item) => removeFrom(listKey, item.id)}
      readOnly={readOnly}
      testId={testId}
      fallbackFocusId={fallbackFocusId}
    />
  );

  const noTechniques = Array.isArray(techniques) && techniques.length === 0;
  const referred = Boolean(caseInfo.referredToIhc);
  const markersOffered =
    referred || Boolean(caseInfo.referToImmunoHistoChemistry);
  // The markers the order already holds as referral analyses are shown as
  // referred and not offered again: the save would not order them twice.
  const referredIds = new Set(
    (caseInfo.ihcReferredTestIds ?? []).map((id) => String(id)),
  );
  const referredMarkers = referred
    ? (immunoHistoChemistryTests ?? []).filter((item) =>
        referredIds.has(String(item.id)),
      )
    : [];
  const markersToOrder = (immunoHistoChemistryTests ?? []).filter(
    (item) => !referredIds.has(String(item.id)),
  );

  const critical = formIsCritical(caseInfo);

  return (
    <Stack gap={6}>
      {critical && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          role="alert"
          data-testid="critical-result-banner"
          subtitle={intl.formatMessage({
            id: caseInfo.signedOut
              ? "pathology.banner.criticalResultSignedOut"
              : "pathology.banner.criticalResult",
          })}
        />
      )}
      <MacroTextArea
        id="grossExam"
        context="PATHOLOGY_GROSS_DESCRIPTION"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({ id: "pathology.label.grossExam" })}
        value={caseInfo.grossExam ?? ""}
        onChange={(e) => updateCase({ grossExam: e.target.value })}
      />
      <MacroTextArea
        id="microscopyExam"
        context="PATHOLOGY_MICROSCOPIC_DESCRIPTION"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({
          id: "pathology.label.microscopyExam",
        })}
        value={caseInfo.microscopyExam ?? ""}
        onChange={(e) => updateCase({ microscopyExam: e.target.value })}
      />
      <div className="pathology-case-view__picker-row">
        <div className="pathology-case-view__field-group">
          <FilterableMultiSelect
            key={"techniques-" + formVersion}
            id="techniques"
            disabled={readOnly || noTechniques}
            titleText={<FormattedMessage id="pathology.label.techniques" />}
            helperText={
              noTechniques
                ? intl.formatMessage({ id: "pathology.empty.noTechniques" })
                : undefined
            }
            items={techniques ?? []}
            itemToString={itemName}
            selectedItems={selectedFrom(techniques, caseInfo.techniques)}
            onChange={(changes) =>
              updateCase({ techniques: changes.selectedItems })
            }
            placeholder={intl.formatMessage({ id: "common.select" })}
            selectionFeedback="top-after-reopen"
          />
        </div>
        {chips("techniques", "findings-technique-chips", "techniques-input")}
      </div>
      <ConclusionPicker
        value={caseInfo.conclusions ?? []}
        onChange={(next) => updateCase({ conclusions: next })}
        readOnly={readOnly}
        canAddDiagnosis={canAddDiagnosis}
      />
      <TextArea
        id="conclusionText"
        disabled={readOnly}
        labelText={intl.formatMessage({
          id: "pathology.label.narrativeConclusion",
        })}
        enableCounter
        maxCount={NARRATIVE_MAX}
        value={caseInfo.conclusionText ?? ""}
        onChange={(e) => updateCase({ conclusionText: e.target.value })}
      />
      <Checkbox
        id="criticalResultFlagged"
        labelText={intl.formatMessage({ id: "pathology.label.flagCritical" })}
        disabled={readOnly}
        checked={Boolean(caseInfo.criticalResultFlagged)}
        onChange={() =>
          updateCase({ criticalResultFlagged: !caseInfo.criticalResultFlagged })
        }
      />
      {/*
        A referral once saved is not withdrawn from this side, so the box
        shows it ticked and fixed. Markers can still be added to it.
      */}
      {referred ? (
        <Checkbox
          id="referToImmunoHistoChemistry"
          labelText={intl.formatMessage({ id: "pathology.label.referIhc" })}
          checked
          disabled
          readOnly
          helperText={
            <span data-testid="findings-ihc-referred">
              {intl.formatMessage({ id: "pathology.label.ihcReferred" })}
            </span>
          }
        />
      ) : (
        <Checkbox
          id="referToImmunoHistoChemistry"
          labelText={intl.formatMessage({ id: "pathology.label.referIhc" })}
          disabled={readOnly}
          checked={Boolean(caseInfo.referToImmunoHistoChemistry)}
          onChange={() =>
            updateCase({
              referToImmunoHistoChemistry:
                !caseInfo.referToImmunoHistoChemistry,
            })
          }
        />
      )}
      {referredMarkers.length > 0 && (
        <div className="pathology-case-view__field-group">
          <p className="cds--label">
            {intl.formatMessage({ id: "pathology.label.ihcMarkersReferred" })}
          </p>
          <SelectionChips
            items={referredMarkers}
            label={itemName}
            readOnly
            testId="findings-ihc-referred-markers"
          />
        </div>
      )}
      {markersOffered && (
        <div className="pathology-case-view__picker-row">
          <div className="pathology-case-view__field-group">
            <FilterableMultiSelect
              key={"ihctests-" + formVersion}
              id="ihctests"
              disabled={readOnly}
              titleText={<FormattedMessage id="pathology.label.ihcMarkers" />}
              items={markersToOrder}
              itemToString={itemName}
              selectedItems={selectedFrom(
                markersToOrder,
                caseInfo.immunoHistoChemistryTestIds,
              )}
              onChange={(changes) =>
                updateCase({
                  immunoHistoChemistryTestIds: changes.selectedItems,
                })
              }
              placeholder={intl.formatMessage({ id: "common.select" })}
              selectionFeedback="top-after-reopen"
            />
          </div>
          {chips(
            "immunoHistoChemistryTestIds",
            "findings-ihc-marker-chips",
            "ihctests-input",
          )}
        </div>
      )}
    </Stack>
  );
};

export default FindingsSection;
