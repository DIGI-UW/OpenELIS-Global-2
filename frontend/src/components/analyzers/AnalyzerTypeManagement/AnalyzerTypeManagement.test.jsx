import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import AnalyzerTypeManagement from "./AnalyzerTypeManagement";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const mockHistory = {
  push: vi.fn(),
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
  };
});

const renderWithIntl = (component) =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );

describe("AnalyzerTypeManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/analyzer/profiles") {
        callback([
          {
            id: "file/quantstudio",
            profileId: "quantstudio",
            displayName: "QuantStudio QS5/QS7",
            protocol: "FILE",
            category: "MOLECULAR",
            supportedConnectionMode: "FILE",
            testMappingCount: 17,
            qcRuleCount: 8,
            resultValueMappingCount: 0,
            readinessStatus: "READY",
          },
        ]);
      } else {
        callback([]);
      }
    });
  });

  test("renders lab-facing profile summaries and routes setup from a profile", async () => {
    renderWithIntl(<AnalyzerTypeManagement />);

    expect(await screen.findByText("QuantStudio QS5/QS7")).toBeInTheDocument();
    const row = screen.getByTestId("profile-row-file-quantstudio");
    expect(within(row).getAllByText("FILE")).toHaveLength(2);
    expect(within(row).getByText("MOLECULAR")).toBeInTheDocument();
    expect(within(row).getByText("READY")).toBeInTheDocument();
    expect(
      screen.getByTestId("profile-test-mapping-count-file-quantstudio"),
    ).toHaveTextContent("17");
    expect(
      screen.getByTestId("profile-qc-rule-count-file-quantstudio"),
    ).toHaveTextContent("8");

    await userEvent.click(screen.getByTestId("profile-setup-file-quantstudio"));

    expect(mockHistory.push).toHaveBeenCalledWith(
      "/analyzers?add=1&profile=file%2Fquantstudio",
    );
  });
});
