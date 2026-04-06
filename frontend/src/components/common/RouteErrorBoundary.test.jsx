import React from "react";
import { render, screen } from "@testing-library/react";
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
    jest.spyOn(console, "error").mockImplementation(() => {});
    reloadMock = jest.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { reload: reloadMock },
    });
  });

  afterEach(() => {
    console.error.mockRestore();
    jest.restoreAllMocks();
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

  it("calls window.location.reload when the Reload button is clicked", async () => {
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

    await userEvent.click(screen.getByRole("button", { name: /Reload/i }));
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

  it("renders generic fallback title and message for uncovered routes", () => {
    const genericMessages = {
      ...messages,
      "errorBoundary.route.generic.title": "This page could not be loaded",
      "errorBoundary.route.generic.message":
        "Something went wrong loading this page. Please reload to try again, or use the menu to navigate elsewhere.",
    };
    render(
      <IntlProvider locale="en" messages={genericMessages}>
        <RouteErrorBoundary
          titleKey="errorBoundary.route.generic.title"
          messageKey="errorBoundary.route.generic.message"
          resetKey="1"
        >
          <ThrowingChild />
        </RouteErrorBoundary>
      </IntlProvider>,
    );

    expect(
      screen.getByRole("heading", { name: /This page could not be loaded/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/use the menu to navigate elsewhere/i),
    ).toBeInTheDocument();
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
