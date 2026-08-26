import React from "react";
import {
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const AstAttemptTable = ({
  runs,
  selectedRunId,
  disabled,
  onView,
  onSelectReportable,
}) => {
  const intl = useIntl();
  const attemptNumberById = new Map(
    runs.map((run, index) => [run.id, index + 1]),
  );
  const reviewedCount = runs.filter((run) => run.status === "REVIEWED").length;
  const title = intl.formatMessage({ id: "microbiology.ast.attempts" });

  const attemptLabel = (run) =>
    intl.formatMessage(
      { id: "microbiology.ast.attemptNumber" },
      { number: attemptNumberById.get(run.id) },
    );

  return (
    <TableContainer
      title={title}
      className="microbiology-ast-attempts"
      tabIndex={0}
      aria-label={title}
    >
      <Table size="sm" aria-label={title}>
        <TableHead>
          <TableRow>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.attempt" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.sourceAttempt" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.method" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.runStatus" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.attemptReason" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.reporting" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.ast.actions" })}
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {runs.map((run) => {
            const number = attemptNumberById.get(run.id);
            const isSelected = run.id === selectedRunId;
            return (
              <TableRow key={run.id} data-testid="microbiology-ast-attempt-row">
                <TableCell>
                  <strong>{attemptLabel(run)}</strong>
                  <Tag type={run.attemptType === "ORIGINAL" ? "gray" : "cyan"}>
                    {formatMicrobiologyEnum(
                      run.attemptType || "ORIGINAL",
                      intl,
                    )}
                  </Tag>
                </TableCell>
                <TableCell>
                  {run.sourceRunId && attemptNumberById.has(run.sourceRunId)
                    ? intl.formatMessage(
                        { id: "microbiology.ast.attemptNumber" },
                        { number: attemptNumberById.get(run.sourceRunId) },
                      )
                    : intl.formatMessage({
                        id: "microbiology.ast.noSourceAttempt",
                      })}
                </TableCell>
                <TableCell>
                  {run.technique
                    ? formatMicrobiologyEnum(run.technique, intl)
                    : run.method || "-"}
                  {(run.measurementType || run.method) && (
                    <div className="microbiology-card__hint">
                      {run.measurementType || run.method}
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <Tag type={run.status === "REVIEWED" ? "green" : "cyan"}>
                    {formatMicrobiologyEnum(run.status, intl)}
                  </Tag>
                </TableCell>
                <TableCell>
                  {run.attemptReason ||
                    intl.formatMessage({
                      id: "microbiology.ast.initialAttemptReason",
                    })}
                </TableCell>
                <TableCell>
                  <Tag type={run.reportable ? "green" : "gray"}>
                    {intl.formatMessage({
                      id: run.reportable
                        ? "microbiology.ast.includedInReport"
                        : "microbiology.ast.notIncludedInReport",
                    })}
                  </Tag>
                </TableCell>
                <TableCell>
                  <div className="microbiology-ast-attempts__actions">
                    <Button
                      kind={isSelected ? "secondary" : "ghost"}
                      size="sm"
                      disabled={isSelected}
                      onClick={() => onView(run.id)}
                      aria-label={intl.formatMessage(
                        {
                          id: isSelected
                            ? "microbiology.ast.viewingAttempt"
                            : "microbiology.ast.viewAttempt",
                        },
                        { number },
                      )}
                    >
                      {intl.formatMessage({
                        id: isSelected
                          ? "microbiology.ast.viewing"
                          : "microbiology.ast.view",
                      })}
                    </Button>
                    {reviewedCount > 1 &&
                    run.status === "REVIEWED" &&
                    !run.reportable ? (
                      <Button
                        kind="ghost"
                        size="sm"
                        disabled={disabled}
                        onClick={() => onSelectReportable(run.id)}
                        aria-label={intl.formatMessage(
                          { id: "microbiology.ast.useAttemptForReporting" },
                          { number },
                        )}
                      >
                        {intl.formatMessage({
                          id: "microbiology.ast.useForReporting",
                        })}
                      </Button>
                    ) : null}
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default AstAttemptTable;
