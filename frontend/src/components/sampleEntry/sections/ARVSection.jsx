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

const ARVSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
  birthDate,
  selectedProject,
}) => {
  const intl = useIntl();
  const [calculatedAge, setCalculatedAge] = useState("");

  // Get ARV centers from organization lists
  const arvCentersByName =
    organizationLists && organizationLists["ARV_ORGS_BY_NAME"]
      ? organizationLists["ARV_ORGS_BY_NAME"]
      : [];

  const arvCentersByCode =
    organizationLists && organizationLists["ARV_ORGS"]
      ? organizationLists["ARV_ORGS"]
      : [];

  // Get YES_NO dictionary for under investigation
  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  // Get HIV_STATUSES dictionary for Follow-up ARV
  const hivStatusList =
    dictionaryLists && dictionaryLists["HIV_STATUSES"]
      ? dictionaryLists["HIV_STATUSES"]
      : [];

  // Check if this is Follow-up ARV (needs HIV Status field)
  const isFollowUpARV =
    selectedProject &&
    (selectedProject.includes("FOLLOWUP") ||
      selectedProject.includes("Follow") ||
      selectedProject === "ARV_FOLLOWUP");

  // Calculate age from birth date
  useEffect(() => {
    if (birthDate) {
      const age = calculateAge(birthDate);
      setCalculatedAge(age);
    } else {
      setCalculatedAge("");
    }
  }, [birthDate]);

  const calculateAge = (birthDateString) => {
    if (!birthDateString) return "";

    // Parse date in dd/MM/yyyy format
    const parts = birthDateString.split("/");
    if (parts.length !== 3) return "";

    const day = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
    const year = parseInt(parts[2], 10);

    const birthDate = new Date(year, month, day);
    const today = new Date();

    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (
      monthDiff < 0 ||
      (monthDiff === 0 && today.getDate() < birthDate.getDate())
    ) {
      age--;
    }

    return age > 0 ? age.toString() : "";
  };

  // Handle ARV center name selection and auto-populate code
  const handleCenterNameChange = (event) => {
    const selectedCenterId = event.target.value;
    onInputChange("arvcenterName", selectedCenterId);

    // Auto-populate center code based on name selection
    if (selectedCenterId && arvCentersByCode.length > 0) {
      const selectedCenter = arvCentersByCode.find(
        (center) => center.id === selectedCenterId,
      );
      if (selectedCenter) {
        onInputChange("arvcenterCode", selectedCenter.id);
      }
    }
  };

  // Handle ARV center code selection and auto-populate name
  const handleCenterCodeChange = (event) => {
    const selectedCenterId = event.target.value;
    onInputChange("arvcenterCode", selectedCenterId);

    // Auto-populate center name based on code selection
    if (selectedCenterId && arvCentersByName.length > 0) {
      const selectedCenter = arvCentersByName.find(
        (center) => center.id === selectedCenterId,
      );
      if (selectedCenter) {
        onInputChange("arvcenterName", selectedCenter.id);
      }
    }
  };

  // Check if under investigation comment should be shown
  const shouldShowInvestigationComment = () => {
    const underInvestigation = observations.underInvestigation;
    if (!underInvestigation) return false;

    // Show if "Yes" is selected (check dictionary entry)
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
          id="sample.entry.arv.section"
          defaultMessage="ARV Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* ARV Center Name */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="arvcenterName"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "sample.entry.project.ARV.centerName",
                  defaultMessage: "ARV Center Name",
                })}
              </>
            }
            value={projectData.arvcenterName || ""}
            onChange={handleCenterNameChange}
          >
            <SelectItem text="" value="" />
            {arvCentersByName.map((center) => (
              <SelectItem
                key={center.id}
                text={center.organizationName || center.name}
                value={center.id}
              />
            ))}
          </Select>
        </Column>

        {/* ARV Center Code */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="arvcenterCode"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.project.centerCode",
                  defaultMessage: "Center Code",
                })}
              </>
            }
            value={projectData.arvcenterCode || ""}
            onChange={handleCenterCodeChange}
          >
            <SelectItem text="" value="" />
            {arvCentersByCode.map((center) => (
              <SelectItem
                key={center.id}
                text={center.doubleName || center.code || center.id}
                value={center.id}
              />
            ))}
          </Select>
        </Column>

        {/* Doctor / Clinician Name */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="doctor"
            labelText={intl.formatMessage({
              id: "sample.entry.project.doctor",
              defaultMessage: "Doctor / Clinician",
            })}
            value={projectData.doctor || ""}
            onChange={(e) => onInputChange("doctor", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.doctor.placeholder",
              defaultMessage: "Enter doctor name",
            })}
            maxLength={50}
          />
        </Column>

        {/* Age (Read-only, calculated from birth date) */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="age"
            labelText={intl.formatMessage({
              id: "patient.age",
              defaultMessage: "Age (Years)",
            })}
            value={calculatedAge}
            readOnly
            placeholder={intl.formatMessage({
              id: "patient.age.calculated",
              defaultMessage: "Calculated from birth date",
            })}
          />
        </Column>

        {/* HIV Status - Only for Follow-up ARV */}
        {isFollowUpARV && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="hivStatus"
              labelText={intl.formatMessage({
                id: "patient.project.hivStatus",
                defaultMessage: "HIV Status",
              })}
              value={observations.hivStatus || ""}
              onChange={(e) => onObservationChange("hivStatus", e.target.value)}
            >
              <SelectItem text="" value="" />
              {hivStatusList.map((item) => (
                <SelectItem
                  key={item.id}
                  text={item.localizedName || item.dictEntry}
                  value={item.id}
                />
              ))}
            </Select>
          </Column>
        )}

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

export default ARVSection;
