import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import type { RunQc, RunQcTestLine } from "./RunQcBlock";

type ItemKind = "BLANK" | "DUPLICATE";

interface SampleOption {
  id: string;
  value: string;
}

interface RunQcItemFormProps {
  runId: number;
  tests: RunQcTestLine[];
  onRecorded: (qc: RunQc) => void;
}

/**
 * OGC-1200 FR-C10, FR-C11: records a lab blank, or a lab duplicate of one of
 * the run's samples, for a test of the run. The run evaluates it against the
 * test's blank ceiling or RPD limit and answers with its QC block.
 */
const RunQcItemForm: React.FC<RunQcItemFormProps> = ({
  runId,
  tests,
  onRecorded,
}) => {
  const intl = useIntl();
  const [open, setOpen] = useState(false);
  const [testId, setTestId] = useState(tests[0]?.testId ?? "");
  const [kind, setKind] = useState<ItemKind>("BLANK");
  const [value, setValue] = useState("");
  const [outcome, setOutcome] = useState("");
  const [parent, setParent] = useState("");
  const [samples, setSamples] = useState<SampleOption[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{
    kind: "success" | "error";
    text: string;
  } | null>(null);

  useEffect(() => {
    if (!open || kind !== "DUPLICATE" || !testId) {
      return undefined;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/result-runs/${runId}/qc-items/samples?testId=${testId}`,
      (body?: SampleOption[]) => {
        if (!controller.signal.aborted) {
          setSamples(Array.isArray(body) ? body : []);
        }
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [open, kind, testId, runId]);

  const reset = () => {
    setValue("");
    setOutcome("");
    setParent("");
  };

  const record = () => {
    if (
      !testId ||
      (!value.trim() && !outcome.trim()) ||
      (kind === "DUPLICATE" && !parent)
    ) {
      setMessage({
        kind: "error",
        text: intl.formatMessage({ id: "run.qc.item.incomplete" }),
      });
      return;
    }
    setBusy(true);
    setMessage(null);
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/qc-items`,
      JSON.stringify({
        testId,
        kind,
        value: value.trim() || null,
        outcome: outcome.trim() || null,
        parentAnalysisId: kind === "DUPLICATE" ? parent : null,
      }),
      (response?: RunQc & { message?: string }) => {
        setBusy(false);
        if (!response || !Array.isArray(response.tests)) {
          setMessage({
            kind: "error",
            text:
              response?.message ||
              intl.formatMessage({ id: "run.qc.item.failed" }),
          });
          return;
        }
        setMessage({
          kind: "success",
          text: intl.formatMessage({ id: "run.qc.item.recorded" }),
        });
        reset();
        setOpen(false);
        onRecorded(response);
      },
    );
  };

  if (!open) {
    return (
      <>
        {message && (
          <InlineNotification
            kind={message.kind}
            lowContrast
            hideCloseButton
            title={message.text}
          />
        )}
        <Button
          kind="tertiary"
          size="sm"
          disabled={tests.length === 0}
          onClick={() => {
            setMessage(null);
            if (!tests.some((test) => test.testId === testId)) {
              setTestId(tests[0]?.testId ?? "");
            }
            setOpen(true);
          }}
          data-testid="run-qc-record-item"
        >
          <FormattedMessage id="run.qc.recordItem" />
        </Button>
      </>
    );
  }

  return (
    <div className="runQcForm" data-testid="run-qc-item-form">
      <Select
        id={`run-${runId}-qc-item-test`}
        labelText={intl.formatMessage({ id: "run.qc.test" })}
        value={testId}
        onChange={(event) => {
          setTestId(event.target.value);
          setParent("");
        }}
      >
        {tests.map((test) => (
          <SelectItem
            key={test.testId}
            value={test.testId}
            text={test.testName}
          />
        ))}
      </Select>
      <RadioButtonGroup
        legendText={intl.formatMessage({ id: "run.qc.item.kind" })}
        name={`run-${runId}-qc-item-kind`}
        valueSelected={kind}
        onChange={(selected) => {
          setKind(selected as ItemKind);
          setParent("");
        }}
      >
        {(["BLANK", "DUPLICATE"] as ItemKind[]).map((option) => (
          <RadioButton
            key={option}
            id={`run-${runId}-qc-item-kind-${option}`}
            value={option}
            labelText={intl.formatMessage({ id: `run.qc.item.${option}` })}
          />
        ))}
      </RadioButtonGroup>
      {kind === "DUPLICATE" && (
        <>
          <Select
            id={`run-${runId}-qc-item-sample`}
            labelText={intl.formatMessage({ id: "run.qc.item.sample" })}
            value={parent}
            onChange={(event) => setParent(event.target.value)}
          >
            <SelectItem value="" text="" />
            {samples.map((sample) => (
              <SelectItem
                key={sample.id}
                value={sample.id}
                text={sample.value}
              />
            ))}
          </Select>
          {samples.length === 0 && (
            <p className="runQcHint">
              <FormattedMessage id="run.qc.item.noSamples" />
            </p>
          )}
        </>
      )}
      <TextInput
        id={`run-${runId}-qc-item-value`}
        labelText={intl.formatMessage({ id: "run.qc.item.value" })}
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
      <TextInput
        id={`run-${runId}-qc-item-outcome`}
        labelText={intl.formatMessage({ id: "run.qc.item.outcome" })}
        value={outcome}
        onChange={(event) => setOutcome(event.target.value)}
      />
      {message && (
        <InlineNotification
          kind={message.kind}
          lowContrast
          hideCloseButton
          title={message.text}
        />
      )}
      <div className="runQcFormActions">
        <Button
          size="sm"
          onClick={record}
          disabled={busy}
          data-testid="run-qc-item-save"
        >
          <FormattedMessage id="run.qc.recordItem" />
        </Button>
        <Button
          size="sm"
          kind="ghost"
          onClick={() => {
            setOpen(false);
            setMessage(null);
            reset();
          }}
        >
          <FormattedMessage id="label.button.cancel" />
        </Button>
      </div>
    </div>
  );
};

export default RunQcItemForm;
