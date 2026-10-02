import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * OGC-1420 (5f), FR-D2: new wards are saved one at a time. When one fails, the
 * user is told which, and Save again sends only the wards still waiting, so
 * the one that saved is never created twice.
 */
const { api, notify } = vi.hoisted(() => ({
  api: {
    createWard: vi.fn(),
    listOrganizations: vi.fn(),
    listWards: vi.fn(),
    moveWard: vi.fn(),
    updateWard: vi.fn(),
  },
  notify: vi.fn(),
}));

vi.mock("../locationsApi", () => api);

import WardsSection from "../WardsSection";
import { LocationsContext } from "../LocationsPage";

const LISTS = { serviceTypes: [{ id: "OUTPATIENT", label: "Outpatient" }] };
const ORGANIZATION = {
  row: { id: "4", name: "Health Services Inc" },
  wards: [],
  gpsLatitude: null,
};

const addDraft = (index, name) => {
  fireEvent.click(screen.getByTestId("locations-add-ward"));
  fireEvent.change(document.getElementById(`new-ward-name-${index}`), {
    target: { value: name },
  });
  fireEvent.change(document.getElementById(`new-ward-service-${index}`), {
    target: { value: "OUTPATIENT" },
  });
};

describe("WardsSection new wards (OGC-1420 5f)", () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    notify.mockReset();
    api.listWards.mockResolvedValue([]);
  });

  it("names the ward that failed and does not send the saved one again", async () => {
    api.createWard
      .mockResolvedValueOnce({ id: "201" })
      .mockRejectedValueOnce(
        Object.assign(new Error("Code OPD is already used by Eye Clinic"), {
          status: 422,
          fieldErrors: {},
        }),
      )
      .mockResolvedValueOnce({ id: "202" });
    const onDraftsChange = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <LocationsContext.Provider value={{ notify }}>
          <WardsSection
            organization={ORGANIZATION}
            lists={LISTS}
            sectionId="wards"
            onDraftsChange={onDraftsChange}
          />
        </LocationsContext.Provider>
      </IntlProvider>,
    );
    addDraft(0, "Maternity");
    addDraft(1, "Outpatients");
    expect(onDraftsChange).toHaveBeenLastCalledWith(2);

    fireEvent.click(screen.getByTestId("locations-save-wards"));
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        "Outpatients was not saved: Code OPD is already used by Eye Clinic",
        "error",
      ),
    );
    expect(screen.getAllByTestId("locations-ward-draft")).toHaveLength(1);
    expect(onDraftsChange).toHaveBeenLastCalledWith(1);

    fireEvent.click(screen.getByTestId("locations-save-wards"));
    await waitFor(() => expect(api.createWard).toHaveBeenCalledTimes(3));
    expect(api.createWard.mock.calls.map(([, body]) => body.name)).toEqual([
      "Maternity",
      "Outpatients",
      "Outpatients",
    ]);
  });
});
