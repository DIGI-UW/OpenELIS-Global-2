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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const initialFormValues = {
  receivedDate: "",
  receivedTime: "",
  dateTaken: "",
  timeTaken: "",
  dateOfBirth: "",
  age: {
    years: "",
    months: "",
  },
  gender: "",
  labNo: "",
  specimens: {
    dryTube: false,
  },
  tests: {
    serologyHIVTest: false,
  },
  underInvestigation: "",
  note: "",
};

function RTN() {
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
    // Handle form submission
    console.log("Form values:", formValues);
  };

  return (
    <div>
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16}>
            <h1>{intl.formatMessage({ id: "RTN.title" })}</h1>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
              <TextInput
                id="received-date"
                labelText={intl.formatMessage({
                  id: "RTN.label.received.date",
                })}
                type="date"
                name="receivedDate"
                value={formValues.receivedDate}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="received-time"
                labelText={intl.formatMessage({
                  id: "RTN.label.received.time",
                })}
                type="time"
                name="receivedTime"
                value={formValues.receivedTime}
                onChange={handleInputChange}
              />

              <TextInput
                id="date-taken"
                labelText={intl.formatMessage({ id: "RTN.label.date.taken" })}
                type="date"
                name="dateTaken"
                value={formValues.dateTaken}
                onChange={handleInputChange}
                required
              />

              <TextInput
                id="time-taken"
                labelText={intl.formatMessage({ id: "RTN.label.time.taken" })}
                type="time"
                name="timeTaken"
                value={formValues.timeTaken}
                onChange={handleInputChange}
              />

              <TextInput
                id="date-of-birth"
                labelText={intl.formatMessage({
                  id: "RTN.label.date.of.birth",
                })}
                type="date"
                name="dateOfBirth"
                value={formValues.dateOfBirth}
                onChange={handleInputChange}
                required
              />

              <FormGroup
                legendText={intl.formatMessage({ id: "RTN.legendText.age" })}
              >
                <div>
                  <TextInput
                    id="age-years"
                    labelText={intl.formatMessage({ id: "RTN.label.year" })}
                    type="number"
                    name="age.years"
                    value={formValues.age.years}
                    onChange={handleInputChange}
                    size="sm"
                  />
                  <TextInput
                    id="age-months"
                    labelText={intl.formatMessage({ id: "RTN.label.month" })}
                    type="number"
                    name="age.months"
                    value={formValues.age.months}
                    onChange={handleInputChange}
                    size="sm"
                  />
                </div>
              </FormGroup>
            </Stack>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Stack gap={5}>
              <Select
                id="gender"
                labelText={intl.formatMessage({ id: "RTN.label.gender" })}
                name="gender"
                value={formValues.gender}
                onChange={handleInputChange}
                required
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "RTN.selectItem.choose.an.option",
                  })}
                />
                <SelectItem
                  value="M"
                  text={intl.formatMessage({ id: "RTN.selectItem.male" })}
                />
                <SelectItem
                  value="F"
                  text={intl.formatMessage({ id: "RTN.selectItem.female" })}
                />
                <SelectItem
                  value="O"
                  text={intl.formatMessage({ id: "RTN.selectItem.other" })}
                />
              </Select>

              <TextInput
                id="lab-no"
                labelText={intl.formatMessage({ id: "RTN.label.lab.no" })}
                name="labNo"
                value={formValues.labNo}
                onChange={handleInputChange}
                required
              />

              <FormGroup
                legendText={intl.formatMessage({
                  id: "RTN.legendText.specimens.collected",
                })}
              >
                <Checkbox
                  id="dry-tube"
                  labelText={intl.formatMessage({ id: "RTN.label.dry.tube" })}
                  name="specimens.dryTube"
                  checked={formValues.specimens.dryTube}
                  onChange={handleInputChange}
                />
              </FormGroup>

              <FormGroup
                legendText={intl.formatMessage({
                  id: "RTN.legendText.dry.tube.tests",
                })}
              >
                <Checkbox
                  id="serology-hiv-test"
                  labelText={intl.formatMessage({
                    id: "RTN.label.serology.HIV.test",
                  })}
                  name="tests.serologyHIVTest"
                  checked={formValues.tests.serologyHIVTest}
                  onChange={handleInputChange}
                />
              </FormGroup>

              <Select
                id="under-investigation"
                labelText={intl.formatMessage({
                  id: "RTN.label.under.investigation",
                })}
                name="underInvestigation"
                value={formValues.underInvestigation}
                onChange={handleInputChange}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "RTN.selectItem.choose.an.option",
                  })}
                />
                <SelectItem
                  value="yes"
                  text={intl.formatMessage({ id: "RTN.selectItem.yes" })}
                />
                <SelectItem
                  value="no"
                  text={intl.formatMessage({ id: "RTN.selectItem.no" })}
                />
              </Select>

              <TextInput
                id="note"
                labelText={intl.formatMessage({ id: "RTN.label.note" })}
                name="note"
                value={formValues.note}
                onChange={handleInputChange}
              />
            </Stack>
          </Column>

          <Column lg={16}>
            <div>
              <Button kind="primary" type="submit">
                {intl.formatMessage({ id: "RTN.button.save" })}
              </Button>
              <Button kind="secondary">
                {intl.formatMessage({ id: "RTN.button.cancel" })}
              </Button>
            </div>
          </Column>
        </Grid>
      </Form>
    </div>
  );
}

export default RTN;
