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
 * ConsentAccordionSection - Informed consent capture per OGC-557 FRS v1.1.
 *
 * Exported for reuse in Add Order, Edit Order, and the Sample Collection Wizard.
 *
 * Features:
 * - Consent acknowledgment checkbox (advisory per FR-5-001/FR-5-002)
 * - Optional consent form reference number
 * - Read-only audit info once consent is recorded server-side
 *
 * Spec: https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/informed-consent.md
 */

export const ConsentAccordionSection = ({
  consentData = {},
  onConsentChange,
  isReadOnly = false,
}) => {
  const intl = useIntl();

  const {
    consentGiven = false,
    consentFormReference = "",
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
      {!consentGiven && (
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
              id="consentGiven"
              labelText={intl.formatMessage({
                id: "collect.informedConsent.provided",
                defaultMessage:
                  "I confirm that informed consent has been obtained from the patient or their authorized representative for the collection and testing of this sample.",
              })}
              checked={consentGiven}
              onChange={(_, { checked }) =>
                handleConsentChange("consentGiven", checked)
              }
              disabled={isReadOnly}
            />
          </FormGroup>
        </Column>

        {/* Consent reference number field - optional */}
        {consentGiven && (
          <Column lg={8} md={4} sm={4} className="consent-field">
            <TextInput
              id="consentFormReference"
              labelText={intl.formatMessage({
                id: "collect.informedConsent.referenceNo",
                defaultMessage: "Consent Reference Number (Optional)",
              })}
              placeholder={intl.formatMessage({
                id: "collect.informedConsent.referenceNo.placeholder",
                defaultMessage: "Enter consent form or reference number",
              })}
              maxLength={100}
              value={consentFormReference}
              onChange={(e) =>
                handleConsentChange("consentFormReference", e.target.value)
              }
              disabled={isReadOnly}
            />
          </Column>
        )}

        {/* Consent audit information - read-only */}
        {consentGiven && consentRecordedAt && (
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

export default ConsentAccordionSection;
