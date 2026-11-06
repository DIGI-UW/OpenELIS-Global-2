import React from "react";
import { vi, it, expect, afterEach } from "vitest";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import PathologyCaseView from "../../pathology/PathologyCaseView";
import CytologyCaseView from "../../cytology/CytologyCaseView";
import ImmunohistochemistryCaseView from "../../immunohistochemistry/ImmunohistochemistryCaseView";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  hasRole: vi.fn(() => true),
}));
vi.mock("../../resultPage/unified/UnifiedResults", () => ({
  default: () => null,
}));
vi.mock("../QuestionnaireResponse", () => ({ default: () => null }));
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
it.each([
  ["pathology", PathologyCaseView],
  ["cytology", CytologyCaseView],
  ["immunohistochemistry", ImmunohistochemistryCaseView],
])(
  "%s explains a rejected attachment and keeps the current case open",
  async (area, Component) => {
    const notify = vi.fn();
    const caseData = {
      pathologySampleId: "9",
      labNumber: "SYNTHETIC-9",
      patientPK: "1",
      status: area === "pathology" ? "MICROTOMY" : "IN_PROGRESS",
      grossExam: "Unsaved clinical description",
      microscopyExam: "",
      conclusionText: "",
      blocks: [],
      slides: [],
      reports: [],
      requests: [],
      techniques: [],
      conclusions: [],
      immunoHistoChemistryTestIds: [],
      diagnosis: { diagnosisResultsMaps: [] },
    };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === `/rest/${area}/caseView/9`) return callback(caseData);
      if (url === "/rest/displayList/PATHOLOGY_STATUS")
        return callback([
          { id: "MICROTOMY", value: "MICROTOMY" },
          { id: "STAINING", value: "STAINING" },
        ]);
      if (url.startsWith("/rest/paginatedDisplayList/"))
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      callback([]);
    });
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({
        status: 400,
        json: async () => ({ messageKey: "clinicalAttachment.uploadHint" }),
      }),
    );
    renderCase(area, Component, notify);
    if (area === "pathology") {
      fireEvent.click(
        await screen.findByRole("combobox", {
          name: messages["common.status"],
        }),
      );
      fireEvent.click(
        within(document.getElementById("status")).getByRole("option", {
          name: messages["pathology.stage.staining"],
        }),
      );
      fireEvent.click(
        screen.getByRole("button", {
          name: messages["caseView.action.saveDraft"],
        }),
      );
    } else {
      await waitFor(() =>
        expect(document.getElementById("pathology_save")).toBeEnabled(),
      );
      fireEvent.click(document.getElementById("pathology_save"));
    }
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        expect.objectContaining({
          message: messages["clinicalAttachment.uploadHint"],
        }),
      ),
    );
    expect(
      getFromOpenElisServer.mock.calls.filter(
        ([url]) => url === `/rest/${area}/caseView/9`,
      ),
    ).toHaveLength(1);
    expect(notify).not.toHaveBeenCalledWith(
      expect.objectContaining({
        message: messages["pathology.error.reloadCase"],
      }),
    );
  },
);

function renderCase(area, Component, notify) {
  return render(
    <MemoryRouter initialEntries={["/case/9"]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: notify,
          }}
        >
          <UserSessionDetailsContext.Provider
            value={{
              userSessionDetails: {
                userId: "1",
                firstName: "Synthetic",
                lastName: "User",
                roles: [],
              },
            }}
          >
            <Route path={`/case/:${area}SampleId`}>
              <Component />
            </Route>
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );
}

it.each([
  ["cytology", CytologyCaseView],
  ["immunohistochemistry", ImmunohistochemistryCaseView],
])(
  "%s uses persisted attachment IDs on successive saves",
  async (area, Component) => {
    const report = {
      id: 41,
      fileType: "data:application/msword",
      image: "aGlzdG9yaWNhbA==",
      fileName: "historical.doc",
    };
    const caseData = {
      status: "IN_PROGRESS",
      reports: [report],
      slides: [],
      blocks: [],
      requests: [],
      techniques: [],
      conclusions: [],
      immunoHistoChemistryTestIds: [],
      diagnosis: { diagnosisResultsMaps: [] },
    };
    let persistedId = report.id;
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === `/rest/${area}/caseView/9`)
        return callback({
          ...caseData,
          reports: [{ ...report, id: persistedId }],
        });
      if (url.endsWith("_STATUS"))
        return callback([
          { id: "IN_PROGRESS", value: "In progress" },
          { id: "COMPLETED", value: "Completed" },
        ]);
      if (url.startsWith("/rest/paginatedDisplayList/"))
        return callback({
          displayItems: [],
          paging: { totalPages: 1, currentPage: 1 },
        });
      callback([]);
    });
    const submitted = [];
    postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => {
        submitted.push(JSON.parse(body));
        persistedId += 1;
        callback({ status: 200, json: async () => ({}) });
      },
    );
    renderCase(area, Component, vi.fn());
    await waitFor(() =>
      expect(document.getElementById("pathology_save")).toBeEnabled(),
    );
    fireEvent.click(document.getElementById("pathology_save"));
    await waitFor(() =>
      expect(
        getFromOpenElisServer.mock.calls.filter(
          ([url]) => url === `/rest/${area}/caseView/9`,
        ),
      ).toHaveLength(2),
    );
    fireEvent.change(document.getElementById("status"), {
      target: { value: "COMPLETED" },
    });
    await waitFor(() =>
      expect(document.getElementById("pathology_save")).toBeEnabled(),
    );
    fireEvent.click(document.getElementById("pathology_save"));
    await waitFor(() => expect(submitted).toHaveLength(2));
    expect(submitted[0].reports).toEqual([report]);
    expect(submitted[1].reports).toEqual([{ ...report, id: 42 }]);
    expect(submitted[1].status).toBe("COMPLETED");
  },
);
