import React from "react";
import { act, cleanup, fireEvent, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { Route } from "react-router-dom";
import NcePulseTile from "../NcePulseTile";
import { renderQa } from "../../testUtils";
import { fetchFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    fetchFromOpenElisServer: vi.fn(),
  };
});

const nce = (id, severity, status) => ({
  id,
  nceNumber: `NCE-${id}`,
  severity,
  status,
});

const mockNceList = (list) => {
  fetchFromOpenElisServer.mockImplementation((url) =>
    url.includes("/rest/nce/dashboard")
      ? Promise.resolve({ nceList: list })
      : Promise.resolve({ enabled: true }),
  );
};

let testLocation;
let testHistory;
const renderTile = async () => {
  cleanup(); // a test that renders twice compares tiles, not documents
  testLocation = undefined;
  await act(async () => {
    renderQa(
      <>
        <NcePulseTile />
        <Route
          path="*"
          render={({ location, history }) => {
            testLocation = location;
            testHistory = history;
            return null;
          }}
        />
      </>,
      { entries: ["/qa/overview"] },
    );
  });
  // The tile captions itself only once the register read has settled.
  await waitFor(() =>
    expect(screen.getAllByText("critical pending").length).toBeGreaterThan(0),
  );
};

const tile = () => screen.getByTestId("qa-overview-tile-nce");

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
      nce("5", "CRITICAL", "CAPA"),
      nce("6", "MINOR", "CAPA"),
    ]);
    await renderTile();

    expect(tile()).toHaveTextContent("2");
    expect(tile().className).toContain("qi-tile--amber");
    expect(screen.getByText("critical pending")).toBeInTheDocument();
    expect(screen.getByText("2 in corrective action")).toBeInTheDocument();
  });

  test("zero critical pending renders green, five or more renders red", async () => {
    mockNceList([nce("1", "MAJOR", "Pending"), nce("2", "CRITICAL", "Closed")]);
    await renderTile();
    expect(tile()).toHaveTextContent("0");
    expect(tile().className).toContain("qi-tile--green");

    mockNceList(
      ["1", "2", "3", "4", "5"].map((id) => nce(id, "CRITICAL", "Pending")),
    );
    await renderTile();
    expect(tile()).toHaveTextContent("5");
    expect(tile().className).toContain("qi-tile--red");
  });

  test("shows an em dash without a color band when the register is unavailable", async () => {
    fetchFromOpenElisServer.mockImplementation((url) =>
      url.includes("/rest/nce/dashboard")
        ? Promise.reject(new Error("unavailable"))
        : Promise.resolve({ enabled: true }),
    );
    await renderTile();

    expect(tile()).toHaveTextContent("—");
    expect(tile().className).toContain("qi-tile--blue");
  });

  test("click drills through to the NCE register pre-filtered to critical + pending", async () => {
    mockNceList([nce("1", "CRITICAL", "Pending")]);
    await renderTile();

    fireEvent.click(screen.getByText("NCE Pulse"));
    expect(testLocation.pathname).toBe("/NceDashboard");
    expect(testLocation.search).toBe("?severity=CRITICAL&status=Pending");
  });

  test("the whole tile navigates once, and the detail link is its keyboard route", async () => {
    mockNceList([nce("1", "CRITICAL", "Pending")]);
    await renderTile();

    const link = screen.getByRole("link", { name: /View detail/ });
    expect(link).toHaveAttribute(
      "href",
      "/NceDashboard?severity=CRITICAL&status=Pending",
    );
    // Following the link must not also fire the tile's own navigation: one
    // click, one history entry.
    const before = testHistory.length;
    fireEvent.click(link);
    expect(testLocation.pathname).toBe("/NceDashboard");
    expect(testHistory.length).toBe(before + 1);
  });
});
