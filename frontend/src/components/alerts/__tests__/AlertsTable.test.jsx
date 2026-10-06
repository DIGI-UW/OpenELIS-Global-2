import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AlertsTable from "../AlertsTable";
import messages from "../../../languages/en.json";

const renderTable = (alerts) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AlertsTable
        alerts={alerts}
        totalCount={alerts.length}
        page={0}
        pageSize={25}
        onPageChange={vi.fn()}
        onAcknowledge={vi.fn()}
      />
    </IntlProvider>,
  );

const alert = () => [
  {
    id: 1,
    alertType: "INVENTORY_LOW",
    severity: "WARNING",
    status: "OPEN",
    message: "Cartridge is at or below its reorder threshold",
  },
];

describe("AlertsTable", () => {
  it("renders the inventory alert type with its own label", () => {
    renderTable(alert());

    expect(screen.getByText("Low stock")).toBeInTheDocument();
  });
});
