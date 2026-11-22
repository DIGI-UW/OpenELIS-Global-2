import React from "react";
import { vi } from "vitest";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { ViewNonConformingEvent } from "../ViewNonConforming";
import { NotificationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const response = (event) => ({
  nceEventsSearchResults: [
    {
      id: "1",
      nceNumber: "NCE-1",
      labOrderNumber: "DEV01",
      ...event,
    },
  ],
  nceCategories: [
    { id: "3", value: "Pre-analytical" },
    { id: "4", value: "Analytical" },
  ],
  nceTypes: [
    { id: "7", value: "Specimen mislabeled" },
    { id: "8", value: "Instrument failure" },
  ],
  severityConsequencesList: [],
  severityRecurrenceList: [],
  labComponentList: [],
  reportingUnits: [],
  specimens: [],
});

const renderDeepLink = async (payload) => {
  getFromOpenElisServer.mockImplementation((url, callback) =>
    callback(payload),
  );
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <MemoryRouter
            initialEntries={["/ViewNonConformingEvent?nceNumber=NCE-1"]}
          >
            <ViewNonConformingEvent />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    ),
  );
};

describe("ViewNonConformingEvent saved classification", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  it("shows the saved severity, category and type by label", async () => {
    await renderDeepLink(
      response({ severity: "MAJOR", nceCategoryId: 4, nceTypeId: 8 }),
    );
    expect(screen.getByText("Major")).toBeVisible();
    expect(screen.getByText("Analytical", { selector: "div" })).toBeVisible();
    expect(
      screen.getByText("Instrument failure", { selector: "div" }),
    ).toBeVisible();
  });

  it("shows no saved classification for an unclassified event", async () => {
    await renderDeepLink(response({}));
    expect(screen.queryByText("Major")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Analytical", { selector: "div" }),
    ).not.toBeInTheDocument();
  });
});
