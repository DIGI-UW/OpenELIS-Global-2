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
  siteName: "",
  siteCode: "",
  dbsIdNumber: "",
  dbsSiteIdNumber: "",
  labNo: "",
  whichPCR: "",
  reasonForSecondPCR: "",
  infantInformation: {
    dateOfBirth: "",
    age: {
      months: "",
      weeks: "",
    },
    gender: "",
    infantBornFromPTME: "",
    typeOfClinic: "",
    howIsChildFed: "",
    stoppedBreastFeeding: "",
    presentingSigns: "",
    infantsProphylaxisARV: "",
    infantTakingCotrimoxazole: "",
  },
  mothersInformation: {
    hivStatus: "",
    arvTreatment: "",
  },
  underInvestigation: "",
  note: "",
  specimensCollected: {
    dryTube: false,
    dryBloodSpot: false,
    edtaPCR: false,
  },
};

function EID() {
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
    <div className="eid-form">
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16}>
            <h1 className="heading">EID</h1>
          </Column>

          <Column lg={16}>
            <Stack gap={5}>
              <TextInput
                id="received-date"
                labelText={
                  <>
                    Received Date (dd/mm/yyyy){" "}
                    <span className="required">*</span>
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
                    Date Taken (dd/mm/yyyy) <span className="required">*</span>
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

              <Select
                id="site-name"
                labelText={
                  <>
                    Site Name <span className="required">*</span>
                  </>
                }
                name="siteName"
                value={formValues.siteName}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
              </Select>

              <Select
                id="site-code"
                labelText={
                  <>
                    Site Code <span className="required">*</span>
                  </>
                }
                name="siteCode"
                value={formValues.siteCode}
                onChange={handleInputChange}
                required
              >
                <SelectItem value="" text="Choose an option" />
              </Select>

              <TextInput
                id="dbs-id-number"
                labelText={
                  <>
                    DBS ID Number <span className="required">*</span>
                  </>
                }
                name="dbsIdNumber"
                value={formValues.dbsIdNumber}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="dbs-site-id-number"
                labelText={
                  <>
                    DBS Site ID Number <span className="required">*</span>
                  </>
                }
                name="dbsSiteIdNumber"
                value={formValues.dbsSiteIdNumber}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="lab-no"
                labelText={
                  <>
                    Lab No <span className="required">*</span>
                  </>
                }
                name="labNo"
                value={formValues.labNo}
                onChange={handleInputChange}
                required
              />

              <Select
                id="which-pcr"
                labelText="Which PCR?"
                name="whichPCR"
                value={formValues.whichPCR}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
              </Select>

              <Select
                id="reason-for-second-pcr"
                labelText="Reason for second PCR test"
                name="reasonForSecondPCR"
                value={formValues.reasonForSecondPCR}
                onChange={handleInputChange}
              >
                <SelectItem value="" text="Choose an option" />
              </Select>

              <FormGroup legendText="Infant Information">
                <Stack gap={4}>
                  <TextInput
                    id="infant-date-of-birth"
                    labelText="Date of Birth (dd/mm/yyyy)"
                    type="date"
                    name="infantInformation.dateOfBirth"
                    value={formValues.infantInformation.dateOfBirth}
                    onChange={handleInputChange}
                  />

                  <div className="age-inputs">
                    <TextInput
                      id="infant-age-months"
                      labelText="months"
                      type="number"
                      name="infantInformation.age.months"
                      value={formValues.infantInformation.age.months}
                      onChange={handleInputChange}
                    />
                    <TextInput
                      id="infant-age-weeks"
                      labelText="weeks"
                      type="number"
                      name="infantInformation.age.weeks"
                      value={formValues.infantInformation.age.weeks}
                      onChange={handleInputChange}
                    />
                  </div>

                  <Select
                    id="infant-gender"
                    labelText="Gender"
                    name="infantInformation.gender"
                    value={formValues.infantInformation.gender}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                    <SelectItem value="M" text="Male" />
                    <SelectItem value="F" text="Female" />
                  </Select>

                  <Select
                    id="infant-born-from-ptme"
                    labelText="Did the infant benefit from PTME?"
                    name="infantInformation.infantBornFromPTME"
                    value={formValues.infantInformation.infantBornFromPTME}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                    <SelectItem value="yes" text="Yes" />
                    <SelectItem value="no" text="No" />
                  </Select>

                  <Select
                    id="type-of-clinic"
                    labelText="From which type of clinic did the infant come from?"
                    name="infantInformation.typeOfClinic"
                    value={formValues.infantInformation.typeOfClinic}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="how-is-child-fed"
                    labelText="How is the child fed?"
                    name="infantInformation.howIsChildFed"
                    value={formValues.infantInformation.howIsChildFed}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="stopped-breast-feeding"
                    labelText="Stopped Breast Feeding"
                    name="infantInformation.stoppedBreastFeeding"
                    value={formValues.infantInformation.stoppedBreastFeeding}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="presenting-signs"
                    labelText="Is the child presenting signs of HIV/AIDS?"
                    name="infantInformation.presentingSigns"
                    value={formValues.infantInformation.presentingSigns}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="infants-prophylaxis-arv"
                    labelText="Infant's Prophylaxis ARV"
                    name="infantInformation.infantsProphylaxisARV"
                    value={formValues.infantInformation.infantsProphylaxisARV}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="infant-taking-cotrimoxazole"
                    labelText="Infant taking cotrimoxazole?"
                    name="infantInformation.infantTakingCotrimoxazole"
                    value={
                      formValues.infantInformation.infantTakingCotrimoxazole
                    }
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>
                </Stack>
              </FormGroup>

              <FormGroup legendText="Mother's Information">
                <Stack gap={4}>
                  <Select
                    id="mothers-hiv-status"
                    labelText="Mother's HIV Status"
                    name="mothersInformation.hivStatus"
                    value={formValues.mothersInformation.hivStatus}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>

                  <Select
                    id="mothers-arv-treatment"
                    labelText="Mother's ARV Treatment"
                    name="mothersInformation.arvTreatment"
                    value={formValues.mothersInformation.arvTreatment}
                    onChange={handleInputChange}
                  >
                    <SelectItem value="" text="Choose an option" />
                  </Select>
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

              <FormGroup legendText="Specimens Collected">
                <Stack gap={2}>
                  <Checkbox
                    id="dry-tube"
                    labelText="Dry tube"
                    name="specimensCollected.dryTube"
                    checked={formValues.specimensCollected.dryTube}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="dry-blood-spot"
                    labelText="Dry Blood Spot"
                    name="specimensCollected.dryBloodSpot"
                    checked={formValues.specimensCollected.dryBloodSpot}
                    onChange={handleInputChange}
                  />
                  <Checkbox
                    id="edta-pcr"
                    labelText="EDTA PCR"
                    name="specimensCollected.edtaPCR"
                    checked={formValues.specimensCollected.edtaPCR}
                    onChange={handleInputChange}
                  />
                </Stack>
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
        .eid-form {
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
        .required {
          color: #da1e28;
        }
        :global(.cds--form-item) {
          margin-bottom: 1rem;
        }
      `}</style>
    </div>
  );
}

export default EID;
