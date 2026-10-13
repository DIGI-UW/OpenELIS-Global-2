import React from "react";
import { cleanup, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

/**
 * Order entry asks the shared patient search for exact-and-prefix name
 * matching (OGC-1443, FR-B6a); other pages keep the default matching. The
 * results table announces each sortable column by its name.
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
      lastName: "Smith",
      firstName: "John",
      gender: "M",
      dob: "01/02/1980",
      nationalId: "NAT-35",
      dataSourceName: "OpenElis",
    },
  ],
};

const searchRequests = () =>
  utilsMock.getFromOpenElisServer.mock.calls
    .map((call: unknown[]) => String(call[0]))
    .filter((url: string) => url.startsWith("/rest/patient-search-results"));

const renderSearch = (props: Record<string, unknown> = {}) => {
  window.history.pushState({}, "", "/patient?labNumber=DEV01260000000001420");
  return render(
    <IntlProvider locale="en" messages={messages}>
      <SearchPatientForm getSelectedPatient={vi.fn()} {...props} />
    </IntlProvider>,
  );
};

describe("SearchPatientForm name matching and result headers", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (value: unknown) => void) => {
        if (url.startsWith("/rest/patient-search-results")) {
          callback(RESULTS);
        }
      },
    );
  });

  afterEach(() => {
    window.history.pushState({}, "", "/");
  });

  it("asks for prefix name matching when the page wants it", () => {
    renderSearch({ nameMatch: "prefix" });

    expect(searchRequests()[0]).toContain("&nameMatch=prefix");
  });

  it("keeps the default matching everywhere else", () => {
    renderSearch();

    expect(searchRequests()[0]).not.toContain("nameMatch");
  });

  it("announces each sortable column by its name", async () => {
    renderSearch();

    expect(await screen.findByText("Smith")).toBeInTheDocument();
    expect(document.body.textContent).not.toContain("[object Object]");
    expect(
      screen.getAllByRole("button", { name: /Last Name/ }).length,
    ).toBeGreaterThan(0);
  });
});
