import React, { useState } from "react";
import { render } from "@testing-library/react";
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
const Harness = ({ plan, regulationDriven = plan != null }) => {
  const [samples, setSamples] = useState([
    { index: 0, sampleTypeId: "7", tests: [], panels: [] },
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
        callback([{ id: "7", value: "Drinking Water" }]);
      } else if (url.startsWith("/rest/sample-type-tests")) {
        callback({
          tests: [
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
