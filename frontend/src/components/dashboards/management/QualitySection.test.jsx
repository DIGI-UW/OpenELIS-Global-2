import React from "react";
import { screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import QualitySection from "./QualitySection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const ROLLUP = {
  composite: 91.3,
  prior: 88.9,
  components: [
    { key: "qcPass", value: 93.33, weight: 0.3 },
    { key: "eqa", value: null, weight: 0.25 },
    { key: "nceInverse", value: 98.2, weight: 0.25 },
    { key: "amendmentInverse", value: 99.1, weight: 0.2 },
  ],
};

const render = (session = {}) =>
  renderQa(<QualitySection scope="30d" scopeLabel="Last 30 days" />, session);

describe("QualitySection", () => {
  it("shows the composite, each component, and which ones have no data", async () => {
    fetchFromOpenElisServer.mockResolvedValue(ROLLUP);
    render({ permissions: ["qa.manage.qi"], roles: ["Results"] });

    const tile = await screen.findByTestId("mgmt-quality-composite");
    expect(tile).toHaveTextContent("91.3");
    expect(tile).toHaveTextContent("↑ 2.4");
    expect(tile).toHaveTextContent("Last 30 days");
    expect(screen.getByTestId("mgmt-quality-qcPass")).toHaveTextContent(
      "QC pass rate93.3weight 0.30",
    );
    expect(screen.getByTestId("mgmt-quality-eqa")).toHaveTextContent("—");
    expect(
      within(screen.getByTestId("mgmt-quality-total")).getByText("91.3"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("mgmt-quality-missing")).toHaveTextContent(
      "EQA performance",
    );
    expect(screen.getByText("Edit weights ↗")).toHaveAttribute(
      "href",
      "/qa/qi/config#composite-weights",
    );
    expect(screen.getByText("QI Dashboard ↗")).toHaveAttribute(
      "href",
      "/qa/qi/dashboard",
    );
  });

  it("hides the links a viewer cannot open", async () => {
    fetchFromOpenElisServer.mockResolvedValue(ROLLUP);
    render({ permissions: ["dashboards.view.management"] });

    await screen.findByTestId("mgmt-quality-composite");
    expect(screen.queryByText("Edit weights ↗")).toBe(null);
    expect(screen.queryByText("QI Dashboard ↗")).toBe(null);
  });
});
