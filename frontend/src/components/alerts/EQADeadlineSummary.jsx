import React from "react";
import { Grid, Column, Button, Tag } from "@carbon/react";
import { Time } from "@carbon/icons-react";
import { useIntl } from "react-intl";

const EQADeadlineSummary = ({ alerts, onFilterByProvider }) => {
  const intl = useIntl();

  const eqaAlerts = (alerts || []).filter(
    (a) =>
      (a.alertType === "EQA_DEADLINE" || a.type === "EQA_DEADLINE") &&
      (a.status === "OPEN" || !a.acknowledged),
  );

  if (eqaAlerts.length === 0) {
    return null;
  }

  // Group by provider - extract from contextData or message
  const providerMap = {};
  eqaAlerts.forEach((alert) => {
    let provider = "Unknown";
    if (alert.contextData) {
      try {
        const ctx = JSON.parse(alert.contextData);
        if (ctx.provider) provider = ctx.provider;
      } catch {
        // try to extract provider from message
      }
    }
    if (provider === "Unknown" && alert.message) {
      const match = alert.message.match(
        /(?:CDC|WHO|CAP|UKNEQAS|EQAS|RIQAS|NHRL|NTRL|NMCP)/i,
      );
      if (match) {
        provider = match[0].toUpperCase();
      }
    }

    if (!providerMap[provider]) {
      providerMap[provider] = { alerts: [], criticalCount: 0 };
    }
    providerMap[provider].alerts.push(alert);

    // Count critical (≤3 days remaining)
    if (alert.severity === "CRITICAL") {
      providerMap[provider].criticalCount++;
    }
  });

  const providers = Object.entries(providerMap).sort(
    (a, b) => b[1].alerts.length - a[1].alerts.length,
  );

  return (
    <div
      style={{
        padding: "1rem",
        backgroundColor: "#edf5ff",
        borderRadius: "8px",
        border: "1px solid #bfdbfe",
        marginTop: "1.5rem",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "0.5rem",
          marginBottom: "0.25rem",
        }}
      >
        <Time size={20} style={{ color: "#0043ce" }} />
        <h4 style={{ color: "#0043ce", margin: 0 }}>
          {intl.formatMessage({ id: "alerts.eqaDeadlineSummary.title" })}
        </h4>
      </div>
      <p
        style={{
          color: "#525252",
          fontSize: "0.875rem",
          marginBottom: "1rem",
        }}
      >
        {intl.formatMessage({ id: "alerts.eqaDeadlineSummary.subtitle" })}
      </p>

      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {providers.map(([provider, data]) => (
          <div
            key={provider}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              padding: "0.75rem 1rem",
              backgroundColor: "#fff",
              borderRadius: "8px",
              border: "1px solid #e0e0e0",
            }}
          >
            <div>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <span style={{ fontWeight: 500 }}>{provider}</span>
                <Tag type="blue" size="sm">
                  {data.alerts.length}
                </Tag>
                {data.criticalCount > 0 && (
                  <Tag type="red" size="sm">
                    {data.criticalCount}{" "}
                    {intl.formatMessage({
                      id: "alerts.eqaDeadlineSummary.critical",
                    })}
                  </Tag>
                )}
              </div>
              <p
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  marginTop: "0.25rem",
                }}
              >
                {intl.formatMessage(
                  { id: "alerts.eqaDeadlineSummary.deadlineCount" },
                  { count: data.alerts.length },
                )}
              </p>
            </div>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => onFilterByProvider && onFilterByProvider(provider)}
            >
              {intl.formatMessage({ id: "alerts.eqaDeadlineSummary.viewAll" })}
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default EQADeadlineSummary;
