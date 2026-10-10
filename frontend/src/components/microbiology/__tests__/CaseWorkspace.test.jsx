import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { Router, Route } from "react-router-dom";
import { createMemoryHistory } from "history";
import CaseWorklist from "../CaseWorklist";
import CaseViewShell from "../CaseViewShell";
import messages from "../../../languages/en.json";

vi.mock("../CaseTestingWorkspace", () => ({ default: () => null }));

const renderPage = (component, path = "/Microbiology/worklist") => {
  const history = createMemoryHistory({ initialEntries: [path] });
  render(
    <IntlProvider locale="en" messages={messages}>
      <Router history={history}>{component}</Router>
    </IntlProvider>,
  );
  return history;
};
const row = {
  id: "case-1",
  accessionNumber: "M2-001",
  patientName: "Test Patient",
  labUnit: "Culture lab",
  specimenType: "Sputum",
  status: "ACTIVE",
  createdAt: "2026-10-08T12:00:00Z",
};
const shell = {
  ...row,
  labUnitId: "10",
  samples: [
    {
      id: "sample-1",
      label: "M2-001-1",
      specimenType: "Sputum",
      collectionDate: "2026-10-08T12:00:00Z",
    },
  ],
  pendingSamples: [{ id: "request-1", specimenType: "Blood" }],
  relatedCases: [{ ...row, id: "case-2", labUnit: "TB lab" }],
  canWrite: true,
  transferLabUnits: [{ id: "11", value: "TB lab" }],
};

describe("MVP microbiology case workspace", () => {
  test("lists cases and keeps filters when opening the shell", async () => {
    const service = {
      searchCases: vi.fn().mockResolvedValue({
        rows: [row],
        page: 1,
        pageSize: 20,
        total: 1,
        labUnits: [{ id: "10", value: "Culture lab" }],
      }),
    };
    const history = renderPage(
      <CaseWorklist service={service} />,
      "/Microbiology/worklist?labUnitId=10",
    );
    fireEvent.click(await screen.findByRole("link", { name: "M2-001" }));
    expect(history.location.pathname).toBe("/Microbiology/cases/case-1");
    expect(history.location.search).toBe("?labUnitId=10");
    expect(service.searchCases).toHaveBeenCalledWith("labUnitId=10");
  });
  test("changing the status resets pagination and requests scoped results", async () => {
    const service = {
      searchCases: vi.fn().mockResolvedValue({
        rows: [],
        page: 3,
        pageSize: 20,
        total: 0,
        labUnits: [],
      }),
    };
    const history = renderPage(
      <CaseWorklist service={service} />,
      "/Microbiology/worklist?page=3&labUnitId=10",
    );
    await screen.findByText("No cases match these filters.");
    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "CANCELLED" },
    });
    await waitFor(() =>
      expect(service.searchCases).toHaveBeenLastCalledWith(
        "labUnitId=10&status=CANCELLED",
      ),
    );
    expect(history.location.search).toBe("?labUnitId=10&status=CANCELLED");
  });
  test("shows patient, collected and pending samples and related links", async () => {
    const service = { getCase: vi.fn().mockResolvedValue(shell) };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    expect(await screen.findByText("Test Patient")).toBeTruthy();
    expect(screen.getByText("M2-001-1")).toBeTruthy();
    expect(screen.getByText("Awaiting collection")).toBeTruthy();
    expect(
      screen
        .getByRole("link", { name: /M2-001 · TB lab/ })
        .getAttribute("href"),
    ).toBe("/Microbiology/cases/case-2");
  });
  test("transfer updates the owning unit only after success", async () => {
    const service = {
      getCase: vi.fn().mockResolvedValue(shell),
      transferCase: vi.fn().mockResolvedValue({
        ...shell,
        labUnit: "TB lab",
        labUnitId: "11",
        transferLabUnits: [],
      }),
    };
    renderPage(<CaseViewShell service={service} caseId="case-1" />);
    await screen.findByText("Test Patient");
    fireEvent.change(screen.getByLabelText("Destination lab unit"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Transfer case" }));
    await waitFor(() =>
      expect(service.transferCase).toHaveBeenCalledWith("case-1", "11"),
    );
    await screen.findByText("TB lab");
    expect(screen.queryByText("Culture lab")).toBeNull();
  });
  test("a completed transfer cannot overwrite a different case after navigation", async () => {
    let finish;
    const service = {
      getCase: vi.fn().mockImplementation((id) =>
        Promise.resolve({
          ...shell,
          id,
          patientName: id === "case-2" ? "Second Patient" : "Test Patient",
        }),
      ),
      transferCase: vi.fn().mockImplementation(
        () =>
          new Promise((resolve) => {
            finish = resolve;
          }),
      ),
    };
    renderPage(
      <Route path="/Microbiology/cases/:caseId">
        <CaseViewShell service={service} />
      </Route>,
      "/Microbiology/cases/case-1",
    );
    await screen.findByText("Test Patient");
    fireEvent.change(screen.getByLabelText("Destination lab unit"), {
      target: { value: "11" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Transfer case" }));
    fireEvent.click(screen.getByRole("link", { name: /M2-001 · TB lab/ }));
    await screen.findByText("Second Patient");
    finish({ ...shell, labUnit: "TB lab" });
    await waitFor(() => expect(screen.queryByText("Test Patient")).toBeNull());
    expect(screen.getByText("Second Patient")).toBeTruthy();
  });
  test("validation readers do not receive transfer controls", async () => {
    renderPage(
      <CaseViewShell
        caseId="case-1"
        service={{
          getCase: vi.fn().mockResolvedValue({ ...shell, canWrite: false }),
        }}
      />,
    );
    await screen.findByText("Test Patient");
    expect(screen.queryByRole("button", { name: "Transfer case" })).toBeNull();
  });
  test("failed access shows an error and never stale case data", async () => {
    renderPage(
      <CaseViewShell
        caseId="case-1"
        service={{ getCase: vi.fn().mockRejectedValue(new Error("denied")) }}
      />,
    );
    expect(
      await screen.findByText(messages["microbiology.case.loadError"]),
    ).toBeTruthy();
    expect(screen.queryByText("Test Patient")).toBeNull();
  });
});
