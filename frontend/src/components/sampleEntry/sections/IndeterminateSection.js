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

const IndeterminateSection = ({
  projectData,
  onInputChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get IND sites from organization lists
  const indSites =
    organizationLists && organizationLists["IND"]
      ? organizationLists["IND"]
      : organizationLists && organizationLists["ind"]
        ? organizationLists["ind"]
        : organizationLists && organizationLists["IND_SITE"]
          ? organizationLists["IND_SITE"]
          : [];

  // Handle IND site selection and auto-populate code
  const handleSiteChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("INDsiteName", selectedSiteId);

    // Find the selected site and auto-populate its code
    if (selectedSiteId && indSites.length > 0) {
      const selectedSite = indSites.find((site) => site.id === selectedSiteId);
      if (selectedSite) {
        onInputChange("INDsiteCode", selectedSite.code || "");
      }
    } else {
      onInputChange("INDsiteCode", "");
    }
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.indeterminate.section"
          defaultMessage="Indeterminate Results Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* IND Site Selection */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="INDsiteName"
            labelText={intl.formatMessage({
              id: "sample.entry.ind.site",
              defaultMessage: "Indeterminate Site",
            })}
            value={projectData.INDsiteName || ""}
            onChange={handleSiteChange}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "sample.entry.ind.site.select",
                defaultMessage: "Select Site",
              })}
              value=""
            />
            {indSites.map((site) => (
              <SelectItem
                key={site.id}
                text={site.organizationName || site.name}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* IND Site Code (Auto-populated) */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="INDsiteCode"
            labelText={intl.formatMessage({
              id: "sample.entry.ind.site.code",
              defaultMessage: "Site Code",
            })}
            value={projectData.INDsiteCode || ""}
            onChange={(e) => onInputChange("INDsiteCode", e.target.value)}
            readOnly
            placeholder={intl.formatMessage({
              id: "sample.entry.ind.site.code.placeholder",
              defaultMessage: "Auto-populated",
            })}
          />
        </Column>

        {/* Investigation Notes */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="underInvestigationNote"
            labelText={intl.formatMessage({
              id: "sample.entry.ind.investigation.note",
              defaultMessage: "Investigation Notes",
            })}
            value={projectData.underInvestigationNote || ""}
            onChange={(e) =>
              onInputChange("underInvestigationNote", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.ind.investigation.note.placeholder",
              defaultMessage:
                "Enter notes regarding indeterminate test results",
            })}
            rows={4}
          />
        </Column>

        {/* Additional Context */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="indeterminateContext"
            labelText={intl.formatMessage({
              id: "sample.entry.ind.context",
              defaultMessage: "Additional Context",
            })}
            value={projectData.indeterminateContext || ""}
            onChange={(e) =>
              onInputChange("indeterminateContext", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.ind.context.placeholder",
              defaultMessage:
                "Enter any additional context or follow-up actions",
            })}
            rows={3}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default IndeterminateSection;
