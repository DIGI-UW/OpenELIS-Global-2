/**
 * ReagentsSection — OGC-949 M15 / OGC-991 + OGC-992 + OGC-993.
 *
 * Linked-reagents table with current stock (OGC-991), Link Reagent button
 * (OGC-992), and inline edit + unlink confirmation (OGC-993). Reads/writes the
 * OGC-987 endpoints.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ReagentsSection from "./ReagentsSection";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const TWO_REAGENTS = [
  {
    id: "link-1",
    reagentId: 7,
    reagentName: "Glucose Reagent",
    manufacturer: "Acme",
    usageType: "PRIMARY",
    quantityPerTest: 2.5,
    quantityUnit: "mL",
    currentStock: 120,
    lowStockThreshold: 50,
  },
  {
    id: "link-2",
    reagentId: 9,
    reagentName: "Buffer Solution",
    manufacturer: "Acme",
    usageType: "SECONDARY",
    quantityPerTest: null,
    quantityUnit: null,
    currentStock: 0,
    lowStockThreshold: 10,
  },
];

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReagentsSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ReagentsSection", () => {
  it("renders a row per linked reagent with stock and editable usage type", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    renderSection();

    expect(await screen.findByText("Glucose Reagent")).toBeInTheDocument();
    expect(screen.getByText("Buffer Solution")).toBeInTheDocument();
    // Stock counts (rendered inside the stock Tag).
    expect(screen.getByText("120")).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
    // Usage type is an editable Select seeded from the server value.
    expect(document.getElementById("usage-link-1").value).toBe("PRIMARY");
    expect(document.getElementById("usage-link-2").value).toBe("SECONDARY");
    // Per-row unlink action.
    expect(screen.getByTestId("unlink-link-1")).toBeInTheDocument();
  });

  it("shows the empty state when no reagents are linked", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.loadError"]),
    ).toBeInTheDocument();
  });

  it("renders the Link Reagent button", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(
      await screen.findByTestId("link-reagent-button"),
    ).toBeInTheDocument();
  });

  // Edits are committed by Save, not by changing a field or leaving it: an admin
  // editing a row's usage type or quantity has to be able to commit deliberately.
  it("commits a usage-type change only when Save is clicked", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    putToOpenElisServer.mockImplementation((url, body, cb) => cb(200));
    renderSection();

    await screen.findByText("Glucose Reagent");
    const save = () => screen.getByTestId("reagents-save");
    expect(save()).toBeDisabled();

    fireEvent.change(document.getElementById("usage-link-1"), {
      target: { value: "SECONDARY" },
    });

    // Changing the field must not write on its own.
    expect(putToOpenElisServer).not.toHaveBeenCalled();
    expect(save()).toBeEnabled();
    expect(
      screen.getByText(messages["label.testCatalog.reagents.unsaved"]),
    ).toBeInTheDocument();

    fireEvent.click(save());

    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-catalog/42/reagents/7",
      expect.stringContaining('"usageType":"SECONDARY"'),
      expect.any(Function),
    );
    expect(
      await screen.findByText(messages["label.testCatalog.reagents.updated"]),
    ).toBeInTheDocument();
  });

  // OGC-1465 review: a unit longer than its column is marked and not sent.
  it("marks a unit over 50 characters and does not send it", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    renderSection();
    const unit = await screen.findByDisplayValue("mL");

    fireEvent.change(unit, { target: { value: "u".repeat(51) } });
    fireEvent.click(screen.getByTestId("reagents-save"));

    expect(
      (
        await screen.findAllByText(
          messages["label.testCatalog.reagents.unitTooLong"],
        )
      ).length,
    ).toBeGreaterThan(0);
    expect(putToOpenElisServer).not.toHaveBeenCalled();
  });

  it("keeps the typed quantity when the save fails, so a retry can send it (OGC-1461)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    putToOpenElisServer.mockImplementation((url, body, cb) => cb(500));
    renderSection();
    const qty = await screen.findByDisplayValue("2.5");

    fireEvent.change(qty, { target: { value: "3.75" } });
    fireEvent.click(screen.getByTestId("reagents-save"));

    expect(
      await screen.findByText(messages["label.testCatalog.reagents.saveError"]),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("3.75")).toBeInTheDocument();
    expect(screen.getByTestId("reagents-save")).toBeEnabled();
  });

  it("a row that failed to save keeps its edit when another row saved (OGC-1461)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    putToOpenElisServer.mockImplementation((url, body, cb) =>
      cb(url.endsWith("/reagents/7") ? 200 : 500),
    );
    renderSection();
    await screen.findByDisplayValue("2.5");

    fireEvent.change(document.getElementById("qty-link-1"), {
      target: { value: "3" },
    });
    fireEvent.change(document.getElementById("qty-link-2"), {
      target: { value: "4" },
    });
    fireEvent.click(screen.getByTestId("reagents-save"));

    expect(
      await screen.findByText(messages["label.testCatalog.reagents.saveError"]),
    ).toBeInTheDocument();
    expect(document.getElementById("qty-link-1")).toHaveValue(3);
    expect(document.getElementById("qty-link-2")).toHaveValue(4);
    expect(screen.getByTestId("reagents-save")).toBeEnabled();
  });

  it("a partly failed link keeps the dialog and its error while the list reloads (OGC-1461)", async () => {
    let linked = [TWO_REAGENTS[1]];
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/inventory/items") {
        cb([
          { id: 7, name: "Glucose Reagent" },
          { id: 9, name: "Buffer Solution" },
          { id: 11, name: "Lysis Buffer" },
        ]);
      } else {
        cb(linked);
      }
    });
    postToOpenElisServer.mockImplementation((url, body, cb) => {
      if (JSON.parse(body).reagentId === 7) {
        linked = [...linked, TWO_REAGENTS[0]];
        cb(201);
      } else {
        cb(500);
      }
    });
    renderSection();
    fireEvent.click(await screen.findByTestId("link-reagent-button"));

    const user = userEvent.setup();
    const linkDialog = () =>
      screen
        .getAllByRole("dialog")
        .find((d) =>
          within(d).queryByText(
            messages["label.testCatalog.reagents.modal.title"],
          ),
        );
    const dialog = linkDialog();
    await user.click(within(dialog).getByRole("button", { name: /open/i }));
    await user.click(
      await screen.findByRole("option", { name: /Glucose Reagent/ }),
    );
    await user.click(screen.getByRole("option", { name: /Lysis Buffer/ }));
    await user.click(
      within(dialog).getByRole("button", { name: /link selected/i }),
    );

    expect(
      await within(dialog).findByText(
        messages["label.testCatalog.reagents.linkError"],
      ),
    ).toBeInTheDocument();
    expect(screen.getByTestId("reagent-row-link-1")).toBeInTheDocument();
    expect(linkDialog()).toBe(dialog);
  });

  it("discards a pending edit on Cancel without writing", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    renderSection();

    await screen.findByText("Glucose Reagent");
    fireEvent.change(document.getElementById("usage-link-1"), {
      target: { value: "SECONDARY" },
    });

    fireEvent.click(screen.getByTestId("reagents-cancel"));

    expect(putToOpenElisServer).not.toHaveBeenCalled();
    expect(document.getElementById("usage-link-1").value).toBe("PRIMARY");
    expect(screen.getByTestId("reagents-save")).toBeDisabled();
  });

  it("confirms unlink and calls DELETE", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(TWO_REAGENTS));
    deleteFromOpenElisServer.mockImplementation((url, cb) => cb(204));
    renderSection();

    await screen.findByText("Glucose Reagent");
    fireEvent.click(screen.getByTestId("unlink-link-1"));

    // Confirmation modal shows the reagent name in the warning copy.
    expect(
      await screen.findByText(/Unlink Glucose Reagent from this test/),
    ).toBeInTheDocument();

    // Click the danger primary button in the modal footer.
    const confirmBtn = document.querySelector(
      ".cds--modal-footer .cds--btn--danger",
    );
    fireEvent.click(confirmBtn);

    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-catalog/42/reagents/7",
      expect.any(Function),
    );
  });
});
