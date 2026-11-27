/**
 * The bench requests table of the Review section (FR-10.1 to FR-10.9).
 *
 * The table reads the requests the case read serves and hands every change
 * to its caller, which posts it and reads the case again; these tests pin
 * what the table shows, what it hands over, and that a held or refused
 * change never takes what was typed or where focus was.
 */
import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider, createIntl } from "react-intl";
import messages from "../../languages/en.json";
import RequestsTable, {
  NEW_REQUEST_BUTTON_ID,
  REQUEST_INSTRUCTION_ID,
  REQUESTS_HOLD_ID,
} from "./RequestsTable";

const intl = createIntl({ locale: "en", messages });

const request = (overrides) => ({
  kind: null,
  targetBlockId: null,
  targetSlideId: null,
  targetName: null,
  stainId: null,
  stainName: null,
  priority: null,
  cancelReason: null,
  resultBlockId: null,
  resultSlideId: null,
  resultName: null,
  raisedAt: null,
  raisedBy: null,
  ...overrides,
});

const slideRequest = request({
  id: 11,
  kind: "SLIDE",
  targetBlockId: 1,
  targetName: "A1",
  priority: "URGENT",
  instruction: "Two deeper levels",
  status: "OPENED",
  raisedAt: "17/11/2026 09:30",
  raisedBy: "Paula Path",
});

const stainRequest = request({
  id: 12,
  kind: "STAIN",
  targetSlideId: 5,
  targetName: "A1.1",
  stainId: "32",
  stainName: "PAS",
  priority: "NORMAL",
  instruction: "PAS on the first level",
  status: "COMPLETED",
  resultSlideId: 6,
  resultName: "A1.2",
  raisedAt: "16/11/2026 14:05",
  raisedBy: "Paula Path",
});

const cancelledRequest = request({
  id: 13,
  kind: "BLOCK",
  priority: "STAT",
  instruction: "Another block from the margin",
  status: "CANCELLED",
  cancelReason: "Tissue exhausted",
  raisedAt: "15/11/2026 08:00",
  raisedBy: "Paula Path",
});

// A request picked from the old dictionary before requests were raised in
// their own row: it has no kind, target, priority, author or date.
const requestOnFile = request({
  id: 14,
  instruction: "Recut",
  status: "OPENED",
});

const blocks = [
  { id: 1, designation: "A1", tissueTypeId: "21", active: true },
  { id: 2, designation: "A2", tissueTypeId: "22", active: true },
  { id: 3, designation: "A3", active: false },
  // An unsaved cassette, never a target.
  { designation: null },
];

const slides = [
  { id: 5, designation: "1", blockId: 1, active: true },
  { id: 6, designation: "2", blockId: 1, active: true },
  { id: 7, designation: "3", blockId: 2, active: false },
];

const stains = [
  { id: "31", value: "H&E" },
  { id: "32", value: "PAS" },
];

const FREE = { disabled: false, reasonKey: null };

let onRaise;
let onChangeStatus;

const props = (overrides = {}) => ({
  requests: [slideRequest, stainRequest, cancelledRequest, requestOnFile],
  blocks,
  slides,
  tissueTypeNames: { 21: "Skin", 22: "Bone" },
  stains,
  canRaise: true,
  canChangeStatus: true,
  lock: FREE,
  held: false,
  onRaise,
  onChangeStatus,
  ...overrides,
});

const renderTable = (overrides) => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <RequestsTable {...props(overrides)} />
    </IntlProvider>,
  );
  return {
    ...view,
    rerenderWith: (more) =>
      view.rerender(
        <IntlProvider locale="en" messages={messages}>
          <RequestsTable {...props({ ...overrides, ...more })} />
        </IntlProvider>,
      ),
  };
};

const rowOf = (requestRow) => screen.getByTestId("requestRow-" + requestRow.id);

// The options of the picker whose list is open, not those of the native
// priority select or of another picker beside it.
const listedOptions = () =>
  within(
    screen
      .getAllByRole("listbox")
      .find((list) => within(list).queryAllByRole("option").length > 0),
  )
    .getAllByRole("option")
    .map((option) => option.textContent);

const tagOf = (element) => element.closest(".cds--tag");

const named = (key, name) => intl.formatMessage({ id: key }, { name });

// By its own text and the instruction it names: the role query bundled here
// names a disabled button by its title rather than by its aria-label.
const button = (id) => document.getElementById(id);

const newRequestButton = () => button(NEW_REQUEST_BUTTON_ID);
const instruction = () => document.getElementById(REQUEST_INSTRUCTION_ID);
const raiseButton = () => button("raiseRequest");
const kindRadio = (kindKey) =>
  screen.getByRole("radio", { name: messages[kindKey] });
const targetCombobox = () => document.getElementById("requestTarget");
const stainCombobox = () => document.getElementById("requestStain");

const describedBy = (element) =>
  (element.getAttribute("aria-describedby") ?? "")
    .split(" ")
    .map((id) => document.getElementById(id)?.textContent)
    .join(" ");

beforeEach(() => {
  onRaise = vi.fn(() => Promise.resolve({ ok: true }));
  onChangeStatus = vi.fn(() => Promise.resolve({ ok: true }));
});

describe("RequestsTable, what each request shows", () => {
  it("heads five columns: the request, its instruction, when it was raised, its status and its actions", () => {
    renderTable();

    expect(
      screen.getAllByRole("columnheader").map((cell) => cell.textContent),
    ).toEqual([
      messages["pathology.label.requestColumn"],
      messages["pathology.label.requestInstruction"],
      messages["pathology.label.raised"],
      messages["common.status"],
      messages["common.actions"],
    ]);
  });

  it("names the kind, the target, the stain and the priority in the request cell, in words", () => {
    renderTable();

    const stain = within(rowOf(stainRequest)).getAllByRole("cell")[0];
    expect(stain).toHaveTextContent(messages["pathology.requestKind.stain"]);
    expect(stain).toHaveTextContent("A1.1");
    expect(stain).toHaveTextContent("PAS");
    expect(stain).toHaveTextContent(messages["common.normal"]);

    const slide = within(rowOf(slideRequest)).getAllByRole("cell")[0];
    expect(slide).toHaveTextContent(messages["pathology.requestKind.slide"]);
    expect(slide).toHaveTextContent("A1");
    // Urgent and STAT carry a red tag, and the word with it.
    expect(
      tagOf(within(slide).getByText(messages["pathology.priority.urgent"])),
    ).toHaveClass("cds--tag--red");
    expect(
      tagOf(
        within(rowOf(cancelledRequest)).getByText(
          messages["pathology.priority.stat"],
        ),
      ),
    ).toHaveClass("cds--tag--red");
    // Normal is the ordinary case and is said plainly, not as an alarm.
    expect(
      tagOf(within(stain).getByText(messages["common.normal"])),
    ).toBeNull();
  });

  it("shows the instruction, the date raised with who raised it, and the status with its reason or what fulfilled it", () => {
    renderTable();

    const slide = within(rowOf(slideRequest));
    expect(slide.getByText("Two deeper levels")).toBeInTheDocument();
    expect(slide.getByText("17/11/2026 09:30")).toBeInTheDocument();
    expect(slide.getByText("Paula Path")).toBeInTheDocument();
    expect(
      tagOf(slide.getByText(messages["pathology.requestStatus.opened"])),
    ).toHaveClass("cds--tag--purple");

    const stain = within(rowOf(stainRequest));
    expect(tagOf(stain.getByText(messages["common.completed"]))).toHaveClass(
      "cds--tag--green",
    );
    expect(
      stain.getByText(named("pathology.label.fulfilledByName", "A1.2")),
    ).toBeInTheDocument();

    const cancelled = within(rowOf(cancelledRequest));
    expect(
      tagOf(cancelled.getByText(messages["pathology.requestStatus.cancelled"])),
    ).toHaveClass("cds--tag--gray");
    expect(cancelled.getByText("Tissue exhausted")).toBeInTheDocument();
  });

  // Nothing can be inferred for a row nobody dated.
  it("says Not recorded where a request on file lacks a kind and a date, and still shows its instruction and status", () => {
    renderTable();

    const cells = within(rowOf(requestOnFile)).getAllByRole("cell");
    expect(cells[0]).toHaveTextContent(messages["common.notRecorded"]);
    expect(cells[1]).toHaveTextContent("Recut");
    expect(cells[2]).toHaveTextContent(messages["common.notRecorded"]);
    expect(cells[3]).toHaveTextContent(
      messages["pathology.requestStatus.opened"],
    );
  });

  it("lists the requests in the order the case read serves them, newest first", () => {
    renderTable();

    expect(
      screen
        .getAllByTestId(/^requestRow-/)
        .map((row) => row.getAttribute("data-testid")),
    ).toEqual([
      "requestRow-11",
      "requestRow-12",
      "requestRow-13",
      "requestRow-14",
    ]);
  });

  it("says the case has no bench requests rather than showing an empty table", () => {
    renderTable({ requests: [] });

    expect(
      screen.getByText(messages["pathology.empty.noRequests"]),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});

describe("RequestsTable, the actions each request offers", () => {
  it("offers an open request Complete and Cancel request, and a closed one Reopen only", () => {
    renderTable();

    const open = within(rowOf(slideRequest));
    expect(
      open.getByText(messages["common.complete"], { selector: "button" }),
    ).toBeInTheDocument();
    expect(
      open.getByText(messages["pathology.action.cancelRequest"], {
        selector: "button",
      }),
    ).toBeInTheDocument();
    expect(
      open.queryByText(messages["pathology.action.reopenRequest"]),
    ).not.toBeInTheDocument();

    [stainRequest, cancelledRequest].forEach((closed) => {
      const row = within(rowOf(closed));
      expect(
        row.getByText(messages["pathology.action.reopenRequest"], {
          selector: "button",
        }),
      ).toBeInTheDocument();
      expect(
        row.queryByText(messages["common.complete"], { selector: "button" }),
      ).not.toBeInTheDocument();
    });
  });

  // WCAG 2.5.3: the accessible name starts with the words on the button, and
  // the instruction tells the rows' buttons apart.
  it("names each row action by the words it shows and then the instruction", () => {
    renderTable();

    expect(button("completeRequest-11")).toHaveAccessibleName(
      named("pathology.action.completeRequestOf", "Two deeper levels"),
    );
    expect(button("cancelRequest-11")).toHaveAccessibleName(
      named("pathology.action.cancelRequestOf", "Two deeper levels"),
    );
    expect(button("reopenRequest-12")).toHaveAccessibleName(
      named("pathology.action.reopenRequestOf", "PAS on the first level"),
    );
  });

  it("offers no action and no actions column where the viewer can change nothing", () => {
    renderTable({ canRaise: false, canChangeStatus: false });

    expect(screen.queryAllByRole("button")).toHaveLength(0);
    expect(
      screen.queryByRole("columnheader", { name: messages["common.actions"] }),
    ).not.toBeInTheDocument();
  });

  // Bench mode: the bench completes what was asked of it, and only the
  // pathologist raises a request.
  it("offers the status actions and no New request in bench mode", () => {
    renderTable({ canRaise: false });

    expect(newRequestButton()).toBeNull();
    expect(button("completeRequest-11")).toBeInTheDocument();
  });

  it("completes a request on file at once, since nothing it names could have fulfilled it", async () => {
    renderTable();

    fireEvent.click(button("completeRequest-14"));

    expect(onChangeStatus).toHaveBeenCalledWith(
      requestOnFile,
      {
        from: "OPENED",
        status: "COMPLETED",
        cancelReason: null,
        resultBlockId: null,
        resultSlideId: null,
      },
      button("completeRequest-14"),
    );
    expect(document.getElementById("requestFulfilledBy-14")).toBeNull();
  });

  it("asks what fulfilled a slide request among the slides in use, and posts the one picked", async () => {
    renderTable();
    const user = userEvent.setup();

    fireEvent.click(button("completeRequest-11"));
    const picker = document.getElementById("requestFulfilledBy-11");
    expect(picker).toBeInTheDocument();
    await waitFor(() => expect(picker).toHaveFocus());

    await user.click(picker);
    expect(listedOptions()).toEqual(["A1.1", "A1.2"]);
    await user.click(screen.getByRole("option", { name: "A1.2" }));
    fireEvent.click(button("requestCompleteConfirm-11"));

    expect(onChangeStatus).toHaveBeenCalledWith(
      slideRequest,
      expect.objectContaining({
        from: "OPENED",
        status: "COMPLETED",
        resultSlideId: 6,
        resultBlockId: null,
      }),
      button("requestCompleteConfirm-11"),
    );
  });

  it("asks what fulfilled a block request among the blocks in use only", async () => {
    const openBlock = { ...cancelledRequest, status: "OPENED" };
    renderTable({ requests: [openBlock] });
    const user = userEvent.setup();

    fireEvent.click(button("completeRequest-13"));
    await user.click(document.getElementById("requestFulfilledBy-13"));

    expect(listedOptions()).toEqual(["A1", "A2"]);
  });

  // The picker is optional: a request done with nothing to name is still done.
  it("completes a request with a kind with nothing named as having fulfilled it", () => {
    renderTable();

    fireEvent.click(button("completeRequest-11"));
    fireEvent.click(button("requestCompleteConfirm-11"));

    expect(onChangeStatus).toHaveBeenCalledWith(
      slideRequest,
      expect.objectContaining({ resultSlideId: null, resultBlockId: null }),
      expect.anything(),
    );
  });

  it("closes the Complete row on Cancel, posting nothing, and hands focus back to Complete", async () => {
    renderTable();

    fireEvent.click(button("completeRequest-11"));
    fireEvent.click(button("requestCompleteCancel-11"));

    expect(document.getElementById("requestFulfilledBy-11")).toBeNull();
    expect(onChangeStatus).not.toHaveBeenCalled();
    await waitFor(() => expect(button("completeRequest-11")).toHaveFocus());
  });

  // A hovered table row takes a background the ghost buttons fall under
  // text contrast on; the form rows are kept off it by this class.
  it("marks the new request and the Complete and Cancel request rows as form rows, and no request row", () => {
    renderTable();
    const formRows = () =>
      document.querySelectorAll(".pathology-case-view__request-form-row");

    fireEvent.click(newRequestButton());
    fireEvent.click(button("completeRequest-11"));
    expect(screen.getByTestId("newRequestRow")).toHaveClass(
      "pathology-case-view__request-form-row",
    );
    expect(screen.getByTestId("requestComplete-11")).toHaveClass(
      "pathology-case-view__request-form-row",
    );

    fireEvent.click(button("requestCompleteCancel-11"));
    fireEvent.click(button("cancelRequest-11"));
    expect(screen.getByTestId("requestCancel-11")).toHaveClass(
      "pathology-case-view__request-form-row",
    );
    expect(formRows()).toHaveLength(2);
    expect(rowOf(slideRequest)).not.toHaveClass(
      "pathology-case-view__request-form-row",
    );
  });

  it("asks for a reason before cancelling a request, holding Confirm until one is given and saying why", async () => {
    renderTable();

    fireEvent.click(button("cancelRequest-11"));
    const reason = document.getElementById("requestCancelReason-11");
    await waitFor(() => expect(reason).toHaveFocus());
    expect(reason).toHaveAttribute("maxLength", "255");

    const confirm = button("requestCancelConfirm-11");
    expect(confirm).toBeDisabled();
    expect(confirm).toHaveAttribute(
      "title",
      messages["pathology.locked.requestCancelReasonRequired"],
    );
    expect(describedBy(confirm)).toContain(
      messages["pathology.locked.requestCancelReasonRequired"],
    );

    // Inversion: a reason of spaces is no reason.
    fireEvent.change(reason, { target: { value: "   " } });
    expect(confirm).toBeDisabled();

    fireEvent.change(reason, { target: { value: " Tissue exhausted " } });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);

    expect(onChangeStatus).toHaveBeenCalledWith(
      slideRequest,
      {
        from: "OPENED",
        status: "CANCELLED",
        cancelReason: "Tissue exhausted",
        resultBlockId: null,
        resultSlideId: null,
      },
      confirm,
    );
  });

  // "Cancel" beside Confirm would read as "go on and cancel"; the way out says
  // what it leaves standing, and names the request as Confirm does.
  it("closes the Cancel request row on Keep request, posting nothing, and hands focus back to Cancel request", async () => {
    renderTable();

    fireEvent.click(button("cancelRequest-11"));
    const keep = button("requestCancelKeep-11");
    expect(keep).toHaveTextContent(messages["pathology.action.keepRequest"]);
    expect(keep).toHaveAccessibleName(
      named("pathology.action.keepRequestOf", "Two deeper levels"),
    );
    expect(
      within(screen.getByTestId("requestCancel-11")).queryByText(
        messages["common.cancel"],
      ),
    ).not.toBeInTheDocument();

    fireEvent.click(keep);

    expect(document.getElementById("requestCancelReason-11")).toBeNull();
    expect(onChangeStatus).not.toHaveBeenCalled();
    await waitFor(() => expect(button("cancelRequest-11")).toHaveFocus());
  });

  // The reason goes into the case's history as typed (FR-17).
  it("asks the cancel reason for no patient names or identifiers", () => {
    renderTable();

    fireEvent.click(button("cancelRequest-11"));

    expect(
      screen.getByRole("textbox", { name: messages["common.reason"] }),
    ).toHaveAccessibleDescription(
      messages["pathology.label.returnReasonHelper"],
    );
  });

  // A pasted text longer than the column is cut at 255, so how much of it is
  // used is shown as the person types.
  it("counts the instruction and the cancel reason against their 255 characters", () => {
    renderTable();

    fireEvent.click(newRequestButton());
    fireEvent.change(instruction(), { target: { value: "Three levels" } });
    expect(screen.getByTestId("newRequestRow")).toHaveTextContent("12/255");

    fireEvent.click(button("cancelRequest-11"));
    fireEvent.change(document.getElementById("requestCancelReason-11"), {
      target: { value: "Exhausted" },
    });
    expect(screen.getByTestId("requestCancel-11")).toHaveTextContent("9/255");
  });

  it("reopens a closed request at once, naming the status the page showed", () => {
    renderTable();

    fireEvent.click(button("reopenRequest-13"));

    expect(onChangeStatus).toHaveBeenCalledWith(
      cancelledRequest,
      expect.objectContaining({ from: "CANCELLED", status: "OPENED" }),
      button("reopenRequest-13"),
    );
  });

  it("keeps the Cancel request row and its reason open after a refusal, and closes it once the change lands", async () => {
    onChangeStatus = vi.fn(() => Promise.resolve({ ok: false }));
    const view = renderTable();

    fireEvent.click(button("cancelRequest-11"));
    fireEvent.change(document.getElementById("requestCancelReason-11"), {
      target: { value: "Tissue exhausted" },
    });
    await act(async () => {
      fireEvent.click(button("requestCancelConfirm-11"));
    });

    expect(document.getElementById("requestCancelReason-11")).toHaveValue(
      "Tissue exhausted",
    );

    onChangeStatus = vi.fn(() => Promise.resolve({ ok: true }));
    view.rerenderWith({ onChangeStatus });
    await act(async () => {
      fireEvent.click(button("requestCancelConfirm-11"));
    });

    expect(document.getElementById("requestCancelReason-11")).toBeNull();
  });
});

describe("RequestsTable, held by the case", () => {
  const HELD = {
    disabled: true,
    reasonKey: "pathology.locked.saveBeforeRequest",
  };

  it("holds every request action over unsaved work, saying why once above the table and on each button", () => {
    renderTable({ lock: HELD });

    expect(document.getElementById(REQUESTS_HOLD_ID)).toHaveTextContent(
      messages["pathology.locked.saveBeforeRequest"],
    );
    ["completeRequest-11", "cancelRequest-11", "reopenRequest-12"].forEach(
      (id) => {
        expect(button(id)).toBeDisabled();
        expect(button(id)).toHaveAttribute(
          "title",
          messages["pathology.locked.saveBeforeRequest"],
        );
        expect(describedBy(button(id))).toBe(
          messages["pathology.locked.saveBeforeRequest"],
        );
      },
    );
  });

  // Held is not read-only. A table that dropped its rows while a
  // change was out would take the typed text and the focused control with it.
  it("keeps the inline rows and what was typed on the page, disabled, while a change is out", () => {
    const view = renderTable();

    fireEvent.click(newRequestButton());
    fireEvent.change(instruction(), { target: { value: "Three levels" } });
    fireEvent.click(button("cancelRequest-11"));
    fireEvent.change(document.getElementById("requestCancelReason-11"), {
      target: { value: "Exhausted" },
    });

    view.rerenderWith({
      held: true,
      lock: { disabled: true, reasonKey: "pathology.locked.requestInFlight" },
    });

    expect(instruction()).toHaveValue("Three levels");
    expect(instruction()).toBeDisabled();
    expect(raiseButton()).toBeDisabled();
    expect(document.getElementById("requestCancelReason-11")).toHaveValue(
      "Exhausted",
    );
    expect(document.getElementById("requestCancelReason-11")).toBeDisabled();
    expect(button("requestCancelConfirm-11")).toBeDisabled();

    view.rerenderWith({ held: false, lock: FREE });

    expect(instruction()).toBeEnabled();
    expect(instruction()).toHaveValue("Three levels");
  });
});

describe("RequestsTable, raising a request", () => {
  const openNewRequest = () => fireEvent.click(newRequestButton());

  it("opens the new request as the table's first row, a slide request at normal priority by default", () => {
    renderTable();

    openNewRequest();

    const body = screen.getAllByRole("rowgroup")[1];
    expect(within(body).getAllByRole("row")[0]).toContainElement(instruction());
    expect(kindRadio("pathology.requestKind.slide")).toBeChecked();
    expect(document.getElementById("requestPriority")).toHaveValue("NORMAL");
    expect(instruction()).toHaveAttribute("maxLength", "255");
    expect(newRequestButton()).toHaveAttribute("aria-expanded", "true");
  });

  it("opens the new request even on a case with no request yet", () => {
    renderTable({ requests: [] });

    openNewRequest();

    expect(instruction()).toBeInTheDocument();
  });

  it("offers a slide request the blocks in use, named with their tissue, and a stain request the slides in use", async () => {
    renderTable();
    const user = userEvent.setup();
    openNewRequest();

    await user.click(targetCombobox());
    expect(listedOptions()).toEqual([
      intl.formatMessage(
        { id: "pathology.label.blockWithTissue" },
        { designation: "A1", tissue: "Skin" },
      ),
      intl.formatMessage(
        { id: "pathology.label.blockWithTissue" },
        { designation: "A2", tissue: "Bone" },
      ),
    ]);
    await user.keyboard("{Escape}");

    fireEvent.click(kindRadio("pathology.requestKind.stain"));
    await user.click(targetCombobox());
    expect(listedOptions()).toEqual(["A1.1", "A1.2"]);
  });

  it("asks a block request for no target and offers the stain picker only to a stain request", () => {
    renderTable();
    openNewRequest();

    expect(stainCombobox()).toBeNull();
    fireEvent.click(kindRadio("pathology.requestKind.block"));
    expect(targetCombobox()).toBeNull();
    expect(stainCombobox()).toBeNull();
    fireEvent.click(kindRadio("pathology.requestKind.stain"));
    expect(targetCombobox()).toBeInTheDocument();
    expect(stainCombobox()).toBeInTheDocument();
  });

  it("holds Raise request until the request is complete, naming the first thing missing in its title and its description", async () => {
    renderTable();
    const user = userEvent.setup();
    openNewRequest();

    const expectHeldFor = (key) => {
      expect(raiseButton()).toBeDisabled();
      expect(raiseButton()).toHaveAttribute("title", messages[key]);
      expect(describedBy(raiseButton())).toBe(messages[key]);
    };

    expectHeldFor("pathology.locked.requestInstructionRequired");
    fireEvent.change(instruction(), { target: { value: "PAS please" } });
    expectHeldFor("pathology.locked.requestTargetRequired");

    fireEvent.click(kindRadio("pathology.requestKind.stain"));
    await user.click(targetCombobox());
    await user.click(screen.getByRole("option", { name: "A1.1" }));
    expectHeldFor("pathology.locked.requestStainRequired");

    await user.click(stainCombobox());
    await user.click(screen.getByRole("option", { name: "PAS" }));
    expect(raiseButton()).toBeEnabled();
    expect(raiseButton()).not.toHaveAttribute("title");
  });

  it("raises the request it shows, with its target, stain and priority", async () => {
    renderTable();
    const user = userEvent.setup();
    openNewRequest();

    fireEvent.click(kindRadio("pathology.requestKind.stain"));
    await user.click(targetCombobox());
    await user.click(screen.getByRole("option", { name: "A1.2" }));
    await user.click(stainCombobox());
    await user.click(screen.getByRole("option", { name: "H&E" }));
    fireEvent.change(document.getElementById("requestPriority"), {
      target: { value: "STAT" },
    });
    fireEvent.change(instruction(), { target: { value: " Recut and H&E " } });
    fireEvent.click(raiseButton());

    expect(onRaise).toHaveBeenCalledWith(
      {
        kind: "STAIN",
        targetBlockId: null,
        targetSlideId: 6,
        stainId: "31",
        priority: "STAT",
        instruction: "Recut and H&E",
      },
      raiseButton(),
    );
  });

  // A target picked for a slide request is left behind when the kind changes
  // to block, and must not travel with it.
  it("raises a block request with no target, whatever was picked before the kind changed", async () => {
    renderTable();
    const user = userEvent.setup();
    openNewRequest();

    await user.click(targetCombobox());
    await user.click(screen.getByRole("option", { name: /^A1/ }));
    fireEvent.click(kindRadio("pathology.requestKind.block"));
    fireEvent.change(instruction(), { target: { value: "One more block" } });
    fireEvent.click(raiseButton());

    expect(onRaise).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "BLOCK",
        targetBlockId: null,
        targetSlideId: null,
      }),
      expect.anything(),
    );
  });

  it("keeps the new request and what was typed after a refusal, and closes it once raised", async () => {
    onRaise = vi.fn(() => Promise.resolve({ ok: false }));
    const view = renderTable();
    openNewRequest();
    fireEvent.click(kindRadio("pathology.requestKind.block"));
    fireEvent.change(instruction(), { target: { value: "One more block" } });

    await act(async () => {
      fireEvent.click(raiseButton());
    });
    expect(instruction()).toHaveValue("One more block");
    expect(kindRadio("pathology.requestKind.block")).toBeChecked();

    onRaise = vi.fn(() => Promise.resolve({ ok: true }));
    view.rerenderWith({ onRaise });
    await act(async () => {
      fireEvent.click(raiseButton());
    });
    expect(instruction()).toBeNull();

    // The next new request starts again from the defaults.
    openNewRequest();
    expect(instruction()).toHaveValue("");
    expect(kindRadio("pathology.requestKind.slide")).toBeChecked();
  });

  it("closes the new request on Cancel, raising nothing, and hands focus back to New request", async () => {
    renderTable();
    openNewRequest();

    fireEvent.click(button("raiseRequestCancel"));

    expect(instruction()).toBeNull();
    expect(onRaise).not.toHaveBeenCalled();
    await waitFor(() => expect(newRequestButton()).toHaveFocus());
  });

  // FR-10.2: a row elsewhere on the case opens the request with its kind and
  // target set, and the pathologist writes the instruction.
  it("opens the new request with the kind and target it is handed, and puts focus on the instruction", async () => {
    const view = renderTable();

    view.rerenderWith({ prefill: { kind: "STAIN", targetId: 6, nonce: 1 } });

    expect(kindRadio("pathology.requestKind.stain")).toBeChecked();
    expect(targetCombobox()).toHaveValue("A1.2");
    await waitFor(() => expect(instruction()).toHaveFocus());
  });
});
