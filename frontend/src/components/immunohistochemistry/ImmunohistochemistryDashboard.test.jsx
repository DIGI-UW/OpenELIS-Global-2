import React from "react";
import { vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import ImmunohistochemistryDashboard from "./ImmunohistochemistryDashboard";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";

// The display list echoes each id as its text, so a label can only come from
// React Intl, never from the served value.
const STATUS_LIST = ["IN_PROGRESS", "READY_PATHOLOGIST", "COMPLETED"].map(
  (id) => ({ id, value: id }),
);

const renderDashboard = () =>
  render(
    <MemoryRouter initialEntries={["/ihc"]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={{ notificationVisible: false }}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: {} }}
          >
            <ImmunohistochemistryDashboard />
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/displayList/IMMUNOHISTOCHEMISTRY_STATUS")) {
      return callback(STATUS_LIST);
    }
    if (url.startsWith("/rest/immunohistochemistry/dashboard/count")) {
      return callback({ inProgress: 1, awaitingReview: 0, complete: 0 });
    }
    if (url.startsWith("/rest/immunohistochemistry/dashboard?")) {
      return callback({
        items: [
          {
            immunohistochemistrySampleId: 4,
            labNumber: "ACC4",
            firstName: "A",
            lastName: "B",
            status: "READY_PATHOLOGIST",
            requestDate: "2026-09-01",
          },
        ],
        paging: { currentPage: "1", totalPages: "1" },
      });
    }
    return callback([]);
  });
});

it("shows the localized stage label in the Stage column, never the raw enum id", async () => {
  renderDashboard();
  const row = (await screen.findByText("ACC4")).closest("tr");
  expect(
    within(row).getByText(
      messages["immunohistochemistry.status.readyPathologist"],
    ),
  ).toBeInTheDocument();
  expect(within(row).queryByText("READY_PATHOLOGIST")).not.toBeInTheDocument();
});

it("lists the stages by label in the status filter", async () => {
  const { container } = renderDashboard();
  await screen.findByText("ACC4");
  const options = Array.from(
    container.querySelector("#statusFilter").querySelectorAll("option"),
  ).map((o) => o.textContent);
  expect(options).toContain(
    messages["immunohistochemistry.status.readyPathologist"],
  );
  expect(options).not.toContain("READY_PATHOLOGIST");
});
