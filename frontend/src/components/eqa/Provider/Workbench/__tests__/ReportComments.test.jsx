import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../../languages/en.json";
import ReportComments from "../ReportComments";
import {
  attachComments,
  detachComment,
  fetchCommentLibrary,
  fetchCycleComments,
} from "../workbenchApi";

/**
 * OGC-934 — the picker's failure paths.
 *
 * Only the transport is mocked. `resolveApiErrorMessage` is deliberately the
 * real implementation: it takes (intl, payload, fallbackId), and calling it
 * with the payload alone throws inside the error branch, leaving the previous
 * success notice on screen while the server refused the write. A mocked
 * resolver accepts either argument order and hides that, which is how the
 * defect reached a live walkthrough.
 */
vi.mock("../workbenchApi", () => ({
  fetchCommentLibrary: vi.fn(),
  fetchCycleComments: vi.fn(),
  attachComments: vi.fn(),
  detachComment: vi.fn(),
}));

const LIBRARY = [
  { id: "4118", text: "Performance acceptable for this cycle." },
  { id: "4119", text: "Result outside the acceptable range." },
];

const ATTACHED = [
  {
    id: "83",
    libraryEntryId: "4119",
    text: "Result outside the acceptable range.",
    attachedBy: "Open ELIS",
    attachedAt: "20/08/2026 19:08",
  },
];

/** Carbon renders the option list only while the menu is open. */
/**
 * Carbon renders the option list only while the menu is open, and a controlled
 * MultiSelect needs the full pointer sequence user-event dispatches — a bare
 * fireEvent.click on the option leaves the selection empty and the Add button
 * disabled.
 */
const pick = async (label) => {
  const user = userEvent.setup();
  await user.click(
    screen.getByRole("combobox", { name: /approved comments/i }),
  );
  await user.click(screen.getByRole("option", { name: label }));
};

const renderPanel = (onNotice) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReportComments cycleId={4} onNotice={onNotice} />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  fetchCommentLibrary.mockImplementation((cb) => cb(LIBRARY));
  fetchCycleComments.mockImplementation((_cycleId, cb) => cb(ATTACHED));
});

describe("ReportComments", () => {
  it("surfaces the server's refusal when an attach is rejected", async () => {
    attachComments.mockImplementation((_cycleId, _ids, cb) =>
      cb({
        ok: false,
        body: {
          error: "Comment 4123 is not an active entry of the library",
        },
      }),
    );
    const onNotice = vi.fn();
    renderPanel(onNotice);

    await pick("Performance acceptable for this cycle.");
    fireEvent.click(screen.getByRole("button", { name: "Add to report" }));

    expect(onNotice).toHaveBeenCalledTimes(1);
    expect(onNotice).toHaveBeenCalledWith({
      kind: "error",
      text: "Comment 4123 is not an active entry of the library",
    });
  });

  it("falls back to a message when a refusal carries no body", async () => {
    attachComments.mockImplementation((_cycleId, _ids, cb) =>
      cb({ ok: false, body: null }),
    );
    const onNotice = vi.fn();
    renderPanel(onNotice);

    await pick("Performance acceptable for this cycle.");
    fireEvent.click(screen.getByRole("button", { name: "Add to report" }));

    expect(onNotice).toHaveBeenCalledTimes(1);
    expect(onNotice).toHaveBeenCalledWith({
      kind: "error",
      text: messages["eqa.report.comments.addFailed"],
    });
  });

  it("surfaces the server's refusal when a remove is rejected", () => {
    detachComment.mockImplementation((_cycleId, _commentId, cb) =>
      cb({
        ok: false,
        body: { error: "Comment 83 is not attached to cycle 4" },
      }),
    );
    const onNotice = vi.fn();
    renderPanel(onNotice);

    fireEvent.click(screen.getByRole("button", { name: "Remove" }));

    expect(onNotice).toHaveBeenCalledTimes(1);
    expect(onNotice).toHaveBeenCalledWith({
      kind: "error",
      text: "Comment 83 is not attached to cycle 4",
    });
    expect(fetchCycleComments).toHaveBeenCalledTimes(1);
  });

  it("reports how many comments were added and reloads the table", async () => {
    attachComments.mockImplementation((_cycleId, ids, cb) => {
      expect(ids).toEqual(["4118"]);
      cb({ ok: true, body: [{ id: "84", libraryEntryId: "4118" }] });
    });
    const onNotice = vi.fn();
    renderPanel(onNotice);

    await pick("Performance acceptable for this cycle.");
    fireEvent.click(screen.getByRole("button", { name: "Add to report" }));

    expect(onNotice).toHaveBeenCalledTimes(1);
    expect(onNotice).toHaveBeenCalledWith({
      kind: "success",
      text: "1 comment(s) added to the report",
    });
    expect(fetchCycleComments).toHaveBeenCalledTimes(2);
  });

  it("offers only entries that are not attached yet", () => {
    renderPanel(vi.fn());

    fireEvent.click(
      screen.getByRole("combobox", { name: /approved comments/i }),
    );

    // 4119 is attached, so the picker offers 4118 alone; the attached wording
    // appears once, in the table rather than the menu.
    const offered = screen.getAllByRole("option").map((o) => o.textContent);
    expect(offered).toEqual(["Performance acceptable for this cycle."]);
    expect(
      screen.getAllByText("Result outside the acceptable range."),
    ).toHaveLength(1);
  });
});
