import React, { useEffect, useState } from "react";
import {
  Button,
  ComboBox,
  InlineLoading,
  InlineNotification,
  Link,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";

const ResultOptionSelector = ({
  analyzerId,
  resultValue,
  selectorId,
  selectedOption,
  onChange,
}) => {
  const intl = useIntl();
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    setLoading(true);
    setLoadError(false);
    analyzerService.getResultValueOptions(
      analyzerId,
      resultValue.testCode,
      (response) => {
        if (response?.error || response?.statusCode >= 400) {
          setOptions([]);
          setLoadError(true);
        } else {
          setOptions(Array.isArray(response) ? response : []);
        }
        setLoading(false);
      },
    );
  }, [analyzerId, resultValue.testCode]);

  if (loading) {
    return (
      <InlineLoading
        description={intl.formatMessage({
          id: "analyzer.fieldMapping.resultValues.options.loading",
        })}
      />
    );
  }

  if (loadError) {
    return (
      <span>
        <FormattedMessage id="analyzer.fieldMapping.resultValues.options.error" />
      </span>
    );
  }

  if (options.length === 0) {
    return (
      <div>
        <span>
          <FormattedMessage id="analyzer.fieldMapping.resultValues.options.empty" />
        </span>{" "}
        <Link href="/MasterListsPage/TestCatalogList">
          <FormattedMessage id="analyzer.fieldMapping.resultValues.options.catalogLink" />
        </Link>
      </div>
    );
  }

  return (
    <ComboBox
      id={`result-value-openelis-${selectorId}`}
      aria-label={intl.formatMessage({
        id: "analyzer.fieldMapping.resultValues.option",
      })}
      items={options}
      selectedItem={selectedOption || null}
      onChange={({ selectedItem }) => onChange(selectedItem || null)}
      itemToString={(item) => (item ? item.label || item.value : "")}
      placeholder={intl.formatMessage({
        id: "analyzer.fieldMapping.resultValues.option.placeholder",
      })}
    />
  );
};

const ResultValueMappingsPanel = ({
  analyzerId,
  mappings = [],
  pendingValues = [],
  onUpdated,
}) => {
  const intl = useIntl();
  const [resolveValues, setResolveValues] = useState({});
  const [mappingValues, setMappingValues] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState(null);

  const mappingKey = (mapping) =>
    `${mapping.testCode || ""}:${mapping.analyzerValue || ""}`;
  const activeMappings = mappings.filter((mapping) => mapping.active !== false);
  const unresolvedPendingValues = pendingValues.filter(
    (pendingValue) => !pendingValue.status || pendingValue.status === "PENDING",
  );
  const hasUnboundMappings = activeMappings.some(
    (mapping) => mapping.bindingStatus !== "BOUND",
  );
  const canSaveMappings =
    hasUnboundMappings &&
    activeMappings.every(
      (mapping) =>
        mapping.openelisResultOptionId ||
        mappingValues[mappingKey(mapping)]?.id,
    );

  const handleSaveMappings = () => {
    const payload = mappings.map((mapping) => {
      const selectedOption = mappingValues[mappingKey(mapping)];
      const updated = {
        analyzerValue: mapping.analyzerValue,
        testCode: mapping.testCode,
        active: mapping.active !== false,
      };
      const optionId =
        selectedOption?.id || mapping.openelisResultOptionId || null;
      if (optionId) {
        updated.openelisResultOptionId = optionId;
      }
      return updated;
    });

    setBusyId("mappings");
    setError(null);
    analyzerService.updateResultValueMappings(
      analyzerId,
      payload,
      (response) => {
        setBusyId(null);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              response?.message ||
              intl.formatMessage({
                id: "analyzer.fieldMapping.resultValues.error.save",
              }),
          );
          return;
        }
        setMappingValues({});
        onUpdated && onUpdated();
      },
    );
  };

  const handleResolve = (pendingValue) => {
    const selectedOption = resolveValues[pendingValue.id];
    if (!selectedOption?.id) {
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
      { openelisResultOptionId: selectedOption.id },
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
          [pendingValue.id]: null,
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
                <td>
                  {mapping.active !== false &&
                  mapping.bindingStatus !== "BOUND" ? (
                    <ResultOptionSelector
                      analyzerId={analyzerId}
                      resultValue={mapping}
                      selectorId={`mapping-${index}`}
                      selectedOption={mappingValues[mappingKey(mapping)]}
                      onChange={(option) =>
                        setMappingValues((current) => ({
                          ...current,
                          [mappingKey(mapping)]: option,
                        }))
                      }
                    />
                  ) : (
                    mapping.openelisLabel || mapping.openelisValue
                  )}
                </td>
                <td>{mapping.testCode || "-"}</td>
                <td>
                  {renderStatusTag(
                    mapping.bindingStatus ||
                      (mapping.active === false ? "INACTIVE" : "ACTIVE"),
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {hasUnboundMappings && (
        <Button
          kind="primary"
          size="sm"
          disabled={!canSaveMappings || busyId === "mappings"}
          onClick={handleSaveMappings}
          data-testid="result-value-mappings-save"
        >
          <FormattedMessage id="analyzer.fieldMapping.resultValues.save" />
        </Button>
      )}

      <h5 className="result-value-pending-heading">
        <FormattedMessage id="analyzer.fieldMapping.resultValues.pendingTitle" />
      </h5>
      {unresolvedPendingValues.length === 0 ? (
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
            {unresolvedPendingValues.map((pendingValue) => (
              <tr key={pendingValue.id}>
                <td>{pendingValue.analyzerValue}</td>
                <td>{pendingValue.testCode || "-"}</td>
                <td>{renderStatusTag(pendingValue.status)}</td>
                <td>
                  <ResultOptionSelector
                    analyzerId={analyzerId}
                    resultValue={pendingValue}
                    selectorId={`pending-${pendingValue.id}`}
                    selectedOption={resolveValues[pendingValue.id]}
                    onChange={(option) =>
                      setResolveValues((current) => ({
                        ...current,
                        [pendingValue.id]: option,
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
