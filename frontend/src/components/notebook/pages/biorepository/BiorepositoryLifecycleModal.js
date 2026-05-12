import React, { useEffect, useMemo, useState } from "react";
import PropTypes from "prop-types";
import { InlineLoading, InlineNotification, Modal, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../../config.json";

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

  if (currentState.workflowStatus === "STORED" || currentState.isPhysicallyInStorage) {
    return { type: "green", label: "Stored" };
  }

  return { type: "gray", label: currentState.workflowStatus || "Unknown" };
};

function BiorepositoryLifecycleModal({ open, onClose, sampleItemId, bioSampleId, sampleLabel }) {
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

  const stateTag = useMemo(() => resolveStateTag(lifecycle?.currentState), [lifecycle]);

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
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.sample"
                defaultMessage="Sample"
              />
              :
            </strong>{" "}
            {sampleLabel || lifecycle?.sampleExternalId || lifecycle?.accessionNumber || "N/A"}
          </div>
          <Tag type={stateTag.type} data-testid="biorepository-lifecycle-state-tag">
            {stateTag.label}
          </Tag>
        </div>

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
          <div style={{ maxHeight: "55vh", overflowY: "auto", display: "grid", gap: "0.75rem" }}>
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
                <div style={{ display: "flex", justifyContent: "space-between", gap: "0.5rem" }}>
                  <strong>{event.custodyAction || event.eventType || "EVENT"}</strong>
                  <span style={{ color: "#525252", fontSize: "0.875rem" }}>
                    {event.eventTimestamp ? new Date(event.eventTimestamp).toLocaleString() : "N/A"}
                  </span>
                </div>
                <div style={{ fontSize: "0.875rem", marginTop: "0.375rem" }}>
                  {event.fromWorkflowStatus || "-"} {"->"} {event.toWorkflowStatus || "-"}
                </div>
                <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                  {event.fromLocationDisplay || "-"} {"->"} {event.toLocationDisplay || "-"}
                </div>
                {event.notes && (
                  <div style={{ marginTop: "0.25rem", fontSize: "0.875rem" }}>{event.notes}</div>
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
};

BiorepositoryLifecycleModal.defaultProps = {
  sampleItemId: null,
  bioSampleId: null,
  sampleLabel: "",
};

export default BiorepositoryLifecycleModal;
