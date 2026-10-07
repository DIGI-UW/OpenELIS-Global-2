import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import CultureSetSummary from "../CultureSetSummary";
import messages from "../../../languages/en.json";

it("counts distinct explicit sets, groups bottle details, and excludes unrelated specimens", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary
        specimens={[
          {
            collectedInSets: true,
            cultureSetNumber: 3,
            bodySite: "Right arm",
            containerType: "Aerobic",
          },
          {
            collectedInSets: true,
            cultureSetNumber: 1,
            bodySite: "Left arm",
            containerType: "Aerobic",
          },
          {
            collectedInSets: true,
            cultureSetNumber: 1,
            bodySite: "Left arm",
            containerType: "Anaerobic",
          },
          {
            collectedInSets: false,
            cultureSetNumber: 9,
            bodySite: "Unrelated",
          },
        ]}
      />
    </IntlProvider>,
  );
  expect(screen.getByText("2 sets, 3 bottles")).toBeInTheDocument();
  const lines = screen.getAllByRole("listitem");
  expect(lines[0]).toHaveTextContent("Set 1: Left arm; Aerobic, Anaerobic");
  expect(lines[1]).toHaveTextContent("Set 3: Right arm; Aerobic");
  expect(screen.queryByText(/Unrelated/)).not.toBeInTheDocument();
});

it("shows missing historical assignments without inventing another set", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary
        specimens={[
          {
            collectedInSets: true,
            cultureSetNumber: null,
            containerType: "Aerobic",
          },
        ]}
      />
    </IntlProvider>,
  );
  expect(screen.getByText("0 sets, 1 bottle")).toBeInTheDocument();
  expect(screen.getByText("Set not recorded: Aerobic")).toBeInTheDocument();
});

it("places readable warning tags on the affected set without disabling anything", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary
        specimens={[
          {
            collectedInSets: true,
            cultureSetNumber: 1,
            containerType: "Aerobic",
          },
          {
            collectedInSets: true,
            cultureSetNumber: 2,
            containerType: "Anaerobic",
          },
        ]}
        warnings={[
          { setNumber: 2, code: "SINGLE_BOTTLE", intervalMinutes: 45 },
          { setNumber: 2, code: "COLLECTION_INTERVAL", intervalMinutes: 45 },
        ]}
      />
    </IntlProvider>,
  );
  const lines = screen.getAllByRole("listitem");
  expect(lines[0]).not.toHaveTextContent("Only one bottle");
  expect(lines[1]).toHaveTextContent("Only one bottle in this set");
  expect(lines[1]).toHaveTextContent(
    "Collection times are more than 45 minutes apart",
  );
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
});
