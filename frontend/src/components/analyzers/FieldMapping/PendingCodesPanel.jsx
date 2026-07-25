import React, { useEffect, useState } from "react";
import { Button, ComboBox, InlineNotification, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";

const PendingCodesPanel = ({ analyzerId, pendingCodes = [], onUpdated }) => {
  const intl = useIntl();
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState(null);
  const [testOptions, setTestOptions] = useState([]);
  const [selectedTests, setSelectedTests] = useState({});

  useEffect(() => {
    analyzerService.getTestMappingOptions(analyzerId, (response) => {
      setTestOptions(Array.isArray(response) ? response : []);
    });
  }, [analyzerId]);

  const handleStatusUpdate = (pendingCodeId, status) => {
    setBusyId(pendingCodeId);
    setError(null);
    analyzerService.updatePendingCodeStatus(
      analyzerId,
      pendingCodeId,
      status,
      (response) => {
        setBusyId(null);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              response?.message ||
              intl.formatMessage({
                id: "analyzer.fieldMapping.pendingCodes.updateError",
              }),
          );
          return;
        }
        onUpdated && onUpdated();
      },
    );
  };

  const handleResolve = (pendingCodeId, openelisTestId) => {
    if (!openelisTestId) return;
    setBusyId(pendingCodeId);
    setError(null);
    analyzerService.resolvePendingCode(
      analyzerId,
      pendingCodeId,
      openelisTestId,
      (response) => {
        setBusyId(null);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              response?.message ||
              intl.formatMessage({
                id: "analyzer.fieldMapping.pendingCodes.resolveError",
              }),
          );
          return;
        }
        onUpdated && onUpdated();
      },
    );
  };

  const optionLabel = (option) => {
    if (!option) return "";
    return option.loinc
      ? `${option.name} (${option.loinc})`
      : String(option.name || "");
  };

  const renderStatusTag = (status) => {
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
          aria-label={intl.formatMessage({
            id: "analyzer.fieldMapping.pendingCodes.tableLabel",
          })}
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
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.openelisTest" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.pendingCodes.actions" />
              </th>
            </tr>
          </thead>
          <tbody>
            {pendingCodes.map((code) => {
              const selectedTest = Object.prototype.hasOwnProperty.call(
                selectedTests,
                code.id,
              )
                ? selectedTests[code.id]
                : testOptions.find(
                    (option) => option.id === code.openelisTestId,
                  ) || null;
              const isResolved =
                code.status === "MAPPED" && Boolean(code.openelisTestId);
              return (
                <tr key={code.id}>
                  <td>{code.analyzerTestName}</td>
                  <td>{code.seenCount}</td>
                  <td>{renderStatusTag(code.status)}</td>
                  <td>
                    <ComboBox
                      id={`pending-code-test-${code.id}`}
                      items={testOptions}
                      itemToString={optionLabel}
                      selectedItem={selectedTest}
                      onChange={({ selectedItem }) =>
                        setSelectedTests((current) => ({
                          ...current,
                          [code.id]: selectedItem || null,
                        }))
                      }
                      titleText={intl.formatMessage({
                        id: "analyzer.fieldMapping.pendingCodes.openelisTest",
                      })}
                      placeholder={intl.formatMessage({
                        id: "analyzer.fieldMapping.pendingCodes.selectTest",
                      })}
                      disabled={isResolved}
                    />
                  </td>
                  <td>
                    <Button
                      kind="ghost"
                      size="sm"
                      disabled={
                        busyId === code.id || isResolved || !selectedTest
                      }
                      onClick={() => handleResolve(code.id, selectedTest?.id)}
                      data-testid={`pending-code-map-${code.id}`}
                    >
                      <FormattedMessage id="analyzer.fieldMapping.pendingCodes.resolve" />
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
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default PendingCodesPanel;
