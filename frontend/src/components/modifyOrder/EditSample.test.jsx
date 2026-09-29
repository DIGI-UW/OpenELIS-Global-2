/**
 * Modify Order's "Current Tests" table renders each row's cells as a keyed list;
 * the Remove Sample cell used to come back wrapped in an unkeyed fragment.
 */
import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
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
