import React, { useState, useContext, useEffect, useRef } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  InlineNotification,
  Modal,
  Pagination,
  Tag,
  TextArea,
  Toggle,
} from "@carbon/react";
import { Copy, Launch, WarningAltFilled } from "@carbon/icons-react";
import DataTable from "react-data-table-component";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationKinds } from "../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { ConfigurationContext } from "../layout/Layout";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
import { jpSet } from "../utils/JsonPath";
import config from "../../config.json";
import ESignatureButton, {
  SignatureMeaning,
} from "../esignature/ESignatureButton";
import {
  FILTERS,
  LANE_CLEAR,
  bulkOutcomeKey,
  bulkReleaseRequest,
  clearRows,
  countByFilter,
  filterTriaged,
  triageRows,
} from "./validationTriage";
import ValidationReviewPanel from "./ValidationReviewPanel";

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
  const [qcAckChecked, setQcAckChecked] = useState(false);
  const [qcJustification, setQcJustification] = useState("");
  const [activeFilter, setActiveFilter] = useState("all");
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkBusy, setBulkBusy] = useState(false);
  const [expandedRowIds, setExpandedRowIds] = useState([]);
  // OGC-1030 (FR-A4): auto-validated rows live behind a toggle, read-only.
  const [includeAutoValidated, setIncludeAutoValidated] = useState(false);
  const [autoValidatedRows, setAutoValidatedRows] = useState(null);

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

  const triaged = triageRows(props.results?.resultList);
  const filterCounts = countByFilter(triaged);
  const visibleRows = filterTriaged(triaged, activeFilter).map(
    (item) => item.row,
  );
  const triageByRowId = new Map(triaged.map((item) => [item.row.id, item]));
  const clearLaneCount = triaged.filter(
    (item) => item.lane === LANE_CLEAR,
  ).length;
  const clearLaneRows = clearRows(triaged);
  // OGC-1029 (FR-B4): the whole bulk capability is an admin switch.
  const bulkAllowed =
    configurationProperties?.ALLOW_BULK_RELEASE_CLEAR === "true";

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
      id: "checkBeforeRelease",
      name: intl.formatMessage({ id: "label.validation.checkBeforeRelease" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "14rem",
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

  /**
   * OGC-1030 (FR-J1) — another validator acted on the row since this page
   * loaded: say who and when, then reload so nobody works from a stale queue.
   */
  const handleStale = (response) => {
    addNotification({
      kind: NotificationKinds.warning,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "label.validation.review.error.stale" },
        {
          who: response?.modifiedBy || "",
          when: response?.modifiedAt || "",
        },
      ),
    });
    setNotificationVisible(true);
    window.location.assign("/validation" + props.params);
  };

  /**
   * OGC-1028 — a per-row action (release / modify / retest / reject) succeeded:
   * reload the queue so the row's new state is served fresh.
   */
  const handleRowActionDone = (outcome) => {
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: `label.validation.review.success.${outcome}`,
      }),
    });
    setNotificationVisible(true);
    window.location.assign("/validation" + props.params);
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

  /**
   * OGC-1030 (FR-A4) — the accession's auto-validated results, fetched only when
   * the validator asks for them; an accession search is the only scope served.
   */
  const accessionSearch = new URLSearchParams(
    (props.params || "").replace(/^\?/, ""),
  );
  const autoValidatedAccession =
    accessionSearch.get("type") === "order"
      ? props.results?.accessionNumber ||
        accessionSearch.get("accessionNumber") ||
        ""
      : "";
  useEffect(() => {
    if (!includeAutoValidated || !autoValidatedAccession) {
      return;
    }
    getFromOpenElisServer(
      `/rest/AccessionValidation/auto-validated?accessionNumber=${encodeURIComponent(
        autoValidatedAccession,
      )}`,
      (rows) => setAutoValidatedRows(Array.isArray(rows) ? rows : []),
    );
  }, [includeAutoValidated, autoValidatedAccession]);

  /**
   * OGC-1028 — the review panel's composer is the single note input for a row.
   * It writes the note, its visibility and the Validation context onto the row
   * so the bulk release carries it too.
   */
  const handleRowNoteChange = (rowId, note, noteVisibility) => {
    let form = props.results;
    jpSet(form, "resultList[" + rowId + "].note", note);
    jpSet(form, "resultList[" + rowId + "].noteVisibility", noteVisibility);
    jpSet(form, "resultList[" + rowId + "].noteContext", "VALIDATION");
  };

  const toggleExpanded = (rowId) => {
    setExpandedRowIds((ids) =>
      ids.includes(rowId) ? ids.filter((id) => id !== rowId) : [...ids, rowId],
    );
  };

  /**
   * OGC-1029 (FR-B2) — the guarded bulk release: only the Clear lane is sent,
   * and the server re-derives the lane before releasing anything. Skipped rows
   * (no longer clear) are reported, never released.
   */
  const handleReleaseAllClear = () => {
    if (bulkBusy) {
      return;
    }
    setBulkBusy(true);
    postToOpenElisServerJsonResponse(
      "/rest/AccessionValidation/release-clear",
      JSON.stringify(
        bulkReleaseRequest(props.results, props.params, clearLaneRows),
      ),
      (response) => {
        setBulkBusy(false);
        const released = response?.released?.length || 0;
        const skipped = response?.skipped?.length || 0;
        if (response?.error || released === 0) {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: bulkOutcomeKey(response) }),
          });
          setNotificationVisible(true);
          return;
        }
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: skipped
            ? intl.formatMessage(
                { id: "label.validation.bulk.partial" },
                { released, skipped },
              )
            : intl.formatMessage(
                { id: "label.validation.bulk.success" },
                { count: released },
              ),
        });
        setNotificationVisible(true);
        setBulkOpen(false);
        window.location.assign("/validation" + props.params);
      },
    );
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
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "flex-start",
              }}
            >
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
              <Button
                kind="ghost"
                hasIconOnly
                renderIcon={Launch}
                iconDescription={intl.formatMessage({
                  id: "order.label.modify",
                })}
                tooltipPosition="right"
                tooltipAlignment="center"
                href={`/ModifyOrder?accessionNumber=${encodeURIComponent(
                  row.accessionNumber,
                )}`}
                target="_blank"
                rel="noopener noreferrer"
                as="a"
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

      case "checkBeforeRelease": {
        const triage = triageByRowId.get(row.id);
        if (!triage) {
          return null;
        }
        const needsReview = triage.lane !== LANE_CLEAR;
        if (triage.chips.length === 0 && !needsReview) {
          return null;
        }
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "flex-start",
              gap: "0.25rem",
            }}
          >
            {triage.chips.length > 0 && (
              <div
                data-testid={`check-before-release-${row.id}`}
                style={{ display: "flex", flexWrap: "wrap", gap: "0.25rem" }}
              >
                {triage.chips.map((chip) => (
                  <Tag
                    key={chip}
                    type="red"
                    size="sm"
                    renderIcon={WarningAltFilled}
                  >
                    <strong>
                      {intl.formatMessage({
                        id: `label.validation.signal.${chip}`,
                      })}
                    </strong>
                  </Tag>
                ))}
              </div>
            )}
            {needsReview && (
              <Button
                kind="ghost"
                size="sm"
                data-testid={`review-row-${row.id}`}
                aria-expanded={expandedRowIds.includes(row.id)}
                onClick={() => toggleExpanded(row.id)}
              >
                <FormattedMessage id="label.validation.review.action.review" />
              </Button>
            )}
          </div>
        );
      }

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
          case "C": {
            const labelFor = (dictId) =>
              row.dictionaryResults?.find((result) => result.id == dictId)
                ?.value || dictId;
            let groups;
            try {
              groups = JSON.parse(row.multiSelectResultValues || "{}");
            } catch {
              groups = {};
            }
            const lines = Object.keys(groups)
              .sort((a, b) => Number(a) - Number(b))
              .map((k) =>
                groups[k].split(",").filter(Boolean).map(labelFor).join(", "),
              )
              .filter(Boolean);
            return (
              <div style={{ display: "flex", flexDirection: "column" }}>
                {lines.map((line, index) => (
                  <div key={index}>
                    {row.resultType === "C" ? `[ ${line} ]` : line}
                  </div>
                ))}
              </div>
            );
          }
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
          <Column
            lg={9}
            md={8}
            sm={4}
            style={{
              display: "flex",
              justifyContent: "flex-end",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            {bulkAllowed ? (
              <Button
                size="sm"
                data-testid="release-all-clear"
                disabled={clearLaneRows.length === 0 || bulkBusy}
                onClick={() => setBulkOpen(true)}
              >
                {intl.formatMessage(
                  { id: "label.validation.bulk.releaseAllClear" },
                  { count: clearLaneRows.length },
                )}
              </Button>
            ) : (
              <span
                className="cds--label"
                data-testid="release-all-clear-disabled"
              >
                {intl.formatMessage({ id: "label.validation.bulk.disabled" })}
              </span>
            )}
          </Column>
        </Grid>
      )}
      {bulkOpen && (
        <Modal
          open
          passiveModal
          size="lg"
          modalHeading={intl.formatMessage({
            id: "label.validation.bulk.heading",
          })}
          onRequestClose={() => setBulkOpen(false)}
        >
          <div data-testid="release-all-clear-modal">
            <p>
              {intl.formatMessage(
                { id: "label.validation.bulk.body" },
                { count: clearLaneRows.length },
              )}
            </p>
            <table
              className="cds--data-table cds--data-table--sm"
              style={{ width: "100%", margin: "1rem 0" }}
              data-testid="release-all-clear-list"
            >
              <thead>
                <tr>
                  <th>
                    <FormattedMessage id="label.validation.bulk.column.accession" />
                  </th>
                  <th>
                    <FormattedMessage id="column.name.testName" />
                  </th>
                  <th>
                    <FormattedMessage id="column.name.result" />
                  </th>
                  <th>
                    <FormattedMessage id="column.name.normalRange" />
                  </th>
                </tr>
              </thead>
              <tbody>
                {clearLaneRows.map((row) => (
                  <tr
                    key={row.id}
                    data-testid={`release-all-clear-row-${row.id}`}
                  >
                    <td>
                      {configurationProperties.AccessionFormat === "ALPHANUM"
                        ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                        : row.accessionNumber}
                    </td>
                    <td>{row.testName}</td>
                    <td>{row.result}</td>
                    <td>{row.normalRange}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {qcAckRequired && !qcAckSatisfied && (
              <p data-testid="release-all-clear-qc-hint">
                <FormattedMessage id="label.validation.review.release.qcAckFirst" />
              </p>
            )}
            <div style={{ display: "flex", gap: "0.5rem", marginTop: "1rem" }}>
              <span data-testid="release-all-clear-sign">
                <ESignatureButton
                  meaning={SignatureMeaning.VALIDATED_AND_RELEASED}
                  context={intl.formatMessage(
                    {
                      id: "esig.context.releaseAllClear",
                      defaultMessage:
                        "Release {count} clear result(s) for {accession}",
                    },
                    {
                      count: clearLaneRows.length,
                      accession:
                        props.results?.accessionNumber ||
                        clearLaneRows[0]?.accessionNumber ||
                        "",
                    },
                  )}
                  recordType="VALIDATION_BATCH"
                  recordId={Number(clearLaneRows[0]?.analysisId) || 0}
                  onBeforeSign={handleBeforeSign}
                  onSign={handleReleaseAllClear}
                  disabled={
                    bulkBusy || !qcAckSatisfied || clearLaneRows.length === 0
                  }
                  size="sm"
                >
                  {intl.formatMessage(
                    { id: "label.validation.bulk.releaseAllClear" },
                    { count: clearLaneRows.length },
                  )}
                </ESignatureButton>
              </span>
              <Button
                kind="secondary"
                size="sm"
                onClick={() => setBulkOpen(false)}
                data-testid="release-all-clear-cancel"
              >
                <FormattedMessage id="label.validation.review.action.cancel" />
              </Button>
            </div>
          </div>
        </Modal>
      )}
      {triaged.length > 0 && (
        <div
          data-testid="validation-triage-filters"
          role="group"
          aria-label={intl.formatMessage({
            id: "label.validation.triage.filterLabel",
          })}
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "0.5rem",
            alignItems: "center",
            margin: "1rem 0",
          }}
        >
          {FILTERS.map((filter) => (
            <Button
              key={filter}
              size="sm"
              kind={activeFilter === filter ? "primary" : "tertiary"}
              aria-pressed={activeFilter === filter}
              data-testid={`triage-filter-${filter}`}
              onClick={() => {
                setActiveFilter(filter);
                setPage(1);
              }}
            >
              {intl.formatMessage({ id: `label.validation.filter.${filter}` })}{" "}
              ({filterCounts[filter]})
            </Button>
          ))}
          <span
            data-testid="validation-lane-summary"
            style={{ marginLeft: "auto", display: "flex", gap: "0.25rem" }}
          >
            <Tag type="green" size="sm">
              {intl.formatMessage({ id: "label.validation.lane.clear" })}:{" "}
              {clearLaneCount}
            </Tag>
            <Tag type="gray" size="sm">
              {intl.formatMessage({ id: "label.validation.lane.needsReview" })}:{" "}
              {triaged.length - clearLaneCount}
            </Tag>
          </span>
        </div>
      )}
      {triaged.length > 0 && autoValidatedAccession && (
        <div
          data-testid="auto-validated-toggle"
          style={{ margin: "0 0 0.5rem 0" }}
        >
          <Toggle
            id="include-auto-validated"
            size="sm"
            hideLabel
            labelText={intl.formatMessage({
              id: "label.validation.autoValidated.toggle",
            })}
            labelA={intl.formatMessage({
              id: "label.validation.autoValidated.toggle",
            })}
            labelB={intl.formatMessage({
              id: "label.validation.autoValidated.toggle",
            })}
            toggled={includeAutoValidated}
            onToggle={(checked) => setIncludeAutoValidated(checked)}
          />
        </div>
      )}
      <>
        <>
          <>
            <DataTable
              data={visibleRows.slice((page - 1) * pageSize, page * pageSize)}
              columns={columns}
              isSortable
              expandableRows
              expandableRowExpanded={(row) => expandedRowIds.includes(row.id)}
              onRowExpandToggled={(expanded, row) =>
                setExpandedRowIds((ids) =>
                  expanded
                    ? ids.includes(row.id)
                      ? ids
                      : [...ids, row.id]
                    : ids.filter((id) => id !== row.id),
                )
              }
              expandableRowsComponent={ValidationReviewPanel}
              expandableRowsComponentProps={{
                rows: props.results?.resultList || [],
                triageByRowId,
                configurationProperties,
                qcAck: {
                  required: qcAckRequired,
                  satisfied: qcAckSatisfied,
                  beforeSign: handleBeforeSign,
                },
                onActionDone: handleRowActionDone,
                onNoteChange: handleRowNoteChange,
                onStale: handleStale,
              }}
            ></DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={visibleRows.length}
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

            {includeAutoValidated && autoValidatedAccession && (
              <div
                data-testid="auto-validated-section"
                style={{ marginTop: "1rem" }}
              >
                <h5 style={{ marginBottom: "0.5rem" }}>
                  {intl.formatMessage(
                    { id: "label.validation.autoValidated.heading" },
                    { count: autoValidatedRows ? autoValidatedRows.length : 0 },
                  )}
                </h5>
                <span className="cds--label">
                  <FormattedMessage id="label.validation.autoValidated.hint" />
                </span>
                {autoValidatedRows && autoValidatedRows.length === 0 && (
                  <p data-testid="auto-validated-empty">
                    <FormattedMessage id="label.validation.autoValidated.empty" />
                  </p>
                )}
                {autoValidatedRows && autoValidatedRows.length > 0 && (
                  <table
                    className="cds--data-table cds--data-table--sm"
                    style={{ width: "100%" }}
                    data-testid="auto-validated-table"
                  >
                    <thead>
                      <tr>
                        <th>
                          <FormattedMessage id="label.validation.bulk.column.accession" />
                        </th>
                        <th>
                          <FormattedMessage id="column.name.testName" />
                        </th>
                        <th>
                          <FormattedMessage id="column.name.result" />
                        </th>
                        <th>
                          <FormattedMessage id="column.name.normalRange" />
                        </th>
                        <th>
                          <FormattedMessage id="label.validation.autoValidated.releasedOn" />
                        </th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {autoValidatedRows.map((row, index) => (
                        <tr
                          key={`${row.analysisId}-${index}`}
                          data-testid={`auto-validated-row-${row.analysisId}`}
                        >
                          <td>
                            {configurationProperties.AccessionFormat ===
                            "ALPHANUM"
                              ? convertAlphaNumLabNumForDisplay(
                                  row.accessionNumber,
                                )
                              : row.accessionNumber}
                          </td>
                          <td>{row.testName}</td>
                          <td>{row.result}</td>
                          <td>{row.normalRange}</td>
                          <td>{row.resultDate}</td>
                          <td>
                            <Tag size="sm" type="gray">
                              <FormattedMessage id="label.validation.autoValidated.tag" />
                            </Tag>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </>
        </>
      </>
    </>
  );
};

export default Validation;
