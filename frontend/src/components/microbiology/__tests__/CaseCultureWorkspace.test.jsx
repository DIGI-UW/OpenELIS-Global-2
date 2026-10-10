import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseCultureWorkspace from "../CaseCultureWorkspace";
const sources = [
  {
    sampleItemId: "s1",
    sampleTypeId: "type1",
    label: "SPEC-1",
    specimenType: "Sputum",
  },
];
const options = {
  sources,
  requireTrackedMedia: false,
  gramStainTest: { id: "gram1", value: "Configured Gram stain" },
  gramStainSampleTypeIds: ["type1"],
  atmospheres: [{ id: "air", value: "Aerobic" }],
  readings: [{ id: "growth", value: "Growth observed" }],
  quantities: [{ id: "scant", value: "Scant" }],
  extensionReasons: [{ id: "await", value: "Awaiting growth" }],
  media: [
    {
      id: 10,
      name: "Blood agar",
      trackLots: true,
      lots: [{ id: 20, lotNumber: "LOT-20" }],
      atmosphereId: "air",
      temperature: 35,
    },
  ],
  mediaLinks: [
    {
      mediumItemId: 10,
      sampleTypeId: "type1",
      duration: 48,
      durationUnit: "HOURS",
      checkIntervalHours: 8,
      loopVolume: 1,
    },
  ],
};
const row = {
  id: "r1",
  sourceSampleItemId: "s1",
  containerIdentifier: "PLATE-1",
  mediumItemId: 10,
  mediumName: "Blood agar",
  lotId: 20,
  lotNumber: "LOT-20",
  atmosphereId: "air",
  atmosphereName: "Aerobic",
  duration: 48,
  durationUnit: "HOURS",
  inoculatedAt: "2026-10-09T10:00:00Z",
  incubationEnds: "2026-10-11T10:00:00Z",
  readings: [],
  extensions: [],
  proposals: [],
  canEditInoculatedAt: true,
};
const detail = { id: "c1", canWrite: true, samples: sources };
const api = (rows = [row]) => ({
  getCultures: vi.fn().mockResolvedValue(rows),
  getCultureOptions: vi.fn().mockResolvedValue(options),
  inoculateCulture: vi.fn().mockResolvedValue(rows),
  cultureAction: vi.fn().mockResolvedValue(rows),
});
const show = (service, extra = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseCultureWorkspace
        detail={detail}
        service={service}
        onSelect={vi.fn()}
        onChooseTests={vi.fn()}
        {...extra}
      />
    </IntlProvider>,
  );
const change = (label, value) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
it("prefills medium links and requires a lot for tracked media", async () => {
  const service = api([]);
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Inoculate culture" }),
  );
  expect(screen.getByLabelText("Medium")).toHaveValue("10");
  expect(screen.getByLabelText("Incubation duration")).toHaveValue(48);
  expect(screen.getByLabelText("Atmosphere")).toHaveValue("air");
  expect(screen.getByLabelText("Lot not tracked")).toBeDisabled();
  change("Bottle or plate identifier", "P-NEW");
  expect(
    screen.getByRole("button", { name: "Save", exact: true }),
  ).toBeDisabled();
  change("Medium lot", "20");
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  await waitFor(() =>
    expect(service.inoculateCulture).toHaveBeenCalledWith(
      "c1",
      expect.objectContaining({
        mediumItemId: 10,
        lotId: 20,
        notTracked: false,
        duration: 48,
        atmosphereId: "air",
      }),
    ),
  );
});
it("renders parent children and separate read logs", async () => {
  const parent = {
    ...row,
    readings: [
      {
        id: "read1",
        readingName: "Growth observed",
        note: "First parent reading",
        incubationDay: 1,
        by: "writer",
        at: row.inoculatedAt,
      },
    ],
  };
  const child = {
    ...row,
    id: "r2",
    parentId: "r1",
    containerIdentifier: "CHILD-1",
    outcome: "NO_GROWTH",
  };
  show(api([parent, child]));
  const parentTile = await screen.findByTestId("culture-row-r1");
  const childTile = screen.getByTestId("culture-row-r2");
  expect(parentTile).toContainElement(childTile);
  expect(within(parentTile).getByText("First parent reading")).toBeVisible();
  expect(
    within(childTile).queryByText("First parent reading"),
  ).not.toBeInTheDocument();
});
it("read-only users see history without mutation controls", async () => {
  show(api(), { detail: { ...detail, canWrite: false } });
  await screen.findByTestId("culture-row-r1");
  for (const name of [
    "Inoculate culture",
    "Add reading",
    "Record outcome",
    "Extend incubation",
    "Gram stain",
  ])
    expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  expect(screen.getByText("LOT-20")).toBeVisible();
});
it("a first reading removes inoculated-time editing", async () => {
  show(
    api([
      {
        ...row,
        canEditInoculatedAt: false,
        readings: [
          {
            id: "first",
            readingName: "No growth observed",
            incubationDay: 1,
            by: "writer",
            at: row.inoculatedAt,
          },
        ],
      },
    ]),
  );
  await screen.findByTestId("culture-row-r1");
  expect(
    screen.queryByRole("button", { name: "Edit inoculated time" }),
  ).not.toBeInTheDocument();
});
it("an unconfirmed instrument proposal leaves the row incubating", async () => {
  const proposal = {
    id: "proposal1",
    signal: "NEGATIVE",
    receivedAt: row.inoculatedAt,
  };
  const service = api([{ ...row, proposals: [proposal] }]);
  show(service);
  await screen.findByText(/Instrument negative proposal: NEGATIVE/);
  expect(screen.getByText("Incubating")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Confirm no growth" }));
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  await waitFor(() =>
    expect(service.cultureAction).toHaveBeenCalledWith("c1", "r1", "outcome", {
      outcome: "NO_GROWTH",
      proposalId: "proposal1",
    }),
  );
});
it("a failed reading save retains its input and shows validation feedback", async () => {
  const service = api();
  service.cultureAction.mockRejectedValue({
    response: { message: "MICROBIOLOGY_CULTURE_CODE" },
  });
  show(service);
  fireEvent.click(await screen.findByRole("button", { name: "Add reading" }));
  change("Reading", "growth");
  change("Note", "Keep this note");
  fireEvent.click(screen.getByRole("button", { name: "Save", exact: true }));
  await screen.findByText("Select an active dictionary option.");
  expect(screen.getByLabelText("Note")).toHaveValue("Keep this note");
});
it("Gram shortcut passes the configured catalog test and the culture source", async () => {
  const choose = vi.fn();
  show(api(), { onChooseTests: choose });
  fireEvent.click(
    await screen.findByRole("button", { name: "Gram stain", exact: true }),
  );
  expect(choose).toHaveBeenCalledWith(
    expect.objectContaining({ id: "r1", source: sources[0] }),
    options.gramStainTest,
  );
});
it("a transfer revokes access to an open mutation form", async () => {
  const service = api();
  const view = show(service);
  fireEvent.click(await screen.findByRole("button", { name: "Add reading" }));
  view.rerender(
    <IntlProvider locale="en" messages={messages}>
      <CaseCultureWorkspace
        detail={{ ...detail, canWrite: false }}
        service={service}
      />
    </IntlProvider>,
  );
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(service.cultureAction).not.toHaveBeenCalled();
});

test("Gram stain is disabled on a source outside its specimen catalog", async () => {
  const service = api([{ ...row, sourceSampleItemId: "incompatible" }]);
  service.getCultureOptions.mockResolvedValue({
    ...options,
    sources: [
      ...sources,
      {
        sampleItemId: "incompatible",
        sampleTypeId: "type2",
        specimenType: "Other specimen",
      },
    ],
  });
  show(service);
  expect(
    await screen.findByRole("button", {
      name: messages["microbiology.culture.gramStain"],
    }),
  ).toBeDisabled();
});

test("growth still offers readings and incubation extension", async () => {
  show(api([{ ...row, outcome: "GROWTH", positiveAt: row.inoculatedAt }]));
  expect(
    await screen.findByRole("button", { name: "Add reading" }),
  ).toBeEnabled();
  expect(
    screen.getByRole("button", { name: "Extend incubation" }),
  ).toBeEnabled();
  expect(screen.queryByRole("button", { name: "Record outcome" })).toBeNull();
});

test("changing media applies source-specific incubation defaults before generic defaults", async () => {
  const service = api();
  service.getCultureOptions.mockResolvedValue({
    ...options,
    media: [
      ...options.media,
      { id: 11, name: "Long incubation medium", trackLots: false, lots: [] },
    ],
    mediaLinks: [
      ...options.mediaLinks,
      { mediumItemId: 11, duration: 3, durationUnit: "DAYS" },
      {
        mediumItemId: 11,
        sampleTypeId: "type1",
        duration: 56,
        durationUnit: "DAYS",
        checkIntervalHours: 24,
        loopVolume: 2,
      },
    ],
  });
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Inoculate culture" }),
  );
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.culture.mediumItemId"]),
    { target: { value: "11" } },
  );
  expect(
    screen.getByLabelText(messages["microbiology.culture.duration"]),
  ).toHaveValue(56);
  expect(
    screen.getByLabelText(messages["microbiology.culture.durationUnit"]),
  ).toHaveValue("DAYS");
  expect(
    screen.getByLabelText(messages["microbiology.culture.checkIntervalHours"]),
  ).toHaveValue(24);
  expect(
    screen.getByLabelText(messages["microbiology.culture.loopVolume"]),
  ).toHaveValue(2);
});
test("changing the source refreshes its media-link incubation defaults", async () => {
  const service = api();
  service.getCultureOptions.mockResolvedValue({
    ...options,
    sources: [
      ...sources,
      { sampleItemId: "s2", sampleTypeId: "type2", label: "SPEC-2" },
    ],
    mediaLinks: [
      ...options.mediaLinks,
      {
        mediumItemId: 10,
        sampleTypeId: "type2",
        duration: 7,
        durationUnit: "DAYS",
      },
    ],
  });
  show(service);
  fireEvent.click(
    await screen.findByRole("button", { name: "Inoculate culture" }),
  );
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.culture.sourceSampleItemId"]),
    { target: { value: "s2" } },
  );
  expect(
    screen.getByLabelText(messages["microbiology.culture.duration"]),
  ).toHaveValue(7);
  expect(
    screen.getByLabelText(messages["microbiology.culture.durationUnit"]),
  ).toHaveValue("DAYS");
});
