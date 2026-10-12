import React from "react";
import { screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import EquipmentSection, { timeAgo } from "./EquipmentSection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const minutesAgo = (minutes) =>
  new Date(Date.now() - minutes * 60000).toISOString();

describe("EquipmentSection", () => {
  it("tags each analyzer by status and says when it was last active", async () => {
    fetchFromOpenElisServer.mockResolvedValue([
      {
        id: "1",
        name: "Sysmex XN-550",
        status: "ACTIVE",
        samples: 1247,
        section: "Hematology",
        lastActivityAt: minutesAgo(2),
      },
      {
        id: "2",
        name: "GeneXpert IV",
        status: "QUIET",
        samples: 1,
        section: "Molecular Biology",
        lastActivityAt: minutesAgo(4 * 60),
      },
      {
        id: "3",
        name: "Roche cobas 4800",
        status: "NO_ACTIVITY",
        samples: 0,
        section: null,
        lastActivityAt: minutesAgo(2 * 24 * 60),
      },
    ]);
    renderQa(
      <EquipmentSection
        scope="30d"
        scopeLabel="Last 30 days"
        features={{ analyzers: true }}
      />,
    );

    const cards = await screen.findAllByTestId("mgmt-equipment-card");
    expect(cards.map((card) => card.querySelector("h4").textContent)).toEqual([
      "Sysmex XN-550",
      "GeneXpert IV",
      "Roche cobas 4800",
    ]);
    const tag = (card, text) =>
      within(card).getByText(text).closest(".cds--tag");
    expect(tag(cards[0], "Active")).toHaveClass("cds--tag--green");
    expect(tag(cards[1], "Quiet")).toHaveClass("qi-rate-tag--amber");
    expect(tag(cards[2], "No activity")).toHaveClass("cds--tag--red");
    expect(cards[0]).toHaveTextContent("1,247 samples · Last 30 days");
    expect(cards[1]).toHaveTextContent("1 sample · Last 30 days");
    expect(cards[0]).toHaveTextContent("Last activity: 2m ago");
    expect(cards[1]).toHaveTextContent("Last activity: 4h ago");
    expect(cards[2]).toHaveTextContent("0 samples · Last 30 days");
    expect(cards[2]).toHaveTextContent("Last activity: 2d ago");
  });

  it("renders nothing and asks for nothing without analyzers", () => {
    fetchFromOpenElisServer.mockClear();
    const { container } = renderQa(
      <EquipmentSection
        scope="30d"
        scopeLabel="Last 30 days"
        features={{ analyzers: false }}
      />,
    );

    expect(container.querySelector("[data-testid='mgmt-equipment']")).toBe(
      null,
    );
    expect(fetchFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("switches from minutes to hours to days at an hour and a day", () => {
    const now = Date.parse("2026-10-12T12:00:00Z");
    const at = (minutes) => new Date(now - minutes * 60000).toISOString();

    expect(timeAgo(at(59), "en", now)).toBe("59m ago");
    expect(timeAgo(at(60), "en", now)).toBe("1h ago");
    expect(timeAgo(at(23 * 60 + 29), "en", now)).toBe("23h ago");
    expect(timeAgo(at(24 * 60), "en", now)).toBe("1d ago");
  });
});
