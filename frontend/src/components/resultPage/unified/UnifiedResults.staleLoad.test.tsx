import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1200 review: choosing a lab unit loads its whole worklist, and a
 * lab-number search right after loads again. When the unit's slower answer
 * came last it replaced the search's table, showing every row of the unit
 * under the search and closing a row the user had opened. Only the answer to
 * the latest request is shown.
 */

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...((await importOriginal()) as object),
  ...utilsMock,
}));

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

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";
import { chooseLabUnit } from "./chooseLabUnit.test-utils";

const row = (
  analysisId: string,
  accessionNumber: string,
  testName: string,
) => ({
  id: `r-${analysisId}`,
  analysisId,
  accessionNumber,
  testName,
  resultType: "N",
  resultValue: "",
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Serum",
});

describe("OGC-1200 a late worklist answer", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    window.localStorage.clear();
    window.history.replaceState(null, "", "/Results");
  });

  it("never replaces the table of a later search", () => {
    const pending: Array<{ url: string; answer: (body: unknown) => void }> = [];
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: any) => {
        if (endPoint === "/rest/results-entry/lab-units")
          return callback([
            { id: "36", value: "Biochemistry", domain: "CLINICAL" },
          ]);
        if (endPoint.startsWith("/rest/LogbookResults"))
          return pending.push({ url: endPoint, answer: callback });
        if (endPoint.startsWith("/rest/result-runs"))
          return callback({ open: 0, held: 0, runs: [] });
        return callback([]);
      },
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <UnifiedResults />
      </IntlProvider>,
    );

    chooseLabUnit("Biochemistry");
    const search = screen.getByRole("searchbox");
    fireEvent.change(search, { target: { value: "DEV0126000000000500" } });
    fireEvent.keyDown(search, { key: "Enter" });
    expect(pending).toHaveLength(2);
    expect(pending[1].url).toContain("labNumber=DEV0126000000000500");

    pending[1].answer({
      testResult: [row("61", "DEV0126000000000500", "Glucose")],
    });
    pending[0].answer({
      testResult: [
        row("61", "DEV0126000000000500", "Glucose"),
        row("62", "DEV0126000000000501", "Urea"),
      ],
    });

    expect(screen.getByText("Glucose")).toBeInTheDocument();
    expect(screen.queryByText("Urea")).not.toBeInTheDocument();
  });
});
