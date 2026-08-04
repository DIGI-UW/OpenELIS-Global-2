import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import ReagentLotPicker from "../ReagentLotPicker";

const requirements = [
  {
    analysisId: "41",
    testId: "22",
    testName: "Blood culture",
    linkId: "link-1",
    reagentId: 13,
    reagentName: "Blood agar",
    usageType: "PRIMARY",
    quantityPerTest: 1,
    quantityUnit: "plate",
    lots: [
      {
        id: 6,
        lotNumber: "MEDIA-EXPIRED",
        effectiveExpirationDate: "2026-07-01T00:00:00Z",
        currentQuantity: 5,
        qcStatus: "PASSED",
        status: "ACTIVE",
        available: false,
        unavailableReason: "INVENTORY_LOT_EXPIRED",
        fefoRecommended: false,
      },
      {
        id: 7,
        lotNumber: "MEDIA-FEFO",
        effectiveExpirationDate: "2026-09-01T00:00:00Z",
        currentQuantity: 10,
        qcStatus: "PASSED",
        status: "ACTIVE",
        available: true,
        unavailableReason: null,
        fefoRecommended: true,
      },
      {
        id: 8,
        lotNumber: "MEDIA-LATER",
        effectiveExpirationDate: "2026-12-01T00:00:00Z",
        currentQuantity: 20,
        qcStatus: "PASSED",
        status: "ACTIVE",
        available: true,
        unavailableReason: null,
        fefoRecommended: false,
      },
    ],
  },
];

const renderPicker = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages} timeZone="UTC">
      <ReagentLotPicker
        id="culture-lots"
        requirements={requirements}
        selectedLots={{}}
        onChange={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

describe("ReagentLotPicker", () => {
  it("shows catalog role separately from lot eligibility and FEFO", () => {
    renderPicker();

    expect(screen.getByText("Blood agar")).toBeInTheDocument();
    expect(screen.getByText("Blood culture")).toHaveClass(
      "microbiology-reagent-lots__test-name",
    );
    expect(screen.getByText("Primary")).toBeInTheDocument();
    expect(screen.getByLabelText(/MEDIA-EXPIRED/)).toBeDisabled();
    expect(screen.getByText("Blocked: Expired")).toBeInTheDocument();
    expect(screen.getByText("FEFO - use first")).toBeInTheDocument();
    expect(screen.getByLabelText(/MEDIA-FEFO/)).toBeEnabled();
    expect(screen.getByLabelText(/MEDIA-LATER/)).toBeEnabled();
  });

  it("returns a typed selection through a normal radio interaction", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderPicker({ onChange });

    await user.click(screen.getByLabelText(/MEDIA-FEFO/));

    expect(onChange).toHaveBeenCalledWith({
      analysisId: "41",
      testReagentLinkId: "link-1",
      lotId: 7,
    });
  });

  it("does not invent a mandatory policy when no links are configured", () => {
    renderPicker({ requirements: [] });

    expect(
      screen.getByText("No reagents are linked to this test."),
    ).toBeInTheDocument();
  });
});
