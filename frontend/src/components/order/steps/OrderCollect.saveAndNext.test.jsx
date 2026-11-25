import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import OrderCollect from "./OrderCollect";

const order = {
  sampleCheckEnabled: true,
  samples: [{ sampleTypeId: "2", tests: [{ id: "1" }] }],
};

vi.mock("../OrderContext", () => ({
  useWorkflowPrefix: () => "/order/clinical",
  useOrderContext: () => ({
    orderId: "",
    orderData: { sampleOrderItems: { labNo: "DEV01260000000000400" } },
    samples: order.samples,
    setSamples: vi.fn(),
    seedSamples: vi.fn(),
    saveOrder: vi.fn().mockResolvedValue({}),
    markStepComplete: vi.fn(),
    isReadOnly: false,
    isEditMode: false,
    isLoading: false,
    testSampleAssignments: {},
    assignTestToSample: vi.fn(),
    removeTestFromSample: vi.fn(),
    updateSampleCollectionDetails: vi.fn(),
    fillSampleDefaults: vi.fn(),
    setOrderData: vi.fn(),
    labNumber: "DEV01260000000000400",
    storageSkipped: false,
    stageStorageSkipped: vi.fn(),
    sampleCheckEnabled: order.sampleCheckEnabled,
  }),
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ onSaveAndNext }) => (
    <button type="button" onClick={onSaveAndNext}>
      Save and next
    </button>
  ),
}));

vi.mock("../api/sampleAcceptanceApi", () => ({
  getEnforcement: () => Promise.resolve({}),
}));
vi.mock("../api/sampleTypeRequestApi", () => ({
  getPendingRequests: () => Promise.resolve([]),
  convertRequestsToSamples: () => [],
}));
vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

/**
 * Prepare Samples' Save and next opened Sample check without the saved
 * message every other v4 save shows (OGC-1598). The message is queued before
 * the page changes, so Sample check shows it; when the order has no Sample
 * check the dashboard reports the save instead.
 */
describe("OrderCollect Save and next", () => {
  let addNotification;

  beforeEach(() => {
    addNotification = vi.fn();
    order.sampleCheckEnabled = true;
  });

  const renderCollect = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification,
            }}
          >
            <MemoryRouter initialEntries={["/order/clinical/collect"]}>
              <Route
                path="*"
                render={({ location }) => (
                  <>
                    <OrderCollect />
                    <span data-testid="where">{location.pathname}</span>
                  </>
                )}
              />
            </MemoryRouter>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

  it("names the saved order when it opens Sample check", async () => {
    renderCollect();

    await userEvent.click(
      screen.getByRole("button", { name: "Save and next" }),
    );

    await waitFor(() =>
      expect(screen.getByTestId("where")).toHaveTextContent(
        /^\/order\/clinical\/qa$/,
      ),
    );
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: messages["save.order.success.labNumber"].replace(
          "{labNumber}",
          "DEV01260000000000400",
        ),
      }),
    );
  });

  it("leaves the message to the dashboard when there is no Sample check", async () => {
    order.sampleCheckEnabled = false;
    renderCollect();

    await userEvent.click(
      screen.getByRole("button", { name: "Save and next" }),
    );

    await waitFor(() =>
      expect(screen.getByTestId("where")).toHaveTextContent(
        /^\/order\/clinical$/,
      ),
    );
    expect(addNotification).not.toHaveBeenCalled();
  });
});
