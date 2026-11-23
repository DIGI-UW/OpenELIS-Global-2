import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextInput,
  Tile,
} from "@carbon/react";
import { WarningFilled } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import useSiteDateFormat from "../../../common/useSiteDateFormat";
import { formatRunTime } from "./runFormat";
import RunTestDecision from "./RunTestDecision";
import RunRetestLink from "./RunRetestLink";

export interface RunControlLine {
  id: string;
  level?: string | null;
  lotNumber?: string | null;
  observed?: string | null;
  expected?: string | null;
  verdict?: string | null;
  verdictSource?: string | null;
  operator?: string | null;
  recordedAt?: string | null;
}

export interface RunLinkItem {
  runId: number;
  label: string;
  count: number;
  total: number;
}

export interface RunQcTestLine {
  testId: string;
  testName: string;
  verdict: string;
  controls: RunControlLine[];
  decision?: string | null;
  nceNumber?: string | null;
  retestOf?: RunLinkItem[];
  retestSuggestions?: RunLinkItem[];
  supersededBy?: RunLinkItem[];
}

export interface RunQc {
  overall: string;
  tests: RunQcTestLine[];
}

interface ControlLot {
  id?: string;
  lotNumber?: string;
  controlLevel?: string;
}

interface RunQcBlockProps {
  runId: number;
  qc: RunQc;
  onRecorded: (qc: RunQc) => void;
  onOpenRun?: (runId: number) => void;
}

type Kind = "measured" | "controlLine";

interface Draft {
  testId: string;
  kind: Kind;
  lotId: string;
  measured: string;
  expected: string;
  uncertainty: string;
  kitLot: string;
  outcome: string;
}

const emptyDraft = (testId: string): Draft => ({
  testId,
  kind: "measured",
  lotId: "",
  measured: "",
  expected: "",
  uncertainty: "",
  kitLot: "",
  outcome: "",
});

const OUTCOME_LABELS: Record<string, string> = {
  PASS: "label.qc.pass",
  FAIL: "label.qc.fail",
  VALID: "label.results.control.capture.valid",
  INVALID: "label.results.control.capture.invalid",
};

const verdictTag = (verdict?: string | null) => {
  switch (verdict) {
    case "FAIL":
    case "HELD":
      return "red";
    case "DECIDED":
      return "purple";
    case "PASS":
      return "green";
    default:
      return "gray";
  }
};

/**
 * OGC-1200 FR-E2: the run header's QC block. The run's overall verdict, one
 * line per test it covers with that test's verdict and each control covering
 * it, and Record control, which adds a control to the run. A failed control
 * holds that test's rows, and only those (FR-C4).
 */
const RunQcBlock: React.FC<RunQcBlockProps> = ({
  runId,
  qc,
  onRecorded,
  onOpenRun,
}) => {
  const intl = useIntl();
  const { pattern } = useSiteDateFormat();
  const recordable = qc.tests.filter((test) => !test.decision);
  const [recording, setRecording] = useState(false);
  const [draft, setDraft] = useState<Draft>(
    emptyDraft(recordable[0]?.testId ?? ""),
  );
  const [lots, setLots] = useState<ControlLot[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{
    kind: "success" | "error";
    text: string;
  } | null>(null);

  useEffect(() => {
    if (!recording || !draft.testId || draft.kind !== "measured") {
      return;
    }
    getFromOpenElisServer(
      `/rest/qc/results/control-lots?testId=${draft.testId}`,
      (body?: ControlLot[]) => setLots(Array.isArray(body) ? body : []),
    );
  }, [recording, draft.testId, draft.kind]);

  const update = (change: Partial<Draft>) =>
    setDraft((current) => ({ ...current, ...change }));

  const complete =
    draft.testId &&
    draft.outcome &&
    (draft.kind === "measured"
      ? draft.lotId && draft.measured.trim()
      : draft.kitLot.trim());

  const record = () => {
    if (!complete) {
      setMessage({
        kind: "error",
        text: intl.formatMessage({
          id: "label.results.control.capture.incomplete",
        }),
      });
      return;
    }
    const body =
      draft.kind === "measured"
        ? {
            source: "MANUAL",
            testId: draft.testId,
            controlLotId: draft.lotId,
            resultValue: draft.measured.trim(),
            expectedValue: draft.expected.trim() || null,
            uncertainty: draft.uncertainty.trim() || null,
            qualitativeOutcome: draft.outcome,
          }
        : {
            source: "RDT",
            testId: draft.testId,
            controlLabel: draft.kitLot.trim(),
            qualitativeOutcome: draft.outcome,
          };
    setBusy(true);
    setMessage(null);
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/controls`,
      JSON.stringify(body),
      (response?: RunQc & { message?: string }) => {
        setBusy(false);
        if (!response || !Array.isArray(response.tests)) {
          setMessage({
            kind: "error",
            text:
              response?.message ||
              intl.formatMessage({
                id: "label.results.control.capture.failed",
              }),
          });
          return;
        }
        setMessage({
          kind: "success",
          text: intl.formatMessage({ id: "run.qc.recorded" }),
        });
        setRecording(false);
        setDraft(emptyDraft(draft.testId));
        onRecorded(response);
      },
    );
  };

  const observedText = (observed?: string | null) =>
    observed && OUTCOME_LABELS[observed]
      ? intl.formatMessage({ id: OUTCOME_LABELS[observed] })
      : observed;

  const controlText = (control: RunControlLine) =>
    [
      control.level,
      control.lotNumber,
      observedText(control.observed) &&
        (control.expected
          ? intl.formatMessage(
              { id: "run.qc.observedAgainst" },
              {
                observed: observedText(control.observed),
                expected: control.expected,
              },
            )
          : observedText(control.observed)),
      control.verdictSource &&
        intl.formatMessage({ id: `run.qc.source.${control.verdictSource}` }),
      control.operator,
      control.recordedAt && formatRunTime(intl, control.recordedAt, pattern),
    ]
      .filter(Boolean)
      .join(" · ");

  const outcomes =
    draft.kind === "measured"
      ? [
          { value: "PASS", id: "label.qc.pass" },
          { value: "FAIL", id: "label.qc.fail" },
        ]
      : [
          { value: "VALID", id: "label.results.control.capture.valid" },
          { value: "INVALID", id: "label.results.control.capture.invalid" },
        ];

  return (
    <Tile className="runHeaderTile" data-testid="run-qc">
      <h4>
        <FormattedMessage id="run.qc.title" />{" "}
        <Tag
          type={verdictTag(qc.overall)}
          size="sm"
          renderIcon={qc.overall === "HELD" ? WarningFilled : undefined}
          data-testid="run-qc-overall"
        >
          {intl.formatMessage({ id: `run.qc.overall.${qc.overall}` })}
        </Tag>
      </h4>
      {qc.tests.length === 0 && (
        <p>
          <FormattedMessage id="run.qc.noTests" />
        </p>
      )}
      <ul className="runQcTests">
        {qc.tests.map((test) => (
          <li key={test.testId} data-testid={`run-qc-test-${test.testId}`}>
            <span className="runQcTestName">{test.testName}</span>{" "}
            <Tag type={verdictTag(test.verdict)} size="sm">
              {intl.formatMessage({ id: `run.verdict.${test.verdict}` })}
            </Tag>
            <RunTestDecision runId={runId} test={test} onDecided={onRecorded} />
            <RunRetestLink
              runId={runId}
              test={test}
              onLinked={onRecorded}
              onOpenRun={onOpenRun}
            />
            {test.controls.length > 0 && (
              <ul className="runQcControls">
                {test.controls.map((control) => (
                  <li
                    key={control.id}
                    data-testid={`run-qc-control-${control.id}`}
                  >
                    <Tag type={verdictTag(control.verdict)} size="sm">
                      {intl.formatMessage({
                        id: control.verdict
                          ? `run.verdict.${control.verdict}`
                          : "run.qc.notEvaluated",
                      })}
                    </Tag>{" "}
                    {controlText(control)}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
      {message && (
        <InlineNotification
          kind={message.kind}
          lowContrast
          hideCloseButton
          title={message.text}
        />
      )}
      {!recording ? (
        <Button
          kind="tertiary"
          size="sm"
          disabled={recordable.length === 0}
          onClick={() => {
            setMessage(null);
            if (!recordable.some((test) => test.testId === draft.testId)) {
              setDraft(emptyDraft(recordable[0]?.testId ?? ""));
            }
            setRecording(true);
          }}
          data-testid="run-qc-record"
        >
          <FormattedMessage id="run.qc.recordControl" />
        </Button>
      ) : (
        <div className="runQcForm" data-testid="run-qc-form">
          <Select
            id={`run-${runId}-qc-test`}
            labelText={intl.formatMessage({ id: "run.qc.test" })}
            value={draft.testId}
            onChange={(event) =>
              update({ testId: event.target.value, lotId: "" })
            }
          >
            {recordable.map((test) => (
              <SelectItem
                key={test.testId}
                value={test.testId}
                text={test.testName}
              />
            ))}
          </Select>
          <RadioButtonGroup
            legendText={intl.formatMessage({ id: "run.qc.kind" })}
            name={`run-${runId}-qc-kind`}
            valueSelected={draft.kind}
            onChange={(value) =>
              update({ kind: value as Kind, outcome: "", lotId: "" })
            }
          >
            <RadioButton
              id={`run-${runId}-qc-kind-measured`}
              value="measured"
              labelText={intl.formatMessage({ id: "run.qc.kind.measured" })}
            />
            <RadioButton
              id={`run-${runId}-qc-kind-line`}
              value="controlLine"
              labelText={intl.formatMessage({
                id: "label.results.control.capture.controlLine",
              })}
            />
          </RadioButtonGroup>
          {draft.kind === "measured" ? (
            <>
              <Select
                id={`run-${runId}-qc-lot`}
                labelText={intl.formatMessage({
                  id: "label.results.control.capture.lot",
                })}
                value={draft.lotId}
                onChange={(event) => update({ lotId: event.target.value })}
              >
                <SelectItem value="" text="" />
                {lots.map((lot) => (
                  <SelectItem
                    key={lot.id}
                    value={lot.id ?? ""}
                    text={[lot.controlLevel, lot.lotNumber]
                      .filter(Boolean)
                      .join(" · ")}
                  />
                ))}
              </Select>
              {lots.length === 0 && (
                <p className="runQcHint">
                  <FormattedMessage id="label.results.control.capture.noBenchLots" />
                </p>
              )}
              <TextInput
                id={`run-${runId}-qc-measured`}
                labelText={intl.formatMessage({
                  id: "label.results.control.capture.measured",
                })}
                value={draft.measured}
                onChange={(event) => update({ measured: event.target.value })}
              />
              <TextInput
                id={`run-${runId}-qc-expected`}
                labelText={intl.formatMessage({
                  id: "label.results.control.capture.expected",
                })}
                value={draft.expected}
                onChange={(event) => update({ expected: event.target.value })}
              />
              <TextInput
                id={`run-${runId}-qc-uncertainty`}
                labelText={intl.formatMessage({
                  id: "label.results.control.capture.uncertainty",
                })}
                value={draft.uncertainty}
                onChange={(event) =>
                  update({ uncertainty: event.target.value })
                }
              />
            </>
          ) : (
            <TextInput
              id={`run-${runId}-qc-kit`}
              labelText={intl.formatMessage({
                id: "label.results.control.capture.kitLot",
              })}
              value={draft.kitLot}
              onChange={(event) => update({ kitLot: event.target.value })}
            />
          )}
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "label.results.control.capture.outcome",
            })}
            name={`run-${runId}-qc-outcome`}
            valueSelected={draft.outcome}
            onChange={(value) => update({ outcome: String(value) })}
          >
            {outcomes.map((outcome) => (
              <RadioButton
                key={outcome.value}
                id={`run-${runId}-qc-outcome-${outcome.value}`}
                value={outcome.value}
                labelText={intl.formatMessage({ id: outcome.id })}
              />
            ))}
          </RadioButtonGroup>
          <div className="runQcFormActions">
            <Button
              size="sm"
              onClick={record}
              disabled={busy}
              data-testid="run-qc-save"
            >
              <FormattedMessage id="run.qc.recordControl" />
            </Button>
            <Button
              size="sm"
              kind="ghost"
              onClick={() => {
                setRecording(false);
                setMessage(null);
              }}
            >
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </div>
      )}
    </Tile>
  );
};

export default RunQcBlock;
