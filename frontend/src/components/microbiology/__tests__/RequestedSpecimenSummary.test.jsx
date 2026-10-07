import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import RequestedSpecimenSummary from "../RequestedSpecimenSummary";
import CultureSetSummary from "../CultureSetSummary";
import messages from "../../../languages/en.json";

it("labels requests as awaiting collection without offering result targets", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <RequestedSpecimenSummary
        specimens={[
          {
            requestId: 7,
            specimenType: "Blood",
            containerType: "Aerobic",
            bodySite: "Left arm",
            collectedInSets: true,
            cultureSetNumber: 2,
          },
        ]}
      />
    </IntlProvider>,
  );
  expect(screen.getByText("Awaiting collection")).toBeInTheDocument();
  expect(screen.getByText(/Blood · Aerobic · Left arm/)).toBeInTheDocument();
  expect(screen.getByText(/Set 2/)).toBeInTheDocument();
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
});

it("counts both requested and collected bottles without doubling their sets", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary
        specimens={[
          {
            collectedInSets: true,
            cultureSetNumber: 1,
            containerType: "Aerobic",
          },
        ]}
        requestedSpecimens={[
          {
            collectedInSets: true,
            cultureSetNumber: 1,
            containerType: "Anaerobic",
          },
          {
            collectedInSets: true,
            cultureSetNumber: 2,
            containerType: "Aerobic",
          },
        ]}
      />
    </IntlProvider>,
  );
  expect(screen.getByText("2 sets, 3 bottles")).toBeInTheDocument();
});

it("removes the pending section once every request has been collected", () => {
  const { container } = render(
    <IntlProvider locale="en" messages={messages}>
      <RequestedSpecimenSummary specimens={[]} />
    </IntlProvider>,
  );
  expect(container).toBeEmptyDOMElement();
});
