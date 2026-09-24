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
});
