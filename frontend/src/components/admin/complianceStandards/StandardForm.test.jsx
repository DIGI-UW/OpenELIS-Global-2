import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) => callback([])),
    postToOpenElisServerJsonResponse: vi.fn(),
    putToOpenElisServer: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  };
});

import { NotificationContext } from "../../layout/Layout";
import StandardForm from "./StandardForm";

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
      >
        <StandardForm isNew onSaved={vi.fn()} onCancel={vi.fn()} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("Add Compliance Standard (OGC-1615 walk)", () => {
  it("marks every field Save needs as required", () => {
    const errors = vi.spyOn(console, "error");
    const { container } = renderForm();
    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes("Failed %s type"),
      ),
    ).toBe(false);
    errors.mockRestore();

    const marked = Array.from(container.querySelectorAll(".requiredlabel")).map(
      (marker) => marker.parentElement.textContent.replace(" *", "").trim(),
    );
    expect(marked).toEqual(
      expect.arrayContaining([
        expect.stringMatching(/Standard Name/),
        expect.stringMatching(/Issuing Body/),
        expect.stringMatching(/Regulation/),
        expect.stringMatching(/Version/),
        expect.stringMatching(/Effective Date/),
        expect.stringMatching(/Country/),
      ]),
    );
    expect(marked).toHaveLength(6);
    const announced = Array.from(
      container.querySelectorAll('[aria-required="true"]'),
    ).map((input) => input.id);
    expect(announced).toEqual(
      expect.arrayContaining([
        "std-name",
        "std-issuing-body",
        "std-reg-number",
        "std-version",
      ]),
    );
    expect(announced).toHaveLength(6);
  });

  it("takes a typed effective date without the calendar", () => {
    renderForm();
    for (const [label, value] of [
      ["Standard Name", "Water"],
      ["Issuing Body", "Ministry"],
      [/Regulation/, "PP-1"],
      ["Version", "2026"],
    ]) {
      fireEvent.change(screen.getByRole("textbox", { name: label }), {
        target: { value },
      });
    }
    fireEvent.change(screen.getByRole("combobox", { name: /Country/ }), {
      target: { value: "Uganda" },
    });
    const save = screen.getByRole("button", { name: "Save" });
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByRole("textbox", { name: /Effective Date/ }), {
      target: { value: "2026-01-01" },
    });

    expect(save).toBeEnabled();
  });
});
