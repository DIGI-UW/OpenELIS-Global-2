import React from "react";
import { Modal } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * OGC-1417 — what the person entering a value is owed before it is saved: a
 * critical value is acknowledged, and a value outside the valid range is
 * confirmed (when Result Configuration asks for it). The server refuses a save
 * that still owes either, with the same shape, so every screen that writes a
 * numeric result shows this one modal.
 */
export interface ResultAlert {
  kind: "CRITICAL" | "INVALID";
  value: string;
  testName?: string;
  accessionNumber?: string;
  lowValid?: number | string | null;
  highValid?: number | string | null;
  analysisId?: string;
  componentId?: string | null;
  rowId?: string | null;
}

export interface AcknowledgementRefusal {
  alerts: ResultAlert[];
  customCriticalMessage: string;
}

/**
 * The alerts a refused save still owes, or null when the response is not that
 * refusal.
 */
export function acknowledgementRefusal(
  response: unknown,
): AcknowledgementRefusal | null {
  if (!response || typeof response !== "object") {
    return null;
  }
  const body = response as {
    status?: number;
    code?: string;
    acknowledgementRequired?: ResultAlert[];
    customCriticalMessage?: string | null;
  };
  if (
    body.status !== 422 ||
    body.code !== "ACKNOWLEDGEMENT_REQUIRED" ||
    !Array.isArray(body.acknowledgementRequired)
  ) {
    return null;
  }
  return {
    alerts: body.acknowledgementRequired,
    customCriticalMessage: body.customCriticalMessage || "",
  };
}

const boundText = (bound: number | string | null | undefined): string =>
  bound === null || bound === undefined || bound === "" ? "—" : String(bound);

export interface ResultAlertModalProps {
  open: boolean;
  alerts: ResultAlert[];
  /** Result Configuration's message; blank falls back to the translated default */
  customCriticalMessage?: string;
  /** "entry" when shown on leaving the field, "save" when shown at Save */
  mode: "entry" | "save";
  onConfirm: () => void;
  onCorrect: () => void;
}

const ResultAlertModal = ({
  open,
  alerts,
  customCriticalMessage,
  mode,
  onConfirm,
  onCorrect,
}: ResultAlertModalProps) => {
  const intl = useIntl();
  if (!open || alerts.length === 0) {
    return null;
  }
  const critical = alerts.some((alert) => alert.kind === "CRITICAL");
  const message = (customCriticalMessage || "").trim();
  const primaryId =
    mode === "entry"
      ? "label.results.alert.keepValue"
      : critical
        ? "label.results.alert.acknowledgeAndSave"
        : "label.results.alert.confirmAndSave";
  return (
    <Modal
      open
      danger={critical}
      size="sm"
      data-testid="result-alert-modal"
      modalHeading={intl.formatMessage({
        id: critical
          ? "label.results.alert.critical.title"
          : "label.results.alert.invalid.title",
      })}
      primaryButtonText={intl.formatMessage({ id: primaryId })}
      secondaryButtonText={intl.formatMessage({
        id: "label.results.alert.correct",
      })}
      onRequestSubmit={onConfirm}
      onSecondarySubmit={onCorrect}
      onRequestClose={onCorrect}
    >
      {critical && (
        <p
          data-testid="result-alert-critical-message"
          style={{ whiteSpace: "pre-wrap", marginBottom: "1rem" }}
        >
          {message ||
            intl.formatMessage({
              id: "label.results.alert.critical.defaultMessage",
            })}
        </p>
      )}
      <ul>
        {alerts.map((alert, index) => (
          <li
            key={`${alert.analysisId || alert.rowId || ""}-${alert.componentId || ""}-${index}`}
            data-testid="result-alert-item"
            style={{ marginBottom: "0.5rem" }}
          >
            <strong>
              {[alert.accessionNumber, alert.testName]
                .filter(Boolean)
                .join(" · ")}
              {": "}
              {alert.value}
            </strong>
            {alert.kind === "INVALID" && (
              <div>
                <FormattedMessage
                  id="label.results.alert.invalid.body"
                  values={{
                    low: boundText(alert.lowValid),
                    high: boundText(alert.highValid),
                  }}
                />
              </div>
            )}
          </li>
        ))}
      </ul>
    </Modal>
  );
};

export default ResultAlertModal;
