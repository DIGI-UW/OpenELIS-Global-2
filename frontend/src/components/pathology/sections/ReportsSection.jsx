import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  DataTable,
  FileUploader,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { toBase64 } from "../../utils/Utils";
import { focusFirstOf } from "../focus";
import { REPORT_TYPE_KEYS, currentGeneratedReport } from "../pathologySections";
import "../pathologyCaseView.scss";
import "../../caseView/caseView.scss";

const TYPE_TAGS = {
  DRAFT: "gray",
  FINAL: "green",
  EXTERNAL: "blue",
};

// Only a version that no longer stands is tagged with its state.
const STATE_KEYS = {
  SUPERSEDED: "pathology.report.state.SUPERSEDED",
  VOIDED: "pathology.report.state.VOIDED",
};

const STATE_TAGS = {
  SUPERSEDED: "cool-gray",
  VOIDED: "red",
};

const GENERATE_ID = "reportGenerate";
const GENERATE_REASON_ID = "reportGenerateReason";
const REGENERATE_CONFIRM_ID = "reportRegenerateConfirm";
const REGENERATE_TEXT_ID = "reportRegenerateText";
const UPLOAD_HINT_ID = "reportUploadHint";
const reportLinkId = (id) => `reportLink${id}`;
const withdrawId = (version) => `reportWithdraw${version}`;
const withdrawConfirmId = (version) => `reportWithdrawConfirm${version}`;
const withdrawTextId = (version) => `reportWithdrawText${version}`;

const isWithdrawable = (report) =>
  report.type === "EXTERNAL" && report.state === "CURRENT";

// Generate is held for one reason at a time, in this order. A send in flight
// holds it without a sentence: the answer is on its way.
const generateHeldKey = ({ busy, dirty, canGenerate }) => {
  if (busy) {
    return null;
  }
  if (dirty) {
    return "pathology.locked.saveBeforeGenerate";
  }
  if (!canGenerate) {
    return "pathology.error.reportNeedsFindings";
  }
  return null;
};

/**
 * FR-15: the report versions the case carries, newest first, each either
 * generated here or uploaded from outside. A version opens in a new tab at its
 * own address; nothing else is done to it from a row, save withdrawing an
 * uploaded file put on the wrong case.
 */
const ReportsSection = ({
  reports = [],
  canGenerate = false,
  dirty = false,
  busy = false,
  onGenerate,
  onUpload,
  onWithdraw,
  fileUrl,
  uploadError,
}) => {
  const intl = useIntl();
  const generateRef = useRef(null);
  const uploaderRef = useRef(null);
  const uploadRef = useRef(null);
  // The control a send started here hands focus back to once the send ends.
  const focusAfterSend = useRef(null);
  const wasBusy = useRef(busy);
  const [regenerateOpen, setRegenerateOpen] = useState(false);
  const [withdrawing, setWithdrawing] = useState(null);

  const rows = [...reports].sort((a, b) => b.version - a.version);
  // The version a new one supersedes. With none current (only superseded or
  // voided ones), a generate goes without asking, as a first one does.
  const newestGenerated = currentGeneratedReport(reports);
  const actionsShown = rows.some(isWithdrawable);

  const heldKey = generateHeldKey({ busy, dirty, canGenerate });
  const heldText = heldKey ? intl.formatMessage({ id: heldKey }) : null;
  const generateDisabled = busy || Boolean(heldKey);

  // A browser drops focus from a control that is disabled under it, and the
  // send holds Generate and the uploader until the case is read back. When a
  // send this section started ends, either way, focus goes back to the
  // control that started it, unless the person has moved it on purpose
  // meanwhile: focus left on nothing or on the page itself counts as lost.
  useEffect(() => {
    const ended = wasBusy.current && !busy;
    wasBusy.current = busy;
    if (!ended || !focusAfterSend.current) {
      return;
    }
    const target = focusAfterSend.current();
    focusAfterSend.current = null;
    const held = document.activeElement;
    const lost = !held || held === document.body || !held.isConnected;
    if (lost && target && !target.disabled) {
      target.focus();
    }
  }, [busy]);

  const generateButton = () => generateRef.current;
  const uploadButton = () => uploadRef.current?.querySelector("button");

  const pressGenerate = () => {
    if (newestGenerated) {
      setRegenerateOpen(true);
      focusFirstOf("#" + REGENERATE_CONFIRM_ID);
      return;
    }
    focusAfterSend.current = generateButton;
    onGenerate();
  };

  // The new tab is opened by the caller inside this click, so the click
  // reaches it directly rather than after a state change. The question took
  // the place of Generate, so focus goes to it once it is rendered again.
  const confirmRegenerate = () => {
    focusAfterSend.current = generateButton;
    onGenerate();
    setRegenerateOpen(false);
    focusFirstOf("#" + GENERATE_ID);
  };

  const cancelRegenerate = () => {
    setRegenerateOpen(false);
    focusFirstOf("#" + GENERATE_ID);
  };

  const chooseFile = async (event) => {
    const file = event?.target?.files?.[0];
    if (!file) {
      return;
    }
    const base64Image = await toBase64(file);
    focusAfterSend.current = uploadButton;
    // Only the file: the server builds the row and decides its version,
    // type and author.
    onUpload({ base64Image });
    uploaderRef.current?.clearFiles();
  };

  const openFile = (id) =>
    window.open(fileUrl(id), "_blank", "noopener,noreferrer");

  const askWithdraw = (report) => {
    setWithdrawing(report.version);
    focusFirstOf("#" + withdrawConfirmId(report.version));
  };

  const confirmWithdraw = (report) => {
    // The question and its button leave the row, but the row stays listed
    // (voided, or as it was when refused), so its version link is where
    // focus goes back to; a row no longer listed takes none.
    focusAfterSend.current = () =>
      document.getElementById(reportLinkId(report.id));
    onWithdraw(report.id);
    setWithdrawing(null);
  };

  const cancelWithdraw = (report) => {
    setWithdrawing(null);
    focusFirstOf("#" + withdrawId(report.version));
  };

  const headers = [
    {
      key: "version",
      header: intl.formatMessage({ id: "pathology.report.version" }),
    },
    {
      key: "generatedAt",
      header: intl.formatMessage({ id: "pathology.report.generated" }),
    },
    {
      key: "generatedBy",
      header: intl.formatMessage({ id: "pathology.report.by" }),
    },
    {
      key: "type",
      header: intl.formatMessage({ id: "common.type" }),
    },
    ...(actionsShown
      ? [
          {
            key: "actions",
            header: intl.formatMessage({ id: "common.actions" }),
          },
        ]
      : []),
  ];

  const withdrawQuestion = (report) => (
    <TableRow className="pathology-case-view__report-question-row">
      <TableCell colSpan={headers.length}>
        <div
          id={`reportWithdrawQuestion${report.version}`}
          role="group"
          aria-labelledby={withdrawTextId(report.version)}
        >
          <Stack gap={3}>
            <p id={withdrawTextId(report.version)}>
              <FormattedMessage
                id="pathology.confirm.withdrawReport"
                values={{ version: report.version }}
              />
            </p>
            <div className="pathology-case-view__stage-control">
              <Button
                kind="secondary"
                size="sm"
                id={withdrawConfirmId(report.version)}
                disabled={busy}
                onClick={() => confirmWithdraw(report)}
              >
                <FormattedMessage id="pathology.action.withdrawReport" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                disabled={busy}
                onClick={() => cancelWithdraw(report)}
              >
                <FormattedMessage id="common.cancel" />
              </Button>
            </div>
          </Stack>
        </div>
      </TableCell>
    </TableRow>
  );

  return (
    <Stack gap={5} className="pathology-case-view__reports">
      {/* No heading of its own: the accordion header above already names
          this section, and repeating the word under it says nothing. */}
      {/* While the regenerate question is open it takes the place of the
          row of buttons, so neither can be pressed past it. */}
      {regenerateOpen && newestGenerated ? (
        <div role="group" aria-labelledby={REGENERATE_TEXT_ID}>
          <Stack gap={3}>
            <p id={REGENERATE_TEXT_ID}>
              <FormattedMessage
                id="pathology.confirm.regenerate"
                values={{ version: newestGenerated.version }}
              />
            </p>
            <div className="pathology-case-view__stage-control">
              <Button
                kind="secondary"
                size="sm"
                id={REGENERATE_CONFIRM_ID}
                disabled={generateDisabled}
                onClick={confirmRegenerate}
              >
                <FormattedMessage id="pathology.action.generateNewVersion" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                disabled={busy}
                onClick={cancelRegenerate}
              >
                <FormattedMessage id="common.cancel" />
              </Button>
            </div>
          </Stack>
        </div>
      ) : (
        <Stack gap={3}>
          <div className="pathology-case-view__report-actions">
            <div className="pathology-case-view__report-generate">
              <Button
                kind="primary"
                size="md"
                id={GENERATE_ID}
                data-testid="report-generate"
                ref={generateRef}
                disabled={generateDisabled}
                title={heldText ?? undefined}
                aria-describedby={heldText ? GENERATE_REASON_ID : undefined}
                onClick={pressGenerate}
              >
                <FormattedMessage id="pathology.action.generateReport" />
              </Button>
            </div>
            {/* The uploader's own description is hidden from view (Carbon
                sets it above the button, out of line with Generate) and
                stays the button's description; the hint under the button
                is the one shown. */}
            <div
              ref={uploadRef}
              className="pathology-case-view__report-upload"
              data-testid="report-upload"
            >
              <FileUploader
                ref={uploaderRef}
                labelDescription={intl.formatMessage({
                  id: "clinicalAttachment.uploadHint",
                })}
                buttonLabel={intl.formatMessage({
                  id: "pathology.action.uploadReport",
                })}
                iconDescription={intl.formatMessage({
                  id: "pathology.action.uploadReport",
                })}
                multiple={false}
                accept={["image/jpeg", "image/png", "application/pdf"]}
                disabled={busy}
                name=""
                buttonKind="tertiary"
                size="md"
                filenameStatus="edit"
                onChange={chooseFile}
              />
              {/* Under its own button. Read once, as the upload button's
                  description, not again here. */}
              <p
                id={UPLOAD_HINT_ID}
                className="pathology-case-view__hint"
                aria-hidden="true"
              >
                <FormattedMessage id="clinicalAttachment.uploadHint" />
              </p>
              {uploadError && (
                <p
                  className="pathology-case-view__report-upload-error"
                  role="alert"
                >
                  {uploadError}
                </p>
              )}
            </div>
          </div>
          {/* The reason is a sentence of any length: under the row, it
              cannot push Upload report below Generate report. */}
          {heldText && (
            <span id={GENERATE_REASON_ID} className="case-view__locked-hint">
              {heldText}
            </span>
          )}
        </Stack>
      )}

      {rows.length === 0 ? (
        <p className="case-view__locked-hint">
          <FormattedMessage id="pathology.empty.noReports" />
        </p>
      ) : (
        <DataTable
          rows={rows.map((report) => ({ id: String(report.id) }))}
          headers={headers}
        >
          {({ headers: tableHeaders, getTableProps }) => (
            <TableContainer
              title={intl.formatMessage({ id: "common.reports" })}
              description={intl.formatMessage({
                id: "pathology.helper.reportOpensNewTab",
              })}
              className="pathology-case-view__report-container"
            >
              <Table
                {...getTableProps()}
                size="md"
                className="pathology-case-view__report-table"
                data-testid="reports-table"
              >
                <TableHead>
                  <TableRow>
                    {tableHeaders.map((header) => (
                      <TableHeader
                        key={header.key}
                        className={`pathology-case-view__report-col--${header.key}`}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {/* The section's own rows, not the table's copy: that copy lags
                      one render behind a list that lost a row. */}
                  {rows.map((report) => {
                    const row = { id: String(report.id) };
                    const asking = withdrawing === report.version;
                    return (
                      <React.Fragment key={row.id}>
                        <TableRow
                          className="pathology-case-view__report-row"
                          data-testid={`report-row-${report.version}`}
                          onClick={() => openFile(report.id)}
                        >
                          <TableCell className="pathology-case-view__report-col--version">
                            <Link
                              id={reportLinkId(report.id)}
                              href={fileUrl(report.id)}
                              target="_blank"
                              rel="noopener noreferrer"
                              aria-label={intl.formatMessage(
                                {
                                  id:
                                    report.state === "VOIDED"
                                      ? "pathology.report.openVoided"
                                      : "pathology.report.open",
                                },
                                { version: report.version },
                              )}
                              // The link opens the file itself; the row
                              // must not open it a second time.
                              onClick={(event) => event.stopPropagation()}
                            >
                              {`v${report.version}`}
                            </Link>
                          </TableCell>
                          <TableCell className="pathology-case-view__report-col--generatedAt">
                            {report.generatedAt}
                          </TableCell>
                          <TableCell className="pathology-case-view__report-col--generatedBy">
                            {report.generatedBy}
                          </TableCell>
                          <TableCell className="pathology-case-view__report-col--type">
                            <span className="pathology-case-view__report-tags">
                              <Tag
                                type={TYPE_TAGS[report.type] ?? "gray"}
                                size="sm"
                              >
                                {REPORT_TYPE_KEYS[report.type]
                                  ? intl.formatMessage({
                                      id: REPORT_TYPE_KEYS[report.type],
                                    })
                                  : report.type}
                              </Tag>
                              {STATE_KEYS[report.state] && (
                                <Tag type={STATE_TAGS[report.state]} size="sm">
                                  {intl.formatMessage({
                                    id: STATE_KEYS[report.state],
                                  })}
                                </Tag>
                              )}
                            </span>
                          </TableCell>
                          {actionsShown && (
                            <TableCell
                              className="pathology-case-view__report-col--actions"
                              // Withdrawing is not opening: a press here
                              // stays in the cell.
                              onClick={(event) => event.stopPropagation()}
                            >
                              {isWithdrawable(report) && !asking && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  id={withdrawId(report.version)}
                                  data-testid={`report-withdraw-${report.version}`}
                                  disabled={busy}
                                  onClick={() => askWithdraw(report)}
                                >
                                  <FormattedMessage id="pathology.action.withdrawReport" />
                                </Button>
                              )}
                            </TableCell>
                          )}
                        </TableRow>
                        {asking && withdrawQuestion(report)}
                      </React.Fragment>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}
    </Stack>
  );
};

export default ReportsSection;
