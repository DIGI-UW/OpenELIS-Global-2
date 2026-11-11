import React from "react";
import { render, screen } from "@testing-library/react";
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
