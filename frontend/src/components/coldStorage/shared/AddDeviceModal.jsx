import React, { useState, useEffect } from "react";
import {
  Modal,
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  Button,
  Checkbox,
  FormLabel,
  NumberInput,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";

const DEVICE_TYPE_OPTIONS = [
  { value: "freezer", label: "Freezer" },
  { value: "refrigerator", label: "Refrigerator" },
  { value: "cabinet", label: "Cabinet" },
  { value: "other", label: "Other" },
];

const PROTOCOL_OPTIONS = [
  { value: "TCP", label: "Modbus TCP" },
  { value: "RTU", label: "Modbus RTU" },
];

const PARITY_OPTIONS = [
  { value: "NONE", label: "None" },
  { value: "EVEN", label: "Even" },
  { value: "ODD", label: "Odd" },
  { value: "MARK", label: "Mark" },
  { value: "SPACE", label: "Space" },
];

const INITIAL_FORM_DATA = {
  name: "",
  deviceType: "freezer",
  roomId: "",
  protocol: "TCP",
  host: "",
  port: 502,
  serialPort: "",
  baudRate: 9600,
  dataBits: 8,
  stopBits: 1,
  parity: "NONE",
  slaveId: 1,
  temperatureRegister: 0,
  temperatureScale: 1.0,
  temperatureOffset: 0.0,
  humidityRegister: null,
  humidityScale: 1.0,
  humidityOffset: 0.0,
  temperatureRegister2: null,
  temperatureScale2: 1.0,
  temperatureOffset2: 0.0,
  registerCount: 1,
  wordOrder: "BIG_ENDIAN",
  rs485Mode: false,
  rs485RtsActiveHigh: false,
  rs485Termination: false,
  rs485RxDuringTx: false,
  rs485DelayBeforeMs: 0,
  rs485DelayAfterMs: 0,
};

export default function AddDeviceModal({
  isOpen,
  onClose,
  onSubmit,
  locations = [],
  onAddRoom,
  editingDevice = null,
}) {
  const intl = useIntl();
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);

  useEffect(() => {
    if (editingDevice) {
      setFormData({
        ...INITIAL_FORM_DATA,
        ...editingDevice,
      });
    } else {
      setFormData(INITIAL_FORM_DATA);
    }
  }, [editingDevice, isOpen]);

  const handleFormChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = () => {
    onSubmit(formData);
  };

  const isValid =
    formData.name &&
    formData.roomId &&
    formData.roomId !== "" &&
    (formData.protocol === "TCP" ? formData.host : formData.serialPort);

  return (
    <>
      <Modal
        open={isOpen}
        onRequestClose={onClose}
        onRequestSubmit={handleSubmit}
        modalHeading={editingDevice ? "Edit Device" : "Add New Device"}
        primaryButtonText={editingDevice ? "Update" : "Create"}
        secondaryButtonText="Cancel"
        primaryButtonDisabled={!isValid}
        size="sm"
      >
        <Form>
          <Stack gap={5}>
            <div>
              <FormLabel
                style={{
                  marginBottom: "1rem",
                  fontSize: "0.875rem",
                  fontWeight: "600",
                  color: "#161616",
                }}
              >
                Basic Information
              </FormLabel>
              <Stack gap={5}>
                <TextInput
                  id="name"
                  labelText="Device Name *"
                  placeholder="Enter device name"
                  value={formData.name}
                  onChange={(e) => handleFormChange("name", e.target.value)}
                  required
                />

                <Select
                  id="deviceType"
                  labelText="Device Type *"
                  value={formData.deviceType}
                  onChange={(e) =>
                    handleFormChange("deviceType", e.target.value)
                  }
                  required
                >
                  {DEVICE_TYPE_OPTIONS.map((opt) => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={opt.label}
                    />
                  ))}
                </Select>

                <div
                  style={{
                    display: "flex",
                    alignItems: "flex-end",
                    gap: "0.5rem",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <Select
                      id="roomId"
                      labelText="Room/Facility *"
                      value={formData.roomId}
                      onChange={(e) =>
                        handleFormChange("roomId", e.target.value)
                      }
                      required
                    >
                      <SelectItem
                        value=""
                        text={
                          locations.length === 0
                            ? "No rooms available"
                            : "Select a room"
                        }
                      />
                      {locations.map((location) => (
                        <SelectItem
                          key={location.id}
                          value={location.id.toString()}
                          text={location.name}
                        />
                      ))}
                    </Select>
                  </div>
                  {onAddRoom && (
                    <Button
                      kind="tertiary"
                      size="md"
                      renderIcon={Add}
                      onClick={onAddRoom}
                      style={{ marginBottom: "0.125rem" }}
                    >
                      Add New Room
                    </Button>
                  )}
                </div>
              </Stack>
            </div>

            <div
              style={{
                borderTop: "1px solid #e0e0e0",
                paddingTop: "1rem",
                marginTop: "0.5rem",
              }}
            >
              <FormLabel
                style={{
                  marginBottom: "1rem",
                  fontSize: "0.875rem",
                  fontWeight: "600",
                  color: "#161616",
                }}
              >
                Connection Settings
              </FormLabel>
              <Stack gap={5}>
                <Select
                  id="protocol"
                  labelText="Protocol *"
                  value={formData.protocol}
                  onChange={(e) => handleFormChange("protocol", e.target.value)}
                >
                  {PROTOCOL_OPTIONS.map((opt) => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={opt.label}
                    />
                  ))}
                </Select>

                {formData.protocol === "TCP" ? (
                  <>
                    <TextInput
                      id="host"
                      labelText="IP Address/Host *"
                      placeholder="192.168.1.100 or modbus-simulator"
                      value={formData.host}
                      onChange={(e) => handleFormChange("host", e.target.value)}
                      required
                    />
                    <NumberInput
                      id="port"
                      label="Port *"
                      value={formData.port}
                      onChange={(e, { value }) =>
                        handleFormChange("port", value)
                      }
                      min={1}
                      max={65535}
                    />
                  </>
                ) : (
                  <>
                    <TextInput
                      id="serialPort"
                      labelText="Serial Port *"
                      placeholder="/dev/ttyUSB0"
                      value={formData.serialPort}
                      onChange={(e) =>
                        handleFormChange("serialPort", e.target.value)
                      }
                      required
                    />
                    <NumberInput
                      id="baudRate"
                      label="Baud Rate *"
                      value={formData.baudRate}
                      onChange={(e, { value }) =>
                        handleFormChange("baudRate", value)
                      }
                      min={300}
                      max={115200}
                    />
                    <NumberInput
                      id="dataBits"
                      label="Data Bits *"
                      value={formData.dataBits}
                      onChange={(e, { value }) =>
                        handleFormChange("dataBits", value)
                      }
                      min={5}
                      max={8}
                    />
                    <NumberInput
                      id="stopBits"
                      label="Stop Bits *"
                      value={formData.stopBits}
                      onChange={(e, { value }) =>
                        handleFormChange("stopBits", value)
                      }
                      min={1}
                      max={2}
                    />
                    <Select
                      id="parity"
                      labelText="Parity *"
                      value={formData.parity}
                      onChange={(e) =>
                        handleFormChange("parity", e.target.value)
                      }
                    >
                      {PARITY_OPTIONS.map((opt) => (
                        <SelectItem
                          key={opt.value}
                          value={opt.value}
                          text={opt.label}
                        />
                      ))}
                    </Select>

                    <div
                      style={{
                        backgroundColor: "#f4f4f4",
                        padding: "1rem",
                        borderRadius: "4px",
                      }}
                    >
                      <FormLabel
                        style={{
                          marginBottom: "0.75rem",
                          fontSize: "0.8125rem",
                          fontWeight: "500",
                        }}
                      >
                        {intl.formatMessage({ id: "coldStorage.device.rs485" })}
                      </FormLabel>
                      <Stack gap={4}>
                        <Checkbox
                          id="rs485Mode"
                          labelText={intl.formatMessage({
                            id: "coldStorage.device.rs485.mode",
                          })}
                          checked={Boolean(formData.rs485Mode)}
                          onChange={(e, { checked }) =>
                            handleFormChange("rs485Mode", checked)
                          }
                        />
                        {formData.rs485Mode && (
                          <>
                            <Checkbox
                              id="rs485RtsActiveHigh"
                              labelText={intl.formatMessage({
                                id: "coldStorage.device.rs485.rtsActiveHigh",
                              })}
                              checked={Boolean(formData.rs485RtsActiveHigh)}
                              onChange={(e, { checked }) =>
                                handleFormChange("rs485RtsActiveHigh", checked)
                              }
                            />
                            <Checkbox
                              id="rs485Termination"
                              labelText={intl.formatMessage({
                                id: "coldStorage.device.rs485.termination",
                              })}
                              checked={Boolean(formData.rs485Termination)}
                              onChange={(e, { checked }) =>
                                handleFormChange("rs485Termination", checked)
                              }
                            />
                            <Checkbox
                              id="rs485RxDuringTx"
                              labelText={intl.formatMessage({
                                id: "coldStorage.device.rs485.rxDuringTx",
                              })}
                              checked={Boolean(formData.rs485RxDuringTx)}
                              onChange={(e, { checked }) =>
                                handleFormChange("rs485RxDuringTx", checked)
                              }
                            />
                            <NumberInput
                              id="rs485DelayBeforeMs"
                              label={intl.formatMessage({
                                id: "coldStorage.device.rs485.delayBefore",
                              })}
                              value={formData.rs485DelayBeforeMs}
                              onChange={(e, { value }) =>
                                handleFormChange("rs485DelayBeforeMs", value)
                              }
                              min={0}
                              max={10000}
                              step={1}
                            />
                            <NumberInput
                              id="rs485DelayAfterMs"
                              label={intl.formatMessage({
                                id: "coldStorage.device.rs485.delayAfter",
                              })}
                              value={formData.rs485DelayAfterMs}
                              onChange={(e, { value }) =>
                                handleFormChange("rs485DelayAfterMs", value)
                              }
                              min={0}
                              max={10000}
                              step={1}
                            />
                          </>
                        )}
                      </Stack>
                    </div>
                  </>
                )}
              </Stack>
            </div>

            <div
              style={{
                borderTop: "1px solid #e0e0e0",
                paddingTop: "1rem",
                marginTop: "0.5rem",
              }}
            >
              <FormLabel
                style={{
                  marginBottom: "0.5rem",
                  fontSize: "0.875rem",
                  fontWeight: "600",
                  color: "#161616",
                }}
              >
                Modbus Configuration
              </FormLabel>
              <p
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                Configure register mappings and data scaling for sensor readings
              </p>
              <Stack gap={5}>
                <NumberInput
                  id="slaveId"
                  label="Slave ID *"
                  value={formData.slaveId}
                  onChange={(e, { value }) =>
                    handleFormChange("slaveId", value)
                  }
                  min={1}
                  max={255}
                />

                <NumberInput
                  id="registerCount"
                  label={intl.formatMessage({
                    id: "coldStorage.device.registerCount",
                  })}
                  helperText={intl.formatMessage({
                    id: "coldStorage.device.registerCount.help",
                  })}
                  value={formData.registerCount}
                  onChange={(e, { value }) =>
                    handleFormChange("registerCount", value)
                  }
                  min={1}
                  max={2}
                  step={1}
                />

                <Select
                  id="wordOrder"
                  labelText={intl.formatMessage({
                    id: "coldStorage.device.wordOrder",
                  })}
                  helperText={intl.formatMessage({
                    id: "coldStorage.device.wordOrder.help",
                  })}
                  value={formData.wordOrder}
                  onChange={(e) =>
                    handleFormChange("wordOrder", e.target.value)
                  }
                >
                  <SelectItem
                    value="BIG_ENDIAN"
                    text={intl.formatMessage({
                      id: "coldStorage.device.wordOrder.bigEndian",
                    })}
                  />
                  <SelectItem
                    value="LITTLE_ENDIAN"
                    text={intl.formatMessage({
                      id: "coldStorage.device.wordOrder.littleEndian",
                    })}
                  />
                </Select>

                <div
                  style={{
                    backgroundColor: "#f4f4f4",
                    padding: "1rem",
                    borderRadius: "4px",
                  }}
                >
                  <FormLabel
                    style={{
                      marginBottom: "0.75rem",
                      fontSize: "0.8125rem",
                      fontWeight: "500",
                    }}
                  >
                    Temperature Configuration
                  </FormLabel>
                  <Stack gap={4}>
                    <NumberInput
                      id="temperatureRegister"
                      label="Temperature Register *"
                      value={formData.temperatureRegister}
                      onChange={(e, { value }) =>
                        handleFormChange("temperatureRegister", value)
                      }
                      min={0}
                      max={65535}
                    />

                    <NumberInput
                      id="temperatureScale"
                      label="Temperature Scale"
                      helperText="Scaling factor (e.g., 0.1 to divide by 10)"
                      value={formData.temperatureScale}
                      onChange={(e, { value }) =>
                        handleFormChange("temperatureScale", value)
                      }
                      step={0.1}
                      min={0.01}
                    />

                    <NumberInput
                      id="temperatureOffset"
                      label="Base Temperature (°C)"
                      helperText="Base offset (e.g., -80 for ultra-low freezers)"
                      value={formData.temperatureOffset}
                      onChange={(e, { value }) =>
                        handleFormChange("temperatureOffset", value)
                      }
                      step={0.1}
                    />
                  </Stack>
                </div>

                <div
                  style={{
                    backgroundColor: "#f4f4f4",
                    padding: "1rem",
                    borderRadius: "4px",
                  }}
                >
                  <FormLabel
                    style={{
                      marginBottom: "0.75rem",
                      fontSize: "0.8125rem",
                      fontWeight: "500",
                    }}
                  >
                    Humidity Configuration (Optional)
                  </FormLabel>
                  <Stack gap={4}>
                    <NumberInput
                      id="humidityRegister"
                      label="Humidity Register"
                      helperText="Modbus register address for humidity reading"
                      value={formData.humidityRegister ?? ""}
                      onChange={(e, { value }) =>
                        handleFormChange(
                          "humidityRegister",
                          value === "" ? null : value,
                        )
                      }
                      // Without allowEmpty, Carbon reports an emptied optional register as Number("") === 0.
                      allowEmpty
                      min={0}
                      max={65535}
                      step={1}
                    />

                    <NumberInput
                      id="humidityScale"
                      label="Humidity Scale"
                      helperText="Scaling factor (e.g., 0.1 to divide by 10)"
                      value={formData.humidityScale}
                      onChange={(e, { value }) =>
                        handleFormChange("humidityScale", value)
                      }
                      step={0.1}
                      min={0.01}
                    />

                    <NumberInput
                      id="humidityOffset"
                      label="Humidity Offset (%)"
                      helperText="Base offset for humidity readings"
                      value={formData.humidityOffset}
                      onChange={(e, { value }) =>
                        handleFormChange("humidityOffset", value)
                      }
                      step={0.1}
                    />
                  </Stack>
                </div>

                <div
                  style={{
                    backgroundColor: "#f4f4f4",
                    padding: "1rem",
                    borderRadius: "4px",
                  }}
                >
                  <FormLabel
                    style={{
                      marginBottom: "0.75rem",
                      fontSize: "0.8125rem",
                      fontWeight: "500",
                    }}
                  >
                    {intl.formatMessage({
                      id: "coldStorage.device.secondProbe",
                    })}
                  </FormLabel>
                  <Stack gap={4}>
                    <NumberInput
                      id="temperatureRegister2"
                      label={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.register",
                      })}
                      helperText={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.registerHelp",
                      })}
                      value={formData.temperatureRegister2 ?? ""}
                      onChange={(e, { value }) =>
                        handleFormChange(
                          "temperatureRegister2",
                          value === "" ? null : value,
                        )
                      }
                      allowEmpty
                      min={0}
                      max={65535}
                      step={1}
                    />

                    <NumberInput
                      id="temperatureScale2"
                      label={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.scale",
                      })}
                      helperText={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.scaleHelp",
                      })}
                      value={formData.temperatureScale2}
                      onChange={(e, { value }) =>
                        handleFormChange("temperatureScale2", value)
                      }
                      step={0.1}
                      min={0.01}
                    />

                    <NumberInput
                      id="temperatureOffset2"
                      label={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.offset",
                      })}
                      helperText={intl.formatMessage({
                        id: "coldStorage.device.secondProbe.offsetHelp",
                      })}
                      value={formData.temperatureOffset2}
                      onChange={(e, { value }) =>
                        handleFormChange("temperatureOffset2", value)
                      }
                      step={0.1}
                    />
                  </Stack>
                </div>
              </Stack>
            </div>
          </Stack>
        </Form>
      </Modal>
    </>
  );
}
