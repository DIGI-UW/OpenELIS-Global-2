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
  Section,
  ButtonSet,
  Link,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const initialFormValues = {
  facility: {
    centerCode: "",
  },
  patientInfo: {
    labNo: "",
    patientId: "",
    hivStatus: "",
    dateOfBirth: "",
    age: {
      years: "",
    },
  },
  sampleInfo: {
    clinicianName: "",
    receivedDate: "",
    receivedTime: "",
    dateTaken: "",
    timeTaken: "",
  },
  specimens: {
    preserveCyt: false,
  },
  tests: {
    hpvHR: false,
    abbottRoche: false,
    geneXpert: false,
  },
};

function HPVTesting() {
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

  const handleLabNoScan = () => {
    // Implement scanner functionality
    console.log("Scanning lab number...");
  };

  const generateLabNo = () => {
    // Implement lab number generation logic
    console.log("Generating lab number...");
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
                    <div>
                      <TextInput
                        id="lab-no"
                        labelText="* Lab No"
                        name="patientInfo.labNo"
                        value={formValues.patientInfo.labNo}
                        onChange={handleInputChange}
                        required
                      />
                      <div>
                        <Button
                          kind="ghost"
                          // hasIconOnly
                          iconDescription="Scan"
                          onClick={handleLabNoScan}
                        >
                          Scan
                        </Button>
                        <span>OR</span>
                        <Button
                          kind="ghost"
                          iconDescription="Enter Manually"
                          onClick={() => {}}
                        >
                          Enter Manually
                        </Button>
                        <span>OR</span>
                        <Button
                          kind="ghost"
                          iconDescription="Generate"
                          onClick={generateLabNo}
                        >
                          Generate
                        </Button>
                      </div>
                    </div>

                    <TextInput
                      id="patient-id"
                      labelText="* Patient Identification"
                      name="patientInfo.patientId"
                      value={formValues.patientInfo.patientId}
                      onChange={handleInputChange}
                      required
                    />

                    <Select
                      id="hiv-status"
                      labelText="HIV Status"
                      name="patientInfo.hivStatus"
                      value={formValues.patientInfo.hivStatus}
                      onChange={handleInputChange}
                    >
                      <SelectItem value="" text="Select HIV Status" />
                      {/* HIV Status yahan pr ane chahiye, SelectItem ka use krke */}
                    </Select>

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
                      id="received-date"
                      labelText={
                        <>
                          <FormattedMessage id="RTN.label.received.date" />
                          <span style={{ color: "red" }}>*</span>
                        </>
                      }
                      type="date"
                      name="sampleInfo.receivedDate"
                      value={formValues.sampleInfo.receivedDate}
                      onChange={handleInputChange}
                      required
                    />

                    <TextInput
                      id="received-time"
                      labelText={
                        <>
                          <FormattedMessage id="RTN.label.received.time" />
                          <span style={{ color: "red" }}>*</span>
                        </>
                      }
                      type="time"
                      name="sampleInfo.receivedTime"
                      value={formValues.sampleInfo.receivedTime}
                      onChange={handleInputChange}
                    />

                    <TextInput
                      id="date-taken"
                      labelText={
                        <>
                          <FormattedMessage id="RTN.label.date.taken" />
                          <span style={{ color: "red" }}>*</span>
                        </>
                      }
                      type="date"
                      name="sampleInfo.dateTaken"
                      value={formValues.sampleInfo.dateTaken}
                      onChange={handleInputChange}
                      required
                    />

                    <TextInput
                      id="time-taken"
                      labelText={
                        <>
                          <FormattedMessage id="RTN.label.time.taken" />
                          <span style={{ color: "red" }}>*</span>
                        </>
                      }
                      type="time"
                      name="sampleInfo.timeTaken"
                      value={formValues.sampleInfo.timeTaken}
                      onChange={handleInputChange}
                    />
                  </Stack>
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Specimens Collected">
                  <Checkbox
                    id="preserve-cyt"
                    labelText="PreservCyt (Cervico-vaginal sample)"
                    name="specimens.preserveCyt"
                    checked={formValues.specimens.preserveCyt}
                    onChange={handleInputChange}
                  />
                </FormGroup>
              </Section>

              <Section>
                <FormGroup legendText="Tests">
                  <Stack gap={3}>
                    <Checkbox
                      id="hpv-hr"
                      labelText="test HPV HR"
                      name="tests.hpvHR"
                      checked={formValues.tests.hpvHR}
                      onChange={handleInputChange}
                    />
                    <Checkbox
                      id="abbott-roche"
                      labelText="Analysis on Abbott or Roche equipment"
                      name="tests.abbottRoche"
                      checked={formValues.tests.abbottRoche}
                      onChange={handleInputChange}
                    />
                    <Checkbox
                      id="gene-xpert"
                      labelText="Analysis on GeneXpert"
                      name="tests.geneXpert"
                      checked={formValues.tests.geneXpert}
                      onChange={handleInputChange}
                    />
                  </Stack>
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

export default HPVTesting;
