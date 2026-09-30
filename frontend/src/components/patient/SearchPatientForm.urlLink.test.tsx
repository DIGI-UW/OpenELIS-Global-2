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
 * A page URL carrying a lab number or a patient id opens the search on that
 * patient. The order entry page also carries the lab number of the order it
 * opens, and loads that order's patient itself, so it turns the lab number
 * link off: following it re-selected the patient and marked a reopened order
 * as changed (OGC-1192).
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

const requests = (prefix: string) =>
  utilsMock.getFromOpenElisServer.mock.calls.filter((c: any[]) =>
    String(c[0]).startsWith(prefix),
  );

const renderAt = (search: string, props: Record<string, unknown> = {}) => {
  window.history.pushState({}, "", "/order/clinical/enter" + search);
  return render(
    <IntlProvider locale="en" messages={messages}>
      <SearchPatientForm getSelectedPatient={vi.fn()} {...props} />
    </IntlProvider>,
  );
};

describe("SearchPatientForm links from the page URL", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
  });

  afterEach(() => {
    window.history.pushState({}, "", "/");
  });

  it("searches for the lab number in the URL", () => {
    renderAt("?labNumber=DEV01260000000000316");

    const [search] = requests("/rest/patient-search-results");
    expect(search[0]).toContain("labNumber=DEV01260000000000316");
  });

  it("leaves the lab number alone when the page loads that order itself", () => {
    renderAt("?labNumber=DEV01260000000000316", { followUrlLabNumber: false });

    expect(requests("/rest/patient-search-results")).toHaveLength(0);
  });

  it("still opens the patient named by a patient id", () => {
    renderAt("?patientId=35", { followUrlLabNumber: false });

    expect(requests("/rest/patient-details?patientID=35")).toHaveLength(1);
  });
});
