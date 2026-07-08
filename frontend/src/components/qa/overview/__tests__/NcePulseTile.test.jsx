import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";
import NcePulseTile from "../NcePulseTile";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

const nce = (id, severity, status) => ({
  id,
  nceNumber: `NCE-${id}`,
  severity,
  status,
});

const mockNceList = (list) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/nce/dashboard")) {
      callback({ nceList: list });
    }
  });
};

let testLocation;
const renderTile = async () => {
  testLocation = undefined;
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter initialEntries={["/qa/overview"]}>
          <NcePulseTile />
          <Route
            path="*"
            render={({ location }) => {
              testLocation = location;
              return null;
            }}
          />
        </MemoryRouter>
      </IntlProvider>,
    ),
  );
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("NcePulseTile", () => {
  test("counts only CRITICAL + Pending events and shows corrective-action line (amber band)", async () => {
    mockNceList([
      nce("1", "CRITICAL", "Pending"),
      nce("2", "CRITICAL", "Pending"),
      nce("3", "CRITICAL", "Closed"),
      nce("4", "MAJOR", "Pending"),
      nce("5", "CRITICAL", "Corrective Action"),
      nce("6", "MINOR", "Corrective Action"),
    ]);
    await renderTile();

    const count = screen.getByText("2");
    expect(count).toHaveClass("qa-live-count", "qa-live-amber");
    expect(screen.getByText("critical pending")).toBeInTheDocument();
    expect(screen.getByText("2 in corrective action")).toBeInTheDocument();
  });

  test("zero critical pending renders green, five or more renders red", async () => {
    mockNceList([nce("1", "MAJOR", "Pending"), nce("2", "CRITICAL", "Closed")]);
    await renderTile();
    expect(screen.getByText("0")).toHaveClass("qa-live-green");

    mockNceList(
      ["1", "2", "3", "4", "5"].map((id) => nce(id, "CRITICAL", "Pending")),
    );
    await renderTile();
    expect(screen.getByText("5")).toHaveClass("qa-live-red");
  });

  test("shows an em dash without a color band when the endpoint returns no data", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => callback());
    await renderTile();

    const count = screen.getByText("—");
    expect(count).toHaveClass("qa-live-count");
    expect(count).not.toHaveClass("qa-live-green", "qa-live-amber");
  });

  test("click drills through to the NCE register pre-filtered to critical + pending", async () => {
    mockNceList([nce("1", "CRITICAL", "Pending")]);
    await renderTile();

    fireEvent.click(screen.getByText("NCE Pulse"));
    expect(testLocation.pathname).toBe("/NceDashboard");
    expect(testLocation.search).toBe("?severity=CRITICAL&status=Pending");
  });
});
