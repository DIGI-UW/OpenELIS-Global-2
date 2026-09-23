import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import ReagentLotPicker, {
  formatReagentLotConflict,
} from "../ReagentLotPicker";

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

const ControlledPicker = () => {
  const [selectedLots, setSelectedLots] = React.useState({});
  return (
    <IntlProvider locale="en" messages={messages} timeZone="UTC">
      <ReagentLotPicker
        id="culture-lots"
        requirements={requirements}
        selectedLots={selectedLots}
        onChange={(selection) =>
          setSelectedLots({
            [`${selection.analysisId}:${selection.testReagentLinkId}`]:
              selection,
          })
        }
      />
    </IntlProvider>
  );
};

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
    expect(screen.getAllByText("QC passed")).toHaveLength(3);
    expect(
      screen.getByRole("button", {
        name: "Lots are ordered by earliest expiry, then by receipt date when expiry is unavailable.",
      }),
    ).toBeInTheDocument();
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

  it("selects an eligible lot through scanner-style Enter input", async () => {
    const user = userEvent.setup();
    render(<ControlledPicker />);

    const scanner = screen.getByRole("searchbox", {
      name: "Scan or enter lot number",
    });
    await user.type(scanner, "MEDIA-FEFO{Enter}");

    expect(screen.getByLabelText(/MEDIA-FEFO/)).toBeChecked();
    expect(screen.getByText("Selected lot MEDIA-FEFO.")).toBeInTheDocument();
  });

  it("names the reagent, lot, reason, and corrective action for a race conflict", () => {
    const intl = {
      formatMessage: ({ id }, values) =>
        messages[id]
          .replace("{reagent}", values.reagent)
          .replace("{lot}", values.lot),
    };

    expect(
      formatReagentLotConflict(
        {
          error: "MICROBIOLOGY_LOT_CONFLICT",
          message: "INVENTORY_LOT_EXPIRED",
          lotNumber: "MEDIA-FEFO",
        },
        requirements,
        {
          "41:link-1": {
            analysisId: "41",
            testReagentLinkId: "link-1",
            lotId: 7,
          },
        },
        intl,
      ),
    ).toBe("Blood agar lot MEDIA-FEFO expired; pick another lot.");
  });
});
