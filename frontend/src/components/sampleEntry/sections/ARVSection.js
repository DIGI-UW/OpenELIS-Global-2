import React, { useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
  Grid,
  Column,
} from "@carbon/react";

const ARVSection = ({ projectData, onInputChange, organizationLists }) => {
  const intl = useIntl();

  // Get ARV centers from organization lists
  const arvCenters =
    organizationLists && organizationLists["ARV"]
      ? organizationLists["ARV"]
      : organizationLists && organizationLists["arv"]
        ? organizationLists["arv"]
        : [];

  // Handle ARV center selection and auto-populate code
  const handleCenterChange = (event) => {
    const selectedCenterId = event.target.value;
    onInputChange("ARVcenterName", selectedCenterId);

    // Find the selected center and auto-populate its code
    if (selectedCenterId && arvCenters.length > 0) {
      const selectedCenter = arvCenters.find(
        (center) => center.id === selectedCenterId,
      );
      if (selectedCenter) {
        onInputChange("ARVcenterCode", selectedCenter.code || "");
      }
    } else {
      onInputChange("ARVcenterCode", "");
    }
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.arv.section"
          defaultMessage="ARV Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="ARVcenterName"
            labelText={intl.formatMessage({
              id: "sample.entry.arv.center",
              defaultMessage: "ARV Center",
            })}
            value={projectData.ARVcenterName || ""}
            onChange={handleCenterChange}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "sample.entry.arv.center.select",
                defaultMessage: "Select ARV Center",
              })}
              value=""
            />
            {arvCenters.map((center) => (
              <SelectItem
                key={center.id}
                text={center.organizationName || center.name}
                value={center.id}
              />
            ))}
          </Select>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="ARVcenterCode"
            labelText={intl.formatMessage({
              id: "sample.entry.arv.center.code",
              defaultMessage: "ARV Center Code",
            })}
            value={projectData.ARVcenterCode || ""}
            onChange={(e) => onInputChange("ARVcenterCode", e.target.value)}
            readOnly
            placeholder={intl.formatMessage({
              id: "sample.entry.arv.center.code.placeholder",
              defaultMessage: "Auto-populated",
            })}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="doctor"
            labelText={intl.formatMessage({
              id: "sample.entry.doctor",
              defaultMessage: "Doctor / Clinician",
            })}
            value={projectData.doctor || ""}
            onChange={(e) => onInputChange("doctor", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.doctor.placeholder",
              defaultMessage: "Enter doctor name",
            })}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default ARVSection;
