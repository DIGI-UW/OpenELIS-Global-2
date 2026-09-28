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

vi.mock("../addOrder/SampleType", () => ({ default: () => <div /> }));
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
