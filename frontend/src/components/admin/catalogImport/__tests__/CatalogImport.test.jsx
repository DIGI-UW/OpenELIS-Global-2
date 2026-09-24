import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const getFromOpenElisServer = vi.fn();
const postToOpenElisServerFormDataJsonResponse = vi.fn();
const postToOpenElisServerJsonResponse = vi.fn();

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServer(...args),
  postToOpenElisServerFormDataJsonResponse: (...args) =>
    postToOpenElisServerFormDataJsonResponse(...args),
  postToOpenElisServerJsonResponse: (...args) =>
    postToOpenElisServerJsonResponse(...args),
}));

const addNotification = vi.fn();
const setNotificationVisible = vi.fn();

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: (...args) => setNotificationVisible(...args),
    addNotification: (...args) => addNotification(...args),
  }),
}));

import CatalogImport from "../CatalogImport";

const DOMAINS = ["tests", "result-components", "result-limits"];

const UNRESOLVED = [
  {
    id: "u1",
    referenceType: "SAMPLE_TYPE",
    referenceValue: "Plasme",
    domain: "tests",
    fileName: "tests-cphl.csv",
    lineNumber: 4,
    context: "tests-cphl.csv line 4",
    occurrences: 2,
  },
];

const CANDIDATES = [
  { id: "3", name: "Plasma" },
  { id: "2", name: "Serum" },
];

/** Answers the page's three GETs; unresolved items are optional. */
const server = ({ unresolved = [] } = {}) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/import/domains")) return callback(DOMAINS);
    if (url.includes("/unresolved/candidates")) return callback(CANDIDATES);
    if (url.includes("/unresolved")) return callback(unresolved);
    return callback([]);
  });
};

const renderPage = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <CatalogImport />
      </IntlProvider>
    </MemoryRouter>,
  );

const csv = (name) =>
  new File(["testName,testSection\n"], name, { type: "text/csv" });

const PLAN_OK = {
  importRunId: "run-1",
  unresolvedCount: 0,
  files: [
    {
      domain: "tests",
      fileName: "tests-cphl.csv",
      created: 2,
      updated: 1,
      skipped: 0,
      rows: [],
      error: null,
    },
  ],
};

/** Previews succeed; the apply answers with whatever the case needs. */
const applyAnswers = (applyResponse) => {
  postToOpenElisServerFormDataJsonResponse.mockImplementation(
    (url, formData, callback) =>
      callback(url.includes("/import/preview") ? PLAN_OK : applyResponse),
  );
};

const previewThenApply = async () => {
  renderPage();
  await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
  await drop([csv("tests-cphl.csv")]);
  await userEvent.click(screen.getByRole("button", { name: "Preview" }));
  await screen.findByRole("heading", { name: "What these files would do" });
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
};

const notificationKinds = () =>
  addNotification.mock.calls.map(([notification]) => notification.kind);

/** Drops files on the page the way Carbon's drop container reports them. */
const drop = async (files) => {
  const input = document.querySelector('input[type="file"]');
  await userEvent.upload(input, files);
};

describe("CatalogImport", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    server();
  });

  test("names each dropped file's catalog area from the file name", async () => {
    renderPage();
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());

    await drop([csv("result-limits-cphl.csv"), csv("unnamed.csv")]);

    const table = await screen.findByTestId("catalog-import-files");
    expect(table).toHaveTextContent("result-limits-cphl.csv");
    // "result-limits" wins over no match; a file that names no area is left blank
    // for the operator to choose.
    expect(
      screen.getByLabelText(/Catalog area/i, {
        selector: "#domain-result-limits-cphl\\.csv",
      }),
    ).toHaveValue("result-limits");
    expect(
      screen.getByLabelText(/Catalog area/i, {
        selector: "#domain-unnamed\\.csv",
      }),
    ).toHaveValue("");
  });

  test("previews before it can apply, and shows each file's counts and skipped rows", async () => {
    postToOpenElisServerFormDataJsonResponse.mockImplementation(
      (url, formData, callback) =>
        callback({
          importRunId: "run-1",
          unresolvedCount: 0,
          files: [
            {
              domain: "tests",
              fileName: "tests-cphl.csv",
              created: 2,
              updated: 1,
              skipped: 1,
              rows: [
                {
                  lineNumber: 5,
                  outcome: "SKIPPED",
                  reason: "no such lab unit",
                },
              ],
              error: null,
            },
          ],
        }),
    );
    renderPage();
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    await drop([csv("tests-cphl.csv")]);

    const apply = screen.getByRole("button", { name: "Apply" });
    expect(apply).toBeDisabled();
    expect(screen.getByTestId("catalog-import-apply-hint")).toHaveTextContent(
      "Preview the files first",
    );

    await userEvent.click(screen.getByRole("button", { name: "Preview" }));

    const plan = await screen.findByTestId("catalog-import-plan");
    expect(plan).toHaveTextContent("tests-cphl.csv");
    expect(screen.getByText("no such lab unit")).toBeInTheDocument();
    expect(postToOpenElisServerFormDataJsonResponse.mock.calls[0][0]).toContain(
      "/import/preview",
    );
    expect(apply).toBeEnabled();
    expect(
      screen.queryByTestId("catalog-import-apply-hint"),
    ).not.toBeInTheDocument();
  });

  test("a refused apply keeps the preview on screen and says why", async () => {
    applyAnswers({
      importRunId: null,
      unresolvedCount: 0,
      files: [
        {
          domain: null,
          fileName: null,
          created: 0,
          updated: 0,
          skipped: 0,
          rows: [],
          error:
            "Could not save tests-cphl.csv to /cfg/tests: permission denied",
        },
      ],
      status: 422,
      statusCode: 422,
    });

    await previewThenApply();

    const failure = await screen.findByTestId("catalog-import-failure");
    expect(failure).toHaveTextContent("The catalog files were not applied.");
    expect(failure).toHaveTextContent(
      "Could not save tests-cphl.csv to /cfg/tests: permission denied",
    );
    expect(
      screen.queryByRole("heading", { name: "What was loaded" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "What these files would do" }),
    ).toBeInTheDocument();
    expect(notificationKinds()).toEqual(["error"]);
  });

  test("a server error on apply is not reported as a load", async () => {
    applyAnswers({
      timestamp: "2026-09-21T07:00:00Z",
      status: 500,
      error: "Internal Server Error",
      statusCode: 500,
    });

    await previewThenApply();

    const failure = await screen.findByTestId("catalog-import-failure");
    expect(failure).toHaveTextContent("Internal Server Error");
    expect(
      screen.queryByRole("heading", { name: "What was loaded" }),
    ).not.toBeInTheDocument();
    expect(notificationKinds()).not.toContain("success");
  });

  test("a file the loader did not read is an error in what was loaded", async () => {
    applyAnswers({
      importRunId: "run-2",
      unresolvedCount: 0,
      files: [
        {
          domain: "tests",
          fileName: "tests-cphl.csv",
          created: 0,
          updated: 0,
          skipped: 0,
          rows: [],
          error: "the tests loader did not find tests-cphl.csv in /cfg/tests",
        },
      ],
    });

    await previewThenApply();

    expect(
      await screen.findByRole("heading", { name: "What was loaded" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "the tests loader did not find tests-cphl.csv in /cfg/tests",
      ),
    ).toBeInTheDocument();
    expect(notificationKinds()).toEqual(["error"]);
    expect(addNotification.mock.calls[0][0].message).toContain(
      "Some catalog files were not loaded",
    );
  });

  test("an apply that loaded every file is reported as a success", async () => {
    applyAnswers(PLAN_OK);

    await previewThenApply();

    expect(
      await screen.findByRole("heading", { name: "What was loaded" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("catalog-import-failure"),
    ).not.toBeInTheDocument();
    expect(notificationKinds()).toEqual(["success"]);
  });

  test("a file the loader rejected blocks Apply", async () => {
    postToOpenElisServerFormDataJsonResponse.mockImplementation(
      (url, formData, callback) =>
        callback({
          importRunId: "run-2",
          unresolvedCount: 0,
          files: [
            {
              domain: "tests",
              fileName: "tests-cphl.csv",
              created: 0,
              updated: 0,
              skipped: 0,
              rows: [],
              error: "must have a 'testName' column",
            },
          ],
        }),
    );
    renderPage();
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    await drop([csv("tests-cphl.csv")]);
    await userEvent.click(screen.getByRole("button", { name: "Preview" }));

    expect(
      await screen.findByText("must have a 'testName' column"),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Apply" })).toBeDisabled();
  });

  test("resolving a name remembers it only when asked", async () => {
    server({ unresolved: UNRESOLVED });
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ id: "u1", status: "RESOLVED" }),
    );
    renderPage();

    const queue = await screen.findByTestId("catalog-import-unresolved");
    expect(queue).toHaveTextContent("Plasme");
    const use = screen.getByRole("button", { name: "Use" });
    expect(use).toBeDisabled();

    await userEvent.selectOptions(
      screen.getByLabelText(/Use this record/i),
      "3",
    );
    await userEvent.click(use);

    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]),
    ).toEqual({ action: "USE_EXISTING", targetId: "3" });

    await userEvent.click(screen.getByLabelText(/Remember this name/i));
    await userEvent.click(use);
    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[1][1]),
    ).toEqual({ action: "ALIAS", targetId: "3" });
  });
});
