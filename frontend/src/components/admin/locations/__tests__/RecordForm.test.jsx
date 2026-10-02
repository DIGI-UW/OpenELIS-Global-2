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
    api.listIdentifierCollisions.mockResolvedValue([]);
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
    expect(notify).toHaveBeenCalledWith(
      "The record was not saved: Code HSI002 is already used by Other Clinic",
      "error",
    );
  });

  it("keeps what this user typed on a stale save and fills in the other admin's changes (FR-C5, OGC-1420 5d)", async () => {
    api.getOrganization.mockResolvedValue(detail());
    api.updateOrganization.mockRejectedValueOnce(
      Object.assign(new Error("Another admin saved this record first"), {
        status: 409,
        current: detail({ phone: "+675 999", lastupdated: 2000 }),
      }),
    );
    api.updateOrganization.mockResolvedValueOnce({
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
    await screen.findByDisplayValue("Health Services Inc");
    fireEvent.change(screen.getByLabelText("Contact name"), {
      target: { value: "Mine" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    expect(
      await screen.findByText(/Another admin saved this record first/),
    ).toBeInTheDocument();
    expect(screen.getByText(/Your changes are kept/)).toHaveTextContent(
      "Phone",
    );
    expect(screen.getByLabelText("Contact name")).toHaveValue("Mine");
    expect(screen.getByLabelText("Phone")).toHaveValue("+675 999");

    fireEvent.click(screen.getByTestId("locations-save"));
    await waitFor(() =>
      expect(api.updateOrganization).toHaveBeenCalledTimes(2),
    );
    const retry = api.updateOrganization.mock.calls[1][1];
    expect(retry.lastupdated).toBe(2000);
    expect(retry.contactName).toBe("Mine");
    expect(retry.phone).toBe("+675 999");
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
  it("says why a save is blocked and takes the user to the field (OGC-1420 5a)", async () => {
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
    fireEvent.click(screen.getByTestId("locations-save"));

    expect(notify).toHaveBeenCalledWith(
      "Not saved yet. Check: Approval status.",
      "error",
    );
    expect(document.getElementById("ref-status-4")).toHaveFocus();
    expect(api.updateOrganization).not.toHaveBeenCalled();
  });

  it("saves a new record without a code when the pre-filled Code row is left blank (OGC-1420 5b)", async () => {
    api.createOrganization.mockResolvedValue({
      detail: detail(),
      warnings: [],
    });
    wrap(
      <RecordForm
        isNew
        kind="site"
        lists={LISTS}
        onCancel={vi.fn()}
        onSaved={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText(/^Name/), {
      target: { value: "No code trap" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));
    await waitFor(() => expect(api.createOrganization).toHaveBeenCalled());
    expect(api.createOrganization.mock.calls[0][0].identifiers).toEqual([]);
  });

  it("does not drop wards typed but not saved when the record is saved (OGC-1420 5e)", async () => {
    api.getOrganization.mockResolvedValue(detail());
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
    fireEvent.click(screen.getByTestId("locations-add-ward"));
    fireEvent.change(document.getElementById("new-ward-name-0"), {
      target: { value: "Maternity" },
    });
    fireEvent.click(screen.getByTestId("locations-save"));

    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        "1 ward / dept is typed but not saved yet. Save them with Save wards, or remove them, then save the record.",
        "warning",
      ),
    );
    expect(api.updateOrganization).not.toHaveBeenCalled();
    expect(document.getElementById("new-ward-service-0")).toHaveFocus();
  });

  it("caps the free-text fields at their column length and names the chosen types (OGC-1420 6a, 7d)", async () => {
    api.getOrganization.mockResolvedValue(detail());
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
    expect(screen.getByLabelText("Contact name")).toHaveAttribute(
      "maxlength",
      "100",
    );
    expect(document.getElementById("description-4")).toHaveAttribute(
      "maxlength",
      "1000",
    );
    expect(document.getElementById("state-4")).toHaveAttribute(
      "maxlength",
      "100",
    );
    expect(screen.getByTestId("locations-types-chosen-4")).toHaveTextContent(
      "referring clinic",
    );
  });
});
