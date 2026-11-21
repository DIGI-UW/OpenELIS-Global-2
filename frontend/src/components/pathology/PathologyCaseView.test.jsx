/**
 * FR-2.4 and FR-2.5: a case is moved along the bench by the action bar's one
 * primary, whose label names where the case goes next, and back by a return
 * that asks for a reason. Each move is its own request naming the stage the
 * page shows, so the whole-case save never carries a stage at all. Before
 * this, a stage picked from a list was posted with the save, and nothing
 * recorded who moved the case or why it went back.
 */
import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider, createIntl } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext, ConfigurationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PathologyCaseView from "./PathologyCaseView";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerForPDF: vi.fn(),
    hasRole: vi.fn(() => false),
  };
});

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const CASE_ID = "9";
const CASE_URL = "/rest/pathology/caseView/" + CASE_ID;

const intl = createIntl({ locale: "en", messages });

// Where the server says a flagged case with every stage tracked moves next
// and back to. The rule itself is the server's; the screen only reads it.
const SERVED_MOVES = {
  ACCESSIONED: [null, "GROSSING"],
  GROSSING: ["ACCESSIONED", "DECALCIFICATION"],
  DECALCIFICATION: ["GROSSING", "PROCESSING"],
  PROCESSING: ["DECALCIFICATION", "EMBEDDING"],
  EMBEDDING: ["PROCESSING", "MICROTOMY"],
  MICROTOMY: ["EMBEDDING", "STAINING"],
  STAINING: ["MICROTOMY", "COVERSLIPPING"],
  COVERSLIPPING: ["STAINING", "READY_PATHOLOGIST"],
  READY_PATHOLOGIST: ["COVERSLIPPING", null],
  UNDER_REVIEW: ["READY_PATHOLOGIST", null],
  COMPLETED: [null, null],
};

// A case whose stored stage meets every rule the server checks on a move, so
// a test sees only the rule it is about.
const caseAtStage = (status, overrides = {}) => ({
  // Every field the case read carries for a move, explicitly, as D24 has it.
  previousStage: SERVED_MOVES[status][0],
  nextStage: SERVED_MOVES[status][1],
  runReference: null,
  runReferenceEditable: false,
  pathologySampleId: CASE_ID,
  patientPK: "1",
  labNumber: "ACC9",
  firstName: "A",
  lastName: "B",
  status,
  assignedTechnicianId: "7",
  assignedTechnician: "Tina Tech",
  grossExam: "Skin ellipse, 2 cm",
  decalcificationRequired: true,
  microscopyExam: "",
  conclusionText: "",
  blocks: [
    {
      id: 1,
      designation: "A1",
      barcode: "ACC9.A1",
      cassetteState: "BLOCK",
      active: true,
      location: "",
    },
  ],
  slides: [
    {
      id: 5,
      designation: "1",
      barcode: "ACC9.A1.1",
      blockId: 1,
      active: true,
      location: "",
    },
  ],
  // The slide's label has been checked against its block, as microtomy
  // requires before the case is sent on.
  labelVerifications: {
    5: { verifiedAt: "02/11/2026 10:05", verifiedBy: "Tech, Tina" },
  },
  reports: [],
  requests: [],
  techniques: [],
  conclusions: [],
  immunoHistoChemistryTestIds: [],
  stageEvents: [],
  ...overrides,
});

const stageEvent = (overrides) => ({
  id: 1,
  stage: "GROSSING",
  direction: "FORWARD",
  enteredAt: "02/11/2026 09:15",
  enteredBy: "Tech, Tina",
  completedAt: null,
  completedBy: null,
  reason: null,
  runReference: null,
  ...overrides,
});

let servedCase;
const addNotification = vi.fn();

const renderCaseView = (configurationProperties = {}) =>
  render(
    <ConfigurationContext.Provider value={{ configurationProperties }}>
      <MemoryRouter initialEntries={["/PathologyCaseView/" + CASE_ID]}>
        <IntlProvider locale="en" messages={messages}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification,
            }}
          >
            <UserSessionDetailsContext.Provider
              value={{
                userSessionDetails: {
                  userId: "1",
                  firstName: "A",
                  lastName: "B",
                  roles: [],
                },
              }}
            >
              <Route path="/PathologyCaseView/:pathologySampleId">
                <PathologyCaseView />
              </Route>
            </UserSessionDetailsContext.Provider>
          </NotificationContext.Provider>
        </IntlProvider>
      </MemoryRouter>
    </ConfigurationContext.Provider>,
  );

const loaded = () =>
  waitFor(() => expect(document.getElementById("caseStage")).not.toBeNull());

const actionGroup = () =>
  document.querySelector(".case-view__action-bar-actions");

// By its own text: the role query bundled here names a disabled button by its
// title rather than by what it says.
const primaryButton = (labelKey) =>
  within(actionGroup()).getByText(messages[labelKey], { selector: "button" });

const returnButton = () => document.getElementById("returnStage");

const confirmReturnButton = () => document.getElementById("returnStageConfirm");

const reasonField = () => document.getElementById("returnStageReason");

const sectionHeader = (sectionId) =>
  document.querySelector("#" + sectionId + " button.cds--accordion__heading");

const saveDraftButton = () =>
  screen.getByRole("button", { name: messages["caseView.action.saveDraft"] });

const caseFetchCount = () =>
  getFromOpenElisServer.mock.calls.filter(([url]) => url === CASE_URL).length;

const lastJsonPost = () => postToOpenElisServerJsonResponse.mock.calls.at(-1);

// Answers the move in flight as the server would.
const answerMove = async (response) => {
  const callback = lastJsonPost()[2];
  await act(async () => {
    callback(response);
  });
};

const describedBy = (element) =>
  document.getElementById(element.getAttribute("aria-describedby"));

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  postToOpenElisServerJsonResponse.mockReset();
  addNotification.mockReset();
  servedCase = caseAtStage("GROSSING");
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === CASE_URL) {
      return callback({ ...servedCase });
    }
    if (url.startsWith("/rest/paginatedDisplayList/")) {
      return callback({
        displayItems: [],
        paging: { totalPages: 1, currentPage: 1 },
      });
    }
    return callback([]);
  });
});

describe("PathologyCaseView stage transitions", () => {
  it.each([
    ["ACCESSIONED", "pathology.action.beginGrossing"],
    ["GROSSING", "pathology.action.sendForDecalc"],
    ["DECALCIFICATION", "pathology.action.sendForProcessing"],
    ["PROCESSING", "pathology.action.sendForEmbedding"],
    ["EMBEDDING", "pathology.action.sendToMicrotomy"],
    ["MICROTOMY", "pathology.action.sendForStaining"],
    ["STAINING", "pathology.action.sendForCoverslip"],
    ["COVERSLIPPING", "pathology.action.sendToPathologist"],
  ])(
    "names where the server says a case at %s goes next on the primary",
    async (status, labelKey) => {
      servedCase = caseAtStage(status);
      renderCaseView();
      await loaded();

      expect(primaryButton(labelKey)).toBeEnabled();
      expect(within(actionGroup()).getAllByRole("button")).toHaveLength(3);
    },
  );

  // Skipping a stage the laboratory does not track, or a decalcification
  // grossing did not flag, is the server's rule; the label follows its
  // answer, whatever this browser's own configuration says.
  it("labels the primary after the stage the server names, not the next one in bench order", async () => {
    servedCase = caseAtStage("STAINING", { nextStage: "READY_PATHOLOGIST" });
    const skipped = renderCaseView({
      PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "true",
    });
    await loaded();
    expect(primaryButton("pathology.action.sendToPathologist")).toBeEnabled();
    expect(
      within(actionGroup()).queryByText(
        messages["pathology.action.sendForCoverslip"],
      ),
    ).toBeNull();
    skipped.unmount();

    servedCase = caseAtStage("GROSSING", {
      decalcificationRequired: false,
      nextStage: "PROCESSING",
    });
    renderCaseView();
    await loaded();
    expect(primaryButton("pathology.action.sendForProcessing")).toBeEnabled();
  });

  it.each(["READY_PATHOLOGIST", "UNDER_REVIEW", "COMPLETED"])(
    "offers no primary on a case at %s, where the server names no next stage",
    async (status) => {
      servedCase = caseAtStage(status);
      renderCaseView();
      await loaded();

      expect(within(actionGroup()).getAllByRole("button")).toHaveLength(2);
    },
  );

  it("offers no primary on a case whose read carries no next stage at all", async () => {
    servedCase = caseAtStage("PROCESSING");
    delete servedCase.nextStage;
    renderCaseView();
    await loaded();

    expect(within(actionGroup()).getAllByRole("button")).toHaveLength(2);
  });

  it("holds grossing back until the specimen is described and cut, and says so on the button and to a screen reader", async () => {
    servedCase = caseAtStage("GROSSING", { grossExam: "", blocks: [] });
    renderCaseView();
    await loaded();

    const primary = primaryButton("pathology.action.sendForDecalc");
    const reason = messages["pathology.locked.grossingIncomplete"];
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute("title", reason);
    expect(describedBy(primary)).toHaveTextContent(reason);
  });

  it("says to save first once the form holds unsaved work, ahead of the stage's own rule", async () => {
    servedCase = caseAtStage("GROSSING", { grossExam: "", blocks: [] });
    renderCaseView();
    await loaded();

    fireEvent.change(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
      { target: { value: "Skin ellipse" } },
    );

    const primary = primaryButton("pathology.action.sendForDecalc");
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeTransition"],
    );
  });

  it("holds embedding back until every cassette is a block, naming the ones outstanding", async () => {
    servedCase = caseAtStage("EMBEDDING", {
      blocks: [
        { id: 1, designation: "A1", cassetteState: "BLOCK", active: true },
        { id: 2, designation: "A2", cassetteState: "CASSETTE", active: true },
      ],
    });
    renderCaseView();
    await loaded();

    const primary = primaryButton("pathology.action.sendToMicrotomy");
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute(
      "title",
      intl.formatMessage(
        { id: "pathology.locked.embeddingIncomplete" },
        {
          outstanding: intl.formatMessage(
            { id: "pathology.badge.outstanding" },
            { designations: "A2" },
          ),
        },
      ),
    );
  });

  it("holds embedding back on a case with no cassette, and says to cut one at grossing", async () => {
    servedCase = caseAtStage("EMBEDDING", { blocks: [] });
    renderCaseView();
    await loaded();

    const primary = primaryButton("pathology.action.sendToMicrotomy");
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute(
      "title",
      messages["pathology.locked.embeddingNoCassettes"],
    );
  });

  it("holds microtomy back until a slide is saved, and lets one saved slide send the case on", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      slides: [
        // A retired slide is on record but is not work in hand.
        {
          id: 6,
          designation: "2",
          blockId: 1,
          active: false,
          location: "",
        },
      ],
    });
    const withNone = renderCaseView();
    await loaded();

    const reason = messages["pathology.locked.microtomyIncomplete"];
    let primary = primaryButton("pathology.action.sendForStaining");
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute("title", reason);
    expect(describedBy(primary)).toHaveTextContent(reason);
    withNone.unmount();

    servedCase = caseAtStage("MICROTOMY");
    renderCaseView();
    await loaded();

    primary = primaryButton("pathology.action.sendForStaining");
    expect(primary).toBeEnabled();
    expect(primary).not.toHaveAttribute("title");
  });

  // FR-7.5: a slide whose label was never checked against its block holds
  // the case at microtomy, named, as the server holds it.
  it("holds microtomy back while a slide's label is unchecked, naming it, and lets the case on once the read covers it", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      slides: [
        ...caseAtStage("MICROTOMY").slides,
        {
          id: 6,
          designation: "2",
          barcode: "ACC9.A1.2",
          blockId: 1,
          active: true,
          location: "",
        },
      ],
    });
    const unchecked = renderCaseView();
    await loaded();

    const reason = intl.formatMessage(
      { id: "pathology.locked.slidesUnverified" },
      {
        outstanding: intl.formatMessage(
          { id: "pathology.badge.outstanding" },
          { designations: "A1.2" },
        ),
      },
    );
    let primary = primaryButton("pathology.action.sendForStaining");
    expect(primary).toBeDisabled();
    expect(primary).toHaveAttribute("title", reason);
    expect(describedBy(primary)).toHaveTextContent(reason);
    unchecked.unmount();

    servedCase = {
      ...servedCase,
      labelVerifications: {
        ...servedCase.labelVerifications,
        6: { verifiedAt: "02/11/2026 10:06", verifiedBy: "Tech, Tina" },
      },
    };
    renderCaseView();
    await loaded();

    primary = primaryButton("pathology.action.sendForStaining");
    expect(primary).toBeEnabled();
    expect(primary).not.toHaveAttribute("title");
  });

  it("posts the stage the page shows to the advance endpoint, then says where the case went and reads it back", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();
    const fetchesBefore = caseFetchCount();

    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));

    const [url, body] = lastJsonPost();
    expect(url).toBe(CASE_URL + "/advance");
    expect(JSON.parse(body)).toEqual({ from: "PROCESSING" });

    servedCase = caseAtStage("EMBEDDING");
    await answerMove({ status: "EMBEDDING" });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: intl.formatMessage(
          { id: "pathology.toast.stageAdvanced" },
          { stage: messages["pathology.stage.embedding"] },
        ),
      }),
    );
    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(document.getElementById("caseStage")).toHaveTextContent(
      messages["pathology.stage.embedding"],
    );
    await waitFor(() =>
      expect(
        document.querySelector(
          "#pathology-section-embedding button.cds--accordion__heading",
        ),
      ).toHaveFocus(),
    );
  });

  it("posts nothing for a second click while the first move is unanswered", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
  });

  // Until the case is read again the page still shows the stage it left, so
  // a second press would post that stage and come back as a conflict.
  const holdCaseReads = () => {
    const held = [];
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        held.push(callback);
        return;
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });
    return held;
  };

  it("keeps the moves and the form held until the case is read again after a move lands", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();
    const heldReads = holdCaseReads();

    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    await answerMove({ status: "EMBEDDING" });

    expect(heldReads).toHaveLength(1);
    expect(saveDraftButton()).toBeDisabled();
    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);

    await act(async () => {
      heldReads[0]({ ...caseAtStage("EMBEDDING") });
    });

    expect(saveDraftButton()).toBeEnabled();
    expect(document.getElementById("caseStage")).toHaveTextContent(
      messages["pathology.stage.embedding"],
    );
  });

  it("lets go of the moves when the read after a move fails, leaving the page as it was", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();
    const heldReads = holdCaseReads();

    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    await answerMove({ status: "EMBEDDING" });
    await act(async () => {
      heldReads[0](undefined);
    });

    expect(saveDraftButton()).toBeEnabled();
    expect(document.getElementById("caseStage")).toHaveTextContent(
      messages["pathology.stage.processing"],
    );
  });

  it("never posts a stage with the case save", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const [url, body] = postToOpenElisServerFullResponse.mock.calls.at(-1);
    expect(url).toBe(CASE_URL);
    expect(JSON.parse(body)).not.toHaveProperty("status");
  });

  it("shows a refusal the server names in the reader's language, with its values, and never the server's sentence", async () => {
    servedCase = caseAtStage("EMBEDDING");
    renderCaseView();
    await loaded();
    const fetchesBefore = caseFetchCount();

    fireEvent.click(primaryButton("pathology.action.sendToMicrotomy"));
    await answerMove({
      status: 400,
      error: "pathology case 9 has cassettes still to embed: A2",
      messageKey: "pathology.error.embeddingIncomplete",
      messageArgs: { designations: "A2" },
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: intl.formatMessage(
          { id: "pathology.error.embeddingIncomplete" },
          { designations: "A2" },
        ),
      }),
    );
    expect(JSON.stringify(addNotification.mock.calls)).not.toContain(
      "pathology case 9",
    );
    // A refusal can mean another change landed since the page was read, so
    // the clean form is read again rather than left stale.
    expect(caseFetchCount()).toBe(fetchesBefore + 1);
  });

  it.each(["stage", "version"])(
    "says the case has moved on and reads it again when the server answers a %s conflict",
    async (conflict) => {
      servedCase = caseAtStage("PROCESSING");
      renderCaseView();
      await loaded();
      const fetchesBefore = caseFetchCount();

      fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
      servedCase = caseAtStage("EMBEDDING");
      await answerMove({ status: 409, conflict, error: "reload the case" });

      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["pathology.error.stageConflict"],
        }),
      );
      expect(caseFetchCount()).toBe(fetchesBefore + 1);
      expect(document.getElementById("caseStage")).toHaveTextContent(
        messages["pathology.stage.embedding"],
      );
    },
  );

  it("reports any other failed move as the ordinary failure, and lets it be tried again", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    await answerMove({ status: 0, error: "Failed to fetch" });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["error.save.msg"],
      }),
    );
    fireEvent.click(primaryButton("pathology.action.sendForEmbedding"));
    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(2);
  });

  it("translates a save the server refuses by message id", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
    await act(async () => {
      await callback({
        status: 400,
        json: async () => ({
          error: "run reference too long",
          messageKey: "pathology.error.runReferenceTooLong",
          messageArgs: {},
        }),
      });
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["pathology.error.runReferenceTooLong"],
      }),
    );
  });
});

describe("while a move is out, and where focus goes after it", () => {
  it("holds the form read-only, Save and both moves back while a move is out, and says why", async () => {
    servedCase = caseAtStage("GROSSING");
    renderCaseView();
    await loaded();

    fireEvent.click(primaryButton("pathology.action.sendForDecalc"));

    expect(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
    ).toBeDisabled();
    expect(saveDraftButton()).toBeDisabled();
    expect(primaryButton("pathology.action.sendForDecalc")).toHaveAttribute(
      "title",
      messages["pathology.locked.requestInFlight"],
    );
    expect(returnButton()).toBeDisabled();
    fireEvent.change(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
      { target: { value: "Typed while the move was out" } },
    );
    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();

    servedCase = caseAtStage("DECALCIFICATION");
    await answerMove({ status: "DECALCIFICATION" });

    expect(saveDraftButton()).toBeEnabled();
    expect(document.getElementById("caseStage")).toHaveTextContent(
      messages["pathology.stage.decalcification"],
    );
  });

  it("takes focus from the control that started the move to the new stage's section", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    const primary = primaryButton("pathology.action.sendForEmbedding");
    primary.focus();
    fireEvent.click(primary);
    servedCase = caseAtStage("EMBEDDING");
    await answerMove({ status: "EMBEDDING" });

    await waitFor(() =>
      expect(sectionHeader("pathology-section-embedding")).toHaveFocus(),
    );
  });

  it("leaves focus on a control the person moved to while the request was out", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    const primary = primaryButton("pathology.action.sendForEmbedding");
    primary.focus();
    fireEvent.click(primary);
    const elsewhere = sectionHeader("pathology-section-case-info");
    elsewhere.focus();
    servedCase = caseAtStage("EMBEDDING");
    await answerMove({ status: "EMBEDDING" });
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));

    expect(elsewhere).toHaveFocus();
  });

  // The review section is read-only to a technician but its header still
  // opens, so it is where a technician who sent the case on lands.
  it("puts a technician who sent the case to the pathologist on the review section's header", async () => {
    servedCase = caseAtStage("COVERSLIPPING");
    renderCaseView();
    await loaded();

    fireEvent.click(primaryButton("pathology.action.sendToPathologist"));
    servedCase = caseAtStage("READY_PATHOLOGIST");
    await answerMove({ status: "READY_PATHOLOGIST" });

    await waitFor(() =>
      expect(sectionHeader("pathology-section-review")).toHaveFocus(),
    );
  });

  it("falls back to the return control when the stage moved to has no section here", async () => {
    servedCase = caseAtStage("COVERSLIPPING");
    renderCaseView();
    await loaded();

    fireEvent.click(primaryButton("pathology.action.sendToPathologist"));
    servedCase = caseAtStage("READY_PATHOLOGIST");
    await answerMove({ status: "A_STAGE_THIS_BUILD_DOES_NOT_KNOW" });

    await waitFor(() => expect(returnButton()).toHaveFocus());
  });
});

describe("returning a case to an earlier stage", () => {
  it("asks for a reason inline, holds the return back until one is given, and posts it with the stage the page shows", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    const confirm = confirmReturnButton();
    expect(confirm).toHaveTextContent(
      intl.formatMessage(
        { id: "pathology.action.returnTo" },
        { stage: messages["pathology.stage.decalcification"] },
      ),
    );
    expect(confirm).toBeDisabled();
    expect(confirm).toHaveAttribute(
      "title",
      messages["pathology.locked.returnReasonRequired"],
    );
    expect(describedBy(confirm)).toHaveTextContent(
      messages["pathology.locked.returnReasonRequired"],
    );

    const reason = reasonField();
    expect(reason).toHaveAccessibleDescription(
      messages["pathology.label.returnReasonHelper"],
    );
    fireEvent.change(reason, { target: { value: "   " } });
    expect(confirm).toBeDisabled();

    fireEvent.change(reason, { target: { value: "  Decalcify further  " } });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);

    const [url, body] = lastJsonPost();
    expect(url).toBe(CASE_URL + "/return");
    expect(JSON.parse(body)).toEqual({
      from: "PROCESSING",
      reason: "Decalcify further",
    });

    servedCase = caseAtStage("DECALCIFICATION");
    await answerMove({ status: "DECALCIFICATION" });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: intl.formatMessage(
          { id: "pathology.toast.stageReturned" },
          { stage: messages["pathology.stage.decalcification"] },
        ),
      }),
    );
    expect(confirmReturnButton()).toBeNull();
    expect(returnButton()).toBeEnabled();
    await waitFor(() =>
      expect(sectionHeader("pathology-section-decalcification")).toHaveFocus(),
    );
  });

  it("puts focus in the reason field when the row opens", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());

    await waitFor(() => expect(reasonField()).toHaveFocus());
  });

  it("puts focus back on the return control when the row is cancelled", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());
    fireEvent.click(
      screen.getByRole("button", { name: messages["common.cancel"] }),
    );

    await waitFor(() => expect(returnButton()).toHaveFocus());
  });

  it("marks the reason as required, to the eye and to a screen reader", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());

    expect(reasonField()).toHaveAttribute("aria-required", "true");
    expect(
      document.querySelector('label[for="returnStageReason"] .requiredlabel'),
    ).toHaveAttribute("aria-hidden", "true");
  });

  // The stage the reason was typed against has gone, so the row closes
  // rather than offering a return to a different stage under the old reason.
  it("closes the row and drops the reason when another move got there first", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());
    fireEvent.change(reasonField(), { target: { value: "Wrong program" } });
    fireEvent.click(confirmReturnButton());
    servedCase = caseAtStage("EMBEDDING");
    await answerMove({ status: 409, conflict: "stage", error: "reload" });

    expect(confirmReturnButton()).toBeNull();
    fireEvent.click(returnButton());
    expect(reasonField()).toHaveValue("");
  });

  // Closing the row while the return is out would drop focus on the page:
  // the return control it hands focus back to is itself held until then.
  it("holds Cancel back while the return is out, and closes the row once it lands", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());
    fireEvent.change(reasonField(), { target: { value: "Decalcify further" } });
    fireEvent.click(confirmReturnButton());

    const cancel = screen.getByRole("button", {
      name: messages["common.cancel"],
    });
    expect(cancel).toBeDisabled();
    fireEvent.click(cancel);
    expect(confirmReturnButton()).not.toBeNull();

    servedCase = caseAtStage("DECALCIFICATION");
    await answerMove({ status: "DECALCIFICATION" });

    expect(confirmReturnButton()).toBeNull();
    expect(returnButton()).toBeEnabled();
  });

  it("closes the reason row on Cancel without posting anything", async () => {
    servedCase = caseAtStage("PROCESSING");
    renderCaseView();
    await loaded();

    fireEvent.click(returnButton());
    fireEvent.change(reasonField(), { target: { value: "Wrong stage" } });
    fireEvent.click(
      screen.getByRole("button", { name: messages["common.cancel"] }),
    );

    expect(confirmReturnButton()).toBeNull();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it.each([
    ["ACCESSIONED", {}, "pathology.locked.nothingToReturnTo"],
    [
      "PROCESSING",
      { previousStage: null },
      "pathology.locked.nothingToReturnTo",
    ],
    ["COMPLETED", {}, "pathology.locked.caseCompleted"],
  ])(
    "holds the return back on a case at %s, with nothing earlier to go back to or signed out, and says why",
    async (status, overrides, reasonKey) => {
      servedCase = caseAtStage(status, overrides);
      renderCaseView();
      await loaded();

      expect(returnButton()).toBeDisabled();
      expect(returnButton()).toHaveAttribute("title", messages[reasonKey]);
      expect(describedBy(returnButton())).toHaveTextContent(
        messages[reasonKey],
      );
    },
  );

  it("holds the return back while the form holds unsaved work", async () => {
    servedCase = caseAtStage("GROSSING");
    renderCaseView();
    await loaded();

    expect(returnButton()).toBeEnabled();
    fireEvent.change(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
      { target: { value: "Changed" } },
    );

    expect(returnButton()).toBeDisabled();
    expect(returnButton()).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeTransition"],
    );
  });
});

describe("the processor run, recorded at processing", () => {
  const openProcessingEntry = stageEvent({
    id: 3,
    stage: "PROCESSING",
    runReference: "RUN-11",
  });

  const runReferenceField = () =>
    screen.getByLabelText(messages["pathology.label.runReference"]);

  const saveAndReadBody = async () => {
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    return JSON.parse(postToOpenElisServerFullResponse.mock.calls.at(-1)[1]);
  };

  it("says no run registry exists yet, and posts the run typed while the server says it can still be recorded", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [{ ...openProcessingEntry, runReference: null }],
      runReference: null,
      runReferenceEditable: true,
    });
    renderCaseView();
    await loaded();

    expect(
      screen.getByText(messages["pathology.banner.runReferenceInterimTitle"]),
    ).toBeInTheDocument();
    expect(runReferenceField()).toBeEnabled();
    expect(runReferenceField()).not.toHaveAttribute("readonly");
    expect(runReferenceField()).toHaveAttribute("maxLength", "64");
    expect(runReferenceField()).toHaveAccessibleDescription(
      messages["pathology.label.runReferenceOnce"],
    );

    fireEvent.change(runReferenceField(), { target: { value: "RUN-12" } });

    expect((await saveAndReadBody()).runReference).toBe("RUN-12");
  });

  // Written once per processing entry: once recorded it is read, and the save
  // carries nothing that could overwrite it.
  it("shows a run already recorded on the open entry read-only, and never posts it", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [openProcessingEntry],
      runReference: "RUN-11",
      runReferenceEditable: false,
    });
    renderCaseView();
    await loaded();

    expect(runReferenceField()).toHaveValue("RUN-11");
    expect(runReferenceField()).toHaveAttribute("readonly");
    expect(runReferenceField()).toHaveAccessibleDescription(
      messages["pathology.label.runReferenceOnce"],
    );
    fireEvent.change(runReferenceField(), { target: { value: "RUN-99" } });
    expect(runReferenceField()).toHaveValue("RUN-11");

    expect(await saveAndReadBody()).not.toHaveProperty("runReference");
  });

  it("holds the field back on a case at processing with no recorded entry, and says how to record one", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [],
      runReferenceEditable: false,
    });
    renderCaseView();
    await loaded();

    expect(runReferenceField()).toBeDisabled();
    expect(runReferenceField()).toHaveAccessibleDescription(
      messages["pathology.locked.noStageEvent"],
    );
  });

  it("shows the run past the stage, read-only", async () => {
    servedCase = caseAtStage("EMBEDDING", {
      stageEvents: [
        {
          ...openProcessingEntry,
          completedAt: "02/11/2026 10:00",
          completedBy: "Tech, Tina",
        },
      ],
      runReference: "RUN-11",
      runReferenceEditable: false,
    });
    renderCaseView();
    await loaded();

    expect(runReferenceField()).toHaveValue("RUN-11");
    expect(runReferenceField()).toHaveAttribute("readonly");
    expect(runReferenceField()).not.toHaveAccessibleDescription();
  });

  it("lists each processing entry with its run, so a run corrected by a return stays visible", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [
        stageEvent({
          id: 3,
          stage: "PROCESSING",
          runReference: "RUN-11",
          completedAt: "02/11/2026 10:00",
          completedBy: "Smith, Sam",
        }),
        stageEvent({
          id: 5,
          stage: "PROCESSING",
          direction: "RETURN",
          enteredAt: "02/11/2026 10:30",
          reason: "Wrong program",
          runReference: "RUN-12",
        }),
      ],
      runReference: "RUN-12",
    });
    renderCaseView();
    await loaded();

    const list = within(
      document.getElementById("pathology-section-processing"),
    ).getByRole("table", { name: messages["pathology.stage.processing"] });
    const rows = within(list).getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent(messages["pathology.label.runReference"]);
    expect(rows[0]).not.toHaveTextContent(messages["pathology.label.stage"]);
    expect(rows[1]).toHaveTextContent("RUN-11");
    expect(rows[1]).toHaveTextContent("Smith, Sam");
    expect(rows[2]).toHaveTextContent("RUN-12");
    expect(rows[2]).toHaveTextContent(messages["pathology.badge.returned"]);
  });

  // The section is narrow, and a Reason column on its right was cut off
  // there; a one-stage list keeps the reason with the Returned tag instead.
  it("keeps a return's reason with its Returned tag rather than in a column of its own", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [
        stageEvent({
          id: 5,
          stage: "PROCESSING",
          direction: "RETURN",
          enteredAt: "02/11/2026 10:30",
          reason: "Wrong program",
          runReference: "RUN-12",
        }),
      ],
    });
    renderCaseView();
    await loaded();

    const list = within(
      document.getElementById("pathology-section-processing"),
    ).getByRole("table", { name: messages["pathology.stage.processing"] });
    const [head, entry] = within(list).getAllByRole("row");
    expect(head).not.toHaveTextContent(messages["common.reason"]);
    const cells = entry.querySelectorAll(".cds--structured-list-td");
    expect(head.querySelectorAll(".cds--structured-list-th")).toHaveLength(
      cells.length,
    );
    expect(cells[1]).toHaveTextContent("02/11/2026 10:30");
    expect(cells[1]).toHaveTextContent(messages["pathology.badge.returned"]);
    expect(cells[1]).toHaveTextContent("Wrong program");
  });

  // The run is what tells one processing entry from the next, so it leads
  // the processing list.
  it("leads the processing list with the processor run", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [
        stageEvent({
          id: 3,
          stage: "PROCESSING",
          enteredAt: "02/11/2026 10:00",
          runReference: "RUN-11",
        }),
      ],
    });
    renderCaseView();
    await loaded();

    const list = within(
      document.getElementById("pathology-section-processing"),
    ).getByRole("table", { name: messages["pathology.stage.processing"] });
    const [head, entry] = within(list).getAllByRole("row");
    expect(
      head.querySelectorAll(".cds--structured-list-th")[0],
    ).toHaveTextContent(messages["pathology.label.runReference"]);
    const cells = entry.querySelectorAll(".cds--structured-list-td");
    expect(cells[0]).toHaveTextContent("RUN-11");
    expect(cells[1]).toHaveTextContent("02/11/2026 10:00");
  });

  // A list wider than the section scrolls inside it, so the scrolling box
  // must be reachable from the keyboard and named for a screen reader.
  it("makes the scrolling list a named stop in the tab order", async () => {
    servedCase = caseAtStage("PROCESSING", {
      stageEvents: [openProcessingEntry],
      runReference: "RUN-11",
    });
    renderCaseView();
    await loaded();

    const region = within(
      document.getElementById("pathology-section-processing"),
    ).getByRole("region", { name: messages["pathology.stage.processing"] });
    expect(region).toHaveClass("pathology-case-view__stage-events");
    expect(region).toHaveAttribute("tabindex", "0");
    expect(
      within(region).getByRole("table", {
        name: messages["pathology.stage.processing"],
      }),
    ).toBeInTheDocument();
  });
});

describe("the stage history in Case Information", () => {
  const caseInfo = () => document.getElementById("pathology-section-case-info");

  it("lists who moved the case and when, and marks a return with its reason", async () => {
    servedCase = caseAtStage("GROSSING", {
      stageEvents: [
        stageEvent({
          id: 1,
          stage: "GROSSING",
          completedAt: "02/11/2026 09:40",
          completedBy: "Tech, Tina",
        }),
        stageEvent({
          id: 2,
          stage: "PROCESSING",
          enteredAt: "02/11/2026 09:40",
          completedAt: "02/11/2026 10:05",
          completedBy: "Smith, Sam",
        }),
        stageEvent({
          id: 3,
          stage: "GROSSING",
          direction: "RETURN",
          enteredAt: "02/11/2026 10:05",
          enteredBy: "Smith, Sam",
          reason: "Recut the margin",
        }),
      ],
    });
    renderCaseView();
    await loaded();

    const history = within(caseInfo()).getByRole("table", {
      name: messages["pathology.label.stageHistory"],
    });
    const rows = within(history).getAllByRole("row");
    expect(rows).toHaveLength(4);
    expect(rows[0]).toHaveTextContent(messages["pathology.label.enteredBy"]);
    // An entry's completion is the row after it, so the two completion
    // columns stay out of the case's own history and leave the reason room.
    expect(rows[0]).not.toHaveTextContent(
      messages["pathology.label.completedBy"],
    );
    expect(rows[0]).not.toHaveTextContent(messages["common.completed"]);
    expect(rows[1]).toHaveTextContent(messages["pathology.stage.grossing"]);
    expect(rows[1]).not.toHaveTextContent(messages["pathology.badge.returned"]);
    expect(rows[2]).toHaveTextContent(messages["pathology.stage.processing"]);
    expect(rows[2]).toHaveTextContent("Tech, Tina");
    expect(rows[2]).not.toHaveTextContent("Smith, Sam");
    expect(rows[3]).toHaveTextContent(messages["pathology.badge.returned"]);
    expect(rows[3]).toHaveTextContent("Smith, Sam");
    expect(rows[3]).toHaveTextContent("Recut the margin");
    expect(within(caseInfo()).queryByRole("textbox")).toBeNull();
  });

  it("keeps the reason in its own last column, and makes the scrolling list a named stop in the tab order", async () => {
    servedCase = caseAtStage("GROSSING", {
      stageEvents: [
        stageEvent({
          id: 3,
          stage: "GROSSING",
          direction: "RETURN",
          enteredAt: "02/11/2026 10:05",
          reason: "Recut the margin",
        }),
      ],
    });
    renderCaseView();
    await loaded();

    const region = within(caseInfo()).getByRole("region", {
      name: messages["pathology.label.stageHistory"],
    });
    expect(region).toHaveAttribute("tabindex", "0");
    const [head, entry] = within(region).getAllByRole("row");
    const headers = head.querySelectorAll(".cds--structured-list-th");
    expect(headers[headers.length - 1]).toHaveTextContent(
      messages["common.reason"],
    );
    const cells = entry.querySelectorAll(".cds--structured-list-td");
    expect(cells).toHaveLength(headers.length);
    expect(cells[cells.length - 1]).toHaveTextContent("Recut the margin");
    expect(cells[1]).not.toHaveTextContent("Recut the margin");
    expect(headers[0]).toHaveTextContent(messages["pathology.label.stage"]);
  });

  it("says no stage change has been recorded on a case with none", async () => {
    servedCase = caseAtStage("GROSSING", { stageEvents: [] });
    renderCaseView();
    await loaded();

    expect(caseInfo()).toHaveTextContent(
      messages["pathology.empty.noStageEvents"],
    );
  });
});

describe("Embed and Deactivate: refusals and the signed-out lock", () => {
  it("shows an embedding the server refuses by message id in the reader's language", async () => {
    servedCase = caseAtStage("EMBEDDING", {
      blocks: [
        {
          id: 1,
          designation: "A1",
          barcode: "ACC9.A1",
          cassetteState: "CASSETTE",
          active: true,
        },
      ],
    });
    renderCaseView();
    await loaded();

    fireEvent.click(document.getElementById("embedCassette-1"));
    expect(lastJsonPost()[0]).toBe("/rest/pathology/block/1/embed");
    await answerMove({
      status: 400,
      error: "pathology case 9 is signed out",
      messageKey: "pathology.error.caseCompleted",
      messageArgs: {},
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["pathology.error.caseCompleted"],
      }),
    );
  });

  it("holds Embed and Deactivate back on a completed case and says why", async () => {
    servedCase = caseAtStage("COMPLETED", {
      blocks: [
        {
          id: 1,
          designation: "A1",
          barcode: "ACC9.A1",
          cassetteState: "CASSETTE",
          active: true,
        },
      ],
      slides: [
        {
          id: 4,
          designation: "1",
          blockId: 1,
          barcode: "ACC9.A1.1",
          active: true,
        },
      ],
    });
    renderCaseView();
    await loaded();

    const embed = document.getElementById("embedCassette-1");
    expect(embed).toBeDisabled();
    expect(embed).toHaveAttribute(
      "title",
      messages["pathology.locked.caseCompleted"],
    );

    const deactivates = screen.getAllByText(messages["common.deactivate"], {
      selector: "button",
    });
    expect(deactivates).toHaveLength(2);
    deactivates.forEach((button) => {
      expect(button).toBeDisabled();
      expect(button).toHaveAttribute(
        "title",
        messages["pathology.locked.caseCompleted"],
      );
    });
  });

  it("holds every other Embed back, and says why, while one embed is out", async () => {
    servedCase = caseAtStage("EMBEDDING", {
      blocks: [
        { id: 1, designation: "A1", cassetteState: "CASSETTE", active: true },
        { id: 2, designation: "A2", cassetteState: "CASSETTE", active: true },
      ],
    });
    renderCaseView();
    await loaded();

    fireEvent.click(document.getElementById("embedCassette-1"));

    const other = document.getElementById("embedCassette-2");
    expect(other).toBeDisabled();
    expect(other).toHaveAttribute(
      "title",
      messages["pathology.locked.requestInFlight"],
    );
  });

  it("leaves Embed and Deactivate to their usual rules before sign-out", async () => {
    servedCase = caseAtStage("EMBEDDING", {
      // The cassette's Deactivate alone, so the query finds one button.
      slides: [],
      blocks: [
        {
          id: 1,
          designation: "A1",
          barcode: "ACC9.A1",
          cassetteState: "CASSETTE",
          active: true,
        },
      ],
    });
    renderCaseView();
    await loaded();

    expect(document.getElementById("embedCassette-1")).toBeEnabled();
    expect(
      screen.getByText(messages["common.deactivate"], { selector: "button" }),
    ).toBeEnabled();
  });
});

/**
 * The technician on a case must be someone allowed to work in Pathology:
 * the list comes from the lab-unit staff endpoint, never from every user.
 */
describe("PathologyCaseView technician list", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/PATHOLOGY_STATUS") {
        return callback(STATUS_LIST);
      }
      if (url === CASE_URL) {
        return callback(caseAtStage("MICROTOMY"));
      }
      if (url === "/rest/users/Results/labUnit/Pathology") {
        return callback([{ id: "10", value: "Kayla Ramlogan" }]);
      }
      if (url === "/rest/users") {
        return callback([
          { id: "10", value: "Kayla Ramlogan" },
          { id: "99", value: "Kevin Mohammed" },
        ]);
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });
  });

  it("offers only staff with Results on Pathology", async () => {
    renderCaseView();

    const select = await waitFor(() => {
      const element = document.getElementById("assignedTechnician");
      expect(element).not.toBeNull();
      return element;
    });
    await waitFor(() =>
      expect(within(select).getByText("Kayla Ramlogan")).toBeInTheDocument(),
    );
    expect(within(select).queryByText("Kevin Mohammed")).toBeNull();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/users/Results/labUnit/Pathology",
      expect.any(Function),
    );
  });
});
