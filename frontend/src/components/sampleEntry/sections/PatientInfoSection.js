import React from "react";
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

const PatientInfoSection = ({ formData, onInputChange, genders }) => {
  const intl = useIntl();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.patient.info"
          defaultMessage="Patient Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="subjectNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.patient.subject.number",
              defaultMessage: "Subject Number",
            })}
            value={formData.subjectNumber || ""}
            onChange={(e) => onInputChange("subjectNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.patient.subject.number.placeholder",
              defaultMessage: "Enter subject number",
            })}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="siteSubjectNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.patient.site.subject.number",
              defaultMessage: "Site Subject Number",
            })}
            value={formData.siteSubjectNumber || ""}
            onChange={(e) => onInputChange("siteSubjectNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.patient.site.subject.number.placeholder",
              defaultMessage: "Enter site subject number",
            })}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="upidCode"
            labelText={intl.formatMessage({
              id: "sample.entry.patient.upid",
              defaultMessage: "UPID Code",
            })}
            value={formData.upidCode || ""}
            onChange={(e) => onInputChange("upidCode", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.patient.upid.placeholder",
              defaultMessage: "Enter UPID code",
            })}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Select
            id="gender"
            labelText={intl.formatMessage({
              id: "sample.entry.patient.gender",
              defaultMessage: "Gender",
            })}
            value={formData.gender || ""}
            onChange={(e) => onInputChange("gender", e.target.value)}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "label.select.gender",
                defaultMessage: "Select Gender",
              })}
              value=""
            />
            {genders &&
              genders.map((gender) => (
                <SelectItem
                  key={gender.id}
                  text={gender.value}
                  value={gender.id}
                />
              ))}
          </Select>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="birthDateForDisplay"
            labelText={intl.formatMessage({
              id: "sample.entry.patient.birth.date",
              defaultMessage: "Date of Birth",
            })}
            value={formData.birthDateForDisplay || ""}
            onChange={(date) => onInputChange("birthDateForDisplay", date)}
            disallowFutureDate={true}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default PatientInfoSection;
