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
  centerName: "",
  centerCode: "",
  nameOfClinician: "",
  nameOfSampler: "",
  receivedDate: "",
  receivedTime: "",
  dateTaken: "",
  timeTaken: "",
  uniqueHealthId: "",
  siteUniqueHealthId: "",
  labNo: "",
  dateOfBirth: "",
  age: {
    years: "",
    months: "",
  },
  sex: "",
  hivType: "",
  isReceivingARV: "",
  reasonForViralLoad: "",
  treatmentInitiation: {
    cd4Count: "",
    cd4PercentageCount: "",
    date: "",
  },
  viralLoadRequest: {
    cd4Count: "",
    cd4PercentageCount: "",
    date: "",
  },
  priorViralLoadRequest: "",
  underInvestigation: "",
  note: "",
  specimens: {
    edtaTube: false,
    dryBloodSpot: false,
    psc: false,
  },
  tests: {
    viralLoadTest: true,
  },
};

function ARVViralOverload() {
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
    <div className="arv-viral-load-form">
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16}>
            <h1 className="heading">ARV - Viral Load</h1>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
              <Select
                id="center-name"
                labelText="* Center Name"
                name="centerName"
                value={formValues.centerName}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                {/* Add center options */}
              </Select>

              <Select
                id="center-code"
                labelText="* Center Code"
                name="centerCode"
                value={formValues.centerCode}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                {/* Add code options */}
              </Select>

              <TextInput
                id="name-of-clinician"
                labelText="Name of clinician"
                name="nameOfClinician"
                value={formValues.nameOfClinician}
                onChange={handleInputChange}
              />

              <TextInput
                id="name-of-sampler"
                labelText="Name of Sampler"
                name="nameOfSampler"
                value={formValues.nameOfSampler}
                onChange={handleInputChange}
              />

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
            </Stack>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
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
                labelText="* Lab No"
                name="labNo"
                value={formValues.labNo}
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

              <FormGroup legendText="Age">
                <div className="age-inputs">
                  <TextInput
                    id="age-years"
                    labelText="Years"
                    type="number"
                    name="age.years"
                    value={formValues.age.years}
                    onChange={handleInputChange}
                    size="sm"
                  />
                  <TextInput
                    id="age-months"
                    labelText="Months"
                    type="number"
                    name="age.months"
                    value={formValues.age.months}
                    onChange={handleInputChange}
                    size="sm"
                  />
                </div>
              </FormGroup>

              <Select
                id="sex"
                labelText="* Sex"
                name="sex"
                value={formValues.sex}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="M" text="Male" />
                <SelectItem value="F" text="Female" />
              </Select>

              <Select
                id="hiv-type"
                labelText="* HIV Type"
                name="hivType"
                value={formValues.hivType}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="1" text="HIV-1" />
                <SelectItem value="2" text="HIV-2" />
              </Select>
            </Stack>
          </Column>

          <Column lg={16}>
            <Stack gap={5}>
              <Select
                id="is-receiving-arv"
                labelText="Is the patient currently receiving ARV treatment?"
                name="isReceivingARV"
                value={formValues.isReceivingARV}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="yes" text="Yes" />
                <SelectItem value="no" text="No" />
              </Select>

              <Select
                id="reason-for-viral-load"
                labelText="Reason for viral load request"
                name="reasonForViralLoad"
                value={formValues.reasonForViralLoad}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
                {/* Add reasons */}
              </Select>

              <FormGroup legendText="At treatment initiation">
                <Stack gap={3}>
                  <TextInput
                    id="treatment-cd4-count"
                    labelText="CD4 Count"
                    name="treatmentInitiation.cd4Count"
                    value={formValues.treatmentInitiation.cd4Count}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="treatment-cd4-percentage"
                    labelText="CD4 Percentage Count"
                    name="treatmentInitiation.cd4PercentageCount"
                    value={formValues.treatmentInitiation.cd4PercentageCount}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="treatment-date"
                    labelText="Date"
                    type="date"
                    name="treatmentInitiation.date"
                    value={formValues.treatmentInitiation.date}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="Viral Load request">
                <Stack gap={3}>
                  <TextInput
                    id="viral-load-cd4-count"
                    labelText="CD4 Count"
                    name="viralLoadRequest.cd4Count"
                    value={formValues.viralLoadRequest.cd4Count}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="viral-load-cd4-percentage"
                    labelText="CD4 Percentage Count"
                    name="viralLoadRequest.cd4PercentageCount"
                    value={formValues.viralLoadRequest.cd4PercentageCount}
                    onChange={handleInputChange}
                  />
                  <TextInput
                    id="viral-load-date"
                    labelText="Date"
                    type="date"
                    name="viralLoadRequest.date"
                    value={formValues.viralLoadRequest.date}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <Select
                id="prior-viral-load"
                labelText="Prior Viral Load Request made?"
                name="priorViralLoadRequest"
                value={formValues.priorViralLoadRequest}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
                <SelectItem value="yes" text="Yes" />
                <SelectItem value="no" text="No" />
              </Select>

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

              <FormGroup legendText="Specimens Collected">
                <Stack gap={3}>
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
                  <Checkbox
                    id="psc"
                    labelText="PSC (?)"
                    name="specimens.psc"
                    checked={formValues.specimens.psc}
                    onChange={handleInputChange}
                  />
                </Stack>
              </FormGroup>

              <FormGroup legendText="Tests">
                <Checkbox
                  id="viral-load-test"
                  labelText="Viral Load Test"
                  name="tests.viralLoadTest"
                  checked={formValues.tests.viralLoadTest}
                  onChange={handleInputChange}
                  disabled
                />
              </FormGroup>
            </Stack>
          </Column>

          <Column lg={16}>
            <div className="button-group">
              <Button kind="primary" type="submit">
                Save
              </Button>
              <Button kind="secondary">Cancel</Button>
            </div>
          </Column>
        </Grid>
      </Form>

      <style>{`
        .arv-viral-load-form {
          padding: 1rem;
        }
        .heading {
          font-size: 1.5rem;
          margin-bottom: 2rem;
          color: #161616;
        }
        .age-inputs {
          display: flex;
          gap: 1rem;
        }
        .button-group {
          display: flex;
          gap: 1rem;
          margin-top: 2rem;
        }
        :global(.cds--form-item) {
          margin-bottom: 1rem;
        }
      `}</style>
    </div>
  );
}

export default ARVViralOverload;
