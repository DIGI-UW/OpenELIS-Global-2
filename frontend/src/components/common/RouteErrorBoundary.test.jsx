import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import RouteErrorBoundaryWithLocation, {
  RouteErrorBoundary,
} from "./RouteErrorBoundary";

const messages = {
  "errorBoundary.reload": "Reload",
  "errorBoundary.route.resultsSearch.message":
    "Something went wrong on the results page. Reload to try again.",
  "errorBoundary.route.resultsSearch.title": "Results could not be loaded",
};

function ThrowingChild() {
  throw new Error("render error");
}

describe("RouteErrorBoundary", () => {
  let reloadMock;

  beforeEach(() => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    reloadMock = vi.fn();
  });

  afterEach(() => {
    console.error.mockRestore();
    vi.restoreAllMocks();
  });

  it("renders configured title and message when a child throws", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <RouteErrorBoundary
          titleKey="errorBoundary.route.resultsSearch.title"
          messageKey="errorBoundary.route.resultsSearch.message"
          resetKey="1"
        >
          <ThrowingChild />
        </RouteErrorBoundary>
      </IntlProvider>,
    );

    expect(
      screen.getByRole("heading", { name: /Results could not be loaded/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /Something went wrong on the results page\. Reload to try again\./i,
      ),
    ).toBeInTheDocument();
  });

  it("calls window.location.reload when the Reload button is clicked", () => {
    // jsdom 26 freezes window.location.reload (non-configurable, non-writable).
    // Use the onReload prop to inject a testable callback.
    render(
      <IntlProvider locale="en" messages={messages}>
        <RouteErrorBoundary
          titleKey="errorBoundary.route.resultsSearch.title"
          messageKey="errorBoundary.route.resultsSearch.message"
          resetKey="1"
          onReload={reloadMock}
        >
          <ThrowingChild />
        </RouteErrorBoundary>
      </IntlProvider>,
    );

    const reloadButton = screen.getByRole("button", { name: /Reload/i });
    fireEvent.click(reloadButton);
    expect(reloadMock).toHaveBeenCalled();
  });

  it("clears the error state when resetKey changes so children can render again", () => {
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <RouteErrorBoundary
          titleKey="errorBoundary.route.resultsSearch.title"
          messageKey="errorBoundary.route.resultsSearch.message"
          resetKey="a"
        >
          <ThrowingChild />
        </RouteErrorBoundary>
      </IntlProvider>,
    );

    expect(
      screen.getByText(/Results could not be loaded/i),
    ).toBeInTheDocument();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <RouteErrorBoundary
          titleKey="errorBoundary.route.resultsSearch.title"
          messageKey="errorBoundary.route.resultsSearch.message"
          resetKey="b"
        >
          <div>Recovered content</div>
        </RouteErrorBoundary>
      </IntlProvider>,
    );

    expect(screen.getByText("Recovered content")).toBeInTheDocument();
  });

  it("default export wires resetKey from the current location", () => {
    render(
      <MemoryRouter initialEntries={["/PatientResults/123"]}>
        <IntlProvider locale="en" messages={messages}>
          <RouteErrorBoundaryWithLocation
            titleKey="errorBoundary.route.resultsSearch.title"
            messageKey="errorBoundary.route.resultsSearch.message"
          >
            <ThrowingChild />
          </RouteErrorBoundaryWithLocation>
        </IntlProvider>
      </MemoryRouter>,
    );

    expect(
      screen.getByText(/Results could not be loaded/i),
    ).toBeInTheDocument();
  });
});
