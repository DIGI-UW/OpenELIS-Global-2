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
} from "@carbon/react";
import { Plus, MoreVertical, Search } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function ProjectsTab({ unit }) {
  const intl = useIntl();
  const [assignedProjects, setAssignedProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [availableProjects, setAvailableProjects] = useState([]);
  const [statusFilter, setStatusFilter] = useState("all");
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedProject, setSelectedProject] = useState(null);
  const [projectDetails, setProjectDetails] = useState(null);

  useEffect(() => {
    fetchAssignedProjects();
    fetchAvailableProjects();
  }, [unit]);

  const fetchAssignedProjects = async () => {
    if (!unit) return;

    setLoading(true);
    try {
      const response = await fetch(`/rest/api/lab-units/${unit.id}/projects`);
      if (response.ok) {
        const data = await response.json();
        setAssignedProjects(data || []);
      }
    } catch (error) {
      console.error("Error fetching assigned projects:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch available projects for assignment
  const fetchAvailableProjects = async () => {
    try {
      const response = await fetch("/rest/api/projects/available");
      if (response.ok) {
        const data = await response.json();
        setAvailableProjects(data.projects || []);
      }
    } catch (error) {
      console.error("Error fetching available projects:", error);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "active":
        return (
          <Tag type="green">
            {intl.formatMessage({ id: "project.status.active" })}
          </Tag>
        );
      case "completed":
        return (
          <Tag type="blue">
            {intl.formatMessage({ id: "project.status.completed" })}
          </Tag>
        );
      case "on_hold":
        return (
          <Tag type="orange">
            {intl.formatMessage({ id: "project.status.on.hold" })}
          </Tag>
        );
      default:
        return (
          <Tag type="gray">
            {intl.formatMessage({ id: "project.status.inactive" })}
          </Tag>
        );
    }
  };

  const formatDateRange = (startDate, endDate) => {
    return `${startDate} - ${endDate}`;
  };

  const handleAssignProjects = () => {
    setShowAssignModal(true);
  };

  const handleViewDetails = async (project) => {
    setSelectedProject(project);
    setShowDetailsModal(true);
    
    // Fetch detailed project information
    try {
      const response = await fetch(`/rest/api/projects/${project.id}/details`);
      if (response.ok) {
        const data = await response.json();
        setProjectDetails(data);
      }
    } catch (error) {
      console.error("Error fetching project details:", error);
    }
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
                {intl.formatMessage({ id: "labunit.projects.assigned" })}
              </h3>
              <p
                style={{
                  margin: 0,
                  color: "#525252",
                  lineHeight: "1.5",
                  fontSize: "0.875rem",
                }}
              >
                {intl.formatMessage({ id: "labunit.projects.description" })}
              </p>
            </div>

            <Button
              renderIcon={Plus}
              onClick={handleAssignProjects}
              disabled={!unit}
            >
              {intl.formatMessage({ id: "button.assign.projects" })}
            </Button>
          </div>

          {/* Projects Table */}
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
                <p>{intl.formatMessage({ id: "loading.projects" })}</p>
              </div>
            ) : (
              <DataTable rows={assignedProjects}>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.name" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.pi" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.status" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.samples" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.date.range" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "project.actions" })}
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assignedProjects.map((project) => (
                        <TableRow key={project.id}>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.25rem",
                              }}
                            >
                              <span
                                style={{
                                  fontWeight: "500",
                                  color: "#161616",
                                  fontSize: "0.875rem",
                                }}
                              >
                                {project.name}
                              </span>
                              <span
                                style={{
                                  fontSize: "0.75rem",
                                  color: "#525252",
                                  fontFamily: "monospace",
                                }}
                              >
                                ({project.code})
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontSize: "0.875rem",
                                color: "#525252",
                              }}
                            >
                              {project.principalInvestigator}
                            </span>
                          </TableCell>
                          <TableCell>
                            {getStatusBadge(project.status)}
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: "500",
                                color: "#161616",
                              }}
                            >
                              {project.sampleCount?.toLocaleString() || "0"}
                            </span>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontSize: "0.875rem",
                                color: "#525252",
                                whiteSpace: "nowrap",
                              }}
                            >
                              {formatDateRange(
                                project.startDate,
                                project.endDate,
                              )}
                            </span>
                          </TableCell>
                          <TableCell>
                            <OverflowMenu size="sm">
                              <OverflowMenuItem>
                                <MoreVertical size={16} />
                                {intl.formatMessage({
                                  id: "button.more.actions",
                                })}
                              </OverflowMenuItem>
                              <OverflowMenuItem
                                onClick={() => handleViewDetails(project)}
                              >
                                {intl.formatMessage({
                                  id: "button.view.details",
                                })}
                              </OverflowMenuItem>
                              <OverflowMenuItem>
                                {intl.formatMessage({ id: "button.remove" })}
                              </OverflowMenuItem>
                            </OverflowMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </DataTable>
            )}
          </div>
        </Column>
      </Grid>

      {/* Assign Projects Modal */}
      <AssignProjectsModal
        isOpen={showAssignModal}
        availableProjects={availableProjects}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedProjectIds) => {
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/projects/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ projectIds: selectedProjectIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedProjects(); // Refresh the list
              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning projects:", error);
          }
        }}
      />

      {/* Assign Projects Modal */}
      <AssignProjectsModal
        isOpen={showAssignModal}
        availableProjects={availableProjects}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedProjectIds) => {
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/projects/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ projectIds: selectedProjectIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedProjects(); // Refresh the list
              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning projects:", error);
          }
        }}
      />

      {/* Project Details Modal */}
      <ProjectDetailsModal
        isOpen={showDetailsModal}
        project={selectedProject}
        projectDetails={projectDetails}
        onClose={() => {
          setShowDetailsModal(false);
          setSelectedProject(null);
          setProjectDetails(null);
        }}
        intl={intl}
      />
    </div>
  );
}

function AssignProjectsModal({
  isOpen,
  availableProjects,
  onClose,
  onConfirm,
}) {
  const intl = useIntl();
  const [selectedProjects, setSelectedProjects] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");

  const filteredProjects = availableProjects.filter((project) => {
    const matchesSearch =
      project.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      project.code.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus =
      statusFilter === "all" || project.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  const toggleProject = (projectId) => {
    setSelectedProjects((prev) =>
      prev.includes(projectId)
        ? prev.filter((id) => id !== projectId)
        : [...prev, projectId],
    );
  };

  const toggleAll = () => {
    if (selectedProjects.length === filteredProjects.length) {
      setSelectedProjects([]);
    } else {
      setSelectedProjects(filteredProjects.map((project) => project.id));
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.assign.projects.modal.title",
      })}
      passiveModal
      size="lg"
    >
      <div style={{ marginBottom: "1.5rem" }}>
        {/* Search and Filters */}
        <div
          style={{
            display: "flex",
            gap: "1rem",
            marginBottom: "1.5rem",
            flexWrap: "wrap",
          }}
        >
          <div style={{ flex: "1", minWidth: "250px" }}>
            <TextInput
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={intl.formatMessage({
                id: "project.search.placeholder",
              })}
              labelText={intl.formatMessage({ id: "project.search.label" })}
              renderIcon={Search}
            />
          </div>

          <div style={{ minWidth: "150px" }}>
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              labelText={intl.formatMessage({ id: "project.filter.status" })}
            >
              <SelectItem value="all">
                {intl.formatMessage({ id: "project.filter.all" })}
              </SelectItem>
              <SelectItem value="active">
                {intl.formatMessage({ id: "project.status.active" })}
              </SelectItem>
              <SelectItem value="completed">
                {intl.formatMessage({ id: "project.status.completed" })}
              </SelectItem>
              <SelectItem value="on_hold">
                {intl.formatMessage({ id: "project.status.on.hold" })}
              </SelectItem>
            </Select>
          </div>
        </div>

        {/* Projects List */}
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
              checked={selectedProjects.length === filteredProjects.length}
              onChange={toggleAll}
              indeterminate={
                selectedProjects.length > 0 &&
                selectedProjects.length < filteredProjects.length
              }
            />
            <span style={{ fontWeight: "500" }}>
              {filteredProjects.length}{" "}
              {intl.formatMessage({ id: "project.projects.available" })}
            </span>
          </div>

          <div style={{ padding: "0.5rem" }}>
            {filteredProjects.map((project) => (
              <div
                key={project.id}
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
                  checked={selectedProjects.includes(project.id)}
                  onChange={() => toggleProject(project.id)}
                />
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontWeight: "500",
                      marginBottom: "0.25rem",
                      color: "#161616",
                    }}
                  >
                    {project.name}
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
                      {project.code}
                    </span>
                    {project.assignedTo && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#da1e28",
                          fontStyle: "italic",
                        }}
                      >
                        ({intl.formatMessage({ id: "project.assigned.to" })}{" "}
                        {project.assignedTo})
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
          {selectedProjects.length}{" "}
          {intl.formatMessage({ id: "project.projects.selected" })}
        </span>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            onClick={() => onConfirm(selectedProjects)}
            disabled={selectedProjects.length === 0}
          >
            {intl.formatMessage({ id: "button.assign.projects" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

// Project Details Modal
function ProjectDetailsModal({ isOpen, project, projectDetails, onClose, intl }) {
  if (!isOpen || !project) return null;

  const getStatusBadge = (status) => {
    const statusMap = {
      active: { type: "green", text: "Active" },
      completed: { type: "blue", text: "Completed" },
      "on-hold": { type: "yellow", text: "On Hold" },
      cancelled: { type: "red", text: "Cancelled" },
    };
    const statusConfig = statusMap[status] || { type: "gray", text: status };
    return (
      <Tag type={statusConfig.type}>
        {statusConfig.text}
      </Tag>
    );
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "project.details.modal.title",
      })}
      passiveModal
      size="lg"
    >
      <div style={{ marginBottom: "1.5rem" }}>
        {/* Project Header */}
        <div
          style={{
            marginBottom: "1.5rem",
            padding: "1rem",
            backgroundColor: "#f8f9fa",
            border: "1px solid #e0e0e0",
            borderRadius: "4px",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "flex-start",
              marginBottom: "0.5rem",
            }}
          >
            <div>
              <h3
                style={{
                  margin: 0,
                  fontSize: "1.25rem",
                  fontWeight: "600",
                  color: "#161616",
                }}
              >
                {project.name}
              </h3>
              <span
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  fontFamily: "monospace",
                  marginLeft: "0.5rem",
                }}
              >
                ({project.code})
              </span>
            </div>
            {getStatusBadge(project.status)}
          </div>
        </div>

        {/* Project Details Grid */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
            gap: "1rem",
          }}
        >
          {/* Description */}
          <div
            style={{
              backgroundColor: "#ffffff",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.description" })}
            </h4>
            <p
              style={{
                margin: 0,
                fontSize: "0.875rem",
                color: "#525252",
                lineHeight: "1.4",
              }}
            >
              {projectDetails?.description ||
                intl.formatMessage({
                  id: "project.description.not.available",
                })}
            </p>
          </div>

          {/* Principal Investigator */}
          <div
            style={{
              backgroundColor: "#ffffff",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.principal.investigator" })}
            </h4>
            <p
              style={{
                margin: 0,
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              {projectDetails?.principalInvestigator || project.principalInvestigator}
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
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.order.entry.form" })}
            </h4>
            <p
              style={{
                margin: 0,
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              {projectDetails?.orderFormName ||
                intl.formatMessage({
                  id: "project.order.entry.form.not.available",
                })}
            </p>
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
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.date.range" })}
            </h4>
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
                {intl.formatMessage({ id: "project.start.date" })}:{" "}
                {projectDetails?.startDate || project.startDate || 
                  intl.formatMessage({
                    id: "project.date.not.available",
                  })}
              </div>
              <div
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                {intl.formatMessage({ id: "project.end.date" })}:{" "}
                {projectDetails?.endDate || project.endDate ||
                  intl.formatMessage({
                    id: "project.date.ongoing",
                  })}
              </div>
            </div>
          </div>

          {/* Statistics */}
          <div
            style={{
              backgroundColor: "#ffffff",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.statistics" })}
            </h4>
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
                  { id: "project.enrolled.patients.count" },
                  {
                    count: projectDetails?.enrolledPatients || project.enrolledPatients || 0,
                  }
                )}
              </div>
              <div
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                {intl.formatMessage(
                  { id: "project.total.samples.count" },
                  {
                    count: projectDetails?.totalSamples || project.sampleCount || 0,
                  }
                )}
              </div>
            </div>
          </div>

          {/* Consent Requirements */}
          <div
            style={{
              backgroundColor: "#ffffff",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "project.consent.requirements" })}
            </h4>
            <p
              style={{
                margin: 0,
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              {projectDetails?.consentRequirements ||
                intl.formatMessage({
                  id: "project.consent.requirements.not.available",
                })}
            </p>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div
        style={{
          padding: "1rem 0",
          borderTop: "1px solid #e0e0e0",
          display: "flex",
          justifyContent: "flex-end",
        }}
      >
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.close" })}
          </Button>
          <Button>
            {intl.formatMessage({ id: "button.edit.project" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
