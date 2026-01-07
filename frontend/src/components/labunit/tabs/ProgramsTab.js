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

export default function ProgramsTab() {
  const intl = useIntl();
  const [assignedPrograms, setAssignedPrograms] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAssignedPrograms();
  }, []);

  const fetchAssignedPrograms = async () => {
    setLoading(true);
    try {
      const response = await fetch("/rest/api/lab-units/programs");
      if (response.ok) {
        const data = await response.json();
        setAssignedPrograms(data.programs || []);
      }
    } catch (error) {
      console.error("Error fetching assigned programs:", error);
    } finally {
      setLoading(false);
    }
  };

  const getPatientCountDisplay = (patientCount) => {
    return patientCount.toLocaleString();
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

            <Button renderIcon={Plus}>
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
              <DataTable rows={assignedPrograms}>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          {intl.formatMessage({ id: "program.name" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "program.code" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({
                            id: "program.order.entry.form",
                          })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({
                            id: "program.active.patients",
                          })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "program.status" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "program.actions" })}
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assignedPrograms.map((program) => (
                        <TableRow key={program.id}>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#161616",
                              }}
                            >
                              {program.name}
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
                              {program.code}
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
                              {program.orderFormName}
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
                              {getPatientCountDisplay(program.patientCount)}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Tag type={program.isActive ? "green" : "gray"}>
                              {program.isActive
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
                              <OverflowMenu size="sm">
                                <OverflowMenuItem>
                                  <MoreVertical size={16} />
                                  {intl.formatMessage({
                                    id: "button.more.actions",
                                  })}
                                </OverflowMenuItem>
                                <OverflowMenuItem>
                                  {intl.formatMessage({
                                    id: "button.configure",
                                  })}
                                </OverflowMenuItem>
                                <OverflowMenuItem>
                                  {intl.formatMessage({ id: "button.remove" })}
                                </OverflowMenuItem>
                              </OverflowMenu>
                            </div>
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
