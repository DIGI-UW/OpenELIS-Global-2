import React, { useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { Form, Grid, Column, Section, Button } from "@carbon/react";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import config from "../../../config.json";

function ReportByLabNo(props) {
  const intl = useIntl();
  const [values, setValues] = useState({ from: "", to: "" });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setValues((prevState) => ({
      ...prevState,
      [name]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    const baseParams = `report=${props.report}&type=patient`;
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&accessionDirect=${values.from}&highAccessionDirect=${values.to}`;

    window.open(url, "_blank");
  };

  const isButtonDisabled = !values.from && !values.to;

  return (
    <>
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <h3>
                <FormattedMessage id={props.id} />
              </h3>
            </Section>
          </Column>
        </Grid>
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <h5>
                <FormattedMessage id="report.enter.labNumber.headline" />
              </h5>
              <h7>
                <FormattedMessage id="sample.search.scanner.instructions" />
              </h7>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={7} md={4} sm={3}>
            <CustomLabNumberInput
              name="from"
              value={values.from}
              labelText={intl.formatMessage({
                id: "from.title",
                defaultMessage: "From",
              })}
              id="from"
              onChange={handleChange}
            />
          </Column>
          <Column lg={7} md={4} sm={3}>
            <CustomLabNumberInput
              name="to"
              value={values.to}
              labelText={intl.formatMessage({
                id: "to.title",
                defaultMessage: "To",
              })}
              id="to"
              onChange={handleChange}
            />
          </Column>
        </Grid>
        <br />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Button
                data-cy="printableVersion"
                type="submit"
                disabled={isButtonDisabled}
              >
                <FormattedMessage id="label.button.generatePrintableVersion" />
              </Button>
            </Section>
          </Column>
        </Grid>
      </Form>
    </>
  );
}

export default injectIntl(ReportByLabNo);
