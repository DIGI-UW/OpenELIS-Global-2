import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import ImmunohistochemistryCaseView from "./ImmunohistochemistryCaseView";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    postToOpenElisServerForPDF: vi.fn(),
    hasRole: vi.fn(() => true),
  };
});

// Both fetch and render a world of their own that the referral display does
// not depend on.
vi.mock("../resultPage/unified/UnifiedResults", () => ({
  default: () => null,
}));
vi.mock("../common/QuestionnaireResponse", () => ({ default: () => null }));

import { getFromOpenElisServer } from "../utils/Utils";

const CASE_ID = "4";
const CASE_URL = "/rest/immunohistochemistry/caseView/" + CASE_ID;

const renderCaseView = () =>
  render(
    <MemoryRouter initialEntries={["/ImmunohistochemistryCaseView/" + CASE_ID]}>
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
                firstName: "Sam",
                lastName: "Session",
                roles: [],
              },
            }}
          >
            <Route path="/ImmunohistochemistryCaseView/:immunohistochemistrySampleId">
              <ImmunohistochemistryCaseView />
            </Route>
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("ImmunohistochemistryCaseView rollout", () => {
  test("exports a component for OGC-284 rollout", () => {
    expect(ImmunohistochemistryCaseView).toBeTruthy();
  });
});

describe("the pathology reading a referred case carries", () => {
  // A macroscopic description runs to paragraphs. Shown as a tag, it was cut
  // to one line with an ellipsis and the pathologist could not read the rest.
  it("shows the whole macroscopic description as text, not as a tag, and labels it as the macroscopic description", async () => {
    const description = "Grey-white firm nodule, cut surface whorled. "
      .repeat(8)
      .slice(0, 300)
      .trim();
    expect(description).toHaveLength(300);
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          // Spelt as the case DTO spells it.
          reffered: true,
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          grossExam: description,
          microscopyExam: "",
          blocks: [],
          slides: [],
          reports: [],
          requests: [],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();

    const text = await waitFor(() => screen.getByText(description));
    expect(text.closest(".cds--tag")).toBeNull();
    expect(
      screen.getByText(messages["pathology.label.grossExam"], { exact: false }),
    ).toBeInTheDocument();
  });

  // A slide's level and stain are what the pathologist reads it by; the
  // referred list showed only its name and location.
  it("shows each referred slide's level and the name of its stain", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          reffered: true,
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          grossExam: "",
          microscopyExam: "",
          blocks: [
            { id: 1, designation: "A1", cassetteState: "BLOCK", active: true },
          ],
          slides: [
            {
              id: 5,
              designation: "1",
              blockId: 1,
              level: "2",
              stainId: "31",
              active: true,
              location: "Tray 4",
            },
          ],
          stainNames: { 31: "PAS" },
          reports: [],
          requests: [],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();

    const level = await waitFor(() =>
      screen.getByLabelText(messages["pathology.label.level"]),
    );
    expect(level).toHaveValue("2");
    expect(level).toBeDisabled();
    expect(
      screen.getByLabelText(messages["pathology.label.stain"]),
    ).toHaveValue("PAS");
    expect(screen.getByDisplayValue("A1.1")).toBeInTheDocument();
  });
  // Whether each referred slide's label was checked against its block at
  // microtomy, read only here.
  it("tags each referred slide Verified or Unverified, naming who checked it, and tags none with no block on record", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          reffered: true,
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          grossExam: "",
          microscopyExam: "",
          blocks: [
            { id: 1, designation: "A1", cassetteState: "BLOCK", active: true },
          ],
          slides: [
            { id: 5, designation: "1", blockId: 1, active: true },
            { id: 6, designation: "2", blockId: 1, active: true },
            { id: 7, designation: "9", blockId: null, active: true },
          ],
          labelVerifications: {
            5: { verifiedAt: "09/11/2026 10:05", verifiedBy: "Tech, Tina" },
          },
          reports: [],
          requests: [],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();

    const slideCell = (name) =>
      screen.getByDisplayValue(name).closest(".cds--css-grid-column");
    await waitFor(() => screen.getByDisplayValue("A1.1"));

    expect(slideCell("A1.1")).toHaveTextContent(
      messages["pathology.badge.verified"],
    );
    expect(
      slideCell("A1.1").querySelector(
        ".pathology-case-view__label-verification",
      ),
    ).toHaveAttribute("title", "Verified by Tech, Tina on 09/11/2026 10:05");
    expect(slideCell("A1.2")).toHaveTextContent(
      messages["pathology.badge.unverified"],
    );
    expect(slideCell("9").querySelector(".cds--tag")).toBeNull();
  });

  // How far each referred slide got at staining and coverslipping, read
  // only here, and nothing for a slide with neither recorded.
  it("tags each referred slide with its stain status and a named coverslip QC, and tags neither where none was recorded", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          reffered: true,
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          grossExam: "",
          microscopyExam: "",
          blocks: [
            { id: 1, designation: "A1", cassetteState: "BLOCK", active: true },
          ],
          slides: [
            {
              id: 5,
              designation: "1",
              blockId: 1,
              active: true,
              stainStatus: "STAINED",
              coverslipQc: "PASS",
            },
            {
              id: 6,
              designation: "2",
              blockId: 1,
              active: true,
              stainStatus: "UNSATISFACTORY",
            },
            { id: 7, designation: "9", blockId: null, active: true },
          ],
          labelVerifications: {},
          reports: [],
          requests: [],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();

    const slideCell = (name) =>
      screen.getByDisplayValue(name).closest(".cds--css-grid-column");
    await waitFor(() => screen.getByDisplayValue("A1.1"));

    expect(slideCell("A1.1")).toHaveTextContent(
      messages["pathology.badge.stained"],
    );
    // Away from its column a QC result says what passed.
    expect(slideCell("A1.1")).toHaveTextContent(
      messages["pathology.badge.coverslipQcResult"].replace(
        "{result}",
        messages["pathology.badge.qcPassed"],
      ),
    );
    expect(slideCell("A1.2")).toHaveTextContent(
      messages["pathology.badge.unsatisfactory"],
    );
    expect(slideCell("A1.2")).not.toHaveTextContent(
      messages["pathology.badge.qcAwaiting"],
    );
    expect(slideCell("9").querySelector(".cds--tag")).toBeNull();
  });

  // The referred case serves each request that is not cancelled as its id
  // and instruction. Two requests may ask for the same thing in the same
  // words, so a tag is keyed on the request, not on what it says.
  it("tags the instruction of every request the referred case serves, keyed on the request", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          reffered: true,
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          grossExam: "",
          microscopyExam: "",
          blocks: [],
          slides: [],
          reports: [],
          requests: [
            { id: 21, value: "Two deeper levels" },
            { id: 22, value: "Two deeper levels" },
            { id: 23, value: "PAS on A1.1" },
          ],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();

    await waitFor(() => screen.getByText("PAS on A1.1"));
    expect(screen.getAllByText("Two deeper levels")).toHaveLength(2);
    screen
      .getAllByText(/Two deeper levels|PAS on A1\.1/)
      .forEach((label) => expect(label.closest(".cds--tag")).not.toBeNull());
    expect(
      consoleError.mock.calls.filter((args) => /\bkey\b/.test(args.join(" "))),
    ).toEqual([]);
    consoleError.mockRestore();
  });
});

describe("the prior anatomic-pathology results under the patient band", () => {
  it("renders the prior results panel directly after the patient header and asks this case's endpoint", async () => {
    // The tests above leave their calls on the shared mock.
    getFromOpenElisServer.mockClear();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "ACC4",
          patientPK: "1",
          lastName: "B",
          firstName: "A",
          status: "IN_PROGRESS",
          assignedTechnicianId: "1",
          assignedPathologistId: "1",
          blocks: [],
          slides: [],
          reports: [],
          requests: [],
          techniques: [],
          conclusions: [],
        });
      }
      if (url.startsWith("/rest/paginatedDisplayList/")) {
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      }
      return callback([]);
    });

    renderCaseView();
    await waitFor(() =>
      expect(document.querySelector(".patient-header2")).not.toBeNull(),
    );

    const panel = screen.getByTestId("prior-results").closest(".prior-results");
    expect(panel.previousElementSibling).toContainElement(
      document.querySelector(".patient-header2"),
    );
    expect(
      getFromOpenElisServer.mock.calls.filter(
        ([url]) => url === CASE_URL + "/priorResults",
      ),
    ).toHaveLength(1);
  });
});
