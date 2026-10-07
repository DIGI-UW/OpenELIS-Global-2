/**
 * FR-2.1: the case view's stage select is where a case is moved along
 * the bench, so its options have to read in the user's language while the
 * value that reaches the server stays the raw enum name the backend stores.
 * Before this, the option text was whatever English the display list carried,
 * which no locale could translate.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PathologyCaseView from "./PathologyCaseView";
import { PATHOLOGY_STAGES, stageDisplayKey } from "./pathologyStages";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    postToOpenElisServerForPDF: vi.fn(),
    hasRole: vi.fn(() => false),
  };
});

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

const CASE_ID = "9";
const CASE_URL = "/rest/pathology/caseView/" + CASE_ID;

/**
 * The display list is served with the stage id as its own text, so the only
 * way an assertion on the catalogue wording can pass is if the label came
 * from React Intl. Had the screen kept using the server's text, or fallen
 * through to stageLabel's fallback, every option would read as a raw id.
 */
const STATUS_LIST = PATHOLOGY_STAGES.map((id) => ({ id, value: id }));

const caseAtStage = (status) => ({
  pathologySampleId: CASE_ID,
  patientPK: "1",
  labNumber: "ACC9",
  firstName: "A",
  lastName: "B",
  status,
  grossExam: "",
  microscopyExam: "",
  conclusionText: "",
  blocks: [],
  slides: [],
  reports: [],
  requests: [],
  techniques: [],
  conclusions: [],
  immunoHistoChemistryTestIds: [],
});

const renderCaseView = () =>
  render(
    <MemoryRouter initialEntries={["/PathologyCaseView/" + CASE_ID]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
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
    </MemoryRouter>,
  );

const statusSelect = async () =>
  await waitFor(() => {
    const select = document.getElementById("status");
    expect(select.options.length).toBe(PATHOLOGY_STAGES.length + 1);
    return select;
  });

const stageOptionTexts = (select) =>
  Array.from(select.options)
    .filter((option) => option.value !== "placeholder")
    .map((option) => option.textContent);

describe("PathologyCaseView stage select", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/PATHOLOGY_STATUS") {
        return callback(STATUS_LIST);
      }
      if (url === CASE_URL) {
        return callback(caseAtStage("MICROTOMY"));
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

  it("lists the eleven bench stages in order, each in the user's language", async () => {
    renderCaseView();

    const select = await statusSelect();

    expect(stageOptionTexts(select)).toEqual(
      PATHOLOGY_STAGES.map((id) => messages[stageDisplayKey(id)]),
    );
    expect(select.value).toBe("MICROTOMY");
  });

  it("never shows a stage under its raw enum name", async () => {
    renderCaseView();

    const select = await statusSelect();

    stageOptionTexts(select).forEach((text) => {
      expect(PATHOLOGY_STAGES).not.toContain(text);
    });
    expect(screen.queryByText("READY_PATHOLOGIST")).not.toBeInTheDocument();
    expect(screen.queryByText("MICROTOMY")).not.toBeInTheDocument();
  });

  it("saves the stage the backend stores, not the translated label", async () => {
    renderCaseView();

    const select = await statusSelect();
    fireEvent.change(select, { target: { value: "STAINING" } });
    // The screen repeats the same save action at the top and the bottom of a
    // long form; either one posts the case.
    fireEvent.click(
      screen.getAllByRole("button", {
        name: messages["label.button.save"],
      })[0],
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const [url, body] = postToOpenElisServerFullResponse.mock.calls.at(-1);
    expect(url).toBe(CASE_URL);
    expect(JSON.parse(body).status).toBe("STAINING");
  });
});
