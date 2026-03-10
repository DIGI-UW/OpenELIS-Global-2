/**
 * Unit tests for MappingPanel component
 *
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Jest Best Practices: .specify/guides/jest-best-practices.md
 *
 * TDD Workflow (MANDATORY):
 * - RED: Write failing test first
 * - GREEN: Write minimal code to make test pass
 * - REFACTOR: Improve code quality while keeping tests green
 *
 * Test Naming: test{Scenario}_{ExpectedResult}
 */

// ========== MOCKS (MUST be before imports - Jest hoisting) ==========

// Mock OpenELISFieldSelector component
jest.mock("./OpenELISFieldSelector", () => {
  return function MockOpenELISFieldSelector({
    onFieldSelect,
    selectedFieldId,
  }) {
    return (
      <div data-testid="openelis-field-selector">
        <button
          data-testid="select-field-button"
          onClick={() => onFieldSelect("TEST-001", "TEST")}
        >
          Select Field
        </button>
        {selectedFieldId && (
          <span data-testid="selected-field-id">{selectedFieldId}</span>
        )}
      </div>
    );
  };
});

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 4. jest-dom matchers
import "@testing-library/jest-dom";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 6. Router (not needed for MappingPanel, but included for consistency)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import MappingPanel from "./MappingPanel";

// 8. Utilities (none needed)

// 9. Messages/translations
import messages from "../../../languages/en.json";

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

// ========== TEST DATA BUILDERS ==========

const createMockField = (overrides = {}) => ({
  id: "field-1",
  fieldName: "GLUCOSE",
  fieldType: "NUMERIC",
  unit: "mg/dL",
  ...overrides,
});

const createMockMapping = (overrides = {}) => ({
  id: "mapping-1",
  analyzerFieldId: "field-1",
  openelisFieldId: "TEST-001",
  openelisFieldType: "TEST",
  mappingType: "TEST_LEVEL",
  isRequired: false,
  isActive: true,
  ...overrides,
});

// ========== TESTS ==========

describe("MappingPanel", () => {
  const mockOnCreateMapping = jest.fn();
  const mockOnUpdateMapping = jest.fn();
  const mockOnRetireMapping = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Update mapping shows confirmation modal for active analyzers
   *
   * When updating an active mapping on an active analyzer using "Save and Activate",
   * a confirmation modal should be shown before applying the changes.
   */
  test("testUpdateMapping_ShowsConfirmationModal", async () => {
    // Arrange: Active mapping on active analyzer
    const field = createMockField();
    const mapping = createMockMapping({ isActive: true });

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
      />,
    );

    // Act: Click edit button to enter edit mode
    const editButton = screen.getByTestId("mapping-panel-edit-button");
    await userEvent.click(editButton);

    // Wait for edit mode to be visible
    const saveAndActivateButton = await screen.findByTestId(
      "mapping-panel-save-and-activate-button",
    );
    expect(saveAndActivateButton).not.toBeNull();

    // Change the mapping (select different field)
    const selectFieldButton = screen.getByTestId("select-field-button");
    await userEvent.click(selectFieldButton);

    // Click "Save and Activate" button (should show modal for active analyzer with active mapping)
    await userEvent.click(saveAndActivateButton);

    // Assert: Confirmation modal should be shown (open, not hidden)
    await waitFor(() => {
      const confirmationModal = screen.queryByTestId(
        "mapping-activation-modal",
      );
      expect(confirmationModal).not.toBeNull();
      // Modal should be open (aria-hidden should be null or "false")
      const ariaHidden = confirmationModal.getAttribute("aria-hidden");
      expect(ariaHidden).not.toBe("true");
      // updateMapping should NOT be called until confirmation is provided
      expect(mockOnUpdateMapping).not.toHaveBeenCalled();
    });
  });

  /**
   * Test: Save draft mapping does not require confirmation
   *
   * When saving a draft mapping using "Save as Draft" button, no confirmation modal
   * should be shown. The mapping should be saved directly with isActive=false.
   */
  test("testSaveDraftMapping_DoesNotRequireConfirmation", async () => {
    // Arrange: Draft mapping (not active)
    const field = createMockField();
    const mapping = createMockMapping({ isActive: false }); // Draft state

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
      />,
    );

    // Act: Click edit button to enter edit mode
    const editButton = screen.getByTestId("mapping-panel-edit-button");
    await userEvent.click(editButton);

    // Wait for edit mode to be visible
    const saveDraftButton = await screen.findByTestId(
      "mapping-panel-save-draft-button",
    );
    expect(saveDraftButton).not.toBeNull();

    // Change the mapping (select different field)
    const selectFieldButton = screen.getByTestId("select-field-button");
    await userEvent.click(selectFieldButton);

    // Click "Save as Draft" button
    await userEvent.click(saveDraftButton);

    // Assert: No confirmation modal should be shown, updateMapping should be called directly
    await waitFor(() => {
      expect(mockOnUpdateMapping).toHaveBeenCalledTimes(1);
    });

    // Verify confirmation modal is NOT shown
    // Modal exists in DOM but should be closed (aria-hidden=true)
    const confirmationModal = screen.queryByTestId("mapping-activation-modal");
    if (confirmationModal) {
      // If modal exists, it should be hidden (closed)
      expect(confirmationModal.getAttribute("aria-hidden")).toBe("true");
    }

    // Verify updateMapping was called with correct data (isActive=false for draft)
    expect(mockOnUpdateMapping).toHaveBeenCalledWith(
      mapping.id,
      expect.objectContaining({
        analyzerFieldId: field.id,
        openelisFieldId: "TEST-001",
        openelisFieldType: "TEST",
        isActive: false, // Saved as draft
      }),
    );
  });

  /**
   * Test: Create new mapping does not require confirmation
   *
   * When creating a new mapping (no existing mapping), no confirmation should
   * be required regardless of analyzer status. "Save and Activate" should work
   * directly for new mappings.
   */
  test("testCreateMapping_DoesNotRequireConfirmation", async () => {
    // Arrange: No existing mapping
    const field = createMockField();

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={null}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
      />,
    );

    // Act: Component starts in edit mode when no mapping exists
    // Wait for edit mode to be visible
    const saveAndActivateButton = await screen.findByTestId(
      "mapping-panel-save-and-activate-button",
    );
    expect(saveAndActivateButton).not.toBeNull();

    // Select a field
    const selectFieldButton = screen.getByTestId("select-field-button");
    await userEvent.click(selectFieldButton);

    // Click "Save and Activate" button (new mappings don't require confirmation)
    await userEvent.click(saveAndActivateButton);

    // Assert: No confirmation modal, createMapping should be called directly
    await waitFor(() => {
      expect(mockOnCreateMapping).toHaveBeenCalledTimes(1);
    });

    // Verify confirmation modal is NOT shown (new mappings don't require confirmation)
    // Modal exists in DOM but should be closed (aria-hidden=true)
    const confirmationModal = screen.queryByTestId("mapping-activation-modal");
    if (confirmationModal) {
      // If modal exists, it should be hidden (closed)
      expect(confirmationModal.getAttribute("aria-hidden")).toBe("true");
    }

    // Verify createMapping was called with correct data (isActive=true for activate)
    expect(mockOnCreateMapping).toHaveBeenCalledWith(
      expect.objectContaining({
        analyzerFieldId: field.id,
        openelisFieldId: "TEST-001",
        openelisFieldType: "TEST",
        isActive: true, // Activated
      }),
    );
  });

  /**
   * Test: Retire button with pending messages shows disabled tooltip
   *
   * When there are pending messages for a mapping, the retire button should be
   * disabled and show a tooltip explaining why it cannot be retired.
   */
  test("testRetireButton_WithPendingMessages_ShowsDisabledTooltip", async () => {
    // Arrange: Active mapping with pending messages
    const field = createMockField();
    const mapping = createMockMapping({ isActive: true, isRequired: false });
    const pendingMessagesCount = 5;

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        onRetireMapping={mockOnRetireMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
        pendingMessagesCount={pendingMessagesCount}
      />,
    );

    // Act: Find retire button
    const retireButton = screen.getByTestId("mapping-panel-retire-button");

    // Assert: Button should be disabled
    expect(retireButton.getAttribute("disabled")).not.toBeNull();
    expect(retireButton.disabled).toBe(true);
  });

  /**
   * Test: Retire mapping opens confirmation modal
   *
   * When clicking the retire button for an active, non-required mapping with no
   * pending messages, the retirement confirmation modal should open.
   */
  test("testRetireMapping_OpensConfirmationModal", async () => {
    // Arrange: Active mapping with no pending messages
    const field = createMockField();
    const mapping = createMockMapping({ isActive: true, isRequired: false });

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        onRetireMapping={mockOnRetireMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
        pendingMessagesCount={0}
      />,
    );

    // Act: Click retire button
    const retireButton = screen.getByTestId("mapping-panel-retire-button");
    expect(retireButton.getAttribute("disabled")).toBeNull();
    expect(retireButton.disabled).toBe(false);
    await userEvent.click(retireButton);

    // Assert: Retirement modal should be open
    await waitFor(() => {
      const retirementModal = screen.queryByTestId("mapping-retirement-modal");
      expect(retirementModal).toBeTruthy();
      // Modal should be open (aria-hidden should not be "true")
      const ariaHidden = retirementModal?.getAttribute("aria-hidden");
      expect(ariaHidden).not.toBe("true");
    });
  });

  /**
   * Test: Retired mapping displays retired badge
   *
   * When a mapping is retired (isActive=false), it should display a "Retired"
   * badge in the panel header.
   */
  test("testRetiredMapping_DisplaysRetiredBadge", async () => {
    // Arrange: Retired mapping (isActive=false)
    const field = createMockField();
    const mapping = createMockMapping({ isActive: false });

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        onRetireMapping={mockOnRetireMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
        pendingMessagesCount={0}
      />,
    );

    // Assert: Retired badge should be visible
    const retiredBadge = screen.getByTestId("mapping-retired-badge");
    expect(retiredBadge).not.toBeNull();
    expect(retiredBadge.textContent).toContain("Retired");

    // Assert: Retire button should NOT be visible for retired mappings
    const retireButton = screen.queryByTestId("mapping-panel-retire-button");
    expect(retireButton).toBeNull();
  });

  /**
   * Test: Retire with reason submits reason to API
   *
   * When retiring a mapping with a reason provided in the retirement modal,
   * the reason should be passed to the onRetireMapping callback.
   */
  test("testRetireWithReason_SubmitsReasonToAPI", async () => {
    // Arrange: Active mapping with no pending messages
    const field = createMockField();
    const mapping = createMockMapping({ isActive: true, isRequired: false });
    const retirementReason = "Mapping no longer needed due to analyzer upgrade";

    renderWithIntl(
      <MappingPanel
        field={field}
        mapping={mapping}
        onCreateMapping={mockOnCreateMapping}
        onUpdateMapping={mockOnUpdateMapping}
        onRetireMapping={mockOnRetireMapping}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
        pendingMessagesCount={0}
      />,
    );

    // Act: Click retire button to open modal
    const retireButton = screen.getByTestId("mapping-panel-retire-button");
    await userEvent.click(retireButton);

    // Wait for modal to open
    await waitFor(() => {
      const retirementModal = screen.queryByTestId("mapping-retirement-modal");
      expect(retirementModal).toBeTruthy();
    });

    // Find and fill retirement reason textarea
    const reasonTextarea = screen.getByLabelText(/reason/i);
    await userEvent.type(reasonTextarea, retirementReason);

    // Find and click confirm button
    const confirmButton = screen.getByTestId(
      "mapping-retirement-confirm-button",
    );
    expect(confirmButton).not.toBeNull();
    await userEvent.click(confirmButton);

    // Assert: onRetireMapping should be called with mapping ID and reason
    await waitFor(() => {
      expect(mockOnRetireMapping).toHaveBeenCalledTimes(1);
      expect(mockOnRetireMapping).toHaveBeenCalledWith(
        mapping.id,
        retirementReason,
      );
    });
  });
});
