import React from "react";
import { Tooltip, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PropTypes from "prop-types";
import "./BoxLayoutViewer.css";

/**
 * BoxLayoutViewer - Visual representation of a 96-well plate layout.
 * Shows which wells are occupied with sample information.
 *
 * @param {Object} props
 * @param {number} props.boxId - The box ID
 * @param {Object} props.layout - Map of well coordinate to routing info
 * @param {number} props.rows - Number of rows (default 8 for A-H)
 * @param {number} props.columns - Number of columns (default 12)
 * @param {function} props.onWellClick - Optional click handler for wells
 */
function BoxLayoutViewer({
  boxId,
  layout = {},
  rows = 8,
  columns = 12,
  onWellClick,
}) {
  // Generate row letters (A-H for 8 rows)
  const rowLetters = Array.from({ length: rows }, (_, i) =>
    String.fromCharCode("A".charCodeAt(0) + i),
  );

  // Generate column numbers (1-12)
  const columnNumbers = Array.from({ length: columns }, (_, i) => i + 1);

  // Check if a well is occupied
  const isOccupied = (wellCoord) => {
    return layout[wellCoord] !== undefined;
  };

  // Get well info
  const getWellInfo = (wellCoord) => {
    return layout[wellCoord] || null;
  };

  // Handle well click
  const handleWellClick = (wellCoord) => {
    if (onWellClick) {
      onWellClick(wellCoord, getWellInfo(wellCoord));
    }
  };

  // Render well tooltip content
  const renderWellTooltip = (wellCoord) => {
    const info = getWellInfo(wellCoord);
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

  // Calculate stats
  const occupiedCount = Object.keys(layout).length;
  const totalWells = rows * columns;
  const occupancyPercent = Math.round((occupiedCount / totalWells) * 100);

  return (
    <div className="box-layout-viewer">
      {/* Header with stats */}
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

      {/* Grid layout */}
      <div className="box-grid-container">
        {/* Column headers */}
        <div className="box-grid-row header-row">
          <div className="box-grid-cell corner-cell"></div>
          {columnNumbers.map((col) => (
            <div key={col} className="box-grid-cell header-cell">
              {col}
            </div>
          ))}
        </div>

        {/* Data rows */}
        {rowLetters.map((row) => (
          <div key={row} className="box-grid-row">
            {/* Row header */}
            <div className="box-grid-cell row-header-cell">{row}</div>

            {/* Wells */}
            {columnNumbers.map((col) => {
              const wellCoord = `${row}${col}`;
              const occupied = isOccupied(wellCoord);

              return (
                <Tooltip
                  key={wellCoord}
                  align="top"
                  label={renderWellTooltip(wellCoord)}
                >
                  <div
                    className={`box-grid-cell well-cell ${occupied ? "occupied" : "empty"}`}
                    onClick={() => handleWellClick(wellCoord)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        handleWellClick(wellCoord);
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

      {/* Legend */}
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
  onWellClick: PropTypes.func,
};

export default BoxLayoutViewer;
