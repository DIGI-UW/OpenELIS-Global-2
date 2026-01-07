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
import { Plus, CheckCircle, MoreVertical } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function WorkflowsTab() {
  const intl = useIntl();
  const [assignedWorkflows, setAssignedWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchAssignedWorkflows();
  }, []);

  const fetchAssignedWorkflows = async () => {
    setLoading(true);
    try {
      const response = await fetch("/rest/api/lab-units/workflows");
      if (response.ok) {
        const data = await response.json();
        setAssignedWorkflows(data.workflows || []);
      }
    } catch (error) {
      console.error("Error fetching workflows:", error);
    } finally {
      setLoading(false);
    }
  };

  const setAsDefault = async (workflowId) => {
    try {
      const response = await fetch(
        `/rest/api/lab-units/workflows/${workflowId}/set-default`,
        {
          method: "POST",
        },
      );

      if (response.ok) {
        await fetchAssignedWorkflows(); // Refresh list
      }
    } catch (error) {
      console.error("Error setting default workflow:", error);
    }
  };

  const getStatusBadge = (isDefault) => {
    return isDefault ? (
      <Tag type="green">{intl.formatMessage({ id: "workflow.default" })}</Tag>
    ) : (
      <Tag type="gray">{intl.formatMessage({ id: "workflow.standard" })}</Tag>
    );
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
                {intl.formatMessage({ id: "labunit.workflows.assigned" })}
              </h3>
              <p
                style={{
                  margin: 0,
                  color: "#525252",
                  lineHeight: "1.5",
                  fontSize: "0.875rem",
                }}
              >
                {intl.formatMessage({ id: "labunit.workflows.description" })}
              </p>
            </div>

            <Button renderIcon={Plus}>
              {intl.formatMessage({ id: "button.assign.workflow" })}
            </Button>
          </div>

          {/* Workflows Table */}
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
                <p>{intl.formatMessage({ id: "loading.workflows" })}</p>
              </div>
            ) : (
              <DataTable rows={assignedWorkflows}>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          {intl.formatMessage({ id: "workflow.name" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "workflow.type" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "workflow.default" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "workflow.actions" })}
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assignedWorkflows.map((workflow) => (
                        <TableRow key={workflow.id}>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#161616",
                              }}
                            >
                              {workflow.name}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Tag>{workflow.type}</Tag>
                          </TableCell>
                          <TableCell>
                            {getStatusBadge(workflow.isDefault)}
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                gap: "0.5rem",
                                alignItems: "center",
                              }}
                            >
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => setAsDefault(workflow.id)}
                                disabled={workflow.isDefault}
                                style={{ marginRight: "0.5rem" }}
                              >
                                {intl.formatMessage({
                                  id: "workflow.set.default",
                                })}
                              </Button>

                              <OverflowMenu size="sm">
                                <OverflowMenuItem>
                                  <MoreVertical size={16} />
                                  {intl.formatMessage({
                                    id: "button.more.actions",
                                  })}
                                </OverflowMenuItem>
                                <OverflowMenuItem
                                  onClick={() => {
                                    /* Handle remove */
                                  }}
                                >
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
