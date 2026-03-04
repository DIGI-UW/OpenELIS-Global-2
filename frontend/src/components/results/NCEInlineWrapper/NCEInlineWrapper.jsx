import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  RadioButtonGroup,
  RadioButton,
  Loading,
} from "@carbon/react";
import { Catalog, ChemistryReference } from "@carbon/react/icons";
import NCEContextBanner from "./NCEContextBanner";
import useNCEInlineForm, { SUBCATEGORY_OPTIONS } from "./useNCEInlineForm";
import "./NCEInlineWrapper.scss";

/**
 * NCEInlineWrapper provides an inline form to create an NCE directly
 * from the Results Entry screen. It includes auto-populated context,
 * radio cards for severity/sample action, and a linked items section.
 */
const NCEInlineWrapper = ({
  resultId,
  context,
  onNCECreated,
  onCancel,
  alertId,
  sourceType,
  triggerType,
  triggerAction,
}) => {
  const intl = useIntl();
  const { formData, updateField, errors, isSubmitting, submitForm, closeForm } =
    useNCEInlineForm(resultId, {
      context,
      onSuccess: onNCECreated,
      alertId,
      sourceType,
      triggerType,
      triggerAction,
    });

  const handleCancel = () => {
    closeForm();
    onCancel?.();
  };

  return (
    <div className="nce-inline-wrapper">
      <div className="nce-inline-wrapper__header">
        <h4>
          <FormattedMessage id="nce.inline.title" />
        </h4>
      </div>

      <NCEContextBanner context={context} />

      <div className="nce-inline-wrapper__form">
        {/* Row 1: Category | Subcategory | Severity | Sample Action — all in one line */}
        <div className="nce-inline-wrapper__row nce-inline-wrapper__row--controls">
          <Select
            id="nce-category"
            labelText={intl.formatMessage({ id: "nce.inline.category" })}
            value={formData.category}
            onChange={(e) => updateField("category", e.target.value)}
            size="sm"
          >
            <SelectItem
              value="Analytical"
              text={intl.formatMessage({
                id: "nce.inline.category.analytical",
              })}
            />
            <SelectItem
              value="PreAnalytical"
              text={intl.formatMessage({
                id: "nce.inline.category.preAnalytical",
              })}
            />
            <SelectItem
              value="PostAnalytical"
              text={intl.formatMessage({
                id: "nce.inline.category.postAnalytical",
              })}
            />
          </Select>

          <Select
            id="nce-subcategory"
            labelText={intl.formatMessage({ id: "nce.inline.subcategory" })}
            value={formData.subcategory}
            onChange={(e) => updateField("subcategory", e.target.value)}
            size="sm"
          >
            {(SUBCATEGORY_OPTIONS[formData.category] || []).map((sub) => (
              <SelectItem
                key={sub}
                value={sub}
                text={intl.formatMessage({
                  id: `nce.inline.subcategory.${sub.replace(/\s+/g, "")}`,
                })}
              />
            ))}
          </Select>

          <div
            className={`nce-inline-wrapper__severity ${errors.severity ? "nce-inline-wrapper__severity--invalid" : ""}`}
          >
            <RadioButtonGroup
              legendText={intl.formatMessage({ id: "nce.inline.severity" })}
              name="nce-severity"
              orientation="horizontal"
              valueSelected={formData.severity}
              onChange={(value) => updateField("severity", value)}
            >
              <RadioButton
                id="nce-severity-critical"
                value="Critical"
                labelText={intl.formatMessage({
                  id: "nce.inline.severity.critical",
                })}
              />
              <RadioButton
                id="nce-severity-major"
                value="Major"
                labelText={intl.formatMessage({
                  id: "nce.inline.severity.major",
                })}
              />
              <RadioButton
                id="nce-severity-minor"
                value="Minor"
                labelText={intl.formatMessage({
                  id: "nce.inline.severity.minor",
                })}
              />
            </RadioButtonGroup>
            {errors.severity && (
              <span className="nce-inline-wrapper__error">
                {intl.formatMessage({ id: errors.severity })}
              </span>
            )}
          </div>

          <div
            className={`nce-inline-wrapper__sample-action ${errors.sampleAction ? "nce-inline-wrapper__sample-action--invalid" : ""}`}
          >
            <RadioButtonGroup
              legendText={intl.formatMessage({ id: "nce.inline.sampleAction" })}
              name="nce-sample-action"
              orientation="horizontal"
              valueSelected={formData.sampleAction}
              onChange={(value) => updateField("sampleAction", value)}
            >
              <RadioButton
                id="nce-sample-continue"
                value="CONTINUE_WITH_FLAG"
                labelText={intl.formatMessage({
                  id: "nce.inline.sampleAction.continueWithFlag",
                })}
              />
              <RadioButton
                id="nce-sample-reject"
                value="REJECT_SAMPLE"
                labelText={intl.formatMessage({
                  id: "nce.inline.sampleAction.rejectSample",
                })}
              />
            </RadioButtonGroup>
          </div>
        </div>

        {/* Row 2: NCE Title */}
        <TextInput
          id="nce-title"
          labelText={intl.formatMessage({ id: "nce.inline.nceTitle" })}
          placeholder={intl.formatMessage({
            id: "nce.inline.nceTitle.placeholder",
          })}
          value={formData.title}
          onChange={(e) => updateField("title", e.target.value)}
          size="sm"
        />

        {/* Row 3: Description + Immediate Action */}
        <div className="nce-inline-wrapper__row">
          <TextArea
            id="nce-description"
            labelText={intl.formatMessage({ id: "nce.inline.description" })}
            placeholder={intl.formatMessage({
              id: "nce.inline.description.placeholder",
            })}
            value={formData.description}
            onChange={(e) => updateField("description", e.target.value)}
            rows={2}
          />

          <TextArea
            id="nce-immediate-action"
            labelText={intl.formatMessage({
              id: "nce.inline.immediateAction",
            })}
            placeholder={intl.formatMessage({
              id: "nce.inline.immediateAction.placeholder",
            })}
            value={formData.immediateAction}
            onChange={(e) => updateField("immediateAction", e.target.value)}
            rows={2}
          />
        </div>

        {/* Footer: Linked Items left, spinner + actions right */}
        <div className="nce-inline-wrapper__footer">
          {context && (
            <div className="nce-linked-items">
              <h5 className="nce-linked-items__title">
                <Catalog size={14} />
                <FormattedMessage id="nce.inline.linkedItems" />
              </h5>
              <div className="nce-linked-items__list">
                {context.labNumber && (
                  <div className="nce-linked-items__item">
                    <ChemistryReference size={14} />
                    <span>
                      <FormattedMessage id="nce.inline.linkedItems.sample" />
                      {": "}
                      {context.labNumber}
                      {context.sampleType && ` – ${context.sampleType}`}
                    </span>
                  </div>
                )}
                {context.testName && (
                  <div className="nce-linked-items__item">
                    <ChemistryReference size={14} />
                    <span>
                      <FormattedMessage id="nce.inline.linkedItems.result" />
                      {": "}
                      {context.testName}
                      {context.resultValue && ` – ${context.resultValue}`}
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          <div className="nce-inline-wrapper__footer-actions">
            {isSubmitting && <Loading small withOverlay={false} />}
            {onCancel && (
              <Button
                kind="ghost"
                size="sm"
                onClick={handleCancel}
                disabled={isSubmitting}
              >
                <FormattedMessage id="nce.inline.cancel" />
              </Button>
            )}
            <Button
              kind="primary"
              size="sm"
              onClick={submitForm}
              disabled={isSubmitting}
            >
              <FormattedMessage id="nce.inline.submitNce" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NCEInlineWrapper;
