import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import LeveyJenningsChart from "../LeveyJenningsChart";

jest.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: jest.fn(),
}));

jest.mock("@carbon/charts-react", () => ({
  LineChart: ({ data, options }) => (
    <div data-testid="line-chart">
      <span>{options.title}</span>
      <span>{data.length} data points</span>
    </div>
  ),
}));

const { postToOpenElisServerJsonResponse } = require("../../utils/Utils");

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("LeveyJenningsChart", () => {
  const mockChartData = {
    mean: "100",
    sd: "5",
    plus2SD: "110",
    minus2SD: "90",
    plus3SD: "115",
    minus3SD: "85",
    dataPoints: [
      { index: 0, value: "98", zScore: "-0.4" },
      { index: 1, value: "102", zScore: "0.4" },
      { index: 2, value: "100", zScore: "0" },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("shows no data message when no values", () => {
    renderWithIntl(<LeveyJenningsChart controlId={1} values={[]} />);
    expect(screen.getByText("No QC data available")).toBeTruthy();
  });

  test("shows no data message when values is null", () => {
    renderWithIntl(<LeveyJenningsChart controlId={1} values={null} />);
    expect(screen.getByText("No QC data available")).toBeTruthy();
  });

  test("renders chart after data loads", () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        callback(mockChartData);
      },
    );

    renderWithIntl(
      <LeveyJenningsChart controlId={1} values={[98, 102, 100]} />,
    );
    expect(screen.getByTestId("line-chart")).toBeTruthy();
    expect(screen.getByText("Levey-Jennings Control Chart")).toBeTruthy();
  });

  test("calls API with correct endpoint", () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        callback(mockChartData);
      },
    );

    renderWithIntl(<LeveyJenningsChart controlId={42} values={[100]} />);
    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/qc/controls/42/chart-data",
      expect.any(String),
      expect.any(Function),
    );
  });

  test("renders chart with correct number of data points", () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        callback(mockChartData);
      },
    );

    renderWithIntl(
      <LeveyJenningsChart controlId={1} values={[98, 102, 100]} />,
    );
    // 3 data points * 6 lines (value + mean + ±2SD + ±3SD) = 18
    expect(screen.getByText("18 data points")).toBeTruthy();
  });
});
