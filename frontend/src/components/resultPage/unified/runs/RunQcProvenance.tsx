import React from "react";
import { Link as CarbonLink, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import useSiteDateFormat from "../../../common/useSiteDateFormat";
import { describeControl, runVerdictTagType } from "./runFormat";
import { RunControlLine } from "./RunQcBlock";

export interface RunQcFact {
  runId: number;
  runLabel: string;
  verdict: string;
  decision?: string | null;
  reason?: string | null;
  nceNumber?: string | null;
  controls: RunControlLine[];
}

interface RunQcProvenanceProps {
  runQc: RunQcFact;
}

/**
 * OGC-1200 FR-G3: read-only, what the run a result came from recorded about
 * its test's quality control, for the review of that result: the run, its
 * verdict for the test, what was decided when the control failed, each control
 * covering the test and the reagent lot. It is edited on the run, never here.
 */
const RunQcProvenance: React.FC<RunQcProvenanceProps> = ({ runQc }) => {
  const intl = useIntl();
  const { pattern } = useSiteDateFormat();
  return (
    <div className="runQcProvenance" data-testid="run-qc-provenance">
      <div>
        <FormattedMessage
          id="run.qc.resultFrom"
          values={{
            run: (
              <CarbonLink
                as={RouterLink}
                to={`/Results?run=${runQc.runId}`}
                data-testid="run-qc-provenance-run"
              >
                {runQc.runLabel}
              </CarbonLink>
            ),
          }}
        />{" "}
        <Tag
          type={runVerdictTagType(runQc.verdict)}
          size="sm"
          data-testid="run-qc-provenance-verdict"
        >
          {intl.formatMessage({ id: `run.verdict.${runQc.verdict}` })}
        </Tag>
      </div>
      {runQc.decision && (
        <p data-testid="run-qc-provenance-decision">
          {intl.formatMessage({ id: `run.decision.${runQc.decision}` })}
          {runQc.reason && (
            <>
              {" · "}
              <FormattedMessage id="run.decision.reason" />: {runQc.reason}
            </>
          )}
          {runQc.nceNumber && (
            <>
              {" · "}
              <FormattedMessage
                id="run.decision.nce"
                values={{ number: runQc.nceNumber }}
              />
            </>
          )}
        </p>
      )}
      {runQc.controls.length > 0 && (
        <ul className="runQcControls">
          {runQc.controls.map((control) => (
            <li
              key={control.id}
              data-testid={`run-qc-provenance-control-${control.id}`}
            >
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
      )}
      <p className="unifiedFieldHint">
        <FormattedMessage id="run.entry.noLot" />
      </p>
    </div>
  );
};

export default RunQcProvenance;
