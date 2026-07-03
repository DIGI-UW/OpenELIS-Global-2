import React, { useState, useContext, useEffect, useRef } from "react";
import { Field, Formik } from "formik";
import {
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
import { postToOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { jpSet } from "../utils/JsonPath";
import config from "../../config.json";
import ESignatureButton, {
  SignatureMeaning,
} from "../esignature/ESignatureButton";

const Validation = (props) => {
  const componentMounted = useRef(false);

  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const parseDisplayDate = (dateStr) => {
    if (!dateStr) return NaN;
    const isFrench = configurationProperties?.DEFAULT_DATE_LOCALE === "fr-FR";
    const [datePart, timePart] = dateStr.trim().split(/\s+/);
    const dateParts = datePart ? datePart.split("/") : [];
    if (dateParts.length !== 3) return NaN;
    const [a, b, year] = dateParts.map(Number);
    const month = isFrench ? b : a;
    const day = isFrench ? a : b;
    const [hours, minutes] = timePart
      ? timePart.split(":").map(Number)
      : [0, 0];
    return new Date(year, month - 1, day, hours || 0, minutes || 0).getTime();
  };

  const HOLDING_STATUS_STYLE = {
    "on-time": { outline: "2px solid #24a148", borderRadius: "4px" },
    approaching: { outline: "2px solid #8d8d8d", borderRadius: "4px" },
    imminent: { outline: "2px solid #ee538b", borderRadius: "4px" },
    exceeded: { outline: "2px solid #FF6B00", borderRadius: "4px" },
  };

  const getHoldingStatus = (row) => {
    if (!row.timeHolding || !row.collectionDate) return null;
    const holdingMinutes = parseFloat(row.timeHolding);
    if (!holdingMinutes || holdingMinutes <= 0) return null;
    const resultMs = parseDisplayDate(row.resultDate);
    const collectionMs = parseDisplayDate(row.collectionDate);
    if (isNaN(resultMs) || isNaN(collectionMs)) return null;
    const holdingMs = holdingMinutes * 60 * 1000;
    const elapsedMs = resultMs - collectionMs;
    const fraction = elapsedMs / holdingMs;
    if (fraction > 1) return "exceeded";
    if (fraction > 0.75) return "imminent";
    if (fraction > 0.5) return "approaching";
    return "on-time";
  };

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [qcAckChecked, setQcAckChecked] = useState(false);
  const [qcJustification, setQcJustification] = useState("");

  // S-08 FR-04: failed QC samples in the current batch, populated by the GET.
  // The acknowledgment is only required when there's a release pending — if the
  // validation queue is empty (e.g. the client analysis was already released in a
  // prior session), the failure record still exists on the result but there's
  // nothing to gate, so the panel stays hidden.
  const qcFailures = props.results?.qcFailureList || [];
  const hasPendingResults = (props.results?.resultList?.length ?? 0) > 0;
  const qcAckRequired = qcFailures.length > 0 && hasPendingResults;
  const qcAckSatisfied =
    !qcAckRequired || (qcAckChecked && qcJustification.trim().length > 0);
  const qcBatchAccession =
    qcFailures[0]?.accessionNumber ||
    props.results?.accessionNumber ||
    props.results?.resultList?.[0]?.accessionNumber;

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

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
      id: "normalRange",
      name: intl.formatMessage({ id: "column.name.normalRange" }),
      selector: (row) => row.normalRange,
      sortable: true,
      width: "8rem",
    },
    {
      id: "result",
      name: intl.formatMessage({ id: "column.name.result" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "uncertainty",
      name: intl.formatMessage({ id: "column.name.uncertainty" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "save",
      name: intl.formatMessage({ id: "column.name.save" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "retest",
      name: intl.formatMessage({ id: "column.name.retest" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    },
    {
      id: "notes",
      name: intl.formatMessage({ id: "column.name.notes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "15rem",
    },
    {
      id: "pastNotes",
      name: intl.formatMessage({ id: "column.name.pastNotes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "28rem",
    },
  ];

  const buildSignContext = () => {
    const results = (props.results && props.results.resultList) || [];
    const count = results.length;
    const accessions = [
      ...new Set(results.map((r) => r.accessionNumber).filter(Boolean)),
    ];
    if (accessions.length === 1) {
      return intl.formatMessage(
        {
          id: "esig.context.validateResults",
          defaultMessage:
            "Validate {count} result(s) for accession {accession}",
        },
        {
          count,
          accession:
            convertAlphaNumLabNumForDisplay(accessions[0]) || accessions[0],
        },
      );
    }
    return intl.formatMessage(
      {
        id: "esig.context.validateResultsMulti",
        defaultMessage:
          "Validate {count} result(s) across {accessionCount} accessions",
      },
      { count, accessionCount: accessions.length },
    );
  };

  const getFirstAnalysisId = () => {
    const results = (props.results && props.results.resultList) || [];
    for (const r of results) {
      if (r.analysisId) return Number(r.analysisId);
    }
    return 0;
  };

  const handleSave = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    postToOpenElisServer(
      "/rest/AccessionValidation",
      JSON.stringify(props.results),
      handleResponse,
    );
  };
  const handleResponse = (status) => {
    let message = intl.formatMessage({ id: "validation.save.error" });
    let kind = NotificationKinds.error;
    setIsSubmitting(false);
    if (status == 200) {
      message = intl.formatMessage({ id: "validation.save.success" });
      kind = NotificationKinds.success;
      window.location.href = "/validation" + props.params;
    }
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: message,
    });
    setNotificationVisible(true);
  };

  /**
   * Posts the QC failure acknowledgment for the current batch. Resolves on 2xx,
   * rejects otherwise. Called via ESignatureButton.onBeforeSign so the ack
   * persists before any E-Sign ceremony opens.
   */
  const postQcAcknowledgment = () => {
    if (!qcAckRequired) {
      return Promise.resolve();
    }
    return fetch(
      config.serverBaseUrl + "/rest/AccessionValidation/qc-acknowledgment",
      {
        credentials: "include",
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify({
          accessionNumber: qcBatchAccession,
          justification: qcJustification.trim(),
        }),
      },
    ).then((response) => {
      if (!response.ok) {
        throw new Error("QC acknowledgment failed: " + response.status);
      }
    });
  };

  const handleBeforeSign = async () => {
    try {
      await postQcAcknowledgment();
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.validation.qc.ackFailed" }),
      });
      setNotificationVisible(true);
      // Re-throw so ESignatureButton aborts the ceremony.
      throw error;
    }
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

  const renderCell = (row, index, column, id) => {
    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
    const fullTestName = row.testName;
    const splitIndex = fullTestName.lastIndexOf("(");
    const testName = fullTestName.substring(0, splitIndex);
    const sampleType = fullTestName.substring(splitIndex);
    switch (column.id) {
      case "sampleInfo":
        return (
          <>
            <div>
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
            </div>
            <div className="sampleInfo" data-testid="LabNo">
              <br></br>
              {formatLabNum
                ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                : row.accessionNumber}
              <br></br>
              {row.vectorPoolId && row.vectorPoolMemberCount > 0 ? (
                <Tag type="teal" size="sm" style={{ marginTop: 2 }}>
                  {intl.formatMessage(
                    {
                      id: "result.vectorPool.label",
                      defaultMessage: "Pool of {count} {animal}",
                    },
                    {
                      count: row.vectorPoolMemberCount,
                      animal: row.sampleType || "",
                    },
                  )}
                </Tag>
              ) : (
                <>
                  {row.patientName} <br></br>
                  {row.patientInfo}
                </>
              )}
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
        );
      case "testName": {
        const unitsOnly = row.units ? row.units.split(" (")[0].trim() : "";
        return (
          <div className="sampleInfo" data-testid="sampleInfo">
            <br></br>
            {testName}
            {unitsOnly && (
              <>
                <br></br>
                {unitsOnly}
              </>
            )}
            <br></br>
            {sampleType}
          </div>
        );
      }

      case "save":
        return (
          <>
            <div data-testid="Checkbox">
              <Field name="isAccepted">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isAccepted"}
                    name={"resultList[" + row.id + "].isAccepted"}
                    labelText=""
                    value={true}
                    onChange={(e) => handleCheckBox(e, row.id)}
                  />
                )}
              </Field>
            </div>
          </>
        );

      case "retest":
        return (
          <>
            <Field name="isRejected">
              {({ field }) => (
                <Checkbox
                  id={"resultList" + row.id + ".isRejected"}
                  name={"resultList[" + row.id + "].isRejected"}
                  labelText=""
                  value={true}
                  onChange={(e) => handleCheckBox(e, row.id)}
                />
              )}
            </Field>
          </>
        );

      case "notes":
        return (
          <>
            <div className="note">
              <TextArea
                id={"resultList" + row.id + ".note"}
                name={"resultList[" + row.id + "].note"}
                disabled={false}
                type="text"
                labelText=""
                rows={2}
                onChange={(e) => handleChange(e, row.id)}
              ></TextArea>
            </div>
          </>
        );

      case "pastNotes":
        return (
          <>
            <div className="note" style={{ whiteSpace: "pre-wrap" }}>
              {row.pastNotes?.replace(/<br\s*\/?>/gi, "\n")}
            </div>
          </>
        );

      case "uncertainty": {
        const uVal = row.expandedUncertainty;
        if (!uVal) return null;
        return (
          <span style={{ fontVariantNumeric: "tabular-nums" }}>
            <span
              style={{
                color: "var(--cds-text-secondary, #525252)",
                marginRight: "0.125rem",
              }}
            >
              {intl.formatMessage({ id: "results.uncertainty.value.prefix" })}
            </span>
            {uVal}
          </span>
        );
      }

      case "result": {
        const holdingStatus = getHoldingStatus(row);
        const holdingStyle = holdingStatus
          ? HOLDING_STATUS_STYLE[holdingStatus]
          : {};
        switch (row.resultType) {
          case "M":
          case "C":
          case "D":
            return (
              <div style={{ padding: "2px", ...holdingStyle }}>
                {
                  row.dictionaryResults.find(
                    (result) => result.id == row.result,
                  )?.value
                }
              </div>
            );
          default:
            return (
              <div style={{ padding: "2px", ...holdingStyle }}>
                {row.result}
              </div>
            );
        }
      }

      default:
    }
    return row.result;
  };

  return (
    <>
      {props.results?.resultList?.length > 0 && (
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
              id={"saveallnormal"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.accept.normal" })}
              onChange={(e) => {
                const nomalResults = props.results.resultList?.filter(
                  (result) => result.normal == true,
                );
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(e.target.checked, checkbox.name);
                });
              }}
            />
          </Column>
          <Column lg={3} md={2} sm={4}>
            <Checkbox
              id={"saveallresults"}
              name={"autochecks"}
              labelText={intl.formatMessage({ id: "validation.accept.all" })}
              onChange={(e) => {
                const nomalResults = props.results.resultList;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
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
                const nomalResults = props.results.resultList;
                nomalResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isRejected",
                  );
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
              data={
                props.results
                  ? props?.results?.resultList?.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )
                  : []
              }
              columns={columns}
              isSortable
            ></DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={
                props.results
                  ? props.results.resultList
                    ? props.results.resultList.length
                    : 0
                  : 0
              }
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

            {qcAckRequired && (
              <div
                style={{
                  marginTop: "16px",
                  padding: "16px",
                  borderLeft: "4px solid #f1c21b",
                  background: "#fcf4d6",
                }}
              >
                <InlineNotification
                  kind="warning"
                  hideCloseButton
                  lowContrast
                  title={intl.formatMessage(
                    { id: "label.validation.qc.banner.title" },
                    { count: qcFailures.length },
                  )}
                  subtitle={intl.formatMessage({
                    id: "label.validation.qc.banner.subtitle",
                  })}
                />
                <h5 style={{ marginTop: "16px", marginBottom: "8px" }}>
                  <FormattedMessage id="label.validation.qc.failedSamples" />
                </h5>
                <table
                  className="cds--data-table cds--data-table--sm"
                  style={{ marginBottom: "16px", width: "100%" }}
                >
                  <thead>
                    <tr>
                      <th>
                        <FormattedMessage id="column.qc.accession" />
                      </th>
                      <th>
                        <FormattedMessage id="column.qc.type" />
                      </th>
                      <th>
                        <FormattedMessage id="column.qc.test" />
                      </th>
                      <th>
                        <FormattedMessage id="column.qc.result" />
                      </th>
                      <th>
                        <FormattedMessage id="column.qc.detail" />
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {qcFailures.map((f) => (
                      <tr key={f.analysisId}>
                        <td>{f.accessionNumber}</td>
                        <td>
                          <Tag size="sm" type="warm-gray">
                            {f.qcType}
                          </Tag>
                        </td>
                        <td>{f.testName}</td>
                        <td>{f.resultValue}</td>
                        <td>{f.qcEvaluationDetail}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <h5 style={{ marginTop: "8px", marginBottom: "8px" }}>
                  <FormattedMessage id="label.validation.qc.ack.heading" />
                </h5>
                <Checkbox
                  id="qc-ack-checkbox"
                  labelText={intl.formatMessage({
                    id: "label.validation.qc.ack.checkbox",
                  })}
                  checked={qcAckChecked}
                  onChange={(_, { checked }) => setQcAckChecked(checked)}
                />
                <TextArea
                  id="qc-ack-justification"
                  labelText={intl.formatMessage({
                    id: "label.validation.qc.justification",
                  })}
                  placeholder={intl.formatMessage({
                    id: "placeholder.validation.qc.justification",
                  })}
                  value={qcJustification}
                  onChange={(e) => setQcJustification(e.target.value)}
                  maxLength={500}
                  rows={3}
                  style={{ marginTop: "8px" }}
                />
              </div>
            )}

            <ESignatureButton
              meaning={SignatureMeaning.VALIDATED_AND_RELEASED}
              context={buildSignContext()}
              recordType="VALIDATION_BATCH"
              recordId={getFirstAnalysisId()}
              onBeforeSign={handleBeforeSign}
              onSign={handleSave}
              disabled={isSubmitting || !qcAckSatisfied}
              style={{ marginTop: "16px" }}
            >
              <FormattedMessage id="label.button.save" />
            </ESignatureButton>
          </Form>
        )}
      </Formik>
    </>
  );
};

export default Validation;
