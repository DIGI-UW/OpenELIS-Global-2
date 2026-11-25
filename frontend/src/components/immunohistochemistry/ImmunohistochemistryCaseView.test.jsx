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
});
