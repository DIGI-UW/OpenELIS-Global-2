import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  Column,
  Form,
  Grid,
  Select,
  SelectItem,
  TextInput,
  Section,
  Table,
  TableHead,
  TableRow,
  TableBody,
  RadioButton,
  TextArea,
  TableHeader,
  Checkbox,
  TableCell,
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";
import { initialReportFormValues, selectOptions } from "./ViewNonConforming";
import {
  getDifferenceInDays,
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import CustomDatePicker from "../../common/CustomDatePicker";
import { headers } from "./ViewNonConforming";
import {
  NotificationKinds,
  AlertDialog,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";

const initialFormData = {
  dateCompleted: null,
  discussionDate: null,
  actionLog: {
    correctiveAction: null,
    actionType: null,
    personResponsible: undefined,
    dateCompleted: undefined,
    turnAroundTime: undefined,
  },
};

export const NCECorrectiveAction = () => {
  const [reportFormValues, setReportFormValues] = useState(
    initialReportFormValues,
  );

  const [selected, setSelected] = useState(null);
  const [tdiscussionDate, setTDiscussionDate] = useState(null);
  const [tData, setTData] = useState(null);
  const [data, setData] = useState(null);
  const [formData, setFormData] = useState(initialFormData);
  const [submit, setSubmit] = useState(null);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  useEffect(() => {
    if (selected) {
      try {
        getFromOpenElisServer(
          `/rest/NCECorrectiveAction?nceNumber=${selected}`,
          (data) => {
            if (!data.cancelAction) {
              return;
            }
            setTData(null);
            setFormData({
              ...formData,
              discussionDate: data.discussionDate,
            });

            setData(data);
          },
        );
      } catch (error) {
        console.log("Error fetching data", error);
      }
    }
  }, [selected]);

  const handleSubmit = () => {
    let other =
      reportFormValues.type === "labNumber" ? "nceNumber" : "labNumber";

    if (reportFormValues.type === "labNumber") {
      getFromOpenElisServer(
        `/rest/nonconformingcorrectiveaction?status=CAPA&${reportFormValues.type}=${reportFormValues.value}&uppressExternalSearch=false&${other}=undefined`,
        (responseData) => {
          if (responseData.nceEventsSearchResults.length > 1) {
            setSelected(null);
            setData(null);
            setTData(responseData);
          } else if (responseData?.nceEventsSearchResults?.length === 1) {
            setSelected(responseData.nceEventsSearchResults[0].nceNumber);
          } else {
            // Handle case when no data is found
          }
        },
      );
    } else {
      // Set selected directly if the search type is not 'labNumber'
      setSelected(reportFormValues.value);
    }
  };

  const handleNCEFormSubmit = () => {
    if (!submit) {
      return;
    }

    let turnAroundTime = formData[`dateCompleted`]
      ? getDifferenceInDays(data.reportDate, formData[`dateCompleted`])
      : 0;

    formData.actionLog.turnAroundTime = turnAroundTime;

    // correctiveAction
    let body = {
      id: data.id,
      actionLog: [...data["actionLog"], formData["actionLog"]],

      dateCompleted: formData[`dateCompleted`] ?? "",
      discussionDate: formData[`discussionDate`] ?? "",
    };

    postToOpenElisServerJsonResponse(
      "/rest/NCECorrectiveAction",
      JSON.stringify(body),
      (df) => {
        setNotificationVisible(true);
        setData(null);
        setFormData(initialFormData);
        setSubmit(null);

        if (df.success) {
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

  const handleCorrectiveActionChange = (e) => {
    setFormData({
      ...data,
      actionLog: {
        ...formData.actionLog,
        correctiveAction: e.target.value,
      },
    });
  };

  const handleDiscussionDateChange = (date) => {
    setTDiscussionDate(date);
  };

  const handleAddDiscussionDate = () => {
    if (tdiscussionDate) {
      if (data.discussionDate) {
        setFormData({
          ...formData,
          discussionDate: data.discussionDate + "," + tdiscussionDate,
        });
      } else {
        setFormData({
          ...formData,
          discussionDate: tdiscussionDate,
        });
      }

      setTDiscussionDate(null);
    }
  };

  const handlePersonResponsibleChange = (e) => {
    setFormData({
      ...formData,
      actionLog: {
        ...formData.actionLog,
        personResponsible: e.target.value,
      },
    });
  };

  const handleActionTypeChange = (value) => {
    setFormData((prevFormData) => {
      const actionTypes = prevFormData.actionLog.actionType
        ? prevFormData.actionLog.actionType.split(",").filter((type) => type) // Filter out empty strings
        : [];

      if (actionTypes.includes(value)) {
        return {
          ...prevFormData,
          actionLog: {
            ...prevFormData.actionLog,
            actionType: actionTypes.filter((type) => type !== value).join(","),
          },
        };
      } else {
        return {
          ...prevFormData,
          actionLog: {
            ...prevFormData.actionLog,
            actionType: [...actionTypes, value].join(","),
          },
        };
      }
    });
  };

  return (
    <div>
      {notificationVisible && <AlertDialog />}
      <Grid fullWidth={true}>
        <Column lg={16}>
          <h2>
            <FormattedMessage id={`nonconform.corrective.title`} />
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
                  onChange={(e) =>
                    setReportFormValues({
                      ...reportFormValues,
                      type: e.target.value,
                    })
                  }
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
                  onChange={(e) =>
                    setReportFormValues({
                      ...reportFormValues,
                      value: e.target.value,
                    })
                  }
                  id={`field.name`}
                />
              </Column>

              <Column lg={16}>
                <br />
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
          <br />
        </Column>
        <Column lg={16}>
          {tData && (
            <div>
              <Grid>
                <Column lg={16} md={16} sm={16}>
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
                              onClick={() => setSelected(row.nceNumber)}
                              labelText=""
                              id={row.id}
                            />
                          </TableCell>
                          <TableCell key={row.key + "date"}>
                            {new Date(row.reportDate).toDateString()}
                          </TableCell>
                          <TableCell key={row.key + "1"}>
                            {row.nceNumber}
                          </TableCell>
                          <TableCell key={row.key + "2"}>
                            {
                              tData.reportingUnits.find(
                                (obj) =>
                                  parseInt(obj.id) === row.reportingUnitId,
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
                  {data.nceNumber}
                </div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.field.date" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>{data.dateOfEvent}</div>
              </Column>

              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.view.nce.severity" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {
                    data.severityConsequencesList.find(
                      (obj) => obj.id === data.consequences,
                    )?.value
                  }
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
                    data.reportingUnits.find(
                      (obj) => obj.id == data.reportingUnit,
                    )?.value
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
                  {data.labOrderNumber}
                </div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.field.reporting.data" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>{data.reportDate}</div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.label.prescibernamesite" />
                  </span>
                </div>
                <div
                  style={{ marginBottom: "10px" }}
                >{`${data.prescriberName}-${data.site}`}</div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.view.event.description" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.description ?? ""}
                </div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.label.suspected.cause.nce" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.suspectedCauses ?? ""}
                </div>
              </Column>
              <Column lg={1}></Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.label.proposed.action" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.proposedAction ?? ""}
                </div>
              </Column>

              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.lab.componenet.nce" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.labComponentList.find(
                    (obj) => obj.id === data.laboratoryComponent,
                  )?.value ?? ""}
                </div>
              </Column>

              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.view.nce.category" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.nceCategories.find((obj) => obj.id === data.nceCategory)
                    ?.name ?? ""}
                </div>
              </Column>

              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.view.nce.type" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.nceTypes.find((obj) => obj.id === data.nceType)?.name ??
                    ""}
                </div>
              </Column>
              <Column lg={16}></Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.corrective.action" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.correctiveAction ?? ""}
                </div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.corrective.control" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.controlAction ?? ""}
                </div>
              </Column>
              <Column lg={3} style={{ marginBottom: "20px" }}>
                <div style={{ marginBottom: "10px" }}>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.corrective.comment" />
                  </span>
                </div>
                <div style={{ marginBottom: "10px" }}>
                  {data.comments ?? ""}
                </div>
              </Column>

              <Column
                lg={16}
                style={{
                  marginTop: "20px",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    flexDirection: "row",
                    margin: "1px",
                  }}
                >
                  {formData?.discussionDate?.split(",").map((d, index) => {
                    return (
                      <p
                        key={index}
                        style={{ fontSize: "12px", marginBottom: "5px" }}
                      >
                        {index > 0 && ","} {d.trim()}
                      </p>
                    );
                  })}
                </div>

                <CustomDatePicker
                  key="tdiscussionDate"
                  id={"tdiscussionDate"}
                  data-testid="start-date"
                  labelText={
                    <FormattedMessage id="nonconform.date.discussion.nce" />
                  }
                  autofillDate={true}
                  value={tdiscussionDate}
                  onChange={handleDiscussionDateChange}
                  style={{ marginBottom: "5px" }}
                />
                <button
                  disabled={!tdiscussionDate}
                  onClick={handleAddDiscussionDate}
                  style={{ margin: "5px 0" }}
                >
                  <FormattedMessage id="nonconform.add.new.date" />
                </button>
              </Column>

              <Column lg={16}>
                {" "}
                <br></br>
              </Column>
              <Column lg={4}>
                {" "}
                <h5>
                  <FormattedMessage id="banner.menu.nonconformity.correctiveActions" />
                </h5>
              </Column>
              <Column lg={3}>
                <h5>
                  <FormattedMessage id="nonconform.person.responsible" />{" "}
                </h5>{" "}
              </Column>
              <Column lg={3}>
                {" "}
                <h5>
                  <FormattedMessage id="nonconform.date.completed" />{" "}
                </h5>
              </Column>
              <Column lg={3}>
                {" "}
                <h5>
                  <FormattedMessage id="nonconform.corrective.actionType" />{" "}
                </h5>
              </Column>
              <Column lg={3}>
                {" "}
                <h5>
                  <FormattedMessage id="nonconform.nce.turnaround.time" />{" "}
                </h5>
              </Column>
              <Column lg={16}>
                {" "}
                <br></br>
              </Column>
              <Column lg={4}>
                <TextArea
                  labelText=""
                  value={formData.actionLog[`correctiveAction`] ?? ""}
                  onChange={handleCorrectiveActionChange}
                  rows={1}
                  id="text-area-corrective"
                />
              </Column>

              <Column lg={3}>
                <TextArea
                  labelText=""
                  value={formData.actionLog[`personResponsible`] ?? ""}
                  onChange={handlePersonResponsibleChange}
                  rows={1}
                  id="text-area-person"
                />
              </Column>

              <Column lg={3}>
                <CustomDatePicker
                  key="dateCompleted"
                  id={"dateCompleted"}
                  labelText=""
                  autofillDate={true}
                  value={formData[`dateCompleted`] ?? undefined}
                  onChange={(e) => {
                    setFormData({
                      ...formData,
                      dateCompleted: e,
                    });
                  }}
                  style={{ marginTop: "5px" }}
                />
              </Column>

              <Column lg={3}>
                <Checkbox
                  checked={formData.actionLog.actionType
                    ?.split(",")
                    .includes("1")}
                  labelText={
                    <FormattedMessage id="banner.menu.nonconformity.correctiveActions" />
                  }
                  onClick={() => handleActionTypeChange("1")}
                  id="correctiveAction"
                  data-testid="nce-action-checkbox"
                />

                <Checkbox
                  labelText={
                    <FormattedMessage id="nonconform.nce.preventive.action" />
                  }
                  onClick={() => handleActionTypeChange("2")}
                  checked={formData.actionLog.actionType
                    ?.split(",")
                    .includes("2")}
                  id="preventiveAction"
                />

                <Checkbox
                  labelText={
                    <FormattedMessage id="nonconform.nce.concurrent.control.action" />
                  }
                  onClick={() => handleActionTypeChange("3")}
                  checked={formData.actionLog.actionType
                    ?.split(",")
                    .includes("3")}
                  id="concurrent Control Action"
                />
              </Column>
              <Column lg={3}>
                {formData[`dateCompleted`] && (
                  <div>
                    <div>
                      {getDifferenceInDays(
                        data.reportDate,
                        formData[`dateCompleted`],
                      )}
                    </div>
                  </div>
                )}
              </Column>
              {data.actionLog?.map((log, index) => (
                <>
                  <Column lg={16}>
                    {" "}
                    <br></br>
                  </Column>
                  <Column lg={4}>
                    <TextArea
                      labelText=""
                      value={log[`correctiveAction`] ?? ""}
                      disabled
                      rows={1}
                      id="text-area-corrective"
                    />
                  </Column>

                  <Column lg={3}>
                    <TextArea
                      labelText=""
                      value={log[`personResponsible`] ?? ""}
                      disabled
                      rows={1}
                      id="text-area-person"
                    />
                  </Column>

                  <Column lg={3}>
                    <CustomDatePicker
                      key="dateCompleted"
                      id={"dateCompleted"}
                      labelText=""
                      autofillDate={true}
                      value={data[`dateCompleted`] ?? undefined}
                      onChange={(e) => {}}
                      disabled
                      style={{ marginTop: "5px" }}
                    />
                  </Column>

                  <Column lg={3}>
                    <Checkbox
                      checked={log.actionType?.split(",").includes("1")}
                      labelText={
                        <FormattedMessage id="banner.menu.nonconformity.correctiveActions" />
                      }
                      disabled
                      id="correctiveAction"
                    />

                    <Checkbox
                      labelText={
                        <FormattedMessage id="nonconform.nce.preventive.action" />
                      }
                      disabled
                      checked={log.actionType?.split(",").includes("2")}
                      id="preventiveAction"
                    />

                    <Checkbox
                      labelText={
                        <FormattedMessage id="nonconform.nce.concurrent.control.action" />
                      }
                      disabled
                      checked={log.actionType?.split(",").includes("3")}
                      id="concurrent Control Action"
                    />
                  </Column>
                  <Column lg={3}>
                    <div>
                      <div>{log[`turnAroundTime`] ?? ""}</div>
                    </div>
                  </Column>
                </>
              ))}
              <Column lg={16}>
                <br></br>
                <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                  <h4>
                    {" "}
                    <FormattedMessage id="nonconform.nce.resolution" />
                  </h4>
                </span>
                <br></br>
              </Column>

              <Column lg={16}>
                <div>
                  <span style={{ color: "#3366B3", fontWeight: "bold" }}>
                    <FormattedMessage id="nonconform.nce.resolution.description" />
                  </span>

                  <div
                    style={{
                      display: "flex",
                      flexDirection: "row",
                      alignItems: "flex-start",
                      marginTop: "10px",
                    }}
                    data-testid="nce-resolution-radio"
                  >
                    <RadioButton
                      checked={submit === true}
                      labelText={<FormattedMessage id="yes.option" />}
                      id={`yes.option`}
                      onChange={() => setSubmit(true)}
                    ></RadioButton>
                    <RadioButton
                      labelText={<FormattedMessage id="no.option" />}
                      id={`no.option`}
                      checked={submit === false}
                      onChange={() => setSubmit(false)}
                    ></RadioButton>
                  </div>
                </div>
              </Column>
              <Column lg={8}>
                <CustomDatePicker
                  key="dateCompleted-0"
                  id="dateCompleted-0"
                  labelText={
                    <FormattedMessage id="nonconform.date.completed" />
                  }
                  autofillDate={true}
                  value={data[`dateCompleted`] ?? undefined}
                  onChange={(e) => {
                    setData({
                      ...data,
                      dateCompleted: e,
                    });
                  }}
                  style={{ marginBottom: "5px" }}
                />
              </Column>
              <Column lg={16}>
                {" "}
                <br></br>
              </Column>

              <Column lg={16}>
                {!!reportFormValues.error && (
                  <div style={{ color: "#c62828", margin: 4 }}>
                    {reportFormValues.error}
                  </div>
                )}

                <Button
                  type="button"
                  disabled={!submit}
                  onClick={handleNCEFormSubmit}
                  data-testid="nce-submit-button"
                >
                  <FormattedMessage id="label.button.submit" />
                </Button>
              </Column>
            </Grid>
          )}
        </Column>
      </Grid>
    </div>
  );
};
