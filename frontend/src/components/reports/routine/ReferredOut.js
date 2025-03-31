import React, { useEffect, useState, useRef } from "react";
import {
  Form,
  Heading,
  Select,
  SelectItem,
  Grid,
  Column,
  Section,
  Button,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../Style.css";
import { getFromOpenElisServer } from "../../utils/Utils";
import { AlertDialog } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import config from "../../../config.json";
import { encodeDate } from "../../utils/Utils";

const ReferredOut = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [reportFormValues, setReportFormValues] = useState({
    startDate: null,
    endDate: null,
  });
  const componentMounted = useRef(false);
  const [locationCodes, setLocationCodes] = useState([]);
  const [selectedLocationCode, setSelectedLocationCode] = useState("");

  const handleSelectionChange = (event) => {
    const selectedValue = event.target.value;
    setSelectedLocationCode(selectedValue);
  };

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

  const handleDatePickerChangeDate = (datePicker, date) => {
    let updatedDate = encodeDate(date);
    let obj = null;
    switch (datePicker) {
      case "startDate":
        obj = {
          ...reportFormValues,
          startDate: updatedDate,
        };
        break;
      case "endDate":
        obj = {
          ...reportFormValues,
          endDate: updatedDate,
        };
        break;
      default:
    }
    setReportFormValues(obj);
  };

  const handlePrinting = () => {
    setLoading(true);
    const baseParams = "report=referredOut&type=patient";
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;
    const url = `${baseUrl}?${baseParams}&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}&locationCode=${selectedLocationCode}`;

    const check = window.open(url, "_blank");
    if (check) {
      setLoading(false);
      setNotificationVisible(true);
    } else {
      setLoading(false);
      <AlertDialog />;
    }
  };

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
                <CustomDatePicker
                  id={"startDate"}
                  labelText={intl.formatMessage({
                    id: "select.start.date.referredTests",
                    defaultMessage: "Start Date",
                  })}
                  autofillDate={true}
                  value={reportFormValues.startDate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("startDate", date)
                  }
                />
              </Column>
              <Column lg={4} md={8} sm={4}>
                <CustomDatePicker
                  id={"endDate"}
                  labelText={intl.formatMessage({
                    id: "select.end.date.referredTests",
                    defaultMessage: "End Date",
                  })}
                  className="inputDate"
                  autofillDate={true}
                  value={reportFormValues.endDate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("endDate", date)
                  }
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
                {locationCodes.length > 0 && (
                  <Select
                    id="locationcode"
                    value={selectedLocationCode}
                    onChange={handleSelectionChange}
                    labelText={intl.formatMessage({
                      id: "select.referral.centre",
                      defaultMessage: "Laboratory",
                    })}
                  >
                    <SelectItem value="" text="" />
                    {locationCodes.map((locationcode) => (
                      <SelectItem
                        key={locationcode.id}
                        value={locationcode.id}
                        text={locationcode.value}
                      />
                    ))}
                  </Select>
                )}
              </Column>
            </Grid>
            <br />
            <Section>
              <br />
              <Button
                data-cy="printableVersion"
                type="button"
                onClick={handlePrinting}
              >
                <FormattedMessage
                  id="label.button.generatePrintableVersion"
                  defaultMessage="Generate printable version"
                />
              </Button>
            </Section>
          </Form>
        </Column>
      </Grid>
    </>
  );
};

export default ReferredOut;
