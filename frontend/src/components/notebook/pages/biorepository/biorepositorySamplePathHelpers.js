/**
 * AHRI BR-F-02 sample path formatting from storage assignment data.
 * Example: Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3
 */

const readText = (value) => {
  if (value === null || value === undefined) {
    return null;
  }
  const str = String(value).trim();
  return str === "" ? null : str;
};

const appendSegment = (segments, prefix, value) => {
  const text = readText(value);
  if (text) {
    segments.push(`${prefix} ${text}`);
  }
};

export const formatBrf02SamplePathFromHierarchical = (
  hierarchicalPath,
  positionCoordinate,
) => {
  const path = readText(hierarchicalPath);
  const position = readText(positionCoordinate);

  if (!path) {
    return position ? `Pos ${position}` : null;
  }

  const parts = path.split(/\s*>\s*/);
  const segments = [];
  appendSegment(segments, "Zn", parts[0]);
  appendSegment(segments, "FRZ", parts[1]);
  appendSegment(segments, "SH", parts[2]);
  appendSegment(segments, "RK", parts[3]);
  appendSegment(segments, "Box", parts[4]);
  appendSegment(segments, "Pos", position);

  return segments.length > 0 ? segments.join(" / ") : path;
};

/**
 * @param {object} sample search result or selected sample row
 * @returns {string|null}
 */
export const formatBrf02SamplePath = (sample) => {
  if (!sample) {
    return null;
  }

  const existing = readText(sample.samplePath);
  if (existing) {
    return existing;
  }

  const roomName =
    readText(sample.roomName) ||
    readText(sample.storageRoom) ||
    readText(sample?.data?.storageRoom);
  const deviceName =
    readText(sample.deviceName) ||
    readText(sample.storageFreezer) ||
    readText(sample?.data?.storageFreezer);
  const shelfLabel =
    readText(sample.shelfLabel) ||
    readText(sample.storageShelf) ||
    readText(sample?.data?.storageShelf);
  const rackLabel =
    readText(sample.rackLabel) ||
    readText(sample.storageRack) ||
    readText(sample?.data?.storageRack);
  const boxLabel =
    readText(sample.boxLabel) ||
    readText(sample.storageBox) ||
    readText(sample?.data?.storageBox);
  const positionCoordinate =
    readText(sample.positionCoordinate) ||
    readText(sample.storageWell) ||
    readText(sample?.data?.storageWell);

  if (roomName || deviceName || shelfLabel || rackLabel || boxLabel) {
    const segments = [];
    appendSegment(segments, "Zn", roomName);
    appendSegment(segments, "FRZ", deviceName);
    appendSegment(segments, "SH", shelfLabel);
    appendSegment(segments, "RK", rackLabel);
    appendSegment(segments, "Box", boxLabel);
    appendSegment(segments, "Pos", positionCoordinate);
    return segments.length > 0 ? segments.join(" / ") : null;
  }

  const hierarchicalPath =
    readText(sample.hierarchicalPath) ||
    readText(sample.storagePath) ||
    readText(sample.storageLocation) ||
    readText(sample?.data?.storagePath);

  return formatBrf02SamplePathFromHierarchical(
    hierarchicalPath,
    positionCoordinate,
  );
};
