import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import ESignatureLog from "./ESignatureLog";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const ITEMS = [
  {
    signatureId: 1,
    signerNamePrinted: "Jane Supervisor",
    signatureMeaning: "REJECTED",
    signedAt: "2026-07-10T09:00:00",
    recordType: "QC_RESULT",
    recordId: 42,
    rejectionReason: "Control out of range",
  },
  {
    signatureId: 2,
    signerNamePrinted: "John Tech",
    signatureMeaning: "AUTHORED",
    signedAt: "2026-07-09T14:30:00",
    recordType: "RESULT_BATCH",
    recordId: 7,
    rejectionReason: null,
  },
];

const mockEndpoints = ({ items = ITEMS, totalCount = items.length } = {}) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/users")) {
      callback([{ id: "10", value: "Jane Supervisor" }]);
    } else if (url.startsWith("/rest/esig/log")) {
      callback({ items, totalCount, page: 0, pageSize: 25 });
    } else {
      callback();
    }
  });
};

const lastLogUrl = () =>
  getFromOpenElisServer.mock.calls
    .map((c) => c[0])
    .filter((u) => u.startsWith("/rest/esig/log"))
    .pop();

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <ESignatureLog />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("ESignatureLog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders signature rows with localized action, subject, and reason", () => {
    mockEndpoints();
    renderPage();

    const url = lastLogUrl();
    expect(url).toContain("fromDate=");
    expect(url).toContain("page=0&pageSize=25");
    expect(url).not.toContain("meaning=");

    expect(screen.getByText("Jane Supervisor")).toBeInTheDocument();
    expect(screen.getByText("Rejected")).toBeInTheDocument();
    expect(screen.getByText("QC_RESULT #42")).toBeInTheDocument();
    expect(screen.getByText("Control out of range")).toBeInTheDocument();
    expect(screen.getByText("John Tech")).toBeInTheDocument();
    expect(screen.getByText("Authored")).toBeInTheDocument();
    expect(screen.getByText("RESULT_BATCH #7")).toBeInTheDocument();
  });

  test("shows the QA empty state when no signatures match", () => {
    mockEndpoints({ items: [] });
    renderPage();

    expect(
      screen.getByText("No signatures in this window"),
    ).toBeInTheDocument();
  });

  test("apply rebuilds the query from draft filters and resets the page", () => {
    mockEndpoints();
    renderPage();

    // Select action = Rejected in the meaning dropdown
    fireEvent.click(
      document.querySelector("#esig-log-meaning .cds--list-box__field"),
    );
    fireEvent.click(screen.getByRole("option", { name: "Rejected" }));

    // Draft change alone must not refetch
    expect(lastLogUrl()).not.toContain("meaning=");

    fireEvent.click(screen.getByTestId("esig-log-apply-filters"));

    const url = lastLogUrl();
    expect(url).toContain("meaning=REJECTED");
    expect(url).toContain("page=0");
  });

  test("filtered export opens the export URL with the applied filters, no paging", () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    mockEndpoints();
    renderPage();

    fireEvent.click(
      document.querySelector("#esig-log-meaning .cds--list-box__field"),
    );
    fireEvent.click(screen.getByRole("option", { name: "Rejected" }));
    fireEvent.click(screen.getByTestId("esig-log-apply-filters"));

    fireEvent.click(screen.getByTestId("esig-log-export-csv"));

    expect(openSpy).toHaveBeenCalledTimes(1);
    const url = openSpy.mock.calls[0][0];
    expect(url).toContain("/rest/esig/log/export?");
    expect(url).toContain("meaning=REJECTED");
    expect(url).not.toContain("page=");
    openSpy.mockRestore();
  });

  test("unfiltered export asks for confirmation before opening", () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    mockEndpoints();
    renderPage();

    fireEvent.click(screen.getByTestId("esig-log-export-pdf"));
    expect(openSpy).not.toHaveBeenCalled();
    expect(
      document.querySelector(".cds--modal.is-visible"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Export" }));
    expect(openSpy).toHaveBeenCalledTimes(1);
    expect(openSpy.mock.calls[0][0]).toContain("/rest/esig/log/exportPdf?");
    openSpy.mockRestore();
  });

  test("cancelling the unfiltered export confirm does not open anything", () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    mockEndpoints();
    renderPage();

    fireEvent.click(screen.getByTestId("esig-log-export-csv"));
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(openSpy).not.toHaveBeenCalled();
    expect(document.querySelector(".cds--modal.is-visible")).toBeNull();
    openSpy.mockRestore();
  });

  test("clear resets filters back to defaults and refetches", () => {
    mockEndpoints();
    renderPage();

    fireEvent.click(
      document.querySelector("#esig-log-meaning .cds--list-box__field"),
    );
    fireEvent.click(screen.getByRole("option", { name: "Rejected" }));
    fireEvent.click(screen.getByTestId("esig-log-apply-filters"));
    expect(lastLogUrl()).toContain("meaning=REJECTED");

    fireEvent.click(screen.getByTestId("esig-log-clear-filters"));
    expect(lastLogUrl()).not.toContain("meaning=");
  });
});
