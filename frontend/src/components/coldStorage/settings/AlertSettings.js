import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Button,
  Loading,
  Section,
  Heading,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Checkbox,
  InlineNotification,
  Toggle,
  NumberInput,
  TextInput,
} from "@carbon/react";
import { Notification } from "@carbon/icons-react";
import { FormattedMessage, injectIntl } from "react-intl";
import { fetchAlertConfig, saveAlertConfig } from "../api";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";

// Map UI alert types to backend NotificationNature enum values
const ALERT_TYPES = [
  {
    id: "temperature-alerts",
    alertType: "Temperature Alerts",
    description:
      "Alerts triggered when freezer temperature exceeds defined thresholds",
    nature: "FREEZER_TEMPERATURE_ALERT",
  },
  {
    id: "equipment-failure",
    alertType: "Equipment Failure",
    description: "Alerts for equipment malfunctions or connectivity issues",
    nature: "EQUIPMENT_ALERT",
  },
  {
    id: "inventory-alerts",
    alertType: "Inventory Alerts",
    description: "Alerts for low inventory or stock management",
    nature: "INVENTORY_ALERT",
  },
];

function AlertSettings({ intl }) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [preferences, setPreferences] = useState(
    ALERT_TYPES.map((type) => ({
      ...type,
      email: false,
      sms: false,
    })),
  );
  const [escalationEnabled, setEscalationEnabled] = useState(false);
  const [escalationDelayMinutes, setEscalationDelayMinutes] = useState(15);
  const [supervisorEmail, setSupervisorEmail] = useState("");

  const loadConfig = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetchAlertConfig();

      // The backend returns per-alert-type configuration
      const alertConfigs = response.alertConfigs || {};

      setPreferences((prev) =>
        prev.map((pref) => ({
          ...pref,
          email: alertConfigs[pref.nature]?.email || false,
          sms: alertConfigs[pref.nature]?.sms || false,
        })),
      );

      // Load escalation settings
      setEscalationEnabled(response.escalationEnabled || false);
      setEscalationDelayMinutes(response.escalationDelayMinutes || 15);
      setSupervisorEmail(response.supervisorEmail || "");
    } catch (err) {
      // If config doesn't exist yet, use defaults
      console.warn("Alert config not found, using defaults:", err);
      notify({
        kind: NotificationKinds.info,
        title: "No Configuration Found",
        subtitle:
          "Using default notification settings. Save to create configuration.",
      });
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  const handleToggle = (id, field, value) => {
    setPreferences((prev) =>
      prev.map((pref) => (pref.id === id ? { ...pref, [field]: value } : pref)),
    );
  };

  const handleSave = async () => {
    try {
      setSaving(true);

      // Build per-alert-type configuration
      const alertConfigs = {};
      preferences.forEach((pref) => {
        alertConfigs[pref.nature] = {
          email: pref.email,
          sms: pref.sms,
        };
      });

      // Build config object with granular alert configurations
      const config = {
        alertConfigs: alertConfigs,
        escalationEnabled: escalationEnabled,
        escalationDelayMinutes: escalationDelayMinutes,
        supervisorEmail: supervisorEmail,
      };

      await saveAlertConfig(config);

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle:
          "Notification preferences saved successfully. Each alert type will use its configured notification methods.",
      });

      await loadConfig();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle:
          "Failed to save notification preferences: " + (err.message || ""),
      });
    } finally {
      setSaving(false);
    }
  };

  const headers = [
    { key: "alertType", header: "Alert Type" },
    { key: "description", header: "Description" },
    { key: "email", header: "Email" },
    { key: "sms", header: "SMS" },
  ];

  if (loading) {
    return <Loading description="Loading notification preferences..." />;
  }

  return (
    <div style={{ padding: "1rem 0" }}>
      {notificationVisible === true ? <AlertDialog /> : ""}

      <Section>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            marginBottom: "1.5rem",
          }}
        >
          <Notification size={24} />
          <Heading>Alert Configuration</Heading>
        </div>

        <Heading style={{ marginBottom: "1rem", fontSize: "1.125rem" }}>
          Email Notifications & SMS Notifications
        </Heading>
        <InlineNotification
          kind="info"
          title="Granular Notification Control"
          subtitle="Configure email and SMS notifications for each alert type independently. Enable the notification methods you want for each specific alert category."
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1.5rem" }}
        />

        <DataTable rows={preferences} headers={headers}>
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <Table {...getTableProps()} size="lg">
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const preference = preferences.find((p) => p.id === row.id);
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      <TableCell>
                        <strong>{preference.alertType}</strong>
                      </TableCell>
                      <TableCell>
                        <span
                          style={{ color: "#525252", fontSize: "0.875rem" }}
                        >
                          {preference.description}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`${preference.id}-email`}
                          labelText=""
                          checked={preference.email}
                          onChange={(e) =>
                            handleToggle(
                              preference.id,
                              "email",
                              e.target.checked,
                            )
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`${preference.id}-sms`}
                          labelText=""
                          checked={preference.sms}
                          onChange={(e) =>
                            handleToggle(preference.id, "sms", e.target.checked)
                          }
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </DataTable>

        <div style={{ marginTop: "2rem", marginBottom: "2rem" }}>
          <Heading style={{ marginBottom: "1rem" }}>Escalation Rules</Heading>
          <div style={{ marginBottom: "1rem" }}>
            <Toggle
              id="escalation-enabled"
              labelText="Automatically escalate unresolved alerts"
              toggled={escalationEnabled}
              onToggle={(checked) => setEscalationEnabled(checked)}
            />
          </div>

          {escalationEnabled && (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
                marginTop: "1rem",
              }}
            >
              <NumberInput
                id="escalation-delay"
                label="Escalation Delay (minutes)"
                helperText="Time to wait before escalating an unresolved alert"
                value={escalationDelayMinutes}
                min={1}
                max={1440}
                step={1}
                onChange={(e, { value }) => setEscalationDelayMinutes(value)}
              />
              <TextInput
                id="supervisor-email"
                labelText="Supervisor Email"
                helperText="Email address to send escalated alerts"
                value={supervisorEmail}
                onChange={(e) => setSupervisorEmail(e.target.value)}
                placeholder="supervisor@lab.com"
              />
            </div>
          )}
        </div>

        <div style={{ marginTop: "1.5rem" }}>
          <InlineNotification
            kind="warning"
            title="How Notifications Work"
            subtitle="When an alert is triggered (temperature threshold violation, equipment failure, etc.), the system will:"
            lowContrast
            hideCloseButton
          />
          <ul
            style={{
              marginTop: "1rem",
              marginLeft: "1.5rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            <li>Create an alert record in the system (visible in Dashboard)</li>
            <li>
              Send EMAIL notifications if enabled (to configured recipients)
            </li>
            <li>
              Send SMS notifications if enabled (to configured phone numbers)
            </li>
            <li>Log the notification attempt for audit trail purposes</li>
          </ul>
        </div>

        <Button
          kind="primary"
          onClick={handleSave}
          disabled={saving}
          style={{ marginTop: "1.5rem", width: "100%", maxWidth: "none" }}
        >
          {saving ? "Saving..." : "Save Notification Preferences"}
        </Button>
      </Section>
    </div>
  );
}

export default injectIntl(AlertSettings);
