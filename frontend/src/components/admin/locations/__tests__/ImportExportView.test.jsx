import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";

/**
 * OGC-1363 section F: files are staged with their area, the preview shows the
 * counts, the decision queue and the Replace deactivations, Apply stays
 * disabled until every decision is made, and Replace asks for an explicit
 * acknowledgement of the deactivations.
 */
const { utils, api, notify } = vi.hoisted(() => ({
  utils: { postToOpenElisServerFormDataJsonResponse: vi.fn() },
  api: { getJson: vi.fn() },
  notify: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => utils);
vi.mock("../locationsApi", () => api);

import ImportExportView from "../ImportExportView";
import { LocationsContext } from "../LocationsPage";

const PLAN = {
  importRunId: "run-1",
  mode: "replace",
  scope: {
    text: "This file has dept rows. Referral labs will not change.",
    types: ["dept"],
    untouched: [],
    wardParents: ["PMGH"],
  },
  counts: {
    new: 1,
    updated: 1,
    unchanged: 0,
    reactivated: 0,
    deactivated: 1,
    decision: 1,
    rejected: 0,
  },
  rows: [
    {
      file: "organizations-png.csv",
      line: 2,
      outcome: "updated",
      type: "dept",
      code: "PMGH-OPD",
      name: "Outpatient Department",
      diffs: [{ field: "contactName", oldValue: "A", newValue: "B" }],
      candidates: [],
      pair: null,
      registry: false,
    },
    {
      file: "organizations-png.csv",
      line: 3,
      outcome: "new",
      type: "dept",
      code: "PMGH-EYE",
      name: "Eye Clinic",
      diffs: [],
      candidates: [],
      pair: null,
      registry: false,
    },
    {
      file: "organizations-png.csv",
      line: 4,
      outcome: "decision",
      type: "dept",
      code: "",
      name: "Outpatient Department",
      diffs: [],
      candidates: [
        {
          id: "c1",
          name: "Outpatient Department",
          code: "",
          parent: "Gerehu",
          active: true,
          inUse: 14,
        },
      ],
      pair: null,
      registry: false,
    },
  ],
  deactivations: [
    {
      id: "u4",
      name: "Medical Ward 3",
      code: "",
      location: "PMGH",
      inUse: { open: 0, total: 410 },
    },
  ],
  unresolvedCount: 0,
  errors: [],
};

const wrap = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LocationsContext.Provider
        value={{ lists: null, reloadLists: vi.fn(), notify, go: vi.fn() }}
      >
        <MemoryRouter initialEntries={["/MasterListsPage/locations/import"]}>
          <ImportExportView />
        </MemoryRouter>
      </LocationsContext.Provider>
    </IntlProvider>,
  );

describe("ImportExportView (OGC-1363)", () => {
  beforeEach(() => {
    utils.postToOpenElisServerFormDataJsonResponse.mockReset();
    api.getJson.mockReset();
    api.getJson.mockResolvedValue([]);
    notify.mockReset();
  });

  it("stages a file with the area inferred from its name, previews, and gates Apply on the decisions", async () => {
    utils.postToOpenElisServerFormDataJsonResponse.mockImplementation(
      (path, data, cb) => cb(PLAN),
    );
    wrap();
    const input = document.querySelector('input[type="file"]');
    const file = new File(["type,name\n"], "organizations-png.csv", {
      type: "text/csv",
    });
    fireEvent.change(input, { target: { files: [file] } });
    expect(
      await screen.findByTestId("locations-import-file"),
    ).toHaveTextContent("organizations-png.csv");
    expect(screen.getByLabelText("Area")).toHaveValue("organizations");

    fireEvent.click(screen.getByRole("radio", { name: /^Replace/ }));
    fireEvent.click(screen.getByTestId("locations-import-preview"));
    expect(
      await screen.findByTestId("locations-import-count-new"),
    ).toHaveTextContent("1");
    expect(
      screen.getByTestId("locations-import-count-deactivated"),
    ).toHaveTextContent("1");
    const [path, data] =
      utils.postToOpenElisServerFormDataJsonResponse.mock.calls[0];
    expect(path).toBe("/rest/locations/import/preview");
    expect(data.get("mode")).toBe("replace");
    expect(data.get("areas")).toBe("organizations");
    expect(screen.getByText("Medical Ward 3 (PMGH)")).toBeInTheDocument();
    expect(screen.getByText(/This file has dept rows/)).toBeInTheDocument();

    const apply = screen.getByTestId("locations-import-apply");
    expect(apply).toBeDisabled();
    fireEvent.click(
      screen.getByLabelText(/Use this record: Outpatient Department/),
    );
    expect(apply).toBeEnabled();
    fireEvent.click(apply);
    expect(await screen.findByText("Apply this import?")).toBeInTheDocument();
    const confirm = screen.getAllByRole("button", { name: "Apply" }).pop();
    expect(confirm).toBeDisabled();
    fireEvent.click(
      screen.getByLabelText("I understand 1 records will be deactivated"),
    );
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);
    await waitFor(() =>
      expect(
        utils.postToOpenElisServerFormDataJsonResponse,
      ).toHaveBeenCalledTimes(2),
    );
    const [applyPath, applyData] =
      utils.postToOpenElisServerFormDataJsonResponse.mock.calls[1];
    expect(applyPath).toBe("/rest/locations/import/apply");
    expect(JSON.parse(applyData.get("decisions"))).toEqual({
      4: { choice: "use:c1", remember: false },
    });
    expect(
      await screen.findByTestId("locations-import-done"),
    ).toBeInTheDocument();
  });

  it("reports a preview the server refused", async () => {
    utils.postToOpenElisServerFormDataJsonResponse.mockImplementation(
      (path, data, cb) =>
        cb({ status: 422, error: "'wrong' is not an import area" }),
    );
    wrap();
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, {
      target: {
        files: [new File(["x"], "organizations-bad.csv", { type: "text/csv" })],
      },
    });
    await screen.findByTestId("locations-import-file");
    fireEvent.click(screen.getByTestId("locations-import-preview"));
    expect(
      await screen.findByText(/'wrong' is not an import area/),
    ).toBeInTheDocument();
  });
  const previewWith = async (plan) => {
    utils.postToOpenElisServerFormDataJsonResponse.mockImplementation(
      (path, data, cb) => cb(plan),
    );
    wrap();
    const input = document.querySelector('input[type="file"]');
    fireEvent.change(input, {
      target: {
        files: [
          new File(["type,name\n"], "organizations-png.csv", {
            type: "text/csv",
          }),
        ],
      },
    });
    await screen.findByTestId("locations-import-file");
    fireEvent.click(screen.getByTestId("locations-import-preview"));
    await screen.findByTestId("locations-import-count-new");
  };

  it("shows a file's own errors and the columns it ignored (OGC-1420 1d)", async () => {
    await previewWith({
      ...PLAN,
      errors: ["organizations-png.csv: the file is empty"],
      ignoredColumns: ["Notes", "Region"],
    });
    expect(screen.getByTestId("locations-import-file-error")).toHaveTextContent(
      "organizations-png.csv: the file is empty",
    );
    expect(screen.getByTestId("locations-import-ignored")).toHaveTextContent(
      "These columns are not imported and were ignored: Notes, Region",
    );
  });

  it("names each recent run's files and tells a preview from an apply (OGC-1420 9b)", async () => {
    api.getJson.mockResolvedValue([
      {
        id: "r2",
        startedAt: "2026-10-02 10:00",
        user: "admin",
        mode: "merge",
        summary: '{"new":2,"updated":1}',
        files: ["organizations-png.csv"],
        action: "apply",
      },
      {
        id: "r1",
        startedAt: "2026-10-02 09:55",
        user: "admin",
        mode: "merge",
        summary: '{"new":2}',
        files: ["organizations-draft.csv"],
        action: "preview",
      },
    ]);
    wrap();
    const applied = await screen.findByTestId("locations-import-run-r2");
    expect(applied).toHaveTextContent("Applied");
    expect(applied).toHaveTextContent("organizations-png.csv");
    expect(applied).toHaveTextContent("2 New, 1 Updated");
    expect(screen.getByTestId("locations-import-run-r1")).toHaveTextContent(
      "Preview",
    );
  });
});
