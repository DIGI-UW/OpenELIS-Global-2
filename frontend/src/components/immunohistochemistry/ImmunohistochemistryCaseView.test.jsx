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
});
