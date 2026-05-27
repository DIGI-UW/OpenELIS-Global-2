import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Column,
  Grid,
  InlineLoading,
  Modal,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import {
  VectorDeconvolutionAPI,
  VectorIdentificationAPI,
} from "./VectorIdentificationService";
import VectorLotDetail from "./VectorLotDetail";
import VectorDeconvolutionWorklist from "./VectorDeconvolutionWorklist";

const STATUS_FILTERS = [
  {
    id: "pending",
    labelKey: "vectorId.filter.pending",
    subKey: "vectorId.filter.pending.sub",
  },
  {
    id: "notstarted",
    labelKey: "vectorId.filter.notstarted",
    subKey: "vectorId.filter.notstarted.sub",
  },
  {
    id: "partialid",
    labelKey: "vectorId.filter.partialid",
    subKey: "vectorId.filter.partialid.sub",
  },
  {
    id: "decon",
    labelKey: "vectorId.filter.decon",
    subKey: "vectorId.filter.decon.sub",
  },
  {
    id: "complete",
    labelKey: "vectorId.filter.complete",
    subKey: "vectorId.filter.complete.sub",
  },
];

const IdStatusTag = ({ value }) => {
  if (!value || value === "RECEIVED")
    return (
      <Tag type="gray">
        <FormattedMessage id="vectorId.status.notStarted" />
      </Tag>
    );
  if (value === "IDENTIFICATION_IN_PROGRESS")
    return (
      <Tag type="blue">
        <FormattedMessage id="vectorId.status.inProgress" />
      </Tag>
    );
  if (value === "COMPLETE")
    return (
      <Tag type="green">
        <FormattedMessage id="vectorId.status.complete" />
      </Tag>
    );
  return <Tag type="gray">{value}</Tag>;
};

const ProgressBar = ({ done, total }) => {
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
            background:
              pct === 100
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

const VectorIdentificationWorklist = () => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [filterId, setFilterId] = useState("pending");
  const [rows, setRows] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [expandedPoolId, setExpandedPoolId] = useState(null);
  const [counts, setCounts] = useState({});
  // Row from the decon worklist whose modal is open (null = closed).
  const [deconViewRow, setDeconViewRow] = useState(null);

  const loadCount = useCallback((statusId) => {
    if (statusId === "decon") {
      return VectorDeconvolutionAPI.getWorklist()
        .then((data) => (Array.isArray(data) ? data.length : 0))
        .catch(() => 0);
    }
    return VectorIdentificationAPI.getWorklist(statusId)
      .then((data) => (Array.isArray(data) ? data.length : 0))
      .catch(() => 0);
  }, []);

  const refreshCounts = useCallback(() => {
    Promise.all(STATUS_FILTERS.map((f) => loadCount(f.id))).then((results) => {
      const map = {};
      STATUS_FILTERS.forEach((f, i) => (map[f.id] = results[i]));
      setCounts(map);
    });
  }, [loadCount]);

  // Same dep-stability pattern as VectorLotDetail.load: intl /
  // addNotification / setNotificationVisible churn on every Layout re-render
  // (every notification dispatch). Including them here re-creates
  // loadWorklist → re-fires the useEffect below → triggers setRows/setLoading
  // during unmount windows, producing "state update on unmounted component"
  // warnings. Capture via closure; depend on nothing.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const loadWorklist = useCallback((statusId) => {
    if (statusId === "decon") {
      setRows([]);
      return;
    }
    setLoading(true);
    VectorIdentificationAPI.getWorklist(statusId)
      .then((data) => setRows(Array.isArray(data) ? data : []))
      .catch((err) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorId.error.loadWorklist" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadWorklist(filterId);
    setExpandedPoolId(null);
  }, [filterId, loadWorklist]);

  useEffect(() => {
    refreshCounts();
  }, [refreshCounts]);

  const onChange = useCallback(() => {
    loadWorklist(filterId);
    refreshCounts();
  }, [loadWorklist, filterId, refreshCounts]);

  const filtered = rows.filter((r) => {
    if (!search) return true;
    const needle = search.toLowerCase();
    const groupHaystack = (r.organismGroups || [r.organismGroup])
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return (
      (r.accessionNumber || "").toLowerCase().includes(needle) ||
      (r.samplingSiteName || "").toLowerCase().includes(needle) ||
      groupHaystack.includes(needle)
    );
  });

  const filterOption =
    STATUS_FILTERS.find((f) => f.id === filterId) || STATUS_FILTERS[0];

  return (
    <Section>
      <Grid fullWidth style={{ padding: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <h2 style={{ marginBottom: "0.25rem" }}>
            <FormattedMessage id="vectorId.heading.worklist" />
          </h2>
          <p
            style={{
              color: "var(--cds-text-secondary, #525252)",
              fontSize: "0.8125rem",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage id={filterOption.subKey} />
          </p>

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.75rem",
              marginBottom: "0.75rem",
              padding: "0.5rem 0.75rem",
              background: "var(--cds-background, #fff)",
              border: "1px solid #e0e0e0",
            }}
          >
            <div style={{ minWidth: 220 }}>
              <Select
                id="vectorId-status-filter"
                labelText={intl.formatMessage({ id: "vectorId.filter.label" })}
                hideLabel
                value={filterId}
                onChange={(e) => setFilterId(e.target.value)}
              >
                {STATUS_FILTERS.map((f) => (
                  <SelectItem
                    key={f.id}
                    value={f.id}
                    text={`${intl.formatMessage({ id: f.labelKey })} (${counts[f.id] ?? 0})`}
                  />
                ))}
              </Select>
            </div>
            <TextInput
              id="vectorId-search"
              labelText=""
              placeholder={intl.formatMessage({
                id: "vectorId.search.placeholder",
              })}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ flex: 1 }}
              size="md"
            />
          </div>

          {filterId === "decon" ? (
            <VectorDeconvolutionWorklist
              embedded
              onView={(row) => setDeconViewRow(row)}
            />
          ) : loading ? (
            <div style={{ padding: "1rem" }}>
              <InlineLoading
                description={intl.formatMessage({ id: "vectorId.loading" })}
              />
            </div>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableHeader style={{ width: 36 }} />
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.lotId" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.site" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.collectionDate" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.organismGroup" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.progress" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.idStatus" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="vectorId.col.decon" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filtered.length === 0 && (
                    <TableRow>
                      <TableCell
                        colSpan={8}
                        style={{
                          textAlign: "center",
                          color: "var(--cds-text-placeholder, #8d8d8d)",
                        }}
                      >
                        <FormattedMessage id="vectorId.empty" />
                      </TableCell>
                    </TableRow>
                  )}
                  {filtered.map((lot) => {
                    const expanded = expandedPoolId === lot.vectorPoolId;
                    const idStatus =
                      (lot.identifiedSpecimens || 0) === 0
                        ? "RECEIVED"
                        : (lot.identifiedSpecimens || 0) <
                            (lot.totalSpecimens || 0)
                          ? "IDENTIFICATION_IN_PROGRESS"
                          : "COMPLETE";
                    return (
                      <React.Fragment key={lot.vectorPoolId ?? lot.sampleId}>
                        <TableRow
                          onClick={() =>
                            setExpandedPoolId(
                              expanded ? null : lot.vectorPoolId,
                            )
                          }
                          style={{ cursor: "pointer" }}
                        >
                          <TableCell
                            style={{
                              textAlign: "center",
                              color: "var(--cds-link-primary, #0f62fe)",
                            }}
                          >
                            {expanded ? "▼" : "▶"}
                          </TableCell>
                          <TableCell style={{ fontWeight: 600 }}>
                            {lot.lotExternalId || lot.accessionNumber}
                          </TableCell>
                          <TableCell>{lot.samplingSiteName || "—"}</TableCell>
                          <TableCell>
                            {lot.collectionDate
                              ? new Date(
                                  lot.collectionDate,
                                ).toLocaleDateString()
                              : "—"}
                          </TableCell>
                          <TableCell>
                            {lot.organismGroups &&
                            lot.organismGroups.length > 0 ? (
                              lot.organismGroups.map((g) => (
                                <Tag
                                  key={g}
                                  type="purple"
                                  style={{ marginRight: 4 }}
                                >
                                  {g}
                                </Tag>
                              ))
                            ) : lot.organismGroup ? (
                              <Tag type="purple">{lot.organismGroup}</Tag>
                            ) : (
                              "—"
                            )}
                          </TableCell>
                          <TableCell>
                            <ProgressBar
                              done={lot.identifiedSpecimens || 0}
                              total={lot.totalSpecimens || 0}
                            />
                          </TableCell>
                          <TableCell>
                            <IdStatusTag value={idStatus} />
                          </TableCell>
                          <TableCell
                            title={intl.formatMessage({
                              id: "vectorId.col.decon.help",
                              defaultMessage:
                                "Deconvolution status — whether positive pool results require splitting into sub-pools.",
                            })}
                          >
                            {lot.deconvolutionStatus === "PENDING" && (
                              <Tag type="red">
                                <FormattedMessage id="vectorId.tag.deconNeeded" />
                              </Tag>
                            )}
                            {lot.deconvolutionStatus === "IN_PROGRESS" && (
                              <>
                                <Tag type="blue">
                                  <FormattedMessage
                                    id="vectorId.tag.deconInProgress"
                                    defaultMessage="In progress"
                                  />
                                </Tag>
                                {lot.pendingSubPoolCount > 0 && (
                                  <Tag type="orange" style={{ marginLeft: 4 }}>
                                    <FormattedMessage
                                      id="vectorId.tag.subPoolPending"
                                      defaultMessage="{count} sub-pool with results"
                                      values={{
                                        count: lot.pendingSubPoolCount,
                                      }}
                                    />
                                  </Tag>
                                )}
                              </>
                            )}
                            {lot.deconvolutionStatus === "COMPLETE" && (
                              <Tag type="green">
                                <FormattedMessage
                                  id="vectorId.tag.deconComplete"
                                  defaultMessage="Complete"
                                />
                              </Tag>
                            )}
                            {(lot.deconvolutionStatus == null ||
                              lot.deconvolutionStatus === "NOT_APPLICABLE") && (
                              <span
                                style={{
                                  color: "var(--cds-text-placeholder, #8d8d8d)",
                                }}
                              >
                                —
                              </span>
                            )}
                          </TableCell>
                        </TableRow>
                        {expanded && (
                          <TableRow>
                            <TableCell
                              colSpan={8}
                              style={{
                                padding: 0,
                                background: "var(--cds-layer-01, #f4f4f4)",
                              }}
                            >
                              <VectorLotDetail
                                vectorPoolId={lot.vectorPoolId}
                                sampleId={lot.sampleId}
                                accessionNumber={
                                  lot.lotExternalId || lot.accessionNumber
                                }
                                samplingSiteName={lot.samplingSiteName}
                                collectionDate={lot.collectionDate}
                                positiveTest={lot.positiveTestName}
                                deconvolutionStatus={lot.deconvolutionStatus}
                                onChange={onChange}
                              />
                            </TableCell>
                          </TableRow>
                        )}
                      </React.Fragment>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Column>
      </Grid>
      <AlertDialog />

      {/* Deconvolution lot detail modal — opened by "View" in the decon worklist */}
      <Modal
        open={deconViewRow !== null}
        onRequestClose={() => setDeconViewRow(null)}
        passiveModal
        size="lg"
        modalHeading={
          deconViewRow
            ? deconViewRow.accessionNumber || String(deconViewRow.vectorPoolId)
            : ""
        }
      >
        {deconViewRow && (
          <VectorLotDetail
            vectorPoolId={deconViewRow.vectorPoolId}
            sampleId={deconViewRow.sampleId}
            accessionNumber={deconViewRow.accessionNumber}
            samplingSiteName={deconViewRow.samplingSiteName}
            positiveTest={deconViewRow.positiveTestName}
            deconvolutionStatus={deconViewRow.deconvolutionStatus}
            onChange={() => {
              setDeconViewRow(null);
              refreshCounts();
            }}
          />
        )}
      </Modal>
    </Section>
  );
};

export default VectorIdentificationWorklist;
