import React, { useState, useEffect } from "react";
import {
  Modal,
  TextArea,
  TextInput,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Button,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const ElectronicSignature = ({ reportId }) => {
  const intl = useIntl();
  const [signatures, setSignatures] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [userId, setUserId] = useState("");
  const [comment, setComment] = useState("");
  const [signSuccess, setSignSuccess] = useState(false);

  const fetchSignatures = () => {
    getFromOpenElisServer(`/rest/qc/reports/${reportId}/signatures`, (data) => {
      if (data && Array.isArray(data)) {
        setSignatures(data);
      }
    });
  };

  useEffect(() => {
    if (reportId) {
      fetchSignatures();
    }
  }, [reportId]);

  const handleSign = () => {
    postToOpenElisServerJsonResponse(
      `/rest/qc/reports/${reportId}/sign`,
      JSON.stringify({ userId, comment }),
      (response) => {
        if (response && response.id) {
          setSignSuccess(true);
          setShowModal(false);
          setUserId("");
          setComment("");
          fetchSignatures();
          setTimeout(() => setSignSuccess(false), 3000);
        }
      },
    );
  };

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1rem",
        }}
      >
        <h4>{intl.formatMessage({ id: "qc.signature.title" })}</h4>
        <Button size="sm" onClick={() => setShowModal(true)}>
          {intl.formatMessage({ id: "qc.signature.sign" })}
        </Button>
      </div>

      {signSuccess && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "qc.signature.success" })}
          hideCloseButton
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {signatures.length === 0 ? (
        <p>{intl.formatMessage({ id: "qc.signature.noSignatures" })}</p>
      ) : (
        <StructuredListWrapper>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                {intl.formatMessage({ id: "qc.signature.userId" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "qc.signature.signedAt" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "qc.signature.ipAddress" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "qc.signature.comment" })}
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {signatures.map((sig) => (
              <StructuredListRow key={sig.id}>
                <StructuredListCell>{sig.userId}</StructuredListCell>
                <StructuredListCell>{sig.signedAt}</StructuredListCell>
                <StructuredListCell>{sig.ipAddress || "—"}</StructuredListCell>
                <StructuredListCell>{sig.comment || "—"}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}

      {showModal && (
        <Modal
          open
          modalHeading={intl.formatMessage({ id: "qc.signature.sign" })}
          primaryButtonText={intl.formatMessage({ id: "qc.signature.sign" })}
          secondaryButtonText={intl.formatMessage({ id: "eqa.program.cancel" })}
          onRequestClose={() => setShowModal(false)}
          onRequestSubmit={handleSign}
          onSecondarySubmit={() => setShowModal(false)}
          primaryButtonDisabled={!userId.trim()}
        >
          <TextInput
            id="sign-user-id"
            labelText={intl.formatMessage({ id: "qc.signature.userId" })}
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
          />
          <TextArea
            id="sign-comment"
            labelText={intl.formatMessage({ id: "qc.signature.comment" })}
            placeholder={intl.formatMessage({
              id: "qc.signature.comment.placeholder",
            })}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            style={{ marginTop: "1rem" }}
          />
        </Modal>
      )}
    </div>
  );
};

export default ElectronicSignature;
