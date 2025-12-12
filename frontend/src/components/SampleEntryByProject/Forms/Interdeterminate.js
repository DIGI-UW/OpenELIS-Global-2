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
import { FormattedMessage } from "react-intl";

const initialFormValues = {
  receivedDate: "",
  receivedTime: "",
  dateTaken: "",
  timeTaken: "",
  siteName: "",
  address: "",
  phoneNumber: "",
  faxNumber: "",
  email: "",
  uniqueHealthId: "",
  siteUniqueHealthId: "",
  labNo: "",
  gender: "",
  dateOfBirth: "",
  age: "",
  firstTest: {
    date: "",
    testName: "",
    result: "",
  },
  secondTest: {
    date: "",
    testName: "",
    result: "",
    finalResult: "",
  },
  specimens: {
    dryTube: false,
  },
  tests: {
    serologyHIVTest: false,
  },
  underInvestigation: "",
  note: "",
};

function Indeterminate() {
  const [formValues, setFormValues] = useState(initialFormValues);

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
                  <FormattedMessage id="project.IndeterminateStudy.name" />
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
                labelText={
                  <>
                    <FormattedMessage id="RTN.label.received.time" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
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
                labelText={
                  <>
                    <FormattedMessage id="RTN.label.time.taken" />
                    <span style={{ color: "red" }}>*</span>
                  </>
                }
                type="time"
                name="timeTaken"
                value={formValues.timeTaken}
                onChange={handleInputChange}
              />

              <Select
                id="site-name"
                labelText="* Site Name"
                name="siteName"
                value={formValues.siteName}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                {/* Add site options here */}
              </Select>

              <TextInput
                id="address"
                labelText="Address"
                name="address"
                value={formValues.address}
                onChange={handleInputChange}
              />

              <TextInput
                id="phone-number"
                labelText="Phone Number"
                name="phoneNumber"
                value={formValues.phoneNumber}
                onChange={handleInputChange}
              />

              <TextInput
                id="fax-number"
                labelText="Fax Number"
                name="faxNumber"
                value={formValues.faxNumber}
                onChange={handleInputChange}
              />

              <TextInput
                id="email"
                labelText="Email"
                type="email"
                name="email"
                value={formValues.email}
                onChange={handleInputChange}
              />
              <TextInput
                id="unique-health-id"
                labelText="* Unique Health ID number"
                name="uniqueHealthId"
                value={formValues.uniqueHealthId}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="site-unique-health-id"
                labelText="* Site Unique Health ID number"
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

              <TextInput
                id="age"
                labelText="Age"
                type="number"
                name="age"
                value={formValues.age}
                onChange={handleInputChange}
              />

              {/* First Test Section */}
              <FormGroup legendText="First Test">
                <Stack gap={3}>
                  <TextInput
                    id="first-test-date"
                    labelText="Date"
                    type="date"
                    name="firstTest.date"
                    value={formValues.firstTest.date}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="first-test-name"
                    labelText="Test Name"
                    name="firstTest.testName"
                    value={formValues.firstTest.testName}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="first-test-result"
                    labelText="Result"
                    name="firstTest.result"
                    value={formValues.firstTest.result}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              {/* Second Test Section */}
              <FormGroup legendText="Second Test">
                <Stack gap={3}>
                  <TextInput
                    id="second-test-date"
                    labelText="Date"
                    type="date"
                    name="secondTest.date"
                    value={formValues.secondTest.date}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="second-test-name"
                    labelText="Test Name"
                    name="secondTest.testName"
                    value={formValues.secondTest.testName}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="second-test-result"
                    labelText="Result"
                    name="secondTest.result"
                    value={formValues.secondTest.result}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="second-test-final-result"
                    labelText="Final Result of Site"
                    name="secondTest.finalResult"
                    value={formValues.secondTest.finalResult}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              {/* Specimens Section */}
              <FormGroup legendText="Specimens Collected">
                <Checkbox
                  id="dry-tube"
                  labelText="Dry Tube"
                  name="specimens.dryTube"
                  checked={formValues.specimens.dryTube}
                  onChange={handleInputChange}
                />
              </FormGroup>

              {/* Tests Section */}
              <FormGroup legendText="Dry Tube Tests">
                <Checkbox
                  id="serology-hiv-test"
                  labelText="Serology HIV Test"
                  name="tests.serologyHIVTest"
                  checked={formValues.tests.serologyHIVTest}
                  onChange={handleInputChange}
                />
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
              <div>
                <Button kind="primary" type="submit">
                  <FormattedMessage id="button.save" />
                </Button>
                <Button kind="secondary">
                  <FormattedMessage id="button.cancel" />
                </Button>
              </div>
            </Stack>
          </Column>
        </Grid>
      </Form>
    </div>
  );
}

export default Indeterminate;
