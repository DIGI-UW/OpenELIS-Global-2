import React from "react";
import { render, screen, within } from "@testing-library/react";
import { Button, Tag } from "@carbon/react";
import AnalyzerConfigTable from "./AnalyzerConfigTable";

describe("AnalyzerConfigTable", () => {
  test("renders Carbon table semantics and custom interactive cells", () => {
    render(
      <AnalyzerConfigTable
        headers={[
          { key: "code", header: "Analyzer code" },
          { key: "status", header: "Status" },
          { key: "actions", header: "Actions" },
        ]}
        rows={[
          {
            id: "code-1",
            code: "MTB",
            status: "Pending",
            actions: "resolve",
          },
        ]}
        tableLabel="Pending analyzer codes"
        testId="pending-codes-table"
        getRowTestId={(row) => `pending-row-${row.id}`}
        renderCell={(cell) => {
          if (cell.info.header === "status") {
            return <Tag type="warm-gray">{cell.value}</Tag>;
          }
          if (cell.info.header === "actions") {
            return <Button size="sm">Resolve</Button>;
          }
          return cell.value;
        }}
      />,
    );

    const table = screen.getByRole("table", {
      name: "Pending analyzer codes",
    });
    expect(table).toHaveClass("cds--data-table");
    expect(
      within(table).getByRole("columnheader", { name: "Analyzer code" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId("pending-row-code-1")).toHaveTextContent("MTB");
    expect(
      within(screen.getByTestId("pending-row-code-1")).getByRole("button", {
        name: "Resolve",
      }),
    ).toBeInTheDocument();
  });
});
