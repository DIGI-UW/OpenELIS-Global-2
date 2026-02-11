import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  TextInput,
  Select,
  SelectItem,
  Grid,
  Column,
} from "@carbon/react";
import CustomDatePicker from "../../common/CustomDatePicker";

const PatientInfoSection = ({
  formData,
  onInputChange,
  genders,
  selectedProject,
}) => {
  const intl = useIntl();
  const [calculatedAge, setCalculatedAge] = useState("");

  // Calculate age from birth date
  useEffect(() => {
    if (formData.birthDateForDisplay) {
      const age = calculateAge(formData.birthDateForDisplay);
      setCalculatedAge(age);
    } else {
      setCalculatedAge("");
    }
  }, [formData.birthDateForDisplay]);

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

  // Determine if we should hide subject number field (for ARV projects)
  const isARVProject =
    selectedProject &&
    (selectedProject.includes("ARV") ||
      selectedProject.includes("Initial") ||
      selectedProject.includes("Follow"));

  // RTN project has NO patient ID fields at all
  const isRTNProject = selectedProject && selectedProject.includes("RTN");

  // EID project has NO standard patient ID fields (uses infant numbers)
  const isEIDProject = selectedProject && selectedProject.includes("EID");

  // Indeterminate project has NO upidCode (only subjectNumber and siteSubjectNumber)
  const isIndeterminateProject =
    selectedProject &&
    (selectedProject.includes("Indeterminate") ||
      selectedProject === "INDETERMINATE");

  // Special Request project has NO upidCode (only subjectNumber and siteSubjectNumber)
  const isSpecialRequestProject =
    selectedProject &&
    (selectedProject.includes("Special") ||
      selectedProject === "SPECIAL_REQUEST");

  // Recency project has NO subjectNumber or upidCode (only siteSubjectNumber as recencyNumber)
  const isRecencyProject =
    selectedProject &&
    (selectedProject.includes("Recency") ||
      selectedProject === "RECENCY_TESTING");

  // HPV project has NO subjectNumber or upidCode (only siteSubjectNumber as hpvSubjectNumber)
  const isHPVProject =
    selectedProject &&
    (selectedProject.includes("HPV") || selectedProject === "HPV_TESTING");

  // VL (Viral Load) project has NO upidCode (only subjectNumber and siteSubjectNumber)
  const isVLProject =
    selectedProject &&
    (selectedProject.includes("VL") || selectedProject === "ARV_VIRAL_LOAD");

  // Get field labels based on project type
  const getFieldLabels = () => {
    if (isARVProject) {
      return {
        siteSubjectNumber: {
          id: "patient.subject.number",
          defaultMessage: "Unique Health ID number",
        },
        upidCode: {
          id: "patient.site.subject.number",
          defaultMessage: "Site Unique Health ID number",
        },
      };
    }

    if (isRecencyProject) {
      return {
        siteSubjectNumber: {
          id: "sample.entry.project.recencyNumber",
          defaultMessage: "Recency Number",
        },
        upidCode: {
          id: "sample.entry.patient.upid",
          defaultMessage: "UPID Code",
        },
      };
    }

    if (isHPVProject) {
      return {
        siteSubjectNumber: {
          id: "sample.entry.project.hpvSubjectNumber",
          defaultMessage: "HPV Subject Number",
        },
        upidCode: {
          id: "sample.entry.patient.upid",
          defaultMessage: "UPID Code",
        },
      };
    }

    // Default labels for other projects
    return {
      siteSubjectNumber: {
        id: "patient.site.subject.number",
        defaultMessage: "Site Unique Health ID number",
      },
      upidCode: {
        id: "sample.entry.patient.upid",
        defaultMessage: "UPID Code",
      },
    };
  };

  const fieldLabels = getFieldLabels();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.patient.info"
          defaultMessage="Patient Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Subject Number - Hidden for ARV, RTN, EID, Recency, and HPV projects */}
        {!isARVProject &&
          !isRTNProject &&
          !isEIDProject &&
          !isRecencyProject &&
          !isHPVProject && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="subjectNumber"
                labelText={
                  <>
                    <span className="required-field">+</span>{" "}
                    {intl.formatMessage({
                      id: "sample.entry.project.subjectNumber",
                      defaultMessage: "Unique Health ID number",
                    })}
                  </>
                }
                value={formData.subjectNumber || ""}
                onChange={(e) => onInputChange("subjectNumber", e.target.value)}
                placeholder={intl.formatMessage({
                  id: "sample.entry.patient.subject.number.placeholder",
                  defaultMessage: "Enter unique health ID number",
                })}
                maxLength={7}
              />
            </Column>
          )}

        {/* Site Subject Number - Label changes based on project, Hidden for RTN and EID, Shows as Recency Number for Recency, HPV Subject Number for HPV */}
        {!isRTNProject && !isEIDProject && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="siteSubjectNumber"
              labelText={
                <>
                  <span className="required-field">+</span>{" "}
                  {intl.formatMessage(fieldLabels.siteSubjectNumber)}
                </>
              }
              value={formData.siteSubjectNumber || ""}
              onChange={(e) =>
                onInputChange("siteSubjectNumber", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "sample.entry.patient.site.subject.number.placeholder",
                defaultMessage: "Enter site unique health ID number",
              })}
              maxLength={18}
            />
          </Column>
        )}

        {/* UPID Code - Label changes based on project, Hidden for RTN, EID, Indeterminate, Special Request, Recency, HPV, and VL */}
        {!isRTNProject &&
          !isEIDProject &&
          !isIndeterminateProject &&
          !isSpecialRequestProject &&
          !isRecencyProject &&
          !isHPVProject &&
          !isVLProject && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="upidCode"
                labelText={
                  <>
                    <span className="required-field">+</span>{" "}
                    {intl.formatMessage(fieldLabels.upidCode)}
                  </>
                }
                value={formData.upidCode || ""}
                onChange={(e) => onInputChange("upidCode", e.target.value)}
                placeholder={intl.formatMessage({
                  id: "sample.entry.patient.upid.placeholder",
                  defaultMessage: "Enter code",
                })}
              />
            </Column>
          )}

        {/* Lab Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="labNo"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "quick.entry.accession.number",
                  defaultMessage: "Lab Number",
                })}
              </>
            }
            value={formData.labNo || ""}
            onChange={(e) => onInputChange("labNo", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.lab.number.placeholder",
              defaultMessage: "Enter lab number",
            })}
          />
        </Column>

        {/* Gender */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="gender"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.gender",
                  defaultMessage: "Gender",
                })}
              </>
            }
            value={formData.gender || ""}
            onChange={(e) => onInputChange("gender", e.target.value)}
          >
            <SelectItem text="" value="" />
            {genders &&
              genders.map((gender) => (
                <SelectItem
                  key={gender.id}
                  text={gender.value || gender.localizedName}
                  value={gender.id}
                />
              ))}
          </Select>
        </Column>

        {/* Date of Birth */}
        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="birthDateForDisplay"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.birthDate",
                  defaultMessage: "Date of Birth",
                })}
              </>
            }
            value={formData.birthDateForDisplay || ""}
            onChange={(date) => onInputChange("birthDateForDisplay", date)}
            disallowFutureDate={true}
          />
        </Column>

        {/* Age (Read-only, calculated) */}
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
      </Grid>
    </Section>
  );
};

export default PatientInfoSection;
