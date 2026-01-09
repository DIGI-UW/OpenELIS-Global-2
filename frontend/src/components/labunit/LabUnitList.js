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
  TextInput,
  Select,
  SelectItem,
  Grid,
  Column,
  Pagination,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Loading,
  AspectRatio,
  Modal,
  Checkbox,
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@carbon/react";

import {
  Search,
  Add as Plus,
  Download,
  Upload,
  Edit,
  Trash2,
  ChevronDown,
  ChevronRight,
  Info,
  Crossroads as X,
  ChevronLeft,
  Settings,
  Document,
  Users,
  Chemistry,
  DragVertical,
  AlertCircle,
  CheckCircle,
  ArrowRight,
  Layers,
  FolderDetails,
  LicenseThirdParty as Workflow,
  Package,
  OverflowMenuVertical,
  Copy,
  Building2,
  FlaskConical,
  Report,
  ContainerSoftware32,
  Branch,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function LabUnitList({ onSelectUnit, onEditUnit }) {
  const intl = useIntl();
  const [labUnits, setLabUnits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [totalItems, setTotalItems] = useState(0);
  const [statusFilter, setStatusFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [sortField, setSortField] = useState("displayOrder");
  const [sortOrder, setSortOrder] = useState("asc");
  const [draggedItem, setDraggedItem] = useState(null);
  const [dragOverIndex, setDragOverIndex] = useState(null);
  const [showDeactivateModal, setShowDeactivateModal] = useState(false);
  const [selectedUnit, setSelectedUnit] = useState(null);
  const [deactivateStep, setDeactivateStep] = useState(1);
  const [deactivateAction, setDeactivateAction] = useState(null);
  const [selectedDestination, setSelectedDestination] = useState("");
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [selectedPrograms, setSelectedPrograms] = useState([]);
  const [selectedWorkflows, setSelectedWorkflows] = useState([]);
  const [labUnitsForReassign, setLabUnitsForReassign] = useState([]);

  // Fetch lab units from API
  const fetchLabUnits = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        status: statusFilter,
        search: searchQuery,
        sortField,
        sortOrder,
      });

      const response = await fetch(`/rest/api/lab-units?${params}`);
      if (response.ok) {
        const data = await response.json();
        setLabUnits(data.labUnits || []);
        setTotalItems(data.total || 0);
      }
    } catch (error) {
      console.error("Error fetching lab units:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLabUnits();
  }, [currentPage, pageSize, statusFilter, searchQuery, sortField, sortOrder]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortOrder("asc");
    }
    setCurrentPage(1);
  };

  const SortIcon = ({ field }) => {
    if (sortField !== field)
      return <AspectRatio size={14} className="text-gray-400" />;
    return sortOrder === "asc" ? (
      <ChevronDown size={14} className="text-teal-600" />
    ) : (
      <ChevronRight size={14} className="text-teal-600 rotate-[-90deg]" />
    );
  };

  const getSortIcon = (field) => {
    if (sortField !== field) return null;
    return sortOrder === "asc" ? "ascending" : "descending";
  };

  const getStatusBadge = (isActive) => {
    return isActive ? (
      <Tag type="green">
        {intl.formatMessage({ id: "labunit.status.active" })}
      </Tag>
    ) : (
      <Tag type="gray">
        {intl.formatMessage({ id: "labunit.status.inactive" })}
      </Tag>
    );
  };

  const totalPages = Math.ceil(totalItems / pageSize);

  // Drag and drop handlers
  const handleDragStart = (e, index, unit) => {
    setDraggedItem({ index, unit });
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/html", e.target.innerHTML);
  };

  const handleDragOver = (e, index) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDragOverIndex(index);
  };

  const handleDragLeave = () => {
    setDragOverIndex(null);
  };

  const handleDrop = async (e, dropIndex) => {
    e.preventDefault();
    setDragOverIndex(null);

    if (!draggedItem || draggedItem.index === dropIndex) {
      return;
    }

    // Create new array with reordered items
    const reorderedUnits = [...labUnits];
    const [draggedUnit] = reorderedUnits.splice(draggedItem.index, 1);
    reorderedUnits.splice(dropIndex, 0, draggedUnit);

    // Update display orders
    const updatedUnits = reorderedUnits.map((unit, index) => ({
      ...unit,
      displayOrder: index + 1,
    }));

    setLabUnits(updatedUnits);

    // Persist to backend
    try {
      const orderItems = updatedUnits.map((unit) => ({
        id: unit.id,
        sortOrder: unit.displayOrder,
      }));

      const response = await fetch("/rest/api/lab-units/reorder", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          items: orderItems,
        }),
      });

      if (!response.ok) {
        // Revert on error
        setLabUnits(labUnits);
        console.error("Failed to reorder lab units");
      } else {
        await fetchLabUnits(); // Refresh from server
      }
    } catch (error) {
      // Revert on error
      setLabUnits(labUnits);
      console.error("Error reordering lab units:", error);
    }

    setDraggedItem(null);
  };

  const handleDragEnd = () => {
    setDraggedItem(null);
    setDragOverIndex(null);
  };

  // Deactivate modal handlers
  const handleDeactivateClick = (unit, e) => {
    e.stopPropagation();
    setSelectedUnit(unit);
    setSelectedTests(unit.testList || []);
    setSelectedPanels(unit.panelList || []);
    setSelectedPrograms(unit.programList || []);
    setSelectedWorkflows(unit.workflowList || []);
    setDeactivateStep(1);
    setDeactivateAction(null);
    setShowDeactivateModal(true);
    fetchLabUnitsForReassign();
  };

  const fetchLabUnitsForReassign = async () => {
    try {
      const response = await fetch("/rest/api/lab-units");
      if (response.ok) {
        const data = await response.json();
        setLabUnitsForReassign(data.labUnits || []);
      }
    } catch (error) {
      console.error("Error fetching lab units for reassign:", error);
    }
  };

  const handleDeactivateConfirm = async () => {
    if (!selectedUnit) return;

    try {
      let response;

      if (deactivateAction === "reassign") {
        // First reassign items
        if (selectedTests.length > 0) {
          response = await fetch(
            `/rest/api/lab-units/${selectedUnit.id}/tests/reassign`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                itemIds: selectedTests,
                targetLabUnitId: selectedDestination,
              }),
            },
          );
        }

        if (selectedPanels.length > 0) {
          response = await fetch(
            `/rest/api/lab-units/${selectedUnit.id}/panels/reassign`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                itemIds: selectedPanels,
                targetLabUnitId: selectedDestination,
              }),
            },
          );
        }

        if (selectedPrograms.length > 0) {
          response = await fetch(
            `/rest/api/lab-units/${selectedUnit.id}/programs/reassign`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                itemIds: selectedPrograms,
                targetLabUnitId: selectedDestination,
              }),
            },
          );
        }

        if (selectedWorkflows.length > 0) {
          response = await fetch(
            `/rest/api/lab-units/${selectedUnit.id}/workflows/reassign`,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                itemIds: selectedWorkflows,
                targetLabUnitId: selectedDestination,
              }),
            },
          );
        }
      }

      // Then deactivate the lab unit
      response = await fetch(
        `/rest/api/lab-units/${selectedUnit.id}/deactivate`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            cascade: deactivateAction === "deactivate-all",
            reason:
              deactivateAction === "deactivate-only"
                ? "Lab unit deactivated only"
                : deactivateAction === "deactivate-all"
                  ? "Lab unit and all items deactivated"
                  : "Lab unit deactivated after reassignment",
          }),
        },
      );

      if (response.ok) {
        setShowDeactivateModal(false);
        await fetchLabUnits(); // Refresh the list
      }
    } catch (error) {
      console.error("Error deactivating lab unit:", error);
    }
  };

  const toggleItem = (list, setList, item) => {
    if (list.includes(item)) {
      setList(list.filter((i) => i !== item));
    } else {
      setList([...list, item]);
    }
  };

  const selectAllInCategory = (sourceList, setList) => {
    setList([...sourceList]);
  };

  const deselectAllInCategory = (setList) => {
    setList([]);
  };

  const totalAffected =
    (selectedUnit?.testList?.length || 0) +
    (selectedUnit?.panelList?.length || 0) +
    (selectedUnit?.programList?.length || 0) +
    (selectedUnit?.workflowList?.length || 0);

  const totalSelected =
    selectedTests.length +
    selectedPanels.length +
    selectedPrograms.length +
    selectedWorkflows.length;

  return (
    <Grid fullWidth>
      <Column lg={16}>
        {/* Header */}
        <div style={{ marginBottom: "2rem" }}>
          <h1 style={{ marginBottom: "0.5rem" }}>
            {intl.formatMessage({ id: "labunit.setup.title" })}
          </h1>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            {intl.formatMessage({ id: "labunit.setup.subtitle" })}
          </p>

          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Button kind="secondary" renderIcon={Upload}>
              {intl.formatMessage({ id: "button.import" })}
            </Button>
            <Button kind="secondary" renderIcon={Download}>
              {intl.formatMessage({ id: "button.export" })}
            </Button>
            <Button renderIcon={Plus} onClick={() => onSelectUnit(null)}>
              {intl.formatMessage({ id: "button.add.lab.unit" })}
            </Button>
          </div>
        </div>

        {/* Filters Bar */}
        <div
          style={{
            marginBottom: "1.5rem",
            display: "flex",
            gap: "1rem",
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <div style={{ minWidth: "300px", flex: "1" }}>
            <TextInput
              id="lab-unit-search"
              labelText={intl.formatMessage({
                id: "labunit.search.placeholder",
              })}
              placeholder={intl.formatMessage({
                id: "labunit.search.placeholder",
              })}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setCurrentPage(1);
              }}
              renderIcon={Search}
            />
          </div>

          <Select
            id="status-filter"
            labelText={intl.formatMessage({ id: "labunit.filter.status" })}
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(1);
            }}
          >
            <SelectItem value="all">
              {intl.formatMessage({ id: "labunit.filter.all" })}
            </SelectItem>
            <SelectItem value="active">
              {intl.formatMessage({ id: "labunit.filter.active" })}
            </SelectItem>
            <SelectItem value="inactive">
              {intl.formatMessage({ id: "labunit.filter.inactive" })}
            </SelectItem>
          </Select>

          <div
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              fontWeight: "500",
            }}
          >
            {totalItems} {intl.formatMessage({ id: "labunit.count.label" })}
          </div>
        </div>

        {/* Table */}
        <div style={{ marginBottom: "1.5rem" }}>
          {loading ? (
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                padding: "3rem",
                gap: "1rem",
              }}
            >
              <Loading />
              <p>{intl.formatMessage({ id: "loading.lab.units" })}</p>
            </div>
          ) : (
            <DataTable rows={labUnits}>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader
                        sortDirection={
                          sortField === "displayOrder"
                            ? getSortIcon("displayOrder")
                            : "none"
                        }
                        onClick={() => handleSort("displayOrder")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.order" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "name" ? getSortIcon("name") : "none"
                        }
                        onClick={() => handleSort("name")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.name" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "code" ? getSortIcon("code") : "none"
                        }
                        onClick={() => handleSort("code")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.code" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "tests" ? getSortIcon("tests") : "none"
                        }
                        onClick={() => handleSort("tests")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.tests" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "panels"
                            ? getSortIcon("panels")
                            : "none"
                        }
                        onClick={() => handleSort("panels")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.panels" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "programs"
                            ? getSortIcon("programs")
                            : "none"
                        }
                        onClick={() => handleSort("programs")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.programs" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "workflows"
                            ? getSortIcon("workflows")
                            : "none"
                        }
                        onClick={() => handleSort("workflows")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.workflows" })}
                      </TableHeader>
                      <TableHeader>
                        {intl.formatMessage({ id: "labunit.column.status" })}
                      </TableHeader>
                      <TableHeader>
                        {intl.formatMessage({ id: "labunit.column.actions" })}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {labUnits.map((unit, index) => (
                      <TableRow
                        key={unit.id}
                        style={{
                          cursor: "pointer",
                          backgroundColor:
                            dragOverIndex === index ? "#f0f9ff" : "transparent",
                        }}
                        onClick={() => onEditUnit && onEditUnit(unit)}
                        draggable
                        onDragStart={(e) => handleDragStart(e, index, unit)}
                        onDragOver={(e) => handleDragOver(e, index)}
                        onDragLeave={handleDragLeave}
                        onDrop={(e) => handleDrop(e, index)}
                        onDragEnd={handleDragEnd}
                      >
                        <TableCell>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.5rem",
                            }}
                          >
                            <DragVertical
                              size={16}
                              style={{ cursor: "grab", color: "#525252" }}
                              onDragStart={(e) => {
                                e.stopPropagation();
                                handleDragStart(e, index, unit);
                              }}
                            />
                            {unit.displayOrder}
                          </div>
                        </TableCell>
                        <TableCell>
                          <span style={{ fontWeight: "500" }}>{unit.name}</span>
                        </TableCell>
                        <TableCell>
                          <span
                            style={{
                              fontFamily: "monospace",
                              fontSize: "0.875rem",
                            }}
                          >
                            {unit.code}
                          </span>
                        </TableCell>
                        <TableCell>{unit.tests}</TableCell>
                        <TableCell>{unit.panels}</TableCell>
                        <TableCell>{unit.programs}</TableCell>
                        <TableCell>{unit.workflows}</TableCell>
                        <TableCell>{getStatusBadge(unit.isActive)}</TableCell>
                        <TableCell>
                          <div
                            style={{
                              display: "flex",
                              gap: "0.25rem",
                              alignItems: "center",
                            }}
                          >
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Edit}
                              onClick={(e) => {
                                e.stopPropagation();
                                onEditUnit && onEditUnit(unit);
                              }}
                              title={intl.formatMessage({ id: "button.edit" })}
                            />
                            <OverflowMenu size="sm">
                              <OverflowMenuItem
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDeactivateClick(unit, e);
                                }}
                              >
                                {selectedUnit?.isActive
                                  ? intl.formatMessage({
                                      id: "button.deactivate",
                                    })
                                  : intl.formatMessage({
                                      id: "button.activate",
                                    })}
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

        {/* Pagination */}
        {!loading && totalItems > 0 && (
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: "1rem",
            }}
          >
            <div style={{ fontSize: "0.875rem", color: "#525252" }}>
              {intl.formatMessage(
                { id: "pagination.showing.items" },
                {
                  start: (currentPage - 1) * pageSize + 1,
                  end: Math.min(currentPage * pageSize, totalItems),
                  total: totalItems,
                },
              )}
            </div>

            <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
              <Select
                id="page-size"
                hideLabel
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(1);
                }}
                size="sm"
              >
                <SelectItem value={25}>25</SelectItem>
                <SelectItem value={50}>50</SelectItem>
                <SelectItem value={100}>100</SelectItem>
              </Select>

              <Pagination
                backwardText={intl.formatMessage({ id: "pagination.previous" })}
                forwardText={intl.formatMessage({ id: "pagination.next" })}
                page={currentPage}
                pageSize={pageSize}
                totalItems={totalItems}
                pagesHidden={false}
                onChange={({ page }) => setCurrentPage(page)}
              />
            </div>
          </div>
        )}
      </Column>

      {/* Deactivate From List Modal */}
      {showDeactivateModal && selectedUnit && (
        <ComposedModal
          open={showDeactivateModal}
          onClose={() => setShowDeactivateModal(false)}
          size="lg"
        >
          <ModalHeader
            title={intl.formatMessage({
              id: selectedUnit.isActive
                ? "labunit.deactivate.title"
                : "labunit.activate.title",
            })}
            subtitle={intl.formatMessage({
              id: selectedUnit.isActive
                ? "labunit.deactivate.subtitle"
                : "labunit.activate.subtitle",
            })}
          />

          {/* Step 1: Warning and Options */}
          {deactivateStep === 1 && (
            <ModalBody>
              <div style={{ marginBottom: "1.5rem" }}>
                <p style={{ fontWeight: "500", marginBottom: "0.5rem" }}>
                  {selectedUnit.name} ({selectedUnit.code})
                </p>
                {selectedUnit.isActive && (
                  <div
                    style={{
                      backgroundColor: "#fef7e6",
                      border: "1px solid #f7c948",
                      borderRadius: "4px",
                      padding: "1rem",
                      marginBottom: "1.5rem",
                    }}
                  >
                    <p
                      style={{
                        color: "#575400",
                        fontWeight: "500",
                        marginBottom: "0.5rem",
                      }}
                    >
                      {intl.formatMessage({
                        id: "labunit.deactivate.warning.message",
                      })}
                    </p>
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "repeat(2, 1fr)",
                        gap: "0.75rem",
                      }}
                    >
                      {selectedUnit.workflowList?.length > 0 && (
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            color: "#575400",
                            fontSize: "0.875rem",
                          }}
                        >
                          <Workflow size={14} />
                          <span>
                            {intl.formatMessage(
                              { id: "labunit.items.workflows.count" },
                              { count: selectedUnit.workflowList.length },
                            )}
                          </span>
                        </div>
                      )}
                      {selectedUnit.programList?.length > 0 && (
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            color: "#575400",
                            fontSize: "0.875rem",
                          }}
                        >
                          <Report size={14} />
                          <span>
                            {intl.formatMessage(
                              { id: "labunit.items.programs.count" },
                              { count: selectedUnit.programList.length },
                            )}
                          </span>
                        </div>
                      )}
                      {selectedUnit.panelList?.length > 0 && (
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            color: "#575400",
                            fontSize: "0.875rem",
                          }}
                        >
                          <Layers size={14} />
                          <span>
                            {intl.formatMessage(
                              { id: "labunit.items.panels.count" },
                              { count: selectedUnit.panelList.length },
                            )}
                          </span>
                        </div>
                      )}
                      {selectedUnit.testList?.length > 0 && (
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            color: "#575400",
                            fontSize: "0.875rem",
                          }}
                        >
                          <Chemistry size={14} />
                          <span>
                            {intl.formatMessage(
                              { id: "labunit.items.tests.count" },
                              { count: selectedUnit.tests },
                            )}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                <p style={{ fontWeight: "500", marginBottom: "1rem" }}>
                  {intl.formatMessage({
                    id: "labunit.deactivate.options.title",
                  })}
                </p>

                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.75rem",
                  }}
                >
                  <label
                    style={{
                      display: "flex",
                      alignItems: "flex-start",
                      gap: "0.75rem",
                      padding: "1rem",
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      cursor: "pointer",
                      backgroundColor:
                        deactivateAction === "deactivate-only"
                          ? "#f0f9ff"
                          : "white",
                      borderColor:
                        deactivateAction === "deactivate-only"
                          ? "#0f62fe"
                          : "#e0e0e0",
                    }}
                  >
                    <input
                      type="radio"
                      name="deactivate-action"
                      value="deactivate-only"
                      checked={deactivateAction === "deactivate-only"}
                      onChange={() => setDeactivateAction("deactivate-only")}
                      style={{ marginTop: "0.125rem" }}
                    />
                    <div>
                      <p style={{ fontWeight: "500", margin: "0 0 0.25rem 0" }}>
                        {intl.formatMessage({ id: "labunit.deactivate.only" })}
                      </p>
                      <p
                        style={{
                          color: "#525252",
                          fontSize: "0.875rem",
                          margin: 0,
                        }}
                      >
                        {intl.formatMessage({
                          id: "labunit.deactivate.only.description",
                        })}
                      </p>
                    </div>
                  </label>

                  {selectedUnit.isActive && (
                    <label
                      style={{
                        display: "flex",
                        alignItems: "flex-start",
                        gap: "0.75rem",
                        padding: "1rem",
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        cursor: "pointer",
                        backgroundColor:
                          deactivateAction === "reassign" ? "#f0f9ff" : "white",
                        borderColor:
                          deactivateAction === "reassign"
                            ? "#0f62fe"
                            : "#e0e0e0",
                      }}
                    >
                      <input
                        type="radio"
                        name="deactivate-action"
                        value="reassign"
                        checked={deactivateAction === "reassign"}
                        onChange={() => setDeactivateAction("reassign")}
                        style={{ marginTop: "0.125rem" }}
                      />
                      <div>
                        <p
                          style={{ fontWeight: "500", margin: "0 0 0.25rem 0" }}
                        >
                          {intl.formatMessage({ id: "labunit.reassign.items" })}
                        </p>
                        <p
                          style={{
                            color: "#525252",
                            fontSize: "0.875rem",
                            margin: 0,
                          }}
                        >
                          {intl.formatMessage({
                            id: "labunit.reassign.items.description",
                          })}
                        </p>
                      </div>
                    </label>
                  )}

                  {selectedUnit.isActive && (
                    <label
                      style={{
                        display: "flex",
                        alignItems: "flex-start",
                        gap: "0.75rem",
                        padding: "1rem",
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        cursor: "pointer",
                        backgroundColor:
                          deactivateAction === "deactivate-all"
                            ? "#f0f9ff"
                            : "white",
                        borderColor:
                          deactivateAction === "deactivate-all"
                            ? "#0f62fe"
                            : "#e0e0e0",
                      }}
                    >
                      <input
                        type="radio"
                        name="deactivate-action"
                        value="deactivate-all"
                        checked={deactivateAction === "deactivate-all"}
                        onChange={() => setDeactivateAction("deactivate-all")}
                        style={{ marginTop: "0.125rem" }}
                      />
                      <div>
                        <p
                          style={{ fontWeight: "500", margin: "0 0 0.25rem 0" }}
                        >
                          {intl.formatMessage({ id: "labunit.deactivate.all" })}
                        </p>
                        <p
                          style={{
                            color: "#525252",
                            fontSize: "0.875rem",
                            margin: 0,
                          }}
                        >
                          {intl.formatMessage({
                            id: "labunit.deactivate.all.description",
                          })}
                        </p>
                      </div>
                    </label>
                  )}
                </div>
              </div>
            </ModalBody>
          )}

          {/* Step 2: Select items to reassign */}
          {deactivateStep === 2 && (
            <ModalBody style={{ maxHeight: "60vh", overflow: "auto" }}>
              <div style={{ marginBottom: "1.5rem" }}>
                <label
                  style={{
                    display: "block",
                    fontWeight: "500",
                    marginBottom: "0.5rem",
                  }}
                >
                  {intl.formatMessage({ id: "labunit.reassign.to" })}{" "}
                  <span style={{ color: "#da1e28" }}>*</span>
                </label>
                <Select
                  id="destination-lab-unit"
                  value={selectedDestination}
                  onChange={(e) => setSelectedDestination(e.target.value)}
                  labelText=""
                >
                  <SelectItem value="">
                    {intl.formatMessage({ id: "labunit.select.destination" })}
                  </SelectItem>
                  {labUnitsForReassign
                    .filter((lu) => lu.id !== selectedUnit.id)
                    .map((lu) => (
                      <SelectItem key={lu.id} value={lu.id}>
                        {lu.name} ({lu.code})
                      </SelectItem>
                    ))}
                </Select>
              </div>

              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                {/* Workflows */}
                {selectedUnit.workflowList?.length > 0 && (
                  <div
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#f8f8f8",
                        padding: "0.75rem 1rem",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Workflow size={16} />
                        <span style={{ fontWeight: "500" }}>
                          {intl.formatMessage({
                            id: "labunit.items.workflows",
                          })}
                        </span>
                        <span
                          style={{ color: "#525252", fontSize: "0.875rem" }}
                        >
                          {intl.formatMessage(
                            { id: "labunit.items.selected.count" },
                            {
                              selected: selectedWorkflows.length,
                              total: selectedUnit.workflowList.length,
                            },
                          )}
                        </span>
                      </div>
                      <div style={{ display: "flex", gap: "0.5rem" }}>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            selectAllInCategory(
                              selectedUnit.workflowList,
                              setSelectedWorkflows,
                            )
                          }
                        >
                          {intl.formatMessage({ id: "button.select.all" })}
                        </Button>
                        <span style={{ color: "#8d8d8d" }}>|</span>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            deselectAllInCategory(setSelectedWorkflows)
                          }
                        >
                          {intl.formatMessage({ id: "button.deselect.all" })}
                        </Button>
                      </div>
                    </div>
                    <div
                      style={{
                        padding: "0.5rem",
                        maxHeight: "120px",
                        overflow: "auto",
                      }}
                    >
                      {selectedUnit.workflowList.map((item, idx) => (
                        <label
                          key={idx}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            padding: "0.5rem",
                            cursor: "pointer",
                            borderRadius: "4px",
                          }}
                        >
                          <Checkbox
                            checked={selectedWorkflows.includes(item)}
                            onChange={() =>
                              toggleItem(
                                selectedWorkflows,
                                setSelectedWorkflows,
                                item,
                              )
                            }
                          />
                          <span style={{ fontSize: "0.875rem" }}>{item}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                )}

                {/* Programs */}
                {selectedUnit.programList?.length > 0 && (
                  <div
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#f8f8f8",
                        padding: "0.75rem 1rem",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Report size={16} />
                        <span style={{ fontWeight: "500" }}>
                          {intl.formatMessage({ id: "labunit.items.programs" })}
                        </span>
                        <span
                          style={{ color: "#525252", fontSize: "0.875rem" }}
                        >
                          {intl.formatMessage(
                            { id: "labunit.items.selected.count" },
                            {
                              selected: selectedPrograms.length,
                              total: selectedUnit.programList.length,
                            },
                          )}
                        </span>
                      </div>
                      <div style={{ display: "flex", gap: "0.5rem" }}>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            selectAllInCategory(
                              selectedUnit.programList,
                              setSelectedPrograms,
                            )
                          }
                        >
                          {intl.formatMessage({ id: "button.select.all" })}
                        </Button>
                        <span style={{ color: "#8d8d8d" }}>|</span>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            deselectAllInCategory(setSelectedPrograms)
                          }
                        >
                          {intl.formatMessage({ id: "button.deselect.all" })}
                        </Button>
                      </div>
                    </div>
                    <div
                      style={{
                        padding: "0.5rem",
                        maxHeight: "120px",
                        overflow: "auto",
                      }}
                    >
                      {selectedUnit.programList.map((item, idx) => (
                        <label
                          key={idx}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            padding: "0.5rem",
                            cursor: "pointer",
                            borderRadius: "4px",
                          }}
                        >
                          <Checkbox
                            checked={selectedPrograms.includes(item)}
                            onChange={() =>
                              toggleItem(
                                selectedPrograms,
                                setSelectedPrograms,
                                item,
                              )
                            }
                          />
                          <span style={{ fontSize: "0.875rem" }}>{item}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                )}

                {/* Panels */}
                {selectedUnit.panelList?.length > 0 && (
                  <div
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#f8f8f8",
                        padding: "0.75rem 1rem",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Layers size={16} />
                        <span style={{ fontWeight: "500" }}>
                          {intl.formatMessage({ id: "labunit.items.panels" })}
                        </span>
                        <span
                          style={{ color: "#525252", fontSize: "0.875rem" }}
                        >
                          {intl.formatMessage(
                            { id: "labunit.items.selected.count" },
                            {
                              selected: selectedPanels.length,
                              total: selectedUnit.panelList.length,
                            },
                          )}
                        </span>
                      </div>
                      <div style={{ display: "flex", gap: "0.5rem" }}>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            selectAllInCategory(
                              selectedUnit.panelList,
                              setSelectedPanels,
                            )
                          }
                        >
                          {intl.formatMessage({ id: "button.select.all" })}
                        </Button>
                        <span style={{ color: "#8d8d8d" }}>|</span>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            deselectAllInCategory(setSelectedPanels)
                          }
                        >
                          {intl.formatMessage({ id: "button.deselect.all" })}
                        </Button>
                      </div>
                    </div>
                    <div
                      style={{
                        padding: "0.5rem",
                        maxHeight: "120px",
                        overflow: "auto",
                      }}
                    >
                      {selectedUnit.panelList.map((item, idx) => (
                        <label
                          key={idx}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            padding: "0.5rem",
                            cursor: "pointer",
                            borderRadius: "4px",
                          }}
                        >
                          <Checkbox
                            checked={selectedPanels.includes(item)}
                            onChange={() =>
                              toggleItem(
                                selectedPanels,
                                setSelectedPanels,
                                item,
                              )
                            }
                          />
                          <span style={{ fontSize: "0.875rem" }}>{item}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                )}

                {/* Tests */}
                {selectedUnit.testList?.length > 0 && (
                  <div
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#f8f8f8",
                        padding: "0.75rem 1rem",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Chemistry size={16} />
                        <span style={{ fontWeight: "500" }}>
                          {intl.formatMessage({ id: "labunit.items.tests" })}
                        </span>
                        <span
                          style={{ color: "#525252", fontSize: "0.875rem" }}
                        >
                          {intl.formatMessage(
                            { id: "labunit.items.selected.count" },
                            {
                              selected: selectedTests.length,
                              total: selectedUnit.testList.length,
                            },
                          )}
                        </span>
                      </div>
                      <div style={{ display: "flex", gap: "0.5rem" }}>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            selectAllInCategory(
                              selectedUnit.testList,
                              setSelectedTests,
                            )
                          }
                        >
                          {intl.formatMessage({ id: "button.select.all" })}
                        </Button>
                        <span style={{ color: "#8d8d8d" }}>|</span>
                        <Button
                          size="sm"
                          kind="ghost"
                          onClick={() =>
                            deselectAllInCategory(setSelectedTests)
                          }
                        >
                          {intl.formatMessage({ id: "button.deselect.all" })}
                        </Button>
                      </div>
                    </div>
                    <div
                      style={{
                        padding: "0.5rem",
                        maxHeight: "120px",
                        overflow: "auto",
                      }}
                    >
                      {selectedUnit.testList.map((item, idx) => (
                        <label
                          key={idx}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            padding: "0.5rem",
                            cursor: "pointer",
                            borderRadius: "4px",
                          }}
                        >
                          <Checkbox
                            checked={selectedTests.includes(item)}
                            onChange={() =>
                              toggleItem(selectedTests, setSelectedTests, item)
                            }
                          />
                          <span style={{ fontSize: "0.875rem" }}>{item}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {totalSelected > 0 && (
                <div
                  style={{
                    backgroundColor: "#e5f6ff",
                    border: "1px solid #4589ff",
                    borderRadius: "4px",
                    padding: "0.75rem",
                    marginTop: "1rem",
                  }}
                >
                  <p
                    style={{
                      color: "#0043ce",
                      fontSize: "0.875rem",
                      margin: 0,
                    }}
                  >
                    <strong>{totalSelected}</strong>{" "}
                    {intl.formatMessage(
                      { id: "labunit.reassign.summary" },
                      { count: totalSelected },
                    )}
                    {totalAffected - totalSelected > 0 && (
                      <span>
                        {" "}
                        {intl.formatMessage(
                          { id: "labunit.reassign.remaining" },
                          { remaining: totalAffected - totalSelected },
                        )}
                      </span>
                    )}
                  </p>
                </div>
              )}
            </ModalBody>
          )}

          {/* Step 3: Confirm deactivate all */}
          {deactivateStep === 3 && (
            <ModalBody>
              <div
                style={{
                  backgroundColor: "#ffd7d9",
                  border: "1px solid #da1e28",
                  borderRadius: "4px",
                  padding: "1rem",
                  marginBottom: "1.5rem",
                }}
              >
                <p
                  style={{
                    color: "#da1e28",
                    fontWeight: "500",
                    marginBottom: "0.5rem",
                  }}
                >
                  {intl.formatMessage({
                    id: "labunit.confirm.bulk.deactivation",
                  })}
                </p>
                <p
                  style={{ color: "#750e13", fontSize: "0.875rem", margin: 0 }}
                >
                  {intl.formatMessage({
                    id: "labunit.bulk.deactivation.message",
                  })}
                </p>
              </div>

              <div style={{ marginBottom: "1rem" }}>
                <p style={{ color: "#525252", marginBottom: "0.75rem" }}>
                  {intl.formatMessage({ id: "labunit.will.be.deactivated" })}:
                </p>

                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.5rem",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                      color: "#750e13",
                    }}
                  >
                    <X size={16} />
                    <span style={{ fontWeight: "500" }}>
                      {selectedUnit.name}
                    </span>{" "}
                    {intl.formatMessage({ id: "labunit.deactivate.item.unit" })}
                  </div>
                  {selectedUnit.tests > 0 && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        color: "#750e13",
                      }}
                    >
                      <X size={16} />
                      <span>
                        {intl.formatMessage(
                          { id: "labunit.items.tests.count" },
                          { count: selectedUnit.tests },
                        )}
                      </span>
                    </div>
                  )}
                  {selectedUnit.panelList?.length > 0 && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        color: "#750e13",
                      }}
                    >
                      <X size={16} />
                      <span>
                        {intl.formatMessage(
                          { id: "labunit.items.panels.count" },
                          { count: selectedUnit.panelList.length },
                        )}
                      </span>
                    </div>
                  )}
                  {selectedUnit.programList?.length > 0 && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        color: "#750e13",
                      }}
                    >
                      <X size={16} />
                      <span>
                        {intl.formatMessage(
                          { id: "labunit.items.programs.count" },
                          { count: selectedUnit.programList.length },
                        )}
                      </span>
                    </div>
                  )}
                  {selectedUnit.workflowList?.length > 0 && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                        color: "#750e13",
                      }}
                    >
                      <X size={16} />
                      <span>
                        {intl.formatMessage(
                          { id: "labunit.items.workflows.count" },
                          { count: selectedUnit.workflowList.length },
                        )}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                {intl.formatMessage({
                  id: "labunit.reversible.action.message",
                })}
              </p>
            </ModalBody>
          )}

          <ModalFooter>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                width: "100%",
              }}
            >
              {deactivateStep > 1 && (
                <Button
                  kind="secondary"
                  onClick={() => setDeactivateStep(deactivateStep - 1)}
                >
                  {intl.formatMessage({ id: "button.back" })}
                </Button>
              )}

              <div
                style={{ display: "flex", gap: "0.5rem", marginLeft: "auto" }}
              >
                <Button
                  kind="secondary"
                  onClick={() => setShowDeactivateModal(false)}
                >
                  {intl.formatMessage({ id: "button.cancel" })}
                </Button>

                {deactivateStep === 1 && (
                  <Button
                    onClick={() => {
                      if (deactivateAction === "reassign") {
                        setDeactivateStep(2);
                      } else if (deactivateAction === "deactivate-all") {
                        setDeactivateStep(3);
                      } else {
                        handleDeactivateConfirm();
                      }
                    }}
                    disabled={!deactivateAction}
                  >
                    {intl.formatMessage({ id: "button.continue" })}
                  </Button>
                )}

                {deactivateStep === 2 && (
                  <Button
                    onClick={handleDeactivateConfirm}
                    disabled={!selectedDestination || totalSelected === 0}
                  >
                    {intl.formatMessage({ id: "button.reassign.deactivate" })}
                  </Button>
                )}

                {deactivateStep === 3 && (
                  <Button kind="danger" onClick={handleDeactivateConfirm}>
                    {intl.formatMessage({ id: "button.deactivate.all" })}
                  </Button>
                )}
              </div>
            </div>
          </ModalFooter>
        </ComposedModal>
      )}
    </Grid>
  );
}
