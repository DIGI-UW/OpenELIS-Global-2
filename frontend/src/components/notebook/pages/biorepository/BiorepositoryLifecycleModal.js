import React, { useEffect, useMemo, useState } from "react";
import PropTypes from "prop-types";
import { InlineLoading, InlineNotification, Modal, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../../config.json";
import { formatTransferSourceLab } from "./biorepositoryTransferHelpers";
import {
  buildLifecycleSummaryRows,
  mergeTransferSummary,
  shouldShowLocationTransition,
  shouldShowWorkflowTransition,
} from "./biorepositoryLifecycleHelpers";

const resolveStateTag = (currentState) => {
  if (!currentState) {
    return { type: "gray", label: "Unknown" };
  }

  if (currentState.workflowStatus === "DISPOSED") {
    return { type: "red", label: "Disposed" };
  }

  if (currentState.awaitingRestorage) {
    return { type: "purple", label: "Returned - Awaiting Re-storage" };
  }

  if (currentState.workflowStatus === "IN_USE") {
    return { type: "blue", label: "Checked Out" };
  }

  if (
    currentState.workflowStatus === "STORED" ||
    currentState.isPhysicallyInStorage
  ) {
    return { type: "green", label: "Stored" };
  }

  return { type: "gray", label: currentState.workflowStatus || "Unknown" };
};

function BiorepositoryLifecycleModal({
  open,
  onClose,
  sampleItemId,
  bioSampleId,
  sampleLabel,
  transferContext,
}) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lifecycle, setLifecycle] = useState(null);

  useEffect(() => {
    if (!open || (!sampleItemId && !bioSampleId)) {
      return;
    }

    const endpoint = sampleItemId
      ? `/rest/biorepository/lifecycle/sample-item/${sampleItemId}`
      : `/rest/biorepository/lifecycle/bio-sample/${bioSampleId}`;

    setIsLoading(true);
    setError(null);

    fetch(`${config.serverBaseUrl}${endpoint}`, {
      credentials: "include",
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load lifecycle");
        }
        return response.json();
      })
      .then((data) => {
        setLifecycle(data || null);
      })
      .catch((fetchError) => {
        setError(fetchError.message || "Unable to load lifecycle");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [open, sampleItemId, bioSampleId]);

  const stateTag = useMemo(
    () => resolveStateTag(lifecycle?.currentState),
    [lifecycle],
  );

  const summaryRows = useMemo(() => {
    const merged = mergeTransferSummary(
      lifecycle?.transferSummary,
      transferContext,
    );
    return buildLifecycleSummaryRows(merged);
  }, [lifecycle, transferContext]);

  const currentState = lifecycle?.currentState;

  return (
    <Modal
      open={open}
      passiveModal
      size="lg"
      data-testid="biorepository-lifecycle-modal"
      modalHeading={intl.formatMessage({
        id: "biorepository.lifecycle.modal.title",
        defaultMessage: "Sample Lifecycle",
      })}
      onRequestClose={onClose}
    >
      <div style={{ display: "grid", gap: "1rem" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.sample"
                defaultMessage="Sample"
              />
              :
            </strong>{" "}
            {sampleLabel ||
              lifecycle?.sampleExternalId ||
              lifecycle?.accessionNumber ||
              "N/A"}
          </div>
          <Tag
            type={stateTag.type}
            data-testid="biorepository-lifecycle-state-tag"
          >
            {stateTag.label}
          </Tag>
        </div>

        {currentState?.lastKnownStorageLocation && (
          <div style={{ fontSize: "0.875rem", color: "#525252" }}>
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.storageLocation"
                defaultMessage="Storage location"
              />
              :
            </strong>{" "}
            {currentState.lastKnownStorageLocation}
          </div>
        )}

        {currentState?.currentCustodian && (
          <div style={{ fontSize: "0.875rem", color: "#525252" }}>
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.custodian"
                defaultMessage="Current custodian"
              />
              :
            </strong>{" "}
            {currentState.currentCustodian}
          </div>
        )}

        {summaryRows.length > 0 && (
          <div
            data-testid="biorepository-lifecycle-transfer-summary"
            style={{
              border: "1px solid #c6c6c6",
              borderRadius: "4px",
              padding: "0.75rem",
              background: "#f4f4f4",
              display: "grid",
              gap: "0.375rem",
            }}
          >
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.transferSummary"
                defaultMessage="Transfer details"
              />
            </strong>
            {summaryRows.map((row) => (
              <div key={row.label} style={{ fontSize: "0.875rem" }}>
                <strong>{row.label}:</strong> {row.value}
              </div>
            ))}
          </div>
        )}

        {isLoading && (
          <InlineLoading
            description={intl.formatMessage({
              id: "biorepository.lifecycle.modal.loading",
              defaultMessage: "Loading lifecycle...",
            })}
          />
        )}

        {error && (
          <InlineNotification
            lowContrast
            kind="error"
            title={intl.formatMessage({
              id: "biorepository.lifecycle.modal.error",
              defaultMessage: "Unable to load lifecycle",
            })}
            subtitle={error}
          />
        )}

        {!isLoading && !error && lifecycle?.events?.length > 0 && (
          <div
            style={{
              maxHeight: "55vh",
              overflowY: "auto",
              display: "grid",
              gap: "0.75rem",
            }}
          >
            {lifecycle.events.map((event, index) => (
              <div
                key={`${event.sourceRecordType || "event"}-${event.sourceRecordId || index}-${index}`}
                data-testid="biorepository-lifecycle-event"
                style={{
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                  padding: "0.75rem",
                  background: "#fff",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: "0.5rem",
                  }}
                >
                  <strong>
                    {event.custodyAction || event.eventType || "EVENT"}
                  </strong>
                  <span style={{ color: "#525252", fontSize: "0.875rem" }}>
                    {event.eventTimestamp
                      ? new Date(event.eventTimestamp).toLocaleString()
                      : "N/A"}
                  </span>
                </div>
                {event.actorDisplayName && (
                  <div style={{ fontSize: "0.875rem", marginTop: "0.25rem" }}>
                    <FormattedMessage
                      id="biorepository.lifecycle.modal.actor"
                      defaultMessage="By"
                    />
                    : {event.actorDisplayName}
                  </div>
                )}
                {event.stage && (
                  <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage
                      id="biorepository.lifecycle.modal.stage"
                      defaultMessage="Stage"
                    />
                    : {event.stage}
                  </div>
                )}
                {shouldShowWorkflowTransition(event) && (
                  <div style={{ fontSize: "0.875rem", marginTop: "0.375rem" }}>
                    {event.fromWorkflowStatus || "-"} {"-> "}
                    {event.toWorkflowStatus || "-"}
                  </div>
                )}
                {shouldShowLocationTransition(event) && (
                  <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                    {formatTransferSourceLab(event.fromLocationDisplay) || "-"}{" "}
                    {"-> "}
                    {formatTransferSourceLab(event.toLocationDisplay) || "-"}
                  </div>
                )}
                {event.storageCoordinates && (
                  <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage
                      id="biorepository.lifecycle.modal.coordinates"
                      defaultMessage="Coordinates"
                    />
                    : {event.storageCoordinates}
                  </div>
                )}
                {event.temperature != null && (
                  <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage
                      id="biorepository.lifecycle.modal.temperature"
                      defaultMessage="Temperature"
                    />
                    : {event.temperature}
                  </div>
                )}
                {event.notes && (
                  <div style={{ marginTop: "0.25rem", fontSize: "0.875rem" }}>
                    {event.notes}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  );
}

BiorepositoryLifecycleModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  sampleItemId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  bioSampleId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  sampleLabel: PropTypes.string,
  transferContext: PropTypes.shape({
    sourceLab: PropTypes.string,
    requestSourceLab: PropTypes.string,
    requestStatus: PropTypes.string,
    status: PropTypes.string,
    projectName: PropTypes.string,
    transferReason: PropTypes.string,
    requestNotes: PropTypes.string,
    requestedByName: PropTypes.string,
    requestedTimestamp: PropTypes.string,
    rejectionReason: PropTypes.string,
    externalId: PropTypes.string,
    accessionNumber: PropTypes.string,
    sampleType: PropTypes.string,
    quantity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    unitOfMeasure: PropTypes.string,
    sampleCondition: PropTypes.string,
    preservationMedium: PropTypes.string,
    collectionDate: PropTypes.string,
  }),
};

BiorepositoryLifecycleModal.defaultProps = {
  sampleItemId: null,
  bioSampleId: null,
  sampleLabel: "",
  transferContext: null,
};

export default BiorepositoryLifecycleModal;
