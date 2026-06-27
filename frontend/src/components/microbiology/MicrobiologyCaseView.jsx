import React, { useEffect, useState } from "react";
import { InlineNotification, Loading, Stack, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import AstEntryPanel from "./AstEntryPanel";
import CaseTimelinePanel from "./CaseTimelinePanel";
import CriticalCommunicationPanel from "./CriticalCommunicationPanel";
import IsolatePanel from "./IsolatePanel";
import MicrobiologyService from "./MicrobiologyService";

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

  const loadCase = () => {
    setLoading(true);
    service.getCaseDetail(caseId).then((detail) => {
      if (!detail || detail.status) {
        setError(intl.formatMessage({ id: "microbiology.case.loadError" }));
        setCaseDetail(null);
      } else {
        setError("");
        setCaseDetail(detail);
      }
      setLoading(false);
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
          <Tag type="blue">{caseDetail.stage}</Tag>
        </header>
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
        />
        <CriticalCommunicationPanel caseId={caseDetail.id} service={service} />
      </Stack>
    </main>
  );
};

export default MicrobiologyCaseView;
