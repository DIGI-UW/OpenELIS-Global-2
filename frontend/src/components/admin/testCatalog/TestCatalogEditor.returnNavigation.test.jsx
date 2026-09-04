import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, useLocation } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import TestCatalogEditor from "./TestCatalogEditor";

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer: vi.fn() }));

vi.mock("../../layout/Layout", async () => {
  const ReactModule = await import("react");
  return {
    NotificationContext: ReactModule.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
      notificationVisible: false,
    }),
  };
});

vi.mock("./sections/SampleResultsSection", () => ({
  default: () => <div>Result options</div>,
}));

const envelope = {
  testId: "7",
  name: "Drug susceptibility",
  code: "DST",
  domain: "CLINICAL",
  applicableSections: ["sample-results"],
};

const ReturnTarget = () => {
  const location = useLocation();
  return (
    <div data-testid="return-target">{`${location.pathname}${location.search}`}</div>
  );
};

describe("TestCatalogEditor return navigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(envelope),
    );
  });

  it("returns to the exact internal analyzer mapping URL", async () => {
    const returnTo =
      "/analyzers/types/shipped.genexpert/mapping?revision=1&returnTo=%2Fanalyzers%2Ftypes%3Fprotocol%3DASTM";

    render(
      <MemoryRouter
        initialEntries={[
          `/MasterListsPage/TestCatalogEditor/7/sample-results?returnTo=${encodeURIComponent(returnTo)}`,
        ]}
      >
        <IntlProvider locale="en" messages={messages}>
          <Route path="/MasterListsPage/TestCatalogEditor/:testId/:section?">
            <TestCatalogEditor />
          </Route>
          <Route path="/analyzers/types/:profileId/mapping">
            <ReturnTarget />
          </Route>
          <Route exact path="/MasterListsPage/TestCatalogList">
            <div data-testid="test-catalog-list">Test Catalog</div>
          </Route>
        </IntlProvider>
      </MemoryRouter>,
    );

    await screen.findByText("Drug susceptibility");
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(await screen.findByTestId("return-target")).toHaveTextContent(
      returnTo,
    );
  });

  it("ignores a protocol-relative return URL", async () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/MasterListsPage/TestCatalogEditor/7/sample-results?returnTo=%2F%2Fevil.example",
        ]}
      >
        <IntlProvider locale="en" messages={messages}>
          <Route path="/MasterListsPage/TestCatalogEditor/:testId/:section?">
            <TestCatalogEditor />
          </Route>
          <Route exact path="/MasterListsPage/TestCatalogList">
            <div data-testid="test-catalog-list">Test Catalog</div>
          </Route>
        </IntlProvider>
      </MemoryRouter>,
    );

    await screen.findByText("Drug susceptibility");
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(await screen.findByTestId("test-catalog-list")).toBeVisible();
  });
});
