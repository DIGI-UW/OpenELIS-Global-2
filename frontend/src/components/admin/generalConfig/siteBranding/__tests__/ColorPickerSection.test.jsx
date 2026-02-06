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
import "@testing-library/jest-dom/extend-expect";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import ColorPickerSection from "../ColorPickerSection";
import messages from "../../../../../languages/en.json";

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

    const colorInput = screen.getByLabelText(/primary color/i);
    expect(colorInput).toBeInTheDocument();
    expect(colorInput).toHaveValue("#1d4ed8");
  });

  /**
   * Test: Color picker updates color input
   * Task Reference: T049
   */
  test("updates color input when color picker changes", async () => {
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByLabelText(/primary color/i);
    await userEvent.type(colorInput, "#ff0000");

    expect(onChange).toHaveBeenCalled();
  });

  /**
   * Test: Color input updates color picker
   * Task Reference: T049
   */
  test("updates color picker when color input changes", async () => {
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByLabelText(/primary color/i);
    fireEvent.change(colorInput, { target: { value: "" } });
    await userEvent.type(colorInput, "#00ff00");

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
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    const colorInput = screen.getByLabelText(/primary color/i);
    fireEvent.change(colorInput, { target: { value: "" } });
    await userEvent.type(colorInput, "rebeccapurple");

    // Should call onChange with the named color
    expect(onChange).toHaveBeenCalled();

    // No validation error should be displayed
    expect(screen.queryByText(/invalid color format/i)).not.toBeInTheDocument();
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

    const preview = screen.getByTestId("color-preview");
    expect(preview).toBeInTheDocument();
    expect(preview).toHaveStyle("background-color: #ff0000");
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

    // The color input has aria-label matching the label prop
    const colorInput = screen.getByLabelText(/secondary color/i);
    expect(colorInput).toBeInTheDocument();
    expect(colorInput).toHaveValue("#64748b");
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

    // The color input has aria-label matching the label prop
    const colorInput = screen.getByLabelText(/header color/i);
    expect(colorInput).toBeInTheDocument();
    expect(colorInput).toHaveValue("#295785");
  });

  /**
   * Test: Default value when empty
   */
  test("uses default color when value is empty", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value=""
        onChange={jest.fn()}
      />,
    );

    // Should use Carbon's default color #0f62fe
    const colorInput = screen.getByLabelText(/primary color/i);
    expect(colorInput).toHaveValue("#0f62fe");
  });

  /**
   * Test: Default value when value is null
   */
  test("uses default color when value is null", () => {
    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value={null}
        onChange={jest.fn()}
      />,
    );

    // Should use Carbon's default color #0f62fe
    const colorInput = screen.getByLabelText(/primary color/i);
    expect(colorInput).toHaveValue("#0f62fe");
  });

  /**
   * Test: onChange not called for empty input
   * The component checks `if (onChange && newColor)` before calling onChange
   */
  test("does not call onChange when text input is cleared to empty", async () => {
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    // Target the TextInput specifically (not the HTML5 color picker)
    // The TextInput has placeholder="#0f62fe or blue"
    const textInput = screen.getByPlaceholderText(/#0f62fe or blue/i);

    // Clear the input to empty
    fireEvent.change(textInput, { target: { value: "" } });

    // onChange should not be called with empty value
    expect(onChange).not.toHaveBeenCalled();
  });

  /**
   * Test: HTML5 color picker triggers onChange
   */
  test("HTML5 color picker change triggers onChange", async () => {
    const onChange = jest.fn();

    renderWithIntl(
      <ColorPickerSection
        label="Primary Color"
        value="#1d4ed8"
        onChange={onChange}
      />,
    );

    // Find the HTML5 color input (type="color")
    const colorPicker = document.querySelector('input[type="color"]');
    expect(colorPicker).toBeInTheDocument();

    // Change the color via the picker
    fireEvent.change(colorPicker, { target: { value: "#ff0000" } });

    expect(onChange).toHaveBeenCalledWith("#ff0000");
  });
});
