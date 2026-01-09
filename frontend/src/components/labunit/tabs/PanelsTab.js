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

export default function PanelsTab({ unit }) {
  const intl = useIntl();
  const [assignedPanels, setAssignedPanels] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [availablePanels, setAvailablePanels] = useState([]);
  const [expandedRows, setExpandedRows] = useState({});
  const [panelTests, setPanelTests] = useState({});

  useEffect(() => {
    fetchAssignedPanels();
    fetchAvailablePanels();
  }, [unit]);

  const toggleRowExpansion = async (panelId) => {
    const newExpandedState = !expandedRows[panelId];
    setExpandedRows((prev) => ({
      ...prev,
      [panelId]: newExpandedState,
    }));

    // Fetch tests if expanding and we don't have them yet
    if (newExpandedState && !panelTests[panelId]) {
      try {
        const response = await fetch(`/rest/api/panels/${panelId}/tests`);
        if (response.ok) {
          const data = await response.json();
          setPanelTests((prev) => ({
            ...prev,
            [panelId]: data.tests || [],
          }));
        }
      } catch (error) {
        console.error("Error fetching panel tests:", error);
      }
    }
  };

  const fetchAssignedPanels = async () => {
    if (!unit) return;

    setLoading(true);
    try {
      const response = await fetch(`/rest/api/lab-units/${unit.id}/panels`);
      if (response.ok) {
        const data = await response.json();
        setAssignedPanels(data || []);
      }
    } catch (error) {
      console.error("Error fetching assigned panels:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch available panels for assignment
  const fetchAvailablePanels = async () => {
    try {
      const response = await fetch("/rest/api/panels/available");
      if (response.ok) {
        const data = await response.json();
        setAvailablePanels(data.panels || []);
      }
    } catch (error) {
      console.error("Error fetching available panels:", error);
    }
  };

  const getTestCountDisplay = (testCount) => {
    return testCount === 1 ? "1 test" : `${testCount} tests`;
  };

  const handleAssignPanels = () => {
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

            <Button
              renderIcon={Plus}
              onClick={handleAssignPanels}
              disabled={!unit}
            >
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
              <DataTable
                rows={assignedPanels}
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
                            {intl.formatMessage({ id: "panel.name" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[1] })}
                          >
                            {intl.formatMessage({ id: "panel.code" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[2] })}
                          >
                            {intl.formatMessage({ id: "panel.tests" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[3] })}
                          >
                            {intl.formatMessage({ id: "panel.status" })}
                          </TableHeader>
                          <TableHeader
                            {...getHeaderProps({ header: headers[4] })}
                          >
                            {intl.formatMessage({ id: "panel.actions" })}
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
                                <span
                                  style={{
                                    fontSize: "0.875rem",
                                    color: "#525252",
                                  }}
                                >
                                  {getTestCountDisplay(row.cells[2].value)}
                                </span>
                              </TableCell>
                              <TableCell>
                                <Tag
                                  type={row.cells[3].value ? "green" : "gray"}
                                >
                                  {row.cells[3].value
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
                                      {intl.formatMessage({
                                        id: "button.remove",
                                      })}
                                    </OverflowMenuItem>
                                  </OverflowMenu>
                                </div>
                              </TableCell>
                            </TableExpandRow>
                            {expandedRows[row.id] && (
                              <TableExpandedRow colSpan={6}>
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
                                      { id: "panel.tests.title" },
                                      { panelName: row.cells[0].value },
                                    )}
                                  </h4>
                                  {panelTests[row.id] ? (
                                    panelTests[row.id].length > 0 ? (
                                      <div
                                        style={{
                                          display: "grid",
                                          gridTemplateColumns:
                                            "repeat(auto-fill, minmax(300px, 1fr))",
                                          gap: "0.75rem",
                                        }}
                                      >
                                        {panelTests[row.id].map((test) => (
                                          <div
                                            key={test.id}
                                            style={{
                                              backgroundColor: "#ffffff",
                                              border: "1px solid #e0e0e0",
                                              borderRadius: "4px",
                                              padding: "0.75rem",
                                            }}
                                          >
                                            <div
                                              style={{
                                                fontWeight: "500",
                                                marginBottom: "0.25rem",
                                                color: "#161616",
                                              }}
                                            >
                                              {test.name}
                                            </div>
                                            <div
                                              style={{
                                                display: "flex",
                                                alignItems: "center",
                                                gap: "0.5rem",
                                                fontSize: "0.875rem",
                                                color: "#525252",
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
                                                {test.code}
                                              </span>
                                              <Tag
                                                type={
                                                  test.isActive
                                                    ? "green"
                                                    : "gray"
                                                }
                                                size="sm"
                                              >
                                                {test.isActive
                                                  ? intl.formatMessage({
                                                      id: "test.status.active",
                                                    })
                                                  : intl.formatMessage({
                                                      id: "test.status.inactive",
                                                    })}
                                              </Tag>
                                            </div>
                                          </div>
                                        ))}
                                      </div>
                                    ) : (
                                      <p
                                        style={{
                                          color: "#525252",
                                          fontStyle: "italic",
                                          margin: 0,
                                        }}
                                      >
                                        {intl.formatMessage({
                                          id: "panel.tests.empty",
                                        })}
                                      </p>
                                    )
                                  ) : (
                                    <div
                                      style={{
                                        textAlign: "center",
                                        color: "#525252",
                                        padding: "1rem",
                                      }}
                                    >
                                      {intl.formatMessage({
                                        id: "panel.tests.loading",
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

      {/* Assign Panels Modal */}
      <AssignPanelsModal
        isOpen={showAssignModal}
        availablePanels={availablePanels}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedPanelIds) => {
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/panels/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ panelIds: selectedPanelIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedPanels(); // Refresh the list
              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning panels:", error);
          }
        }}
      />
    </div>
  );
}

function AssignPanelsModal({ isOpen, availablePanels, onClose, onConfirm }) {
  const intl = useIntl();
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredPanels = availablePanels.filter((panel) => {
    return (
      panel.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      panel.code.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const togglePanel = (panelId) => {
    setSelectedPanels((prev) =>
      prev.includes(panelId)
        ? prev.filter((id) => id !== panelId)
        : [...prev, panelId],
    );
  };

  const toggleAll = () => {
    if (selectedPanels.length === filteredPanels.length) {
      setSelectedPanels([]);
    } else {
      setSelectedPanels(filteredPanels.map((panel) => panel.id));
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.assign.panels.modal.title",
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
              id: "panel.search.placeholder",
            })}
            labelText={intl.formatMessage({ id: "panel.search.label" })}
            renderIcon={Search}
          />
        </div>

        {/* Panels List */}
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
              checked={selectedPanels.length === filteredPanels.length}
              onChange={toggleAll}
              indeterminate={
                selectedPanels.length > 0 &&
                selectedPanels.length < filteredPanels.length
              }
            />
            <span style={{ fontWeight: "500" }}>
              {filteredPanels.length}{" "}
              {intl.formatMessage({ id: "panel.panels.available" })}
            </span>
          </div>

          <div style={{ padding: "0.5rem" }}>
            {filteredPanels.map((panel) => (
              <div
                key={panel.id}
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
                  checked={selectedPanels.includes(panel.id)}
                  onChange={() => togglePanel(panel.id)}
                />
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontWeight: "500",
                      marginBottom: "0.25rem",
                      color: "#161616",
                    }}
                  >
                    {panel.name}
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
                      {panel.code}
                    </span>
                    {panel.assignedTo && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#da1e28",
                          fontStyle: "italic",
                        }}
                      >
                        ({intl.formatMessage({ id: "panel.assigned.to" })}{" "}
                        {panel.assignedTo})
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
          {selectedPanels.length}{" "}
          {intl.formatMessage({ id: "panel.panels.selected" })}
        </span>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            onClick={() => onConfirm(selectedPanels)}
            disabled={selectedPanels.length === 0}
          >
            {intl.formatMessage({ id: "button.assign.panels" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
