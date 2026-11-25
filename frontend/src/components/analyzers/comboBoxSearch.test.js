import { afterEach, describe, expect, test, vi } from "vitest";
import { includesComboBoxText } from "./comboBoxSearch";

describe("includesComboBoxText", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("filters deterministically without locale-sensitive casing", () => {
    vi.spyOn(String.prototype, "toLocaleLowerCase").mockImplementation(() => {
      throw new Error("locale-sensitive casing must not be used");
    });

    expect(
      includesComboBoxText({
        item: { name: "Rifampin Resistance" },
        itemToString: (item) => item.name,
        inputValue: "RIFAMPIN",
      }),
    ).toBe(true);
  });

  test("ignores accents in the item and in what is typed", () => {
    const matches = (inputValue) =>
      includesComboBoxText({
        item: { name: "Résistance à la rifampicine" },
        itemToString: (item) => item.name,
        inputValue,
      });

    expect(matches("resistance a la")).toBe(true);
    expect(matches("RÉSIST")).toBe(true);
    expect(matches("isoniazide")).toBe(false);
  });
});
