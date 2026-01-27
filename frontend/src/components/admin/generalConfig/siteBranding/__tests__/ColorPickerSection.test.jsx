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
    </BrowserRouter>,
  );
};

// ========== TEST SUITE ==========

describe("ColorPickerSection", () => {
  /**
   * Test: Component renders with color picker
   * Task Reference: T049
   */
  test("renders color picker input and color input field", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={jest.fn()}
      />,
    );

    expect(screen.getByLabelText(/primary color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#1d4ed8")).toBeInTheDocument();
  });

  /**
   * Test: Color picker updates color input
   * Task Reference: T049
   */
  test("updates color input when color picker changes", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByLabelText(/primary color/i);
    await user.type(colorInput, "#ff0000");

    expect(onChange).toHaveBeenCalled();
  });

  /**
   * Test: Color input updates color picker
   * Task Reference: T049
   */
  test("updates color picker when color input changes", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByDisplayValue("#1d4ed8");
    await user.clear(colorInput);
    await user.type(colorInput, "#00ff00");

    expect(onChange).toHaveBeenCalled();
  });

  /**
   * Test: Accepts CSS named colors
   * Task Reference: T049
   *
   * Color validation is now permissive - any CSS color format is accepted.
   * The color preview square shows whether the color is valid in CSS.
   */
  test("accepts CSS named colors without showing validation error", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByDisplayValue("#1d4ed8");
    await user.clear(colorInput);
    await user.type(colorInput, "rebeccapurple");

    // Should call onChange with the named color
    expect(onChange).toHaveBeenCalled();

    // No validation error should be displayed
    await waitFor(() => {
      expect(
        screen.queryByText(/invalid color format/i),
      ).not.toBeInTheDocument();
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
      />,
    );

    const preview =
      screen.getByRole("img", { hidden: true }) ||
      screen.getByTestId("color-preview");
    expect(preview).toBeInTheDocument();
  });

  /**
   * Test: Secondary color configuration
   * Task Reference: T055
   */
  test("renders secondary color picker", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Secondary Color"
        value="#64748b"
        onChange={jest.fn()}
      />,
    );

    expect(screen.getByLabelText(/secondary color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#64748b")).toBeInTheDocument();
  });

  /**
   * Test: Header color configuration
   * Task Reference: T055
   */
  test("renders header color picker", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Header Color"
        value="#295785"
        onChange={jest.fn()}
      />,
    );

    expect(screen.getByLabelText(/header color/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue("#295785")).toBeInTheDocument();
  });
});
