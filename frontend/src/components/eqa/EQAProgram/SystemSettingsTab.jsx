import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Toggle,
  TextInput,
  Select,
  SelectItem,
  Button,
  InlineNotification,
  NumberInput,
} from "@carbon/react";
import { Save } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const SystemSettingsTab = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notification, setNotification] = useState(null);

  // Notification Settings
  const [eqaDeadlineAlerts, setEqaDeadlineAlerts] = useState(true);
  const [emailNotifications, setEmailNotifications] = useState(false);
  const [statOrderAlerts, setStatOrderAlerts] = useState(true);
  const [alertThreshold, setAlertThreshold] = useState("7");

  // Integration Settings
  const [fhirIntegration, setFhirIntegration] = useState(false);
  const [apiBaseUrl, setApiBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");

  // Performance Analysis
  const [autoZScore, setAutoZScore] = useState(true);
  const [zScoreMin, setZScoreMin] = useState(-2);
  const [zScoreMax, setZScoreMax] = useState(2);
  const [generateReports, setGenerateReports] = useState(true);

  const fetchConfig = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/alert-notification-config", (data) => {
      if (data) {
        const configs = data.alertConfigs || {};
        if (configs.EQA_DEADLINE_ALERT) {
          setEqaDeadlineAlerts(configs.EQA_DEADLINE_ALERT.email || false);
        }
        if (configs.EQUIPMENT_ALERT) {
          setEmailNotifications(configs.EQUIPMENT_ALERT.email || false);
        }
        setStatOrderAlerts(
          configs.STAT_ORDER_ALERT ? configs.STAT_ORDER_ALERT.email : true,
        );
        if (data.escalationEnabled != null) {
          setStatOrderAlerts(data.escalationEnabled);
        }
        if (data.escalationDelayMinutes) {
          setAlertThreshold(String(data.escalationDelayMinutes));
        }
        if (data.supervisorEmail) {
          setApiBaseUrl(data.supervisorEmail);
        }

        // Integration settings from extended config
        if (data.fhirIntegration != null) {
          setFhirIntegration(data.fhirIntegration);
        }
        if (data.apiBaseUrl) {
          setApiBaseUrl(data.apiBaseUrl);
        }
        if (data.apiKey) {
          setApiKey(data.apiKey);
        }

        // Performance settings
        if (data.autoZScore != null) {
          setAutoZScore(data.autoZScore);
        }
        if (data.zScoreMin != null) {
          setZScoreMin(data.zScoreMin);
        }
        if (data.zScoreMax != null) {
          setZScoreMax(data.zScoreMax);
        }
        if (data.generateReports != null) {
          setGenerateReports(data.generateReports);
        }
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  const handleSave = () => {
    setSaving(true);
    const payload = {
      alertConfigs: {
        EQA_DEADLINE_ALERT: {
          email: eqaDeadlineAlerts,
          sms: false,
        },
        EQUIPMENT_ALERT: {
          email: emailNotifications,
          sms: false,
        },
        STAT_ORDER_ALERT: {
          email: statOrderAlerts,
          sms: false,
        },
      },
      escalationEnabled: statOrderAlerts,
      escalationDelayMinutes: parseInt(alertThreshold, 10) || 7,
      supervisorEmail: "",
      fhirIntegration: fhirIntegration,
      apiBaseUrl: apiBaseUrl,
      apiKey: apiKey,
      autoZScore: autoZScore,
      zScoreMin: zScoreMin,
      zScoreMax: zScoreMax,
      generateReports: generateReports,
    };

    postToOpenElisServerJsonResponse(
      "/rest/alert-notification-config",
      JSON.stringify(payload),
      (response) => {
        setSaving(false);
        if (response) {
          setNotification({
            kind: "success",
            message: intl.formatMessage({ id: "eqa.settings.saveSuccess" }),
          });
        } else {
          setNotification({
            kind: "error",
            message: intl.formatMessage({ id: "eqa.settings.saveError" }),
          });
        }
      },
    );
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem 0", color: "#525252" }}>
        {intl.formatMessage({ id: "eqa.settings.loading" })}
      </div>
    );
  }

  const cardStyle = {
    padding: "1.5rem",
    backgroundColor: "#fff",
    borderRadius: "8px",
    border: "1px solid #e0e0e0",
    marginBottom: "1.5rem",
  };

  const cardTitleStyle = {
    fontSize: "1rem",
    fontWeight: 600,
    marginBottom: "0.25rem",
  };

  const cardSubtitleStyle = {
    fontSize: "0.875rem",
    color: "#525252",
    marginBottom: "1.25rem",
  };

  const toggleRowStyle = {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "0.75rem 0",
    borderBottom: "1px solid #f4f4f4",
  };

  return (
    <div style={{ paddingTop: "1rem" }}>
      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.message}
          onCloseButtonClick={() => setNotification(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          {/* Notification Settings */}
          <div style={cardStyle}>
            <h4 style={cardTitleStyle}>
              {intl.formatMessage({ id: "eqa.settings.notifications.title" })}
            </h4>
            <p style={cardSubtitleStyle}>
              {intl.formatMessage({
                id: "eqa.settings.notifications.subtitle",
              })}
            </p>

            <div style={toggleRowStyle}>
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.eqaDeadline",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.eqaDeadline.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-eqa-deadline"
                toggled={eqaDeadlineAlerts}
                onToggle={(val) => setEqaDeadlineAlerts(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>

            <div style={toggleRowStyle}>
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.email",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.email.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-email"
                toggled={emailNotifications}
                onToggle={(val) => setEmailNotifications(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>

            <div style={toggleRowStyle}>
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.stat",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.notifications.stat.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-stat"
                toggled={statOrderAlerts}
                onToggle={(val) => setStatOrderAlerts(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>

            <div style={{ marginTop: "1rem" }}>
              <Select
                id="alert-threshold"
                labelText={intl.formatMessage({
                  id: "eqa.settings.notifications.threshold",
                })}
                value={alertThreshold}
                onChange={(e) => setAlertThreshold(e.target.value)}
              >
                <SelectItem value="3" text="3 days" />
                <SelectItem value="5" text="5 days" />
                <SelectItem value="7" text="7 days" />
                <SelectItem value="14" text="14 days" />
              </Select>
            </div>
          </div>

          {/* Integration Settings */}
          <div style={cardStyle}>
            <h4 style={cardTitleStyle}>
              {intl.formatMessage({ id: "eqa.settings.integration.title" })}
            </h4>
            <p style={cardSubtitleStyle}>
              {intl.formatMessage({ id: "eqa.settings.integration.subtitle" })}
            </p>

            <div style={toggleRowStyle}>
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.integration.fhir",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.integration.fhir.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-fhir"
                toggled={fhirIntegration}
                onToggle={(val) => setFhirIntegration(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>

            {fhirIntegration && (
              <Grid condensed style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="api-base-url"
                    labelText={intl.formatMessage({
                      id: "eqa.settings.integration.apiUrl",
                    })}
                    value={apiBaseUrl}
                    onChange={(e) => setApiBaseUrl(e.target.value)}
                    placeholder="https://fhir.example.org/fhir"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="api-key"
                    type="password"
                    labelText={intl.formatMessage({
                      id: "eqa.settings.integration.apiKey",
                    })}
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    placeholder="••••••••"
                  />
                </Column>
              </Grid>
            )}
          </div>

          {/* Performance Analysis */}
          <div style={cardStyle}>
            <h4 style={cardTitleStyle}>
              {intl.formatMessage({ id: "eqa.settings.performance.title" })}
            </h4>
            <p style={cardSubtitleStyle}>
              {intl.formatMessage({ id: "eqa.settings.performance.subtitle" })}
            </p>

            <div style={toggleRowStyle}>
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.performance.autoZScore",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.performance.autoZScore.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-zscore"
                toggled={autoZScore}
                onToggle={(val) => setAutoZScore(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>

            {autoZScore && (
              <Grid condensed style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <NumberInput
                    id="zscore-min"
                    label={intl.formatMessage({
                      id: "eqa.settings.performance.zScoreMin",
                    })}
                    value={zScoreMin}
                    onChange={(e, { value }) => setZScoreMin(value)}
                    min={-5}
                    max={0}
                    step={0.5}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <NumberInput
                    id="zscore-max"
                    label={intl.formatMessage({
                      id: "eqa.settings.performance.zScoreMax",
                    })}
                    value={zScoreMax}
                    onChange={(e, { value }) => setZScoreMax(value)}
                    min={0}
                    max={5}
                    step={0.5}
                  />
                </Column>
              </Grid>
            )}

            <div
              style={{
                ...toggleRowStyle,
                marginTop: autoZScore ? "1rem" : 0,
              }}
            >
              <div>
                <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.performance.generateReports",
                  })}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {intl.formatMessage({
                    id: "eqa.settings.performance.generateReports.desc",
                  })}
                </div>
              </div>
              <Toggle
                id="toggle-reports"
                toggled={generateReports}
                onToggle={(val) => setGenerateReports(val)}
                labelA=""
                labelB=""
                size="sm"
              />
            </div>
          </div>

          {/* Save Button */}
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <Button renderIcon={Save} onClick={handleSave} disabled={saving}>
              {saving
                ? intl.formatMessage({ id: "eqa.settings.saving" })
                : intl.formatMessage({ id: "button.save" })}
            </Button>
          </div>
        </Column>
      </Grid>
    </div>
  );
};

export default SystemSettingsTab;
