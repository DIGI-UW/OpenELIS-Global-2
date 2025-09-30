import React, { useEffect, useState, useRef, useContext } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import {
  Grid,
  Column,
  Tile,
  Button,
  Section,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Checkbox,
  Select,
  SelectItem,
  Heading,
  Loading,
  Pagination,
} from "@carbon/react";
import { Search } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";

function GeneralProgrammeDashboard() {
  const [orderEntries, setOrderEntries] = useState([]);
  const [selectedProgramme, setSelectedProgramme] = useState(null);
  const [orderEntriesLoading, setOrderEntriesLoading] = useState(false);
  const componentMounted = useRef(false);
  const { notificationVisible } = useContext(NotificationContext);
  const [programmes, setProgrammes] = useState([]);
  const [filteredProgrammes, setFilteredProgrammes] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const intl = useIntl();
  const history = useHistory();

  useEffect(() => {
    componentMounted.current = true;
    fetch("/rest/programs/general")
      .then((res) => res.json())
      .then((data) => {
        if (componentMounted.current) {
          setProgrammes(data.map((p) => ({ ...p, id: p.id.toString() })));
          setLoading(false);
        }
      });
    return () => {
      componentMounted.current = false;
    };
  }, []);
  fetch("/rest/programs/general")
    .then((res) => {
      if (!res.ok) throw new Error("Failed to fetch programmes");
      return res.json();
    })
    .then((data) => {
      if (componentMounted.current) {
        setProgrammes(data.map((p) => ({ ...p, id: p.id.toString() })));
      }
    })
    .catch(() => {})
    .finally(() => {
      if (componentMounted.current) setLoading(false);
    });

  useEffect(() => {
    if (searchTerm) {
      setFilteredProgrammes(
        programmes.filter((p) =>
          p.name.toLowerCase().includes(searchTerm.toLowerCase()),
        ),
      );
    } else {
      setFilteredProgrammes(programmes);
    }
  }, [searchTerm, programmes]);

  const handleProgrammeClick = (programme) => {
    setSelectedProgramme(programme);
    setOrderEntries([]);
    setOrderEntriesLoading(false);
    fetch(`/rest/program/${programme.id}/orders`)
      .then((res) => res.json())
      .then((data) => {
        setOrderEntries(data);
        setOrderEntriesLoading(false);
      })
      .catch(() => setOrderEntriesLoading(false));
  };

  const breadcrumbs = [
    {
      label: "banner.menu.generalProgramme",
      link: "/GeneralProgrammeDashboard",
    },
  ];

  const tileList = [
    {
      title: intl.formatMessage({ id: "banner.menu.generalProgramme" }),
      count: filteredProgrammes.length,
    },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Dashboard..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="banner.menu.generalProgramme" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container">
        {tileList.map((tile, index) => (
          <Tile key={index} className="dashboard-tile">
            <h3 className="tile-title">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
      </div>
      <div className="orderLegendBody">
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={8} md={4} sm={2}>
            <Search
              size="sm"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={intl.formatMessage({
                id: "label.search.labno.family",
              })}
              labelText={intl.formatMessage({
                id: "label.search.labno.family",
              })}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={filteredProgrammes.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              headers={[
                {
                  key: "name",
                  header: intl.formatMessage({
                    id: "admin.page.configuration.formEntryConfigMenu.name",
                  }),
                },
                {
                  key: "description",
                  header: intl.formatMessage({
                    id: "admin.page.configuration.formEntryConfigMenu.description",
                  }),
                },
              ]}
              isSortable
            >
              {({ rows, headers, getHeaderProps, getTableProps }) => (
                <TableContainer title="" description="">
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
                        <TableHeader>
                          <FormattedMessage id="label.button.view" />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                          <TableCell>
                            <Button
                              kind="primary"
                              size="sm"
                              onClick={() => handleProgrammeClick(row)}
                            >
                              <FormattedMessage id="label.button.view" />
                            </Button>

                            {selectedProgramme && (
                              <div style={{ marginTop: 32 }}>
                                <h3>
                                  {intl.formatMessage({
                                    id: "label.order.entries.for",
                                  })}
                                  :{" "}
                                  {selectedProgramme.name ||
                                    selectedProgramme.programName}
                                </h3>
                                {orderEntriesLoading ? (
                                  <Loading description="Loading Order Entries..." />
                                ) : orderEntries.length === 0 ? (
                                  <p>
                                    No order entries found for this programme.
                                  </p>
                                ) : (
                                  <DataTable
                                    rows={orderEntries}
                                    headers={[
                                      { key: "orderId", header: "Order ID" },
                                      {
                                        key: "patientName",
                                        header: "Patient Name",
                                      },
                                      {
                                        key: "orderDate",
                                        header: "Order Date",
                                      },
                                      { key: "status", header: "Status" },
                                      {
                                        key: "accessionNumber",
                                        header: "Accession Number",
                                      },
                                    ]}
                                    isSortable
                                  >
                                    {({
                                      rows,
                                      headers,
                                      getHeaderProps,
                                      getTableProps,
                                    }) => (
                                      <TableContainer title="" description="">
                                        <Table {...getTableProps()}>
                                          <TableHead>
                                            <TableRow>
                                              {headers.map((header) => (
                                                <TableHeader
                                                  key={header.key}
                                                  {...getHeaderProps({
                                                    header,
                                                  })}
                                                >
                                                  {header.header}
                                                </TableHeader>
                                              ))}
                                            </TableRow>
                                          </TableHead>
                                          <TableBody>
                                            {rows.map((row) => (
                                              <TableRow
                                                key={row.id || row.orderId}
                                              >
                                                {row.cells.map((cell) => (
                                                  <TableCell key={cell.id}>
                                                    {cell.value}
                                                  </TableCell>
                                                ))}
                                              </TableRow>
                                            ))}
                                          </TableBody>
                                        </Table>
                                      </TableContainer>
                                    )}
                                  </DataTable>
                                )}
                              </div>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
            <Pagination
              onChange={({ page, pageSize }) => {
                setPage(page);
                setPageSize(pageSize);
              }}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={filteredProgrammes.length}
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
      </div>
    </>
  );
}
export default GeneralProgrammeDashboard;
