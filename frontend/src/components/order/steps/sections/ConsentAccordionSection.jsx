import React from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Checkbox,
  TextInput,
  Tag,
  Tile,
  Stack,
} from "@carbon/react";
import { UserAvatar, Time } from "@carbon/icons-react";

/**
 * ConsentAccordionSection - Informed consent capture per OGC-557 FRS v1.1.
 *
 * Exported for reuse in Add Order, Edit Order, and the Sample Collection Wizard.
 *
 * Features:
 * - Carbon Accordion section, expanded by default (FR-1-001..FR-1-003)
 * - Teal "Consent Recorded" Tag in header when consent is given (FR-1-004)
 * - Consent acknowledgment checkbox (advisory per FR-5-001/FR-5-002)
 * - Optional consent form reference number, revealed when checkbox checked
 * - Read-only audit Tile once consent is recorded server-side
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
              // Clearing the checkbox clears the reference per FR-3-002
              if (!checked) {
                onConsentChange({
                  ...consentData,
                  consentGiven: false,
                  consentFormReference: "",
                });
              } else {
                handleConsentChange("consentGiven", true);
              }
            }}
            disabled={isReadOnly}
          />

          {/* Reference number field — revealed only when checkbox is checked (FR-3-001) */}
          {consentGiven && (
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
          )}

          {/* Read-only audit Tile — shown when editing an order with previously recorded consent (FR-4-004) */}
          {consentGiven && consentRecordedAt && (
            <Tile>
              <p className="consent-audit-heading">
                <FormattedMessage
                  id="heading.informedConsent.auditRecord"
                  defaultMessage="Consent Audit Record"
                />
              </p>
              <Stack gap={3}>
                {consentRecordedBy && (
                  <Stack orientation="horizontal" gap={3}>
                    <UserAvatar size={16} />
                    <span>
                      <strong>
                        <FormattedMessage
                          id="label.informedConsent.recordedBy"
                          defaultMessage="Recorded by"
                        />
                        :
                      </strong>{" "}
                      {consentRecordedBy}
                    </span>
                  </Stack>
                )}
                <Stack orientation="horizontal" gap={3}>
                  <Time size={16} />
                  <span>
                    <strong>
                      <FormattedMessage
                        id="label.informedConsent.recordedAt"
                        defaultMessage="Recorded on"
                      />
                      :
                    </strong>{" "}
                    {consentRecordedAt}
                  </span>
                </Stack>
              </Stack>
            </Tile>
          )}
        </Stack>
      </AccordionItem>
    </Accordion>
  );
};

export default ConsentAccordionSection;
