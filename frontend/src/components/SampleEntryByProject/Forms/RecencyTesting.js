import React, { useState } from "react";
import {
  Form,
  TextInput,
  Select,
  SelectItem,
  Checkbox,
  Grid,
  Column,
  Button,
  Stack,
  FormGroup,
  FormLabel,
  Section,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const initialFormValues = {
  formType: "Recency Testing",
  facility: {
    centerCode: "",
  },
  patientInfo: {
    labNo: "",
    recencyIDNo: "",
    dateOfBirth: "",
    age: {
      years: "",
    },
    sex: "",
  },
  sampleInfo: {
    clinicianName: "",
    samplerName: "",
    receivedDate: "",
    receivedTime: "",
    dateTaken: "",
    timeTaken: "",
  },
  sampleType: {
    plasma: false,
    serum: false,
  },
  tests: {
    asanteHIV1Recency: false,
  },
};

function RecencyTesting() {
  const [formValues, setFormValues] = useState(initialFormValues);
  const intl = useIntl();

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;

    setFormValues((prev) => {
      const [section, field] = name.split(".");
      if (section && field) {
        return {
          ...prev,
          [section]: {
            ...prev[section],
            [field]: type === "checkbox" ? checked : value,
          },
        };
      }
      return {
        ...prev,
        [name]: type === "checkbox" ? checked : value,
      };
    });
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    console.log("Form values:", formValues);
  };

  return (
    <div>
      <Form onSubmit={handleSubmit}>
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Stack gap={7}>
              {/* <Section>
                <FormGroup legendText="Form">
                  <Select
                    id="form-type"
                    labelText=""
                    name="formType"
                    value={formValues.formType}
                    onChange={handleInputChange}
                  >
                    <SelectItem
                      value="Recency Testing"
                      text="Recency Testing"
                    />
                  </Select>
                </FormGroup>
              </Section> */}

              <Section>
                <FormGroup legendText="Facility">
                  <Select
                    id="center-code"
                    labelText="* Center Code"
                    name="facility.centerCode"
                    value={formValues.facility.centerCode}
                    onChange={handleInputChange}
                    required
                  >
                    <SelectItem value="" text="Select Center Code" />
                    {/* API call to get array and then map to get SelectItem Components */}
                  </Select>
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Patient Information">
                  <Stack gap={5}>
                    <TextInput
                      id="lab-no"
                      labelText="* Lab No"
                      name="patientInfo.labNo"
                      value={formValues.patientInfo.labNo}
                      onChange={handleInputChange}
                      required
                    />
                    <TextInput
                      id="recency-id"
                      labelText="* Recency ID No"
                      name="patientInfo.recencyIDNo"
                      value={formValues.patientInfo.recencyIDNo}
                      onChange={handleInputChange}
                      required
                    />
                    <TextInput
                      id="date-of-birth"
                      labelText="* Date of Birth (dd/mm/yyyy)"
                      type="date"
                      name="patientInfo.dateOfBirth"
                      value={formValues.patientInfo.dateOfBirth}
                      onChange={handleInputChange}
                      required
                    />
                    <TextInput
                      id="age-years"
                      labelText="Age (years)"
                      type="number"
                      name="patientInfo.age.years"
                      value={formValues.patientInfo.age.years}
                      onChange={handleInputChange}
                    />
                    <Select
                      id="sex"
                      labelText="* Sex"
                      name="patientInfo.sex"
                      value={formValues.patientInfo.sex}
                      onChange={handleInputChange}
                      required
                    >
                      <SelectItem value="" text="Select Sex" />
                      <SelectItem value="M" text="Male" />
                      <SelectItem value="F" text="Female" />
                      <SelectItem value="O" text="Other" />
                    </Select>
                  </Stack>
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Sample Information">
                  <Stack gap={5}>
                    <TextInput
                      id="clinician-name"
                      labelText="Name of Clinician"
                      name="sampleInfo.clinicianName"
                      value={formValues.sampleInfo.clinicianName}
                      onChange={handleInputChange}
                    />
                    <TextInput
                      id="sampler-name"
                      labelText="Name of Sampler"
                      name="sampleInfo.samplerName"
                      value={formValues.sampleInfo.samplerName}
                      onChange={handleInputChange}
                    />
                    <TextInput
                      id="received-date"
                      labelText="* Received Date (dd/mm/yyyy)"
                      type="date"
                      name="sampleInfo.receivedDate"
                      value={formValues.sampleInfo.receivedDate}
                      onChange={handleInputChange}
                      required
                    />
                    <TextInput
                      id="received-time"
                      labelText="Received Time (HH:mm)"
                      type="time"
                      name="sampleInfo.receivedTime"
                      value={formValues.sampleInfo.receivedTime}
                      onChange={handleInputChange}
                    />
                    <TextInput
                      id="date-taken"
                      labelText="* Date Taken (dd/mm/yyyy)"
                      type="date"
                      name="sampleInfo.dateTaken"
                      value={formValues.sampleInfo.dateTaken}
                      onChange={handleInputChange}
                      required
                    />
                    <TextInput
                      id="time-taken"
                      labelText="Time Taken (HH:mm)"
                      type="time"
                      name="sampleInfo.timeTaken"
                      value={formValues.sampleInfo.timeTaken}
                      onChange={handleInputChange}
                    />
                  </Stack>
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Sample Type">
                  <Stack gap={3}>
                    <Checkbox
                      id="plasma"
                      labelText="Plasma"
                      name="sampleType.plasma"
                      checked={formValues.sampleType.plasma}
                      onChange={handleInputChange}
                    />
                    <Checkbox
                      id="serum"
                      labelText="Serum"
                      name="sampleType.serum"
                      checked={formValues.sampleType.serum}
                      onChange={handleInputChange}
                    />
                  </Stack>
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Tests">
                  <Checkbox
                    id="asante-hiv1-recency"
                    labelText="Asante HIV-1 Rapid Recency"
                    name="tests.asanteHIV1Recency"
                    checked={formValues.tests.asanteHIV1Recency}
                    onChange={handleInputChange}
                  />
                </FormGroup>
              </Section>

              <Section>
                <div>
                  <Button kind="primary" type="submit">
                    <FormattedMessage id="button.save" />
                  </Button>
                  <Button kind="secondary">
                    <FormattedMessage id="button.cancel" />
                  </Button>
                </div>
              </Section>
            </Stack>
          </Column>
        </Grid>
      </Form>
    </div>
  );
}

export default RecencyTesting;
