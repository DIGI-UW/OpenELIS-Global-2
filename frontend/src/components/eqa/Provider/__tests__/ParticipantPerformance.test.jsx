import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";
import ParticipantPerformance from "../ParticipantPerformance";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

test("names the scheme and prints verdict labels, not codes", async () => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/eqa/programs/3") cb({ id: 3, name: "Regional CD4 PT" });
    else if (url === "/rest/eqa/provider/schemes/3/performance")
      cb([
        {
          organizationId: 100,
          organizationName: "District Lab A",
          judged: 1,
          accepted: 1,
          mostRecentPerformance: "ACCEPTABLE",
          openFollowups: 0,
          cycles: [],
        },
      ]);
    else cb([]);
  });

  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/schemes/3/performance"]}>
        <Route path="/qa/eqa/provider/schemes/:schemeId/performance">
          <ParticipantPerformance />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );

  expect(
    await screen.findByText("Participant performance: Regional CD4 PT"),
  ).toBeInTheDocument();
  expect(screen.getByText("Acceptable")).toBeInTheDocument();
  expect(screen.queryByText("ACCEPTABLE")).toBeNull();
});

// OGC-1666: three rows of one test were identical; each names its panel sample.
test("names the panel sample of each result in a cycle's history", async () => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/eqa/programs/3") cb({ id: 3, name: "HIV Serology PT" });
    else if (url === "/rest/eqa/provider/schemes/3/performance")
      cb([
        {
          organizationId: 100,
          organizationName: "Alotau Hospital",
          judged: 3,
          accepted: 1,
          mostRecentPerformance: "UNACCEPTABLE",
          openFollowups: 1,
          cycles: [
            {
              cycleId: 7,
              cycleNumber: 4,
              cycleName: "HIV-SERO-RDT-PT-26-4",
              analytes: [
                {
                  test: "Determine",
                  sampleCode: "HIV-C-2",
                  reported: "NR",
                  performance: "UNACCEPTABLE",
                },
                {
                  test: "Determine",
                  sampleCode: "HIV-C-3",
                  reported: "NR",
                  performance: "ACCEPTABLE",
                },
                {
                  test: "Determine",
                  sampleCode: null,
                  reported: "R",
                  performance: "ACCEPTABLE",
                },
              ],
            },
          ],
        },
      ]);
    else cb([]);
  });

  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/schemes/3/performance"]}>
        <Route path="/qa/eqa/provider/schemes/:schemeId/performance">
          <ParticipantPerformance />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );
  await screen.findByText("Alotau Hospital");
  fireEvent.click(screen.getByRole("button", { name: "Cycle history" }));

  const history = (await screen.findByText("Sample")).closest("table");
  expect(within(history).getByText("HIV-SERO-RDT-PT-26-4")).toBeInTheDocument();
  expect(
    [...history.querySelectorAll(":scope > tbody > tr")].map(
      (row) => row.querySelectorAll("td")[2].textContent,
    ),
  ).toEqual(["HIV-C-2", "HIV-C-3", ""]);
});
