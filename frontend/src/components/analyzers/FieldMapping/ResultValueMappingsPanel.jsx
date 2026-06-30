import React, { useState } from "react";
import { Button, InlineNotification, Tag, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";

const ResultValueMappingsPanel = ({
  analyzerId,
  mappings = [],
  pendingValues = [],
  onUpdated,
}) => {
  const intl = useIntl();
  const [resolveValues, setResolveValues] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState(null);

  const handleResolve = (pendingValue) => {
    const openelisValue = (resolveValues[pendingValue.id] || "").trim();
    if (!openelisValue) {
      setError(
        intl.formatMessage({
          id: "analyzer.fieldMapping.resultValues.error.required",
        }),
      );
      return;
    }

    setBusyId(pendingValue.id);
    setError(null);
    analyzerService.resolvePendingResultValue(
      analyzerId,
      pendingValue.id,
      { openelisValue },
      (response) => {
        setBusyId(null);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              response?.message ||
              intl.formatMessage({
                id: "analyzer.fieldMapping.resultValues.error.resolve",
              }),
          );
          return;
        }
        setResolveValues((current) => ({
          ...current,
          [pendingValue.id]: "",
        }));
        onUpdated && onUpdated();
      },
    );
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
        {status || "ACTIVE"}
      </Tag>
    );
  };

  return (
    <div
      data-testid="result-value-mappings-panel"
      className="result-value-mappings-panel"
    >
      <h4>
        <FormattedMessage id="analyzer.fieldMapping.resultValues.title" />
      </h4>
      <p>
        <FormattedMessage id="analyzer.fieldMapping.resultValues.subtitle" />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton
        />
      )}

      <h5>
        <FormattedMessage id="analyzer.fieldMapping.resultValues.mappingsTitle" />
      </h5>
      {mappings.length === 0 ? (
        <p data-testid="result-value-mappings-empty">
          <FormattedMessage id="analyzer.fieldMapping.resultValues.empty" />
        </p>
      ) : (
        <table
          className="result-value-mappings-table"
          data-testid="result-value-mappings-table"
          aria-label={intl.formatMessage({
            id: "analyzer.fieldMapping.resultValues.tableLabel",
          })}
        >
          <thead>
            <tr>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.analyzerValue" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.openelisValue" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.testCode" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.status" />
              </th>
            </tr>
          </thead>
          <tbody>
            {mappings.map((mapping, index) => (
              <tr
                key={`${mapping.testCode || "all"}-${
                  mapping.analyzerValue || index
                }`}
              >
                <td>{mapping.analyzerValue}</td>
                <td>{mapping.openelisValue}</td>
                <td>{mapping.testCode || "-"}</td>
                <td>
                  {renderStatusTag(
                    mapping.active === false ? "INACTIVE" : "ACTIVE",
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h5 className="result-value-pending-heading">
        <FormattedMessage id="analyzer.fieldMapping.resultValues.pendingTitle" />
      </h5>
      {pendingValues.length === 0 ? (
        <p data-testid="pending-result-values-empty">
          <FormattedMessage id="analyzer.fieldMapping.resultValues.pendingEmpty" />
        </p>
      ) : (
        <table
          className="result-value-mappings-table"
          data-testid="pending-result-values-table"
          aria-label={intl.formatMessage({
            id: "analyzer.fieldMapping.resultValues.pendingTableLabel",
          })}
        >
          <thead>
            <tr>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.analyzerValue" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.testCode" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.status" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.openelisValue" />
              </th>
              <th>
                <FormattedMessage id="analyzer.fieldMapping.resultValues.actions" />
              </th>
            </tr>
          </thead>
          <tbody>
            {pendingValues.map((pendingValue) => (
              <tr key={pendingValue.id}>
                <td>{pendingValue.analyzerValue}</td>
                <td>{pendingValue.testCode || "-"}</td>
                <td>{renderStatusTag(pendingValue.status)}</td>
                <td>
                  <TextInput
                    id={`result-value-openelis-${pendingValue.id}`}
                    data-testid={`result-value-openelis-${pendingValue.id}`}
                    labelText={intl.formatMessage({
                      id: "analyzer.fieldMapping.resultValues.openelisValue",
                    })}
                    hideLabel
                    value={resolveValues[pendingValue.id] || ""}
                    onChange={(event) =>
                      setResolveValues((current) => ({
                        ...current,
                        [pendingValue.id]: event.target.value,
                      }))
                    }
                  />
                </td>
                <td>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busyId === pendingValue.id}
                    onClick={() => handleResolve(pendingValue)}
                    data-testid={`result-value-resolve-${pendingValue.id}`}
                  >
                    <FormattedMessage id="analyzer.fieldMapping.resultValues.resolve" />
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

export default ResultValueMappingsPanel;
