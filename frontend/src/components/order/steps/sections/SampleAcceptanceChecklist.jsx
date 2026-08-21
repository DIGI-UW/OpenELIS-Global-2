import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Tile,
  Tag,
  Button,
  RadioButtonGroup,
  RadioButton,
  TextArea,
  InlineNotification,
  InlineLoading,
} from "@carbon/react";
import { Warning, Renew, Close } from "@carbon/icons-react";
import { useIntl, FormattedMessage } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { Roles } from "../../../utils/Utils";
import {
  ANSWER,
  getSampleItemEvaluation,
  recordAssessment,
  recordPoolAssessment,
} from "../../api/sampleAcceptanceApi";
import InlineNceForm from "../../../nonconform/common/InlineNceForm";
import ResampleDialog from "./ResampleDialog";
import RejectDialog from "./RejectDialog";
import "./SampleAcceptanceChecklist.css";

/**
 * SampleAcceptanceChecklist — the S-09 (OGC-580) pre-analytical intake
 * acceptance surface, run per specimen at the Collect (reception) step.
 * Embedded in each SampleCollectionCard, so the receiving tech accepts /
 * reviews each specimen individually before it goes to the bench.
 *
 * It is domain-agnostic by design: the resolved checklist and the order's
 * domain both come from the backend (GET /sample-item/{id}), so no domain
 * logic lives here. Acceptance is recorded per sample_item.
 *
 * Props:
 *  - sampleItemId: backend sample_item(id); empty until the specimen is saved
 *                  at Collect (then the checklist becomes actionable)
 *  - labNumber:    accession, for display + NCE/resample context
 *  - collectedAt / receivedAt: optional ISO-ish strings (this specimen's live
 *                  collect-form values) for the read-only transit context block
 *                  (transit never auto-grades) — fixes CG-7
 *  - onBlockedChange(blocked): lifts the Mandatory-enforcement gate state to
 *                  the host so the Collect → Label & Store step can react
 */
const STATUS = { ACCEPTED: "ACCEPTED", REVIEW: "REVIEW", PENDING: "PENDING" };
const LONG_TRANSIT_MINUTES = 24 * 60;

const DOMAIN_LABEL_ID = {
  CLINICAL: "sampleAcceptance.domain.clinical",
  ENVIRONMENTAL: "sampleAcceptance.domain.environmental",
  VECTOR: "sampleAcceptance.domain.vector",
};

/** Transit duration as "Xh Ym", or null when either timestamp is unusable. */
export const computeTransitMinutes = (collectedAt, receivedAt) => {
  if (!collectedAt || !receivedAt) return null;
  const start = new Date(collectedAt).getTime();
  const end = new Date(receivedAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) return null;
  return Math.round((end - start) / 60000);
};

export const formatTransit = (minutes) => {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
};

const SampleAcceptanceChecklist = ({
  sampleItemId,
  vectorPoolId,
  labNumber,
  collectedAt,
  receivedAt,
  onBlockedChange,
  onEvaluation,
  onAccepted,
  onRejected,
  rejected = false,
}) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [evaluation, setEvaluation] = useState(null);
  const [answers, setAnswers] = useState({}); // itemKey -> { answer, note }
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showNce, setShowNce] = useState(false);
  const [showResample, setShowResample] = useState(false);
  const [showReject, setShowReject] = useState(false);
  const [newAccession, setNewAccession] = useState(null);

  const items = evaluation?.items || [];
  const domain = evaluation?.domain || null;
  const enforcement = evaluation?.enforcement || null;
  // Vector is the pool-acceptance domain: the pool is the unit, rejected catches
  // are terminal (no resample / re-collection).
  const poolMode = !!vectorPoolId;
  const isVector = domain === "VECTOR";

  const seedAnswers = useCallback((latest) => {
    const seeded = {};
    (latest?.answers || []).forEach((a) => {
      seeded[a.itemKey] = { answer: a.answer || "", note: a.note || "" };
    });
    setAnswers(seeded);
  }, []);

  const load = useCallback(() => {
    if (!sampleItemId) return;
    setLoading(true);
    getSampleItemEvaluation(sampleItemId)
      .then((data) => {
        setEvaluation(data);
        seedAnswers(data.latest);
        onEvaluation?.(data);
      })
      .catch(() => setEvaluation(null))
      .finally(() => setLoading(false));
  }, [sampleItemId, seedAnswers, onEvaluation]);

  useEffect(() => {
    load();
  }, [load]);

  // ---- derived state -------------------------------------------------------

  const computeStatus = useCallback(() => {
    if (!items.length) return null;
    let answered = 0;
    let anyFail = false;
    items.forEach((it) => {
      const a = answers[it.itemKey]?.answer;
      if (a) answered += 1;
      if (a === ANSWER.FAIL) anyFail = true;
    });
    if (anyFail) return STATUS.REVIEW;
    if (answered < items.length) return STATUS.PENDING;
    return STATUS.ACCEPTED;
  }, [items, answers]);

  const status = computeStatus();
  const blocked = enforcement === "MANDATORY" && status !== STATUS.ACCEPTED;

  // Lift the gate state to the host (OrderQA blocks submit on it).
  useEffect(() => {
    onBlockedChange?.(!!blocked);
  }, [blocked, onBlockedChange]);

  /** Reason text built live from the currently-failed items (label + note). */
  const buildFailureReason = useCallback(() => {
    return items
      .filter((it) => answers[it.itemKey]?.answer === ANSWER.FAIL)
      .map((it) => {
        const label = it.localizedName || it.label || it.itemKey;
        const note = (answers[it.itemKey]?.note || "").trim();
        return `${label}: FAIL${note ? ` — ${note}` : ""}`;
      })
      .join("\n");
  }, [items, answers]);

  const canResample = (userSessionDetails?.roles || []).some((r) =>
    [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN].includes(r),
  );

  // ---- handlers ------------------------------------------------------------

  const setAnswer = (itemKey, answer) => {
    setAnswers((prev) => ({
      ...prev,
      [itemKey]: { answer, note: prev[itemKey]?.note || "" },
    }));
  };

  const setNote = (itemKey, note) => {
    setAnswers((prev) => ({
      ...prev,
      [itemKey]: { answer: prev[itemKey]?.answer || "", note },
    }));
  };

  const notify = (kind, messageId, values) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }, values),
    });
    setNotificationVisible(true);
  };

  const buildAnswerPayload = () =>
    items.map((it) => ({
      itemKey: it.itemKey,
      label: it.localizedName || it.label || it.itemKey,
      answer: answers[it.itemKey]?.answer || "",
      note: answers[it.itemKey]?.note || "",
    }));

  const handleAccept = () => {
    setSaving(true);
    const action = poolMode
      ? recordPoolAssessment(vectorPoolId, buildAnswerPayload())
      : recordAssessment(sampleItemId, buildAnswerPayload());
    action
      .then((refreshed) => {
        if (poolMode) {
          // The pool endpoint returns a representative MEMBER's evaluation whose
          // sampleItemId may differ from this row's representative; reload THIS
          // row's own eval so the status map/tag key on the row's representative
          // (not a sibling) and the tag flips without a wrong-key flicker.
          load();
        } else {
          setEvaluation(refreshed);
          seedAnswers(refreshed.latest);
          onEvaluation?.(refreshed);
        }
        // A pool accept changed every member server-side; let the host re-pull all
        // per-member statuses so the table tags + gate reflect the full cascade.
        onAccepted?.();
        notify(
          NotificationKinds.success,
          "sampleAcceptance.qa.accepted.success",
        );
      })
      .catch(() =>
        notify(NotificationKinds.error, "sampleAcceptance.notify.recordFailed"),
      )
      .finally(() => setSaving(false));
  };

  const handleResampleSuccess = (result) => {
    setNewAccession(result?.newAccessionNumber || null);
    notify(NotificationKinds.success, "sampleAcceptance.resample.success", {
      accession: result?.newAccessionNumber || "",
    });
    load();
  };

  const handleRejectSuccess = () => {
    notify(NotificationKinds.success, "sampleAcceptance.reject.success");
    onRejected?.();
    load();
  };

  // ---- render --------------------------------------------------------------

  if (!sampleItemId) {
    return (
      <Tile className="sac-tile">
        <h4>
          <FormattedMessage
            id="sampleAcceptance.qa.title"
            defaultMessage="Intake acceptance"
          />
        </h4>
        <p className="sac-subtitle">
          <FormattedMessage
            id="sampleAcceptance.qa.noSample"
            defaultMessage="Save collection to run intake acceptance for this specimen."
          />
        </p>
      </Tile>
    );
  }

  if (loading && !evaluation) {
    return (
      <Tile className="sac-tile">
        <InlineLoading
          description={intl.formatMessage({
            id: "label.loading",
            defaultMessage: "Loading...",
          })}
        />
      </Tile>
    );
  }

  const statusTag = rejected ? (
    <Tag type="red">
      <FormattedMessage
        id="sampleAcceptance.qa.status.rejected"
        defaultMessage="Rejected"
      />
    </Tag>
  ) : status === STATUS.ACCEPTED ? (
    <Tag type="green">
      <FormattedMessage
        id="sampleAcceptance.qa.status.accepted"
        defaultMessage="Accepted"
      />
    </Tag>
  ) : status === STATUS.REVIEW ? (
    <Tag type="warm-gray">
      <FormattedMessage
        id="sampleAcceptance.qa.status.review"
        defaultMessage="Review"
      />
    </Tag>
  ) : status === STATUS.PENDING ? (
    <Tag type="gray">
      <FormattedMessage
        id="sampleAcceptance.qa.status.pending"
        defaultMessage="Pending"
      />
    </Tag>
  ) : null;

  const transitMinutes = computeTransitMinutes(collectedAt, receivedAt);
  const domainLabel = domain
    ? intl.formatMessage({
        id: DOMAIN_LABEL_ID[domain] || "sampleAcceptance.domain.all",
        defaultMessage: domain,
      })
    : "";

  const resampledToSampleId = evaluation?.resample?.resampledToSampleId;
  const resampledFromSampleId = evaluation?.resample?.resampledFromSampleId;

  return (
    <Tile className="sac-tile">
      <div className="sac-header">
        <div>
          <h4>
            <FormattedMessage
              id="sampleAcceptance.qa.title"
              defaultMessage="Intake acceptance"
            />
            {labNumber ? ` — ${labNumber}` : ""}
          </h4>
          <p className="sac-subtitle">
            {rejected ? (
              isVector ? (
                <FormattedMessage
                  id="sampleAcceptance.qa.subtitle.rejectedVector"
                  defaultMessage="This catch was rejected — read-only."
                />
              ) : (
                <FormattedMessage
                  id="sampleAcceptance.qa.subtitle.rejected"
                  defaultMessage="This specimen was rejected and resampled — read-only."
                />
              )
            ) : (
              <FormattedMessage
                id="sampleAcceptance.qa.subtitle"
                defaultMessage="Complete the acceptance checklist for this sample"
              />
            )}
          </p>
        </div>
        {statusTag}
      </div>

      {/* Cross-reference banner (FR-11). Only on the specimen actually
          rejected/resampled — the rejected one (persisted), or the one just
          resampled in-place (newAccession set). The resampled_to link is
          order-level, so an unconditional check would misattribute the resample to
          a sibling (accepted) specimen. */}
      {(rejected || newAccession) && resampledToSampleId && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "sampleAcceptance.banner.resampledTo.title",
            defaultMessage: "Resampled",
          })}
          subtitle={intl.formatMessage(
            {
              id: "sampleAcceptance.banner.resampledTo",
              defaultMessage:
                "This sample was rejected and resampled. Replacement order: {accession}.",
            },
            { accession: newAccession || `#${resampledToSampleId}` },
          )}
        />
      )}
      {resampledFromSampleId && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "sampleAcceptance.banner.resampledFrom.title",
            defaultMessage: "Replacement sample",
          })}
          subtitle={intl.formatMessage(
            {
              id: "sampleAcceptance.banner.resampledFrom",
              defaultMessage:
                "This is a replacement created by a resample of sample #{id}.",
            },
            { id: resampledFromSampleId },
          )}
        />
      )}

      {/* Read-only context — transit never auto-grades (FRS) */}
      <div className="sac-context">
        <div className="sac-line">
          <strong>{domainLabel}</strong>
        </div>
        {(collectedAt || receivedAt) && (
          <div className="sac-line">
            <FormattedMessage
              id="sampleAcceptance.qa.context.collected"
              defaultMessage="Collected"
            />{" "}
            <strong>{collectedAt || "—"}</strong>
            {" · "}
            <FormattedMessage
              id="sampleAcceptance.qa.context.received"
              defaultMessage="Received"
            />{" "}
            <strong>{receivedAt || "—"}</strong>
          </div>
        )}
        {transitMinutes !== null && (
          <div className="sac-line">
            <FormattedMessage
              id="sampleAcceptance.qa.context.transit"
              defaultMessage="Transit time"
            />
            : <strong>{formatTransit(transitMinutes)}</strong>
            {transitMinutes > LONG_TRANSIT_MINUTES && (
              <Tag type="warm-gray" size="sm" className="sac-banner-tag">
                <FormattedMessage
                  id="sampleAcceptance.qa.context.longTransit"
                  defaultMessage="long transit — your call"
                />
              </Tag>
            )}
          </div>
        )}
        <div className="sac-context-note">
          <FormattedMessage
            id="sampleAcceptance.qa.context.note"
            defaultMessage="Transit is shown for reference only — it does not auto-grade anything. Active list: {domain}."
            values={{ domain: domainLabel }}
          />
        </div>
      </div>

      {/* Checklist items, or empty state */}
      {items.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title=""
          subtitle={intl.formatMessage({
            id: "sampleAcceptance.qa.empty",
            defaultMessage:
              "No acceptance checklist is configured for this domain.",
          })}
        />
      ) : (
        items.map((it) => {
          const itemLabel = it.localizedName || it.label || it.itemKey;
          const current = answers[it.itemKey]?.answer || "";
          return (
            <div className="sac-item" key={it.itemKey}>
              <div className="sac-item-label">
                <span>{itemLabel}</span>
              </div>
              <RadioButtonGroup
                name={`sac-item-${it.itemKey}`}
                legendText=""
                valueSelected={current}
                onChange={(value) => setAnswer(it.itemKey, value)}
                disabled={saving || rejected}
              >
                <RadioButton
                  id={`sac-${it.itemKey}-pass`}
                  labelText={intl.formatMessage({
                    id: "sampleAcceptance.qa.answer.pass",
                    defaultMessage: "Pass",
                  })}
                  value={ANSWER.PASS}
                />
                <RadioButton
                  id={`sac-${it.itemKey}-fail`}
                  labelText={intl.formatMessage({
                    id: "sampleAcceptance.qa.answer.fail",
                    defaultMessage: "Fail",
                  })}
                  value={ANSWER.FAIL}
                />
                <RadioButton
                  id={`sac-${it.itemKey}-na`}
                  labelText={intl.formatMessage({
                    id: "sampleAcceptance.qa.answer.na",
                    defaultMessage: "N/A",
                  })}
                  value={ANSWER.NA}
                />
              </RadioButtonGroup>
              {current === ANSWER.FAIL && (
                <div className="sac-note">
                  <TextArea
                    id={`sac-note-${it.itemKey}`}
                    labelText={intl.formatMessage({
                      id: "sampleAcceptance.qa.note.label",
                      defaultMessage: "Note (optional)",
                    })}
                    placeholder={intl.formatMessage({
                      id: "sampleAcceptance.qa.note.placeholder",
                      defaultMessage: "Optional — note any observed condition",
                    })}
                    rows={2}
                    value={answers[it.itemKey]?.note || ""}
                    onChange={(e) => setNote(it.itemKey, e.target.value)}
                    disabled={saving || rejected}
                  />
                </div>
              )}
            </div>
          );
        })
      )}

      {!rejected && status === STATUS.REVIEW && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title=""
          subtitle={intl.formatMessage({
            id: "sampleAcceptance.qa.failHint",
            defaultMessage:
              "This sample has failed checks — report an NCE or resample instead of accepting.",
          })}
        />
      )}

      {!rejected && items.length > 0 && (
        <div className="sac-actions">
          <Button
            kind="primary"
            disabled={status !== STATUS.ACCEPTED || saving}
            onClick={handleAccept}
          >
            <FormattedMessage
              id="sampleAcceptance.qa.button.accept"
              defaultMessage="Accept sample"
            />
          </Button>
          <Button
            kind="danger--tertiary"
            renderIcon={Warning}
            onClick={() => setShowNce((v) => !v)}
          >
            <FormattedMessage
              id="nce.button.reportNce"
              defaultMessage="Report NCE"
            />
          </Button>
          {canResample && !isVector && (
            <Button
              kind="tertiary"
              renderIcon={Renew}
              onClick={() => setShowResample(true)}
            >
              <FormattedMessage
                id="sampleAcceptance.qa.button.resample"
                defaultMessage="Resample"
              />
            </Button>
          )}
          {canResample && isVector && (
            <Button
              kind="danger--tertiary"
              renderIcon={Close}
              onClick={() => setShowReject(true)}
            >
              <FormattedMessage
                id="sampleAcceptance.qa.button.reject"
                defaultMessage="Reject"
              />
            </Button>
          )}
        </div>
      )}

      {showNce && (
        <InlineNceForm
          accessionNumber={labNumber}
          initialDescription={buildFailureReason()}
          onClose={() => setShowNce(false)}
          onSubmitSuccess={() => setShowNce(false)}
        />
      )}

      {showResample && (
        <ResampleDialog
          sampleItemId={sampleItemId}
          labNumber={labNumber}
          initialReason={buildFailureReason()}
          onClose={() => setShowResample(false)}
          onSuccess={handleResampleSuccess}
        />
      )}

      {showReject && (
        <RejectDialog
          vectorPoolId={vectorPoolId}
          sampleItemId={sampleItemId}
          labNumber={labNumber}
          initialReason={buildFailureReason()}
          onClose={() => setShowReject(false)}
          onSuccess={handleRejectSuccess}
        />
      )}
    </Tile>
  );
};

export default SampleAcceptanceChecklist;
