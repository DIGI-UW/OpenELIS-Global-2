vi.mock("./TextMacroService", () => ({
  getTextMacros: vi.fn(),
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import MacroTextArea from "./MacroTextArea";
import { getTextMacros } from "./TextMacroService";

const macros = [
  {
    id: "gpc",
    code: ".gpc",
    expansionText: "Gram-positive cocci",
    category: "GRAM_STAIN",
    contexts: ["MICROBIOLOGY_CULTURE_READING"],
  },
  {
    id: "ng24",
    code: ".ng24",
    expansionText: "No growth at 24 hours",
    category: "CULTURE",
    contexts: ["MICROBIOLOGY_CULTURE_READING"],
  },
];

const renderField = ({ initialValue = "", maxLength } = {}) => {
  const Harness = () => {
    const [value, setValue] = React.useState(initialValue);
    return (
      <MacroTextArea
        id="reading-note"
        labelText="Reading note"
        context="MICROBIOLOGY_CULTURE_READING"
        maxLength={maxLength}
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
    );
  };
  return render(
    <IntlProvider locale="en" messages={messages}>
      <Harness />
    </IntlProvider>,
  );
};

beforeEach(() => {
  vi.clearAllMocks();
  getTextMacros.mockResolvedValue(macros);
});

describe("MacroTextArea", () => {
  it("expands an exact code on Space and persists only plain text", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    await user.type(field, "Observed: .gpc ");

    expect(field).toHaveValue("Observed: Gram-positive cocci ");
    expect(getTextMacros).toHaveBeenCalledWith(
      "MICROBIOLOGY_CULTURE_READING",
      expect.any(AbortSignal),
    );
  });

  it("replaces only the code at the caret and keeps the text on both sides", async () => {
    const user = userEvent.setup();
    renderField({ initialValue: "Seen  today" });

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    field.setSelectionRange(5, 5);
    await user.type(field, ".gpc ", {
      initialSelectionStart: 5,
      initialSelectionEnd: 5,
    });

    expect(field).toHaveValue("Seen Gram-positive cocci today");
  });

  it("supports discoverable keyboard selection and shows each phrase category", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    await user.type(field, ".ng");
    const option = await screen.findByRole("option", {
      name: /\.ng24.*No growth.*Culture/,
    });
    expect(option).toBeInTheDocument();
    expect(field).toHaveAttribute(
      "aria-controls",
      "reading-note-macro-options",
    );
    expect(field).toHaveAttribute(
      "aria-activedescendant",
      "reading-note-macro-options-ng24",
    );

    await user.keyboard("{ArrowDown}{Enter}");

    expect(field).toHaveValue("No growth at 24 hours");
    expect(field).toHaveFocus();
    expect(screen.getByText("Inserted .ng24.")).toBeInTheDocument();
  });

  it("supports pointer selection and returns focus to the controlled field", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    await user.type(field, ".gp");
    await user.click(
      await screen.findByRole("option", {
        name: /\.gpc.*Gram-positive cocci.*Gram stain/,
      }),
    );

    expect(field).toHaveValue("Gram-positive cocci");
    expect(field).toHaveFocus();
  });

  it("leaves unknown codes unchanged and closes suggestions on Escape", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    await user.type(field, ".unknown");
    await user.keyboard("{Escape} ");

    expect(field).toHaveValue(".unknown ");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });

  it("does not expand a phrase that would exceed the field limit", async () => {
    const user = userEvent.setup();
    renderField({ maxLength: 10 });

    const field = screen.getByLabelText("Reading note");
    await user.click(field);
    await user.type(field, ".gpc ");

    expect(field).toHaveValue(".gpc ");
    expect(
      screen.getByText(
        ".gpc was not inserted because the text would be longer than this field allows.",
      ),
    ).toBeInTheDocument();
  });
});
