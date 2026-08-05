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
    contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
  },
  {
    id: "ng24",
    code: ".ng24",
    expansionText: "No growth at 24 hours",
    contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
  },
];

const renderField = ({ context = "MICROBIOLOGY_CULTURE_ACTIVITY" } = {}) => {
  const Harness = () => {
    const [value, setValue] = React.useState("");
    return (
      <MacroTextArea
        id="activity-note"
        labelText="Activity note"
        context={context}
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

    const field = screen.getByRole("textbox", { name: "Activity note" });
    await user.click(field);
    await user.type(field, "Observed: .gpc ");

    expect(field).toHaveValue("Observed: Gram-positive cocci ");
    expect(getTextMacros).toHaveBeenCalledWith(
      "MICROBIOLOGY_CULTURE_ACTIVITY",
      expect.any(AbortSignal),
    );
  });

  it("supports discoverable keyboard selection and restores field focus", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByRole("textbox", { name: "Activity note" });
    await user.click(field);
    await user.type(field, ".ng");
    expect(
      await screen.findByRole("option", { name: /\.ng24.*No growth/ }),
    ).toBeInTheDocument();

    await user.keyboard("{ArrowDown}{Enter}");

    expect(field).toHaveValue("No growth at 24 hours");
    expect(field).toHaveFocus();
  });

  it("leaves unknown codes unchanged and closes suggestions on Escape", async () => {
    const user = userEvent.setup();
    renderField();

    const field = screen.getByRole("textbox", { name: "Activity note" });
    await user.click(field);
    await user.type(field, ".unknown");
    await user.keyboard("{Escape} ");

    expect(field).toHaveValue(".unknown ");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  });
});
