/**
 * OGC-1514: adding a test to an existing sample on Modify Order is a search
 * over the sample's available tests, with the chosen tests shown as tags and
 * the sample type's panels on offer, instead of a five-row paged checkbox
 * table.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ExistingSampleTests, {
  panelTestIdsOn,
  sortedByName,
} from "./ExistingSampleTests";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer };
});

const available = (testId, testName, add = false) => ({
  testId,
  testName,
  sampleItemId: "291",
  add,
});

const SERUM_TESTS = [
  available("3", "Zinc (Zn)"),
  available("2", "Copper (Cu)"),
  available("5", "Albumin"),
];

/**
 * Holds the form rows the way EditSample does: every onAddChange marks the
 * rows, and the component reads its tags and ticks back from them.
 */
const FormRows = ({ tests, onAddChange, ...props }) => {
  const [rows, setRows] = React.useState(tests);
  const markRows = (testIds, add) => {
    onAddChange(testIds, add);
    setRows((current) =>
      current.map((row) =>
        testIds.includes(row.testId) ? { ...row, add } : row,
      ),
    );
  };
  return <ExistingSampleTests tests={rows} onAddChange={markRows} {...props} />;
};

const renderSample = (props = {}) => {
  const onAddChange = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <FormRows
        sampleItemId="291"
        accessionNumber="DEV01260000000000454-1"
        sampleType="Serum"
        tests={SERUM_TESTS}
        onAddChange={onAddChange}
        {...props}
      />
    </IntlProvider>,
  );
  return onAddChange;
};

beforeEach(() => {
  getFromOpenElisServer.mockReset();
});

describe("ExistingSampleTests helpers", () => {
  it("orders the tests by name", () => {
    expect(sortedByName(SERUM_TESTS).map((t) => t.testName)).toEqual([
      "Albumin",
      "Copper (Cu)",
      "Zinc (Zn)",
    ]);
  });

  it("keeps only the panel tests the sample can take", () => {
    expect(panelTestIdsOn({ testIds: "2, 3,99" }, SERUM_TESTS).sort()).toEqual([
      "2",
      "3",
    ]);
  });
});

describe("ExistingSampleTests search", () => {
  it("names the sample and lists its tests alphabetically with no paging", () => {
    renderSample();

    expect(
      screen.getByRole("heading", { name: "DEV01260000000000454-1 (Serum)" }),
    ).toBeInTheDocument();
    const boxes = screen.getAllByRole("checkbox");
    expect(boxes.map((box) => box.id)).toEqual([
      "existing_test_291_5",
      "existing_test_291_2",
      "existing_test_291_3",
    ]);
    expect(screen.queryByText(/items per page/i)).not.toBeInTheDocument();
  });

  it("filters the tests by name as the user types and adds the one picked", () => {
    const onAddChange = renderSample();
    const search = screen.getByPlaceholderText("Choose Available Test");

    fireEvent.change(search, { target: { value: "zin" } });

    const matches = screen.getAllByRole("menuitem");
    expect(matches.map((item) => item.textContent)).toEqual(["Zinc (Zn)"]);

    fireEvent.click(matches[0]);

    expect(onAddChange).toHaveBeenCalledWith(["3"], true);
    expect(search).toHaveValue("");
  });

  it("says when nothing matches", () => {
    renderSample();

    fireEvent.change(screen.getByPlaceholderText("Choose Available Test"), {
      target: { value: "xyz" },
    });

    expect(screen.getByText(/No test Found Matching/)).toBeInTheDocument();
    expect(screen.getByText('"xyz"')).toBeInTheDocument();
  });

  it("shows a chosen test as a tag that removes it, and leaves it out of the search", () => {
    const onAddChange = renderSample({
      tests: [available("2", "Copper (Cu)", true), available("3", "Zinc (Zn)")],
    });

    const tag = screen.getByText("Copper (Cu)", { selector: ".cds--tag span" });
    expect(tag).toBeInTheDocument();
    expect(screen.getByLabelText("Copper (Cu)")).toBeChecked();

    fireEvent.change(screen.getByPlaceholderText("Choose Available Test"), {
      target: { value: "c" },
    });
    expect(
      screen.getAllByRole("menuitem").map((item) => item.textContent),
    ).toEqual(["Zinc (Zn)"]);

    fireEvent.click(within(tag.closest(".cds--tag")).getByRole("button"));

    expect(onAddChange).toHaveBeenCalledWith(["2"], false);
  });

  it("ticking a checkbox adds the test and unticking removes it", () => {
    const onAddChange = renderSample({
      tests: [available("2", "Copper (Cu)", true), available("3", "Zinc (Zn)")],
    });

    fireEvent.click(screen.getByLabelText("Zinc (Zn)"));
    expect(onAddChange).toHaveBeenLastCalledWith(["3"], true);

    fireEvent.click(screen.getByLabelText("Copper (Cu)"));
    expect(onAddChange).toHaveBeenLastCalledWith(["2"], false);
  });
});

describe("ExistingSampleTests panels", () => {
  const panels = [
    { id: "7", name: "Metals", testIds: "2,3,99" },
    { id: "8", name: "Liver", testIds: "5" },
  ];

  it("offers no panels without a sample type", () => {
    renderSample();

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(screen.queryByText("Order Panels")).not.toBeInTheDocument();
  });

  it("loads the sample type's panels and a picked panel adds its available tests", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ tests: [], panels }),
    );
    const onAddChange = renderSample({ sampleTypeId: "2" });

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-type-tests?sampleType=2",
      expect.any(Function),
    );
    expect(screen.getByText("Order Panels")).toBeInTheDocument();

    const search = screen.getByPlaceholderText("Choose Available panel");
    fireEvent.change(search, { target: { value: "met" } });
    const matches = screen.getAllByRole("menuitem");
    expect(matches.map((item) => item.textContent)).toEqual(["Metals"]);

    fireEvent.click(matches[0]);

    expect(onAddChange).toHaveBeenCalledWith(["2", "3"], true);
    expect(search).toHaveValue("");
    expect(screen.getByLabelText("Metals")).toBeChecked();

    const tag = screen.getByText("Metals", { selector: ".cds--tag span" });
    fireEvent.click(within(tag.closest(".cds--tag")).getByRole("button"));

    expect(onAddChange).toHaveBeenLastCalledWith(["2", "3"], false);
    expect(screen.getByLabelText("Metals")).not.toBeChecked();
  });

  it("says when no panel matches", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ tests: [], panels }),
    );
    renderSample({ sampleTypeId: "2" });

    fireEvent.change(screen.getByPlaceholderText("Choose Available panel"), {
      target: { value: "renal" },
    });

    expect(screen.getByText(/No panel found matching/)).toBeInTheDocument();
  });

  it("reads a chosen panel back from the ticked rows, so Back and Next keep it", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ tests: [], panels }),
    );
    renderSample({
      sampleTypeId: "2",
      tests: [
        available("2", "Copper (Cu)", true),
        available("3", "Zinc (Zn)", true),
        available("5", "Albumin"),
      ],
    });

    expect(screen.getByLabelText("Metals")).toBeChecked();
    expect(
      screen.getByText("Metals", { selector: ".cds--tag span" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Liver")).not.toBeChecked();
  });

  it("unticking one of a panel's tests unticks the panel, and a panel with nothing to add is disabled", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({
        tests: [],
        panels: [...panels, { id: "9", name: "Bone", testIds: "99" }],
      }),
    );
    const onAddChange = renderSample({ sampleTypeId: "2" });

    fireEvent.click(screen.getByLabelText("Metals"));
    expect(onAddChange).toHaveBeenLastCalledWith(["2", "3"], true);
    expect(screen.getByLabelText("Metals")).toBeChecked();

    fireEvent.click(screen.getByLabelText("Zinc (Zn)"));
    expect(onAddChange).toHaveBeenLastCalledWith(["3"], false);
    expect(screen.getByLabelText("Metals")).not.toBeChecked();
    expect(
      screen.queryByText("Metals", { selector: ".cds--tag span" }),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Metals"));
    expect(onAddChange).toHaveBeenLastCalledWith(["3"], true);

    expect(screen.getByLabelText("Bone")).toBeDisabled();
  });
});
