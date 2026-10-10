/**
 * LinkReagentModal — OGC-949 M15 / OGC-992.
 *
 * Multi-select of reagent inventory minus already-linked reagents; "Link
 * Selected" creates one link per selection with default usage_type=PRIMARY.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LinkReagentModal from "./LinkReagentModal";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderModal = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LinkReagentModal
        open
        onClose={() => {}}
        testId="42"
        linkedReagentIds={[]}
        onLinked={() => {}}
        {...props}
      />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LinkReagentModal", () => {
  it("shows the reagent multi-select populated from inventory", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([
        { id: 7, name: "Glucose Reagent", itemType: "REAGENT" },
        { id: 9, name: "Buffer Solution", itemType: "REAGENT" },
      ]),
    );
    renderModal();
    expect(
      await screen.findByText(
        messages["label.testCatalog.reagents.modal.title"],
      ),
    ).toBeInTheDocument();
    // The multi-select's label renders (reagents are available, not all linked).
    expect(
      screen.getByText(
        messages["label.testCatalog.reagents.modal.select.label"],
      ),
    ).toBeInTheDocument();
  });

  it("excludes already-linked reagents and shows the all-linked notice", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ id: 7, name: "Glucose Reagent", itemType: "REAGENT" }]),
    );
    renderModal({ linkedReagentIds: [7] });
    expect(
      await screen.findByText(
        messages["label.testCatalog.reagents.modal.allLinked"],
      ),
    ).toBeInTheDocument();
  });

  it("a reagent another admin linked meanwhile (409) counts as linked, not as a retry (OGC-1461)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([
        { id: 7, name: "Glucose Reagent", itemType: "REAGENT" },
        { id: 9, name: "Buffer Solution", itemType: "REAGENT" },
      ]),
    );
    postToOpenElisServer.mockImplementation((url, body, cb) =>
      cb(JSON.parse(body).reagentId === 9 ? 409 : 201),
    );
    const onClose = vi.fn();
    const onLinked = vi.fn();
    renderModal({ onLinked, onClose });
    await screen.findByText(messages["label.testCatalog.reagents.modal.title"]);

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /open/i }));
    await user.click(
      await screen.findByRole("option", { name: /Glucose Reagent/ }),
    );
    await user.click(screen.getByRole("option", { name: /Buffer Solution/ }));
    await user.click(screen.getByRole("button", { name: /link selected/i }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(onLinked).toHaveBeenCalled();
    expect(
      screen.queryByText(messages["label.testCatalog.reagents.linkError"]),
    ).toBeNull();
  });

  it("after a partial failure keeps only the reagent that failed, so Retry finishes the job (OGC-1461)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([
        { id: 7, name: "Glucose Reagent", itemType: "REAGENT" },
        { id: 9, name: "Buffer Solution", itemType: "REAGENT" },
      ]),
    );
    postToOpenElisServer.mockImplementation((url, body, cb) =>
      cb(JSON.parse(body).reagentId === 9 ? 500 : 201),
    );
    const onClose = vi.fn();
    let rerenderWith = () => {};
    let reloaded = false;
    const onLinked = vi.fn(() =>
      setTimeout(() => {
        rerenderWith([7]);
        reloaded = true;
      }, 0),
    );
    const { rerender } = renderModal({ onLinked, onClose });
    rerenderWith = (linkedReagentIds) =>
      rerender(
        <IntlProvider locale="en" messages={messages}>
          <LinkReagentModal
            open
            onClose={onClose}
            testId="42"
            linkedReagentIds={linkedReagentIds}
            onLinked={onLinked}
          />
        </IntlProvider>,
      );
    await screen.findByText(messages["label.testCatalog.reagents.modal.title"]);

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /open/i }));
    await user.click(
      await screen.findByRole("option", { name: /Glucose Reagent/ }),
    );
    await user.click(screen.getByRole("option", { name: /Buffer Solution/ }));
    const submit = screen.getByRole("button", { name: /link selected/i });
    await user.click(submit);
    await waitFor(() => expect(postToOpenElisServer).toHaveBeenCalledTimes(2));

    expect(
      await screen.findByText(messages["label.testCatalog.reagents.linkError"]),
    ).toBeInTheDocument();
    expect(onLinked).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(reloaded).toBe(true));
    expect(
      screen.getByText(messages["label.testCatalog.reagents.linkError"]),
    ).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();

    postToOpenElisServer.mockClear();
    postToOpenElisServer.mockImplementation((url, body, cb) => cb(201));
    await user.click(submit);

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(postToOpenElisServer).toHaveBeenCalledTimes(1);
    expect(JSON.parse(postToOpenElisServer.mock.calls[0][1]).reagentId).toBe(9);
  });
});

it("offers tagged media of any inventory type while excluding unrelated inventory", async () => {
  getFromOpenElisServer.mockImplementation((url, cb) =>
    cb([
      {
        id: 1,
        name: "Tagged agar",
        itemType: "OTHER",
        microbiologyMedium: true,
      },
      {
        id: 2,
        name: "Unrelated gloves",
        itemType: "OTHER",
        microbiologyMedium: false,
      },
    ]),
  );
  renderModal();
  fireEvent.click(await screen.findByRole("combobox"));
  expect(await screen.findByText("Tagged agar")).toBeInTheDocument();
  expect(screen.queryByText("Unrelated gloves")).toBeNull();
});
