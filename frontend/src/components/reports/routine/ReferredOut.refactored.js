import React, { useEffect, useState, useRef } from "react";
import {
  Form as CarbonForm,
  Heading,
  Grid,
  Column,
  Section,
  Button,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Formik, Form } from "formik";
import * as Yup from "yup";
import "../../Style.css";
import { getFromOpenElisServer, encodeDate } from "../../utils/Utils";
import { AlertDialog } from "../../common/CustomNotification";
import { FormSelectField, FormDatePickerField } from "../../common/formFields";
import config from "../../../config.json";

const ReferredOut = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const componentMounted = useRef(false);
  const [locationCodes, setLocationCodes] = useState([]);

  const fetchLocationCodes = (locationCodez) => {
    if (componentMounted.current) {
      setLocationCodes(locationCodez);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/referral-organizations", fetchLocationCodes);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "routine.reports", link: "/RoutineReports" },
    {
      label: "external.referrals.report",
      link: "/RoutineReport?type=patient&report=referredOut",
    },
  ];

  // Validation schema
  const validationSchema = Yup.object({
    startDate: Yup.string().required(
      intl.formatMessage({ id: "error.startDate.required" }),
    ),
    endDate: Yup.string().required(
      intl.formatMessage({ id: "error.endDate.required" }),
    ),
    locationCode: Yup.string().required(
      intl.formatMessage({ id: "error.locationCode.required" }),
    ),
  });

  // Initial form values
  const initialValues = {
    startDate: "",
    endDate: "",
    locationCode: "",
  };

  const handlePrinting = (values) => {
    setLoading(true);
    const baseParams = "report=referredOut&type=patient";
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&upperDateRange=${values.endDate}&lowerDateRange=${values.startDate}&locationCode=${values.locationCode}`;

    const check = window.open(url, "_blank");
    if (check) {
      setLoading(false);
      setNotificationVisible(true);
    } else {
      setLoading(false);
      <AlertDialog />;
    }
  };

  // Transform locationCodes to options format
  const locationOptions = locationCodes.map((locationcode) => ({
    value: locationcode.id,
    text: locationcode.value,
  }));

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="externalReferredOutTests.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Formik
            initialValues={initialValues}
            validationSchema={validationSchema}
            onSubmit={handlePrinting}
          >
            {({ isSubmitting }) => (
              <Form>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <Section>
                      <br />
                      <h5>
                        <FormattedMessage id="select.datarange.label" />
                      </h5>
                    </Section>
                  </Column>
                  <Column lg={4} md={8} sm={4}>
                    <FormDatePickerField
                      name="startDate"
                      labelText={intl.formatMessage({
                        id: "select.start.date.referredTests",
                        defaultMessage: "Start Date",
                      })}
                      autofillDate={true}
                    />
                  </Column>
                  <Column lg={4} md={8} sm={4}>
                    <FormDatePickerField
                      name="endDate"
                      labelText={intl.formatMessage({
                        id: "select.end.date.referredTests",
                        defaultMessage: "End Date",
                      })}
                      autofillDate={true}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <Section>
                      <br />
                      <h5>
                        <FormattedMessage id="select.referral.centre.is.required" />
                      </h5>
                    </Section>
                  </Column>
                  <Column lg={10} md={8} sm={4}>
                    {locationOptions.length > 0 && (
                      <FormSelectField
                        name="locationCode"
                        labelText={intl.formatMessage({
                          id: "select.referral.centre",
                          defaultMessage: "Laboratory",
                        })}
                        options={locationOptions}
                        required={true}
                      />
                    )}
                  </Column>
                </Grid>
                <br />
                <Section>
                  <br />
                  <Button
                    data-cy="printableVersion"
                    type="submit"
                    disabled={isSubmitting}
                  >
                    <FormattedMessage
                      id="label.button.generatePrintableVersion"
                      defaultMessage="Generate printable version"
                    />
                  </Button>
                </Section>
              </Form>
            )}
          </Formik>
        </Column>
      </Grid>
    </>
  );
};

export default ReferredOut;
