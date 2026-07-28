import React, {
  useState,
  useEffect,
  useCallback,
  useContext,
  useMemo,
} from "react";
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
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { fetchAlertConfig, saveAlertConfig } from "../api";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";

// Map UI alert types to backend NotificationNature enum values
const getAlertTypes = (intl) => [
  {
    id: "temperature-alerts",
    alertType: intl.formatMessage({ id: "coldStorage.alert.temperature" }),
    description: intl.formatMessage({
      id: "coldStorage.alert.temperature.description",
    }),
    nature: "FREEZER_TEMPERATURE_ALERT",
  },
  {
    id: "equipment-failure",
    alertType: intl.formatMessage({ id: "coldStorage.alert.equipment" }),
    description: intl.formatMessage({
      id: "coldStorage.alert.equipment.description",
    }),
    nature: "EQUIPMENT_ALERT",
  },
  {
    id: "inventory-alerts",
    alertType: intl.formatMessage({ id: "coldStorage.alert.inventory" }),
    description: intl.formatMessage({
      id: "coldStorage.alert.inventory.description",
    }),
    nature: "INVENTORY_ALERT",
  },
];

function AlertSettings() {
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

  const alertTypes = useMemo(() => getAlertTypes(intl), [intl]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [preferences, setPreferences] = useState([]);
  const [escalationEnabled, setEscalationEnabled] = useState(false);
  const [escalationDelayMinutes, setEscalationDelayMinutes] = useState(15);
  const [supervisorEmail, setSupervisorEmail] = useState("");

  // Initialize preferences when alertTypes are available
  useEffect(() => {
    if (alertTypes.length > 0 && preferences.length === 0) {
      setPreferences(
        alertTypes.map((type) => ({
          ...type,
          email: false,
          sms: false,
        })),
      );
    }
  }, [alertTypes, preferences.length]);

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
        title: intl.formatMessage({ id: "coldStorage.noConfigFound" }),
        subtitle: intl.formatMessage({ id: "coldStorage.usingDefaults" }),
      });
    } finally {
      setLoading(false);
    }
  }, [notify, intl]);

  useEffect(() => {
    if (preferences.length > 0) {
      loadConfig();
    }
  }, [loadConfig, preferences.length]);

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
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: intl.formatMessage({ id: "coldStorage.alert.saveSuccess" }),
      });

      await loadConfig();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "error.title" }),
        subtitle:
          intl.formatMessage({ id: "coldStorage.alert.saveFailed" }) +
          (err.message || ""),
      });
    } finally {
      setSaving(false);
    }
  };

  const headers = [
    {
      key: "alertType",
      header: intl.formatMessage({ id: "coldStorage.alert.type" }),
    },
    {
      key: "description",
      header: intl.formatMessage({ id: "coldStorage.description" }),
    },
    { key: "email", header: intl.formatMessage({ id: "coldStorage.email" }) },
    { key: "sms", header: intl.formatMessage({ id: "coldStorage.sms" }) },
  ];

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "coldStorage.loadingPrefs" })}
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
          <Notification size={24} />
          <Heading>
            <FormattedMessage id="coldStorage.alertConfiguration" />
          </Heading>
        </div>

        <Heading style={{ marginBottom: "1rem", fontSize: "1.125rem" }}>
          <FormattedMessage id="coldStorage.emailSmsNotifications" />
        </Heading>
        <InlineNotification
          kind="info"
          title={intl.formatMessage({
            id: "coldStorage.granularNotificationControl",
          })}
          subtitle={intl.formatMessage({
            id: "coldStorage.granularNotificationControlDesc",
          })}
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
          <Heading style={{ marginBottom: "1rem" }}>
            <FormattedMessage id="coldStorage.escalationRules" />
          </Heading>
          <div style={{ marginBottom: "1rem" }}>
            <Toggle
              id="escalation-enabled"
              labelText={intl.formatMessage({
                id: "coldStorage.autoEscalate",
              })}
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
                label={intl.formatMessage({
                  id: "coldStorage.escalationDelay",
                })}
                helperText={intl.formatMessage({
                  id: "coldStorage.escalationDelayHelper",
                })}
                value={escalationDelayMinutes}
                min={1}
                max={1440}
                step={1}
                onChange={(e, { value }) => setEscalationDelayMinutes(value)}
              />
              <TextInput
                id="supervisor-email"
                labelText={intl.formatMessage({
                  id: "coldStorage.supervisorEmail",
                })}
                helperText={intl.formatMessage({
                  id: "coldStorage.supervisorEmailHelper",
                })}
                value={supervisorEmail}
                onChange={(e) => setSupervisorEmail(e.target.value)}
                placeholder={intl.formatMessage({
                  id: "coldStorage.supervisorEmailPlaceholder",
                })}
              />
            </div>
          )}
        </div>

        <div style={{ marginTop: "1.5rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "coldStorage.howNotificationsWork",
            })}
            subtitle={intl.formatMessage({
              id: "coldStorage.howNotificationsWorkDesc",
            })}
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
            <li>
              <FormattedMessage id="coldStorage.notification.createRecord" />
            </li>
            <li>
              <FormattedMessage id="coldStorage.notification.sendEmail" />
            </li>
            <li>
              <FormattedMessage id="coldStorage.notification.sendSms" />
            </li>
            <li>
              <FormattedMessage id="coldStorage.notification.logAttempt" />
            </li>
          </ul>
        </div>

        <Button
          kind="primary"
          onClick={handleSave}
          disabled={saving}
          style={{ marginTop: "1.5rem", width: "100%", maxWidth: "none" }}
        >
          {saving
            ? intl.formatMessage({ id: "coldStorage.saving" })
            : intl.formatMessage({ id: "coldStorage.saveNotificationPrefs" })}
        </Button>
      </Section>
    </div>
  );
}

export default injectIntl(AlertSettings);
