import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import MacroEditorModal from "./MacroEditorModal";

const renderEditor = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MacroEditorModal
        open
        mode="create"
        value={{
          code: "",
          expansionText: "",
          contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
          active: true,
        }}
        saving={false}
        onClose={vi.fn()}
        onSave={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

describe("MacroEditorModal", () => {
  it("uses labeled Carbon controls and returns the complete edited value", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    renderEditor({ onSave });

    await user.type(screen.getByLabelText("Shortcut code"), "ng24");
    await user.type(
      screen.getByRole("textbox", { name: "Phrase text" }),
      "No growth at 24 hours",
    );
    await user.click(screen.getByLabelText("Clinical history"));
    await user.click(screen.getByRole("button", { name: "Save phrase" }));

    expect(onSave).toHaveBeenCalledWith({
      code: "ng24",
      expansionText: "No growth at 24 hours",
      contexts: [
        "MICROBIOLOGY_CULTURE_ACTIVITY",
        "MICROBIOLOGY_CLINICAL_HISTORY",
      ],
      active: true,
    });
  });

  it("keeps save disabled until required fields are complete", () => {
    renderEditor();

    expect(screen.getByRole("button", { name: "Save phrase" })).toBeDisabled();
  });
});
