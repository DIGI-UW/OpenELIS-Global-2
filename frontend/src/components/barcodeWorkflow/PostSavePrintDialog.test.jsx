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
  test("renders dialog rows for printable label types", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-001"
        printableLabelTypes={[
          { labelType: "order", quantity: 2, dimensionsMm: "25 x 50" },
          { labelType: "specimen", quantity: 1, dimensionsMm: "12 x 30" },
        ]}
      />,
    );

    expect(screen.getByText(/LAB-001/)).toBeInTheDocument();
    expect(screen.getByText("Order label")).toBeInTheDocument();
    expect(screen.getByText("Specimen label")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Print" })).toHaveLength(2);
  });

  test("invokes onPrint with labelType and quantity", () => {
    const onPrint = vi.fn();

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-002"
        printableLabelTypes={[{ labelType: "order", quantity: 1 }]}
        onPrint={onPrint}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(onPrint).toHaveBeenCalledWith("order", 1);
  });

  test("does not offer print actions before accession number is assigned", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber=""
        printableLabelTypes={[{ labelType: "order", quantity: 1 }]}
      />,
    );

    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
  });

  test("appends sampleNumber to specimen rows so the user can tell them apart", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-MULTI"
        printableLabelTypes={[
          { labelType: "specimen", quantity: 2, sampleNumber: 1 },
          { labelType: "specimen", quantity: 3, sampleNumber: 2 },
          { labelType: "specimen", quantity: 1, sampleNumber: 3 },
        ]}
      />,
    );

    expect(screen.getByText("Specimen label 1")).toBeInTheDocument();
    expect(screen.getByText("Specimen label 2")).toBeInTheDocument();
    expect(screen.getByText("Specimen label 3")).toBeInTheDocument();
  });

  test("renders sampleNumber 0 (boundary against truthy-check regression)", () => {
    // formatLabelName / rowKey use `!= null` rather than truthy so a future
    // 0-indexed payload doesn't silently render as a bare "Specimen label".
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-ZERO"
        printableLabelTypes={[
          { labelType: "specimen", quantity: 1, sampleNumber: 0 },
          { labelType: "specimen", quantity: 1, sampleNumber: 1 },
        ]}
      />,
    );

    expect(screen.getByText("Specimen label 0")).toBeInTheDocument();
    expect(screen.getByText("Specimen label 1")).toBeInTheDocument();
  });

  test("forwards the row quantity to onPrint", () => {
    const onPrint = vi.fn();

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-Q"
        printableLabelTypes={[
          { labelType: "order", quantity: 7 },
          { labelType: "specimen", quantity: 4, sampleNumber: 1 },
        ]}
        onPrint={onPrint}
      />,
    );

    const printButtons = screen.getAllByRole("button", { name: "Print" });
    fireEvent.click(printButtons[0]);
    fireEvent.click(printButtons[1]);

    expect(onPrint).toHaveBeenNthCalledWith(1, "order", 7);
    expect(onPrint).toHaveBeenNthCalledWith(2, "specimen", 4);
  });

  test("opens printUrl when no onPrint handler is wired", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-URL"
        printableLabelTypes={[
          {
            labelType: "order",
            quantity: 1,
            printUrl: "/LabelMakerServlet?labNo=LAB-URL&type=order&quantity=1",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(openSpy).toHaveBeenCalledWith(
      "/LabelMakerServlet?labNo=LAB-URL&type=order&quantity=1",
    );

    openSpy.mockRestore();
  });

  test("warns when the popup blocker swallows the print window", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-BLOCKED"
        printableLabelTypes={[
          {
            labelType: "order",
            quantity: 1,
            printUrl: "/LabelMakerServlet?labNo=LAB-BLOCKED",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(warnSpy).toHaveBeenCalled();

    openSpy.mockRestore();
    warnSpy.mockRestore();
  });

  test("invokes onPopupBlocked when the popup is blocked and the callback is wired", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    const onPopupBlocked = vi.fn();

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-BLOCKED-CB"
        printableLabelTypes={[
          {
            labelType: "specimen",
            quantity: 2,
            sampleNumber: 1,
            printUrl: "/LabelMakerServlet?labNo=LAB-BLOCKED-CB.1",
          },
        ]}
        onPopupBlocked={onPopupBlocked}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(onPopupBlocked).toHaveBeenCalledTimes(1);
    expect(onPopupBlocked.mock.calls[0][0]).toMatchObject({
      labelType: "specimen",
      sampleNumber: 1,
      quantity: 2,
    });

    openSpy.mockRestore();
    warnSpy.mockRestore();
  });
});
