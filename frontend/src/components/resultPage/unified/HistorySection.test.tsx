import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));
// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

import HistorySection from "./HistorySection";

/**
 * OGC-811 — component-aware history: a component row asks the endpoint for
 * its own events plus analysis-level ones via the componentId param; rows
 * without a component keep the unchanged analysis-level request.
 */
const wrap = (node: React.ReactElement) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

describe("HistorySection component scoping", () => {
  beforeEach(() => {
    getMock.mockReset();
  });

  it("component rows request the component-filtered timeline", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) =>
      cb({ events: [], total: 0, page: 1, pageSize: 25 }),
    );
    wrap(
      <HistorySection
        analysisId="28"
        componentId="comp-A"
        open={true}
        onToggle={() => {}}
      />,
    );
    expect(getMock).toHaveBeenCalledWith(
      "/rest/results-entry/analysis/28/history?page=1&pageSize=25&componentId=comp-A",
      expect.any(Function),
    );
  });

  it("rows without a component keep the unchanged analysis-level request", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) =>
      cb({ events: [], total: 0, page: 1, pageSize: 25 }),
    );
    wrap(<HistorySection analysisId="28" open={true} onToggle={() => {}} />);
    expect(getMock).toHaveBeenCalledWith(
      "/rest/results-entry/analysis/28/history?page=1&pageSize=25",
      expect.any(Function),
    );
  });

  it("renders returned events", async () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) =>
      cb({
        events: [
          {
            type: "NOTE",
            when: "10/08/2026 10:00",
            detail: "Result Note: haemolysed",
            by: "Doe,John",
            componentId: "comp-A",
          },
        ],
        total: 1,
        page: 1,
        pageSize: 25,
      }),
    );
    wrap(
      <HistorySection
        analysisId="28"
        componentId="comp-A"
        open={true}
        onToggle={() => {}}
      />,
    );
    expect(
      await screen.findByText("Result Note: haemolysed"),
    ).toBeInTheDocument();
  });
});
