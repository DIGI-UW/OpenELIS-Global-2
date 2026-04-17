import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  FormGroup,
  Checkbox,
  TextInput,
  Column,
  Grid,
  Heading,
  InlineNotification,
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
    consentReferenceNo = "",
    consentRecordedAt = "",
    consentRecordedBy = "",
  } = consentData;

  const handleConsentChange = (field, value) => {
    const updatedConsent = {
      ...consentData,
      [field]: value,
    };

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
              onChange={(_, { checked }) =>
                handleConsentChange("consentProvided", checked)
              }
              disabled={isReadOnly}
            />
          </FormGroup>
        </Column>

        {/* Consent reference number field - optional */}
        {consentProvided && (
          <Column lg={8} md={4} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <TextInput
                id="consentReferenceNo"
                labelText={intl.formatMessage({
                  id: "collect.informedConsent.referenceNo",
                  defaultMessage: "Consent Reference Number (Optional)",
                })}
                placeholder={intl.formatMessage({
                  id: "collect.informedConsent.referenceNo.placeholder",
                  defaultMessage: "Enter consent form or reference number",
                })}
                value={consentReferenceNo}
                onChange={(e) =>
                  handleConsentChange("consentReferenceNo", e.target.value)
                }
                disabled={isReadOnly}
              />
            </div>
          </Column>
        )}

        {/* Consent audit information - read-only */}
        {consentProvided && consentRecordedAt && (
          <Column lg={16} md={8} sm={4}>
            <div style={{ marginTop: "1rem", padding: "1rem", backgroundColor: "#f4f4f4", borderRadius: "4px" }}>
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#525252" }}>
                <strong>
                  <FormattedMessage
                    id="collect.informedConsent.auditInfo"
                    defaultMessage="Consent recorded:"
                  />
                </strong>{" "}
                {consentRecordedAt}
                {consentRecordedBy && (
                  <>
                    {" "}
                    <FormattedMessage
                      id="collect.informedConsent.auditBy"
                      defaultMessage="by user"
                    />{" "}
                    {consentRecordedBy}
                  </>
                )}
              </p>
            </div>
          </Column>
        )}
      </Grid>
    </div>
  );
};

export default InformedConsentSection;
