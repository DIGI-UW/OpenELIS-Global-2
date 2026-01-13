import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  TextInput,
  TextArea,
  Grid,
  Column,
} from "@carbon/react";

const SpecialRequestSection = ({ projectData, onInputChange }) => {
  const intl = useIntl();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.special.request.section"
          defaultMessage="Special Request Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Address */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="address"
            labelText={intl.formatMessage({
              id: "sample.entry.address",
              defaultMessage: "Address",
            })}
            value={projectData.address || ""}
            onChange={(e) => onInputChange("address", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.address.placeholder",
              defaultMessage: "Enter complete address",
            })}
            rows={2}
          />
        </Column>

        {/* Phone Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="phoneNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.phone",
              defaultMessage: "Phone Number",
            })}
            value={projectData.phoneNumber || ""}
            onChange={(e) => onInputChange("phoneNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.phone.placeholder",
              defaultMessage: "Enter phone number",
            })}
          />
        </Column>

        {/* Fax Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="faxNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.fax",
              defaultMessage: "Fax Number",
            })}
            value={projectData.faxNumber || ""}
            onChange={(e) => onInputChange("faxNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.fax.placeholder",
              defaultMessage: "Enter fax number",
            })}
          />
        </Column>

        {/* Email */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="email"
            labelText={intl.formatMessage({
              id: "sample.entry.email",
              defaultMessage: "Email Address",
            })}
            value={projectData.email || ""}
            onChange={(e) => onInputChange("email", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.email.placeholder",
              defaultMessage: "Enter email address",
            })}
            type="email"
          />
        </Column>

        {/* Reason for Request */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="reasonForRequest"
            labelText={intl.formatMessage({
              id: "sample.entry.reason",
              defaultMessage: "Reason for Request",
            })}
            value={projectData.reasonForRequest || ""}
            onChange={(e) => onInputChange("reasonForRequest", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.reason.placeholder",
              defaultMessage: "Enter reason for special request",
            })}
            rows={3}
          />
        </Column>

        {/* Under Investigation Note */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="underInvestigationNote"
            labelText={intl.formatMessage({
              id: "sample.entry.investigation.note",
              defaultMessage: "Under Investigation Note",
            })}
            value={projectData.underInvestigationNote || ""}
            onChange={(e) =>
              onInputChange("underInvestigationNote", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.investigation.note.placeholder",
              defaultMessage: "Enter investigation notes",
            })}
            rows={3}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default SpecialRequestSection;
