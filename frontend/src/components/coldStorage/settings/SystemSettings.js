import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Button,
  Form,
  Stack,
  TextInput,
  Loading,
  Section,
  Heading,
  Toggle,
  Dropdown,
  Tile,
  InlineNotification,
} from "@carbon/react";
import { Settings } from "@carbon/icons-react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { fetchSystemConfig, saveSystemConfig } from "../api";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";

const SESSION_TIMEOUT_OPTIONS = [
  { label: "15 minutes", value: 15 },
  { label: "30 minutes", value: 30 },
  { label: "1 hour", value: 60 },
  { label: "2 hours", value: 120 },
  { label: "4 hours", value: 240 },
];

function SystemSettings() {
  const intl = useIntl();
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
  const [config, setConfig] = useState({
    modbusTcpPort: 502,
    bacnetUdpPort: 47808,
    twoFactorAuthEnabled: false,
    sessionTimeoutMinutes: 30,
  });
  const [systemInfo, setSystemInfo] = useState({
    systemVersion: "2.1.0",
    databaseVersion: "PostgreSQL 14.5",
    lastUpdate: new Date().toISOString(),
    uptimeSeconds: 0,
  });

  const loadConfig = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetchSystemConfig();
      setConfig({
        modbusTcpPort: response.modbusTcpPort || 502,
        bacnetUdpPort: response.bacnetUdpPort || 47808,
        twoFactorAuthEnabled: response.twoFactorAuthEnabled || false,
        sessionTimeoutMinutes: response.sessionTimeoutMinutes || 30,
      });
      if (response.systemInfo) {
        setSystemInfo({
          systemVersion: response.systemInfo.systemVersion || "2.1.0",
          databaseVersion:
            response.systemInfo.databaseVersion || "PostgreSQL 14.5",
          lastUpdate:
            response.systemInfo.lastUpdate || new Date().toISOString(),
          uptimeSeconds: response.systemInfo.uptimeSeconds || 0,
        });
      }
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "error.title" }),
        subtitle:
          intl.formatMessage({ id: "coldStorage.error.loadConfig" }) +
          (err.message || ""),
      });
    } finally {
      setLoading(false);
    }
  }, [notify, intl]);

  useEffect(() => {
    loadConfig();
  }, [loadConfig]);

  const handleToggle = (field, value) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
  };

  const handleChange = (field, value) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);

      await saveSystemConfig(config);
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: intl.formatMessage({ id: "coldStorage.config.saveSuccess" }),
      });
      await loadConfig();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "error.title" }),
        subtitle:
          intl.formatMessage({ id: "coldStorage.error.saveConfig" }) +
          (err.message || ""),
      });
    } finally {
      setSaving(false);
    }
  };

  const formatUptime = (seconds) => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${days}d ${hours}h ${minutes}m`;
  };

  const formatDateTime = (isoString) => {
    try {
      return new Date(isoString).toLocaleString();
    } catch {
      return isoString;
    }
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "coldStorage.loadingConfig" })}
      />
    );
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
          <Settings size={24} />
          <Heading>
            <FormattedMessage id="coldStorage.systemConfiguration" />
          </Heading>
        </div>

        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "coldStorage.configMoved" })}
          subtitle={intl.formatMessage({ id: "coldStorage.configMovedDesc" })}
          lowContrast={false}
          hideCloseButton
          style={{ marginBottom: "1.5rem" }}
        />

        <Form>
          <Stack gap={6}>
            {/* Protocol Configuration */}
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
              }}
            >
              <Heading style={{ marginBottom: "1rem", fontSize: "1rem" }}>
                <FormattedMessage id="coldStorage.protocolConfiguration" />
              </Heading>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
                  gap: "1rem",
                }}
              >
                <TextInput
                  id="modbus-tcp-port"
                  labelText={intl.formatMessage({
                    id: "coldStorage.modbusTcpPort",
                  })}
                  type="number"
                  min="1"
                  max="65535"
                  value={config.modbusTcpPort}
                  onChange={(e) =>
                    handleChange(
                      "modbusTcpPort",
                      parseInt(e.target.value) || 502,
                    )
                  }
                  disabled
                  readOnly
                />

                <TextInput
                  id="bacnet-udp-port"
                  labelText={intl.formatMessage({
                    id: "coldStorage.bacnetUdpPort",
                  })}
                  type="number"
                  min="1"
                  max="65535"
                  value={config.bacnetUdpPort}
                  onChange={(e) =>
                    handleChange(
                      "bacnetUdpPort",
                      parseInt(e.target.value) || 47808,
                    )
                  }
                  disabled
                  readOnly
                />
              </div>
            </div>

            {/* Security Settings */}
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
              }}
            >
              <Heading style={{ marginBottom: "1rem", fontSize: "1rem" }}>
                <FormattedMessage id="coldStorage.securitySettings" />
              </Heading>

              <Toggle
                id="two-factor-auth"
                labelText={intl.formatMessage({
                  id: "coldStorage.twoFactorAuth",
                })}
                labelA=""
                labelB=""
                toggled={config.twoFactorAuthEnabled}
                onToggle={(checked) =>
                  handleToggle("twoFactorAuthEnabled", checked)
                }
                style={{ marginBottom: "1rem" }}
                disabled
              />

              <Dropdown
                id="session-timeout"
                titleText={intl.formatMessage({
                  id: "coldStorage.sessionTimeout",
                })}
                label={`${config.sessionTimeoutMinutes} minutes`}
                items={SESSION_TIMEOUT_OPTIONS}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  SESSION_TIMEOUT_OPTIONS.find(
                    (opt) => opt.value === config.sessionTimeoutMinutes,
                  ) || SESSION_TIMEOUT_OPTIONS[1]
                }
                onChange={({ selectedItem }) =>
                  handleChange("sessionTimeoutMinutes", selectedItem.value)
                }
                disabled
              />
            </div>

            {/* System Information (Read-only) */}
            <Tile
              style={{
                padding: "1rem",
                backgroundColor: "#f4f4f4",
              }}
            >
              <Heading style={{ marginBottom: "1rem", fontSize: "1rem" }}>
                <FormattedMessage id="coldStorage.systemInformation" />
              </Heading>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
                  gap: "1rem",
                }}
              >
                <div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      marginBottom: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="coldStorage.systemVersion" />
                  </p>
                  <p style={{ fontWeight: "600" }}>
                    {systemInfo.systemVersion}
                  </p>
                </div>

                <div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      marginBottom: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="coldStorage.databaseVersion" />
                  </p>
                  <p style={{ fontWeight: "600" }}>
                    {systemInfo.databaseVersion}
                  </p>
                </div>

                <div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      marginBottom: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="coldStorage.lastUpdate" />
                  </p>
                  <p style={{ fontWeight: "600" }}>
                    {formatDateTime(systemInfo.lastUpdate)}
                  </p>
                </div>

                <div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      marginBottom: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="coldStorage.uptime" />
                  </p>
                  <p style={{ fontWeight: "600" }}>
                    {formatUptime(systemInfo.uptimeSeconds)}
                  </p>
                </div>
              </div>
            </Tile>

            <InlineNotification
              kind="info"
              title={intl.formatMessage({ id: "coldStorage.readOnlyMode" })}
              subtitle={intl.formatMessage({
                id: "coldStorage.readOnlyModeDesc",
              })}
              lowContrast
              hideCloseButton
            />
          </Stack>
        </Form>
      </Section>
    </div>
  );
}

export default injectIntl(SystemSettings);
