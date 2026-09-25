import React from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

/**
 * Clear puts the patient search back to blank: typed criteria, the gender
 * radio, the date and the results all go, without a request to the server.
 * Before it existed the only way to drop a chosen gender was to reload.
 */

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  },
}));

vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {
      UseExternalPatientInfo: "false",
      ENABLE_CLIENT_REGISTRY: "false",
    },
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("./photoManagement/photoAvatar/AyncAvatar", () => ({
  default: () => <span />,
}));

import SearchPatientForm from "./SearchPatientForm";

const RESULTS = {
  paging: { currentPage: "1", totalPages: "1" },
  patientSearchResults: [
    {
      patientID: "35",
      lastName: "PAGEPATIENT",
      firstName: "Number035",
      gender: "F",
      dob: "01/02/1980",
      nationalId: "PAGEPAT0035",
      dataSourceName: "OpenElis",
    },
  ],
};

const searchRequests = () =>
  utilsMock.getFromOpenElisServer.mock.calls.filter((c: any[]) =>
    String(c[0]).startsWith("/rest/patient-search-results"),
  );

const input = (id: string) => document.getElementById(id) as HTMLInputElement;

describe("SearchPatientForm Clear", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: any) => {
        if (endPoint.startsWith("/rest/patient-search-results")) {
          return callback(RESULTS);
        }
        return callback([]);
      },
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <SearchPatientForm getSelectedPatient={vi.fn()} />
      </IntlProvider>,
    );
  });

  it("empties every criterion, the gender and the results without asking the server", async () => {
    fireEvent.change(input("lastName"), { target: { value: "PAGEPATIENT" } });
    fireEvent.change(input("patientId"), { target: { value: "PAGEPAT0035" } });
    fireEvent.click(input("search-radio-2"));
    expect(input("search-radio-2").checked).toBe(true);

    await act(async () => {
      fireEvent.click(document.getElementById("local_search")!);
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
    expect(document.querySelectorAll("tbody tr").length).toBe(1);
    const requestsBeforeClear = searchRequests().length;
    expect(requestsBeforeClear).toBe(1);

    fireEvent.click(document.getElementById("clear_search")!);

    expect(input("lastName").value).toBe("");
    expect(input("patientId").value).toBe("");
    expect(input("search-radio-2").checked).toBe(false);
    expect(input("search-radio-1").checked).toBe(false);
    expect(document.querySelectorAll("tbody tr").length).toBe(0);
    expect(searchRequests().length).toBe(requestsBeforeClear);
  });

  it("keeps the prefixed id so each form on a page clears only itself", () => {
    cleanup();
    render(
      <IntlProvider locale="en" messages={messages}>
        <SearchPatientForm idPrefix="patient2" getSelectedPatient={vi.fn()} />
      </IntlProvider>,
    );
    expect(document.getElementById("patient2-clear_search")).not.toBeNull();
    expect(
      screen.getByRole("button", { name: messages["label.button.clear"] }),
    ).toBeInTheDocument();
  });
});
