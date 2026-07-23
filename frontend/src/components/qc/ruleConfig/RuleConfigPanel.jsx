/**
 * RuleConfigPanel Component
 *
 * Page-level container for Westgard rule configuration management.
 * Task Reference: T074
 * Specification: FR-015 to FR-021, User Story 7
 *
 * Features:
 * - Table listing all configured rule sets by (analyzer, test) pair
 * - Widget listing unconfigured control-lot mappings
 * - Shared modal form for creating and editing rule configs
 */

import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Loading,
  InlineNotification,
  OverflowMenu,
  OverflowMenuItem,
  Tag,
  Pagination,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageTitle from "../../common/PageTitle/PageTitle";
import RuleConfigFormModal from "./RuleConfigFormModal";
import "./RuleConfigPanel.css";

const configuredHeaders = [
  { key: "instrumentName", header: "qc.ruleConfig.field.analyzer" },
  { key: "testName", header: "qc.ruleConfig.field.test" },
  { key: "enabledRules", header: "qc.ruleConfig.field.enabledRules" },
  { key: "actions", header: "" },
];

const unconfiguredHeaders = [
  { key: "instrumentName", header: "qc.ruleConfig.field.analyzer" },
  { key: "testName", header: "qc.ruleConfig.field.test" },
  { key: "activeLots", header: "qc.ruleConfig.unconfigured.activeLots" },
  { key: "actions", header: "" },
];

const RuleConfigPanel = () => {
  const intl = useIntl();

  const [summaries, setSummaries] = useState([]);
  const [unconfigured, setUnconfigured] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);
  const [configPage, setConfigPage] = useState(1);
  const [configPageSize, setConfigPageSize] = useState(10);
  const [uncfgPage, setUncfgPage] = useState(1);
  const [uncfgPageSize, setUncfgPageSize] = useState(10);

  const loadData = useCallback(() => {
    setLoading(true);
    setError(null);

    let completed = 0;
    const checkDone = () => {
      completed++;
      if (completed === 2) {
        setLoading(false);
      }
    };

    getFromOpenElisServer("/rest/qc/ruleConfig/summaries", (response) => {
      if (Array.isArray(response)) {
        setSummaries(response);
      } else {
        setSummaries([]);
      }
      checkDone();
    });

    getFromOpenElisServer("/rest/qc/ruleConfig/unconfigured", (response) => {
      if (Array.isArray(response)) {
        setUnconfigured(response);
      } else {
        setUnconfigured([]);
      }
      checkDone();
    });
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleEdit = (summary) => {
    setEditingConfig({
      testId: summary.testId,
      instrumentId: summary.instrumentId,
      analyzerName: summary.instrumentName,
      testName: summary.testName,
      isNew: false,
    });
    setModalOpen(true);
  };

  const handleConfigure = (mapping) => {
    setEditingConfig({
      testId: mapping.testId,
      instrumentId: mapping.instrumentId,
      analyzerName: mapping.instrumentName,
      testName: mapping.testName,
      isNew: true,
    });
    setModalOpen(true);
  };

  const handleModalClose = () => {
    setModalOpen(false);
    setEditingConfig(null);
  };

  const handleModalSave = () => {
    setModalOpen(false);
    setEditingConfig(null);
    loadData();
  };

  // Paginated configured rows
  const paginatedSummaries = useMemo(() => {
    const start = (configPage - 1) * configPageSize;
    return summaries.slice(start, start + configPageSize);
  }, [summaries, configPage, configPageSize]);

  const configuredRows = paginatedSummaries.map((s) => ({
    id: `${s.testId}-${s.instrumentId}`,
    instrumentName: s.instrumentName || "-",
    testName: s.testName || "-",
    enabledRules: `${s.enabledRuleCount}/${s.totalRuleCount}`,
    actions: s,
  }));

  const translatedConfiguredHeaders = configuredHeaders.map((h) => ({
    key: h.key,
    header: h.header
      ? intl.formatMessage({ id: h.header, defaultMessage: h.header })
      : "",
  }));

  // Paginated unconfigured rows
  const paginatedUnconfigured = useMemo(() => {
    const start = (uncfgPage - 1) * uncfgPageSize;
    return unconfigured.slice(start, start + uncfgPageSize);
  }, [unconfigured, uncfgPage, uncfgPageSize]);

  const unconfiguredRows = paginatedUnconfigured.map((m) => ({
    id: `uncfg-${m.testId}-${m.instrumentId}`,
    instrumentName: m.instrumentName || "-",
    testName: m.testName || "-",
    activeLots: String(m.activeControlLotCount),
    actions: m,
  }));

  const translatedUnconfiguredHeaders = unconfiguredHeaders.map((h) => ({
    key: h.key,
    header: h.header
      ? intl.formatMessage({ id: h.header, defaultMessage: h.header })
      : "",
  }));

  const handleConfigPaginationChange = ({
    page: newPage,
    pageSize: newPageSize,
  }) => {
    setConfigPage(newPage);
    setConfigPageSize(newPageSize);
  };

  const handleUncfgPaginationChange = ({
    page: newPage,
    pageSize: newPageSize,
  }) => {
    setUncfgPage(newPage);
    setUncfgPageSize(newPageSize);
  };

  if (loading) {
    return (
      <div className="rule-config-panel" data-testid="rule-config-panel">
        <Loading
          description={intl.formatMessage({ id: "qc.ruleConfig.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="rule-config-panel" data-testid="rule-config-panel">
      <div
        className="rule-config-panel-header"
        data-testid="rule-config-panel-header"
      >
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/analyzers/qc/db",
            },
            {
              label: intl.formatMessage({ id: "qc.ruleConfig.title" }),
            },
          ]}
          subtitle={intl.formatMessage({ id: "qc.ruleConfig.subtitle" })}
        />
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.ruleConfig.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="rule-config-error"
        />
      )}

      {/* Configured Rule Sets Table */}
      <h4 className="rule-config-section-title">
        {intl.formatMessage({ id: "qc.ruleConfig.table.title" })}
      </h4>
      <DataTable
        rows={configuredRows}
        headers={translatedConfiguredHeaders}
        isSortable
      >
        {({
          rows: tableRows,
          headers: tableHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {tableHeaders.map((header) => (
                    <TableHeader
                      {...getHeaderProps({ header })}
                      key={header.key}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {tableRows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={configuredHeaders.length}>
                      {intl.formatMessage({ id: "qc.ruleConfig.table.empty" })}
                    </TableCell>
                  </TableRow>
                ) : (
                  tableRows.map((row) => (
                    <TableRow {...getRowProps({ row })} key={row.id}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "enabledRules") {
                          const parts = cell.value.split("/");
                          const enabled = parseInt(parts[0], 10);
                          const total = parseInt(parts[1], 10);
                          return (
                            <TableCell key={cell.id}>
                              <Tag
                                type={
                                  enabled === total
                                    ? "green"
                                    : enabled > 0
                                      ? "blue"
                                      : "gray"
                                }
                              >
                                {cell.value}
                              </Tag>
                            </TableCell>
                          );
                        }
                        if (cell.info.header === "actions") {
                          return (
                            <TableCell key={cell.id}>
                              <OverflowMenu flipped size="sm">
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "button.edit",
                                  })}
                                  onClick={() => handleEdit(cell.value)}
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }
                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        );
                      })}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      {summaries.length > 0 && (
        <Pagination
          totalItems={summaries.length}
          page={configPage}
          pageSize={configPageSize}
          pageSizes={[10, 25, 50]}
          onChange={handleConfigPaginationChange}
        />
      )}

      {/* Unconfigured Control Mappings */}
      {unconfigured.length > 0 && (
        <div className="rule-config-unconfigured-section">
          <h4 className="rule-config-section-title">
            {intl.formatMessage({ id: "qc.ruleConfig.unconfigured.title" })}
          </h4>
          <p className="rule-config-unconfigured-subtitle">
            {intl.formatMessage({ id: "qc.ruleConfig.unconfigured.subtitle" })}
          </p>
          <DataTable
            rows={unconfiguredRows}
            headers={translatedUnconfiguredHeaders}
            isSortable
          >
            {({
              rows: tableRows,
              headers: tableHeaders,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {tableHeaders.map((header) => (
                        <TableHeader
                          {...getHeaderProps({ header })}
                          key={header.key}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
                      <TableRow {...getRowProps({ row })} key={row.id}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "actions") {
                            return (
                              <TableCell key={cell.id}>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={Add}
                                  onClick={() => handleConfigure(cell.value)}
                                >
                                  {intl.formatMessage({
                                    id: "qc.ruleConfig.unconfigured.configure",
                                  })}
                                </Button>
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          {unconfigured.length > 0 && (
            <Pagination
              totalItems={unconfigured.length}
              page={uncfgPage}
              pageSize={uncfgPageSize}
              pageSizes={[10, 25, 50]}
              onChange={handleUncfgPaginationChange}
            />
          )}
        </div>
      )}

      {/* Shared Form Modal */}
      {editingConfig && (
        <RuleConfigFormModal
          open={modalOpen}
          testId={editingConfig.testId}
          instrumentId={editingConfig.instrumentId}
          analyzerName={editingConfig.analyzerName}
          testName={editingConfig.testName}
          isNew={editingConfig.isNew}
          onClose={handleModalClose}
          onSave={handleModalSave}
        />
      )}
    </div>
  );
};

export default RuleConfigPanel;
