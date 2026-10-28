import React from "react";
import { screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import SurveillanceSection from "./SurveillanceSection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const metric = (key, label, value, numerator, denominator, delta = null) => ({
  key,
  label,
  value,
  numerator,
  denominator,
  delta,
});

const renderSection = () =>
  renderQa(<SurveillanceSection scope="30d" scopeLabel="Last 30 days" />);

describe("SurveillanceSection", () => {
  it("draws a card per program, named by its message key or its own text", async () => {
    fetchFromOpenElisServer.mockResolvedValue({
      programs: [
        {
          code: "HIV",
          name: "label.mgmt.surveillance.hiv",
          testCount: 1089,
          metrics: [
            metric("VL_SUPPRESSED", "VL suppressed", 78.4, 784, 1000, 2.1),
          ],
          missingCodes: [],
        },
        {
          code: "COVID",
          name: "COVID-19",
          testCount: 0,
          metrics: [
            metric("COVID_POSITIVE", "SARS-CoV-2 detected", null, 0, 0),
          ],
          missingCodes: [],
        },
        {
          code: "AMR",
          name: "label.mgmt.surveillance.amr",
          testCount: 0,
          metrics: [metric("MRSA", "MRSA", null, 0, 0)],
          missingCodes: ["FOX", "sau"],
        },
      ],
    });
    renderSection();

    const cards = await screen.findAllByTestId("mgmt-surveillance-card");
    expect(cards.map((card) => card.querySelector("h4").textContent)).toEqual([
      "HIV",
      "COVID-19",
      "AMR",
    ]);
    expect(cards[0]).toHaveTextContent("1,089 tests · Last 30 days");
    expect(cards[0]).toHaveTextContent("VL suppressed78.4%");
    expect(cards[0]).toHaveTextContent("784 of 1,000 tested");
    expect(cards[0]).toHaveTextContent(
      "VL suppressed ↑ 2.1pp vs. prior period",
    );
    expect(cards[1]).toHaveTextContent("SARS-CoV-2 detected—");
    expect(cards[1]).not.toHaveTextContent("vs. prior period");
    expect(screen.getByTestId("mgmt-surveillance-setup")).toHaveTextContent(
      "FOX, sau",
    );
  });

  it("renders nothing when no program has a metric", async () => {
    fetchFromOpenElisServer.mockResolvedValue({ programs: [] });
    const { container } = renderSection();

    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());
    await waitFor(() =>
      expect(container.querySelector(".cds--skeleton__text")).toBe(null),
    );
    expect(screen.queryByTestId("mgmt-surveillance")).toBe(null);
  });
});
