/**
 * The dashboard's stage filter and stage column used to treat every status
 * except COMPLETED as "in progress" and render the raw enum id. With the
 * eleven bench stages, "in progress" must match the backend dashboard tile
 * grouping (everything except awaiting-review and complete) and the stage
 * column must show the localized bench-stage name.
 */
import React from "react";
import { vi } from "vitest";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PathologyDashboard from "./PathologyDashboard";
import { PATHOLOGY_STAGES } from "./pathologyStages";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";

/**
 * The display list is served with the stage id as its own text, so the only
 * way the catalogue-wording assertions below can pass is if the label came
 * from React Intl. Had the screen kept using the server's text, or fallen
 * through to stageLabel's fallback, every option would read as a raw id.
 */
const PATHOLOGY_STATUS_LIST = PATHOLOGY_STAGES.map((id) => ({ id, value: id }));

const DASHBOARD_COUNTS = {
  inProgress: 3,
  awaitingReview: 1,
  additionalRequests: 0,
  complete: 2,
};

const DASHBOARD_ENTRIES = [
  {
    pathologySampleId: 9,
    labNumber: "ACC9",
    firstName: "A",
    lastName: "B",
    status: "MICROTOMY",
    requestDate: "2026-09-01",
  },
];

const renderDashboard = () =>
  render(
    <MemoryRouter initialEntries={["/PathologyDashboard"]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={{ notificationVisible: false }}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: {} }}
          >
            <PathologyDashboard />
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/displayList/PATHOLOGY_STATUS")) {
      return callback(PATHOLOGY_STATUS_LIST);
    }
    if (url.startsWith("/rest/pathology/dashboard/count")) {
      return callback(DASHBOARD_COUNTS);
    }
    if (url.startsWith("/rest/pathology/dashboard?")) {
      return callback(DASHBOARD_ENTRIES);
    }
    return callback([]);
  });
});

it("shows the localized bench-stage name in the Stage column, never the raw enum id", async () => {
  renderDashboard();

  const labNumberCell = await screen.findByText("ACC9");
  const row = labNumberCell.closest("tr");

  expect(
    within(row).getByText(messages["pathology.stage.microtomy"]),
  ).toBeInTheDocument();
  expect(within(row).queryByText("MICROTOMY")).not.toBeInTheDocument();
});

it("requests exactly the nine in-progress stages, in bench order, once the stage list loads", async () => {
  renderDashboard();

  const expectedStatuses = [
    "ACCESSIONED",
    "GROSSING",
    "DECALCIFICATION",
    "PROCESSING",
    "EMBEDDING",
    "MICROTOMY",
    "STAINING",
    "COVERSLIPPING",
    "UNDER_REVIEW",
  ].join(",");

  await waitFor(() => {
    const matchingCall = getFromOpenElisServer.mock.calls.find(([url]) => {
      if (!url.startsWith("/rest/pathology/dashboard?")) {
        return false;
      }
      const statusesParam = new URLSearchParams(url.split("?")[1]).get(
        "statuses",
      );
      return statusesParam === expectedStatuses;
    });

    expect(matchingCall).toBeDefined();
  });
});

it("lists the eleven bench stages in order with localized labels in the status filter", async () => {
  const { container } = renderDashboard();
  await screen.findByText("ACC9");

  const select = container.querySelector("#statusFilter");
  const stageOptionTexts = Array.from(select.querySelectorAll("option"))
    .slice(3)
    .map((option) => option.textContent);

  expect(stageOptionTexts).toEqual([
    messages["pathology.stage.accessioned"],
    messages["pathology.stage.grossing"],
    messages["pathology.stage.decalcification"],
    messages["pathology.stage.processing"],
    messages["pathology.stage.embedding"],
    messages["pathology.stage.microtomy"],
    messages["pathology.stage.staining"],
    messages["pathology.stage.coverslipping"],
    messages["pathology.stage.readyPathologist"],
    messages["pathology.stage.underReview"],
    messages["pathology.stage.completed"],
  ]);
});

it("issues a request scoped to COMPLETED only when the Completed stage is chosen", async () => {
  const { container } = renderDashboard();
  await screen.findByText("ACC9");

  const select = container.querySelector("#statusFilter");
  fireEvent.change(select, { target: { value: "COMPLETED" } });

  await waitFor(() => {
    const matchingCall = getFromOpenElisServer.mock.calls.find(([url]) => {
      if (!url.startsWith("/rest/pathology/dashboard?")) {
        return false;
      }
      return (
        new URLSearchParams(url.split("?")[1]).get("statuses") === "COMPLETED"
      );
    });

    expect(matchingCall).toBeDefined();
  });
});
