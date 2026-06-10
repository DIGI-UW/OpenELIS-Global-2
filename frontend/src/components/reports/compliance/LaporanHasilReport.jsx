import React, { useState, useCallback } from "react";
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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";

const STATUS_TAG = {
  COMPLIANT: { type: "green", label: "✓ Compliant" },
  NON_COMPLIANT: { type: "red", label: "✗ Non-Compliant" },
  BORDERLINE: { type: "warm-gray", label: "⚑ Borderline" },
  NONE: { type: "gray", label: "–" },
};

function statusTag(status) {
  const cfg = STATUS_TAG[status] || STATUS_TAG.NONE;
  return <Tag type={cfg.type}>{cfg.label}</Tag>;
}

const COMPLIANCE_STATUS_ITEMS = [
  { id: "all", text: "All" },
  { id: "COMPLIANT", text: "Compliant" },
  { id: "NON_COMPLIANT", text: "Non-Compliant" },
  { id: "BORDERLINE", text: "Borderline" },
];

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
  const [complianceStatusFilter, setComplianceStatusFilter] = useState("all");
  const [generationStatusFilter, setGenerationStatusFilter] = useState("all");

  const [reportData, setReportData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [generatingPdf, setGeneratingPdf] = useState(null);

  const fetchReport = useCallback(() => {
    setIsLoading(true);
    setError(null);

    const params = new URLSearchParams();
    if (dateFrom) params.set("dateFrom", dateFrom);
    if (dateTo) params.set("dateTo", dateTo);
    if (complianceStatusFilter && complianceStatusFilter !== "all") {
      params.set("complianceStatus", complianceStatusFilter);
    }
    if (generationStatusFilter && generationStatusFilter !== "all") {
      params.set("generationStatus", generationStatusFilter);
    }

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
  }, [dateFrom, dateTo, complianceStatusFilter, generationStatusFilter, intl]);

  const handleGeneratePdf = useCallback(
    (sampleId, labNumber) => {
      setGeneratingPdf(sampleId);
      const safeLabel = labNumber.replace(/[^a-zA-Z0-9-]/g, "");
      window.open(
        `${config.serverBaseUrl}/rest/complianceReport/exportPdf?sampleId=${sampleId}`,
        "_blank",
      );
      setTimeout(() => {
        setGeneratingPdf(null);
        fetchReport();
      }, 2000);
    },
    [fetchReport],
  );

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
            id="filter-compliance-status"
            titleText={intl.formatMessage({ id: "laporanHasil.filter.complianceStatus" })}
            label={intl.formatMessage({ id: "laporanHasil.filter.all" })}
            items={COMPLIANCE_STATUS_ITEMS}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) =>
              setComplianceStatusFilter(selectedItem?.id ?? "all")
            }
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <Dropdown
            id="filter-generation-status"
            titleText={intl.formatMessage({ id: "laporanHasil.filter.generationStatus" })}
            label={intl.formatMessage({ id: "laporanHasil.filter.all" })}
            items={GENERATION_STATUS_ITEMS}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) =>
              setGenerationStatusFilter(selectedItem?.id ?? "all")
            }
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
                                  {statusTag(cell.value)}
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "actions") {
                              return (
                                <TableCell key={cell.id}>
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
                                </TableCell>
                              );
                            }
                            return (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            );
                          })}
                        </TableExpandRow>
                        <TableExpandedRow colSpan={headers.length + 1}>
                          {order && <OrderDetail order={order} />}
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
    </div>
  );
}

function OrderDetail({ order }) {
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
                              {statusTag(cell.value)}
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
