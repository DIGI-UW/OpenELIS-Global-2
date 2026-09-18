import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ManageTagsModal from "./ManageTagsModal";
import { InventoryTagAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryTagAPI: {
    getDirectory: vi.fn(),
    create: vi.fn(),
    deactivate: vi.fn(),
    activate: vi.fn(),
  },
}));

const DIRECTORY = [
  { name: "Cartridge", itemCount: 3, active: true },
  { name: "Glove", itemCount: 1, active: true },
  { name: "Gloves", itemCount: 0, active: false },
];

const renderModal = async (props = {}) => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <ManageTagsModal open onClose={vi.fn()} onSave={vi.fn()} {...props} />
    </IntlProvider>,
  );
  await screen.findByText("Cartridge");
  return view;
};

const rowNamed = (name) => screen.getByText(name).closest("tr");

beforeEach(() => {
  vi.clearAllMocks();
  InventoryTagAPI.getDirectory.mockResolvedValue(DIRECTORY);
  InventoryTagAPI.create.mockResolvedValue({});
  InventoryTagAPI.deactivate.mockResolvedValue([]);
  InventoryTagAPI.activate.mockResolvedValue([]);
});

describe("ManageTagsModal", () => {
  it("lists each tag with how many items carry it", async () => {
    await renderModal();

    const row = rowNamed("Cartridge");
    expect(row).toHaveTextContent("3");
    expect(row).toHaveTextContent("Active");
  });

  /** The whole point of the panel: duplicates have to be visible side by side. */
  it("hides deactivated tags until asked for them", async () => {
    await renderModal();

    expect(screen.queryByText("Gloves")).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Show deactivated"));

    expect(screen.getByText("Gloves")).toBeInTheDocument();
    expect(rowNamed("Gloves")).toHaveTextContent("Deactivated");
  });

  it("offers to retire an active tag and to bring back a retired one", async () => {
    await renderModal();
    fireEvent.click(screen.getByLabelText("Show deactivated"));

    expect(rowNamed("Glove")).toHaveTextContent("Deactivate");
    expect(rowNamed("Gloves")).toHaveTextContent("Reactivate");
  });

  it("retires a tag by name", async () => {
    await renderModal();

    fireEvent.click(
      within(rowNamed("Cartridge")).getByRole("button", {
        name: "Deactivate",
      }),
    );

    await waitFor(() =>
      expect(InventoryTagAPI.deactivate).toHaveBeenCalledWith("Cartridge"),
    );
  });

  it("brings a retired tag back by name", async () => {
    await renderModal();
    fireEvent.click(screen.getByLabelText("Show deactivated"));

    fireEvent.click(
      within(rowNamed("Gloves")).getByRole("button", { name: "Reactivate" }),
    );

    await waitFor(() =>
      expect(InventoryTagAPI.activate).toHaveBeenCalledWith("Gloves"),
    );
  });

  it("creates a tag ahead of use and clears the field", async () => {
    await renderModal();
    const field = screen.getByLabelText("New tag");

    fireEvent.change(field, { target: { value: "Cold chain" } });
    fireEvent.click(screen.getByRole("button", { name: "Add tag" }));

    await waitFor(() =>
      expect(InventoryTagAPI.create).toHaveBeenCalledWith("Cold chain"),
    );
    await waitFor(() => expect(field).toHaveValue(""));
  });

  it("refuses to add a blank tag", async () => {
    await renderModal();

    fireEvent.change(screen.getByLabelText("New tag"), {
      target: { value: "   " },
    });

    expect(screen.getByRole("button", { name: "Add tag" })).toBeDisabled();
  });

  /**
   * Enter in the new-tag field means "add this tag". A dialog that reads it as
   * submit closes on the keystroke instead — the failure the item editor hit.
   */
  it("adds on Enter without closing the dialog", async () => {
    const onClose = vi.fn();
    await renderModal({ onClose });

    fireEvent.change(screen.getByLabelText("New tag"), {
      target: { value: "Cold chain" },
    });
    fireEvent.keyDown(screen.getByLabelText("New tag"), {
      key: "Enter",
      code: "Enter",
    });

    await waitFor(() =>
      expect(InventoryTagAPI.create).toHaveBeenCalledWith("Cold chain"),
    );
    expect(onClose).not.toHaveBeenCalled();
  });

  it("tells the board to reload only when something actually changed", async () => {
    const onSave = vi.fn();
    const onClose = vi.fn();
    const { rerender } = await renderModal({ onSave, onClose });

    // Closing without touching anything must not make the board refetch.
    fireEvent.click(screen.getByRole("button", { name: /close/i }));
    expect(onSave).not.toHaveBeenCalled();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <ManageTagsModal open onClose={onClose} onSave={onSave} />
      </IntlProvider>,
    );
    await screen.findByText("Cartridge");
    fireEvent.click(
      within(rowNamed("Cartridge")).getByRole("button", {
        name: "Deactivate",
      }),
    );
    await waitFor(() => expect(InventoryTagAPI.deactivate).toHaveBeenCalled());
    fireEvent.click(screen.getByRole("button", { name: /close/i }));

    expect(onSave).toHaveBeenCalled();
  });

  /**
   * The row being retired leaves the visible list on the very next render,
   * because "show deactivated" is off. Any render that has to look the row back
   * up by id — which is what Carbon's DataTable forces — crashes here. The
   * directory has to survive its own list getting shorter.
   */
  it("survives a tag leaving the list it was retired from", async () => {
    await renderModal();
    InventoryTagAPI.getDirectory.mockResolvedValue([
      { name: "Cartridge", itemCount: 3, active: false },
      { name: "Glove", itemCount: 1, active: true },
      { name: "Gloves", itemCount: 0, active: false },
    ]);

    fireEvent.click(
      within(rowNamed("Cartridge")).getByRole("button", {
        name: "Deactivate",
      }),
    );

    await waitFor(() =>
      expect(screen.queryByText("Cartridge")).not.toBeInTheDocument(),
    );
    // Still rendering, not blown up: the one remaining active tag is there.
    expect(screen.getByText("Glove")).toBeInTheDocument();
  });

  it("shows a retired tag again as soon as deactivated ones are revealed", async () => {
    await renderModal();
    InventoryTagAPI.getDirectory.mockResolvedValue([
      { name: "Cartridge", itemCount: 3, active: false },
      { name: "Glove", itemCount: 1, active: true },
    ]);

    fireEvent.click(
      within(rowNamed("Cartridge")).getByRole("button", {
        name: "Deactivate",
      }),
    );
    await waitFor(() =>
      expect(screen.queryByText("Cartridge")).not.toBeInTheDocument(),
    );

    fireEvent.click(screen.getByLabelText("Show deactivated"));

    expect(rowNamed("Cartridge")).toHaveTextContent("Deactivated");
    expect(rowNamed("Cartridge")).toHaveTextContent("Reactivate");
    expect(rowNamed("Cartridge")).toHaveTextContent("3");
  });

  it("says what to do when there are no tags at all", async () => {
    InventoryTagAPI.getDirectory.mockResolvedValue([]);
    render(
      <IntlProvider locale="en" messages={messages}>
        <ManageTagsModal open onClose={vi.fn()} onSave={vi.fn()} />
      </IntlProvider>,
    );

    expect(
      await screen.findByText(
        "No tags yet. Add one here, or tag an item directly.",
      ),
    ).toBeInTheDocument();
  });
});
