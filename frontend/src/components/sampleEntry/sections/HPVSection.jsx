import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
  RadioButtonGroup,
  RadioButton,
  Grid,
  Column,
} from "@carbon/react";

const HPVSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get ARV centers from organization lists (HPV uses ARV_ORGS)
  const arvCentersByCode =
    organizationLists && organizationLists["ARV_ORGS"]
      ? organizationLists["ARV_ORGS"]
      : [];

  // Get HIV_STATUSES dictionary
  const hivStatusList =
    dictionaryLists && dictionaryLists["HIV_STATUSES"]
      ? dictionaryLists["HIV_STATUSES"]
      : [];

  // Get HPV_SAMPLING_METHOD dictionary
  const hpvSamplingMethodList =
    dictionaryLists && dictionaryLists["HPV_SAMPLING_METHOD"]
      ? dictionaryLists["HPV_SAMPLING_METHOD"]
      : [];

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.HPV.title"
          defaultMessage="HPV Testing"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Organization Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.title.org"
              defaultMessage="Organization"
            />
          </h4>
        </Column>

        {/* Center Code */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="ARVcenterCode"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.project.centerCode",
                  defaultMessage: "Center Code",
                })}
              </>
            }
            value={projectData.ARVcenterCode || ""}
            onChange={(e) => onInputChange("ARVcenterCode", e.target.value)}
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

        {/* Patient Information Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.title.patientInfo"
              defaultMessage="Patient Information"
            />
          </h4>
        </Column>

        {/* HIV Status */}
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

        {/* Sample Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.title.sample"
              defaultMessage="Sample Information"
            />
          </h4>
        </Column>

        {/* Name of Clinician */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="nameOfDoctor"
            labelText={intl.formatMessage({
              id: "patient.project.nameOfClinician",
              defaultMessage: "Name of Clinician",
            })}
            value={observations.nameOfDoctor || ""}
            onChange={(e) =>
              onObservationChange("nameOfDoctor", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "patient.project.nameOfClinician.placeholder",
              defaultMessage: "Enter clinician name",
            })}
            maxLength={50}
          />
        </Column>

        {/* HPV Sampling Method */}
        <Column lg={16} md={8} sm={4}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "sample.entry.project.title.sampleType",
              defaultMessage: "Sample Collection Method",
            })}
            name="hpv-sampling-method"
            valueSelected={observations.hpvSamplingMethod || ""}
            onChange={(value) =>
              onObservationChange("hpvSamplingMethod", value)
            }
            orientation="vertical"
          >
            {hpvSamplingMethodList.map((item) => (
              <RadioButton
                key={item.id}
                id={`hpvSamplingMethod_${item.id}`}
                labelText={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </RadioButtonGroup>
        </Column>
      </Grid>
    </Section>
  );
};

export default HPVSection;
