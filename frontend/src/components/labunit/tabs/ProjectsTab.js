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
  Grid,
  Column,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import { Plus, MoreVertical } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function ProjectsTab() {
  const intl = useIntl();
  const [assignedProjects, setAssignedProjects] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAssignedProjects();
  }, []);

  const fetchAssignedProjects = async () => {
    setLoading(true);
    try {
      const response = await fetch("/rest/api/lab-units/projects");
      if (response.ok) {
        const data = await response.json();
        setAssignedProjects(data.projects || []);
      }
    } catch (error) {
      console.error("Error fetching assigned projects:", error);
    } finally {
      setLoading(false);
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

            <Button renderIcon={Plus}>
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
                              <OverflowMenuItem>
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
    </div>
  );
}
