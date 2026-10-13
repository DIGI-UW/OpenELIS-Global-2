import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import messages from "../../../languages/en.json";
import CaseIncomingWorkspace from "../CaseIncomingWorkspace";
import CultureWorklist from "../CultureWorklist";
import useContainerScanner from "../useContainerScanner";
const detail = {
  id: "case-1",
  canWrite: true,
  samples: [{ sampleItemId: "tube-1", label: "Tube 1" }],
};
const incoming = {
  id: "incoming-1",
  testName: "Culture assay",
  state: "WAITING",
  rawResultValue: "Growth",
  sampleItemId: "tube-1",
  receivedAt: "2026-01-01T10:00:00Z",
  result: { components: [{ value: "Growth" }] },
};
function renderPage(element) {
  const history = createMemoryHistory({
    initialEntries: ["/Microbiology/worklist"],
  });
  render(
    <IntlProvider locale="en" messages={messages}>
      <Router history={history}>{element}</Router>
    </IntlProvider>,
  );
  return history;
}
test("reviewing a queue row does not place it until the user confirms a sample", async () => {
  const service = {
    getIncoming: vi.fn().mockResolvedValue([incoming]),
    placeIncoming: vi.fn().mockResolvedValue({ ...incoming, state: "PLACED" }),
  };
  renderPage(<CaseIncomingWorkspace detail={detail} service={service} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Review placement" }),
  );
  expect(service.placeIncoming).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Place result" }));
  await waitFor(() =>
    expect(service.placeIncoming).toHaveBeenCalledWith("case-1", "incoming-1", {
      sampleItemId: "tube-1",
      targetCaseId: "case-1",
      reason: "",
    }),
  );
});
test("read-only links can inspect incoming evidence but have no placement action", async () => {
  const service = { getIncoming: vi.fn().mockResolvedValue([incoming]) };
  renderPage(
    <CaseIncomingWorkspace
      detail={{ ...detail, canWrite: false }}
      service={service}
    />,
  );
  expect(await screen.findByText("Culture assay")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Review placement" })).toBeNull();
});
function Scanner({ service }) {
  useContainerScanner(service);
  return <input aria-label="Ordinary typing" />;
}
test("a rapid complete container scan resolves before navigating, ordinary input stays put", async () => {
  const caseId = "11111111-1111-4111-8111-111111111111",
    rowId = "22222222-2222-4222-8222-222222222222";
  const service = {
    resolveContainer: vi
      .fn()
      .mockResolvedValue({ caseId, rowId, scope: "CULTURE" }),
  };
  const history = renderPage(<Scanner service={service} />);
  fireEvent.keyDown(window, { key: "a" });
  fireEvent.keyDown(window, { key: "Enter" });
  expect(service.resolveContainer).not.toHaveBeenCalled();
  const code = `PLT-${caseId}-${rowId}`;
  for (const key of code) fireEvent.keyDown(window, { key });
  fireEvent.keyDown(window, { key: "Enter" });
  await waitFor(() =>
    expect(history.location.pathname).toBe(`/Microbiology/cases/${caseId}`),
  );
  expect(history.location.search).toContain(`row=${rowId}`);
  expect(service.resolveContainer).toHaveBeenCalledWith(code);
});
test("culture filters request culture-grain rows and show due reasons", async () => {
  const service = {
    searchBench: vi.fn().mockResolvedValue({
      rows: [
        {
          id: "case-1:row-1",
          caseId: "case-1",
          cultureId: "row-1",
          accessionNumber: "OWNED-001",
          canWrite: true,
          attention: ["CHECK_DUE"],
          culture: { containerIdentifier: "Plate A" },
        },
      ],
      labUnits: [],
      total: 1,
    }),
  };
  renderPage(<CultureWorklist service={service} />);
  expect(await screen.findByText("Plate A")).toBeVisible();
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.culture.title"]),
    { target: { value: "CHECK_DUE" } },
  );
  await waitFor(() =>
    expect(service.searchBench).toHaveBeenLastCalledWith(
      expect.stringContaining("cultureFilter=CHECK_DUE"),
    ),
  );
  expect(screen.getByText("Culture check due")).toBeVisible();
});
