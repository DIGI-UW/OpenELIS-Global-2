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
  OverflowMenu,
  OverflowMenuItem,
  Loading,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  putToOpenElisServerFullResponse,
  resolveApiErrorMessage,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import InlineEnrollmentForm from "./InlineEnrollmentForm";
import EnrollmentStatusModal from "./EnrollmentStatusModal";

/** The heading each status group carries, and the colour its tag takes. */
const STATUS_GROUP_KEY = {
  Active: "eqa.myPrograms.group.active",
  Suspended: "eqa.myPrograms.group.suspended",
  Withdrawn: "eqa.myPrograms.group.withdrawn",
};

const STATUS_LABEL_KEY = {
  Active: "eqa.enrollment.status.active",
  Suspended: "eqa.enrollment.status.suspended",
  Withdrawn: "eqa.enrollment.status.withdrawn",
};

const STATUS_TAG_TYPE = {
  Active: "green",
  Suspended: "cyan",
  Withdrawn: "gray",
};

/** Where an enrolment in each status may go next. */
const NEXT_STATUSES = {
  Active: ["Suspended", "Withdrawn"],
  Suspended: ["Active", "Withdrawn"],
  Withdrawn: [],
};

const STATUS_ACTION_KEY = {
  Active: "eqa.enrollment.resume",
  Suspended: "eqa.enrollment.suspend",
  Withdrawn: "eqa.enrollment.withdraw",
};

const breadcrumbs = [
  { label: "home.label", link: "/" },
  // qa-076 deactivated menu_eqa_tests when the group was flattened; My Programs
  // now sits directly under EQA.
  { label: "banner.menu.eqa", link: "" },
  { label: "eqa.myPrograms.title", link: "/EQAMyPrograms" },
];

const MyProgramsPage = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showNewForm, setShowNewForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [statusChange, setStatusChange] = useState(null);

  useEffect(() => {
    fetchEnrollments();
  }, []);

  const fetchEnrollments = () => {
    getFromOpenElisServer("/rest/eqa/my-programs", (data) => {
      setLoading(false);
      if (data && Array.isArray(data)) {
        setEnrollments(data);
      }
    });
  };

  const handleCreate = (payload) => {
    postToOpenElisServerJsonResponse(
      "/rest/eqa/my-programs",
      JSON.stringify(payload),
      (response) => {
        if (response && !response.error) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: intl.formatMessage({
              id: "eqa.enrollment.created.success",
            }),
            message: "",
          });
          setShowNewForm(false);
          fetchEnrollments();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle:
              response?.error ||
              intl.formatMessage({ id: "error.save.failed" }),
            message: "",
          });
        }
      },
    );
  };

  const handleUpdate = (id, payload) => {
    putToOpenElisServer(
      `/rest/eqa/my-programs/${id}`,
      JSON.stringify(payload),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: intl.formatMessage({
              id: "eqa.enrollment.updated.success",
            }),
            message: "",
          });
          setEditingId(null);
          fetchEnrollments();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: intl.formatMessage({ id: "error.save.failed" }),
            message: "",
          });
        }
      },
    );
  };

  const handleStatusChange = (reason, effectiveDate) => {
    const { enrollment, nextStatus } = statusChange;
    putToOpenElisServerFullResponse(
      `/rest/eqa/my-programs/${enrollment.id}/status`,
      JSON.stringify({ status: nextStatus, reason, effectiveDate }),
      (response) => {
        setStatusChange(null);
        if (response && response.ok) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: intl.formatMessage(
              { id: "eqa.enrollment.statusChanged" },
              {
                status: intl.formatMessage({
                  id: STATUS_LABEL_KEY[nextStatus],
                }),
              },
            ),
            message: "",
          });
          fetchEnrollments();
          return;
        }
        const report = (body) =>
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: resolveApiErrorMessage(
              intl,
              body,
              "eqa.enrollment.statusChangeFailed",
            ),
            message: "",
          });
        if (!response) {
          report(null);
          return;
        }
        response
          .json()
          .then(report)
          .catch(() => report(null));
      },
    );
  };

  const headers = [
    {
      key: "programName",
      header: intl.formatMessage({ id: "eqa.myPrograms.programName" }),
    },
    {
      key: "provider",
      header: intl.formatMessage({ id: "eqa.myPrograms.provider" }),
    },
    {
      key: "labUnits",
      header: intl.formatMessage({ id: "eqa.myPrograms.labUnits" }),
    },
    {
      key: "tests",
      header: intl.formatMessage({ id: "eqa.myPrograms.tests" }),
    },
    {
      key: "panels",
      header: intl.formatMessage({ id: "eqa.myPrograms.panels" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.myPrograms.status" }),
    },
  ];

  const rows = enrollments.map((e) => ({
    id: String(e.id),
    programName: e.programName || "",
    provider: e.provider || "",
    labUnits: (e.labUnits || []).length,
    tests: (e.tests || []).length,
    panels: (e.panels || []).length,
    // An enrolment made before the lifecycle columns existed is read from the
    // flag the rest of the module still keys on.
    status: e.status || (e.isActive ? "Active" : "Suspended"),
  }));

  const statusOf = (row) =>
    row.cells.find((c) => c.info.header === "status")?.value;

  const getEnrollmentById = (id) => {
    return enrollments.find((e) => String(e.id) === String(id));
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="pageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage({ id: "eqa.myPrograms.title" })}
            </Heading>
            <p style={{ color: "#525252", marginBottom: "1rem" }}>
              {intl.formatMessage({ id: "eqa.myPrograms.subtitle" })}
            </p>
          </Section>

          {showNewForm && (
            <div style={{ marginBottom: "1rem" }}>
              <InlineEnrollmentForm
                enrollment={null}
                enrollments={enrollments}
                onSave={handleCreate}
                onCancel={() => setShowNewForm(false)}
              />
            </div>
          )}

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
                        id: "eqa.myPrograms.search.placeholder",
                      })}
                    />
                    <Button
                      renderIcon={Add}
                      onClick={() => {
                        setShowNewForm(true);
                        setEditingId(null);
                      }}
                      disabled={showNewForm}
                    >
                      {intl.formatMessage({
                        id: "eqa.myPrograms.enrollInProgram",
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
                    {tableRows.map((row, index) => {
                      const enrollment = getEnrollmentById(row.id);
                      // The rows arrive grouped, active first, so a heading is due
                      // wherever the status changes from the row above.
                      const status = statusOf(row);
                      const startsGroup =
                        index === 0 ||
                        statusOf(tableRows[index - 1]) !== status;
                      return (
                        <React.Fragment key={row.id}>
                          {startsGroup && (
                            <TableRow>
                              <TableCell
                                colSpan={hdrs.length + 1}
                                className="eqa-my-programs-group"
                              >
                                <strong>
                                  {intl.formatMessage(
                                    { id: STATUS_GROUP_KEY[status] },
                                    {
                                      count: tableRows.filter(
                                        (r) => statusOf(r) === status,
                                      ).length,
                                    },
                                  )}
                                </strong>
                              </TableCell>
                            </TableRow>
                          )}
                          <TableRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "status") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Tag
                                      type={STATUS_TAG_TYPE[cell.value]}
                                      size="sm"
                                    >
                                      {intl.formatMessage({
                                        id: STATUS_LABEL_KEY[cell.value],
                                      })}
                                    </Tag>
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "labUnits") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value > 0 ? (
                                      <Tag type="blue" size="sm">
                                        {cell.value}
                                      </Tag>
                                    ) : (
                                      "—"
                                    )}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "tests") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value > 0 ? (
                                      <Tag type="teal" size="sm">
                                        {cell.value}
                                      </Tag>
                                    ) : (
                                      "—"
                                    )}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "panels") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value > 0 ? (
                                      <Tag type="purple" size="sm">
                                        {cell.value}
                                      </Tag>
                                    ) : (
                                      "—"
                                    )}
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                            <TableCell>
                              <OverflowMenu flipped>
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "eqa.action.edit",
                                  })}
                                  onClick={() => {
                                    setEditingId(row.id);
                                    setShowNewForm(false);
                                  }}
                                />
                                {(NEXT_STATUSES[status] || []).map(
                                  (nextStatus) => (
                                    <OverflowMenuItem
                                      key={nextStatus}
                                      isDelete={nextStatus === "Withdrawn"}
                                      itemText={intl.formatMessage({
                                        id: STATUS_ACTION_KEY[nextStatus],
                                      })}
                                      onClick={() =>
                                        enrollment &&
                                        setStatusChange({
                                          enrollment,
                                          nextStatus,
                                        })
                                      }
                                    />
                                  ),
                                )}
                              </OverflowMenu>
                            </TableCell>
                          </TableRow>
                          {editingId === row.id && enrollment && (
                            <TableRow>
                              <TableCell
                                colSpan={hdrs.length + 1}
                                style={{ padding: 0 }}
                              >
                                <InlineEnrollmentForm
                                  enrollment={enrollment}
                                  enrollments={enrollments}
                                  onSave={(payload) =>
                                    handleUpdate(enrollment.id, payload)
                                  }
                                  onCancel={() => setEditingId(null)}
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
          </DataTable>
        </Column>
      </Grid>

      {statusChange && (
        <EnrollmentStatusModal
          enrollment={statusChange.enrollment}
          nextStatus={statusChange.nextStatus}
          onClose={() => setStatusChange(null)}
          onConfirm={handleStatusChange}
        />
      )}
    </div>
  );
};

export default MyProgramsPage;
