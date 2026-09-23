/**
 * PanelTestsSection — OGC-224 C3 (FRS v2.2, the centerpiece).
 *
 * - members load ordered from /panels/{id}/test-order with name + code;
 * - the add typeahead is DOMAIN-GUARDED (fetch carries the panel's domain)
 *   and already-member tests are not offered;
 * - picking a result appends it to the end; up/down reorder; remove;
 * - Save PUTs 1-based positions (+ autoActivate only on the create flow);
 * - empty state when the panel has no tests.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PanelTestsSection from "./PanelTestsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import messages from "../../../../languages/en.json";

const PANEL = { id: "7", name: "CBC", domain: "CLINICAL", active: true };
const MEMBERS = [
  { testId: "10", testName: "WBC Count", code: "WBC", position: 1 },
  { testId: "11", testName: "RBC Count", code: "RBC", position: 2 },
];
const CANDIDATES = {
  rows: [
    { testId: "12", name: "Hemoglobin", code: "HGB", domain: "CLINICAL" },
    { testId: "10", name: "WBC Count", code: "WBC", domain: "CLINICAL" },
  ],
};

const notification = {
  addNotification: vi.fn(),
  setNotificationVisible: vi.fn(),
};

const SAMPLE_TYPES = {
  success: true,
  data: [
    { id: 2, name: "Serum" },
    { id: 4, name: "Whole Blood" },
  ],
};
// tests filtered to sample type 2 — only Hemoglobin is on Serum
const SERUM_ONLY = {
  rows: [{ testId: "12", name: "Hemoglobin", code: "HGB", domain: "CLINICAL" }],
};

const mockServer = (members = MEMBERS) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/test-order")) {
      cb({ panelId: "7", tests: members });
    } else if (url === "/rest/sample-types") {
      cb(SAMPLE_TYPES);
    } else if (url.startsWith("/rest/test-catalog/tests?")) {
      cb(url.includes("sampleType=2") ? SERUM_ONLY : CANDIDATES);
    } else {
      cb(undefined);
    }
  });
};

const wrap = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notification}>
        <PanelTestsSection panel={PANEL} onSaved={() => {}} {...props} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("PanelTestsSection", () => {
  it("loads ordered members with name and code", async () => {
    mockServer();
    wrap();
    expect(await screen.findByText("WBC Count")).toBeInTheDocument();
    expect(screen.getByText("RBC")).toBeInTheDocument();
    expect(screen.getByTestId("panel-tests-count")).toHaveTextContent("2");
  });

  it("the typeahead fetch is domain-guarded and excludes existing members", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");
    const pickerCall = getFromOpenElisServer.mock.calls.find(([url]) =>
      url.startsWith("/rest/test-catalog/tests?"),
    );
    expect(pickerCall[0]).toContain("domain=CLINICAL");
    expect(pickerCall[0]).toContain("status=active");
    // open the combobox: candidate 12 offered, member 10 not re-offered
    fireEvent.click(document.getElementById("panel-add-test"));
    expect(screen.getByText(/Hemoglobin — HGB/)).toBeInTheDocument();
    expect(screen.queryByText(/WBC Count — WBC/)).not.toBeInTheDocument();
  });

  it("picking a candidate appends it at the end", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");
    fireEvent.click(document.getElementById("panel-add-test"));
    fireEvent.click(screen.getByText(/Hemoglobin — HGB/));
    const rows = screen.getAllByRole("row").map((row) => row.textContent || "");
    expect(rows[rows.length - 1]).toContain("Hemoglobin");
    expect(rows[rows.length - 1]).toContain("3");
  });

  it("reorder and remove operate on the list", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");
    fireEvent.click(screen.getByTestId("panel-test-down-10"));
    let rows = screen.getAllByRole("row").map((r) => r.textContent || "");
    expect(rows[1]).toContain("RBC Count");
    fireEvent.click(screen.getByTestId("panel-test-remove-11"));
    expect(screen.queryByText("RBC Count")).not.toBeInTheDocument();
  });

  it("Save PUTs 1-based positions with autoActivate only for the create flow", async () => {
    mockServer();
    wrap({ autoActivate: true });
    await screen.findByText("WBC Count");
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/test-catalog/panels/7/tests",
      JSON.stringify({
        tests: [
          { testId: "10", position: 1 },
          { testId: "11", position: 2 },
        ],
        autoActivate: true,
      }),
      expect.any(Function),
    );
  });

  it("shows the empty state when the panel has no tests", async () => {
    mockServer([]);
    wrap();
    expect(
      await screen.findByText(messages["empty.panel.tests"]),
    ).toBeInTheDocument();
  });

  it("offers a Sample Type filter populated from the shared sample-types source", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");
    const filter = document.querySelector("#panel-test-sampletype-filter");
    expect(filter).not.toBeNull();
    const options = Array.from(filter.querySelectorAll("option")).map((o) => ({
      value: o.value,
      text: o.textContent,
    }));
    // "All sample types" sentinel plus every shared sample type
    expect(options[0].value).toBe("");
    expect(options.map((o) => o.value)).toContain("2");
    expect(options.map((o) => o.text)).toContain("Whole Blood");
  });

  it("the filter narrows the candidates server-side and clearing it restores them", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");

    fireEvent.change(document.querySelector("#panel-test-sampletype-filter"), {
      target: { value: "2" },
    });
    await waitFor(() =>
      expect(
        getFromOpenElisServer.mock.calls.some(
          ([url]) =>
            url.startsWith("/rest/test-catalog/tests?") &&
            url.includes("sampleType=2"),
        ),
      ).toBe(true),
    );
    // only the Serum test is offered now
    fireEvent.click(document.getElementById("panel-add-test"));
    expect(screen.getByText(/Hemoglobin — HGB/)).toBeInTheDocument();

    // clearing returns to the unfiltered domain-compatible fetch
    fireEvent.change(document.querySelector("#panel-test-sampletype-filter"), {
      target: { value: "" },
    });
    await waitFor(() => {
      const last = getFromOpenElisServer.mock.calls
        .map(([url]) => url)
        .filter((url) => url.startsWith("/rest/test-catalog/tests?"))
        .pop();
      expect(last).not.toContain("sampleType=");
      expect(last).toContain("domain=CLINICAL");
    });
  });

  it("the filter composes with the typeahead search", async () => {
    mockServer();
    wrap();
    await screen.findByText("WBC Count");
    fireEvent.change(document.querySelector("#panel-test-sampletype-filter"), {
      target: { value: "2" },
    });
    await waitFor(() =>
      expect(document.getElementById("panel-add-test")).toBeInTheDocument(),
    );
    // typing still filters within the sample-type-narrowed candidate set
    fireEvent.click(document.getElementById("panel-add-test"));
    fireEvent.change(document.getElementById("panel-add-test"), {
      target: { value: "zzz" },
    });
    expect(screen.queryByText(/Hemoglobin — HGB/)).not.toBeInTheDocument();
  });
});
