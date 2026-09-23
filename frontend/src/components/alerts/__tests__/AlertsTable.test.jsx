import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AlertsTable from "../AlertsTable";
import messages from "../../../languages/en.json";

/**
 * The Created column, against the shape the server actually sends.
 *
 * The API writes an alert's start time as a decimal number of epoch seconds —
 * Jackson's default for an OffsetDateTime — and `new Date(n)` reads a number as
 * milliseconds, so every alert on this screen was dated to January 1970. The
 * existing fixtures all use ISO strings, which is why nothing caught it: they
 * are not what the wire carries.
 */
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

const alert = (startTime) => [
  {
    id: 1,
    alertType: "INVENTORY_LOW",
    severity: "WARNING",
    status: "OPEN",
    message: "Cartridge is at or below its reorder threshold",
    startTime,
  },
];

describe("AlertsTable — the Created column", () => {
  it("reads the numeric start time the server sends as epoch seconds", () => {
    // 2026-09-18T14:51:51Z, as the API writes it
    renderTable(alert(1789743111.77328));

    expect(screen.getByText(/2026/)).toBeInTheDocument();
    expect(screen.queryByText(/1970/)).not.toBeInTheDocument();
  });

  it("still reads an ISO string, so a change of wire format does not break it", () => {
    renderTable(alert("2026-01-15T10:00:00Z"));

    expect(screen.getByText(/2026/)).toBeInTheDocument();
  });

  it("shows nothing rather than an epoch date when there is no start time", () => {
    renderTable(alert(null));

    expect(screen.queryByText(/1970/)).not.toBeInTheDocument();
  });

  it("renders the inventory alert type with its own label", () => {
    renderTable(alert(1789743111.77328));

    expect(screen.getByText("Low stock")).toBeInTheDocument();
  });
});
