import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
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

  const mockOnMove = jest.fn();
  const mockOnDispose = jest.fn();
  const mockOnViewStorage = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T088a: Test renders all four menu items
   */
  test("testRendersAllFourMenuItems", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
      />,
    );

    // Carbon OverflowMenu renders items in a menu button
    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Click to open menu
    fireEvent.click(menuButton);

    // Verify menu items are present (text may be in aria-label or visible text)
    expect(screen.getByText(/move/i)).toBeTruthy();
    expect(screen.getByText(/dispose/i)).toBeTruthy();
    expect(screen.getByText(/view audit/i)).toBeTruthy();
    expect(screen.getByText(/view storage/i)).toBeTruthy();
  });

  /**
   * T088a: Test View Audit is disabled
   */
  test("testViewAuditIsDisabled", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
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
   * T088a: Test calls onMove when Move clicked
   */
  test("testCallsOnMoveWhenMoveClicked", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
      />,
    );

    // Carbon OverflowMenu renders a button - find it by role
    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    // Click to open the menu
    fireEvent.click(menuButton);

    // Find the Move menu item by testid - Carbon should render it
    const moveItem = screen.getByTestId("move-menu-item");
    expect(moveItem).toBeTruthy();

    // Carbon OverflowMenuItem wraps onClick in its own handler
    // We need to trigger the onClick directly or find the actual button
    // Try clicking the item - Carbon should handle it
    fireEvent.click(moveItem);

    // Verify callback was called
    expect(mockOnMove).toHaveBeenCalledWith(mockSample);
    expect(mockOnMove).toHaveBeenCalledTimes(1);
  });

  /**
   * T088a: Test calls onDispose when Dispose clicked
   */
  test("testCallsOnDisposeWhenDisposeClicked", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
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
   * T088a: Test calls onViewStorage when View Storage clicked
   */
  test("testCallsOnViewStorageWhenViewStorageClicked", () => {
    renderWithIntl(
      <SampleActionsOverflowMenu
        sample={mockSample}
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
      />,
    );

    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    fireEvent.click(menuButton);

    const viewStorageItem = screen.getByTestId("view-storage-menu-item");
    expect(viewStorageItem).toBeTruthy();

    fireEvent.click(viewStorageItem);

    expect(mockOnViewStorage).toHaveBeenCalledWith(mockSample);
    expect(mockOnViewStorage).toHaveBeenCalledTimes(1);
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
        onMove={undefined}
        onDispose={undefined}
        onViewStorage={undefined}
      />,
    );

    const menuButton = screen.getByRole("button");
    expect(menuButton).toBeTruthy();

    fireEvent.click(menuButton);

    // Menu items should still be visible
    const moveItem = screen.getByTestId("move-menu-item");
    expect(moveItem).toBeTruthy();

    // Clicking should not throw an error, even if callback is undefined
    expect(() => {
      fireEvent.click(moveItem);
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
        onMove={mockOnMove}
        onDispose={mockOnDispose}
        onViewStorage={mockOnViewStorage}
      />,
    );

    const menuButton = screen.getByRole("button");
    fireEvent.click(menuButton);

    // If onClick handlers are properly attached, clicking should trigger them
    // This test will help identify if Carbon OverflowMenuItem has issues with onClick
    const moveItem = screen.getByTestId("move-menu-item");

    // Try to find the actual clickable element within Carbon's structure
    const clickableButton =
      moveItem.closest("button") || moveItem.querySelector("button");

    if (clickableButton) {
      fireEvent.click(clickableButton);
    } else {
      // Fallback: click the item directly
      fireEvent.click(moveItem);
    }

    // Verify callback was called - if this fails, onClick is not properly wired
    expect(mockOnMove).toHaveBeenCalledTimes(1);

    consoleSpy.mockRestore();
  });
});
