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
import { Plus, CheckCircle, MoreVertical, Search } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function WorkflowsTab({ unit }) {
  const intl = useIntl();
  const [assignedWorkflows, setAssignedWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [availableWorkflows, setAvailableWorkflows] = useState([]);

  useEffect(() => {
    fetchAssignedWorkflows();
    fetchAvailableWorkflows();
  }, [unit]);

  const fetchAssignedWorkflows = async () => {
    if (!unit) return;

    setLoading(true);
    try {
      const response = await fetch(`/rest/api/lab-units/${unit.id}/workflows`);
      if (response.ok) {
        const data = await response.json();
        setAssignedWorkflows(data || []);
      }
    } catch (error) {
      console.error("Error fetching workflows:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch available workflows for assignment
  const fetchAvailableWorkflows = async () => {
    try {
      const response = await fetch("/rest/api/workflows/available");
      if (response.ok) {
        const data = await response.json();
        setAvailableWorkflows(data.workflows || []);
      }
    } catch (error) {
      console.error("Error fetching available workflows:", error);
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

  const handleAssignWorkflows = () => {
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

            <Button
              renderIcon={Plus}
              onClick={handleAssignWorkflows}
              disabled={!unit}
            >
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

      {/* Assign Workflows Modal */}
      <AssignWorkflowsModal
        isOpen={showAssignModal}
        availableWorkflows={availableWorkflows}
        currentDefaultWorkflow={assignedWorkflows.find((w) => w.isDefault)}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedWorkflowIds, defaultWorkflowId) => {
          try {
            // First assign workflows
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/workflows/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ workflowIds: selectedWorkflowIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedWorkflows(); // Refresh list

              // Then set default workflow if specified
              if (defaultWorkflowId) {
                await fetch(
                  `/rest/api/lab-units/${unit.id}/workflows/${defaultWorkflowId}/set-default`,
                  {
                    method: "POST",
                  },
                );
              }

              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning workflows:", error);
          }
        }}
      />
    </div>
  );
}

function AssignWorkflowsModal({
  isOpen,
  availableWorkflows,
  currentDefaultWorkflow,
  onClose,
  onConfirm,
}) {
  const intl = useIntl();
  const [selectedWorkflows, setSelectedWorkflows] = useState([]);
  const [defaultWorkflowId, setDefaultWorkflowId] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  const filteredWorkflows = availableWorkflows.filter((workflow) => {
    return (
      workflow.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      workflow.type.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const toggleWorkflow = (workflowId) => {
    setSelectedWorkflows((prev) => {
      const newSelection = prev.includes(workflowId)
        ? prev.filter((id) => id !== workflowId)
        : [...prev, workflowId];

      // Auto-select default if this workflow is selected and no default set
      const selectedWorkflow = availableWorkflows.find(
        (w) => w.id === workflowId,
      );
      if (
        selectedWorkflow &&
        selectedWorkflow.isDefault &&
        !defaultWorkflowId
      ) {
        setDefaultWorkflowId(workflowId);
      }

      // Clear default if workflow is deselected
      if (
        !newSelection.includes(workflowId) &&
        defaultWorkflowId === workflowId
      ) {
        setDefaultWorkflowId("");
      }

      return newSelection;
    });
  };

  const toggleAll = () => {
    if (selectedWorkflows.length === filteredWorkflows.length) {
      setSelectedWorkflows([]);
      setDefaultWorkflowId("");
    } else {
      setSelectedWorkflows(filteredWorkflows.map((workflow) => workflow.id));
      // Auto-select first default workflow if none selected
      const defaultWorkflow = filteredWorkflows.find((w) => w.isDefault);
      if (defaultWorkflow) {
        setDefaultWorkflowId(defaultWorkflow.id);
      }
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.assign.workflows.modal.title",
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
              id: "workflow.search.placeholder",
            })}
            labelText={intl.formatMessage({ id: "workflow.search.label" })}
            renderIcon={Search}
          />
        </div>

        {/* Workflows List */}
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
              checked={selectedWorkflows.length === filteredWorkflows.length}
              onChange={toggleAll}
              indeterminate={
                selectedWorkflows.length > 0 &&
                selectedWorkflows.length < filteredWorkflows.length
              }
            />
            <span style={{ fontWeight: "500" }}>
              {filteredWorkflows.length}{" "}
              {intl.formatMessage({ id: "workflow.workflows.available" })}
            </span>
          </div>

          <div style={{ padding: "0.5rem" }}>
            {filteredWorkflows.map((workflow) => (
              <div
                key={workflow.id}
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
                  checked={selectedWorkflows.includes(workflow.id)}
                  onChange={() => toggleWorkflow(workflow.id)}
                />
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontWeight: "500",
                      marginBottom: "0.25rem",
                      color: "#161616",
                    }}
                  >
                    {workflow.name}
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
                    <Tag style={{ marginRight: "0.5rem" }}>{workflow.type}</Tag>
                    {workflow.isDefault && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#24a148",
                          fontWeight: "500",
                        }}
                      >
                        ({intl.formatMessage({ id: "workflow.default" })})
                      </span>
                    )}
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center" }}>
                  <Checkbox
                    checked={defaultWorkflowId === workflow.id}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setDefaultWorkflowId(workflow.id);
                      } else if (defaultWorkflowId === workflow.id) {
                        setDefaultWorkflowId("");
                      }
                    }}
                    disabled={!selectedWorkflows.includes(workflow.id)}
                  />
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
          {selectedWorkflows.length}{" "}
          {intl.formatMessage({ id: "workflow.workflows.selected" })}
        </span>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            onClick={() => onConfirm(selectedWorkflows, defaultWorkflowId)}
            disabled={selectedWorkflows.length === 0}
          >
            {intl.formatMessage({ id: "button.assign.workflows" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
