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

const DOMAINS = [
  { id: "CLINICAL", labelKey: "label.domain.CLINICAL" },
  { id: "ENVIRONMENTAL", labelKey: "label.domain.ENVIRONMENTAL" },
  { id: "VECTOR", labelKey: "label.domain.VECTOR" },
];

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AssociatedTestsSection sampleTypeId="5" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.endsWith("/domains")) {
      cb(DOMAINS);
    } else if (url.includes("AllTestsForSampleTypeProvider")) {
      cb({ tests: [{ id: "10", name: "Glucose (Serum)", isActive: true }] });
    } else if (url.includes("/associable-tests")) {
      cb([
        { id: "20", name: "Sodium (Serum)", domain: "CLINICAL", active: true },
        {
          id: "21",
          name: "Lead (Water)",
          domain: "ENVIRONMENTAL",
          active: true,
        },
      ]);
    }
  });
});

describe("AssociatedTestsSection", () => {
  it("lists linked tests and offers candidates to add", async () => {
    renderSection();
    expect(await screen.findByText("Glucose (Serum)")).toBeInTheDocument();
    // candidate picker holds the associable tests
    const picker = document.getElementById("assoc-test-picker");
    expect(picker).toBeInTheDocument();
    expect(screen.getByText("Sodium (Serum)")).toBeInTheDocument();
  });

  it("adds a test to the sample type via PUT and reloads", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(200));
    renderSection();
    await screen.findByText("Glucose (Serum)");

    fireEvent.change(document.getElementById("assoc-test-picker"), {
      target: { value: "20" },
    });
    fireEvent.click(document.getElementById("assoc-test-add"));

    expect(putToOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/5/tests/20",
      expect.any(String),
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
