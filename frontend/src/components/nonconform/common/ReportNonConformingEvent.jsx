import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  Column,
  Form,
  Grid,
  Section,
  Select,
  SelectItem,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableBody,
  TextArea,
  TextInput,
  Table,
  RadioButton,
  UnorderedList,
  ListItem,
  Checkbox,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  NotificationKinds,
  AlertDialog,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import CustomDatePicker from "../../common/CustomDatePicker";
import { sampleObject } from "../../addOrder/Index";

const initialReportFormValues = {
  type: undefined,
  value: "",
  error: undefined,
  data: undefined,
};

const initialSelected = {
  specimenId: undefined,
  selected: false,
  labOrderNumber: undefined,
};

const initialNCEForm = {
  show: false,
  data: undefined,
  error: undefined,
};

export const ReportNonConformingEvent = () => {
  const [reportFormValues, setReportFormValues] = useState(
    initialReportFormValues,
  );
  const [ldata, setLData] = useState(null);
  const [selectedSample, setSelectedSample] = useState(initialSelected);
  // const [nceForm, setnceForm] = useState(initialNCEForm);
  const [orderSampleMap, setOrderSampleMap] = useState({});
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [nceForm, setnceForm] = useState({
    data: {
      reporterName: "",
      dateOfEvent: null,
      reportingUnit: "",
      description: "",
      suspectedCauses: "",
      proposedAction: "",
      reportingUnits: [],
    },
    errors: {
      reporterName: "",
      dateOfEvent: "",
      reportingUnit: "",
      description: "",
      suspectedCauses: "",
      proposedAction: "",
    },
  });

  const handleSubmit = () => {
    if (reportFormValues.type === undefined || reportFormValues.value === "") {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.nonconform.report",
        }),
      });
      return;
    }

    setReportFormValues({
      ...reportFormValues,
      error: undefined,
    });

    try {
      getFromOpenElisServer(
        `/rest/nonconformevents?${reportFormValues.type}=${reportFormValues.value}`,
        (data) => {
          if (data) {
            if (data.length > 0) {
              setLData(data);
            } else {
              setLData(null);
              setReportFormValues({
                ...reportFormValues,
                error: intl.formatMessage({
                  id: "error.nonconform.report.data.found",
                  defaultMessage: "No data found",
                }),
              });
            }
          } else {
            setReportFormValues({
              ...reportFormValues,
              error: intl.formatMessage({
                id: "error.nonconform.report.data.found",
                defaultMessage: "No data found",
              }),
            });
          }
        },
      );
    } catch (error) {
      setReportFormValues({
        ...reportFormValues,
        error: intl.formatMessage({
          id: "error.nonconform.report.data.found",
          defaultMessage: "No data found",
        }),
      });
    }
  };

  const selectOptions = [
    {
      text: "Last Name",
      value: "lastName",
    },
    {
      text: "First Name",
      value: "firstName",
    },
    {
      value: "STNumber",
      text: "Patient Identification Code",
    },
    {
      text: "Lab Number",
      value: "labNumber",
    },
  ];

  const headers = [
    {
      key: "labOrderNumber",
      value: "Lab Number",
    },
    {
      key: "type",
      value: "Specimen type",
    },
  ];

  useEffect(() => {
    console.log(JSON.stringify(selectedSample));
    if (selectedSample.labOrderNumber && selectedSample.specimenId) {
      getFromOpenElisServer(
        `/rest/reportnonconformingevent?labOrderNumber=${selectedSample.labOrderNumber}&specimenId=${selectedSample.specimenId}`,
        (data) => {
          console.log("data from server for selected", data);
          setLData(undefined);
          setnceForm({
            ...nceForm,
            show: true,
            data: data,
          });
        },
      );
    }
  }, [selectedSample]);

  const handleNCEFormSubmit = () => {
    console.log("nceForm.data", nceForm.data);
    const newErrors = {};
    if (!nceForm.data.dateOfEvent) {
      newErrors.dateOfEvent = "Date of event is required";
    }
    if (!nceForm.data.reportingUnit) {
      newErrors.reportingUnit = "Reporting unit is required";
    }
    if (!nceForm.data.description) {
      newErrors.description = "Description is required";
    }
    if (!nceForm.data.suspectedCauses) {
      newErrors.suspectedCauses = "Suspected causes are required";
    }
    if (!nceForm.data.proposedAction) {
      newErrors.proposedAction = "Proposed action is required";
    }
    if (Object.keys(newErrors).length > 0) {
      setnceForm((prev) => ({
        ...prev,
        errors: newErrors,
      }));
    } else {
      let body = {
        currentUserId: parseInt(nceForm.data.currentUserId),
        id: parseInt(nceForm.data.id),
        reportDate: nceForm.data.reportDate,
        name: nceForm.data.name,
        reporterName: nceForm.data.reporterName,
        dateOfEvent: nceForm.data.dateOfEvent,
        labOrderNumber: nceForm.data.labOrderNumber,
        prescriberName: nceForm.data.prescriberName,
        site: nceForm.data.site,
        reportingUnit: nceForm.data.reportingUnit,
        description: nceForm.data.description,
        suspectedCauses: nceForm.data.suspectedCauses,
        proposedAction: nceForm.data.proposedAction,
      };
      postToOpenElisServerJsonResponse(
        "/rest/reportnonconformingevent",
        JSON.stringify(body),
        (data) => {
          setNotificationVisible(true);
          setReportFormValues(initialReportFormValues);
          setLData(null);
          setSelectedSample(initialSelected);
          setnceForm(initialNCEForm);

          if (data.success) {
            addNotification({
              kind: NotificationKinds.success,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "nonconform.order.save.success",
              }),
            });
          } else {
            addNotification({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({ id: "nonconform.order.save.fail" }),
            });
          }
        },
      );
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id={`nonconform.report`} />
          </h2>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Form>
            <Grid fullWidth={true}>
              <Column lg={4} md={4} sm={2}>
                <Select
                  id="type"
                  labelText={intl.formatMessage({
                    id: "label.form.searchby",
                  })}
                  value={reportFormValues.type}
                  onChange={(e) => {
                    setReportFormValues({
                      ...reportFormValues,
                      type: e.target.value,
                    });
                  }}
                >
                  <SelectItem key={"emptyselect"} value={""} text={""} />
                  {selectOptions.map((statusOption) => (
                    <SelectItem
                      key={statusOption.value}
                      value={statusOption.value}
                      text={statusOption.text}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={4} md={4} sm={2}>
                <TextInput
                  labelText={intl.formatMessage({
                    id: "testcalculation.label.textValue",
                  })}
                  value={reportFormValues.value}
                  onChange={(e) => {
                    setReportFormValues({
                      ...reportFormValues,
                      value: e.target.value,
                    });
                  }}
                  id={`field.name`}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br></br>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <Button
                  type="button"
                  data-testid="nce-search-button"
                  onClick={handleSubmit}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
            </Grid>
            <br />
            <Section>
              <br />
              {!!reportFormValues.error && (
                <div style={{ color: "#c62828", margin: 4 }}>
                  {reportFormValues.error}
                </div>
              )}
            </Section>
          </Form>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
      </Grid>
      {ldata && ldata.length > 0 && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Table style={{ marginTop: "1em" }}>
              <TableHead>
                <TableRow>
                  <TableHeader key="checkbox" />
                  {headers.map((header) => (
                    <TableHeader id={header.key} key={header.key}>
                      {header.value}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {ldata.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell key={`${row.id}-checkbox`}></TableCell>
                    {headers.map((header) => (
                      <TableCell
                        key={header.key}
                        data-testid="nce-search-result"
                      >
                        <UnorderedList>
                          {header.key === "type"
                            ? row.sampleItems.map((item) => (
                                <Checkbox
                                  id={row.labOrderNumber + "-" + item.number}
                                  data-testid="nce-sample-checkbox"
                                  key={item.id}
                                  labelText={
                                    item.type +
                                    " (" +
                                    row.labOrderNumber +
                                    "-" +
                                    item.number +
                                    ")"
                                  }
                                  checked={
                                    orderSampleMap[row.labOrderNumber]
                                      ? orderSampleMap[
                                          row.labOrderNumber
                                        ].indexOf(item.id) !== -1
                                      : false
                                  }
                                  onChange={(e) => {
                                    let orderSample = { ...orderSampleMap };
                                    if (e.target.checked) {
                                      if (row.labOrderNumber in orderSample) {
                                        orderSample[row.labOrderNumber].push(
                                          item.id,
                                        );
                                      } else {
                                        orderSample = {
                                          [row.labOrderNumber]: [item.id],
                                        };
                                      }
                                    } else {
                                      if (row.labOrderNumber in orderSample) {
                                        var index = orderSample[
                                          row.labOrderNumber
                                        ].indexOf(item.id);
                                        if (index !== -1) {
                                          orderSample[
                                            row.labOrderNumber
                                          ].splice(index, 1);
                                        }
                                      }
                                    }
                                    setOrderSampleMap(orderSample);
                                  }}
                                />
                              ))
                            : row[header.key]}
                        </UnorderedList>
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Column>
          <Column lg={16}>
            <Button
              type="button"
              onClick={() => {
                const labNo = Object.keys(orderSampleMap)[0];
                if (labNo) {
                  setSelectedSample({
                    specimenId: orderSampleMap[labNo].join(","),
                    selected: true,
                    labOrderNumber: labNo,
                  });
                }
              }}
              data-testid="nce-goto-form-button"
            >
              <FormattedMessage id="nonconform.goToNceForm" />
            </Button>
          </Column>
        </Grid>
      )}
      {nceForm.show && nceForm.data && (
        <Grid fullWidth={true}>
          <Column lg={3} md={3} sm={3}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <b>
                  <FormattedMessage id="nonconform.report.field.date" />
                </b>
              </span>
            </div>
            <div style={{ marginBottom: "10px", color: "#555" }}>
              {nceForm.data.reportDate}
            </div>
          </Column>
          <Column lg={3} md={3} sm={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="patient.label.name" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>{nceForm.data.name}</div>
          </Column>
          <Column lg={3} md={3} sm={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.nce.number" />
              </span>
            </div>
            <div
              style={{ marginBottom: "10px" }}
              data-testid="nce-number-result"
            >
              {nceForm.data.nceNumber}
            </div>
          </Column>

          <Column lg={3} md={3} sm={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="sample.label.labnumber" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {nceForm.data.labOrderNumber}
            </div>
          </Column>
          <Column lg={4} md={3} sm={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.label.prescibernamesite" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {`${nceForm.data.prescriberName} | ${nceForm.data.site}`}
            </div>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <br></br>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              labelText={
                <FormattedMessage id="nonconform.person.reporting.different" />
              }
              value={nceForm.data.reporterName}
              onChange={(e) => {
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    reporterName: e.target.value,
                  },
                }));
              }}
              id={`field.name`}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              key="startDate"
              id={"startDate"}
              labelText={<FormattedMessage id="nonconform.date.event" />}
              disallowFutureDate={true}
              autofillDate={true}
              value={nceForm.data.dateOfEvent}
              onChange={(date) =>
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    dateOfEvent: date,
                  },
                  errors: {
                    ...prev.errors,
                    dateOfEvent: "",
                  },
                }))
              }
              invalid={!!nceForm.errors.dateOfEvent}
              invalidText={nceForm.errors.dateOfEvent}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              labelText={
                <FormattedMessage id="nonconform.label.reportingunit" />
              }
              id="reportingUnits"
              value={nceForm.data.reportingUnit}
              onChange={(e) => {
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    reportingUnit: e.target.value,
                  },
                  errors: {
                    ...prev.errors,
                    reportingUnit: "",
                  },
                }));
              }}
              invalid={!!nceForm.errors.reportingUnit}
              invalidText={nceForm.errors.reportingUnit}
            >
              <SelectItem key={"emptyselect"} value={""} text={""} />
              {nceForm.data.reportingUnits.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.id}
                  text={option.value}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={<FormattedMessage id="nonconform.description.nce" />}
              value={nceForm.data.description}
              onChange={(e) => {
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    description: e.target.value,
                  },
                  errors: {
                    ...prev.errors,
                    description: "",
                  },
                }));
              }}
              rows={2}
              id="text-area-1"
              invalid={!!nceForm.errors.description}
              invalidText={nceForm.errors.description}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={
                <FormattedMessage id="nonconform.label.suspected.cause.nce" />
              }
              value={nceForm.data.suspectedCauses}
              onChange={(e) => {
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    suspectedCauses: e.target.value,
                  },
                  errors: {
                    ...prev.errors,
                    suspectedCauses: "",
                  },
                }));
              }}
              rows={2}
              id="text-area-2"
              invalid={!!nceForm.errors.suspectedCauses}
              invalidText={nceForm.errors.suspectedCauses}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={
                <FormattedMessage id="nonconform.label.proposed.action" />
              }
              value={nceForm.data.proposedAction}
              onChange={(e) => {
                setnceForm((prev) => ({
                  ...prev,
                  data: {
                    ...prev.data,
                    proposedAction: e.target.value,
                  },
                  errors: {
                    ...prev.errors,
                    proposedAction: "",
                  },
                }));
              }}
              rows={2}
              id="text-area-3"
              invalid={!!nceForm.errors.proposedAction}
              invalidText={nceForm.errors.proposedAction}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <br></br>
          </Column>
          <Column lg={16} md={8} sm={4}>
            {!!nceForm.error && (
              <div style={{ color: "#c62828", margin: 4 }}>{nceForm.error}</div>
            )}
            <Button
              type="button"
              data-testid="nce-submit-button"
              onClick={() => handleNCEFormSubmit()}
            >
              <FormattedMessage id="label.button.submit" />
            </Button>
          </Column>
        </Grid>
      )}
    </>
  );
};

export default ReportNonConformingEvent;
