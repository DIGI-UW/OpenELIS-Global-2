/**
 * Coordinate labels and occupancy lookup for storage box grids.
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
  if (occupiedCoordinates[coordinate]) {
    return occupiedCoordinates[coordinate];
  }
  if (hasLegacyContinuousOccupancy(occupiedCoordinates, hint)) {
    const legacyKey = getLegacyContinuousCoordinate(rowIdx, colIdx, columns);
    return occupiedCoordinates[legacyKey];
  }
  return undefined;
}
