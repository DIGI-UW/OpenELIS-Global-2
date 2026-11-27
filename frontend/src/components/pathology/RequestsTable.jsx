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
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import SearchableSelect from "../common/SearchableSelect";
import { RequiredMarker, requiredProps } from "../common/RequiredMarker";
import StatusBadge from "../caseView/StatusBadge";
import {
  blockIdentifier,
  isDeactivatedRow,
  isUnsavedRow,
  slideOnBlockName,
} from "./pathologyRows";
import {
  requestDraftBlockedReason,
  requestDraftBody,
  requestStatusBody,
} from "./pathologySections";
import { focusFirstOf } from "./focus";
import "./pathologyCaseView.scss";

/** The button that opens a new request, where focus lands after a change. */
export const NEW_REQUEST_BUTTON_ID = "newRequest";

/** The instruction of the new request, focused when a row elsewhere opens it. */
export const REQUEST_INSTRUCTION_ID = "requestInstruction";

/** The line above the table that says why every request action is held. */
export const REQUESTS_HOLD_ID = "requestsHold";

const RAISE_BUTTON_ID = "raiseRequest";
const RAISE_HINT_ID = "raiseRequestHint";
const KIND_RADIO_ID = (kind) => "requestKind-" + kind;

// The columns an instruction and a cancellation's reason are stored in.
const REQUEST_TEXT_MAX_LENGTH = 255;

// i18n-keys: pathology.requestKind.*
const KIND_KEYS = Object.freeze({
  BLOCK: "pathology.requestKind.block",
  SLIDE: "pathology.requestKind.slide",
  STAIN: "pathology.requestKind.stain",
});

const PRIORITIES = Object.freeze([
  { value: "NORMAL", key: "common.normal" },
  { value: "URGENT", key: "pathology.priority.urgent" },
  { value: "STAT", key: "pathology.priority.stat" },
]);

const STATUS_BADGES = Object.freeze({
  OPENED: { kind: "pending", textKey: "pathology.requestStatus.opened" },
  COMPLETED: { kind: "complete", textKey: "common.completed" },
  CANCELLED: { kind: "none", textKey: "pathology.requestStatus.cancelled" },
});

const emptyDraft = () => ({
  kind: "SLIDE",
  targetId: "",
  stainId: "",
  priority: "NORMAL",
  instruction: "",
});

const savedInUse = (rows) =>
  (rows ?? []).filter((row) => !isUnsavedRow(row) && !isDeactivatedRow(row));

// The new request and the Complete and Cancel request rows are forms laid
// over the table, not requests, so they are not highlighted under the
// pointer (see the stylesheet).
const FORM_ROW_CLASS = "pathology-case-view__request-form-row";

// Each column's cells carry a class of their own, so the stylesheet can give
// the instruction the room and keep the rest compact.
const columnClass = (key) => "pathology-case-view__request-col--" + key;

// Confirm on a request's own row says the word it shows and then, for a
// screen reader moving through the table, which request it confirms.
const ConfirmLabel = ({ instruction }) => (
  <>
    <FormattedMessage id="common.confirm" />
    <span className="cds--visually-hidden"> {instruction}</span>
  </>
);

/**
 * The bench requests on a case (FR-10.1 to FR-10.9): what was asked, of
 * which block or slide, how urgently, by whom and when, and where it stands.
 * The rows are the case read's, newest first. Every change is handed to the
 * caller, which posts it and reads the case again, and resolves `{ ok }` once
 * that read has landed.
 *
 * `canRaise` offers New request, which opens the request as the table's first
 * row; `canChangeStatus` offers Complete and Cancel request on an open row and
 * Reopen on a closed one. Complete on a request with a kind, and Cancel
 * request, open a row under the request for what fulfilled it (optional) or
 * the reason (required); the rest act at once. The new request and those
 * rows keep their own state, outside the case form.
 *
 * `lock` holds every change (requestActionLock), and its reason is written
 * once above the table. `held` is a change still out: the rows and what was
 * typed into them stay on the page, disabled, so neither the text nor focus
 * is lost while the case is read again, and a refused change can be sent
 * again as it stands. `prefill` (`{ kind, targetId, nonce }`) opens the new
 * request with its kind and target set, each time its nonce changes, and
 * puts focus on the instruction.
 */
const RequestsTable = ({
  requests,
  blocks,
  slides,
  tissueTypeNames,
  stains,
  canRaise,
  canChangeStatus,
  lock,
  held,
  prefill,
  onRaise,
  onChangeStatus,
}) => {
  const intl = useIntl();
  const mounted = useRef(true);
  const [draftOpen, setDraftOpen] = useState(false);
  const [draft, setDraft] = useState(emptyDraft);
  // The one request whose Complete or Cancel request row is open.
  const [inline, setInline] = useState(null);
  const [resultId, setResultId] = useState("");
  const [cancelReason, setCancelReason] = useState("");

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!prefill || !canRaise) {
      return;
    }
    setDraftOpen(true);
    setDraft((current) => ({
      ...current,
      kind: prefill.kind,
      targetId: String(prefill.targetId ?? ""),
      stainId: "",
    }));
    focusFirstOf("#" + REQUEST_INSTRUCTION_ID);
  }, [prefill?.nonce]); // eslint-disable-line react-hooks/exhaustive-deps

  const rows = requests ?? [];
  const showActions = canChangeStatus;
  const columnCount = showActions ? 5 : 4;
  const lockText =
    lock.disabled && lock.reasonKey
      ? intl.formatMessage({ id: lock.reasonKey })
      : null;
  const holdProps = {
    disabled: lock.disabled,
    title: lockText ?? undefined,
    "aria-describedby": lockText ? REQUESTS_HOLD_ID : undefined,
  };

  const blockOptions = savedInUse(blocks).map((block) => {
    const tissue = tissueTypeNames?.[block.tissueTypeId];
    return {
      id: String(block.id),
      value: tissue
        ? intl.formatMessage(
            { id: "pathology.label.blockWithTissue" },
            { designation: blockIdentifier(block), tissue },
          )
        : blockIdentifier(block),
    };
  });
  const slideOptions = savedInUse(slides).map((slide) => ({
    id: String(slide.id),
    value: slideOnBlockName(slide, blocks),
  }));
  const resultOptions = (request) =>
    request.kind === "BLOCK"
      ? savedInUse(blocks).map((block) => ({
          id: String(block.id),
          value: blockIdentifier(block),
        }))
      : slideOptions;

  const patchDraft = (patch) =>
    setDraft((current) => ({ ...current, ...patch }));

  const toggleDraft = () => {
    if (draftOpen) {
      setDraftOpen(false);
      return;
    }
    setDraftOpen(true);
    focusFirstOf("#" + KIND_RADIO_ID(draft.kind));
  };

  const closeDraft = () => {
    setDraftOpen(false);
    setDraft(emptyDraft());
    focusFirstOf("#" + NEW_REQUEST_BUTTON_ID);
  };

  const raise = async (event) => {
    const outcome = await onRaise(requestDraftBody(draft), event.currentTarget);
    if (outcome?.ok && mounted.current) {
      setDraftOpen(false);
      setDraft(emptyDraft());
    }
  };

  const openInline = (request, mode) => {
    setInline({ id: request.id, mode });
    setResultId("");
    setCancelReason("");
    focusFirstOf(
      "#" +
        (mode === "complete"
          ? "requestFulfilledBy-" + request.id
          : "requestCancelReason-" + request.id),
    );
  };

  const closeInline = (request, startedBy) => {
    setInline(null);
    focusFirstOf("#" + startedBy + "-" + request.id);
  };

  const changeStatus = async (request, status, button, extra) => {
    const outcome = await onChangeStatus(
      request,
      requestStatusBody(request, status, extra),
      button,
    );
    if (outcome?.ok && mounted.current) {
      setInline(null);
    }
  };

  const complete = (request, event) => {
    if (request.kind) {
      openInline(request, "complete");
      return;
    }
    changeStatus(request, "COMPLETED", event.currentTarget);
  };

  const draftReason = lockText
    ? null
    : (requestDraftBlockedReason(draft)?.key ?? null);
  const raiseBlockedText =
    lockText ?? (draftReason ? intl.formatMessage({ id: draftReason }) : null);

  const newRequestRow = () => (
    <TableRow data-testid="newRequestRow" className={FORM_ROW_CLASS}>
      <TableCell colSpan={columnCount}>
        <div className="pathology-case-view__row pathology-case-view__request-form">
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "pathology.label.requestKind",
            })}
            name="requestKind"
            valueSelected={draft.kind}
            disabled={held}
            onChange={(kind) =>
              patchDraft({ kind: String(kind), targetId: "", stainId: "" })
            }
          >
            {Object.entries(KIND_KEYS).map(([kind, key]) => (
              <RadioButton
                key={kind}
                id={KIND_RADIO_ID(kind)}
                value={kind}
                labelText={intl.formatMessage({ id: key })}
              />
            ))}
          </RadioButtonGroup>
          {draft.kind !== "BLOCK" && (
            <div className="pathology-case-view__row-field">
              <SearchableSelect
                id="requestTarget"
                size="sm"
                required
                labelText={intl.formatMessage({
                  id: "pathology.label.requestTarget",
                })}
                placeholder={intl.formatMessage({ id: "common.select" })}
                options={draft.kind === "STAIN" ? slideOptions : blockOptions}
                value={draft.targetId}
                disabled={held}
                onChange={(targetId) => patchDraft({ targetId })}
              />
            </div>
          )}
          {draft.kind === "STAIN" && (
            <div className="pathology-case-view__row-field">
              <SearchableSelect
                id="requestStain"
                size="sm"
                required
                labelText={intl.formatMessage({ id: "pathology.label.stain" })}
                placeholder={intl.formatMessage({ id: "common.select" })}
                options={stains ?? []}
                value={draft.stainId}
                disabled={held || !Array.isArray(stains)}
                onChange={(stainId) => patchDraft({ stainId })}
              />
            </div>
          )}
          <div className="pathology-case-view__row-field">
            <Select
              id="requestPriority"
              size="sm"
              labelText={intl.formatMessage({ id: "common.priority" })}
              value={draft.priority}
              disabled={held}
              onChange={(event) => patchDraft({ priority: event.target.value })}
            >
              {PRIORITIES.map((priority) => (
                <SelectItem
                  key={priority.value}
                  value={priority.value}
                  text={intl.formatMessage({ id: priority.key })}
                />
              ))}
            </Select>
          </div>
          <div className="pathology-case-view__request-instruction">
            <TextArea
              id={REQUEST_INSTRUCTION_ID}
              rows={2}
              labelText={
                <>
                  {intl.formatMessage({
                    id: "pathology.label.requestInstruction",
                  })}
                  <RequiredMarker />
                </>
              }
              {...requiredProps()}
              maxLength={REQUEST_TEXT_MAX_LENGTH}
              enableCounter
              maxCount={REQUEST_TEXT_MAX_LENGTH}
              value={draft.instruction}
              disabled={held}
              onChange={(event) =>
                patchDraft({ instruction: event.target.value })
              }
            />
          </div>
          <div className="pathology-case-view__request-buttons">
            <Button
              kind="primary"
              size="sm"
              id={RAISE_BUTTON_ID}
              disabled={Boolean(raiseBlockedText)}
              title={raiseBlockedText ?? undefined}
              aria-describedby={raiseBlockedText ? RAISE_HINT_ID : undefined}
              onClick={raise}
            >
              <FormattedMessage id="pathology.action.raiseRequest" />
            </Button>
            {/* Held while the request is out: closing the row then would
                drop focus while the case is still read again. */}
            <Button
              kind="ghost"
              size="sm"
              id="raiseRequestCancel"
              disabled={held}
              onClick={closeDraft}
            >
              <FormattedMessage id="common.cancel" />
            </Button>
            {/* A disabled button takes no focus and shows its title to a
                mouse only, so the reason is also written out beside it. */}
            {raiseBlockedText && (
              <span id={RAISE_HINT_ID} className="case-view__locked-hint">
                {raiseBlockedText}
              </span>
            )}
          </div>
        </div>
      </TableCell>
    </TableRow>
  );

  const completeRow = (request) => (
    <TableRow
      data-testid={"requestComplete-" + request.id}
      className={FORM_ROW_CLASS}
    >
      <TableCell colSpan={columnCount}>
        <div className="pathology-case-view__row pathology-case-view__request-form">
          <div className="pathology-case-view__row-field">
            <SearchableSelect
              id={"requestFulfilledBy-" + request.id}
              size="sm"
              labelText={intl.formatMessage({
                id: "pathology.label.fulfilledByOptional",
              })}
              placeholder={intl.formatMessage({ id: "common.select" })}
              options={resultOptions(request)}
              value={resultId}
              disabled={held}
              onChange={setResultId}
            />
          </div>
          <div className="pathology-case-view__request-buttons">
            <Button
              kind="primary"
              size="sm"
              id={"requestCompleteConfirm-" + request.id}
              {...holdProps}
              onClick={(event) =>
                changeStatus(request, "COMPLETED", event.currentTarget, {
                  resultId,
                })
              }
            >
              <ConfirmLabel instruction={request.instruction} />
            </Button>
            <Button
              kind="ghost"
              size="sm"
              id={"requestCompleteCancel-" + request.id}
              disabled={held}
              onClick={() => closeInline(request, "completeRequest")}
            >
              <FormattedMessage id="common.cancel" />
            </Button>
          </div>
        </div>
      </TableCell>
    </TableRow>
  );

  const cancelRow = (request) => {
    const hintId = "requestCancelHint-" + request.id;
    const blockedText =
      lockText ??
      (cancelReason.trim() === ""
        ? intl.formatMessage({
            id: "pathology.locked.requestCancelReasonRequired",
          })
        : null);
    return (
      <TableRow
        data-testid={"requestCancel-" + request.id}
        className={FORM_ROW_CLASS}
      >
        <TableCell colSpan={columnCount}>
          <div className="pathology-case-view__row pathology-case-view__request-form">
            <div className="pathology-case-view__request-instruction">
              <TextInput
                id={"requestCancelReason-" + request.id}
                size="sm"
                labelText={
                  <>
                    {intl.formatMessage({ id: "common.reason" })}
                    <RequiredMarker />
                  </>
                }
                {...requiredProps()}
                helperText={intl.formatMessage({
                  id: "pathology.label.returnReasonHelper",
                })}
                maxLength={REQUEST_TEXT_MAX_LENGTH}
                enableCounter
                maxCount={REQUEST_TEXT_MAX_LENGTH}
                value={cancelReason}
                disabled={held}
                onChange={(event) => setCancelReason(event.target.value)}
              />
            </div>
            <div className="pathology-case-view__request-buttons">
              <Button
                kind="primary"
                size="sm"
                id={"requestCancelConfirm-" + request.id}
                disabled={Boolean(blockedText)}
                title={blockedText ?? undefined}
                aria-describedby={blockedText ? hintId : undefined}
                onClick={(event) =>
                  changeStatus(request, "CANCELLED", event.currentTarget, {
                    cancelReason,
                  })
                }
              >
                <ConfirmLabel instruction={request.instruction} />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                id={"requestCancelKeep-" + request.id}
                disabled={held}
                aria-label={intl.formatMessage(
                  { id: "pathology.action.keepRequestOf" },
                  { name: request.instruction },
                )}
                onClick={() => closeInline(request, "cancelRequest")}
              >
                <FormattedMessage id="pathology.action.keepRequest" />
              </Button>
              {blockedText && (
                <span id={hintId} className="case-view__locked-hint">
                  {blockedText}
                </span>
              )}
            </div>
          </div>
        </TableCell>
      </TableRow>
    );
  };

  const rowAction = (request, id, labelKey, nameKey, onClick) => (
    <Button
      kind="ghost"
      size="sm"
      id={id + "-" + request.id}
      {...holdProps}
      aria-label={intl.formatMessage(
        { id: nameKey },
        { name: request.instruction },
      )}
      onClick={onClick}
    >
      <FormattedMessage id={labelKey} />
    </Button>
  );

  const requestCell = (request) => {
    if (!request.kind) {
      return <FormattedMessage id="common.notRecorded" />;
    }
    return (
      <div className="pathology-case-view__slide-status">
        <Tag type="outline" size="sm">
          {intl.formatMessage({ id: KIND_KEYS[request.kind] })}
        </Tag>
        {request.targetName && <span>{request.targetName}</span>}
        {request.stainName && <span>{request.stainName}</span>}
        {request.priority === "NORMAL" && (
          <span className="pathology-case-view__slide-stamp">
            <FormattedMessage id="common.normal" />
          </span>
        )}
        {(request.priority === "URGENT" || request.priority === "STAT") && (
          <StatusBadge
            kind="critical"
            textKey={
              request.priority === "URGENT"
                ? "pathology.priority.urgent"
                : "pathology.priority.stat"
            }
          />
        )}
      </div>
    );
  };

  const statusCell = (request) => {
    const badge = STATUS_BADGES[request.status];
    return (
      <div className="pathology-case-view__slide-status">
        {badge ? (
          <StatusBadge kind={badge.kind} textKey={badge.textKey} />
        ) : (
          <FormattedMessage id="common.notRecorded" />
        )}
        {request.status === "CANCELLED" && request.cancelReason && (
          <span className="pathology-case-view__slide-stamp">
            {request.cancelReason}
          </span>
        )}
        {request.status === "COMPLETED" && request.resultName && (
          <span className="pathology-case-view__slide-stamp">
            <FormattedMessage
              id="pathology.label.fulfilledByName"
              values={{ name: request.resultName }}
            />
          </span>
        )}
      </div>
    );
  };

  const raisedCell = (request) =>
    request.raisedAt ? (
      <div className="pathology-case-view__slide-status">
        <span>{request.raisedAt}</span>
        {request.raisedBy && (
          <span className="pathology-case-view__slide-stamp">
            {request.raisedBy}
          </span>
        )}
      </div>
    ) : (
      <FormattedMessage id="common.notRecorded" />
    );

  const actionsCell = (request) => (
    <div className="pathology-case-view__slide-actions">
      {request.status === "OPENED" ? (
        <>
          {rowAction(
            request,
            "completeRequest",
            "common.complete",
            "pathology.action.completeRequestOf",
            (event) => complete(request, event),
          )}
          {rowAction(
            request,
            "cancelRequest",
            "pathology.action.cancelRequest",
            "pathology.action.cancelRequestOf",
            () => openInline(request, "cancel"),
          )}
        </>
      ) : (
        rowAction(
          request,
          "reopenRequest",
          "pathology.action.reopenRequest",
          "pathology.action.reopenRequestOf",
          (event) => changeStatus(request, "OPENED", event.currentTarget),
        )
      )}
    </div>
  );

  const inlineRowOf = (request) => {
    if (
      !showActions ||
      request.status !== "OPENED" ||
      inline?.id !== request.id
    ) {
      return null;
    }
    return inline.mode === "complete"
      ? completeRow(request)
      : cancelRow(request);
  };

  const tableShown = rows.length > 0 || (canRaise && draftOpen);
  const title = intl.formatMessage({ id: "pathology.label.benchRequests" });

  return (
    <div className="pathology-case-view__requests">
      <TableContainer title={title}>
        {canRaise && (
          <TableToolbar aria-label={title}>
            <TableToolbarContent>
              <Button
                kind="primary"
                size="sm"
                id={NEW_REQUEST_BUTTON_ID}
                aria-expanded={draftOpen}
                disabled={held}
                onClick={toggleDraft}
              >
                <FormattedMessage id="pathology.action.requestWork" />
              </Button>
            </TableToolbarContent>
          </TableToolbar>
        )}
        {/* The reason every request action is held, written once where a
            sighted keyboard user can read it, and named by every button it
            holds. */}
        {lockText && (canRaise || canChangeStatus) && (
          <p id={REQUESTS_HOLD_ID} className="case-view__locked-hint">
            {lockText}
          </p>
        )}
        {tableShown ? (
          <div
            id="requestsRegion"
            className="pathology-case-view__slide-scroll"
            role="region"
            aria-label={title}
            tabIndex={0}
          >
            <Table size="md" className="pathology-case-view__slide-table">
              <TableHead>
                <TableRow>
                  <TableHeader className={columnClass("request")}>
                    {intl.formatMessage({
                      id: "pathology.label.requestColumn",
                    })}
                  </TableHeader>
                  <TableHeader className={columnClass("instruction")}>
                    {intl.formatMessage({
                      id: "pathology.label.requestInstruction",
                    })}
                  </TableHeader>
                  <TableHeader className={columnClass("raised")}>
                    {intl.formatMessage({ id: "pathology.label.raised" })}
                  </TableHeader>
                  <TableHeader className={columnClass("status")}>
                    {intl.formatMessage({ id: "common.status" })}
                  </TableHeader>
                  {showActions && (
                    <TableHeader className={columnClass("actions")}>
                      {intl.formatMessage({ id: "common.actions" })}
                    </TableHeader>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {canRaise && draftOpen && newRequestRow()}
                {rows.map((request) => (
                  <React.Fragment key={request.id}>
                    <TableRow data-testid={"requestRow-" + request.id}>
                      <TableCell className={columnClass("request")}>
                        {requestCell(request)}
                      </TableCell>
                      <TableCell className={columnClass("instruction")}>
                        {request.instruction}
                      </TableCell>
                      <TableCell className={columnClass("raised")}>
                        {raisedCell(request)}
                      </TableCell>
                      <TableCell className={columnClass("status")}>
                        {statusCell(request)}
                      </TableCell>
                      {showActions && (
                        <TableCell className={columnClass("actions")}>
                          {actionsCell(request)}
                        </TableCell>
                      )}
                    </TableRow>
                    {inlineRowOf(request)}
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
            title={intl.formatMessage({ id: "pathology.empty.noRequests" })}
          />
        )}
      </TableContainer>
    </div>
  );
};

export default RequestsTable;
