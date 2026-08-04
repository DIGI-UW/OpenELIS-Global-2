import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
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
  it("shows due action, critical communication, and sibling workflows", async () => {
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
        },
      }),
    };

    renderWorklist(service);

    expect(
      await screen.findByRole("heading", { name: "Microbiology worklist" }),
    ).toBeInTheDocument();
    expect(service.getWorklistRows).toHaveBeenCalledWith({
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
    expect(worklistRow).toHaveTextContent("AST Review");
    expect(worklistRow).toHaveTextContent("Critical communication");
    expect(
      screen.getByTestId("microbiology-worklist-siblings"),
    ).toHaveTextContent("Mycobacteriology Tb");
    expect(
      screen.getByTestId("microbiology-worklist-summary-ast-review"),
    ).toHaveTextContent("AST review");
    expect(
      screen.getByTestId("microbiology-worklist-summary-critical"),
    ).toHaveTextContent("Open critical follow-ups");
  });

  it("preserves worklist filters when opening a case", async () => {
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
    fireEvent.click(screen.getByRole("button", { name: "Open case" }));

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
      "Search sample or workflow",
    );
    await user.type(search, "1");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?q=1",
      ),
    );
    expect(screen.getByPlaceholderText("Search sample or workflow")).toBe(
      search,
    );

    await user.type(search, "2");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?q=12",
      ),
    );
    expect(screen.getByPlaceholderText("Search sample or workflow")).toBe(
      search,
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
      screen.getByPlaceholderText("Search sample or workflow"),
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
      screen.getByTestId("microbiology-worklist-summary-ast-review"),
    );

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?workflow=BACTERIOLOGY&due=AST_REVIEW&q=blood",
      ),
    );
  });
});
