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
  Link as CarbonLink,
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
import { Link as RouterLink, useHistory, useLocation } from "react-router-dom";
import ValidationSearchFormValues from "../formModel/innitialValues/ValidationSearchFormValues";
import { NotificationKinds } from "../common/CustomNotification";
import { postToOpenElisServerFullResponse } from "../utils/Utils";
import {
  serverPageArrowsProps,
  serverPaginationProps,
} from "../utils/serverPaging";
import ServerPageArrows from "../common/ServerPageArrows";
import { NotificationContext } from "../layout/Layout";
import { ConfigurationContext } from "../layout/Layout";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { jpSet } from "../utils/JsonPath";
import config from "../../config.json";
import ResultAlertModal, {
  acknowledgementRefusal,
} from "../resultPage/ResultAlertModal";
import PlacementNotice from "./PlacementNotice";
import InstrumentReported from "./InstrumentReported";
import ResultParts, { groupTestParts } from "./ResultParts";
import DeliveryBundleModal from "./DeliveryBundleModal";
import RedirectControl from "./RedirectControl";

// Held for a decision the reviewer can make on the page, not for a mapping fix.
const REVIEWABLE_HOLDS = ["awaiting_specimen", "awaiting_placement"];

export const buildAnalyzerResultsRedirectUrl = (analyzerId) => {
  if (!analyzerId) {
    return "/AnalyzerResults";
  }

  return `/AnalyzerResults?id=${encodeURIComponent(analyzerId)}`;
};

export const buildHeldResultResolutionUrl = (row, analyzerId) => {
  const mappingIssues = [
    "unknown_analyzer_test",
    "test_mapping_not_ready",
    "unknown_analyzer_result_value",
    "result_mapping_not_ready",
    "invalid_result_mapping",
  ];
  if (
    !mappingIssues.includes(row.importIssueReason) ||
    !row.sourceProfileId ||
    !row.sourceProfileRevision ||
    !row.rawTestCode ||
    !analyzerId
  ) {
    return null;
  }

  const query = new URLSearchParams({
    returnTo: buildAnalyzerResultsRedirectUrl(analyzerId),
    focusTest: row.rawTestCode,
  });
  if (row.rawResultValue) {
    query.set("focusValue", row.rawResultValue);
  }
  return `/analyzers/${encodeURIComponent(analyzerId)}/mapping?${query.toString()}`;
};
const AnalyserResults = (props) => {
  const componentMounted = useRef(false);
  const draftEdits = useRef({});
  const history = useHistory();
  const location = useLocation();

  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [isSubmitting, setIsSubmitting] = useState(false);
  // OGC-1417: retyped values the server will not accept until the reviewer
  // acknowledges them as critical, or confirms them outside the valid range
  const [resultAlert, setResultAlert] = useState(null);
  const [bundleReceiptId, setBundleReceiptId] = useState(null);
  const [dismissingRunIds, setDismissingRunIds] = useState([]);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Edits restored after a mapping visit stay unsaved drafts, so the next
  // visit carries them again.
  useEffect(() => {
    draftEdits.current = Object.fromEntries(
      Object.entries(props.restoredEdits ?? {}).map(([id, fields]) => [
        id,
        { ...fields },
      ]),
    );
  }, [props.results, props.restoredEdits]);

  const rememberEdit = (rowId, field, value) => {
    const id = String(rowId);
    draftEdits.current[id] = { ...draftEdits.current[id], [field]: value };
  };

  const openMappingWithDraft = (event, resolutionUrl) => {
    if (
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    ) {
      return;
    }
    event.preventDefault();
    const edits = Object.fromEntries(
      Object.entries(draftEdits.current).map(([id, fields]) => [
        id,
        { ...fields },
      ]),
    );
    const worklistDraft = {
      analyzerId: String(props.analyzerId),
      page: Number(props.results?.paging?.currentPage) || 1,
      edits,
    };
    history.replace({
      pathname: location.pathname,
      search: location.search,
      state: { ...location.state, worklistDraft },
    });
    history.push(resolutionUrl, { worklistDraft });
  };

  const allResults = props.results?.resultList ?? [];
  const patientResults = allResults.filter((r) => !r.isControl);
  // Accept, retest and ignore are decided per test: its first row carries the
  // decision, and the rows on its components show beneath its main result.
  const { decisionHeadIds, partsByHeadId, partIds } =
    groupTestParts(patientResults);
  const tableRows = patientResults.filter((row) => !partIds.has(row.id));
  const arrows = serverPageArrowsProps({
    paging: props.results?.paging,
    onPageRequest: (pageNumber) => props.loadPage?.(pageNumber),
  });
  const heldPatientResults = patientResults.filter(
    (result) => result.importIssueReason,
  );
  const actionablePatientResults = patientResults.filter(
    (result) =>
      !result.importIssueReason ||
      REVIEWABLE_HOLDS.includes(result.importIssueReason),
  );
  // One tick saves a whole grouping, so "accept all" ticks only the groupings
  // whose every result has exactly one analysis waiting for it.
  const groupIsMatched = (grouping) =>
    actionablePatientResults
      .filter((result) => result.sampleGroupingNumber === grouping)
      .every((result) => result.placement?.state === "RESOLVED");
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
    if (response.status == 422) {
      const body = await response.json().catch(() => null);
      const refusal = acknowledgementRefusal(
        body ? { ...body, status: 422 } : null,
      );
      if (refusal) {
        setResultAlert(refusal);
        return;
      }
    }
    if (response.status == 200) {
      message = intl.formatMessage({ id: "validation.save.success" });
      kind = NotificationKinds.success;
      props.refreshResults?.(Number(props.results?.paging?.currentPage) || 1);
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

  const confirmResultAlert = () => {
    const pending = resultAlert;
    setResultAlert(null);
    if (!pending) {
      return;
    }
    const rows = props.results?.resultList || [];
    pending.alerts.forEach((alert) => {
      const row = rows.find(
        (candidate) => String(candidate.id) === String(alert.rowId),
      );
      if (!row) {
        return;
      }
      if (alert.kind === "CRITICAL") {
        row.criticalAcknowledged = true;
      } else {
        row.invalidResultConfirmed = true;
      }
    });
    handleSave();
  };

  const handleChange = (e, rowId) => {
    const { name, id, value } = e.target;
    let form = props.results;
    jpSet(form, name, value);
    const field = name.match(/\.(result|note)$/)?.[1];
    if (field) rememberEdit(rowId, field, value);
    if (field === "result") {
      const row = (form.resultList || []).find(
        (candidate) => String(candidate.id) === String(rowId),
      );
      if (row) {
        row.criticalAcknowledged = false;
        row.invalidResultConfirmed = false;
      }
    }
  };

  const handleDatePickerChange = (date, rowId) => {
    console.debug("handleDatePickerChange:" + date);
    const d = new Date(date).toLocaleDateString("fr-FR");
    var form = props.results;
    jpSet(form, "resultList[" + rowId + "].sentDate_", d);
  };
  const handleCheckBox = (e, rowId, fieldName) => {
    const row = (props.results.resultList || []).find(
      (result) => String(result.id) === String(rowId),
    );
    if (row) {
      row[fieldName] = e.target.checked;
      rememberEdit(rowId, fieldName, e.target.checked);
    }
  };

  // OGC-1145 FR-8 — set the choice directly on the row: the field is absent
  // from the loaded JSON (nulls are stripped), and a jsonpath set cannot
  // create a missing terminal property.
  const handleSampleTypeChoice = (e, rowId) => {
    const row = (props.results.resultList || []).find((r) => r.id === rowId);
    if (row) {
      row.typeOfSampleId = e.target.value;
      rememberEdit(rowId, "typeOfSampleId", e.target.value);
    }
  };

  const handleAnalysisChoice = (analysisId, rowId) => {
    const row = (props.results.resultList || []).find(
      (r) => String(r.id) === String(rowId),
    );
    if (row) {
      row.chosenAnalysisId = analysisId;
      rememberEdit(rowId, "chosenAnalysisId", analysisId);
    }
  };

  const handleRedirect = (field, value, rowId) => {
    const row = (props.results.resultList || []).find(
      (r) => String(r.id) === String(rowId),
    );
    if (row) {
      row[field] = value;
      rememberEdit(rowId, field, value);
    }
  };

  const handleAutomatedCheck = (checked, rowId, fieldName) => {
    const row = (props.results.resultList || []).find(
      (result) => String(result.id) === String(rowId),
    );
    if (row) {
      row[fieldName] = checked;
      rememberEdit(rowId, fieldName, checked);
    }
  };
  const validateResults = (e, rowId) => {
    handleChange(e, rowId);
  };

  const sampleGroupHasId = (id) => {
    return props.sampleGroup.some((item) => item.id === id);
  };

  // The run produced no result: the reviewer records the failure on the order's
  // waiting test, which stays open for the repeat.
  const dismissFailedRun = (row) => {
    setDismissingRunIds((ids) => [...ids, row.id]);
    postToOpenElisServerFullResponse(
      `/rest/analyzer/results/${encodeURIComponent(row.id)}/failed-run`,
      JSON.stringify({}),
      // The response is undefined when the request never reached the server.
      async (response) => {
        setDismissingRunIds((ids) => ids.filter((id) => id !== row.id));
        const succeeded = response?.status == 200;
        let message = intl.formatMessage({
          id: succeeded
            ? "analyzer.results.failedRun.dismissed"
            : "analyzer.results.failedRun.error",
        });
        if (!succeeded) {
          const body = await response?.json?.().catch(() => null);
          if (body?.error) {
            message = message + ": " + body.error;
          }
        }
        addNotification({
          kind: succeeded ? NotificationKinds.success : NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message,
        });
        setNotificationVisible(true);
        if (succeeded) {
          props.refreshResults?.(
            Number(props.results?.paging?.currentPage) || 1,
          );
        }
      },
    );
  };

  const renderHeldResult = (row) => {
    const resolutionUrl = buildHeldResultResolutionUrl(row, props.analyzerId);
    return (
      <div data-testid={`held-analyzer-result-${row.id}`}>
        <Tag type="warm-gray" size="sm">
          <FormattedMessage id="analyzer.results.held.tag" />
        </Tag>
        <div>
          <strong>{row.rawResultValue || row.result}</strong>
        </div>
        <div>
          <FormattedMessage
            id="analyzer.results.held.code"
            values={{ code: row.rawTestCode || row.testName }}
          />
        </div>
        {resolutionUrl && (
          <CarbonLink
            as={RouterLink}
            to={resolutionUrl}
            onClick={(event) => openMappingWithDraft(event, resolutionUrl)}
          >
            <FormattedMessage id="analyzer.results.held.reviewMapping" />
          </CarbonLink>
        )}
        {row.instrumentNote && (
          <div data-testid={`instrument-note-${row.id}`}>
            <FormattedMessage
              id="analyzer.results.held.instrumentNote"
              values={{ note: row.instrumentNote }}
            />
          </div>
        )}
        {row.importIssueReason === "run_failed" && !row.isControl && (
          <Button
            kind="tertiary"
            size="sm"
            disabled={dismissingRunIds.includes(row.id)}
            onClick={() => dismissFailedRun(row)}
          >
            <FormattedMessage id="analyzer.results.failedRun.dismiss" />
          </Button>
        )}
        {row.importIssueReason === "qc_target_missing" && (
          <>
            <div>
              <FormattedMessage id="analyzer.results.held.qcTargetMissing" />
            </div>
            {row.testId && (
              <CarbonLink
                as={RouterLink}
                to={`/MasterListsPage/TestCatalogEditor/${encodeURIComponent(row.testId)}/qc-targets`}
              >
                <FormattedMessage id="analyzer.results.held.setQcTarget" />
              </CarbonLink>
            )}
          </>
        )}
      </div>
    );
  };

  const renderCell = (row, index, column, id) => {
    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
    const held = Boolean(row.importIssueReason);
    const awaitingReview = REVIEWABLE_HOLDS.includes(row.importIssueReason);
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
                  {row.instrumentSpecimenId &&
                    row.instrumentSpecimenId !== row.accessionNumber && (
                      <div data-testid="InstrumentSpecimenId">
                        <FormattedMessage
                          id="analyzer.placement.instrumentSpecimen"
                          values={{ id: row.instrumentSpecimenId }}
                        />
                      </div>
                    )}
                  <br></br>
                  <br></br>
                </div>
                {row.placement && (
                  <RedirectControl row={row} onChange={handleRedirect} />
                )}
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
            <PlacementNotice
              row={row}
              onChooseAnalysis={handleAnalysisChoice}
              onViewBundle={setBundleReceiptId}
            />
            {/* OGC-1145 FR-8 — specimen-ambiguous row: the reviewer picks the
                sample type; accepting without a choice keeps the row staged
                (awaiting specimen) instead of guessing. */}
            {row.sampleTypeOptions && row.sampleTypeOptions.length > 0 && (
              <Select
                id={"resultList" + row.id + ".typeOfSampleId"}
                name={"resultList[?(@.id == " + row.id + ")].typeOfSampleId"}
                labelText={intl.formatMessage({
                  id: "label.testCatalog.specimenType",
                })}
                aria-label={intl.formatMessage({
                  id: "label.testCatalog.specimenType",
                })}
                helperText={intl.formatMessage({
                  id: "notice.testCatalog.intake.awaitingSpecimen",
                })}
                defaultValue={row.typeOfSampleId || ""}
                onChange={(e) => handleSampleTypeChoice(e, row.id)}
              >
                <SelectItem value="" text="--" />
                {row.sampleTypeOptions.map((option) => (
                  <SelectItem
                    key={option.id}
                    value={option.id}
                    text={option.value}
                  />
                ))}
              </Select>
            )}
          </div>
        );

      case "save":
        if (held && !awaitingReview) {
          return null;
        }
        return (
          <>
            <div>
              {decisionHeadIds.has(row.id) && (
                <Field name="isAccepted">
                  {({ field }) => (
                    <Checkbox
                      id={"resultList" + row.id + ".isAccepted"}
                      name={"resultList[?(@.id == " + row.id + ")].isAccepted"}
                      labelText=""
                      value={true}
                      defaultChecked={Boolean(row.isAccepted)}
                      onChange={(e) => handleCheckBox(e, row.id, "isAccepted")}
                    />
                  )}
                </Field>
              )}
            </div>
          </>
        );

      case "retest":
        if (held) {
          return null;
        }
        return (
          <>
            {decisionHeadIds.has(row.id) && (
              <Field name="isRejected">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isRejected"}
                    name={"resultList[?(@.id == " + row.id + ")].isRejected"}
                    labelText=""
                    value={true}
                    defaultChecked={Boolean(row.isRejected)}
                    onChange={(e) => handleCheckBox(e, row.id, "isRejected")}
                  />
                )}
              </Field>
            )}
          </>
        );

      case "ignore":
        if (held) {
          return null;
        }
        return (
          <>
            {decisionHeadIds.has(row.id) && (
              <Field name="isDeleted">
                {({ field }) => (
                  <Checkbox
                    id={"resultList" + row.id + ".isDeleted"}
                    name={"resultList[?(@.id == " + row.id + ")].isDeleted"}
                    labelText=""
                    value={true}
                    defaultChecked={Boolean(row.isDeleted)}
                    onChange={(e) => handleCheckBox(e, row.id, "isDeleted")}
                  />
                )}
              </Field>
            )}
          </>
        );

      case "notes":
        if (held) {
          return null;
        }
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
                defaultValue={row.note || ""}
                onChange={(e) => handleChange(e, row.id)}
              ></TextArea>
            </div>
          </>
        );

      case "result":
        return (
          <>
            {renderResultValue(row, held && !awaitingReview)}
            <InstrumentReported row={row} />
            <ResultParts
              headId={row.id}
              parts={partsByHeadId.get(row.id) || []}
            />
          </>
        );

      default:
    }
    return row.result;
  };

  const renderResultValue = (row, heldForMapping) => {
    if (heldForMapping) {
      return renderHeldResult(row);
    }
    switch (row.testResultType) {
      case "M":
      case "C":
      case "D":
        return (
          <>
            {
              row.dictionaryResultList.find((result) => result.id == row.result)
                ?.displayValue
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
      {heldPatientResults.length > 0 && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage(
            { id: "analyzer.results.held.title" },
            { count: heldPatientResults.length },
          )}
          subtitle={intl.formatMessage({
            id: "analyzer.results.held.subtitle",
          })}
          lowContrast
          hideCloseButton
          style={{ marginTop: "16px", marginBottom: "8px" }}
        />
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
      {actionablePatientResults.length > 0 && (
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
                actionablePatientResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isAccepted",
                  );
                  if (!checkbox) return;
                  if (
                    e.target.checked &&
                    !groupIsMatched(result.sampleGroupingNumber)
                  ) {
                    return;
                  }
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(
                    e.target.checked,
                    result.id,
                    "isAccepted",
                  );
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
                actionablePatientResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isRejected",
                  );
                  if (!checkbox) return;
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(
                    e.target.checked,
                    result.id,
                    "isRejected",
                  );
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
                actionablePatientResults.forEach((result) => {
                  const checkbox = document.getElementById(
                    "resultList" + result.id + ".isDeleted",
                  );
                  if (!checkbox) return;
                  checkbox.checked = e.target.checked;
                  handleAutomatedCheck(
                    e.target.checked,
                    result.id,
                    "isDeleted",
                  );
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
            {arrows.show && <ServerPageArrows {...arrows} />}
            <DataTable
              data={tableRows}
              columns={columns}
              isSortable
            ></DataTable>
            <Pagination
              {...serverPaginationProps({
                paging: props.results?.paging,
                rowsOnPage: patientResults.length,
                pageSize: props.serverPageSize,
                onPageRequest: (pageNumber) => props.loadPage?.(pageNumber),
                intl,
              })}
            />

            {actionablePatientResults.length > 0 && (
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
            )}
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
                  cell: (row) =>
                    row.importIssueReason ? renderHeldResult(row) : row.result,
                  width: "20rem",
                },
              ]}
            />
          </AccordionItem>
        </Accordion>
      )}
      <ResultAlertModal
        open={Boolean(resultAlert)}
        alerts={resultAlert?.alerts || []}
        mode="save"
        customCriticalMessage={resultAlert?.customCriticalMessage}
        onConfirm={confirmResultAlert}
        onCorrect={() => setResultAlert(null)}
      />
      <DeliveryBundleModal
        receiptId={bundleReceiptId}
        onClose={() => setBundleReceiptId(null)}
      />
    </>
  );
};

export default AnalyserResults;
