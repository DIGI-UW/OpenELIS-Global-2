import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ModuleErrorBoundary from "./ModuleErrorBoundary";
import messages from "../languages/en.json";

function Page({ fail }) {
  if (fail) {
    throw new Error("bundle exploded");
  }
  return <p>module page</p>;
}

describe("ModuleErrorBoundary", () => {
  let consoleError;

  beforeEach(() => {
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  it("names the module and the error, offers a retry, and passes Carbon plain strings", () => {
    let fail = true;
    const Wrapped = () => <Page fail={fail} />;
    render(
      <IntlProvider locale="en" messages={messages}>
        <ModuleErrorBoundary moduleId="alpha">
          <Wrapped />
        </ModuleErrorBoundary>
      </IntlProvider>,
    );

    expect(screen.getByText("Module unavailable")).toBeInTheDocument();
    expect(
      screen.getByText("The module alpha could not be shown: bundle exploded"),
    ).toBeInTheDocument();
    const propTypeWarnings = consoleError.mock.calls.filter((call) =>
      String(call[0]).includes("Failed %s type"),
    );
    expect(propTypeWarnings).toEqual([]);

    fail = false;
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(screen.getByText("module page")).toBeInTheDocument();
    expect(screen.queryByTestId("module-error")).toBeNull();
  });

  it("renders nothing for a silent slot contribution that fails", () => {
    const { container } = render(
      <IntlProvider locale="en" messages={messages}>
        <ModuleErrorBoundary moduleId="alpha" silent>
          <Page fail />
        </ModuleErrorBoundary>
      </IntlProvider>,
    );

    expect(container.textContent).toBe("");
  });
});
