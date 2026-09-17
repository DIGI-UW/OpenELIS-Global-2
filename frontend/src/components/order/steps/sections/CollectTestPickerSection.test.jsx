import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

import CollectTestPickerSection from "./CollectTestPickerSection";

const renderPicker = (samples, setSamples = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CollectTestPickerSection
        samples={samples}
        setSamples={setSamples}
        isReadOnly={false}
      />
    </IntlProvider>,
  );

describe("CollectTestPickerSection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({
        tests: [{ id: "9", name: "Malaria RDT" }],
        panels: [{ id: "4", name: "Renal Panel" }],
      }),
    );
  });

  // OGC-1201 A: Collect showed the ordered tests but offered no way to add
  // one, so a test asked for at the bedside meant walking back to step 1.
  it("offers the catalogue for each collectable sample's own type", () => {
    renderPicker([
      { sampleTypeId: "5", sampleTypeName: "Blood", tests: [], panels: [] },
    ]);

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-type-tests?sampleType=5",
      expect.any(Function),
    );
    expect(screen.getByText("Malaria RDT")).toBeInTheDocument();
    expect(screen.getByText("Renal Panel")).toBeInTheDocument();
  });

  it("adds a chosen test to that sample only", async () => {
    const setSamples = vi.fn();
    renderPicker(
      [
        { sampleTypeId: "5", sampleTypeName: "Blood", tests: [], panels: [] },
        { sampleTypeId: "5", sampleTypeName: "Blood", tests: [], panels: [] },
      ],
      setSamples,
    );

    await userEvent.setup().click(screen.getAllByText("Malaria RDT")[0]);

    expect(setSamples).toHaveBeenCalledTimes(1);
    const updated = setSamples.mock.calls[0][0];
    expect(updated[0].tests).toEqual([{ id: "9", name: "Malaria RDT" }]);
    expect(updated[1].tests).toEqual([]);
  });

  it("stays out of the way when nothing is collectable yet", () => {
    const { container } = renderPicker([{ sampleTypeId: "" }]);
    expect(container).toBeEmptyDOMElement();
  });

  it("ignores a rejected specimen", () => {
    const { container } = renderPicker([
      { sampleTypeId: "5", sampleRejected: true },
    ]);
    expect(container).toBeEmptyDOMElement();
  });
});
