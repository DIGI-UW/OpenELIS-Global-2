import React, { useContext, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { format } from "date-fns";
import CustomDatePicker from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import { labNow } from "../../utils/labClock";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import {
  displayDateToIso,
  isoToDisplayDate,
  toLocalDateTime,
} from "./batchWorkplanUtils";

/**
 * Inline QC entry on a batch tile (OGC-427 FR-QCE-001..003). It records a
 * bench control run through the existing bench QC capture endpoint, so the
 * run lands in the same QC history, Levey-Jennings chart and violation path
 * as any other bench run; the workplan evaluates nothing itself.
 */
export default function QcEntryForm({ batchId, lots, onSaved, onCancel }) {
  const intl = useIntl();
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE;

  const [lotId, setLotId] = useState(lots[0]?.controlLotId || "");
  const [date, setDate] = useState(() =>
    isoToDisplayDate(labNow(), dateLocale),
  );
  const [time, setTime] = useState(() => format(labNow(), "HH:mm"));
  const [value, setValue] = useState("");
  const [outcome, setOutcome] = useState("");
  const [notes, setNotes] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const lot = lots.find((candidate) => candidate.controlLotId === lotId);
  const numeric = value.trim() !== "" && Number.isFinite(Number(value));
  const runDateTime = toLocalDateTime(date, time, dateLocale);
  const dateValid = Boolean(displayDateToIso(date, dateLocale));
  const future = runDateTime && new Date(runDateTime) > labNow();
  const valid = lot && numeric && outcome && runDateTime && !future;

  const save = () => {
    setSubmitted(true);
    if (!valid) return;
    setBusy(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      "/rest/qc/results",
      JSON.stringify({
        source: "MANUAL",
        qualitativeOutcome: outcome,
        resultValue: value.trim(),
        controlLotId: lot.controlLotId,
        testId: lot.testId,
        testSectionId: lot.testSectionId,
        runDateTime,
        notes: notes.trim() || null,
      }),
      (response) => {
        setBusy(false);
        // Only a body carrying the saved run's id is a save; a transport
        // failure also reaches this callback, with a falsy status.
        if (!response || !response.id) {
          setError(
            response?.message ||
              intl.formatMessage({ id: "workplan.batch.qc.entry.failed" }),
          );
          return;
        }
        onSaved(response);
      },
    );
  };

  const prefix = `batch-${batchId}-qc`;
  return (
    <div className="batch-workplan__qc-entry" data-testid={`${prefix}-form`}>
      <h5>
        <FormattedMessage id="workplan.batch.qc.entry.title" />
      </h5>
      {lots.length > 1 && (
        <Select
          id={`${prefix}-lot`}
          size="sm"
          labelText={intl.formatMessage({ id: "workplan.batch.qc.controlLot" })}
          value={lotId}
          onChange={(event) => setLotId(event.target.value)}
        >
          {lots.map((candidate) => (
            <SelectItem
              key={candidate.controlLotId}
              value={candidate.controlLotId}
              text={`${candidate.testName} - ${candidate.lotNumber} (${candidate.controlLevel})`}
            />
          ))}
        </Select>
      )}
      {lots.length === 1 && lot && (
        <p className="batch-workplan__muted">
          <FormattedMessage
            id="workplan.batch.qc.entry.lot"
            values={{
              test: lot.testName,
              lot: lot.lotNumber,
              level: lot.controlLevel,
            }}
          />
        </p>
      )}
      <div className="batch-workplan__qc-entry-row">
        <CustomDatePicker
          id={`${prefix}-date`}
          labelText={intl.formatMessage({ id: "workplan.batch.qc.date" })}
          value={date}
          disallowFutureDate
          invalid={submitted && (!dateValid || future)}
          invalidText={intl.formatMessage({
            id: future
              ? "workplan.batch.qc.entry.future"
              : "workplan.batch.qc.entry.dateRequired",
          })}
          onChange={setDate}
        />
        <TextInput
          id={`${prefix}-time`}
          size="sm"
          labelText={intl.formatMessage({ id: "workplan.batch.qc.time" })}
          placeholder="HH:mm"
          value={time}
          invalid={submitted && !/^([01]\d|2[0-3]):[0-5]\d$/.test(time)}
          invalidText={intl.formatMessage({
            id: "workplan.batch.qc.entry.timeInvalid",
          })}
          onChange={(event) => setTime(event.target.value)}
        />
        <TextInput
          id={`${prefix}-value`}
          size="sm"
          labelText={intl.formatMessage({ id: "workplan.batch.qc.value" })}
          placeholder={intl.formatMessage({
            id: "workplan.batch.qc.value.placeholder",
          })}
          value={value}
          invalid={submitted && !numeric}
          invalidText={intl.formatMessage({
            id: "workplan.batch.qc.entry.valueRequired",
          })}
          onChange={(event) => setValue(event.target.value)}
        />
      </div>
      <RadioButtonGroup
        legendText={intl.formatMessage({ id: "workplan.batch.qc.result" })}
        name={`${prefix}-outcome`}
        valueSelected={outcome}
        onChange={(selected) => setOutcome(selected)}
        invalid={submitted && !outcome}
        invalidText={intl.formatMessage({
          id: "workplan.batch.qc.entry.resultRequired",
        })}
      >
        <RadioButton
          id={`${prefix}-pass`}
          value="PASS"
          labelText={intl.formatMessage({ id: "workplan.batch.qc.pass" })}
        />
        <RadioButton
          id={`${prefix}-fail`}
          value="FAIL"
          labelText={intl.formatMessage({ id: "workplan.batch.qc.fail" })}
        />
      </RadioButtonGroup>
      <TextArea
        id={`${prefix}-notes`}
        rows={2}
        labelText={intl.formatMessage({ id: "workplan.batch.qc.notes" })}
        value={notes}
        onChange={(event) => setNotes(event.target.value)}
      />
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "notification.title" })}
          subtitle={error}
        />
      )}
      <div className="batch-workplan__row-actions">
        <Button size="sm" onClick={save} disabled={busy}>
          <FormattedMessage id="workplan.batch.qc.entry.save" />
        </Button>
        <Button size="sm" kind="ghost" onClick={onCancel} disabled={busy}>
          <FormattedMessage id="label.button.cancel" />
        </Button>
      </div>
    </div>
  );
}
