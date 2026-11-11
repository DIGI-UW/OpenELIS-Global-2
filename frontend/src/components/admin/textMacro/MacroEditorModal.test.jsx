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
          category: "",
          contexts: ["MICROBIOLOGY_CULTURE_READING"],
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
    await user.selectOptions(screen.getByLabelText("Category"), "CULTURE");
    await user.type(
      screen.getByLabelText("Phrase text"),
      "No growth at 24 hours",
    );
    await user.click(screen.getByLabelText("Clinical history"));
    await user.click(screen.getByRole("switch", { name: "Status" }));
    await user.click(screen.getByRole("button", { name: "Save phrase" }));

    expect(onSave).toHaveBeenCalledWith({
      code: "ng24",
      expansionText: "No growth at 24 hours",
      category: "CULTURE",
      contexts: [
        "MICROBIOLOGY_CULTURE_READING",
        "MICROBIOLOGY_CLINICAL_HISTORY",
      ],
      active: false,
    });
  });

  it("offers every V2 field context and no removed V1 context", () => {
    renderEditor();

    expect(
      screen
        .getAllByRole("checkbox")
        .map((checkbox) => checkbox.labels[0].textContent),
    ).toEqual([
      "Clinical history",
      "Case notes",
      "Culture reading notes",
      "Override reasons",
      "Amendment reasons",
    ]);
    expect(screen.queryByLabelText("Antibiotic exposure")).toBeNull();
  });

  it("shows where a category is meant to be used once chosen", async () => {
    const user = userEvent.setup();
    renderEditor();

    expect(
      screen.getByText(
        "The kind of phrase. Field contexts decide where it is offered.",
      ),
    ).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Category"), "AST");

    expect(
      screen.getByText("Susceptibility comments and override decisions."),
    ).toBeInTheDocument();
  });

  it("keeps save disabled until code, category, text, and context are complete", async () => {
    const user = userEvent.setup();
    renderEditor();
    const save = screen.getByRole("button", { name: "Save phrase" });

    await user.type(screen.getByLabelText("Shortcut code"), ".abc");
    await user.type(screen.getByLabelText("Phrase text"), "Text");
    expect(save).toBeDisabled();
    await user.selectOptions(screen.getByLabelText("Category"), "CLINICAL");
    expect(save).toBeEnabled();
  });

  it("stops phrase text at 500 characters and shows the count", async () => {
    const user = userEvent.setup();
    renderEditor();
    const text = screen.getByLabelText("Phrase text");

    expect(text).toHaveAttribute("maxlength", "500");
    expect(screen.getByText("0/500")).toBeInTheDocument();
    await user.click(text);
    await user.paste("x".repeat(600));

    expect(text.value).toHaveLength(500);
    expect(screen.getByText("500/500")).toBeInTheDocument();
  });

  it("shows a reserved-code rejection on the code field", () => {
    renderEditor({
      error: {
        code: "MACRO_CODE_RESERVED",
        message: "This code is reserved by the system.",
      },
    });

    expect(screen.getByLabelText("Shortcut code")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(
      screen.getByText("This code is reserved by the system."),
    ).toBeInTheDocument();
  });

  it("shows other server validation inside the dialog", () => {
    renderEditor({
      error: {
        code: "MACRO_TEXT_TOO_LONG",
        message: "Phrase text must be 500 characters or fewer.",
      },
    });

    expect(
      screen.getByText("Phrase text must be 500 characters or fewer."),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Shortcut code")).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });
});
