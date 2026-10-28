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
 * A slide's name on a screen that lists slides without their blocks: under its
 * block when one is recorded, since a slide designation repeats under every
 * block, and on its own otherwise.
 */
export function slideOnBlockName(slide, blocks) {
  const parent = parentBlockOf(slide, blocks);
  return parent
    ? blockIdentifier(parent) + "." + slideIdentifier(slide)
    : slideIdentifier(slide);
}

/**
 * A block has been embedded, so it is a verified object; a cassette is still
 * waiting for embedding.
 */
export function cassetteStateBadge(block) {
  return isEmbeddedRow(block)
    ? { kind: "verified", textKey: "pathology.label.block" }
    : { kind: "pending", textKey: "pathology.label.cassette" };
}

/**
 * A cassette that has been embedded is a block. A row with no state is a
 * cassette, as the server reads it.
 */
export function isEmbeddedRow(block) {
  return block?.cassetteState === "BLOCK";
}

// The saved cassettes in use that are not blocks yet, in list order.
function outstandingRows(blocks) {
  return countedRows(blocks).filter((block) => !isEmbeddedRow(block));
}

/**
 * How far embedding has got on a case: the saved cassettes in use, how many
 * of them are blocks, the ones still outstanding, named in list order, and
 * whether there is nothing left to embed (FR-6.3). Derived on the screen from
 * the rows every time, and never stored anywhere (FR-9.6).
 */
export function embeddingSummary(blocks) {
  const total = countedRows(blocks).length;
  const outstanding = outstandingRows(blocks).map(blockIdentifier);

  return {
    embedded: total - outstanding.length,
    total,
    outstanding,
    complete: total > 0 && outstanding.length === 0,
  };
}

/**
 * The saved cassette still waiting after the given one, in list order and
 * wrapping to the first, or undefined when the given one is the last left.
 * Ids arrive as numbers or strings, so they are compared as strings.
 */
export function nextOutstandingCassette(blocks, block) {
  const outstanding = outstandingRows(blocks);
  const at = outstanding.findIndex(
    (candidate) => String(candidate.id) === String(block?.id),
  );
  const rest = [...outstanding.slice(at + 1), ...outstanding.slice(0, at)];
  return rest.find((candidate) => String(candidate.id) !== String(block?.id));
}

/**
 * The tissue types a cassette's picker offers: the served ones, plus the one
 * the cassette already holds when it is not among them. The served list holds
 * active entries only, so without that row a tissue type retired after it was
 * recorded would leave the cassette looking as if none had been.
 */
export function tissueTypeItems(served, block, names) {
  const items = served ?? [];
  const held = block?.tissueTypeId;
  if (
    held === null ||
    held === undefined ||
    held === "" ||
    items.some((item) => String(item.id) === String(held))
  ) {
    return items;
  }
  return [...items, { id: held, value: names?.[held] ?? String(held) }];
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
