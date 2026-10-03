import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  InlineNotification,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  TextArea,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyService from "./MicrobiologyService";

const isErrorResponse = (response) =>
  response?.status === 0 || response?.status >= 400;

const AmendmentHistoryPanel = ({
  caseId,
  finalReleaseState,
  service = MicrobiologyService,
  onCaseUpdated,
  active = true,
}) => {
  const intl = useIntl();
  const headingRef = useRef(null);
  const [amendments, setAmendments] = useState([]);
  const [reportVersions, setReportVersions] = useState([]);
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [announcement, setAnnouncement] = useState("");

  const amendmentOpen = finalReleaseState === "AMENDMENT_IN_PROGRESS";
  const finalReleased = finalReleaseState === "FINAL_RELEASED";

  const loadHistory = () =>
    Promise.all([
      service.getCaseAmendments(caseId),
      service.getCaseReportVersions(caseId),
    ]).then(([amendmentHistory, versions]) => {
      setAmendments(Array.isArray(amendmentHistory) ? amendmentHistory : []);
      setReportVersions(Array.isArray(versions) ? versions : []);
    });

  useEffect(() => {
    if (!active) {
      return undefined;
    }
    let mounted = true;
    Promise.all([
      service.getCaseAmendments(caseId),
      service.getCaseReportVersions(caseId),
    ])
      .then(([amendmentHistory, versions]) => {
        if (!mounted) {
          return;
        }
        setAmendments(Array.isArray(amendmentHistory) ? amendmentHistory : []);
        setReportVersions(Array.isArray(versions) ? versions : []);
      })
      .catch((loadError) => {
        if (mounted) {
          setError(loadError?.message || "AMENDMENT_HISTORY_LOAD_FAILED");
        }
      });
    return () => {
      mounted = false;
    };
  }, [active, caseId, service]);

  const runAction = (operation, successMessageId) => {
    setSaving(true);
    setError("");
    setAnnouncement("");
    return operation()
      .then((response) => {
        if (isErrorResponse(response)) {
          throw response;
        }
        setReason("");
        return loadHistory().then(() => {
          if (onCaseUpdated) {
            return onCaseUpdated();
          }
          return undefined;
        });
      })
      .then(() => {
        setAnnouncement(intl.formatMessage({ id: successMessageId }));
        headingRef.current?.focus();
      })
      .catch((actionError) => {
        setError(
          actionError?.message ||
            actionError?.error ||
            "AMENDMENT_ACTION_FAILED",
        );
      })
      .finally(() => setSaving(false));
  };

  const versionNumberFor = (versionId) =>
    reportVersions.find((version) => version.id === versionId)?.versionNumber;

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-amendment-card"
      aria-labelledby="microbiology-amendment-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3
            id="microbiology-amendment-heading"
            ref={headingRef}
            tabIndex={-1}
          >
            {intl.formatMessage({ id: "microbiology.amendment.title" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.amendment.hint" })}
          </p>
        </div>
        <Tag type={amendmentOpen ? "purple" : "cool-gray"}>
          {intl.formatMessage({
            id: amendmentOpen
              ? "microbiology.amendment.status.open"
              : "microbiology.amendment.status.closed",
          })}
        </Tag>
      </div>

      {announcement && (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={announcement}
        />
      )}
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "microbiology.amendment.actionError",
          })}
          subtitle={formatMicrobiologyEnum(error, intl)}
        />
      )}

      {(finalReleased || amendmentOpen) && (
        <div className="microbiology-amendment-actions">
          <TextArea
            id="microbiology-amendment-reason"
            labelText={intl.formatMessage({
              id: amendmentOpen
                ? "microbiology.amendment.cancelReason"
                : "microbiology.amendment.reason",
            })}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <div className="microbiology-report-actions">
            {amendmentOpen ? (
              <>
                <Button
                  kind="danger--tertiary"
                  disabled={saving || !reason.trim()}
                  onClick={() =>
                    runAction(
                      () =>
                        service.cancelCaseAmendment(caseId, {
                          reason: reason.trim(),
                        }),
                      "microbiology.amendment.cancelled",
                    )
                  }
                >
                  {intl.formatMessage({
                    id: "microbiology.amendment.cancel",
                  })}
                </Button>
                <Button
                  disabled={saving}
                  onClick={() =>
                    runAction(
                      () => service.releaseAmendedReport(caseId),
                      "microbiology.amendment.released",
                    )
                  }
                >
                  {intl.formatMessage({
                    id: "microbiology.amendment.release",
                  })}
                </Button>
              </>
            ) : (
              <Button
                disabled={saving || !reason.trim()}
                onClick={() =>
                  runAction(
                    () =>
                      service.openCaseAmendment(caseId, {
                        reason: reason.trim(),
                      }),
                    "microbiology.amendment.opened",
                  )
                }
              >
                {intl.formatMessage({ id: "microbiology.amendment.open" })}
              </Button>
            )}
          </div>
        </div>
      )}

      <div className="microbiology-amendment-history">
        <div>
          <h4>
            {intl.formatMessage({ id: "microbiology.amendment.history" })}
          </h4>
          {amendments.length === 0 ? (
            <p>{intl.formatMessage({ id: "microbiology.amendment.none" })}</p>
          ) : (
            <StructuredListWrapper
              aria-label={intl.formatMessage({
                id: "microbiology.amendment.history",
              })}
            >
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>
                    {intl.formatMessage({
                      id: "microbiology.amendment.number",
                    })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({
                      id: "microbiology.amendment.reason",
                    })}
                  </StructuredListCell>
                  <StructuredListCell head>
                    {intl.formatMessage({
                      id: "microbiology.amendment.status",
                    })}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                {amendments.map((amendment) => (
                  <StructuredListRow key={amendment.id}>
                    <StructuredListCell>
                      {intl.formatMessage(
                        { id: "microbiology.amendment.numberValue" },
                        { number: amendment.sequenceNumber },
                      )}
                    </StructuredListCell>
                    <StructuredListCell>{amendment.reason}</StructuredListCell>
                    <StructuredListCell>
                      <Tag
                        type={
                          amendment.status === "RELEASED"
                            ? "green"
                            : amendment.status === "OPEN"
                              ? "purple"
                              : "warm-gray"
                        }
                      >
                        {formatMicrobiologyEnum(amendment.status, intl)}
                      </Tag>
                      {amendment.openedBy && (
                        <div className="microbiology-list__meta">
                          {amendment.openedBy}
                        </div>
                      )}
                    </StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
          )}
        </div>

        <div>
          <h4>
            {intl.formatMessage({
              id: "microbiology.amendment.reportVersions",
            })}
          </h4>
          {reportVersions.length === 0 ? (
            <p>
              {intl.formatMessage({
                id: "microbiology.amendment.noReportVersions",
              })}
            </p>
          ) : (
            <ol className="microbiology-list">
              {reportVersions.map((version) => (
                <li className="microbiology-list__row" key={version.id}>
                  <div className="microbiology-amendment-version-heading">
                    <strong>
                      {intl.formatMessage(
                        { id: "microbiology.amendment.version" },
                        { number: version.versionNumber },
                      )}
                    </strong>
                    <Tag
                      type={version.releaseType === "FINAL" ? "blue" : "purple"}
                    >
                      {formatMicrobiologyEnum(version.releaseType, intl)}
                    </Tag>
                  </div>
                  <p>{version.content}</p>
                  {version.correctsVersionId && (
                    <div className="microbiology-list__meta">
                      {intl.formatMessage(
                        { id: "microbiology.amendment.correctsVersion" },
                        {
                          number:
                            versionNumberFor(version.correctsVersionId) || "?",
                        },
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </section>
  );
};

export default AmendmentHistoryPanel;
