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
  Heading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const initialFormValues = {
  centerName: "",
  centerCode: "",
  doctor: "",
  receivedDate: "",
  receivedTime: "",
  dateTaken: "",
  timeTaken: "",
  uniqueHealthId: "",
  siteUniqueHealthId: "",
  labNo: "",
  gender: "",
  dateOfBirth: "",
  age: {
    years: "",
  },
  specimens: {
    dryTube: false,
    edtaTube: false,
  },
  dryTubeTests: {
    serologyHIVTest: false,
    glycemiaTest: false,
    creatinineTest: false,
    transaminaseTest: false,
  },
  edtaTubeTests: {
    nfsTest: false,
    cd4cd8Test: false,
  },
  otherTests: {
    viralLoadTest: false,
    genotyping: false,
  },
  underInvestigation: "",
  note: "",
};

function InitialARV() {
  const [formValues, setFormValues] = useState(initialFormValues);
  const intl = useIntl();

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;

    setFormValues((prev) => {
      if (name.includes(".")) {
        const [parent, child] = name.split(".");
        return {
          ...prev,
          [parent]: {
            ...prev[parent],
            [child]: type === "checkbox" ? checked : value,
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
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Section>
                <Heading>
                  <FormattedMessage id="project.ARVStudy.name" />
                </Heading>
              </Section>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Form onSubmit={handleSubmit}>
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Stack gap={5}>
              <Select
                id="center-name"
                labelText={
                  <>
                    <FormattedMessage id="sample.entry.project.ARV.centerCode" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="centerName"
                value={formValues.centerName}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                {/* API call to get array and then map to get SelectItem Components */}
              </Select>

              <Select
                id="center-code"
                labelText={
                  <>
                    <FormattedMessage id="sample.entry.project.ARV.centerCode" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="centerCode"
                value={formValues.centerCode}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                {/* API call to get array and then map to get SelectItem Components */}
              </Select>

              <TextInput
                id="doctor"
                labelText="Doctor"
                name="doctor"
                value={formValues.doctor}
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
                name="receivedDate"
                value={formValues.receivedDate}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="received-time"
                labelText="Received Time (HH:mm)"
                type="time"
                name="receivedTime"
                value={formValues.receivedTime}
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
                name="dateTaken"
                value={formValues.dateTaken}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="time-taken"
                labelText="Time Taken (HH:mm)"
                type="time"
                name="timeTaken"
                value={formValues.timeTaken}
                onChange={handleInputChange}
              />

              <TextInput
                id="unique-health-id"
                labelText={
                  <>
                    <FormattedMessage id="patient.subject.number" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="uniqueHealthId"
                value={formValues.uniqueHealthId}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="site-unique-health-id"
                labelText={
                  <>
                    <FormattedMessage id="patient.site.subject.number" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="siteUniqueHealthId"
                value={formValues.siteUniqueHealthId}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="lab-no"
                labelText={
                  <>
                    <FormattedMessage id="RTN.label.lab.no" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="labNo"
                value={formValues.labNo}
                onChange={handleInputChange}
                required
              />

              <Select
                id="gender"
                labelText={
                  <>
                    <FormattedMessage id="RTN.label.gender" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                name="gender"
                value={formValues.gender}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="M" text="Male" />
                <SelectItem value="F" text="Female" />
                <SelectItem value="O" text="Other" />
              </Select>

              <TextInput
                id="date-of-birth"
                labelText={
                  <>
                    <FormattedMessage id="RTN.label.date.of.birth" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                type="date"
                name="dateOfBirth"
                value={formValues.dateOfBirth}
                onChange={handleInputChange}
                required
              />

              <FormGroup legendText="Age">
                <TextInput
                  id="age-years"
                  labelText="year"
                  type="number"
                  name="age.years"
                  value={formValues.age.years}
                  onChange={handleInputChange}
                />
              </FormGroup>

              <FormGroup legendText="Specimens Collected">
                <Stack gap={2}>
                  <Checkbox
                    id="dry-tube"
                    labelText="Dry tube"
                    name="specimens.dryTube"
                    checked={formValues.specimens.dryTube}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="edta-tube"
                    labelText="EDTA tube"
                    name="specimens.edtaTube"
                    checked={formValues.specimens.edtaTube}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="Dry Tube Tests">
                <Stack gap={2}>
                  <Checkbox
                    id="serology-hiv-test"
                    labelText="Serology HIV Test"
                    name="dryTubeTests.serologyHIVTest"
                    checked={formValues.dryTubeTests.serologyHIVTest}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="glycemia-test"
                    labelText="Glycemia Test"
                    name="dryTubeTests.glycemiaTest"
                    checked={formValues.dryTubeTests.glycemiaTest}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="creatinine-test"
                    labelText="Creatinine Test"
                    name="dryTubeTests.creatinineTest"
                    checked={formValues.dryTubeTests.creatinineTest}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="transaminase-test"
                    labelText="Transaminase Test"
                    name="dryTubeTests.transaminaseTest"
                    checked={formValues.dryTubeTests.transaminaseTest}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="EDTA Tube Tests">
                <Stack gap={2}>
                  <Checkbox
                    id="nfs-test"
                    labelText="NFS Test"
                    name="edtaTubeTests.nfsTest"
                    checked={formValues.edtaTubeTests.nfsTest}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="cd4-cd8-test"
                    labelText="CD4/CD8 Test"
                    name="edtaTubeTests.cd4cd8Test"
                    checked={formValues.edtaTubeTests.cd4cd8Test}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="Other Tests">
                <Stack gap={2}>
                  <Checkbox
                    id="viral-load-test"
                    labelText="Viral Load Test"
                    name="otherTests.viralLoadTest"
                    checked={formValues.otherTests.viralLoadTest}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="genotyping"
                    labelText="Genotyping"
                    name="otherTests.genotyping"
                    checked={formValues.otherTests.genotyping}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <Select
                id="under-investigation"
                labelText="Under Investigation"
                name="underInvestigation"
                value={formValues.underInvestigation}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="yes" text="Yes" />
                <SelectItem value="no" text="No" />
              </Select>

              <TextInput
                id="note"
                labelText="Note"
                name="note"
                value={formValues.note}
                onChange={handleInputChange}
              />
            </Stack>
            <div>
              <Button kind="primary" type="submit">
                <FormattedMessage id="button.save" />
              </Button>
              <Button kind="secondary">
                <FormattedMessage id="button.cancel" />
              </Button>
            </div>
          </Column>
        </Grid>
      </Form>
    </div>
  );
}

export default InitialARV;
