import React, { useState, useContext, useEffect, useRef } from "react";
import { Field, Formik } from "formik";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
  Column,
  Form,
  Grid,
  InlineNotification,
  Pagination,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import DataTable from "react-data-table-component";
import { FormattedMessage, useIntl } from "react-intl";
import ValidationSearchFormValues from "../formModel/innitialValues/ValidationSearchFormValues";
import { NotificationKinds } from "../common/CustomNotification";
import { postToOpenElisServerFullResponse } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { ConfigurationContext } from "../layout/Layout";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { jpSet } from "../utils/JsonPath";
import config from "../../config.json";

export const buildAnalyzerResultsRedirectUrl = (queryMode, queryValue) => {
  if (!queryValue) {
    return "/AnalyzerResults";
  }

  return queryMode === "id"
    ? `/AnalyzerResults?id=${queryValue}`
    : `/AnalyzerResults?type=${queryValue}`;
};

const AnalyserResults = (props) => {
  const componentMounted = useRef(false);

  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Split results into patient rows and QC control rows. The bridge tags
  // QC rows with meta.tag[QC] → OE sets isControl=true on import.
  const allResults = props.results?.resultList ?? [];
  const patientResults = allResults.filter((r) => !r.isControl);
  const qcResults = allResults.filter((r) => r.isControl);
  const hasQcFailures = qcResults.some(
    (r) =>
      r.result && (/failed/i.test(r.result) || /\binvalid\b/i.test(r.result)),
  );

  const columns = [
    {
      id: "sampleInfo",
      name: intl.formatMessage({ id: "column.name.sampleInfo" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      selector: (row) => row.accessionNumber,
      sortable: true,
      width: "16rem",
    },
    {
      id: "testName",
      name: intl.formatMessage({ id: "column.name.testName" }),
      selector: (row) => row.testName,
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      sortable: true,
      width: "15rem",
    },
    {
      id: "result",
      name: intl.formatMessage({ id: "column.name.result" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "15rem",
    },
    {
      id: "completeDate",
      name: intl.formatMessage({ id: "column.name.testDate" }),
      selector: (row) => row.completeDate,
      sortable: true,
      width: "7rem",
    },
    {
      id: "save",
      name: intl.formatMessage({ id: "column.name.save" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "5rem",
    },
    {
      id: "retest",
      name: intl.formatMessage({ id: "column.name.retest" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "5rem",
    },
    {
      id: "ignore",
      name: intl.formatMessage({ id: "column.name.ignore" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "5rem",
    },
    {
      id: "notes",
      name: intl.formatMessage({ id: "column.name.notes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "15rem",
    },
  ];

  const handleSave = (values) => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    postToOpenElisServerFullResponse(
      "/rest/AnalyzerResults",
      JSON.stringify(props.results),
      handleResponse,
    );
  };
  const handleResponse = async (response) => {
    let message = intl.formatMessage({ id: "validation.save.error" });
    let kind = NotificationKinds.error;
    setIsSubmitting(false);
    if (response.status == 200) {
      message = intl.formatMessage({ id: "validation.save.success" });
      kind = NotificationKinds.success;
      window.location.href = buildAnalyzerResultsRedirectUrl(
        props.queryMode,
        props.queryValue || props.type,
      );
    } else {
      const detail = await response.text().catch(() => "");
      if (detail) {
        message = message + ": " + detail.substring(0, 200);
      }
    }
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: message,
    });
    setNotificationVisible(true);
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }
    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const handleChange = (e, rowId) => {
    const { name, id, value } = e.target;
    let form = props.results;
    jpSet(form, name, value);
  };

  const handleDatePickerChange = (date, rowId) => {
    console.debug("handleDatePickerChange:" + date);
    const d = new Date(date).toLocaleDateString("fr-FR");
    var form = props.results;
    jpSet(form, "resultList[" + rowId + "].sentDate_", d);
  };
  const handleCheckBox = (e, rowId) => {
    const { name, id, checked } = e.target;
    let form = props.results;
    jpSet(form, name, checked);
  };

  const handleAutomatedCheck = (checked, name) => {
    let form = props.results;
    jpSet(form, name, checked);
  };
  const validateResults = (e, rowId) => {
    handleChange(e, rowId);
  };

  const sampleGroupHasId = (id) => {
    return props.sampleGroup.some((item) => item.id === id);
  };

  const renderCell = (row, index, column, id) => {
    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
    switch (column.id) {
      case "sampleInfo":
        return (
          <>
            {sampleGroupHasId(row.id) && (
              <>
                <Button
                  onClick={async () => {
                    if ("clipboard" in navigator) {
                      return await navigator.clipboard.writeText(
                        row.accessionNumber,
                      );
                    } else {
                      return document.execCommand(
                        "copy",
                        true,
                        row.accessionNumber,
                      );
                    }
                  }}
                  kind="ghost"
                  iconDescription={intl.formatMessage({
                    id: "instructions.copy.labnum",
                  })}
                  hasIconOnly
                  renderIcon={Copy}
                />
                <div className="sampleInfo" data-testid="LabNo">
                  <br></br>
                  {formatLabNum
                    ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                    : row.accessionNumber}
                  <br></br>
                  <br></br>
                </div>
                {row.nonconforming && (
                  <picture>
                    <img
                      src={config.serverBaseUrl + "/images/nonconforming.gif"}
                      alt="nonconforming"
                      width="20"
                      height="15"
                    />
                  </picture>
                )}
              </>
            )}
          </>
        );
      case "testName":
        return (
          <div className="sampleInfo" data-testid="sampleInfo">
            {row.testName}
          </div>
        );

      case "save":
        return (
          <>
            <div>
              {sampleGroupHasId(row.id) && (
                <Field name="isAccepted">
                  {({ field }) => (
                    <Checkbox
                      id={"resultList" + row.id + ".isAccepted"}
                      name={"resultList[?(@.id == " + row.id + ")].isAccepted"}
                      labelText=""
                      value={true}
                      onChange={(e) => handleCheckBox(e, row.id)}
                    />
                  )}
                </Field>
              )}
            </div>
          </>
        );

      case "retest":
        return (
          <>
            {sampleGroupHasId(row.id) && (
              <Field name="isRejected">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isRejected"}
                    name={"resultList[?(@.id == " + row.id + ")].isRejected"}
                    labelText=""
                    value={true}
                    onChange={(e) => handleCheckBox(e, row.id)}
                  />
                )}
              </Field>
            )}
          </>
        );

      case "ignore":
        return (
          <>
            {sampleGroupHasId(row.id) && (
              <Field name="isDeleted">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isDeleted"}
                    name={"resultList[?(@.id == " + row.id + ")].isDeleted"}
                    labelText=""
                    value={true}
                    onChange={(e) => handleCheckBox(e, row.id)}
                  />
                )}
              </Field>
            )}
          </>
        );

      case "notes":
        return (
          <>
            <div className="note">
              <TextArea
                id={"resultList" + row.id + ".note"}
                name={"resultList[?(@.id == " + row.id + ")].note"}
                disabled={false}
                type="text"
                labelText=""
                rows={2}
                onChange={(e) => handleChange(e, row.id)}
              ></TextArea>
            </div>
          </>
        );

      case "result":
        switch (row.resultType) {
          case "M":
          case "C":
          case "D":
            return (
              <>
                {
                  row.dictionaryResults.find(
                    (result) => result.id == row.result,
                  )?.value
                }
              </>
            );
          default:
            if (row.readOnly) {
              return row.result;
            } else {
              return (
                <>
                  <div className="result">
                    <TextInput
                      id={"resultList" + row.id + ".result"}
                      name={"resultList[?(@.id == " + row.id + ")].result"}
                      disabled={false}
                      type="text"
                      value={row.result}
                      labelText=""
                      size="lg"
                      onChange={(e) => handleChange(e, row.id)}
                    ></TextInput>
                  </div>
                </>
              );
            }
        }

      default:
    }
    return row.result;
  };

  return (
    <>
      {patientResults.length === 0 && qcResults.length === 0 && (
        <div
          className="orderLegendBody"
          data-testid="analyzer-results-empty"
          style={{ marginTop: "20px" }}
        >
          <FormattedMessage id="validation.no.records.display" />
        </div>
      )}
      {hasQcFailures && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "analyzer.qc.batch.failure.title",
            defaultMessage: "QC Controls Failed",
          })}
          subtitle={intl.formatMessage({
            id: "analyzer.qc.batch.failure.subtitle",
            defaultMessage:
              "QC controls in this batch have failures. Review QC results below before accepting patient results.",
          })}
          lowContrast
          hideCloseButton
          style={{ marginTop: "16px", marginBottom: "8px" }}
        />
      )}
      {qcResults.length > 0 && (
        <Tag type={hasQcFailures ? "red" : "gray"} style={{ marginTop: "8px" }}>
          {qcResults.length}{" "}
          {intl.formatMessage({
            id: "analyzer.qc.controls.hidden",
            defaultMessage: "QC controls hidden from patient view",
          })}
        </Tag>
      )}
      {patientResults.length > 0 && (
        <Grid style={{ marginTop: "20px" }} className="gridBoundary">
          <Column lg={7} md={8} sm={2}>
            <picture>
              <img
                src={config.serverBaseUrl + "/images/nonconforming.gif"}
                alt="nonconforming"
                width="25" // Set your desired width
                height="20" // Set your desired height
              />
            </picture>
            <b>
              {" "}
              <FormattedMessage id="validation.label.nonconform" />
            </b>
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"saveallresults"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.accept.all" })}
              onChange={(e) => {
                const nomalResults = patientResults;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  if (!checkbox) return;
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
                });
              }}
            />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"retestalltests"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.reject.all" })}
              onChange={(e) => {
                const nomalResults = patientResults;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isRejected",
                  );
                  if (!checkbox) return;
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
                });
              }}
            />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"ignorealltests"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.ignore.all" })}
              onChange={(e) => {
                const nomalResults = patientResults;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isDeleted",
                  );
                  if (!checkbox) return;
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
                });
              }}
            />
          </Column>
        </Grid>
      )}
      <Formik
        initialValues={ValidationSearchFormValues}
        //validationSchema={}
        onSubmit
        onChange
      >
        {({ values, errors, touched, handleChange }) => (
          <Form onChange={handleChange}>
            <DataTable
              data={patientResults.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              columns={columns}
              isSortable
            ></DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={patientResults.length}
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
              itemRangeText={(min, max, total) =>
                intl.formatMessage(
                  { id: "pagination.item-range" },
                  { min: min, max: max, total: total },
                )
              }
              itemsPerPageText={intl.formatMessage({
                id: "pagination.items-per-page",
              })}
              itemText={(min, max) =>
                intl.formatMessage(
                  { id: "pagination.item" },
                  { min: min, max: max },
                )
              }
              pageNumberText={intl.formatMessage({
                id: "pagination.page-number",
              })}
              pageRangeText={(_current, total) =>
                intl.formatMessage(
                  { id: "pagination.page-range" },
                  { total: total },
                )
              }
              pageText={(page, pagesUnknown) =>
                intl.formatMessage(
                  { id: "pagination.page" },
                  { page: pagesUnknown ? "" : page },
                )
              }
            />

            <Button
              type="button"
              onClick={() => handleSave(values)}
              id="submit"
              style={{ marginTop: "16px" }}
              data-testid="Save-btn"
              disabled={isSubmitting}
            >
              <FormattedMessage id="label.button.save" />
            </Button>
            {isSubmitting && (
              <span data-testid="analyzer-results-save-in-progress" />
            )}
          </Form>
        )}
      </Formik>
      {qcResults.length > 0 && (
        <Accordion style={{ marginTop: "24px" }}>
          <AccordionItem
            title={intl.formatMessage(
              {
                id: "analyzer.qc.controls.section.title",
                defaultMessage: "QC Controls ({count})",
              },
              { count: qcResults.length },
            )}
          >
            <DataTable
              data={qcResults}
              columns={[
                {
                  id: "accessionNumber",
                  name: intl.formatMessage({
                    id: "column.name.sampleInfo",
                  }),
                  selector: (row) => row.accessionNumber,
                  width: "12rem",
                },
                {
                  id: "testName",
                  name: intl.formatMessage({ id: "column.name.testName" }),
                  selector: (row) => row.testName,
                  width: "12rem",
                },
                {
                  id: "result",
                  name: intl.formatMessage({ id: "column.name.result" }),
                  selector: (row) => row.result,
                  width: "20rem",
                },
              ]}
            />
          </AccordionItem>
        </Accordion>
      )}
    </>
  );
};

export default AnalyserResults;
