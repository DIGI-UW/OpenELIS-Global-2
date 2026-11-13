import React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";

vi.mock("../../../../utils/Utils", () => ({
  fetchFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

import ValidationPolicySection from "../ValidationPolicySection";
import {
  fetchFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../../../utils/Utils";
import { QueryClientProvider } from "@tanstack/react-query";
import { createQueryClient } from "../../../../utils/queryClient";

const LAB_UNITS = "/rest/validation-policy/lab-units";

const labDefault = (overrides = {}) => ({
  trigger: "ALL",
  levels: 1,
  levelRoles: [{ level: 1, roleId: 10, roleName: "Validation" }],
  enabled: true,
  maxLevels: 1,
  blockSelfValidation: false,
  ...overrides,
});

const override = (fields) => ({
  levels: 1,
  levelRoles: [{ level: 1, roleId: 10, roleName: "Validation" }],
  active: true,
  domain: "CLINICAL",
  ...fields,
});

const waterQuality = override({
  id: 7,
  testSectionId: "3",
  labUnitName: "Water Quality",
  domain: "ENVIRONMENTAL",
  trigger: "NEEDS_REVIEW",
});
const serology = override({
  id: 8,
  testSectionId: "4",
  labUnitName: "Serology",
  trigger: "ALL",
  active: false,
});

const labUnits = [
  { id: "1", name: "Hematology", domain: "CLINICAL" },
  { id: "2", name: "Biochemistry", domain: "CLINICAL" },
  { id: "3", name: "Water Quality", domain: "ENVIRONMENTAL" },
  { id: "4", name: "Serology", domain: "CLINICAL" },
];

let served;

const renderSection = (loaded = labDefault()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <QueryClientProvider client={createQueryClient()}>
        <ValidationPolicySection policy={loaded} />
      </QueryClientProvider>
    </IntlProvider>,
  );

const respond = (body, ok = true) => ({ ok, json: async () => body });

const effective = (unitId) =>
  screen.getByTestId(`validation-effective-${unitId}`);

describe("Lab unit overrides on Result Configuration (OGC-1364 v1 slice 2)", () => {
  beforeEach(() => {
    cleanup();
    served = { labUnits, overrides: [waterQuality, serology] };
    vi.mocked(fetchFromOpenElisServer).mockReset();
    vi.mocked(fetchFromOpenElisServer).mockImplementation(async (url) => {
      if (url === LAB_UNITS) {
        return served;
      }
      throw new Error(`unexpected read ${url}`);
    });
    vi.mocked(postToOpenElisServerFullResponse).mockReset();
    vi.mocked(putToOpenElisServerFullResponse).mockReset();
  });

  it("lists active overrides and shows deactivated ones on request", async () => {
    renderSection();

    const row = await screen.findByTestId("validation-override-3");
    expect(row).toHaveTextContent("Water Quality");
    expect(row).toHaveTextContent("Environmental");
    expect(row).toHaveTextContent("Only results that need review");
    expect(row).toHaveTextContent("Active");
    expect(
      screen.queryByTestId("validation-override-4"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("switch", { name: "Show deactivated" }));

    const deactivated = await screen.findByTestId("validation-override-4");
    expect(deactivated).toHaveTextContent("Deactivated");
    expect(
      within(deactivated).queryByRole("button", { name: "Edit" }),
    ).not.toBeInTheDocument();
    expect(
      within(deactivated).getByRole("button", { name: "Reactivate" }),
    ).toBeInTheDocument();
  });

  it("says when no lab unit has an override", async () => {
    served = { labUnits, overrides: [] };
    renderSection();

    expect(
      await screen.findByText(
        "No lab unit has an override. Every lab unit follows the lab-wide default.",
      ),
    ).toBeInTheDocument();
  });

  it("says no override is active when only deactivated ones remain", async () => {
    served = { labUnits, overrides: [serology] };
    renderSection();

    expect(
      await screen.findByText(
        "No lab unit has an active override. Every lab unit follows the lab-wide default.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(
        "No lab unit has an override. Every lab unit follows the lab-wide default.",
      ),
    ).not.toBeInTheDocument();
  });

  it("adds an override for a lab unit that has none and warns when it turns review off", async () => {
    vi.mocked(postToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback(
          respond(
            override({
              id: 9,
              testSectionId: "1",
              labUnitName: "Hematology",
              trigger: "NONE",
              levels: 0,
              levelRoles: [],
            }),
          ),
        ),
    );
    renderSection();

    await screen.findByTestId("validation-override-3");
    fireEvent.click(
      screen.getByRole("button", { name: "Add lab unit override" }),
    );
    const dialog = await screen.findByRole("dialog");
    const unit = within(dialog).getByLabelText(/^Lab unit/);
    expect(
      within(unit)
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["Choose a lab unit", "Hematology", "Biochemistry"]);

    fireEvent.change(unit, { target: { value: "1" } });
    fireEvent.click(within(dialog).getByLabelText("No review"));
    expect(
      within(dialog).getByText(
        "This turns review off for Hematology. Results will be released without a validator.",
      ),
    ).toBeInTheDocument();

    served = {
      labUnits,
      overrides: [
        waterQuality,
        serology,
        override({
          id: 9,
          testSectionId: "1",
          labUnitName: "Hematology",
          trigger: "NONE",
          levels: 0,
          levelRoles: [],
        }),
      ],
    };
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Save override" }),
    );

    expect(
      await screen.findByText("Override for Hematology saved."),
    ).toBeInTheDocument();
    const [url, body] = vi.mocked(postToOpenElisServerFullResponse).mock
      .calls[0];
    expect(url).toBe(LAB_UNITS);
    expect(JSON.parse(body)).toEqual({
      testSectionId: "1",
      trigger: "NONE",
      levels: 0,
    });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      await screen.findByTestId("validation-override-1"),
    ).toHaveTextContent("No review");
  });

  it("asks for a lab unit before adding", async () => {
    renderSection();

    await screen.findByTestId("validation-override-3");
    fireEvent.click(
      screen.getByRole("button", { name: "Add lab unit override" }),
    );
    const dialog = await screen.findByRole("dialog");
    const labUnit = dialog.querySelector("#validation-override-lab-unit");
    expect(labUnit).toHaveAttribute("aria-required", "true");
    expect(
      dialog.querySelector('label[for="validation-override-lab-unit"]')
        .textContent,
    ).toMatch(/Lab unit \*/);
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Save override" }),
    );

    expect(within(dialog).getByText("Choose a lab unit.")).toBeInTheDocument();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  it("edits an active override and names the server's refusal", async () => {
    vi.mocked(putToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback(respond({ field: "id", code: "overrideDeactivated" }, false)),
    );
    renderSection();

    fireEvent.click(await screen.findByTestId("validation-override-edit-3"));
    const dialog = await screen.findByRole("dialog");
    expect(dialog).toHaveTextContent("Override for Water Quality");
    expect(
      within(dialog).getByLabelText("Only results that need review"),
    ).toBeChecked();
    expect(
      within(dialog).queryByLabelText(/^Lab unit/),
    ).not.toBeInTheDocument();

    fireEvent.click(within(dialog).getByLabelText("All results"));
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Save override" }),
    );

    expect(
      await within(dialog).findByText(
        "Reactivate the override before editing it.",
      ),
    ).toBeInTheDocument();
    const [url, body] = vi.mocked(putToOpenElisServerFullResponse).mock
      .calls[0];
    expect(url).toBe(`${LAB_UNITS}/7`);
    expect(JSON.parse(body)).toEqual({ trigger: "ALL", levels: 1 });
  });

  it("refuses a contradictory override in the dialog", async () => {
    renderSection();

    fireEvent.click(await screen.findByTestId("validation-override-edit-3"));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText("Number of reviews"), {
      target: { value: "0" },
    });

    expect(
      within(dialog).getByText("With 0 reviews, choose No review."),
    ).toBeInTheDocument();
    expect(
      within(dialog).getByRole("button", { name: "Save override" }),
    ).toBeDisabled();
  });

  it("deactivates and reactivates an override", async () => {
    vi.mocked(putToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback(respond({ ...waterQuality, active: false })),
    );
    renderSection();

    served = {
      labUnits,
      overrides: [{ ...waterQuality, active: false }, serology],
    };
    fireEvent.click(
      await screen.findByTestId("validation-override-deactivate-3"),
    );

    expect(
      await screen.findByText(
        "Override for Water Quality deactivated. The lab unit follows the lab-wide default.",
      ),
    ).toBeInTheDocument();
    const [url, body] = vi.mocked(putToOpenElisServerFullResponse).mock
      .calls[0];
    expect(url).toBe(`${LAB_UNITS}/7/active`);
    expect(JSON.parse(body)).toEqual({ active: false });
    await waitFor(() =>
      expect(
        screen.queryByTestId("validation-override-3"),
      ).not.toBeInTheDocument(),
    );

    fireEvent.click(screen.getByRole("switch", { name: "Show deactivated" }));
    fireEvent.click(
      await screen.findByTestId("validation-override-reactivate-3"),
    );
    const [reactivateUrl, reactivateBody] = vi.mocked(
      putToOpenElisServerFullResponse,
    ).mock.calls[1];
    expect(reactivateUrl).toBe(`${LAB_UNITS}/7/active`);
    expect(JSON.parse(reactivateBody)).toEqual({ active: true });
  });

  it("shows the policy each active lab unit follows, with unsaved default changes", async () => {
    renderSection();

    await screen.findByTestId("validation-effective-policy");
    expect(effective("3")).toHaveTextContent("Override");
    expect(effective("3")).toHaveTextContent("released at entry");
    expect(effective("1")).toHaveTextContent("Lab-wide default");
    expect(effective("1")).toHaveTextContent(
      "reviewed by Validation (1 review)reviewed by Validation (1 review)",
    );
    expect(effective("4")).toHaveTextContent("Lab-wide default");

    fireEvent.click(screen.getAllByLabelText("No review")[0]);

    expect(effective("1")).toHaveTextContent(
      "released at entryreleased at entry",
    );
    expect(effective("3")).toHaveTextContent(
      "released at entryreviewed by Validation (1 review)",
    );
    expect(
      screen.getByText(
        "This turns review off for every lab unit without an override. Their results will be released without a validator.",
      ),
    ).toBeInTheDocument();
  });

  it("says when the overrides could not be loaded", async () => {
    vi.mocked(fetchFromOpenElisServer).mockImplementation(async () => {
      throw new Error("503");
    });
    renderSection();

    expect(
      await screen.findByText(
        "The lab unit overrides could not be loaded. Reload the page to try again.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Add lab unit override" }),
    ).toBeDisabled();
  });
});
