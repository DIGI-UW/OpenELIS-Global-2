import React, { useEffect, useState } from "react";
import { InlineLoading, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import CaseIsolateWorkspace from "./CaseIsolateWorkspace";
import service from "./CaseWorkspaceService";

export default function IsolatePanel({
  caseId,
  onChanged,
  onLogCritical,
  readOnly = false,
}) {
  const intl = useIntl();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState(false);
  useEffect(() => {
    let live = true;
    setDetail(null);
    setError(false);
    service
      .getCase(caseId)
      .then((value) => {
        if (live) setDetail(value);
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [caseId, readOnly]);
  if (error)
    return (
      <InlineNotification
        kind="error"
        title={intl.formatMessage({ id: "microbiology.case.loadError" })}
        hideCloseButton
      />
    );
  if (!detail)
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "common.loading" })}
      />
    );
  return (
    <CaseIsolateWorkspace
      key={caseId}
      detail={{ ...detail, canWrite: detail.canWrite && !readOnly }}
      service={service}
      onChanged={onChanged}
      onLogCritical={onLogCritical}
    />
  );
}
