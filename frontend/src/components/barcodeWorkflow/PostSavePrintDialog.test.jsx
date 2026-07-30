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

// Two persisted presets, each with a distinct saved quantity (the decrease-only
// ceiling) — the M6 preset-driven shape the rewritten dialog consumes.
const presetRows = () => [
  {
    presetId: 1,
    labelName: "Specimen 25x50",
    savedQty: 3,
    dimensionsMm: "50 × 25 mm",
    printUrl: "/api/OpenELIS-Global/api/barcode/print/77/1",
  },
  {
    presetId: 2,
    labelName: "Aliquot 12x30",
    savedQty: 5,
    dimensionsMm: "30 × 12 mm",
    printUrl: "/api/OpenELIS-Global/api/barcode/print/77/2",
  },
];

describe("PostSavePrintDialog", () => {
  test("renders one row per persisted preset, by name", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-001"
        printableLabelTypes={presetRows()}
      />,
    );

    expect(screen.getByText(/LAB-001/)).toBeInTheDocument();
    expect(screen.getByText("Specimen 25x50")).toBeInTheDocument();
    expect(screen.getByText("Aliquot 12x30")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Print" })).toHaveLength(2);
  });

  test("each quantity is an editable NumberInput (spinbutton), not static text", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-NUM"
        printableLabelTypes={presetRows()}
      />,
    );

    // Inversion guard: the OGC-284 dialog rendered "Quantity: N" as a <p>. If a
    // regression reverts to that, these spinbuttons disappear and the test fails.
    const spinbuttons = screen.getAllByRole("spinbutton");
    expect(spinbuttons).toHaveLength(2);

    // The pre-rewrite static "Quantity: 3" paragraph must NOT be present.
    expect(screen.queryByText(/Quantity:\s*3/)).toBeNull();
  });

  test("seeds each input to its saved quantity and enforces min=0 / max=savedQty", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-MINMAX"
        printableLabelTypes={presetRows()}
      />,
    );

    const specimenInput = screen.getByRole("spinbutton", {
      name: /Specimen 25x50/,
    });
    const aliquotInput = screen.getByRole("spinbutton", {
      name: /Aliquot 12x30/,
    });

    // Seeded to savedQty.
    expect(specimenInput).toHaveValue(3);
    expect(aliquotInput).toHaveValue(5);

    // Decrease-only ceiling + non-negative floor are expressed on the input.
    expect(specimenInput).toHaveAttribute("min", "0");
    expect(specimenInput).toHaveAttribute("max", "3");
    expect(aliquotInput).toHaveAttribute("min", "0");
    expect(aliquotInput).toHaveAttribute("max", "5");
  });

  test("clamps an over-max entry down to the saved quantity (decrease-only)", () => {
    const onPrint = vi.fn();
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-CLAMP"
        printableLabelTypes={presetRows()}
        onPrint={onPrint}
      />,
    );

    const specimenInput = screen.getByRole("spinbutton", {
      name: /Specimen 25x50/,
    });
    // The decrease-only ceiling is enforced two ways, both asserted here:
    //   1. Declaratively, via the input's max attribute (the browser/Carbon
    //      steppers cannot exceed it).
    //   2. Effectively, via clampToMax on the committed value — the quantity
    //      actually sent to print is capped at savedQty even if a raw typed
    //      entry momentarily exceeds it.
    expect(specimenInput).toHaveAttribute("max", "3");

    // Type above the saved 3; the committed print quantity must clamp to 3.
    fireEvent.change(specimenInput, { target: { value: "99" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Print" })[0]);
    expect(onPrint).toHaveBeenCalledWith(
      1,
      3,
      expect.objectContaining({ presetId: 1 }),
    );
  });

  test("forwards a decreased quantity to onPrint", () => {
    const onPrint = vi.fn();
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-DEC"
        printableLabelTypes={presetRows()}
        onPrint={onPrint}
      />,
    );

    const aliquotInput = screen.getByRole("spinbutton", {
      name: /Aliquot 12x30/,
    });
    fireEvent.change(aliquotInput, { target: { value: "2" } });
    expect(aliquotInput).toHaveValue(2);

    fireEvent.click(screen.getAllByRole("button", { name: "Print" })[1]);
    expect(onPrint).toHaveBeenCalledWith(
      2,
      2,
      expect.objectContaining({ presetId: 2 }),
    );
  });

  test("shows the Skip — Print Later action when onSkip is wired", () => {
    const onSkip = vi.fn();
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-SKIP"
        printableLabelTypes={presetRows()}
        onSkip={onSkip}
      />,
    );

    const skip = screen.getByRole("button", { name: "Skip — Print Later" });
    expect(skip).toBeInTheDocument();

    fireEvent.click(skip);
    expect(onSkip).toHaveBeenCalledTimes(1);
  });

  test("omits the Skip action when no onSkip handler is provided", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-NOSKIP"
        printableLabelTypes={presetRows()}
      />,
    );

    expect(
      screen.queryByRole("button", { name: "Skip — Print Later" }),
    ).toBeNull();
  });

  test("does not render before an accession number is assigned", () => {
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber=""
        printableLabelTypes={presetRows()}
        onSkip={() => {}}
      />,
    );

    expect(screen.queryByRole("button", { name: "Print" })).toBeNull();
    expect(
      screen.queryByRole("button", { name: "Skip — Print Later" }),
    ).toBeNull();
  });

  test("opens the snapshot reprint URL when no onPrint handler is wired", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-URL"
        printableLabelTypes={[
          {
            presetId: 9,
            labelName: "Order",
            savedQty: 1,
            printUrl: "/api/OpenELIS-Global/api/barcode/print/77/9",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(openSpy).toHaveBeenCalledWith(
      "/api/OpenELIS-Global/api/barcode/print/77/9",
    );

    openSpy.mockRestore();
  });

  test("rewrites the quantity in a legacy print URL to the decreased value", () => {
    // The legacy LabelMakerServlet URL carries &quantity=N. Decreasing the
    // NumberInput must actually lower what prints — otherwise the control is a
    // no-op for the (currently only) live print path.
    const openSpy = vi.spyOn(window, "open").mockReturnValue({});

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-LEGACY-QTY"
        printableLabelTypes={[
          {
            labelType: "order",
            quantity: 7,
            printUrl:
              "/LabelMakerServlet?labNo=LAB-LEGACY-QTY&type=order&quantity=7",
          },
        ]}
      />,
    );

    const input = screen.getByRole("spinbutton");
    fireEvent.change(input, { target: { value: "3" } });
    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(openSpy).toHaveBeenCalledWith(
      "/LabelMakerServlet?labNo=LAB-LEGACY-QTY&type=order&quantity=3",
    );

    openSpy.mockRestore();
  });

  test("invokes onPopupBlocked when the print window is blocked", () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    const onPopupBlocked = vi.fn();

    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-BLOCKED"
        printableLabelTypes={[
          {
            presetId: 3,
            labelName: "Specimen",
            savedQty: 2,
            printUrl: "/api/OpenELIS-Global/api/barcode/print/77/3",
          },
        ]}
        onPopupBlocked={onPopupBlocked}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Print" }));

    expect(warnSpy).toHaveBeenCalled();
    expect(onPopupBlocked).toHaveBeenCalledTimes(1);
    expect(onPopupBlocked.mock.calls[0][0]).toMatchObject({ presetId: 3 });

    openSpy.mockRestore();
    warnSpy.mockRestore();
  });

  test("supports the legacy flat {labelType, quantity} row shape", () => {
    // ExistingOrder's reprint list still passes the OGC-284 shape; the dialog
    // degrades gracefully — labelType -> name, quantity -> savedQty ceiling.
    renderWithIntl(
      <PostSavePrintDialog
        accessionNumber="LAB-LEGACY"
        printableLabelTypes={[
          { labelType: "order", quantity: 4, sampleNumber: null },
          { labelType: "specimen", quantity: 1, sampleNumber: 2 },
        ]}
      />,
    );

    expect(screen.getByText("order")).toBeInTheDocument();
    // sampleNumber is appended so multi-specimen rows are distinguishable.
    expect(screen.getByText("specimen 2")).toBeInTheDocument();

    const inputs = screen.getAllByRole("spinbutton");
    expect(inputs).toHaveLength(2);
    expect(inputs[0]).toHaveValue(4);
    expect(inputs[0]).toHaveAttribute("max", "4");
  });
});
