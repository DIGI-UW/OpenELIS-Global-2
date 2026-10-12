import React from "react";
import { screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import StockSection from "./StockSection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const STOCK = {
  state: "OK",
  criticalReagents: [
    {
      name: "VITEK AST cards (GN)",
      lot: "VK-887",
      expiry: "2026-07-15",
      onHand: 12,
      threshold: 100,
      status: "REORDER_NOW",
    },
    {
      name: "Malaria RDT kits",
      lot: "MR-2419",
      expiry: "2026-11-30",
      onHand: 486,
      threshold: 600,
      status: "ADEQUATE",
    },
  ],
  expiring: [
    { name: "HIV VL cartridges", lot: "HV-099", daysLeft: -2, remaining: 8 },
    { name: "Agar plates", lot: "AP-1", daysLeft: 1, remaining: 220 },
    {
      name: "VITEK AST cards (GN)",
      lot: "VK-887",
      daysLeft: 18,
      remaining: 12,
    },
  ],
  reorder: [
    {
      name: "VITEK AST cards (GN)",
      onHand: 12,
      threshold: 100,
      kind: "BELOW",
      orderBy: null,
      orderedOn: "2026-06-02",
    },
    {
      name: "HIV VL cartridges",
      onHand: 28,
      threshold: 20,
      kind: "APPROACHING",
      orderBy: "2026-10-20",
      orderedOn: null,
    },
  ],
};

const render = (features = { inventory: true }) =>
  renderQa(<StockSection features={features} />);

const card = (title) => screen.getByText(title).closest(".mgmt-stock__card");

describe("StockSection", () => {
  it("shows the three stock lists with their severity", async () => {
    fetchFromOpenElisServer.mockResolvedValue(STOCK);
    render();

    await screen.findByText("Malaria RDT kits");
    const critical = card("Critical reagents — on hand");
    expect(critical).toHaveTextContent("Lot VK-887 · exp Jul 15, 2026");
    expect(critical).toHaveTextContent("12 / 100");
    expect(critical).toHaveTextContent("12%");
    expect(critical.querySelector(".mgmt-stock__meter--red")).not.toBe(null);
    expect(critical.querySelector(".mgmt-stock__meter--green")).not.toBe(null);

    const expiring = card("Expiring in 30 days");
    expect(within(expiring).getByText("Expired")).toBeInTheDocument();
    expect(within(expiring).getByText("Expires in 1 day")).toBeInTheDocument();
    expect(
      within(expiring).getByText("Expires in 18 days"),
    ).toBeInTheDocument();
    expect(expiring).toHaveTextContent(
      "HIV VL cartridges · Lot HV-099 · 8 remaining",
    );
    expect(
      expiring.querySelectorAll(".cds--inline-notification--error"),
    ).toHaveLength(1);

    const reorder = card("Reorder alerts");
    expect(reorder).toHaveTextContent(
      "VITEK AST cards (GN) · 12 on hand, reorder at 100 · order placed Jun 02, 2026",
    );
    expect(reorder).toHaveTextContent(
      "HIV VL cartridges · 28 on hand, reorder at 20 · order by Oct 20, 2026",
    );
    expect(
      within(reorder)
        .getByText("Approaching reorder point")
        .closest(".cds--inline-notification"),
    ).toHaveClass("cds--inline-notification--warning");
  });

  it("explains an inventory with no stock yet", async () => {
    fetchFromOpenElisServer.mockResolvedValue({
      state: "EMPTY",
      criticalReagents: [],
      expiring: [],
      reorder: [],
    });
    render();

    expect(
      await screen.findByText(/Inventory is being set up at this deployment/),
    ).toBeInTheDocument();
  });

  it("is absent without inventory and asks for nothing", () => {
    fetchFromOpenElisServer.mockClear();
    const { container } = render({ inventory: false });

    expect(container.querySelector("[data-testid='mgmt-stock']")).toBe(null);
    expect(fetchFromOpenElisServer).not.toHaveBeenCalled();
  });
});
