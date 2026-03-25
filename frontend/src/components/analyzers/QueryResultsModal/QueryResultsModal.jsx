import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { queryResults } from "../../../services/analyzerService";
import "./QueryResultsModal.css";

const QueryResultsModal = ({ analyzer, open, onClose }) => {
  const intl = useIntl();
  const [accessionNumber, setAccessionNumber] = useState("");
  const [testCodesRaw, setTestCodesRaw] = useState("");
  const [status, setStatus] = useState("initial");
  const [message, setMessage] = useState("");
  const [importedCount, setImportedCount] = useState(0);

  useEffect(() => {
    if (open && analyzer) {
      setAccessionNumber("");
      setTestCodesRaw("");
      setStatus("initial");
      setMessage("");
      setImportedCount(0);
    }
  }, [open, analyzer]);

  const handleSubmit = () => {
    if (!analyzer?.id) {
      setStatus("error");
      setMessage(
        intl.formatMessage({
          id: "analyzer.queryResults.validation.analyzerIdRequired",
        }),
      );
      return;
    }
    const trimmed = (accessionNumber || "").trim();
    if (!trimmed) {
      setStatus("error");
      setMessage(
        intl.formatMessage({
          id: "analyzer.queryResults.validation.accessionRequired",
        }),
      );
      return;
    }
    const testCodes = testCodesRaw
      .trim()
      .split(/[\s,]+/)
      .filter(Boolean);
    const testCodesFilter = testCodes.length > 0 ? testCodes : null;

    setStatus("loading");
    setMessage("");

    queryResults(analyzer.id, trimmed, testCodesFilter, (response) => {
      if (response.error || response.statusCode >= 400) {
        setStatus("error");
        setMessage(
          response.message ||
            response.error ||
            intl.formatMessage({ id: "analyzer.queryResults.error" }),
        );
      } else {
        setStatus("success");
        setMessage(
          response.message ||
            intl.formatMessage({ id: "analyzer.queryResults.success" }),
        );
        setImportedCount(response.importedResultCount ?? 0);
      }
    });
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      data-testid="query-results-modal"
    >
      <ModalHeader
        title={intl.formatMessage({ id: "analyzer.queryResults.title" })}
        data-testid="query-results-modal-header"
      />
      <ModalBody>
        <p className="query-results-subtitle">
          {intl.formatMessage({ id: "analyzer.queryResults.subtitle" })}
        </p>
        {analyzer && (
          <p className="query-results-analyzer-name">
            <strong>{analyzer.name}</strong>
          </p>
        )}
        <TextInput
          id="query-results-accession"
          labelText={intl.formatMessage({
            id: "analyzer.queryResults.accessionLabel",
          })}
          value={accessionNumber}
          onChange={(e) => setAccessionNumber(e.target.value)}
          placeholder="e.g. 2026-A01"
          data-testid="query-results-accession-input"
          disabled={status === "loading"}
        />
        <TextInput
          id="query-results-test-codes"
          labelText={intl.formatMessage({
            id: "analyzer.queryResults.testCodesLabel",
          })}
          value={testCodesRaw}
          onChange={(e) => setTestCodesRaw(e.target.value)}
          placeholder="e.g. HIV, TB"
          data-testid="query-results-test-codes-input"
          disabled={status === "loading"}
        />
        {status === "success" && (
          <Tag type="green" data-testid="query-results-success">
            {message}
            {importedCount >= 0 &&
              ` — ${intl.formatMessage({ id: "analyzer.queryResults.importedCount" }, { count: importedCount })}`}
          </Tag>
        )}
        {status === "error" && (
          <Tag type="red" data-testid="query-results-error">
            {message}
          </Tag>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="query-results-close-button"
        >
          {intl.formatMessage({ id: "analyzer.queryResults.close" })}
        </Button>
        {status !== "loading" && (
          <Button
            kind="primary"
            onClick={handleSubmit}
            data-testid="query-results-submit-button"
          >
            {intl.formatMessage({ id: "analyzer.queryResults.submit" })}
          </Button>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

export default QueryResultsModal;
