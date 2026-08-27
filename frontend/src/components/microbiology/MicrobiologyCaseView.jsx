import React, { useEffect, useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
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
import AstEntryPanel from "./AstEntryPanel";
import CaseCultureTransitionPanel from "./CaseCultureTransitionPanel";
import CaseInfoSummary from "./CaseInfoSummary";
import CaseInoculationPanel from "./CaseInoculationPanel";
import CaseNonconformancePanel from "./CaseNonconformancePanel";
import CaseProtocolPanel from "./CaseProtocolPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import ChangeWorkflowPanel from "./ChangeWorkflowPanel";
import CriticalCommunicationPanel from "./CriticalCommunicationPanel";
import IsolatePanel from "./IsolatePanel";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  parseMicrobiologyCaseSearch,
} from "./MicrobiologyRoutes";
import MicrobiologyService from "./MicrobiologyService";
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
    review: astReviewed || noGrowthReady ? "done" : "todo",
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

const failed = (response) =>
  !response ||
  response.error ||
  response.status === 0 ||
  response.status >= 400 ||
  response.statusCode >= 400;

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
  const focusedSection = routeState.section || "case-info";
  const [caseDetail, setCaseDetail] = useState(null);
  const [inoculations, setInoculations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [activeAction, setActiveAction] = useState("");
  const [protocolOpen, setProtocolOpen] = useState(false);
  const [readinessRefreshToken, setReadinessRefreshToken] = useState(0);
  const [projectedResultIds, setProjectedResultIds] = useState([]);

  const loadCase = ({ showLoading = true } = {}) => {
    if (showLoading) {
      setLoading(true);
    }
    const timeline = service.getCaseTimeline
      ? service.getCaseTimeline(caseId)
      : Promise.resolve(null);
    const caseInoculations = service.getCaseInoculations
      ? service.getCaseInoculations(caseId)
      : Promise.resolve([]);
    return Promise.all([
      service.getCaseDetail(caseId),
      timeline,
      caseInoculations,
    ])
      .then(([detail, activities, records]) => {
        if (failed(detail)) {
          setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
          setCaseDetail(null);
          return;
        }
        setError("");
        setCaseDetail({
          ...detail,
          activities: Array.isArray(activities)
            ? activities
            : detail.activities || [],
        });
        setInoculations(Array.isArray(records) ? records : []);
      })
      .catch(() => {
        setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
        setCaseDetail(null);
      })
      .finally(() => {
        if (showLoading) {
          setLoading(false);
        }
      });
  };

  useEffect(() => {
    loadCase();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId]);

  useEffect(() => {
    let active = true;
    const projection = service.getReportProjection
      ? service.getReportProjection(caseId)
      : Promise.resolve(null);

    Promise.resolve(projection)
      .then((value) => {
        if (active) {
          setProjectedResultIds(value?.projectedResultIds || []);
        }
      })
      .catch(() => {
        if (active) {
          setProjectedResultIds([]);
        }
      });

    return () => {
      active = false;
    };
  }, [caseId, service]);

  useEffect(() => {
    if (
      !routeState.section &&
      location.pathname.startsWith("/Microbiology/cases/")
    ) {
      history.replace(
        getMicrobiologyCaseUrl(caseId, {
          ...routeState,
          section: "case-info",
        }),
      );
    }
  }, [caseId, history, location.pathname, routeState.section]);

  const selectSection = (section) => {
    history.push(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section,
      }),
    );
  };

  const focusedProgressIndex = Math.max(
    0,
    progressItems.findIndex((item) => item.section === focusedSection),
  );

  const runAction = (
    request,
    afterSave = () => loadCase({ showLoading: false }),
  ) => {
    setSaving(true);
    setActionError("");
    return Promise.resolve(request())
      .then((response) => {
        if (failed(response)) {
          throw new Error(
            response?.message || response?.error || "UNKNOWN_ERROR",
          );
        }
        return afterSave(response);
      })
      .catch((actionFailure) => {
        setActionError(actionFailure?.message || String(actionFailure));
        throw actionFailure;
      })
      .finally(() => setSaving(false));
  };

  const recordInoculation = (payload) =>
    runAction(
      () => service.recordCaseInoculation(caseId, payload),
      () => loadCase({ showLoading: false }),
    );

  const addTimelineNote = (text) =>
    runAction(
      () => service.addCaseNote(caseId, text),
      () =>
        service.getCaseTimeline(caseId).then((activities) => {
          setCaseDetail((current) => ({ ...current, activities }));
        }),
    );

  const createIsolate = (payload) =>
    runAction(() => service.createIsolate(payload));

  const updateIdentification = (isolateId, payload) =>
    runAction(() => service.updateIsolateIdentification(isolateId, payload));

  const completeCultureAction = (detail) => {
    setCaseDetail(detail);
    setActiveAction("");
    return loadCase({ showLoading: false });
  };

  const completeNonconformance = () => {
    setActiveAction("");
    return loadCase({ showLoading: false });
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

  const finalReleased =
    caseDetail.stage === "FINAL_RELEASED" ||
    caseDetail.finalReleaseState === "FINAL_RELEASED";
  const unassigned = caseDetail.workflowType === "UNASSIGNED";

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
                  {intl.formatMessage({ id: "microbiology.case.specimenType" })}
                  : <strong>{caseDetail.specimenType}</strong>
                </span>
              )}
            </div>
          </div>
          <Stack orientation="horizontal" gap={3}>
            <Tag type={finalReleased ? "green" : "blue"}>
              {formatMicrobiologyEnum(caseDetail.stage, intl)}
            </Tag>
            <Button
              kind="ghost"
              size="sm"
              disabled={finalReleased}
              onClick={() => setActiveAction("report-nce")}
            >
              {intl.formatMessage({ id: "microbiology.nce.report" })}
            </Button>
            <Button
              kind="danger--tertiary"
              size="sm"
              disabled={finalReleased}
              onClick={() => setActiveAction("mark-lost")}
            >
              {intl.formatMessage({ id: "microbiology.nce.markLost" })}
            </Button>
          </Stack>
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

        {unassigned && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.workflowChange.requiredTitle",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.workflowChange.requiredMessage",
            })}
          />
        )}

        {actionError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.case.error" })}
            subtitle={actionError}
          />
        )}

        {["report-nce", "mark-lost"].includes(activeAction) && (
          <CaseNonconformancePanel
            caseId={caseDetail.id}
            mode={activeAction}
            service={service}
            onComplete={completeNonconformance}
            onCancel={() => setActiveAction("")}
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

          <section
            className="microbiology-workbench__content"
            aria-label={intl.formatMessage({
              id: "microbiology.case.workflowSections",
            })}
          >
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
                <Layer>
                  <CaseInfoSummary
                    accessionNumber={caseDetail.accessionNumber}
                    requestingLocation={caseDetail.requestingLocation}
                    orderDetail={caseDetail.orderDetail}
                  />
                </Layer>
                <ChangeWorkflowPanel
                  caseId={caseDetail.id}
                  workflowType={caseDetail.workflowType}
                  cultureMethodId={caseDetail.cultureMethodId}
                  requiresConfirmation={
                    caseDetail.workflowChangeRequiresConfirmation
                  }
                  service={service}
                  onChanged={(detail) => {
                    setCaseDetail(detail);
                    setActionError("");
                  }}
                />
              </AccordionItem>

              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.inoculation",
                })}
                open={focusedSection === "setup"}
                onHeadingClick={() => selectSection("setup")}
                disabled={unassigned}
              >
                {!unassigned && (
                  <Stack gap={5}>
                    <CaseProtocolPanel
                      caseId={caseDetail.id}
                      currentMethodId={caseDetail.cultureMethodId}
                      open={protocolOpen}
                      readOnly={finalReleased}
                      service={service}
                      onOpen={() => setProtocolOpen(true)}
                      onClose={() => setProtocolOpen(false)}
                      onChanged={(detail) => setCaseDetail(detail)}
                    />
                    <CaseInoculationPanel
                      inoculations={inoculations}
                      onRecord={recordInoculation}
                      saving={saving}
                      readOnly={finalReleased}
                      stage={caseDetail.stage}
                      action={activeAction}
                      onInoculationAction={setActiveAction}
                      onCultureAction={setActiveAction}
                    />
                    <CaseCultureTransitionPanel
                      action={activeAction}
                      caseId={caseDetail.id}
                      service={service}
                      onComplete={completeCultureAction}
                      onCancel={() => setActiveAction("")}
                    />
                  </Stack>
                )}
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
                  onAddNote={addTimelineNote}
                  saving={saving}
                />
              </AccordionItem>

              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.isolates",
                })}
                open={focusedSection === "isolates"}
                onHeadingClick={() => selectSection("isolates")}
                disabled={unassigned}
              >
                {!unassigned && (
                  <IsolatePanel
                    caseId={caseDetail.id}
                    isolates={caseDetail.isolates}
                    onCreateIsolate={createIsolate}
                    onUpdateIdentification={updateIdentification}
                    saving={saving}
                    readOnly={finalReleased}
                    service={service}
                  />
                )}
              </AccordionItem>

              <AccordionItem
                title={intl.formatMessage({ id: "microbiology.progress.ast" })}
                open={focusedSection === "ast"}
                onHeadingClick={() => selectSection("ast")}
                disabled={unassigned}
              >
                {!unassigned && (
                  <AstEntryPanel
                    caseId={caseDetail.id}
                    workflowType={caseDetail.workflowType}
                    isolates={caseDetail.isolates}
                    service={service}
                    saving={saving}
                    readOnly={finalReleased}
                    onAstUpdated={() =>
                      setReadinessRefreshToken(
                        (currentValue) => currentValue + 1,
                      )
                    }
                  />
                )}
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
                  finalReleaseState={caseDetail.stage}
                  patientId={caseDetail.patientId}
                  onReleased={() => loadCase({ showLoading: false })}
                  onProjectionLoaded={setProjectedResultIds}
                  refreshToken={readinessRefreshToken}
                />
              </AccordionItem>
            </Accordion>
          </section>
        </div>
      </Stack>
    </main>
  );
};

export default MicrobiologyCaseView;
