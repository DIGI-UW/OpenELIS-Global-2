import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { NceDashboard } from "../NceDashboard";
import { NotificationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServer: vi.fn(),
  };
});

const nce = (id, severity, status) => ({
  id,
  nceNumber: `NCE-2026-000${id}`,
  title: `Event ${id}`,
  description: "",
  severity,
  status,
  dateOfEvent: "2026-07-01",
  reportDate: "2026-07-01",
  nameOfReporter: "Reporter",
  assignedTo: null,
  assignedToName: null,
  notesCount: 0,
  notes: [],
  linkedSpecimens: [],
  attachments: [],
  history: [],
});

const NCE_LIST = [
  nce("1", "CRITICAL", "Pending"),
  nce("2", "MAJOR", "Pending"),
  nce("3", "CRITICAL", "Completed"),
];

const renderAt = async (url) => {
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ notificationVisible: false, addNotification: vi.fn() }}
        >
          <MemoryRouter initialEntries={[url]}>
            <NceDashboard />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    ),
  );
};

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/nce/dashboard")) {
      callback({ nceList: NCE_LIST });
    } else if (url.includes("/rest/nce/categories")) {
      callback([]);
    } else if (url.includes("/rest/nce/users")) {
      callback([]);
    }
  });
});

describe("NceDashboard URL-seeded filters", () => {
  test("without params all events are listed", async () => {
    await renderAt("/NceDashboard");

    expect(screen.getByText("NCE-2026-0001")).toBeInTheDocument();
    expect(screen.getByText("NCE-2026-0002")).toBeInTheDocument();
    expect(screen.getByText("NCE-2026-0003")).toBeInTheDocument();
  });

  test("?severity=CRITICAL&status=Pending seeds the filters and narrows the list", async () => {
    await renderAt("/NceDashboard?severity=CRITICAL&status=Pending");

    expect(screen.getByText("NCE-2026-0001")).toBeInTheDocument();
    expect(screen.queryByText("NCE-2026-0002")).not.toBeInTheDocument();
    expect(screen.queryByText("NCE-2026-0003")).not.toBeInTheDocument();

    expect(document.getElementById("severity-filter")).toHaveValue("CRITICAL");
    expect(document.getElementById("status-filter")).toHaveValue("Pending");
  });
});
