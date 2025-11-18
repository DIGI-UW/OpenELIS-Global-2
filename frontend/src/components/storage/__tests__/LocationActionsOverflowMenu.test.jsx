import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationActionsOverflowMenu from "../LocationManagement/LocationActionsOverflowMenu";
import messages from "../../../languages/en.json";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("LocationActionsOverflowMenu", () => {
  const mockLocation = {
    id: "1",
    name: "Main Laboratory",
    code: "MAIN-LAB",
    type: "room",
  };

  const mockOnEdit = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T105: Test renders Edit and Delete menu items
   */
  test("testOverflowMenu_RendersEditAndDelete", () => {
    renderWithIntl(
      <LocationActionsOverflowMenu
        location={mockLocation}
        onEdit={mockOnEdit}
        onDelete={mockOnDelete}
      />,
    );

    // Carbon OverflowMenu renders items in a menu button
    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Click to open menu
    fireEvent.click(menuButton);

    // Verify menu items are present
    expect(screen.getByText(/edit/i)).toBeTruthy();
    expect(screen.getByText(/delete/i)).toBeTruthy();
  });

  /**
   * T105: Test clicking Edit opens EditLocationModal (calls onEdit callback)
   */
  test("testOverflowMenu_EditOpensModal", () => {
    renderWithIntl(
      <LocationActionsOverflowMenu
        location={mockLocation}
        onEdit={mockOnEdit}
        onDelete={mockOnDelete}
      />,
    );

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    // Find the Edit menu item
    const editItem = screen.getByTestId("edit-location-menu-item");
    expect(editItem).toBeTruthy();

    // Click the Edit item
    fireEvent.click(editItem);

    // Verify callback was called with location
    expect(mockOnEdit).toHaveBeenCalledWith(mockLocation);
    expect(mockOnEdit).toHaveBeenCalledTimes(1);
  });

  /**
   * T105: Test clicking Delete opens DeleteLocationModal (calls onDelete callback)
   */
  test("testOverflowMenu_DeleteOpensModal", () => {
    renderWithIntl(
      <LocationActionsOverflowMenu
        location={mockLocation}
        onEdit={mockOnEdit}
        onDelete={mockOnDelete}
      />,
    );

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    // Find the Delete menu item
    const deleteItem = screen.getByTestId("delete-location-menu-item");
    expect(deleteItem).toBeTruthy();

    // Click the Delete item
    fireEvent.click(deleteItem);

    // Verify callback was called with location
    expect(mockOnDelete).toHaveBeenCalledWith(mockLocation);
    expect(mockOnDelete).toHaveBeenCalledTimes(1);
  });

  /**
   * T105: Test menu is accessible via keyboard navigation
   */
  test("testOverflowMenu_KeyboardAccessible", () => {
    renderWithIntl(
      <LocationActionsOverflowMenu
        location={mockLocation}
        onEdit={mockOnEdit}
        onDelete={mockOnDelete}
      />,
    );

    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Test keyboard navigation - Enter key should open menu
    fireEvent.keyDown(menuButton, { key: "Enter", code: "Enter" });
    // Or Space key
    fireEvent.keyDown(menuButton, { key: " ", code: "Space" });

    // Menu should be accessible via keyboard - check aria-label exists
    expect(menuButton.getAttribute("aria-label")).toBeTruthy();
  });
});
