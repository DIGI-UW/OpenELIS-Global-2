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
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LinkReagentModal from "./LinkReagentModal";
import { getFromOpenElisServer } from "../../../utils/Utils";
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
        { id: 7, name: "Glucose Reagent" },
        { id: 9, name: "Buffer Solution" },
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
      cb([{ id: 7, name: "Glucose Reagent" }]),
    );
    renderModal({ linkedReagentIds: [7] });
    expect(
      await screen.findByText(
        messages["label.testCatalog.reagents.modal.allLinked"],
      ),
    ).toBeInTheDocument();
  });
});
