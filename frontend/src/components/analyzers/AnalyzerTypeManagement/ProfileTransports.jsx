import React from "react";
import { Checkbox } from "@carbon/react";
import { useIntl } from "react-intl";
import { profileAuthoringMessage } from "./profileAuthoringMessages";

// Field names and enums come from the Bridge v1 contract; no instrument values are supplied.
const SERIAL_NUMBERS = [
  ["default_baud_rate", "baudRate"],
  ["data_bits", "dataBits"],
  ["stop_bits", "stopBits"],
  ["read_timeout_ms", "readTimeout"],
  ["message_timeout_ms", "messageTimeout"],
  ["reconnect_interval_ms", "reconnectInterval"],
  ["max_reconnect_attempts", "maxReconnectAttempts"],
];
const ProfileTransports = ({ profile, onChange, input, select, boolean }) => {
  const intl = useIntl();
  const text = (key, values) => profileAuthoringMessage(intl, key, values);
  const transports = profile.transport || [];
  return (
    <fieldset aria-label={text("supportedTransports")}>
      <legend>{text("supportedTransports")}</legend>
      {["TCP/IP", "MLLP", "HTTP", "RS-232"].map((transport) => (
        <Checkbox
          key={transport}
          id={`profile-support-${transport}`}
          labelText={text("supportTransport", { transport })}
          checked={transports.includes(transport)}
          onChange={(_, { checked }) => {
            const next = checked
              ? [...transports, transport]
              : transports.filter((value) => value !== transport);
            // Retain stored settings when a transport is deselected so toggling it back loses no data.
            onChange(
              next,
              checked
                ? {
                    ...profile.transport_config,
                    [transport]: profile.transport_config?.[transport] || {},
                  }
                : profile.transport_config,
            );
          }}
        />
      ))}
      {transports
        .filter((transport) => transport === "TCP/IP" || transport === "MLLP")
        .map((transport) => (
          <fieldset
            key={transport}
            aria-label={text("transportSettings", { transport })}
          >
            <legend>{text("transportSettings", { transport })}</legend>
            <p>{text("transportPortHelp")}</p>
            {input(
              ["transport_config", transport, "default_port"],
              "port",
              "number",
            )}
          </fieldset>
        ))}
      {transports.includes("RS-232") && (
        <fieldset aria-label={text("serialSettings")}>
          <legend>{text("serialSettings")}</legend>
          {SERIAL_NUMBERS.map(([key, label]) =>
            input(["transport_config", "RS-232", key], label, "number"),
          )}
          {select(["transport_config", "RS-232", "parity"], "parity", [
            "NONE",
            "ODD",
            "EVEN",
            "MARK",
            "SPACE",
          ])}
          {select(
            ["transport_config", "RS-232", "flow_control"],
            "flowControl",
            ["NONE", "RTS_CTS", "XON_XOFF"],
          )}
          {boolean(["transport_config", "RS-232", "rts_enabled"], "rtsEnabled")}
          {boolean(["transport_config", "RS-232", "dtr_enabled"], "dtrEnabled")}
        </fieldset>
      )}
    </fieldset>
  );
};
export default ProfileTransports;
