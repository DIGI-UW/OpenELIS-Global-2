import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import SampleActionsOverflowMenu from "./SampleActionsOverflowMenu";
import messages from "../../../languages/en.json";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("SampleActionsOverflowMenu", () => {
  const mockSample = {
    id: "1",
    sampleId: "S-2025-001",
    type: "Blood Serum",
    status: "Active",
  };

  const mockOnManageLocation = jest.fn();
  const mockOnDispose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T201: Test renders all three menu items (Manage Location, Dispose, View Audit)
   */
  test("testOverflowMenu_RendersThreeItems", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={mockOnManageLocation}
        onDispose={mockOnDispose}
      />,
    );

    // Carbon OverflowMenu renders items in a menu button
    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Click to open menu
    fireEvent.click(menuButton);

    // Verify menu items are present (text may be in aria-label or visible text)
    expect(screen.getByText(/manage location/i)).toBeTruthy();
    expect(screen.getByText(/dispose/i)).toBeTruthy();
    expect(screen.getByText(/view audit/i)).toBeTruthy();
    // Verify "Move" and "View Storage" are NOT present
    expect(screen.queryByText(/^move$/i)).toBeNull();
    expect(screen.queryByText(/view storage/i)).toBeNull();
  });

  /**
   * T201: Test View Audit is disabled
   */
  test("testOverflowMenu_ViewAuditIsDisabled", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={mockOnManageLocation}
        onDispose={mockOnDispose}
      />,
    );

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    // View Audit should be disabled
    const viewAuditItem = screen.getByTestId("view-audit-menu-item");
    const button = viewAuditItem.closest("button") || viewAuditItem;
    expect(button.hasAttribute("disabled")).toBe(true);
  });

  /**
   * T201: Test calls onManageLocation when Manage Location clicked
   */
  test("testOverflowMenu_ManageLocationOpensModal", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={mockOnManageLocation}
        onDispose={mockOnDispose}
      />,
    );

    // Carbon OverflowMenu renders a button - find it by role
    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Click to open the menu
    fireEvent.click(menuButton);

    // Find the Manage Location menu item by testid - Carbon should render it
    const manageLocationItem = screen.getByTestId("manage-location-menu-item");
    expect(manageLocationItem).toBeTruthy();

    // Carbon OverflowMenuItem wraps onClick in its own handler
    // We need to trigger the onClick directly or find the actual button
    // Try clicking the item - Carbon should handle it
    fireEvent.click(manageLocationItem);

    // Verify callback was called
    expect(mockOnManageLocation).toHaveBeenCalledWith(mockSample);
    expect(mockOnManageLocation).toHaveBeenCalledTimes(1);
  });

  /**
   * Test calls onDispose when Dispose clicked
   */
  test("testCallsOnDisposeWhenDisposeClicked", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={mockOnManageLocation}
        onDispose={mockOnDispose}
      />,
    );

    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    fireEvent.click(menuButton);

    const disposeItem = screen.getByTestId("dispose-menu-item");
    expect(disposeItem).toBeTruthy();

    fireEvent.click(disposeItem);

    expect(mockOnDispose).toHaveBeenCalledWith(mockSample);
    expect(mockOnDispose).toHaveBeenCalledTimes(1);
  });

  /**
   * Test that component fails gracefully when callbacks are not provided
   * This ensures we don't have silent failures in production
   */
  test("testHandlesMissingCallbacksWithoutErrors", () => {
    // Component should render even without callbacks
    const { container } = renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={undefined}
        onDispose={undefined}
      />,
    );

    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    fireEvent.click(menuButton);

    // Menu items should still be visible
    const manageLocationItem = screen.getByTestId("manage-location-menu-item");
    expect(manageLocationItem).toBeTruthy();

    // Clicking should not throw an error, even if callback is undefined
    expect(() => {
      fireEvent.click(manageLocationItem);
    }).not.toThrow();
  });

  /**
   * Test that verifies onClick handlers are actually attached
   * This test would fail if Carbon OverflowMenuItem doesn't properly wire up onClick
   */
  test("testVerifiesOnClickHandlersAreAttached", () => {
    const consoleSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});

    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onManageLocation={mockOnManageLocation}
        onDispose={mockOnDispose}
      />,
    );

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    // If onClick handlers are properly attached, clicking should trigger them
    // This test will help identify if Carbon OverflowMenuItem has issues with onClick
    const manageLocationItem = screen.getByTestId("manage-location-menu-item");

    // Try to find the actual clickable element within Carbon's structure
    const clickableButton =
      manageLocationItem.closest("button") ||
      manageLocationItem.querySelector("button");

    if (clickableButton) {
      fireEvent.click(clickableButton);
    } else {
      // Fallback: click the item directly
      fireEvent.click(manageLocationItem);
    }

    // Verify callback was called - if this fails, onClick is not properly wired
    expect(mockOnManageLocation).toHaveBeenCalledTimes(1);

    consoleSpy.mockRestore();
  });
});
