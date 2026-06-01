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
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";

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
  retrievalContext,
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
      .then(async (response) => {
        const contentType = response.headers.get("content-type");
        const isJson = contentType?.includes("application/json");
        const body = isJson ? await response.json() : null;

        if (!response.ok) {
          const message =
            body?.error ||
            body?.message ||
            response.statusText ||
            "Failed to load lifecycle";
          throw new Error(message);
        }

        return body;
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
  const retrievalSummary = lifecycle?.retrievalSummary;

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

        {retrievalContext && (
          <div
            style={{
              padding: "0.75rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              fontSize: "0.875rem",
            }}
          >
            <div style={{ marginBottom: "0.5rem" }}>
              <strong>
                <FormattedMessage
                  id="biorepository.retrieval.lifecycle.requestedReference"
                  defaultMessage="Requested Reference"
                />
                :
              </strong>{" "}
              {retrievalContext.requestedReference || "—"}
              {retrievalContext.requestedQuantity != null &&
                ` (${formatQuantityWithUnit(
                  retrievalContext.requestedQuantity,
                  retrievalContext.requestedUnit,
                )})`}
            </div>
            <div>
              <strong>
                <FormattedMessage
                  id="biorepository.retrieval.lifecycle.fulfilledSample"
                  defaultMessage="Fulfilled Sample"
                />
                :
              </strong>{" "}
              {retrievalContext.fulfilledSample || sampleLabel || "—"}
              {retrievalContext.storagePath &&
                ` — ${retrievalContext.storagePath}`}
              {retrievalContext.quantityReleased != null &&
                ` — released: ${formatQuantityWithUnit(
                  retrievalContext.quantityReleased,
                  retrievalContext.requestedUnit,
                )}`}
            </div>
          </div>
        )}

        {currentState?.lastKnownStorageLocation && (
          <div style={{ fontSize: "0.875rem", color: "#525252" }}>
            <strong>
              <FormattedMessage
                id="biorepository.lifecycle.modal.storageLocation"
                defaultMessage="Current storage"
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

        {(currentState?.quantityRequested != null ||
          currentState?.quantityReleased != null ||
          currentState?.remainingQuantity != null) && (
          <div style={{ fontSize: "0.875rem", color: "#525252" }}>
            {currentState.quantityRequested != null && (
              <span>
                <strong>Requested:</strong>{" "}
                {formatQuantityWithUnit(
                  currentState.quantityRequested,
                  currentState.unitOfMeasure,
                )}{" "}
              </span>
            )}
            {currentState.quantityReleased != null && (
              <span>
                <strong>Released:</strong>{" "}
                {formatQuantityWithUnit(
                  currentState.quantityReleased,
                  currentState.unitOfMeasure,
                )}{" "}
              </span>
            )}
            {currentState.remainingQuantity != null && (
              <span>
                <strong>Remaining:</strong>{" "}
                {formatQuantityWithUnit(
                  currentState.remainingQuantity,
                  currentState.unitOfMeasure,
                )}
              </span>
            )}
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
                defaultMessage="Origin transfer details"
              />
            </strong>
            {summaryRows.map((row) => (
              <div key={row.label} style={{ fontSize: "0.875rem" }}>
                <strong>{row.label}:</strong> {row.value}
              </div>
            ))}
          </div>
        )}

        {retrievalSummary && (
          <div
            data-testid="biorepository-lifecycle-retrieval-summary"
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
                id="biorepository.lifecycle.modal.retrievalSummary"
                defaultMessage="Withdrawal details"
              />
            </strong>
            {retrievalSummary.requestNumber && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Request number:</strong> {retrievalSummary.requestNumber}
              </div>
            )}
            {retrievalSummary.requestorName && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Requestor name:</strong> {retrievalSummary.requestorName}
              </div>
            )}
            {retrievalSummary.requesterLabUnit && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Lab unit:</strong> {retrievalSummary.requesterLabUnit}
              </div>
            )}
            {retrievalSummary.principalInvestigator && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Principal investigator:</strong>{" "}
                {retrievalSummary.principalInvestigator}
              </div>
            )}
            {retrievalSummary.projectTitle && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Project title:</strong> {retrievalSummary.projectTitle}
              </div>
            )}
            {retrievalSummary.requesterContactInfo && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Contact information:</strong>{" "}
                {retrievalSummary.requesterContactInfo}
              </div>
            )}
            {retrievalSummary.requestPurpose && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Intended use:</strong> {retrievalSummary.requestPurpose}
              </div>
            )}
            {(retrievalSummary.quantityRequested != null ||
              retrievalSummary.unitOfMeasure ||
              retrievalSummary.remark) && (
              <div style={{ fontSize: "0.875rem" }}>
                {retrievalSummary.quantityRequested != null && (
                  <>
                    <strong>Quantity requested:</strong>{" "}
                    {formatQuantityWithUnit(
                      retrievalSummary.quantityRequested,
                      retrievalSummary.unitOfMeasure,
                    )}{" "}
                  </>
                )}
                {retrievalSummary.remark && (
                  <>
                    <strong>Remark:</strong> {retrievalSummary.remark}
                  </>
                )}
              </div>
            )}
            {(retrievalSummary.quantityReleased != null ||
              retrievalSummary.receivedByName ||
              retrievalSummary.releasedTimestamp ||
              retrievalSummary.conditionAtRelease) && (
              <div style={{ fontSize: "0.875rem" }}>
                {retrievalSummary.quantityReleased != null && (
                  <>
                    <strong>Released:</strong>{" "}
                    {formatQuantityWithUnit(
                      retrievalSummary.quantityReleased,
                      retrievalSummary.unitOfMeasure,
                    )}{" "}
                  </>
                )}
                {retrievalSummary.receivedByName && (
                  <>
                    <strong>Received by:</strong>{" "}
                    {retrievalSummary.receivedByName}{" "}
                  </>
                )}
                {retrievalSummary.conditionAtRelease && (
                  <>
                    <strong>Condition at release:</strong>{" "}
                    {retrievalSummary.conditionAtRelease}{" "}
                  </>
                )}
                {retrievalSummary.releasedTimestamp && (
                  <>
                    <strong>Released at:</strong>{" "}
                    {new Date(
                      retrievalSummary.releasedTimestamp,
                    ).toLocaleString()}
                  </>
                )}
              </div>
            )}
            {retrievalSummary.checkoutStoragePath && (
              <div style={{ fontSize: "0.875rem" }}>
                <strong>Checkout storage:</strong>{" "}
                {retrievalSummary.checkoutStoragePath}
                {retrievalSummary.checkoutStorageCoordinates
                  ? ` (${retrievalSummary.checkoutStorageCoordinates})`
                  : ""}
              </div>
            )}
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
    sourceNotebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    sourceNotebookEntryId: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
    ]),
    sourceStorageLocation: PropTypes.string,
    sourceStorageLocationType: PropTypes.string,
    sourceStoragePositionCoordinate: PropTypes.string,
  }),
  retrievalContext: PropTypes.shape({
    requestedReference: PropTypes.string,
    requestedQuantity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    requestedUnit: PropTypes.string,
    fulfilledSample: PropTypes.string,
    storagePath: PropTypes.string,
    quantityReleased: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
};

BiorepositoryLifecycleModal.defaultProps = {
  sampleItemId: null,
  bioSampleId: null,
  sampleLabel: "",
  transferContext: null,
  retrievalContext: null,
};

export default BiorepositoryLifecycleModal;
