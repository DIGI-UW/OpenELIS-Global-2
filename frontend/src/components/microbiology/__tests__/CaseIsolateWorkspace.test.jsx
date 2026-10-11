import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseIsolateWorkspace from "../CaseIsolateWorkspace";

const isolate = {
  id: "i1",
  label: "ISO-1",
  cultureId: "r1",
  cultureLabel: "PLATE-1",
  significance: "UNKNOWN",
  status: "PENDING",
  sourceSampleItemId: "s1",
  canIdentify: true,
  canReidentify: false,
  canAcknowledge: false,
  version: "v1",
  history: [],
};
const options = {
  methods: [{ id: "m1", value: "Biochemical identification" }],
  organisms: [{ id: "o1", value: "Escherichia coli" }],
  sampleTypes: [{ id: "isolate-type", value: "Configured isolate specimen" }],
  sources: [{ id: "s1", value: "SOURCE-1", sampleTypeId: "specimen" }],
  tests: [],
  organizations: [],
  reasons: [],
};
const api = (rows = [isolate]) => ({
  getIsolates: vi
    .fn()
    .mockResolvedValue({ isolates: rows, referrals: [], referred: false }),
  getIsolateOptions: vi.fn().mockResolvedValue(options),
  getCultures: vi.fn().mockResolvedValue([
    {
      id: "r1",
      containerIdentifier: "PLATE-1",
      outcome: "GROWTH",
      sourceSampleItemId: "s1",
    },
  ]),
  pickIsolate: vi
    .fn()
    .mockResolvedValue({ isolates: rows, referrals: [], referred: false }),
  isolateAction: vi
    .fn()
    .mockResolvedValue({ isolates: rows, referrals: [], referred: false }),
});
const show = (service, canWrite = true) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseIsolateWorkspace
        detail={{ id: "c1", labUnitId: "unit1", canWrite }}
        service={service}
      />
    </IntlProvider>,
  );
it("requires a growth culture and sends the technician's isolate observations", async () => {
  const service = api([]);
  show(service);
  fireEvent.click(await screen.findByRole("button", { name: "Pick isolate" }));
  const dialog = screen.getByRole("dialog");
  const save = within(dialog).getByRole("button", {
    name: "Save",
    exact: true,
  });
  expect(save).toBeDisabled();
  fireEvent.change(within(dialog).getByLabelText("Source culture row"), {
    target: { value: "r1" },
  });
  fireEvent.change(
    within(dialog).getByLabelText(messages["microbiology.isolate.gramStain"]),
    { target: { value: "Gram-negative rods" } },
  );
  fireEvent.change(within(dialog).getByLabelText("Colony morphology"), {
    target: { value: "Smooth colonies" },
  });
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.pickIsolate).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({
        cultureId: "r1",
        gramStain: "Gram-negative rods",
        colonyMorphology: "Smooth colonies",
      }),
    ),
  );
});
it("renders history for an outside reader without mutation controls", async () => {
  const service = api([
    {
      ...isolate,
      canIdentify: false,
      status: "FINAL",
      organismName: "Escherichia coli",
      history: [
        {
          id: "h1",
          organismName: "Escherichia coli",
          status: "PRELIMINARY",
          method: "Biochemical",
          by: "42",
          reason: "Initial local identification",
          at: "2026-01-01T10:00:00Z",
          identifiedAt: "2026-01-01T09:00:00Z",
        },
      ],
    },
  ]);
  show(service, false);
  await screen.findByRole("heading", { name: /ISO-1/ });
  expect(
    screen.queryByRole("button", { name: "Pick isolate" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Identify organism" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Refer isolate out" }),
  ).not.toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Identification history" }),
  );
  expect(screen.getByText("Initial local identification")).toBeVisible();
});
it("a validator must provide a reason to acknowledge the sender mismatch", async () => {
  const service = api([
    {
      ...isolate,
      received: true,
      mismatch: true,
      canIdentify: false,
      canAcknowledge: true,
      status: "FINAL",
    },
  ]);
  show(service, false);
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Acknowledge identification mismatch",
    }),
  );
  const dialog = screen.getByRole("dialog");
  const save = within(dialog).getByRole("button", {
    name: "Save",
    exact: true,
  });
  expect(save).toBeDisabled();
  fireEvent.change(
    within(dialog).getByLabelText(
      messages["microbiology.isolate.identificationReason"],
    ),
    { target: { value: "Reviewed sending laboratory report" } },
  );
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.isolateAction).toHaveBeenCalledWith(
      "c1",
      "i1",
      "acknowledge",
      expect.objectContaining({
        reason: "Reviewed sending laboratory report",
        version: "v1",
      }),
    ),
  );
});

it("keeps a failed pick open and shows the error inside the active dialog", async () => {
  const service = api([]);
  service.pickIsolate.mockRejectedValue(new Error("Save failed"));
  show(service);
  fireEvent.click(await screen.findByRole("button", { name: "Pick isolate" }));
  const dialog = screen.getByRole("dialog");
  fireEvent.change(within(dialog).getByLabelText("Source culture row"), {
    target: { value: "r1" },
  });
  fireEvent.click(
    within(dialog).getByRole("button", { name: "Save", exact: true }),
  );
  expect(
    await within(dialog).findByText(
      messages["microbiology.isolateWorkspace.saveError"],
    ),
  ).toBeVisible();
  expect(within(dialog).getByLabelText("Source culture row")).toHaveValue("r1");
  expect(
    within(dialog).getByRole("button", { name: "Save", exact: true }),
  ).toBeEnabled();
});

it("requires a reason and valid confidence before submitting a versioned reidentification", async () => {
  const service = api([
    {
      ...isolate,
      organismId: "o1",
      status: "FINAL",
      method: "Biochemical identification",
      confidence: 98,
      canIdentify: false,
      canReidentify: true,
    },
  ]);
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Reidentify", exact: true }),
  );
  const dialog = screen.getByRole("dialog");
  const fields = within(dialog);
  const save = fields.getByRole("button", { name: "Save", exact: true });
  const reason = fields.getByLabelText("Re-identification reason");
  expect(save).toBeDisabled();
  fireEvent.change(reason, { target: { value: "   " } });
  expect(save).toBeDisabled();
  fireEvent.change(reason, {
    target: { value: "Corrected after confirmatory panel" },
  });
  fireEvent.change(fields.getByLabelText("ID confidence (%)"), {
    target: { value: "101" },
  });
  expect(save).toBeDisabled();
  fireEvent.change(fields.getByLabelText("ID confidence (%)"), {
    target: { value: "99.5" },
  });
  fireEvent.change(fields.getByLabelText("Clinical significance"), {
    target: { value: "COLONIZER" },
  });
  fireEvent.change(fields.getByLabelText("Identification performed at"), {
    target: { value: "2026-01-15T09:30" },
  });
  expect(save).toBeEnabled();
  fireEvent.click(save);
  await waitFor(() =>
    expect(service.isolateAction).toHaveBeenCalledWith(
      "c1",
      "i1",
      "reidentify",
      expect.objectContaining({
        organismId: "o1",
        identificationStatus: "FINAL",
        identificationMethod: "m1",
        confidence: 99.5,
        significance: "COLONIZER",
        identifiedAt: new Date("2026-01-15T09:30").toISOString(),
        version: "v1",
        reason: "Corrected after confirmatory panel",
      }),
    ),
  );
});
