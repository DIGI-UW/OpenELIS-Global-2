import React, { useEffect, useState } from "react";
import { Button, ComboBox, InlineNotification, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";
import AnalyzerConfigTable from "../AnalyzerConfigTable/AnalyzerConfigTable";

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

  const headers = [
    {
      key: "code",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.pendingCodes.code",
      }),
    },
    {
      key: "seenCount",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.pendingCodes.seenCount",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.pendingCodes.status",
      }),
    },
    {
      key: "openelisTest",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.pendingCodes.openelisTest",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.pendingCodes.actions",
      }),
    },
  ];
  const rows = pendingCodes.map((code) => ({
    id: code.id,
    code: code.analyzerTestName,
    seenCount: code.seenCount,
    status: code.status,
    openelisTest: code.openelisTestId || "",
    actions: code.status,
  }));

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
        <AnalyzerConfigTable
          headers={headers}
          rows={rows}
          tableLabel={intl.formatMessage({
            id: "analyzer.fieldMapping.pendingCodes.tableLabel",
          })}
          testId="pending-codes-table"
          renderCell={(cell, row) => {
            const code = pendingCodes.find((item) => item.id === row.id);
            if (!code) return cell.value;
            if (cell.info.header === "status") {
              return renderStatusTag(code.status);
            }
            if (cell.info.header === "openelisTest") {
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
              );
            }
            if (cell.info.header === "actions") {
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
                <>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busyId === code.id || isResolved || !selectedTest}
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
                </>
              );
            }
            return cell.value;
          }}
        />
      )}
    </div>
  );
};

export default PendingCodesPanel;
