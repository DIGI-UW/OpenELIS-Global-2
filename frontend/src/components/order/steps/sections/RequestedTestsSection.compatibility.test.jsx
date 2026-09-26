import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));
vi.mock("./TestAssignmentModal", () => ({ default: () => null }));

import RequestedTestsSection from "./RequestedTestsSection";

const samples = [
  {
    sampleTypeId: "",
    tests: [{ id: "7", name: "Amylase" }],
    panels: [{ id: "30", name: "Liver Function Panel", testIds: "8,9" }],
  },
];

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RequestedTestsSection
        samples={samples}
        setSamples={() => {}}
        assignTestToSample={() => {}}
        sampleTypes={[
          { id: "1", value: "Histopathology" },
          { id: "2", value: "Serum" },
        ]}
        isReadOnly={false}
      />
    </IntlProvider>,
  );

describe("RequestedTestsSection compatible sample types (FR-K13)", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((_url, cb) =>
      cb({
        tests: [{ testId: "7", compatibleSampleTypes: [] }],
        panels: [
          {
            panelId: "30",
            compatibleSampleTypes: [{ id: "2", name: "Serum" }],
          },
        ],
      }),
    );
  });

  it("asks for the tests and the panels in one request", () => {
    renderSection();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-sample-types?testIds=7&panelIds=30",
      expect.any(Function),
    );
  });

  it("offers a panel its own mapped sample types", () => {
    renderSection();

    expect(screen.getByText("+ Serum")).toBeInTheDocument();
  });

  it("never falls back to an arbitrary list for a test with no mapping", () => {
    renderSection();

    expect(screen.getByTestId("no-compatible-types-7")).toHaveTextContent(
      "No sample type is set up for this test in the test catalog",
    );
    expect(screen.queryByText(/Histopathology/)).toBeNull();
  });
});
