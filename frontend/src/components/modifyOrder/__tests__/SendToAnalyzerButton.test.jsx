import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

import SendToAnalyzerButton from "../SendToAnalyzerButton";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const flush = () => new Promise((r) => setTimeout(r, 0));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

const ANALYZERS = [
  {
    id: "gx-1",
    name: "Mock GeneXpert",
    protocolVersion: "ASTM_LIS2_A2",
    communicationMode: "LIS_INITIATED",
  },
  {
    id: "mr-1",
    name: "Mock Mindray",
    protocolVersion: "HL7_V2_3_1",
    communicationMode: "BOTH",
  },
  {
    id: "old-1",
    name: "Old Push-only",
    protocolVersion: "ASTM_LIS2_A2",
    communicationMode: "ANALYZER_INITIATED",
  },
];

describe("SendToAnalyzerButton", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("button renders with the Send to analyzer label", () => {
    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-1" />);
    expect(
      screen.getByRole("button", { name: /send to analyzer/i }),
    ).toBeInTheDocument();
  });

  test("opens modal and filters out ANALYZER_INITIATED analyzers", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });
    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-1" />);

    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();

    expect(screen.getByText(/send order to analyzer/i)).toBeInTheDocument();
    expect(screen.getByText(/Mock GeneXpert/)).toBeInTheDocument();
    expect(screen.getByText(/Mock Mindray/)).toBeInTheDocument();
    expect(screen.queryByText(/Old Push-only/)).not.toBeInTheDocument();
  });

  // Regression: GET /rest/analyzer/analyzers returns { analyzers: [...] }, not a
  // bare array. The component must read the wrapped list — reading `data`
  // directly leaves the dispatch dropdown permanently empty (real bug: the
  // <select> never rendered live even though analyzers existed).
  test("reads the { analyzers: [...] } wrapped response shape", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });
    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-1" />);

    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();

    expect(screen.getByText(/Mock GeneXpert/)).toBeInTheDocument();
    expect(screen.getByText(/Mock Mindray/)).toBeInTheDocument();
  });

  test("shows 'no analyzers' message when fetch returns no dispatchable analyzers", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers")
        cb({ analyzers: ANALYZERS.slice(2) });
    });
    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-1" />);

    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();

    expect(
      screen.getByText(
        /no analyzers are configured for LIS-initiated dispatch/i,
      ),
    ).toBeInTheDocument();
  });

  test("submit POSTs send-order with ONLY the accession (analyzer-agnostic — no test codes)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) => {
      cb({ status: "DISPATCHED", protocol: "ASTM" });
    });

    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-555" />);

    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();
    await userEvent.click(screen.getByRole("button", { name: /^send$/i }));
    await flush();

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalled();
    const [endpoint, bodyJson] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(endpoint).toBe("/rest/analyzer/analyzers/gx-1/send-order");
    const body = JSON.parse(bodyJson);
    expect(body.accessionNumber).toBe("ACC-555");
    // OE2 sends NO analyzer/test codes — the bridge resolves them from LOINC.
    expect(body.testCodes).toBeUndefined();
  });

  test("DISPATCHED response surfaces success notification", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) => {
      cb({ status: "DISPATCHED", protocol: "ASTM" });
    });

    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-555" />);
    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();
    await userEvent.click(screen.getByRole("button", { name: /^send$/i }));
    await flush();

    expect(screen.getByText(/order dispatched/i)).toBeInTheDocument();
  });

  test("FAILED response surfaces error subtitle from backend", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) => {
      cb({
        status: "FAILED",
        protocol: "HL7",
        error: "Connection refused to analyzer-mock:5380",
      });
    });

    renderWithIntl(<SendToAnalyzerButton accessionNumber="ACC-555" />);
    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();
    await userEvent.click(screen.getByRole("button", { name: /^send$/i }));
    await flush();

    expect(screen.getByText(/failed to dispatch order/i)).toBeInTheDocument();
    expect(screen.getByText(/Connection refused/i)).toBeInTheDocument();
  });

  test("missing accession shows validation error without POSTing", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/analyzer/analyzers") cb({ analyzers: ANALYZERS });
    });

    renderWithIntl(<SendToAnalyzerButton accessionNumber="" />);
    await userEvent.click(
      screen.getByRole("button", { name: /send to analyzer/i }),
    );
    await flush();
    await userEvent.click(screen.getByRole("button", { name: /^send$/i }));
    await flush();

    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(
      screen.getByText(/order has no accession number/i),
    ).toBeInTheDocument();
  });
});
