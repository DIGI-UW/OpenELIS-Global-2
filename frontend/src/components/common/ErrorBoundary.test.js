import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ErrorBoundary from "./ErrorBoundary";

const ProblemChild = () => {
  throw new Error("boom");
};

test("renders fallback when a child throws", () => {
  const consoleErrorSpy = jest
    .spyOn(console, "error")
    .mockImplementation(() => {});

  render(
    <IntlProvider locale="en" messages={messages}>
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>
    </IntlProvider>,
  );

  expect(screen.getByTestId("error-boundary-fallback")).toBeTruthy();

  consoleErrorSpy.mockRestore();
});
