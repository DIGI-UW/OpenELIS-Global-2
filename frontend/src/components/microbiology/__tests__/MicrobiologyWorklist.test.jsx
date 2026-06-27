import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import MicrobiologyWorklist from "../MicrobiologyWorklist";
import messages from "../../../languages/en.json";

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

    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <MicrobiologyWorklist service={service} />
        </IntlProvider>
      </MemoryRouter>,
    );

    expect(
      await screen.findByText("Microbiology worklist"),
    ).toBeInTheDocument();
    expect(screen.getByText("AST_REVIEW")).toBeInTheDocument();
    expect(screen.getByText("Critical communication")).toBeInTheDocument();
    expect(
      screen.getByTestId("microbiology-worklist-siblings"),
    ).toHaveTextContent("MYCOBACTERIOLOGY_TB");
  });
});
