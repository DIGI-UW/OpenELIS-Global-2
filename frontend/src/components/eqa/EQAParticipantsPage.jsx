import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Select,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
  InlineNotification,
} from "@carbon/react";
import {
  Add,
  PauseOutline,
  StopOutline,
  Renew,
  Certificate,
  GroupPresentation,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../utils/Utils";
import PageBreadCrumb from "../common/PageBreadCrumb";
import WithdrawModal from "./WithdrawModal";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.mgmt", link: "" },
  { label: "eqa.management.participants.title", link: "/EQAParticipants" },
];

const ENROLLMENT_STATUS_TAG = {
  Active: "green",
  Suspended: "gray",
  Withdrawn: "red",
};

const EQAParticipantsPage = () => {
  const intl = useIntl();
  const [programs, setPrograms] = useState([]);
  const [selectedProgramId, setSelectedProgramId] = useState("");
  const [enrollments, setEnrollments] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [selectedOrgId, setSelectedOrgId] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    getFromOpenElisServer("/rest/eqa/programs", (data) => {
      if (data && Array.isArray(data)) {
        setPrograms(data.filter((p) => p.isActive));
      }
    });
    getFromOpenElisServer("/rest/organization-list", (data) => {
      if (data && Array.isArray(data)) {
        const active = data
          .filter((o) => o.isActive === "Y")
          .map((o) => ({ id: o.id, value: o.organizationName }));
        setOrganizations(active);
      }
    });
  }, []);

  const fetchEnrollments = useCallback(() => {
    if (!selectedProgramId) {
      setEnrollments([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/eqa/programs/${selectedProgramId}/enrollments`,
      (data) => {
        if (data && Array.isArray(data)) {
          setEnrollments(data);
        } else {
          setEnrollments([]);
        }
      },
    );
  }, [selectedProgramId]);

  useEffect(() => {
    fetchEnrollments();
  }, [fetchEnrollments]);

  const handleEnrollOrg = () => {
    if (!selectedOrgId || !selectedProgramId) return;
    postToOpenElisServerJsonResponse(
      `/rest/eqa/programs/${selectedProgramId}/enrollments`,
      JSON.stringify({ organizationIds: [Number(selectedOrgId)] }),
      (response) => {
        if (response && !response.error) {
          setSelectedOrgId("");
          setNotification({
            kind: "success",
            message: intl.formatMessage(
              { id: "eqa.enrollment.success" },
              { count: 1 },
            ),
          });
        } else {
          setNotification({
            kind: "error",
            message: response?.error || "Failed to enroll organization",
          });
        }
        fetchEnrollments();
      },
    );
  };

  const handleStatusChange = (enrollmentId, newStatus, reason) => {
    putToOpenElisServer(
      `/rest/eqa/programs/${selectedProgramId}/enrollments/${enrollmentId}`,
      JSON.stringify({ status: newStatus, reason: reason || "" }),
      (status) => {
        if (status === 200) {
          setWithdrawModalOpen(false);
          setSelectedEnrollment(null);
          fetchEnrollments();
        }
      },
    );
  };

  const filtered = enrollments.filter((e) => {
    return !statusFilter || e.status === statusFilter;
  });

  const headers = [
    {
      key: "organizationName",
      header: intl.formatMessage({ id: "eqa.enrollment.organizationName" }),
    },
    {
      key: "organizationCode",
      header: intl.formatMessage({ id: "eqa.enrollment.organizationCode" }),
    },
    {
      key: "district",
      header: intl.formatMessage({ id: "eqa.enrollment.district" }),
    },
    {
      key: "enrollmentDate",
      header: intl.formatMessage({ id: "eqa.enrollment.enrollmentDate" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.enrollment.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "column.actions" }),
    },
  ];

  const rows = filtered.map((e) => ({
    id: String(e.id),
    organizationName: e.organizationName || "",
    organizationCode: e.organizationCode || "",
    district: e.district || "",
    enrollmentDate: e.enrollmentDate
      ? new Date(e.enrollmentDate).toLocaleDateString()
      : "",
    status: e.status || "Active",
    isLocal: e.isLocal || false,
    _raw: e,
  }));

  return (
    <div className="pageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage({
                id: "eqa.management.participants.title",
              })}
            </Heading>
            <p style={{ color: "#525252", marginBottom: "1rem" }}>
              {intl.formatMessage({
                id: "eqa.management.participants.subtitle",
              })}
            </p>
          </Section>
        </Column>
      </Grid>

      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.message}
          onCloseButtonClick={() => setNotification(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Program Selector */}
      <div
        style={{
          display: "flex",
          alignItems: "flex-end",
          gap: "1rem",
          padding: "1rem",
          backgroundColor: "#e0f2f1",
          borderRadius: "8px",
          border: "1px solid rgba(0,105,92,0.2)",
          marginBottom: "1.5rem",
        }}
      >
        <Certificate
          size={20}
          style={{ color: "#00695c", marginBottom: "0.5rem" }}
        />
        <div style={{ flex: 1, maxWidth: "400px" }}>
          <Select
            id="program-selector"
            labelText={intl.formatMessage({
              id: "eqa.enrollment.selectProgram",
            })}
            value={selectedProgramId}
            onChange={(e) => setSelectedProgramId(e.target.value)}
            data-testid="program-selector"
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "eqa.enrollment.selectProgram",
              })}
            />
            {programs.map((p) => (
              <SelectItem key={p.id} value={String(p.id)} text={p.name} />
            ))}
          </Select>
        </div>
      </div>

      {!selectedProgramId ? (
        <div
          style={{ textAlign: "center", padding: "3rem 0" }}
          data-testid="select-program-prompt"
        >
          <GroupPresentation size={48} style={{ color: "#c6c6c6" }} />
          <p style={{ color: "#525252", marginTop: "1rem" }}>
            {intl.formatMessage({ id: "eqa.enrollment.selectProgramPrompt" })}
          </p>
        </div>
      ) : (
        <>
          {/* Filters and Enroll Button */}
          <Grid condensed style={{ marginBottom: "1rem" }}>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="enrollment-status-filter"
                labelText=""
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                data-testid="enrollment-status-filter"
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "eqa.enrollment.allStatuses",
                  })}
                />
                <SelectItem
                  value="Active"
                  text={intl.formatMessage({
                    id: "eqa.enrollment.status.active",
                  })}
                />
                <SelectItem
                  value="Suspended"
                  text={intl.formatMessage({
                    id: "eqa.enrollment.status.suspended",
                  })}
                />
                <SelectItem
                  value="Withdrawn"
                  text={intl.formatMessage({
                    id: "eqa.enrollment.status.withdrawn",
                  })}
                />
              </Select>
            </Column>
            <Column lg={5} md={4} sm={4}>
              <Select
                id="enroll-org-select"
                labelText=""
                value={selectedOrgId}
                onChange={(e) => setSelectedOrgId(e.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "eqa.enrollment.selectOrg",
                  })}
                />
                {organizations
                  .filter(
                    (o) =>
                      !enrollments.some(
                        (e) =>
                          e.status === "Active" &&
                          String(e.organizationId) === String(o.id),
                      ),
                  )
                  .map((o) => (
                    <SelectItem
                      key={o.id}
                      value={String(o.id)}
                      text={o.value}
                    />
                  ))}
              </Select>
            </Column>
            <Column lg={3} md={2} sm={4}>
              <Button
                renderIcon={Add}
                onClick={handleEnrollOrg}
                disabled={!selectedOrgId}
                data-testid="enroll-button"
                size="md"
              >
                {intl.formatMessage({
                  id: "eqa.enrollment.enroll",
                })}
              </Button>
            </Column>
          </Grid>

          {/* Enrollments Table */}
          {filtered.length === 0 ? (
            <p
              style={{ color: "#525252", padding: "2rem 0" }}
              data-testid="empty-message"
            >
              {intl.formatMessage({ id: "eqa.enrollment.empty" })}
            </p>
          ) : (
            <DataTable rows={rows} headers={headers}>
              {({
                rows: tableRows,
                headers: tableHeaders,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()} data-testid="enrollments-table">
                  <TableHead>
                    <TableRow>
                      {tableHeaders.map((header) => (
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
                    {tableRows.map((row) => {
                      const rawRow = rows.find((r) => r.id === row.id);
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => {
                            if (cell.info.header === "organizationName") {
                              return (
                                <TableCell key={cell.id}>
                                  <div
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.5rem",
                                    }}
                                  >
                                    <span style={{ fontWeight: 500 }}>
                                      {cell.value}
                                    </span>
                                    {rawRow?.isLocal && (
                                      <Tag type="teal" size="sm">
                                        {intl.formatMessage({
                                          id: "eqa.enrollment.thisLab",
                                        })}
                                      </Tag>
                                    )}
                                  </div>
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "organizationCode") {
                              return (
                                <TableCell key={cell.id}>
                                  <span
                                    style={{
                                      fontFamily: "monospace",
                                      fontSize: "0.85rem",
                                    }}
                                  >
                                    {cell.value}
                                  </span>
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "status") {
                              const tagType =
                                ENROLLMENT_STATUS_TAG[cell.value] || "gray";
                              return (
                                <TableCell key={cell.id}>
                                  <Tag type={tagType} size="sm">
                                    {intl.formatMessage({
                                      id: `eqa.enrollment.status.${(cell.value || "active").toLowerCase()}`,
                                      defaultMessage: cell.value,
                                    })}
                                  </Tag>
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "actions") {
                              const enrollment = rawRow?._raw;
                              return (
                                <TableCell key={cell.id}>
                                  <div
                                    style={{
                                      display: "flex",
                                      gap: "0.25rem",
                                    }}
                                  >
                                    {rawRow?.status === "Active" && (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        iconDescription={intl.formatMessage({
                                          id: "eqa.enrollment.suspend",
                                        })}
                                        renderIcon={PauseOutline}
                                        onClick={() =>
                                          handleStatusChange(
                                            enrollment?.id,
                                            "Suspended",
                                          )
                                        }
                                      />
                                    )}
                                    {rawRow?.status !== "Withdrawn" && (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        iconDescription={intl.formatMessage({
                                          id: "eqa.enrollment.withdraw",
                                        })}
                                        renderIcon={StopOutline}
                                        onClick={() => {
                                          setSelectedEnrollment(enrollment);
                                          setWithdrawModalOpen(true);
                                        }}
                                      />
                                    )}
                                    {rawRow?.status === "Suspended" && (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        hasIconOnly
                                        iconDescription={intl.formatMessage({
                                          id: "eqa.enrollment.reactivate",
                                        })}
                                        renderIcon={Renew}
                                        onClick={() =>
                                          handleStatusChange(
                                            enrollment?.id,
                                            "Active",
                                          )
                                        }
                                      />
                                    )}
                                  </div>
                                </TableCell>
                              );
                            }
                            return (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            );
                          })}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          )}
        </>
      )}

      <WithdrawModal
        open={withdrawModalOpen}
        enrollment={selectedEnrollment}
        onClose={() => {
          setWithdrawModalOpen(false);
          setSelectedEnrollment(null);
        }}
        onConfirm={(enrollmentId, reason) =>
          handleStatusChange(enrollmentId, "Withdrawn", reason)
        }
      />
    </div>
  );
};

export default EQAParticipantsPage;
