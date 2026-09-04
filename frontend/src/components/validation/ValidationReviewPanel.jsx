import React, { useMemo, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { WarningAltFilled } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import ESignatureButton, {
  SignatureMeaning,
} from "../esignature/ESignatureButton";
import {
  convertAlphaNumLabNumForDisplay,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import ReferenceSection from "../resultPage/unified/ReferenceSection";
import HistorySection from "../resultPage/unified/HistorySection";
import CriticalBanner from "../resultPage/unified/CriticalBanner";
import {
  AttachmentsSection,
  OrderInfoSection,
  useOrderContext,
} from "../resultPage/unified/orderContextSections";
import { FlagChip, accentClass } from "../resultPage/unified/flags";
import {
  isSectionOpen,
  loadSectionLayout,
  rememberSectionChoice,
} from "../resultPage/unified/sectionLayout";
import "../resultPage/unified/unified-results.scss";
import { triageRows } from "./validationTriage";
import {
  NOTE_CONTEXT_MODIFICATION,
  NOTE_CONTEXT_VALIDATION,
  NOTE_EXTERNAL,
  NOTE_INTERNAL,
  actionPayload,
  componentRowsFor,
  displayResult,
  errorMessageKey,
  flagFor,
  isEditableHere,
  unitsOnly,
} from "./validationReview";

/**
 * OGC-1028 (Validation v4 slice V2) — the per-row review panel (FR-C1..C4):
 * a read-only summary led by the flag-styled result with Method and Analyzer
 * as two fields (FR-G1), "Before releasing, check:" echoing the row's chips
 * (FR-C2), the analysis's components in display order (FR-C4), the critical
 * acknowledgment banner (FR-D6), per-row Validate & release / Modify (FR-D1,
 * FR-D4), the Results Entry hand-off for referral (FR-D5), and the collapsed
 * reference sections — dual-axis Notes (FR-F1), QC, Order info, History (FR-E1)
 * and Attachments — reusing the Results Entry building blocks.
 */
const QC_TAG_TYPE = { PASS: "green", FAIL: "red" };

/**
 * The expanded row spans the whole (horizontally scrolling) table, so the
 * panel caps its own width to stay readable without scrolling sideways.
 */
const PANEL_STYLE = { maxWidth: "1200px" };
const LABEL_STYLE = { display: "block" };

const resultsEntryLink = (accessionNumber) =>
  `/result?type=order&doRange=false&accessionNumber=${encodeURIComponent(
    accessionNumber || "",
  )}`;

const Field = ({ labelKey, value, testId }) => (
  <div data-testid={testId}>
    <span className="cds--label" style={LABEL_STYLE}>
      <FormattedMessage id={labelKey} />
    </span>
    <span>{value}</span>
  </div>
);

const ValidationReviewPanel = ({
  data: row,
  rows = [],
  triageByRowId,
  configurationProperties,
  qcAck,
  onActionDone,
  onNoteChange,
}) => {
  const intl = useIntl();
  const triage =
    (triageByRowId && triageByRowId.get(row.id)) || triageRows([row])[0];
  const chips = triage.chips;
  const flag = flagFor(row, triage.signals);
  const qcStatus = ["PASS", "FAIL"].includes(row.qcStatus)
    ? row.qcStatus
    : "UNKNOWN";
  const notRecorded = intl.formatMessage({
    id: "label.validation.review.notRecorded",
  });
  const accession =
    configurationProperties?.AccessionFormat === "ALPHANUM"
      ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
      : row.accessionNumber;
  const notesRequired =
    configurationProperties?.notesRequiredForModifyResults === "true";
  const editableHere = isEditableHere(row.resultType);
  const notes = row.analysisNotes || [];
  const components = useMemo(
    () => componentRowsFor(rows, row.analysisId),
    [rows, row.analysisId],
  );
  const order = useOrderContext(row.accessionNumber);

  const [layout, setLayout] = useState(() => loadSectionLayout());
  // The composer is the row's single note input (the queue has no Notes
  // column): it starts from whatever the row already carries, so collapsing
  // and re-expanding keeps the text, and every change is published back to
  // the row for the legacy batch release.
  const [noteText, setNoteText] = useState(row.note || "");
  // Release notes print on the patient report unless the validator opts out
  // (legacy parity); a modification reason stays internal unless opted in.
  const [noteVisibility, setNoteVisibility] = useState(
    row.noteVisibility === NOTE_INTERNAL ? NOTE_INTERNAL : NOTE_EXTERNAL,
  );
  const [visibilityChosen, setVisibilityChosen] = useState(false);

  const publishNote = (text, visibility) => {
    if (onNoteChange) {
      onNoteChange(row.id, text, visibility);
    }
  };
  const changeNoteText = (text) => {
    setNoteText(text);
    publishNote(text, noteVisibility);
  };
  const changeNoteVisibility = (visibility, chosen) => {
    setNoteVisibility(visibility);
    if (chosen) {
      setVisibilityChosen(true);
    }
    publishNote(noteText, visibility);
  };
  const [modifying, setModifying] = useState(false);
  const [newValue, setNewValue] = useState(row.result ?? "");
  const [busy, setBusy] = useState(false);
  const [errorKey, setErrorKey] = useState("");

  const sectionOpen = (id, autoOpen = false) =>
    isSectionOpen(layout, id, autoOpen);
  const toggleSection = (id) => (open) =>
    setLayout(rememberSectionChoice(id, open));

  const submit = (action, payload) => {
    setBusy(true);
    setErrorKey("");
    postToOpenElisServerJsonResponse(
      `/rest/AccessionValidation/analysis/${row.analysisId}/${action}`,
      JSON.stringify(payload),
      (response) => {
        setBusy(false);
        if (response && response.outcome && !response.error) {
          if (onActionDone) {
            onActionDone(response.outcome, row);
          }
          return;
        }
        setErrorKey(errorMessageKey(response));
      },
    );
  };

  const release = () =>
    submit(
      "release",
      actionPayload(row, {
        note: noteText,
        noteVisibility,
        noteContext: NOTE_CONTEXT_VALIDATION,
      }),
    );

  const saveModification = () =>
    submit(
      "modify",
      actionPayload(row, {
        note: noteText,
        noteVisibility,
        noteContext: NOTE_CONTEXT_MODIFICATION,
        result: newValue,
      }),
    );

  const qcAckBlocksRelease = Boolean(qcAck?.required && !qcAck?.satisfied);
  const releaseBlocked = busy || qcAckBlocksRelease;
  const reasonMissing = notesRequired && !noteText.trim();
  const modificationBlocked =
    busy || !editableHere || !String(newValue ?? "").trim() || reasonMissing;

  const signContext = intl.formatMessage(
    {
      id: "esig.context.validateSingle",
      defaultMessage: "Validate and release {test} for accession {accession}",
    },
    { test: row.testName, accession },
  );

  return (
    <div
      className="unifiedExpandedPanel validationReviewPanel"
      style={PANEL_STYLE}
      data-testid={`validation-review-panel-${row.id}`}
    >
      <div className="unifiedContextStrip">
        <span className="unifiedContextPrimary">{accession}</span>
        {row.patientName && <span>{row.patientName}</span>}
        {row.patientInfo && <span>{row.patientInfo}</span>}
        {row.sampleType && <span>{row.sampleType}</span>}
        {row.collectionDate && (
          <span>
            <FormattedMessage id="label.validation.review.collected" />:{" "}
            {row.collectionDate}
          </span>
        )}
      </div>

      <div className="unifiedWorkZone">
        <div className="unifiedWorkZoneGrid">
          <div className={accentClass(flag)} data-testid="review-result">
            <span className="cds--label" style={LABEL_STYLE}>
              <FormattedMessage id="label.validation.review.result" />
            </span>
            <strong>{displayResult(row)}</strong>
            {unitsOnly(row.units) && <span> {unitsOnly(row.units)}</span>}{" "}
            <FlagChip flag={flag} />
          </div>
          <Field
            labelKey="label.validation.review.normalRange"
            value={row.normalRange || notRecorded}
            testId="review-normal-range"
          />
          <Field
            labelKey="label.validation.review.criticalRange"
            value={row.criticalRange || notRecorded}
            testId="review-critical-range"
          />
          <Field
            labelKey="label.validation.review.method"
            value={row.methodName || notRecorded}
            testId="review-method"
          />
          <Field
            labelKey="label.validation.review.analyzer"
            value={row.analyzerName || notRecorded}
            testId="review-analyzer"
          />
          <Field
            labelKey="label.validation.review.enteredBy"
            value={row.enteredBy || notRecorded}
            testId="review-entered-by"
          />
          <Field
            labelKey="label.validation.review.enteredDate"
            value={row.enteredDate || row.resultDate || notRecorded}
            testId="review-entered-date"
          />
          <div data-testid="review-qc">
            <span className="cds--label" style={LABEL_STYLE}>
              <FormattedMessage id="label.validation.review.qc" />
            </span>
            <Tag size="sm" type={QC_TAG_TYPE[qcStatus] || "gray"}>
              {intl.formatMessage({
                id: `label.validation.review.qc.${qcStatus}`,
              })}
            </Tag>
          </div>
        </div>

        {chips.length > 0 && (
          <div
            data-testid="review-before-release"
            style={{
              display: "flex",
              flexWrap: "wrap",
              gap: "0.25rem",
              alignItems: "center",
              margin: "0.5rem 0",
            }}
          >
            <strong>
              <FormattedMessage id="label.validation.review.beforeRelease" />
            </strong>
            {chips.map((chip) => (
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

        {components.length > 1 && (
          <div data-testid="review-components" style={{ margin: "0.5rem 0" }}>
            <span className="cds--label">
              <FormattedMessage id="label.validation.review.components" />
            </span>
            <table className="cds--data-table cds--data-table--sm">
              <thead>
                <tr>
                  <th>
                    <FormattedMessage id="label.validation.review.component" />
                  </th>
                  <th>
                    <FormattedMessage id="label.validation.review.result" />
                  </th>
                  <th>
                    <FormattedMessage id="label.validation.review.units" />
                  </th>
                  <th>
                    <FormattedMessage id="label.validation.review.normalRange" />
                  </th>
                  <th>
                    <FormattedMessage id="label.validation.review.flag" />
                  </th>
                </tr>
              </thead>
              <tbody>
                {components.map((component) => {
                  const componentTriage =
                    (triageByRowId && triageByRowId.get(component.id)) ||
                    triageRows([component])[0];
                  return (
                    <tr
                      key={component.id}
                      data-testid={`review-component-${component.id}`}
                    >
                      <td>
                        {component.componentLabel ||
                          intl.formatMessage({
                            id: "label.validation.review.primary",
                          })}
                      </td>
                      <td>{displayResult(component)}</td>
                      <td>{unitsOnly(component.units)}</td>
                      <td>{component.normalRange}</td>
                      <td>
                        <FlagChip
                          flag={flagFor(component, componentTriage.signals)}
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {(row.critical || row.ackPending) && (
          <CriticalBanner
            analysisId={row.analysisId}
            criticalRange={row.criticalRange}
          />
        )}

        {modifying && (
          <div
            className="unifiedWorkZoneValue"
            data-testid="review-modify-editor"
          >
            {editableHere && row.resultType === "D" && (
              <Select
                id={`review-new-value-${row.id}`}
                labelText={intl.formatMessage({
                  id: "label.validation.review.modify.newValue",
                })}
                value={newValue}
                onChange={(event) => setNewValue(event.target.value)}
              >
                <SelectItem value="" text="" />
                {(row.dictionaryResults || []).map((entry) => (
                  <SelectItem
                    key={entry.id}
                    value={String(entry.id)}
                    text={entry.value}
                  />
                ))}
              </Select>
            )}
            {editableHere && row.resultType !== "D" && (
              <TextInput
                id={`review-new-value-${row.id}`}
                labelText={intl.formatMessage({
                  id: "label.validation.review.modify.newValue",
                })}
                value={newValue}
                onChange={(event) => setNewValue(event.target.value)}
              />
            )}
            {!editableHere && (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title=""
                subtitle={intl.formatMessage({
                  id: "label.validation.review.modify.notEditable",
                })}
              />
            )}
          </div>
        )}

        <div className="unifiedNoteComposer" data-testid="review-note-composer">
          <TextArea
            id={`review-note-${row.id}`}
            labelText={intl.formatMessage({
              id: modifying
                ? "label.validation.review.modify.reason"
                : "label.validation.review.notes.add",
            })}
            placeholder={intl.formatMessage({
              id: "label.validation.review.notes.placeholder",
            })}
            rows={2}
            value={noteText}
            onChange={(event) => changeNoteText(event.target.value)}
            invalid={modifying && reasonMissing}
            invalidText={intl.formatMessage({
              id: "label.validation.review.modify.reasonRequired",
            })}
          />
          <RadioButtonGroup
            name={`review-note-visibility-${row.id}`}
            legendText={intl.formatMessage({
              id: "label.validation.review.notes.visibility",
            })}
            valueSelected={noteVisibility}
            onChange={(value) => changeNoteVisibility(value, true)}
            data-testid="review-note-visibility"
          >
            <RadioButton
              id={`review-note-internal-${row.id}`}
              labelText={intl.formatMessage({
                id: "label.validation.review.notes.internal",
              })}
              value={NOTE_INTERNAL}
            />
            <RadioButton
              id={`review-note-external-${row.id}`}
              labelText={intl.formatMessage({
                id: "label.validation.review.notes.external",
              })}
              value={NOTE_EXTERNAL}
            />
          </RadioButtonGroup>
          {noteVisibility === NOTE_EXTERNAL && (
            <span
              className="unifiedFieldHint"
              data-testid="review-note-external-warning"
            >
              <FormattedMessage id="label.validation.review.notes.externalWarning" />
            </span>
          )}
          <span className="unifiedFieldHint">
            <FormattedMessage id="label.validation.review.notes.contextHint" />
          </span>
        </div>

        {errorKey && (
          <div data-testid="review-error">
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "notification.title" })}
              subtitle={intl.formatMessage({ id: errorKey })}
            />
          </div>
        )}

        <div
          className="unifiedWorkZoneActions"
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "0.5rem",
            alignItems: "center",
            marginTop: "0.5rem",
          }}
        >
          {!modifying && (
            <span data-testid="review-release">
              <ESignatureButton
                meaning={SignatureMeaning.VALIDATED_AND_RELEASED}
                context={signContext}
                recordType="VALIDATION_BATCH"
                recordId={Number(row.analysisId)}
                onBeforeSign={qcAck?.beforeSign}
                onSign={release}
                disabled={releaseBlocked}
                size="sm"
              >
                <FormattedMessage id="label.validation.review.action.release" />
              </ESignatureButton>
            </span>
          )}
          {!modifying && (
            <Button
              kind="tertiary"
              size="sm"
              disabled={busy}
              onClick={() => {
                setModifying(true);
                if (!visibilityChosen) {
                  changeNoteVisibility(NOTE_INTERNAL, false);
                }
              }}
              data-testid="review-modify"
            >
              <FormattedMessage id="label.validation.review.action.modify" />
            </Button>
          )}
          {modifying && (
            <Button
              size="sm"
              disabled={modificationBlocked}
              onClick={saveModification}
              data-testid="review-save-modification"
            >
              <FormattedMessage id="label.validation.review.action.saveModification" />
            </Button>
          )}
          {modifying && (
            <Button
              kind="ghost"
              size="sm"
              disabled={busy}
              onClick={() => {
                setModifying(false);
                setNewValue(row.result ?? "");
                setErrorKey("");
                if (!visibilityChosen) {
                  changeNoteVisibility(NOTE_EXTERNAL, false);
                }
              }}
              data-testid="review-cancel-modification"
            >
              <FormattedMessage id="label.validation.review.action.cancel" />
            </Button>
          )}
          <Button
            kind="ghost"
            size="sm"
            as="a"
            href={resultsEntryLink(row.accessionNumber)}
            target="_blank"
            rel="noopener noreferrer"
            data-testid="review-refer"
          >
            <FormattedMessage id="label.validation.review.action.refer" />
          </Button>
          {qcAckBlocksRelease && (
            <span className="unifiedFieldHint" data-testid="review-qc-ack-hint">
              <FormattedMessage id="label.validation.review.release.qcAckFirst" />
            </span>
          )}
        </div>
      </div>

      <div className="unifiedRefZone">
        <div className="unifiedRefZoneHeader">
          <span className="unifiedRefZoneLabel">
            <FormattedMessage id="label.validation.review.reference" />
          </span>
        </div>

        <ReferenceSection
          sectionId="notes"
          title={<FormattedMessage id="label.validation.review.notes" />}
          summary={intl.formatMessage(
            { id: "label.validation.review.notes.summary" },
            { count: notes.length },
          )}
          open={sectionOpen("notes", notes.length > 0 || modifying)}
          onToggle={toggleSection("notes")}
        >
          {notes.length === 0 && (
            <div className="unifiedHistoryEmpty">
              <FormattedMessage id="label.validation.review.notes.empty" />
            </div>
          )}
          {notes.length > 0 && (
            <div className="unifiedNotes" data-testid="review-notes-list">
              {notes.map((note, index) => (
                <div className="unifiedNoteItem" key={index}>
                  <div className="unifiedNoteMeta">
                    <Tag size="sm" type="gray">
                      {note.subject ||
                        intl.formatMessage({
                          id: "label.validation.review.notes.context.result",
                        })}
                    </Tag>
                    <Tag
                      size="sm"
                      type={
                        note.noteType === NOTE_EXTERNAL ? "blue" : "cool-gray"
                      }
                    >
                      {intl.formatMessage({
                        id:
                          note.noteType === NOTE_EXTERNAL
                            ? "label.validation.review.notes.external"
                            : "label.validation.review.notes.internal",
                      })}
                    </Tag>
                    {note.author && <span>{note.author}</span>}
                    {note.date && <span>{note.date}</span>}
                  </div>
                  <div style={{ whiteSpace: "pre-wrap" }}>{note.text}</div>
                </div>
              ))}
            </div>
          )}
        </ReferenceSection>

        <ReferenceSection
          sectionId="qc"
          title={<FormattedMessage id="label.validation.review.qcSection" />}
          summary={intl.formatMessage({
            id: `label.validation.review.qc.${qcStatus}`,
          })}
          open={sectionOpen("qc")}
          onToggle={toggleSection("qc")}
        >
          <Tag size="sm" type={QC_TAG_TYPE[qcStatus] || "gray"}>
            {intl.formatMessage({
              id: `label.validation.review.qc.${qcStatus}`,
            })}
          </Tag>
          <div className="unifiedFieldHint">
            <FormattedMessage id="label.validation.review.reagents.followUp" />
          </div>
        </ReferenceSection>

        <OrderInfoSection
          open={sectionOpen("orderInfo")}
          onToggle={toggleSection("orderInfo")}
          order={order}
          sampleType={row.sampleType}
          receivedDate={row.receivedDate}
          technician={row.enteredBy}
        />

        <HistorySection
          analysisId={row.analysisId}
          componentId={row.testResultComponentId || undefined}
          open={sectionOpen("history")}
          onToggle={toggleSection("history")}
        />

        <AttachmentsSection
          open={sectionOpen("attachments")}
          onToggle={toggleSection("attachments")}
          accessionNumber={row.accessionNumber}
          analysisId={row.analysisId}
          componentId={row.testResultComponentId || undefined}
          editable={false}
        />
      </div>
    </div>
  );
};

export default ValidationReviewPanel;
