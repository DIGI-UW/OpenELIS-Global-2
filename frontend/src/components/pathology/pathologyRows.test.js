import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import {
  activeRows,
  blockIdentifier,
  cassetteStateBadge,
  countedRows,
  deactivatedCount,
  isDeactivatedRow,
  isUnsavedRow,
  labelStreamUrl,
  needsParentBlock,
  newRowName,
  objectNaming,
  parentBlockOf,
  rowListEditors,
  rowObjectName,
  slideIdentifier,
  stripClientKeys,
  unsavedPosition,
  withClientKey,
} from "./pathologyRows";

const intl = createIntl({ locale: "en", messages });

// A deactivated cassette or slide is kept for the record, so every rule that
// counts work has to see past it; a row without the flag is one in use.
describe("activeRows and isDeactivatedRow", () => {
  it("treats a missing list as empty", () => {
    expect(activeRows(undefined)).toEqual([]);
    expect(activeRows([])).toEqual([]);
  });

  it("keeps a row that carries no active flag and drops one marked inactive", () => {
    const inUse = { id: "1" };
    const flagged = { id: "2", active: true };
    const deactivated = { id: "3", active: false };

    expect(activeRows([inUse, deactivated, flagged])).toEqual([inUse, flagged]);
    expect(isDeactivatedRow(deactivated)).toBe(true);
    expect(isDeactivatedRow(inUse)).toBe(false);
    expect(isDeactivatedRow(undefined)).toBe(false);
  });
});

// Showing a row and counting it are different questions: a cassette added on
// the screen is shown at once but is not work recorded until it is saved.
describe("countedRows against activeRows", () => {
  const saved = { id: "1" };
  const unsaved = { location: "" };
  const deactivated = { id: "2", active: false };

  it("shows the unsaved row but counts only the saved row in use", () => {
    const rows = [saved, unsaved, deactivated];

    expect(activeRows(rows)).toEqual([saved, unsaved]);
    expect(countedRows(rows)).toEqual([saved]);
    expect(countedRows(undefined)).toEqual([]);
  });

  it("counts the deactivated rows apart, for the switch that shows them", () => {
    expect(deactivatedCount([saved, unsaved, deactivated])).toBe(1);
    expect(deactivatedCount([saved])).toBe(0);
    expect(deactivatedCount(undefined)).toBe(0);
  });
});

describe("isUnsavedRow", () => {
  it.each([
    ["a null id", { id: null }],
    ["no id at all", {}],
  ])("reads a row with %s as unsaved", (_, row) => {
    expect(isUnsavedRow(row)).toBe(true);
  });

  it.each([
    ["the number zero", { id: 0 }],
    ["a string id", { id: "7" }],
  ])("reads a row whose id is %s as saved", (_, row) => {
    expect(isUnsavedRow(row)).toBe(false);
  });
});

describe("blockIdentifier and slideIdentifier", () => {
  it("prefer the designation the server assigned, trimmed as the server trims it", () => {
    expect(
      blockIdentifier({ id: "4", designation: "A1", blockNumber: 3 }),
    ).toBe("A1");
    expect(blockIdentifier({ id: "4", designation: " A1 " })).toBe("A1");
    expect(slideIdentifier({ id: "4", designation: "2", slideNumber: 9 })).toBe(
      "2",
    );
  });

  it("fall back to a legacy number, then to the row id, then to nothing", () => {
    expect(blockIdentifier({ id: "4", blockNumber: 3 })).toBe("3");
    expect(blockIdentifier({ id: "4", blockNumber: 0 })).toBe("0");
    expect(blockIdentifier({ id: "4", designation: "  " })).toBe("B4");
    expect(blockIdentifier({ id: "4" })).toBe("B4");
    expect(blockIdentifier({ location: "" })).toBe("");

    expect(slideIdentifier({ id: "6", slideNumber: 2 })).toBe("2");
    expect(slideIdentifier({ id: "6" })).toBe("S6");
    expect(slideIdentifier({ blockId: null, location: "" })).toBe("");
  });
});

describe("parentBlockOf", () => {
  const blocks = [
    { id: 1, designation: "A1" },
    { id: "2", designation: "A2" },
  ];

  it("finds the parent whether the ids arrive as numbers or strings", () => {
    expect(parentBlockOf({ blockId: "1" }, blocks)).toBe(blocks[0]);
    expect(parentBlockOf({ blockId: 2 }, blocks)).toBe(blocks[1]);
  });

  it("finds nothing for a legacy slide with no recorded parent, or an unknown one", () => {
    expect(parentBlockOf({ blockId: null }, blocks)).toBeUndefined();
    expect(parentBlockOf({ blockId: "9" }, blocks)).toBeUndefined();
    expect(parentBlockOf({ blockId: "1" }, undefined)).toBeUndefined();
  });
});

describe("cassetteStateBadge", () => {
  it("names an embedded block as verified and a cassette as still pending", () => {
    expect(cassetteStateBadge({ cassetteState: "BLOCK" })).toEqual({
      kind: "verified",
      textKey: "pathology.label.block",
    });
    expect(cassetteStateBadge({ cassetteState: "CASSETTE" })).toEqual({
      kind: "pending",
      textKey: "pathology.label.cassette",
    });
    expect(cassetteStateBadge({ location: "" })).toEqual({
      kind: "pending",
      textKey: "pathology.label.cassette",
    });
  });
});

describe("labelStreamUrl", () => {
  it("asks the servlet for the case's labels by its accession number, encoded", () => {
    expect(labelStreamUrl("https://lab/api", "block", "AC C&9")).toBe(
      "https://lab/api/LabelMakerServlet?labelType=block&code=AC%20C%269",
    );
  });
});

// The sentence a reader hears names the object as the bench knows it: a
// cassette or block by its designation, a slide by its barcode.
describe("objectNaming", () => {
  it("names a cassette by its designation", () => {
    expect(objectNaming("block", { id: "1", designation: "A1" })).toEqual({
      actionKey: "pathology.action.deactivateCassette",
      headingKey: "pathology.modal.deactivateCassetteHeading",
      toastKey: "pathology.toast.cassetteDeactivated",
      values: { designation: "A1" },
    });
  });

  it("names an embedded block as a block", () => {
    expect(
      objectNaming("block", {
        id: "1",
        designation: "A1",
        cassetteState: "BLOCK",
      }),
    ).toEqual({
      actionKey: "pathology.action.deactivateBlock",
      headingKey: "pathology.modal.deactivateBlockHeading",
      toastKey: "pathology.toast.blockDeactivated",
      values: { designation: "A1" },
    });
  });

  it("names a slide by its barcode, and by its identifier when it has none", () => {
    expect(
      objectNaming("slide", {
        id: "5",
        designation: "1",
        barcode: "ACC9.A1.1",
      }),
    ).toEqual({
      actionKey: "pathology.action.deactivateSlide",
      headingKey: "pathology.modal.deactivateSlideHeading",
      toastKey: "pathology.toast.slideDeactivated",
      values: { barcode: "ACC9.A1.1" },
    });
    expect(objectNaming("slide", { id: "6", slideNumber: 2 }).values).toEqual({
      barcode: "2",
    });
  });
});

describe("newRowName", () => {
  it("names an unsaved row by kind and position", () => {
    expect(newRowName(intl, "block", 2)).toBe(
      messages["pathology.label.newCassette"].replace("{position}", "2"),
    );
    expect(newRowName(intl, "slide", 1)).toBe(
      messages["pathology.label.newSlide"].replace("{position}", "1"),
    );
  });
});

// A React key taken from the position would hand an unsaved row's controls
// to its neighbour once an earlier row is removed.
describe("withClientKey and stripClientKeys", () => {
  it("gives every added row its own key", () => {
    const first = withClientKey({ location: "" });
    const second = withClientKey({ location: "" });

    expect(first.clientKey).toBeTruthy();
    expect(second.clientKey).toBeTruthy();
    expect(first.clientKey).not.toBe(second.clientKey);
    expect(first.location).toBe("");
  });

  it("posts nothing of the key and leaves every other field as it was", () => {
    const saved = { id: "1", designation: "A1", location: "Tray 1" };
    const added = withClientKey({ blockId: "1", location: "Tray 2" });

    expect(stripClientKeys([saved, added])).toEqual([
      { id: "1", designation: "A1", location: "Tray 1" },
      { blockId: "1", location: "Tray 2" },
    ]);
    expect(stripClientKeys(undefined)).toBeUndefined();
  });
});

describe("needsParentBlock", () => {
  it("holds back only a new slide that names no block", () => {
    expect(needsParentBlock({ blockId: null, location: "" })).toBe(true);
    expect(needsParentBlock({ location: "" })).toBe(true);
    expect(needsParentBlock({ blockId: "1", location: "" })).toBe(false);
    // A slide cut before slides named their block is already on the case.
    expect(needsParentBlock({ id: "6", blockId: null })).toBe(false);
  });
});

describe("unsavedPosition and rowObjectName", () => {
  const rows = [
    { id: "1", designation: "A1" },
    { location: "" },
    { id: "2", designation: "A2", active: false },
    { location: "" },
  ];

  it("numbers an unsaved row among the unsaved rows only", () => {
    expect(unsavedPosition(rows, 1)).toBe(1);
    expect(unsavedPosition(rows, 3)).toBe(2);
  });

  it("names a saved cassette by its designation, a saved slide by its barcode and a new row by its place", () => {
    expect(rowObjectName(intl, "block", rows[0], undefined)).toBe("A1");
    expect(
      rowObjectName(
        intl,
        "slide",
        { id: "5", designation: "1", barcode: "ACC9.A1.1" },
        undefined,
      ),
    ).toBe("ACC9.A1.1");
    expect(rowObjectName(intl, "block", rows[3], 2)).toBe(
      messages["pathology.label.newCassette"].replace("{position}", "2"),
    );
  });
});

describe("rowListEditors", () => {
  // Applies each update the way the screen's updateCase does.
  const editorsOver = (initial) => {
    let state = initial;
    const updateCase = (updater) => {
      state = { ...state, ...updater(state) };
    };
    return { editors: rowListEditors("slides", updateCase), read: () => state };
  };

  it("replaces the edited row, drops only the removed one and keys the added one", () => {
    const first = { id: "5", location: "" };
    const second = { blockId: "1", location: "" };
    const { editors, read } = editorsOver({ slides: [first, second] });

    editors.patchRow(0, { location: "Tray 1" });
    expect(read().slides[0]).toEqual({ id: "5", location: "Tray 1" });
    expect(first.location).toBe("");

    editors.removeRow(1);
    expect(read().slides).toEqual([{ id: "5", location: "Tray 1" }]);

    editors.addRow({ blockId: null, location: "" });
    expect(read().slides).toHaveLength(2);
    expect(read().slides[1]).toMatchObject({ blockId: null, location: "" });
    expect(read().slides[1].clientKey).toBeTruthy();
    expect(read().slides[1]).not.toHaveProperty("id");
  });
});
