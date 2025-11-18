/**
 * Unit tests for ErrorDashboard component
 *
 * Task Reference: T086
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - Test user-visible behavior, not implementation details
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../components/utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 6. Router
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import ErrorDashboard from "./ErrorDashboard";

// 8. Utilities
import { getFromOpenElisServer } from "../../../components/utils/Utils";

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

const createMockError = (overrides = {}) => ({
  id: "ERROR-001",
  analyzerId: "1000",
  analyzerName: "Test Analyzer 1",
  errorType: "MAPPING",
  severity: "ERROR",
  errorMessage: "No mapping found for field: UNMAPPED_FIELD_001",
  rawMessage: "H|1|UNMAPPED_FIELD_001",
  status: "UNACKNOWLEDGED",
  timestamp: new Date().toISOString(),
  ...overrides,
});

// ========== TESTS ==========

describe("ErrorDashboard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Renders ErrorDashboard with errors displays table
   * Task Reference: T086
   */
  test("testRendersErrorDashboard_WithErrors_DisplaysTable", async () => {
    // Arrange: Mock API response with errors
    const mockErrors = [
      createMockError({ id: "ERROR-001" }),
      createMockError({
        id: "ERROR-002",
        severity: "CRITICAL",
        errorType: "CONNECTION",
      }),
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockErrors);
    });

    // Act: Render component
    renderWithIntl(<ErrorDashboard />);

    // Assert: Verify dashboard renders
    const dashboard = await screen.findByTestId("error-dashboard");
    expect(dashboard).toBeInTheDocument();

    // Assert: Verify statistics cards are visible
    expect(await screen.findByTestId("stat-total")).toBeInTheDocument();
    expect(await screen.findByTestId("stat-unacknowledged")).toBeInTheDocument();
    expect(await screen.findByTestId("stat-critical")).toBeInTheDocument();
    expect(await screen.findByTestId("stat-last24hours")).toBeInTheDocument();

    // Assert: Verify table is visible
    const table = await screen.findByTestId("error-table");
    expect(table).toBeInTheDocument();

    // Assert: Verify error rows are displayed
    await waitFor(() => {
      const errorRow1 = screen.queryByTestId("error-row-ERROR-001");
      expect(errorRow1).toBeInTheDocument();
    });
  });

  /**
   * Test: Filter errors by type filters results
   * Task Reference: T086
   */
  test("testFilterErrors_ByType_FiltersResults", async () => {
    // Arrange: Mock API responses
    const allErrors = [
      createMockError({ id: "ERROR-001", errorType: "MAPPING" }),
      createMockError({ id: "ERROR-002", errorType: "CONNECTION" }),
      createMockError({ id: "ERROR-003", errorType: "MAPPING" }),
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      // Simulate filtering on backend
      if (url.includes("errorType=MAPPING")) {
        callback(
          allErrors.filter((e) => e.errorType === "MAPPING"),
        );
      } else {
        callback(allErrors);
      }
    });

    // Act: Render component
    renderWithIntl(<ErrorDashboard />);

    // Wait for initial load
    await screen.findByTestId("error-dashboard");

    // Act: Select error type filter
    const errorTypeFilter = await screen.findByTestId("error-type-filter");
    await userEvent.click(errorTypeFilter);

    // Wait for dropdown to open and select "Mapping" option
    await waitFor(async () => {
      const mappingOption = screen.queryByText("Mapping");
      if (mappingOption) {
        await userEvent.click(mappingOption);
      }
    });

    // Assert: Verify API was called with filter
    await waitFor(() => {
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("errorType"),
        expect.any(Function),
      );
    });
  });

  /**
   * Test: Open error details shows modal
   * Task Reference: T086
   */
  test("testOpenErrorDetails_ShowsModal", async () => {
    // Arrange: Mock API response
    const mockError = createMockError();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([mockError]);
    });

    // Act: Render component
    renderWithIntl(<ErrorDashboard />);

    // Wait for table to load
    await screen.findByTestId("error-table");

    // Act: Click "View Details" action (via OverflowMenu)
    await waitFor(async () => {
      const actionsButton = screen.queryByTestId("error-actions-ERROR-001");
      if (actionsButton) {
        await userEvent.click(actionsButton);
      }
    });

    // Wait for menu to open and click "View Details"
    await waitFor(async () => {
      const viewDetailsButton = screen.queryByTestId(
        "error-action-view-ERROR-001",
      );
      if (viewDetailsButton) {
        await userEvent.click(viewDetailsButton);
      }
    });

    // Assert: Verify modal is visible
    await waitFor(() => {
      const modal = screen.queryByTestId("error-details-modal");
      expect(modal).toBeInTheDocument();
    });
  });

  /**
   * Test: Search errors filters results
   * Task Reference: T086
   */
  test("testSearchErrors_WithQuery_FiltersResults", async () => {
    // Arrange: Mock API responses
    const allErrors = [
      createMockError({ id: "ERROR-001", errorMessage: "Mapping error" }),
      createMockError({ id: "ERROR-002", errorMessage: "Connection timeout" }),
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      // Simulate search filtering
      if (url.includes("search=Mapping")) {
        callback([allErrors[0]]);
      } else {
        callback(allErrors);
      }
    });

    // Act: Render component
    renderWithIntl(<ErrorDashboard />);

    // Wait for search input
    const searchInput = await screen.findByTestId("error-search-input");

    // Act: Type search query
    await userEvent.type(searchInput, "Mapping", { delay: 0 });

    // Wait for debounced search (300ms)
    await waitFor(
      () => {
        expect(getFromOpenElisServer).toHaveBeenCalledWith(
          expect.stringContaining("search=Mapping"),
          expect.any(Function),
        );
      },
      { timeout: 1000 },
    );
  });

  /**
   * Test: Acknowledge all button calls handler
   * Task Reference: T086
   */
  test("testAcknowledgeAll_CallsHandler", async () => {
    // Arrange: Mock API response
    const mockErrors = [createMockError()];
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockErrors);
    });

    // Spy on console.log to verify acknowledge all is called
    const consoleSpy = jest.spyOn(console, "log").mockImplementation();

    // Act: Render component
    renderWithIntl(<ErrorDashboard />);

    // Wait for acknowledge all button
    const acknowledgeAllButton = await screen.findByTestId(
      "acknowledge-all-button",
    );

    // Act: Click acknowledge all button
    await userEvent.click(acknowledgeAllButton);

    // Assert: Verify handler was called (console.log for now, will be API call when implemented)
    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining("Acknowledge all errors"),
      );
    });

    consoleSpy.mockRestore();
  });
});

