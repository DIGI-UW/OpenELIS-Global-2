/**
 * Coordinate labels, normalization, and occupancy lookup for storage box grids.
 */

export function getStorageCoordinateLabel(
  rowIdx,
  colIdx,
  columns,
  positionSchemaHint,
) {
  const hint = positionSchemaHint || "letter-number";
  if (hint === "letter-number") {
    const letter = String.fromCharCode(65 + rowIdx);
    return `${letter}${colIdx + 1}`;
  }
  if (hint === "number-number") {
    return `${rowIdx + 1}-${colIdx + 1}`;
  }
  if (hint === "continuous") {
    return String(rowIdx * (columns || 0) + colIdx + 1);
  }
  return `${rowIdx + 1}-${colIdx + 1}`;
}

export function getRowHeaderLabel(rowIdx, positionSchemaHint) {
  const hint = positionSchemaHint || "letter-number";
  if (hint === "letter-number") {
    return String.fromCharCode(65 + rowIdx);
  }
  return String(rowIdx + 1);
}

/**
 * Parse any supported coordinate string into zero-based row/col indices.
 */
export function parseCoordinateToIndices(coord, columns) {
  if (!coord || typeof coord !== "string") {
    return null;
  }
  const trimmed = coord.trim();

  let match = trimmed.match(/^(\d+)-(\d+)$/);
  if (match) {
    return {
      rowIdx: parseInt(match[1], 10) - 1,
      colIdx: parseInt(match[2], 10) - 1,
    };
  }

  match = trimmed.match(/^([A-Za-z])(\d+)$/);
  if (match) {
    return {
      rowIdx: match[1].toUpperCase().charCodeAt(0) - 65,
      colIdx: parseInt(match[2], 10) - 1,
    };
  }

  match = trimmed.match(/^(\d+)$/);
  if (match && columns > 0) {
    const index = parseInt(match[1], 10) - 1;
    return {
      rowIdx: Math.floor(index / columns),
      colIdx: index % columns,
    };
  }

  return null;
}

export function normalizeCoordinateForSchema(
  coord,
  positionSchemaHint,
  columns,
) {
  if (!coord) {
    return coord;
  }
  const indices = parseCoordinateToIndices(coord, columns);
  if (!indices || indices.rowIdx < 0 || indices.colIdx < 0) {
    return coord;
  }
  return getStorageCoordinateLabel(
    indices.rowIdx,
    indices.colIdx,
    columns,
    positionSchemaHint,
  );
}

export function coordinatesReferToSameCell(a, b, hint, columns) {
  if (!a || !b) {
    return false;
  }
  const normalizedA = normalizeCoordinateForSchema(a, hint, columns);
  const normalizedB = normalizeCoordinateForSchema(b, hint, columns);
  return normalizedA === normalizedB;
}

export function findLayoutEntryForCell(
  layout,
  rowIdx,
  colIdx,
  columns,
  hint,
) {
  if (!layout) {
    return undefined;
  }
  const coordinate = getStorageCoordinateLabel(rowIdx, colIdx, columns, hint);
  if (layout[coordinate]) {
    return layout[coordinate];
  }
  for (const key of Object.keys(layout)) {
    if (coordinatesReferToSameCell(key, coordinate, hint, columns)) {
      return layout[key];
    }
  }
  return undefined;
}

export function countOccupiedCells(layout, rows, columns, hint) {
  if (!layout) {
    return 0;
  }
  let count = 0;
  for (let rowIdx = 0; rowIdx < rows; rowIdx++) {
    for (let colIdx = 0; colIdx < columns; colIdx++) {
      if (findLayoutEntryForCell(layout, rowIdx, colIdx, columns, hint)) {
        count++;
      }
    }
  }
  return count;
}

export function getLegacyContinuousCoordinate(rowIdx, colIdx, columns) {
  return String(rowIdx * (columns || 0) + colIdx + 1);
}

export function hasLegacyContinuousOccupancy(occupiedCoordinates, hint) {
  if (hint !== "number-number" || !occupiedCoordinates) {
    return false;
  }
  return Object.keys(occupiedCoordinates).some((coordinate) =>
    /^\d+$/.test(coordinate),
  );
}

export function resolveOccupancyAtCell(
  occupiedCoordinates,
  coordinate,
  rowIdx,
  colIdx,
  columns,
  hint,
) {
  if (!occupiedCoordinates) {
    return undefined;
  }
  const entry = findLayoutEntryForCell(
    occupiedCoordinates,
    rowIdx,
    colIdx,
    columns,
    hint,
  );
  if (entry) {
    return entry;
  }
  if (hasLegacyContinuousOccupancy(occupiedCoordinates, hint)) {
    const legacyKey = getLegacyContinuousCoordinate(rowIdx, colIdx, columns);
    return occupiedCoordinates[legacyKey];
  }
  return undefined;
}

/**
 * Assign selected samples to the next empty wells using the box position schema.
 */
export function autoPopulateEmptyWells(box, boxLayout, sampleIds) {
  const rows = box?.rows || 8;
  const columns = box?.columns || 12;
  const hint = box?.positionSchemaHint || "letter-number";
  const newAssignments = {};
  let sampleIndex = 0;

  for (let rowIdx = 0; rowIdx < rows; rowIdx++) {
    for (let colIdx = 0; colIdx < columns; colIdx++) {
      if (sampleIndex >= sampleIds.length) {
        break;
      }
      if (!findLayoutEntryForCell(boxLayout, rowIdx, colIdx, columns, hint)) {
        newAssignments[sampleIds[sampleIndex]] = getStorageCoordinateLabel(
          rowIdx,
          colIdx,
          columns,
          hint,
        );
        sampleIndex++;
      }
    }
    if (sampleIndex >= sampleIds.length) {
      break;
    }
  }

  return { assignments: newAssignments, assignedCount: sampleIndex };
}
