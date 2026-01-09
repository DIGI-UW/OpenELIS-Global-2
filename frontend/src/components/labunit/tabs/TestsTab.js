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
  Checkbox,
  OverflowMenu,
  OverflowMenuItem,
  Tag,
  Modal,
  TextInput,
} from "@carbon/react";
import {
  Edit,
  Trash2,
  Renew,
  CheckCircle,
  View,
  ViewOff,
  Add,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function TestsTab({ unit }) {
  const intl = useIntl();
  const [assignedTests, setAssignedTests] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [showReassignModal, setShowReassignModal] = useState(false);
  const [availableTests, setAvailableTests] = useState([]);

  // Fetch assigned tests for this lab unit
  const fetchAssignedTests = async () => {
    if (!unit) return;

    setLoading(true);
    try {
      const response = await fetch(`/rest/api/lab-units/${unit.id}/tests`);
      if (response.ok) {
        const data = await response.json();
        setAssignedTests(data.tests || []);
      }
    } catch (error) {
      console.error("Error fetching assigned tests:", error);
    } finally {
      setLoading(false);
    }
  };

  // Fetch available tests for assignment
  const fetchAvailableTests = async () => {
    try {
      const response = await fetch("/rest/api/tests/available");
      if (response.ok) {
        const data = await response.json();
        setAvailableTests(data.tests || []);
      }
    } catch (error) {
      console.error("Error fetching available tests:", error);
    }
  };

  useEffect(() => {
    fetchAssignedTests();
    fetchAvailableTests();
  }, [unit]);

  const toggleTest = (testId) => {
    setSelectedTests((prev) =>
      prev.includes(testId)
        ? prev.filter((id) => id !== testId)
        : [...prev, testId],
    );
  };

  const toggleAll = () => {
    if (selectedTests.length === assignedTests.length) {
      setSelectedTests([]);
    } else {
      setSelectedTests(assignedTests.map((test) => test.id));
    }
  };

  const handleAssignTests = () => {
    setShowAssignModal(true);
  };

  const handleReassignTests = () => {
    if (selectedTests.length === 0) return;
    setShowReassignModal(true);
  };

  const handleRemoveTests = async () => {
    const selectedTestIds = selectedTests.map((test) => test.id);
    try {
      const response = await fetch(
        `/rest/api/lab-units/${unit.id}/tests/remove`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ testIds: selectedTestIds }),
        },
      );

      if (response.ok) {
        await fetchAssignedTests(); // Refresh the list
        setSelectedTests([]);
      }
    } catch (error) {
      console.error("Error removing tests:", error);
    }
  };

  const handleActivateTests = async () => {
    const selectedTestIds = selectedTests.map((test) => test.id);
    try {
      const response = await fetch(
        `/rest/api/lab-units/${unit.id}/tests/activate`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ testIds: selectedTestIds }),
        },
      );

      if (response.ok) {
        await fetchAssignedTests(); // Refresh the list
        setSelectedTests([]);
      }
    } catch (error) {
      console.error("Error activating tests:", error);
    }
  };

  const handleDeactivateTests = async () => {
    const selectedTestIds = selectedTests.map((test) => test.id);
    try {
      const response = await fetch(
        `/rest/api/lab-units/${unit.id}/tests/deactivate`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ testIds: selectedTestIds }),
        },
      );

      if (response.ok) {
        await fetchAssignedTests(); // Refresh the list
        setSelectedTests([]);
      }
    } catch (error) {
      console.error("Error deactivating tests:", error);
    }
  };

  const getStatusBadge = (isActive) => {
    return isActive ? (
      <Tag type="green">{intl.formatMessage({ id: "test.status.active" })}</Tag>
    ) : (
      <Tag type="gray">
        {intl.formatMessage({ id: "test.status.inactive" })}
      </Tag>
    );
  };

  const activeCount = assignedTests.filter((test) => test.isActive).length;
  const inactiveCount = assignedTests.filter((test) => !test.isActive).length;

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
                {intl.formatMessage({ id: "labunit.tests.assigned" })}
              </h3>
              <p
                style={{
                  margin: 0,
                  color: "#525252",
                  lineHeight: "1.5",
                  fontSize: "0.875rem",
                }}
              >
                {intl.formatMessage(
                  { id: "labunit.tests.count" },
                  {
                    total: assignedTests.length,
                    active: activeCount,
                    inactive: inactiveCount,
                  },
                )}
              </p>
            </div>
            <Button
              renderIcon={Add}
              onClick={handleAssignTests}
              disabled={!unit}
            >
              {intl.formatMessage({ id: "button.assign.tests" })}
            </Button>
          </div>

          {/* Bulk Actions */}
          {selectedTests.length > 0 && (
            <div
              style={{
                marginBottom: "1.5rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
                border: "1px solid #e0e0e0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                flexWrap: "wrap",
                gap: "1rem",
              }}
            >
              <span
                style={{
                  fontWeight: "500",
                  color: "#161616",
                }}
              >
                {selectedTests.length}{" "}
                {intl.formatMessage({ id: "test.selected" })}
              </span>

              <div
                style={{
                  display: "flex",
                  gap: "0.5rem",
                  flexWrap: "wrap",
                }}
              >
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={handleReassignTests}
                >
                  {intl.formatMessage({ id: "button.reassign" })}
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={View}
                  onClick={handleActivateTests}
                >
                  {intl.formatMessage({ id: "button.activate" })}
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={ViewOff}
                  onClick={handleDeactivateTests}
                >
                  {intl.formatMessage({ id: "button.deactivate" })}
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={Trash2}
                  onClick={handleRemoveTests}
                  iconDescription={intl.formatMessage({ id: "button.remove" })}
                />
              </div>
            </div>
          )}

          {/* Tests Table */}
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
                <p>{intl.formatMessage({ id: "loading.tests" })}</p>
              </div>
            ) : (
              <DataTable rows={assignedTests}>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <Checkbox
                            checked={
                              selectedTests.length === assignedTests.length
                            }
                            onChange={toggleAll}
                            indeterminate={
                              selectedTests.length > 0 &&
                              selectedTests.length < assignedTests.length
                            }
                          />
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.name" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.code" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.loinc" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.status" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.primary" })}
                        </TableHeader>
                        <TableHeader>
                          {intl.formatMessage({ id: "test.actions" })}
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assignedTests.map((test) => (
                        <TableRow key={test.id}>
                          <TableCell>
                            <Checkbox
                              checked={selectedTests.includes(test.id)}
                              onChange={() => toggleTest(test.id)}
                            />
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: "500",
                                color: "#161616",
                              }}
                            >
                              {test.name}
                            </span>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontFamily: "monospace",
                                fontSize: "0.875rem",
                                color: "#525252",
                              }}
                            >
                              {test.code}
                            </span>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontFamily: "monospace",
                                fontSize: "0.875rem",
                                color: "#525252",
                              }}
                            >
                              {test.loinc}
                            </span>
                          </TableCell>
                          <TableCell>{getStatusBadge(test.isActive)}</TableCell>
                          <TableCell>
                            {test.isPrimary ? (
                              <CheckCircle
                                size={16}
                                style={{ color: "#24a148" }}
                              />
                            ) : (
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => {
                                  /* Handle set primary */
                                }}
                              >
                                {intl.formatMessage({ id: "test.set.primary" })}
                              </Button>
                            )}
                          </TableCell>
                          <TableCell>
                            <OverflowMenu size="sm">
                              <OverflowMenuItem
                                onClick={() => {
                                  /* Handle edit */
                                }}
                              >
                                <Edit size={16} />
                                {intl.formatMessage({ id: "button.edit" })}
                              </OverflowMenuItem>
                              <OverflowMenuItem
                                onClick={() => {
                                  /* Handle remove single */
                                }}
                              >
                                <Trash2 size={16} />
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

      {/* Assign Tests Modal */}
      <AssignTestsModal
        isOpen={showAssignModal}
        availableTests={availableTests}
        onClose={() => setShowAssignModal(false)}
        onConfirm={async (selectedTestIds) => {
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/tests/assign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ testIds: selectedTestIds }),
              },
            );

            if (response.ok) {
              await fetchAssignedTests(); // Refresh the list
              setShowAssignModal(false);
            }
          } catch (error) {
            console.error("Error assigning tests:", error);
          }
        }}
      />

      {/* Reassign Tests Modal */}
      <ReassignTestsModal
        isOpen={showReassignModal}
        tests={assignedTests.filter((test) => selectedTests.includes(test.id))}
        onClose={() => setShowReassignModal(false)}
        onConfirm={async (destinationLabUnitId, keepReference) => {
          const selectedTestIds = selectedTests.map((test) => test.id);
          try {
            const response = await fetch(
              `/rest/api/lab-units/${unit.id}/tests/reassign`,
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({
                  testIds: selectedTestIds,
                  destinationLabUnitId,
                  keepReference,
                }),
              },
            );

            if (response.ok) {
              await fetchAssignedTests(); // Refresh the list
              setShowReassignModal(false);
              setSelectedTests([]);
            }
          } catch (error) {
            console.error("Error reassigning tests:", error);
          }
        }}
      />
    </div>
  );
}

function AssignTestsModal({ isOpen, availableTests, onClose, onConfirm }) {
  const intl = useIntl();
  const [selectedTests, setSelectedTests] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterType, setFilterType] = useState("all");

  const filteredTests = availableTests.filter((test) => {
    const matchesSearch =
      test.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      test.code.toLowerCase().includes(searchQuery.toLowerCase());

    if (filterType === "unassigned") {
      return matchesSearch && !test.isAssigned;
    } else if (filterType === "all") {
      return matchesSearch;
    }

    return false;
  });

  const toggleTest = (testId) => {
    setSelectedTests((prev) =>
      prev.includes(testId)
        ? prev.filter((id) => id !== testId)
        : [...prev, testId],
    );
  };

  const toggleAll = () => {
    if (selectedTests.length === filteredTests.length) {
      setSelectedTests([]);
    } else {
      setSelectedTests(filteredTests.map((test) => test.id));
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.assign.tests.modal.title",
      })}
      passiveModal
      size="lg"
    >
      <div style={{ marginBottom: "1.5rem" }}>
        {/* Filters */}
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
                id: "test.search.placeholder",
              })}
              labelText={intl.formatMessage({ id: "test.search.label" })}
            />
          </div>

          <div style={{ minWidth: "150px" }}>
            <Select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              labelText={intl.formatMessage({ id: "test.filter.type" })}
            >
              <SelectItem value="all">
                {intl.formatMessage({ id: "test.filter.all" })}
              </SelectItem>
              <SelectItem value="unassigned">
                {intl.formatMessage({ id: "test.filter.unassigned" })}
              </SelectItem>
            </Select>
          </div>
        </div>

        {/* Tests List */}
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
              checked={selectedTests.length === filteredTests.length}
              onChange={toggleAll}
              indeterminate={
                selectedTests.length > 0 &&
                selectedTests.length < filteredTests.length
              }
            />
            <span style={{ fontWeight: "500" }}>
              {filteredTests.length}{" "}
              {intl.formatMessage({ id: "test.tests.available" })}
            </span>
          </div>

          <div style={{ padding: "0.5rem" }}>
            {filteredTests.map((test) => (
              <div
                key={test.id}
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
                  checked={selectedTests.includes(test.id)}
                  onChange={() => toggleTest(test.id)}
                />
                <div style={{ flex: 1 }}>
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
                      {test.code}
                    </span>
                    {test.isAssigned && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#da1e28",
                          fontStyle: "italic",
                        }}
                      >
                        ({intl.formatMessage({ id: "test.assigned.to" })}{" "}
                        {test.assignedTo})
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
          {selectedTests.length}{" "}
          {intl.formatMessage({ id: "test.tests.selected" })}
        </span>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" onClick={onClose}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            onClick={() => onConfirm(selectedTests)}
            disabled={selectedTests.length === 0}
          >
            {intl.formatMessage({ id: "button.assign.tests" })}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function ReassignTestsModal({ isOpen, tests, onClose, onConfirm }) {
  const intl = useIntl();
  const [destinationLabUnit, setDestinationLabUnit] = useState("");
  const [keepReference, setKeepReference] = useState(true);

  if (!isOpen) return null;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({ id: "labunit.reassign.tests.title" })}
      passiveModal
      size="md"
    >
      <div style={{ marginBottom: "1.5rem" }}>
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ margin: 0, color: "#525252" }}>
            {intl.formatMessage(
              { id: "labunit.reassign.tests.count" },
              { count: tests.length },
            )}
          </p>
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "500",
              color: "#161616",
            }}
          >
            {intl.formatMessage({ id: "labunit.reassign.tests.destination" })}:
          </label>
          <Select
            value={destinationLabUnit}
            onChange={(e) => setDestinationLabUnit(e.target.value)}
            labelText=""
          >
            <SelectItem value="">
              {intl.formatMessage({ id: "labunit.select.destination" })}
            </SelectItem>
            <SelectItem value="chem">
              {intl.formatMessage({ id: "labunit.clinical.chemistry" })}
            </SelectItem>
            <SelectItem value="hema">
              {intl.formatMessage({ id: "labunit.hematology" })}
            </SelectItem>
            <SelectItem value="micro">
              {intl.formatMessage({ id: "labunit.microbiology" })}
            </SelectItem>
            <SelectItem value="immu">
              {intl.formatMessage({ id: "labunit.immunology.serology" })}
            </SelectItem>
          </Select>
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <Checkbox
            checked={keepReference}
            onChange={(e) => setKeepReference(e.target.checked)}
            labelText={intl.formatMessage({
              id: "labunit.reassign.keep.reference",
            })}
          />
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <h4
            style={{
              marginBottom: "1rem",
              fontSize: "1rem",
              fontWeight: "600",
              color: "#161616",
            }}
          >
            {intl.formatMessage({ id: "labunit.tests.to.reassign" })}:
          </h4>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
              gap: "0.5rem",
              maxHeight: "200px",
              overflow: "auto",
              padding: "1rem",
              backgroundColor: "#f8f8f8",
              borderRadius: "4px",
              border: "1px solid #e0e0e0",
            }}
          >
            {tests.map((test) => (
              <div
                key={test.id}
                style={{
                  padding: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                  backgroundColor: "#ffffff",
                  borderRadius: "4px",
                  border: "1px solid #e0e0e0",
                }}
              >
                <span>
                  • {test.name} ({test.code})
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          justifyContent: "flex-end",
          borderTop: "1px solid #e0e0e0",
          paddingTop: "1rem",
        }}
      >
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({ id: "button.cancel" })}
        </Button>
        <Button
          onClick={() => onConfirm(destinationLabUnit, keepReference)}
          disabled={!destinationLabUnit}
        >
          {intl.formatMessage({ id: "button.reassign.tests" })}
        </Button>
      </div>
    </Modal>
  );
}
