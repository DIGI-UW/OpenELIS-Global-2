import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  Select,
  SelectItem,
  Grid,
  Column,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Checkbox,
  Modal,
  TextInput,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
} from "@carbon/react";
import { Plus, MoreVertical, Search } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function ProgramsTab({ unit }) {
  const intl = useIntl();
  const [assignedPrograms, setAssignedPrograms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [availablePrograms, setAvailablePrograms] = useState([]);
  const [expandedRows, setExpandedRows] = useState({});
  const [programDetails, setProgramDetails] = useState({});

  useEffect(() => {
    fetchAssignedPrograms();
    fetchAvailablePrograms();
  }, [unit]);

  const toggleRowExpansion = async (programId) => {
    const newExpandedState = !expandedRows[programId];
    setExpandedRows((prev) => ({
      ...prev,
      [programId]: newExpandedState,
    }));

    // Fetch program details if expanding and we don't have them yet
    if (newExpandedState && !programDetails[programId]) {
      try {
        const response = await fetch(`/rest/api/programs/${programId}/details`);
        if (response.ok) {
          const data = await response.json();
          setProgramDetails((prev) => ({
            ...prev,
            [programId]: data,
          }));
        }
      } catch (error) {
        console.error("Error fetching program details:", error);
      }
    }
  };

  const fetchAssignedPrograms = async () => {
    if (!unit) return;

    setLoading(true);
    try {
      const response = await fetch(`/rest/api/lab-units/${unit.id}/programs`);
      if (response.ok) {
        const data = await response.json();
        setAssignedPrograms(data || []);
      }
    } catch (error) {
      console.error("Error fetching assigned programs:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch available programs for assignment
  const fetchAvailablePrograms = async () => {
    try {
      const response = await fetch("/rest/api/programs/available");
      if (response.ok) {
        const data = await response.json();
        setAvailablePrograms(data.programs || []);
      }
    } catch (error) {
      console.error("Error fetching available programs:", error);
    }
  };

  const getPatientCountDisplay = (patientCount) => {
    return patientCount.toLocaleString();
  };

  const handleAssignPrograms = () => {
    setShowAssignModal(true);
  };

  return (
    <div style={{ padding: "1rem" }}>
      <Grid fullWidth>
        <Column lg={16}>
          {/* Header */}
          <div
            style={{
              marginBottom: "2rem",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "flex-start",
              flexDirection: "column",
              gap: "1rem",
            }}
          >
            <div>
              <h3
                style={{
                  marginBottom: "0.5rem",
                  fontSize: "1.25rem",
                  fontWeight: "600",
                  color: "#161616",
                }}
              >
                {intl.formatMessage({ id: "labunit.programs.assigned" })}
              </h3>
              <p
                style={{
                  margin: 0,
                  color: "#525252",
                  lineHeight: "1.5",
                  fontSize: "0.875rem",
                }}
              >
                {intl.formatMessage({ id: "labunit.programs.description" })}
              </p>
            </div>

            <Button
              renderIcon={Plus}
              onClick={handleAssignPrograms}
              disabled={!unit}
            >
              {intl.formatMessage({ id: "button.assign.programs" })}
            </Button>
          </div>

          {/* Programs Table */}
          <div
            style={{
              backgroundColor: "#ffffff",
              borderRadius: "4px",
              border: "1px solid #e0e0e0",
              overflow: "hidden",
            }}
          >
            {loading ? (
              <div
                style={{
                  padding: "3rem",
                  textAlign: "center",
                  color: "#525252",
                }}
              >
                <p>{intl.formatMessage({ id: "loading.programs" })}</p>
              </div>
            ) : (
              <DataTable
                rows={assignedPrograms}
                expandable
                render={({
                  rows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                  getTableProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          <TableExpandHeader />
                          <TableHeader
                            {...getHeaderProps({ header: headers[0] })}
                          >
                            {intl.formatMessage({ id: "program.name" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[1] })}
                          >
                            {intl.formatMessage({ id: "program.code" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[2] })}
                          >
                            {intl.formatMessage({
                              id: "program.order.entry.form",
                            })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[3] })}
                          >
                            {intl.formatMessage({
                              id: "program.active.patients",
                            })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[4] })}
                          >
                            {intl.formatMessage({ id: "program.status" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[5] })}
                          >
                            {intl.formatMessage({ id: "program.actions" })}
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row, index) => (
                          <React.Fragment key={row.id}>
                            <TableExpandRow
                              {...getRowProps({ row })}
                              isExpanded={expandedRows[row.id]}
                              onExpand={() => toggleRowExpansion(row.id)}
                            >
                              <TableCell>
                                <span
                                  style={{
                                    fontWeight: "500",
                                    color: "#161616",
                                  }}
                                >
                                  {row.cells[0].value}
                                </span>
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    fontFamily: "monospace",
                                    fontSize: "0.875rem",
                                    color: "#525252",
                                    backgroundColor: "#f0f0f0",
                                    padding: "0.125rem 0.25rem",
                                    borderRadius: "2px",
                                  }}
                                >
                                  {row.cells[1].value}
                                </span>
                              </TableCell>
                              <TableCell>
                                <a
                                  href="#"
                                  style={{
                                    color: "#0f62fe",
                                    textDecoration: "none",
                                    fontSize: "0.875rem",
                                    "&:hover": {
                                      textDecoration: "underline",
                                    },
                                  }}
                                  onClick={(e) => e.preventDefault()}
                                >
                                  {row.cells[2].value}
                                </a>
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    fontSize: "0.875rem",
                                    fontWeight: "500",
                                    color: "#161616",
                                  }}
                                >
                                  {row.cells[3].value}
                                </span>
                              </TableCell>
                              <TableCell>
                                <Tag
                                  type={row.cells[4].value ? "green" : "gray"}
                                >
                                  {row.cells[4].value
                                    ? intl.formatMessage({
                                        id: "program.status.active",
                                      })
                                    : intl.formatMessage({
                                        id: "program.status.inactive",
                                      })}
                                </Tag>
                              </TableCell>
                              <TableCell>
                                <div
                                  style={{
                                    display: "flex",
                                    gap: "0.5rem",
                                    alignItems: "center",
                                  }}
                                >
                                  <Button kind="ghost" size="sm">
                                    {intl.formatMessage({
                                      id: "button.configure",
                                    })}
                                  </Button>

                                  <OverflowMenu size="sm">
                                    <OverflowMenuItem>
                                      <MoreVertical size={16} />
                                      {intl.formatMessage({
                                        id: "button.more.actions",
                                      })}
                                    </OverflowMenuItem>
                                    <OverflowMenuItem>
                                      {intl.formatMessage({
                                        id: "button.remove",
                                      })}
                                    </OverflowMenuItem>
                                  </OverflowMenu>
                                </div>
                              </TableCell>
                            </TableExpandRow>
                            {expandedRows[row.id] && (
                              <TableExpandedRow colSpan={7}>
                                <div
                                  style={{
                                    padding: "1rem",
                                    backgroundColor: "#f8f9fa",
                                    border: "1px solid #e0e0e0",
                                    borderRadius: "4px",
                                  }}
                                >
                                  <h4
                                    style={{
                                      marginBottom: "1rem",
                                      fontSize: "1rem",
                                      fontWeight: "600",
                                      color: "#161616",
                                    }}
                                  >
                                    {intl.formatMessage(
                                      { id: "program.details.title" },
                                      { programName: row.cells[0].value },
                                    )}
                                  </h4>
                                  {programDetails[row.id] ? (
                                    <div
                                      style={{
                                        display: "grid",
                                        gridTemplateColumns:
                                          "repeat(auto-fit, minmax(300px, 1fr))",
                                        gap: "1rem",
                                      }}
                                    >
                                      {/* Program Description */}
                                      <div
                                        style={{
                                          backgroundColor: "#ffffff",
                                          border: "1px solid #e0e0e0",
                                          borderRadius: "4px",
                                          padding: "1rem",
                                        }}
                                      >
                                        <h5
                                          style={{
                                            marginBottom: "0.5rem",
                                            fontSize: "0.875rem",
                                            fontWeight: "600",
                                            color: "#161616",
                                          }}
                                        >
                                          {intl.formatMessage({
                                            id: "program.description",
                                          })}
                                        </h5>
                                        <p
                                          style={{
                                            margin: 0,
                                            fontSize: "0.875rem",
                                            color: "#525252",
                                            lineHeight: "1.4",
                                          }}
                                        >
                                          {programDetails[row.id].description ||
                                            intl.formatMessage({
                                              id: "program.description.not.available",
                                            })}
                                        </p>
                                      </div>

                                      {/* Order Entry Form */}
                                      <div
                                        style={{
                                          backgroundColor: "#ffffff",
                                          border: "1px solid #e0e0e0",
                                          borderRadius: "4px",
                                          padding: "1rem",
                                        }}
                                      >
                                        <h5
                                          style={{
                                            marginBottom: "0.5rem",
                                            fontSize: "0.875rem",
                                            fontWeight: "600",
                                            color: "#161616",
                                          }}
                                        >
                                          {intl.formatMessage({
                                            id: "program.order.entry.form",
                                          })}
                                        </h5>
                                        <p
                                          style={{
                                            margin: 0,
                                            fontSize: "0.875rem",
                                            color: "#525252",
                                          }}
                                        >
                                          {programDetails[row.id].orderFormName}
                                        </p>
                                      </div>

                                      {/* Patient Statistics */}
                                      <div
                                        style={{
                                          backgroundColor: "#ffffff",
                                          border: "1px solid #e0e0e0",
                                          borderRadius: "4px",
                                          padding: "1rem",
                                        }}
                                      >
                                        <h5
                                          style={{
                                            marginBottom: "0.5rem",
                                            fontSize: "0.875rem",
                                            fontWeight: "600",
                                            color: "#161616",
                                          }}
                                        >
                                          {intl.formatMessage({
                                            id: "program.statistics",
                                          })}
                                        </h5>
                                        <div
                                          style={{
                                            display: "flex",
                                            flexDirection: "column",
                                            gap: "0.25rem",
                                          }}
                                        >
                                          <div
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                            }}
                                          >
                                            {intl.formatMessage(
                                              {
                                                id: "program.active.patients.count",
                                              },
                                              {
                                                count:
                                                  programDetails[row.id]
                                                    .activePatients || 0,
                                              },
                                            )}
                                          </div>
                                          <div
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                            }}
                                          >
                                            {intl.formatMessage(
                                              {
                                                id: "program.total.patients.count",
                                              },
                                              {
                                                count:
                                                  programDetails[row.id]
                                                    .totalPatients || 0,
                                              },
                                            )}
                                          </div>
                                          <div
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                            }}
                                          >
                                            {intl.formatMessage(
                                              {
                                                id: "program.total.samples.count",
                                              },
                                              {
                                                count:
                                                  programDetails[row.id]
                                                    .totalSamples || 0,
                                              },
                                            )}
                                          </div>
                                        </div>
                                      </div>

                                      {/* Date Range */}
                                      <div
                                        style={{
                                          backgroundColor: "#ffffff",
                                          border: "1px solid #e0e0e0",
                                          borderRadius: "4px",
                                          padding: "1rem",
                                        }}
                                      >
                                        <h5
                                          style={{
                                            marginBottom: "0.5rem",
                                            fontSize: "0.875rem",
                                            fontWeight: "600",
                                            color: "#161616",
                                          }}
                                        >
                                          {intl.formatMessage({
                                            id: "program.date.range",
                                          })}
                                        </h5>
                                        <div
                                          style={{
                                            display: "flex",
                                            flexDirection: "column",
                                            gap: "0.25rem",
                                          }}
                                        >
                                          <div
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                            }}
                                          >
                                            {intl.formatMessage({
                                              id: "program.start.date",
                                            })}
                                            :{" "}
                                            {programDetails[row.id].startDate ||
                                              intl.formatMessage({
                                                id: "program.date.not.available",
                                              })}
                                          </div>
                                          <div
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                            }}
                                          >
                                            {intl.formatMessage({
                                              id: "program.end.date",
                                            })}
                                            :{" "}
                                            {programDetails[row.id].endDate ||
                                              intl.formatMessage({
                                                id: "program.date.ongoing",
                                              })}
                                          </div>
                                        </div>
                                      </div>
                                    </div>
                                  ) : (
                                    <div
                                      style={{
                                        textAlign: "center",
                                        color: "#525252",
                                        padding: "1rem",
                                      }}
                                    >
                                      {intl.formatMessage({
                                        id: "program.details.loading",
                                      })}
                                    </div>
                                  )}
                                </div>
                              </TableExpandedRow>
                            )}
                          </React.Fragment>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              />
            )}
          </div>
        </Column>
      </Grid>

      {/* Assign Programs Modal */}
      <AssignProgramsModal
        isOpen={showAssignModal}
        availablePrograms={availablePrograms}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedProgramIds) => {
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/programs/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ programIds: selectedProgramIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedPrograms(); // Refresh the list
              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning programs:", error);
          }
        }}
      />
    </div>
  );
}

function AssignProgramsModal({
  isOpen,
  availablePrograms,
  onClose,
  onConfirm,
}) {
  const intl = useIntl();
  const [selectedPrograms, setSelectedPrograms] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredPrograms = availablePrograms.filter((program) => {
    return (
      program.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      program.code.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const toggleProgram = (programId) => {
    setSelectedPrograms((prev) =>
      prev.includes(programId)
        ? prev.filter((id) => id !== programId)
        : [...prev, programId],
    );
  };

  const toggleAll = () => {
    if (selectedPrograms.length === filteredPrograms.length) {
      setSelectedPrograms([]);
    } else {
      setSelectedPrograms(filteredPrograms.map((program) => program.id));
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.assign.programs.modal.title",
      })}
      passiveModal
      size="lg"
    >
      <div style={{ marginBottom: "1.5rem" }}>
        {/* Search */}
        <div
          style={{
            marginBottom: "1.5rem",
          }}
        >
          <TextInput
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={intl.formatMessage({
              id: "program.search.placeholder",
            })}
            labelText={intl.formatMessage({ id: "program.search.label" })}
            renderIcon={Search}
          />
        </div>

        {/* Programs List */}
        <div
          style={{
            border: "1px solid #e0e0e0",
            borderRadius: "4px",
            maxHeight: "400px",
            overflow: "auto",
          }}
        >
          <div
            style={{
              padding: "1rem",
              borderBottom: "1px solid #e0e0e0",
              backgroundColor: "#f8f8f8",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            <Checkbox
              checked={selectedPrograms.length === filteredPrograms.length}
              onChange={toggleAll}
              indeterminate={
                selectedPrograms.length > 0 &&
                selectedPrograms.length < filteredPrograms.length
              }
            />
            <span style={{ fontWeight: "500" }}>
              {filteredPrograms.length}{" "}
              {intl.formatMessage({ id: "program.programs.available" })}
            </span>
          </div>

          <div style={{ padding: "0.5rem" }}>
            {filteredPrograms.map((program) => (
              <div
                key={program.id}
                style={{
                  display: "flex",
                  alignItems: "flex-start",
                  gap: "0.75rem",
                  padding: "0.75rem",
                  borderRadius: "4px",
                  border: "1px solid transparent",
                  "&:hover": {
                    backgroundColor: "#f8f8f8",
                    borderColor: "#e0e0e0",
                  },
                }}
              >
                <Checkbox
                  checked={selectedPrograms.includes(program.id)}
                  onChange={() => toggleProgram(program.id)}
                />
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontWeight: "500",
                      marginBottom: "0.25rem",
                      color: "#161616",
                    }}
                  >
                    {program.name}
                  </div>
                  <div
                    style={{
                      fontSize: "0.875rem",
                      color: "#525252",
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                    }}
                  >
                    <span
                      style={{
                        fontFamily: "monospace",
                        backgroundColor: "#f0f0f0",
                        padding: "0.125rem 0.25rem",
                        borderRadius: "2px",
                      }}
                    >
                      {program.code}
                    </span>
                    {program.assignedTo && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#da1e28",
                          fontStyle: "italic",
                        }}
                      >
                        ({intl.formatMessage({ id: "program.assigned.to" })}{" "}
                        {program.assignedTo})
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Footer */}
      <div
        style={{
          padding: "1rem 0",
          borderTop: "1px solid #e0e0e0",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <span
          style={{
            fontSize: "0.875rem",
            color: "#525252",
          }}
        >
          {selectedPrograms.length}{" "}
          {intl.formatMessage({ id: "program.programs.selected" })}
        </span>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            onClick={() => onConfirm(selectedPrograms)}
            disabled={selectedPrograms.length === 0}
          >
            {intl.formatMessage({ id: "button.assign.programs" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
