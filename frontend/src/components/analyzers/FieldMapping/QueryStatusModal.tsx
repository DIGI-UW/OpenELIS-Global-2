import React, { useEffect, useState, useRef } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  ProgressBar,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";
import type { AnalyzerApiResponse, AnalyzerField } from "../types";

interface QueryStatus {
  state: string;
  progress: number;
  logs: string[];
  fields?: AnalyzerField[];
  error?: string;
}

interface QueryStatusModalProps {
  open: boolean;
  onClose?: () => void;
  analyzerId: string;
  jobId?: string | null;
  onCompleted?: (data: QueryStatus) => void;
}

export default function QueryStatusModal({
  open,
  onClose,
  analyzerId,
  jobId,
  onCompleted,
}: QueryStatusModalProps) {
  const intl = useIntl();
  const [status, setStatus] = useState<QueryStatus>({
    state: "pending",
    progress: 0,
    logs: [],
  });
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const completedRef = useRef(false); // Track if onCompleted has been called

  useEffect(() => {
    if (!open) {
      completedRef.current = false; // Reset when modal closes
      return;
    }

    // Reset completion flag when modal opens
    completedRef.current = false;

    const poll = () => {
      analyzerService.getQueryStatus(analyzerId, jobId || "", (data) => {
        const queryStatus = (data as AnalyzerApiResponse & QueryStatus) || {
          state: "pending",
          progress: 0,
          logs: [],
        };
        setStatus(queryStatus);
        if (
          queryStatus &&
          (queryStatus.state === "completed" ||
            queryStatus.state === "failed" ||
            queryStatus.state === "cancelled" ||
            queryStatus.state === "not_found")
        ) {
          if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
          }
          // Only call onCompleted once
          if (
            queryStatus.state === "completed" &&
            typeof onCompleted === "function" &&
            !completedRef.current
          ) {
            completedRef.current = true;
            onCompleted(queryStatus);
          }
        }
      });
    };
    poll();
    timerRef.current = setInterval(poll, 2000);
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [open, analyzerId, jobId]); // Removed onCompleted from dependencies

  const handleClose = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    // Remove focus from any button before closing to prevent aria-hidden warning
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
    if (onClose) {
      onClose();
    }
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      aria-label="Query Status"
      data-testid="query-status-modal"
      preventCloseOnClickOutside={false}
    >
      <ModalHeader
        title={<FormattedMessage id="analyzer.query.modal.title" />}
        label={<FormattedMessage id="analyzer.query.modal.subtitle" />}
      />
      <ModalBody>
        <div data-testid="query-status-state">
          <strong>
            <FormattedMessage id="analyzer.query.modal.state" />:
          </strong>{" "}
          {status.state}
        </div>
        <div style={{ marginTop: "1rem" }} data-testid="query-status-progress">
          <ProgressBar
            label={intl.formatMessage({ id: "analyzer.query.modal.progress" })}
            value={status.progress || 0}
          />
        </div>
        <div style={{ marginTop: "1rem" }}>
          <strong>
            <FormattedMessage id="analyzer.query.modal.logs" />
          </strong>
          <div
            data-testid="query-status-logs"
            style={{
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "0.5rem",
              marginTop: "0.5rem",
              maxHeight: "200px",
              overflowY: "auto",
              fontFamily: "monospace",
              fontSize: "12px",
            }}
          >
            {Array.isArray(status.logs) && status.logs.length > 0 ? (
              status.logs.map((line, idx) => <div key={idx}>{line}</div>)
            ) : (
              <div>
                <FormattedMessage id="analyzer.query.modal.noLogs" />
              </div>
            )}
          </div>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="query-status-close"
        >
          <FormattedMessage id="button.close" defaultMessage="Close" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}
