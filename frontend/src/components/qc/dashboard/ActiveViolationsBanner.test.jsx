import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import ActiveViolationsBanner from "./ActiveViolationsBanner";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

const violation = (id, severity, dateTime) => ({
  id,
  severity,
  ruleCode: `rule-${id}`,
  violationDateTime: dateTime,
  instrumentName: `Instrument ${id}`,
  testName: `Test ${id}`,
  resolutionStatus: "UNRESOLVED",
});

const renderBanner = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <ActiveViolationsBanner />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("ActiveViolationsBanner", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders nothing when there are no unresolved violations", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => callback([]));

    const { container } = renderBanner();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/qc/violations?unresolved=true",
      expect.any(Function),
    );
    expect(container.firstChild).toBeNull();
  });

  test("shows top 5 violations, REJECTION first then newest, with view-all link", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback([
        violation("w1", "WARNING", "2026-07-13T10:00:00"),
        violation("r1", "REJECTION", "2026-07-13T08:00:00"),
        violation("w2", "WARNING", "2026-07-13T12:00:00"),
        violation("r2", "REJECTION", "2026-07-13T11:00:00"),
        violation("w3", "WARNING", "2026-07-13T09:00:00"),
        violation("r3", "REJECTION", "2026-07-13T07:00:00"),
      ]),
    );

    renderBanner();

    expect(
      screen.getByText("6 active QC violation(s) require attention"),
    ).toBeInTheDocument();

    const rows = screen
      .getAllByTestId(/^banner-violation-/)
      .map((el) => el.getAttribute("data-testid"));
    expect(rows).toEqual([
      "banner-violation-r2",
      "banner-violation-r1",
      "banner-violation-r3",
      "banner-violation-w2",
      "banner-violation-w1",
    ]);
    expect(screen.queryByTestId("banner-violation-w3")).not.toBeInTheDocument();

    expect(
      screen.getByTestId("active-violations-banner-view-all"),
    ).toBeInTheDocument();
    expect(screen.getByText("rule-r2")).toBeInTheDocument();
    expect(screen.getByTestId("banner-violation-r2")).toHaveTextContent(
      "Instrument r2",
    );
    expect(screen.getByTestId("banner-violation-r2")).toHaveTextContent(
      "Test r2",
    );
  });

  test("acknowledge posts to the endpoint and refetches the list", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback([violation("r1", "REJECTION", "2026-07-13T08:00:00")]),
    );
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => callback({ ok: true }),
    );

    renderBanner();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByTestId("banner-acknowledge-r1"));

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/qc/violations/r1/acknowledge",
      JSON.stringify({}),
      expect.any(Function),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(2);
  });

  test("shows error message when acknowledge fails", () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback([violation("r1", "REJECTION", "2026-07-13T08:00:00")]),
    );
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => callback({ ok: false }),
    );

    renderBanner();
    fireEvent.click(screen.getByTestId("banner-acknowledge-r1"));

    expect(
      screen.getByText("Failed to acknowledge violation"),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
  });
});
