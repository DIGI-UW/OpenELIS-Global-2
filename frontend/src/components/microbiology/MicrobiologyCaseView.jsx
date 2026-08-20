import React, { forwardRef, useEffect, useRef, useState } from "react";
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
import {
  Link as RouterLink,
  useHistory,
  useLocation,
  useParams,
} from "react-router-dom";
import AmendmentHistoryPanel from "./AmendmentHistoryPanel";
import AstEntryPanel from "./AstEntryPanel";
import CaseInfoSummary, { CaseInfoCompactSummary } from "./CaseInfoSummary";
import CaseInoculationPanel from "./CaseInoculationPanel";
import CaseCultureTransitionPanel from "./CaseCultureTransitionPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import CaseNonconformancePanel from "./CaseNonconformancePanel";
import ChangeWorkflowPanel from "./ChangeWorkflowPanel";
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
import { getMicrobiologyCurrentStep } from "./MicrobiologyCaseState";
import OrderDetailPanel from "./OrderDetailPanel";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ReportReadinessPanel from "./ReportReadinessPanel";
import { formatReagentLotConflict } from "./ReagentLotPicker";
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

const profileDependentSections = new Set([
  "setup",
  "isolates",
  "ast",
  "reports",
]);

const sectionLabelIds = {
  "case-info": "microbiology.progress.caseInfo",
  "order-detail": "microbiology.orderDetail.title",
  setup: "microbiology.progress.inoculation",
  timeline: "microbiology.progress.timeline",
  nonconformance: "microbiology.nce.sectionTitle",
  isolates: "microbiology.progress.isolates",
  ast: "microbiology.ast.title",
  "critical-communication": "microbiology.critical.title",
  reports: "microbiology.progress.reports",
  amendment: "microbiology.amendment.title",
};

const CaseSectionFocusTarget = forwardRef(
  ({ section, focused, label, children }, ref) => {
    if (!focused) {
      return null;
    }
    return (
      <div
        ref={ref}
        className="microbiology-workbench__section-focus"
        data-testid={`microbiology-case-section-${section}`}
        role="region"
        aria-label={label}
        tabIndex={-1}
      >
        {children}
      </div>
    );
  },
);

CaseSectionFocusTarget.displayName = "CaseSectionFocusTarget";

const hasActivity = (caseDetail, activityType) =>
  (caseDetail.activities || []).some(
    (activity) => activity.activityType === activityType,
  );

const getProgressStatus = (caseDetail, itemId) => {
  if (caseDetail.workflowType === "UNASSIGNED") {
    return itemId === "case-info" ? "current" : "todo";
  }
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
  if (caseDetail.workflowType === "UNASSIGNED") {
    return "microbiology.next.classifyWorkflow";
  }
  if (caseDetail.finalReleaseState === "AMENDMENT_IN_PROGRESS") {
    return "microbiology.next.completeAmendment";
  }
  if (caseDetail.stage === "FINAL_RELEASED") {
    return "microbiology.next.finalReleased";
  }
  if (
    !hasActivity(caseDetail, "INOCULATION_RECORDED") &&
    !hasActivity(caseDetail, "STAGE_CHANGED")
  ) {
    return "microbiology.next.recordSetup";
  }
  if (caseDetail.stage === "NO_GROWTH_READY") {
    return "microbiology.next.release";
  }
  if (caseDetail.stage === "POSITIVE_SIGNAL") {
    return "microbiology.next.subculturePositive";
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
  const [inoculations, setInoculations] = useState([]);
  const [actionError, setActionError] = useState("");
  const focusedSectionRef = useRef(null);

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
    const timelinePromise = service.getCaseTimeline
      ? service.getCaseTimeline(caseId)
      : Promise.resolve(null);
    return Promise.all([service.getCaseDetail(caseId), timelinePromise]).then(
      ([detail, timeline]) => {
        if (!detail || detail.status) {
          setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
          setCaseDetail(null);
        } else {
          setError("");
          setCaseDetail({
            ...detail,
            activities: Array.isArray(timeline) ? timeline : detail.activities,
          });
        }
        if (showLoading) {
          setLoading(false);
        }
      },
    );
  };

  useEffect(() => {
    let active = true;

    Promise.all([
      service.getCaseDetail(caseId),
      service.getReportProjection(caseId),
      service.getReagentLotOverview
        ? service.getReagentLotOverview(caseId)
        : Promise.resolve({ requirements: [], usages: [] }),
      service.getCaseInoculations
        ? service.getCaseInoculations(caseId)
        : Promise.resolve([]),
      service.getCaseTimeline
        ? service.getCaseTimeline(caseId)
        : Promise.resolve(null),
    ]).then(([detail, projection, overview, caseInoculations, timeline]) => {
      if (!active) {
        return;
      }
      if (!detail || detail.status) {
        setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
        setCaseDetail(null);
      } else {
        setError("");
        setCaseDetail({
          ...detail,
          activities: Array.isArray(timeline) ? timeline : detail.activities,
        });
      }
      setProjectedResultIds(projection?.projectedResultIds || []);
      setReagentOverview({
        requirements: overview?.requirements || [],
        usages: overview?.usages || [],
      });
      setInoculations(Array.isArray(caseInoculations) ? caseInoculations : []);
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

  useEffect(() => {
    if (!caseDetail || !location.pathname.startsWith("/Microbiology/cases/")) {
      return;
    }
    const currentStep = getMicrobiologyCurrentStep(caseDetail);
    const section =
      caseDetail.workflowType === "UNASSIGNED" &&
      profileDependentSections.has(routeState.section)
        ? "case-info"
        : routeState.section || currentStep.section;
    if (section !== routeState.section) {
      history.replace(
        getMicrobiologyCaseUrl(caseId, { ...routeState, section }),
      );
    }
  }, [caseDetail, caseId, history, location.pathname, routeState.section]);

  const recordInoculation = (payload) => {
    setSaving(true);
    setActionError("");
    return service
      .recordCaseInoculation(caseId, payload)
      .then((result) => {
        if (!result || result.error || result.statusCode >= 400) {
          const selectedLots = Object.fromEntries(
            (payload.lotSelections || []).map((selection) => [
              `${selection.analysisId}:${selection.testReagentLinkId}`,
              selection,
            ]),
          );
          throw new Error(
            formatReagentLotConflict(
              result,
              reagentOverview.requirements || [],
              selectedLots,
              intl,
            ) || formatMicrobiologyEnum(result?.message || result?.error),
          );
        }
        return Promise.all([
          loadCase({ showLoading: false }),
          loadReagentOverview(),
          service.getCaseInoculations(caseId).then((records) => {
            setInoculations(Array.isArray(records) ? records : []);
          }),
        ]);
      })
      .catch((inoculationError) => {
        setActionError(inoculationError?.message || String(inoculationError));
        throw inoculationError;
      })
      .finally(() => setSaving(false));
  };

  const addTimelineNote = (text) => {
    setSaving(true);
    setActionError("");
    return service
      .addCaseNote(caseId, text)
      .then((result) => {
        if (!result || result.error || result.statusCode >= 400) {
          throw new Error(
            formatMicrobiologyEnum(result?.message || result?.error),
          );
        }
        return service.getCaseTimeline(caseId).then((timeline) => {
          setCaseDetail((current) => ({ ...current, activities: timeline }));
        });
      })
      .catch((noteError) => {
        setActionError(noteError?.message || String(noteError));
        throw noteError;
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

  const workflowChanged = (detail) => {
    setCaseDetail(detail);
    history.replace(
      getMicrobiologyCaseUrl(caseId, { ...routeState, section: "case-info" }),
    );
  };

  const selectSection = (section) => {
    history.push(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section,
        action: "",
        targetType: "",
        targetId: "",
      }),
    );
  };

  const openCriticalCommunication = (targetType, targetId) => {
    history.push(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: "critical-communication",
        action: "log-critical",
        targetType,
        targetId,
      }),
    );
  };

  const completeCriticalEntry = () => {
    history.replace(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: "critical-communication",
        action: "",
        targetType: "",
        targetId: "",
      }),
    );
  };

  const completeCultureTransition = (detail) => {
    setCaseDetail(detail);
    history.replace(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: "setup",
        action: "",
      }),
    );
  };

  const completeAstAttempt = (run) => {
    history.replace(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: "ast",
        action: "",
        astIsolateId: run.isolateId || routeState.astIsolateId,
        astRunId: run.id,
      }),
    );
  };

  const openNonconformance = (action) => {
    history.push(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: "nonconformance",
        action,
        targetType: "",
        targetId: "",
      }),
    );
  };

  const completeNonconformance = (result) => {
    loadCase({ showLoading: false });
    history.replace(
      getMicrobiologyCaseUrl(caseId, {
        ...routeState,
        section: result?.disposition === "RETEST" ? "ast" : "timeline",
        action: "",
        targetType: "",
        targetId: "",
      }),
    );
  };

  const currentStep = getMicrobiologyCurrentStep(caseDetail || {});
  const focusedSection = routeState.section || currentStep.section;
  const focusedProgressIndex = Math.max(
    0,
    progressItems.findIndex((item) => item.section === focusedSection),
  );
  const focusedSectionLabel = intl.formatMessage({
    id: sectionLabelIds[focusedSection] || "microbiology.progress.caseInfo",
  });

  useEffect(() => {
    const actionOwnsFocus = [
      "report-nce",
      "mark-lost",
      "mark-positive",
      "mark-no-growth",
    ].includes(routeState.action);
    if (
      loading ||
      error ||
      !caseDetail ||
      routeState.section !== focusedSection ||
      !focusedSectionRef.current ||
      actionOwnsFocus
    ) {
      return;
    }
    const frame = window.requestAnimationFrame(() =>
      focusedSectionRef.current?.focus(),
    );
    return () => window.cancelAnimationFrame(frame);
  }, [
    error,
    focusedSection,
    loading,
    routeState.action,
    routeState.astIsolateId,
    routeState.astRunId,
    routeState.section,
    routeState.targetId,
    routeState.targetType,
  ]);

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
  const unassigned = caseDetail.workflowType === "UNASSIGNED";

  return (
    <main
      className="microbiology-workbench"
      data-testid="microbiology-case-view"
    >
      <div
        className="cds--visually-hidden"
        role="status"
        aria-live="polite"
        aria-label={intl.formatMessage({
          id: "microbiology.case.sectionStatus",
        })}
      >
        {intl.formatMessage(
          { id: "microbiology.case.sectionExpanded" },
          { section: focusedSectionLabel },
        )}
      </div>
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
                  {formatMicrobiologyEnum(caseDetail.workflowType)}
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
              {caseDetail.lastActivityBy && (
                <span>
                  {intl.formatMessage({
                    id: "microbiology.case.lastActivityBy",
                  })}
                  : <strong>{caseDetail.lastActivityBy}</strong>
                  {caseDetail.lastActivityAt && (
                    <>
                      {" "}
                      {intl.formatDate(caseDetail.lastActivityAt, {
                        dateStyle: "medium",
                        timeStyle: "short",
                      })}
                    </>
                  )}
                </span>
              )}
            </div>
            {(caseDetail.siblingCases || []).length > 0 && (
              <nav
                className="microbiology-sibling-links"
                aria-label={intl.formatMessage({
                  id: "microbiology.case.relatedWorkflows",
                })}
              >
                <span>
                  {intl.formatMessage({
                    id: "microbiology.case.relatedWorkflows",
                  })}
                  :
                </span>
                {(caseDetail.siblingCases || []).map((sibling) => (
                  <RouterLink
                    key={sibling.id}
                    aria-label={`${formatMicrobiologyEnum(
                      sibling.workflowType,
                    )} (${formatMicrobiologyEnum(sibling.stage)})`}
                    to={getMicrobiologyCaseUrl(sibling.id, {
                      ...routeState,
                      section: "case-info",
                    })}
                  >
                    {formatMicrobiologyEnum(sibling.workflowType)} (
                    {formatMicrobiologyEnum(sibling.stage)})
                  </RouterLink>
                ))}
              </nav>
            )}
          </div>
          <div className="microbiology-workbench__hero-actions">
            <Tag
              type={caseDetail.stage === "FINAL_RELEASED" ? "green" : "blue"}
            >
              {formatMicrobiologyEnum(caseDetail.stage)}
            </Tag>
            {caseDetail.nonconformanceCount > 0 && (
              <Tag type="red">
                {intl.formatMessage(
                  { id: "microbiology.case.nonconformanceCount" },
                  { count: caseDetail.nonconformanceCount },
                )}
              </Tag>
            )}
            <Button
              kind="ghost"
              size="sm"
              disabled={finalReleased}
              onClick={() => openNonconformance("report-nce")}
            >
              {intl.formatMessage({ id: "microbiology.nce.report" })}
            </Button>
            <Button
              kind="danger--tertiary"
              size="sm"
              disabled={finalReleased}
              onClick={() => openNonconformance("mark-lost")}
            >
              {intl.formatMessage({ id: "microbiology.nce.markLost" })}
            </Button>
            <Button
              kind="tertiary"
              size="sm"
              onClick={() => openCriticalCommunication("CASE", caseDetail.id)}
            >
              {intl.formatMessage({
                id: "microbiology.critical.logNotification",
              })}
            </Button>
          </div>
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
                title={
                  <span className="microbiology-case-info__heading">
                    <span>
                      {intl.formatMessage({
                        id: "microbiology.progress.caseInfo",
                      })}
                    </span>
                    <CaseInfoCompactSummary
                      accessionNumber={caseDetail.accessionNumber}
                      requestingLocation={caseDetail.requestingLocation}
                      orderDetail={caseDetail.orderDetail}
                    />
                  </span>
                }
                open={focusedSection === "case-info"}
                onHeadingClick={() => selectSection("case-info")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="case-info"
                  focused={focusedSection === "case-info"}
                  label={intl.formatMessage({
                    id: sectionLabelIds["case-info"],
                  })}
                >
                  <CaseInfoSummary
                    accessionNumber={caseDetail.accessionNumber}
                    requestingLocation={caseDetail.requestingLocation}
                    orderDetail={caseDetail.orderDetail}
                  />
                  <ChangeWorkflowPanel
                    caseId={caseDetail.id}
                    workflowType={caseDetail.workflowType}
                    cultureMethodId={caseDetail.cultureMethodId}
                    requiresConfirmation={
                      caseDetail.workflowChangeRequiresConfirmation
                    }
                    service={service}
                    onChanged={workflowChanged}
                  />
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.orderDetail.title",
                })}
                open={focusedSection === "order-detail"}
                onHeadingClick={() => selectSection("order-detail")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="order-detail"
                  focused={focusedSection === "order-detail"}
                  label={intl.formatMessage({
                    id: sectionLabelIds["order-detail"],
                  })}
                >
                  <OrderDetailPanel
                    caseId={caseDetail.id}
                    orderDetail={caseDetail.orderDetail}
                    service={service}
                    onSaved={() => loadCase({ showLoading: false })}
                  />
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.inoculation",
                })}
                open={focusedSection === "setup"}
                onHeadingClick={() => selectSection("setup")}
                disabled={unassigned}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="setup"
                  focused={focusedSection === "setup"}
                  label={intl.formatMessage({ id: sectionLabelIds.setup })}
                >
                  {!unassigned && (
                    <Stack gap={5}>
                      {["mark-positive", "mark-no-growth"].includes(
                        routeState.action,
                      ) && (
                        <CaseCultureTransitionPanel
                          action={routeState.action}
                          caseId={caseId}
                          service={service}
                          onComplete={completeCultureTransition}
                          onCancel={() => selectSection("setup")}
                        />
                      )}
                      <CaseInoculationPanel
                        inoculations={inoculations}
                        onRecord={recordInoculation}
                        saving={saving}
                        reagentRequirements={reagentOverview.requirements}
                        reagentUsages={reagentOverview.usages}
                        readOnly={finalReleased && !amendmentOpen}
                      />
                    </Stack>
                  )}
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.timeline",
                })}
                open={focusedSection === "timeline"}
                onHeadingClick={() => selectSection("timeline")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="timeline"
                  focused={focusedSection === "timeline"}
                  label={intl.formatMessage({ id: sectionLabelIds.timeline })}
                >
                  <CaseTimelinePanel
                    activities={caseDetail.activities}
                    timelineSectionId="microbiology-timeline"
                    onAddNote={addTimelineNote}
                    saving={saving}
                  />
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.nce.sectionTitle",
                })}
                open={focusedSection === "nonconformance"}
                onHeadingClick={() => selectSection("nonconformance")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="nonconformance"
                  focused={focusedSection === "nonconformance"}
                  label={intl.formatMessage({
                    id: sectionLabelIds.nonconformance,
                  })}
                >
                  {routeState.section === "nonconformance" &&
                    ["report-nce", "mark-lost"].includes(routeState.action) && (
                      <CaseNonconformancePanel
                        caseId={caseDetail.id}
                        mode={routeState.action}
                        isolates={caseDetail.isolates}
                        workflowType={caseDetail.workflowType}
                        service={service}
                        onComplete={completeNonconformance}
                        onCancel={() => selectSection("timeline")}
                      />
                    )}
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.isolates",
                })}
                open={focusedSection === "isolates"}
                onHeadingClick={() => selectSection("isolates")}
                disabled={unassigned}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="isolates"
                  focused={focusedSection === "isolates"}
                  label={intl.formatMessage({ id: sectionLabelIds.isolates })}
                >
                  {!unassigned && (
                    <IsolatePanel
                      caseId={caseDetail.id}
                      isolates={caseDetail.isolates}
                      onCreateIsolate={createIsolate}
                      onUpdateIdentification={updateIdentification}
                      saving={saving}
                      readOnly={finalReleased}
                      amendmentOpen={amendmentOpen}
                      onLogCritical={(isolate) =>
                        openCriticalCommunication("ISOLATE", isolate.id)
                      }
                      service={service}
                    />
                  )}
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({ id: "microbiology.ast.title" })}
                open={focusedSection === "ast"}
                onHeadingClick={() => selectSection("ast")}
                disabled={unassigned}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="ast"
                  focused={focusedSection === "ast"}
                  label={intl.formatMessage({ id: sectionLabelIds.ast })}
                >
                  {!unassigned && (
                    <AstEntryPanel
                      key={`${routeState.astIsolateId}:${routeState.astRunId}:${routeState.action}`}
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
                      initialIsolateId={routeState.astIsolateId}
                      initialRunId={routeState.astRunId}
                      initialAction={routeState.action}
                      onAttemptStarted={completeAstAttempt}
                    />
                  )}
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.critical.title",
                })}
                open={focusedSection === "critical-communication"}
                onHeadingClick={() => selectSection("critical-communication")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="critical-communication"
                  focused={focusedSection === "critical-communication"}
                  label={intl.formatMessage({
                    id: sectionLabelIds["critical-communication"],
                  })}
                >
                  <CriticalCommunicationPanel
                    caseId={caseDetail.id}
                    sampleItemId={caseDetail.sampleItemId}
                    isolates={caseDetail.isolates}
                    projectedResultIds={projectedResultIds}
                    entryTargetType={
                      routeState.action === "log-critical"
                        ? routeState.targetType
                        : ""
                    }
                    entryTargetId={
                      routeState.action === "log-critical"
                        ? routeState.targetId
                        : ""
                    }
                    service={service}
                    onCaseUpdated={() => loadCase({ showLoading: false })}
                    onEntryComplete={completeCriticalEntry}
                  />
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.progress.reports",
                })}
                open={focusedSection === "reports"}
                onHeadingClick={() => selectSection("reports")}
                disabled={unassigned}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="reports"
                  focused={focusedSection === "reports"}
                  label={intl.formatMessage({ id: sectionLabelIds.reports })}
                >
                  {!unassigned && (
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
                  )}
                </CaseSectionFocusTarget>
              </AccordionItem>
              <AccordionItem
                title={intl.formatMessage({
                  id: "microbiology.amendment.title",
                })}
                open={focusedSection === "amendment"}
                onHeadingClick={() => selectSection("amendment")}
              >
                <CaseSectionFocusTarget
                  ref={focusedSectionRef}
                  section="amendment"
                  focused={focusedSection === "amendment"}
                  label={intl.formatMessage({ id: sectionLabelIds.amendment })}
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
                </CaseSectionFocusTarget>
              </AccordionItem>
            </Accordion>
            <footer
              className="microbiology-current-step-action"
              data-testid="microbiology-current-step-action"
            >
              <div>
                <p className="microbiology-workbench__eyebrow">
                  {intl.formatMessage({ id: "microbiology.currentStep.title" })}
                </p>
                <strong>
                  {intl.formatMessage({ id: currentStep.labelId })}
                </strong>
              </div>
              <Button
                size="sm"
                onClick={() => selectSection(currentStep.section)}
              >
                {intl.formatMessage(
                  { id: "microbiology.currentStep.open" },
                  { step: intl.formatMessage({ id: currentStep.labelId }) },
                )}
              </Button>
            </footer>
          </div>
        </div>
      </Stack>
    </main>
  );
};

export default MicrobiologyCaseView;
