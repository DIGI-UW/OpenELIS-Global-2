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

// The display list is served with the stage id as its own text, so an
// assertion on the catalogue wording can only pass if the label came from
// React Intl rather than from the server's English.
const STATUS_LIST = PATHOLOGY_STAGES.map((id) => ({ id, value: id }));

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
  ...overrides,
});

let servedCase;
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

// The stage control is a Carbon Dropdown. Its wrapper carries the id, its
// toggle carries the accessible name and the current stage, and its options
// exist only while the menu is open.
const statusSelect = () => document.getElementById("status");

const statusToggle = () =>
  screen.getByRole("combobox", {
    name: messages["common.status"],
  });

// Scoped to the stage control: the technician and pathologist selectors on
// the same screen are native selects, whose own <option> elements answer to
// the same role.
const statusOptionTexts = () => {
  fireEvent.click(statusToggle());
  const texts = within(statusSelect())
    .getAllByRole("option")
    .map((option) => option.textContent);
  fireEvent.click(statusToggle());
  return texts;
};

const setStage = (stageKey) => {
  fireEvent.click(statusToggle());
  fireEvent.click(
    within(statusSelect()).getByRole("option", { name: messages[stageKey] }),
  );
};

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

const addSlideButton = () =>
  screen.getByRole("button", { name: messages["pathology.action.addSlide"] });

// Named after the row it sits in, so the label starts with the field's word.
const parentBlockCombobox = (row) =>
  within(row).getByRole("combobox", {
    name: new RegExp("^" + messages["pathology.label.parentBlock"]),
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
  requestCatalogue = [];
  requestStatusCatalogue = [
    { id: "OPENED", value: "Opened" },
    { id: "COMPLETED", value: "Completed" },
    { id: "CANCELLED", value: "Cancelled" },
  ];
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === "/rest/displayList/PATHOLOGY_STATUS") {
      return callback(STATUS_LIST);
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

      await waitFor(() => expect(statusSelect()).not.toBeNull());

      EXPECTED_SECTIONS.forEach(([number, titleKey]) => {
        expect(
          screen.getByText(sectionHeading(number, titleKey)),
        ).toBeInTheDocument();
      });
    },
  );

  it("opens the section the bench is working on and leaves case information collapsed", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(
      screen.getByRole("button", { name: /1\. Case Information/ }),
    ).toHaveAttribute("aria-expanded", "false");
    expect(
      screen.getByRole("button", { name: /2\. Grossing/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });

  it("names the case in its own page heading", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

  it("tells a screen reader why the stage is set by hand, from the control itself", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    const describedBy = statusToggle().getAttribute("aria-describedby");
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy)).toHaveTextContent(
      messages["pathology.banner.transitionsLater"],
    );
  });

  it("keeps the grossing description open beside the reading the pathologist is writing", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(summaryValue(messages["pathology.label.report"])).toBe(
      messages["common.none"],
    );
  });

  it("counts the reports a case does carry", async () => {
    servedCase = caseAtStage("COMPLETED", { reports: [{ id: "1" }] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(summaryValue(messages["pathology.label.report"])).toBe("1");
  });

  it("says a case information field was not recorded instead of printing an empty or null value", async () => {
    servedCase = caseAtStage("GROSSING", {
      requester: null,
      specimenTypes: [],
    });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(caseInfoValue(messages["sample.receivedDate"])).toBe("2024-06-03");
    expect(caseInfoValue(messages["sample.type"])).toBe("Tissue, Tissue");
  });

  it("marks a stage the laboratory does not track as not applicable and stops offering it", async () => {
    servedCase = caseAtStage("STAINING");
    renderCaseView({ PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false" });

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(
      screen.getByText(messages["caseView.badge.notApplicable"]),
    ).toBeInTheDocument();
    expect(section("pathology-section-coverslipping").textContent).toContain(
      messages["pathology.locked.stageDisabled"],
    );
    expect(statusOptionTexts()).not.toContain(
      messages["pathology.stage.coverslipping"],
    );
    expect(statusOptionTexts()).toContain(messages["pathology.stage.staining"]);
  });

  it("offers every stage and marks none not applicable when the laboratory tracks them all", async () => {
    servedCase = caseAtStage("STAINING");
    renderCaseView({ PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "true" });

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(
      screen.queryByText(messages["caseView.badge.notApplicable"]),
    ).not.toBeInTheDocument();
    expect(statusOptionTexts()).toContain(
      messages["pathology.stage.coverslipping"],
    );
    expect(statusOptionTexts()).toContain(messages["pathology.stage.staining"]);
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(rail().queryByText(/Open requests:/)).not.toBeInTheDocument();
  });

  it("offers a discard and a save and nothing else, and only lets a discard undo real work", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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
    setStage("pathology.stage.staining");

    expect(discard).toBeEnabled();
    expect(
      screen.getByText(messages["caseView.label.unsavedChanges"]),
    ).toBeInTheDocument();

    fireEvent.click(discard);

    expect(caseFetchCount()).toBe(fetchesBefore + 1);
    expect(
      screen.queryByText(messages["caseView.label.unsavedChanges"]),
    ).not.toBeInTheDocument();
    expect(statusToggle()).toHaveTextContent(
      messages["pathology.stage.grossing"],
    );
  });

  it("posts the fields the backend has always been sent, then reads the saved case back", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    setStage("pathology.stage.staining");
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
      "grossExam",
      "immunoHistoChemistryTestIds",
      "microscopyExam",
      "referToImmunoHistoChemistry",
      "release",
      "reports",
      "requests",
      "slides",
      "status",
      "techniques",
    ]);
    expect(JSON.parse(body).status).toBe("STAINING");

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    setStage("pathology.stage.staining");

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    const statusOfRequest = document.getElementById("requeststatus0");
    expect(
      Array.from(statusOfRequest.options).map((o) => o.textContent),
    ).toEqual(requestStatusCatalogue.map((status) => status.value));
    expect(statusOfRequest.value).toBe("OPENED");
  });

  it("shows the reading sections to someone who cannot sign the case out, but lets them change nothing", async () => {
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(section("pathology-section-review").textContent).toContain(
      messages["caseView.badge.readOnly"],
    );
    expect(
      screen.getByLabelText(messages["pathology.label.microexam"]),
    ).toBeDisabled();
  });

  it("lets the pathologist write the reading on the same case", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("READY_PATHOLOGIST");
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(
      screen.getByLabelText(messages["pathology.label.microexam"]),
    ).toBeEnabled();
  });

  it("tells a case still on the bench which stage unlocks the reading, in words", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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
  it("puts the macroscopic description first in grossing, the cassettes under it and the attribution last", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [{ id: "1" }] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    const grossing = section("pathology-section-grossing");
    const order = [
      grossing.querySelector("#grossExam"),
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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
    expect(within(deactivated).queryAllByRole("button")).toHaveLength(0);
  });

  it("names each row's controls after the object the row stands for", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(addCassetteButton());

    expect(
      screen.getByLabelText(messages["pathology.label.location"] + " A1"),
    ).toBe(document.getElementById("blockLocation0"));
    expect(
      screen.getByLabelText(
        messages["pathology.label.location"] + " " + newCassette(1),
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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
    servedCase = caseAtStage("MICROTOMY", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(addSlideButton());
    fireEvent.click(addSlideButton());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    const deactivate = () =>
      screen.getByRole("button", { name: deactivateCassette("A1") });

    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );

    expect(deactivate()).toBeDisabled();
    expect(deactivate()).toHaveAttribute(
      "title",
      messages["pathology.locked.saveBeforeDeactivate"],
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(document.querySelectorAll('input[type="number"]')).toHaveLength(0);
    expect(document.getElementById("blocksToAdd")).toBeNull();
    expect(document.getElementById("slidesToAdd")).toBeNull();
    expect(addCassetteButton()).toBeInTheDocument();
  });

  it("adds a cassette the server will name, and posts it with no id at all", async () => {
    servedCase = caseAtStage("GROSSING", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(showDeactivatedSwitch("pathology-section-microtomy"));

    const slide = rowIn("pathology-section-microtomy", "1");
    expect(document.getElementById("slideLocation0")).toBeDisabled();
    expect(
      within(slide).getByText(messages["label.button.uploadfile"], {
        selector: "button",
      }),
    ).toBeDisabled();
  });

  it("names the block each slide was cut from, and says when that was never recorded", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2", { active: false })],
      slides: [
        savedSlide("5", "1", "1"),
        // A slide cut before slides named their block.
        { id: "6", designation: "7", blockId: null, active: true },
        savedSlide("8", "2", 2),
      ],
    });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    const cut = rowIn("pathology-section-microtomy", "1");
    expect(
      within(cut).getByText(messages["pathology.label.parentBlock"]),
    ).toBeInTheDocument();
    expect(within(cut).getByText("A1")).toBeInTheDocument();

    const legacy = rowIn("pathology-section-microtomy", "7");
    expect(
      within(legacy).getByText(messages["caseView.label.notRecorded"]),
    ).toBeInTheDocument();

    const underDeactivated = rowIn("pathology-section-microtomy", "2");
    expect(within(underDeactivated).getByText("A2")).toBeInTheDocument();
    expect(
      within(underDeactivated).getByText(
        messages["caseView.badge.deactivated"],
      ),
    ).toBeInTheDocument();
  });

  it("holds Add slide back until the case has a saved cassette in use, and says why", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1", { active: false })],
    });
    const { unmount } = renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    const addSlide = () =>
      buttonWithText(document.body, messages["pathology.action.addSlide"]);
    expect(addSlide()).toBeDisabled();
    expect(addSlide()).toHaveAttribute(
      "title",
      messages["pathology.locked.slideNeedsSavedBlock"],
    );

    unmount();
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1")],
    });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    expect(addSlide()).toBeEnabled();
    expect(addSlide()).not.toHaveAttribute("title");
  });

  it("lets a new slide name only a saved cassette in use, and posts the one chosen", async () => {
    servedCase = caseAtStage("MICROTOMY", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2", { active: false })],
    });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(addCassetteButton());
    fireEvent.click(addSlideButton());

    const user = userEvent.setup();
    const newSlideRow = pendingRows("pathology-section-microtomy")[0].closest(
      ".pathology-case-view__row",
    );
    await user.click(parentBlockCombobox(newSlideRow));
    const offered = within(newSlideRow).getAllByRole("option");
    expect(offered.map((option) => option.textContent)).toEqual(["A1"]);

    await user.click(offered[0]);

    const body = await postedCase();
    expect(body.slides).toEqual([{ blockId: "1", location: "" }]);
  });

  it("will not post a new slide that names no block, and marks the slide that needs one", async () => {
    servedCase = caseAtStage("MICROTOMY", { blocks: [savedBlock("1", "A1")] });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(addSlideButton());
    const newSlideRow = pendingRows("pathology-section-microtomy")[0].closest(
      ".pathology-case-view__row",
    );
    expect(
      within(newSlideRow).queryByText(
        messages["pathology.locked.slideNeedsBlock"],
      ),
    ).toBeNull();

    fireEvent.click(saveDraftButton());

    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["pathology.locked.slideNeedsBlock"],
      }),
    );
    expect(
      within(newSlideRow).getByText(
        messages["pathology.locked.slideNeedsBlock"],
      ),
    ).toBeInTheDocument();
    expect(parentBlockCombobox(newSlideRow)).toHaveAttribute(
      "aria-invalid",
      "true",
    );

    const user = userEvent.setup();
    await user.click(parentBlockCombobox(newSlideRow));
    await user.click(within(newSlideRow).getByRole("option", { name: "A1" }));
    expect(
      within(newSlideRow).queryByText(
        messages["pathology.locked.slideNeedsBlock"],
      ),
    ).toBeNull();
    expect(parentBlockCombobox(newSlideRow)).not.toHaveAttribute(
      "aria-invalid",
    );

    const body = await postedCase();
    expect(body.slides).toEqual([{ blockId: "1", location: "" }]);
  });

  it("deactivates a cassette through a dialog that names it, then reads the case back", async () => {
    servedCase = caseAtStage("GROSSING", {
      blocks: [savedBlock("1", "A1"), savedBlock("2", "A2")],
    });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

      await waitFor(() => expect(statusSelect()).not.toBeNull());
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

    await waitFor(() => expect(statusSelect()).not.toBeNull());
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

  // The server's rule text is English and names the case's internal id, so a
  // refusal without a meaning of its own reads as the ordinary save failure.
  it("never shows the server's own sentence for a refused save", async () => {
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());
    fireEvent.click(saveDraftButton());
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const callback = postToOpenElisServerFullResponse.mock.calls.at(-1)[2];
    const sentence =
      "a slide must name the block it was cut from on pathology case 9";

    await act(async () => {
      await callback({
        status: 400,
        json: async () => ({ error: sentence }),
      });
    });

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["error.save.msg"],
      }),
    );
    expect(JSON.stringify(addNotification.mock.calls)).not.toContain(sentence);
  });

  // The bar sits at the foot of a long page, where the native select the
  // screen used before opened its menu downward and the browser clipped it
  // against the window.
  it("opens the stage menu upward, away from the foot of the page", async () => {
    servedCase = caseAtStage("MICROTOMY");
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(statusSelect()).toHaveClass("cds--list-box--up");
  });
  // S-7.4: a control the case is not ready for is the pathologist's answer to
  // "why can I not release this", so it is shown and disabled rather than
  // removed. The flat form rendered it only once both assignments existed,
  // which left the screen saying nothing at all.
  it("shows the release a case is not ready for, disabled and saying why", async () => {
    hasRole.mockReturnValue(true);
    servedCase = caseAtStage("UNDER_REVIEW", { assignedPathologistId: "" });
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    expect(document.getElementById("release")).toBeEnabled();
    expect(
      screen.queryByText(messages["pathology.locked.releaseNoPathologist"]),
    ).not.toBeInTheDocument();
  });

  it("says the case carries no report rather than showing an empty list", async () => {
    servedCase = caseAtStage("COMPLETED");
    renderCaseView();

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

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

    await waitFor(() => expect(statusSelect()).not.toBeNull());

    const hint = screen.getByText(
      messages["pathology.locked.releaseNoPathologist"],
    );
    expect(hint.closest(".cds--checkbox-wrapper")).not.toBeNull();
  });
});
