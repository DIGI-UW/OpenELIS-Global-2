import React from "react";
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
 * - Optional consent reference number
 * - Warning when consent is not provided
 * - Read-only audit info once consent is recorded server-side
 */

const InformedConsentSection = ({
  consentData = {},
  onConsentChange,
  isReadOnly = false,
}) => {
  const intl = useIntl();

  const {
    consentProvided = false,
    consentReferenceNo = "",
    consentRecordedAt = "",
    consentRecordedBy = "",
  } = consentData;

  const handleConsentChange = (field, value) => {
    onConsentChange({
      ...consentData,
      [field]: value,
    });
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
      {!consentProvided && (
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
          <Column lg={8} md={4} sm={4} className="consent-field">
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
              maxLength={100}
              value={consentReferenceNo}
              onChange={(e) =>
                handleConsentChange("consentReferenceNo", e.target.value)
              }
              disabled={isReadOnly}
            />
          </Column>
        )}

        {/* Consent audit information - read-only */}
        {consentProvided && consentRecordedAt && (
          <Column lg={16} md={8} sm={4} className="consent-field">
            <div className="consent-audit">
              <p>
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
