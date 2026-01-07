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

export default function PanelsTab() {
  const intl = useIntl();
  const [assignedPanels, setAssignedPanels] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAssignedPanels();
  }, []);

  const fetchAssignedPanels = async () => {
    setLoading(true);
    try {
      const response = await fetch("/rest/api/lab-units/panels");
      if (response.ok) {
        const data = await response.json();
        setAssignedPanels(data.panels || []);
      }
    } catch (error) {
      console.error("Error fetching assigned panels:", error);
    } finally {
      setLoading(false);
    }
  };

  const getTestCountDisplay = (testCount) => {
    return testCount === 1 ? "1 test" : `${testCount} tests`;
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
                {intl.formatMessage({ id: "labunit.panels.assigned" })}
              </h3>
              <p
                style={{
                  margin: 0,
                  color: "#525252",
                  lineHeight: "1.5",
                  fontSize: "0.875rem",
                }}
              >
                {intl.formatMessage({ id: "labunit.panels.description" })}
              </p>
            </div>

            <Button renderIcon={Plus}>
              {intl.formatMessage({ id: "button.assign.panels" })}
            </Button>
          </div>

          {/* Panels Table */}
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
                <p>{intl.formatMessage({ id: "loading.panels" })}</p>
              </div>
            ) : (
              <DataTable rows={assignedPanels}>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          {intl.formatMessage({ id: "panel.name" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "panel.code" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "panel.tests" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "panel.status" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "panel.actions" })}
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assignedPanels.map((panel) => (
                        <TableRow key={panel.id}>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#161616",
                              }}
                            >
                              {panel.name}
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
                              {panel.code}
                            </span>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontSize: "0.875rem",
                                color: "#525252",
                              }}
                            >
                              {getTestCountDisplay(panel.testCount)}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Tag type={panel.isActive ? "green" : "gray"}>
                              {panel.isActive
                                ? intl.formatMessage({
                                    id: "panel.status.active",
                                  })
                                : intl.formatMessage({
                                    id: "panel.status.inactive",
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
                                  id: "button.view.tests",
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
