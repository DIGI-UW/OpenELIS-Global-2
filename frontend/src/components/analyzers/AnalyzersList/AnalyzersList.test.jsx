/**
 * AnalyzersList Component Tests
 * 
 * Task Reference: T037
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 * 
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - No reliance on async timing - use proper queries
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../services/analyzerService", () => ({
  getAnalyzers: jest.fn(),
}));

const mockHistory = {
  push: jest.fn(),
  replace: jest.fn(),
};

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useHistory: () => mockHistory,
  useLocation: () => ({ pathname: "/analyzers" }),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library (all utilities in one import)
import {
  render,
  screen,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 6. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import AnalyzersList from "./AnalyzersList";

// 8. Utilities (import functions, not just for mocking)
import { getAnalyzers } from "../../../services/analyzerService";

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

// Standard render helper with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

// Mock data builder
const createMockAnalyzer = (overrides = {}) => ({
  id: "1",
  name: "Test Analyzer",
  analyzerType: "CHEMISTRY",
  ipAddress: "192.168.1.100",
  port: 5000,
  testUnitIds: ["1", "2"],
  active: true,
  lastModified: "2025-01-27T10:00:00Z",
  ...overrides,
});

describe("AnalyzersList", () => {
  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    mockHistory.push.mockClear();
    mockHistory.replace.mockClear();
  });

  /**
   * Test: Renders AnalyzersList with data displays table
   * Task Reference: T037
   * 
   * Arrange-Act-Assert pattern:
   * 1. Arrange: Setup API mocks with analyzer data
   * 2. Act: Render component
   * 3. Assert: Verify table displays analyzers using data-testid
   */
  test("testRendersAnalyzersList_WithData_DisplaysTable", async () => {
    // Arrange: Setup API mocks with analyzer data
    const mockAnalyzers = [
      createMockAnalyzer({ id: "1", name: "Hematology Analyzer 1" }),
      createMockAnalyzer({ id: "2", name: "Chemistry Analyzer 1" }),
    ];

    // Mock getAnalyzers to call callback immediately with data
    getAnalyzers.mockImplementation((filters, callback) => {
      // Call callback synchronously - React will process state update
      callback(mockAnalyzers);
    });

    // Act: Render component
    renderWithIntl(<AnalyzersList />);

    // Verify mock was called
    expect(getAnalyzers).toHaveBeenCalled();

    // Assert: Wait for table container to appear (using data-testid)
    const tableContainer = await screen.findByTestId("analyzers-table-container");
    expect(tableContainer).not.toBeNull();

    // Assert: Verify analyzer names are displayed using data-testid
    // Use findByTestId which waits automatically
    const name1 = await screen.findByTestId("analyzer-name-1", {}, { timeout: 3000 });
    const name2 = await screen.findByTestId("analyzer-name-2", {}, { timeout: 3000 });
    expect(name1).not.toBeNull();
    expect(name2).not.toBeNull();
    expect(name1.textContent).toContain("Hematology Analyzer 1");
    expect(name2.textContent).toContain("Chemistry Analyzer 1");
  });

  /**
   * Test: Search analyzers with query filters results
   * Task Reference: T037
   * 
   * Arrange-Act-Assert pattern:
   * 1. Arrange: Setup API mocks with analyzer data
   * 2. Act: Type search query
   * 3. Assert: Verify filtered results are displayed
   */
  test("testSearchAnalyzers_WithQuery_FiltersResults", async () => {
    // Arrange: Setup API mocks with analyzer data
    const allAnalyzers = [
      createMockAnalyzer({ id: "1", name: "Hematology Analyzer 1" }),
      createMockAnalyzer({ id: "2", name: "Chemistry Analyzer 1" }),
    ];

    // Mock getAnalyzers to filter based on search parameter
    getAnalyzers.mockImplementation((filters, callback) => {
      if (filters && filters.search) {
        // Filter by search query
        const filtered = allAnalyzers.filter((analyzer) =>
          analyzer.name.toLowerCase().includes(filters.search.toLowerCase())
        );
        callback(filtered);
      } else {
        callback(allAnalyzers);
      }
    });

    // Act: Render component
    renderWithIntl(<AnalyzersList />);

    // Wait for initial data load
    await screen.findByTestId("analyzer-name-1", {}, { timeout: 3000 });

    // Find search input and type search query
    const searchInput = screen.getByTestId("analyzer-search-input");
    await userEvent.type(searchInput, "Hematology", { delay: 0 });

    // Wait for debounced search to trigger (300ms) and filtered results to appear
    await waitFor(
      () => {
        // Verify filtered results are displayed
        expect(screen.queryByTestId("analyzer-name-1")).not.toBeNull();
        expect(screen.queryByTestId("analyzer-name-2")).toBeNull();
      },
      { timeout: 2000 }
    );
  });

  /**
   * Test: Open Add Analyzer modal shows form
   * Task Reference: T037
   * 
   * Arrange-Act-Assert pattern:
   * 1. Arrange: Setup API mocks
   * 2. Act: Click "Add Analyzer" button
   * 3. Assert: Verify modal opens with form
   */
  test("testOpenAddAnalyzerModal_ShowsForm", async () => {
    // Arrange: Setup API mocks
    getAnalyzers.mockImplementation((filters, callback) => {
      callback([]);
    });

    // Act: Render component
    renderWithIntl(<AnalyzersList />);

    // Wait for component to render
    await waitFor(() => {
      expect(screen.queryByTestId("analyzers-list")).not.toBeNull();
    });

    // Find and click "Add Analyzer" button using data-testid
    const addButton = screen.getByTestId("add-analyzer-button");
    await userEvent.click(addButton);

    // Assert: Wait for modal to open (AnalyzerForm should have data-testid)
    await waitFor(() => {
      // AnalyzerForm should render with data-testid="analyzer-form"
      expect(screen.queryByTestId("analyzer-form")).not.toBeNull();
    }, { timeout: 2000 });
  });
});
