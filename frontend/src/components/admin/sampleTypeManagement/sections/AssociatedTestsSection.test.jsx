import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AssociatedTestsSection from "./AssociatedTestsSection";
import messages from "../../../../languages/en.json";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

import {
  getFromOpenElisServer,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../../utils/Utils";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AssociatedTestsSection sampleTypeId="5" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("AllTestsForSampleTypeProvider")) {
      cb({ tests: [{ id: "10", name: "Glucose (Serum)", isActive: true }] });
    } else if (url.includes("/associable-tests")) {
      cb([
        { id: "20", name: "Sodium (Serum)", domain: "CLINICAL", active: true },
        { id: "22", name: "Urea (Serum)", domain: "CLINICAL", active: true },
      ]);
    } else if (url.endsWith("/rest/sample-types")) {
      cb({
        success: true,
        data: [
          { id: "5", name: "Serum" },
          { id: "6", name: "Plasma" },
        ],
      });
    }
  });
});

describe("AssociatedTestsSection", () => {
  it("lists linked tests and exposes the add autocomplete + sample-type filter", async () => {
    renderSection();
    expect(await screen.findByText("Glucose (Serum)")).toBeInTheDocument();
    expect(document.getElementById("assoc-test-combo")).toBeInTheDocument();
    expect(
      document.getElementById("assoc-test-sampletype-filter"),
    ).toBeInTheDocument();
  });

  it("adds a test through the autocomplete via PUT", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(200));
    renderSection();
    await screen.findByText("Glucose (Serum)");

    // open the autocomplete and pick a candidate
    const combo = document.getElementById("assoc-test-combo");
    fireEvent.click(combo);
    fireEvent.click(await screen.findByText("Sodium (Serum)"));

    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/5/tests/20",
      expect.any(String),
      expect.any(Function),
    );
  });

  it("narrows candidates by the selected sample-type filter", async () => {
    renderSection();
    await screen.findByText("Glucose (Serum)");

    fireEvent.change(document.getElementById("assoc-test-sampletype-filter"), {
      target: { value: "6" },
    });

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/5/associable-tests?sampleTypeFilter=6",
      expect.any(Function),
    );
  });

  it("removes a linked test via DELETE", async () => {
    deleteFromOpenElisServer.mockImplementation((_url, cb) => cb(200));
    renderSection();
    await screen.findByText("Glucose (Serum)");

    fireEvent.click(screen.getByRole("button", { name: /Remove test/i }));

    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/5/tests/10",
      expect.any(Function),
    );
  });
});
