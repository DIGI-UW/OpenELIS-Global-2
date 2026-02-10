import React, { useState } from "react";
import {
  Button,
  InlineNotification,
  TextArea,
  TextInput,
  Grid,
  Column,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../utils/Utils";

const ResultSubmission = ({
  distributionId,
  organizationId,
  onSubmitComplete,
}) => {
  const intl = useIntl();
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [isLate, setIsLate] = useState(false);
  const [justification, setJustification] = useState("");
  const [supervisorUserId, setSupervisorUserId] = useState("");

  const handleSubmit = () => {
    setSubmitting(true);
    setResult(null);

    postToOpenElisServerJsonResponse(
      `/rest/eqa/distributions/${distributionId}/submit/${organizationId}`,
      JSON.stringify({}),
      (response) => {
        setSubmitting(false);
        if (response && response.supervisorApprovalRequired) {
          setIsLate(true);
        } else if (response && response.success) {
          setResult({ success: true });
          if (onSubmitComplete) onSubmitComplete(response);
        } else {
          setResult({
            success: false,
            error: response?.error || "Unknown error",
          });
        }
      },
    );
  };

  const handleLateApproval = () => {
    setSubmitting(true);

    postToOpenElisServerJsonResponse(
      `/rest/eqa/distributions/${distributionId}/submit/${organizationId}/approve-late`,
      JSON.stringify({ justification, supervisorUserId }),
      (response) => {
        setSubmitting(false);
        if (response && response.approved) {
          setResult({ success: true });
          setIsLate(false);
          if (onSubmitComplete) onSubmitComplete(response);
        } else {
          setResult({
            success: false,
            error: response?.error || "Approval failed",
          });
        }
      },
    );
  };

  return (
    <div>
      <h4>{intl.formatMessage({ id: "eqa.submission.title" })}</h4>
      <p>{intl.formatMessage({ id: "eqa.submission.fhir.description" })}</p>

      {!isLate && (
        <Button onClick={handleSubmit} disabled={submitting}>
          {intl.formatMessage({ id: "eqa.submission.fhir" })}
        </Button>
      )}

      {isLate && (
        <div style={{ marginTop: "1rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({ id: "eqa.submission.late.warning" })}
            hideCloseButton
            lowContrast
          />
          <Grid condensed style={{ marginTop: "1rem" }}>
            <Column lg={8} md={8} sm={4}>
              <TextArea
                id="late-justification"
                labelText={intl.formatMessage({
                  id: "eqa.submission.late.justification",
                })}
                placeholder={intl.formatMessage({
                  id: "eqa.submission.late.justification.placeholder",
                })}
                value={justification}
                onChange={(e) => setJustification(e.target.value)}
              />
            </Column>
            <Column lg={8} md={8} sm={4}>
              <TextInput
                id="supervisor-id"
                labelText={intl.formatMessage({
                  id: "eqa.submission.late.supervisor",
                })}
                value={supervisorUserId}
                onChange={(e) => setSupervisorUserId(e.target.value)}
              />
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
              <Button
                onClick={handleLateApproval}
                disabled={submitting || !justification || !supervisorUserId}
              >
                {intl.formatMessage({ id: "eqa.submission.late.approve" })}
              </Button>
            </Column>
          </Grid>
        </div>
      )}

      {result && result.success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "eqa.submission.success" })}
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}

      {result && !result.success && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "eqa.submission.error" })}
          subtitle={result.error}
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}
    </div>
  );
};

export default ResultSubmission;
