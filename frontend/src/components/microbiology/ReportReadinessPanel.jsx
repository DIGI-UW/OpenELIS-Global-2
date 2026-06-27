import React, { useEffect, useRef, useState } from "react";
import { Button, InlineNotification, Stack, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";

const ReportReadinessPanel = ({
  caseId,
  service = MicrobiologyService,
  onReleased,
  refreshToken = 0,
}) => {
  const intl = useIntl();
  const [readiness, setReadiness] = useState(null);
  const [whonetReadiness, setWhonetReadiness] = useState(null);
  const [releaseState, setReleaseState] = useState(null);
  const [saving, setSaving] = useState(false);
  const mountedRef = useRef(true);

  const loadState = () =>
    Promise.all([
      service.getCaseReadiness(caseId),
      service.getWhonetReadiness(caseId),
    ]).then(([caseReadiness, whonetState]) => {
      if (!mountedRef.current) {
        return;
      }
      setReadiness(caseReadiness);
      setWhonetReadiness(whonetState);
    });

  useEffect(() => {
    mountedRef.current = true;
    loadState();
    return () => {
      mountedRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId, refreshToken]);

  const releaseFinal = () => {
    setSaving(true);
    service
      .releaseFinalReport(caseId)
      .then((state) => {
        if (mountedRef.current) {
          setReleaseState(state);
        }
        if (onReleased) {
          onReleased();
        }
      })
      .then(loadState)
      .finally(() => {
        if (mountedRef.current) {
          setSaving(false);
        }
      });
  };

  return (
    <section aria-labelledby="microbiology-release-heading">
      <Stack gap={4}>
        <h3 id="microbiology-release-heading">
          {intl.formatMessage({ id: "microbiology.release.title" })}
        </h3>
        {readiness && (
          <InlineNotification
            kind={readiness.finalReleaseReady ? "success" : "warning"}
            title={intl.formatMessage({
              id: readiness.finalReleaseReady
                ? "microbiology.readiness.ready"
                : "microbiology.readiness.blocked",
            })}
            subtitle={(readiness.blockers || []).join(", ")}
            hideCloseButton
          />
        )}
        {whonetReadiness && (
          <InlineNotification
            kind={whonetReadiness.whonetReady ? "success" : "warning"}
            title={intl.formatMessage({
              id: whonetReadiness.whonetReady
                ? "microbiology.whonet.ready"
                : "microbiology.whonet.blocked",
            })}
            subtitle={(whonetReadiness.blockers || []).join(", ")}
            hideCloseButton
          />
        )}
        {releaseState && (
          <div data-testid="microbiology-release-state">
            <Tag type="green">{releaseState.finalReleaseState}</Tag>
          </div>
        )}
        <Button
          onClick={releaseFinal}
          disabled={saving || !readiness?.finalReleaseReady}
        >
          {intl.formatMessage({ id: "microbiology.release.final" })}
        </Button>
      </Stack>
    </section>
  );
};

export default ReportReadinessPanel;
