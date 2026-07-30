import React from "react";
import { useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Checkbox,
  TextInput,
  Tag,
  Stack,
} from "@carbon/react";
import CustomDatePicker from "../../../common/CustomDatePicker";

/**
 * ConsentAccordionSection - Informed consent capture.
 *
 * Exported for reuse in Add Order, Edit Order, and the Sample Collection Wizard.
 *
 * Features:
 * - Carbon Accordion section, expanded by default
 * - Teal "Consent Recorded" Tag in header when consent is given
 * - Consent acknowledgment checkbox
 * - Optional consent form reference number, revealed when checkbox checked
 * - Operator-entered "Recorded by" name and "Recorded at" date
 *
 * Spec: https://github.com/DIGI-UW/openelis-work/blob/main/designs/sample-collection/informed-consent.md
 */

// FRS §10 + BR-005: alphanumeric, hyphens, and spaces only.
const FORM_REF_MAX_LENGTH = 100;
const FORM_REF_PATTERN = /^[a-zA-Z0-9\- ]*$/;

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

  const validateFormRef = (value) => {
    if (!value) return null;
    if (value.length > FORM_REF_MAX_LENGTH) {
      return intl.formatMessage({
        id: "error.informedConsent.formReferenceMaxLength",
        defaultMessage:
          "Consent form reference must be 100 characters or fewer",
      });
    }
    if (!FORM_REF_PATTERN.test(value)) {
      return intl.formatMessage({
        id: "error.informedConsent.formReferenceInvalidChars",
        defaultMessage:
          "Only letters, numbers, hyphens, and spaces are allowed",
      });
    }
    return null;
  };

  const formRefError = validateFormRef(consentFormReference);

  const handleConsentChange = (field, value) => {
    onConsentChange({
      ...consentData,
      [field]: value,
    });
  };

  const sectionTitle = intl.formatMessage({
    id: "heading.informedConsent.sectionTitle",
    defaultMessage: "Informed Consent",
  });
  const statusTagLabel = intl.formatMessage({
    id: "label.informedConsent.statusTag",
    defaultMessage: "Consent Recorded",
  });

  return (
    <Accordion>
      <AccordionItem
        open
        title={
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            {sectionTitle}
            {consentGiven && (
              <Tag kind="teal" size="sm">
                {statusTagLabel}
              </Tag>
            )}
          </span>
        }
      >
        <Stack gap={5}>
          <Checkbox
            id="consentGiven"
            labelText={intl.formatMessage({
              id: "label.informedConsent.consentGiven",
              defaultMessage: "Patient has provided signed consent",
            })}
            checked={consentGiven}
            onChange={(_, { checked }) => {
              // Clearing the checkbox clears all fields
              if (!checked) {
                onConsentChange({
                  ...consentData,
                  consentGiven: false,
                  consentFormReference: "",
                  consentRecordedAt: "",
                  consentRecordedBy: "",
                });
              } else {
                onConsentChange({
                  ...consentData,
                  consentGiven: true,
                });
              }
            }}
            disabled={isReadOnly}
          />

          {/* Form fields — revealed only when checkbox is checked */}
          {consentGiven && (
            <>
              <TextInput
                id="consentFormReference"
                labelText={intl.formatMessage({
                  id: "label.informedConsent.formReference",
                  defaultMessage: "Consent Form Reference No.",
                })}
                placeholder={intl.formatMessage({
                  id: "placeholder.informedConsent.formReference",
                  defaultMessage: "e.g. CF-2026-00123",
                })}
                maxLength={FORM_REF_MAX_LENGTH}
                value={consentFormReference}
                onChange={(e) =>
                  handleConsentChange("consentFormReference", e.target.value)
                }
                invalid={!!formRefError}
                invalidText={formRefError || ""}
                disabled={isReadOnly}
                style={{ maxWidth: "400px" }}
              />

              <TextInput
                id="consentRecordedBy"
                labelText={intl.formatMessage({
                  id: "label.informedConsent.recordedBy",
                  defaultMessage: "Consent Recorded By",
                })}
                placeholder={intl.formatMessage({
                  id: "placeholder.informedConsent.recordedBy",
                  defaultMessage: "e.g. Dr. Smith",
                })}
                maxLength={255}
                value={consentRecordedBy}
                onChange={(e) =>
                  handleConsentChange("consentRecordedBy", e.target.value)
                }
                disabled={isReadOnly}
                style={{ maxWidth: "400px" }}
              />

              <div style={{ maxWidth: "400px" }}>
                <CustomDatePicker
                  id="consentRecordedAt"
                  labelText={intl.formatMessage({
                    id: "label.informedConsent.recordedAt",
                    defaultMessage: "Consent Recorded At",
                  })}
                  value={consentRecordedAt}
                  updateStateValue={true}
                  disallowFutureDate={true}
                  disabled={isReadOnly}
                  onChange={(date) =>
                    handleConsentChange("consentRecordedAt", date)
                  }
                />
              </div>
            </>
          )}
        </Stack>
      </AccordionItem>
    </Accordion>
  );
};

export default ConsentAccordionSection;
