import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
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
      getWorklistRows: vi.fn().mockResolvedValue([
        {
          caseId: "case-1",
          sampleItemId: "1001",
          workflowType: "BACTERIOLOGY",
          dueAction: "AST_REVIEW",
          urgency: "HIGH",
          needsAstReview: true,
          hasOpenCriticalCommunication: true,
          siblingWorkflows: ["MYCOBACTERIOLOGY_TB"],
        },
      ]),
    };

    renderWorklist(service);

    expect(
      await screen.findByText("Microbiology worklist"),
    ).toBeInTheDocument();
    const worklistRow = screen.getByTestId("microbiology-worklist-row-case-1");
    expect(worklistRow).toHaveTextContent("AST Review");
    expect(worklistRow).toHaveTextContent("Critical communication");
    expect(
      screen.getByTestId("microbiology-worklist-siblings"),
    ).toHaveTextContent("Mycobacteriology Tb");
  });

  it("preserves worklist filters when opening a case", async () => {
    const service = {
      getWorklistRows: vi.fn().mockResolvedValue([
        {
          caseId: "case-1",
          sampleItemId: "1001",
          workflowType: "BACTERIOLOGY",
          dueAction: "AST_REVIEW",
          urgency: "HIGH",
          needsAstReview: true,
          hasOpenCriticalCommunication: false,
          siblingWorkflows: [],
        },
      ]),
    };

    renderWorklist(
      service,
      "/Microbiology/worklist?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
    );

    await screen.findByText("Microbiology worklist");
    fireEvent.click(screen.getByRole("button", { name: "Open case" }));

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
      ),
    );
  });
});
