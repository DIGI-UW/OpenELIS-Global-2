import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import StatisticsDisplay from "../StatisticsDisplay";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("StatisticsDisplay", () => {
  const fullStatistics = {
    mean: "5.50000",
    standardDeviation: "0.37417",
    participantCount: 5,
    hasEnoughParticipants: true,
    results: [
      {
        organizationId: 10,
        resultValue: "5.0",
        zScore: "-1.33631",
        performanceStatus: "ACCEPTABLE",
      },
      {
        organizationId: 11,
        resultValue: "6.0",
        zScore: "1.33631",
        performanceStatus: "ACCEPTABLE",
      },
      {
        organizationId: 12,
        resultValue: "5.5",
        zScore: "0.00000",
        performanceStatus: "ACCEPTABLE",
      },
      {
        organizationId: 13,
        resultValue: "8.5",
        zScore: "2.50000",
        performanceStatus: "QUESTIONABLE",
      },
      {
        organizationId: 14,
        resultValue: "12.0",
        zScore: "4.50000",
        performanceStatus: "UNACCEPTABLE",
      },
    ],
  };

  const fewParticipantStats = {
    mean: "5.5",
    standardDeviation: "0.707",
    participantCount: 2,
    hasEnoughParticipants: false,
    results: [
      {
        organizationId: 10,
        resultValue: "5.0",
        zScore: null,
        performanceStatus: null,
      },
      {
        organizationId: 11,
        resultValue: "6.0",
        zScore: null,
        performanceStatus: null,
      },
    ],
  };

  test("renders statistics title", () => {
    renderWithIntl(<StatisticsDisplay statistics={fullStatistics} />);
    expect(screen.getByText("Statistics")).toBeTruthy();
  });

  test("renders mean and standard deviation", () => {
    renderWithIntl(<StatisticsDisplay statistics={fullStatistics} />);
    expect(screen.getByText("5.50000")).toBeTruthy();
    expect(screen.getByText("0.37417")).toBeTruthy();
  });

  test("renders participant count", () => {
    renderWithIntl(<StatisticsDisplay statistics={fullStatistics} />);
    expect(screen.getByText("5")).toBeTruthy();
  });

  test("shows warning when not enough participants", () => {
    renderWithIntl(<StatisticsDisplay statistics={fewParticipantStats} />);
    expect(
      screen.getByText(
        "Minimum 5 participants required for statistical analysis",
      ),
    ).toBeTruthy();
  });

  test("does not show warning when enough participants", () => {
    renderWithIntl(<StatisticsDisplay statistics={fullStatistics} />);
    expect(
      screen.queryByText(
        "Minimum 5 participants required for statistical analysis",
      ),
    ).toBeNull();
  });

  test("renders performance tags with correct colors", () => {
    const { container } = renderWithIntl(
      <StatisticsDisplay statistics={fullStatistics} />,
    );
    const greenTags = container.querySelectorAll(".cds--tag--green");
    const warmGrayTags = container.querySelectorAll(".cds--tag--warm-gray");
    const redTags = container.querySelectorAll(".cds--tag--red");
    expect(greenTags.length).toBeGreaterThanOrEqual(1);
    expect(warmGrayTags.length).toBeGreaterThanOrEqual(1);
    expect(redTags.length).toBeGreaterThanOrEqual(1);
  });

  test("renders acceptable text", () => {
    renderWithIntl(<StatisticsDisplay statistics={fullStatistics} />);
    expect(screen.getAllByText("Acceptable").length).toBeGreaterThanOrEqual(1);
  });

  test("renders null statistics gracefully", () => {
    const { container } = renderWithIntl(
      <StatisticsDisplay statistics={null} />,
    );
    expect(container.innerHTML).toBe("");
  });
});
