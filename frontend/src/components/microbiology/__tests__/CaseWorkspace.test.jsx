import React from "react";
import { render, screen, fireEvent, act, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { Router, Route } from "react-router-dom";
import { createMemoryHistory } from "history";
import CaseWorklist from "../CaseWorklist";
import CaseViewShell from "../CaseViewShell";
import {
  MICROBIOLOGY_CASE_READY_MARK,
  markMicrobiologyReady,
} from "../MicrobiologyPerformance";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { Roles } from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../MicrobiologyTime", async (original) => ({
  ...(await original()),
  labTimeZone: () => "UTC",
}));
vi.mock("../CaseTestingWorkspace", () => ({ default: () => null }));
vi.mock("../MicrobiologyPerformance", async (importOriginal) => ({
  ...(await importOriginal()),
  markMicrobiologyReady: vi.fn(),
}));

const renderPage = (component, path = "/Microbiology/worklist") => {
  const history = createMemoryHistory({ initialEntries: [path] });
  render(
    <IntlProvider locale="en" messages={messages}>
      <Router history={history}>{component}</Router>
    </IntlProvider>,
  );
  return history;
};
const row = {
  id: "case-1",
  accessionNumber: "M2-001",
  patientName: "Test Patient",
  labUnit: "Culture lab",
  specimenType: "Sputum",
  status: "ACTIVE",
  createdAt: "2026-10-08T12:00:00Z",
};
const shell = {
  ...row,
  labUnitId: "10",
  samples: [
    {
      id: "sample-1",
      label: "M2-001-1",
      specimenType: "Sputum",
      collectionDate: "2026-10-08T12:00:00Z",
    },
  ],
  pendingSamples: [{ id: "request-1", specimenType: "Blood" }],
  relatedCases: [{ ...row, id: "case-2", labUnit: "TB lab" }],
  canWrite: true,
  transferLabUnits: [{ id: "11", value: "TB lab" }],
};

describe("MVP microbiology case workspace", () => {
  test("does not shift a lab birthday into the previous browser day", async () => {
    const service = {
      getCase: vi
        .fn()
        .mockResolvedValue({ ...shell, birthDate: "1980-01-15T00:00:00Z" }),
    };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    expect(await screen.findByText(/1\/15\/1980/)).toBeVisible();
  });
  test("marks case ready only after a successful shell load", async () => {
    markMicrobiologyReady.mockClear();
    let finish;
    const service = {
      getCase: vi.fn().mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      ),
    };
    renderPage(
      <CaseViewShell service={service} caseId="case-1" />,
      "/Microbiology/cases/case-1?view=workbench",
    );
    expect(markMicrobiologyReady).not.toHaveBeenCalled();
    await act(async () => finish(shell));
    expect(screen.getByText("Test Patient")).toBeVisible();
    expect(markMicrobiologyReady).toHaveBeenCalledWith(
      MICROBIOLOGY_CASE_READY_MARK,
    );
  });
  test("failed case loads never emit the ready mark", async () => {
    markMicrobiologyReady.mockClear();
    const service = {
      getCase: vi.fn().mockRejectedValue(new Error("unavailable")),
    };
    await act(async () =>
      renderPage(<CaseViewShell service={service} caseId="case-1" />),
    );
    expect(markMicrobiologyReady).not.toHaveBeenCalled();
    expect(screen.queryByText("Test Patient")).not.toBeInTheDocument();
  });
  test("lists cases and keeps filters when opening the shell", async () => {
    const service = {
      searchCases: vi.fn().mockResolvedValue({
        rows: [row],
        page: 1,
        pageSize: 20,
        total: 1,
        labUnits: [{ id: "10", value: "Culture lab" }],
      }),
    };
    const history = renderPage(
      <CaseWorklist service={service} />,
      "/Microbiology/worklist?labUnitId=10",
    );
    fireEvent.click(await screen.findByRole("link", { name: "M2-001" }));
    expect(history.location.pathname).toBe("/Microbiology/cases/case-1");
    expect(history.location.search).toBe("?labUnitId=10");
    expect(service.searchCases).toHaveBeenCalledWith("labUnitId=10");
  });
  test("changing the status resets pagination and requests scoped results", async () => {
    const service = {
      searchCases: vi.fn().mockResolvedValue({
        rows: [],
        page: 3,
        pageSize: 20,
        total: 0,
        labUnits: [],
      }),
    };
    const history = renderPage(
      <CaseWorklist service={service} />,
      "/Microbiology/worklist?page=3&labUnitId=10",
    );
    await screen.findByText("No cases match these filters.");
    expect(
      [...screen.getByLabelText("Status").options].map((o) => o.text),
    ).toEqual([
      "All case statuses",
      "In progress",
      "Partial",
      "Final",
      "Amended",
      "Referred",
      "Referral returned",
      "Amendment open",
    ]);
    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "PARTIAL" },
    });
    await waitFor(() =>
      expect(service.searchCases).toHaveBeenLastCalledWith(
        "labUnitId=10&caseStatus=PARTIAL",
      ),
    );
    expect(history.location.search).toBe("?labUnitId=10&caseStatus=PARTIAL");
  });
  test("each case shows its status, tags and STAT under the lab number", async () => {
    const service = {
      searchCases: vi.fn().mockResolvedValue({
        rows: [
          {
            ...row,
            caseStatus: "PARTIAL",
            statusTags: ["REFERRED"],
            stat: true,
          },
        ],
        page: 1,
        pageSize: 20,
        total: 1,
        labUnits: [],
      }),
    };
    renderPage(<CaseWorklist service={service} />);
    const cell = (await screen.findByRole("link", { name: "M2-001" })).closest(
      "td",
    );
    expect(cell).toHaveTextContent("Partial");
    expect(cell).toHaveTextContent("Referred");
    expect(cell).toHaveTextContent("STAT");
    expect(screen.queryByText("Active")).not.toBeInTheDocument();
    expect(
      screen.getAllByRole("columnheader").map((h) => h.textContent),
    ).not.toContain("Status");
  });
  test("the header shows one status with the current step under it and STAT beside the case number", async () => {
    const service = {
      getCase: vi.fn().mockResolvedValue({
        ...shell,
        caseStatus: "FINAL",
        statusTags: ["AMENDMENT_OPEN"],
        stat: true,
        stage: "AMENDED",
        finalReleaseState: "AMENDMENT_IN_PROGRESS",
      }),
    };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    const heading = await screen.findByRole("heading", {
      level: 1,
      name: /Case M2-001/,
    });
    expect(heading).toHaveTextContent("STAT");
    const status = within(
      screen.getByText("Status", { selector: "p.cds--label" }).parentElement,
    );
    expect(status.getByText("Final")).toBeVisible();
    expect(status.getByText("Amendment open")).toBeVisible();
    expect(status.getByText("Amendments")).toBeVisible();
    expect(screen.queryByText("Active")).not.toBeInTheDocument();
    expect(screen.queryByText(/Work stage/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Release state/)).not.toBeInTheDocument();
  });
  test("shows patient, collected and pending samples and related links", async () => {
    const service = { getCase: vi.fn().mockResolvedValue(shell) };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    expect(await screen.findByText("Test Patient")).toBeTruthy();
    expect(screen.getByText("M2-001-1")).toBeTruthy();
    expect(screen.getByText("Awaiting collection")).toBeTruthy();
    expect(
      screen
        .getByRole("link", { name: /M2-001 · TB lab/ })
        .getAttribute("href"),
    ).toBe("/Microbiology/cases/case-2");
  });
  test("transfer updates the owning unit only after success", async () => {
    const service = {
      getCase: vi.fn().mockResolvedValue(shell),
      transferCase: vi.fn().mockResolvedValue({
        ...shell,
        labUnit: "TB lab",
        labUnitId: "11",
        transferLabUnits: [],
      }),
    };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    await screen.findByText("Test Patient");
    fireEvent.change(screen.getByLabelText("Destination lab unit"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Transfer case" }));
    await waitFor(() =>
      expect(service.transferCase).toHaveBeenCalledWith("case-1", "11"),
    );
    await screen.findByText("TB lab");
    expect(screen.queryByText("Culture lab")).toBeNull();
  });
  test("a completed transfer cannot overwrite a different case after navigation", async () => {
    let finish;
    const service = {
      getCase: vi.fn().mockImplementation((id) =>
        Promise.resolve({
          ...shell,
          id,
          patientName: id === "case-2" ? "Second Patient" : "Test Patient",
        }),
      ),
      transferCase: vi.fn().mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      ),
    };
    renderPage(
      <Route path="/Microbiology/cases/:caseId">
        <CaseViewShell service={service} />
      </Route>,
      "/Microbiology/cases/case-1",
    );
    await screen.findByText("Test Patient");
    fireEvent.change(screen.getByLabelText("Destination lab unit"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Transfer case" }));
    fireEvent.click(screen.getByRole("link", { name: /M2-001 · TB lab/ }));
    await screen.findByText("Second Patient");
    finish({ ...shell, labUnit: "TB lab" });
    await waitFor(() => expect(screen.queryByText("Test Patient")).toBeNull());
    expect(screen.getByText("Second Patient")).toBeTruthy();
  });
  test("a transfer from a previous visit cannot overwrite a case revisited through another case", async () => {
    let finish;
    const service = {
      getCase: vi.fn().mockImplementation((id) =>
        Promise.resolve({
          ...shell,
          id,
          patientName: id === "case-2" ? "Second Patient" : "Test Patient",
        }),
      ),
      transferCase: vi.fn().mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      ),
    };
    const history = renderPage(
      <Route path="/Microbiology/cases/:caseId">
        <CaseViewShell service={service} />
      </Route>,
      "/Microbiology/cases/case-1",
    );
    await screen.findByText("Test Patient");
    fireEvent.change(screen.getByLabelText("Destination lab unit"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Transfer case" }));
    act(() => history.push("/Microbiology/cases/case-2"));
    await screen.findByText("Second Patient");
    act(() => history.push("/Microbiology/cases/case-1"));
    await screen.findByText("Test Patient");
    await act(async () =>
      finish({
        ...shell,
        patientName: "Stale transferred patient",
        labUnit: "TB lab",
      }),
    );
    expect(screen.queryByText("Stale transferred patient")).toBeNull();
    expect(screen.getByText("Test Patient")).toBeTruthy();
    expect(screen.getByText("Culture lab")).toBeTruthy();
  });
  test("validation readers do not receive transfer controls", async () => {
    renderPage(
      <CaseViewShell
        caseId="case-1"
        service={{
          getCase: vi.fn().mockResolvedValue({ ...shell, canWrite: false }),
        }}
      />,
    );
    await screen.findByText("Test Patient");
    expect(screen.queryByRole("button", { name: "Transfer case" })).toBeNull();
  });
  test("failed access shows an error and never stale case data", async () => {
    renderPage(
      <CaseViewShell
        caseId="case-1"
        service={{ getCase: vi.fn().mockRejectedValue(new Error("denied")) }}
      />,
    );
    expect(
      await screen.findByText(messages["microbiology.case.loadError"]),
    ).toBeTruthy();
    expect(screen.queryByText("Test Patient")).toBeNull();
  });
});

test("related cases retain worklist filters but discard the previous case's action target", async () => {
  const service = { getCase: vi.fn().mockResolvedValue(shell) };
  renderPage(
    <CaseViewShell service={service} caseId="case-1" />,
    "/Microbiology/cases/case-1?labUnitId=10&scope=CRITICAL&action=log-critical&targetType=CASE&targetId=case-1&astView=reviewed&astRunId=run-1",
  );
  expect(
    await screen.findByRole("link", { name: /M2-001 · TB lab/ }),
  ).toHaveAttribute("href", "/Microbiology/cases/case-2?labUnitId=10");
});

test.each([
  [[Roles.RESULTS], false],
  [[Roles.RESULTS, Roles.RECEPTION], true],
  [[Roles.GLOBAL_ADMIN], false],
])(
  "sample correction follows the Reception route gate for roles %j",
  async (roles, allowed) => {
    const service = {
      getCase: vi.fn().mockResolvedValue({ ...shell, orderId: "42" }),
    };
    renderPage(
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles } }}
      >
        <CaseViewShell service={service} caseId="case-1" />
      </UserSessionDetailsContext.Provider>,
      "/Microbiology/cases/case-1",
    );
    await screen.findByText("Test Patient");
    const link = screen.queryByRole("link", {
      name: messages["microbiology.case.correctSample"],
    });
    if (allowed)
      expect(link).toHaveAttribute(
        "href",
        "/ModifyOrder?accessionNumber=M2-001",
      );
    else expect(link).not.toBeInTheDocument();
  },
);

const barService = (
  readiness = { blockers: [], finalReleaseReady: true, pendingIncomingIds: [] },
) => ({
  getCase: vi.fn().mockResolvedValue({
    ...shell,
    caseStatus: "PARTIAL",
    statusTags: [],
    stat: true,
    canValidate: true,
  }),
  getReadiness: vi.fn().mockResolvedValue(readiness),
  getReport: vi.fn().mockResolvedValue({ validatedResults: 3 }),
  getReportVersions: vi.fn().mockResolvedValue([{ id: "v1" }]),
  releaseReport: vi.fn().mockResolvedValue({}),
});

describe("case bar", () => {
  test("names the case, its status and STAT, and opens a section from its links", async () => {
    const history = renderPage(
      <CaseViewShell service={barService()} caseId="case-1" />,
      "/Microbiology/cases/case-1",
    );
    const bar = await screen.findByRole("region", { name: "Case summary" });
    expect(bar).toHaveTextContent("M2-001");
    expect(bar).toHaveTextContent("Test Patient");
    expect(bar).toHaveTextContent("Partial");
    expect(bar).toHaveTextContent("STAT");
    fireEvent.click(
      within(bar).getByRole("button", { name: "Report and release" }),
    );
    expect(new URLSearchParams(history.location.search).get("scope")).toBe(
      "REPORT",
    );
  });

  test("says how many items keep final release blocked", async () => {
    renderPage(
      <CaseViewShell
        service={barService({
          blockers: ["ISOLATE_REQUIRED", "PROGRAM_REQUIRED"],
          finalReleaseReady: false,
          pendingIncomingIds: [],
        })}
        caseId="case-1"
      />,
    );
    const bar = await screen.findByRole("region", { name: "Case summary" });
    expect(
      await within(bar).findByRole("button", {
        name: "Release final report (2 missing)",
      }),
    ).toBeDisabled();
  });

  test("final release asks for confirmation and says what will happen", async () => {
    const service = barService();
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    const bar = await screen.findByRole("region", { name: "Case summary" });
    fireEvent.click(
      await within(bar).findByRole("button", { name: "Release final report" }),
    );
    const dialog = await screen.findByRole("dialog", {
      name: "Release final report?",
    });
    expect(
      await within(dialog).findByText(
        "The case will be locked. 3 results will be released as report version 2. Any later change needs an amendment.",
      ),
    ).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(service.releaseReport).not.toHaveBeenCalled();
    fireEvent.click(
      within(bar).getByRole("button", { name: "Release final report" }),
    );
    const again = await screen.findByRole("dialog", {
      name: "Release final report?",
    });
    fireEvent.click(
      await within(again).findByRole("button", { name: "Release final" }),
    );
    await waitFor(() =>
      expect(service.releaseReport).toHaveBeenCalledWith("case-1", "final", []),
    );
    await waitFor(() => expect(service.getCase).toHaveBeenCalledTimes(2));
  });

  test("partial release stays one click", async () => {
    const service = barService();
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    const bar = await screen.findByRole("region", { name: "Case summary" });
    fireEvent.click(
      await within(bar).findByRole("button", {
        name: "Release partial report",
      }),
    );
    await waitFor(() =>
      expect(service.releaseReport).toHaveBeenCalledWith(
        "case-1",
        "preliminary",
        [],
      ),
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
