/**
 * AnalyzersList Component Tests
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - No reliance on async timing - use proper queries
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/analyzerService", () => ({
  createAnalyzer: vi.fn(),
  deactivateAnalyzer: vi.fn(),
  getAnalyzer: vi.fn(),
  getAnalyzers: vi.fn(),
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypeMapping: vi.fn(),
  reactivateAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library (all utilities in one import)
import { act, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

// 5. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 6. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import AnalyzersList from "./AnalyzersList";

// 8. Utilities (import functions, not just for mocking)
import {
  deactivateAnalyzer,
  getAnalyzers,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  reactivateAnalyzer,
} from "../../../services/analyzerService";

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

// Standard render helper with IntlProvider
const renderWithIntl = (component, localeMessages = messages) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={localeMessages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

// Mock data builder
const createMockAnalyzer = (overrides = {}) => ({
  id: "1",
  name: "Test Analyzer",
  analyzerType: "CHEMISTRY",
  profileId: "site.synthetic-analyzer",
  profileRevision: 1,
  bridgeConnectionId: "bridge-1",
  connected: true,
  testUnitIds: ["1", "2"],
  active: true,
  lastModified: "2025-01-27T10:00:00Z",
  lifecycleStage: "SETUP",
  ...overrides,
});

describe("AnalyzersList", () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();
    vi.spyOn(Element.prototype, "getBoundingClientRect").mockReturnValue({
      bottom: 40,
      height: 40,
      left: 0,
      right: 160,
      top: 0,
      width: 160,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });
    window.history.replaceState({}, "", "/analyzers");
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"a".repeat(64)}`,
        summary: {
          total: 0,
          inUse: 0,
          needsAttention: 0,
          deactivated: 0,
        },
        types: [],
      }),
    );
    getAnalyzerLabUnits.mockImplementation((callback) => callback([]));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /**
   * Test: Renders AnalyzersList with data displays table
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

    // Mock getAnalyzers to call callback immediately with envelope
    getAnalyzers.mockImplementation((filters, callback) => {
      act(() => {
        callback({ analyzers: mockAnalyzers });
      });
    });

    // Act: Render component
    act(() => {
      renderWithIntl(<AnalyzersList />);
    });

    // Verify mock was called
    expect(getAnalyzers).toHaveBeenCalled();

    // Assert: Wait for table container to appear (using data-testid)
    const tableContainer = await screen.findByTestId(
      "analyzers-table-container",
    );
    expect(tableContainer).not.toBeNull();

    // Assert: Verify analyzer names are displayed using data-testid
    // Use findByTestId which waits automatically
    const name1 = await screen.findByTestId(
      "analyzer-name-1",
      {},
      { timeout: 3000 },
    );
    const name2 = await screen.findByTestId(
      "analyzer-name-2",
      {},
      { timeout: 3000 },
    );
    expect(name1).not.toBeNull();
    expect(name2).not.toBeNull();
    expect(name1.textContent).toContain("Hematology Analyzer 1");
    expect(name2.textContent).toContain("Chemistry Analyzer 1");
    expect(
      screen.getByRole("heading", { level: 1, name: "Analyzers" }),
    ).toBeVisible();
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute(
      "href",
      "/",
    );
  });

  test("positions row actions inside the viewport", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({ analyzers: [createMockAnalyzer()] });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(await screen.findByTestId("analyzer-row-overflow-1"));

    expect(
      document.querySelector(".cds--overflow-menu--flip"),
    ).toBeInTheDocument();
  });

  test("does not expose the superseded analyzer QC rule editor", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({ analyzers: [createMockAnalyzer()] });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(await screen.findByTestId("analyzer-row-overflow-1"));

    expect(
      screen.queryByTestId("analyzer-action-qc-rules-1"),
    ).not.toBeInTheDocument();
  });

  test("does not expose per-analyzer mapping or copy actions", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({ analyzers: [createMockAnalyzer()] });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(await screen.findByTestId("analyzer-row-overflow-1"));

    expect(
      screen.queryByTestId("analyzer-action-mappings-1"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("analyzer-action-copy-mappings-1"),
    ).not.toBeInTheDocument();
  });

  test("deactivates an active analyzer without exposing hard delete", async () => {
    window.history.replaceState({}, "", "/analyzers?search=gene&status=ACTIVE");
    const analyzer = createMockAnalyzer({
      id: "42",
      name: "GeneXpert Lab 1",
      status: "ACTIVE",
    });
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => callback({ analyzers: [analyzer] }));
    });
    deactivateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "INACTIVE",
        deactivated: true,
        failure: null,
      }),
    );

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-42"),
    );
    expect(screen.queryByRole("menuitem", { name: "Delete" })).toBeNull();
    await userEvent.click(screen.getByRole("menuitem", { name: "Deactivate" }));

    let params = new URLSearchParams(window.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("status")).toBe("ACTIVE");
    expect(params.get("lifecycle")).toBe("deactivate");
    expect(params.get("lifecycleAnalyzerId")).toBe("42");
    expect(
      screen.getByRole("heading", { name: "Deactivate analyzer" }),
    ).toBeVisible();
    expect(
      screen.getByText(/configuration and history will be retained/i),
    ).toBeVisible();

    await userEvent.click(
      screen.getByRole("button", { name: /Deactivate analyzer$/ }),
    );

    expect(deactivateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    await waitFor(() => expect(getAnalyzers).toHaveBeenCalledTimes(2));
    params = new URLSearchParams(window.location.search);
    expect(params.get("lifecycle")).toBeNull();
    expect(params.get("lifecycleAnalyzerId")).toBeNull();
    expect(params.get("search")).toBe("gene");
    expect(params.get("status")).toBe("ACTIVE");
  });

  test("keeps lifecycle evidence visible while a request is in flight", async () => {
    const analyzer = createMockAnalyzer({ id: "42", status: "ACTIVE" });
    let finishDeactivation;
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => callback({ analyzers: [analyzer] }));
    });
    deactivateAnalyzer.mockImplementation((_id, callback) => {
      finishDeactivation = callback;
    });

    renderWithIntl(<AnalyzersList />);
    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-42"),
    );
    await userEvent.click(screen.getByRole("menuitem", { name: "Deactivate" }));
    await userEvent.click(
      screen.getByRole("button", { name: /Deactivate analyzer$/ }),
    );

    expect(
      screen.getByRole("button", { name: "Cancel deactivation" }),
    ).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Close" }));
    expect(
      screen.getByRole("heading", { name: "Deactivate analyzer" }),
    ).toBeVisible();
    expect(new URLSearchParams(window.location.search).get("lifecycle")).toBe(
      "deactivate",
    );

    act(() => {
      finishDeactivation({
        analyzerId: "42",
        status: "INACTIVE",
        deactivated: true,
      });
    });
    expect(
      screen.queryByRole("heading", { name: "Deactivate analyzer" }),
    ).not.toBeInTheDocument();
  });

  test("reactivates an inactive analyzer through the exact activation boundary", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers?search=gene&lifecycle=reactivate&lifecycleAnalyzerId=42",
    );
    const analyzer = createMockAnalyzer({
      id: "42",
      name: "GeneXpert Lab 1",
      status: "INACTIVE",
    });
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => callback({ analyzers: [analyzer] }));
    });
    reactivateAnalyzer.mockImplementation((_id, callback) =>
      callback({
        analyzerId: "42",
        status: "INACTIVE",
        ready: false,
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings" }],
        statusCode: 422,
      }),
    );

    renderWithIntl(<AnalyzersList />);

    expect(
      await screen.findByRole("heading", { name: "Reactivate analyzer" }),
    ).toBeVisible();
    expect(
      screen.getByText(/setup will be checked again before it can be used/i),
    ).toBeVisible();

    await userEvent.click(
      screen.getByRole("button", { name: "Reactivate analyzer" }),
    );

    expect(reactivateAnalyzer).toHaveBeenCalledWith("42", expect.any(Function));
    expect(
      await screen.findByText("Analyzer mappings must be verified again."),
    ).toBeVisible();
    expect(getAnalyzers).toHaveBeenCalledTimes(1);
    expect(new URLSearchParams(window.location.search).get("lifecycle")).toBe(
      "reactivate",
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Cancel reactivation" }),
    );
    expect(
      new URLSearchParams(window.location.search).get("lifecycle"),
    ).toBeNull();
  });

  test("offers reactivation from error and offline states", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() =>
        callback({
          analyzers: [
            createMockAnalyzer({ id: "41", status: "ERROR_PENDING" }),
            createMockAnalyzer({ id: "42", status: "OFFLINE" }),
          ],
        }),
      );
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-41"),
    );
    expect(screen.getByRole("menuitem", { name: "Reactivate" })).toBeVisible();
    expect(screen.getByRole("menuitem", { name: "Deactivate" })).toBeVisible();
    await userEvent.keyboard("{Escape}");
    await userEvent.click(screen.getByTestId("analyzer-row-overflow-42"));
    expect(screen.getByRole("menuitem", { name: "Reactivate" })).toBeVisible();
    expect(screen.getByRole("menuitem", { name: "Deactivate" })).toBeVisible();
  });

  test("opens an existing analyzer in linkable inline Instrument setup", async () => {
    window.history.replaceState({}, "", "/analyzers?search=gene");
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({
          analyzers: [
            createMockAnalyzer({
              id: "42",
              profileId: "shipped.genexpert-astm",
              profileRevision: 3,
              profileBindingStatus: "PINNED",
            }),
          ],
        });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-42"),
    );
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Edit setup" }),
    );

    const params = new URLSearchParams(window.location.search);
    expect(window.location.pathname).toBe("/analyzers");
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("instrument");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe("shipped.genexpert-astm");
    expect(params.get("revision")).toBe("3");
  });

  test("opens connection settings through the same linkable inline setup", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({
          analyzers: [
            createMockAnalyzer({
              id: "42",
              profileId: "shipped.genexpert-astm",
              profileRevision: 3,
              profileBindingStatus: "PINNED",
            }),
          ],
        });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-42"),
    );
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Configure connection" }),
    );

    const params = new URLSearchParams(window.location.search);
    expect(window.location.pathname).toBe("/analyzers");
    expect(params.get("setup")).toBe("connect");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe("shipped.genexpert-astm");
    expect(params.get("revision")).toBe("3");
    expect(
      screen.queryByRole("menuitem", { name: "Test connection" }),
    ).not.toBeInTheDocument();
  });

  test("opens the canonical Quality Control workflow with analyzer context and an exact return path", async () => {
    window.history.replaceState({}, "", "/analyzers?search=gene&status=ACTIVE");
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({ analyzers: [createMockAnalyzer({ id: "42" })] });
      });
    });

    renderWithIntl(<AnalyzersList />);

    await userEvent.click(
      await screen.findByTestId("analyzer-row-overflow-42"),
    );
    await userEvent.click(
      await screen.findByRole("menuitem", { name: "Quality Control" }),
    );

    const params = new URLSearchParams(window.location.search);
    expect(window.location.pathname).toBe("/analyzers/qc/instruments/42");
    expect(params.get("returnTo")).toBe("/analyzers?search=gene&status=ACTIVE");
  });

  test("localizes the assigned test-unit count", async () => {
    getAnalyzers.mockImplementation((_filters, callback) => {
      act(() => {
        callback({ analyzers: [createMockAnalyzer()] });
      });
    });

    renderWithIntl(<AnalyzersList />, {
      ...messages,
      "analyzer.testUnits.count": "Localized {count} assigned",
    });

    expect(
      await screen.findByTestId("analyzer-test-units-1"),
    ).toHaveTextContent("Localized 2 assigned");
  });

  test("shows a visible loading state until the analyzer list resolves", async () => {
    let resolveAnalyzers;
    getAnalyzers.mockImplementation((_filters, callback) => {
      resolveAnalyzers = callback;
    });

    renderWithIntl(<AnalyzersList />);

    expect(await screen.findByTestId("analyzers-loading")).toBeVisible();
    expect(screen.queryByTestId("analyzers-table")).not.toBeInTheDocument();

    act(() => {
      resolveAnalyzers({ analyzers: [] });
    });

    expect(await screen.findByTestId("analyzers-table")).toBeVisible();
    expect(screen.queryByTestId("analyzers-loading")).not.toBeInTheDocument();
  });

  /**
   * Test: Search analyzers with query filters results
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
        const filtered = allAnalyzers.filter((analyzer) =>
          analyzer.name.toLowerCase().includes(filters.search.toLowerCase()),
        );
        act(() => {
          callback({ analyzers: filtered });
        });
      } else {
        act(() => {
          callback({ analyzers: allAnalyzers });
        });
      }
    });

    // Act: Render component
    act(() => {
      renderWithIntl(<AnalyzersList />);
    });

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
      { timeout: 2000 },
    );
  });

  test("opens linkable Instrument setup inline while preserving the list", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers?search=chemistry&analyzerId=42&profile=stale&revision=1",
    );
    // Arrange: Setup API mocks
    getAnalyzers.mockImplementation((filters, callback) => {
      act(() => {
        callback({ analyzers: [] });
      });
    });

    // Act: Render component
    act(() => {
      renderWithIntl(<AnalyzersList />);
    });

    // Wait for component to render
    await waitFor(() => {
      expect(screen.queryByTestId("analyzers-list")).not.toBeNull();
    });

    // Find and click "Add Analyzer" button using data-testid
    const addButton = screen.getByTestId("add-analyzer-button");
    await userEvent.click(addButton);

    expect(window.location.pathname).toBe("/analyzers");
    expect(new URLSearchParams(window.location.search).get("setup")).toBe(
      "instrument",
    );
    expect(new URLSearchParams(window.location.search).get("search")).toBe(
      "chemistry",
    );
    expect(
      new URLSearchParams(window.location.search).get("analyzerId"),
    ).toBeNull();
    expect(
      new URLSearchParams(window.location.search).get("profile"),
    ).toBeNull();
    expect(
      new URLSearchParams(window.location.search).get("revision"),
    ).toBeNull();
    expect(screen.getByTestId("analyzers-list")).toBeVisible();
    expect(
      await screen.findByRole("region", { name: "Set up a new analyzer" }),
    ).toBeVisible();
    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Set up a new analyzer",
      }),
    ).toBeVisible();
    expect(
      screen.getByRole("heading", { level: 3, name: "Instrument" }),
    ).toBeVisible();

    await userEvent.click(
      screen.getByRole("button", { name: "Close analyzer setup" }),
    );

    expect(new URLSearchParams(window.location.search).get("setup")).toBeNull();
    expect(new URLSearchParams(window.location.search).get("search")).toBe(
      "chemistry",
    );
    expect(
      screen.queryByRole("region", { name: "Set up a new analyzer" }),
    ).not.toBeInTheDocument();
  });

  /**
   * Test: Lifecycle stage badge displays correctly
   *
   * Arrange-Act-Assert pattern:
   * 1. Arrange: Setup API mocks with analyzer data including lifecycleStage
   * 2. Act: Render component
   * 3. Assert: Verify lifecycle stage badge is displayed with correct color
   */
  test("testStatusBadge_DisplaysCorrectly", async () => {
    // Arrange: Setup API mocks with analyzer data including status
    const mockAnalyzers = [
      createMockAnalyzer({
        id: "1",
        name: "Test Analyzer",
        status: "VALIDATION",
      }),
    ];

    getAnalyzers.mockImplementation((filters, callback) => {
      act(() => {
        callback({ analyzers: mockAnalyzers });
      });
    });

    // Act: Render component
    act(() => {
      renderWithIntl(<AnalyzersList />);
    });

    // Assert: Wait for status badge to appear
    const statusBadge = await screen.findByTestId(
      "status-badge-1",
      {},
      { timeout: 3000 },
    );
    expect(statusBadge).not.toBeNull();
    // Verify badge contains the status text
    expect(statusBadge.textContent).toMatch(/validation/i);
  });

  /**
   * Test: Lifecycle stage filter filters analyzers correctly
   *
   * Arrange-Act-Assert pattern:
   * 1. Arrange: Setup API mocks with analyzers in different lifecycle stages
   * 2. Act: Select lifecycle stage filter
   * 3. Assert: Verify only analyzers with matching lifecycle stage are displayed
   */
  test("testStatusFilter_FiltersAnalyzers", async () => {
    // Arrange: Setup API mocks with analyzers in different statuses
    const allAnalyzers = [
      createMockAnalyzer({
        id: "1",
        name: "Analyzer 1",
        status: "SETUP",
      }),
      createMockAnalyzer({
        id: "2",
        name: "Analyzer 2",
        status: "VALIDATION",
      }),
      createMockAnalyzer({
        id: "3",
        name: "Analyzer 3",
        status: "ACTIVE",
      }),
    ];

    // Mock getAnalyzers to filter based on status parameter
    getAnalyzers.mockImplementation((filters, callback) => {
      if (filters && filters.status) {
        const filtered = allAnalyzers.filter(
          (analyzer) => analyzer.status === filters.status,
        );
        act(() => {
          callback({ analyzers: filtered });
        });
      } else {
        act(() => {
          callback({ analyzers: allAnalyzers });
        });
      }
    });

    // Act: Render component
    act(() => {
      renderWithIntl(<AnalyzersList />);
    });

    // Wait for initial data load
    await screen.findByTestId("analyzer-name-1", {}, { timeout: 3000 });

    const statusFilter = screen.getByRole("combobox", { name: "Status" });
    await userEvent.click(statusFilter);
    await userEvent.click(
      await screen.findByRole("option", { name: "Validation" }),
    );

    await waitFor(() => {
      expect(screen.queryByTestId("analyzer-name-1")).not.toBeInTheDocument();
      expect(screen.getByTestId("analyzer-name-2")).toBeVisible();
      expect(screen.queryByTestId("analyzer-name-3")).not.toBeInTheDocument();
    });
    expect(new URLSearchParams(window.location.search).get("status")).toBe(
      "VALIDATION",
    );
  });

  test("restores bookmarked controls and results when browser history changes", async () => {
    const allAnalyzers = [
      createMockAnalyzer({ id: "1", name: "Hematology Analyzer" }),
      createMockAnalyzer({ id: "2", name: "Chemistry Analyzer" }),
    ];
    getAnalyzers.mockImplementation((filters, callback) => {
      const search = filters?.search?.toLowerCase();
      callback({
        analyzers: search
          ? allAnalyzers.filter((analyzer) =>
              analyzer.name.toLowerCase().includes(search),
            )
          : allAnalyzers,
      });
    });
    window.history.replaceState({}, "", "/analyzers?search=Hematology");

    renderWithIntl(<AnalyzersList />);

    expect(await screen.findByTestId("analyzer-name-1")).toBeVisible();
    expect(screen.queryByTestId("analyzer-name-2")).not.toBeInTheDocument();
    expect(screen.getByTestId("analyzer-search-input")).toHaveValue(
      "Hematology",
    );

    window.history.pushState({}, "", "/analyzers?search=Chemistry");
    window.dispatchEvent(new PopStateEvent("popstate"));

    expect(await screen.findByTestId("analyzer-name-2")).toBeVisible();
    expect(screen.queryByTestId("analyzer-name-1")).not.toBeInTheDocument();
    expect(screen.getByTestId("analyzer-search-input")).toHaveValue(
      "Chemistry",
    );
  });

  test("passes a cancellable signal to the type catalog read", () => {
    let catalogSignal;
    getAnalyzers.mockImplementation(() => undefined);
    getAnalyzerTypeCatalog.mockImplementation((_callback, signal) => {
      catalogSignal = signal;
    });

    renderWithIntl(<AnalyzersList />);

    expect(catalogSignal).toBeInstanceOf(AbortSignal);
    expect(catalogSignal?.aborted).toBe(false);
  });
});
