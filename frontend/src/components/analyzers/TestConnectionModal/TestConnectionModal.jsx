import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  ProgressBar,
  Accordion,
  AccordionItem,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { testConnection } from "../../../services/analyzerService";
import "./TestConnectionModal.css";

const TestConnectionModal = ({ analyzer, open, onClose }) => {
  const intl = useIntl();
  const [status, setStatus] = useState("initial"); // initial, testing, success, error
  const [logs, setLogs] = useState([]);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (open && analyzer) {
      // Reset state when modal opens
      setStatus("initial");
      setLogs([]);
      setProgress(0);
    }
  }, [open, analyzer]);

  const handleTest = () => {
    if (!analyzer || !analyzer.id) {
      setStatus("error");
      setLogs([{ level: "error", message: "Analyzer ID is required" }]);
      return;
    }

    setStatus("testing");
    setProgress(0);
    setLogs([{ level: "info", message: "Starting connection test..." }]);

    // Simulate progress
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 90) {
          clearInterval(progressInterval);
          return 90;
        }
        return prev + 10;
      });
    }, 200);

    testConnection(analyzer.id, (response) => {
      clearInterval(progressInterval);
      setProgress(100);

      // Check for errors: HTTP errors, network errors, OR success=false from backend
      if (
        response.error ||
        response.statusCode >= 400 ||
        response.success === false
      ) {
        setStatus("error");
        setLogs((prev) => [
          ...prev,
          {
            level: "error",
            message: response.message || response.error || "Connection failed",
          },
        ]);
      } else {
        setStatus("success");
        setLogs((prev) => [
          ...prev,
          {
            level: "success",
            message: response.message || "Connection successful!",
          },
        ]);
      }
    });
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      data-testid="test-connection-modal"
    >
      <ModalHeader
        title={intl.formatMessage({ id: "analyzer.testConnection.title" })}
        data-testid="test-connection-modal-header"
      />
      <ModalBody>
        {analyzer && (
          <div data-testid="test-connection-analyzer-info">
            <p>
              <strong>Name:</strong> {analyzer.name}
            </p>
            <p>
              <strong>IP:</strong> {analyzer.ipAddress}
            </p>
            <p>
              <strong>Port:</strong> {analyzer.port}
            </p>
          </div>
        )}

        {status === "testing" && (
          <div data-testid="test-connection-progress">
            <ProgressBar
              value={progress}
              label={intl.formatMessage({
                id: "analyzer.testConnection.testing",
              })}
            />
          </div>
        )}

        {status === "success" && (
          <Tag type="green" data-testid="test-connection-success">
            {intl.formatMessage({ id: "analyzer.testConnection.success" })}
          </Tag>
        )}

        {status === "error" && (
          <Tag type="red" data-testid="test-connection-error">
            Connection Failed
          </Tag>
        )}

        {logs.length > 0 && (
          <Accordion data-testid="test-connection-logs">
            <AccordionItem
              title={intl.formatMessage({ id: "analyzer.testConnection.logs" })}
            >
              <div>
                {logs.map((log, index) => (
                  <div key={index} data-testid={`test-connection-log-${index}`}>
                    <strong>{log.level}:</strong> {log.message}
                  </div>
                ))}
              </div>
            </AccordionItem>
          </Accordion>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="test-connection-close-button"
        >
          {intl.formatMessage({ id: "analyzer.testConnection.close" })}
        </Button>
        {status !== "testing" && (
          <Button
            kind="primary"
            onClick={handleTest}
            data-testid="test-connection-test-button"
          >
            {intl.formatMessage({ id: "analyzer.testConnection.testAgain" })}
          </Button>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

export default TestConnectionModal;
