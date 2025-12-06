import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Button,
  Form,
  Stack,
  TextInput,
  Loading,
  Section,
  Heading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, injectIntl } from "react-intl";
import { fetchDevices, updateDeviceThresholds } from "../api";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";

function TemperatureThresholds({ intl }) {
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
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [thresholds, setThresholds] = useState({});

  const loadDevices = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetchDevices("");
      setDevices(response || []);

      // Initialize thresholds from devices
      const initialThresholds = {};
      (response || []).forEach((device) => {
        initialThresholds[device.id] = {
          targetTemperature: device.targetTemperature || -20,
          warningThreshold: device.warningThreshold || -18,
          criticalThreshold: device.criticalThreshold || -15,
          pollInterval: device.pollingIntervalSeconds || 60,
        };
      });
      setThresholds(initialThresholds);
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: "Failed to load devices: " + (err.message || ""),
      });
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    loadDevices();
  }, [loadDevices]);

  const handleThresholdChange = (deviceId, field, value) => {
    setThresholds((prev) => ({
      ...prev,
      [deviceId]: {
        ...prev[deviceId],
        [field]: parseFloat(value) || 0,
      },
    }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);

      for (const device of devices) {
        const deviceThresholds = thresholds[device.id];
        if (deviceThresholds) {
          await updateDeviceThresholds(
            device.id,
            deviceThresholds.targetTemperature,
            deviceThresholds.warningThreshold,
            deviceThresholds.criticalThreshold,
            deviceThresholds.pollInterval,
          );
        }
      }

      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: "Threshold configuration saved successfully",
      });
      await loadDevices();
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle:
          "Failed to save threshold configuration: " + (err.message || ""),
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <Loading description="Loading devices..." />;
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
          <Heading>Temperature Threshold Configuration</Heading>
        </div>

        {devices.length === 0 ? (
          <InlineNotification
            kind="info"
            title="No Devices Configured"
            subtitle="Please add devices in Device Management to configure temperature thresholds."
            lowContrast
            hideCloseButton
          />
        ) : (
          <Form>
            <Stack gap={7}>
              {devices.map((device) => {
                const deviceThresholds = thresholds[device.id] || {};
                return (
                  <div
                    key={device.id}
                    style={{
                      padding: "1.5rem",
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      backgroundColor: "#f4f4f4",
                    }}
                  >
                    <div style={{ marginBottom: "1rem" }}>
                      <strong style={{ fontSize: "1rem" }}>
                        {device.name}
                      </strong>
                      <br />
                      <span style={{ color: "#525252", fontSize: "0.875rem" }}>
                        {device.id}
                      </span>
                    </div>

                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns:
                          "repeat(auto-fit, minmax(200px, 1fr))",
                        gap: "1rem",
                      }}
                    >
                      <TextInput
                        id={`target-${device.id}`}
                        labelText="Target Temperature (°C)"
                        type="number"
                        step="0.1"
                        value={deviceThresholds.targetTemperature || ""}
                        onChange={(e) =>
                          handleThresholdChange(
                            device.id,
                            "targetTemperature",
                            e.target.value,
                          )
                        }
                      />

                      <TextInput
                        id={`warning-${device.id}`}
                        labelText="Warning Threshold (°C)"
                        type="number"
                        step="0.1"
                        value={deviceThresholds.warningThreshold || ""}
                        onChange={(e) =>
                          handleThresholdChange(
                            device.id,
                            "warningThreshold",
                            e.target.value,
                          )
                        }
                      />

                      <TextInput
                        id={`critical-${device.id}`}
                        labelText="Critical Threshold (°C)"
                        type="number"
                        step="0.1"
                        value={deviceThresholds.criticalThreshold || ""}
                        onChange={(e) =>
                          handleThresholdChange(
                            device.id,
                            "criticalThreshold",
                            e.target.value,
                          )
                        }
                      />

                      <TextInput
                        id={`poll-${device.id}`}
                        labelText="Poll Interval (seconds)"
                        type="number"
                        value={deviceThresholds.pollInterval || ""}
                        onChange={(e) =>
                          handleThresholdChange(
                            device.id,
                            "pollInterval",
                            e.target.value,
                          )
                        }
                      />
                    </div>
                  </div>
                );
              })}

              <Button
                kind="primary"
                onClick={handleSave}
                disabled={saving || devices.length === 0}
                style={{ width: "100%", maxWidth: "none" }}
              >
                {saving ? "Saving..." : "Save Threshold Configuration"}
              </Button>
            </Stack>
          </Form>
        )}
      </Section>
    </div>
  );
}

export default injectIntl(TemperatureThresholds);
