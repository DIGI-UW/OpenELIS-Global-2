/**
 * The rules every identified row on the case view follows: which cassettes
 * and slides are in use, which are only on this screen, what each is called
 * and how its labels are fetched. No React, no I/O.
 */

export function isDeactivatedRow(row) {
  return row?.active === false;
}

/**
 * The rows still in use. A row without the flag is in use: the server always
 * sends it, but a row added on this screen has none.
 */
export function activeRows(rows) {
  return (rows ?? []).filter((row) => !isDeactivatedRow(row));
}

/**
 * Whether a row exists only on this screen; the server names it on save.
 */
export function isUnsavedRow(row) {
  return row?.id === null || row?.id === undefined;
}

/**
 * The rows a count may include: in use and known to the server. A row added
 * on this screen is not work recorded until the save that names it.
 */
export function countedRows(rows) {
  return activeRows(rows).filter((row) => !isUnsavedRow(row));
}

export function deactivatedCount(rows) {
  return (rows ?? []).length - activeRows(rows).length;
}

/**
 * A new slide that names no block yet, which the server refuses to save.
 */
export function needsParentBlock(slide) {
  return isUnsavedRow(slide) && slide?.blockId == null;
}

/**
 * An unsaved row's place among the unsaved rows of its list, counted from
 * one, so "New cassette 1" is the first new one whatever was saved before it.
 */
export function unsavedPosition(rows, index) {
  return (rows ?? []).slice(0, index + 1).filter(isUnsavedRow).length;
}

// The same fallback order as the server's displayIdentifier(), so a legacy
// row reads the same on the screen, the label and the audit trail.
function displayIdentifier(row, numberKey, idPrefix) {
  const designation = row?.designation?.trim();
  if (designation) {
    return designation;
  }
  if (row?.[numberKey] !== null && row?.[numberKey] !== undefined) {
    return String(row[numberKey]);
  }
  if (row && !isUnsavedRow(row)) {
    return idPrefix + row.id;
  }
  return "";
}

export function blockIdentifier(block) {
  return displayIdentifier(block, "blockNumber", "B");
}

export function slideIdentifier(slide) {
  return displayIdentifier(slide, "slideNumber", "S");
}

/**
 * The block a slide was cut from. Ids arrive as numbers or strings depending
 * on the path that produced the row, so they are compared as strings.
 */
export function parentBlockOf(slide, blocks) {
  if (slide?.blockId === null || slide?.blockId === undefined) {
    return undefined;
  }
  return (blocks ?? []).find(
    (block) =>
      block.id !== null &&
      block.id !== undefined &&
      String(block.id) === String(slide.blockId),
  );
}

/**
 * A block has been embedded, so it is a verified object; a cassette is still
 * waiting for embedding.
 */
export function cassetteStateBadge(block) {
  return block?.cassetteState === "BLOCK"
    ? { kind: "verified", textKey: "pathology.label.block" }
    : { kind: "pending", textKey: "pathology.label.cassette" };
}

/**
 * The servlet prints every saved label of the case from its accession
 * number, so the code is the case's, never a row's.
 */
export function labelStreamUrl(serverBaseUrl, labelType, labNumber) {
  return (
    serverBaseUrl +
    "/LabelMakerServlet?labelType=" +
    labelType +
    "&code=" +
    encodeURIComponent(labNumber)
  );
}

/**
 * The words a row's deactivation is announced in. A slide is named by its
 * barcode because its designation repeats under every block.
 */
export function objectNaming(kind, row) {
  if (kind === "slide") {
    return {
      actionKey: "pathology.action.deactivateSlide",
      headingKey: "pathology.modal.deactivateSlideHeading",
      toastKey: "pathology.toast.slideDeactivated",
      values: { barcode: row?.barcode || slideIdentifier(row) },
    };
  }
  if (row?.cassetteState === "BLOCK") {
    return {
      actionKey: "pathology.action.deactivateBlock",
      headingKey: "pathology.modal.deactivateBlockHeading",
      toastKey: "pathology.toast.blockDeactivated",
      values: { designation: blockIdentifier(row) },
    };
  }
  return {
    actionKey: "pathology.action.deactivateCassette",
    headingKey: "pathology.modal.deactivateCassetteHeading",
    toastKey: "pathology.toast.cassetteDeactivated",
    values: { designation: blockIdentifier(row) },
  };
}

/**
 * What an unsaved row is called until the server names it.
 */
export function newRowName(intl, kind, position) {
  return intl.formatMessage(
    {
      id:
        kind === "slide"
          ? "pathology.label.newSlide"
          : "pathology.label.newCassette",
    },
    { position },
  );
}

/**
 * What a row is called wherever it is named: a saved slide by its barcode, a
 * saved cassette or block by its designation, an unsaved row by its place.
 */
export function rowObjectName(intl, kind, row, position) {
  if (isUnsavedRow(row)) {
    return newRowName(intl, kind, position);
  }
  return kind === "slide"
    ? objectNaming(kind, row).values.barcode
    : blockIdentifier(row);
}

let nextClientKey = 0;

/**
 * A key for a row added on this screen, so React keeps each unsaved row's
 * own controls when an earlier one is removed.
 */
export function withClientKey(row) {
  nextClientKey += 1;
  return { ...row, clientKey: "new-" + nextClientKey };
}

export function stripClientKeys(rows) {
  if (!rows) {
    return rows;
  }
  return rows.map((row) => {
    const posted = { ...row };
    delete posted.clientKey;
    return posted;
  });
}

/**
 * The edits a section makes to one of the case's lists. Each replaces the row
 * rather than writing through to it, so a refused save leaves nothing behind.
 */
export function rowListEditors(listKey, updateCase) {
  return {
    patchRow: (index, patch) =>
      updateCase((prev) => ({
        [listKey]: (prev[listKey] ?? []).map((row, position) =>
          position === index ? { ...row, ...patch } : row,
        ),
      })),
    removeRow: (index) =>
      updateCase((prev) => ({
        [listKey]: (prev[listKey] ?? []).filter(
          (_, position) => position !== index,
        ),
      })),
    // No id key at all: the server reads a missing id as a new row and names
    // it.
    addRow: (row) =>
      updateCase((prev) => ({
        [listKey]: [...(prev[listKey] ?? []), withClientKey(row)],
      })),
  };
}
