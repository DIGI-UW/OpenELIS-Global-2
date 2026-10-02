import React from "react";
import {
  Button,
  Checkbox,
  DismissibleTag,
  FilterableMultiSelect,
  Stack,
  TextArea,
} from "@carbon/react";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathologyCaseView.scss";

/**
 * FR-12 to FR-13: what the pathologist read down the microscope, the
 * techniques that produced it, the coded and free-text conclusions, and the
 * two decisions that close the reading — refer for immunohistochemistry, and
 * release.
 *
 * The three multiselects are remounted with the form version for the reason
 * given in ReviewSection; the two checkboxes are controlled for the same
 * reason, since an uncontrolled box keeps the tick a discard has just thrown
 * away.
 */
const FindingsSection = ({
  caseInfo,
  updateCase,
  readOnly,
  techniques,
  conclusions,
  immunoHistoChemistryTests,
  pagination,
  currentApiPage,
  totalApiPages,
  previousPage,
  nextPage,
  onPreviousPage,
  onNextPage,
  formVersion,
}) => {
  const intl = useIntl();

  // Carbon labels a tag's close button from title, and falls back to
  // dismissTooltipLabel only once the tag's own text has been ellipsised, so
  // both have to be given or the button reads Carbon's English "Dismiss".
  const removeLabel = (name) =>
    intl.formatMessage({ id: "common.removeSelection" }, { name });

  const removeFrom = (listKey, index) =>
    updateCase((prev) => ({
      [listKey]: (prev[listKey] ?? []).filter(
        (_, position) => position !== index,
      ),
    }));

  // i18n-keys: pathology.locked.release*
  //
  // Which assignment is missing, not merely that one is: a hint that names a
  // condition the case already satisfies sends the pathologist to check a
  // field that is already filled in (S-7.4).
  const missingAssignmentKey = !caseInfo.assignedPathologistId
    ? "pathology.locked.releaseNoPathologist"
    : !caseInfo.assignedTechnicianId
      ? "pathology.locked.releaseNoTechnician"
      : null;

  return (
    <Stack gap={6}>
      <TextArea
        id="microscopyExam"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({ id: "pathology.label.microexam" })}
        value={caseInfo.microscopyExam}
        onChange={(e) => updateCase({ microscopyExam: e.target.value })}
      />
      <div className="pathology-case-view__picker-row">
        <div className="pathology-case-view__field-group">
          <FilterableMultiSelect
            key={"techniques-" + formVersion}
            id="techniques"
            disabled={readOnly}
            titleText={<FormattedMessage id="pathology.label.techniques" />}
            items={techniques}
            itemToString={(item) => (item ? item.value : "")}
            initialSelectedItems={caseInfo.techniques}
            onChange={(changes) =>
              updateCase({ techniques: changes.selectedItems })
            }
            placeholder={intl.formatMessage({ id: "common.select" })}
            selectionFeedback="top-after-reopen"
          />
        </div>
        <div className="pathology-case-view__chips">
          {(caseInfo.techniques ?? []).map((technique, index) => (
            <DismissibleTag
              key={index}
              text={technique.value}
              disabled={readOnly}
              onClose={() => removeFrom("techniques", index)}
              title={removeLabel(technique.value)}
              dismissTooltipLabel={removeLabel(technique.value)}
            />
          ))}
        </div>
      </div>
      {pagination && (
        <Stack gap={3}>
          <span className="cds--type-helper-text-01">
            {currentApiPage} / {totalApiPages}
          </span>
          <Stack orientation="horizontal" gap={3}>
            <Button
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "pagination.backward",
              })}
              disabled={previousPage === null}
              onClick={onPreviousPage}
              renderIcon={ArrowLeft}
              size="sm"
            />
            <Button
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "pagination.forward",
              })}
              disabled={nextPage === null}
              renderIcon={ArrowRight}
              onClick={onNextPage}
              size="sm"
            />
          </Stack>
        </Stack>
      )}
      <div className="pathology-case-view__picker-row">
        <div className="pathology-case-view__field-group">
          <FilterableMultiSelect
            key={"conclusion-" + formVersion}
            id="conclusion"
            disabled={readOnly}
            titleText={<FormattedMessage id="pathology.label.conclusion" />}
            items={conclusions}
            itemToString={(item) => (item ? item.value : "")}
            initialSelectedItems={caseInfo.conclusions}
            onChange={(changes) =>
              updateCase({ conclusions: changes.selectedItems })
            }
            placeholder={intl.formatMessage({ id: "common.select" })}
            selectionFeedback="top-after-reopen"
          />
        </div>
        <div className="pathology-case-view__chips">
          {(caseInfo.conclusions ?? []).map((conclusion, index) => (
            <DismissibleTag
              key={index}
              text={conclusion.value}
              disabled={readOnly}
              onClose={() => removeFrom("conclusions", index)}
              title={removeLabel(conclusion.value)}
              dismissTooltipLabel={removeLabel(conclusion.value)}
            />
          ))}
        </div>
      </div>
      <TextArea
        id="conclusionText"
        disabled={readOnly}
        labelText={intl.formatMessage({
          id: "pathology.label.textconclusion",
        })}
        value={caseInfo.conclusionText}
        onChange={(e) => updateCase({ conclusionText: e.target.value })}
      />
      <Checkbox
        labelText={intl.formatMessage({ id: "pathology.label.refer" })}
        id="referToImmunoHistoChemistry"
        disabled={readOnly}
        checked={Boolean(caseInfo.referToImmunoHistoChemistry)}
        onChange={() =>
          updateCase({
            referToImmunoHistoChemistry: !caseInfo.referToImmunoHistoChemistry,
          })
        }
      />
      {caseInfo.referToImmunoHistoChemistry && (
        <div className="pathology-case-view__picker-row">
          <div className="pathology-case-view__field-group">
            <FilterableMultiSelect
              key={"ihctests-" + formVersion}
              id="ihctests"
              disabled={readOnly}
              titleText={<FormattedMessage id="label.button.select.test" />}
              items={immunoHistoChemistryTests}
              itemToString={(item) => (item ? item.value : "")}
              onChange={(changes) =>
                updateCase({
                  immunoHistoChemistryTestIds: changes.selectedItems,
                })
              }
              placeholder={intl.formatMessage({ id: "common.select" })}
              selectionFeedback="top-after-reopen"
            />
          </div>
          <div className="pathology-case-view__chips">
            {(caseInfo.immunoHistoChemistryTestIds ?? []).map((test, index) => (
              <DismissibleTag
                key={index}
                text={test.value}
                disabled={readOnly}
                onClose={() => removeFrom("immunoHistoChemistryTestIds", index)}
                title={removeLabel(test.value)}
                dismissTooltipLabel={removeLabel(test.value)}
              />
            ))}
          </div>
        </div>
      )}
      {/*
        Rendered whether or not it can be used, and disabled with the reason
        it cannot: a release the case is not yet ready for is a fact the
        pathologist needs, and a control that is simply absent explains
        nothing (S-7.4).
      */}
      <Checkbox
        labelText={intl.formatMessage({ id: "pathology.label.release" })}
        id="release"
        disabled={readOnly || Boolean(missingAssignmentKey)}
        title={
          missingAssignmentKey
            ? intl.formatMessage({ id: missingAssignmentKey })
            : undefined
        }
        helperText={
          missingAssignmentKey
            ? intl.formatMessage({ id: missingAssignmentKey })
            : undefined
        }
        checked={Boolean(caseInfo.release)}
        onChange={() => updateCase({ release: !caseInfo.release })}
      />
    </Stack>
  );
};

export default FindingsSection;
