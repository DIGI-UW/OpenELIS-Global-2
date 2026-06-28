import React, { useState, useCallback, useEffect } from "react";
import {
  Grid,
  Column,
  Button,
  Tag,
  Tile,
  DatePicker,
  DatePickerInput,
  Dropdown,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Loading,
  InlineNotification,
  Modal,
  TextArea,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";

const STATUS_TAG_TYPE = {
  COMPLIANT: "green",
  NON_COMPLIANT: "red",
  BORDERLINE: "warm-gray",
  INELIGIBLE: "gray",
  NONE: "gray",
};

const STATUS_TAG_ICON = {
  COMPLIANT: "✓",
  NON_COMPLIANT: "✗",
  BORDERLINE: "⚑",
  INELIGIBLE: "—",
};

function statusTag(status, statusLabels) {
  const type = STATUS_TAG_TYPE[status] || STATUS_TAG_TYPE.NONE;
  const label = statusLabels?.[status]
    ? `${STATUS_TAG_ICON[status] ?? ""} ${statusLabels[status]}`.trim()
    : status ?? "–";
  return <Tag type={type}>{label}</Tag>;
}

const GENERATION_STATUS_ITEMS = [
  { id: "all", text: "All" },
  { id: "generated", text: "Generated" },
  { id: "notGenerated", text: "Not Yet Generated" },
];

const TABLE_HEADERS = [
  { key: "labNumber", header: "Lab Number" },
  { key: "siteName", header: "Site" },
  { key: "standardName", header: "Standard" },
  { key: "collectionDate", header: "Collection Date" },
  { key: "testCount", header: "Tests" },
  { key: "complianceStatus", header: "Compliance" },
  { key: "lastGenerated", header: "Last Generated" },
  { key: "actions", header: "Actions" },
];

export default function LaporanHasilReport() {
  const intl = useIntl();

  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [standardFilter, setStandardFilter] = useState("all");
  const [complianceStatusFilter, setComplianceStatusFilter] = useState("all");
  const [generationStatusFilter, setGenerationStatusFilter] = useState("all");

  const [reportData, setReportData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [generatingPdf, setGeneratingPdf] = useState(null);

  // Amendment modal state
  const [amendmentModal, setAmendmentModal] = useState({
    open: false,
    sampleId: null,
    labNumber: null,
    reason: "",
    submitting: false,
    error: null,
  });

  // { COMPLIANT: "Compliant", NON_COMPLIANT: "Non-Compliant", BORDERLINE: "Borderline" }
  const [statusLabels, setStatusLabels] = useState({});
  // [{ id: "all", text: "All" }, { id: "COMPLIANT", text: "Compliant" }, ...]
  const [complianceStatusItems, setComplianceStatusItems] = useState([
    { id: "all", text: "All" },
  ]);
  // [{ id: "all", text: "All Standards" }, { id: "1", text: "PP 22/2021 — Water Quality" }, ...]
  const [standardItems, setStandardItems] = useState([
    { id: "all", text: intl.formatMessage({ id: "laporanHasil.filter.allStandards", defaultMessage: "All Standards" }) },
  ]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/complianceReport/compliance-statuses",
      (data) => {
        if (!data || !Array.isArray(data)) return;
        const labels = {};
        data.forEach((s) => {
          labels[s.id] = s.text;
        });
        setStatusLabels(labels);
        setComplianceStatusItems([
          { id: "all", text: intl.formatMessage({ id: "laporanHasil.filter.all", defaultMessage: "All" }) },
          ...data,
        ]);
      },
    );
    getFromOpenElisServer("/rest/compliance/standards/active", (data) => {
      if (!data || !Array.isArray(data)) return;
      setStandardItems([
        { id: "all", text: intl.formatMessage({ id: "laporanHasil.filter.allStandards", defaultMessage: "All Standards" }) },
        ...data.map((s) => ({
          id: String(s.id),
          text: s.regulationNumber
            ? `${s.regulationNumber} — ${s.name}`
            : s.name,
        })),
      ]);
    });
  }, [intl]);

  const doFetch = useCallback(
    (overrides = {}) => {
      const df = overrides.dateFrom ?? dateFrom;
      const dt = overrides.dateTo ?? dateTo;
      const std = overrides.standardFilter ?? standardFilter;
      const cs = overrides.complianceStatusFilter ?? complianceStatusFilter;
      const gs = overrides.generationStatusFilter ?? generationStatusFilter;

      setIsLoading(true);
      setError(null);

      const params = new URLSearchParams();
      if (df) params.set("dateFrom", df);
      if (dt) params.set("dateTo", dt);
      if (std && std !== "all") params.set("standardId", std);
      if (cs && cs !== "all") params.set("complianceStatus", cs);
      if (gs && gs !== "all") params.set("generationStatus", gs);

      getFromOpenElisServer(
        `/rest/complianceReport?${params.toString()}`,
        (data) => {
          if (data) {
            setReportData(data);
          } else {
            setError(intl.formatMessage({ id: "laporanHasil.error" }));
          }
          setIsLoading(false);
        },
      );
    },
    [dateFrom, dateTo, standardFilter, complianceStatusFilter, generationStatusFilter, intl],
  );

  const fetchReport = useCallback(() => doFetch(), [doFetch]);


  const handleGeneratePdf = useCallback(
    (sampleId, labNumber) => {
      setGeneratingPdf(sampleId);
      const safeLabel = labNumber.replace(/[^a-zA-Z0-9-]/g, "");
      fetch(
        `${config.serverBaseUrl}/rest/complianceReport/exportPdf?sampleId=${sampleId}`,
        {
          credentials: "include",
          headers: { "X-CSRF-Token": localStorage.getItem("CSRF") },
        },
      )
        .then((res) => {
          if (!res.ok) throw new Error("HTTP " + res.status);
          return res.blob();
        })
        .then((blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          a.download = `LH-${safeLabel}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
          const now = new Date().toLocaleString();
          setReportData((prev) => {
            if (!prev) return prev;
            return {
              ...prev,
              generatedCount: prev.generatedCount + 1,
              notYetGeneratedCount: Math.max(0, prev.notYetGeneratedCount - 1),
              orders: prev.orders.map((o) =>
                o.sampleId === sampleId ? { ...o, lastGenerated: now } : o,
              ),
            };
          });
        })
        .catch((err) => {
          setError(
            intl.formatMessage(
              { id: "laporanHasil.error.pdf", defaultMessage: "Failed to download PDF: {msg}" },
              { msg: err.message },
            ),
          );
        })
        .finally(() => setGeneratingPdf(null));
    },
    [intl],
  );

  const openAmendmentModal = useCallback((sampleId, labNumber) => {
    setAmendmentModal({
      open: true,
      sampleId,
      labNumber,
      reason: "",
      submitting: false,
      error: null,
    });
  }, []);

  const closeAmendmentModal = useCallback(() => {
    setAmendmentModal((prev) => ({ ...prev, open: false }));
  }, []);

  const handleReissue = useCallback(() => {
    const { sampleId, reason } = amendmentModal;
    if (!reason || !reason.trim()) return;

    setAmendmentModal((prev) => ({ ...prev, submitting: true, error: null }));

    fetch(`${config.serverBaseUrl}/rest/complianceReport/reissue`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      credentials: "include",
      body: JSON.stringify({ sampleId, reason: reason.trim() }),
    })
      .then((res) => {
        if (!res.ok) {
          return res.text().then((msg) => {
            throw new Error(msg || intl.formatMessage({ id: "lhu.amendment.error.generic" }));
          });
        }
        return res.blob();
      })
      .then((blob) => {
        // Trigger PDF download
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `LH-${amendmentModal.labNumber}-amendment.pdf`;
        a.click();
        URL.revokeObjectURL(url);

        const now = new Date().toLocaleString();
        setReportData((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            orders: prev.orders.map((o) =>
              o.sampleId === sampleId ? { ...o, lastGenerated: now } : o,
            ),
          };
        });
        setAmendmentModal((prev) => ({ ...prev, open: false, submitting: false }));
      })
      .catch((err) => {
        setAmendmentModal((prev) => ({
          ...prev,
          submitting: false,
          error: err.message,
        }));
      });
  }, [amendmentModal, intl]);

  const tableRows = reportData?.orders?.map((order) => ({
    id: String(order.sampleId),
    labNumber: order.labNumber || "–",
    siteName: order.siteName || order.siteCode || "–",
    standardName: order.regulationNumber
      ? `${order.regulationNumber}`
      : (order.standardName ?? "–"),
    collectionDate: order.collectionDate || "–",
    testCount: order.testCount ?? 0,
    complianceStatus: order.complianceStatus || "NONE",
    lastGenerated: order.lastGenerated
      ? new Date(order.lastGenerated).toLocaleString()
      : "Not Yet Generated",
    actions: order.sampleId,
    _order: order,
  })) ?? [];

  return (
    <div className="orderLegendBody">
      <h2 style={{ marginBottom: "0.25rem" }}>
        <FormattedMessage id="laporanHasil.title" />
      </h2>
      <p style={{ color: "#525252", marginBottom: "1.5rem" }}>
        <FormattedMessage id="laporanHasil.subtitle" />
      </p>

      {reportData && (
        <Grid narrow style={{ marginBottom: "1.5rem" }}>
          <Column lg={4} md={4} sm={4}>
            <Tile style={{ borderLeft: "4px solid #8d8d8d" }}>
              <p style={{ fontSize: "2rem", fontWeight: "600" }}>
                {reportData.ineligibleCount}
              </p>
              <p>
                <FormattedMessage id="laporanHasil.tile.ineligible" />
              </p>
              <p style={{ fontSize: "0.75rem", color: "#525252" }}>
                <FormattedMessage id="laporanHasil.tile.ineligible.desc" />
              </p>
            </Tile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Tile style={{ borderLeft: "4px solid #24a148" }}>
              <p style={{ fontSize: "2rem", fontWeight: "600" }}>
                {reportData.generatedCount}
              </p>
              <p>
                <FormattedMessage id="laporanHasil.tile.generated" />
              </p>
            </Tile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Tile style={{ borderLeft: "4px solid #f1c21b" }}>
              <p style={{ fontSize: "2rem", fontWeight: "600" }}>
                {reportData.notYetGeneratedCount}
              </p>
              <p>
                <FormattedMessage id="laporanHasil.tile.notGenerated" />
              </p>
            </Tile>
          </Column>
        </Grid>
      )}

      <Grid narrow style={{ marginBottom: "1.5rem", alignItems: "flex-end" }}>
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            dateFormat="Y-m-d"
            datePickerType="single"
            onChange={(dates) =>
              setDateFrom(
                dates[0] ? dates[0].toISOString().split("T")[0] : "",
              )
            }
          >
            <DatePickerInput
              id="filter-date-from"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({ id: "laporanHasil.filter.dateFrom" })}
            />
          </DatePicker>
        </Column>
        <Column lg={3} md={4} sm={4}>
          <DatePicker
            dateFormat="Y-m-d"
            datePickerType="single"
            onChange={(dates) =>
              setDateTo(dates[0] ? dates[0].toISOString().split("T")[0] : "")
            }
          >
            <DatePickerInput
              id="filter-date-to"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({ id: "laporanHasil.filter.dateTo" })}
            />
          </DatePicker>
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="filter-standard"
            titleText={intl.formatMessage({ id: "laporanHasil.filter.standard" })}
            label={intl.formatMessage({ id: "laporanHasil.filter.allStandards", defaultMessage: "All Standards" })}
            items={standardItems}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) => {
              const val = selectedItem?.id ?? "all";
              setStandardFilter(val);
              if (reportData)
                doFetch({
                  dateFrom,
                  dateTo,
                  standardFilter: val,
                  complianceStatusFilter,
                  generationStatusFilter,
                });
            }}
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="filter-compliance-status"
            titleText={intl.formatMessage({ id: "laporanHasil.filter.complianceStatus" })}
            label={intl.formatMessage({ id: "laporanHasil.filter.all" })}
            items={complianceStatusItems}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) => {
              const val = selectedItem?.id ?? "all";
              setComplianceStatusFilter(val);
              if (reportData)
                doFetch({
                  dateFrom,
                  dateTo,
                  standardFilter,
                  complianceStatusFilter: val,
                  generationStatusFilter,
                });
            }}
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="filter-generation-status"
            titleText={intl.formatMessage({ id: "laporanHasil.filter.generationStatus" })}
            label={intl.formatMessage({ id: "laporanHasil.filter.all" })}
            items={GENERATION_STATUS_ITEMS}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) => {
              const val = selectedItem?.id ?? "all";
              setGenerationStatusFilter(val);
              if (reportData)
                doFetch({
                  dateFrom,
                  dateTo,
                  standardFilter,
                  complianceStatusFilter,
                  generationStatusFilter: val,
                });
            }}
          />
        </Column>
        <Column lg={4} md={8} sm={4} style={{ paddingTop: "1.5rem" }}>
          <Button onClick={fetchReport} disabled={isLoading}>
            <FormattedMessage id="label.button.search" defaultMessage="Search" />
          </Button>
        </Column>
      </Grid>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {isLoading && <Loading withOverlay={false} />}

      {!isLoading && reportData && (
        <DataTable rows={tableRows} headers={TABLE_HEADERS}>
          {({
            rows,
            headers,
            getHeaderProps,
            getRowProps,
            getTableProps,
            getExpandHeaderProps,
          }) => (
            <TableContainer>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    <TableExpandHeader {...getExpandHeaderProps()} />
                    {headers.map((header) => (
                      <TableHeader key={header.key} {...getHeaderProps({ header })}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => {
                    const order = tableRows.find((r) => r.id === row.id)?._order;
                    return (
                      <React.Fragment key={row.id}>
                        <TableExpandRow {...getRowProps({ row })}>
                          {row.cells.map((cell) => {
                            if (cell.info.header === "complianceStatus") {
                              return (
                                <TableCell key={cell.id}>
                                  {statusTag(cell.value, statusLabels)}
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "actions") {
                              const isIneligible =
                                order?.complianceStatus === "INELIGIBLE";
                              const hasBeenReleased = !!order?.lastGenerated;
                              return (
                                <TableCell key={cell.id}>
                                  {isIneligible ? (
                                    <span style={{ color: "#525252", fontSize: "0.875rem" }}>
                                      <FormattedMessage
                                        id="laporanHasil.status.ineligible"
                                        defaultMessage="No standard linked"
                                      />
                                    </span>
                                  ) : hasBeenReleased ? (
                                    <Button
                                      size="sm"
                                      kind="tertiary"
                                      onClick={() =>
                                        openAmendmentModal(cell.value, order?.labNumber)
                                      }
                                    >
                                      <FormattedMessage id="lhu.amendment.reissue" />
                                    </Button>
                                  ) : (
                                    <Button
                                      size="sm"
                                      kind="primary"
                                      disabled={generatingPdf === cell.value}
                                      onClick={() =>
                                        handleGeneratePdf(
                                          cell.value,
                                          order?.labNumber,
                                        )
                                      }
                                    >
                                      <FormattedMessage id="laporanHasil.action.generatePdf" />
                                    </Button>
                                  )}
                                </TableCell>
                              );
                            }
                            return (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            );
                          })}
                        </TableExpandRow>
                        <TableExpandedRow colSpan={headers.length + 1}>
                          {order && <OrderDetail order={order} statusLabels={statusLabels} />}
                        </TableExpandedRow>
                      </React.Fragment>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}

      {!isLoading && reportData && tableRows.length === 0 && (
        <p style={{ marginTop: "1rem", color: "#525252" }}>
          <FormattedMessage id="laporanHasil.empty" />
        </p>
      )}

      <Modal
        open={amendmentModal.open}
        modalHeading={intl.formatMessage({ id: "lhu.amendment.modal.heading" })}
        primaryButtonText={
          amendmentModal.submitting ? (
            <InlineLoading description={intl.formatMessage({ id: "lhu.amendment.submitting" })} />
          ) : (
            intl.formatMessage({ id: "lhu.amendment.modal.submit" })
          )
        }
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel", defaultMessage: "Cancel" })}
        primaryButtonDisabled={
          !amendmentModal.reason?.trim() || amendmentModal.submitting
        }
        onRequestSubmit={handleReissue}
        onRequestClose={closeAmendmentModal}
        onSecondarySubmit={closeAmendmentModal}
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="lhu.amendment.modal.description"
            values={{ labNumber: amendmentModal.labNumber }}
          />
        </p>
        <TextArea
          id="amendment-reason"
          labelText={intl.formatMessage({ id: "lhu.amendment.reason.label" })}
          helperText={intl.formatMessage({ id: "lhu.amendment.reason.helper" })}
          placeholder={intl.formatMessage({ id: "lhu.amendment.reason.placeholder" })}
          value={amendmentModal.reason}
          onChange={(e) =>
            setAmendmentModal((prev) => ({ ...prev, reason: e.target.value }))
          }
          invalid={amendmentModal.reason !== undefined && amendmentModal.reason.trim() === "" && amendmentModal.open}
          invalidText={intl.formatMessage({ id: "lhu.amendment.reason.required" })}
          rows={4}
        />
        {amendmentModal.error && (
          <InlineNotification
            kind="error"
            title={amendmentModal.error}
            style={{ marginTop: "1rem" }}
          />
        )}
      </Modal>
    </div>
  );
}

function OrderDetail({ order, statusLabels }) {
  return (
    <Grid narrow style={{ padding: "1rem 0" }}>
      <Column lg={5} md={4} sm={4}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="laporanHasil.detail.siteInfo" />
        </h5>
        <StructuredListWrapper>
          <StructuredListBody>
            {[
              ["laporanHasil.detail.site", order.siteName || order.siteCode],
              ["laporanHasil.detail.gps", order.gpsCoordinates],
              ["laporanHasil.detail.collectionDateTime", order.collectionDate],
              ["laporanHasil.detail.collectionMethod", order.collectionMethod],
              ["laporanHasil.detail.ppNo", order.regulationNumber],
              ["laporanHasil.detail.standard", order.standardName],
            ].map(([id, value]) => (
              <StructuredListRow key={id}>
                <StructuredListCell noWrap>
                  <FormattedMessage id={id} />
                </StructuredListCell>
                <StructuredListCell>{value || "–"}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </Column>

      <Column lg={4} md={4} sm={4}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="laporanHasil.detail.conditions" />
        </h5>
        <StructuredListWrapper>
          <StructuredListBody>
            {[
              ["laporanHasil.detail.waterTemp", order.waterTemp],
              ["laporanHasil.detail.ambientTemp", order.ambientTemp],
              ["laporanHasil.detail.weather", order.weather],
              ["laporanHasil.detail.preservation", order.preservation],
            ].map(([id, value]) => (
              <StructuredListRow key={id}>
                <StructuredListCell noWrap>
                  <FormattedMessage id={id} />
                </StructuredListCell>
                <StructuredListCell>{value || "–"}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </Column>

      <Column lg={7} md={8} sm={4}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="laporanHasil.detail.complianceSummary" />
        </h5>
        {order.parameterResults && order.parameterResults.length > 0 ? (
          <DataTable
            rows={order.parameterResults.map((pr, i) => ({
              id: String(i),
              parameter: pr.displayName || pr.parameterCode,
              result: pr.resultValue
                ? `${pr.resultValue}${pr.units ? " " + pr.units : ""}`
                : "–",
              threshold: pr.thresholdDisplay || "–",
              status: pr.status,
            }))}
            headers={[
              { key: "parameter", header: "Parameter" },
              { key: "result", header: "Result" },
              { key: "threshold", header: "Threshold" },
              { key: "status", header: "Status" },
            ]}
            size="sm"
          >
            {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((h) => (
                        <TableHeader key={h.key} {...getHeaderProps({ header: h })}>
                          {h.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) =>
                          cell.info.header === "status" ? (
                            <TableCell key={cell.id}>
                              {statusTag(cell.value, statusLabels)}
                            </TableCell>
                          ) : (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ),
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        ) : (
          <p style={{ color: "#525252", fontSize: "0.875rem" }}>–</p>
        )}
      </Column>

      <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="laporanHasil.detail.signatures" />
        </h5>
        <Grid narrow>
          <Column lg={8} md={4} sm={4}>
            <SignatureCard
              sig={order.analystSignature}
              defaultRole="laporanHasil.detail.labAnalyst"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <SignatureCard
              sig={order.managerSignature}
              defaultRole="laporanHasil.detail.labManager"
            />
          </Column>
        </Grid>
      </Column>
    </Grid>
  );
}

function SignatureCard({ sig, defaultRole }) {
  return (
    <Tile>
      <p style={{ fontWeight: "600", marginBottom: "0.25rem" }}>
        <FormattedMessage id={defaultRole} />
      </p>
      {sig ? (
        <>
          <p>{sig.signerName || "–"}</p>
          <p style={{ fontSize: "0.75rem", color: "#525252" }}>
            {sig.signerRole || "–"}
          </p>
          <p style={{ fontSize: "0.75rem", color: "#525252" }}>
            {sig.signedAt || "–"}
          </p>
        </>
      ) : (
        <p style={{ color: "#525252" }}>–</p>
      )}
    </Tile>
  );
}
