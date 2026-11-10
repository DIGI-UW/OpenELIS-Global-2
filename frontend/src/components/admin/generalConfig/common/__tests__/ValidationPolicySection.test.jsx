import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";

vi.mock("../../../../utils/Utils", () => ({
  putToOpenElisServerFullResponse: vi.fn(),
}));

import ValidationPolicySection from "../ValidationPolicySection";
import { putToOpenElisServerFullResponse } from "../../../../utils/Utils";
import { QueryClientProvider } from "@tanstack/react-query";
import { createQueryClient } from "../../../../utils/queryClient";

const policy = (overrides = {}) => ({
  trigger: "ALL",
  levels: 1,
  levelRoles: [{ level: 1, roleId: 10, roleName: "Validation" }],
  enabled: true,
  maxLevels: 1,
  blockSelfValidation: false,
  ...overrides,
});

const renderSection = (loaded = policy()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <QueryClientProvider client={createQueryClient()}>
        <ValidationPolicySection policy={loaded} />
      </QueryClientProvider>
    </IntlProvider>,
  );

const summary = () => screen.getByTestId("validation-policy-summary");

describe("Validation policy on Result Configuration (OGC-1364 v1)", () => {
  beforeEach(() => {
    cleanup();
    vi.mocked(putToOpenElisServerFullResponse).mockReset();
  });

  it("shows the lab-wide default and what it does with each kind of result", async () => {
    renderSection();

    expect(await screen.findByLabelText("All results")).toBeChecked();
    expect(summary()).toHaveTextContent(
      "Results the clearance rule clears: reviewed by Validation (1 review)",
    );

    fireEvent.click(screen.getByLabelText("Only results that need review"));
    expect(summary()).toHaveTextContent(
      "Results the clearance rule clears: released at entry",
    );
    expect(summary()).toHaveTextContent(
      "Results that need review: reviewed by Validation (1 review)",
    );
    expect(
      screen.getByText(
        "Uses the same clearance rule as the Clear lane on the Validation page.",
      ),
    ).toBeInTheDocument();
  });

  it("No review takes the reviews to 0 and saves trigger and levels", async () => {
    vi.mocked(putToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback({
          ok: true,
          json: async () =>
            policy({ trigger: "NONE", levels: 0, levelRoles: [] }),
        }),
    );
    renderSection();

    fireEvent.click(await screen.findByLabelText("No review"));
    expect(screen.getByLabelText("Number of reviews")).toHaveValue(0);
    expect(screen.queryByLabelText("Level 1 role")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save policy" }));

    await screen.findByText("Validation policy saved.");
    const [url, body] = vi.mocked(putToOpenElisServerFullResponse).mock
      .calls[0];
    expect(url).toBe("/rest/validation-policy/default");
    expect(JSON.parse(body)).toEqual({ trigger: "NONE", levels: 0 });
  });

  it("refuses 0 reviews unless the policy is No review", async () => {
    renderSection();

    const levels = await screen.findByLabelText("Number of reviews");
    fireEvent.change(levels, { target: { value: "0" } });

    expect(
      await screen.findByText("With 0 reviews, choose No review."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save policy" })).toBeDisabled();
    expect(
      screen.queryByTestId("validation-policy-summary"),
    ).not.toBeInTheDocument();
  });

  it("says results still wait when self-validation is blocked", async () => {
    renderSection(policy({ blockSelfValidation: true }));

    fireEvent.click(await screen.findByLabelText("No review"));
    expect(
      screen.getByText(/Self-validation is blocked \(blockSelfValidation\)/),
    ).toBeInTheDocument();
    expect(summary()).toHaveTextContent(
      "Results the clearance rule clears: wait for a validator",
    );
  });

  it("names the server's refusal", async () => {
    vi.mocked(putToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback({
          ok: false,
          json: async () => ({ field: "roleIds", code: "roleNotAvailable" }),
        }),
    );
    renderSection();

    fireEvent.click(
      await screen.findByLabelText("Only results that need review"),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save policy" }));

    expect(
      await screen.findByText("That role cannot sign reviews yet."),
    ).toBeInTheDocument();
  });
});
