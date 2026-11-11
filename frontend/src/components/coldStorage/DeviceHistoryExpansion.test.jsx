import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import DeviceHistoryExpansion from "./DeviceHistoryExpansion";
import {
  fetchCorrectiveActions,
  fetchFilteredAlerts,
  fetchHistoricalReadings,
} from "./api";

vi.mock("./api", () => ({
  fetchCorrectiveActions: vi.fn(),
  fetchFilteredAlerts: vi.fn(),
  fetchHistoricalReadings: vi.fn(),
}));

vi.mock("@carbon/charts-react", () => ({
  LineChart: () => <div data-testid="line-chart" />,
}));

const DEVICE = { id: 47, name: "Demo Vaccine Fridge" };

const renderExpansion = (overrides = {}) =>
  render(
    <IntlProvider locale="en" messages={{ ...messages, ...overrides }}>
      <DeviceHistoryExpansion device={DEVICE} />
    </IntlProvider>,
  );

const openTrendsTab = async () => {
  fireEvent.click(await screen.findByText("Temperature Trends"));
};

describe("DeviceHistoryExpansion metric selector", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchCorrectiveActions.mockResolvedValue([]);
    fetchFilteredAlerts.mockResolvedValue([]);
    fetchHistoricalReadings.mockResolvedValue([]);
  });

  it("names each metric from the message bundle", async () => {
    renderExpansion();
    await openTrendsTab();

    fireEvent.click(screen.getByRole("combobox", { name: /Temperature/ }));

    expect(screen.getByText("Humidity")).toBeInTheDocument();
    expect(screen.getByText("Temperature (Probe 2)")).toBeInTheDocument();
  });

  it("renders the bundle's wording, not a hardcoded English label", async () => {
    renderExpansion({
      "coldStorage.trends.metric.temperature": "Température",
      "coldStorage.trends.metric.humidity": "Humidité",
      "coldStorage.trends.metric.temperature2": "Température (Sonde 2)",
    });
    await openTrendsTab();

    expect(screen.getByText("Température")).toBeInTheDocument();
    expect(screen.queryByText("Temperature")).not.toBeInTheDocument();
  });
});
