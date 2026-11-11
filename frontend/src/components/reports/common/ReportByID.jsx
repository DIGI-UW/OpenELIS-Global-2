import React, { useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { Form, Grid, Column, Section, Button, Loading } from "@carbon/react";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import { AlertDialog } from "../../common/CustomNotification";
import config from "../../../config.json";

function ReportByID(props) {
  const intl = useIntl();
  const [nationalId, setNationalId] = useState("");
  const [errors, setErrors] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = () => {
    if (!nationalId) {
      setErrors({ nationalId: "National ID is required" });
      return;
    }

    setLoading(true);

    console.log("National ID:", nationalId);
    const baseParams = `report=${props.report}&type=patient`;
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&patientNumberDirect=${nationalId}`;
    window.open(url, "_blank");

    setNationalId("");
    setErrors({});
    setLoading(false);
    setNotificationVisible(true);
  };

  // Function to handle changes in the input field
  const handleInputChange = (event) => {
    setErrors({});
    setNationalId(event.target.value);
  };

  return (
    <>
      <br />
      <Form>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <h3>
                  <FormattedMessage id={props.id} />
                </h3>
              </Section>
            </Section>
          </Column>
        </Grid>
        <br />
        {notificationVisible && <AlertDialog />}
        {loading && <Loading />}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <FormattedMessage id="label.report.byNationalId" />
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={6} md={4} sm={4}>
            <CustomLabNumberInput
              id="nationalID"
              labelText={intl.formatMessage({
                id: "nationalID.title",
                defaultMessage: "National ID",
              })}
              value={nationalId}
              onChange={handleInputChange} // Use the new handler here
              invalid={errors.nationalId}
              invalidText={errors.nationalId}
            />
          </Column>
        </Grid>
        <br />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Button
                data-cy="printableVersion"
                type="button"
                onClick={handleSubmit}
                disabled={!nationalId}
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

export default injectIntl(ReportByID);
