import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { NceDashboard } from "../NceDashboard";
import { NotificationContext } from "../../../layout/Layout";
import { loadLabClock, resetLabClock } from "../../../utils/labClock";
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

test("event ages use the lab calendar and overdue begins after seven days", async () => {
  const clock = vi
    .spyOn(Date, "now")
    .mockReturnValue(Date.parse("2031-03-10T04:01:00Z"));
  const events = [
    { ...nce("1", "MAJOR", "Pending"), dateOfEvent: "2031-03-10" },
    { ...nce("2", "MAJOR", "Pending"), dateOfEvent: "2031-03-03" },
    { ...nce("3", "MAJOR", "Pending"), dateOfEvent: "2031-03-02" },
  ];
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/server-time") {
      cb({ date: "2031-03-10", time: "00:01", timezone: "America/New_York" });
    } else if (url.includes("/rest/nce/dashboard")) {
      cb({ nceList: events });
    } else cb([]);
  });
  try {
    await loadLabClock();
    await renderAt("/NceDashboard");
    const row = (id) =>
      screen.getByText(`NCE-2026-000${id}`).closest(".nce-list-item");
    expect(row("1")).toHaveTextContent("0 days");
    expect(row("2")).toHaveTextContent("7 days");
    expect(row("2").querySelector(".overdue")).toBeNull();
    expect(row("3")).toHaveTextContent("8 days");
    expect(row("3").querySelector(".overdue")).not.toBeNull();
  } finally {
    resetLabClock();
    clock.mockRestore();
  }
});
