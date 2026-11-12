import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
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
  default: ({ children }) => <div>{children}</div>,
}));

vi.mock("./sections/RequestedTestsSection", () => ({ default: () => null }));
vi.mock("./sections/CollectTestPickerSection", () => ({ default: () => null }));
vi.mock("./sections/SamplesCollectionSection", () => ({ default: () => null }));
vi.mock("./sections/PrepareLabelsSection", () => ({ default: () => null }));
vi.mock("./sections/PrepareStorageSection", () => ({ default: () => null }));
vi.mock("./referOut/OrderReferOutSection", () => ({ default: () => null }));
vi.mock("./sections/ConsentAccordionSection", () => ({ default: () => null }));
vi.mock("./sections/SampleAcceptanceReview", () => ({ default: () => null }));
vi.mock("../../nonconform/common/InlineNceForm", () => ({
  default: () => null,
}));
vi.mock("../SaveFailureNotice", () => ({
  default: () => null,
  saveFailureMessage: () => "",
}));
vi.mock("../api/sampleAcceptanceApi", () => ({
  getEnforcement: () => Promise.resolve({}),
}));
vi.mock("../api/sampleTypeRequestApi", () => ({
  getPendingRequests: () => Promise.resolve([]),
  convertRequestsToSamples: () => [],
}));
const proposal = { result: () => Promise.resolve([]) };
vi.mock("../api/containerProposalApi", () => ({
  proposeContainers: () => proposal.result(),
}));
vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

/**
 * OGC-1611 P4: a proposal that fails to load says so instead of leaving the
 * samples without container choices and no explanation.
 */
describe("OrderCollect container proposal", () => {
  const renderCollect = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <MemoryRouter initialEntries={["/order/clinical/collect"]}>
              <Route path="*" render={() => <OrderCollect />} />
            </MemoryRouter>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

  beforeEach(() => {
    proposal.result = () => Promise.resolve([]);
  });

  it("says the containers could not be proposed when the request fails", async () => {
    proposal.result = () => Promise.reject(new Error("offline"));
    renderCollect();

    await waitFor(() =>
      expect(screen.getByTestId("container-proposal-failed")).toHaveTextContent(
        "The container choices for these samples did not load. Reload the page to try again.",
      ),
    );
  });

  it("shows no warning when the proposal answers", async () => {
    let answered;
    proposal.result = () =>
      new Promise((resolve) => {
        answered = resolve;
        resolve([]);
      });
    renderCollect();

    await waitFor(() => expect(answered).toBeDefined());
    expect(
      screen.queryByTestId("container-proposal-failed"),
    ).not.toBeInTheDocument();
  });
});
