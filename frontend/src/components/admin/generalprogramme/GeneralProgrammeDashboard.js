import { useEffect, useState } from "react";
import {
  Heading,
  Loading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Grid,
  Column,
  Section,
  Pagination,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import { View, Search } from "@carbon/icons-react";

// === BREADCRUMB CONFIGURATION ===
// Navigation breadcrumb showing: Home > Admin > General Programme
const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "menu.generalprogramme.label",
    link: "/GeneralProgrammeDashboard",
  },
];

function GeneralProgrammeDashboard() {
  const intl = useIntl();

  // === STATE MANAGEMENT ===
  const [loading, setLoading] = useState(true);
  const [programmeData, setProgrammeData] = useState([]); // List of general programmes
  const [selectedProgramme, setSelectedProgramme] = useState(null); // Currently selected programme
  const [capturedData, setCapturedData] = useState([]); // Captured data during order entry
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchCriteria, setSearchCriteria] = useState({
    startDate: "",
    endDate: "",
    programmeId: "",
  });

  useEffect(() => {
    fetchProgrammes();
  }, []);

  const fetchProgrammes = () => {
    getFromOpenElisServer("/rest/displayList/PROGRAM", (data) => {
      const filtered = data.filter(
        (entry) =>
          !["Cytology", "ImmunoHistoChemistry", "Pathology"].includes(
            entry.value,
          ),
      );
      setProgrammeData(filtered);
      setLoading(false);
    });
  };

  // Fetch captured data for selected programme (data captured during order entry)
  const fetchCapturedData = (programmeId, startDate = "", endDate = "") => {
    setLoading(true);
    let endpoint = `/rest/program/${programmeId}/captured-data`;

    const params = new URLSearchParams();
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);

    if (params.toString()) {
      endpoint += `?${params.toString()}`;
    }

    getFromOpenElisServer(endpoint, (data) => {
      setCapturedData(data || []);
      setLoading(false);
    });
  };

  const handleViewProgramme = (programmeId, programmeName) => {
    setSelectedProgramme({ id: programmeId, name: programmeName });
    fetchCapturedData(
      programmeId,
      searchCriteria.startDate,
      searchCriteria.endDate,
    );
  };

  const handleBackToList = () => {
    setSelectedProgramme(null);
    setCapturedData([]);
  };

  const handleSearch = () => {
    if (selectedProgramme) {
      fetchCapturedData(
        selectedProgramme.id,
        searchCriteria.startDate,
        searchCriteria.endDate,
      );
    }
  };

  const handlePageChange = (pageInfo) => {
    setPage(pageInfo.page);
    setPageSize(pageInfo.pageSize);
  };

  const programmeHeaders = [
    {
      key: "value",
      header: intl.formatMessage({ id: "program.name.label" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.action" }),
    },
  ];

  const capturedDataHeaders = [
    {
      key: "labNumber",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "patientName",
      header: intl.formatMessage({ id: "patient.label.name" }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({ id: "sample.label.collectiondate" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "label.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.action" }),
    },
  ];

  const programmeRows = programmeData
    .slice((page - 1) * pageSize, page * pageSize)
    .map((programme) => ({
      id: programme.id,
      value: programme.value,
      actions: programme.id,
    }));

  const capturedDataRows = capturedData
    .slice((page - 1) * pageSize, page * pageSize)
    .map((item) => ({
      id: item.id,
      labNumber: item.labNumber || "",
      patientName: item.patientName || "",
      collectionDate: item.collectionDate || "",
      status: item.status || "",
      actions: item.id,
    }));

  if (selectedProgramme) {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "1rem",
                  marginBottom: "2rem",
                }}
              >
                <Button kind="secondary" onClick={handleBackToList}>
                  <FormattedMessage id="label.button.back" />
                </Button>
                <Heading>
                  {selectedProgramme.name} -{" "}
                  <FormattedMessage id="program.captured.data.label" />
                </Heading>
              </div>

              {/* === SEARCH FILTERS === */}
              <div
                style={{
                  display: "flex",
                  gap: "1rem",
                  alignItems: "end",
                  marginBottom: "2rem",
                  padding: "1rem",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <DatePicker dateFormat="d/m/Y" datePickerType="single">
                  <DatePickerInput
                    id="start-date"
                    placeholder="dd/mm/yyyy"
                    labelText={intl.formatMessage({ id: "label.start.date" })}
                    value={searchCriteria.startDate}
                    onChange={(e) =>
                      setSearchCriteria({
                        ...searchCriteria,
                        startDate: e.target.value,
                      })
                    }
                  />
                </DatePicker>

                <DatePicker dateFormat="d/m/Y" datePickerType="single">
                  <DatePickerInput
                    id="end-date"
                    placeholder="dd/mm/yyyy"
                    labelText={intl.formatMessage({ id: "label.end.date" })}
                    value={searchCriteria.endDate}
                    onChange={(e) =>
                      setSearchCriteria({
                        ...searchCriteria,
                        endDate: e.target.value,
                      })
                    }
                  />
                </DatePicker>

                <Button
                  kind="primary"
                  renderIcon={Search}
                  onClick={handleSearch}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </div>
            </Section>
          </Column>
        </Grid>

        {loading ? (
          <Loading />
        ) : (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <DataTable
                rows={capturedDataRows}
                headers={capturedDataHeaders}
                isSortable
              >
                {({ rows, headers, getHeaderProps, getTableProps }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "program.captured.data.title",
                    })}
                    description={intl.formatMessage(
                      {
                        id: "program.captured.data.description",
                      },
                      { programmeName: selectedProgramme.name },
                    )}
                  >
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
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
                        {rows.map((row) => (
                          <TableRow key={row.id}>
                            <TableCell>{row.cells[0].value}</TableCell>
                            <TableCell>{row.cells[1].value}</TableCell>
                            <TableCell>{row.cells[2].value}</TableCell>
                            <TableCell>{row.cells[3].value}</TableCell>
                            <TableCell>
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={View}
                                onClick={() => {
                                  window.location.href = `/GeneralProgrammeCase/${row.id}`;
                                }}
                              >
                                <FormattedMessage id="label.button.view" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>

              {capturedDataRows.length === 0 && !loading && (
                <div
                  style={{
                    textAlign: "center",
                    padding: "2rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    marginTop: "1rem",
                  }}
                >
                  <p>
                    <FormattedMessage id="program.no.captured.data" />
                  </p>
                </div>
              )}

              <Pagination
                onChange={handlePageChange}
                page={page}
                pageSize={pageSize}
                pageSizes={[5, 10, 20, 30]}
                totalItems={capturedData.length}
                forwardText={intl.formatMessage({ id: "pagination.forward" })}
                backwardText={intl.formatMessage({ id: "pagination.backward" })}
                itemRangeText={(min, max, total) =>
                  intl.formatMessage(
                    { id: "pagination.item-range" },
                    { min: min, max: max, total: total },
                  )
                }
                itemsPerPageText={intl.formatMessage({
                  id: "pagination.items-per-page",
                })}
                itemText={(min, max) =>
                  intl.formatMessage(
                    { id: "pagination.item" },
                    { min: min, max: max },
                  )
                }
                pageNumberText={intl.formatMessage({
                  id: "pagination.page-number",
                })}
                pageRangeText={(_current, total) =>
                  intl.formatMessage(
                    { id: "pagination.page-range" },
                    { total: total },
                  )
                }
                pageText={(page, pagesUnknown) =>
                  intl.formatMessage(
                    { id: "pagination.page" },
                    { page: pagesUnknown ? "" : page },
                  )
                }
              />
            </Column>
          </Grid>
        )}
      </div>
    );
  }
  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="menu.generalprogramme.label" />
            </Heading>
            <p style={{ marginTop: "1rem", marginBottom: "2rem" }}>
              <FormattedMessage id="menu.generalprogramme.description" />
            </p>
          </Section>
        </Column>
      </Grid>

      {loading ? (
        <Loading />
      ) : (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={programmeRows}
              headers={programmeHeaders}
              isSortable
            >
              {({ rows, headers, getHeaderProps, getTableProps }) => (
                <TableContainer
                  title={intl.formatMessage({ id: "program.list.title" })}
                  description={intl.formatMessage({
                    id: "program.list.description",
                  })}
                >
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
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
                      {rows.map((row) => (
                        <TableRow key={row.id}>
                          <TableCell>{row.cells[0].value}</TableCell>
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={View}
                              onClick={() =>
                                handleViewProgramme(row.id, row.cells[0].value)
                              }
                            >
                              <FormattedMessage id="label.button.view" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[5, 10, 20, 30]}
              totalItems={programmeData.length}
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
              itemRangeText={(min, max, total) =>
                intl.formatMessage(
                  { id: "pagination.item-range" },
                  { min: min, max: max, total: total },
                )
              }
              itemsPerPageText={intl.formatMessage({
                id: "pagination.items-per-page",
              })}
              itemText={(min, max) =>
                intl.formatMessage(
                  { id: "pagination.item" },
                  { min: min, max: max },
                )
              }
              pageNumberText={intl.formatMessage({
                id: "pagination.page-number",
              })}
              pageRangeText={(_current, total) =>
                intl.formatMessage(
                  { id: "pagination.page-range" },
                  { total: total },
                )
              }
              pageText={(page, pagesUnknown) =>
                intl.formatMessage(
                  { id: "pagination.page" },
                  { page: pagesUnknown ? "" : page },
                )
              }
            />
          </Column>
        </Grid>
      )}
    </div>
  );
}

export default GeneralProgrammeDashboard;
