import React, { useState, useEffect } from "react";
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

const EIDSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get EID sites from organization lists
  const eidSitesByName =
    organizationLists && organizationLists["EID_ORGS_BY_NAME"]
      ? organizationLists["EID_ORGS_BY_NAME"]
      : [];

  const eidSitesByCode =
    organizationLists && organizationLists["EID_ORGS"]
      ? organizationLists["EID_ORGS"]
      : [];

  // Get dictionary lists
  const eidWhichPCRList =
    dictionaryLists && dictionaryLists["EID_WHICH_PCR"]
      ? dictionaryLists["EID_WHICH_PCR"]
      : [];

  const eidSecondPCRReasonList =
    dictionaryLists && dictionaryLists["EID_SECOND_PCR_REASON"]
      ? dictionaryLists["EID_SECOND_PCR_REASON"]
      : [];

  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  const yesNoUnknownList =
    dictionaryLists && dictionaryLists["YES_NO_UNKNOWN"]
      ? dictionaryLists["YES_NO_UNKNOWN"]
      : [];

  const eidTypeOfClinicList =
    dictionaryLists && dictionaryLists["EID_TYPE_OF_CLINIC"]
      ? dictionaryLists["EID_TYPE_OF_CLINIC"]
      : [];

  const eidHowChildFedList =
    dictionaryLists && dictionaryLists["EID_HOW_CHILD_FED"]
      ? dictionaryLists["EID_HOW_CHILD_FED"]
      : [];

  const eidStoppedBreastfeedingList =
    dictionaryLists && dictionaryLists["EID_STOPPED_BREASTFEEDING"]
      ? dictionaryLists["EID_STOPPED_BREASTFEEDING"]
      : [];

  const eidInfantProphylaxisARVList =
    dictionaryLists && dictionaryLists["EID_INFANT_PROPHYLAXIS_ARV"]
      ? dictionaryLists["EID_INFANT_PROPHYLAXIS_ARV"]
      : [];

  const eidMothersHIVStatusList =
    dictionaryLists && dictionaryLists["EID_MOTHERS_HIV_STATUS"]
      ? dictionaryLists["EID_MOTHERS_HIV_STATUS"]
      : [];

  const eidMothersARVTreatmentList =
    dictionaryLists && dictionaryLists["EID_MOTHERS_ARV_TREATMENT"]
      ? dictionaryLists["EID_MOTHERS_ARV_TREATMENT"]
      : [];

  // Handle EID site name selection and auto-populate code
  const handleSiteNameChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("EIDsiteName", selectedSiteId);

    // Auto-populate site code based on name selection
    if (selectedSiteId && eidSitesByCode.length > 0) {
      const selectedSite = eidSitesByCode.find(
        (site) => site.id === selectedSiteId,
      );
      if (selectedSite) {
        onInputChange("EIDsiteCode", selectedSite.id);
      }
    }
  };

  // Handle EID site code selection and auto-populate name
  const handleSiteCodeChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("EIDsiteCode", selectedSiteId);

    // Auto-populate site name based on code selection
    if (selectedSiteId && eidSitesByName.length > 0) {
      const selectedSite = eidSitesByName.find(
        (site) => site.id === selectedSiteId,
      );
      if (selectedSite) {
        onInputChange("EIDsiteName", selectedSite.id);
      }
    }
  };

  // Check if "Other" is selected for type of clinic
  const shouldShowTypeOfClinicOther = () => {
    const typeOfClinic = observations.eidTypeOfClinic;
    if (!typeOfClinic) return false;

    const otherOption = eidTypeOfClinicList.find(
      (item) =>
        item.id === typeOfClinic &&
        (item.dictEntry?.toLowerCase().includes("other") ||
          item.localizedName?.toLowerCase().includes("other") ||
          item.localizedName?.toLowerCase().includes("autre")),
    );

    return !!otherOption;
  };

  // Check if under investigation comment should be shown
  const shouldShowInvestigationComment = () => {
    const underInvestigation = observations.underInvestigation;
    if (!underInvestigation) return false;

    const yesOption = yesNoList.find(
      (item) =>
        item.id === underInvestigation &&
        (item.dictEntry === "Yes" ||
          item.dictEntry === "Oui" ||
          item.localizedName === "Yes"),
    );

    return !!yesOption;
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.EID.title"
          defaultMessage="EID (Early Infant Diagnosis)"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* EID Site Name */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="EIDsiteName"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "sample.entry.project.siteName",
                  defaultMessage: "Site Name",
                })}
              </>
            }
            value={projectData.EIDsiteName || ""}
            onChange={handleSiteNameChange}
          >
            <SelectItem text="" value="" />
            {eidSitesByName.map((site) => (
              <SelectItem
                key={site.id}
                text={site.organizationName || site.name}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* EID Site Code */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="EIDsiteCode"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "sample.entry.project.siteCode",
                  defaultMessage: "Site Code",
                })}
              </>
            }
            value={projectData.EIDsiteCode || ""}
            onChange={handleSiteCodeChange}
          >
            <SelectItem text="" value="" />
            {eidSitesByCode.map((site) => (
              <SelectItem
                key={site.id}
                text={site.doubleName || site.code || site.id}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* Which PCR */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidWhichPCR"
            labelText={intl.formatMessage({
              id: "patient.project.eidWhichPCR",
              defaultMessage: "Which PCR?",
            })}
            value={observations.whichPCR || ""}
            onChange={(e) => onObservationChange("whichPCR", e.target.value)}
          >
            <SelectItem text="" value="" />
            {eidWhichPCRList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Reason for Second PCR Test */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidSecondPCRReason"
            labelText={intl.formatMessage({
              id: "sample.entry.project.EID.reasonForPCRTest",
              defaultMessage: "Reason for Second PCR Test",
            })}
            value={observations.reasonForSecondPCRTest || ""}
            onChange={(e) =>
              onObservationChange("reasonForSecondPCRTest", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidSecondPCRReasonList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Name of Requestor */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="nameOfRequestor"
            labelText={intl.formatMessage({
              id: "patient.project.nameOfRequestor",
              defaultMessage: "Name of Requestor",
            })}
            value={observations.nameOfRequestor || ""}
            onChange={(e) =>
              onObservationChange("nameOfRequestor", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "patient.project.nameOfRequestor.placeholder",
              defaultMessage: "Enter requestor name",
            })}
          />
        </Column>

        {/* Name of Sampler */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="nameOfSampler"
            labelText={intl.formatMessage({
              id: "patient.project.nameOfSampler",
              defaultMessage: "Name of Sampler",
            })}
            value={observations.nameOfSampler || ""}
            onChange={(e) =>
              onObservationChange("nameOfSampler", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "patient.project.nameOfSampler.placeholder",
              defaultMessage: "Enter sampler name",
            })}
          />
        </Column>

        {/* Infant Information Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.title.infantInformation"
              defaultMessage="Infant Information"
            />
          </h4>
        </Column>

        {/* Benefit from PTME */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidInfantPTME"
            labelText={intl.formatMessage({
              id: "patient.project.eidBenefitPTME",
              defaultMessage: "Did infant benefit from PTME?",
            })}
            value={observations.eidInfantPTME || ""}
            onChange={(e) =>
              onObservationChange("eidInfantPTME", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Type of Clinic */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidTypeOfClinic"
            labelText={intl.formatMessage({
              id: "patient.project.eidTypeOfClinic",
              defaultMessage: "Type of Clinic",
            })}
            value={observations.eidTypeOfClinic || ""}
            onChange={(e) =>
              onObservationChange("eidTypeOfClinic", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidTypeOfClinicList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Type of Clinic Other (Conditional) */}
        {shouldShowTypeOfClinicOther() && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="eidTypeOfClinicOther"
              labelText={intl.formatMessage({
                id: "patient.project.specify",
                defaultMessage: "Specify",
              })}
              value={observations.eidTypeOfClinicOther || ""}
              onChange={(e) =>
                onObservationChange("eidTypeOfClinicOther", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "patient.project.specify.placeholder",
                defaultMessage: "Please specify",
              })}
            />
          </Column>
        )}

        {/* How Child Fed */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidHowChildFed"
            labelText={intl.formatMessage({
              id: "patient.project.eidHowChildFed",
              defaultMessage: "How is the child fed?",
            })}
            value={observations.eidHowChildFed || ""}
            onChange={(e) =>
              onObservationChange("eidHowChildFed", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidHowChildFedList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Stopped Breastfeeding */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidStoppedBreastfeeding"
            labelText={intl.formatMessage({
              id: "patient.project.eidStoppedBreastfeeding",
              defaultMessage: "Has breastfeeding stopped?",
            })}
            value={observations.eidStoppedBreastfeeding || ""}
            onChange={(e) =>
              onObservationChange("eidStoppedBreastfeeding", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidStoppedBreastfeedingList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Infant Symptomatic */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidInfantSymptomatic"
            labelText={intl.formatMessage({
              id: "patient.project.eidInfantSymptomatic",
              defaultMessage: "Is infant symptomatic?",
            })}
            value={observations.eidInfantSymptomatic || ""}
            onChange={(e) =>
              onObservationChange("eidInfantSymptomatic", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Infant's ARV Prophylaxis */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidInfantsARV"
            labelText={intl.formatMessage({
              id: "patient.project.eidInfantProphy",
              defaultMessage: "Infant's ARV Prophylaxis",
            })}
            value={observations.eidInfantsARV || ""}
            onChange={(e) =>
              onObservationChange("eidInfantsARV", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidInfantProphylaxisARVList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Infant Cotrimoxazole */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidInfantCotrimoxazole"
            labelText={intl.formatMessage({
              id: "patient.project.eidInfantCotrimoxazole",
              defaultMessage: "Infant receiving Cotrimoxazole?",
            })}
            value={observations.eidInfantCotrimoxazole || ""}
            onChange={(e) =>
              onObservationChange("eidInfantCotrimoxazole", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoUnknownList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Mother's Information Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.title.mothersInformation"
              defaultMessage="Mother's Information"
            />
          </h4>
        </Column>

        {/* Mother's HIV Status */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidMothersHIVStatus"
            labelText={intl.formatMessage({
              id: "patient.project.eidMothersStatus",
              defaultMessage: "Mother's HIV Status",
            })}
            value={observations.eidMothersHIVStatus || ""}
            onChange={(e) =>
              onObservationChange("eidMothersHIVStatus", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidMothersHIVStatusList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Mother's ARV Treatment */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="eidMothersARV"
            labelText={intl.formatMessage({
              id: "patient.project.eidMothersARV",
              defaultMessage: "Mother's ARV Treatment",
            })}
            value={observations.eidMothersARV || ""}
            onChange={(e) =>
              onObservationChange("eidMothersARV", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {eidMothersARVTreatmentList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Under Investigation */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="underInvestigation"
            labelText={intl.formatMessage({
              id: "patient.project.underInvestigation",
              defaultMessage: "Under Investigation?",
            })}
            value={observations.underInvestigation || ""}
            onChange={(e) =>
              onObservationChange("underInvestigation", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Under Investigation Comment (Conditional) */}
        {shouldShowInvestigationComment() && (
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="underInvestigationNote"
              labelText={
                <>
                  <span className="required-field">*</span>{" "}
                  {intl.formatMessage({
                    id: "patient.project.underInvestigationComment",
                    defaultMessage: "Investigation Notes",
                  })}
                </>
              }
              value={projectData.underInvestigationNote || ""}
              onChange={(e) =>
                onInputChange("underInvestigationNote", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "patient.project.underInvestigationComment.placeholder",
                defaultMessage: "Enter investigation notes",
              })}
              rows={3}
              maxLength={1000}
            />
          </Column>
        )}
      </Grid>
    </Section>
  );
};

export default EIDSection;
