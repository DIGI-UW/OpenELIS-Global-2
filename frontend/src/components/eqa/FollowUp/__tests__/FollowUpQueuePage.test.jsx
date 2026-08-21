import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import FollowUpQueuePage, { queueCsv } from "../FollowUpQueuePage";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const QUEUE = [
  {
    id: 7,
    schemeId: 1,
    schemeName: "National HIV VL PT",
    schemeType: "INTERNATIONAL_PT",
    cycleId: 3,
    cycleNumber: 2,
    cycleName: "2026 Round 2",
    source: "External provider",
    followupStatus: "NOTIFIED",
    notifiedAt: "2026-08-01 09:15:00.0",
    results: [
      {
        participantResultId: 41,
        analyteId: 900,
        analyteName: "HIV-1 viral load",
        reported: "4.9",
        target: "4.2",
        zScore: 2.4,
        performanceStatus: "QUESTIONABLE",
      },
    ],
  },
  {
    id: 8,
    schemeId: 2,
    schemeName: "In-house blinded panel",
    schemeType: "IN_HOUSE",
    cycleId: 4,
    cycleNumber: 1,
    cycleName: "Blind run 1",
    source: "In-house",
    followupStatus: "NOTIFIED",
    notifiedAt: "2026-08-10 09:15:00.0",
    results: [
      {
        participantResultId: 55,
        analyteId: 901,
        analyteName: "Malaria RDT",
        reported: "Negative",
        target: "Positive",
        zScore: null,
        performanceStatus: "UNACCEPTABLE",
      },
      {
        participantResultId: 56,
        analyteId: 902,
        analyteName: "Haemoglobin",
        reported: "8",
        target: "12",
        zScore: -1.2,
        performanceStatus: "QUESTIONABLE",
      },
    ],
  },
];

const renderPage = ({ permissions = ["qa.view.eqa", "qa.manage.eqa"] } = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles: [], permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        <MemoryRouter>
          <FollowUpQueuePage />
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const jsonResponse = (ok, body) => ({ ok, json: () => Promise.resolve(body) });

const expand = (rowId) =>
  fireEvent.click(screen.getAllByRole("button", { name: /triage/i })[rowId]);

describe("FollowUpQueuePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(QUEUE),
    );
  });

  it("renders one row per register entry with its source and worst z-score", async () => {
    renderPage();

    expect(await screen.findByText("2026 Round 2")).toBeInTheDocument();
    expect(screen.getByText("HIV-1 viral load")).toBeInTheDocument();
    expect(screen.getByText("2.4")).toBeInTheDocument();
    expect(screen.getByText("External provider")).toBeInTheDocument();
    // Two analytes on one cycle stay one register row — triage acts on the row.
    expect(screen.getByText("Malaria RDT +1 more")).toBeInTheDocument();
    expect(screen.getByText("In-house")).toBeInTheDocument();
  });

  it("tags the triage band from the scheme, not from the verdict", async () => {
    renderPage();

    await screen.findByText("2026 Round 2");
    // The in-house row is an in-house failure. The external row is tagged
    // Questionable even though its worst verdict is UNACCEPTABLE: an external
    // unacceptable outside the questionable band would have auto-raised an NCE
    // instead of queueing. The other "Questionable" on the page is a KPI label,
    // so the tag count is what distinguishes them.
    expect(screen.getByText("In-house fail")).toBeInTheDocument();
    expect(
      screen
        .getAllByText("Questionable")
        .filter((node) => node.closest(".cds--tag")),
    ).toHaveLength(1);
  });

  it("counts the KPI tiles off the queue", async () => {
    renderPage();

    await screen.findByText("2026 Round 2");
    expect(screen.getByTestId("kpi-queued")).toHaveTextContent("2");
    expect(screen.getByTestId("kpi-questionable")).toHaveTextContent("1");
    expect(screen.getByTestId("kpi-inhouse")).toHaveTextContent("1");
  });

  it("escalates a row and reports the NCE the server raised", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { nceId: 12, nceNumber: "NCE-2026-00045" })),
    );
    renderPage();

    await screen.findByText("2026 Round 2");
    expand(0);
    fireEvent.click(screen.getByRole("button", { name: /escalate to nce/i }));

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/eqa/followups/7/escalate",
      "{}",
      expect.any(Function),
    );
    expect(
      await screen.findByText(/NCE-2026-00045 raised for this cycle/i),
    ).toBeInTheDocument();
  });

  it("surfaces the server's refusal instead of reporting success", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(false, { error: "This follow-up is already ESCALATED" })),
    );
    renderPage();

    await screen.findByText("2026 Round 2");
    expand(0);
    fireEvent.click(screen.getByRole("button", { name: /escalate to nce/i }));

    expect(await screen.findByText(/already ESCALATED/i)).toBeInTheDocument();
    expect(
      screen.queryByText(/raised for this cycle/i),
    ).not.toBeInTheDocument();
  });

  it("dismisses with the chosen category and notes", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { followupStatus: "RESOLVED" })),
    );
    renderPage();

    await screen.findByText("2026 Round 2");
    expand(0);
    fireEvent.click(
      screen.getByRole("button", { name: /dismiss with reason/i }),
    );
    fireEvent.change(screen.getByLabelText(/category/i), {
      target: { value: "KNOWN_EQUIPMENT_ISSUE" },
    });
    fireEvent.change(screen.getByLabelText(/notes/i), {
      target: { value: "Analyser flagged the same day" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^dismiss$/i }));

    await vi.waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/followups/7/dismiss",
        JSON.stringify({
          category: "KNOWN_EQUIPMENT_ISSUE",
          notes: "Analyser flagged the same day",
        }),
        expect.any(Function),
      ),
    );
  });

  it("hides both triage actions from a viewer without qa.manage.eqa", async () => {
    renderPage({ permissions: ["qa.view.eqa"] });

    await screen.findByText("2026 Round 2");
    expand(0);
    expect(
      screen.queryByRole("button", { name: /escalate to nce/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /dismiss with reason/i }),
    ).not.toBeInTheDocument();
    // The read-only affordance stays, or the expansion would be empty.
    expect(
      screen.getByRole("link", { name: /view cycle/i }),
    ).toBeInTheDocument();
  });

  it("exports one CSV line per result, quoting what the analyte name contains", () => {
    const csv = queueCsv(
      [
        {
          ...QUEUE[1],
          sourceKey: "in_house",
          reason: "inHouseFailure",
          results: [
            { ...QUEUE[1].results[0], analyteName: 'RDT "rapid", visual' },
            QUEUE[1].results[1],
          ],
        },
      ],
      (key) => key,
      (reason) => reason,
    );

    const lines = csv.split("\n");
    expect(lines).toHaveLength(3);
    expect(lines[1]).toContain('"RDT ""rapid"", visual"');
    expect(lines[2]).toContain('"Haemoglobin"');
    // A null z-score exports as empty, not as the string "null".
    expect(lines[1]).toContain('"Negative","Positive","",');
  });
});
