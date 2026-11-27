import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import SearchableSelect from "../common/SearchableSelect";
import { RequiredMarker, requiredProps } from "../common/RequiredMarker";
import StatusBadge from "../caseView/StatusBadge";
import { focusFirstOf } from "./focus";
import { consultationRowActions } from "./pathologySections";
import "./pathologyCaseView.scss";

/** The button that opens a new second opinion, where focus lands after one. */
export const REQUEST_CONSULTATION_BUTTON_ID = "requestConsultation";

/** The line above the table that says why every second opinion action is held. */
export const CONSULTATIONS_HOLD_ID = "consultationsHold";

const COLLEAGUE_ID = "consultationColleague";
const REASON_ID = "consultationReason";
const PRE_TREATMENT_NAME = "consultationPreTreatment";
const ASK_BUTTON_ID = "askColleague";
const ASK_HINT_ID = "askColleagueHint";

// The column the outcome's note is stored in.
const NOTE_MAX_LENGTH = 1000;

const OUTCOMES = Object.freeze(["AGREE", "DISAGREE", "MODIFIED"]);

// i18n-keys: pathology.consultationStatus.*, pathology.consultationOutcome.*
const STATUS_BADGES = Object.freeze({
  REQUESTED: {
    kind: "pending",
    textKey: "pathology.consultationStatus.REQUESTED",
  },
  WITHDRAWN: {
    kind: "none",
    textKey: "pathology.consultationStatus.WITHDRAWN",
  },
});

const OUTCOME_BADGES = Object.freeze({
  AGREE: { kind: "complete", textKey: "pathology.consultationOutcome.AGREE" },
  DISAGREE: {
    kind: "partial",
    textKey: "pathology.consultationOutcome.DISAGREE",
  },
  MODIFIED: {
    kind: "partial",
    textKey: "pathology.consultationOutcome.MODIFIED",
  },
});

const emptyDraft = () => ({
  consultedUserId: "",
  reasonId: "",
  preTreatment: null,
});

// The first rule the request does not meet yet, in the order of the form.
const draftBlockedKey = (draft) => {
  if (!draft.consultedUserId) {
    return "pathology.locked.consultationColleagueFirst";
  }
  if (draft.preTreatment === null) {
    return "pathology.locked.consultationPreTreatmentFirst";
  }
  return null;
};

// Each column's header and cells carry its class, which sets its width.
const COLUMN_CLASS = Object.freeze({
  colleague: "pathology-case-view__consultation-colleague",
  requested: "pathology-case-view__consultation-requested",
  outcome: "pathology-case-view__consultation-outcome",
  actions: "pathology-case-view__consultation-actions",
});

// The inline rows are forms laid over the table, not second opinions.
const FORM_ROW_CLASS = "pathology-case-view__request-form-row";
// Their one cell, which breaks a long word, such as a name in the withdraw
// question, rather than widening the row.
const FORM_CELL_CLASS = "pathology-case-view__consultation-form-cell";

const describedBy = (...ids) => ids.filter(Boolean).join(" ") || undefined;

// A user's display name is "last,first" with no space. The narrow first column
// may break it after the comma, so it is not cut inside a word while it fits.
const nameWithBreaks = (name) =>
  String(name ?? "")
    .split(",")
    .flatMap((part, index, parts) =>
      index < parts.length - 1 ? [part + ",", <wbr key={index} />] : [part],
    );

/**
 * The second opinions on a case (FR-11): whom was asked and why, when and by
 * whom, before treatment or not, and where each stands. The rows are the case
 * read's, newest first. Every change is handed to the caller, which posts it
 * and reads the case again, and resolves `{ ok }` once that read has landed.
 *
 * `shown` false renders nothing (a locked Review). `canRequest` offers Request
 * second opinion, which opens the request as the table's first row. A row
 * offers Record the outcome to the consulted user and Withdraw request to
 * another pathologist while it awaits a response; each opens a row under it.
 * The request and those rows keep their own state, outside the case form.
 *
 * `reasons` is the dictionary list, null while it is read, or
 * `{ failed: true }` when it could not be read; either way the request goes
 * without a reason. `lock` holds every change, its reason written once above
 * the table. `held` is a change still out: the rows and what was typed stay on
 * the page, disabled, and a refused change can be sent again as it stands.
 */
const ConsultationsPanel = ({
  consultations,
  canRequest,
  viewerId,
  isPathologist,
  colleagues,
  reasons,
  suggestedFor,
  lock,
  held,
  shown,
  onRequest,
  onAnswer,
  onWithdraw,
}) => {
  const intl = useIntl();
  const mounted = useRef(true);
  const [draftOpen, setDraftOpen] = useState(false);
  const [draft, setDraft] = useState(emptyDraft);
  // The one row whose outcome or withdrawal row is open.
  const [inline, setInline] = useState(null);
  const [outcome, setOutcome] = useState("");
  const [note, setNote] = useState("");

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  if (!shown) {
    return null;
  }

  const rows = consultations ?? [];
  const actionsByRow = new Map(
    rows.map((row) => [
      row.id,
      consultationRowActions(row, viewerId, isPathologist),
    ]),
  );
  const showActions = [...actionsByRow.values()].some(
    (actions) => actions.answer || actions.withdraw,
  );
  const columnCount = showActions ? 4 : 3;
  const lockText =
    lock?.disabled && lock.reasonKey
      ? intl.formatMessage({ id: lock.reasonKey })
      : null;
  const holdId = lockText ? CONSULTATIONS_HOLD_ID : null;
  const holdProps = {
    disabled: Boolean(lock?.disabled),
    title: lockText ?? undefined,
    "aria-describedby": describedBy(holdId),
  };

  const reasonList = Array.isArray(reasons) ? reasons : [];
  const reasonSentenceKey = reasons?.failed
    ? "pathology.error.consultationReasonsFailed"
    : Array.isArray(reasons) && reasons.length === 0
      ? "pathology.empty.noConsultationReasons"
      : null;

  const patchDraft = (patch) =>
    setDraft((current) => ({ ...current, ...patch }));

  const toggleDraft = () => {
    if (draftOpen) {
      setDraftOpen(false);
      return;
    }
    setDraftOpen(true);
    focusFirstOf("#" + COLLEAGUE_ID);
  };

  const closeDraft = () => {
    setDraftOpen(false);
    setDraft(emptyDraft());
    focusFirstOf("#" + REQUEST_CONSULTATION_BUTTON_ID);
  };

  const ask = async (event) => {
    const answer = await onRequest(
      {
        consultedUserId: draft.consultedUserId,
        reasonId: draft.reasonId || null,
        preTreatment: draft.preTreatment,
      },
      event.currentTarget,
    );
    if (answer?.ok && mounted.current) {
      setDraftOpen(false);
      setDraft(emptyDraft());
      focusFirstOf("#" + REQUEST_CONSULTATION_BUTTON_ID);
    }
  };

  const openInline = (row, mode) => {
    setInline({ id: row.id, mode });
    setOutcome("");
    setNote("");
    focusFirstOf(
      mode === "answer"
        ? "#consultationOutcome-" + row.id + "-" + OUTCOMES[0]
        : "#consultationWithdrawKeep-" + row.id,
    );
  };

  const closeInline = (row, startedBy) => {
    setInline(null);
    focusFirstOf("#" + startedBy + "-" + row.id);
  };

  const closeOnOk = (answer) => {
    if (answer?.ok && mounted.current) {
      setInline(null);
    }
  };

  const answer = async (row, event) =>
    closeOnOk(
      await onAnswer(
        row,
        { outcome, note: note.trim() === "" ? null : note.trim() },
        event.currentTarget,
      ),
    );

  const withdraw = async (row, event) =>
    closeOnOk(await onWithdraw(row, event.currentTarget));

  const draftKey = draftBlockedKey(draft);
  const askBlockedText =
    lockText ?? (draftKey ? intl.formatMessage({ id: draftKey }) : null);

  const requestRow = () => (
    <TableRow data-testid="newConsultationRow" className={FORM_ROW_CLASS}>
      <TableCell colSpan={columnCount} className={FORM_CELL_CLASS}>
        <div className="pathology-case-view__row pathology-case-view__request-form">
          <div className="pathology-case-view__row-field">
            <SearchableSelect
              id={COLLEAGUE_ID}
              size="sm"
              required
              labelText={intl.formatMessage({
                id: "pathology.label.consultedPathologist",
              })}
              placeholder={intl.formatMessage({ id: "common.select" })}
              options={colleagues ?? []}
              value={draft.consultedUserId}
              disabled={held}
              onChange={(consultedUserId) => patchDraft({ consultedUserId })}
            />
          </div>
          <div className="pathology-case-view__row-field">
            <Select
              id={REASON_ID}
              size="sm"
              labelText={intl.formatMessage({ id: "common.reason" })}
              helperText={
                reasonSentenceKey
                  ? intl.formatMessage({ id: reasonSentenceKey })
                  : undefined
              }
              value={draft.reasonId}
              disabled={held || reasonList.length === 0}
              onChange={(event) => patchDraft({ reasonId: event.target.value })}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "common.none" })}
              />
              {reasonList.map((reason) => (
                <SelectItem
                  key={reason.id}
                  value={String(reason.id)}
                  text={reason.value}
                />
              ))}
            </Select>
          </div>
          <RadioButtonGroup
            legendText={
              <>
                {intl.formatMessage({ id: "pathology.label.preTreatment" })}
                <RequiredMarker />
              </>
            }
            name={PRE_TREATMENT_NAME}
            valueSelected={
              draft.preTreatment === null ? "" : String(draft.preTreatment)
            }
            disabled={held}
            onChange={(value) =>
              patchDraft({ preTreatment: String(value) === "true" })
            }
          >
            <RadioButton
              id={PRE_TREATMENT_NAME + "-yes"}
              value="true"
              labelText={intl.formatMessage({ id: "label.yes" })}
              {...requiredProps()}
            />
            <RadioButton
              id={PRE_TREATMENT_NAME + "-no"}
              value="false"
              labelText={intl.formatMessage({ id: "label.no" })}
              {...requiredProps()}
            />
          </RadioButtonGroup>
          <div className="pathology-case-view__request-buttons">
            <Button
              kind="primary"
              size="sm"
              id={ASK_BUTTON_ID}
              disabled={Boolean(askBlockedText)}
              title={askBlockedText ?? undefined}
              aria-describedby={describedBy(
                holdId,
                !lockText && draftKey ? ASK_HINT_ID : null,
              )}
              onClick={ask}
            >
              <FormattedMessage id="pathology.action.askColleague" />
            </Button>
            {/* Held while the request is out: closing the row then would
                drop focus while the case is still read again. */}
            <Button
              kind="ghost"
              size="sm"
              id="askColleagueCancel"
              disabled={held}
              onClick={closeDraft}
            >
              <FormattedMessage id="common.cancel" />
            </Button>
            {/* A disabled button takes no focus and shows its title to a
                mouse only, so the rule is also written out beside it. The
                hold reason is already written above the table. */}
            {!lockText && draftKey && (
              <span id={ASK_HINT_ID} className="case-view__locked-hint">
                {intl.formatMessage({ id: draftKey })}
              </span>
            )}
          </div>
        </div>
      </TableCell>
    </TableRow>
  );

  const answerRow = (row) => {
    const hintId = "consultationAnswerHint-" + row.id;
    const ruleText = outcome
      ? null
      : intl.formatMessage({ id: "pathology.locked.consultationOutcomeFirst" });
    const blockedText = lockText ?? ruleText;
    return (
      <TableRow
        data-testid={"consultationAnswer-" + row.id}
        className={FORM_ROW_CLASS}
      >
        <TableCell colSpan={columnCount} className={FORM_CELL_CLASS}>
          <div className="pathology-case-view__row pathology-case-view__request-form">
            <RadioButtonGroup
              legendText={
                <>
                  {intl.formatMessage({
                    id: "pathology.label.consultationOutcome",
                  })}
                  <RequiredMarker />
                </>
              }
              name={"consultationOutcome-" + row.id}
              valueSelected={outcome}
              disabled={held}
              onChange={(value) => setOutcome(String(value))}
            >
              {OUTCOMES.map((value) => (
                <RadioButton
                  key={value}
                  id={"consultationOutcome-" + row.id + "-" + value}
                  value={value}
                  labelText={intl.formatMessage({
                    id: OUTCOME_BADGES[value].textKey,
                  })}
                  {...requiredProps()}
                />
              ))}
            </RadioButtonGroup>
            <div className="pathology-case-view__request-instruction">
              <TextArea
                id={"consultationNote-" + row.id}
                rows={2}
                labelText={intl.formatMessage({ id: "common.notes" })}
                maxLength={NOTE_MAX_LENGTH}
                enableCounter
                maxCount={NOTE_MAX_LENGTH}
                value={note}
                disabled={held}
                onChange={(event) => setNote(event.target.value)}
              />
            </div>
            <div className="pathology-case-view__request-buttons">
              <Button
                kind="primary"
                size="sm"
                id={"consultationAnswerConfirm-" + row.id}
                disabled={Boolean(blockedText)}
                title={blockedText ?? undefined}
                aria-describedby={describedBy(
                  holdId,
                  !lockText && ruleText ? hintId : null,
                )}
                onClick={(event) => answer(row, event)}
              >
                <FormattedMessage id="pathology.action.recordOutcome" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                id={"consultationAnswerCancel-" + row.id}
                disabled={held}
                onClick={() => closeInline(row, "recordOutcome")}
              >
                <FormattedMessage id="common.cancel" />
              </Button>
              {!lockText && ruleText && (
                <span id={hintId} className="case-view__locked-hint">
                  {ruleText}
                </span>
              )}
            </div>
          </div>
        </TableCell>
      </TableRow>
    );
  };

  const withdrawRow = (row) => {
    const question = intl.formatMessage(
      { id: "pathology.label.consultationWithdrawConfirm" },
      { name: row.consultedUser },
    );
    const questionId = "consultationWithdrawQuestion-" + row.id;
    return (
      <TableRow
        data-testid={"consultationWithdraw-" + row.id}
        className={FORM_ROW_CLASS}
      >
        <TableCell colSpan={columnCount} className={FORM_CELL_CLASS}>
          <div className="pathology-case-view__row pathology-case-view__request-form">
            <p id={questionId}>{question}</p>
            <div className="pathology-case-view__request-buttons">
              {/* A danger button describes itself by its own assistive text
                  and ignores aria-describedby, so the question and any hold
                  reason are that text. */}
              <Button
                kind="danger"
                size="sm"
                id={"consultationWithdrawConfirm-" + row.id}
                disabled={holdProps.disabled}
                title={holdProps.title}
                dangerDescription={[question, lockText]
                  .filter(Boolean)
                  .join(" ")}
                onClick={(event) => withdraw(row, event)}
              >
                <FormattedMessage id="pathology.action.withdrawConsultation" />
              </Button>
              {/* Focus lands here when the row opens, so the question is
                  read out with it. */}
              <Button
                kind="ghost"
                size="sm"
                id={"consultationWithdrawKeep-" + row.id}
                disabled={held}
                aria-describedby={questionId}
                onClick={() => closeInline(row, "withdrawConsultation")}
              >
                <FormattedMessage id="pathology.action.keepConsultation" />
              </Button>
            </div>
          </div>
        </TableCell>
      </TableRow>
    );
  };

  const rowAction = (row, id, labelKey, nameKey, onClick) => (
    <Button
      kind="ghost"
      size="sm"
      id={id + "-" + row.id}
      {...holdProps}
      aria-label={intl.formatMessage(
        { id: nameKey },
        { name: row.consultedUser },
      )}
      onClick={onClick}
    >
      <FormattedMessage id={labelKey} />
    </Button>
  );

  const colleagueCell = (row) => (
    <div className="pathology-case-view__slide-status">
      <span>{nameWithBreaks(row.consultedUser)}</span>
      {row.reason && (
        <span className="pathology-case-view__slide-stamp">{row.reason}</span>
      )}
    </div>
  );

  const requestedCell = (row) => (
    <div className="pathology-case-view__slide-status">
      {row.requested?.at ? (
        <span>{row.requested.at}</span>
      ) : (
        <FormattedMessage id="common.notRecorded" />
      )}
      {row.requested?.by && (
        <span className="pathology-case-view__slide-stamp">
          <FormattedMessage
            id="pathology.consultation.requestedBy"
            values={{ name: row.requested.by }}
          />
        </span>
      )}
      <span className="pathology-case-view__slide-stamp">
        <FormattedMessage id="pathology.label.preTreatment" />{" "}
        <strong>
          <FormattedMessage id={row.preTreatment ? "label.yes" : "label.no"} />
        </strong>
      </span>
    </div>
  );

  const outcomeCell = (row) => {
    const badge =
      row.status === "ANSWERED"
        ? OUTCOME_BADGES[row.outcome]
        : STATUS_BADGES[row.status];
    return (
      <div className="pathology-case-view__slide-status">
        {badge ? (
          <StatusBadge kind={badge.kind} textKey={badge.textKey} />
        ) : (
          <FormattedMessage id="common.notRecorded" />
        )}
        {row.status === "ANSWERED" && row.answered?.at && (
          <span className="pathology-case-view__slide-stamp">
            {row.answered.at}
          </span>
        )}
        {row.status === "ANSWERED" && row.outcomeNote && (
          <span className="pathology-case-view__slide-stamp">
            <span className="cds--visually-hidden">
              <FormattedMessage id="common.notes" />{" "}
            </span>
            {row.outcomeNote}
          </span>
        )}
      </div>
    );
  };

  const actionsCell = (row) => {
    const actions = actionsByRow.get(row.id);
    return (
      <div className="pathology-case-view__slide-actions">
        {actions.answer &&
          rowAction(
            row,
            "recordOutcome",
            "pathology.action.recordOutcome",
            "pathology.action.recordOutcomeOf",
            () => openInline(row, "answer"),
          )}
        {actions.withdraw &&
          rowAction(
            row,
            "withdrawConsultation",
            "pathology.action.withdrawConsultation",
            "pathology.action.withdrawConsultationOf",
            () => openInline(row, "withdraw"),
          )}
      </div>
    );
  };

  const inlineRowOf = (row) => {
    if (inline?.id !== row.id) {
      return null;
    }
    const actions = actionsByRow.get(row.id);
    if (inline.mode === "answer" && actions.answer) {
      return answerRow(row);
    }
    if (inline.mode === "withdraw" && actions.withdraw) {
      return withdrawRow(row);
    }
    return null;
  };

  const tableShown = rows.length > 0 || (canRequest && draftOpen);
  const title = intl.formatMessage({ id: "pathology.label.secondOpinion" });
  const suggestions = suggestedFor ?? [];

  return (
    <div className="pathology-case-view__consultations">
      <TableContainer
        title={title}
        description={intl.formatMessage({
          id: "pathology.helper.consultationRules",
        })}
      >
        {canRequest && (
          <TableToolbar aria-label={title}>
            <TableToolbarContent>
              <Button
                kind="secondary"
                size="sm"
                id={REQUEST_CONSULTATION_BUTTON_ID}
                aria-expanded={draftOpen}
                {...holdProps}
                disabled={holdProps.disabled || Boolean(held)}
                onClick={toggleDraft}
              >
                <FormattedMessage id="pathology.action.requestConsultation" />
              </Button>
            </TableToolbarContent>
          </TableToolbar>
        )}
        {canRequest && suggestions.length > 0 && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            data-testid="review-second-opinion-suggested"
            title={intl.formatMessage(
              { id: "pathology.banner.secondOpinionSuggested" },
              { names: intl.formatList(suggestions, { type: "conjunction" }) },
            )}
          />
        )}
        {/* The reason every action is held, written once where a sighted
            keyboard user can read it, and named by every control it holds. */}
        {lockText && (canRequest || showActions) && (
          <p id={CONSULTATIONS_HOLD_ID} className="case-view__locked-hint">
            {lockText}
          </p>
        )}
        {tableShown ? (
          <div
            id="consultationsRegion"
            className="pathology-case-view__slide-scroll"
            role="region"
            aria-label={title}
            tabIndex={0}
          >
            <Table
              size="md"
              className="pathology-case-view__slide-table pathology-case-view__consultation-table"
            >
              <TableHead>
                <TableRow>
                  <TableHeader className={COLUMN_CLASS.colleague}>
                    {intl.formatMessage({
                      id: "pathology.label.consultedPathologist",
                    })}
                  </TableHeader>
                  <TableHeader className={COLUMN_CLASS.requested}>
                    {intl.formatMessage({
                      id: "pathology.label.consultationRequested",
                    })}
                  </TableHeader>
                  <TableHeader className={COLUMN_CLASS.outcome}>
                    {intl.formatMessage({
                      id: "pathology.label.consultationOutcome",
                    })}
                  </TableHeader>
                  {showActions && (
                    <TableHeader className={COLUMN_CLASS.actions}>
                      {intl.formatMessage({ id: "common.actions" })}
                    </TableHeader>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {canRequest && draftOpen && requestRow()}
                {rows.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableRow data-testid={"consultationRow-" + row.id}>
                      <TableCell className={COLUMN_CLASS.colleague}>
                        {colleagueCell(row)}
                      </TableCell>
                      <TableCell className={COLUMN_CLASS.requested}>
                        {requestedCell(row)}
                      </TableCell>
                      <TableCell className={COLUMN_CLASS.outcome}>
                        {outcomeCell(row)}
                      </TableCell>
                      {showActions && (
                        <TableCell className={COLUMN_CLASS.actions}>
                          {actionsCell(row)}
                        </TableCell>
                      )}
                    </TableRow>
                    {inlineRowOf(row)}
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "pathology.empty.noConsultation" })}
          />
        )}
      </TableContainer>
    </div>
  );
};

export default ConsultationsPanel;
