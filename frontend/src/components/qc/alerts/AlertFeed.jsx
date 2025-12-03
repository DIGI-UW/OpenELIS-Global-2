/**
 * AlertFeed Component
 *
 * Displays real-time QC alerts in a notification feed
 * Task Reference: T104
 * Specification: FR-065 to FR-073, User Story 5
 *
 * Features:
 * - Real-time alert notifications
 * - Severity-coded display (red for rejection, yellow for warning)
 * - Click to navigate to violation details
 * - Mark as read functionality
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  ClickableTile,
  Button,
  Tag,
  InlineNotification,
} from "@carbon/react";
import {
  Notification,
  NotificationNew,
  CheckmarkOutline,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer, postToOpenElisServerFullResponse } from "../../utils/Utils";
import PropTypes from "prop-types";
import "./AlertFeed.css";

const AlertFeed = ({ maxItems, autoRefresh, refreshInterval }) => {
  const intl = useIntl();
  const history = useHistory();

  // State
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load alerts
  const loadAlerts = useCallback(() => {
    getFromOpenElisServer(`/rest/qc/alerts?limit=${maxItems}&unreadOnly=false`, (response) => {
      if (response && response.data) {
        setAlerts(response.data.alerts || response.data || []);
      } else if (Array.isArray(response)) {
        setAlerts(response);
      } else {
        setError(intl.formatMessage({ id: "qc.alerts.error.loadFailed" }));
      }
      setLoading(false);
    });
  }, [maxItems, intl]);

  // Initial load and auto-refresh
  useEffect(() => {
    loadAlerts();

    if (autoRefresh) {
      const intervalId = setInterval(loadAlerts, refreshInterval);
      return () => clearInterval(intervalId);
    }
  }, [loadAlerts, autoRefresh, refreshInterval]);

  // Handle mark as read (FR-069)
  const handleMarkAsRead = (alertId, e) => {
    e.stopPropagation();

    postToOpenElisServerFullResponse(
      `/rest/qc/alerts/${alertId}/read`,
      JSON.stringify({}),
      (response) => {
        if (response.ok) {
          // Update local state
          setAlerts((prev) =>
            prev.map((alert) =>
              alert.id === alertId ? { ...alert, isRead: true } : alert
            )
          );
        }
      }
    );
  };

  // Handle mark all as read
  const handleMarkAllAsRead = () => {
    const unreadAlertIds = alerts.filter((a) => !a.isRead).map((a) => a.id);

    if (unreadAlertIds.length === 0) return;

    postToOpenElisServerFullResponse(
      "/rest/qc/alerts/read-all",
      JSON.stringify({ alertIds: unreadAlertIds }),
      (response) => {
        if (response.ok) {
          setAlerts((prev) =>
            prev.map((alert) => ({ ...alert, isRead: true }))
          );
        }
      }
    );
  };

  // Handle alert click - navigate to violation
  const handleAlertClick = (alert) => {
    if (alert.violationId) {
      history.push(`/analyzers/qc/violations?violationId=${alert.violationId}`);
    }
  };

  // Get severity color
  const getSeverityColor = (severity) => {
    return severity === "REJECTION" ? "red" : "yellow";
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return "";
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) {
      return intl.formatMessage({ id: "qc.alerts.time.justNow" });
    } else if (diffMins < 60) {
      return intl.formatMessage({ id: "qc.alerts.time.minutesAgo" }, { count: diffMins });
    } else if (diffHours < 24) {
      return intl.formatMessage({ id: "qc.alerts.time.hoursAgo" }, { count: diffHours });
    } else if (diffDays < 7) {
      return intl.formatMessage({ id: "qc.alerts.time.daysAgo" }, { count: diffDays });
    } else {
      return intl.formatDate(date, {
        month: "short",
        day: "numeric",
      });
    }
  };

  // Count unread alerts
  const unreadCount = alerts.filter((a) => !a.isRead).length;

  if (loading) {
    return (
      <div className="alert-feed-loading" data-testid="alert-feed-loading">
        <div className="alert-feed-skeleton">
          {[1, 2, 3].map((i) => (
            <div key={i} className="alert-feed-skeleton-item" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="alert-feed" data-testid="alert-feed">
      {/* Header */}
      <div className="alert-feed-header" data-testid="alert-feed-header">
        <div className="alert-feed-header-title">
          <Notification size={20} />
          <span>{intl.formatMessage({ id: "qc.alerts.title" })}</span>
          {unreadCount > 0 && (
            <Tag type="red" size="sm" data-testid="alert-feed-unread-count">
              {unreadCount}
            </Tag>
          )}
        </div>
        {unreadCount > 0 && (
          <Button
            kind="ghost"
            size="sm"
            onClick={handleMarkAllAsRead}
            data-testid="alert-feed-mark-all-read"
          >
            {intl.formatMessage({ id: "qc.alerts.markAllRead" })}
          </Button>
        )}
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          subtitle={error}
          hideCloseButton
          lowContrast
          data-testid="alert-feed-error"
        />
      )}

      {/* Alert list */}
      <div className="alert-feed-list" data-testid="alert-feed-list">
        {alerts.length === 0 ? (
          <div className="alert-feed-empty" data-testid="alert-feed-empty">
            <Notification size={32} />
            <span>{intl.formatMessage({ id: "qc.alerts.noAlerts" })}</span>
          </div>
        ) : (
          alerts.slice(0, maxItems).map((alert) => (
            <ClickableTile
              key={alert.id}
              className={`alert-feed-item ${!alert.isRead ? "alert-feed-item--unread" : ""}`}
              onClick={() => handleAlertClick(alert)}
              data-testid={`alert-item-${alert.id}`}
            >
              {/* Unread indicator */}
              {!alert.isRead && (
                <div className="alert-feed-item-unread-indicator">
                  <NotificationNew size={16} />
                </div>
              )}

              {/* Alert content */}
              <div className="alert-feed-item-content">
                <div className="alert-feed-item-header">
                  <Tag
                    type={getSeverityColor(alert.severity)}
                    size="sm"
                    data-testid={`alert-severity-${alert.id}`}
                  >
                    {alert.ruleCode}
                  </Tag>
                  <span className="alert-feed-item-time" data-testid={`alert-time-${alert.id}`}>
                    {formatTimestamp(alert.createdDate || alert.timestamp)}
                  </span>
                </div>

                <div className="alert-feed-item-message" data-testid={`alert-message-${alert.id}`}>
                  {alert.message ||
                    intl.formatMessage(
                      { id: "qc.alerts.defaultMessage" },
                      {
                        rule: alert.ruleCode,
                        analyzer: alert.analyzerName,
                      }
                    )}
                </div>

                <div className="alert-feed-item-meta">
                  <span className="alert-feed-item-analyzer" data-testid={`alert-analyzer-${alert.id}`}>
                    {alert.analyzerName}
                  </span>
                  {alert.testName && (
                    <span className="alert-feed-item-test" data-testid={`alert-test-${alert.id}`}>
                      • {alert.testName}
                    </span>
                  )}
                </div>
              </div>

              {/* Mark as read button */}
              {!alert.isRead && (
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={CheckmarkOutline}
                  iconDescription={intl.formatMessage({ id: "qc.alerts.markRead" })}
                  onClick={(e) => handleMarkAsRead(alert.id, e)}
                  className="alert-feed-item-mark-read"
                  data-testid={`alert-mark-read-${alert.id}`}
                />
              )}
            </ClickableTile>
          ))
        )}
      </div>

      {/* View all link */}
      {alerts.length > 0 && (
        <div className="alert-feed-footer" data-testid="alert-feed-footer">
          <Button
            kind="ghost"
            size="sm"
            onClick={() => history.push("/analyzers/qc/violations")}
            data-testid="alert-feed-view-all"
          >
            {intl.formatMessage({ id: "qc.alerts.viewAll" })}
          </Button>
        </div>
      )}
    </div>
  );
};

AlertFeed.propTypes = {
  maxItems: PropTypes.number,
  autoRefresh: PropTypes.bool,
  refreshInterval: PropTypes.number,
};

AlertFeed.defaultProps = {
  maxItems: 10,
  autoRefresh: true,
  refreshInterval: 60000, // 1 minute
};

export default AlertFeed;
