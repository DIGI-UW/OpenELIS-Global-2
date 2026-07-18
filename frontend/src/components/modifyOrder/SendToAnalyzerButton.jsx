import React, { useState, useEffect } from "react";
import {
  Button,
  Modal,
  Select,
  SelectItem,
  InlineNotification,
} from "@carbon/react";
import { SendFilled } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const DISPATCHABLE_MODES = new Set(["LIS_INITIATED", "BOTH"]);

/**
 * Carbon button + modal for OE2 → analyzer LIS-initiated order dispatch.
 * Lists analyzers whose communication_mode allows OE-initiated dispatch
 * (LIS_INITIATED or BOTH); posts to
 * /rest/analyzer/analyzers/{id}/send-order on confirm.
 *
 * OE2 is analyzer-agnostic: it sends only the accession. The backend resolves
 * the accession's ordered tests → their LOINCs and posts a LOINC order to the
 * bridge, which owns LOINC→analyzer-code + message building.
 *
 * Props:
 *   - accessionNumber: string  the order's accession (the backend resolves its
 *                              ordered tests; the analyzer echoes it on results)
 */
const SendToAnalyzerButton = ({ accessionNumber }) => {
  const intl = useIntl();
  const [modalOpen, setModalOpen] = useState(false);
  const [analyzers, setAnalyzers] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    if (!modalOpen) return;
    setFeedback(null);
    getFromOpenElisServer("/rest/analyzer/analyzers", (data) => {
      // GET /rest/analyzer/analyzers returns { analyzers: [...] } (the
      // controller wraps the list in a Map). Tolerate a bare array too, in
      // case the endpoint shape changes.
      const list = Array.isArray(data)
        ? data
        : Array.isArray(data?.analyzers)
          ? data.analyzers
          : null;
      if (!list) {
        setAnalyzers([]);
        return;
      }
      const dispatchable = list.filter((a) =>
        DISPATCHABLE_MODES.has(a.communicationMode),
      );
      setAnalyzers(dispatchable);
      if (dispatchable.length > 0) {
        setSelectedId((curr) => curr || dispatchable[0].id);
      }
    });
  }, [modalOpen]);

  const handleClose = () => {
    setModalOpen(false);
    setFeedback(null);
  };

  const handleSubmit = () => {
    if (!selectedId || submitting) return;
    if (!accessionNumber) {
      setFeedback({
        kind: "error",
        title: intl.formatMessage({ id: "order.send.error" }),
        subtitle: intl.formatMessage({ id: "order.send.missingAccession" }),
      });
      return;
    }
    setSubmitting(true);
    setFeedback(null);
    postToOpenElisServerJsonResponse(
      `/rest/analyzer/analyzers/${selectedId}/send-order`,
      JSON.stringify({ accessionNumber }),
      (response) => {
        setSubmitting(false);
        if (response && response.status === "DISPATCHED") {
          setFeedback({
            kind: "success",
            title: intl.formatMessage({ id: "order.send.success" }),
            subtitle: `${response.protocol || ""} → ${accessionNumber}`,
          });
        } else {
          setFeedback({
            kind: "error",
            title: intl.formatMessage({ id: "order.send.error" }),
            subtitle:
              (response && response.error) ||
              intl.formatMessage({ id: "order.send.unknownError" }),
          });
        }
      },
    );
  };

  return (
    <>
      <Button
        kind="secondary"
        renderIcon={SendFilled}
        onClick={() => setModalOpen(true)}
        data-cy="send-to-analyzer"
      >
        {intl.formatMessage({ id: "order.send.toAnalyzer" })}
      </Button>
      <Modal
        open={modalOpen}
        modalHeading={intl.formatMessage({ id: "order.send.modal.title" })}
        primaryButtonText={intl.formatMessage({
          id: "order.send.modal.confirm",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "order.send.modal.cancel",
        })}
        onRequestClose={handleClose}
        onRequestSubmit={handleSubmit}
        primaryButtonDisabled={
          !selectedId || submitting || analyzers.length === 0
        }
      >
        {feedback && (
          <InlineNotification
            kind={feedback.kind}
            title={feedback.title}
            subtitle={feedback.subtitle}
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem", maxWidth: "100%" }}
          />
        )}
        {analyzers.length === 0 ? (
          <p style={{ marginBottom: "0.5rem" }}>
            {intl.formatMessage({ id: "order.send.modal.noAnalyzers" })}
          </p>
        ) : (
          <>
            <p style={{ marginBottom: "0.75rem" }}>
              {intl.formatMessage(
                { id: "order.send.modal.accession" },
                { accession: accessionNumber || "—" },
              )}
            </p>
            <Select
              id="send-order-analyzer-select"
              labelText={intl.formatMessage({
                id: "order.send.modal.analyzerLabel",
              })}
              value={selectedId}
              onChange={(e) => setSelectedId(e.target.value)}
            >
              {analyzers.map((a) => (
                <SelectItem
                  key={a.id}
                  value={a.id}
                  text={`${a.name} (${a.protocolVersion || "?"})`}
                />
              ))}
            </Select>
          </>
        )}
      </Modal>
    </>
  );
};

export default SendToAnalyzerButton;
