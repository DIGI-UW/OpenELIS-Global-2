import React, { useEffect, useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
} from "../../utils/Utils";
import { BATCH_ENDPOINT, formatDateTime } from "./batchWorkplanUtils";
import "../wpStyle.css";

/**
 * The printable workplan of a released batch (OGC-427 FR-WG-002): what it runs
 * with, the QC it was released on and the NCE an override raised, then the
 * tests to run.
 */
export default function BatchWorkplanPrint() {
  const intl = useIntl();
  const { id } = useParams();
  const [batch, setBatch] = useState(undefined);

  useEffect(() => {
    getFromOpenElisServer(`${BATCH_ENDPOINT}/batches/${id}`, (body) =>
      setBatch(body && body.id ? body : null),
    );
  }, [id]);

  if (batch === undefined) {
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "loading.description" })}
      />
    );
  }
  if (batch === null) {
    return (
      <InlineNotification
        kind="error"
        hideCloseButton
        title={intl.formatMessage({ id: "workplan.batch.print.notFound" })}
      />
    );
  }

  const latestRelease = (batch.releases || [])[0];
  const releasedAt = latestRelease?.releasedAt;
  const releaseRows = (batch.releases || []).filter(
    (release) => release.releasedAt === releasedAt,
  );
  const ruleCodes = (lotId) =>
    (batch.qc?.lots || []).find((lot) => lot.controlLotId === lotId)
      ?.ruleCodes || [];
  const nceNumber = releaseRows.find((release) => release.nceNumber)?.nceNumber;
  const released = batch.status === "ACTIVE" || batch.status === "COMPLETED";

  return (
    <div className="batch-workplan-print" data-testid="batch-workplan-print">
      <div className="batch-workplan-print__actions">
        <Button size="sm" renderIcon={Printer} onClick={() => window.print()}>
          <FormattedMessage id="workplan.batch.print" />
        </Button>
      </div>
      {!released && (
        <InlineNotification
          kind="warning"
          hideCloseButton
          lowContrast
          title={intl.formatMessage({ id: "workplan.batch.print.notReleased" })}
        />
      )}
      <h2>
        <FormattedMessage id="workplan.batch.print.title" />
        {`: ${batch.name}`}
      </h2>
      <dl className="batch-workplan-print__facts">
        <dt>
          <FormattedMessage id="workplan.batch.print.date" />
        </dt>
        <dd>{formatDateTime(intl, batch.generatedAt || releasedAt)}</dd>
        <dt>
          <FormattedMessage id="workplan.batch.reagent" />
        </dt>
        <dd>{batch.reagentName || "-"}</dd>
        <dt>
          <FormattedMessage id="workplan.batch.reagentLot" />
        </dt>
        <dd>{batch.reagentLotNumber || "-"}</dd>
        <dt>
          <FormattedMessage id="workplan.batch.instrument" />
        </dt>
        <dd>
          {batch.instrumentName ||
            intl.formatMessage({ id: "workplan.batch.instrument.bench" })}
        </dd>
        <dt>
          <FormattedMessage id="workplan.batch.print.generatedBy" />
        </dt>
        <dd>{batch.generatedBy || latestRelease?.releasedBy || "-"}</dd>
        {nceNumber && (
          <>
            <dt>
              <FormattedMessage id="workplan.batch.print.nce" />
            </dt>
            <dd>{nceNumber}</dd>
          </>
        )}
      </dl>

      <h3>
        <FormattedMessage id="workplan.batch.print.qc" />
      </h3>
      <Table size="sm">
        <TableHead>
          <TableRow>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.test" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.qc.controlLot" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.qc.statusColumn" />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {releaseRows.map((release) => (
            <TableRow key={release.controlLotId}>
              <TableCell>{release.testName}</TableCell>
              <TableCell>{release.lotNumber}</TableCell>
              <TableCell>
                <FormattedMessage
                  id={`workplan.batch.qc.status.${release.qcStatus}`}
                />
                {ruleCodes(release.controlLotId).length > 0 &&
                  ` (${ruleCodes(release.controlLotId).join(", ")})`}
                {release.overridden && (
                  <>
                    {" - "}
                    <FormattedMessage id="workplan.batch.print.overridden" />
                  </>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <h3>
        <FormattedMessage
          id="workplan.batch.testsInBatch"
          values={{ count: batch.itemCount }}
        />
      </h3>
      <Table size="sm">
        <TableHead>
          <TableRow>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.labNumber" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.patient" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.test" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.sampleType" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.priority" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="workplan.batch.print.result" />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {(batch.items || []).map((item) => (
            <TableRow key={item.analysisId}>
              <TableCell>
                {convertAlphaNumLabNumForDisplay(item.accessionNumber)}
              </TableCell>
              <TableCell>{item.patientName}</TableCell>
              <TableCell>{item.testName}</TableCell>
              <TableCell>{item.sampleType}</TableCell>
              <TableCell>
                {item.priority && (
                  <FormattedMessage id={`sample.priority.${item.priority}`} />
                )}
              </TableCell>
              <TableCell className="batch-workplan-print__blank" />
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
