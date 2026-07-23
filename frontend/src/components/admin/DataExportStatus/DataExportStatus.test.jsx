import React from "react";
import { fireEvent, render, screen, wait } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

const { getFromOpenElisServerMock, postToOpenElisServerFullResponseMock } =
  vi.hoisted(() => ({
    getFromOpenElisServerMock: vi.fn(),
    postToOpenElisServerFullResponseMock: vi.fn(),
  }));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: getFromOpenElisServerMock,
  postToOpenElisServerFullResponse: postToOpenElisServerFullResponseMock,
}));

vi.mock("../../common/PageBreadCrumb", () => ({
  default: () => null,
}));

import DataExportStatus from "./DataExportStatus";

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

// Drives a queue of callback responses keyed by URL substring. Lets each test
// declare the API shape it wants for the status and attempts endpoints, while
// the polling timer keeps re-firing in the background.
function wireServer(handlers) {
  getFromOpenElisServerMock.mockImplementation((url, cb) => {
    for (const [match, response] of handlers) {
      if (url.includes(match)) {
        cb(typeof response === "function" ? response(url) : response);
        return;
      }
    }
    cb(null);
  });
}

describe("DataExportStatus", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    postToOpenElisServerFullResponseMock.mockReset();
  });

  test("renders empty-state message when backend returns no subscribers", async () => {
    wireServer([[`/rest/DataExportStatus`, []]]);

    renderWithIntl(<DataExportStatus />);

    expect(
      await screen.findByText(messages["dataexport.status.empty"]),
    ).toBeInTheDocument();
  });

  test("renders a row per subscriber and tags the last status", async () => {
    wireServer([
      [
        `/rest/DataExportStatus`,
        [
          {
            id: 4820,
            endpoint: "https://partner.example.org/fhir/",
            maxIntervalMinutes: 5,
            lastSuccess: "2026-05-12T07:23:27Z",
            lastAttempt: "2026-05-12T07:23:27Z",
            lastStatus: "SUCCEEDED",
            failedLast24h: 0,
            totalLast24h: 12,
          },
        ],
      ],
    ]);

    renderWithIntl(<DataExportStatus />);

    expect(
      await screen.findByText("https://partner.example.org/fhir/"),
    ).toBeInTheDocument();
    expect(screen.getByText("SUCCEEDED")).toBeInTheDocument();
    expect(screen.getByText("5 min")).toBeInTheDocument();
    expect(screen.getByText("0 / 12")).toBeInTheDocument();
  });

  test("expanding a row triggers the attempts request", async () => {
    wireServer([
      [
        `/rest/DataExportStatus/4820/attempts`,
        [
          {
            id: 1,
            status: "SUCCEEDED",
            startTime: "2026-05-12T07:23:27Z",
            endTime: "2026-05-12T07:23:28Z",
            durationMs: 600,
          },
        ],
      ],
      [
        `/rest/DataExportStatus`,
        [
          {
            id: 4820,
            endpoint: "https://partner.example.org/fhir/",
            maxIntervalMinutes: 5,
            lastSuccess: "2026-05-12T07:23:27Z",
            lastAttempt: "2026-05-12T07:23:27Z",
            lastStatus: "SUCCEEDED",
            failedLast24h: 0,
            totalLast24h: 1,
          },
        ],
      ],
    ]);

    renderWithIntl(<DataExportStatus />);
    await screen.findByText("https://partner.example.org/fhir/");

    const attemptsBefore = getFromOpenElisServerMock.mock.calls.filter((c) =>
      c[0].includes("/attempts"),
    ).length;
    expect(attemptsBefore).toBe(0);

    const expandButton = document.querySelector(
      "button.cds--table-expand__button",
    );
    expect(expandButton).not.toBeNull();
    fireEvent.click(expandButton);

    await wait(() => {
      const attemptsAfter = getFromOpenElisServerMock.mock.calls.filter((c) =>
        c[0].includes("/attempts"),
      ).length;
      expect(attemptsAfter).toBeGreaterThan(0);
    });
  });

  test("retry button POSTs to the trigger endpoint and re-fetches statuses", async () => {
    wireServer([
      [
        `/rest/DataExportStatus`,
        [
          {
            id: 4820,
            endpoint: "https://partner.example.org/fhir/",
            maxIntervalMinutes: 5,
            lastSuccess: "2026-05-12T07:23:27Z",
            lastAttempt: "2026-05-12T07:23:27Z",
            lastStatus: "FAILED",
            failedLast24h: 3,
            totalLast24h: 5,
          },
        ],
      ],
    ]);
    postToOpenElisServerFullResponseMock.mockImplementation(
      (url, payload, cb) => {
        cb({ ok: true });
      },
    );

    renderWithIntl(<DataExportStatus />);
    await screen.findByText("https://partner.example.org/fhir/");

    const fetchesBeforeClick = getFromOpenElisServerMock.mock.calls.filter(
      (c) => c[0] === "/rest/DataExportStatus",
    ).length;

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["dataexport.status.retry"],
      }),
    );

    expect(postToOpenElisServerFullResponseMock).toHaveBeenCalledWith(
      "/rest/DataExportStatus/4820/trigger",
      "",
      expect.any(Function),
    );

    await wait(() => {
      const fetchesAfterClick = getFromOpenElisServerMock.mock.calls.filter(
        (c) => c[0] === "/rest/DataExportStatus",
      ).length;
      expect(fetchesAfterClick).toBe(fetchesBeforeClick + 1);
    });
  });

  test("refresh button re-fetches the status list", async () => {
    wireServer([[`/rest/DataExportStatus`, []]]);

    renderWithIntl(<DataExportStatus />);
    await screen.findByText(messages["dataexport.status.empty"]);

    const before = getFromOpenElisServerMock.mock.calls.filter(
      (c) => c[0] === "/rest/DataExportStatus",
    ).length;

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["dataexport.status.refresh"],
      }),
    );

    await wait(() => {
      const after = getFromOpenElisServerMock.mock.calls.filter(
        (c) => c[0] === "/rest/DataExportStatus",
      ).length;
      expect(after).toBe(before + 1);
    });
  });
});
