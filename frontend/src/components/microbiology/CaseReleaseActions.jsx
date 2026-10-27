import React, { useState } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  InlineLoading,
  InlineNotification,
  Modal,
} from "@carbon/react";

export default function CaseReleaseActions({
  detail,
  service,
  readiness,
  onReleased,
  partialReady,
}) {
  const intl = useIntl();
  const [open, setOpen] = useState(false);
  const [summary, setSummary] = useState(null);
  const [confirmIncoming, setConfirmIncoming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const pendingIncomingIds = readiness?.pendingIncomingIds || [];
  const blocked = !readiness || !readiness.finalReleaseReady;
  const finalLabel = detail.amendmentOpen
    ? intl.formatMessage({ id: "microbiology.m6.amendedRelease" })
    : blocked && readiness
      ? intl.formatMessage(
          { id: "microbiology.release.final.missingCount" },
          { count: (readiness.blockers || []).length },
        )
      : intl.formatMessage({ id: "microbiology.m6.finalRelease" });
  // i18n-keys: microbiology.m6.blocker.*, microbiology.m6.field.*
  const blockerLabel = (blocker) => {
    const [code, field] = blocker.split(":");
    if (code === "CLINICAL_FIELD_REQUIRED") {
      return intl.formatMessage(
        { id: "microbiology.m6.blocker.fieldRequired" },
        {
          field: intl.formatMessage({
            id: `microbiology.m6.field.${field}`,
          }),
        },
      );
    }
    const label = intl.formatMessage({ id: `microbiology.m6.blocker.${code}` });
    const detailLabel = readiness?.blockerLabels?.[blocker];
    return detailLabel ? `${label}: ${detailLabel}` : label;
  };

  const openDialog = () => {
    setOpen(true);
    setSummary(null);
    setConfirmIncoming(false);
    setError(null);
    Promise.all([
      service.getReport(detail.id),
      service.getReportVersions(detail.id),
    ])
      .then(([report, versions]) =>
        setSummary({
          count: report.validatedResults,
          version: versions.length + 1,
        }),
      )
      .catch((loadError) => setError(loadError));
  };

  const release = async (type) => {
    setBusy(true);
    setError(null);
    try {
      await service.releaseReport(
        detail.id,
        type,
        type === "preliminary" ? [] : pendingIncomingIds,
      );
      await onReleased?.();
      return true;
    } catch (releaseError) {
      setError(releaseError);
      return false;
    } finally {
      setBusy(false);
    }
  };

  const confirmRelease = async () => {
    if (await release(detail.amendmentOpen ? "amended" : "final"))
      setOpen(false);
  };

  return (
    <>
      <div>
        {detail.canWrite && !detail.amendmentOpen && (
          <Button
            kind="tertiary"
            disabled={busy || !readiness || !partialReady}
            onClick={() => release("preliminary")}
          >
            {intl.formatMessage({ id: "microbiology.m6.partialRelease" })}
          </Button>
        )}
        {detail.canValidate && (
          <Button disabled={busy || blocked} onClick={openDialog}>
            {finalLabel}
          </Button>
        )}
      </div>
      {error && !open && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "microbiology.release.failed" })}
          hideCloseButton
        />
      )}
      {open && (
        <Modal
          open
          modalHeading={intl.formatMessage({
            id: detail.amendmentOpen
              ? "microbiology.release.amended.confirmTitle"
              : "microbiology.release.final.confirmTitle",
          })}
          primaryButtonText={intl.formatMessage({
            id: detail.amendmentOpen
              ? "microbiology.m6.amendedRelease"
              : "microbiology.release.final.confirm",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "common.cancel",
          })}
          primaryButtonDisabled={
            busy ||
            !summary ||
            (pendingIncomingIds.length > 0 && !confirmIncoming)
          }
          onRequestClose={() => setOpen(false)}
          onSecondarySubmit={() => setOpen(false)}
          onRequestSubmit={confirmRelease}
        >
          {summary ? (
            <p>
              {intl.formatMessage(
                { id: "microbiology.release.final.confirmBody" },
                { count: summary.count, version: summary.version },
              )}
            </p>
          ) : (
            <InlineLoading
              description={intl.formatMessage({ id: "common.loading" })}
            />
          )}
          {pendingIncomingIds.length > 0 && (
            <Checkbox
              id="confirm-incoming-release"
              aria-label={intl.formatMessage({
                id: "microbiology.incoming.confirmRelease",
              })}
              labelText={intl.formatMessage({
                id: "microbiology.incoming.confirmRelease",
              })}
              checked={confirmIncoming}
              onChange={(_, { checked }) => setConfirmIncoming(checked)}
            />
          )}
          {error && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "microbiology.release.failed",
              })}
              hideCloseButton
            />
          )}
          {(error?.response?.blockers || []).length > 0 && (
            <ul>
              {error.response.blockers.map((blocker) => (
                <li key={blocker}>{blockerLabel(blocker)}</li>
              ))}
            </ul>
          )}
        </Modal>
      )}
    </>
  );
}
