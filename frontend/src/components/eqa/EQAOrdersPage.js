import React, { useState, useEffect, useContext } from "react";
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
  Button,
  Select,
  SelectItem,
  OverflowMenu,
  OverflowMenuItem,
  Loading,
  Tile,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.tests", link: "" },
  { label: "eqa.tests.orders.title", link: "/EQAOrders" },
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

const EQAOrdersPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const { addNotification } = useContext(NotificationContext);

  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState({
    pending: 0,
    inProgress: 0,
    overdue: 0,
    completedThisMonth: 0,
  });
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [programFilter, setProgramFilter] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("");
  const [programs, setPrograms] = useState([]);

  useEffect(() => {
    fetchOrders();
    fetchSummary();
    fetchPrograms();
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [statusFilter, programFilter, priorityFilter]);

  const fetchOrders = () => {
    let url = "/rest/eqa/orders?";
    const params = [];
    if (statusFilter) params.push(`status=${statusFilter}`);
    if (programFilter) params.push(`programId=${programFilter}`);
    if (priorityFilter) params.push(`priority=${priorityFilter}`);
    url += params.join("&");

    getFromOpenElisServer(url, (data) => {
      setLoading(false);
      if (data) {
        setOrders(data);
      }
    });
  };

  const fetchSummary = () => {
    getFromOpenElisServer("/rest/eqa/orders/summary", (data) => {
      if (data) {
        setSummary(data);
      }
    });
  };

  const fetchPrograms = () => {
    getFromOpenElisServer("/rest/eqa/my-programs", (data) => {
      if (data) {
        setPrograms(data);
      }
    });
  };

  const headers = [
    {
      key: "labNumber",
      header: intl.formatMessage({ id: "eqa.tests.labNumber" }),
    },
    {
      key: "programName",
      header: intl.formatMessage({ id: "eqa.tests.program" }),
    },
    {
      key: "providerName",
      header: intl.formatMessage({ id: "eqa.tests.provider" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.tests.status" }),
    },
    {
      key: "deadline",
      header: intl.formatMessage({ id: "eqa.tests.deadline" }),
    },
    {
      key: "priority",
      header: intl.formatMessage({ id: "eqa.tests.priority" }),
    },
    {
      key: "dateEntered",
      header: intl.formatMessage({ id: "eqa.tests.dateEntered" }),
    },
  ];

  const rows = orders.map((order) => ({
    id: String(order.id),
    labNumber: order.labNumber || "",
    programName: order.programName || "",
    providerName: order.providerName || "",
    status: order.status || "",
    deadline: order.deadline
      ? new Date(order.deadline).toLocaleDateString()
      : "",
    priority: order.priority || "",
    dateEntered: order.dateEntered
      ? new Date(order.dateEntered).toLocaleDateString()
      : "",
  }));

  const handleEnterNewTest = () => {
    history.push("/SampleAdd?isEQA=true");
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage({ id: "eqa.tests.orders.title" })}
            </Heading>
            <p style={{ color: "#525252", marginBottom: "1rem" }}>
              {intl.formatMessage({ id: "eqa.tests.orders.subtitle" })}
            </p>
          </Section>

          <Grid condensed style={{ marginBottom: "1.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Tile>
                <h4 style={{ fontSize: "0.75rem", color: "#525252" }}>
                  {intl.formatMessage({ id: "eqa.orders.pending" })}
                </h4>
                <p style={{ fontSize: "1.75rem", fontWeight: 600 }}>
                  {summary.pending}
                </p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile>
                <h4 style={{ fontSize: "0.75rem", color: "#525252" }}>
                  {intl.formatMessage({ id: "eqa.orders.inProgress" })}
                </h4>
                <p style={{ fontSize: "1.75rem", fontWeight: 600 }}>
                  {summary.inProgress}
                </p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile>
                <h4 style={{ fontSize: "0.75rem", color: "#525252" }}>
                  {intl.formatMessage({ id: "eqa.orders.overdue" })}
                </h4>
                <p
                  style={{
                    fontSize: "1.75rem",
                    fontWeight: 600,
                    color: summary.overdue > 0 ? "#da1e28" : "inherit",
                  }}
                >
                  {summary.overdue}
                </p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile>
                <h4 style={{ fontSize: "0.75rem", color: "#525252" }}>
                  {intl.formatMessage({ id: "eqa.orders.completedThisMonth" })}
                </h4>
                <p style={{ fontSize: "1.75rem", fontWeight: 600 }}>
                  {summary.completedThisMonth}
                </p>
              </Tile>
            </Column>
          </Grid>

          <Grid condensed style={{ marginBottom: "1rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Select
                id="eqa-status-filter"
                labelText={intl.formatMessage({ id: "eqa.filter.status" })}
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "eqa.filter.allStatuses" })}
                />
                <SelectItem
                  value="PENDING"
                  text={intl.formatMessage({ id: "eqa.status.pending" })}
                />
                <SelectItem
                  value="IN_PROGRESS"
                  text={intl.formatMessage({ id: "eqa.status.inProgress" })}
                />
                <SelectItem
                  value="OVERDUE"
                  text={intl.formatMessage({ id: "eqa.status.overdue" })}
                />
                <SelectItem
                  value="COMPLETED"
                  text={intl.formatMessage({ id: "eqa.status.completed" })}
                />
              </Select>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Select
                id="eqa-program-filter"
                labelText={intl.formatMessage({ id: "eqa.filter.program" })}
                value={programFilter}
                onChange={(e) => setProgramFilter(e.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "eqa.filter.allPrograms" })}
                />
                {programs.map((p) => (
                  <SelectItem
                    key={p.id}
                    value={String(p.id)}
                    text={p.programName}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Select
                id="eqa-priority-filter"
                labelText={intl.formatMessage({ id: "eqa.filter.priority" })}
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "eqa.filter.allPriorities" })}
                />
                <SelectItem
                  value="STANDARD"
                  text={intl.formatMessage({ id: "eqa.priority.standard" })}
                />
                <SelectItem
                  value="URGENT"
                  text={intl.formatMessage({ id: "eqa.priority.urgent" })}
                />
                <SelectItem
                  value="CRITICAL"
                  text={intl.formatMessage({ id: "eqa.priority.critical" })}
                />
              </Select>
            </Column>
          </Grid>

          <DataTable rows={rows} headers={headers}>
            {({
              rows: tableRows,
              headers: hdrs,
              getTableProps,
              getHeaderProps,
              getRowProps,
              onInputChange,
            }) => (
              <TableContainer>
                <TableToolbar>
                  <TableToolbarContent>
                    <TableToolbarSearch
                      onChange={onInputChange}
                      placeholder={intl.formatMessage({
                        id: "eqa.orders.search.placeholder",
                      })}
                    />
                    <Button renderIcon={Add} onClick={handleEnterNewTest}>
                      {intl.formatMessage({
                        id: "eqa.orders.enterNewTest",
                      })}
                    </Button>
                  </TableToolbarContent>
                </TableToolbar>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {hdrs.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                      <TableHeader>
                        {intl.formatMessage({ id: "eqa.column.actions" })}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "status") {
                            return (
                              <TableCell key={cell.id}>
                                <Tag
                                  type={STATUS_TAG_MAP[cell.value] || "gray"}
                                  size="sm"
                                >
                                  {intl.formatMessage({
                                    id: `eqa.status.${(cell.value || "").toLowerCase().replace("_", "")}`,
                                    defaultMessage: cell.value,
                                  })}
                                </Tag>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "priority") {
                            return (
                              <TableCell key={cell.id}>
                                <Tag
                                  type={PRIORITY_TAG_MAP[cell.value] || "gray"}
                                  size="sm"
                                >
                                  {intl.formatMessage({
                                    id: `eqa.priority.${(cell.value || "").toLowerCase()}`,
                                    defaultMessage: cell.value,
                                  })}
                                </Tag>
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                        <TableCell>
                          <OverflowMenu flipped>
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "eqa.action.viewOrder",
                              })}
                            />
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "eqa.action.enterResults",
                              })}
                            />
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "eqa.action.viewResults",
                              })}
                            />
                          </OverflowMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </Column>
      </Grid>
    </div>
  );
};

export default EQAOrdersPage;
