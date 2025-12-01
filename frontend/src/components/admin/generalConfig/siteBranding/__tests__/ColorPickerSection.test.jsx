/**
 * Unit tests for ColorPickerSection component
 * 
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Jest Best Practices: .specify/guides/jest-best-practices.md
 * 
 * Task Reference: T049
 */

// ========== MOCKS ==========

// ========== IMPORTS ==========

import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import ColorPickerSection from "../ColorPickerSection";
import messages from "../../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

// ========== TEST SUITE ==========

describe("ColorPickerSection", () => {
  /**
   * Test: Component renders with color picker
   * Task Reference: T049
   */
  test("renders color picker input and hex input field", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={jest.fn()}
      />
    );

    expect(screen.getByLabelText(/primary color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#1d4ed8")).toBeInTheDocument();
  });

  /**
   * Test: Color picker updates hex input
   * Task Reference: T049
   */
  test("updates hex input when color picker changes", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />
    );

    const colorInput = screen.getByLabelText(/primary color/i);
    await user.type(colorInput, "#ff0000");

    expect(onChange).toHaveBeenCalled();
  });

  /**
   * Test: Hex input updates color picker
   * Task Reference: T049
   */
  test("updates color picker when hex input changes", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />
    );

    const hexInput = screen.getByDisplayValue("#1d4ed8");
    await user.clear(hexInput);
    await user.type(hexInput, "#00ff00");

    expect(onChange).toHaveBeenCalled();
  });

  /**
   * Test: Displays validation error for invalid hex color
   * Task Reference: T049
   */
  test("displays validation error for invalid hex color format", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />
    );

    const hexInput = screen.getByDisplayValue("#1d4ed8");
    await user.clear(hexInput);
    await user.type(hexInput, "invalid-color");

    await waitFor(() => {
      expect(screen.getByText(/invalid color format/i)).toBeInTheDocument();
    });
  });

  /**
   * Test: Displays color preview
   * Task Reference: T049
   */
  test("displays color preview", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#ff0000"
        onChange={jest.fn()}
      />
    );

    const preview = screen.getByRole("img", { hidden: true }) || 
                    screen.getByTestId("color-preview");
    expect(preview).toBeInTheDocument();
  });

  /**
   * Test: Secondary and accent color configuration
   * Task Reference: T055
   */
  test("renders secondary color picker", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Secondary Color"
        value="#64748b"
        onChange={jest.fn()}
      />
    );

    expect(screen.getByLabelText(/secondary color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#64748b")).toBeInTheDocument();
  });

  /**
   * Test: Accent color configuration
   * Task Reference: T055
   */
  test("renders accent color picker", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Accent Color"
        value="#0891b2"
        onChange={jest.fn()}
      />
    );

    expect(screen.getByLabelText(/accent color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#0891b2")).toBeInTheDocument();
  });
});

