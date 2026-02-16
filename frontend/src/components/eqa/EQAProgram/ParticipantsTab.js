import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
  Select,
  SelectItem,
  Search,
  Modal,
  TextArea,
  FilterableMultiSelect,
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
} from "../../utils/Utils";

const ENROLLMENT_STATUS_TAG = {
  Active: "green",
  Suspended: "gray",
  Withdrawn: "red",
};

const ParticipantsTab = ({ programs }) => {
  const intl = useIntl();
  const [selectedProgramId, setSelectedProgramId] = useState("");
  const [enrollments, setEnrollments] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [searchText, setSearchText] = useState("");
  const [enrollModalOpen, setEnrollModalOpen] = useState(false);
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [withdrawReason, setWithdrawReason] = useState("");
  const [selectedOrgs, setSelectedOrgs] = useState([]);
  const [notification, setNotification] = useState(null);

  const activePrograms = (programs || []).filter((p) => p.isActive);

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

  const fetchOrganizations = useCallback(() => {
    getFromOpenElisServer(
      "/rest/displaylist/referral-organizations",
      (data) => {
        if (data && Array.isArray(data)) {
          setOrganizations(data);
        }
      },
    );
  }, []);

  useEffect(() => {
    fetchEnrollments();
  }, [fetchEnrollments]);

  useEffect(() => {
    fetchOrganizations();
  }, [fetchOrganizations]);

  const handleEnrollSubmit = () => {
    if (selectedOrgs.length === 0 || !selectedProgramId) return;
    const orgIds = selectedOrgs.map((o) => o.id);
    postToOpenElisServerJsonResponse(
      `/rest/eqa/programs/${selectedProgramId}/enrollments`,
      JSON.stringify({ organizationIds: orgIds }),
      (response) => {
        if (response) {
          setEnrollModalOpen(false);
          setSelectedOrgs([]);
          setNotification({
            kind: "success",
            message: intl.formatMessage(
              { id: "eqa.enrollment.success" },
              { count: orgIds.length },
            ),
          });
          fetchEnrollments();
        }
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
          setWithdrawReason("");
          setSelectedEnrollment(null);
          fetchEnrollments();
        }
      },
    );
  };

  const filtered = enrollments.filter((e) => {
    const matchesStatus = !statusFilter || e.status === statusFilter;
    const matchesSearch =
      !searchText ||
      (e.organizationName || "")
        .toLowerCase()
        .includes(searchText.toLowerCase()) ||
      (e.organizationCode || "")
        .toLowerCase()
        .includes(searchText.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const enrolledOrgIds = new Set(
    enrollments
      .filter((e) => e.status === "Active")
      .map((e) => String(e.organizationId)),
  );

  const availableOrgs = organizations
    .filter((o) => !enrolledOrgIds.has(String(o.id)))
    .map((o) => ({ id: String(o.id), text: o.value || o.name || "" }));

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
    <div style={{ paddingTop: "1rem" }}>
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
            id="participant-program-selector"
            labelText={intl.formatMessage({
              id: "eqa.enrollment.selectProgram",
            })}
            value={selectedProgramId}
            onChange={(e) => setSelectedProgramId(e.target.value)}
          >
            <SelectItem value="" text="" />
            {activePrograms.map((p) => (
              <SelectItem key={p.id} value={String(p.id)} text={p.name} />
            ))}
          </Select>
        </div>
      </div>

      {!selectedProgramId ? (
        <div style={{ textAlign: "center", padding: "3rem 0" }}>
          <GroupPresentation size={48} style={{ color: "#c6c6c6" }} />
          <p style={{ color: "#525252", marginTop: "1rem" }}>
            {intl.formatMessage({ id: "eqa.enrollment.selectProgramPrompt" })}
          </p>
        </div>
      ) : (
        <>
          {/* Filters and Enroll Button */}
          <Grid condensed style={{ marginBottom: "1rem" }}>
            <Column lg={5} md={4} sm={4}>
              <Search
                id="enrollment-search"
                labelText=""
                placeholder={intl.formatMessage({
                  id: "eqa.enrollment.searchPlaceholder",
                })}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="enrollment-status-filter"
                labelText=""
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
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
            <Column lg={7} md={4} sm={4}>
              <div style={{ display: "flex", justifyContent: "flex-end" }}>
                <Button
                  renderIcon={Add}
                  onClick={() => setEnrollModalOpen(true)}
                >
                  {intl.formatMessage({
                    id: "eqa.enrollment.enrollParticipant",
                  })}
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Enrollments Table */}
          {filtered.length === 0 ? (
            <p style={{ color: "#525252", padding: "2rem 0" }}>
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
                <Table {...getTableProps()}>
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

      {/* Enroll Participant Modal */}
      <Modal
        open={enrollModalOpen}
        modalHeading={intl.formatMessage({
          id: "eqa.enrollment.enrollParticipant",
        })}
        primaryButtonText={intl.formatMessage(
          { id: "eqa.enrollment.enrollSelected" },
          { count: selectedOrgs.length },
        )}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => {
          setEnrollModalOpen(false);
          setSelectedOrgs([]);
        }}
        onRequestSubmit={handleEnrollSubmit}
        primaryButtonDisabled={selectedOrgs.length === 0}
        size="lg"
      >
        <p style={{ marginBottom: "1rem", color: "#525252" }}>
          {intl.formatMessage({ id: "eqa.enrollment.selectOrgsPrompt" })}
        </p>
        <FilterableMultiSelect
          id="org-multiselect"
          titleText={intl.formatMessage({
            id: "eqa.enrollment.organizationName",
          })}
          items={availableOrgs}
          itemToString={(item) => (item ? item.text : "")}
          onChange={(e) => setSelectedOrgs(e.selectedItems)}
          placeholder={intl.formatMessage({
            id: "eqa.enrollment.searchOrgs",
          })}
        />
        {selectedOrgs.length > 0 && (
          <p
            style={{
              marginTop: "0.5rem",
              fontSize: "0.875rem",
              color: "#0043ce",
              fontWeight: 500,
            }}
          >
            {selectedOrgs.length}{" "}
            {intl.formatMessage({ id: "eqa.enrollment.selected" })}
          </p>
        )}
      </Modal>

      {/* Withdraw Confirmation Modal */}
      <Modal
        open={withdrawModalOpen}
        danger
        modalHeading={intl.formatMessage({ id: "eqa.enrollment.withdraw" })}
        primaryButtonText={intl.formatMessage({
          id: "eqa.enrollment.withdraw",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => {
          setWithdrawModalOpen(false);
          setSelectedEnrollment(null);
          setWithdrawReason("");
        }}
        onRequestSubmit={() =>
          handleStatusChange(
            selectedEnrollment?.id,
            "Withdrawn",
            withdrawReason,
          )
        }
      >
        <p style={{ marginBottom: "1rem" }}>
          {intl.formatMessage({ id: "eqa.enrollment.confirmWithdraw" })}
        </p>
        {selectedEnrollment && (
          <p style={{ fontWeight: 600, marginBottom: "1rem" }}>
            {selectedEnrollment.organizationName} (
            {selectedEnrollment.organizationCode})
          </p>
        )}
        <TextArea
          id="withdraw-reason"
          labelText={intl.formatMessage({
            id: "eqa.enrollment.withdrawReason",
          })}
          value={withdrawReason}
          onChange={(e) => setWithdrawReason(e.target.value)}
          placeholder={intl.formatMessage({
            id: "eqa.enrollment.withdrawReasonPlaceholder",
          })}
        />
      </Modal>
    </div>
  );
};

export default ParticipantsTab;
