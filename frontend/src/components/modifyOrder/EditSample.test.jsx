/**
 * Modify Order's "Current Tests" table renders each row's cells as a keyed list;
 * the Remove Sample cell used to come back wrapped in an unkeyed fragment.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import EditSample from "./EditSample";

const { sampleTypeProps } = vi.hoisted(() => ({ sampleTypeProps: [] }));

vi.mock("../addOrder/SampleType", () => ({
  default: (props) => {
    sampleTypeProps.push(props);
    return <div />;
  },
}));
vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

const test = (id, name, accessionNumber) => ({
  testId: id,
  testName: name,
  accessionNumber,
  sampleType: "Whole Blood",
  collectionDate: "",
  collectionTime: "",
  removeSample: false,
  hasResults: false,
  canceled: false,
  add: false,
});

describe("EditSample current tests", () => {
  it("lists the order's tests as keyed rows and cells", () => {
    window.scrollTo = vi.fn();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <IntlProvider locale="en" messages={messages}>
        <EditSample
          samples={[]}
          setSamples={vi.fn()}
          orderFormValues={{
            existingTests: [
              test("13", "Hemoglobin", "DEV01260000000000270-1"),
              test("14", "Hematocrit", ""),
            ],
            possibleTests: [test("20", "Platelets", "DEV01260000000000270-1")],
          }}
          setOrderFormValues={vi.fn()}
          error={() => null}
        />
      </IntlProvider>,
    );

    expect(screen.getByText("Hemoglobin")).toBeInTheDocument();
    expect(screen.getByText("Hematocrit")).toBeInTheDocument();
    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes('unique "key"'),
      ),
    ).toBe(false);
    errors.mockRestore();
  });
});

describe("EditSample added samples (OGC-1388)", () => {
  it("an unticked test on an added sample is no longer carried", () => {
    window.scrollTo = vi.fn();
    sampleTypeProps.length = 0;
    const added = {
      index: 1,
      sampleTypeId: "37",
      sampleXML: null,
      panels: [],
      tests: [{ id: "322", name: "Histopathology examination" }],
      referralItems: [],
    };
    const setSamples = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <EditSample
          samples={[added]}
          setSamples={setSamples}
          orderFormValues={{ existingTests: [], possibleTests: [] }}
          setOrderFormValues={vi.fn()}
          error={() => null}
        />
      </IntlProvider>,
    );

    sampleTypeProps[0].sampleTypeObject({
      selectedTests: [],
      sampleObjectIndex: 0,
    });

    const update = setSamples.mock.calls[0][0];
    expect(update([added])[0].tests).toEqual([]);
    expect(added.tests).toHaveLength(1);
  });
});

describe("EditSample rows for a test on two samples", () => {
  const glucose = (analysisId, sampleItemId, accessionNumber) => ({
    ...test("3", "Glucose", accessionNumber),
    analysisId,
    sampleItemId,
    sampleType: "Plasma",
  });
  const possible = (sampleItemId, accessionNumber) => ({
    ...test("9", "Platelets", accessionNumber),
    sampleItemId,
  });

  const renderTwoSamples = (setOrderFormValues) => {
    window.scrollTo = vi.fn();
    return render(
      <IntlProvider locale="en" messages={messages}>
        <EditSample
          samples={[]}
          setSamples={vi.fn()}
          orderFormValues={{
            existingTests: [
              glucose("276", "291", "DEV01260000000000454-2"),
              glucose("315", "312", "DEV01260000000000454-3"),
            ],
            possibleTests: [
              possible("291", "DEV01260000000000454-2"),
              possible("312", "DEV01260000000000454-3"),
            ],
          }}
          setOrderFormValues={setOrderFormValues}
          error={() => null}
        />
      </IntlProvider>,
    );
  };

  it("shows each sample's row and cancels only the ticked one", () => {
    const setOrderFormValues = vi.fn();
    const { container } = renderTwoSamples(setOrderFormValues);

    const currentTests = within(container.querySelectorAll("table")[0]);
    expect(
      currentTests.getByText("DEV01260000000000454-2"),
    ).toBeInTheDocument();
    expect(
      currentTests.getByText("DEV01260000000000454-3"),
    ).toBeInTheDocument();
    const cancelBoxes = container.querySelectorAll('input[name="canceled"]');
    expect(new Set([...cancelBoxes].map((box) => box.id)).size).toBe(2);

    fireEvent.click(cancelBoxes[1]);

    const saved = setOrderFormValues.mock.calls.at(-1)[0].existingTests;
    expect(saved.map((t) => [t.analysisId, t.canceled])).toEqual([
      ["276", false],
      ["315", true],
    ]);
  });

  it("adds a test to only the sample it was ticked on", () => {
    const setOrderFormValues = vi.fn();
    const { container } = renderTwoSamples(setOrderFormValues);

    fireEvent.click(container.querySelectorAll('input[name="add"]')[0]);

    const saved = setOrderFormValues.mock.calls.at(-1)[0].possibleTests;
    expect(saved.map((t) => [t.sampleItemId, t.add])).toEqual([
      ["291", true],
      ["312", false],
    ]);
  });
});
