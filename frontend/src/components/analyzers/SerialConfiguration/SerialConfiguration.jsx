/**
 * SerialConfiguration Component
 *
 * Form component for configuring serial port settings for analyzers
 * Uses Carbon Design System components
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Dropdown,
  InlineNotification,
  FormGroup,
  NumberInput,
  Toggle,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createSerialPortConfiguration,
  updateSerialPortConfiguration,
  connectSerialPort,
  disconnectSerialPort,
  getSerialPortStatus,
} from "../../../services/serialService";
import "./SerialConfiguration.css";

const SerialConfiguration = ({
  analyzerId,
  configuration,
  open,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const isEditMode = !!configuration;

  const [formData, setFormData] = useState({
    analyzerId: analyzerId || null,
    portName: "",
    baudRate: 9600,
    dataBits: 8,
    stopBits: "ONE",
    parity: "NONE",
    flowControl: "NONE",
    active: true,
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState(null);

  // Stop bits options
  const stopBitsOptions = [
    { id: "ONE", text: "1" },
    { id: "ONE_POINT_FIVE", text: "1.5" },
    { id: "TWO", text: "2" },
  ];

  // Parity options
  const parityOptions = [
    {
      id: "NONE",
      text: intl.formatMessage({ id: "serial.config.parity.none" }),
    },
    {
      id: "EVEN",
      text: intl.formatMessage({ id: "serial.config.parity.even" }),
    },
    { id: "ODD", text: intl.formatMessage({ id: "serial.config.parity.odd" }) },
    {
      id: "MARK",
      text: intl.formatMessage({ id: "serial.config.parity.mark" }),
    },
    {
      id: "SPACE",
      text: intl.formatMessage({ id: "serial.config.parity.space" }),
    },
  ];

  // Flow control options
  const flowControlOptions = [
    {
      id: "NONE",
      text: intl.formatMessage({ id: "serial.config.flowControl.none" }),
    },
    { id: "RTS_CTS", text: "RTS/CTS" },
    { id: "XON_XOFF", text: "XON/XOFF" },
  ];

  // Initialize form data
  useEffect(() => {
    if (configuration) {
      setFormData({
        analyzerId: configuration.analyzerId,
        portName: configuration.portName || "",
        baudRate: configuration.baudRate || 9600,
        dataBits: configuration.dataBits || 8,
        stopBits: configuration.stopBits || "ONE",
        parity: configuration.parity || "NONE",
        flowControl: configuration.flowControl || "NONE",
        active:
          configuration.active !== undefined ? configuration.active : true,
      });

      // Load connection status if configuration has ID
      if (configuration.id) {
        loadConnectionStatus(configuration.id);
      }
    } else {
      setFormData({
        analyzerId: analyzerId || null,
        portName: "",
        baudRate: 9600,
        dataBits: 8,
        stopBits: "ONE",
        parity: "NONE",
        flowControl: "NONE",
        active: true,
      });
    }
    setErrors({});
    setNotification(null);
    setConnectionStatus(null);
  }, [configuration, analyzerId, open]);

  const loadConnectionStatus = (configId) => {
    getSerialPortStatus(configId, (data) => {
      if (data && data.status) {
        setConnectionStatus(data.status);
      }
    });
  };

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.analyzerId) {
      newErrors.analyzerId = intl.formatMessage({
        id: "serial.config.validation.analyzerId.required",
      });
    }

    if (!formData.portName || !formData.portName.trim()) {
      newErrors.portName = intl.formatMessage({
        id: "serial.config.validation.portName.required",
      });
    }

    if (formData.baudRate < 9600 || formData.baudRate > 115200) {
      newErrors.baudRate = intl.formatMessage({
        id: "serial.config.validation.baudRate.invalid",
      });
    }

    if (formData.dataBits !== 7 && formData.dataBits !== 8) {
      newErrors.dataBits = intl.formatMessage({
        id: "serial.config.validation.dataBits.invalid",
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setNotification(null);

    const callback = (response, extraParams) => {
      setIsSubmitting(false);
      if (response.error) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "serial.config.save.error" }),
          subtitle: response.error,
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "serial.config.save.success" }),
        });
        if (onSave) {
          onSave(response);
        }
        setTimeout(() => {
          onClose();
        }, 1000);
      }
    };

    if (isEditMode) {
      updateSerialPortConfiguration(configuration.id, formData, callback);
    } else {
      createSerialPortConfiguration(formData, callback);
    }
  };

  const handleConnect = () => {
    if (!configuration || !configuration.id) {
      return;
    }

    connectSerialPort(configuration.id, (response) => {
      if (response.error) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "serial.config.connect.error" }),
          subtitle: response.error,
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "serial.config.connect.success" }),
        });
        loadConnectionStatus(configuration.id);
      }
    });
  };

  const handleDisconnect = () => {
    if (!configuration || !configuration.id) {
      return;
    }

    disconnectSerialPort(configuration.id, (response) => {
      if (response.error) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "serial.config.disconnect.error" }),
          subtitle: response.error,
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "serial.config.disconnect.success" }),
        });
        loadConnectionStatus(configuration.id);
      }
    });
  };

  return (
    <ComposedModal open={open} onClose={onClose} size="md">
      <ModalHeader>
        <h2>
          {isEditMode
            ? intl.formatMessage({ id: "serial.config.edit.title" })
            : intl.formatMessage({ id: "serial.config.create.title" })}
        </h2>
      </ModalHeader>
      <ModalBody>
        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            lowContrast
          />
        )}

        <FormGroup
          legendText={intl.formatMessage({ id: "serial.config.form.legend" })}
        >
          <TextInput
            id="portName"
            labelText={intl.formatMessage({
              id: "serial.config.portName.label",
            })}
            value={formData.portName}
            onChange={(e) => handleFieldChange("portName", e.target.value)}
            invalid={!!errors.portName}
            invalidText={errors.portName}
            placeholder="/dev/ttyUSB0 or COM3"
            required
          />

          <NumberInput
            id="baudRate"
            label={intl.formatMessage({ id: "serial.config.baudRate.label" })}
            value={formData.baudRate}
            onChange={(e) =>
              handleFieldChange(
                "baudRate",
                e.imaginaryTarget?.value || e.target.value,
              )
            }
            invalid={!!errors.baudRate}
            invalidText={errors.baudRate}
            min={9600}
            max={115200}
            step={9600}
            required
          />

          <NumberInput
            id="dataBits"
            label={intl.formatMessage({ id: "serial.config.dataBits.label" })}
            value={formData.dataBits}
            onChange={(e) =>
              handleFieldChange(
                "dataBits",
                e.imaginaryTarget?.value || e.target.value,
              )
            }
            invalid={!!errors.dataBits}
            invalidText={errors.dataBits}
            min={7}
            max={8}
            required
          />

          <Dropdown
            id="stopBits"
            titleText={intl.formatMessage({
              id: "serial.config.stopBits.label",
            })}
            items={stopBitsOptions}
            selectedItem={stopBitsOptions.find(
              (opt) => opt.id === formData.stopBits,
            )}
            onChange={(e) =>
              handleFieldChange("stopBits", e.selectedItem?.id || "ONE")
            }
            itemToString={(item) => (item ? item.text : "")}
          />

          <Dropdown
            id="parity"
            titleText={intl.formatMessage({ id: "serial.config.parity.label" })}
            items={parityOptions}
            selectedItem={parityOptions.find(
              (opt) => opt.id === formData.parity,
            )}
            onChange={(e) =>
              handleFieldChange("parity", e.selectedItem?.id || "NONE")
            }
            itemToString={(item) => (item ? item.text : "")}
          />

          <Dropdown
            id="flowControl"
            titleText={intl.formatMessage({
              id: "serial.config.flowControl.label",
            })}
            items={flowControlOptions}
            selectedItem={flowControlOptions.find(
              (opt) => opt.id === formData.flowControl,
            )}
            onChange={(e) =>
              handleFieldChange("flowControl", e.selectedItem?.id || "NONE")
            }
            itemToString={(item) => (item ? item.text : "")}
          />

          <Toggle
            id="active"
            labelText={intl.formatMessage({ id: "serial.config.active.label" })}
            toggled={formData.active}
            onToggle={(checked) => handleFieldChange("active", checked)}
          />

          {isEditMode && configuration?.id && (
            <div style={{ marginTop: "1rem" }}>
              <p>
                <strong>
                  {intl.formatMessage({ id: "serial.config.status.label" })}:
                </strong>{" "}
                {connectionStatus || "DISCONNECTED"}
              </p>
              <div style={{ marginTop: "0.5rem" }}>
                <Button
                  kind="secondary"
                  onClick={handleConnect}
                  disabled={connectionStatus === "CONNECTED"}
                  style={{ marginRight: "0.5rem" }}
                >
                  {intl.formatMessage({ id: "serial.config.connect.button" })}
                </Button>
                <Button
                  kind="danger"
                  onClick={handleDisconnect}
                  disabled={connectionStatus !== "CONNECTED"}
                >
                  {intl.formatMessage({
                    id: "serial.config.disconnect.button",
                  })}
                </Button>
              </div>
            </div>
          )}
        </FormGroup>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose} disabled={isSubmitting}>
          {intl.formatMessage({ id: "button.cancel" })}
        </Button>
        <Button kind="primary" onClick={handleSave} disabled={isSubmitting}>
          {isSubmitting
            ? intl.formatMessage({ id: "button.saving" })
            : intl.formatMessage({ id: "button.save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default SerialConfiguration;
