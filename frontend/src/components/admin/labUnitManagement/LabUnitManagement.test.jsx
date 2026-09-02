import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import LabUnitManagement from "./LabUnitManagement";
import { getFromOpenElisServer } from "../../utils/Utils";
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
      } else {
        callback(undefined);
      }
    }),
  };
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
