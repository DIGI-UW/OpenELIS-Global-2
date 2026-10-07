import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";

/**
 * OGC-1363 sections B and E: the Organizations list shows what the server
 * answers, searches and filters through the URL, explains a match on a former
 * name, guards a deactivation that would hide open orders, and offers Undo.
 */
const { api, notify } = vi.hoisted(() => ({
  api: {
    listOrganizations: vi.fn(),
    getOrganization: vi.fn(),
    createOrganization: vi.fn(),
    updateOrganization: vi.fn(),
    getUsage: vi.fn(),
    setActive: vi.fn(),
    listWards: vi.fn(),
    createWard: vi.fn(),
    updateWard: vi.fn(),
    moveWard: vi.fn(),
    getHistory: vi.fn(),
    getLists: vi.fn(),
    listIdentifierCollisions: vi.fn(() => Promise.resolve([])),
    getAreaLevels: vi.fn(),
    listAreas: vi.fn(),
    searchAreas: vi.fn(),
    createArea: vi.fn(),
    updateArea: vi.fn(),
    setAreaActive: vi.fn(),
    getJson: vi.fn(),
    pathText: (location) =>
      location && Array.isArray(location.path) ? location.path.join(" / ") : "",
  },
  notify: vi.fn(),
}));

vi.mock("../locationsApi", () => api);

import OrganizationsView from "../OrganizationsView";
import { LocationsContext } from "../LocationsPage";

const LISTS = {
  categories: [{ id: "c1", label: "Urban clinic" }],
  ownerships: [{ id: "o1", label: "Government" }],
  serviceTypes: [{ id: "OUTPATIENT", label: "Outpatient" }],
  siteTypes: [],
  environmentalZones: [],
  referralStatuses: [],
  identifierLabels: ["Code"],
  facilityTypes: [{ id: "5", name: "referring clinic", hierarchyLevel: null }],
  areaLevels: [],
};

const row = (overrides = {}) => ({
  id: "4",
  name: "Health Services Inc",
  shortName: "HSI",
  code: "HSI002",
  kind: "facility",
  types: [{ id: "5", name: "referring clinic", hierarchyLevel: null }],
  category: { id: "c1", label: "Urban clinic" },
  ownership: null,
  location: {
    id: "9101",
    name: "Lae",
    levelName: "District",
    path: ["Morobe Province", "Lae"],
  },
  wardCount: 2,
  siteType: null,
  inUse: { open: 3, total: 402 },
  active: true,
  registry: false,
  reviewOverdue: false,
  accreditationExpired: false,
  matchNote: null,
  ...overrides,
});

const renderView = (
  initialEntry = "/MasterListsPage/locations",
  view = "organizations",
) => {
  const go = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <LocationsContext.Provider
        value={{ lists: LISTS, reloadLists: vi.fn(), notify, go }}
      >
        <MemoryRouter initialEntries={[initialEntry]}>
          <Route path="/MasterListsPage/locations">
            <OrganizationsView view={view} lists={LISTS} />
          </Route>
        </MemoryRouter>
      </LocationsContext.Provider>
    </IntlProvider>,
  );
  return { go };
};

describe("OrganizationsView (OGC-1363)", () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset && fn.mockReset());
    notify.mockReset();
    api.listIdentifierCollisions.mockResolvedValue([]);
    api.searchAreas.mockResolvedValue([]);
    api.listOrganizations.mockResolvedValue({
      items: [row()],
      total: 1,
      page: 1,
      pageSize: 25,
    });
    api.setActive.mockResolvedValue({
      changed: ["4"],
      names: ["Health Services Inc"],
    });
  });

  it("lists the rows the server answers with code, location path, ward count and in-use counts", async () => {
    renderView();
    expect(await screen.findByText("Health Services Inc")).toBeInTheDocument();
    expect(screen.getByText("HSI002")).toBeInTheDocument();
    expect(screen.getByText("Lae")).toBeInTheDocument();
    expect(screen.getByText("Morobe Province")).toBeInTheDocument();
    expect(screen.getByText("Urban clinic")).toBeInTheDocument();
    expect(screen.getByText("402")).toBeInTheDocument();
    expect(api.listOrganizations).toHaveBeenCalledWith(
      expect.objectContaining({
        view: "organizations",
        status: "active",
        page: 1,
        pageSize: 25,
      }),
    );
  });

  it("reads its filters from the URL and shows them as named tags", async () => {
    renderView("/MasterListsPage/locations?status=inactive&type=5&q=hsi");
    expect(await screen.findByText("Health Services Inc")).toBeInTheDocument();
    expect(api.listOrganizations).toHaveBeenCalledWith(
      expect.objectContaining({ status: "inactive", type: ["5"], q: "hsi" }),
    );
    const tags = screen.getByTestId("locations-filter-tags");
    expect(tags).toHaveTextContent("referring clinic");
    expect(tags).toHaveTextContent("Inactive");
  });

  it("names a location filter that came from the URL even when no listed row sits in it", async () => {
    api.listOrganizations.mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      pageSize: 25,
    });
    api.getOrganization.mockResolvedValue({
      row: {
        id: "9100",
        name: "Morobe Province",
        kind: "area",
        types: [],
        location: null,
      },
      identifiers: [],
      wards: [],
    });
    renderView("/MasterListsPage/locations?location=9100");
    await waitFor(() =>
      expect(api.getOrganization).toHaveBeenCalledWith("9100"),
    );
    await waitFor(() =>
      expect(screen.getByTestId("locations-filter-tags")).toHaveTextContent(
        "Morobe Province",
      ),
    );
    expect(screen.getByTestId("locations-filter-tags")).not.toHaveTextContent(
      "9100",
    );
  });

  it("confirms an Undo once the record is active again", async () => {
    api.getUsage.mockResolvedValue({
      id: "4",
      inUse: { open: 0, total: 1 },
      activeChildren: [],
    });
    renderView();
    await screen.findByText("Health Services Inc");
    fireEvent.click(screen.getByLabelText("Active, Health Services Inc"));
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        "Health Services Inc deactivated.",
        "success",
        expect.objectContaining({ label: "Undo" }),
      ),
    );
    notify.mock.calls[0][2].run();
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith("Health Services Inc reactivated."),
    );
  });

  it("says when a row matched on a former name or a ward", async () => {
    api.listOrganizations.mockResolvedValue({
      items: [
        row({ matchNote: "formerly:Nine Mile Clinic" }),
        row({ id: "7", name: "Angau", matchNote: "ward:TB Clinic" }),
      ],
      total: 2,
      page: 1,
      pageSize: 25,
    });
    renderView("/MasterListsPage/locations?q=nine");
    expect(
      await screen.findByText("Formerly Nine Mile Clinic"),
    ).toBeInTheDocument();
    expect(screen.getByText("Ward / dept: TB Clinic")).toBeInTheDocument();
  });

  it("deactivates a record that is not in use straight away and offers Undo", async () => {
    api.getUsage.mockResolvedValue({
      id: "4",
      inUse: { open: 0, total: 1 },
      activeChildren: [],
    });
    renderView();
    await screen.findByText("Health Services Inc");
    fireEvent.click(screen.getByLabelText("Active, Health Services Inc"));
    await waitFor(() =>
      expect(api.setActive).toHaveBeenCalledWith(["4"], false, false),
    );
    expect(notify).toHaveBeenCalledWith(
      "Health Services Inc deactivated.",
      "success",
      expect.objectContaining({ label: "Undo" }),
    );
    notify.mock.calls[0][2].run();
    await waitFor(() =>
      expect(api.setActive).toHaveBeenCalledWith(["4"], true),
    );
  });

  it("guards a deactivation that would hide open orders or active wards", async () => {
    api.getUsage.mockResolvedValue({
      id: "4",
      name: "Health Services Inc",
      kind: "facility",
      inUse: { open: 3, total: 402 },
      activeChildren: [
        {
          id: "9102",
          name: "Outpatient Department",
          kind: "ward",
          inUse: { open: 2, total: 20 },
        },
      ],
    });
    renderView();
    await screen.findByText("Health Services Inc");
    fireEvent.click(screen.getByLabelText("Active, Health Services Inc"));
    expect(
      await screen.findByText(/has 3 open orders and 1 active wards/),
    ).toBeInTheDocument();
    expect(api.setActive).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: /and its 1 wards/ }));
    await waitFor(() =>
      expect(api.setActive).toHaveBeenCalledWith(["4"], false, true),
    );
  });

  it("shows the empty state with a link to matches in the other view", async () => {
    api.listOrganizations.mockImplementation((params) =>
      Promise.resolve(
        params.view === "sites"
          ? { items: [], total: 2, page: 1, pageSize: 1 }
          : { items: [], total: 0, page: 1, pageSize: 25 },
      ),
    );
    const { go } = renderView("/MasterListsPage/locations?q=trap");
    expect(
      await screen.findByText("No records match these filters."),
    ).toBeInTheDocument();
    const link = await screen.findByText("In Sampling Sites: 2 matches");
    fireEvent.click(link);
    expect(go).toHaveBeenCalledWith("sites", "?q=trap");
  });

  it("opens the add form from the toolbar and the edit form from a row", async () => {
    api.getOrganization.mockResolvedValue({
      row: row(),
      parentId: "9101",
      identifiers: [{ id: 1, label: "Code", value: "HSI002", reporting: true }],
      wards: [],
      referral: null,
      site: null,
      lastupdated: 1,
      historyCount: 0,
    });
    renderView();
    await screen.findByText("Health Services Inc");
    fireEvent.click(screen.getByTestId("locations-add"));
    expect(await screen.findByTestId("locations-form-new")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("locations-edit-4"));
    expect(await screen.findByTestId("locations-form-4")).toBeInTheDocument();
    await waitFor(() => expect(api.getOrganization).toHaveBeenCalledWith("4"));
  });
  const detailOf = (overrides = {}) => ({
    row: row(overrides),
    parentId: "9101",
    identifiers: [{ id: 1, label: "Code", value: "HSI002", reporting: true }],
    wards: [{ id: "77", name: "Maternity Ward", code: "", active: true }],
    contactName: "Peter",
    referral: null,
    site: null,
    lastupdated: 1,
    historyCount: 0,
  });

  it("asks before closing a form with unsaved changes (OGC-1420 5c)", async () => {
    api.getOrganization.mockResolvedValue(detailOf());
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    renderView("/MasterListsPage/locations?id=4");
    expect(await screen.findByDisplayValue("Peter")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Contact name"), {
      target: { value: "Grace" },
    });
    fireEvent.click(screen.getByTestId("locations-edit-4"));

    expect(confirm).toHaveBeenCalled();
    expect(screen.getByLabelText("Contact name")).toHaveValue("Grace");
    confirm.mockRestore();
  });

  it("opens a record named in the address even when it is not on this page (OGC-1420 7a)", async () => {
    api.getOrganization.mockResolvedValue(
      detailOf({ id: "88", name: "Page Two Clinic" }),
    );
    renderView("/MasterListsPage/locations?id=88");
    expect(
      await screen.findByTestId("locations-pinned-form"),
    ).toBeInTheDocument();
    expect(await screen.findByTestId("locations-form-88")).toBeInTheDocument();
    await waitFor(() => expect(api.getOrganization).toHaveBeenCalledWith("88"));
  });

  it("opens the organization a ward-name search found, with the ward highlighted (OGC-1420 7c)", async () => {
    api.listOrganizations.mockResolvedValue({
      items: [row({ matchNote: "ward:Maternity Ward" })],
      total: 1,
      page: 1,
      pageSize: 25,
    });
    api.getOrganization.mockResolvedValue(detailOf());
    renderView("/MasterListsPage/locations?q=Maternity");
    expect(await screen.findByTestId("locations-form-4")).toBeInTheDocument();
    expect(await screen.findByTestId("locations-ward-77")).toHaveClass(
      "lo-highlight",
    );
  });

  it("lists identifiers two records share so an admin can resolve them (OGC-1420 8)", async () => {
    api.listIdentifierCollisions.mockResolvedValue([
      {
        label: "Code",
        value: "QADUP1",
        records: [
          { id: "4", name: "Health Services Inc", kind: "facility" },
          { id: "9", name: "QA Referring Clinic", kind: "facility" },
        ],
      },
    ]);
    renderView();
    const list = await screen.findByTestId("locations-identifier-collisions");
    expect(list).toHaveTextContent(
      "Code QADUP1: Health Services Inc, QA Referring Clinic",
    );
    expect(
      list.closest(".cds--inline-notification"),
      "Carbon notifications may not hold the record links",
    ).toBeNull();
  });
});
