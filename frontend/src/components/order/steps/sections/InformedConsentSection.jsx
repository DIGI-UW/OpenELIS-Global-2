import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  FormGroup,
  Checkbox,
  TextArea,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Toggle,
} from "@carbon/react";

/**
 * InformedConsentSection - Informed consent capture for sample collection
 *
 * Features:
 * - Consent acknowledgment checkbox
 * - Optional consent notes/comments
 * - Warning when consent is not provided
 * - Integration with sample collection workflow
 */

const InformedConsentSection = ({
  consentData = {},
  onConsentChange,
  isReadOnly = false,
}) => {
  const intl = useIntl();

  const [showWarning, setShowWarning] = useState(false);

  const {
    consentProvided = false,
    consentNotes = "",
    consentDate = "",
    consentWitness = "",
    alternativeConsentMethod = false,
  } = consentData;

  const handleConsentChange = (field, value) => {
    const updatedConsent = {
      ...consentData,
      [field]: value,
    };

    // Auto-populate consent date when consent is provided
    if (field === "consentProvided" && value) {
      const now = new Date();
      const yyyy = now.getFullYear();
      const mm = String(now.getMonth() + 1).padStart(2, "0");
      const dd = String(now.getDate()).padStart(2, "0");
      updatedConsent.consentDate = `${yyyy}-${mm}-${dd}`;
    }

    // Show warning if consent is withdrawn
    if (field === "consentProvided" && !value) {
      setShowWarning(true);
    } else {
      setShowWarning(false);
    }

    onConsentChange(updatedConsent);
  };

  return (
    <div className="informed-consent-section">
      <Heading size="compact-02">
        <FormattedMessage
          id="collect.informedConsent.title"
          defaultMessage="Informed Consent"
        />
      </Heading>

      {/* Warning when consent is not provided */}
      {(!consentProvided || showWarning) && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "collect.informedConsent.warning.title",
            defaultMessage: "Consent required",
          })}
          subtitle={intl.formatMessage({
            id: "collect.informedConsent.warning.message",
            defaultMessage:
              "Sample collection requires informed consent from the patient or authorized representative.",
          })}
          hideCloseButton
          lowContrast
        />
      )}

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <FormGroup legendText="">
            {/* Primary consent checkbox */}
            <Checkbox
              id="consentProvided"
              labelText={intl.formatMessage({
                id: "collect.informedConsent.provided",
                defaultMessage:
                  "I confirm that informed consent has been obtained from the patient or their authorized representative for the collection and testing of this sample.",
              })}
              checked={consentProvided}
              onChange={(value) => handleConsentChange("consentProvided", value)}
              disabled={isReadOnly}
            />

            {/* Alternative consent method toggle */}
            <div style={{ marginTop: "1rem" }}>
              <Toggle
                id="alternativeConsentMethod"
                labelText={intl.formatMessage({
                  id: "collect.informedConsent.alternativeMethod",
                  defaultMessage: "Alternative consent method used (verbal, witness required)",
                })}
                toggled={alternativeConsentMethod}
                onToggle={(value) => handleConsentChange("alternativeConsentMethod", value)}
                disabled={isReadOnly}
              />
            </div>
          </FormGroup>
        </Column>

        {/* Consent witness field - shown when alternative method is used */}
        {alternativeConsentMethod && (
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <label htmlFor="consentWitness" className="cds--label">
                <FormattedMessage
                  id="collect.informedConsent.witness"
                  defaultMessage="Witness Name/ID"
                />
              </label>
              <input
                id="consentWitness"
                className="cds--text-input"
                type="text"
                placeholder={intl.formatMessage({
                  id: "collect.informedConsent.witness.placeholder",
                  defaultMessage: "Enter witness name or ID",
                })}
                value={consentWitness}
                onChange={(e) => handleConsentChange("consentWitness", e.target.value)}
                disabled={isReadOnly}
              />
            </div>
          </Column>
        )}

        {/* Consent notes/comments */}
        <Column lg={16} md={8} sm={4}>
          <div style={{ marginTop: "1rem" }}>
            <TextArea
              id="consentNotes"
              labelText={intl.formatMessage({
                id: "collect.informedConsent.notes",
                defaultMessage: "Consent Notes/Comments (Optional)",
              })}
              placeholder={intl.formatMessage({
                id: "collect.informedConsent.notes.placeholder",
                defaultMessage: "Additional information about consent process, patient concerns, etc.",
              })}
              value={consentNotes}
              onChange={(e) => handleConsentChange("consentNotes", e.target.value)}
              disabled={isReadOnly}
              rows={3}
            />
          </div>
        </Column>

        {/* Consent date - auto-populated but editable */}
        {consentProvided && (
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <label htmlFor="consentDate" className="cds--label">
                <FormattedMessage
                  id="collect.informedConsent.date"
                  defaultMessage="Consent Date"
                />
                <span className="helper-inline">
                  {" "}
                  <FormattedMessage
                    id="collect.informedConsent.date.helper"
                    defaultMessage="(auto-filled when consent is provided)"
                  />
                </span>
              </label>
              <input
                id="consentDate"
                className="cds--text-input"
                type="date"
                value={consentDate}
                onChange={(e) => handleConsentChange("consentDate", e.target.value)}
                disabled={isReadOnly}
                max={new Date().toISOString().split('T')[0]}
              />
            </div>
          </Column>
        )}
      </Grid>
    </div>
  );
};

export default InformedConsentSection;