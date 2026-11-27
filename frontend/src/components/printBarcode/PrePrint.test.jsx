import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

import { getFromOpenElisServer } from "../utils/Utils";
import PrePrint from "./PrePrint";

const listed = (container, prefix) =>
  [...container.querySelectorAll(`input[id^="${prefix}"]`)].map(
    (box) => box.labels[0].textContent,
  );

const labelTestIds = (container) =>
  new URL(
    container.querySelector("iframe").getAttribute("src"),
    "https://placeholder.invalid/",
  ).searchParams.get("testIds");

describe("PrePrint", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([
          { id: "2", value: "Serum" },
          { id: "3", value: "Sang total" },
        ]);
      } else if (url === "/rest/sample-type-tests?sampleType=2") {
        callback({
          tests: [
            { id: "4", name: "Creatinine" },
            { id: "1", name: "GPT/ALAT" },
            { id: "6", name: "Glycémie" },
          ],
          panels: [
            { id: "9", name: "Bilan Biochimique", testIds: "1, 4" },
            { id: "10", name: "Bilan Hépatique", testIds: "1" },
          ],
        });
      } else if (url === "/rest/sample-type-tests?sampleType=3") {
        callback({ tests: [{ id: "7", name: "Hémoglobine" }], panels: [] });
      } else {
        callback([]);
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const renderPage = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <PrePrint />
      </IntlProvider>,
    );

  const chooseSampleType = async (name) => {
    const box = screen.getByRole("combobox", { name: "Sample Type" });
    await userEvent.clear(box);
    await userEvent.type(box, name.slice(0, 3));
    await userEvent.click(screen.getByRole("option", { name }));
  };

  const prePrint = () =>
    userEvent.click(
      screen.getByRole("button", {
        name: messages["barcode.print.preprint.button"],
      }),
    );

  test("pre-printed labels load from the label servlet, not from an app page", async () => {
    const { container } = renderPage();

    await chooseSampleType("Serum");
    await userEvent.click(await screen.findByLabelText("Creatinine"));
    await prePrint();

    await waitFor(() =>
      expect(container.querySelector("iframe")).toBeInTheDocument(),
    );
    const source = container.querySelector("iframe").getAttribute("src");
    expect(source).toMatch(/^\/api\/OpenELIS-Global\/LabelMakerServlet\?/);
    expect(source).toContain("prePrinting=true");
    expect(labelTestIds(container)).toBe("4");
  });

  test("the sample type, panels and tests are each searched by typing, ignoring accents", async () => {
    const { container } = renderPage();

    const box = screen.getByRole("combobox", { name: "Sample Type" });
    await userEvent.type(box, "sang");
    expect(
      screen.queryAllByRole("option").map((option) => option.textContent),
    ).toEqual(["Sang total"]);
    await userEvent.clear(box);
    await chooseSampleType("Serum");

    await userEvent.type(
      await screen.findByRole("searchbox", { name: "Search panels" }),
      "hepa",
    );
    expect(listed(container, "panel_")).toEqual(["Bilan Hépatique"]);

    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "glyce",
    );
    expect(listed(container, "test_")).toEqual(["Glycémie"]);
  });

  test("a test ticked before its panel is printed once and can still be unticked", async () => {
    const { container } = renderPage();
    await chooseSampleType("Serum");

    await userEvent.click(await screen.findByLabelText("GPT/ALAT"));
    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));
    await prePrint();
    expect(labelTestIds(container).split(",").sort()).toEqual(["1", "4"]);

    await userEvent.click(screen.getByLabelText("GPT/ALAT"));
    expect(screen.getByLabelText("GPT/ALAT")).not.toBeChecked();
    await prePrint();
    expect(labelTestIds(container)).toBe("4");
  });

  test("unticking a panel keeps a test that was ticked by hand", async () => {
    const { container } = renderPage();
    await chooseSampleType("Serum");

    await userEvent.click(await screen.findByLabelText("GPT/ALAT"));
    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));
    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));

    expect(screen.getByLabelText("GPT/ALAT")).toBeChecked();
    expect(screen.getByLabelText("Creatinine")).not.toBeChecked();
    await prePrint();
    expect(labelTestIds(container)).toBe("1");
  });

  test("two panels sharing a test print it once, and unticking a panel leaves the other's tests", async () => {
    const { container } = renderPage();
    await chooseSampleType("Serum");

    await userEvent.click(await screen.findByLabelText("Bilan Biochimique"));
    await userEvent.click(screen.getByLabelText("Bilan Hépatique"));
    await prePrint();
    expect(labelTestIds(container).split(",").sort()).toEqual(["1", "4"]);

    await userEvent.click(screen.getByLabelText("Bilan Biochimique"));
    expect(screen.getByLabelText("GPT/ALAT")).toBeChecked();
    expect(screen.getByLabelText("Creatinine")).not.toBeChecked();
    await prePrint();
    expect(labelTestIds(container)).toBe("1");
  });

  test("chosen tests show as tags, and changing the sample type clears them", async () => {
    renderPage();
    await chooseSampleType("Serum");
    await userEvent.click(await screen.findByLabelText("Creatinine"));

    const chosen = screen.getByTestId("preprint-tests-selected");
    expect(within(chosen).getByText("Creatinine")).toBeInTheDocument();

    await chooseSampleType("Sang total");
    expect(await screen.findByLabelText("Hémoglobine")).not.toBeChecked();
    expect(
      screen.queryByTestId("preprint-tests-selected"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: messages["barcode.print.preprint.button"],
      }),
    ).toBeDisabled();
  });
});
