/**
 * ValidationDashboard Component
 *
 * Displays validation metrics and test results for analyzer field mappings
 *
 * Features:
 * - Metrics display: Mapping accuracy, unmapped field count, type compatibility warnings
 * - Coverage by test unit (bar chart or table)
 * - Test results table with historical validation results
 * - Action buttons: "Validate All Mappings", "View Test History"
 * - Conditional visibility: Only displayed when analyzer lifecycle_stage is VALIDATION
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  ProgressBar,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../components/utils/Utils";
import "./ValidationDashboard.css";

const ValidationDashboard = ({ analyzerId, status }) => {
  const intl = useIntl();
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [validating, setValidating] = useState(false);

  // Load validation metrics when component mounts (only if in VALIDATION status)
  useEffect(() => {
    if (status === "VALIDATION" && analyzerId) {
      loadValidationMetrics();
    }
  }, [analyzerId, status]);

  // Only display when lifecycle stage is VALIDATION (early return AFTER hooks)
  if (status !== "VALIDATION") {
    return null;
  }

  const loadValidationMetrics = () => {
    setLoading(true);
    setError(null);

    const endpoint = `/rest/analyzer/analyzers/${analyzerId}/validation-metrics`;

    getFromOpenElisServer(endpoint, (data) => {
      if (data && !data.error) {
        setMetrics(data);
      } else {
        setError(
          data?.error || intl.formatMessage({ id: "error.loading.metrics" }),
        );
      }
      setLoading(false);
    });
  };

  const handleValidateAllMappings = () => {
    setValidating(true);
    setError(null);

    // Trigger validation for all mappings
    // This would call a validation endpoint that tests all configured mappings
    const endpoint = `/rest/analyzer/analyzers/${analyzerId}/validate-all-mappings`;

    postToOpenElisServerJsonResponse(endpoint, JSON.stringify({}), (data) => {
      if (data && !data.error) {
        // Reload metrics after validation
        loadValidationMetrics();
        // Show success notification
        setError(null);
      } else {
        setError(
          data?.error || intl.formatMessage({ id: "error.validation.failed" }),
        );
      }
      setValidating(false);
    });
  };

  const handleViewTestHistory = () => {
    // TODO: Implement test history modal — not yet available
  };

  if (loading) {
    return (
      <Grid fullWidth className="validation-dashboard">
        <Column lg={16} md={8} sm={4}>
          <Loading
            description={intl.formatMessage({ id: "loading.metrics" })}
          />
        </Column>
      </Grid>
    );
  }

  if (error) {
    return (
      <Grid fullWidth className="validation-dashboard">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
            onClose={() => setError(null)}
          />
        </Column>
      </Grid>
    );
  }

  if (!metrics) {
    return null;
  }

  // Calculate accuracy percentage
  const accuracyPercent = Math.round(metrics.accuracy * 100);

  // Coverage table headers
  const coverageHeaders = [
    {
      key: "testUnit",
      header: intl.formatMessage({ id: "validation.coverage.test.unit" }),
    },
    {
      key: "coverage",
      header: intl.formatMessage({ id: "validation.coverage.percentage" }),
    },
  ];

  // Coverage table rows
  const coverageRows = Object.entries(metrics.coverageByTestUnit || {}).map(
    ([testUnit, coverage]) => ({
      id: testUnit,
      testUnit,
      coverage: Math.round(coverage * 100),
    }),
  );

  return (
    <Grid
      fullWidth
      className="validation-dashboard"
      data-testid="validation-dashboard"
    >
      <Column lg={16} md={8} sm={4}>
        <Tile className="validation-dashboard-tile">
          <h3>
            <FormattedMessage id="validation.dashboard.title" />
          </h3>

          {/* Metrics Section */}
          <Grid className="metrics-section">
            <Column lg={4} md={4} sm={4}>
              <Tile
                className="metric-tile"
                data-testid="validation-metric-accuracy"
              >
                <div className="metric-label">
                  <FormattedMessage id="validation.metric.accuracy" />
                </div>
                <div className="metric-value">{accuracyPercent}%</div>
                <ProgressBar
                  value={accuracyPercent}
                  label={intl.formatMessage({
                    id: "validation.accuracy.label",
                  })}
                />
              </Tile>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Tile
                className="metric-tile"
                data-testid="validation-metric-unmapped-count"
              >
                <div className="metric-label">
                  <FormattedMessage id="validation.metric.unmapped.count" />
                </div>
                <div className="metric-value">{metrics.unmappedCount || 0}</div>
                <div className="metric-description">
                  <FormattedMessage id="validation.unmapped.fields.description" />
                </div>
              </Tile>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Tile
                className="metric-tile"
                data-testid="validation-metric-warnings"
              >
                <div className="metric-label">
                  <FormattedMessage id="validation.metric.warnings" />
                </div>
                <div className="metric-value">
                  {metrics.warnings?.length || 0}
                </div>
                <div className="metric-description">
                  <FormattedMessage id="validation.warnings.description" />
                </div>
              </Tile>
            </Column>
          </Grid>

          {/* Unmapped Fields Section */}
          {metrics.unmappedFields && metrics.unmappedFields.length > 0 && (
            <div className="unmapped-fields-section">
              <h4>
                <FormattedMessage id="validation.unmapped.fields.title" />
              </h4>
              <div className="unmapped-fields-list">
                {metrics.unmappedFields.map((field) => (
                  <Tag key={field} type="red" size="sm">
                    {field}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          {/* Warnings Section */}
          {metrics.warnings && metrics.warnings.length > 0 && (
            <div className="warnings-section">
              <h4>
                <FormattedMessage id="validation.warnings.title" />
              </h4>
              <ul className="warnings-list">
                {metrics.warnings.map((warning) => (
                  <li key={warning} className="warning-item">
                    {warning}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Coverage by Test Unit Section */}
          {coverageRows.length > 0 && (
            <div className="coverage-section">
              <h4>
                <FormattedMessage id="validation.coverage.title" />
              </h4>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      {coverageHeaders.map((header) => (
                        <TableHeader key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {coverageRows.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>{row.testUnit}</TableCell>
                        <TableCell>
                          <ProgressBar
                            value={row.coverage}
                            label={`${row.coverage}%`}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </div>
          )}

          {/* Action Buttons */}
          <div className="action-buttons">
            <Button
              kind="primary"
              onClick={handleValidateAllMappings}
              disabled={validating}
              data-testid="validate-all-mappings-button"
            >
              {validating ? (
                <FormattedMessage id="validation.validating" />
              ) : (
                <FormattedMessage id="validation.validate.all.mappings" />
              )}
            </Button>
            <Button
              kind="secondary"
              onClick={handleViewTestHistory}
              disabled
              data-testid="view-test-history-button"
            >
              <FormattedMessage id="validation.view.test.history" />
            </Button>
          </div>
        </Tile>
      </Column>
    </Grid>
  );
};

export default ValidationDashboard;
