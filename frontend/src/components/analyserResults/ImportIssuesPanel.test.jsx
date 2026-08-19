import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";
import ImportIssuesPanel from "./ImportIssuesPanel";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const renderPanel = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ImportIssuesPanel />
    </IntlProvider>,
  );

describe("ImportIssuesPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) => {
      callback({
        status: "success",
        data: {
          eventRows: [
            {
              id: "91",
              externalEventId: "evt-unmatched-1",
              eventType: "AST_RESULT_AVAILABLE",
              analyzerId: "77",
              sourceId: "card-404",
              targetReference: "missing-accession",
              failureReason: "AST_ANALYZER_RUN_NOT_MATCHED",
              receivedAt: "2026-08-19T16:30:00Z",
            },
          ],
          rows: [],
        },
      });
    });
  });

  it("shows unmatched analyzer events with failure context and a mapping path", async () => {
    renderPanel();

    expect(
      await screen.findByRole("heading", { name: "Analyzer import issues" }),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/import-issues",
      expect.any(Function),
    );
    expect(screen.getByText("AST result available")).toBeInTheDocument();
    expect(screen.getByText("card-404")).toBeInTheDocument();
    expect(screen.getByText("missing-accession")).toBeInTheDocument();
    expect(
      screen.getByText("No AST run matched the analyzer and card identifiers."),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open mappings" })).toHaveAttribute(
      "href",
      "/analyzers/77/mappings",
    );
    expect(
      screen.queryByRole("button", { name: /retry|reprocess/i }),
    ).not.toBeInTheDocument();
  });
});
