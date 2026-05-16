import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  InlineLoading,
  Section,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { VectorDeconvolutionAPI } from "./VectorIdentificationService";

const ProgressBar = ({ done, total, complete }) => {
  const pct = total > 0 ? (done / total) * 100 : 0;
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "0.5rem",
        minWidth: 120,
      }}
    >
      <div
        style={{
          flex: 1,
          height: 6,
          background: "var(--cds-border-subtle-01, #e0e0e0)",
          borderRadius: 3,
        }}
      >
        <div
          style={{
            width: `${pct}%`,
            height: "100%",
            background: complete
              ? "var(--cds-support-success, #24a148)"
              : "var(--cds-link-primary, #0f62fe)",
            borderRadius: 3,
          }}
        />
      </div>
      <span
        style={{
          fontSize: "0.75rem",
          color: "var(--cds-text-secondary, #525252)",
          whiteSpace: "nowrap",
        }}
      >
        {done}/{total}
      </span>
    </div>
  );
};

const StatusTag = ({ value }) => {
  if (value === "PENDING")
    return (
      <Tag type="red">
        <FormattedMessage id="vectorDec.status.pending" />
      </Tag>
    );
  if (value === "IN_PROGRESS")
    return (
      <Tag type="blue">
        <FormattedMessage id="vectorDec.status.inProgress" />
      </Tag>
    );
  if (value === "COMPLETE")
    return (
      <Tag type="teal">
        <FormattedMessage id="vectorDec.status.complete" />
      </Tag>
    );
  return <Tag type="gray">{value || "—"}</Tag>;
};

const VectorDeconvolutionWorklist = ({ embedded = false, onView }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    VectorDeconvolutionAPI.getWorklist()
      .then((data) => setRows(Array.isArray(data) ? data : []))
      .catch((err) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorDec.error.loadWorklist" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => setLoading(false));
  }, [intl, addNotification, setNotificationVisible]);

  useEffect(() => {
    load();
  }, [load]);

  const inner = (
    <>
      {!embedded && (
        <h2 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="vectorDec.heading.worklist" />
        </h2>
      )}
      {loading ? (
        <InlineLoading
          description={intl.formatMessage({ id: "vectorId.loading" })}
        />
      ) : (
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableHeader>
                  <FormattedMessage id="vectorId.col.lotId" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="vectorId.col.site" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="vectorDec.col.positiveTest" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="vectorDec.col.children" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="vectorDec.col.results" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="vectorDec.col.status" />
                </TableHeader>
                <TableHeader />
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} style={{ textAlign: "center" }}>
                    <FormattedMessage id="vectorDec.empty" />
                  </TableCell>
                </TableRow>
              )}
              {rows.map((r) => {
                const isComplete = r.deconvolutionStatus === "COMPLETE";
                return (
                  <TableRow key={r.vectorPoolId ?? r.sampleId}>
                    <TableCell style={{ fontWeight: 600 }}>
                      {r.accessionNumber}
                    </TableCell>
                    <TableCell>{r.samplingSiteName || "—"}</TableCell>
                    <TableCell>{r.positiveTestName || "—"}</TableCell>
                    <TableCell>{r.childCount}</TableCell>
                    <TableCell>
                      <ProgressBar
                        done={r.doneCount || 0}
                        total={r.childCount || 0}
                        complete={isComplete}
                      />
                      {isComplete && r.positiveCount != null && (
                        <span
                          style={{
                            marginLeft: "0.5rem",
                            fontSize: "0.75rem",
                            color: "var(--cds-text-secondary, #525252)",
                          }}
                        >
                          <FormattedMessage
                            id="vectorDec.results.positive"
                            values={{ count: r.positiveCount }}
                          />
                        </span>
                      )}
                    </TableCell>
                    <TableCell>
                      <StatusTag value={r.deconvolutionStatus} />
                    </TableCell>
                    <TableCell>
                      {onView && (
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => onView(r.vectorPoolId)}
                        >
                          <FormattedMessage id="vectorDec.button.view" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );

  if (embedded) return inner;

  return (
    <Section>
      <Grid fullWidth style={{ padding: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          {inner}
        </Column>
      </Grid>
      <AlertDialog />
    </Section>
  );
};

export default VectorDeconvolutionWorklist;
