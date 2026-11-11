import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseAttachmentNotes from "../CaseAttachmentNotes";

it("moves keyboard focus to the note field and back to the notes button when the dialog closes", async () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseAttachmentNotes
        detail={{ id: "c1", canWrite: true }}
        service={{ addNote: vi.fn() }}
        targetType="CULTURE"
        targetId="r1"
      />
    </IntlProvider>,
  );
  const trigger = screen.getByRole("button", { name: "Notes (0)" });
  trigger.focus();
  fireEvent.click(trigger);
  const dialog = screen.getByRole("dialog");
  expect(within(dialog).getByRole("textbox")).toHaveFocus();
  fireEvent.keyDown(dialog, { key: "Escape" });
  await waitFor(() =>
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
  );
  await waitFor(() => expect(trigger).toHaveFocus());
});

it("keeps Save off and says why when the browser is offline", () => {
  const onLine = vi
    .spyOn(window.navigator, "onLine", "get")
    .mockReturnValue(false);
  try {
    render(
      <IntlProvider locale="en" messages={messages}>
        <CaseAttachmentNotes
          detail={{ id: "c1", canWrite: true }}
          service={{ addNote: vi.fn() }}
          targetType="CULTURE"
          targetId="r1"
        />
      </IntlProvider>,
    );
    fireEvent.click(screen.getByRole("button", { name: "Notes (0)" }));
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByRole("textbox"), {
      target: { value: "Plate checked" },
    });
    expect(
      within(dialog).getByText(
        "Working offline; refresh when network returns.",
      ),
    ).toBeInTheDocument();
    expect(
      within(dialog).getByRole("button", { name: "Save note" }),
    ).toBeDisabled();
  } finally {
    onLine.mockRestore();
  }
});
