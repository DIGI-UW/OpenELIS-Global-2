/**
 * The pathology case view on the shared case-view shell.
 *
 * The screen it replaced was a flat form whose pathologist blocks were removed
 * from the page entirely for anyone without the role, so a technician could
 * not tell that a reading step existed at all, and whose stage select, header
 * and summary each described the case in their own way. These tests pin what
 * the shell is for: eleven sections that are always present whatever the
 * status or the role, state that is derived rather than stored, counts taken
 * from the rows themselves, and a save that still posts exactly what it always
 * posted.
 */
import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider, createIntl } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext, ConfigurationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PathologyCaseView from "./PathologyCaseView";
import { PATHOLOGY_STAGES, stageDisplayKey } from "./pathologyStages";

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
  hasRole,
} from "../utils/Utils";

const CASE_ID = "9";
const CASE_URL = "/rest/pathology/caseView/" + CASE_ID;

const intl = createIntl({ locale: "en", messages });

// Listed here rather than read from the module the screen renders from, so a
// section silently renumbered, renamed or dropped fails this file instead of
// agreeing with itself.
const EXPECTED_SECTIONS = [
  [1, "pathology.section.caseInfo"],
  [2, "pathology.stage.grossing"],
  [3, "pathology.stage.decalcification"],
  [4, "pathology.stage.processing"],
  [5, "pathology.stage.embedding"],
  [6, "pathology.stage.microtomy"],
  [7, "pathology.stage.staining"],
  [8, "pathology.stage.coverslipping"],
  [9, "pathology.section.review"],
  [10, "pathology.section.findings"],
  [11, "common.reports"],
];

const sectionHeading = (number, titleKey) =>
  intl.formatMessage(
    { id: "caseView.label.numberedSection" },
    { number, title: messages[titleKey] },
  );

const caseAtStage = (status, overrides = {}) => ({
  pathologySampleId: CASE_ID,
  patientPK: "1",
  labNumber: "ACC9",
  firstName: "Ama",
  lastName: "Doe",
  sex: "F",
  status,
  assignedTechnicianId: "7",
  assignedTechnician: "Tina Tech",
  assignedPathologistId: "8",
  assignedPathologist: "Paula Path",
  grossExam: "",
  // Flagged, so the stage-order and stage-switch tests below see only the rule
  // they are about; the tests of the flag itself set it.
  decalcificationRequired: true,
  microscopyExam: "",
  conclusionText: "",
  // Set here on purpose so the posted key set below is deterministic. The
  // case DTO carries no such field, so on a real case it is undefined until
  // the checkbox is used and JSON.stringify drops the key, exactly as it did
  // before this screen was rebuilt.
  referToImmunoHistoChemistry: false,
  blocks: [],
  slides: [],
  reports: [],
  requests: [],
  techniques: [],
  conclusions: [],
  immunoHistoChemistryTestIds: [],
  // The fields the case read carries for a move, stated rather than left
  // out: this file's tests are about the sections, so the case names no
  // stage to move to and the bar holds Discard and Save only.
  nextStage: null,
  previousStage: null,
  runReference: null,
  runReferenceEditable: false,
  stageEvents: [],
  // As the server answers for a case that was released when it was signed
  // out; a case set to Completed without a release says so itself.
  releasable: status === "READY_PATHOLOGIST" || status === "UNDER_REVIEW",
  ...overrides,
});

let servedCase;
let tissueCatalogue;
let stainCatalogue;
let requestCatalogue;
let requestStatusCatalogue;

const addNotification = vi.fn();

const renderCaseView = (configurationProperties) => {
  const tree = (
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
                firstName: "Sam",
                lastName: "Session",
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
  );

  // Without a provider the context serves its own null default, which is the
  // shape a screen rendered outside Layout actually sees.
  return render(
    configurationProperties ? (
      <ConfigurationContext.Provider value={{ configurationProperties }}>
        {tree}
      </ConfigurationContext.Provider>
    ) : (
      tree
    ),
  );
};

const section = (id) => document.getElementById(id);

// The stage the action bar reads out, there once the case has loaded.
const stageReadout = () => document.getElementById("caseStage");

const caseInfoValue = (labelText) => {
  const rows = Array.from(
    section("pathology-section-case-info").querySelectorAll(
      ".cds--structured-list-row",
    ),
  );
  const row = rows.find(
    (candidate) => candidate.children[0].textContent === labelText,
  );
  return row.children[1].textContent;
};

const summaryRow = (labelText) =>
  Array.from(document.querySelectorAll(".case-view__summary-row")).find(
    (candidate) => candidate.children[0].textContent === labelText,
  );

// The row's value, without the label it starts with and without the sentence
// the row adds for a screen reader, which carries its own title text.
const summaryValue = (labelText) => {
  const row = summaryRow(labelText);
  const hidden = row.querySelector(".cds--visually-hidden");
  return row.textContent
    .slice(labelText.length)
    .replace(hidden ? hidden.textContent : "", "");
};

// Scoped to the rail, because the summary row names the same requests for a
// screen reader and would otherwise answer the same query.
const rail = () => within(document.querySelector(".case-view__rail"));

const caseFetchCount = () =>
  getFromOpenElisServer.mock.calls.filter(([url]) => url === CASE_URL).length;

// Rows as the server serves them once it has named them.
const savedBlock = (id, designation, overrides = {}) => ({
  id,
  designation,
  barcode: "ACC9." + designation,
  cassetteState: "CASSETTE",
  active: true,
  location: "",
  ...overrides,
});

const savedSlide = (id, designation, blockId, overrides = {}) => ({
  id,
  designation,
  blockId,
  barcode: "ACC9.A1." + designation,
  active: true,
  location: "",
  ...overrides,
});

// One row of a list, found by the identifier it shows rather than by its
// position, so a hidden deactivated row cannot shift what a test is looking at.
const rowIn = (sectionId, identifier) =>
  within(section(sectionId))
    .getByText(identifier, {
      selector: ".pathology-case-view__row-designation",
    })
    .closest(".pathology-case-view__row");

const queryRowIn = (sectionId, identifier) =>
  within(section(sectionId)).queryByText(identifier, {
    selector: ".pathology-case-view__row-designation",
  });

const pendingRows = (sectionId) =>
  within(section(sectionId)).queryAllByText(
    messages["pathology.label.designationPending"],
    { selector: ".pathology-case-view__row-designation" },
  );

const deactivateCassette = (designation) =>
  intl.formatMessage(
    { id: "pathology.action.deactivateCassette" },
    { designation },
  );

const newCassette = (position) =>
  intl.formatMessage({ id: "pathology.label.newCassette" }, { position });

const newSlide = (position) =>
  intl.formatMessage({ id: "pathology.label.newSlide" }, { position });

const removeName = (name) =>
  intl.formatMessage({ id: "common.removeSelection" }, { name });

const addCassetteButton = () =>
  screen.getByRole("button", {
    name: messages["pathology.action.addCassette"],
  });

// The Add slide of the block it is named after.
const addSlideTo = (designation) =>
  screen.getByRole("button", {
    name: intl.formatMessage(
      { id: "pathology.action.addSlideTo" },
      { designation },
    ),
  });

// The control that opens and closes a block's slides, named after the block.
const slidesToggle = (designation) =>
  screen.getByRole("button", {
    name: intl.formatMessage(
      { id: "pathology.label.slidesOf" },
      { designation },
    ),
  });

// A block's group in the microtomy section: its header and its slides.
const slideGroup = (designation) =>
  slidesToggle(designation).closest(".pathology-case-view__group");

const slideGroupBody = (designation) =>
  document.getElementById(
    slidesToggle(designation).getAttribute("aria-controls"),
  );

// Named after the row it sits in, so the label starts with the field's word.
const stainCombobox = (row) =>
  within(row).getByRole("combobox", {
    name: new RegExp("^" + messages["pathology.label.stain"]),
  });

const reasonField = () =>
  within(screen.getByRole("dialog")).getByLabelText(messages["common.reason"]);

const pressEnter = (element) =>
  fireEvent.keyDown(element, {
    key: "Enter",
    code: "Enter",
    keyCode: 13,
    which: 13,
  });

const saveDraftButton = () =>
  screen.getByRole("button", { name: messages["caseView.action.saveDraft"] });

const postedCase = async () => {
  fireEvent.click(saveDraftButton());
  await waitFor(() =>
    expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
  );
  return JSON.parse(postToOpenElisServerFullResponse.mock.calls.at(-1)[1]);
};

// By its own text: the role query bundled with this Testing Library names a
// disabled button by its title, and Carbon puts an English word before a
// danger button's label.
const buttonWithText = (container, text) =>
  within(container).getByText(text, { selector: "button" });

const showDeactivatedSwitch = (sectionId) =>
  within(section(sectionId)).queryByRole("switch", {
    name: messages["caseView.action.showDeactivated"],
  });

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  postToOpenElisServerJsonResponse.mockReset();
  addNotification.mockReset();
  hasRole.mockReset();
  hasRole.mockReturnValue(false);
  servedCase = caseAtStage("GROSSING");
  tissueCatalogue = [
    { id: "21", value: "Skin" },
    { id: "22", value: "Bone" },
  ];
  stainCatalogue = [
    { id: "31", value: "H&E" },
    { id: "32", value: "PAS" },
  ];
  requestCatalogue = [];
  requestStatusCatalogue = [
    { id: "OPENED", value: "Opened" },
    { id: "COMPLETED", value: "Completed" },
    { id: "CANCELLED", value: "Cancelled" },
  ];
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === "/rest/dictionary/category/pathology_tissue_types") {
      return callback(tissueCatalogue);
    }
    if (url === "/rest/dictionary/category/Pathology%20Stains") {
      return callback(stainCatalogue);
    }
    if (url === "/rest/displayList/PATHOLOGIST_REQUESTS") {
      return callback(requestCatalogue);
    }
    if (url === "/rest/displayList/PATHOLOGY_REQUEST_STATUS") {
      return callback(requestStatusCatalogue);
    }
    if (url === CASE_URL) {
      // A copy per fetch: the screen stamps the session user onto an
      // unassigned case, and that must not leak into the next fetch.
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

describe("PathologyCaseView sections", () => {
  it.each(PATHOLOGY_STAGES)(
    "keeps all eleven sections on the page with the case at %s",
    async (status) => {
      servedCase = caseAtStage(status);
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());

      EXPECTED_SECTIONS.forEach(([number, titleKey]) => {
        expect(
          screen.getByText(sectionHeading(number, titleKey)),
        ).toBeInTheDocument();
      });
    },
  );

  it("opens the section the bench is working on and leaves case information collapsed", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByRole("button", { name: /1\. Case Information/ }),
    ).toHaveAttribute("aria-expanded", "false");
    expect(
      screen.getByRole("button", { name: /2\. Grossing/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });

  it("names the case in its own page heading", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByRole("heading", {
        // Two Sections deep, as every case view in this application sizes
        // its title.
        level: 3,
        name: intl.formatMessage(
          { id: "pathology.label.caseTitle" },
          { labNumber: "ACC9" },
        ),
      }),
    ).toBeInTheDocument();
  });

  it("reads the stage out as the status, in the user's language", async () => {
    servedCase = caseAtStage("MICROTOMY");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(stageReadout()).toHaveTextContent(
      messages["common.status"] + " " + messages["pathology.stage.microtomy"],
    );
    expect(screen.queryByText("MICROTOMY")).not.toBeInTheDocument();
  });

  it("keeps the grossing description open beside the reading the pathologist is writing", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByRole("button", { name: /2\. Grossing/ }),
    ).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("button", { name: /10\. Findings & Conclusion/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });

  it("badges grossing complete on the work recorded there, not on where the case has got to", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("COMPLETED", {
      grossExam: "Firm tan nodule, 20mm",
      blocks: [{ id: "1", blockNumber: 1 }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section("pathology-section-grossing")).getByText(
        messages["common.complete"],
      ),
    ).toBeInTheDocument();
  });

  // A completed case has been through every bench stage by definition, so if
  // the badge followed the case's position rather than the record, this is
  // where it would be at its most wrong: five sections claiming work while
  // their own bodies say OpenELIS records nothing at them yet, and grossing
  // claiming a description nobody wrote.
  it("badges nothing on a completed case whose bench work was never recorded", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("COMPLETED");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    [
      "pathology-section-grossing",
      "pathology-section-decalcification",
      "pathology-section-processing",
      "pathology-section-embedding",
      "pathology-section-microtomy",
      "pathology-section-staining",
      "pathology-section-coverslipping",
      "pathology-section-reports",
    ].forEach((id) => {
      expect(
        within(section(id)).queryByText(messages["common.complete"]),
      ).not.toBeInTheDocument();
    });
  });

  it("badges the review section with the requests the case is still waiting on", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST", {
      requests: [
        { id: "1", value: "Deeper sections", status: "OPENED" },
        { id: "2", value: "Special stain", status: "OPENED" },
      ],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section("pathology-section-review")).getByText(
        intl.formatMessage(
          { id: "pathology.badge.openRequestCount" },
          { count: 2 },
        ),
      ),
    ).toBeInTheDocument();
  });

  // Inversion test: the badge is the request rows counted, and nothing else,
  // so the same two requests with nothing left open on them badge nothing at
  // all rather than announcing a count of none.
  it("badges nothing on a review whose requests have all been closed", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST", {
      requests: [
        { id: "1", value: "Deeper sections", status: "COMPLETED" },
        { id: "2", value: "Special stain", status: "CANCELLED" },
      ],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section("pathology-section-review")).queryByText(/\d+ open/),
    ).not.toBeInTheDocument();
  });

  // The header carries one badge, and for a technician the fact that they
  // cannot act on the reading outranks the count of what it is waiting for;
  // the requests are still named on the rail beside it.
  it("keeps the read-only marker on the review a technician cannot act on, even with a request open", async () => {
    servedCase = caseAtStage("READY_PATHOLOGIST", {
      requests: [{ id: "1", value: "Deeper sections", status: "OPENED" }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const review = within(section("pathology-section-review"));
    expect(
      review.getByText(messages["caseView.badge.readOnly"]),
    ).toBeInTheDocument();
    expect(review.queryByText(/\d+ open/)).not.toBeInTheDocument();
    expect(
      rail().getByText(
        intl.formatMessage(
          { id: "pathology.badge.openRequests" },
          { names: "Deeper sections" },
        ),
      ),
    ).toBeInTheDocument();
  });

  it("says a case carries no report at all, rather than counting none", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(summaryValue(messages["pathology.label.report"])).toBe(
      messages["common.none"],
    );
  });

  it("counts the reports a case does carry", async () => {
    servedCase = caseAtStage("COMPLETED", { reports: [{ id: "1" }] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(summaryValue(messages["pathology.label.report"])).toBe("1");
  });

  it("says a case information field was not recorded instead of printing an empty or null value", async () => {
    servedCase = caseAtStage("GROSSING", {
      requester: null,
      specimenTypes: [],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(caseInfoValue(messages["sample.label.requester"])).toBe(
      messages["caseView.label.notRecorded"],
    );
    expect(caseInfoValue(messages["sample.type"])).toBe(
      messages["caseView.label.notRecorded"],
    );
    expect(section("pathology-section-case-info").textContent).not.toContain(
      "null",
    );
  });

  it("shows the arrival date and every specimen the case arrived with", async () => {
    servedCase = caseAtStage("GROSSING", {
      receivedDate: "2024-06-03",
      specimenTypes: ["Tissue", "Tissue"],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(caseInfoValue(messages["sample.receivedDate"])).toBe("2024-06-03");
    expect(caseInfoValue(messages["sample.type"])).toBe("Tissue, Tissue");
  });

  it("marks a stage the laboratory does not track as not applicable", async () => {
    servedCase = caseAtStage("STAINING");
    renderCaseView({ PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false" });

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByText(messages["caseView.badge.notApplicable"]),
    ).toBeInTheDocument();
    expect(section("pathology-section-coverslipping").textContent).toContain(
      messages["pathology.locked.stageDisabled"],
    );
  });

  it("marks no stage not applicable when the laboratory tracks them all", async () => {
    servedCase = caseAtStage("STAINING");
    renderCaseView({ PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "true" });

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.queryByText(messages["caseView.badge.notApplicable"]),
    ).not.toBeInTheDocument();
  });

  it("counts the rows the case actually holds in the summary", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      // A deactivated cassette and slide are kept for the record but are not
      // work in hand, so neither row counts them.
      blocks: [
        { id: "1" },
        { id: "2" },
        { id: "3" },
        { id: "4", active: false },
      ],
      slides: [{ id: "1" }, { id: "2" }, { id: "3", active: false }],
      requests: [
        { id: "1", value: "Deeper sections", status: "OPENED" },
        { id: "2", value: "Special stain", status: "COMPLETED" },
      ],
      conclusions: [],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(summaryValue(messages["pathology.label.cassettes"])).toBe("3");
    expect(summaryValue(messages["pathology.label.slides"])).toBe("2");
    expect(summaryValue(messages["pathology.label.request"])).toBe(
      intl.formatMessage(
        { id: "pathology.badge.openRequestCount" },
        { count: 1 },
      ),
    );
    expect(summaryValue(messages["pathology.label.conclusion"])).toBe(
      messages["caseView.label.notRecorded"],
    );
  });

  it("names the outstanding requests on the rail rather than counting them", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      requests: [
        { id: "1", value: "Deeper sections", status: "OPENED" },
        { id: "2", value: "Special stain", status: "OPENED" },
      ],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      rail().getByText(
        intl.formatMessage(
          { id: "pathology.badge.openRequests" },
          { names: "Deeper sections, Special stain" },
        ),
      ),
    ).toBeInTheDocument();
  });

  it("names nothing on the rail when no request is outstanding", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      requests: [{ id: "2", value: "Special stain", status: "COMPLETED" }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(rail().queryByText(/Open requests:/)).not.toBeInTheDocument();
  });

  it("offers a discard and a save on a case with no next stage, and only lets a discard undo real work", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const actions = document.querySelector(".case-view__action-bar-actions");
    expect(within(actions).getAllByRole("button")).toHaveLength(2);

    const discard = screen.getByRole("button", {
      name: messages["caseView.action.discard"],
    });
    expect(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    ).toBeInTheDocument();
    expect(discard).toBeDisabled();
    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();

    const fetchesBefore = caseFetchCount();
    fireEvent.change(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
      { target: { value: "Skin ellipse" } },
    );

    expect(discard).toBeEnabled();
    expect(
      screen.getByText(messages["caseView.label.unsavedChanges"]),
    ).toBeInTheDocument();

    fireEvent.click(discard);

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();
    expect(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
    ).toHaveValue("");
  });

  it("posts the fields the backend has always been sent, then reads the saved case back", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    const fetchesBefore = caseFetchCount();

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const [url, body, callback] =
      postToOpenElisServerFullResponse.mock.calls.at(-1);
    expect(url).toBe(CASE_URL);
    expect(Object.keys(JSON.parse(body)).sort()).toEqual([
      "assignedPathologistId",
      "assignedTechnicianId",
      "blocks",
      "conclusionText",
      "conclusions",
      "decalcificationRequired",
      "grossExam",
      "immunoHistoChemistryTestIds",
      "microscopyExam",
      "referToImmunoHistoChemistry",
      "release",
      "reports",
      "requests",
      "slides",
      "techniques",
    ]);

    await act(async () => {
      await callback({ status: 200, json: async () => ({}) });
    });

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();
  });

  it("reports a save that never reached the server, and lets it be tried again", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const saveDraft = () =>
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      });

    fireEvent.click(saveDraft());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );

    // A network failure is delivered as no response at all.
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
    await act(async () => {
      await callback(undefined);
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
    expect(saveDraft()).toBeEnabled();

    // Inversion of the stuck state: before this, the failure left the screen
    // believing a save was still in flight and swallowed every later click.
    fireEvent.click(saveDraft());
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2);
  });

  // A request raised on the screen used to reach the server with no status at
  // all, and the server's own default was what made it open. The screen then
  // had to treat a status-less row as open to agree with it, which is a
  // second rule for the same fact. The request is stamped as it is raised, so
  // there is only the one rule left.
  it("posts a newly raised request as open", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    requestCatalogue = [{ id: "3", value: "Recut" }];
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const user = userEvent.setup();
    const review = within(section("pathology-section-review"));
    // The filterable multiselect's own field, named for the list it filters
    // and distinct from the two native selects beside it.
    // Named for the list it filters, which is what distinguishes it from the
    // two native selects beside it; its full accessible name also carries
    // Carbon's running count of what is selected.
    await user.click(review.getByRole("combobox", { name: /^Requests/ }));
    await user.click(review.getByRole("option", { name: "Recut" }));

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const body = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls.at(-1)[1],
    );
    expect(body.requests).toEqual([{ value: "3", status: "OPENED" }]);
  });

  // The multiselect hands back the objects it was given at mount, so a status
  // set on one request used to be reverted to open the moment a second
  // request was picked.
  it("keeps a request's status when another request is raised beside it", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    requestCatalogue = [
      { id: "3", value: "Recut" },
      { id: "4", value: "Deeper sections" },
    ];
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const user = userEvent.setup();
    const review = within(section("pathology-section-review"));
    await user.click(review.getByRole("combobox", { name: /^Requests/ }));
    await user.click(review.getByRole("option", { name: "Recut" }));
    fireEvent.change(document.getElementById("requeststatus0"), {
      target: { value: "COMPLETED" },
    });
    await user.click(review.getByRole("option", { name: "Deeper sections" }));

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["caseView.action.saveDraft"],
      }),
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const body = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls.at(-1)[1],
    );
    expect(body.requests).toEqual([
      { value: "3", status: "COMPLETED" },
      { value: "4", status: "OPENED" },
    ]);
  });

  // An empty option posts an empty status, which the server reads as open, so
  // the one control for closing a request also offered a way to silently
  // reopen it.
  it("offers a raised request the served statuses and nothing besides", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST", {
      requests: [{ id: "1", value: "Deeper sections", status: "OPENED" }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const statusOfRequest = document.getElementById("requeststatus0");
    expect(
      Array.from(statusOfRequest.options).map((o) => o.textContent),
    ).toEqual(requestStatusCatalogue.map((status) => status.value));
    expect(statusOfRequest.value).toBe("OPENED");
  });

  it("shows the reading sections to someone who cannot sign the case out, but lets them change nothing", async () => {
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(section("pathology-section-review").textContent).toContain(
      messages["caseView.badge.readOnly"],
    );
    expect(
      screen.getByLabelText(messages["pathology.label.microscopyExam"]),
    ).toBeDisabled();
  });

  it("lets the pathologist write the reading on the same case", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByLabelText(messages["pathology.label.microscopyExam"]),
    ).toBeEnabled();
  });

  it("tells a case still on the bench which stage unlocks the reading, in words", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const awaiting = intl.formatMessage(
      { id: "caseView.locked.awaitingStage" },
      { stage: messages["pathology.stage.readyPathologist"] },
    );
    expect(section("pathology-section-review").textContent).toContain(awaiting);
    expect(section("pathology-section-findings").textContent).toContain(
      awaiting,
    );
    expect(document.body.textContent).not.toContain("READY_PATHOLOGIST");
  });

  it("puts the stage and both assignments in the patient band", async () => {
    servedCase = caseAtStage("MICROTOMY");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const band = document.querySelector(".patient-header2");
    expect(
      within(band).getByText(messages[stageDisplayKey("MICROTOMY")]),
    ).toBeInTheDocument();
    expect(
      within(band).getByText(
        intl.formatMessage(
          { id: "caseView.label.assignedStaff" },
          { role: messages["assigned.technician.label"], name: "Tina Tech" },
        ),
      ),
    ).toBeInTheDocument();
    expect(
      within(band).getByText(
        intl.formatMessage(
          { id: "caseView.label.assignedStaff" },
          { role: messages["assigned.pathologist.label"], name: "Paula Path" },
        ),
      ),
    ).toBeInTheDocument();
  });

  // The bench reads and writes the macroscopic description while the specimen
  // is in front of them; the blocks record what it was cut into and the
  // technician is an attribution the case usually fills in by itself. Laid
  // out the other way round, as the flat form had it, the description read as
  // a footnote to a dropdown.
  it("puts the macroscopic description first in grossing, the decalcification flag and the cassettes under it and the attribution last", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [{ id: "1" }] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const grossing = section("pathology-section-grossing");
    const order = [
      grossing.querySelector("#grossExam"),
      grossing.querySelector("#decalcificationRequired"),
      within(grossing).getByText(messages["pathology.label.cassettes"]),
      grossing.querySelector("#assignedTechnician"),
    ];
    order.forEach((element) => expect(element).not.toBeNull());
    order.slice(1).forEach((element, index) => {
      expect(
        order[index].compareDocumentPosition(element) &
          Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    });
  });

  it("lets an unsaved row be removed and a saved one only deactivated, and offers a deactivated row neither", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2", { active: false })],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    // Numbered among the new rows only, so A1 and the hidden A2 before it
    // leave it the first.
    const unsaved = pendingRows("pathology-section-grossing")[0].closest(
      ".pathology-case-view__row",
    );
    expect(
      within(unsaved).getByRole("button", {
        name: removeName(newCassette(1)),
      }),
    ).toBeInTheDocument();
    expect(
      within(unsaved).queryByText(messages["common.deactivate"], {
        selector: "button",
      }),
    ).toBeNull();

    const saved = rowIn("pathology-section-grossing", "A1");
    expect(
      within(saved).getByRole("button", { name: deactivateCassette("A1") }),
    ).toBeInTheDocument();
    expect(
      within(saved).queryByText(messages["common.remove"], {
        selector: "button",
      }),
    ).toBeNull();

    fireEvent.click(showDeactivatedSwitch("pathology-section-grossing"));
    const deactivated = rowIn("pathology-section-grossing", "A2");
    // Scoped to the row's actions: its disabled fields keep their own toggles.
    expect(
      within(
        deactivated.querySelector(".pathology-case-view__row-actions"),
      ).queryAllByRole("button"),
    ).toHaveLength(0);
  });

  it("names each row's controls after the object the row stands for", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    // The embedding list names the same saved cassette's field the same way.
    const grossing = within(section("pathology-section-grossing"));
    expect(
      grossing.getByLabelText(messages["common.storageLocation"] + " A1"),
    ).toBe(document.getElementById("blockLocation0"));
    expect(
      grossing.getByLabelText(
        messages["common.storageLocation"] + " " + newCassette(1),
      ),
    ).toBe(document.getElementById("blockLocation1"));
    expect(
      screen.getByRole("button", { name: removeName(newCassette(1)) }),
    ).toBeInTheDocument();
  });

  it("shows each saved row's barcode and state, and an unsaved one as a cassette with no barcode yet", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [
        savedBlock("1", "A1"),
        savedBlock("2", "A2", { cassetteState: "BLOCK" }),
      ],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    const cassette = rowIn("pathology-section-grossing", "A1");
    expect(within(cassette).getByText("ACC9.A1")).toBeInTheDocument();
    expect(
      within(cassette).getByText(messages["pathology.label.cassette"]),
    ).toBeInTheDocument();
    // The hidden label carries its own separator, so the barcode is heard
    // as "Barcode: ACC9.A1" rather than run into the word before it.
    expect(
      cassette.querySelector(".pathology-case-view__row-barcode").textContent,
    ).toBe(messages["label.barcode"] + ": ACC9.A1");

    const block = rowIn("pathology-section-grossing", "A2");
    expect(
      within(block).getByText(messages["pathology.label.block"]),
    ).toBeInTheDocument();

    const unsaved = pendingRows("pathology-section-grossing")[0].closest(
      ".pathology-case-view__row",
    );
    expect(
      within(unsaved).getByText(messages["pathology.label.cassette"]),
    ).toBeInTheDocument();
    expect(
      unsaved.querySelector(".pathology-case-view__row-barcode"),
    ).toBeNull();
  });

  it("removes only the unsaved row whose own Remove was pressed", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());
    fireEvent.change(document.getElementById("blockLocation1"), {
      target: { value: "Tray 1" },
    });
    fireEvent.click(addCassetteButton());
    fireEvent.change(document.getElementById("blockLocation2"), {
      target: { value: "Tray 2" },
    });

    fireEvent.click(
      screen.getByRole("button", { name: removeName(newCassette(1)) }),
    );

    // Posted exactly as the server reads a new row: no id and no key the
    // screen kept for itself.
    const body = await postedCase();
    expect(body.blocks).toEqual([
      savedBlock("1", "A1"),
      { location: "Tray 2" },
    ]);
  });

  // A key taken from the position would hand the remaining row the removed
  // row's controls, and with them the wrong file shown as attached.
  it("keeps a new slide's own attachment when an earlier new slide is removed", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1", { cassetteState: "BLOCK" })],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addSlideTo("A1"));
    fireEvent.click(addSlideTo("A1"));

    const second = pendingRows("pathology-section-microtomy")[1].closest(
      ".pathology-case-view__row",
    );
    await act(async () => {
      fireEvent.change(second.querySelector('input[type="file"]'), {
        target: {
          files: [new File(["scan"], "scan-a1.png", { type: "image/png" })],
        },
      });
    });
    expect(within(second).getByText("scan-a1.png")).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: removeName(newSlide(1)) }),
    );

    const remaining = pendingRows("pathology-section-microtomy");
    expect(remaining).toHaveLength(1);
    expect(
      within(remaining[0].closest(".pathology-case-view__row")).getByText(
        "scan-a1.png",
      ),
    ).toBeInTheDocument();
  });

  // The case is read back after a deactivation, so an edit not yet saved
  // would be thrown away by it without a word.
  it("holds deactivation back while there are unsaved changes, and says why", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    const deactivate = () =>
      screen.getByRole("button", { name: deactivateCassette("A1") });
    expect(deactivate()).toBeEnabled();

    fireEvent.change(document.getElementById("grossExam"), {
      target: { value: "Firm tan nodule" },
    });

    expect(deactivate()).toBeDisabled();
    expect(deactivate()).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeDeactivate"],
    );

    const fetchesBefore = caseFetchCount();
    fireEvent.click(
      screen.getByRole("button", { name: messages["caseView.action.discard"] }),
    );

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(deactivate()).toBeEnabled();
    expect(deactivate()).not.toHaveAttribute("title");
  });

  // The read-back after a deactivation would race the save's own read-back.
  it("holds deactivation back while a save is in flight", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    const deactivate = () =>
      screen.getByRole("button", { name: deactivateCassette("A1") });

    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );

    expect(deactivate()).toBeDisabled();
    expect(deactivate()).toHaveAttribute(
      "title",
      messages["pathology.locked.requestInFlight"],
    );

    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
    await act(async () => {
      await callback({ status: 200, json: async () => ({}) });
    });

    expect(deactivate()).toBeEnabled();
  });

  it("offers no count input anywhere on the case", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1")],
      slides: [savedSlide("5", "1", "1")],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(document.querySelectorAll('input[type="number"]')).toHaveLength(0);
    expect(document.getElementById("blocksToAdd")).toBeNull();
    expect(document.getElementById("slidesToAdd")).toBeNull();
    expect(addCassetteButton()).toBeInTheDocument();
  });

  it("adds a cassette the server will name, and posts it with no id at all", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    const added = pendingRows("pathology-section-grossing");
    expect(added).toHaveLength(1);
    expect(
      added[0]
        .closest(".pathology-case-view__row")
        .querySelector(".pathology-case-view__row-barcode"),
    ).toBeNull();

    const body = await postedCase();
    expect(body.blocks).toEqual([savedBlock("1", "A1"), { location: "" }]);
    expect(body.blocks[1]).not.toHaveProperty("id");
  });

  it("keeps a deactivated row out of sight until it is asked for, then shows it as deactivated", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2", { active: false })],
    });
    const { unmount } = renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(queryRowIn("pathology-section-grossing", "A2")).toBeNull();
    const toggle = showDeactivatedSwitch("pathology-section-grossing");
    expect(toggle).toBeInTheDocument();

    fireEvent.click(toggle);

    const deactivated = rowIn("pathology-section-grossing", "A2");
    expect(
      within(deactivated).getByText(messages["caseView.badge.deactivated"]),
    ).toBeInTheDocument();
    expect(document.getElementById("blockLocation1")).toBeDisabled();

    unmount();
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    expect(showDeactivatedSwitch("pathology-section-grossing")).toBeNull();
  });

  // The server leaves a deactivated slide untouched, so a location or image
  // given to one would be dropped without a word.
  it("lets nothing be changed on a deactivated slide", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1")],
      slides: [savedSlide("5", "1", "1", { active: false })],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(showDeactivatedSwitch("pathology-section-microtomy"));

    const slide = rowIn("pathology-section-microtomy", "1");
    expect(document.getElementById("slideLocation0")).toBeDisabled();
    expect(
      within(slide).getByText(messages["label.button.uploadfile"], {
        selector: "button",
      }),
    ).toBeDisabled();
  });

  it("deactivates a cassette through a dialog that names it, then reads the case back", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2")],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", { name: deactivateCassette("A2") }),
    );

    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getByText(
        intl.formatMessage(
          { id: "pathology.modal.deactivateCassetteHeading" },
          { designation: "A2" },
        ),
      ),
    ).toBeInTheDocument();

    fireEvent.change(reasonField(), {
      target: { value: "  Section folded " },
    });
    fireEvent.click(buttonWithText(dialog, messages["common.deactivate"]));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [url, body, callback] =
      postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/pathology/block/2/deactivate");
    expect(body).toBe('{"reason":"Section folded"}');

    const fetchesBefore = caseFetchCount();
    act(() => callback({ id: 2, designation: "A2", active: false }));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: intl.formatMessage(
          { id: "pathology.toast.cassetteDeactivated" },
          { designation: "A2" },
        ),
      }),
    );
  });

  // The audit trail keeps why a retained object stopped being used, so the
  // dialog takes no deactivation without one.
  it("asks for a reason before deactivating, and posts the one given", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("2", "A2")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", { name: deactivateCassette("A2") }),
    );
    const dialog = screen.getByRole("dialog");

    expect(reasonField()).toBeRequired();
    expect(
      within(dialog).queryByText(messages["pathology.locked.reasonRequired"]),
    ).toBeNull();

    fireEvent.click(buttonWithText(dialog, messages["common.deactivate"]));

    expect(
      within(dialog).getByText(messages["pathology.locked.reasonRequired"]),
    ).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();

    fireEvent.change(reasonField(), { target: { value: "Section folded" } });
    pressEnter(reasonField());

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    expect(postToOpenElisServerJsonResponse.mock.calls[0][1]).toBe(
      '{"reason":"Section folded"}',
    );
  });

  it("closes the dialog on Escape and on Cancel without deactivating anything, and hands focus back to the row", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("2", "A2")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    const launcher = () =>
      screen.getByRole("button", { name: deactivateCassette("A2") });

    fireEvent.click(launcher());
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    fireEvent.keyDown(document.activeElement, { key: "Escape" });
    expect(screen.queryByRole("dialog")).toBeNull();

    fireEvent.click(launcher());
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: messages["common.cancel"],
      }),
    );
    expect(screen.queryByRole("dialog")).toBeNull();
    await waitFor(() => expect(launcher()).toHaveFocus());

    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  // The row whose Deactivate was pressed leaves the list on the read-back, so
  // focus goes to the switch that brings it back rather than to the page.
  it("puts focus on the list's show-deactivated switch once the row has gone", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2")],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", { name: deactivateCassette("A2") }),
    );
    fireEvent.change(reasonField(), { target: { value: "Section folded" } });
    fireEvent.click(
      buttonWithText(screen.getByRole("dialog"), messages["common.deactivate"]),
    );

    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2", { active: false })],
    });
    const callback = postToOpenElisServerJsonResponse.mock.calls[0][2];
    act(() => callback({ id: 2, designation: "A2", active: false }));

    await waitFor(() =>
      expect(showDeactivatedSwitch("pathology-section-grossing")).toHaveFocus(),
    );
  });

  // Closing mid-flight would hide the outcome of a request that lands anyway.
  it("stays open while the deactivation is in flight, then closes on its answer", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("2", "A2")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", { name: deactivateCassette("A2") }),
    );
    fireEvent.change(reasonField(), { target: { value: "Section folded" } });
    fireEvent.click(
      buttonWithText(screen.getByRole("dialog"), messages["common.deactivate"]),
    );

    fireEvent.keyDown(document.activeElement, { key: "Escape" });
    fireEvent.click(
      within(screen.getByRole("dialog")).getByText(messages["common.cancel"], {
        selector: "button",
      }),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    const fetchesBefore = caseFetchCount();
    const callback = postToOpenElisServerJsonResponse.mock.calls[0][2];
    act(() => callback({ id: 2, designation: "A2", active: false }));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(addNotification).toHaveBeenCalledTimes(1);
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });

  // The server's sentence is English and names internal ids; the dialog says
  // what happened in the reader's language and nothing else.
  it("keeps the dialog open and says the deactivation was refused when the server refuses it", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("2", "A2")] });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", { name: deactivateCassette("A2") }),
    );
    fireEvent.change(reasonField(), { target: { value: "Section folded" } });
    fireEvent.click(
      buttonWithText(screen.getByRole("dialog"), messages["common.deactivate"]),
    );

    const fetchesBefore = caseFetchCount();
    const callback = postToOpenElisServerJsonResponse.mock.calls[0][2];
    act(() => callback({ status: 404, error: "No pathology block with id 2" }));

    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getByText(messages["pathology.modal.deactivateFailed"]),
    ).toBeInTheDocument();
    expect(dialog).not.toHaveTextContent("No pathology block with id 2");
    expect(caseFetchCount()).toBe(fetchesBefore);
    expect(addNotification).not.toHaveBeenCalled();
  });

  it("deactivates a slide through the slide endpoint, naming it by its barcode", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1")],
      slides: [savedSlide("5", "1", "1")],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(
      screen.getByRole("button", {
        name: intl.formatMessage(
          { id: "pathology.action.deactivateSlide" },
          { barcode: "ACC9.A1.1" },
        ),
      }),
    );
    fireEvent.change(reasonField(), { target: { value: "Cracked" } });
    fireEvent.click(
      buttonWithText(screen.getByRole("dialog"), messages["common.deactivate"]),
    );

    expect(postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/pathology/slide/5/deactivate",
    );
  });

  // A cassette on the screen is not work recorded until the save names it.
  it("counts an unsaved cassette nowhere: not on the header, the summary or grossing complete", async () => {
    servedCase = caseAtStage("GROSSING", {
      grossExam: "Firm tan nodule, 20mm",
      blocks: [savedBlock("1", "A1", { active: false })],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    const grossing = within(section("pathology-section-grossing"));
    expect(grossing.queryByText(messages["common.complete"])).toBeNull();
    expect(
      grossing.queryByText(
        intl.formatMessage(
          { id: "pathology.badge.cassetteCount" },
          { count: 1 },
        ),
      ),
    ).toBeNull();
    expect(summaryValue(messages["pathology.label.cassettes"])).toBe("0");
  });

  // The servlet prints every saved label of the case from its accession
  // number; the per-row buttons it replaced sent a block number it could not
  // resolve.
  it("prints the case's own labels, and only once there is a saved row to print", async () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    try {
      servedCase = caseAtStage("MICROTOMY", {
        labNumber: "AC C&9",
        blocks: [savedBlock("1", "A1")],
        slides: [savedSlide("5", "1", "1", { active: false })],
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      fireEvent.click(
        screen.getByRole("button", {
          name: messages["pathology.action.printCassetteLabels"],
        }),
      );

      expect(open).toHaveBeenCalledTimes(1);
      const [url, target] = open.mock.calls[0];
      expect(url).toContain(
        "/LabelMakerServlet?labelType=block&code=AC%20C%269",
      );
      expect(target).toBe("_blank");

      const printSlides = buttonWithText(
        document.body,
        messages["pathology.action.printSlideLabels"],
      );
      expect(printSlides).toBeDisabled();
      expect(printSlides).toHaveAttribute(
        "title",
        messages["pathology.locked.nothingToPrint"],
      );
    } finally {
      open.mockRestore();
    }
  });

  it("says a concurrent save named the same cassette, in the reader's language", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];

    await act(async () => {
      await callback({
        status: 409,
        json: async () => ({
          status: 409,
          conflict: "designation",
          error: "duplicate key value violates unique constraint",
        }),
      });
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["pathology.error.designationConflict"],
      }),
    );
  });

  // The server's rule text is English and names the case's internal id. On
  // this screen a broken case rule means a row someone else changed first, so
  // the bench is told to reload rather than shown the server's words.
  it("never shows the server's own sentence for a refused save, and says to reload the case", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
    const sentence = "block 7 is not on pathology case 9";

    await act(async () => {
      await callback({
        status: 400,
        json: async () => ({ error: sentence }),
      });
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["pathology.error.reloadCase"],
      }),
    );
    expect(JSON.stringify(addNotification.mock.calls)).not.toContain(sentence);
  });

  it("reports any other refused save as the ordinary save failure", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());
    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];

    await act(async () => {
      await callback({
        status: 500,
        json: async () => ({ error: "could not execute statement" }),
      });
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["error.save.msg"],
      }),
    );
  });

  // S-7.4: a control the case is not ready for is the pathologist's answer to
  // "why can I not release this", so it is shown and disabled rather than
  // removed. The flat form rendered it only once both assignments existed,
  // which left the screen saying nothing at all.
  it("shows the release a case is not ready for, disabled and saying why", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("UNDER_REVIEW", { assignedPathologistId: "" });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(document.getElementById("release")).toBeDisabled();
    // The pathologist is the half that is missing, and saying so is the whole
    // point: the technician on this case is already assigned, so a hint
    // naming both would send its reader to a field that is already filled.
    expect(
      screen.getByText(messages["pathology.locked.releaseNoPathologist"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["pathology.locked.releaseNoTechnician"]),
    ).not.toBeInTheDocument();
  });

  it("lets the pathologist release a case that carries both assignments", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("UNDER_REVIEW");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(document.getElementById("release")).toBeEnabled();
    expect(
      screen.queryByText(messages["pathology.locked.releaseNoPathologist"]),
    ).not.toBeInTheDocument();
  });

  // A released case is not released again, and the server refuses a second
  // release, so the box says so rather than offering a tick that fails.
  it("holds the release back on a released case and says why", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("COMPLETED");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const release = document.getElementById("release");
    expect(release).toBeDisabled();
    // Carbon puts a checkbox's title on its label, the part a pointer rests on.
    expect(document.querySelector('label[for="release"]')).toHaveAttribute(
      "title",
      messages["pathology.locked.caseCompleted"],
    );
    expect(
      within(section("pathology-section-findings")).getByText(
        messages["pathology.locked.caseCompleted"],
      ),
    ).toBeInTheDocument();
  });

  // The stage control once set Completed without releasing the case. The
  // server still takes one release of such a case, and says so, so the box is
  // offered rather than held back as if the case were already released.
  it("offers the release on a case left at Completed without one", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("COMPLETED", { releasable: true });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(document.getElementById("release")).toBeEnabled();
    expect(
      within(section("pathology-section-findings")).queryByText(
        messages["pathology.locked.caseCompleted"],
      ),
    ).not.toBeInTheDocument();
  });

  it("says the case carries no report rather than showing an empty list", async () => {
    servedCase = caseAtStage("COMPLETED");
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section("pathology-section-reports")).getByText(
        messages["pathology.empty.noReports"],
      ),
    ).toBeInTheDocument();
  });

  // FR-10.10: the summary shows the count, and resting on the row is what
  // names the requests behind it.
  it("names the open requests on the summary row that counts them", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      requests: [
        { id: "1", value: "Deeper sections", status: "OPENED" },
        { id: "2", value: "Special stain", status: "OPENED" },
      ],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const named = intl.formatMessage(
      { id: "pathology.badge.openRequests" },
      { names: "Deeper sections, Special stain" },
    );
    const row = summaryRow(messages["pathology.label.request"]);

    expect(row).toHaveAttribute("title", named);
    // A title is a hover and nothing more, so the same sentence has to be in
    // the row for a reader who cannot hover anything.
    expect(row).toHaveTextContent(named);
  });
  // Carbon's own label for a tag's close button is the English word Dismiss,
  // and the deprecated filter Tag this replaced said "Clear filter" in
  // English whatever the user's language.
  it("names the request a chip removes, in the user's language", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST", {
      requests: [{ id: "1", value: "Deeper sections", status: "OPENED" }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByRole("button", {
        name: intl.formatMessage(
          { id: "common.removeSelection" },
          { name: "Deeper sections" },
        ),
      }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Dismiss" })).toBeNull();
  });
  // The header names what the section holds when it holds anything, derived
  // from the rows, which is the outcome badge the shell asks for and a
  // different claim from Complete. A section with no rows and no rule says
  // nothing.
  it("counts the cassettes in use on the grossing header", async () => {
    servedCase = caseAtStage("COMPLETED", {
      blocks: [{ id: "1" }, { id: "2" }, { id: "3", active: false }],
    });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section("pathology-section-grossing")).getByText(
        intl.formatMessage(
          { id: "pathology.badge.cassetteCount" },
          { count: 2 },
        ),
      ),
    ).toBeInTheDocument();
  });

  it("puts the release reason in the control's own helper slot", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("UNDER_REVIEW", { assignedPathologistId: "" });
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const hint = screen.getByText(
      messages["pathology.locked.releaseNoPathologist"],
    );
    expect(hint.closest(".cds--checkbox-wrapper")).not.toBeNull();
  });

  describe("grossing records whether the specimen needs decalcifying", () => {
    const decalcificationBox = () =>
      screen.getByLabelText(
        messages["pathology.label.decalcificationRequired"],
      );

    const decalcificationHeader = () =>
      screen.getByRole("button", { name: /^3\. Decalcification/ });

    const decalcificationStep = () =>
      rail()
        .getByText(messages["pathology.stage.decalcification"])
        .closest("li");

    it("posts an untouched case as not needing decalcification, and a ticked one as needing it", async () => {
      servedCase = caseAtStage("GROSSING", { decalcificationRequired: false });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(decalcificationBox()).not.toBeChecked();
      expect((await postedCase()).decalcificationRequired).toBe(false);

      const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
      await act(async () => {
        await callback({ status: 200, json: async () => ({}) });
      });

      fireEvent.click(decalcificationBox());
      expect(decalcificationBox()).toBeChecked();
      expect((await postedCase()).decalcificationRequired).toBe(true);
    });

    it("locks decalcification as not required on a specimen grossing did not flag, and marks its rail step not applicable", async () => {
      servedCase = caseAtStage("GROSSING", { decalcificationRequired: false });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());

      expect(decalcificationHeader()).toBeDisabled();
      expect(
        section("pathology-section-decalcification").textContent,
      ).toContain(messages["pathology.locked.decalcNotRequired"]);
      expect(decalcificationStep()).toHaveTextContent(
        messages["caseView.badge.notApplicable"],
      );
    });

    // The flag records what was done to the specimen once the case has gone
    // past the stage, and the server ignores it from then on.
    it("freezes the flag on a case stored past decalcification, and says why", async () => {
      servedCase = caseAtStage("PROCESSING");
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(decalcificationBox()).toBeDisabled();
      expect(decalcificationBox()).toHaveAccessibleDescription(
        messages["pathology.locked.decalcificationPassed"],
      );
    });

    it("leaves the flag editable at grossing and at decalcification", async () => {
      servedCase = caseAtStage("DECALCIFICATION");
      const atDecalcification = renderCaseView();
      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(decalcificationBox()).toBeEnabled();
      expect(
        screen.queryByText(messages["pathology.locked.decalcificationPassed"]),
      ).toBeNull();
      atDecalcification.unmount();

      servedCase = caseAtStage("GROSSING");
      renderCaseView();
      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(decalcificationBox()).toBeEnabled();
      expect(decalcificationBox()).not.toHaveAccessibleDescription(
        messages["pathology.locked.decalcificationPassed"],
      );
    });

    it("says on the summary whether the specimen needs decalcifying", async () => {
      servedCase = caseAtStage("GROSSING", { decalcificationRequired: true });
      const { unmount } = renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(
        summaryRow(messages["pathology.label.decalcificationRequired"]),
      ).toBeDefined();
      expect(
        summaryValue(messages["pathology.label.decalcificationRequired"]),
      ).toBe(messages["label.yes"]);
      unmount();

      servedCase = caseAtStage("GROSSING", { decalcificationRequired: false });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      expect(
        summaryValue(messages["pathology.label.decalcificationRequired"]),
      ).toBe(messages["label.no"]);
    });
  });

  it("labels the grossing description as the macroscopic description", async () => {
    renderCaseView();

    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      screen.getByLabelText(messages["pathology.label.grossExam"]),
    ).toHaveAttribute("id", "grossExam");
  });

  describe("the tissue each cassette holds", () => {
    // Named after the row it sits in, so the label starts with the field's
    // word.
    const tissueCombobox = (row) =>
      within(row).getByRole("combobox", {
        name: new RegExp("^" + messages["pathology.label.tissueType"]),
      });

    it("shows a cassette's recorded tissue, offers the served ones, and posts the one picked on that cassette alone", async () => {
      servedCase = caseAtStage("GROSSING", {
        blocks: [
          savedBlock("1", "A1", { tissueTypeId: "21" }),
          savedBlock("2", "A2"),
        ],
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());

      expect(
        tissueCombobox(rowIn("pathology-section-grossing", "A1")),
      ).toHaveValue("Skin");

      const user = userEvent.setup();
      const a2 = rowIn("pathology-section-grossing", "A2");
      await user.click(tissueCombobox(a2));
      const offered = within(a2).getAllByRole("option");
      expect(offered.map((option) => option.textContent)).toEqual([
        "Skin",
        "Bone",
      ]);
      await user.click(offered[1]);

      const body = await postedCase();
      expect(body.blocks.map((block) => block.tissueTypeId)).toEqual([
        "21",
        "22",
      ]);
    });

    // The served list holds active entries only, so a tissue type retired
    // after a cassette recorded it would otherwise leave the row looking as
    // if nothing had been recorded.
    it("still names a tissue type a cassette holds after the laboratory has retired it, and keeps it on save", async () => {
      servedCase = caseAtStage("GROSSING", {
        blocks: [savedBlock("1", "A1", { tissueTypeId: "23" })],
        tissueTypeNames: { 23: "Cartilage" },
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());

      expect(
        tissueCombobox(rowIn("pathology-section-grossing", "A1")),
      ).toHaveValue("Cartilage");
      expect((await postedCase()).blocks[0].tissueTypeId).toBe("23");
    });

    it("holds the picker back without claiming none are configured while the tissue types have not arrived", async () => {
      // What the server helper hands back when the request fails.
      tissueCatalogue = undefined;
      servedCase = caseAtStage("GROSSING", {
        blocks: [savedBlock("1", "A1")],
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());

      const a1 = rowIn("pathology-section-grossing", "A1");
      expect(tissueCombobox(a1)).toBeDisabled();
      expect(
        within(a1).queryByText(messages["pathology.empty.noTissueTypes"]),
      ).toBeNull();
    });

    // A refused save tells the bench to reload, and one of the things that
    // can have changed is the tissue type list itself.
    it("reads the tissue types again whenever it reads the case again", async () => {
      servedCase = caseAtStage("GROSSING", {
        blocks: [savedBlock("1", "A1")],
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      await postedCase();
      tissueCatalogue = [...tissueCatalogue, { id: "24", value: "Fat" }];
      const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
      await act(async () => {
        await callback({ status: 200, json: async () => ({}) });
      });

      const user = userEvent.setup();
      const a1 = rowIn("pathology-section-grossing", "A1");
      await user.click(tissueCombobox(a1));
      expect(
        within(a1)
          .getAllByRole("option")
          .map((option) => option.textContent),
      ).toEqual(["Skin", "Bone", "Fat"]);
    });

    it("holds the picker back and says why when the laboratory has configured no tissue types, but says nothing on a row locked for its own reasons", async () => {
      tissueCatalogue = [];
      servedCase = caseAtStage("GROSSING", {
        blocks: [
          savedBlock("1", "A1"),
          savedBlock("2", "A2", { active: false }),
        ],
      });
      renderCaseView();

      await waitFor(() => expect(stageReadout()).not.toBeNull());
      fireEvent.click(showDeactivatedSwitch("pathology-section-grossing"));

      const a1 = rowIn("pathology-section-grossing", "A1");
      expect(tissueCombobox(a1)).toBeDisabled();
      expect(
        within(a1).getByText(messages["pathology.empty.noTissueTypes"]),
      ).toBeInTheDocument();

      const a2 = rowIn("pathology-section-grossing", "A2");
      expect(tissueCombobox(a2)).toBeDisabled();
      expect(
        within(a2).queryByText(messages["pathology.empty.noTissueTypes"]),
      ).toBeNull();
    });
  });
});

describe("Embedding section", () => {
  const EMBEDDING = "pathology-section-embedding";

  const embedName = (designation) =>
    intl.formatMessage(
      { id: "pathology.action.embedCassette" },
      { designation },
    );

  const embedButton = (designation) =>
    screen.getByRole("button", { name: embedName(designation) });

  const embeddedOf = (embedded, total) =>
    intl.formatMessage(
      { id: "pathology.badge.embeddedOf" },
      { embedded, total },
    );

  const outstanding = (designations) =>
    intl.formatMessage({ id: "pathology.badge.outstanding" }, { designations });

  // The badge sits in the section's header; the body repeats the count in
  // its banner, so the header is where the badge is looked for.
  const header = () =>
    within(section(EMBEDDING).querySelector(".cds--accordion__heading"));

  const asBlock = (row) => ({ ...row, cassetteState: "BLOCK" });

  const oneOfThree = () => [
    asBlock(savedBlock("1", "A1", { tissueTypeId: "21" })),
    savedBlock("2", "A2"),
    savedBlock("3", "A3"),
  ];

  const openAtEmbedding = async (blocks, overrides = {}) => {
    servedCase = caseAtStage("EMBEDDING", {
      blocks,
      tissueTypeNames: { 21: "Skin" },
      ...overrides,
    });
    renderCaseView();
    await waitFor(() => expect(stageReadout()).not.toBeNull());
  };

  it("lists every saved cassette and block with its tissue type, and not a cassette added since the last save", async () => {
    await openAtEmbedding(oneOfThree());

    expect(
      within(rowIn(EMBEDDING, "A1")).getByText("Skin"),
    ).toBeInTheDocument();
    expect(
      within(rowIn(EMBEDDING, "A2")).getByText(
        messages["caseView.label.notRecorded"],
      ),
    ).toBeInTheDocument();

    fireEvent.click(addCassetteButton());

    expect(pendingRows("pathology-section-grossing")).toHaveLength(1);
    expect(pendingRows(EMBEDDING)).toHaveLength(0);
    expect(
      section(EMBEDDING).querySelectorAll(".pathology-case-view__row"),
    ).toHaveLength(3);
  });

  // The grossing list already deactivates these rows; a second button of the
  // same name here would be announced twice. Remove never arises: the list
  // holds saved rows only.
  it("offers Embed on a cassette only, and no Deactivate on any row", async () => {
    await openAtEmbedding(oneOfThree());

    expect(embedButton("A2")).toBeEnabled();
    expect(embedButton("A2")).toHaveAttribute("id", "embedCassette-2");
    expect(
      within(rowIn(EMBEDDING, "A1")).queryByRole("button", {
        name: embedName("A1"),
      }),
    ).toBeNull();
    expect(
      within(section(EMBEDDING)).queryByText(messages["common.deactivate"], {
        selector: "button",
      }),
    ).toBeNull();
  });

  it("posts the cassette's own id, then reads the case back and says it was embedded", async () => {
    await openAtEmbedding(oneOfThree());

    fireEvent.click(embedButton("A2"));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [url, , callback] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/pathology/block/2/embed");
    // One embedding at a time, so the read-back answers for one cassette.
    expect(embedButton("A3")).toBeDisabled();

    const fetchesBefore = caseFetchCount();
    act(() => callback(asBlock(savedBlock("2", "A2"))));

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: intl.formatMessage(
          { id: "pathology.toast.embedded" },
          { designation: "A2" },
        ),
      }),
    );
  });

  it("posts nothing for a second Embed while the first is unanswered", async () => {
    await openAtEmbedding(oneOfThree());

    fireEvent.click(embedButton("A2"));
    fireEvent.click(embedButton("A3"));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
  });

  // A refusal can mean another change landed on the row, so a clean form is
  // read again to show the row as it is stored.
  it("says the embedding failed and reads the case back when the server refuses it", async () => {
    await openAtEmbedding(oneOfThree());

    fireEvent.click(embedButton("A2"));
    const callback = postToOpenElisServerJsonResponse.mock.calls[0][2];
    const fetchesBefore = caseFetchCount();
    act(() => callback({ error: "Block 2 is deactivated", status: 400 }));

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: intl.formatMessage(
          { id: "pathology.toast.embedFailed" },
          { designation: "A2" },
        ),
      }),
    );
    await waitFor(() => expect(embedButton("A2")).toHaveFocus());
  });

  // The bench can type while the request is out; reading the case back then
  // would throw the typing away, so only the embedded row changes.
  it("keeps what was typed while an embedding was answered, and marks the row a block", async () => {
    await openAtEmbedding(oneOfThree());

    fireEvent.click(embedButton("A2"));
    const field = document.getElementById("embeddingLocation2");
    field.focus();
    fireEvent.change(field, { target: { value: "Tray 4" } });
    const fetchesBefore = caseFetchCount();
    act(() =>
      postToOpenElisServerJsonResponse.mock.calls[0][2](
        asBlock(savedBlock("2", "A2")),
      ),
    );
    // The answer's focus move is deferred; let it run before looking.
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));

    expect(caseFetchCount()).toBe(fetchesBefore);
    expect(field).toHaveValue("Tray 4");
    expect(field, "typing goes on where it was").toHaveFocus();
    expect(
      within(rowIn(EMBEDDING, "A2")).getByText(
        messages["pathology.label.block"],
      ),
    ).toBeInTheDocument();
    expect(
      within(rowIn(EMBEDDING, "A2")).queryByRole("button", {
        name: embedName("A2"),
      }),
    ).toBeNull();
    // Still unsaved, so the next Embed waits for a save.
    expect(embedButton("A3")).toBeDisabled();
    expect(embedButton("A3")).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeEmbed"],
    );
  });

  it("moves focus to the next cassette still waiting, and to the section header once none is", async () => {
    const [a1, a2, a3] = oneOfThree();
    await openAtEmbedding([a1, a2, a3]);

    fireEvent.click(embedButton("A2"));
    servedCase = { ...servedCase, blocks: [a1, asBlock(a2), a3] };
    act(() => postToOpenElisServerJsonResponse.mock.calls[0][2](asBlock(a2)));
    await waitFor(() => expect(embedButton("A3")).toHaveFocus());

    fireEvent.click(embedButton("A3"));
    servedCase = { ...servedCase, blocks: [a1, asBlock(a2), asBlock(a3)] };
    act(() => postToOpenElisServerJsonResponse.mock.calls[1][2](asBlock(a3)));
    await waitFor(() =>
      expect(
        section(EMBEDDING).querySelector("button.cds--accordion__heading"),
      ).toHaveFocus(),
    );
  });

  // The case is read back after an embedding, so an edit not yet saved
  // would be thrown away by it without a word.
  it("holds Embed back while there are unsaved changes, and says why", async () => {
    await openAtEmbedding(oneOfThree());
    expect(embedButton("A2")).toBeEnabled();

    fireEvent.change(document.getElementById("embeddingLocation1"), {
      target: { value: "Rack 3, shelf B" },
    });

    expect(embedButton("A2")).toBeDisabled();
    expect(embedButton("A2")).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeEmbed"],
    );

    fireEvent.click(
      screen.getByRole("button", { name: messages["caseView.action.discard"] }),
    );

    expect(embedButton("A2")).toBeEnabled();
    expect(embedButton("A2")).not.toHaveAttribute("title");
  });

  it("edits the location of the row it is typed into", async () => {
    await openAtEmbedding([
      savedBlock("1", "A1", { active: false }),
      savedBlock("2", "A2"),
    ]);

    fireEvent.change(document.getElementById("embeddingLocation1"), {
      target: { value: "Rack 3, shelf B" },
    });

    const posted = await postedCase();
    expect(posted.blocks.map((block) => block.location)).toEqual([
      "",
      "Rack 3, shelf B",
    ]);
  });

  it("labels the storage location and says it is free text for now", async () => {
    await openAtEmbedding(oneOfThree());

    const field = within(rowIn(EMBEDDING, "A2")).getByLabelText(
      new RegExp("^" + messages["common.storageLocation"]),
    );
    expect(field).toHaveAttribute("id", "embeddingLocation1");
    expect(
      within(rowIn(EMBEDDING, "A2")).getByText(
        messages["pathology.label.storageLocationInterim"],
      ),
    ).toBeInTheDocument();
  });

  it("badges the section with how many are embedded, out of the cassettes in use", async () => {
    const retired = savedBlock("4", "A4", { active: false });
    await openAtEmbedding([...oneOfThree(), retired]);

    expect(header().getByText(embeddedOf(1, 3))).toBeInTheDocument();
  });

  it("badges the section Complete once every cassette is a block", async () => {
    await openAtEmbedding(oneOfThree().map(asBlock));

    expect(header().getByText(messages["common.complete"])).toBeInTheDocument();
    expect(within(section(EMBEDDING)).queryByText(embeddedOf(3, 3))).toBeNull();
  });

  it("badges nothing and says how cassettes arrive when none has been cut", async () => {
    await openAtEmbedding([]);

    expect(
      section(EMBEDDING)
        .querySelector(".cds--accordion__heading")
        .querySelector(".cds--tag"),
    ).toBeNull();
    expect(
      within(section(EMBEDDING)).getByText(
        messages["pathology.empty.noCassettes"],
      ),
    ).toBeInTheDocument();
  });

  it("opens on a warning that counts the cassettes and names the outstanding ones", async () => {
    await openAtEmbedding(oneOfThree());

    const body = within(section(EMBEDDING));
    expect(
      body.getByText(embeddedOf(1, 3), {
        selector: ".cds--inline-notification__title",
      }),
    ).toBeInTheDocument();
    expect(body.getByText(outstanding("A2, A3"))).toBeInTheDocument();
  });

  it("names the outstanding cassettes on the rail at embedding", async () => {
    await openAtEmbedding(oneOfThree());

    expect(rail().getByText(outstanding("A2, A3"))).toBeInTheDocument();
  });

  it("names nothing on the rail for a case still at grossing", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: oneOfThree() });
    renderCaseView();
    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(rail().queryByText(outstanding("A2, A3"))).toBeNull();
  });

  it("sums embedding up in the summary and names the outstanding cassettes on the row", async () => {
    await openAtEmbedding(oneOfThree());

    const label = messages["pathology.stage.embedding"];
    expect(summaryValue(label)).toBe(embeddedOf(1, 3));
    expect(summaryRow(label)).toHaveAttribute("title", outstanding("A2, A3"));
  });

  it("keeps a deactivated cassette out of the list until the list's switch is on", async () => {
    await openAtEmbedding([
      ...oneOfThree(),
      savedBlock("4", "A4", { active: false }),
    ]);

    expect(queryRowIn(EMBEDDING, "A4")).toBeNull();

    fireEvent.click(showDeactivatedSwitch(EMBEDDING));

    const deactivated = rowIn(EMBEDDING, "A4");
    expect(
      within(deactivated).getByText(messages["caseView.badge.deactivated"]),
    ).toBeInTheDocument();
    expect(
      within(deactivated).queryByRole("button", { name: embedName("A4") }),
    ).toBeNull();
  });

  it("offers no count input in the embedding section", async () => {
    await openAtEmbedding(oneOfThree());

    expect(section(EMBEDDING).querySelector('input[type="number"]')).toBeNull();
    expect(section(EMBEDDING).querySelector(".cds--number")).toBeNull();
  });
});

describe("Microtomy section", () => {
  const MICROTOMY = "pathology-section-microtomy";
  const asBlock = (row) => ({ ...row, cassetteState: "BLOCK" });

  const slideCount = (count) =>
    intl.formatMessage({ id: "pathology.badge.slideCount" }, { count });

  // The group's own header, apart from the slides under it.
  const groupHeader = (designation) =>
    within(
      slideGroup(designation).querySelector(
        ".pathology-case-view__group-header",
      ),
    );

  const openAtMicrotomy = async (overrides = {}, configurationProperties) => {
    servedCase = caseAtStage("MICROTOMY", overrides);
    renderCaseView(configurationProperties);
    await waitFor(() => expect(stageReadout()).not.toBeNull());
  };

  it("lists each saved block in use with its tissue and the count of its slides, and says so for a block with none", async () => {
    await openAtMicrotomy({
      blocks: [
        asBlock(savedBlock("1", "A1", { tissueTypeId: "21" })),
        asBlock(savedBlock("2", "A2")),
      ],
      slides: [
        savedSlide("5", "1", "1"),
        savedSlide("6", "2", "1"),
        savedSlide("7", "3", "1", { active: false }),
      ],
      tissueTypeNames: { 21: "Skin" },
    });

    expect(groupHeader("A1").getByText("Skin")).toBeInTheDocument();
    expect(groupHeader("A1").getByText(slideCount(2))).toBeInTheDocument();
    expect(groupHeader("A1").getByText("ACC9.A1")).toBeInTheDocument();
    expect(
      groupHeader("A2").getByText(
        messages["pathology.empty.noSlidesUnderBlock"],
      ),
    ).toBeInTheDocument();
    // The tissue says what it is to a screen reader, so "Not recorded" says
    // what is missing.
    expect(
      groupHeader("A2").getByText(messages["caseView.label.notRecorded"]),
    ).toHaveTextContent(
      messages["pathology.label.tissueType"] +
        " " +
        messages["caseView.label.notRecorded"],
    );
    expect(
      within(slideGroupBody("A1")).getAllByText(/^[12]$/, {
        selector: ".pathology-case-view__row-designation",
      }),
    ).toHaveLength(2);
    expect(
      within(section(MICROTOMY)).getByText(
        messages["pathology.label.blockByBlock"],
      ),
    ).toBeInTheDocument();
    // The section header counts the slides of the whole case.
    expect(
      within(
        section(MICROTOMY).querySelector(".cds--accordion__heading"),
      ).getByText(slideCount(2)),
    ).toBeInTheDocument();
  });

  it("adds a slide under the block whose Add slide was pressed, and posts its block, level and stain with no block picker", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1")), asBlock(savedBlock("2", "A2"))],
    });

    fireEvent.click(addSlideTo("A2"));

    const added = pendingRows(MICROTOMY);
    expect(added).toHaveLength(1);
    expect(slideGroupBody("A2")).toContainElement(added[0]);
    const row = added[0].closest(".pathology-case-view__row");
    expect(within(row).getAllByRole("combobox")).toEqual([stainCombobox(row)]);

    fireEvent.change(document.getElementById("slideLevel0"), {
      target: { value: "2" },
    });
    const user = userEvent.setup();
    await user.click(stainCombobox(row));
    await user.click(within(row).getByRole("option", { name: "PAS" }));

    const body = await postedCase();
    expect(body.slides).toEqual([
      { blockId: "2", level: "2", stainId: "32", location: "" },
    ]);
  });

  it("moves focus from the Add slide pressed to the new row's level", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [savedSlide("5", "1", "1")],
    });

    const user = userEvent.setup();
    await user.click(addSlideTo("A1"));

    const added = pendingRows(MICROTOMY)[0].closest(
      ".pathology-case-view__row",
    );
    const level = within(added).getByLabelText(
      new RegExp("^" + messages["pathology.label.level"]),
    );
    await waitFor(() => expect(level).toHaveFocus());
  });

  it("leaves focus where the person is typing when a slide is added", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [savedSlide("5", "1", "1")],
    });

    const existingLevel = document.getElementById("slideLevel0");
    existingLevel.focus();
    fireEvent.click(addSlideTo("A1"));
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));

    expect(pendingRows(MICROTOMY)).toHaveLength(1);
    expect(existingLevel).toHaveFocus();
  });

  it("holds a cassette's Add slide back until it is embedded, and says why", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1")), savedBlock("2", "A2")],
    });

    expect(addSlideTo("A1")).toBeEnabled();
    expect(addSlideTo("A1")).not.toHaveAttribute("title");
    expect(addSlideTo("A1")).not.toHaveAttribute("aria-describedby");
    expect(document.getElementById("slideGroupHint-1")).toBeNull();
    expect(addSlideTo("A2")).toBeDisabled();
    expect(addSlideTo("A2")).toHaveAttribute(
      "title",
      messages["pathology.locked.embedBeforeSlides"],
    );
    // A disabled button takes no focus, so the reason is written out too,
    // and the button is described by it.
    const hint = groupHeader("A2").getByText(
      messages["pathology.locked.embedBeforeSlides"],
    );
    expect(hint).toHaveAttribute("id", "slideGroupHint-2");
    expect(addSlideTo("A2")).toHaveAttribute(
      "aria-describedby",
      "slideGroupHint-2",
    );
  });

  it("offers every saved row in use once the laboratory has stopped tracking embedding", async () => {
    await openAtMicrotomy(
      { blocks: [savedBlock("2", "A2")] },
      { PATHOLOGY_STAGE_EMBEDDING_ENABLED: "false" },
    );

    expect(addSlideTo("A2")).toBeEnabled();
    fireEvent.click(addSlideTo("A2"));
    expect(pendingRows(MICROTOMY)).toHaveLength(1);
  });

  it("holds every Add slide back on a signed-out case, and says why", async () => {
    servedCase = caseAtStage("COMPLETED", {
      blocks: [asBlock(savedBlock("1", "A1")), savedBlock("2", "A2")],
    });
    renderCaseView({ PATHOLOGY_STAGE_EMBEDDING_ENABLED: "false" });
    await waitFor(() => expect(stageReadout()).not.toBeNull());

    ["A1", "A2"].forEach((designation) => {
      expect(addSlideTo(designation)).toBeDisabled();
      expect(addSlideTo(designation)).toHaveAttribute(
        "title",
        messages["pathology.locked.caseCompleted"],
      );
      const hintId = addSlideTo(designation).getAttribute("aria-describedby");
      expect(document.getElementById(hintId)).toHaveTextContent(
        messages["pathology.locked.caseCompleted"],
      );
      expect(
        groupHeader(designation).getByText(
          messages["pathology.locked.caseCompleted"],
        ),
      ).toBeInTheDocument();
    });
  });

  it("says no cassette has been cut, and offers no Add slide, while the case has no saved cassette in use", async () => {
    await openAtMicrotomy({
      blocks: [savedBlock("1", "A1", { active: false })],
    });

    expect(
      within(section(MICROTOMY)).getByText(
        messages["pathology.empty.noCassettes"],
      ),
    ).toBeInTheDocument();
    expect(
      within(section(MICROTOMY)).queryByText(
        messages["pathology.action.addSlide"],
        { selector: "button" },
      ),
    ).toBeNull();
  });

  it("names a stain a slide holds after the laboratory has retired it, among the served ones", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [savedSlide("5", "1", "1", { stainId: "39" })],
      stainNames: { 39: "Orcein" },
    });

    const row = rowIn(MICROTOMY, "1");
    expect(stainCombobox(row)).toHaveValue("Orcein");
    const user = userEvent.setup();
    await user.click(stainCombobox(row));
    expect(
      within(row)
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["H&E", "PAS", "Orcein"]);
  });

  it("holds the stain picker back and says why when the laboratory has configured no stains", async () => {
    stainCatalogue = [];
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [savedSlide("5", "1", "1")],
    });

    const row = rowIn(MICROTOMY, "1");
    expect(stainCombobox(row)).toBeDisabled();
    expect(
      within(row).getByText(messages["pathology.empty.noStains"]),
    ).toBeInTheDocument();
  });

  it("caps the level at the width the server stores", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [savedSlide("5", "1", "1", { level: "3" })],
    });

    const level = within(rowIn(MICROTOMY, "1")).getByLabelText(
      new RegExp("^" + messages["pathology.label.level"]),
    );
    expect(level).toHaveValue("3");
    expect(level).toHaveAttribute("maxLength", "16");
  });

  it("opens a block holding slides and closes one with none, keeps the slides on the page while closed, and opens a group a slide is added to", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1")), asBlock(savedBlock("2", "A2"))],
      slides: [savedSlide("5", "1", "1")],
    });

    expect(slidesToggle("A1")).toHaveAttribute("aria-expanded", "true");
    expect(slideGroupBody("A1")).not.toHaveAttribute("hidden");
    expect(slidesToggle("A2")).toHaveAttribute("aria-expanded", "false");
    expect(slideGroupBody("A2")).toHaveAttribute("hidden");

    fireEvent.click(slidesToggle("A1"));

    expect(slidesToggle("A1")).toHaveAttribute("aria-expanded", "false");
    expect(slideGroupBody("A1")).toHaveAttribute("hidden");
    expect(queryRowIn(MICROTOMY, "1")).toBeInTheDocument();

    // A slide added to a group the person closed opens it, so the new row
    // is never typed into out of sight.
    fireEvent.click(addSlideTo("A1"));
    expect(slidesToggle("A1")).toHaveAttribute("aria-expanded", "true");
    expect(slideGroupBody("A1")).not.toHaveAttribute("hidden");

    fireEvent.click(addSlideTo("A2"));
    expect(slidesToggle("A2")).toHaveAttribute("aria-expanded", "true");
    expect(slideGroupBody("A2")).not.toHaveAttribute("hidden");
  });

  it("says a block has no slides only when nothing is listed under it", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1")), asBlock(savedBlock("2", "A2"))],
      slides: [
        savedSlide("5", "1", "1", { active: false }),
        // A retired slide cut before slides named their block.
        { id: "6", designation: "7", blockId: null, active: false },
      ],
    });

    const noSlides = messages["pathology.empty.noSlidesUnderBlock"];
    expect(groupHeader("A1").getByText(noSlides)).toBeInTheDocument();

    fireEvent.click(showDeactivatedSwitch(MICROTOMY));

    // A1 lists its retired slide, so it has had one cut.
    expect(slideGroupBody("A1")).toContainElement(rowIn(MICROTOMY, "1"));
    expect(groupHeader("A1").queryByText(noSlides)).toBeNull();
    expect(groupHeader("A1").queryByText(slideCount(0))).toBeNull();
    expect(groupHeader("A2").getByText(noSlides)).toBeInTheDocument();
    const legacy = screen
      .getByRole("button", {
        name: messages["pathology.label.slidesWithoutBlock"],
      })
      .closest(".pathology-case-view__group");
    expect(legacy).toContainElement(rowIn(MICROTOMY, "7"));
    expect(
      within(
        legacy.querySelector(".pathology-case-view__group-header"),
      ).queryByText(noSlides),
    ).toBeNull();
  });

  it("keeps a retired block in sight while it still holds a slide in use, and offers it no Add slide", async () => {
    await openAtMicrotomy({
      blocks: [
        asBlock(savedBlock("1", "A1")),
        asBlock(savedBlock("2", "A2", { active: false })),
        asBlock(savedBlock("3", "A3", { active: false })),
      ],
      slides: [savedSlide("5", "1", "2")],
    });

    expect(
      groupHeader("A2").getByText(messages["caseView.badge.deactivated"]),
    ).toBeInTheDocument();
    expect(slideGroupBody("A2")).toContainElement(rowIn(MICROTOMY, "1"));
    expect(
      groupHeader("A2").queryByText(messages["pathology.action.addSlide"]),
    ).toBeNull();
    // A retired block with no slide in use waits for the switch.
    expect(
      screen.queryByRole("button", {
        name: intl.formatMessage(
          { id: "pathology.label.slidesOf" },
          { designation: "A3" },
        ),
      }),
    ).toBeNull();

    fireEvent.click(showDeactivatedSwitch(MICROTOMY));
    expect(slideGroup("A3")).toBeInTheDocument();
  });

  it("lists the slides no block is recorded for together, last, with no Add slide", async () => {
    await openAtMicrotomy({
      blocks: [asBlock(savedBlock("1", "A1"))],
      slides: [
        savedSlide("5", "1", "1"),
        // A slide cut before slides named their block.
        { id: "6", designation: "7", blockId: null, active: true },
      ],
    });

    const legacyToggle = screen.getByRole("button", {
      name: messages["pathology.label.slidesWithoutBlock"],
    });
    const legacy = legacyToggle.closest(".pathology-case-view__group");
    const groups = section(MICROTOMY).querySelectorAll(
      ".pathology-case-view__group",
    );
    expect(groups[groups.length - 1]).toBe(legacy);
    expect(legacy).toContainElement(rowIn(MICROTOMY, "7"));
    expect(
      within(legacy).queryByText(messages["pathology.action.addSlide"], {
        selector: "button",
      }),
    ).toBeNull();
  });

  it("shows the slides on record read-only once the laboratory has stopped tracking microtomy", async () => {
    servedCase = caseAtStage("STAINING", {
      // A2 is still a cassette, which would hold its Add slide back for a
      // reason of its own on a section the bench can edit.
      blocks: [asBlock(savedBlock("1", "A1")), savedBlock("2", "A2")],
      slides: [savedSlide("5", "1", "1", { level: "1" })],
    });
    renderCaseView({ PATHOLOGY_STAGE_MICROTOMY_ENABLED: "false" });
    await waitFor(() => expect(stageReadout()).not.toBeNull());

    const microtomy = section(MICROTOMY);
    expect(
      within(microtomy.querySelector(".cds--accordion__heading")).getByText(
        messages["pathology.locked.stageDisabled"],
      ),
    ).toBeInTheDocument();
    expect(
      within(microtomy).getByText(
        messages["pathology.banner.stageDisabledRows"],
      ),
    ).toBeInTheDocument();
    expect(document.getElementById("slideLevel0")).toBeDisabled();
    expect(document.getElementById("slideLocation0")).toBeDisabled();
    expect(addSlideTo("A1")).toBeDisabled();
    // The badge says why the whole section is read-only, so no block adds a
    // reason of its own.
    ["A1", "A2"].forEach((designation) => {
      expect(addSlideTo(designation)).toBeDisabled();
      expect(addSlideTo(designation)).not.toHaveAttribute("aria-describedby");
    });
    expect(
      microtomy.querySelector(".pathology-case-view__group-hint"),
    ).toBeNull();
    expect(
      within(microtomy).queryByText(messages["common.deactivate"], {
        selector: "button",
      }),
    ).toBeNull();
    // Still not applicable on the rail, and the slide on record still counts.
    expect(
      rail().getByText(messages["pathology.stage.microtomy"]).closest("li"),
    ).toHaveTextContent(messages["caseView.badge.notApplicable"]);
    expect(summaryValue(messages["pathology.label.slides"])).toBe("1");
  });

  it("keeps a switched-off microtomy with no slides locked, saying it is not tracked", async () => {
    servedCase = caseAtStage("STAINING", {
      blocks: [asBlock(savedBlock("1", "A1"))],
    });
    renderCaseView({ PATHOLOGY_STAGE_MICROTOMY_ENABLED: "false" });
    await waitFor(() => expect(stageReadout()).not.toBeNull());

    expect(
      within(section(MICROTOMY)).queryByText(
        messages["pathology.banner.stageDisabledRows"],
      ),
    ).toBeNull();
    expect(
      section(MICROTOMY).querySelector("button.cds--accordion__heading"),
    ).toBeDisabled();
  });
});
