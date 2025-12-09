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
} from "@carbon/react";

const initialFormValues = {
  receivedDate: "",
  receivedTime: "",
  dateTaken: "",
  timeTaken: "",
  uniqueHealthId: "",
  siteUniqueHealthId: "",
  dateOfBirth: "",
  age: "",
  gender: "",
  labNo: "",
  reasonForRequest: "",
  specimens: {
    dryTube: false,
    edtaTube: false,
    dryBloodSpot: false,
  },
  dryTubeTests: {
    maricCombination: false,
    genscreen: false,
    vitrosvida: false,
    innolia: false,
    glycemiaTest: false,
    creatineTest: false,
    transaminaseTest: false,
    transaminaseALTL: false,
    transaminaseASTL: false,
  },
  edtaTubeTests: {
    nfsTest: false,
    gb: false,
    lymphPercent: false,
    mono: false,
    eoPercent: false,
    basoPercent: false,
    gr: false,
    hb: false,
    hct: false,
    vgm: false,
    tcmh: false,
    ccmh: false,
    plq: false,
    cd4cd8Test: false,
    cd3Count: false,
    cd4Count: false,
  },
  otherTests: {
    dnaPCR: false,
    viralLoadTest: false,
    genotyping: false,
  },
  underInvestigation: "",
  note: "",
};

function SpecialRequest() {
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
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16}>
            <h1>Special Request</h1>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
              <TextInput
                id="received-date"
                labelText="* Received Date (dd/mm/yyyy)"
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
                labelText="* Date Taken (dd/mm/yyyy)"
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
                id="date-of-birth"
                labelText="* Date of Birth (dd/mm/yyyy)"
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

              <Select
                id="gender"
                labelText="* Gender"
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
                id="lab-no"
                labelText="* Lab No"
                name="labNo"
                value={formValues.labNo}
                onChange={handleInputChange}
                required
              />

              <Select
                id="reason-for-request"
                labelText="Reason for Request"
                name="reasonForRequest"
                value={formValues.reasonForRequest}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
                {/* Add reason options here */}
              </Select>
            </Stack>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
              <FormGroup legendText="Specimens Collected">
                <Stack gap={3}>
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
                  <Checkbox
                    id="dry-blood-spot"
                    labelText="Dry Blood Spot"
                    name="specimens.dryBloodSpot"
                    checked={formValues.specimens.dryBloodSpot}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="Dry Tube Tests">
                <Stack gap={3}>
                  {Object.entries(formValues.dryTubeTests).map(
                    ([key, value]) => (
                      <Checkbox
                        key={key}
                        id={key}
                        labelText={key
                          .replace(/([A-Z])/g, " $1")
                          .replace(/^./, (str) => str.toUpperCase())}
                        name={`dryTubeTests.${key}`}
                        checked={value}
                        onChange={handleInputChange}
                      />
                    ),
                  )}
                </Stack>
              </FormGroup>

              <FormGroup legendText="EDTA Tube Tests">
                <Stack gap={3}>
                  {Object.entries(formValues.edtaTubeTests).map(
                    ([key, value]) => (
                      <Checkbox
                        key={key}
                        id={key}
                        labelText={key
                          .replace(/([A-Z])/g, " $1")
                          .replace(/^./, (str) => str.toUpperCase())}
                        name={`edtaTubeTests.${key}`}
                        checked={value}
                        onChange={handleInputChange}
                      />
                    ),
                  )}
                </Stack>
              </FormGroup>

              <FormGroup legendText="Other Tests">
                <Stack gap={3}>
                  {Object.entries(formValues.otherTests).map(([key, value]) => (
                    <Checkbox
                      key={key}
                      id={key}
                      labelText={key
                        .replace(/([A-Z])/g, " $1")
                        .replace(/^./, (str) => str.toUpperCase())}
                      name={`otherTests.${key}`}
                      checked={value}
                      onChange={handleInputChange}
                    />
                  ))}
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
          </Column>

          <Column lg={16}>
            <div>
              <Button kind="primary" type="submit">
                Save
              </Button>
              <Button kind="secondary">Cancel</Button>
            </div>
          </Column>
        </Grid>
      </Form>
    </div>
  );
}

export default SpecialRequest;
