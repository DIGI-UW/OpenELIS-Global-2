import React, { useState, useEffect } from "react";
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

const EIDSection = ({
  projectData,
  onInputChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get EID sites from organization lists
  const eidSites =
    organizationLists && organizationLists["EID"]
      ? organizationLists["EID"]
      : organizationLists && organizationLists["eid"]
        ? organizationLists["eid"]
        : organizationLists && organizationLists["EID_SITE"]
          ? organizationLists["EID_SITE"]
          : [];

  // Get PCR options from dictionary lists
  const pcrOptions =
    dictionaryLists && dictionaryLists["EID_WHICH_PCR"]
      ? dictionaryLists["EID_WHICH_PCR"]
      : [];

  // Get second PCR reasons from dictionary lists
  const pcrReasons =
    dictionaryLists && dictionaryLists["EID_SECOND_PCR_REASON"]
      ? dictionaryLists["EID_SECOND_PCR_REASON"]
      : [];

  // Track if second PCR is selected to show conditional fields
  const [showSecondPCRReason, setShowSecondPCRReason] = useState(false);

  useEffect(() => {
    // Show second PCR reason if PCR type indicates it's a second test
    if (projectData.eidWhichPCR) {
      const selectedPCR = pcrOptions.find(
        (option) => option.id === projectData.eidWhichPCR,
      );
      if (selectedPCR) {
        const isSecondPCR =
          selectedPCR.value?.toLowerCase().includes("second") ||
          selectedPCR.value?.toLowerCase().includes("2nd") ||
          selectedPCR.dictEntry?.toLowerCase().includes("second");
        setShowSecondPCRReason(isSecondPCR);
      }
    } else {
      setShowSecondPCRReason(false);
    }
  }, [projectData.eidWhichPCR, pcrOptions]);

  // Handle EID site selection and auto-populate code
  const handleSiteChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("EIDsiteName", selectedSiteId);

    // Find the selected site and auto-populate its code
    if (selectedSiteId && eidSites.length > 0) {
      const selectedSite = eidSites.find((site) => site.id === selectedSiteId);
      if (selectedSite) {
        onInputChange("EIDsiteCode", selectedSite.code || "");
      }
    } else {
      onInputChange("EIDsiteCode", "");
    }
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.eid.section"
          defaultMessage="EID (Early Infant Diagnosis) Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* EID Site Selection */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="EIDsiteName"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.site",
              defaultMessage: "EID Site",
            })}
            value={projectData.EIDsiteName || ""}
            onChange={handleSiteChange}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "sample.entry.eid.site.select",
                defaultMessage: "Select EID Site",
              })}
              value=""
            />
            {eidSites.map((site) => (
              <SelectItem
                key={site.id}
                text={site.organizationName || site.name}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* EID Site Code (Auto-populated) */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="EIDsiteCode"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.site.code",
              defaultMessage: "EID Site Code",
            })}
            value={projectData.EIDsiteCode || ""}
            onChange={(e) => onInputChange("EIDsiteCode", e.target.value)}
            readOnly
            placeholder={intl.formatMessage({
              id: "sample.entry.eid.site.code.placeholder",
              defaultMessage: "Auto-populated",
            })}
          />
        </Column>

        {/* DBS Infant Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="dbsInfantNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.infant.number",
              defaultMessage: "DBS Infant Number",
            })}
            value={projectData.dbsInfantNumber || ""}
            onChange={(e) => onInputChange("dbsInfantNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.eid.infant.number.placeholder",
              defaultMessage: "Enter infant number",
            })}
          />
        </Column>

        {/* DBS Site Infant Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="dbsSiteInfantNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.site.infant.number",
              defaultMessage: "DBS Site Infant Number",
            })}
            value={projectData.dbsSiteInfantNumber || ""}
            onChange={(e) =>
              onInputChange("dbsSiteInfantNumber", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.eid.site.infant.number.placeholder",
              defaultMessage: "Enter site infant number",
            })}
          />
        </Column>

        {/* Which PCR */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidWhichPCR"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.which.pcr",
              defaultMessage: "Which PCR",
            })}
            value={projectData.eidWhichPCR || ""}
            onChange={(e) => onInputChange("eidWhichPCR", e.target.value)}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "sample.entry.eid.which.pcr.select",
                defaultMessage: "Select PCR Type",
              })}
              value=""
            />
            {pcrOptions.map((option) => (
              <SelectItem
                key={option.id}
                text={option.value || option.dictEntry}
                value={option.id}
              />
            ))}
          </Select>
        </Column>

        {/* Second PCR Reason (Conditional) */}
        {showSecondPCRReason && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="eidSecondPCRReason"
              labelText={intl.formatMessage({
                id: "sample.entry.eid.second.pcr.reason",
                defaultMessage: "Reason for Second PCR",
              })}
              value={projectData.eidSecondPCRReason || ""}
              onChange={(e) =>
                onInputChange("eidSecondPCRReason", e.target.value)
              }
            >
              <SelectItem
                text={intl.formatMessage({
                  id: "sample.entry.eid.second.pcr.reason.select",
                  defaultMessage: "Select Reason",
                })}
                value=""
              />
              {pcrReasons.map((reason) => (
                <SelectItem
                  key={reason.id}
                  text={reason.value || reason.dictEntry}
                  value={reason.id}
                />
              ))}
            </Select>
          </Column>
        )}

        {/* Requester */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="requester"
            labelText={intl.formatMessage({
              id: "sample.entry.eid.requester",
              defaultMessage: "Requester",
            })}
            value={projectData.requester || ""}
            onChange={(e) => onInputChange("requester", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.eid.requester.placeholder",
              defaultMessage: "Enter requester name",
            })}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default EIDSection;
