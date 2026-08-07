import React from "react";
import { act, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import MicrobiologyWorklist from "../MicrobiologyWorklist";
import messages from "../../../languages/en.json";

const renderWorklist = (service, initialEntry = "/Microbiology/worklist") =>
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <IntlProvider locale="en" messages={messages}>
        <MicrobiologyWorklist service={service} />
        <Route
          render={({ location }) => (
            <output data-testid="microbiology-current-url">
              {location.pathname}
              {location.search}
            </output>
          )}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("MicrobiologyWorklist", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows due action, critical communication, and sibling workflows", async () => {
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            caseId: "case-1",
            sampleItemId: "1001",
            accessionNumber: "LAB-1001",
            patientDisplay: "Mendez, Olivia",
            specimenDisplay: "Blood",
            workflowType: "BACTERIOLOGY",
            stage: "AST_IN_PROGRESS",
            dueAction: "AST_REVIEW",
            urgency: "HIGH",
            priority: "STAT",
            lastActivityBy: "Morgan Lee",
            lastActivityAt: "2026-08-06T09:30:00Z",
            needsAstReview: true,
            hasOpenCriticalCommunication: true,
            siblingWorkflows: ["MYCOBACTERIOLOGY_TB"],
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
        summary: {
          totalPending: 4,
          incubating: 1,
          growthDetected: 1,
          identification: 0,
          needsAstReview: 1,
          readyForCaseReview: 1,
          openCriticalFollowUps: 1,
          resistanceHits: {
            ESBL: 2,
            MRSA: 1,
            CRE: 0,
            VRE: 0,
            MDR: 1,
          },
        },
        recentActivity: [
          {
            caseId: "case-1",
            accessionNumber: "LAB-1001",
            activityType: "AST_REVIEWED",
            note: "AST run reviewed",
            occurredAt: "2026-08-06T09:30:00Z",
            performedByDisplay: "Morgan Lee",
          },
        ],
      }),
    };

    renderWorklist(service);

    expect(
      await screen.findByRole("heading", { name: "Microbiology worklist" }),
    ).toBeInTheDocument();
    expect(service.getWorklistRows).toHaveBeenCalledWith({
      grain: "cultures",
      status: "",
      workflow: "",
      stage: "",
      urgency: "",
      due: "",
      q: "",
      sort: "priority",
      page: 1,
      pageSize: 20,
    });
    const worklistRow = screen.getByTestId("microbiology-worklist-row-case-1");
    expect(screen.getByRole("columnheader", { name: "Lab #" })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Patient" })).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Specimen" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Last activity by" }),
    ).toBeVisible();
    expect(worklistRow).toHaveTextContent("LAB-1001");
    expect(worklistRow).toHaveTextContent("Mendez, Olivia");
    expect(worklistRow).toHaveTextContent("Blood");
    expect(worklistRow).toHaveTextContent("Morgan Lee");
    expect(worklistRow).toHaveTextContent("Linked · 2 workflows");
    expect(worklistRow).toHaveTextContent("AST Review");
    expect(worklistRow).toHaveTextContent("Critical communication");
    expect(
      screen.getByTestId("microbiology-worklist-siblings"),
    ).toHaveTextContent("Linked · 2 workflows");
    expect(
      screen.getByTestId("microbiology-worklist-summary-growth"),
    ).toHaveTextContent("Growth detected");
    expect(
      screen.getByTestId("microbiology-worklist-summary-critical"),
    ).toHaveTextContent("Open critical follow-ups");
    expect(
      screen.getByRole("heading", { name: "Today's resistance hits" }),
    ).toBeVisible();
    expect(
      screen.getByTestId("microbiology-resistance-hit-ESBL"),
    ).toHaveTextContent("2");
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: /Recent activity/ }));
    expect(screen.getByText("AST run reviewed")).toBeVisible();
    expect(screen.getAllByText("Morgan Lee")).toHaveLength(2);
  });

  it("shows day-aware incubation detail and the accurate no-timing fallback", async () => {
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            caseId: "case-timed",
            sampleItemId: "1001",
            workflowType: "BACTERIOLOGY",
            stage: "INCUBATING",
            dueAction: "INCUBATING",
            incubationDay: 2,
            maxIncubationDays: 5,
            urgency: "ROUTINE",
            siblingWorkflows: [],
          },
          {
            caseId: "case-fallback",
            sampleItemId: "1002",
            workflowType: "BACTERIOLOGY",
            stage: "INCUBATING",
            dueAction: "INCUBATING",
            urgency: "ROUTINE",
            siblingWorkflows: [],
          },
        ],
        total: 2,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(service);

    const timed = await screen.findByTestId(
      "microbiology-worklist-row-case-timed",
    );
    const fallback = screen.getByTestId(
      "microbiology-worklist-row-case-fallback",
    );
    expect(timed).toHaveTextContent("Incubating");
    expect(timed).toHaveTextContent("Day 2 of 5");
    expect(fallback).toHaveTextContent("Incubating");
    expect(fallback).not.toHaveTextContent(/Day \d+ of \d+/);
  });

  it("preserves worklist filters when opening a case", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            caseId: "case-1",
            sampleItemId: "1001",
            workflowType: "BACTERIOLOGY",
            stage: "AST_IN_PROGRESS",
            dueAction: "AST_REVIEW",
            urgency: "HIGH",
            needsAstReview: true,
            hasOpenCriticalCommunication: false,
            siblingWorkflows: [],
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(
      service,
      "/Microbiology/worklist?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
    );

    await screen.findByRole("heading", { name: "Microbiology worklist" });
    await user.click(screen.getByRole("button", { name: "Row actions" }));
    await user.click(await screen.findByText("Open case"));

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
      ),
    );
  });

  it("canonicalizes filter changes in the URL and re-queries the server", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [],
        total: 0,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(service);

    await screen.findByRole("heading", { name: "Microbiology worklist" });
    await user.selectOptions(screen.getByLabelText("Stage"), "AST_IN_PROGRESS");

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?stage=AST_IN_PROGRESS",
      ),
    );
    await waitFor(() =>
      expect(service.getWorklistRows).toHaveBeenLastCalledWith({
        grain: "cultures",
        status: "",
        workflow: "",
        stage: "AST_IN_PROGRESS",
        urgency: "",
        due: "",
        q: "",
        sort: "priority",
        page: 1,
        pageSize: 20,
      }),
    );
  });

  it("keeps the search control mounted while filtered rows revalidate", async () => {
    const user = userEvent.setup();
    const pendingResponse = new Promise(() => {});
    const service = {
      getWorklistRows: vi
        .fn()
        .mockResolvedValueOnce({ rows: [], total: 0, page: 1, pageSize: 20 })
        .mockReturnValue(pendingResponse),
    };

    renderWorklist(service);

    const search = await screen.findByPlaceholderText(
      "Search lab number, patient, specimen, or workflow",
    );
    await user.type(search, "1");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?q=1",
      ),
    );
    expect(
      screen.getByPlaceholderText(
        "Search lab number, patient, specimen, or workflow",
      ),
    ).toBe(search);

    await user.type(search, "2");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?q=12",
      ),
    );
    expect(
      screen.getByPlaceholderText(
        "Search lab number, patient, specimen, or workflow",
      ),
    ).toBe(search);
  });

  it("refreshes at 30 seconds without losing URL, focus, or table scroll", async () => {
    vi.useFakeTimers();
    const worklistRow = (caseId, stage = "INCUBATING") => ({
      caseId,
      sampleItemId: caseId,
      accessionNumber: `LAB-${caseId}`,
      workflowType: "BACTERIOLOGY",
      stage,
      dueAction: stage === "POSITIVE_SIGNAL" ? "CONFIRM_GROWTH" : "SETUP",
      urgency: "ROUTINE",
      siblingWorkflows: [],
    });
    const service = {
      getWorklistRows: vi
        .fn()
        .mockResolvedValueOnce({
          rows: [worklistRow("1001")],
          total: 1,
          page: 1,
          pageSize: 20,
        })
        .mockResolvedValueOnce({
          rows: [worklistRow("1001"), worklistRow("1002", "POSITIVE_SIGNAL")],
          total: 2,
          page: 1,
          pageSize: 20,
        }),
    };

    renderWorklist(
      service,
      "/Microbiology/worklist?workflow=BACTERIOLOGY&sort=newest",
    );
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });

    const search = screen.getByPlaceholderText(
      "Search lab number, patient, specimen, or workflow",
    );
    const tableScroll = screen.getByTestId(
      "microbiology-worklist-table-scroll",
    );
    search.focus();
    tableScroll.scrollLeft = 144;
    const canonicalUrl = screen.getByTestId(
      "microbiology-current-url",
    ).textContent;

    await act(async () => {
      await vi.advanceTimersByTimeAsync(29_999);
    });
    expect(service.getWorklistRows).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1);
    });

    expect(service.getWorklistRows).toHaveBeenCalledTimes(2);
    expect(
      screen.getByTestId("microbiology-worklist-row-1002"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("microbiology-worklist-row-1002")).toHaveClass(
      "microbiology-worklist__row--new-positive",
    );
    expect(screen.getByText("New positive")).toBeVisible();
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      canonicalUrl,
    );
    expect(document.activeElement).toBe(search);
    expect(tableScroll.scrollLeft).toBe(144);
    expect(screen.getByText(/Updated 0s ago/)).toBeVisible();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000);
    });
    expect(screen.queryByText("New positive")).not.toBeInTheDocument();
    expect(
      screen.getByTestId("microbiology-worklist-row-1002"),
    ).not.toHaveClass("microbiology-worklist__row--new-positive");
  });

  it("opens a culture case when a non-interactive row cell is clicked", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            caseId: "case-1",
            sampleItemId: "1001",
            accessionNumber: "LAB-1001",
            patientDisplay: "Mendez, Olivia",
            workflowType: "BACTERIOLOGY",
            stage: "INCUBATING",
            dueAction: "SETUP",
            urgency: "ROUTINE",
            siblingWorkflows: [],
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(service, "/Microbiology/worklist?status=incubating");

    const row = await screen.findByTestId("microbiology-worklist-row-case-1");
    await user.click(within(row).getByText("Mendez, Olivia"));

    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "/Microbiology/cases/case-1?status=incubating",
    );
  });

  it("opens the exact AST context when its row is keyboard-activated", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            rowId: "run-1",
            grain: "ast",
            caseId: "case-1",
            sampleItemId: "1001",
            workflowType: "BACTERIOLOGY",
            isolateId: "isolate-1",
            isolateLabel: "Isolate 1",
            astRunId: "run-1",
            astStatus: "RESULTS_IN",
            urgency: "HIGH",
            siblingWorkflows: [],
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(
      service,
      "/Microbiology/worklist?grain=ast&status=results-in",
    );

    const row = await screen.findByTestId("microbiology-worklist-row-run-1");
    row.focus();
    expect(row).toHaveFocus();
    await user.keyboard("{Enter}");

    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "/Microbiology/cases/case-1?grain=ast&status=results-in&section=ast&astIsolateId=isolate-1&astRunId=run-1",
    );
  });

  it("reconciles Carbon rows when a filtered response replaces row IDs", async () => {
    const user = userEvent.setup();
    const worklistRow = (caseId, sampleItemId) => ({
      caseId,
      sampleItemId,
      workflowType: "BACTERIOLOGY",
      stage: "RECEIVED",
      dueAction: "SETUP",
      urgency: "ROUTINE",
      needsAstReview: false,
      hasOpenCriticalCommunication: false,
      siblingWorkflows: [],
    });
    const service = {
      getWorklistRows: vi.fn().mockImplementation((filters) =>
        Promise.resolve(
          filters.q === "2002"
            ? {
                rows: [worklistRow("case-2", "2002")],
                total: 1,
                page: 1,
                pageSize: 20,
              }
            : {
                rows: [worklistRow("case-1", "1001")],
                total: 1,
                page: 1,
                pageSize: 20,
              },
        ),
      ),
    };

    renderWorklist(service);

    expect(
      await screen.findByTestId("microbiology-worklist-row-case-1"),
    ).toBeInTheDocument();
    await user.type(
      screen.getByPlaceholderText(
        "Search lab number, patient, specimen, or workflow",
      ),
      "2002",
    );

    expect(
      await screen.findByTestId("microbiology-worklist-row-case-2"),
    ).toBeInTheDocument();
  });

  it("uses action summary tiles to set canonical worklist state", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [],
        total: 0,
        page: 1,
        pageSize: 20,
        summary: {
          totalPending: 3,
          incubating: 1,
          growthDetected: 0,
          identification: 0,
          needsAstReview: 1,
          readyForCaseReview: 1,
          openCriticalFollowUps: 0,
        },
      }),
    };

    renderWorklist(
      service,
      "/Microbiology/worklist?workflow=BACTERIOLOGY&q=blood",
    );

    await screen.findByRole("heading", { name: "Microbiology worklist" });
    await user.click(
      screen.getByTestId("microbiology-worklist-summary-growth"),
    );

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?status=growth&workflow=BACTERIOLOGY&q=blood",
      ),
    );
  });

  it("switches to the AST grain and opens the exact run with queue state", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockImplementation(({ grain }) =>
        Promise.resolve(
          grain === "ast"
            ? {
                rows: [
                  {
                    rowId: "run-1",
                    grain: "ast",
                    caseId: "case-1",
                    sampleItemId: "1001",
                    accessionNumber: "LAB-1001",
                    patientDisplay: "Mendez, Olivia",
                    workflowType: "BACTERIOLOGY",
                    priority: "STAT",
                    urgency: "HIGH",
                    isolateId: "isolate-1",
                    isolateLabel: "Isolate 1",
                    organismDisplay: "E. coli",
                    astRunId: "run-1",
                    panelId: "GN-STD",
                    panelName: "Gram-negative standard panel",
                    astStatus: "RESULTS_IN",
                    astStartedAt: "2026-08-06T09:30:00Z",
                    analyzerResultsAvailable: true,
                    analyzerExpertFlags: "ESBL|MDR",
                  },
                ],
                total: 1,
                page: 1,
                pageSize: 20,
                summary: {
                  astInQueue: 1,
                  astPendingSetup: 0,
                  astInProgress: 0,
                  astAwaitingResults: 0,
                  astResultsIn: 1,
                },
              }
            : { rows: [], total: 0, page: 1, pageSize: 20 },
        ),
      ),
    };

    renderWorklist(service);

    await screen.findByRole("heading", { name: "Microbiology worklist" });
    await user.click(screen.getByRole("tab", { name: "AST runs" }));

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?grain=ast",
      ),
    );
    const row = await screen.findByTestId("microbiology-worklist-row-run-1");
    expect(row).toHaveTextContent("Isolate 1");
    expect(row).toHaveTextContent("E. coli");
    expect(row).toHaveTextContent("LAB-1001");
    expect(row).toHaveTextContent("Mendez, Olivia");
    expect(row).toHaveTextContent("Gram-negative standard panel");
    expect(row).toHaveTextContent("Results In");
    expect(row).toHaveTextContent("ESBL");
    expect(row).toHaveTextContent("MDR");
    expect(screen.getByRole("columnheader", { name: "Flags" })).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Export to WHONET (Phase 1B)" }),
    ).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Row actions" }));
    await user.click(await screen.findByText("Open case"));
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?grain=ast&section=ast&astIsolateId=isolate-1&astRunId=run-1",
      ),
    );
  });

  it.each([
    ["Mark positive", "mark-positive"],
    ["Mark no growth", "mark-no-growth"],
  ])(
    "navigates the %s culture row action with the keyboard without mutating the queue",
    async (actionLabel, action) => {
      const user = userEvent.setup();
      const service = {
        recordCaseActivity: vi.fn(),
        getWorklistRows: vi.fn().mockResolvedValue({
          rows: [
            {
              rowId: "case-1",
              caseId: "case-1",
              sampleItemId: "1001",
              workflowType: "BACTERIOLOGY",
              stage: "INCUBATING",
              dueAction: "INCUBATING",
              urgency: "ROUTINE",
              siblingWorkflows: [],
            },
          ],
          total: 1,
          page: 1,
          pageSize: 20,
        }),
      };

      renderWorklist(service, "/Microbiology/worklist?status=incubating");

      const rowActions = await screen.findByRole("button", {
        name: "Row actions",
      });
      rowActions.focus();
      await user.keyboard("{Enter}");
      const actionText = await screen.findByText(actionLabel);
      const actionItem = actionText.closest("button");
      expect(actionItem).not.toBeNull();
      actionItem.focus();
      await user.keyboard("{Enter}");

      expect(service.recordCaseActivity).not.toHaveBeenCalled();
      await waitFor(() =>
        expect(
          screen.getByTestId("microbiology-current-url"),
        ).toHaveTextContent(
          `/Microbiology/cases/case-1?status=incubating&section=setup&action=${action}`,
        ),
      );
    },
  );

  it("opens the preserved new-attempt flow for a reviewed AST run", async () => {
    const user = userEvent.setup();
    const service = {
      startRepeatAstRun: vi.fn(),
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [
          {
            rowId: "run-1",
            caseId: "case-1",
            sampleItemId: "1001",
            workflowType: "BACTERIOLOGY",
            stage: "REVIEW_READY",
            dueAction: "CASE_REVIEW",
            urgency: "ROUTINE",
            isolateId: "isolate-1",
            astRunId: "run-1",
            astStatus: "REVIEWED",
            siblingWorkflows: [],
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
      }),
    };

    renderWorklist(service, "/Microbiology/worklist?grain=ast");

    await user.click(
      await screen.findByRole("button", { name: "Row actions" }),
    );
    await user.click(await screen.findByText("Set up new AST run"));

    expect(service.startRepeatAstRun).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?grain=ast&section=ast&astIsolateId=isolate-1&astRunId=run-1&action=new-ast-attempt",
      ),
    );
  });

  it("exposes an AST summary card as a keyboard-operable canonical link", async () => {
    const user = userEvent.setup();
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue({
        rows: [],
        total: 0,
        page: 1,
        pageSize: 20,
        summary: {
          astInQueue: 3,
          astPendingSetup: 1,
          astInProgress: 1,
          astResultsIn: 1,
        },
      }),
    };

    renderWorklist(service, "/Microbiology/worklist?grain=ast");

    const resultsIn = await screen.findByRole("link", {
      name: "Results in - review",
    });
    expect(resultsIn).toHaveAttribute(
      "href",
      "/Microbiology/worklist?grain=ast&status=results-in",
    );
    resultsIn.focus();
    expect(resultsIn).toHaveFocus();
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?grain=ast&status=results-in",
      ),
    );
  });
});
