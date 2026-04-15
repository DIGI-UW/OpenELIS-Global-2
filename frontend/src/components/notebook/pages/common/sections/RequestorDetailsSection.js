import React from "react";
import { TextInput, Grid, Column } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Section A: Requestor Details (AHRI BR-F-02)
 * Captures who is requesting samples.
 */
function RequestorDetailsSection({ formData, onChange, readOnly }) {
  const intl = useIntl();

  const handleChange = (field) => (e) => {
    onChange(field, e.target.value);
  };

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.requestor"
          defaultMessage="Section A: Requestor Details"
        />
      </h4>
      <Grid condensed>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="requestorName"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.requestorName",
              defaultMessage: "Requestor Name",
            })}
            value={formData.requestorName || ""}
            readOnly
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="requesterLabUnit"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.labUnit",
              defaultMessage: "Lab Unit",
            })}
            value={formData.requesterLabUnit || ""}
            onChange={handleChange("requesterLabUnit")}
            readOnly={readOnly}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="principalInvestigator"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.principalInvestigator",
              defaultMessage: "Principal Investigator",
            })}
            value={formData.principalInvestigator || ""}
            onChange={handleChange("principalInvestigator")}
            readOnly={readOnly}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="projectTitle"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.projectTitle",
              defaultMessage: "Project Title",
            })}
            value={formData.projectTitle || ""}
            onChange={handleChange("projectTitle")}
            readOnly={readOnly}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="requesterContactInfo"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.contactInfo",
              defaultMessage: "Contact Information",
            })}
            value={formData.requesterContactInfo || ""}
            onChange={handleChange("requesterContactInfo")}
            readOnly={readOnly}
          />
        </Column>
      </Grid>
    </div>
  );
}

export default RequestorDetailsSection;
