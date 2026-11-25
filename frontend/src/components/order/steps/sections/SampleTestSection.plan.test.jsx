import React, { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

import SampleTestSection from "./SampleTestSection";

let current;
const Harness = ({
  plan,
  regulationDriven = plan != null,
  initialSamples = [{ index: 0, sampleTypeId: "7", tests: [], panels: [] }],
}) => {
  const [samples, setSamples] = useState(initialSamples);
  current = samples;
  return (
    <IntlProvider locale="en" messages={messages}>
      <SampleTestSection
        samples={samples}
        setSamples={setSamples}
        orderData={{}}
        isReadOnly={false}
        workflowType="environmental"
        planTestIds={plan}
        regulationDriven={regulationDriven}
      />
    </IntlProvider>
  );
};

const testIds = () => (current[0].tests || []).map((test) => String(test.id));

describe("SampleTestSection — regulation-driven test plan (OGC-768)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/environmental-sample-types")) {
        callback([
          { id: "7", value: "Drinking Water" },
          { id: "8", value: "Surface Water" },
        ]);
      } else if (url.startsWith("/rest/sample-type-tests")) {
        callback({
          tests: url.endsWith("=8")
            ? []
            : [
                { id: "32", name: "Lead (Pb)" },
                { id: "33", name: "pH" },
                { id: "40", name: "Chloride" },
              ],
          panels: [],
        });
      } else {
        callback([]);
      }
    });
  });

  it("starts a sample with the plan tests its sample type runs, and takes off the ones dropped from the plan", async () => {
    const { rerender } = render(<Harness plan={["32", "33", "99"]} />);

    await waitFor(() => expect(testIds()).toEqual(["32", "33"]));

    rerender(<Harness plan={["32"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));
  });

  it("ticks the plan tests again after switching away from a sample type and back", async () => {
    render(<Harness plan={["32"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));

    fireEvent.click(screen.getByRole("combobox", { name: "Sample Type" }));
    fireEvent.click(screen.getByRole("option", { name: "Surface Water" }));
    await waitFor(() => expect(current[0].sampleTypeId).toBe("8"));
    await waitFor(() => expect(testIds()).toEqual([]));

    fireEvent.click(screen.getByRole("combobox", { name: "Sample Type" }));
    fireEvent.click(screen.getByRole("option", { name: "Drinking Water" }));
    await waitFor(() => expect(current[0].sampleTypeId).toBe("7"));
    await waitFor(() => expect(testIds()).toEqual(["32"]));
  });

  it("ticks the plan tests for a new row reusing a removed row's index", async () => {
    render(<Harness plan={["32"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));

    fireEvent.click(screen.getByRole("button", { name: "Remove Sample" }));
    expect(current).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "+ Add sample row" }));
    fireEvent.click(screen.getByRole("combobox", { name: "Sample Type" }));
    fireEvent.click(screen.getByRole("option", { name: "Drinking Water" }));

    await waitFor(() => expect(testIds()).toEqual(["32"]));
  });

  it("keeps plan ownership on a surviving row when an earlier row is removed", async () => {
    const initialSamples = [0, 1].map((index) => ({
      index,
      sampleTypeId: "7",
      tests: [],
      panels: [],
    }));
    const { rerender } = render(
      <Harness plan={["32"]} initialSamples={initialSamples} />,
    );
    await waitFor(() =>
      expect(
        current.map((sample) => sample.tests.map((test) => test.id)),
      ).toEqual([["32"], ["32"]]),
    );

    fireEvent.click(
      screen.getAllByRole("button", { name: "Remove Sample" })[0],
    );
    expect(current).toHaveLength(1);
    expect(testIds()).toEqual(["32"]);
    rerender(<Harness plan={[]} initialSamples={initialSamples} />);

    await waitFor(() => expect(testIds()).toEqual([]));
  });

  it("keeps the tests of a row that had some before the plan saw it, and ticks only what a standard adds later", async () => {
    const Reopened = ({ plan }) => {
      const [samples, setSamples] = useState([
        {
          index: 0,
          sampleTypeId: "7",
          tests: [{ id: "32", name: "Lead (Pb)" }],
          panels: [],
        },
      ]);
      current = samples;
      return (
        <IntlProvider locale="en" messages={messages}>
          <SampleTestSection
            samples={samples}
            setSamples={setSamples}
            orderData={{}}
            isReadOnly={false}
            workflowType="environmental"
            planTestIds={plan}
          />
        </IntlProvider>
      );
    };
    const { rerender } = render(<Reopened plan={["32", "33"]} />);
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/sample-type-tests?sampleType=7",
        expect.any(Function),
        expect.anything(),
      ),
    );
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(testIds()).toEqual(["32"]);

    rerender(<Reopened plan={["32", "33", "40"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32", "40"]));

    rerender(<Reopened plan={[]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));
  });

  it("keeps the tests an earlier plan ticked once the order turned ad-hoc and back", async () => {
    const { rerender } = render(<Harness plan={["32"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));

    rerender(<Harness plan={null} regulationDriven={false} />);
    rerender(<Harness plan={["32"]} regulationDriven />);
    await new Promise((resolve) => setTimeout(resolve, 50));
    rerender(<Harness plan={[]} regulationDriven />);
    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(testIds()).toEqual(["32"]);
  });

  it("takes off the plan's tests when the last standard is removed", async () => {
    const { rerender } = render(<Harness plan={["32"]} />);
    await waitFor(() => expect(testIds()).toEqual(["32"]));

    rerender(<Harness plan={null} regulationDriven />);
    rerender(<Harness plan={[]} regulationDriven />);

    await waitFor(() => expect(testIds()).toEqual([]));
  });

  it("adds nothing without a plan", async () => {
    render(<Harness plan={null} />);

    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/sample-type-tests?sampleType=7",
        expect.any(Function),
        expect.anything(),
      ),
    );
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(testIds()).toEqual([]);
  });
});
