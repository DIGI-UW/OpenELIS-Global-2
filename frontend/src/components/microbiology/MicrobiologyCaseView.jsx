import React, { useEffect, useState } from "react";
import {
  Accordion,
  AccordionItem,
  InlineNotification,
  Layer,
  Loading,
  ProgressIndicator,
  ProgressStep,
  Stack,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import AmendmentHistoryPanel from "./AmendmentHistoryPanel";
import AstEntryPanel from "./AstEntryPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import CriticalCommunicationPanel from "./CriticalCommunicationPanel";
import IsolatePanel from "./IsolatePanel";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  parseMicrobiologyCaseSearch,
} from "./MicrobiologyRoutes";
import {
  markMicrobiologyReady,
  MICROBIOLOGY_CASE_READY_MARK,
} from "./MicrobiologyPerformance";
import MicrobiologyService from "./MicrobiologyService";
import OrderDetailPanel from "./OrderDetailPanel";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ReportReadinessPanel from "./ReportReadinessPanel";
import "./MicrobiologyCaseView.css";

const progressItems = [
  {
    id: "case-info",
    section: "case-info",
    labelId: "microbiology.progress.caseInfo",
  },
  {
    id: "setup",
    section: "setup",
    labelId: "microbiology.progress.inoculation",
  },
  {
    id: "timeline",
    section: "timeline",
    labelId: "microbiology.progress.timeline",
  },
  {
    id: "isolates",
    section: "isolates",
    labelId: "microbiology.progress.isolates",
  },
  { id: "ast", section: "ast", labelId: "microbiology.progress.ast" },
  { id: "review", section: "ast", labelId: "microbiology.progress.review" },
  {
    id: "critical-communication",
    section: "critical-communication",
    labelId: "microbiology.critical.title",
  },
  {
    id: "reports",
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
  {
    id: "amendment",
    section: "amendment",
    labelId: "microbiology.amendment.title",
  },
];

const hasActivity = (caseDetail, activityType) =>
  (caseDetail.activities || []).some(
    (activity) => activity.activityType === activityType,
  );

const getProgressStatus = (caseDetail, itemId) => {
  const hasIsolate = (caseDetail.isolates || []).length > 0;
  const astReviewed = hasActivity(caseDetail, "AST_REVIEWED");
  const finalReleased = caseDetail.stage === "FINAL_RELEASED";
  const noGrowthReady = caseDetail.stage === "NO_GROWTH_READY";
  const statusByItem = {
    "case-info": "done",
    setup: caseDetail.stage !== "RECEIVED" ? "done" : "current",
    timeline: caseDetail.stage !== "RECEIVED" ? "done" : "todo",
    isolates:
      hasIsolate || noGrowthReady
        ? "done"
        : caseDetail.stage !== "RECEIVED"
          ? "current"
          : "todo",
    ast:
      astReviewed || noGrowthReady ? "done" : hasIsolate ? "current" : "todo",
    review:
      astReviewed || noGrowthReady ? "done" : hasIsolate ? "todo" : "todo",
    reports: finalReleased ? "done" : astReviewed ? "current" : "todo",
    amendment:
      caseDetail.finalReleaseState === "AMENDMENT_IN_PROGRESS"
        ? "current"
        : "todo",
  };
  return statusByItem[itemId] || "todo";
};

const getNextStepMessageId = (caseDetail) => {
  if (caseDetail.finalReleaseState === "AMENDMENT_IN_PROGRESS") {
    return "microbiology.next.completeAmendment";
  }
  if (caseDetail.stage === "FINAL_RELEASED") {
    return "microbiology.next.finalReleased";
  }
  if (!hasActivity(caseDetail, "STAGE_CHANGED")) {
    return "microbiology.next.recordSetup";
  }
  if (caseDetail.stage === "NO_GROWTH_READY") {
    return "microbiology.next.release";
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
  const history = useHistory();
  const location = useLocation();
  const params = useParams();
  const caseId = caseIdProp || params.caseId;
  const routeState = parseMicrobiologyCaseSearch(location.search);
  const [caseDetail, setCaseDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [readinessRefreshToken, setReadinessRefreshToken] = useState(0);
  const [projectedResultIds, setProjectedResultIds] = useState([]);
  const [reagentOverview, setReagentOverview] = useState({
    requirements: [],
    usages: [],
  });
  const [actionError, setActionError] = useState("");

  const loadReagentOverview = () => {
    if (!service.getReagentLotOverview) {
      return Promise.resolve();
    }
    return service.getReagentLotOverview(caseId).then((overview) => {
      if (overview && !overview.status && !overview.error) {
        setReagentOverview({
          requirements: overview.requirements || [],
          usages: overview.usages || [],
        });
      }
    });
  };

  const loadCase = ({ showLoading = true } = {}) => {
    if (showLoading) {
      setLoading(true);
    }
    return service.getCaseDetail(caseId).then((detail) => {
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
    let active = true;

    Promise.all([
      service.getCaseDetail(caseId),
      service.getReportProjection(caseId),
      service.getReagentLotOverview
        ? service.getReagentLotOverview(caseId)
        : Promise.resolve({ requirements: [], usages: [] }),
    ]).then(([detail, projection, overview]) => {
      if (!active) {
        return;
      }
      if (!detail || detail.status) {
        setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
        setCaseDetail(null);
      } else {
        setError("");
        setCaseDetail(detail);
      }
      setProjectedResultIds(projection?.projectedResultIds || []);
      setReagentOverview({
        requirements: overview?.requirements || [],
        usages: overview?.usages || [],
      });
      setLoading(false);
    });

    return () => {
      active = false;
    };
  }, [caseId, intl, service]);

  useEffect(() => {
    if (!loading && !error && caseDetail) {
      markMicrobiologyReady(MICROBIOLOGY_CASE_READY_MARK);
    }
  }, [caseDetail, error, loading]);

  const recordActivity = (payload) => {
    setSaving(true);
    setActionError("");
    return service
      .recordCaseActivity(caseId, payload)
      .then((detail) => {
        if (!detail || detail.error || detail.statusCode >= 400) {
          throw new Error(
            formatMicrobiologyEnum(detail?.message || detail?.error),
          );
        }
        setCaseDetail(detail);
        return loadReagentOverview();
      })
      .catch((activityError) => {
        setActionError(activityError?.message || String(activityError));
      })
      .finally(() => setSaving(false));
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

  const updateIdentification = (isolateId, payload) => {
    setSaving(true);
    return service.updateIsolateIdentification(isolateId, payload).then(() => {
      loadCase({ showLoading: false });
      setSaving(false);
    });
  };

  const selectSection = (section) => {
    history.push(getMicrobiologyCaseUrl(caseId, { ...routeState, section }));
  };

  const focusedSection = routeState.section || "case-info";
  const focusedProgressIndex = Math.max(
    0,
    progressItems.findIndex((item) => item.section === focusedSection),
  );

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

  const finalReleased =
    caseDetail.finalReleaseState === "FINAL_RELEASED" ||
    caseDetail.stage === "FINAL_RELEASED";
  const amendmentOpen =
    caseDetail.finalReleaseState === "AMENDMENT_IN_PROGRESS";

  return (
    <main
      className="microbiology-workbench"
      data-testid="microbiology-case-view"
    >
      <Stack gap={7}>
        <PageBreadCrumb
          breadcrumbs={[
            { label: "home.label", link: "/" },
            {
              label: "microbiology.navigation.worklist",
              link: getMicrobiologyWorklistUrl(routeState),
            },
            {
              label: "microbiology.case.title",
              link: getMicrobiologyCaseUrl(caseId, routeState),
              isCurrentPage: true,
            },
          ]}
        />
        <header className="microbiology-workbench__hero">
          <div>
            <p className="microbiology-workbench__eyebrow">
              {intl.formatMessage({ id: "microbiology.case.eyebrow" })}
            </p>
            <h1>{intl.formatMessage({ id: "microbiology.case.title" })}</h1>
            <div className="microbiology-workbench__meta">
              <span>
                {intl.formatMessage({ id: "microbiology.case.sampleItem" })}:{" "}
                <strong>{caseDetail.sampleItemId}</strong>
              </span>
              <span>
                {intl.formatMessage({ id: "microbiology.case.workflow" })}:{" "}
                <strong>
                  {formatMicrobiologyEnum(caseDetail.workflowType, intl)}
                </strong>
              </span>
              {caseDetail.patientName && (
                <span>
                  {intl.formatMessage({ id: "microbiology.case.patient" })}:{" "}
                  <strong>{caseDetail.patientName}</strong>
                </span>
              )}
              {caseDetail.accessionNumber && (
                <span>
                  {intl.formatMessage({
                    id: "microbiology.case.accessionNumber",
                  })}
                  : <strong>{caseDetail.accessionNumber}</strong>
                </span>
              )}
              {caseDetail.specimenType && (
                <span>
                  {intl.formatMessage({
                    id: "microbiology.case.specimenType",
                  })}
                  : <strong>{caseDetail.specimenType}</strong>
                </span>
              )}
            </div>
          </div>
          <Tag type={caseDetail.stage === "FINAL_RELEASED" ? "green" : "blue"}>
            {formatMicrobiologyEnum(caseDetail.stage, intl)}
          </Tag>
        </header>

        {finalReleased && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.case.finalLocked.title",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.case.finalLocked.message",
            })}
          />
        )}

        {amendmentOpen && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.amendment.inProgress.title",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.amendment.inProgress.message",
            })}
          />
        )}

        {actionError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.case.actionError",
            })}
            subtitle={actionError}
          />
        )}

        <div className="microbiology-workbench__layout">
          <aside
            className="microbiology-workbench__rail"
            data-testid="microbiology-progress-rail"
            aria-label={intl.formatMessage({
              id: "microbiology.progress.title",
            })}
          >
            <h2>{intl.formatMessage({ id: "microbiology.progress.title" })}</h2>
            <ProgressIndicator
              currentIndex={focusedProgressIndex}
              vertical
              onChange={(index) => selectSection(progressItems[index].section)}
            >
              {progressItems.map((item) => (
                <ProgressStep
                  key={item.id}
                  complete={getProgressStatus(caseDetail, item.id) === "done"}
                  label={intl.formatMessage({ id: item.labelId })}
                />
              ))}
            </ProgressIndicator>
          </aside>

          <div className="microbiology-workbench__content">
            <Layer className="microbiology-next-step">
              <div>
                <p className="microbiology-workbench__eyebrow">
                  {intl.formatMessage({ id: "microbiology.next.title" })}
                </p>
                <p>
                  {intl.formatMessage({ id: getNextStepMessageId(caseDetail) })}
                </p>
              </div>
            </Layer>

            <Accordion>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.caseInfo",
                })}
                open={focusedSection === "case-info"}
                onHeadingClick={() => selectSection("case-info")}
              >
                <Layer
                  className="microbiology-case-summary"
                  data-testid="microbiology-case-summary"
                >
                  <span>
                    {intl.formatMessage({ id: "microbiology.case.sampleItem" })}
                    : {caseDetail.sampleItemId}
                  </span>
                  <span>
                    {intl.formatMessage({ id: "microbiology.case.workflow" })}:{" "}
                    {formatMicrobiologyEnum(caseDetail.workflowType, intl)}
                  </span>
                </Layer>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.orderDetail.title",
                })}
                open={focusedSection === "order-detail"}
                onHeadingClick={() => selectSection("order-detail")}
              >
                <OrderDetailPanel
                  caseId={caseDetail.id}
                  orderDetail={caseDetail.orderDetail}
                  service={service}
                  onSaved={() => loadCase({ showLoading: false })}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.inoculation",
                })}
                open={focusedSection === "setup"}
                onHeadingClick={() => selectSection("setup")}
              >
                <CaseTimelinePanel
                  activities={caseDetail.activities}
                  onRecordActivity={recordActivity}
                  saving={saving}
                  setupSectionId="microbiology-setup"
                  showTimeline={false}
                  reagentRequirements={reagentOverview.requirements}
                  reagentUsages={reagentOverview.usages}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.timeline",
                })}
                open={focusedSection === "timeline"}
                onHeadingClick={() => selectSection("timeline")}
              >
                <CaseTimelinePanel
                  activities={caseDetail.activities}
                  onRecordActivity={recordActivity}
                  saving={saving}
                  timelineSectionId="microbiology-timeline"
                  showSetup={false}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.isolates",
                })}
                open={focusedSection === "isolates"}
                onHeadingClick={() => selectSection("isolates")}
              >
                <IsolatePanel
                  caseId={caseDetail.id}
                  isolates={caseDetail.isolates}
                  onCreateIsolate={createIsolate}
                  onUpdateIdentification={updateIdentification}
                  saving={saving}
                  readOnly={finalReleased}
                  amendmentOpen={amendmentOpen}
                  service={service}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({ id: "microbiology.ast.title" })}
                open={focusedSection === "ast"}
                onHeadingClick={() => selectSection("ast")}
              >
                <AstEntryPanel
                  caseId={caseDetail.id}
                  workflowType={caseDetail.workflowType}
                  isolates={caseDetail.isolates}
                  service={service}
                  saving={saving}
                  onAstUpdated={() => {
                    setReadinessRefreshToken(
                      (currentValue) => currentValue + 1,
                    );
                    loadReagentOverview();
                  }}
                  readOnly={finalReleased}
                  reagentRequirements={reagentOverview.requirements}
                  reagentUsages={reagentOverview.usages}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.critical.title",
                })}
                open={focusedSection === "critical-communication"}
                onHeadingClick={() => selectSection("critical-communication")}
              >
                <CriticalCommunicationPanel
                  caseId={caseDetail.id}
                  sampleItemId={caseDetail.sampleItemId}
                  isolates={caseDetail.isolates}
                  projectedResultIds={projectedResultIds}
                  service={service}
                  onCaseUpdated={() => loadCase({ showLoading: false })}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.reports",
                })}
                open={focusedSection === "reports"}
                onHeadingClick={() => selectSection("reports")}
              >
                <ReportReadinessPanel
                  caseId={caseDetail.id}
                  service={service}
                  finalReleaseState={
                    caseDetail.finalReleaseState || caseDetail.stage
                  }
                  amendmentOpen={amendmentOpen}
                  patientId={caseDetail.patientId}
                  onReleased={() => loadCase({ showLoading: false })}
                  onProjectionLoaded={setProjectedResultIds}
                  refreshToken={readinessRefreshToken}
                />
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.amendment.title",
                })}
                open={focusedSection === "amendment"}
                onHeadingClick={() => selectSection("amendment")}
              >
                <AmendmentHistoryPanel
                  caseId={caseDetail.id}
                  finalReleaseState={
                    caseDetail.finalReleaseState || caseDetail.stage
                  }
                  service={service}
                  active={focusedSection === "amendment"}
                  onCaseUpdated={() => loadCase({ showLoading: false })}
                />
              </AccordionItem>
            </Accordion>
          </div>
        </div>
      </Stack>
    </main>
  );
};

export default MicrobiologyCaseView;
