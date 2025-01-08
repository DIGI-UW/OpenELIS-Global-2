import React, { useState, useEffect } from "react";
import {
  Form,
  FormLabel,
  Grid,
  Column,
  Section,
  SelectItem,
  Select,
  Button,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../Style.css";
import { AlertDialog } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import config from "../../../config.json";
import { encodeDate, Roles } from "../../utils/Utils";
import { getFromOpenElisServer } from "../../utils/Utils";
const ReportByDate = (props) => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [list, setList] = useState([]);

  const [reportFormValues, setReportFormValues] = useState({
    startDate: null,
    endDate: null,
    value: "",
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

  const handleSubmit = () => {
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

    if (
      (props.report === "activityReportByTest" ||
        props.report === "activityReportByPanel" ||
        props.report === "activityReportByTestSection") &&
      !reportFormValues.value
    ) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.value",
          defaultMessage: "Please select a value.",
        }),
      });
      return;
    }

    setReportFormValues({
      ...reportFormValues,
      error: "",
    });

    setLoading(true);
    let baseParams = "";

    if (
      props.report === "activityReportByTest" ||
      props.report === "activityReportByPanel" ||
      props.report === "activityReportByTestSection"
    ) {
      baseParams = `type=indicator&report=${props.report}&selectList.selection=${reportFormValues.value}`;
    } else if (props.report === "CISampleRoutineExport") {
      baseParams = `report=${props.report}&type=routine`;
    } else {
      baseParams = `report=${props.report}&type=patient`;
    }
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;

    const url = `${baseUrl}?${baseParams}&upperDateRange=${reportFormValues.endDate}&lowerDateRange=${reportFormValues.startDate}`;

    window.open(url, "_blank");
    setLoading(false);
    setNotificationVisible(true);
  };

  const setTempData = (data) => {
    setList(data);
    setLoading(false);
  };

  useEffect(() => {
    const fetchData = async () => {
      switch (props.report) {
        case "activityReportByTest":
          getFromOpenElisServer("/rest/test-list", setTempData);
          break;
        case "activityReportByPanel":
          getFromOpenElisServer("/rest/panels", setTempData);
          break;
        case "activityReportByTestSection":
          getFromOpenElisServer(
            "/rest/user-test-sections/" + Roles.REPORTS,
            setTempData,
          );
          break;
        default:
          break;
      }
    };

    console.log("props", props);
    if (
      props.report === "activityReportByTest" ||
      props.report === "activityReportByPanel" ||
      props.report === "activityReportByTestSection"
    ) {
      fetchData();
    }
  }, [props]);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={8} md={8} sm={4}>
          <FormLabel>
            <Section>
              <Section>
                <h1>
                  <FormattedMessage id={props.id ?? props.report} />
                </h1>
              </Section>
            </Section>
          </FormLabel>
        </Column>
      </Grid>
      {notificationVisible && <AlertDialog />}
      {loading && <Loading />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Form>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <br />
                  <br />
                  <h5>
                    <FormattedMessage id="label.select.dateRange" />
                  </h5>
                </Section>
              </Column>
              <Column lg={4} md={8} sm={4}>
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
                  onChange={(date) =>
                    handleDatePickerChangeDate("startDate", date)
                  }
                />
              </Column>
              <Column lg={4} md={8} sm={4}>
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
                  onChange={(date) =>
                    handleDatePickerChangeDate("endDate", date)
                  }
                />
              </Column>
              <Column lg={16}>
                {" "}
                <br />
              </Column>
              <Column lg={8} md={8} sm={4}>
                {list && list.length > 0 && (
                  <Select
                    id="type"
                    labelText={intl.formatMessage({
                      id: "label.form.searchby",
                    })}
                    value={reportFormValues.value}
                    onChange={(e) =>
                      setReportFormValues({
                        ...reportFormValues,
                        value: e.target.value,
                      })
                    }
                  >
                    <SelectItem
                      key={"emptyselect"}
                      value={""}
                      text="Select Test Type"
                    />
                    {list.map((statusOption) => (
                      <SelectItem
                        key={statusOption.id}
                        value={statusOption.id}
                        text={statusOption.value}
                      />
                    ))}
                  </Select>
                )}
              </Column>
            </Grid>
            <br />
            <Section>
              <br />
              {reportFormValues.error !== "" && (
                <div style={{ color: "#c62828", margin: 4 }}>
                  {reportFormValues.error}
                </div>
              )}

              <Button
                type="button"
                onClick={handleSubmit}
                disabled={
                  !reportFormValues.startDate && !reportFormValues.endDate
                }
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

export default ReportByDate;
