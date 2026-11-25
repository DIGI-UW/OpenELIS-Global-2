import React, { useEffect, useState } from "react";
import { InlineLoading, Link, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import useSiteDateFormat from "../../../common/useSiteDateFormat";
import {
  describeControl,
  runSourceKey,
  runStateKey,
  runVerdictTagType,
} from "./runFormat";
import { RunQc } from "./RunQcBlock";

interface RowRunHeader {
  id: number;
  label: string;
  source: string;
  state: string;
  reagentLot?: string | null;
  qc?: RunQc | null;
}

interface RowRunSummaryProps {
  runId: number;
  testId?: string;
  /** "reagents" lists the lot and controls; "history" is the one-line reference. */
  variant: "reagents" | "history";
}

const OPEN_STATES = ["OPEN", "HELD"];

/**
 * OGC-1200 FR-D6, FR-D7: a row's run, read from the run. In the row's Reagents,
 * QC and Controls section it shows the run, its reagent lot and the controls
 * covering the row's test with their verdict, read-only, with a link to the
 * run header where they are recorded for the whole run. In the History section
 * of a row whose run is complete or superseded it is the run reference: id,
 * source and the test's control verdict.
 */
const RowRunSummary: React.FC<RowRunSummaryProps> = ({
  runId,
  testId,
  variant,
}) => {
  const intl = useIntl();
  const { pattern } = useSiteDateFormat();
  const [header, setHeader] = useState<RowRunHeader | null | undefined>(
    undefined,
  );

  useEffect(() => {
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/result-runs/${runId}`,
      (body?: RowRunHeader) => {
        if (!controller.signal.aborted) {
          setHeader(body && body.id ? body : null);
        }
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [runId]);

  if (header === undefined) {
    return <InlineLoading />;
  }
  if (header === null) {
    return null;
  }
  const open = OPEN_STATES.includes(header.state);
  if (variant === "history" && open) {
    return null;
  }
  const test = header.qc?.tests.find((line) => line.testId === testId);
  const runLink = (
    <Link href={`/Results?run=${header.id}`} data-testid="row-run-link">
      {header.label}
    </Link>
  );
  const verdict = test && (
    <Tag type={runVerdictTagType(test.verdict)} size="sm">
      {intl.formatMessage({ id: `run.verdict.${test.verdict}` })}
    </Tag>
  );

  if (variant === "history") {
    return (
      <div className="runRowReference" data-testid="row-run-reference">
        <FormattedMessage
          id="run.row.recordedIn"
          values={{
            run: runLink,
            source: intl.formatMessage({ id: runSourceKey(header.source) }),
          }}
        />{" "}
        {verdict}
      </div>
    );
  }

  return (
    <div className="runRowSummary" data-testid="row-run-summary">
      <div className="runRowLine">
        <FormattedMessage
          id={open ? "run.row.inRun" : "run.row.recordedIn"}
          values={{
            run: runLink,
            source: intl.formatMessage({ id: runSourceKey(header.source) }),
          }}
        />{" "}
        <Tag size="sm" type={header.state === "HELD" ? "red" : "cyan"}>
          {intl.formatMessage({ id: runStateKey(header.state) })}
        </Tag>
      </div>
      <p data-testid="row-run-lot">
        {header.reagentLot ? (
          <FormattedMessage
            id="run.entry.lot"
            values={{ lot: header.reagentLot }}
          />
        ) : (
          <FormattedMessage id="run.entry.noLot" />
        )}
      </p>
      {test ? (
        <div data-testid="row-run-controls">
          {verdict}
          {test.controls.length > 0 ? (
            <ul className="runQcControls">
              {test.controls.map((control) => (
                <li key={control.id}>
                  <Tag type={runVerdictTagType(control.verdict)} size="sm">
                    {intl.formatMessage({
                      id: control.verdict
                        ? `run.verdict.${control.verdict}`
                        : "run.qc.notEvaluated",
                    })}
                  </Tag>{" "}
                  {describeControl(intl, control, pattern)}
                </li>
              ))}
            </ul>
          ) : (
            <p className="runsChipNote">
              <FormattedMessage id="run.row.noControlOnRun" />
            </p>
          )}
        </div>
      ) : (
        <p className="runsChipNote">
          <FormattedMessage id="run.row.noControlOnRun" />
        </p>
      )}
      {open && (
        <p className="runsChipNote">
          <FormattedMessage id="run.row.editOnRun" />
        </p>
      )}
    </div>
  );
};

export default RowRunSummary;
