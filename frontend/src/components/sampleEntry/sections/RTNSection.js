import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Grid,
  Column,
} from "@carbon/react";

const RTNSection = ({
  projectData,
  onInputChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get RTN sites from organization lists
  const rtnSites =
    organizationLists && organizationLists["RTN"]
      ? organizationLists["RTN"]
      : organizationLists && organizationLists["rtn"]
        ? organizationLists["rtn"]
        : organizationLists && organizationLists["RTN_SITE"]
          ? organizationLists["RTN_SITE"]
          : [];

  // Handle RTN site selection and auto-populate code
  const handleSiteChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("RTNsiteName", selectedSiteId);

    // Find the selected site and auto-populate its code
    if (selectedSiteId && rtnSites.length > 0) {
      const selectedSite = rtnSites.find((site) => site.id === selectedSiteId);
      if (selectedSite) {
        onInputChange("RTNsiteCode", selectedSite.code || "");
      }
    } else {
      onInputChange("RTNsiteCode", "");
    }
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.rtn.section"
          defaultMessage="RTN (Routine Testing Network) Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* RTN Site Selection */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="RTNsiteName"
            labelText={intl.formatMessage({
              id: "sample.entry.rtn.site",
              defaultMessage: "RTN Site",
            })}
            value={projectData.RTNsiteName || ""}
            onChange={handleSiteChange}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "sample.entry.rtn.site.select",
                defaultMessage: "Select RTN Site",
              })}
              value=""
            />
            {rtnSites.map((site) => (
              <SelectItem
                key={site.id}
                text={site.organizationName || site.name}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* RTN Site Code (Auto-populated) */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="RTNsiteCode"
            labelText={intl.formatMessage({
              id: "sample.entry.rtn.site.code",
              defaultMessage: "RTN Site Code",
            })}
            value={projectData.RTNsiteCode || ""}
            onChange={(e) => onInputChange("RTNsiteCode", e.target.value)}
            readOnly
            placeholder={intl.formatMessage({
              id: "sample.entry.rtn.site.code.placeholder",
              defaultMessage: "Auto-populated",
            })}
          />
        </Column>

        {/* RTN Reference Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="rtnReferenceNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.rtn.reference",
              defaultMessage: "RTN Reference Number",
            })}
            value={projectData.rtnReferenceNumber || ""}
            onChange={(e) =>
              onInputChange("rtnReferenceNumber", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.rtn.reference.placeholder",
              defaultMessage: "Enter RTN reference number",
            })}
          />
        </Column>

        {/* RTN Notes */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="rtnNotes"
            labelText={intl.formatMessage({
              id: "sample.entry.rtn.notes",
              defaultMessage: "RTN Notes",
            })}
            value={projectData.rtnNotes || ""}
            onChange={(e) => onInputChange("rtnNotes", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.rtn.notes.placeholder",
              defaultMessage: "Enter any additional notes for RTN",
            })}
            rows={3}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default RTNSection;
