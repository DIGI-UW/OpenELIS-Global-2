import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import PostSavePrintDialog from "./PostSavePrintDialog";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("PostSavePrintDialog", () => {
  test("renders dialog rows for printable label types and done action", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-001"
        printableLabelTypes={[
          { labelType: "order", quantity: 2, dimensionsMm: "25 x 50" },
          { labelType: "specimen", quantity: 1, dimensionsMm: "12 x 30" },
        ]}
        onDone={() => {}}
      />,
    );

    expect(screen.getByText(/LAB-001/)).toBeInTheDocument();
    expect(screen.getByText("Order label")).toBeInTheDocument();
    expect(screen.getByText("Specimen label")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Print" })).toHaveLength(2);
    expect(screen.getByRole("button", { name: "Done" })).toBeInTheDocument();
  });

  test("hides the done action when no onDone is provided", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-003"
        printableLabelTypes={[{ labelType: "order", quantity: 1 }]}
      />,
    );

    expect(screen.queryByRole("button", { name: "Done" })).toBeNull();
  });

  test("invokes callbacks for print and done actions", () => {
    const onPrint = vi.fn();
    const onDone = vi.fn();

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-002"
        printableLabelTypes={[{ labelType: "order", quantity: 1 }]}
        onPrint={onPrint}
        onDone={onDone}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));
    fireEvent.click(screen.getByRole("button", { name: "Done" }));

    expect(onPrint).toHaveBeenCalledWith("order", 1);
    expect(onDone).toHaveBeenCalled();
  });

  test("does not offer print actions before accession number is assigned", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber=""
        printableLabelTypes={[{ labelType: "order", quantity: 1 }]}
      />,
    );

    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Done" })).toBeNull();
  });
});
