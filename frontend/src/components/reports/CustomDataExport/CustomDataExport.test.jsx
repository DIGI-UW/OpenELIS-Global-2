import React from "react";
import {
  render,
  screen,
  fireEvent,
  within,
  cleanup,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { QueryClientProvider } from "@tanstack/react-query";
import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import { act } from "react-dom/test-utils";
import { vi, beforeEach, afterEach, test, expect } from "vitest";
import messages from "../../../languages/en.json";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { createQueryClient } from "../../utils/queryClient";
import CustomDataExport, { clearReportingDraft } from "./CustomDataExport";

const source = {
  id: "SAMPLE_TESTING",
  label: "Sample & Testing",
  dateAnchor: "collectionDate",
  layouts: ["SPREADSHEET", "RESULT_LIST"],
};
let configuredFilters;
let configuredDefaults;
let alternateCatalog;
const field = (id, label, group = "sample") => ({ id, label, group });
const catalog = (layout) => ({
  definition: { ...source, filters: configuredFilters },
  variables: [
    field("accessionNumber", "Accession Number"),
    field("test:1", "Hemoglobin", "tests"),
    field("test:2", "White Cell Count", "tests"),
    field("resultValue", "Result Value", "result"),
  ],
  defaultColumns: configuredDefaults[layout],
  labSections: [{ id: "1", label: "Hematology" }],
  tests: [{ id: "1", label: "Hemoglobin" }],
  statuses: [{ id: "FINALIZED", label: "Finalized" }],
  maxDays: 90,
  timezone: "UTC",
});
let requests;
let failSubmission;
let submissionGate;
let catalogGate;
let job;
let savedReports;
let savedMutations;
let failSavedUpdate;
let failSavedCreate;
let deletedSaved;
let consoleErrors;
let recoveryRequests;
let failCancellation;
const json = (body, status = 200) => ({
  ok: status < 400,
  status,
  headers: { get: () => "application/json" },
  json: async () => body,
});

beforeEach(() => {
  consoleErrors = vi.spyOn(console, "error");
  configuredFilters = ["labSectionIds", "testIds", "resultStatuses"];
  configuredDefaults = { SPREADSHEET: [], RESULT_LIST: [] };
  alternateCatalog = undefined;
  clearReportingDraft();
  requests = [];
  failSubmission = false;
  submissionGate = undefined;
  catalogGate = undefined;
  failSavedUpdate = false;
  failSavedCreate = false;
  job = undefined;
  savedReports = [];
  savedMutations = [];
  deletedSaved = [];
  recoveryRequests = [];
  failCancellation = false;
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url, options = {}) => {
      if (url.includes("/report-types"))
        return json(
          alternateCatalog ? [source, alternateCatalog.definition] : [source],
        );
      if (url.includes("/variables")) {
        if (catalogGate) await catalogGate;
        if (
          alternateCatalog &&
          url.includes(`reportType=${alternateCatalog.definition.id}`)
        )
          return json(alternateCatalog);
        return json(
          catalog(
            url.includes("layout=RESULT_LIST") ? "RESULT_LIST" : "SPREADSHEET",
          ),
        );
      }
      if (
        url.includes("/saved-configs/") &&
        (!options.method || options.method === "GET")
      ) {
        const id = decodeURIComponent(url.split("/saved-configs/")[1]);
        return json(
          savedReports.find((saved) => saved.id === id) || {
            code: "reporting.saved.notFound",
          },
          savedReports.some((saved) => saved.id === id) ? 200 : 404,
        );
      }
      if (
        url.includes("/saved-configs") &&
        (!options.method || options.method === "GET")
      )
        return json({ reports: savedReports, hasMore: false, page: 0 });
      if (url.includes("/saved-configs") && options.method === "POST") {
        if (failSavedCreate)
          return json({ code: "reporting.networkError" }, 503);
        const body = JSON.parse(options.body);
        savedMutations.push(body);
        const created = {
          id: `saved-${savedReports.length + 1}`,
          name: body.name,
          version: "2026-09-13T20:00:00Z",
          createdBy: "1",
          updatedBy: "1",
          definition: body.definition,
        };
        savedReports.push(created);
        return json(created, 201);
      }
      if (url.includes("/saved-configs/") && options.method === "PUT") {
        const body = JSON.parse(options.body);
        savedMutations.push(body);
        if (failSavedUpdate)
          return json({ code: "reporting.saved.changed" }, 409);
        const updated = {
          ...savedReports[0],
          ...body,
          version: "next-version",
        };
        savedReports[0] = updated;
        return json(updated);
      }
      if (url.includes("/saved-configs/") && options.method === "DELETE") {
        const id = decodeURIComponent(
          url.split("/saved-configs/")[1].split("?")[0],
        );
        deletedSaved.push(id);
        savedReports = savedReports.filter((saved) => saved.id !== id);
        return json({}, 204);
      }
      if (url.endsWith("/cancel") && options.method === "POST") {
        recoveryRequests.push({ action: "cancel" });
        if (failCancellation)
          return json({ code: "reporting.networkError" }, 503);
        job = { ...job, state: "CANCELLED" };
        return json(job);
      }
      if (url.endsWith("/retry") && options.method === "POST") {
        recoveryRequests.push({ action: "retry", ...JSON.parse(options.body) });
        job = { ...job, id: "retry-child", parentId: job.id, state: "QUEUED" };
        return json(job, 202);
      }
      if (options.method === "POST") {
        const body = JSON.parse(options.body);
        requests.push(body);
        if (submissionGate) await submissionGate;
        if (failSubmission) return json({ code: "reporting.jobs.limit" }, 429);
        job = {
          id: `job-${requests.length}`,
          state: "READY",
          rowCount: 2,
          request: { definition: source, filterSpec: body.filterSpec },
        };
        return json(job, 202);
      }
      if (job && url.endsWith(`/jobs/${job.id}`)) return json(job);
      return json({ jobs: job ? [job] : [], hasMore: false, activeCount: 0 });
    }),
  );
});
afterEach(() => {
  const expectedErrors = new Set([
    "reporting.jobs.limit",
    "reporting.saved.changed",
    "reporting.networkError",
    "Request failed (404): /rest/reports/data-export/saved-configs/missing",
  ]);
  const unexpected = consoleErrors.mock.calls
    .map(([error]) => error?.message || String(error))
    .filter((message) => !expectedErrors.has(message));
  cleanup();
  vi.unstubAllGlobals();
  consoleErrors.mockRestore();
  expect(unexpected).toEqual([]);
});

function open(entry = "/reports/custom-data-export") {
  const history = createMemoryHistory({ initialEntries: [entry] });
  const rendered = render(
    <Router history={history}>
      <QueryClientProvider client={createQueryClient()}>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: { userId: "1" } }}
          >
            <CustomDataExport />
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </QueryClientProvider>
    </Router>,
  );
  return { ...rendered, history };
}

function recoverableJob(state) {
  return {
    id: "original-job",
    state,
    submittedAt: "2026-09-14T00:00:00Z",
    rowCount: null,
    request: {
      definition: source,
      layout: "SPREADSHEET",
      variables: [
        field("test:1", "Hemoglobin", "tests"),
        field("accessionNumber", "Accession Number"),
      ],
      filterSpec: {
        dateFrom: "2026-05-05",
        dateTo: "2026-05-05",
        labSectionIds: ["1"],
        testIds: ["1"],
        resultStatuses: ["FINALIZED"],
      },
    },
  };
}

test("queue cancellation requires confirmation and retains the dialog after a network failure", async () => {
  job = recoverableJob("QUEUED");
  failCancellation = true;
  open("/reports/custom-data-export?view=queue");
  fireEvent.click(
    await screen.findByRole("button", { name: "Cancel", exact: true }),
  );
  expect(recoveryRequests).toEqual([]);
  fireEvent.click(
    await screen.findByRole("button", { name: /Cancel export$/ }),
  );
  await waitFor(() =>
    expect(
      within(screen.getByRole("dialog")).getByText(
        messages["reporting.networkError"],
      ),
    ).toBeVisible(),
  );
  expect(recoveryRequests).toEqual([{ action: "cancel" }]);
  failCancellation = false;
  fireEvent.click(screen.getByRole("button", { name: /Cancel export$/ }));
  await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
  expect(await screen.findByText("Cancelled", { exact: true })).toBeVisible();
});

test("retry creates a linked queue job without opening or replacing the builder draft", async () => {
  job = recoverableJob("FAILED");
  const { history } = open("/reports/custom-data-export?view=queue");
  fireEvent.click(
    await screen.findByRole("button", { name: "Retry", exact: true }),
  );
  await waitFor(() => expect(recoveryRequests).toHaveLength(1));
  expect(recoveryRequests[0]).toEqual({
    action: "retry",
    clientRequestId: expect.any(String),
  });
  await waitFor(() =>
    expect(history.location.search).toContain("job=retry-child"),
  );
  expect(history.location.search).toContain("view=queue");
  await waitFor(() =>
    expect(screen.getByText("Queued", { exact: true })).toBeVisible(),
  );
});

test("expired rerun restores frozen ordered fields and filters but requires fresh dates", async () => {
  job = recoverableJob("EXPIRED");
  open("/reports/custom-data-export?view=queue");
  fireEvent.click(
    await screen.findByRole("button", { name: "Re-run", exact: true }),
  );
  expect(await screen.findByLabelText("Date from")).toHaveValue("");
  expect(screen.getByLabelText("Date to")).toHaveValue("");
  expect(
    screen.getByText(messages["reporting.saved.freshDates"]),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Back", exact: true }));
  expect(
    await screen.findByRole("heading", { name: "Your CSV columns (2)" }),
  ).toBeVisible();
  const columns = screen.getByRole("region", {
    name: messages["reporting.selected"],
  });
  const removeButtons = within(columns).getAllByRole("button", {
    name: /^Remove /,
  });
  expect(removeButtons[0]).toHaveAccessibleName("Remove Hemoglobin");
  expect(removeButtons[1]).toHaveAccessibleName("Remove Accession Number");
  expect(requests).toEqual([]);
});

test("rerun waits for its catalog without reporting valid fields or filters as unavailable", async () => {
  let resolveCatalog;
  catalogGate = new Promise((resolve) => {
    resolveCatalog = resolve;
  });
  job = recoverableJob("EXPIRED");
  open("/reports/custom-data-export?view=queue");
  fireEvent.click(
    await screen.findByRole("button", { name: "Re-run", exact: true }),
  );
  await waitFor(() =>
    expect(fetch.mock.calls.some(([url]) => url.includes("/variables"))).toBe(
      true,
    ),
  );
  expect(
    screen.queryByText(messages["reporting.filters.unavailable"]),
  ).toBeNull();
  expect(screen.queryByText(messages["reporting.columns.stale"])).toBeNull();
  expect(screen.getByText(messages["reporting.loading"])).toBeVisible();
  expect(screen.queryByLabelText("Date from")).toBeNull();
  await act(async () => resolveCatalog());
  expect(await screen.findByLabelText("Date from")).toHaveValue("");
  expect(
    screen.getByRole("combobox", { name: /^Tests / }),
  ).toHaveAccessibleName(/Total items selected: 1/);
  expect(
    screen.queryByText(messages["reporting.filters.unavailable"]),
  ).toBeNull();
  expect(screen.queryByText(messages["reporting.columns.stale"])).toBeNull();
  expect(requests).toEqual([]);
});

test("rerun preserves a removed field and requires correction after its catalog loads", async () => {
  job = recoverableJob("EXPIRED");
  job.request.variables.push(field("test:removed", "Removed test", "tests"));
  open("/reports/custom-data-export?view=queue");
  fireEvent.click(
    await screen.findByRole("button", { name: "Re-run", exact: true }),
  );
  expect(
    await screen.findByText(messages["reporting.columns.stale"]),
  ).toBeVisible();
  expect(
    await screen.findByRole("button", { name: "Remove test:removed" }),
  ).toBeVisible();
  expect(
    screen.getByRole("button", {
      name: messages["reporting.design.nextFilters"],
    }),
  ).toBeDisabled();
  expect(requests).toEqual([]);
});

test("the mock overview leads to collapsed groups and Add actions without selecting the whole test catalog", async () => {
  open();
  expect(
    await screen.findByRole("heading", { name: "Create a new export" }),
  ).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Use a saved report" }),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Start a new export" }));
  fireEvent.click(
    await screen.findByRole("radio", { name: /Sample & Testing/ }),
  );
  const available = await screen.findByRole("region", {
    name: "Available fields",
  });
  expect(within(available).queryByRole("checkbox")).toBeNull();
  expect(
    within(available).getByRole("button", { name: "Configured tests" }),
  ).toHaveAttribute("aria-expanded", "false");
  expect(
    within(available).queryByRole("button", { name: "Add Hemoglobin" }),
  ).toBeNull();
  expect(
    screen.getByRole("heading", { name: "Your CSV columns (0)" }),
  ).toBeVisible();
  fireEvent.change(screen.getByRole("searchbox", { name: "Find a field" }), {
    target: { value: "Configured tests" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Add Hemoglobin" }),
  );
  expect(
    screen.getByRole("button", { name: "Added Hemoglobin" }),
  ).toHaveAttribute("aria-disabled", "true");
  fireEvent.click(screen.getByRole("button", { name: "Clear search" }));
  expect(
    screen.getByRole("button", { name: "Configured tests" }),
  ).toHaveAttribute("aria-expanded", "false");
  expect(
    screen.getByRole("button", { name: "Drag Hemoglobin to reorder" }),
  ).toBeVisible();
});

test.each(["SPREADSHEET", "RESULT_LIST"])(
  "%s honors configured defaults and retains a deliberately cleared selection after reload",
  async (layout) => {
    configuredDefaults[layout] = ["accessionNumber"];
    const entry = `/reports/custom-data-export?view=builder&step=columns&type=SAMPLE_TESTING&layout=${layout}`;
    const rendered = open(entry);
    fireEvent.click(
      await screen.findByRole("button", { name: "Remove Accession Number" }),
    );
    expect(
      screen.getByRole("heading", { name: "Your CSV columns (0)" }),
    ).toBeVisible();
    rendered.unmount();
    open(entry);
    await screen.findByRole("region", { name: "Available fields" });
    expect(
      screen.getByRole("heading", { name: "Your CSV columns (0)" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Remove Accession Number" }),
    ).toBeNull();
  },
);

test.each([
  ["sentDate", "referral sent dates"],
  ["requestDate", "referral request dates"],
  [
    "eventOrRecordedDate",
    "event dates when known, otherwise recorded dates (identified by Date Basis)",
  ],
])(
  "another report starts with explicit Add choices and explains its %s period",
  async (dateAnchor, dateMeaning) => {
    const nc = dateAnchor === "eventOrRecordedDate";
    const typeLabel = nc ? "Non-Conformance" : "Referrals";
    const groupLabel = nc ? "Non-Conformance / Rejections" : "Referrals";
    const fieldId = nc ? "ncOccurrenceId" : "referralId";
    const fieldLabel = nc ? "Occurrence ID" : "Referral ID";
    alternateCatalog = {
      ...catalog("TABLE"),
      definition: {
        id: nc ? "NON_CONFORMANCE" : "REFERRALS",
        label: typeLabel,
        layouts: ["TABLE"],
        dateAnchor,
        filters: [],
      },
      statuses: [],
      variables: [
        field(fieldId, fieldLabel, nc ? "nonConformance" : "referrals"),
      ],
      defaultColumns: [fieldId],
    };
    open();
    fireEvent.click(
      await screen.findByRole("button", { name: "Start a new export" }),
    );
    fireEvent.click(
      await screen.findByRole("radio", { name: new RegExp(typeLabel) }),
    );
    expect(
      await screen.findByRole("heading", { name: "Your CSV columns (0)" }),
    ).toBeVisible();
    const available = screen.getByRole("region", { name: "Available fields" });
    const group = within(available).getByRole("button", {
      name: groupLabel,
      exact: true,
    });
    expect(group).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(group);
    fireEvent.click(
      within(available).getByRole("button", { name: `Add ${fieldLabel}` }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Next: Set Filters" }));
    expect(
      screen.getByText(
        `Uses ${dateMeaning} in UTC, including both dates. Maximum period: 90 days.`,
      ),
    ).toBeVisible();
    expect(screen.queryByRole("combobox", { name: /^Tests/ })).toBeNull();
    expect(
      screen.queryByRole("combobox", { name: /^Result statuses/ }),
    ).toBeNull();
    fireEvent.change(screen.getByLabelText("Date from"), {
      target: { value: "2026-05-07" },
    });
    fireEvent.change(screen.getByLabelText("Date to"), {
      target: { value: "2026-05-07" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Next: Review & Submit" }),
    );
    expect(
      await screen.findByRole("button", { name: "Generate CSV" }),
    ).toBeVisible();
    expect(screen.queryByText(/Finalized/)).toBeNull();
  },
);

test("the mock column picker keeps exact keyboard ordering and focus", async () => {
  open();
  fireEvent.click(
    await screen.findByRole("button", { name: "Start a new export" }),
  );
  fireEvent.click(
    await screen.findByRole("radio", { name: /Sample & Testing/ }),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Expand all" }));
  for (const label of ["Accession Number", "Hemoglobin", "White Cell Count"])
    fireEvent.click(screen.getByRole("button", { name: `Add ${label}` }));
  const handle = screen.getByRole("button", {
    name: "Drag White Cell Count to reorder",
  });
  fireEvent.keyDown(handle, { key: "Home" });
  expect(
    within(screen.getByRole("region", { name: "CSV header preview" }))
      .getAllByRole("columnheader")
      .map((n) => n.textContent),
  ).toEqual(["White Cell Count", "Accession Number", "Hemoglobin"]);
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Drag White Cell Count to reorder" }),
    ).toHaveFocus(),
  );
  fireEvent.keyDown(
    screen.getByRole("button", { name: "Drag White Cell Count to reorder" }),
    { key: "End" },
  );
  expect(
    within(screen.getByRole("region", { name: "CSV header preview" }))
      .getAllByRole("columnheader")
      .map((n) => n.textContent),
  ).toEqual(["Accession Number", "Hemoglobin", "White Cell Count"]);
});
test("browser Back returns to the source chooser and Forward restores columns without losing external query parameters", async () => {
  const { history } = open("/reports/custom-data-export?uat=review");
  fireEvent.click(
    await screen.findByRole("button", { name: "Start a new export" }),
  );
  fireEvent.click(
    await screen.findByRole("radio", { name: /Sample & Testing/ }),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Expand all" }));
  fireEvent.click(screen.getByRole("button", { name: "Add Hemoglobin" }));
  act(() => history.goBack());
  await waitFor(() =>
    expect(
      screen.queryByRole("region", { name: "Available fields" }),
    ).not.toBeInTheDocument(),
  );
  expect(new URLSearchParams(history.location.search).get("uat")).toBe(
    "review",
  );
  act(() => history.goForward());
  expect(
    await screen.findByRole("columnheader", { name: "Hemoglobin" }),
  ).toBeVisible();
});

test("a review link without a draft returns to the required column selection", async () => {
  const { history } = open(
    "/reports/custom-data-export?view=builder&type=SAMPLE_TESTING&layout=SPREADSHEET&step=review",
  );
  await waitFor(() =>
    expect(new URLSearchParams(history.location.search).get("step")).toBe(
      "columns",
    ),
  );
  expect(
    screen.getByRole("heading", { name: "Your CSV columns (0)" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Generate CSV" }),
  ).not.toBeInTheDocument();
  expect(requests).toHaveLength(0);
});
async function start(columns = ["Accession Number", "Hemoglobin"]) {
  fireEvent.click(
    await screen.findByRole("button", { name: "Start a new export" }),
  );
  fireEvent.click(
    await screen.findByRole("radio", { name: /Sample & Testing/ }),
  );
  await addColumns(columns);
}
async function addColumns(columns) {
  fireEvent.click(await screen.findByRole("button", { name: "Expand all" }));
  for (const label of columns)
    fireEvent.click(screen.getByRole("button", { name: `Add ${label}` }));
}
async function period() {
  const next = screen.queryByRole("button", { name: "Next: Set Filters" });
  if (next) fireEvent.click(next);
  fireEvent.change(await screen.findByLabelText("Date from"), {
    target: { value: "2026-08-01" },
  });
  fireEvent.change(screen.getByLabelText("Date to"), {
    target: { value: "2026-08-31" },
  });
}
function review() {
  fireEvent.click(
    screen.getByRole("button", { name: "Next: Review & Submit" }),
  );
}
function generate() {
  fireEvent.click(screen.getByRole("button", { name: "Generate CSV" }));
}
function headers() {
  const preview = screen.queryByRole("region", { name: "CSV header preview" });
  return preview
    ? within(preview)
        .getAllByRole("columnheader")
        .map((n) => n.textContent)
    : within(screen.getByRole("list", { name: "CSV columns in order" }))
        .getAllByRole("listitem")
        .map((n) => n.textContent);
}
function savedFixture() {
  return {
    id: "saved-1",
    name: "Monthly hematology",
    version: "old-version",
    definition: {
      schemaVersion: 1,
      reportType: "SAMPLE_TESTING",
      layout: "SPREADSHEET",
      selectedVariables: ["accessionNumber", "test:1"],
      filters: {
        labSectionIds: [],
        testIds: [],
        resultStatuses: ["FINALIZED"],
      },
    },
  };
}
async function useSaved() {
  fireEvent.click(await screen.findByRole("button", { name: "Use report" }));
  await screen.findByLabelText("Date from");
}

test("configured selection and reordered preview are submitted, then a download appears in place", async () => {
  open();
  await start(["Accession Number", "Hemoglobin", "White Cell Count"]);
  fireEvent.click(
    screen.getByRole("button", { name: "Move White Cell Count up" }),
  );
  expect(headers()).toEqual([
    "Accession Number",
    "White Cell Count",
    "Hemoglobin",
  ]);
  await period();
  review();
  generate();
  expect(
    await screen.findByRole("link", { name: "Download CSV" }),
  ).toHaveAttribute("href", expect.stringContaining("/jobs/job-1/download"));
  expect(requests[0].selectedVariables).toEqual([
    "accessionNumber",
    "test:2",
    "test:1",
  ]);
  expect(requests[0].filterSpec.dateFrom).toBe("2026-08-01");
});

test("failed submission retains choices and retries with the same request identity", async () => {
  failSubmission = true;
  open();
  await start();
  await period();
  review();
  generate();
  await screen.findByText(messages["reporting.jobs.limit"]);
  failSubmission = false;
  generate();
  await screen.findByRole("link", { name: "Download CSV" });
  expect(requests).toHaveLength(2);
  expect(requests[1]).toEqual(requests[0]);
});

test("a rerun cannot download the prior file while submitting or after failure, and its retry keeps the request identity", async () => {
  open();
  await start();
  await period();
  review();
  generate();
  expect(
    await screen.findByRole("link", { name: "Download CSV" }),
  ).toHaveAttribute("href", expect.stringContaining("/job-1/download"));
  fireEvent.click(screen.getByRole("button", { name: "Edit report" }));
  fireEvent.click(screen.getByRole("combobox", { name: "CSV layout" }));
  fireEvent.click(
    screen.getByRole("option", { name: "Detailed list — results in rows" }),
  );
  await addColumns(["Accession Number", "Result Value"]);
  let release;
  submissionGate = new Promise((resolve) => {
    release = resolve;
  });
  failSubmission = true;
  await period();
  review();
  generate();
  await waitFor(() => expect(requests).toHaveLength(2));
  expect(
    screen.queryByRole("link", { name: "Download CSV" }),
  ).not.toBeInTheDocument();
  release();
  await screen.findByText(messages["reporting.jobs.limit"]);
  expect(
    screen.queryByRole("link", { name: "Download CSV" }),
  ).not.toBeInTheDocument();
  expect(headers()).toEqual(["Accession Number", "Result Value"]);
  submissionGate = undefined;
  failSubmission = false;
  generate();
  expect(
    await screen.findByRole("link", { name: "Download CSV" }),
  ).toHaveAttribute("href", expect.stringContaining("/job-3/download"));
  expect(requests).toHaveLength(3);
  expect(requests[2]).toEqual(requests[1]);
  expect(requests[2].clientRequestId).not.toBe(requests[0].clientRequestId);
});

test("switching layouts and browser navigation retain each layout's columns and the period", async () => {
  const { history } = open();
  await start(["Accession Number", "Hemoglobin", "White Cell Count"]);
  await period();
  review();
  fireEvent.click(screen.getByRole("button", { name: "My Report Queue" }));
  expect(new URLSearchParams(history.location.search).get("view")).toBe(
    "queue",
  );
  act(() => history.goBack());
  expect(
    await screen.findByRole("button", { name: "Generate CSV" }),
  ).toBeEnabled();
  expect(screen.getByText(/2026-08-01 – 2026-08-31/)).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Edit report" }));
  fireEvent.click(screen.getByRole("combobox", { name: "CSV layout" }));
  fireEvent.click(
    screen.getByRole("option", { name: "Detailed list — results in rows" }),
  );
  await addColumns(["Result Value"]);
  expect(headers()).toEqual(["Result Value"]);
  act(() => history.goBack());
  await waitFor(() =>
    expect(headers()).toEqual([
      "Accession Number",
      "Hemoglobin",
      "White Cell Count",
    ]),
  );
  await period();
  review();
  generate();
  await screen.findByRole("link", { name: "Download CSV" });
  expect(requests[0].layout).toBe("SPREADSHEET");
  expect(requests[0].filterSpec.dateTo).toBe("2026-08-31");
});

test("missing, reversed and excessive dates are explained before submitting", async () => {
  open();
  await start();
  fireEvent.click(screen.getByRole("button", { name: "Next: Set Filters" }));
  const from = await screen.findByLabelText("Date from");
  const to = screen.getByLabelText("Date to");
  review();
  expect(screen.getByText("Choose a start date.")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Generate CSV" }),
  ).not.toBeInTheDocument();
  fireEvent.change(from, { target: { value: "2026-01-01" } });
  fireEvent.change(to, { target: { value: "2025-12-31" } });
  expect(
    screen.getByText("End date must be on or after the start date."),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Next: Review & Submit" }),
  ).toBeDisabled();
  fireEvent.change(to, { target: { value: "2026-04-01" } });
  expect(
    screen.getByText(
      "Choose a period of 90 days or fewer, including both dates.",
    ),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Next: Review & Submit" }),
  ).toBeDisabled();
  fireEvent.change(to, { target: { value: "2026-03-31" } });
  expect(
    screen.getByRole("button", { name: "Next: Review & Submit" }),
  ).toBeEnabled();
  expect(requests).toHaveLength(0);
});

test("reloading a review URL restores the draft, while a cleared session returns to column selection", async () => {
  const first = open();
  await start();
  await period();
  review();
  const url = first.history.location.pathname + first.history.location.search;
  first.unmount();
  const returned = open(url);
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Generate CSV" })).toBeEnabled(),
  );
  expect(screen.getByText(/2026-08-01 – 2026-08-31/)).toBeVisible();
  returned.unmount();
  clearReportingDraft();
  const fresh = open(url);
  await waitFor(() =>
    expect(new URLSearchParams(fresh.history.location.search).get("step")).toBe(
      "columns",
    ),
  );
  expect(
    screen.getByRole("heading", { name: "Your CSV columns (0)" }),
  ).toBeVisible();
});

test("a shared report saves choices without dates and reopening requires fresh dates", async () => {
  open();
  await start(["Accession Number", "Hemoglobin", "White Cell Count"]);
  await period();
  review();
  fireEvent.click(
    screen.getByLabelText(messages["reporting.design.saveLater"]),
  );
  fireEvent.change(screen.getByLabelText("Report name"), {
    target: { value: "Monthly hematology" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save report settings" }));
  expect(await screen.findByText("Saved as Monthly hematology.")).toBeVisible();
  expect(savedMutations[0].definition.selectedVariables).toEqual([
    "accessionNumber",
    "test:1",
    "test:2",
  ]);
  expect(JSON.stringify(savedMutations[0])).not.toContain("dateFrom");
  expect(JSON.stringify(savedMutations[0])).not.toContain("dateTo");
  fireEvent.click(screen.getByRole("button", { name: "Export overview" }));
  await useSaved();
  expect(screen.getByLabelText("Date from")).toHaveValue("");
  expect(screen.getByLabelText("Date to")).toHaveValue("");
  expect(
    screen.getByText("Choose fresh dates before running this report."),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Back" }));
  expect(headers()).toEqual([
    "Accession Number",
    "Hemoglobin",
    "White Cell Count",
  ]);
});

test("a saved-report deep link loads its server definition with fresh dates and survives reload", async () => {
  savedReports.push(savedFixture());
  const first = open("/reports/custom-data-export?view=builder&saved=saved-1");
  expect(await screen.findByLabelText("Date from")).toHaveValue("");
  expect(new URLSearchParams(first.history.location.search).get("type")).toBe(
    "SAMPLE_TESTING",
  );
  await period();
  review();
  const url = first.history.location.pathname + first.history.location.search;
  first.unmount();
  open(url);
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Generate CSV" })).toBeEnabled(),
  );
  expect(headers()).toEqual(["Accession Number", "Hemoglobin"]);
  expect(screen.getByText(/2026-08-01 – 2026-08-31/)).toBeVisible();
});

test("a missing saved link offers recovery without presenting another draft as that report", async () => {
  open("/reports/custom-data-export?view=builder&saved=missing");
  expect(
    await screen.findByText(messages["reporting.saved.loadError"]),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Generate CSV" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Start a new export" }));
  expect(
    await screen.findByRole("radio", { name: /Sample & Testing/ }),
  ).toBeVisible();
});

test("a stale shared-report update keeps the draft and explains the conflict", async () => {
  savedReports.push(savedFixture());
  failSavedUpdate = true;
  open();
  await useSaved();
  await period();
  review();
  fireEvent.click(screen.getByRole("button", { name: "Update shared report" }));
  fireEvent.click(screen.getByRole("button", { name: "Update" }));
  expect(
    await screen.findByText(messages["reporting.saved.changed"]),
  ).toBeVisible();
  expect(headers()).toEqual(["Accession Number", "Hemoglobin"]);
});

test("saving a copy resolves a stale-edit warning only after the copy succeeds", async () => {
  const original = savedFixture();
  savedReports.push(original);
  failSavedUpdate = true;
  open();
  await useSaved();
  await period();
  review();
  fireEvent.click(screen.getByRole("button", { name: "Update shared report" }));
  fireEvent.click(screen.getByRole("button", { name: "Update" }));
  expect(
    await screen.findByText(messages["reporting.saved.changed"]),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Save a copy" }));
  fireEvent.change(screen.getByLabelText("Report name"), {
    target: { value: "Recovered report" },
  });
  failSavedCreate = true;
  fireEvent.click(screen.getByRole("button", { name: "Save shared report" }));
  expect(
    await within(screen.getByRole("dialog")).findByText(
      messages["reporting.networkError"],
    ),
  ).toBeVisible();
  expect(screen.getByLabelText("Report name")).toHaveValue("Recovered report");
  expect(savedReports).toEqual([original]);
  expect(headers()).toEqual(["Accession Number", "Hemoglobin"]);
  failSavedCreate = false;
  fireEvent.click(screen.getByRole("button", { name: "Save shared report" }));
  expect(await screen.findByText("Saved as Recovered report.")).toBeVisible();
  expect(
    screen.queryByText(messages["reporting.saved.changed"]),
  ).not.toBeInTheDocument();
  expect(savedReports[0]).toEqual(original);
  expect(savedReports[1].definition).toEqual(original.definition);
});

test("configured filters exclude unsupported restored choices from review, save, and generation", async () => {
  configuredFilters = ["labSectionIds"];
  const saved = savedFixture();
  saved.definition.filters = {
    labSectionIds: ["1"],
    testIds: ["2"],
    resultStatuses: ["CANCELED"],
  };
  savedReports.push(saved);
  open();
  await useSaved();
  await period();
  expect(screen.getByRole("combobox", { name: /^Lab sections/ })).toBeVisible();
  expect(
    screen.queryByRole("combobox", { name: /^Tests/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("combobox", { name: /^Result statuses/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText(messages["reporting.filters.unavailable"]),
  ).toBeVisible();
  review();
  expect(screen.getByText("Hematology")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Update shared report" }));
  fireEvent.click(screen.getByRole("button", { name: "Update" }));
  await screen.findByText("Updated Monthly hematology.");
  expect(savedMutations.at(-1).definition.filters).toEqual({
    labSectionIds: ["1"],
    testIds: [],
    resultStatuses: [],
  });
  generate();
  await screen.findByRole("link", { name: "Download CSV" });
  expect(requests[0].filterSpec).toEqual({
    dateFrom: "2026-08-01",
    dateTo: "2026-08-31",
    labSectionIds: ["1"],
    testIds: [],
    resultStatuses: [],
  });
});

test("a shared report can be copied and deleted without changing generated jobs", async () => {
  savedReports.push(savedFixture());
  open();
  fireEvent.click(
    await screen.findByRole("button", { name: "Shared reports" }),
  );
  let card = await screen.findByRole("article", { name: "Monthly hematology" });
  fireEvent.click(within(card).getByRole("button", { name: "Save a copy" }));
  expect(screen.getByLabelText("Report name")).toHaveValue(
    "Copy of Monthly hematology",
  );
  fireEvent.click(screen.getByRole("button", { name: "Save shared report" }));
  expect(
    await screen.findByText("Saved as Copy of Monthly hematology."),
  ).toBeVisible();
  expect(savedMutations.at(-1).expectedVersion).toBeUndefined();
  fireEvent.click(screen.getByRole("button", { name: "Export overview" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Shared reports" }),
  );
  card = await screen.findByRole("article", { name: "Monthly hematology" });
  fireEvent.click(
    within(card).getByRole("button", { name: /Delete shared report/ }),
  );
  fireEvent.click(screen.getByRole("button", { name: /Delete$/ }));
  await waitFor(() =>
    expect(
      screen.queryByRole("article", { name: "Monthly hematology" }),
    ).not.toBeInTheDocument(),
  );
  expect(deletedSaved).toEqual(["saved-1"]);
  expect(requests).toHaveLength(0);
});

test("a delayed job response does not attach its download to a new draft", async () => {
  let release;
  submissionGate = new Promise((resolve) => {
    release = resolve;
  });
  open();
  await start();
  await period();
  review();
  generate();
  await waitFor(() => expect(requests).toHaveLength(1));
  fireEvent.click(screen.getByRole("button", { name: "Export overview" }));
  await start(["White Cell Count"]);
  await act(async () => {
    release();
    await submissionGate;
  });
  expect(
    screen.queryByRole("region", { name: "Your current report" }),
  ).not.toBeInTheDocument();
  expect(headers()).toEqual(["White Cell Count"]);
  fireEvent.click(screen.getByRole("button", { name: "My Report Queue" }));
  expect(
    await screen.findByRole("link", { name: "Download CSV" }),
  ).toHaveAttribute("href", expect.stringContaining("/job-1/download"));
});

test("saved-report search is URL state without adding a history entry for every edit", async () => {
  const { history } = open();
  fireEvent.click(
    await screen.findByRole("button", { name: "Shared reports" }),
  );
  fireEvent.change(
    screen.getByRole("searchbox", { name: "Search shared reports" }),
    { target: { value: "Monthly & weekly" } },
  );
  expect(new URLSearchParams(history.location.search).get("q")).toBe(
    "Monthly & weekly",
  );
  await waitFor(() =>
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("search=Monthly%20%26%20weekly"),
      expect.anything(),
    ),
  );
  act(() => history.goBack());
  expect(
    await screen.findByRole("heading", { name: "Create a new export" }),
  ).toBeVisible();
  act(() => history.goForward());
  expect(
    screen.getByRole("searchbox", { name: "Search shared reports" }),
  ).toHaveValue("Monthly & weekly");
});

test("an invalid queue page is normalized while a job link opens its frozen details", async () => {
  job = {
    id: "job-linked",
    state: "READY",
    rowCount: 2,
    request: {
      definition: source,
      layout: "RESULT_LIST",
      filterSpec: { dateFrom: "2026-05-05", dateTo: "2026-05-05" },
      variables: [{ id: "resultValue", label: "Result Value" }],
    },
  };
  const { history } = open(
    "/reports/custom-data-export?view=queue&page=-2&job=job-linked&uat=review",
  );
  expect(
    await screen.findByRole("link", { name: "Download CSV" }),
  ).toHaveAttribute(
    "href",
    expect.stringContaining("/jobs/job-linked/download"),
  );
  expect(screen.getByText("Result Value")).toBeVisible();
  expect(new URLSearchParams(history.location.search).get("page")).toBeNull();
  expect(new URLSearchParams(history.location.search).get("uat")).toBe(
    "review",
  );
  expect(fetch).toHaveBeenCalledWith(
    expect.stringContaining("/jobs?page=0"),
    expect.anything(),
  );
  fireEvent.click(screen.getByRole("button", { name: "Details" }));
  expect(new URLSearchParams(history.location.search).get("job")).toBeNull();
  act(() => history.goBack());
  expect(screen.getByText("Result Value")).toBeVisible();
  expect(requests).toHaveLength(0);
});

test("an empty queue page offers a route back to the first page", async () => {
  const { history } = open("/reports/custom-data-export?view=queue&page=3");
  fireEvent.click(
    await screen.findByRole("button", { name: "Return to the first page" }),
  );
  expect(new URLSearchParams(history.location.search).get("page")).toBeNull();
  expect(
    await screen.findByText(messages["reporting.queueEmpty"]),
  ).toBeVisible();
  expect(requests).toHaveLength(0);
});

test("starting a new export resets the previous report's save controls and name", async () => {
  open();
  await start();
  await period();
  review();
  fireEvent.click(
    screen.getByLabelText(messages["reporting.design.saveLater"]),
  );
  fireEvent.change(screen.getByRole("textbox", { name: "Report name" }), {
    target: { value: "Previous report" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Export overview" }));
  await start(["White Cell Count"]);
  await period();
  review();
  expect(
    screen.getByLabelText(messages["reporting.design.saveLater"]),
  ).not.toBeChecked();
  fireEvent.click(
    screen.getByLabelText(messages["reporting.design.saveLater"]),
  );
  expect(screen.getByRole("textbox", { name: "Report name" })).toHaveValue("");
  expect(requests).toHaveLength(0);
});
