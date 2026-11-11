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
 * block, and on its own otherwise. The join is the default identifier
 * separator; the server joins with the configured one, which only differs
 * where a deployment changed pathology.identifier.separator.
 */
export function slideOnBlockName(slide, blocks) {
  const parent = parentBlockOf(slide, blocks);
  return parent
    ? blockIdentifier(parent) + "." + slideIdentifier(slide)
    : slideIdentifier(slide);
}

/**
 * Whether a slide's label has been checked against its block. The case read
 * carries the verified slides in `labelVerifications`, keyed by slide id, and
 * the screen reads that map only, never a field of the row itself.
 */
export function isLabelVerified(slide, labelVerifications) {
  if (isUnsavedRow(slide) || !labelVerifications) {
    return false;
  }
  return Object.prototype.hasOwnProperty.call(
    labelVerifications,
    String(slide.id),
  );
}

/**
 * Whether a slide's label can be checked at all: it is saved, so its label
 * can be printed, and it names the block it was cut from, which the scan is
 * checked against. A slide cut before slides named their block has neither.
 */
export function hasVerifiableLabel(slide) {
  return (
    !isUnsavedRow(slide) &&
    slide?.blockId !== null &&
    slide?.blockId !== undefined &&
    slide?.blockId !== ""
  );
}

/**
 * The saved slides in use whose labels are still unchecked, in list order,
 * each with the name it goes by under its block. These hold the case at
 * microtomy, as the server holds it (FR-7.5).
 */
export function unverifiedSlides(blocks, slides, labelVerifications) {
  return activeRows(slides)
    .filter(hasVerifiableLabel)
    .filter((slide) => !isLabelVerified(slide, labelVerifications))
    .map((slide) => ({ slide, name: slideOnBlockName(slide, blocks) }));
}

/**
 * The row after the given one in a list of rows still to be worked, wrapping
 * to the first, or undefined when the given one is the last left. A row no
 * longer on the list still has a next, the first. Ids arrive as numbers or
 * strings, so they are compared as strings.
 */
export function nextAfter(rows, row) {
  const sameRow = (candidate) => String(candidate.id) === String(row?.id);
  const at = rows.findIndex(sameRow);
  const rest = [...rows.slice(at + 1), ...rows.slice(0, Math.max(at, 0))];
  return rest.find((candidate) => !sameRow(candidate));
}

/**
 * The unchecked slide after the given one, in list order and wrapping to the
 * first, or undefined when the given one is the last left.
 */
export function nextUnverifiedSlide(blocks, slides, labelVerifications, slide) {
  return nextAfter(
    unverifiedSlides(blocks, slides, labelVerifications).map(
      (entry) => entry.slide,
    ),
    slide,
  );
}

// Whether a row belongs on a list that shows retired rows only on request.
function shownOnList(row, showDeactivated) {
  return showDeactivated || !isDeactivatedRow(row);
}

/**
 * The microtomy section's groups, in list order: one per saved block in use,
 * each with the slides cut from it, and last the slides no block on the case
 * is recorded for (`block: null`, `legacy: true`), only when there are any.
 * A retired block is listed whenever it still holds a slide in use, so no
 * slide in use can drop out of sight with its block, and otherwise only when
 * retired rows are asked for; retired slides likewise. An unsaved slide sits
 * under the block it was added to. Ids arrive as numbers or strings, so they
 * are compared as strings.
 */
export function microtomyGroups(blocks, slides, showDeactivated) {
  const allSlides = slides ?? [];
  const shownSlides = allSlides.filter((slide) =>
    shownOnList(slide, showDeactivated),
  );
  const cutFrom = (block) =>
    shownSlides.filter((slide) => String(slide.blockId) === String(block.id));
  const holdsSlideInUse = (block) =>
    allSlides.some(
      (slide) =>
        !isDeactivatedRow(slide) && String(slide.blockId) === String(block.id),
    );

  const groups = (blocks ?? [])
    .filter((block) => !isUnsavedRow(block))
    .filter(
      (block) =>
        !isDeactivatedRow(block) || showDeactivated || holdsSlideInUse(block),
    )
    .map((block) => ({
      block,
      slides: cutFrom(block),
      retired: isDeactivatedRow(block),
      legacy: false,
    }));

  const withoutBlock = shownSlides.filter(
    (slide) => parentBlockOf(slide, blocks) === undefined,
  );
  if (withoutBlock.length > 0) {
    groups.push({
      block: null,
      slides: withoutBlock,
      retired: false,
      legacy: true,
    });
  }
  return groups;
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
  return nextAfter(outstandingRows(blocks), block);
}

/**
 * The entries a dictionary picker on a row offers: the served ones, plus the
 * one the row already holds when it is not among them. The served list holds
 * active entries only, so without that entry a tissue type or a stain retired
 * after it was recorded would leave the row looking as if none had been.
 * `names` maps a held id to its name, as the case read carries it.
 */
export function heldOrServedItems(served, heldId, names) {
  const items = served ?? [];
  if (
    heldId === null ||
    heldId === undefined ||
    heldId === "" ||
    items.some((item) => String(item.id) === String(heldId))
  ) {
    return items;
  }
  return [...items, { id: heldId, value: names?.[heldId] ?? String(heldId) }];
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
    // it. Answers the new row's client key, so the caller can find the row
    // once it is on the page.
    addRow: (row) => {
      const added = withClientKey(row);
      updateCase((prev) => ({
        [listKey]: [...(prev[listKey] ?? []), added],
      }));
      return added.clientKey;
    },
  };
}
