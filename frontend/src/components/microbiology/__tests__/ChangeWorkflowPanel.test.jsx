import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import ChangeWorkflowPanel from "../ChangeWorkflowPanel";
import messages from "../../../languages/en.json";

const renderPanel = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ChangeWorkflowPanel {...props} />
    </IntlProvider>,
  );

describe("ChangeWorkflowPanel", () => {
  it("classifies an unassigned case with a compatible Method and reason", async () => {
    const user = userEvent.setup();
    const service = {
      getCultureMethods: vi
        .fn()
        .mockResolvedValue([
          { id: "method-1", label: "Routine blood culture" },
        ]),
      changeCaseWorkflow: vi.fn().mockResolvedValue({
        workflowType: "BACTERIOLOGY",
        cultureMethodId: "method-1",
      }),
    };
    const onChanged = vi.fn();

    renderPanel({
      caseId: "case-1",
      workflowType: "UNASSIGNED",
      cultureMethodId: "",
      requiresConfirmation: false,
      service,
      onChanged,
    });

    await user.selectOptions(screen.getByLabelText("Workflow"), "BACTERIOLOGY");
    await user.selectOptions(
      await screen.findByLabelText("Culture Method"),
      "method-1",
    );
    await user.type(
      screen.getByLabelText("Reason for change"),
      "Correct routing",
    );
    await user.click(screen.getByRole("button", { name: "Apply workflow" }));

    expect(service.changeCaseWorkflow).toHaveBeenCalledWith("case-1", {
      workflowType: "BACTERIOLOGY",
      cultureMethodId: "method-1",
      reason: "Correct routing",
      preserveExistingWorkConfirmed: false,
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it("requires explicit preservation confirmation when clinical work exists", async () => {
    const user = userEvent.setup();
    const service = {
      getCultureMethods: vi
        .fn()
        .mockResolvedValue([{ id: "method-2", label: "TB culture" }]),
      changeCaseWorkflow: vi.fn().mockResolvedValue({}),
    };

    renderPanel({
      caseId: "case-1",
      workflowType: "BACTERIOLOGY",
      cultureMethodId: "method-1",
      requiresConfirmation: true,
      service,
      onChanged: vi.fn(),
    });

    await user.selectOptions(
      screen.getByLabelText("Workflow"),
      "MYCOBACTERIOLOGY_TB",
    );
    await user.selectOptions(
      await screen.findByLabelText("Culture Method"),
      "method-2",
    );
    await user.type(
      screen.getByLabelText("Reason for change"),
      "Corrected after setup",
    );

    expect(
      screen.getByRole("button", { name: "Apply workflow" }),
    ).toBeDisabled();
    await user.click(
      screen.getByLabelText(
        /preserve the existing setup, isolate, and AST history/i,
      ),
    );
    expect(
      screen.getByRole("button", { name: "Apply workflow" }),
    ).toBeEnabled();
  });
});
