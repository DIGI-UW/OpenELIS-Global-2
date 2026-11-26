/**
 * The prior anatomic-pathology results under the patient band (Shell S-2):
 * the patient's earlier pathology, immunohistochemistry and cytology cases,
 * newest first, read from the bench's own endpoint and never re-read after a
 * save.
 */
import React from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { ConfigurationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import PriorResultsPanel, { canOpenBench } from "./PriorResultsPanel";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

import { getFromOpenElisServer } from "../utils/Utils";

const CAN_READ_ALL = { userLabRolesMap: { AllLabUnits: ["Results"] } };

const row = (overrides) => ({
  bench: "PATHOLOGY",
  caseId: 11,
  labNumber: "PR-0101",
  requestDate: "2026-01-10",
  specimen: "Tissue biopsy, Fluid",
  conclusionState: "REPORTED",
  conclusion: "Adenocarcinoma, Old term",
  ...overrides,
});

const fiveRows = [11, 12, 13, 14, 15].map((caseId) =>
  row({ caseId, labNumber: "PR-0" + caseId }),
);
const sixRows = [...fiveRows, row({ caseId: 16, labNumber: "PR-016" })];

// Each request in order, so a test answers the one it is about.
const requests = () => getFromOpenElisServer.mock.calls;
const answer = async (index, response) => {
  await act(async () => {
    requests()[index][1](response);
  });
};

const renderPanel = ({
  bench = "pathology",
  caseId = "7",
  userSessionDetails = CAN_READ_ALL,
} = {}) => {
  const tree = (props) => (
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
    >
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider value={{ userSessionDetails }}>
          <PriorResultsPanel {...props} />
        </UserSessionDetailsContext.Provider>
      </IntlProvider>
    </ConfigurationContext.Provider>
  );
  const view = render(tree({ bench, caseId }));
  return {
    ...view,
    rerenderWith: (props) => view.rerender(tree({ bench, caseId, ...props })),
  };
};

const toggleButton = () => screen.getByTestId("prior-results-toggle");
const statusElement = () => screen.getByRole("status");
const follows = (earlier, later) =>
  Boolean(
    earlier.compareDocumentPosition(later) & Node.DOCUMENT_POSITION_FOLLOWING,
  );

beforeEach(() => {
  getFromOpenElisServer.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("the prior results panel, prior anatomic-pathology results", () => {
  it.each(["pathology", "immunohistochemistry", "cytology"])(
    "asks its own bench's endpoint once on mount (%s)",
    (bench) => {
      renderPanel({ bench, caseId: "7" });

      expect(requests()).toHaveLength(1);
      expect(requests()[0][0]).toBe(`/rest/${bench}/caseView/7/priorResults`);
      expect(requests()[0][2]).toBeInstanceOf(AbortSignal);
    },
  );

  it("shows a skeleton until answered", async () => {
    renderPanel();

    expect(
      screen.getByRole("heading", {
        level: 4,
        name: messages["caseView.label.priorResults"],
      }),
    ).toBeInTheDocument();
    expect(screen.getByTestId("prior-results-loading")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.getByTestId("prior-results")).toHaveAttribute(
      "aria-busy",
      "true",
    );
    expect(statusElement()).toBeEmptyDOMElement();

    await answer(0, { rows: [row()], total: 1 });

    expect(screen.queryByTestId("prior-results-loading")).toBeNull();
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByTestId("prior-results")).toHaveAttribute(
      "aria-busy",
      "false",
    );
    expect(statusElement()).toBeEmptyDOMElement();
  });

  it("shows the empty sentence and no table when total is 0", async () => {
    renderPanel();

    await answer(0, { rows: [], total: 0 });

    expect(
      screen.getByText(messages["pathology.empty.noPriorResults"]),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.queryByTestId("prior-results-toggle")).toBeNull();
    expect(
      screen.queryByText(messages["caseView.priorResults.loadFailed"]),
    ).toBeNull();
    expect(
      screen.queryByText(messages["caseView.priorResults.noneInYourUnits"]),
    ).toBeNull();
  });

  describe("tells a user whose lab units limit the list that it is limited", () => {
    it("says none in your lab units, not none for this patient, when restricted and total is 0", async () => {
      renderPanel();

      await answer(0, { rows: [], total: 0, restricted: true });

      expect(
        screen.getByText(messages["caseView.priorResults.noneInYourUnits"]),
      ).toBeInTheDocument();
      expect(
        screen.queryByText(messages["pathology.empty.noPriorResults"]),
      ).toBeNull();
      expect(
        screen.queryByText(
          messages["caseView.priorResults.otherUnitsNotListed"],
        ),
      ).toBeNull();
      expect(screen.queryByRole("table")).toBeNull();
      expect(statusElement()).toBeEmptyDOMElement();
    });

    it("says other units' cases are not listed under the table and above the button, also after Show all", async () => {
      renderPanel();
      await answer(0, { rows: fiveRows, total: 6, restricted: true });

      const limited = () =>
        screen.getByText(messages["caseView.priorResults.otherUnitsNotListed"]);
      expect(follows(screen.getByRole("table"), limited())).toBe(true);
      expect(follows(limited(), toggleButton())).toBe(true);
      expect(
        screen.queryByText(messages["caseView.priorResults.noneInYourUnits"]),
      ).toBeNull();

      fireEvent.click(toggleButton());
      await answer(1, { rows: sixRows, total: 6, restricted: true });

      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(6);
      expect(follows(screen.getByRole("table"), limited())).toBe(true);
      expect(follows(limited(), toggleButton())).toBe(true);
    });

    it.each([
      ["missing", undefined],
      ["false", false],
      ["the string true", "true"],
      ["1", 1],
    ])(
      "reads restricted %s as not limited, and the answer is still good",
      async (_label, restricted) => {
        renderPanel();
        await answer(0, { rows: [], total: 0, restricted });

        expect(
          screen.getByText(messages["pathology.empty.noPriorResults"]),
        ).toBeInTheDocument();
        expect(
          screen.queryByText(messages["caseView.priorResults.noneInYourUnits"]),
        ).toBeNull();
        expect(
          screen.queryByText(messages["caseView.priorResults.loadFailed"]),
        ).toBeNull();
      },
    );

    it("adds no line under the table when not restricted", async () => {
      renderPanel();
      await answer(0, { rows: fiveRows, total: 6, restricted: false });

      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(5);
      expect(
        screen.queryByText(
          messages["caseView.priorResults.otherUnitsNotListed"],
        ),
      ).toBeNull();
    });
  });

  it("lists lab number, bench name, site-format date, specimen and conclusion, under a title that labels the table", async () => {
    renderPanel();

    await answer(0, {
      rows: [
        row(),
        row({
          bench: "CYTOLOGY",
          caseId: 31,
          labNumber: "CY-0031",
          requestDate: "2025-12-03",
          specimen: "Cervical smear",
          conclusionState: "NOT_REPORTED",
          conclusion: null,
        }),
      ],
      total: 2,
    });

    const table = screen.getByRole("table");
    expect(table).toHaveAttribute("aria-labelledby", "prior-results-title");
    expect(document.getElementById("prior-results-title")).toHaveTextContent(
      messages["caseView.label.priorResults"],
    );
    const headers = within(table)
      .getAllByRole("columnheader")
      .map((header) => header.textContent);
    expect(headers).toEqual([
      messages["sample.label.labnumber"],
      messages["sample.requestDate"],
      messages["caseView.label.specimen"],
      messages["pathology.label.conclusion"],
    ]);

    const pathology = screen.getByTestId("prior-result-PATHOLOGY-11");
    const cells = within(pathology).getAllByRole("cell");
    expect(cells[0]).toHaveTextContent("PR-0101");
    expect(cells[0]).toHaveTextContent(messages["pathology.label.title"]);
    // The site reads dates day first.
    expect(cells[1]).toHaveTextContent("10/01/2026");
    expect(cells[2]).toHaveTextContent("Tissue biopsy, Fluid");
    expect(cells[3]).toHaveTextContent("Adenocarcinoma, Old term");

    const cytology = screen.getByTestId("prior-result-CYTOLOGY-31");
    expect(cytology).toHaveTextContent(messages["cytology.label.title"]);
    expect(cytology).toHaveTextContent("03/12/2025");
    // Newest first, as the server sent them.
    const order = screen
      .getAllByTestId(/^prior-result-/)
      .map((element) => element.getAttribute("data-testid"));
    expect(order).toEqual([
      "prior-result-PATHOLOGY-11",
      "prior-result-CYTOLOGY-31",
    ]);
  });

  it("prints the three fixed sentences for NOT_REPORTED, NONE_RECORDED and IN_REPORT, never a text sent with them", async () => {
    renderPanel();

    await answer(0, {
      rows: [
        row({ caseId: 1, conclusionState: "NOT_REPORTED", conclusion: null }),
        row({ caseId: 2, conclusionState: "NONE_RECORDED", conclusion: null }),
        row({
          bench: "IMMUNOHISTOCHEMISTRY",
          caseId: 3,
          conclusionState: "IN_REPORT",
          conclusion: "Should not show",
        }),
      ],
      total: 3,
    });

    const conclusion = (testId) =>
      within(screen.getByTestId(testId)).getAllByRole("cell")[3];
    expect(conclusion("prior-result-PATHOLOGY-1")).toHaveTextContent(
      messages["caseView.priorResults.notReported"],
    );
    expect(conclusion("prior-result-PATHOLOGY-2")).toHaveTextContent(
      messages["caseView.priorResults.noConclusion"],
    );
    expect(conclusion("prior-result-IMMUNOHISTOCHEMISTRY-3")).toHaveTextContent(
      messages["caseView.priorResults.inReport"],
    );
    expect(screen.queryByText("Should not show")).toBeNull();
  });

  it("prints Not recorded for a blank specimen", async () => {
    renderPanel();

    await answer(0, { rows: [row({ specimen: "" })], total: 1 });

    const specimen = within(
      screen.getByTestId("prior-result-PATHOLOGY-11"),
    ).getAllByRole("cell")[2];
    expect(specimen).toHaveTextContent(messages["common.notRecorded"]);
  });

  describe("links the lab number in a new tab only for a bench the user may open", () => {
    const threeBenches = {
      rows: [
        row({ bench: "PATHOLOGY", caseId: 11, labNumber: "PR-0101" }),
        row({
          bench: "IMMUNOHISTOCHEMISTRY",
          caseId: 21,
          labNumber: "IH-0201",
        }),
        row({ bench: "CYTOLOGY", caseId: 31, labNumber: "CY-0301" }),
      ],
      total: 3,
    };
    const linkIn = (testId) =>
      within(screen.getByTestId(testId)).queryByRole("link");

    it("links every bench, in a new tab, for a user with Results under all lab units", async () => {
      renderPanel({ userSessionDetails: CAN_READ_ALL });
      await answer(0, threeBenches);

      const pathology = linkIn("prior-result-PATHOLOGY-11");
      expect(pathology).toHaveAttribute("href", "/PathologyCaseView/11");
      expect(pathology).toHaveAttribute("target", "_blank");
      expect(pathology).toHaveAttribute("rel", "noopener noreferrer");
      expect(pathology).toHaveAccessibleName(
        "Open Pathology case PR-0101 in a new tab",
      );
      expect(pathology).toHaveTextContent("PR-0101");
      expect(linkIn("prior-result-IMMUNOHISTOCHEMISTRY-21")).toHaveAttribute(
        "href",
        "/ImmunohistochemistryCaseView/21",
      );
      expect(linkIn("prior-result-CYTOLOGY-31")).toHaveAttribute(
        "href",
        "/CytologyCaseView/31",
      );
    });

    it("links its own unit's cases and leaves another unit's as plain text", async () => {
      renderPanel({
        userSessionDetails: { userLabRolesMap: { Cytology: ["Results"] } },
      });
      await answer(0, threeBenches);

      expect(linkIn("prior-result-CYTOLOGY-31")).toHaveAttribute(
        "href",
        "/CytologyCaseView/31",
      );
      expect(linkIn("prior-result-PATHOLOGY-11")).toBeNull();
      expect(linkIn("prior-result-IMMUNOHISTOCHEMISTRY-21")).toBeNull();
      expect(screen.getByTestId("prior-result-PATHOLOGY-11")).toHaveTextContent(
        "PR-0101",
      );
    });

    it("links nothing when the user has no lab unit map", async () => {
      renderPanel({ userSessionDetails: { userId: "1", roles: [] } });
      await answer(0, threeBenches);

      expect(screen.queryAllByRole("link")).toHaveLength(0);
      expect(screen.getByTestId("prior-result-CYTOLOGY-31")).toHaveTextContent(
        "CY-0301",
      );
    });

    it("links nothing when all lab units lack Results, though the user's own unit has it, as the route decides", async () => {
      const userSessionDetails = {
        userLabRolesMap: {
          AllLabUnits: ["Reception"],
          Pathology: ["Results"],
        },
      };
      renderPanel({ userSessionDetails });
      await answer(0, threeBenches);

      expect(screen.queryAllByRole("link")).toHaveLength(0);
      expect(canOpenBench(userSessionDetails, "pathology")).toBe(false);
    });

    it("reads the route's rule for each case view's unit", () => {
      const own = { userLabRolesMap: { Immunohistochemistry: ["Results"] } };
      expect(canOpenBench(own, "immunohistochemistry")).toBe(true);
      expect(canOpenBench(own, "IMMUNOHISTOCHEMISTRY")).toBe(true);
      expect(canOpenBench(own, "pathology")).toBe(false);
      expect(canOpenBench(CAN_READ_ALL, "cytology")).toBe(true);
      expect(canOpenBench(undefined, "cytology")).toBe(false);
      expect(
        canOpenBench(
          { userLabRolesMap: { Cytology: ["Reception"] } },
          "cytology",
        ),
      ).toBe(false);
    });
  });

  it("gives two rows with one lab number distinct keys and distinct link names", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    renderPanel();

    await answer(0, {
      rows: [
        row({ bench: "IMMUNOHISTOCHEMISTRY", caseId: 21, labNumber: "AP-01" }),
        row({ bench: "PATHOLOGY", caseId: 21, labNumber: "AP-01" }),
      ],
      total: 2,
    });

    expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(2);
    const names = screen
      .getAllByRole("link")
      .map((link) => link.getAttribute("aria-label"));
    expect(names).toEqual([
      "Open Immunohistochemistry case AP-01 in a new tab",
      "Open Pathology case AP-01 in a new tab",
    ]);
    const keyWarnings = consoleError.mock.calls.filter((call) =>
      String(call[0]).includes("same key"),
    );
    expect(keyWarnings).toHaveLength(0);
    consoleError.mockRestore();
  });

  describe("offers Show all only when there are more cases than rows", () => {
    it("offers no button when total equals the rows", async () => {
      renderPanel();

      await answer(0, { rows: fiveRows, total: 5 });

      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(5);
      expect(screen.queryByTestId("prior-results-toggle")).toBeNull();
    });

    it("offers Show all {total} as a small ghost button, collapsed, when total exceeds the rows", async () => {
      renderPanel();

      await answer(0, { rows: fiveRows, total: 6 });

      const button = toggleButton();
      expect(button).toHaveTextContent("Show all 6");
      expect(button).toHaveClass("cds--btn--ghost", "cds--btn--sm");
      expect(button).toHaveAttribute("aria-expanded", "false");
    });

    it("keeps the rows and disables the button while every row is read", async () => {
      renderPanel({ caseId: "7" });
      await answer(0, { rows: fiveRows, total: 6 });

      fireEvent.click(toggleButton());

      expect(requests()).toHaveLength(2);
      expect(requests()[1][0]).toBe(
        "/rest/pathology/caseView/7/priorResults?all=true",
      );
      expect(requests()[1][2]).toBeInstanceOf(AbortSignal);
      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(5);
      expect(toggleButton()).toBeDisabled();
    });

    it("lists every row once answered, then Show fewer lists the first rows with no request, and Show all again reads nothing", async () => {
      renderPanel();
      await answer(0, { rows: fiveRows, total: 6 });

      fireEvent.click(toggleButton());
      await answer(1, { rows: sixRows, total: 6 });

      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(6);
      expect(toggleButton()).toHaveTextContent(
        messages["caseView.priorResults.showFewer"],
      );
      expect(toggleButton()).toHaveAttribute("aria-expanded", "true");
      expect(toggleButton()).toBeEnabled();

      fireEvent.click(toggleButton());

      expect(requests()).toHaveLength(2);
      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(5);
      expect(toggleButton()).toHaveTextContent("Show all 6");

      fireEvent.click(toggleButton());

      expect(requests()).toHaveLength(2);
      expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(6);
    });

    it("keeps focus on the button through Show all and Show fewer", async () => {
      renderPanel();
      await answer(0, { rows: fiveRows, total: 6 });

      toggleButton().focus();
      fireEvent.click(toggleButton());
      // A browser drops focus from a button to the page once the button is
      // disabled; jsdom keeps it there and refuses to blur it, so the test
      // drops it to the page through an element that then leaves.
      expect(toggleButton()).toBeDisabled();
      const elsewhere = document.createElement("button");
      document.body.appendChild(elsewhere);
      act(() => {
        elsewhere.focus();
        elsewhere.remove();
      });
      expect(document.activeElement).toBe(document.body);
      await answer(1, { rows: sixRows, total: 6 });

      expect(toggleButton()).toHaveFocus();

      fireEvent.click(toggleButton());

      expect(toggleButton()).toHaveFocus();
    });
  });

  it("keeps the rows and says so when Show all fails, with the button enabled to try again", async () => {
    renderPanel();
    await answer(0, { rows: fiveRows, total: 6 });

    fireEvent.click(toggleButton());
    await answer(1, undefined);

    expect(screen.getAllByTestId(/^prior-result-/)).toHaveLength(5);
    expect(
      within(statusElement()).getByText(
        messages["caseView.priorResults.loadFailed"],
      ),
    ).toBeInTheDocument();
    expect(follows(screen.getByRole("table"), statusElement())).toBe(true);
    expect(toggleButton()).toBeEnabled();
    expect(toggleButton()).toHaveTextContent("Show all 6");

    fireEvent.click(toggleButton());

    expect(requests()).toHaveLength(3);
    expect(
      screen.queryByText(messages["caseView.priorResults.loadFailed"]),
    ).toBeNull();
    expect(statusElement()).toBeEmptyDOMElement();
  });

  describe("says prior results could not be loaded, never that there are none", () => {
    const saysCouldNotLoad = () => {
      expect(
        within(statusElement()).getByText(
          messages["caseView.priorResults.loadFailed"],
        ),
      ).toBeInTheDocument();
      expect(screen.getByTestId("prior-results")).toHaveAttribute(
        "aria-busy",
        "false",
      );
      expect(
        screen.queryByText(messages["pathology.empty.noPriorResults"]),
      ).toBeNull();
      expect(screen.queryByRole("table")).toBeNull();
      expect(screen.queryByTestId("prior-results-loading")).toBeNull();
    };

    it.each([
      ["[]", []],
      ["undefined", undefined],
      ["an error body", { message: "boom" }],
    ])("for %s", async (_label, response) => {
      renderPanel();

      await answer(0, response);

      saysCouldNotLoad();
    });

    it("for a failed request", async () => {
      const actual = await vi.importActual("../utils/Utils");
      getFromOpenElisServer.mockImplementation(actual.getFromOpenElisServer);
      vi.spyOn(console, "error").mockImplementation(() => {});
      vi.stubGlobal(
        "fetch",
        vi.fn(() =>
          Promise.resolve({
            ok: false,
            status: 500,
            headers: new Headers({ "content-type": "application/json" }),
            json: () => Promise.resolve({ error: "Internal Server Error" }),
          }),
        ),
      );

      renderPanel();

      await screen.findByText(messages["caseView.priorResults.loadFailed"]);
      saysCouldNotLoad();
      console.error.mockRestore();
    });
  });

  it("aborts its request on unmount, and sets nothing from a late answer", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const { unmount } = renderPanel();
    const signal = requests()[0][2];
    expect(signal.aborted).toBe(false);

    // React 17 runs an effect's cleanup after the commit; act flushes it.
    act(() => {
      unmount();
    });

    expect(signal.aborted).toBe(true);
    await answer(0, { rows: [row()], total: 1 });
    expect(consoleError).not.toHaveBeenCalled();
    consoleError.mockRestore();
  });

  it("aborts its requests when the case changes, and reads the new case's", async () => {
    const view = renderPanel({ caseId: "7" });
    await answer(0, { rows: fiveRows, total: 6 });
    fireEvent.click(toggleButton());
    const first = requests()[0][2];
    const every = requests()[1][2];

    view.rerenderWith({ caseId: "8" });

    expect(first.aborted).toBe(true);
    expect(every.aborted).toBe(true);
    expect(requests()[2][0]).toBe("/rest/pathology/caseView/8/priorResults");
    // A late answer for the case that was left changes nothing.
    await answer(1, { rows: sixRows, total: 6 });
    expect(screen.getByTestId("prior-results-loading")).toBeInTheDocument();
  });
});
