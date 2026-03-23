import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Loading,
} from "@carbon/react";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../utils/Utils";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.mgmt", link: "" },
  { label: "eqa.results.title", link: "/EQAResults" },
];

const STATUS_TAG_MAP = {
  PENDING: "blue",
  IN_PROGRESS: "teal",
  OVERDUE: "red",
  COMPLETED: "green",
};

const PRIORITY_TAG_MAP = {
  STANDARD: "gray",
  URGENT: "magenta",
  CRITICAL: "red",
};

const EQAResultsPage = () => {
  const intl = useIntl();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    getFromOpenElisServer("/rest/eqa/orders", (data) => {
      if (data) {
        setOrders(data);
      }
      setLoading(false);
    });
  }, []);

  const headers = [
    {
      key: "labNumber",
      header: intl.formatMessage({ id: "eqa.results.col.labNumber" }),
    },
    {
      key: "programName",
      header: intl.formatMessage({ id: "eqa.results.col.programme" }),
    },
    {
      key: "providerName",
      header: intl.formatMessage({ id: "eqa.results.col.provider" }),
    },
    {
      key: "priority",
      header: intl.formatMessage({ id: "eqa.results.col.priority" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.results.col.status" }),
    },
    {
      key: "deadline",
      header: intl.formatMessage({ id: "eqa.results.col.deadline" }),
    },
  ];

  const filteredOrders = orders.filter((order) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (order.labNumber && order.labNumber.toLowerCase().includes(term)) ||
      (order.programName && order.programName.toLowerCase().includes(term)) ||
      (order.providerName && order.providerName.toLowerCase().includes(term))
    );
  });

  const rows = filteredOrders.map((order) => ({
    id: String(order.id),
    labNumber: order.labNumber || "",
    programName: order.programName || "",
    providerName: order.providerName || "",
    priority: order.priority || "",
    status: order.status || "",
    deadline: order.deadline
      ? new Date(order.deadline).toLocaleDateString()
      : "",
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>{intl.formatMessage({ id: "eqa.results.title" })}</Heading>
          </Section>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={rows} headers={headers} isSortable>
            {({
              rows,
              headers,
              getHeaderProps,
              getRowProps,
              getTableProps,
              getTableContainerProps,
            }) => (
              <TableContainer {...getTableContainerProps()}>
                <TableToolbar>
                  <TableToolbarContent>
                    <TableToolbarSearch
                      onChange={(e) => setSearchTerm(e.target.value || "")}
                      placeholder={intl.formatMessage({
                        id: "eqa.results.search.placeholder",
                      })}
                    />
                  </TableToolbarContent>
                </TableToolbar>
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
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ? (
                              <Tag type={STATUS_TAG_MAP[cell.value] || "gray"}>
                                {cell.value}
                              </Tag>
                            ) : cell.info.header === "priority" ? (
                              <Tag
                                type={PRIORITY_TAG_MAP[cell.value] || "gray"}
                              >
                                {cell.value}
                              </Tag>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          {rows.length === 0 && (
            <div style={{ padding: "2rem", textAlign: "center" }}>
              {intl.formatMessage({ id: "eqa.results.noResults" })}
            </div>
          )}
        </Column>
      </Grid>
    </>
  );
};

export default EQAResultsPage;
