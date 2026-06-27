import React, { useEffect, useState } from "react";
import { InlineNotification, Loading, Stack, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import AstEntryPanel from "./AstEntryPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import CriticalCommunicationPanel from "./CriticalCommunicationPanel";
import IsolatePanel from "./IsolatePanel";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyService from "./MicrobiologyService";
import ReportReadinessPanel from "./ReportReadinessPanel";
import "./MicrobiologyCaseView.css";

const progressItems = [
  { id: "case-info", labelId: "microbiology.progress.caseInfo" },
  { id: "setup", labelId: "microbiology.progress.inoculation" },
  { id: "timeline", labelId: "microbiology.progress.timeline" },
  { id: "isolates", labelId: "microbiology.progress.isolates" },
  { id: "ast", labelId: "microbiology.progress.ast" },
  { id: "review", labelId: "microbiology.progress.review" },
  { id: "reports", labelId: "microbiology.progress.reports" },
];

const hasActivity = (caseDetail, activityType) =>
  (caseDetail.activities || []).some(
    (activity) => activity.activityType === activityType,
  );

const getProgressStatus = (caseDetail, itemId) => {
  const hasIsolate = (caseDetail.isolates || []).length > 0;
  const astReviewed = hasActivity(caseDetail, "AST_REVIEWED");
  const finalReleased = caseDetail.stage === "FINAL_RELEASED";
  const statusByItem = {
    "case-info": "done",
    setup: caseDetail.stage !== "RECEIVED" ? "done" : "current",
    timeline: caseDetail.stage !== "RECEIVED" ? "done" : "todo",
    isolates: hasIsolate
      ? "done"
      : caseDetail.stage !== "RECEIVED"
        ? "current"
        : "todo",
    ast: astReviewed ? "done" : hasIsolate ? "current" : "todo",
    review: astReviewed ? "done" : hasIsolate ? "todo" : "todo",
    reports: finalReleased ? "done" : astReviewed ? "current" : "todo",
  };
  return statusByItem[itemId] || "todo";
};

const getNextStepMessageId = (caseDetail) => {
  if (caseDetail.stage === "FINAL_RELEASED") {
    return "microbiology.next.finalReleased";
  }
  if (!hasActivity(caseDetail, "STAGE_CHANGED")) {
    return "microbiology.next.recordSetup";
  }
  if ((caseDetail.isolates || []).length === 0) {
    return "microbiology.next.createIsolate";
  }
  if (!hasActivity(caseDetail, "AST_REVIEWED")) {
    return "microbiology.next.reviewAst";
  }
  return "microbiology.next.release";
};

const MicrobiologyCaseView = ({
  caseId: caseIdProp,
  service = MicrobiologyService,
}) => {
  const intl = useIntl();
  const params = useParams();
  const caseId = caseIdProp || params.caseId;
  const [caseDetail, setCaseDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [readinessRefreshToken, setReadinessRefreshToken] = useState(0);

  const loadCase = ({ showLoading = true } = {}) => {
    if (showLoading) {
      setLoading(true);
    }
    service.getCaseDetail(caseId).then((detail) => {
      if (!detail || detail.status) {
        setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
        setCaseDetail(null);
      } else {
        setError("");
        setCaseDetail(detail);
      }
      if (showLoading) {
        setLoading(false);
      }
    });
  };

  useEffect(() => {
    loadCase();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId]);

  const recordActivity = (payload) => {
    setSaving(true);
    service.recordCaseActivity(caseId, payload).then((detail) => {
      setCaseDetail(detail);
      setSaving(false);
    });
  };

  const createIsolate = (payload) => {
    setSaving(true);
    service.createIsolate(payload).then(() => {
      service.getCaseDetail(caseId).then((detail) => {
        setCaseDetail(detail);
        setSaving(false);
      });
    });
  };

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        title={intl.formatMessage({ id: "microbiology.case.error" })}
        subtitle={error}
        hideCloseButton
      />
    );
  }

  return (
    <main
      className="microbiology-workbench"
      data-testid="microbiology-case-view"
    >
      <header className="microbiology-workbench__hero">
        <div>
          <p className="microbiology-workbench__eyebrow">
            {intl.formatMessage({ id: "microbiology.case.eyebrow" })}
          </p>
          <h2>{intl.formatMessage({ id: "microbiology.case.title" })}</h2>
          <div className="microbiology-workbench__meta">
            <span>
              {intl.formatMessage({ id: "microbiology.case.sampleItem" })}:{" "}
              <strong>{caseDetail.sampleItemId}</strong>
            </span>
            <span>
              {intl.formatMessage({ id: "microbiology.case.workflow" })}:{" "}
              <strong>{formatMicrobiologyEnum(caseDetail.workflowType)}</strong>
            </span>
          </div>
        </div>
        <Tag type={caseDetail.stage === "FINAL_RELEASED" ? "green" : "blue"}>
          {formatMicrobiologyEnum(caseDetail.stage)}
        </Tag>
      </header>

      <div className="microbiology-workbench__layout">
        <aside
          className="microbiology-workbench__rail"
          data-testid="microbiology-progress-rail"
          aria-label={intl.formatMessage({ id: "microbiology.progress.title" })}
        >
          <h3>{intl.formatMessage({ id: "microbiology.progress.title" })}</h3>
          <ol className="microbiology-progress">
            {progressItems.map((item, index) => {
              const status = getProgressStatus(caseDetail, item.id);
              return (
                <li
                  key={item.id}
                  className={`microbiology-progress__item microbiology-progress__item--${status}`}
                >
                  <span className="microbiology-progress__marker">
                    {status === "done" ? "\u2713" : index + 1}
                  </span>
                  <span>{intl.formatMessage({ id: item.labelId })}</span>
                </li>
              );
            })}
          </ol>
        </aside>

        <div className="microbiology-workbench__content">
          <section className="microbiology-next-step">
            <div>
              <p className="microbiology-workbench__eyebrow">
                {intl.formatMessage({ id: "microbiology.next.title" })}
              </p>
              <p>
                {intl.formatMessage({ id: getNextStepMessageId(caseDetail) })}
              </p>
            </div>
          </section>
          <section
            className="microbiology-case-summary"
            data-testid="microbiology-case-summary"
            aria-label={intl.formatMessage({
              id: "microbiology.progress.caseInfo",
            })}
          >
            <strong>
              {intl.formatMessage({ id: "microbiology.progress.caseInfo" })}
            </strong>
            <span>
              {intl.formatMessage({ id: "microbiology.case.sampleItem" })}:{" "}
              {caseDetail.sampleItemId}
            </span>
            <span>
              {intl.formatMessage({ id: "microbiology.case.workflow" })}:{" "}
              {formatMicrobiologyEnum(caseDetail.workflowType)}
            </span>
          </section>
          <Stack gap={5}>
            <CaseTimelinePanel
              activities={caseDetail.activities}
              onRecordActivity={recordActivity}
              saving={saving}
            />
            <IsolatePanel
              caseId={caseDetail.id}
              isolates={caseDetail.isolates}
              onCreateIsolate={createIsolate}
              saving={saving}
            />
            <AstEntryPanel
              caseId={caseDetail.id}
              workflowType={caseDetail.workflowType}
              isolates={caseDetail.isolates}
              service={service}
              saving={saving}
              onAstUpdated={() =>
                setReadinessRefreshToken((currentValue) => currentValue + 1)
              }
            />
            <CriticalCommunicationPanel
              caseId={caseDetail.id}
              service={service}
            />
            <ReportReadinessPanel
              caseId={caseDetail.id}
              service={service}
              finalReleaseState={caseDetail.stage}
              onReleased={() => loadCase({ showLoading: false })}
              refreshToken={readinessRefreshToken}
            />
          </Stack>
        </div>
      </div>
    </main>
  );
};

export default MicrobiologyCaseView;
