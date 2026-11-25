import React from "react";
import { screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import KpiStrip from "./KpiStrip";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const kpi = (value, prior) => ({ value, prior, spark: [] });

describe("KpiStrip", () => {
  it("says when a figure has no prior period to compare with", async () => {
    fetchFromOpenElisServer.mockResolvedValue({
      samples: kpi(88, 0),
      medianTat: kpi(3.6, 4.1),
      onTime: kpi(97.66, null),
      rejection: kpi(null, null),
      qcPass: kpi(null, null),
    });
    renderQa(<KpiStrip scope="30d" />);

    const tile = (key) => screen.getByTestId(`mgmt-kpi-${key}`);
    await within(await screen.findByTestId("mgmt-kpi-onTime")).findByText(
      "97.66%",
    );
    expect(tile("samples")).toHaveTextContent("No prior data");
    expect(tile("onTime")).toHaveTextContent("No prior data");
    expect(tile("medianTat")).toHaveTextContent("vs. prior period");
    expect(tile("rejection")).not.toHaveTextContent("No prior data");
    expect(
      within(tile("medianTat")).getByRole("button", {
        name: "What does this tile measure?",
      }),
    ).toBeInTheDocument();
  });
});
