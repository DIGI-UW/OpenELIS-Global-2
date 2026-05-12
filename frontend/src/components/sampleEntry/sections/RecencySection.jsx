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

const RecencySection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
  gender,
}) => {
  const intl = useIntl();
  const [showPregnancySuckle, setShowPregnancySuckle] = useState({
    pregnancy: false,
    suckle: false,
  });

  // Get ARV centers from organization lists
  const arvCentersByCode =
    organizationLists && organizationLists["ARV_ORGS"]
      ? organizationLists["ARV_ORGS"]
      : [];

  // Get YES_NO dictionary
  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  // Check gender and show pregnancy/suckle fields if female
  useEffect(() => {
    if (gender) {
      // Find if gender is female (check various possible values)
      const isFemale =
        gender.toLowerCase().includes("f") ||
        gender.toLowerCase().includes("femme") ||
        gender.toLowerCase().includes("female");

      setShowPregnancySuckle({
        pregnancy: isFemale,
        suckle: isFemale,
      });
    } else {
      setShowPregnancySuckle({
        pregnancy: false,
        suckle: false,
      });
    }
  }, [gender]);

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.RT.title"
          defaultMessage="Recency Testing"
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

        {/* Pregnancy (Female only) */}
        {showPregnancySuckle.pregnancy && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="vlPregnancy"
              labelText={intl.formatMessage({
                id: "sample.project.vlPregnancy",
                defaultMessage: "Pregnant?",
              })}
              value={observations.vlPregnancy || ""}
              onChange={(e) =>
                onObservationChange("vlPregnancy", e.target.value)
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
        )}

        {/* Breastfeeding/Suckling (Female only) */}
        {showPregnancySuckle.suckle && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="vlSuckle"
              labelText={intl.formatMessage({
                id: "sample.project.vlSuckle",
                defaultMessage: "Breastfeeding?",
              })}
              value={observations.vlSuckle || ""}
              onChange={(e) => onObservationChange("vlSuckle", e.target.value)}
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
        )}

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
            maxLength={50}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default RecencySection;
