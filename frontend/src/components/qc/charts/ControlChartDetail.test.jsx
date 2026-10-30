import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import ControlChartDetail from "./ControlChartDetail";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  toLocalIsoDate: (date) => date.toISOString().slice(0, 10),
}));
// Keep Carbon's real filters and tabs; expose chart inputs instead of SVG layout.
vi.mock("./LeveyJenningsChart", () => ({
  default: ({ data }) => (
    <div>{data.map((point) => point.value).join(", ")}</div>
  ),
}));

const lots = [
  {
    id: "normal",
    lotNumber: "SHARED",
    controlLevel: "NORMAL",
    testId: "test-a",
    productName: "Glucose control",
  },
  {
    id: "high",
    lotNumber: "SHARED",
    controlLevel: "HIGH",
    testId: "test-a",
    productName: "Glucose control",
  },
];
beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("controlLots?")) return callback(lots);
    if (url.includes("/analyzer/analyzers/"))
      return callback({ name: "Analyzer" });
    if (url.endsWith("/statistics"))
      return callback({ mean: url.includes("/high/") ? 100 : 10 });
    const high = url.includes("/charts/high");
    callback({
      dataPoints: [
        {
          resultId: high ? "high-result" : "normal-result",
          value: high ? 102 : 9,
          controlLevel: high ? "HIGH" : "NORMAL",
        },
      ],
    });
  });
});
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={["/chart/42"]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/chart/:analyzerId">
          <ControlChartDetail />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );
it("switches matching lots and statistics when changing the control-level tab", async () => {
  renderPage();
  expect(await screen.findByTestId("chart-panel-NORMAL")).toHaveTextContent(
    "9",
  );
  expect(screen.getByRole("tab", { name: "Normal" })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  await userEvent.click(screen.getByRole("tab", { name: "High" }));
  expect(await screen.findByTestId("chart-panel-HIGH")).toHaveTextContent(
    "102",
  );
  expect(screen.getByTestId("chart-filter-control-lot")).toHaveTextContent(
    "SHARED — High — Glucose control",
  );
  expect(screen.getByTestId("control-chart-detail-stats")).toHaveTextContent(
    "100.00",
  );
  await userEvent.click(screen.getByRole("tab", { name: "Low" }));
  expect(screen.getByTestId("chart-panel-LOW")).not.toHaveTextContent("102");
  expect(
    screen.queryByTestId("control-chart-detail-stats"),
  ).not.toBeInTheDocument();
});
it("distinguishes shared lot numbers and aligns the tab when selecting a lot", async () => {
  renderPage();
  const dropdown = await screen.findByTestId("chart-filter-control-lot");
  await userEvent.click(within(dropdown).getByRole("combobox"));
  expect(
    screen.getByRole("option", { name: "SHARED — Normal — Glucose control" }),
  ).toBeVisible();
  await userEvent.click(
    screen.getByRole("option", { name: "SHARED — High — Glucose control" }),
  );
  expect(screen.getByRole("tab", { name: "High" })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  expect(await screen.findByTestId("chart-panel-HIGH")).toHaveTextContent(
    "102",
  );
});
it("filters lots by level and clears the chart when that level has no lot", async () => {
  renderPage();
  const dropdown = await screen.findByTestId("chart-filter-control-level");
  await userEvent.click(within(dropdown).getByRole("combobox"));
  await userEvent.click(screen.getByRole("option", { name: "High" }));
  expect(await screen.findByTestId("chart-panel-single")).toHaveTextContent(
    "102",
  );
  await userEvent.click(
    within(screen.getByTestId("chart-filter-control-lot")).getByRole(
      "combobox",
    ),
  );
  expect(
    screen.queryByRole("option", { name: /Normal/ }),
  ).not.toBeInTheDocument();
  await userEvent.keyboard("{Escape}");
  await userEvent.click(within(dropdown).getByRole("combobox"));
  await userEvent.click(screen.getByRole("option", { name: "Low" }));
  expect(screen.getByTestId("chart-panel-single")).not.toHaveTextContent("102");
  expect(
    screen.queryByTestId("control-chart-detail-stats"),
  ).not.toBeInTheDocument();
});
