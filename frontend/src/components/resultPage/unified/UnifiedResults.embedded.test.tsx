import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Embedded in another screen (the Notebook's results modal, an
 * Immunohistochemistry case), the worklist shows the one order it was given:
 * it loads that accession by itself, shows no breadcrumb, heading or toolbar,
 * and leaves the browser address alone.
 */
const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children }: any) => <button type="button">{children}</button>,
  SignatureMeaning: { AUTHORED: "AUTHORED", MODIFIED: "MODIFIED" },
}));

vi.mock("../../common/PageBreadCrumb", () => ({
  default: () => <div data-testid="breadcrumb" />,
}));

import UnifiedResults from "./UnifiedResults";

const WORKLIST = {
  testResult: [
    {
      id: "r60",
      analysisId: "60",
      testResultComponentId: "c60",
      accessionNumber: "DEV0126000000000843",
      testName: "Glucose",
      resultType: "N",
      resultValue: "",
      analysisStatusId: "4",
    },
  ],
  paging: { currentPage: "1", totalPages: "1" },
};

const worklistUrls = () =>
  utilsMock.getFromOpenElisServer.mock.calls
    .map(([url]) => url as string)
    .filter((url) => url.startsWith("/rest/LogbookResults?"));

describe("UnifiedResults embedded in another screen", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) => {
        if (url.startsWith("/rest/LogbookResults?")) {
          callback(WORKLIST);
        } else if (url.includes("analysis-status-types")) {
          callback([{ id: "4", value: "Not started" }]);
        } else {
          callback([]);
        }
      },
    );
    window.history.replaceState(null, "", "/NotebookSampleOrder/1");
  });

  it("loads the given order, hides the page shell and keeps the address", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <UnifiedResults
          accessionNumber="DEV0126000000000843"
          embedded
          includeFinished
        />
      </IntlProvider>,
    );

    await waitFor(() => expect(worklistUrls()).toHaveLength(1));
    const query = new URLSearchParams(worklistUrls()[0].split("?")[1]);
    expect(query.get("labNumber")).toBe("DEV0126000000000843");
    expect(query.get("finished")).toBe("true");

    expect(await screen.findByText("Glucose")).toBeInTheDocument();
    expect(screen.getByTestId("unified-results-embedded")).toBeInTheDocument();
    expect(screen.queryByTestId("breadcrumb")).toBeNull();
    expect(screen.queryByRole("button", { name: "Load results" })).toBeNull();
    expect(
      screen.queryByRole("button", { name: /Search by patient/ }),
    ).toBeNull();
    expect(window.location.pathname).toBe("/NotebookSampleOrder/1");
    expect(window.location.search).toBe("");
  });
});
