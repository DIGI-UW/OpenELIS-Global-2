import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

import AcceptUnconditionallyGuard from "../AcceptUnconditionallyGuard";

/**
 * OGC-745 — Accept Unconditionally must be a two-stage gate with a required
 * justification. The previous design used an inline checkbox + browser
 * alert() that only fired on the first click per row and silently flipped
 * on subsequent clicks. A misclick bypassed validation review (audit-impact).
 *
 * This guard:
 *   - Stage 1 (idle):    warning-tone trigger button labeled "Accept unconditionally…"
 *   - Stage 2 (armed):   inline TextArea (required) + Confirm (disabled while empty) + Cancel
 *   - Stage 3 (committed): "Accepted unconditionally" with an Undo affordance
 *
 * Research basis: NN/g modal-fatigue + Fitts'-Law spatial separation +
 * FDA/EMA "override decisions documented with rationale".
 */

const renderGuard = (props = {}) => {
  const defaults = {
    rowId: "row-1",
    accepted: false,
    onAccept: vi.fn(),
    onUnaccept: vi.fn(),
  };
  return {
    onAccept: defaults.onAccept,
    onUnaccept: defaults.onUnaccept,
    ...render(
      <IntlProvider locale="en" messages={messages}>
        <AcceptUnconditionallyGuard {...defaults} {...props} />
      </IntlProvider>,
    ),
  };
};

describe("AcceptUnconditionallyGuard — safety guard for audit-impact override", () => {
  test("idle: shows the warning-tone trigger, no TextArea, no Confirm", () => {
    renderGuard();
    expect(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.queryByRole("button", { name: /^confirm/i })).toBeNull();
  });

  test("clicking the trigger arms the guard — does NOT call onAccept yet", async () => {
    const user = userEvent.setup();
    const { onAccept } = renderGuard();
    await user.click(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    );
    expect(onAccept).not.toHaveBeenCalled();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /confirm acceptance/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  test("armed: Confirm is disabled while reason is empty or whitespace", async () => {
    const user = userEvent.setup();
    renderGuard();
    await user.click(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    );
    const confirm = screen.getByRole("button", { name: /confirm acceptance/i });
    expect(confirm).toBeDisabled();
    const textarea = screen.getByRole("textbox");
    await user.type(textarea, "   ");
    expect(confirm).toBeDisabled();
  });

  test("armed: typing a non-empty reason enables Confirm", async () => {
    const user = userEvent.setup();
    renderGuard();
    await user.click(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    );
    await user.type(
      screen.getByRole("textbox"),
      "Retested twice, same result.",
    );
    expect(
      screen.getByRole("button", { name: /confirm acceptance/i }),
    ).toBeEnabled();
  });

  test("Confirm calls onAccept with the trimmed reason", async () => {
    const user = userEvent.setup();
    const { onAccept } = renderGuard();
    await user.click(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    );
    await user.type(
      screen.getByRole("textbox"),
      "  No result; do not cancel.  ",
    );
    await user.click(
      screen.getByRole("button", { name: /confirm acceptance/i }),
    );
    expect(onAccept).toHaveBeenCalledTimes(1);
    expect(onAccept).toHaveBeenCalledWith("No result; do not cancel.");
  });

  test("Cancel collapses back to idle without calling onAccept", async () => {
    const user = userEvent.setup();
    const { onAccept } = renderGuard();
    await user.click(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    );
    await user.type(screen.getByRole("textbox"), "draft reason");
    await user.click(screen.getByRole("button", { name: /cancel/i }));
    expect(onAccept).not.toHaveBeenCalled();
    expect(screen.queryByRole("textbox")).toBeNull();
    expect(
      screen.getByRole("button", { name: /accept unconditionally/i }),
    ).toBeInTheDocument();
  });

  test("committed state shows the accepted indicator + Undo, no trigger", () => {
    renderGuard({ accepted: true });
    expect(screen.getByText(/accepted unconditionally/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /undo/i })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /accept unconditionally/i }),
    ).toBeNull();
  });

  test("Undo calls onUnaccept", async () => {
    const user = userEvent.setup();
    const { onUnaccept } = renderGuard({ accepted: true });
    await user.click(screen.getByRole("button", { name: /undo/i }));
    expect(onUnaccept).toHaveBeenCalledTimes(1);
  });
});
