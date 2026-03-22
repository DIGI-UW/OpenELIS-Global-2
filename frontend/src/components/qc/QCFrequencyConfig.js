import React, { useState, useEffect } from "react";
import {
  Select,
  SelectItem,
  NumberInput,
  Button,
  Grid,
  Column,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";

const FREQUENCY_TYPES = [
  { value: "DAILY_STARTUP", labelKey: "qc.frequency.daily" },
  { value: "PER_SHIFT", labelKey: "qc.frequency.perShift" },
  { value: "EVERY_N_SAMPLES", labelKey: "qc.frequency.everySamples" },
  { value: "EVERY_N_HOURS", labelKey: "qc.frequency.everyHours" },
  { value: "MANUAL", labelKey: "qc.frequency.manual" },
];

const QCFrequencyConfig = ({ instrumentId }) => {
  const intl = useIntl();
  const [frequencyType, setFrequencyType] = useState("");
  const [frequencyValue, setFrequencyValue] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (instrumentId) {
      getFromOpenElisServer(
        `/rest/qc/instruments/${instrumentId}/frequency`,
        (data) => {
          if (data && data.frequencyType) {
            setFrequencyType(data.frequencyType);
            setFrequencyValue(data.frequencyValue || "");
          }
        },
      );
    }
  }, [instrumentId]);

  const handleSave = () => {
    putToOpenElisServer(
      `/rest/qc/instruments/${instrumentId}/frequency`,
      JSON.stringify({
        frequencyType,
        frequencyValue: frequencyValue ? Number(frequencyValue) : null,
      }),
      () => {
        setSaved(true);
        setTimeout(() => setSaved(false), 3000);
      },
    );
  };

  const showValueInput =
    frequencyType === "EVERY_N_SAMPLES" || frequencyType === "EVERY_N_HOURS";

  return (
    <div>
      <h4>{intl.formatMessage({ id: "qc.frequency.config" })}</h4>

      <Grid condensed>
        <Column lg={8} md={8} sm={4}>
          <Select
            id="frequency-type"
            labelText={intl.formatMessage({ id: "qc.frequency.type" })}
            value={frequencyType}
            onChange={(e) => setFrequencyType(e.target.value)}
          >
            <SelectItem value="" text="Select frequency type" />
            {FREQUENCY_TYPES.map((ft) => (
              <SelectItem
                key={ft.value}
                value={ft.value}
                text={intl.formatMessage({ id: ft.labelKey })}
              />
            ))}
          </Select>
        </Column>
        {showValueInput && (
          <Column lg={4} md={4} sm={4}>
            <NumberInput
              id="frequency-value"
              label={intl.formatMessage({ id: "qc.frequency.value" })}
              value={frequencyValue}
              onChange={(e) => setFrequencyValue(e.target.value)}
              min={1}
              allowEmpty
            />
          </Column>
        )}
        <Column
          lg={4}
          md={4}
          sm={4}
          style={{ display: "flex", alignItems: "flex-end" }}
        >
          <Button onClick={handleSave} disabled={!frequencyType}>
            {intl.formatMessage({ id: "qc.frequency.save" })}
          </Button>
        </Column>
      </Grid>

      {saved && (
        <InlineNotification
          kind="success"
          title="Configuration saved"
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}
    </div>
  );
};

export default QCFrequencyConfig;
