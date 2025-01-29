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

export const initialReportFormValues = {
  type: undefined,
  value: "",
  error: undefined,
};

export const selectOptions = [
  {
    value: "nceNumber",
    text: "NCE Number",
  },
  {
    text: "Lab Number",
    value: "labNumber",
  },
];

export const headers = [
  {
    key: "date",
    value: "Date",
  },
  {
    key: "nceNumber",
    value: "NCE Number",
  },
  {
    key: "specimen",
    value: "Lab Section/Unit",
  },
];

const initialFormData = {
  nceCategory: undefined,
  nceType: undefined,
  consequences: undefined,
  recurrence: undefined,
  severityScore: 0,
  correctiveAction: undefined,
  controlAction: undefined,
  comments: undefined,
  labComponent: undefined,
};

export const ViewNonConformingEvent = () => {
  const [reportFormValues, setReportFormValues] = useState(
    initialReportFormValues,
  );

  const [data, setData] = useState(null);

  const [formData, setFormData] = useState(initialFormData);
  const [nceTypes, setNceTypes] = useState([]);
  const [tData, setTData] = useState(null);
  const [selected, setSelected] = useState(null);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  useEffect(() => {
    let a = parseInt(formData.consequences ?? 0);
    let b = parseInt(formData.recurrence ?? 0);

    let c = a * b;

    if (
      typeof a === "number" &&
      typeof b === "number" &&
      typeof c === "number"
    ) {
      setFormData({
        ...formData,
        severityScore: c,
      });
    }
  }, [formData.consequences, formData.recurrence]);

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
    let other =
      reportFormValues.type == "labNumber" ? "nceNumber" : "labNumber";

    try {
      getFromOpenElisServer(
        `/rest/viewNonConformEvents?${reportFormValues.type}=${reportFormValues.value}&${other}=&status=Pending`,
        (data) => {
          //setReportFormValues(initialReportFormValues);
          if (!data.nceEventsSearchResults) {
            setReportFormValues({
              ...reportFormValues,
              error: intl.formatMessage({
                id: "no.data.found",
              }),
            });
            setData(null);
            setTData(null);
          } else {
            if (data.nceEventsSearchResults.length < 2) {
              setData(data);
              setNceTypes(data.nceTypes);
              setTData(null);
            } else {
              setTData(data);
              setData(null);
            }
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
      setData(null);
    }
  };

  useEffect(() => {
    if (data) {
      setNceTypes(
        data.nceTypes.filter((obj) => {
          let bol = Number(obj.categoryId) === Number(formData.nceCategory);
          return bol;
        }),
      );
    }
  }, [formData.nceCategory]);

  const handleNCEFormSubmit = () => {
    let body = {
      id: data.nceEventsSearchResults[0].id,
      laboratoryComponent: formData.labComponent,
      nceCategory: formData.nceCategory,
      nceType: formData.nceType,
      consequences: formData.consequences,
      recurrence: formData.recurrence,
      severityScore: formData.severityScore,
      correctiveAction: formData.correctiveAction,
      controlAction: formData.controlAction,
      comments: formData.comments,
      currentUserId: data.currentUserId ?? "",
      reporterName: data.nceEventsSearchResults[0].nameOfReporter ?? "",
      site: data.nceEventsSearchResults[0].site,
      nceNumber: data.nceEventsSearchResults[0].nceNumber,
      reportDate: data.reportDate,
      dateOfEvent: data.dateOfEvent,
      name: data.nceEventsSearchResults[0].name,
      reportingUnit: data.nceEventsSearchResults[0].reportingUnitId,
      prescriberName: data.nceEventsSearchResults[0].prescriberName,
      description: data.nceEventsSearchResults[0].description,
      suspectedCauses: data.nceEventsSearchResults[0].suspectedCauses,
    };

    postToOpenElisServerJsonResponse(
      "/rest/viewNonConformEvents",
      JSON.stringify(body),
      (data) => {
        setNotificationVisible(true);
        //setReportFormValues(initialReportFormValues);
        setData(null);

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
  };

  useEffect(() => {
    if (selected) {
      try {
        getFromOpenElisServer(
          `/rest/viewNonConformEvents?nceNumber=${selected}&labNumber=&status=Pending`,
          (data) => {
            if (!data.nceEventsSearchResults) {
              setReportFormValues({
                ...reportFormValues,
                error: intl.formatMessage({
                  id: "no.data.found",
                }),
              });
            } else {
              setData(data);
              setNceTypes(data.nceTypes);
              setTData(null);
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
    }
  }, [selected]);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Grid fullWidth={true}>
        <Column lg={16}>
          <h2>
            <FormattedMessage id={`nonconform.view.report`} />
          </h2>
        </Column>
        <Column lg={16} md={10} sm={8}>
          <Form>
            <Grid fullWidth={true}>
              <Column lg={4} md={8}>
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
              <Column lg={4}>
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
              <Column lg={16}>
                <br></br>
              </Column>
              <Column lg={16}>
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
        <Column lg={16}>
          <br></br>
        </Column>
      </Grid>

      {tData && (
        <div>
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
                  {tData.nceEventsSearchResults.map((row) => (
                    <TableRow key={row.nceNumber}>
                      <TableCell
                        key={`${row}-checkbox`}
                        data-testid="Radio-button"
                      >
                        <RadioButton
                          name="radio-group"
                          onClick={() => {
                            setSelected(row.nceNumber);
                            console.log(row);
                          }}
                          labelText=""
                          id={row.id}
                        />
                      </TableCell>

                      <TableCell key={row.key + "date"}>
                        {new Date(row.reportDate).toDateString()}
                      </TableCell>

                      <TableCell key={row.key + "1"}>{row.nceNumber}</TableCell>

                      <TableCell key={row.key + "2"}>
                        {
                          tData.reportingUnits.find(
                            (obj) => parseInt(obj.id) === row.reportingUnitId,
                          ).value
                        }
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Column>
          </Grid>
        </div>
      )}

      {data && (
        <Grid fullWidth={true}>
          <Column lg={3}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <b>
                  <FormattedMessage id="nonconform.nce.number" />
                </b>
              </span>
            </div>
            <div
              style={{ marginBottom: "10px", color: "#555" }}
              data-testid="nce-number-result"
            >
              {data.nceEventsSearchResults[0].nceNumber}
            </div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.view.event.date" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>{data.dateOfEvent}</div>
          </Column>

          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.view.reporting.person" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {data.nceEventsSearchResults[0].nameOfReporter}
            </div>
          </Column>

          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.label.reportingunit" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {
                data.reportingUnits.find((obj) => obj.id == data.reportingUnit)
                  .value
              }
            </div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.view.specimen" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {data.specimens[0].typeOfSample.description}
            </div>
          </Column>
          <Column lg={1}></Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="sample.label.labnumber" />
              </span>
            </div>
            <div
              style={{ marginBottom: "10px" }}
              data-testid="nce-search-result"
            >
              {data.nceEventsSearchResults[0].labOrderNumber}
            </div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.label.prescibernamesite" />
              </span>
            </div>
            <div
              style={{ marginBottom: "10px" }}
            >{`${data.nceEventsSearchResults[0].prescriberName}-${data.nceEventsSearchResults[0].site}`}</div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.view.event.description" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {data.nceEventsSearchResults[0].description ?? ""}
            </div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.label.suspected.cause.nce" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {data.nceEventsSearchResults[0].suspectedCauses ?? ""}
            </div>
          </Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.label.proposed.action" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>
              {data.nceEventsSearchResults[0].proposedAction ?? ""}
            </div>
          </Column>
          <Column lg={1}></Column>
          <Column lg={3} style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "10px" }}>
              <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                <FormattedMessage id="nonconform.severity.score" />
              </span>
            </div>
            <div style={{ marginBottom: "10px" }}>{formData.severityScore}</div>
          </Column>
          <pre>
            <p>
              {" "}
              <FormattedMessage id="nonconform.label.lowseverity" />{" "}
            </p>
            <p>
              {" "}
              <FormattedMessage id="nonconform.label.highseverity" />
            </p>
          </pre>
          <Column lg={13}></Column>

          <Column lg={16}>
            <br></br>
          </Column>
          <Column lg={8}>
            <Select
              labelText={<FormattedMessage id="nonconform.view.nce.category" />}
              id="nceCategory"
              value={formData.nceCategory}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  nceCategory: e.target.value,
                });
              }}
            >
              <SelectItem key={"emptyselect"} value={""} text={""} />
              {data?.nceCategories?.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.name}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8}>
            <Select
              labelText={<FormattedMessage id="nonconform.view.nce.type" />}
              id="nceType"
              value={formData.nceType}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  nceType: e.target.value,
                });
              }}
            >
              <SelectItem key={"emptyselect"} value={""} text={""} />
              {nceTypes.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.name}
                />
              ))}
            </Select>
          </Column>

          <Column lg={16}>
            <br></br>
          </Column>

          <Column lg={8}>
            <Select
              labelText={
                <FormattedMessage id="nonconform.view.severe.consequences" />
              }
              id="consequences"
              value={formData.consequences}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  consequences: e.target.value,
                });
              }}
            >
              {data.severityConsequencesList.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.value}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8}>
            <Select
              labelText={
                <FormattedMessage id="nonconform.view.nce.likely.occur" />
              }
              id="recurrence"
              value={formData.recurrence}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  recurrence: e.target.value,
                });
              }}
            >
              {data.severityRecurrenceList.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.value}
                />
              ))}
            </Select>
          </Column>

          <Column lg={16}>
            <br></br>
          </Column>
          <Column lg={8}>
            <Select
              labelText={
                <FormattedMessage id="nonconform.view.lab.component" />
              }
              id="labComponent"
              value={formData.labComponent}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  labComponent: e.target.value,
                });
              }}
            >
              <SelectItem value="" text="" />
              {data.labComponentList.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.value}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={
                <FormattedMessage id="nonconform.view.corrective.action.description" />
              }
              value={formData.correctiveAction}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  correctiveAction: e.target.value,
                });
              }}
              invalid={formData.correctiveAction?.length > 200}
              invalidText={<FormattedMessage id="text.length.max" />}
              rows={2}
              id="text-area-10"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={
                <FormattedMessage id="nonconform.view.preventive.description" />
              }
              value={formData.controlAction}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  controlAction: e.target.value,
                });
              }}
              rows={2}
              id="text-area-3"
              invalid={formData.controlAction?.length > 200}
              invalidText={<FormattedMessage id="text.length.max" />}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextArea
              labelText={<FormattedMessage id="nonconform.view.comments" />}
              value={formData.comments}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  comments: e.target.value,
                });
              }}
              rows={2}
              id="text-area-2"
              invalid={formData.comments?.length > 200}
              invalidText={<FormattedMessage id="text.length.max" />}
            />
          </Column>
          <Column lg={16}>
            <br></br>
          </Column>

          <Column lg={16}>
            {false && (
              <div style={{ color: "#c62828", margin: 4 }}>
                {formData.error}
              </div>
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
