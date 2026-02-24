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
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
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
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import InlineEnrollmentForm from "./InlineEnrollmentForm";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.tests", link: "" },
  { label: "eqa.myPrograms.title", link: "/EQAMyPrograms" },
];

const MyProgramsPage = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showNewForm, setShowNewForm] = useState(false);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    fetchEnrollments();
  }, []);

  const fetchEnrollments = () => {
    getFromOpenElisServer("/rest/eqa/my-programs", (data) => {
      setLoading(false);
      if (data) {
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

  const handleDeactivate = (enrollment) => {
    const payload = {
      programName: enrollment.programName,
      provider: enrollment.provider,
      description: enrollment.description,
      isActive: !enrollment.isActive,
      labUnitIds: (enrollment.labUnits || []).map((u) => u.id),
      testIds: (enrollment.tests || []).map((t) => t.id),
      panelIds: (enrollment.panels || []).map((p) => p.id),
    };
    putToOpenElisServer(
      `/rest/eqa/my-programs/${enrollment.id}`,
      JSON.stringify(payload),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            subtitle: intl.formatMessage({
              id: enrollment.isActive
                ? "eqa.enrollment.deactivated"
                : "eqa.enrollment.reactivated",
            }),
            message: "",
          });
          fetchEnrollments();
        }
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
      key: "status",
      header: intl.formatMessage({ id: "eqa.myPrograms.status" }),
    },
  ];

  const rows = enrollments.map((e) => ({
    id: String(e.id),
    programName: e.programName || "",
    provider: e.provider || "",
    labUnits: (e.labUnits || []).length,
    tests: (e.tests || []).length + (e.panels || []).length,
    status: e.isActive ? "Active" : "Inactive",
  }));

  const getEnrollmentById = (id) => {
    return enrollments.find((e) => String(e.id) === String(id));
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
                    {tableRows.map((row) => {
                      const enrollment = getEnrollmentById(row.id);
                      return (
                        <React.Fragment key={row.id}>
                          <TableRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "status") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Tag
                                      type={
                                        cell.value === "Active"
                                          ? "green"
                                          : "gray"
                                      }
                                      size="sm"
                                    >
                                      {intl.formatMessage({
                                        id:
                                          cell.value === "Active"
                                            ? "eqa.status.active"
                                            : "eqa.status.inactive",
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
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id:
                                      enrollment && enrollment.isActive
                                        ? "eqa.action.deactivate"
                                        : "eqa.action.reactivate",
                                  })}
                                  onClick={() =>
                                    enrollment && handleDeactivate(enrollment)
                                  }
                                />
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
    </div>
  );
};

export default MyProgramsPage;
