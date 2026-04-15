import React, { useState } from "react";
import {
  Tag,
  Button,
  TextArea,
  Grid,
  Column,
  InlineNotification,
} from "@carbon/react";
import { Checkmark, Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../../utils/Utils";

/**
 * Section D: Approval Status (AHRI BR-F-02)
 * Displays request status and provides inline approve/reject for supervisors.
 */
function ApprovalStatusSection({
  request,
  onStatusChange,
  showActions = true,
}) {
  const intl = useIntl();
  const [approvalNotes, setApprovalNotes] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);

  if (!request) return null;

  const statusTagType = {
    DRAFT: "cool-gray",
    PENDING_APPROVAL: "blue",
    APPROVED: "green",
    IN_PROGRESS: "teal",
    PARTIALLY_COMPLETED: "warm-gray",
    COMPLETED: "green",
    REJECTED: "red",
    CANCELLED: "warm-gray",
  };

  const handleApprove = () => {
    setProcessing(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/requests/${request.id}/approve`,
      JSON.stringify({ approvalNotes: approvalNotes || null }),
      (data) => {
        setProcessing(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        if (onStatusChange) onStatusChange();
      },
    );
  };

  const handleReject = () => {
    if (!rejectionReason.trim()) return;
    setProcessing(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/requests/${request.id}/reject`,
      JSON.stringify({ reason: rejectionReason }),
      (data) => {
        setProcessing(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        setShowRejectForm(false);
        if (onStatusChange) onStatusChange();
      },
    );
  };

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.approval"
          defaultMessage="Section D: Approval"
        />
      </h4>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid condensed>
        <Column lg={16} md={8} sm={4}>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <span style={{ fontWeight: 600 }}>
              <FormattedMessage
                id="biorepo.import.approval.status"
                defaultMessage="Status:"
              />
            </span>
            <Tag type={statusTagType[request.status] || "cool-gray"}>
              {request.status}
            </Tag>
            {request.requestNumber && (
              <span style={{ color: "#525252" }}>{request.requestNumber}</span>
            )}
          </div>
        </Column>

        {request.approvedByName && (
          <>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "0.5rem" }}>
              <span style={{ fontWeight: 600 }}>
                <FormattedMessage
                  id="biorepo.import.approval.approvedBy"
                  defaultMessage="Approved By:"
                />{" "}
              </span>
              {request.approvedByName}
            </Column>
            <Column lg={8} md={4} sm={4} style={{ marginTop: "0.5rem" }}>
              <span style={{ fontWeight: 600 }}>
                <FormattedMessage
                  id="biorepo.import.approval.date"
                  defaultMessage="Date:"
                />{" "}
              </span>
              {request.approvedTimestamp || "-"}
            </Column>
          </>
        )}

        {request.approvalNotes && (
          <Column lg={16} md={8} sm={4} style={{ marginTop: "0.5rem" }}>
            <span style={{ fontWeight: 600 }}>
              <FormattedMessage
                id="biorepo.import.approval.notes"
                defaultMessage="Notes:"
              />{" "}
            </span>
            {request.approvalNotes}
          </Column>
        )}

        {request.rejectionReason && (
          <Column lg={16} md={8} sm={4} style={{ marginTop: "0.5rem" }}>
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "biorepo.import.approval.rejectionReason",
                defaultMessage: "Rejection Reason",
              })}
              subtitle={request.rejectionReason}
              lowContrast
              hideCloseButton
            />
          </Column>
        )}

        {/* Approve/Reject buttons for supervisor when PENDING_APPROVAL */}
        {showActions && request.status === "PENDING_APPROVAL" && (
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            {!showRejectForm ? (
              <div style={{ display: "flex", gap: "1rem" }}>
                <div style={{ flex: 1 }}>
                  <TextArea
                    id="approvalNotes"
                    labelText={intl.formatMessage({
                      id: "biorepo.import.approval.notesInput",
                      defaultMessage: "Approval Notes (optional)",
                    })}
                    value={approvalNotes}
                    onChange={(e) => setApprovalNotes(e.target.value)}
                    rows={2}
                  />
                </div>
                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    alignSelf: "flex-end",
                  }}
                >
                  <Button
                    kind="primary"
                    size="sm"
                    renderIcon={Checkmark}
                    onClick={handleApprove}
                    disabled={processing}
                  >
                    <FormattedMessage
                      id="biorepo.import.approval.approve"
                      defaultMessage="Approve"
                    />
                  </Button>
                  <Button
                    kind="danger--tertiary"
                    size="sm"
                    renderIcon={Close}
                    onClick={() => setShowRejectForm(true)}
                    disabled={processing}
                  >
                    <FormattedMessage
                      id="biorepo.import.approval.reject"
                      defaultMessage="Reject"
                    />
                  </Button>
                </div>
              </div>
            ) : (
              <div>
                <TextArea
                  id="rejectionReason"
                  labelText={intl.formatMessage({
                    id: "biorepo.import.approval.rejectionReasonInput",
                    defaultMessage: "Reason for Rejection",
                  })}
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  rows={2}
                  invalid={!rejectionReason.trim()}
                  invalidText={intl.formatMessage({
                    id: "biorepo.import.approval.rejectionReasonRequired",
                    defaultMessage: "Please provide a reason for rejection",
                  })}
                />
                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    marginTop: "0.5rem",
                  }}
                >
                  <Button
                    kind="danger"
                    size="sm"
                    onClick={handleReject}
                    disabled={processing || !rejectionReason.trim()}
                  >
                    <FormattedMessage
                      id="biorepo.import.approval.confirmReject"
                      defaultMessage="Confirm Rejection"
                    />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => setShowRejectForm(false)}
                  >
                    <FormattedMessage
                      id="label.button.cancel"
                      defaultMessage="Cancel"
                    />
                  </Button>
                </div>
              </div>
            )}
          </Column>
        )}
      </Grid>
    </div>
  );
}

export default ApprovalStatusSection;
