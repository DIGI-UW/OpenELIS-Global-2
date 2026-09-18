import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ImportItemsModal from "./ImportItemsModal";
import { InventoryImportAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryImportAPI: {
    preview: vi.fn(),
    apply: vi.fn(),
    templateUrl: () => "/rest/inventory/import/template",
  },
}));

const renderModal = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ImportItemsModal
        open
        onClose={vi.fn()}
        onImported={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

/** A File whose text() resolves, which is how the modal reads it. */
const csvFile = (text, name = "items.csv") => ({
  name,
  text: () => Promise.resolve(text),
});

const addFile = async (file) => {
  const input = document.querySelector('input[type="file"]');
  Object.defineProperty(input, "files", { value: [file], configurable: true });
  fireEvent.change(input);
  await waitFor(() => expect(screen.getByText(file.name)).toBeInTheDocument());
};

const plan = (overrides = {}) => ({
  created: 2,
  updated: 0,
  unchanged: 0,
  skipped: 0,
  rows: [
    { lineNumber: 2, name: "Malaria RDT", outcome: "CREATE", reason: "" },
    { lineNumber: 3, name: "HIV kit", outcome: "CREATE", reason: "" },
  ],
  ...overrides,
});

// The modal's own close control also answers to /close/i, so the footer's
// primary button is selected by position rather than by name.
const primaryButton = () =>
  document.querySelector(".cds--modal-footer .cds--btn--primary");

beforeEach(() => {
  vi.clearAllMocks();
  InventoryImportAPI.preview.mockResolvedValue(plan());
  InventoryImportAPI.apply.mockResolvedValue(plan());
});

describe("ImportItemsModal", () => {
  it("cannot do anything until a file is chosen", async () => {
    renderModal();

    expect(primaryButton()).toBeDisabled();
  });

  /**
   * The whole point of the card: nothing is written until somebody has seen what
   * would happen and confirmed it.
   */
  it("previews first, and writes nothing on that step", async () => {
    renderModal();
    await addFile(csvFile("name,units\nMalaria RDT,tests\n"));

    fireEvent.click(primaryButton());

    await waitFor(() => expect(InventoryImportAPI.preview).toHaveBeenCalled());
    expect(InventoryImportAPI.apply).not.toHaveBeenCalled();
  });

  it("applies only on a second press, and sends the same text it previewed", async () => {
    const csv = "name,units\nMalaria RDT,tests\n";
    renderModal();
    await addFile(csvFile(csv));

    fireEvent.click(primaryButton());
    await waitFor(() =>
      expect(screen.getByText(/what this file would do/i)).toBeInTheDocument(),
    );
    fireEvent.click(primaryButton());

    await waitFor(() => expect(InventoryImportAPI.apply).toHaveBeenCalled());
    expect(InventoryImportAPI.apply.mock.calls[0][0]).toBe(csv);
    expect(InventoryImportAPI.preview.mock.calls[0][0]).toBe(csv);
  });

  it("warns about rows it cannot use, and still offers to apply the rest", async () => {
    InventoryImportAPI.preview.mockResolvedValue(
      plan({
        created: 1,
        skipped: 1,
        rows: [
          { lineNumber: 2, name: "Malaria RDT", outcome: "CREATE", reason: "" },
          {
            lineNumber: 3,
            name: "Broken",
            outcome: "SKIP",
            reason: "reorder threshold must be a whole number, was 'x'",
          },
        ],
      }),
    );
    renderModal();
    await addFile(csvFile("name,units\nMalaria RDT,tests\n"));

    fireEvent.click(primaryButton());

    expect(
      await screen.findByText(/some rows cannot be used/i),
    ).toBeInTheDocument();
    // One bad row used to block the eleven good ones behind it, even though the
    // server evaluates and commits each row on its own.
    expect(primaryButton()).toBeEnabled();

    InventoryImportAPI.apply.mockResolvedValue(
      plan({ created: 1, skipped: 1, rows: [] }),
    );
    fireEvent.click(primaryButton());

    await waitFor(() => expect(InventoryImportAPI.apply).toHaveBeenCalled());
  });

  it("shows the reason and the line number for a row it cannot use", async () => {
    InventoryImportAPI.preview.mockResolvedValue(
      plan({
        created: 0,
        skipped: 1,
        rows: [
          {
            lineNumber: 7,
            name: "Broken",
            outcome: "SKIP",
            reason: "A row needs units",
          },
        ],
      }),
    );
    renderModal();
    await addFile(csvFile("x"));

    fireEvent.click(primaryButton());

    expect(await screen.findByText("A row needs units")).toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
  });

  /**
   * Applying one file against another file's preview is exactly what the confirm
   * step exists to prevent.
   */
  it("drops the plan when a different file is chosen", async () => {
    renderModal();
    await addFile(csvFile("name,units\nA,tests\n", "first.csv"));
    fireEvent.click(primaryButton());
    await waitFor(() =>
      expect(screen.getByText(/what this file would do/i)).toBeInTheDocument(),
    );

    await addFile(csvFile("name,units\nB,tests\n", "second.csv"));

    expect(
      screen.queryByText(/what this file would do/i),
    ).not.toBeInTheDocument();
    expect(primaryButton()).toHaveTextContent(/preview/i);
  });

  it("says so when a file would change nothing, rather than offering to apply it", async () => {
    InventoryImportAPI.preview.mockResolvedValue(
      plan({
        created: 0,
        unchanged: 2,
        rows: [
          {
            lineNumber: 2,
            name: "Malaria RDT",
            outcome: "UNCHANGED",
            reason: "",
          },
          { lineNumber: 3, name: "HIV kit", outcome: "UNCHANGED", reason: "" },
        ],
      }),
    );
    renderModal();
    await addFile(csvFile("x"));

    fireEvent.click(primaryButton());

    expect(
      await screen.findByText(/would change nothing/i),
    ).toBeInTheDocument();
    expect(primaryButton()).toBeDisabled();
  });

  it("tells the board to refresh once items have been imported", async () => {
    const onImported = vi.fn();
    renderModal({ onImported });
    await addFile(csvFile("name,units\nA,tests\n"));
    fireEvent.click(primaryButton());
    await waitFor(() =>
      expect(screen.getByText(/what this file would do/i)).toBeInTheDocument(),
    );

    fireEvent.click(primaryButton());

    await waitFor(() => expect(onImported).toHaveBeenCalled());
    expect(await screen.findByText(/what was imported/i)).toBeInTheDocument();
  });

  it("never parses the file itself, so the preview cannot disagree with the commit", async () => {
    const csv = 'name,units\n"Gloves, examination",boxes\n';
    renderModal();
    await addFile(csvFile(csv));

    fireEvent.click(primaryButton());

    await waitFor(() => expect(InventoryImportAPI.preview).toHaveBeenCalled());
    // The quoted comma survives because nothing here split on one.
    expect(InventoryImportAPI.preview.mock.calls[0][0]).toBe(csv);
  });
});
