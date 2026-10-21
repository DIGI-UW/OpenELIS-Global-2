import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseAstWorkspace from "../CaseAstWorkspace";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
const t = (key) => messages[`microbiology.m6.${key}`];
const reading = (id, antibioticId, value) => ({
  id,
  antibioticId,
  rawText: String(value),
  units: "ug/mL",
  interpretation: "SUSCEPTIBLE",
  createdAt: "2026-10-01T10:00:00Z",
  matchedBy: "ORGANISM",
  overrideHistory: [],
});
const workspace = (status = "REVIEWED") => ({
  canEnter: true,
  canValidate: true,
  standards: [{ id: "std", label: "CLSI 2026" }],
  dst: { classification: "NOT_APPLICABLE", drugs: [] },
  isolates: [
    {
      id: "i1",
      label: "ISO-1",
      organism: "Escherichia coli",
      protocol: "GENERAL",
      assignments: [],
      panels: [],
      choices: [{ readingId: "r-b" }],
      runs: [
        {
          id: "run",
          status,
          technique: "BROTH_MICRODILUTION",
          startedAt: "2026-10-01T10:00:00Z",
          breakpointVersion: "2026",
          orderedAntibiotics: [
            { antibioticId: "a", antibioticName: "Ampicillin" },
            { antibioticId: "b", antibioticName: "Ceftriaxone" },
          ],
          readings: [reading("r-a", "a", 4), reading("r-b", "b", 2)],
        },
      ],
    },
  ],
});
const show = (data, rights = { canWrite: true, canValidate: true }) => {
  const service = {
    getAstWorkspace: vi.fn().mockResolvedValue(data),
    astAction: vi.fn().mockResolvedValue(data),
  };
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseAstWorkspace detail={{ id: "c1", ...rights }} service={service} />
    </IntlProvider>,
  );
  return service;
};
it("selects only the requested validated drug with a recorded reason", async () => {
  const service = show(workspace());
  fireEvent.click(
    await screen.findByRole("button", { name: t("action.select") }),
  );
  const modal = screen.getByRole("dialog");
  const save = within(modal).getByRole("button", {
    name: t("save"),
    exact: true,
  });
  expect(save).toBeDisabled();
  fireEvent.change(within(modal).getByLabelText(t("reason")), {
    target: { value: "Confirmed on repeat" },
  });
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.astAction).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({
        action: "select",
        isolateId: "i1",
        runId: "run",
        readingId: "r-a",
        antibioticId: "a",
        reason: "Confirmed on repeat",
      }),
      true,
    ),
  );
});
it("outside readers see readings and history without mutation controls", async () => {
  show(workspace(), { canWrite: false, canValidate: false });
  expect(await screen.findByText("Ampicillin")).toBeInTheDocument();
  for (const action of [
    "assign",
    "start",
    "select",
    "repeat",
    "reading",
    "review",
  ])
    expect(
      screen.queryByRole("button", { name: t(`action.${action}`) }),
    ).not.toBeInTheDocument();
});
it("keeps raw input after rejection and rejects negative values before submission", async () => {
  const service = show(workspace("IN_PROGRESS"));
  service.astAction.mockRejectedValue(new Error("Conflict"));
  fireEvent.click(
    (await screen.findAllByRole("button", { name: t("action.reading") }))[0],
  );
  const modal = screen.getByRole("dialog");
  const field = within(modal).getByLabelText(t("raw"));
  const save = within(modal).getByRole("button", {
    name: t("save"),
    exact: true,
  });
  fireEvent.change(field, { target: { value: "-1" } });
  expect(save).toBeDisabled();
  expect(service.astAction).not.toHaveBeenCalled();
  fireEvent.change(field, { target: { value: "4" } });
  fireEvent.click(save);
  await within(modal).findByText(t("saveError"));
  expect(service.astAction).toHaveBeenCalledTimes(1);
  expect(field).toHaveValue(4);
});
it("requires and submits a replacement card when invalidating an instrument run", async () => {
  const data = workspace("QC_FAILED");
  data.isolates[0].runs[0].analyzerInstrumentId = "instrument";
  const service = show(data);
  fireEvent.click(
    await screen.findByRole("button", { name: t("action.invalidate") }),
  );
  const modal = screen.getByRole("dialog");
  fireEvent.change(within(modal).getByLabelText(t("reason")), {
    target: { value: "Failed QC" },
  });
  const save = within(modal).getByRole("button", {
    name: t("save"),
    exact: true,
  });
  expect(save).toBeDisabled();
  fireEvent.change(within(modal).getByLabelText(t("replacementCard")), {
    target: { value: "CARD-REPEAT" },
  });
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.astAction).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({
        action: "invalidate",
        analyzerCardId: "CARD-REPEAT",
      }),
      false,
    ),
  );
});

it("explains a validation blocker and clears it when another action opens", async () => {
  const service = show(workspace("IN_PROGRESS"));
  service.astAction.mockRejectedValue({
    response: { message: "AST_NO_BREAKPOINT_UNRESOLVED" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: t("action.review") }),
  );
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: t("save"),
      exact: true,
    }),
  );
  const message =
    messages["microbiology.m6.error.AST_NO_BREAKPOINT_UNRESOLVED"];
  expect(
    await within(screen.getByRole("dialog")).findByText(message),
  ).toBeInTheDocument();
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: t("cancel"),
      exact: true,
    }),
  );
  const run = screen.getByTestId("ast-run-run");
  const drug = within(run).getByRole("row", { name: /Ampicillin/ });
  fireEvent.click(
    within(drug).getByRole("button", { name: t("action.reading") }),
  );
  expect(screen.queryByText(message)).not.toBeInTheDocument();
});

it("repeats a chosen antibiotic without silently ordering the whole panel", async () => {
  const service = show(workspace());
  fireEvent.click(
    await screen.findByRole("button", { name: t("action.retest") }),
  );
  const modal = screen.getByRole("dialog");
  fireEvent.change(
    within(modal).getByLabelText(messages["microbiology.ast.attemptScope"]),
    { target: { value: "b" } },
  );
  fireEvent.change(within(modal).getByLabelText(t("reason")), {
    target: { value: "Confirm the discordant agent" },
  });
  fireEvent.click(
    within(modal).getByRole("button", { name: t("save"), exact: true }),
  );
  await waitFor(() =>
    expect(service.astAction).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({
        action: "retest",
        orderedAntibioticIds: ["b"],
        reason: "Confirm the discordant agent",
      }),
      false,
    ),
  );
});

it("lets Results users save a manual reading without requesting the analyzer list", async () => {
  const data = workspace("IN_PROGRESS");
  const service = {
    getAstWorkspace: vi.fn().mockResolvedValue(data),
    astAction: vi.fn().mockResolvedValue(data),
    getReagentLotOverview: vi
      .fn()
      .mockResolvedValue({ requirements: [], usages: [] }),
    getAnalyzers: vi.fn().mockRejectedValue(new Error("Forbidden")),
  };
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles: ["Results"] } }}
      >
        <CaseAstWorkspace
          detail={{ id: "c1", canWrite: true, canValidate: false }}
          service={service}
        />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
  fireEvent.click(
    (await screen.findAllByRole("button", { name: t("action.reading") }))[0],
  );
  const dialog = screen.getByRole("dialog");
  fireEvent.change(within(dialog).getByLabelText(t("raw")), {
    target: { value: "4" },
  });
  fireEvent.click(
    within(dialog).getByRole("button", { name: t("save"), exact: true }),
  );
  await waitFor(() =>
    expect(service.astAction).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({ action: "reading" }),
      false,
    ),
  );
  expect(service.getAnalyzers).not.toHaveBeenCalled();
  expect(screen.queryByText(t("saveError"))).not.toBeInTheDocument();
});

it("loads analyzer choices when the session has analyzer import rights", async () => {
  const service = {
    getAstWorkspace: vi.fn().mockResolvedValue(workspace()),
    getAnalyzers: vi
      .fn()
      .mockResolvedValue([{ id: "instrument", name: "Instrument" }]),
  };
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles: ["Analyser Import"] } }}
      >
        <CaseAstWorkspace
          detail={{ id: "c1", canWrite: true }}
          service={service}
        />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
  await waitFor(() => expect(service.getAnalyzers).toHaveBeenCalled());
});
