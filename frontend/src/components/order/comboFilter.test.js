import { filterByTypedLabel } from "./comboFilter";

const itemToString = (item) => (item ? item.value : "");
const programs = [
  { id: "2", value: "Routine Testing" },
  { id: "3", value: "People living with HIV Program - Initial Visit" },
  { id: "5", value: "Cytology" },
];
const visible = (inputValue, selectedLabel) =>
  programs
    .filter((item) =>
      filterByTypedLabel(selectedLabel)({ item, itemToString, inputValue }),
    )
    .map((item) => item.value);

describe("filterByTypedLabel", () => {
  it("keeps the items whose label contains the typed text, ignoring case", () => {
    expect(visible("cyto")).toEqual(["Cytology"]);
    expect(visible("HIV")).toEqual([
      "People living with HIV Program - Initial Visit",
    ]);
  });

  it("lists everything before anything is typed", () => {
    expect(visible("")).toHaveLength(3);
  });

  it("lists everything while the input shows the selected label", () => {
    expect(visible("Cytology", "Cytology")).toHaveLength(3);
  });

  it("ignores accents in the label and in what is typed", () => {
    const accented = [{ id: "7", value: "Hématologie" }];
    const shown = (inputValue) =>
      accented.filter((item) =>
        filterByTypedLabel("")({ item, itemToString, inputValue }),
      );
    expect(shown("hemato")).toHaveLength(1);
    expect(shown("HÉMA")).toHaveLength(1);
  });

  it("lists everything while the input shows the selected label, accents aside", () => {
    expect(visible("Hematologie", "Hématologie")).toHaveLength(3);
  });

  it("lists nothing when no label matches", () => {
    expect(visible("zzz")).toEqual([]);
  });
});
