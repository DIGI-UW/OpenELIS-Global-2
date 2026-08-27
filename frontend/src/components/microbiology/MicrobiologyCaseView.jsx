import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Layer,
  Loading,
  Stack,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import AstEntryPanel from "./AstEntryPanel";
import CaseCultureTransitionPanel from "./CaseCultureTransitionPanel";
import CaseInfoSummary from "./CaseInfoSummary";
import CaseInoculationPanel from "./CaseInoculationPanel";
import CaseNonconformancePanel from "./CaseNonconformancePanel";
import CaseProtocolPanel from "./CaseProtocolPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import ChangeWorkflowPanel from "./ChangeWorkflowPanel";
import IsolatePanel from "./IsolatePanel";
import MicrobiologyService from "./MicrobiologyService";

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
  const params = useParams();
  const caseId = caseIdProp || params.caseId;
  const [caseDetail, setCaseDetail] = useState(null);
  const [inoculations, setInoculations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [activeAction, setActiveAction] = useState("");
  const [protocolOpen, setProtocolOpen] = useState(false);

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
    <main data-testid="microbiology-case-view">
      <Stack gap={7}>
        <header>
          <h2>{intl.formatMessage({ id: "microbiology.case.title" })}</h2>
          <p>
            {intl.formatMessage({ id: "microbiology.case.sampleItem" })}:{" "}
            <strong>{caseDetail.sampleItemId}</strong>
          </p>
          <p>
            {intl.formatMessage({ id: "microbiology.case.workflow" })}:{" "}
            <strong>{caseDetail.workflowType}</strong>
          </p>
          <Stack orientation="horizontal" gap={3}>
            <Tag type={finalReleased ? "green" : "blue"}>
              {caseDetail.stage}
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

        <Layer>
          <CaseInfoSummary
            accessionNumber={caseDetail.accessionNumber}
            requestingLocation={caseDetail.requestingLocation}
            orderDetail={caseDetail.orderDetail}
          />
        </Layer>

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

        <ChangeWorkflowPanel
          caseId={caseDetail.id}
          workflowType={caseDetail.workflowType}
          cultureMethodId={caseDetail.cultureMethodId}
          requiresConfirmation={caseDetail.workflowChangeRequiresConfirmation}
          service={service}
          onChanged={(detail) => {
            setCaseDetail(detail);
            setActionError("");
          }}
        />

        {actionError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.case.error" })}
            subtitle={actionError}
          />
        )}

        {!unassigned && (
          <>
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
          </>
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

        <CaseTimelinePanel
          activities={caseDetail.activities}
          onAddNote={addTimelineNote}
          saving={saving}
        />

        {!unassigned && (
          <>
            <IsolatePanel
              caseId={caseDetail.id}
              isolates={caseDetail.isolates}
              onCreateIsolate={createIsolate}
              onUpdateIdentification={updateIdentification}
              saving={saving}
              readOnly={finalReleased}
              service={service}
            />
            <AstEntryPanel
              caseId={caseDetail.id}
              workflowType={caseDetail.workflowType}
              isolates={caseDetail.isolates}
              service={service}
              saving={saving}
              readOnly={finalReleased}
            />
          </>
        )}
      </Stack>
    </main>
  );
};

export default MicrobiologyCaseView;
