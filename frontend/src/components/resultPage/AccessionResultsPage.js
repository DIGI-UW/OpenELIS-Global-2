import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Heading,
  Loading,
  Section,
  Table,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../utils/Utils";

const STATUS_FILTERS = [
  { key: "all", id: "accession.results.filter.all", defaultMessage: "All" },
  {
    key: "pending",
    id: "accession.results.filter.pending",
    defaultMessage: "Pending",
  },
  {
    key: "accepted",
    id: "accession.results.filter.accepted",
    defaultMessage: "Accepted",
  },
  {
    key: "finalized",
    id: "accession.results.filter.finalized",
    defaultMessage: "Finalized",
  },
  {
    key: "rejected",
    id: "accession.results.filter.rejected",
    defaultMessage: "Rejected",
  },
];

const matchesFilter = (statusLabel, activeFilter) => {
  if (activeFilter === "all") return true;
  const status = (statusLabel || "").toLowerCase();
  if (activeFilter === "pending") return status.includes("pending");
  if (activeFilter === "accepted") {
    return status.includes("accept") || status.includes("release");
  }
  if (activeFilter === "finalized") return status.includes("final");
  if (activeFilter === "rejected") {
    return status.includes("reject") || status.includes("cancel");
  }
  return true;
};

const statusTagType = (statusLabel) => {
  const status = (statusLabel || "").toLowerCase();
  if (status.includes("reject") || status.includes("cancel")) return "red";
  if (status.includes("final")) return "green";
  if (status.includes("accept") || status.includes("release")) return "teal";
  if (status.includes("pending")) return "purple";
  return "gray";
};

const formatResult = (item) => {
  const value = item.resultValue || item.value || "";
  if (!value) return "-";
  const units = item.unitsOfMeasure || "";
  return units ? `${value} ${units}` : value;
};

function AccessionResultsPage() {
  const intl = useIntl();
  const [searchValue, setSearchValue] = useState("");
  const [activeFilter, setActiveFilter] = useState("all");
  const [loading, setLoading] = useState(false);
  const [statusMap, setStatusMap] = useState({});
  const [resultsState, setResultsState] = useState({
    accessionNumber: "",
    firstName: "",
    lastName: "",
    testResult: [],
    searchFinished: false,
  });

  const headers = useMemo(
    () => [
      {
        key: "sampleInfo",
        header: intl.formatMessage({
          id: "accession.results.header.sampleInfo",
          defaultMessage: "Sample Info",
        }),
      },
      {
        key: "testName",
        header: intl.formatMessage({
          id: "accession.results.header.testName",
          defaultMessage: "Test Name",
        }),
      },
      {
        key: "status",
        header: intl.formatMessage({
          id: "accession.results.header.status",
          defaultMessage: "Status",
        }),
      },
      {
        key: "result",
        header: intl.formatMessage({
          id: "accession.results.header.result",
          defaultMessage: "Result",
        }),
      },
    ],
    [intl],
  );

  const loadAccessionResults = useCallback((accessionNumber) => {
    const trimmed = (accessionNumber || "").trim();
    setSearchValue(trimmed);
    if (!trimmed) {
      setResultsState((prev) => ({
        ...prev,
        accessionNumber: "",
        testResult: [],
        searchFinished: false,
      }));
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/accession-results?accessionNumber=${encodeURIComponent(trimmed)}`,
      (data) => {
        setLoading(false);
        if (!data) {
          setResultsState((prev) => ({
            ...prev,
            accessionNumber: trimmed,
            testResult: [],
            searchFinished: true,
          }));
          return;
        }
        const withIds = (data.testResult || []).map((item, index) => ({
          ...item,
          id: String(item.analysisId || `row-${index}`),
        }));
        setResultsState({
          ...data,
          accessionNumber: data.accessionNumber || trimmed,
          testResult: withIds,
        });
      },
    );
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/analysis-status-types", (statuses) => {
      const byId = {};
      (statuses || []).forEach((entry) => {
        byId[entry.id] = entry.value;
      });
      setStatusMap(byId);
    });

    const initialAccession =
      new URLSearchParams(window.location.search).get("accessionNumber") || "";
    if (initialAccession) {
      loadAccessionResults(initialAccession);
    }
  }, [loadAccessionResults]);

  const rows = useMemo(() => {
    return (resultsState.testResult || [])
      .map((item, index) => {
        const statusLabel =
          statusMap[item.analysisStatusId] ||
          item.analysisStatusName ||
          item.analysisStatusId ||
          "-";
        return {
          ...item,
          id: item.id || item.analysisId || `result-${index}`,
          sampleInfo: `${item.accessionNumber || resultsState.accessionNumber || "-"}-${item.sequenceNumber || "-"}`,
          testName: item.testName || "-",
          status: statusLabel,
          result: formatResult(item),
        };
      })
      .filter((row) => matchesFilter(row.status, activeFilter));
  }, [
    activeFilter,
    resultsState.accessionNumber,
    resultsState.testResult,
    statusMap,
  ]);

  const onSearch = () => {
    const nextAccession = searchValue.trim();
    const nextUrl = nextAccession
      ? `/AccessionResults?accessionNumber=${encodeURIComponent(nextAccession)}`
      : "/AccessionResults";
    window.history.replaceState({}, "", nextUrl);
    loadAccessionResults(nextAccession);
  };

  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "sidenav.label.results", link: "/LogbookResults" },
          { label: "banner.menu.results.accession" },
        ]}
      />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="banner.menu.results.accession"
                defaultMessage="Results By Order"
              />
            </Heading>
          </Section>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr auto",
              gap: "0.75rem",
              alignItems: "end",
            }}
          >
            <TextInput
              id="accession-results-search"
              labelText={intl.formatMessage({
                id: "result.accession.number",
                defaultMessage: "Accession Number",
              })}
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  onSearch();
                }
              }}
            />
            <Button onClick={onSearch}>
              <FormattedMessage
                id="label.button.search"
                defaultMessage="Search"
              />
            </Button>
          </div>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
            {STATUS_FILTERS.map((filter) => (
              <Tag
                key={filter.key}
                type={activeFilter === filter.key ? "blue" : "gray"}
                onClick={() => setActiveFilter(filter.key)}
              >
                {intl.formatMessage({
                  id: filter.id,
                  defaultMessage: filter.defaultMessage,
                })}
              </Tag>
            ))}
          </div>
        </Column>
        <Column lg={16} md={8} sm={4}>
          {loading ? (
            <Loading />
          ) : (
            <DataTable rows={rows} headers={headers}>
              {({ rows, headers, getHeaderProps, getRowProps }) => (
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader />
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={headers.length + 1}>
                          <FormattedMessage
                            id="accession.results.empty"
                            defaultMessage="No results found for accession number {accessionNumber}"
                            values={{
                              accessionNumber:
                                resultsState.accessionNumber ||
                                searchValue ||
                                "-",
                            }}
                          />
                        </TableCell>
                      </TableRow>
                    ) : (
                      rows.map((row) => (
                        <React.Fragment key={row.id}>
                          <TableExpandRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  <Tag type={statusTagType(cell.value)}>
                                    {cell.value || "-"}
                                  </Tag>
                                ) : (
                                  cell.value
                                )}
                              </TableCell>
                            ))}
                          </TableExpandRow>
                          <TableExpandedRow colSpan={headers.length + 1}>
                            {/*
                              Carbon row objects only contain transformed cell values,
                              so we resolve full detail fields from the source list by id.
                            */}
                            {(() => {
                              const detailRow =
                                (resultsState.testResult || []).find(
                                  (item) =>
                                    String(item.id || item.analysisId) ===
                                    row.id,
                                ) || {};
                              return (
                                <Grid fullWidth={true}>
                                  <Column lg={8} md={4} sm={2}>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.method"
                                          defaultMessage="Method:"
                                        />
                                      </strong>{" "}
                                      {detailRow.testMethod || "-"}
                                    </p>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.normalRange"
                                          defaultMessage="Normal range:"
                                        />
                                      </strong>{" "}
                                      {detailRow.normalRange || "-"}
                                    </p>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.result"
                                          defaultMessage="Result value:"
                                        />
                                      </strong>{" "}
                                      {formatResult(detailRow)}
                                    </p>
                                  </Column>
                                  <Column lg={8} md={4} sm={2}>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.technician"
                                          defaultMessage="Technician:"
                                        />
                                      </strong>{" "}
                                      {detailRow.technician || "-"}
                                    </p>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.notes"
                                          defaultMessage="Notes:"
                                        />
                                      </strong>{" "}
                                      {detailRow.note ||
                                        detailRow.remarks ||
                                        "-"}
                                    </p>
                                    <p>
                                      <strong>
                                        <FormattedMessage
                                          id="accession.results.detail.testDate"
                                          defaultMessage="Test date:"
                                        />
                                      </strong>{" "}
                                      {detailRow.testDate || "-"}
                                    </p>
                                  </Column>
                                </Grid>
                              );
                            })()}
                          </TableExpandedRow>
                        </React.Fragment>
                      ))
                    )}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          )}
        </Column>
      </Grid>
    </>
  );
}

export default AccessionResultsPage;
