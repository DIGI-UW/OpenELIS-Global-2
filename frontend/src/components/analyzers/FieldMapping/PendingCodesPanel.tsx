import React, { useState } from "react";
import { Button, InlineNotification, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";
import type { AnalyzerApiResponse, PendingCode } from "../types";

interface PendingCodesPanelProps {
  analyzerId: string;
  pendingCodes?: PendingCode[];
  onUpdated?: () => void;
}

const PendingCodesPanel = ({
  analyzerId,
  pendingCodes = [],
  onUpdated,
}: PendingCodesPanelProps) => {
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleStatusUpdate = (pendingCodeId: string, status: string) => {
    setBusyId(pendingCodeId);
    setError(null);
    analyzerService.updatePendingCodeStatus(
      analyzerId,
      pendingCodeId,
      status,
      (response: AnalyzerApiResponse) => {
        setBusyId(null);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              response?.message ||
              "Failed to update pending code status.",
          );
          return;
        }
        if (onUpdated) {
          onUpdated();
        }
      },
    );
  };

  const renderStatusTag = (status?: string) => {
    if (status === "PENDING") {
      return (
        <Tag type="warm-gray" size="sm">
          {status}
        </Tag>
      );
    }
    if (status === "MAPPED") {
      return (
        <Tag type="green" size="sm">
          {status}
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        {status}
      </Tag>
    );
  };

  return (
    <div data-testid="pending-codes-panel" className="pending-codes-panel">
      <h4>
        <FormattedMessage id="analyzer.fieldMapping.pendingCodes.title" />
      </h4>
      <p>
        <FormattedMessage id="analyzer.fieldMapping.pendingCodes.subtitle" />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton
        />
      )}

      {pendingCodes.length === 0 ? (
        <p data-testid="pending-codes-empty">
          <FormattedMessage id="analyzer.fieldMapping.pendingCodes.empty" />
        </p>
      ) : (
        <table
          className="pending-codes-table"
          data-testid="pending-codes-table"
          aria-label="Pending analyzer codes"
        >
          <thead>
            <tr>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.code" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.seenCount" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.status" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.actions" />
              </th>
            </tr>
          </thead>
          <tbody>
            {pendingCodes.map((code) => (
              <tr key={code.id}>
                <td>{code.analyzerTestName}</td>
                <td>{code.seenCount}</td>
                <td>{renderStatusTag(code.status)}</td>
                <td>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busyId === code.id || code.status === "MAPPED"}
                    onClick={() => handleStatusUpdate(code.id, "MAPPED")}
                    data-testid={`pending-code-map-${code.id}`}
                  >
                    <FormattedMessage id="analyzer.fieldMapping.pendingCodes.markMapped" />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busyId === code.id || code.status === "IGNORED"}
                    onClick={() => handleStatusUpdate(code.id, "IGNORED")}
                    data-testid={`pending-code-ignore-${code.id}`}
                  >
                    <FormattedMessage id="analyzer.fieldMapping.pendingCodes.ignore" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default PendingCodesPanel;
