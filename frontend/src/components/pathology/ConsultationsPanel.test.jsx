/**
 * The second opinion panel of the Review section (FR-11).
 *
 * The panel reads the second opinions the case read serves and hands every
 * change to its caller, which posts it and reads the case again; these tests
 * pin what the panel shows, to whom it offers each action, what it hands
 * over, and that a held or refused change never takes what was typed.
 */
import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider, createIntl } from "react-intl";
import messages from "../../languages/en.json";
import ConsultationsPanel, {
  CONSULTATIONS_HOLD_ID,
  REQUEST_CONSULTATION_BUTTON_ID,
} from "./ConsultationsPanel";

const intl = createIntl({ locale: "en", messages });

const consultation = (overrides) => ({
  reason: null,
  preTreatment: false,
  requested: { at: "20/11/2026 10:15", by: "Paula Path" },
  outcome: null,
  outcomeNote: null,
  answered: null,
  ...overrides,
});

const awaited = consultation({
  id: 21,
  status: "REQUESTED",
  consultedUserId: "1003",
  consultedUser: "Ann Okello",
  reason: "Rare tumour",
  preTreatment: true,
});

const answered = consultation({
  id: 22,
  status: "ANSWERED",
  consultedUserId: "1004",
  consultedUser: "Brian Mutua",
  outcome: "DISAGREE",
  outcomeNote: "Favour a reactive process",
  answered: { at: "21/11/2026 16:40", by: "Brian Mutua" },
});

const withdrawn = consultation({
  id: 23,
  status: "WITHDRAWN",
  consultedUserId: "1005",
  consultedUser: "Joanna Annan",
});

const colleagues = [
  { id: "1003", value: "Ann Okello" },
  { id: "1004", value: "Brian Mutua" },
  { id: "1005", value: "Joanna Annan" },
];

const reasons = [
  { id: "9401", value: "Rare entity" },
  { id: "9402", value: "Patient request" },
];

const FREE = { disabled: false, reasonKey: null };
const SAVE_FIRST = {
  disabled: true,
  reasonKey: "pathology.locked.saveBeforeConsultation",
};

// The viewer is a pathologist who asked nobody here.
const PATHOLOGIST = "1001";

let onRequest;
let onAnswer;
let onWithdraw;

const props = (overrides = {}) => ({
  consultations: [awaited, answered, withdrawn],
  canRequest: true,
  viewerId: PATHOLOGIST,
  isPathologist: true,
  colleagues,
  reasons,
  suggestedFor: [],
  lock: FREE,
  held: false,
  shown: true,
  onRequest,
  onAnswer,
  onWithdraw,
  ...overrides,
});

const renderPanel = (overrides) => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <ConsultationsPanel {...props(overrides)} />
    </IntlProvider>,
  );
  return {
    ...view,
    rerenderWith: (more) =>
      view.rerender(
        <IntlProvider locale="en" messages={messages}>
          <ConsultationsPanel {...props({ ...overrides, ...more })} />
        </IntlProvider>,
      ),
  };
};

// By id: the role query bundled here names a disabled button by its title
// rather than by its aria-label.
const byId = (id) => document.getElementById(id);
const requestButton = () => byId(REQUEST_CONSULTATION_BUTTON_ID);
const askButton = () => byId("askColleague");
const colleagueBox = () => byId("consultationColleague");
const reasonSelect = () => byId("consultationReason");
const rowOf = (row) => screen.getByTestId("consultationRow-" + row.id);
const radio = (name) => screen.getByRole("radio", { name });

const listedOptions = () =>
  within(
    screen
      .getAllByRole("listbox")
      .find((list) => within(list).queryAllByRole("option").length > 0),
  )
    .getAllByRole("option")
    .map((option) => option.textContent);

const describedBy = (element) =>
  (element.getAttribute("aria-describedby") ?? "")
    .split(" ")
    .map((id) => document.getElementById(id)?.textContent)
    .join(" ");

const tagOf = (element) => element.closest(".cds--tag");

const chooseColleague = async (user, name) => {
  await user.click(colleagueBox());
  await user.click(screen.getByRole("option", { name }));
};

beforeEach(() => {
  onRequest = vi.fn(() => Promise.resolve({ ok: true }));
  onAnswer = vi.fn(() => Promise.resolve({ ok: true }));
  onWithdraw = vi.fn(() => Promise.resolve({ ok: true }));
});

describe("ConsultationsPanel, what it shows", () => {
  it("says no second opinion has been requested rather than showing an empty table, and offers Request second opinion only to someone who may request", () => {
    const view = renderPanel({ consultations: [] });

    expect(
      screen.getByText(messages["pathology.empty.noConsultation"]),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(requestButton()).toHaveTextContent(
      messages["pathology.action.requestConsultation"],
    );
    expect(requestButton()).toHaveAttribute("aria-expanded", "false");

    view.rerenderWith({ canRequest: false });

    expect(
      screen.getByText(messages["pathology.empty.noConsultation"]),
    ).toBeInTheDocument();
    expect(requestButton()).toBeNull();
  });

  it("heads four columns, and three when the viewer has no action on any row; each row shows the colleague with the reason, when and by whom it was requested with the before-treatment answer, and the status or the outcome with its note and time", () => {
    const view = renderPanel();
    const heads = () =>
      screen.getAllByRole("columnheader").map((cell) => cell.textContent);

    expect(heads()).toEqual([
      messages["pathology.label.consultedPathologist"],
      messages["pathology.label.consultationRequested"],
      messages["pathology.label.consultationOutcome"],
      messages["common.actions"],
    ]);

    const first = within(rowOf(awaited));
    expect(first.getByText("Ann Okello")).toBeInTheDocument();
    expect(first.getByText("Rare tumour")).toBeInTheDocument();
    expect(first.getByText("20/11/2026 10:15")).toBeInTheDocument();
    expect(
      first.getByText(
        intl.formatMessage(
          { id: "pathology.consultation.requestedBy" },
          { name: "Paula Path" },
        ),
      ),
    ).toBeInTheDocument();
    expect(
      first.getByText(messages["pathology.label.preTreatment"], {
        exact: false,
      }),
    ).toHaveTextContent(
      messages["pathology.label.preTreatment"] + " " + messages["label.yes"],
    );
    expect(
      first.getByText(messages["pathology.consultationStatus.REQUESTED"]),
    ).toBeInTheDocument();

    const second = within(rowOf(answered));
    expect(second.getByText("Brian Mutua")).toBeInTheDocument();
    expect(
      second.getByText(messages["pathology.label.preTreatment"], {
        exact: false,
      }),
    ).toHaveTextContent(
      messages["pathology.label.preTreatment"] + " " + messages["label.no"],
    );
    expect(
      second.getByText(messages["pathology.consultationOutcome.DISAGREE"]),
    ).toBeInTheDocument();
    expect(second.getByText("21/11/2026 16:40")).toBeInTheDocument();
    // The note is named as one to a screen reader, not on screen.
    const noteStamp = second.getByText("Favour a reactive process", {
      exact: false,
    });
    expect(noteStamp).toHaveTextContent(
      messages["common.notes"] + " Favour a reactive process",
    );
    expect(within(noteStamp).getByText(messages["common.notes"])).toHaveClass(
      "cds--visually-hidden",
    );

    // Each column's header and cells carry its own class, which sets its
    // width.
    const columnClasses = [
      "pathology-case-view__consultation-colleague",
      "pathology-case-view__consultation-requested",
      "pathology-case-view__consultation-outcome",
      "pathology-case-view__consultation-actions",
    ];
    screen
      .getAllByRole("columnheader")
      .forEach((cell, index) => expect(cell).toHaveClass(columnClasses[index]));
    within(rowOf(awaited))
      .getAllByRole("cell")
      .forEach((cell, index) => expect(cell).toHaveClass(columnClasses[index]));

    expect(
      within(rowOf(withdrawn)).getByText(
        messages["pathology.consultationStatus.WITHDRAWN"],
      ),
    ).toBeInTheDocument();

    // A colleague who was not asked, and is no pathologist, acts on no row.
    view.rerenderWith({
      viewerId: "2000",
      isPathologist: false,
      canRequest: false,
    });

    expect(heads()).toEqual([
      messages["pathology.label.consultedPathologist"],
      messages["pathology.label.consultationRequested"],
      messages["pathology.label.consultationOutcome"],
    ]);
    expect(screen.queryAllByRole("button")).toHaveLength(0);
  });
});

describe("ConsultationsPanel, requesting a second opinion", () => {
  it("the request form: the colleague is required and found by typing part of a name, the reason is optional, before treatment needs an answer, and Ask for the opinion says the first unmet rule in words", async () => {
    renderPanel();
    const user = userEvent.setup();

    fireEvent.click(requestButton());

    expect(requestButton()).toHaveAttribute("aria-expanded", "true");
    const body = screen.getAllByRole("rowgroup")[1];
    expect(within(body).getAllByRole("row")[0]).toContainElement(
      colleagueBox(),
    );
    await waitFor(() => expect(colleagueBox()).toHaveFocus());

    const expectHeldFor = (key) => {
      expect(askButton()).toBeDisabled();
      expect(askButton()).toHaveAttribute("title", messages[key]);
      expect(describedBy(askButton())).toBe(messages[key]);
      expect(byId("askColleagueHint")).toHaveTextContent(messages[key]);
    };

    expect(colleagueBox()).toHaveAttribute("aria-required", "true");
    expect(
      screen.getByText(messages["pathology.label.consultedPathologist"], {
        selector: "label",
      }),
    ).toBeInTheDocument();
    expect(reasonSelect()).not.toHaveAttribute("aria-required");
    expect(reasonSelect()).toHaveValue("");
    expect(reasonSelect().options[0]).toHaveTextContent(
      messages["common.none"],
    );
    expect(radio(messages["label.yes"])).not.toBeChecked();
    expect(radio(messages["label.no"])).not.toBeChecked();
    expectHeldFor("pathology.locked.consultationColleagueFirst");

    await user.type(colleagueBox(), "ann");
    expect(listedOptions()).toEqual(["Ann Okello", "Joanna Annan"]);
    await user.click(screen.getByRole("option", { name: "Joanna Annan" }));
    expectHeldFor("pathology.locked.consultationPreTreatmentFirst");

    fireEvent.click(radio(messages["label.yes"]));
    expect(askButton()).toBeEnabled();
    expect(askButton()).not.toHaveAttribute("title");
    expect(byId("askColleagueHint")).toBeNull();
  });

  it("an empty reasons list and a failed one each say so and still let the request go", async () => {
    const view = renderPanel({ reasons: [] });
    const user = userEvent.setup();

    fireEvent.click(requestButton());
    expect(reasonSelect()).toBeDisabled();
    expect(
      screen.getByText(messages["pathology.empty.noConsultationReasons"]),
    ).toBeInTheDocument();

    await chooseColleague(user, "Ann Okello");
    fireEvent.click(radio(messages["label.no"]));
    expect(askButton()).toBeEnabled();

    view.rerenderWith({ reasons: { failed: true } });
    expect(reasonSelect()).toBeDisabled();
    expect(
      screen.getByText(messages["pathology.error.consultationReasonsFailed"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["pathology.empty.noConsultationReasons"]),
    ).toBeNull();
    const sentBy = askButton();
    expect(sentBy).toBeEnabled();

    await act(async () => {
      fireEvent.click(sentBy);
    });
    expect(onRequest).toHaveBeenCalledWith(
      { consultedUserId: "1003", reasonId: null, preTreatment: false },
      sentBy,
    );
  });

  it("Ask for the opinion sends the colleague, the reason and the answer; accepted, the form closes and focus returns to Request second opinion; refused, it stays as typed", async () => {
    onRequest = vi.fn(() => Promise.resolve({ ok: false }));
    const view = renderPanel();
    const user = userEvent.setup();

    fireEvent.click(requestButton());
    await chooseColleague(user, "Brian Mutua");
    fireEvent.change(reasonSelect(), { target: { value: "9402" } });
    fireEvent.click(radio(messages["label.no"]));

    const sentBy = askButton();
    await act(async () => {
      fireEvent.click(sentBy);
    });
    expect(onRequest).toHaveBeenCalledWith(
      { consultedUserId: "1004", reasonId: "9402", preTreatment: false },
      sentBy,
    );

    // Refused: everything typed is still there to send again.
    expect(colleagueBox()).toHaveValue("Brian Mutua");
    expect(reasonSelect()).toHaveValue("9402");
    expect(radio(messages["label.no"])).toBeChecked();

    onRequest = vi.fn(() => Promise.resolve({ ok: true }));
    view.rerenderWith({ onRequest });
    await act(async () => {
      fireEvent.click(askButton());
    });

    expect(onRequest).toHaveBeenCalledTimes(1);
    expect(colleagueBox()).toBeNull();
    expect(requestButton()).toHaveAttribute("aria-expanded", "false");
    await waitFor(() => expect(requestButton()).toHaveFocus());

    // The next request starts empty.
    fireEvent.click(requestButton());
    expect(colleagueBox()).toHaveValue("");
    expect(radio(messages["label.no"])).not.toBeChecked();
  });
});

describe("ConsultationsPanel, the actions on a row", () => {
  it("Record outcome is offered only on the viewer's own row, needs an outcome, counts the note to 1,000 and sends the outcome and the note", async () => {
    // The consulted colleague, a pathologist, records and never withdraws.
    const view = renderPanel({ viewerId: "1003" });
    expect(byId("recordOutcome-21")).toBeInTheDocument();
    expect(byId("withdrawConsultation-21")).toBeNull();

    // The consulted colleague need not be a pathologist.
    view.rerenderWith({ isPathologist: false, canRequest: false });

    expect(byId("recordOutcome-21")).toBeInTheDocument();
    expect(byId("recordOutcome-22")).toBeNull();
    expect(byId("recordOutcome-23")).toBeNull();
    expect(byId("withdrawConsultation-21")).toBeNull();

    fireEvent.click(byId("recordOutcome-21"));
    const confirm = byId("consultationAnswerConfirm-21");
    const outcomeFirst = messages["pathology.locked.consultationOutcomeFirst"];
    expect(confirm).toBeDisabled();
    expect(confirm).toHaveAttribute("title", outcomeFirst);
    expect(describedBy(confirm)).toBe(outcomeFirst);

    const note = byId("consultationNote-21");
    expect(note).toHaveAttribute("maxLength", "1000");
    expect(screen.getByText("0/1000")).toBeInTheDocument();

    fireEvent.click(radio(messages["pathology.consultationOutcome.MODIFIED"]));
    expect(confirm).toBeEnabled();
    fireEvent.change(note, { target: { value: " Grade 2, not 3 " } });
    expect(screen.getByText("16/1000")).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(confirm);
    });
    expect(onAnswer).toHaveBeenCalledWith(
      awaited,
      { outcome: "MODIFIED", note: "Grade 2, not 3" },
      confirm,
    );
    expect(screen.queryByTestId("consultationAnswer-21")).toBeNull();
  });

  it("Withdraw asks first, names the colleague, sends on confirm, and Keep returns focus to the button that opened it", async () => {
    renderPanel();

    expect(byId("recordOutcome-21")).toBeNull();
    expect(byId("withdrawConsultation-22")).toBeNull();
    expect(byId("withdrawConsultation-23")).toBeNull();

    fireEvent.click(byId("withdrawConsultation-21"));
    const question = intl.formatMessage(
      { id: "pathology.label.consultationWithdrawConfirm" },
      { name: "Ann Okello" },
    );
    const inline = within(screen.getByTestId("consultationWithdraw-21"));
    expect(inline.getByText(question, { selector: "p" })).toBeVisible();
    // The row's one cell carries the class that breaks a long name.
    expect(inline.getByRole("cell")).toHaveClass(
      "pathology-case-view__consultation-form-cell",
    );
    expect(onWithdraw).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(byId("consultationWithdrawKeep-21")).toHaveFocus(),
    );
    // Focus lands on Keep, so Keep carries the question to a screen reader.
    expect(describedBy(byId("consultationWithdrawKeep-21"))).toBe(question);

    fireEvent.click(byId("consultationWithdrawKeep-21"));
    expect(screen.queryByTestId("consultationWithdraw-21")).toBeNull();
    expect(onWithdraw).not.toHaveBeenCalled();
    await waitFor(() => expect(byId("withdrawConsultation-21")).toHaveFocus());

    fireEvent.click(byId("withdrawConsultation-21"));
    const confirm = byId("consultationWithdrawConfirm-21");
    expect(confirm).toHaveTextContent(
      messages["pathology.action.withdrawConsultation"],
    );
    expect(describedBy(confirm)).toBe(question);
    await act(async () => {
      fireEvent.click(confirm);
    });
    expect(onWithdraw).toHaveBeenCalledWith(awaited, confirm);
    expect(screen.queryByTestId("consultationWithdraw-21")).toBeNull();
  });

  it("row buttons are named by their words and the colleague; the hold reason is written once and described on every held control", () => {
    const view = renderPanel();

    expect(
      screen.getByRole("button", {
        name: intl.formatMessage(
          { id: "pathology.action.withdrawConsultationOf" },
          { name: "Ann Okello" },
        ),
      }),
    ).toHaveTextContent(messages["pathology.action.withdrawConsultation"]);

    view.rerenderWith({ viewerId: "1003" });
    expect(
      screen.getByRole("button", {
        name: intl.formatMessage(
          { id: "pathology.action.recordOutcomeOf" },
          { name: "Ann Okello" },
        ),
      }),
    ).toHaveTextContent(messages["pathology.action.recordOutcome"]);

    // Open every form first, then hold the panel over unsaved work.
    view.rerenderWith({ viewerId: PATHOLOGIST });
    fireEvent.click(requestButton());
    fireEvent.click(byId("withdrawConsultation-21"));
    view.rerenderWith({ viewerId: PATHOLOGIST, lock: SAVE_FIRST });

    const holdText = messages["pathology.locked.saveBeforeConsultation"];
    expect(byId(CONSULTATIONS_HOLD_ID)).toHaveTextContent(holdText);
    expect(screen.getAllByText(holdText)).toHaveLength(1);
    [
      REQUEST_CONSULTATION_BUTTON_ID,
      "askColleague",
      "withdrawConsultation-21",
    ].forEach((id) => {
      expect(byId(id)).toBeDisabled();
      expect(byId(id)).toHaveAttribute("title", holdText);
      expect(describedBy(byId(id))).toBe(holdText);
    });
    const confirm = byId("consultationWithdrawConfirm-21");
    expect(confirm).toBeDisabled();
    expect(describedBy(confirm)).toContain(holdText);
  });

  it("the suggestion note names the diagnoses, and shows only to someone who may request", () => {
    const view = renderPanel({
      suggestedFor: ["C50.9 Breast", "C61 Prostate"],
    });

    const note = screen.getByTestId("review-second-opinion-suggested");
    expect(note).toHaveTextContent(
      intl.formatMessage(
        { id: "pathology.banner.secondOpinionSuggested" },
        { names: intl.formatList(["C50.9 Breast", "C61 Prostate"]) },
      ),
    );
    expect(note).toHaveTextContent("C50.9 Breast");
    expect(note).toHaveTextContent("C61 Prostate");
    expect(note.querySelector(".cds--inline-notification__close-button")).toBe(
      null,
    );

    view.rerenderWith({ canRequest: false });
    expect(screen.queryByTestId("review-second-opinion-suggested")).toBeNull();

    view.rerenderWith({ canRequest: true, suggestedFor: [] });
    expect(screen.queryByTestId("review-second-opinion-suggested")).toBeNull();
  });

  it("renders nothing while Review is locked", () => {
    const { container } = renderPanel({
      shown: false,
      suggestedFor: ["C61 Prostate"],
    });

    expect(container).toBeEmptyDOMElement();
  });

  it("status and outcome are words as well as colour", () => {
    renderPanel({
      consultations: [
        awaited,
        answered,
        withdrawn,
        consultation({
          id: 24,
          status: "ANSWERED",
          consultedUserId: "1006",
          consultedUser: "Grace Wanjiru",
          outcome: "AGREE",
          answered: { at: "22/11/2026 09:00", by: "Grace Wanjiru" },
        }),
        consultation({
          id: 25,
          status: "ANSWERED",
          consultedUserId: "1007",
          consultedUser: "Peter Kamau",
          outcome: "MODIFIED",
          answered: { at: "22/11/2026 11:00", by: "Peter Kamau" },
        }),
      ],
    });

    [
      [21, "pathology.consultationStatus.REQUESTED", "purple"],
      [22, "pathology.consultationOutcome.DISAGREE", "warm-gray"],
      [23, "pathology.consultationStatus.WITHDRAWN", "gray"],
      [24, "pathology.consultationOutcome.AGREE", "green"],
      [25, "pathology.consultationOutcome.MODIFIED", "warm-gray"],
    ].forEach(([id, key, colour]) => {
      const words = within(
        screen.getByTestId("consultationRow-" + id),
      ).getByText(messages[key]);
      expect(tagOf(words)).toHaveClass("cds--tag--" + colour);
      expect(tagOf(words)).toHaveTextContent(messages[key]);
    });
  });
});
