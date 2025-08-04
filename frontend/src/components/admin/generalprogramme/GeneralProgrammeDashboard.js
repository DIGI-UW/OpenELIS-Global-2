"use client";

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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import { View } from "@carbon/icons-react";

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
  const [loading, setLoading] = useState(true);
  const [programmeData, setProgrammeData] = useState([]);
  const [selectedProgramme, setSelectedProgramme] = useState(null);
  const [programmeDetails, setProgrammeDetails] = useState(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

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

  const fetchProgrammeDetails = (programmeId) => {
    setLoading(true);
    getFromOpenElisServer(`/rest/program/${programmeId}`, (data) => {
      setProgrammeDetails(data);
      setLoading(false);
    });
  };

  const handleViewProgramme = (programmeId, programmeName) => {
    setSelectedProgramme({ id: programmeId, name: programmeName });
    fetchProgrammeDetails(programmeId);
  };

  const handleBackToList = () => {
    setSelectedProgramme(null);
    setProgrammeDetails(null);
  };

  const handlePageChange = (pageInfo) => {
    setPage(pageInfo.page);
    setPageSize(pageInfo.pageSize);
  };

  const headers = [
    {
      key: "value",
      header: intl.formatMessage({ id: "program.name.label" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.action" }),
    },
  ];

  const rows = programmeData
    .slice((page - 1) * pageSize, page * pageSize)
    .map((programme) => ({
      id: programme.id,
      value: programme.value,
      actions: programme.id,
    }));

  if (selectedProgramme && programmeDetails) {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <div
                style={{ display: "flex", alignItems: "center", gap: "1rem" }}
              >
                <Button kind="secondary" onClick={handleBackToList}>
                  <FormattedMessage id="label.button.back" />
                </Button>
                <Heading>
                  {selectedProgramme.name} -{" "}
                  <FormattedMessage id="program.details.label" />
                </Heading>
              </div>
            </Section>
          </Column>
        </Grid>

        {loading ? (
          <Loading />
        ) : (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <div style={{ marginBottom: "2rem" }}>
                  <h4>
                    <FormattedMessage id="program.information.label" />
                  </h4>
                  <div style={{ marginTop: "1rem" }}>
                    <p>
                      <strong>
                        <FormattedMessage id="program.name.label" />:
                      </strong>{" "}
                      {programmeDetails.program?.programName}
                    </p>
                    <p>
                      <strong>Code:</strong> {programmeDetails.program?.code}
                    </p>
                    <p>
                      <strong>UUID:</strong>{" "}
                      {programmeDetails.program?.questionnaireUUID}
                    </p>
                    {programmeDetails.testSectionName && (
                      <p>
                        <strong>
                          <FormattedMessage id="test.section.label" />:
                        </strong>{" "}
                        {programmeDetails.testSectionName}
                      </p>
                    )}
                  </div>
                </div>

                {programmeDetails.additionalOrderEntryQuestions && (
                  <div>
                    <h4>
                      <FormattedMessage id="program.questionnaire.label" />
                    </h4>
                    <div
                      style={{
                        backgroundColor: "#f4f4f4",
                        padding: "1rem",
                        borderRadius: "4px",
                        marginTop: "1rem",
                      }}
                    >
                      <pre
                        style={{
                          whiteSpace: "pre-wrap",
                          fontFamily: "monospace",
                          fontSize: "0.875rem",
                        }}
                      >
                        {typeof programmeDetails.additionalOrderEntryQuestions ===
                        "string"
                          ? programmeDetails.additionalOrderEntryQuestions
                          : JSON.stringify(
                              programmeDetails.additionalOrderEntryQuestions,
                              null,
                              2,
                            )}
                      </pre>
                    </div>
                  </div>
                )}
              </Section>
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
            <DataTable rows={rows} headers={headers} isSortable>
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
