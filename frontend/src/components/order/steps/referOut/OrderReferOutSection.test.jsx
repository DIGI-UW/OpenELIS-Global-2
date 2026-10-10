import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const orderContext = {
  samples: [],
  setSamples: vi.fn(),
  orderData: {
    referralOrganizations: [
      { id: "9000101", value: "CEDRES" },
      { id: "9000102", value: "Central Reference Lab" },
    ],
    referralReasons: [
      { id: "1", value: "Test not performed here" },
      { id: "2", value: "Confirmation" },
    ],
  },
  loadOrder: vi.fn(),
  labNumber: "DEV0126000000000099",
};

vi.mock("../../OrderContext", () => ({
  useOrderContext: () => orderContext,
}));

vi.mock("../../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
    ConfigurationContext: createContext({ configurationProperties: {} }),
  };
});

import { NotificationContext } from "../../../layout/Layout";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import OrderReferOutSection from "./OrderReferOutSection";

const tube = (sampleItemId, overrides = {}) => ({
  sampleItemId,
  sampleTypeId: "2",
  sampleTypeName: "Serum",
  tests: [{ id: "7", name: "Glucose" }],
  ...overrides,
});

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification: vi.fn(), setNotificationVisible: vi.fn() }}
      >
        <OrderReferOutSection />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const pickLab = async (name) => {
  const combo = screen.getByRole("combobox", { name: "Reference lab" });
  fireEvent.click(combo);
  const option = await screen.findByText(name);
  fireEvent.click(option);
};

describe("OrderReferOutSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    orderContext.samples = [tube("501"), tube("502")];
  });

  test("Save Referral stages the referral on the tube for the step's save instead of saving at once", async () => {
    renderSection();
    fireEvent.click(screen.getAllByRole("button", { name: "Refer Out" })[0]);
    await pickLab("CEDRES");
    fireEvent.change(screen.getByLabelText("Reason"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Referral" }));

    await waitFor(() => expect(orderContext.setSamples).toHaveBeenCalled());
    const staged = orderContext.setSamples.mock.calls[0][0];
    expect(staged[0].referralItems[0]).toMatchObject({
      referredInstituteId: "9000101",
      referralReasonId: "2",
      pendingSave: true,
    });
    expect(staged[1].referralItems).toBeUndefined();
    // No order save of its own: the step's Save carries it (FR-E5).
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("a staged referral shows as pending save with its laboratory", () => {
    orderContext.samples = [
      tube("501", {
        referralItems: [{ referredInstituteId: "9000101", pendingSave: true }],
      }),
      tube("502"),
    ];
    renderSection();

    const rows = screen.getAllByRole("row");
    const first = rows.find((row) =>
      within(row).queryByText("DEV0126000000000099-1"),
    );
    expect(within(first).getByText("Pending save")).toBeInTheDocument();
    expect(within(first).getByText("CEDRES")).toBeInTheDocument();
    expect(
      within(first).queryByRole("button", { name: "Refer Out" }),
    ).toBeNull();
  });

  test("Refer out all in-house samples stages one referral per tube that has none", async () => {
    orderContext.samples = [
      tube("501", {
        referralItems: [
          {
            referralId: "26",
            referredInstituteId: "9000102",
            referralStatus: "DRAFT",
          },
        ],
      }),
      tube("502"),
      tube("503"),
    ];
    renderSection();
    fireEvent.click(screen.getByTestId("refer-out-all"));
    const bulkForm = screen.getByTestId("refer-out-bulk-form");
    const combo = within(bulkForm).getByRole("combobox", {
      name: "Reference lab",
    });
    fireEvent.click(combo);
    fireEvent.click(await screen.findByText("CEDRES"));
    fireEvent.click(
      within(bulkForm).getByRole("button", { name: "Save Referral" }),
    );

    await waitFor(() => expect(orderContext.setSamples).toHaveBeenCalled());
    const staged = orderContext.setSamples.mock.calls[0][0];
    expect(staged[0].referralItems[0]).toMatchObject({
      referralId: "26",
      referredInstituteId: "9000102",
    });
    expect(staged[0].referralItems[0].pendingSave).toBeUndefined();
    expect(staged[1].referralItems[0]).toMatchObject({
      referredInstituteId: "9000101",
      pendingSave: true,
    });
    expect(staged[2].referralItems[0]).toMatchObject({
      referredInstituteId: "9000101",
      pendingSave: true,
    });
  });

  test("the bulk action is unavailable when every tube is already referred", () => {
    orderContext.samples = [
      tube("501", {
        referralItems: [{ referredInstituteId: "9000101", pendingSave: true }],
      }),
    ];
    renderSection();
    expect(screen.getByTestId("refer-out-all")).toBeDisabled();
  });

  test("names each tube by the order's lab number and its position, not the database id (OGC-1443)", () => {
    renderSection();

    expect(screen.getByText("DEV0126000000000099-1")).toBeInTheDocument();
    expect(screen.getByText("DEV0126000000000099-2")).toBeInTheDocument();
    expect(screen.queryByText("501")).toBeNull();
    expect(
      screen.getByRole("columnheader", { name: /Reference lab/ }),
    ).toBeInTheDocument();
  });

  test("refers the tube on the clicked row when an uncollected tube comes first (OGC-1443)", async () => {
    orderContext.samples = [
      { sampleTypeId: "2", sampleTypeName: "Serum", tests: [] },
      tube("502"),
      tube("503"),
    ];
    renderSection();

    const row = screen
      .getAllByRole("row")
      .find((r) => within(r).queryByText("DEV0126000000000099-3"));
    fireEvent.click(within(row).getByRole("button", { name: "Refer Out" }));
    await pickLab("CEDRES");
    fireEvent.change(screen.getByLabelText("Reason"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Referral" }));

    await waitFor(() => expect(orderContext.setSamples).toHaveBeenCalled());
    const staged = orderContext.setSamples.mock.calls[0][0];
    expect(staged[2].referralItems[0]).toMatchObject({
      referredInstituteId: "9000101",
      pendingSave: true,
    });
    expect(staged[0].referralItems).toBeUndefined();
    expect(staged[1].referralItems).toBeUndefined();
  });
});
