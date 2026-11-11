import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { beforeEach, expect, it, vi } from "vitest";
import { getAnalyzerMapping } from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerSetupReception from "./AnalyzerSetupReception";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerMapping: vi.fn(),
}));

const show = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerSetupReception
        candidate={{
          id: "42",
          status: "SETUP",
          connection: { actualRuntimeState: "INACTIVE" },
        }}
        onStart={vi.fn()}
        onStop={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getAnalyzerMapping.mockImplementation((_id, callback) =>
    callback({ tests: [] }),
  );
});

it("explains setup isolation before explicitly starting reception", async () => {
  const onStart = vi.fn();
  show({ onStart });
  expect(
    screen.getByText(
      /cannot be accepted as patient results or recorded as quality control/,
    ),
  ).toBeVisible();
  expect(getAnalyzerMapping).not.toHaveBeenCalled();
  await userEvent.click(
    screen.getByRole("button", { name: "Start receiving setup samples" }),
  );
  expect(onStart).toHaveBeenCalledTimes(1);
});

it("reads retained evidence and lets the operator stop reception", async () => {
  const onStop = vi.fn();
  getAnalyzerMapping.mockImplementation((_id, callback) =>
    callback({
      tests: [
        {
          receivedValues: [
            { kind: "NUMBER", value: "1.02" },
            { kind: "TEXT", value: "Positive" },
          ],
        },
      ],
    }),
  );
  show({
    candidate: {
      id: "42",
      status: "VALIDATION",
      connection: { actualRuntimeState: "ACTIVE", receptionPurpose: "SETUP" },
    },
    onStop,
  });
  expect(
    await screen.findByText("2 received values are available in Verify."),
  ).toBeVisible();
  await userEvent.click(
    screen.getByRole("button", { name: "Stop setup reception" }),
  );
  expect(onStop).toHaveBeenCalledTimes(1);
});

it("does not claim reception when the Bridge is inactive after restart", () => {
  show({
    candidate: {
      id: "42",
      status: "VALIDATION",
      connection: { actualRuntimeState: "INACTIVE" },
    },
  });
  expect(
    screen.getByRole("button", { name: "Start receiving setup samples" }),
  ).toBeEnabled();
  expect(
    screen.queryByRole("button", { name: "Stop setup reception" }),
  ).not.toBeInTheDocument();
});

it("shows failed evidence reads without reporting zero received samples", async () => {
  getAnalyzerMapping.mockImplementation((_id, callback) =>
    callback({ error: "unavailable" }),
  );
  show();
  await userEvent.click(
    screen.getByRole("button", { name: "Check for setup samples" }),
  );
  expect(
    await screen.findByText(
      "Received setup values could not be checked. Try again.",
    ),
  ).toBeVisible();
  expect(
    screen.queryByText("No setup values received yet."),
  ).not.toBeInTheDocument();
});
