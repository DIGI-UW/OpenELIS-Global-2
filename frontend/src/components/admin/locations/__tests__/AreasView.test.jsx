import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";

/**
 * OGC-1363 FR-B7: the geographic areas tree loads children on expand, adds a
 * sub-area under its parent with the level below named on the button, shows
 * search matches with their ancestors, and blocks deactivating a parent that
 * still holds active areas.
 */
const { api, notify } = vi.hoisted(() => ({
  api: {
    listAreas: vi.fn(),
    searchAreas: vi.fn(),
    createArea: vi.fn(),
    updateArea: vi.fn(),
    setAreaActive: vi.fn(),
    getHistory: vi.fn(),
    getJson: vi.fn(),
    pathText: () => "",
  },
  notify: vi.fn(),
}));

vi.mock("../locationsApi", () => api);

import AreasView from "../AreasView";
import { LocationsContext } from "../LocationsPage";

const LISTS = {
  areaLevels: [
    { level: 1, name: "Province", typeId: "913" },
    { level: 2, name: "District", typeId: "914" },
  ],
};

const MOROBE = {
  id: "9100",
  name: "Morobe Province",
  code: "P-MOR",
  level: 1,
  levelName: "Province",
  parentId: null,
  active: true,
  childCount: 1,
  matched: false,
  lastupdated: 1,
};
const LAE = {
  id: "9101",
  name: "Lae",
  code: "MOR-LAE",
  level: 2,
  levelName: "District",
  parentId: "9100",
  active: true,
  childCount: 0,
  matched: false,
  lastupdated: 1,
};

const wrap = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LocationsContext.Provider
        value={{ lists: LISTS, reloadLists: vi.fn(), notify, go: vi.fn() }}
      >
        <MemoryRouter>
          <AreasView lists={LISTS} />
        </MemoryRouter>
      </LocationsContext.Provider>
    </IntlProvider>,
  );

describe("AreasView (OGC-1363)", () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset && fn.mockReset());
    notify.mockReset();
    api.listAreas.mockImplementation((parentId) =>
      Promise.resolve(parentId ? [LAE] : [MOROBE]),
    );
  });

  it("shows the top level expanded with its children loaded", async () => {
    wrap();
    expect(await screen.findByText("Morobe Province")).toBeInTheDocument();
    expect(await screen.findByText("Lae")).toBeInTheDocument();
    expect(api.listAreas).toHaveBeenCalledWith("", "active");
    expect(api.listAreas).toHaveBeenCalledWith("9100", "active");
    expect(screen.getByTestId("locations-area-9101")).toHaveAttribute(
      "aria-level",
      "2",
    );
  });

  it("adds a district under a province with the level below named on the button", async () => {
    api.createArea.mockResolvedValue({ ...LAE, id: "9105", name: "Huon Gulf" });
    wrap();
    await screen.findByText("Lae");
    fireEvent.click(screen.getByTestId("locations-area-add-9100"));
    expect(screen.getByTestId("locations-area-add-9100")).toHaveTextContent(
      "District",
    );
    fireEvent.change(screen.getByLabelText(/New District · Morobe Province/), {
      target: { value: "Huon Gulf" },
    });
    fireEvent.click(screen.getByTestId("locations-area-save"));
    await waitFor(() =>
      expect(api.createArea).toHaveBeenCalledWith(
        expect.objectContaining({ name: "Huon Gulf", parentId: "9100" }),
      ),
    );
    expect(notify).toHaveBeenCalledWith("Huon Gulf added.");
  });

  it("shows a search match with the areas it sits in", async () => {
    api.searchAreas.mockResolvedValue([MOROBE, { ...LAE, matched: true }]);
    wrap();
    await screen.findByText("Lae");
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "lae" },
    });
    await waitFor(() =>
      expect(api.searchAreas).toHaveBeenCalledWith("lae", "active"),
    );
    expect(
      await screen.findByText("1 matches, shown with the areas they sit in"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("locations-area-9101")).toHaveClass(
      "lo-highlight",
    );
  });

  it("reports the server's refusal when a parent with active areas is deactivated", async () => {
    api.setAreaActive.mockRejectedValue(
      Object.assign(
        new Error("Morobe Province still has 1 active areas inside it."),
        { status: 409 },
      ),
    );
    wrap();
    await screen.findByText("Lae");
    fireEvent.click(screen.getByLabelText("Active, Morobe Province"));
    await waitFor(() =>
      expect(api.setAreaActive).toHaveBeenCalledWith("9100", false),
    );
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        "Morobe Province still has 1 active areas inside it.",
        "error",
      ),
    );
  });
});
