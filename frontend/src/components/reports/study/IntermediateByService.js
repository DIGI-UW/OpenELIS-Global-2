import React, { useState, useRef, useContext, useEffect } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { Form, Grid, Column, Section, Button } from "@carbon/react";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import AutoComplete from "../../common/AutoComplete";
import { ConfigurationContext } from "../../layout/Layout";
import CustomDatePicker from "../../common/CustomDatePicker";
import { encodeDate } from "../../utils/Utils";

const IntermediateByService = (props) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const componentMounted = useRef(false);
  const [siteNames, setSiteNames] = useState([]);
  const [selectedSiteName, setSelectedSiteName] = useState("");
  const [reportFormValues, setReportFormValues] = useState({
    startDate: null,
    endDate: null,
    error: null,
  });

  const handleDatePickerChangeDate = (datePicker, date) => {
    let updatedDate = encodeDate(date);
    let obj = null;
    switch (datePicker) {
      case "startDate":
        obj = {
          ...reportFormValues,
          startDate: updatedDate,
          error: null,
        };
        break;
      case "endDate":
        obj = {
          ...reportFormValues,
          endDate: updatedDate,
          error: null,
        };
        break;
      default:
    }
    setReportFormValues(obj);
  };

  function handleSiteName(e) {
    setSelectedSiteName(e.target.value);
  }

  function handleAutoCompleteSiteName(siteId) {
    setSelectedSiteName(siteId);
  }

  const getSiteList = (response) => {
    if (componentMounted.current) {
      setSiteNames(response);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/site-names", getSiteList);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!reportFormValues.startDate || !reportFormValues.endDate) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.dateRange.start",
          defaultMessage: "Please select Start and end date.",
        }),
      });
      return;
    }
    const baseParams = `report=${props.report}&type=patient`;
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}&locationCode=${selectedSiteName}`;

    window.open(url);
  };

  return (
    <>
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={16}>
            <Section>
              <h3>
                <FormattedMessage id={props.id} />
              </h3>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              key="startDate"
              id={"startDate"}
              labelText={intl.formatMessage({
                id: "eorder.date.start",
                defaultMessage: "Start Date",
              })}
              disallowFutureDate={true}
              autofillDate={true}
              value={reportFormValues.startDate}
              onChange={(date) => handleDatePickerChangeDate("startDate", date)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              key="endDate"
              id={"endDate"}
              labelText={intl.formatMessage({
                id: "eorder.date.end",
                defaultMessage: "End Date",
              })}
              disallowFutureDate={true}
              autofillDate={true}
              value={reportFormValues.endDate}
              onChange={(date) => handleDatePickerChangeDate("endDate", date)}
            />
          </Column>
        </Grid>
        <br />
        <br />
        <Grid fullWidth={true}>
          <Column lg={7} md={4} sm={3}>
            <AutoComplete
              name="siteName"
              id="siteName"
              className="inputText"
              allowFreeText={
                !(
                  configurationProperties.restrictFreeTextRefSiteEntry ===
                  "true"
                )
              }
              onChange={handleSiteName}
              onSelect={handleAutoCompleteSiteName}
              label={
                <>
                  <FormattedMessage id="select.referral.centre" />
                </>
              }
              suggestions={siteNames.length > 0 ? siteNames : []}
            />
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            {reportFormValues.error && (
              <p style={{ color: "red" }}>{reportFormValues.error}</p>
            )}
            <Section>
              <Button data-cy="printableVersion" type="submit">
                <FormattedMessage id="label.button.generatePrintableVersion" />
              </Button>
            </Section>
          </Column>
        </Grid>
      </Form>
    </>
  );
};

export default injectIntl(IntermediateByService);
