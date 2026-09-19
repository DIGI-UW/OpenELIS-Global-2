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

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
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

    await userEvent.click(screen.getByRole("button", { name: "Preview" }));

    const plan = await screen.findByTestId("catalog-import-plan");
    expect(plan).toHaveTextContent("tests-cphl.csv");
    expect(screen.getByText("no such lab unit")).toBeInTheDocument();
    expect(postToOpenElisServerFormDataJsonResponse.mock.calls[0][0]).toContain(
      "/import/preview",
    );
    expect(apply).toBeEnabled();
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
