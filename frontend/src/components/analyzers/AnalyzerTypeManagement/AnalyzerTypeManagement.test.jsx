import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import AnalyzerTypeManagement from "./AnalyzerTypeManagement";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const renderWithIntl = (component, initialRoute = "/analyzers/types") => {
  const history = createMemoryHistory({ initialEntries: [initialRoute] });
  const result = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </Router>,
  );
  return { ...result, history };
};

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
    const { history } = renderWithIntl(<AnalyzerTypeManagement />);

    expect(await screen.findByText("QuantStudio QS5/QS7")).toBeInTheDocument();
    const row = screen.getByTestId("profile-row-file-quantstudio");
    expect(within(row).getAllByText("FILE")).toHaveLength(2);
    expect(within(row).getByText("Molecular")).toBeInTheDocument();
    expect(within(row).getByText("Ready")).toBeInTheDocument();
    expect(
      screen.getByTestId("profile-test-mapping-count-file-quantstudio"),
    ).toHaveTextContent("17");
    expect(
      screen.getByTestId("profile-qc-rule-count-file-quantstudio"),
    ).toHaveTextContent("8");

    await userEvent.click(screen.getByTestId("profile-setup-file-quantstudio"));

    expect(`${history.location.pathname}${history.location.search}`).toBe(
      "/analyzers?add=1&step=instrument&profile=file%2Fquantstudio&returnTo=%2Fanalyzers%2Ftypes",
    );
  });

  test("restores search and filters from a bookmarkable URL", async () => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      callback([
        {
          id: "astm/genexpert",
          displayName: "GeneXpert",
          protocol: "ASTM",
          readinessStatus: "READY",
        },
        {
          id: "file/quantstudio",
          displayName: "QuantStudio",
          protocol: "FILE",
          readinessStatus: "READY",
        },
      ]);
    });

    renderWithIntl(
      <AnalyzerTypeManagement />,
      "/analyzers/types?search=gene&protocol=ASTM&readiness=READY",
    );

    expect(await screen.findByText("GeneXpert")).toBeInTheDocument();
    expect(screen.queryByText("QuantStudio")).not.toBeInTheDocument();
    expect(screen.getByRole("searchbox")).toHaveValue("gene");
  });

  test("supports the DRAFT readiness state defined by the profile schema", async () => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      callback([
        {
          id: "astm/draft",
          displayName: "Draft ASTM",
          protocol: "ASTM",
          readinessStatus: "DRAFT",
        },
        {
          id: "astm/ready",
          displayName: "Ready ASTM",
          protocol: "ASTM",
          readinessStatus: "READY",
        },
      ]);
    });

    renderWithIntl(
      <AnalyzerTypeManagement />,
      "/analyzers/types?readiness=DRAFT",
    );

    expect(await screen.findByText("Draft ASTM")).toBeInTheDocument();
    expect(screen.queryByText("Ready ASTM")).not.toBeInTheDocument();
    expect(
      within(screen.getByTestId("profile-row-astm-draft")).getByText("Draft"),
    ).toBeInTheDocument();
  });
});
