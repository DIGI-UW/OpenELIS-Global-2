import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConfirmedBulkActionModal from "./ConfirmedBulkActionModal";

describe("ConfirmedBulkActionModal", () => {
  it("names the action, selected items, and explicit confirmation", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <ConfirmedBulkActionModal
        open
        danger
        title="Remove 2 local phrases?"
        description="This cannot be undone."
        items={[".first", ".second"]}
        confirmLabel="Remove phrases"
        cancelLabel="Cancel"
        closeLabel="Close"
        onClose={vi.fn()}
        onConfirm={onConfirm}
      />,
    );

    expect(
      screen.getByRole("dialog", { name: "Remove 2 local phrases?" }),
    ).toBeInTheDocument();
    expect(screen.getByText(".first")).toBeInTheDocument();
    expect(screen.getByText(".second")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /remove phrases/i }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
