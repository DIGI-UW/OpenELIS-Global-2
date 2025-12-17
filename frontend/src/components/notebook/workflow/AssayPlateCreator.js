import React, { useState, useMemo, useCallback } from "react";
import {
  NumberInput,
  TextInput,
  Button,
  Tile,
  Tag,
  InlineNotification,
} from "@carbon/react";
import { Add, TrashCan, View } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "./AssayPlateCreator.css";

/**
 * AssayPlateCreator - Component for creating and managing assay plates
 * for internal analysis routing. These are temporary plates NOT connected
 * to the hierarchical storage system.
 *
 * @param {Object} props
 * @param {Array} props.plates - Array of existing assay plates
 * @param {Function} props.onPlatesChange - Callback when plates array changes
 * @param {number|null} props.selectedPlateId - Currently selected plate ID
 * @param {Function} props.onPlateSelect - Callback when a plate is selected
 * @param {number} props.sampleCount - Number of samples to be assigned
 */
function AssayPlateCreator({
  plates = [],
  onPlatesChange,
  selectedPlateId,
  onPlateSelect,
  sampleCount = 0,
}) {
  const intl = useIntl();

  // New plate creation state
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newPlateName, setNewPlateName] = useState("");
  const [newPlateRows, setNewPlateRows] = useState(8);
  const [newPlateColumns, setNewPlateColumns] = useState(12);

  // Calculate total capacity and usage
  const plateStats = useMemo(() => {
    let totalCapacity = 0;
    let totalUsed = 0;

    plates.forEach((plate) => {
      const capacity = plate.rows * plate.columns;
      totalCapacity += capacity;
      totalUsed += plate.assignedCount || 0;
    });

    return {
      totalCapacity,
      totalUsed,
      totalAvailable: totalCapacity - totalUsed,
      platesNeeded: sampleCount > 0 ? Math.ceil(sampleCount / 96) : 0,
    };
  }, [plates, sampleCount]);

  // Handle creating a new plate
  const handleCreatePlate = useCallback(() => {
    if (!newPlateName.trim()) {
      return;
    }

    const newPlate = {
      id: `assay-plate-${Date.now()}`,
      name: newPlateName.trim(),
      rows: newPlateRows,
      columns: newPlateColumns,
      capacity: newPlateRows * newPlateColumns,
      assignedCount: 0,
      assignments: {}, // wellCoordinate -> sampleId mapping
      createdAt: new Date().toISOString(),
    };

    const updatedPlates = [...plates, newPlate];
    onPlatesChange(updatedPlates);

    // Reset form
    setNewPlateName("");
    setShowCreateForm(false);

    // Always auto-select the newly created plate for better UX
    onPlateSelect(newPlate.id);
  }, [
    newPlateName,
    newPlateRows,
    newPlateColumns,
    plates,
    onPlatesChange,
    onPlateSelect,
  ]);

  // Handle deleting a plate
  const handleDeletePlate = useCallback(
    (plateId) => {
      const updatedPlates = plates.filter((p) => p.id !== plateId);
      onPlatesChange(updatedPlates);

      // If deleted plate was selected, clear selection
      if (selectedPlateId === plateId) {
        onPlateSelect(null);
      }
    },
    [plates, onPlatesChange, selectedPlateId, onPlateSelect],
  );

  // Quick create standard plates
  const handleQuickCreate = useCallback(
    (type) => {
      let rows, columns, namePrefix;

      switch (type) {
        case "96":
          rows = 8;
          columns = 12;
          namePrefix = "96-Well Plate";
          break;
        case "48":
          rows = 6;
          columns = 8;
          namePrefix = "48-Well Plate";
          break;
        case "24":
          rows = 4;
          columns = 6;
          namePrefix = "24-Well Plate";
          break;
        default:
          return;
      }

      const plateNumber = plates.length + 1;
      const newPlate = {
        id: `assay-plate-${Date.now()}`,
        name: `${namePrefix} #${plateNumber}`,
        rows,
        columns,
        capacity: rows * columns,
        assignedCount: 0,
        assignments: {},
        createdAt: new Date().toISOString(),
      };

      const updatedPlates = [...plates, newPlate];
      onPlatesChange(updatedPlates);

      // Always auto-select the newly created plate for better UX
      onPlateSelect(newPlate.id);
    },
    [plates, onPlatesChange, onPlateSelect],
  );

  // Generate well coordinate from index
  const getWellCoordinate = (index, columns) => {
    const row = Math.floor(index / columns);
    const col = (index % columns) + 1;
    const rowLetter = String.fromCharCode(65 + row); // A, B, C, ...
    return `${rowLetter}${col}`;
  };

  // Render plate grid preview
  const renderPlatePreview = (plate) => {
    const wells = [];
    const totalWells = plate.rows * plate.columns;

    for (let i = 0; i < totalWells; i++) {
      const coord = getWellCoordinate(i, plate.columns);
      const isAssigned = plate.assignments && plate.assignments[coord];

      wells.push(
        <div
          key={coord}
          className={`preview-well ${isAssigned ? "assigned" : "empty"}`}
          title={isAssigned ? `${coord}: ${plate.assignments[coord]}` : coord}
        />,
      );
    }

    return (
      <div
        className="plate-preview-grid"
        style={{
          gridTemplateColumns: `repeat(${plate.columns}, 1fr)`,
        }}
      >
        {wells}
      </div>
    );
  };

  return (
    <div className="assay-plate-creator">
      {/* Header with sample count info */}
      <div className="creator-header">
        <h5>
          <FormattedMessage
            id="notebook.routing.assayPlates"
            defaultMessage="Assay Plates"
          />
        </h5>
        {sampleCount > 0 && (
          <Tag type="blue">
            {sampleCount}{" "}
            <FormattedMessage
              id="notebook.routing.samplesToAssign"
              defaultMessage="samples to assign"
            />
          </Tag>
        )}
      </div>

      {/* Capacity summary */}
      {plates.length > 0 && (
        <div className="capacity-summary">
          <span>
            <FormattedMessage
              id="notebook.routing.platesSummary"
              defaultMessage="{count} plate(s) | {available} wells available | {used} wells used"
              values={{
                count: plates.length,
                available: plateStats.totalAvailable,
                used: plateStats.totalUsed,
              }}
            />
          </span>
          {sampleCount > plateStats.totalAvailable && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "notebook.routing.needMorePlates",
                defaultMessage: "More plates needed",
              })}
              subtitle={intl.formatMessage(
                {
                  id: "notebook.routing.needMorePlatesDetail",
                  defaultMessage:
                    "Need {needed} more wells for all samples. Create additional plates.",
                },
                { needed: sampleCount - plateStats.totalAvailable },
              )}
              lowContrast
              hideCloseButton
              style={{ marginTop: "0.5rem" }}
            />
          )}
        </div>
      )}

      {/* Quick create buttons */}
      <div className="quick-create-buttons">
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => handleQuickCreate("96")}
        >
          <FormattedMessage
            id="notebook.routing.add96Well"
            defaultMessage="Add 96-Well"
          />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => handleQuickCreate("48")}
        >
          <FormattedMessage
            id="notebook.routing.add48Well"
            defaultMessage="Add 48-Well"
          />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => handleQuickCreate("24")}
        >
          <FormattedMessage
            id="notebook.routing.add24Well"
            defaultMessage="Add 24-Well"
          />
        </Button>
        <Button
          kind="ghost"
          size="sm"
          onClick={() => setShowCreateForm(!showCreateForm)}
        >
          <FormattedMessage
            id="notebook.routing.customPlate"
            defaultMessage="Custom..."
          />
        </Button>
      </div>

      {/* Custom plate creation form */}
      {showCreateForm && (
        <Tile className="create-plate-form">
          <h6>
            <FormattedMessage
              id="notebook.routing.createCustomPlate"
              defaultMessage="Create Custom Plate"
            />
          </h6>
          <div className="form-row">
            <TextInput
              id="plate-name"
              labelText={intl.formatMessage({
                id: "notebook.routing.plateName",
                defaultMessage: "Plate Name",
              })}
              value={newPlateName}
              onChange={(e) => setNewPlateName(e.target.value)}
              placeholder="e.g., ELISA Plate 1"
              size="sm"
            />
          </div>
          <div className="form-row dimensions">
            <NumberInput
              id="plate-rows"
              label={intl.formatMessage({
                id: "notebook.routing.rows",
                defaultMessage: "Rows",
              })}
              value={newPlateRows}
              onChange={(e, { value }) => setNewPlateRows(value)}
              min={1}
              max={16}
              size="sm"
            />
            <NumberInput
              id="plate-columns"
              label={intl.formatMessage({
                id: "notebook.routing.columns",
                defaultMessage: "Columns",
              })}
              value={newPlateColumns}
              onChange={(e, { value }) => setNewPlateColumns(value)}
              min={1}
              max={24}
              size="sm"
            />
            <span className="capacity-preview">
              = {newPlateRows * newPlateColumns} wells
            </span>
          </div>
          <div className="form-actions">
            <Button
              kind="primary"
              size="sm"
              onClick={handleCreatePlate}
              disabled={!newPlateName.trim()}
            >
              <FormattedMessage
                id="notebook.routing.createPlate"
                defaultMessage="Create Plate"
              />
            </Button>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => setShowCreateForm(false)}
            >
              <FormattedMessage
                id="notebook.routing.cancel"
                defaultMessage="Cancel"
              />
            </Button>
          </div>
        </Tile>
      )}

      {/* Plate list */}
      <div className="plates-list">
        {plates.length === 0 ? (
          <div className="empty-plates">
            <p>
              <FormattedMessage
                id="notebook.routing.noPlates"
                defaultMessage="No assay plates created. Click a button above to create one."
              />
            </p>
          </div>
        ) : (
          plates.map((plate) => (
            <Tile
              key={plate.id}
              className={`plate-tile ${selectedPlateId === plate.id ? "selected" : ""}`}
              onClick={() => onPlateSelect(plate.id)}
            >
              <div className="plate-header">
                <div className="plate-info">
                  <span className="plate-name">{plate.name}</span>
                  <span className="plate-dimensions">
                    {plate.rows}x{plate.columns} ({plate.capacity} wells)
                  </span>
                </div>
                <div className="plate-actions">
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={View}
                    iconDescription="View"
                    hasIconOnly
                    onClick={(e) => {
                      e.stopPropagation();
                      onPlateSelect(plate.id);
                    }}
                  />
                  <Button
                    kind="danger--ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    iconDescription="Delete"
                    hasIconOnly
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeletePlate(plate.id);
                    }}
                    disabled={plate.assignedCount > 0}
                  />
                </div>
              </div>
              <div className="plate-stats">
                <Tag type={plate.assignedCount > 0 ? "green" : "gray"}>
                  {plate.assignedCount}/{plate.capacity} assigned
                </Tag>
                {plate.assignedCount < plate.capacity && (
                  <Tag type="blue">
                    {plate.capacity - plate.assignedCount} available
                  </Tag>
                )}
              </div>
              {renderPlatePreview(plate)}
            </Tile>
          ))
        )}
      </div>
    </div>
  );
}

export default AssayPlateCreator;
