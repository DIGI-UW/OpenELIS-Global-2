import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";

/**
 * OGC-1363 sections C and I: the inline form validates on the field, posts the
 * identifiers with one reporting code, shows the server's field errors, and
 * on a stale save shows the other admin's values instead of overwriting them.
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

import RecordForm from "../RecordForm";
import { LocationsContext } from "../LocationsPage";

const LISTS = {
  categories: [{ id: "c1", label: "Urban clinic" }],
  ownerships: [{ id: "o1", label: "Government" }],
  serviceTypes: [{ id: "OUTPATIENT", label: "Outpatient" }],
  siteTypes: [{ id: "Vector trap", label: "Vector trap" }],
  environmentalZones: [],
  referralStatuses: [{ id: "Approved", label: "Approved" }],
  identifierLabels: ["Code", "DHIS2 ID"],
  facilityTypes: [
    { id: "5", name: "referring clinic", hierarchyLevel: null },
    { id: "6", name: "referralLab", hierarchyLevel: null },
  ],
  areaLevels: [],
};

const detail = (overrides = {}) => ({
  row: {
    id: "4",
    name: "Health Services Inc",
    shortName: "HSI",
    code: "HSI002",
    kind: "facility",
    types: [{ id: "5", name: "referring clinic", hierarchyLevel: null }],
    category: null,
    ownership: null,
    location: {
      id: "9101",
      name: "Lae",
      levelName: "District",
      path: ["Morobe Province", "Lae"],
    },
    wardCount: 0,
    inUse: { open: 0, total: 0 },
    active: true,
    registry: false,
    reviewOverdue: false,
    accreditationExpired: false,
    matchNote: null,
  },
  parentId: "9101",
  streetAddress: "",
  city: "",
  state: "",
  zipCode: "",
  phone: "",
  fax: "",
  email: "",
  internetAddress: "",
  contactName: "Peter",
  description: "",
  gpsLatitude: null,
  gpsLongitude: null,
  serviceType: null,
  source: "LOCAL",
  identifiers: [{ id: 1, label: "Code", value: "HSI002", reporting: true }],
  wards: [],
  referral: null,
  site: null,
  lastupdated: 1000,
  historyCount: 2,
  ...overrides,
});

const wrap = (node) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LocationsContext.Provider
        value={{ lists: LISTS, reloadLists: vi.fn(), notify, go: vi.fn() }}
      >
        <MemoryRouter>{node}</MemoryRouter>
      </LocationsContext.Provider>
    </IntlProvider>,
  );

describe("RecordForm (OGC-1363)", () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset && fn.mockReset());
    notify.mockReset();
    api.searchAreas.mockResolvedValue([]);
  });

  it("refuses a new facility without a name or a type before calling the server", () => {
    const onSaved = vi.fn();
    wrap(
      <RecordForm
        isNew
        kind="facility"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={onSaved}
      />,
    );
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(screen.getByText("Name is required")).toBeInTheDocument();
    expect(
      screen.getByText("Choose at least one organization type"),
    ).toBeInTheDocument();
    expect(api.createOrganization).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("refuses GPS outside decimal degrees inline", () => {
    wrap(
      <RecordForm
        isNew
        kind="site"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText("GPS latitude"), {
      target: { value: "120" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(
      screen.getByText(
        "Enter decimal degrees: latitude -90 to 90, longitude -180 to 180",
      ),
    ).toBeInTheDocument();
    expect(api.createOrganization).not.toHaveBeenCalled();
  });

  it("posts a site with its reporting code and site details", async () => {
    api.createOrganization.mockResolvedValue({
      detail: detail({ row: { ...detail().row, kind: "site" } }),
      warnings: [],
    });
    const onSaved = vi.fn();
    wrap(
      <RecordForm
        isNew
        kind="site"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={onSaved}
      />,
    );
    fireEvent.change(screen.getByLabelText(/^Name/), {
      target: { value: "Bumbu light trap" },
    });
    fireEvent.change(screen.getByLabelText("Code Value"), {
      target: { value: "VT-LAE-03" },
    });
    fireEvent.change(screen.getByLabelText("Site type"), {
      target: { value: "Vector trap" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    await waitFor(() => expect(api.createOrganization).toHaveBeenCalled());
    const body = api.createOrganization.mock.calls[0][0];
    expect(body.kind).toBe("site");
    expect(body.name).toBe("Bumbu light trap");
    expect(body.identifiers).toEqual([
      { label: "Code", value: "VT-LAE-03", reporting: true },
    ]);
    expect(body.site.siteType).toBe("Vector trap");
    await waitFor(() => expect(onSaved).toHaveBeenCalled());
  });

  it("loads an existing record into the form and sends its version token on save", async () => {
    api.getOrganization.mockResolvedValue(detail());
    api.updateOrganization.mockResolvedValue({
      detail: detail(),
      warnings: [],
    });
    wrap(
      <RecordForm
        id="4"
        kind="facility"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    expect(
      await screen.findByDisplayValue("Health Services Inc"),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("HSI002")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Peter")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Contact name"), {
      target: { value: "Grace" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    await waitFor(() => expect(api.updateOrganization).toHaveBeenCalled());
    const [id, body] = api.updateOrganization.mock.calls[0];
    expect(id).toBe("4");
    expect(body.lastupdated).toBe(1000);
    expect(body.contactName).toBe("Grace");
    expect(body.parentId).toBe("9101");
  });

  it("shows the server's field errors on a refused save", async () => {
    api.getOrganization.mockResolvedValue(detail());
    api.updateOrganization.mockRejectedValue(
      Object.assign(new Error("The record was not saved"), {
        status: 422,
        fieldErrors: {
          identifiers: "Code HSI002 is already used by Other Clinic",
        },
      }),
    );
    wrap(
      <RecordForm
        id="4"
        kind="facility"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    await screen.findByDisplayValue("Health Services Inc");
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(
      await screen.findByText("Code HSI002 is already used by Other Clinic"),
    ).toBeInTheDocument();
    expect(notify).toHaveBeenCalledWith("The record was not saved.", "error");
  });

  it("shows the other admin's values on a stale save instead of overwriting them", async () => {
    api.getOrganization.mockResolvedValue(detail());
    api.updateOrganization.mockRejectedValue(
      Object.assign(new Error("Another admin saved this record first"), {
        status: 409,
        current: detail({ contactName: "Someone else", lastupdated: 2000 }),
      }),
    );
    wrap(
      <RecordForm
        id="4"
        kind="facility"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    await screen.findByDisplayValue("Health Services Inc");
    fireEvent.change(screen.getByLabelText("Contact name"), {
      target: { value: "Mine" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(
      await screen.findByText(/Another admin saved this record first/),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("Someone else")).toBeInTheDocument();
    expect(screen.queryByDisplayValue("Mine")).not.toBeInTheDocument();
  });

  it("asks for an approval status once a referral-lab type is chosen", async () => {
    api.getOrganization.mockResolvedValue(
      detail({
        row: {
          ...detail().row,
          types: [{ id: "6", name: "referralLab", hierarchyLevel: null }],
        },
      }),
    );
    wrap(
      <RecordForm
        id="4"
        kind="facility"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    await screen.findByDisplayValue("Health Services Inc");
    expect(screen.getAllByText(/Referral laboratory/).length).toBeGreaterThan(
      0,
    );
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(
      await screen.findByText("Choose an approval status"),
    ).toBeInTheDocument();
    expect(api.updateOrganization).not.toHaveBeenCalled();
  });
});
