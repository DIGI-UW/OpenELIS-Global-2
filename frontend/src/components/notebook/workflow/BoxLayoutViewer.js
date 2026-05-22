import React from "react";
import { Tooltip, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PropTypes from "prop-types";
import {
  getStorageCoordinateLabel,
  getRowHeaderLabel,
  findLayoutEntryForCell,
  countOccupiedCells,
} from "../../../utils/storagePositionUtils";
import "./BoxLayoutViewer.css";

/**
 * BoxLayoutViewer - Visual representation of a storage box / plate layout.
 * Shows which wells are occupied with sample information.
 */
function BoxLayoutViewer({
  boxId,
  layout = {},
  rows = 8,
  columns = 12,
  positionSchemaHint = "letter-number",
  onWellClick,
}) {
  const hint = positionSchemaHint || "letter-number";
  const rowIndices = Array.from({ length: rows }, (_, i) => i);
  const columnNumbers = Array.from({ length: columns }, (_, i) => i + 1);

  const getWellCoord = (rowIdx, colIdx) =>
    getStorageCoordinateLabel(rowIdx, colIdx, columns, hint);

  const getWellInfo = (rowIdx, colIdx) =>
    findLayoutEntryForCell(layout, rowIdx, colIdx, columns, hint) || null;

  const isOccupied = (rowIdx, colIdx) => !!getWellInfo(rowIdx, colIdx);

  const handleWellClick = (rowIdx, colIdx) => {
    if (onWellClick) {
      const wellCoord = getWellCoord(rowIdx, colIdx);
      onWellClick(wellCoord, getWellInfo(rowIdx, colIdx));
    }
  };

  const renderWellTooltip = (rowIdx, colIdx) => {
    const wellCoord = getWellCoord(rowIdx, colIdx);
    const info = getWellInfo(rowIdx, colIdx);
    if (!info) {
      return (
        <span>
          {wellCoord} -{" "}
          <FormattedMessage id="boxLayout.well.empty" defaultMessage="Empty" />
        </span>
      );
    }

    return (
      <div className="well-tooltip-content">
        <div>
          <strong>{wellCoord}</strong>
        </div>
        <div>Sample ID: {info.sampleItemId}</div>
        {info.externalId && <div>External: {info.externalId}</div>}
        {info.destinationType && (
          <div>Destination: {info.destinationType.replace("_", " ")}</div>
        )}
        {info.routedAt && <div>Routed: {info.routedAt}</div>}
      </div>
    );
  };

  const occupiedCount = countOccupiedCells(layout, rows, columns, hint);
  const totalWells = rows * columns;
  const occupancyPercent =
    totalWells > 0 ? Math.round((occupiedCount / totalWells) * 100) : 0;

  return (
    <div className="box-layout-viewer">
      <div className="box-layout-header">
        <span className="box-label">
          <FormattedMessage
            id="boxLayout.box"
            defaultMessage="Box {boxId}"
            values={{ boxId }}
          />
        </span>
        <div className="box-stats">
          <Tag
            type={
              occupancyPercent > 80
                ? "red"
                : occupancyPercent > 50
                  ? "blue"
                  : "green"
            }
          >
            {occupiedCount}/{totalWells} ({occupancyPercent}%)
          </Tag>
        </div>
      </div>

      <div className="box-grid-container">
        <div className="box-grid-row header-row">
          <div className="box-grid-cell corner-cell"></div>
          {columnNumbers.map((col) => (
            <div key={col} className="box-grid-cell header-cell">
              {col}
            </div>
          ))}
        </div>

        {rowIndices.map((rowIdx) => (
          <div key={rowIdx} className="box-grid-row">
            <div className="box-grid-cell row-header-cell">
              {getRowHeaderLabel(rowIdx, hint)}
            </div>

            {columnNumbers.map((col) => {
              const colIndex = col - 1;
              const wellCoord = getWellCoord(rowIdx, colIndex);
              const occupied = isOccupied(rowIdx, colIndex);

              return (
                <Tooltip
                  key={wellCoord}
                  align="top"
                  label={renderWellTooltip(rowIdx, colIndex)}
                >
                  <div
                    className={`box-grid-cell well-cell ${occupied ? "occupied" : "empty"}`}
                    onClick={() => handleWellClick(rowIdx, colIndex)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        handleWellClick(rowIdx, colIndex);
                      }
                    }}
                  >
                    <div className="well-dot"></div>
                  </div>
                </Tooltip>
              );
            })}
          </div>
        ))}
      </div>

      <div className="box-layout-legend">
        <div className="legend-item">
          <div className="well-dot empty"></div>
          <span>
            <FormattedMessage
              id="boxLayout.legend.empty"
              defaultMessage="Empty"
            />
          </span>
        </div>
        <div className="legend-item">
          <div className="well-dot occupied"></div>
          <span>
            <FormattedMessage
              id="boxLayout.legend.occupied"
              defaultMessage="Occupied"
            />
          </span>
        </div>
      </div>
    </div>
  );
}

BoxLayoutViewer.propTypes = {
  boxId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  layout: PropTypes.object,
  rows: PropTypes.number,
  columns: PropTypes.number,
  positionSchemaHint: PropTypes.string,
  onWellClick: PropTypes.func,
};

export default BoxLayoutViewer;
