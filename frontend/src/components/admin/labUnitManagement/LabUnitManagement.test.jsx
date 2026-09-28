import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import LabUnitManagement from "./LabUnitManagement";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Serve the endpoints the screen depends on so the test exercises the real
// data flow: active locales (multi-language names), domains, and the list.
vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((endpoint, callback) => {
      if (endpoint === "/rest/supportedlocales/active") {
        callback([
          {
            id: "1",
            localeCode: "en",
            displayName: "English",
            active: true,
            fallback: true,
            sortOrder: 1,
          },
          {
            id: "2",
            localeCode: "id",
            displayName: "Indonesian",
            active: true,
            fallback: false,
            sortOrder: 2,
          },
        ]);
      } else if (endpoint === "/rest/domains") {
        callback([
          { id: "CLINICAL", labelKey: "label.domain.CLINICAL" },
          { id: "ENVIRONMENTAL", labelKey: "label.domain.ENVIRONMENTAL" },
          { id: "VECTOR", labelKey: "label.domain.VECTOR" },
        ]);
      } else if (endpoint === "/rest/lab-units-management") {
        callback({
          success: true,
          data: [
            {
              id: "10",
              name: "Hematology",
              names: { en: "Hematology", id: "Hematologi" },
              description: "Blood analysis unit",
              domain: "CLINICAL",
              isActive: true,
              sortOrder: 1,
              testCount: 4,
            },
          ],
        });
      } else if (endpoint.includes("/deactivation-impact")) {
        callback({
          success: true,
          data: {
            testCount: 4,
            activeTestCount: 3,
            pendingAnalysisCount: 2,
            historicalAnalysisCount: 17,
            recommendedOption: "deactivate_all",
          },
        });
      } else {
        callback(undefined);
      }
    }),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

beforeEach(() => {
  postToOpenElisServerJsonResponse.mockClear();
});

const mockIntl = {
  formatMessage: ({ id, defaultMessage }) =>
    messages[id] || defaultMessage || id,
};

// The editor is URL-driven: /MasterListsPage/LabUnitManagement/:labUnitId?/:section?
// Wrap in a matching Route so useParams() sees the current segment when the
// user navigates from the list to the editor.
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={["/MasterListsPage/LabUnitManagement"]}>
      <IntlProvider locale="en" messages={messages}>
        <Route
          path="/MasterListsPage/LabUnitManagement/:labUnitId?/:section?"
          render={() => <LabUnitManagement intl={mockIntl} />}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("LabUnitManagement", () => {
  test("renders lab unit list with domain tag and test count", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", { name: "Lab Unit Management" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Add Lab Unit")).toBeInTheDocument();
    expect(screen.getByText("Hematology")).toBeInTheDocument();
  });

  test("add form renders one name input per active locale", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Lab Unit");
    fireEvent.click(addButton);

    expect(screen.getByText("Add New Lab Unit")).toBeInTheDocument();
    // One input per active language from the localization mechanism —
    // not hard-coded English/French.
    expect(screen.getByLabelText(/Name \(English\)/)).toBeInTheDocument();
    expect(screen.getByLabelText("Name (Indonesian)")).toBeInTheDocument();
    // Domain radio group is required (OGC-361 CFG-1).
    expect(screen.getByText("Domain")).toBeInTheDocument();
  });

  test("can navigate back to list from add form", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Lab Unit");
    fireEvent.click(addButton);

    const backButton = screen.getByText("← Back to List");
    fireEvent.click(backButton);

    expect(
      screen.getByRole("heading", { name: "Lab Unit Management" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Add Lab Unit")).toBeInTheDocument();
  });

  // OGC-189 (QA LU-W-3): the 20-character cap was gated behind `view === "add"`,
  // so a 32-character name entered in the editor saved and persisted.
  test("enforces the 20-character name cap when editing, not just adding", async () => {
    renderPage();

    fireEvent.click(await screen.findByText("Edit"));

    const nameInput = await screen.findByLabelText(/Name \(English\)/);
    fireEvent.change(nameInput, { target: { value: "a".repeat(32) } });
    fireEvent.click(screen.getByText("Save"));

    expect(
      await screen.findByText("Name must be 20 characters or less"),
    ).toBeInTheDocument();
    // Rejected client-side, so no update request is issued.
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("accepts a name of exactly 20 characters when editing", async () => {
    renderPage();

    fireEvent.click(await screen.findByText("Edit"));

    const nameInput = await screen.findByLabelText(/Name \(English\)/);
    fireEvent.change(nameInput, { target: { value: "a".repeat(20) } });
    fireEvent.click(screen.getByText("Save"));

    // Boundary is inclusive — without this the fix could pass by rejecting
    // every rename.
    expect(
      screen.queryByText("Name must be 20 characters or less"),
    ).not.toBeInTheDocument();
  });

  test("returning to the list refetches it (counts/order changed in editor)", async () => {
    renderPage();

    await screen.findByText("Add Lab Unit");
    const listCalls = () =>
      getFromOpenElisServer.mock.calls.filter(
        ([endpoint]) => endpoint === "/rest/lab-units-management",
      ).length;
    const callsAfterLoad = listCalls();

    // Editor sections (Assigned Tests, Display Order) mutate counts and
    // ordering server-side, so coming back to the list must re-read it.
    fireEvent.click(screen.getByText("Add Lab Unit"));
    fireEvent.click(screen.getByText("← Back to List"));

    expect(listCalls()).toBeGreaterThan(callsAfterLoad);
  });
});

/**
 * OGC-189 (M3) — guarded deactivation.
 *
 * The Active toggle used to save silently with tests still attached: no impact
 * summary, no options, no confirmation (QA LU-W-10). Switching a unit off now
 * opens the guarded flow instead.
 */
describe("LabUnitManagement deactivation flow (OGC-189 M3)", () => {
  const openFlow = async () => {
    renderPage();
    fireEvent.click(await screen.findByText("Edit"));
    // Carbon renders the Toggle as a button with role="switch"; clicking the
    // label text does not fire onToggle.
    const toggle = await screen.findByRole("switch");
    fireEvent.click(toggle);
  };

  test("switching a lab unit off opens the impact summary instead of saving", async () => {
    await openFlow();

    expect(
      await screen.findByText("This lab unit currently holds:"),
    ).toBeInTheDocument();
    // The counts come from the server, not from a cached list.
    expect(screen.getByText("4 assigned tests (3 active)")).toBeInTheDocument();
    expect(screen.getByText("2 pending analyses")).toBeInTheDocument();
    // Nothing is written until the flow is confirmed.
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("does not warn about reflex targets", async () => {
    await openFlow();
    await screen.findByText("This lab unit currently holds:");

    // Removed 2026-09-07. The warning named the reflex TARGET tests assigned to
    // this unit, but the reflex gate gates on the unit the work LANDS in (the
    // parent's, T159) — so it warned about reflexes that keep firing and stayed
    // silent on the ones that break.
    expect(
      screen.queryByText("Reflex rules will stop firing"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/reflex or calculation targets/),
    ).not.toBeInTheDocument();
  });

  test("all three options are offered", async () => {
    await openFlow();
    await screen.findByText("This lab unit currently holds:");

    // D6 — all three ship, including "keep".
    expect(screen.getByLabelText(/Keep assignments/)).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Deactivate all assigned tests/),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Reassign the tests/)).toBeInTheDocument();
    // The server's recommendedOption drives the preselection. With the
    // reflex-risk input removed it is "deactivate_all" for a unit holding
    // tests, so assert on what the server actually sent rather than pinning a
    // specific radio here.
    expect(
      screen.getByLabelText(/Deactivate all assigned tests/),
    ).toBeChecked();
  });

  test("confirmation is required before the unit can be deactivated", async () => {
    await openFlow();
    await screen.findByText("This lab unit currently holds:");

    // Carbon's danger Button renders its kind into textContent
    // ("dangerDeactivate lab unit"), so match on the label substring.
    const submit = screen.getByRole("button", {
      name: /Deactivate lab unit$/,
    });
    expect(submit).toBeDisabled();

    // Even the right option is not enough on its own.
    fireEvent.click(screen.getByLabelText(/Keep assignments/));
    expect(submit).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Type DEACTIVATE to confirm"), {
      target: { value: "deactivate" },
    });
    expect(submit).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Type DEACTIVATE to confirm"), {
      target: { value: "DEACTIVATE" },
    });
    expect(submit).toBeEnabled();
  });

  test("pending analyses are called out as staying on the worklists", async () => {
    await openFlow();
    await screen.findByText("This lab unit currently holds:");

    // M2 guarantees the unit stays reachable until its work finishes; say so
    // rather than letting the user assume it disappears.
    expect(
      screen.getByText(
        "This lab unit stays on the worklists until its 2 pending analyses are completed.",
      ),
    ).toBeInTheDocument();
  });
});
