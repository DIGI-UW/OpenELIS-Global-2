import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import CaseTestingWorkspace from "../CaseTestingWorkspace";
import messages from "../../../languages/en.json";
vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, cb) =>
    cb(
      url === "/rest/users"
        ? [{ id: "8", value: "Other technician" }]
        : { tests: [{ id: "30", name: "Follow-up test" }], panels: [] },
    ),
  ),
}));
const detail = {
  id: "case-1",
  canWrite: true,
  canValidate: true,
  samples: [
    {
      sampleItemId: "sample-1",
      sampleTypeId: "type-1",
      specimenType: "Sputum",
    },
  ],
};
const component = (id, name) => ({
  analysisId: "20",
  testResultComponentId: id,
  testName: name,
  resultType: "N",
  resultValue: "",
  rawResultValue: "",
  significantDigits: 0,
  dictionaryResults: [],
});
const assay = {
  analysisId: "20",
  testId: "10",
  testName: "Two-component assay",
  status: "NotStarted",
  version: "123",
  placement: "INITIAL",
  canEdit: true,
  canValidate: false,
  testedElsewhere: false,
  components: [component("primary", "MTB"), component("rif", "Rifampicin")],
};
const service = () => ({
  getTests: vi.fn().mockResolvedValue([assay]),
  getTimeline: vi.fn().mockResolvedValue([]),
  saveResults: vi
    .fn()
    .mockResolvedValue([{ ...assay, status: "TechnicalAcceptance" }]),
  setTestedElsewhere: vi.fn(),
  addTests: vi.fn().mockResolvedValue([assay]),
  addNote: vi.fn(),
  validateResult: vi.fn(),
});
const show = (api, data = detail) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseTestingWorkspace detail={data} service={api} />
    </IntlProvider>,
  );
const openEditor = async () =>
  fireEvent.click(await screen.findByRole("button", { name: "Enter or edit" }));
const enter = (name, value) =>
  fireEvent.change(
    screen.getByRole("textbox", { name: `Result for ${name}` }),
    { target: { value } },
  );
describe("case testing workspace", () => {
  test("edits components independently and submits the current version", async () => {
    const api = service();
    show(api);
    await openEditor();
    enter("MTB", "12");
    enter("Rifampicin", "34");
    expect(screen.getByRole("textbox", { name: "Result for MTB" })).toHaveValue(
      "12",
    );
    fireEvent.click(screen.getByRole("button", { name: "Save results" }));
    await waitFor(() =>
      expect(api.saveResults).toHaveBeenCalledWith(
        "case-1",
        "20",
        expect.objectContaining({
          version: "123",
          components: [
            expect.objectContaining({ componentId: "primary", value: "12" }),
            expect.objectContaining({ componentId: "rif", value: "34" }),
          ],
        }),
      ),
    );
    await screen.findByText("Awaiting validation");
  });
  test.each(["M", "C"])(
    "empty %s selections cannot be submitted",
    async (type) => {
      const api = service();
      api.getTests.mockResolvedValue([
        {
          ...assay,
          components: [
            {
              ...component("selection", "Selection"),
              resultType: type,
              multiSelectResultValues: "{}",
            },
          ],
        },
      ]);
      show(api);
      await openEditor();
      expect(
        screen.getByRole("button", { name: "Save results" }),
      ).toBeDisabled();
      expect(api.saveResults).not.toHaveBeenCalled();
    },
  );
  test.each(["M", "C"])(
    "a selected %s result remains saveable",
    async (type) => {
      const api = service();
      api.getTests.mockResolvedValue([
        {
          ...assay,
          components: [
            {
              ...component("selection", "Selection"),
              resultType: type,
              multiSelectResultValues: '{"1":"42"}',
              dictionaryResults: [{ id: "42", value: "Selected option" }],
            },
          ],
        },
      ]);
      show(api);
      await openEditor();
      expect(
        screen.getByRole("button", { name: "Save results" }),
      ).toBeEnabled();
    },
  );
  test("blank selection groups cannot be submitted", async () => {
    const api = service();
    api.getTests.mockResolvedValue([
      {
        ...assay,
        components: [
          {
            ...component("selection", "Selection"),
            resultType: "M",
            multiSelectResultValues: '{"1":" , "}',
          },
        ],
      },
    ]);
    show(api);
    await openEditor();
    expect(screen.getByRole("button", { name: "Save results" })).toBeDisabled();
  });
  test("invalid precision blocks saving and failed saves preserve both fields", async () => {
    const api = service();
    api.saveResults.mockRejectedValue(new Error("stale"));
    show(api);
    await openEditor();
    enter("MTB", "12.5");
    expect(screen.getByRole("button", { name: "Save results" })).toBeDisabled();
    enter("MTB", "12");
    enter("Rifampicin", "34");
    fireEvent.click(screen.getByRole("button", { name: "Save results" }));
    await screen.findByText(messages["microbiology.testing.preservedDraft"]);
    expect(screen.getByRole("textbox", { name: "Result for MTB" })).toHaveValue(
      "12",
    );
    expect(
      screen.getByRole("textbox", { name: "Result for Rifampicin" }),
    ).toHaveValue("34");
  });
  test("out-of-unit readers cannot edit, validate, add tests, or append notes", async () => {
    const api = service();
    api.getTests.mockResolvedValue([
      { ...assay, canEdit: true, canValidate: true },
    ]);
    show(api, { ...detail, canWrite: false, canValidate: false });
    await screen.findByText("Two-component assay");
    for (const name of [
      "Enter or edit",
      "Validate",
      "Add selected tests",
      "Save note",
    ])
      expect(screen.queryByRole("button", { name })).toBeNull();
    expect(screen.queryByLabelText("New note")).toBeNull();
  });
  test("switching scope shows additional analyses", async () => {
    const api = service();
    api.getTests.mockResolvedValue([
      assay,
      {
        ...assay,
        analysisId: "21",
        testName: "Additional assay",
        placement: "ADDITIONAL",
      },
    ]);
    show(api);
    await screen.findByText("Two-component assay");
    expect(screen.queryByText("Additional assay")).toBeNull();
    fireEvent.change(screen.getByLabelText("Testing section"), {
      target: { value: "ADDITIONAL" },
    });
    expect(screen.getByText("Additional assay")).toBeTruthy();
    expect(screen.queryByText("Two-component assay")).toBeNull();
  });
  test("notes preserve a failed draft and display the attributed saved entry", async () => {
    const api = service();
    api.addNote.mockRejectedValueOnce(new Error("failed")).mockResolvedValue({
      id: "n1",
      activityType: "MANUAL_NOTE",
      note: "Bench observation",
      occurredAt: "2026-10-01T10:00:00Z",
      performedByDisplay: "Technician",
    });
    show(api);
    await screen.findByText("Two-component assay");
    fireEvent.change(screen.getByLabelText("New note"), {
      target: { value: "Bench observation" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save note" }));
    await screen.findByText(messages["microbiology.testing.saveError"]);
    expect(screen.getByLabelText("New note")).toHaveValue("Bench observation");
    fireEvent.click(screen.getByRole("button", { name: "Save note" }));
    await screen.findByText("Technician");
    expect(screen.getByLabelText("New note")).toHaveValue("");
    expect(api.addNote).toHaveBeenLastCalledWith("case-1", "Bench observation");
  });
  test("provenance saves preserve the result draft and refresh its version", async () => {
    const api = service();
    api.setTestedElsewhere.mockResolvedValue([
      {
        ...assay,
        version: "124",
        testedElsewhere: true,
        performingUserId: "8",
        performedAt: "2026-10-01",
        performedByDisplay: "Other technician",
      },
    ]);
    show(api);
    await openEditor();
    enter("MTB", "12");
    fireEvent.click(screen.getByLabelText("Tested elsewhere"));
    const picker = screen.getByRole("combobox", { name: "Performed by" });
    fireEvent.click(picker);
    fireEvent.keyDown(picker, { key: "ArrowDown" });
    fireEvent.keyDown(picker, { key: "Enter" });
    fireEvent.change(screen.getByLabelText("Date performed"), {
      target: { value: "2026-10-01" },
    });
    expect(screen.getByRole("button", { name: "Save results" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Save provenance" }));
    await waitFor(() =>
      expect(api.setTestedElsewhere).toHaveBeenCalledWith(
        "case-1",
        "20",
        expect.objectContaining({
          performingUserId: "8",
          testedElsewhere: true,
        }),
      ),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Save results" }),
      ).not.toBeDisabled(),
    );
    expect(screen.getByRole("textbox", { name: "Result for MTB" })).toHaveValue(
      "12",
    );
    fireEvent.click(screen.getByRole("button", { name: "Save results" }));
    await waitFor(() =>
      expect(api.saveResults).toHaveBeenCalledWith(
        "case-1",
        "20",
        expect.objectContaining({ version: "124" }),
      ),
    );
  });
});

test("critical acknowledgement applies only to the flagged component", async () => {
  const api = service();
  api.saveResults.mockRejectedValueOnce(
    Object.assign(new Error("acknowledgement required"), {
      response: {
        status: 422,
        code: "ACKNOWLEDGEMENT_REQUIRED",
        acknowledgementRequired: [
          {
            analysisId: "20",
            componentId: "primary",
            kind: "CRITICAL",
            value: "12",
            testName: "MTB",
          },
        ],
      },
    }),
  );
  show(api);
  await openEditor();
  enter("MTB", "12");
  enter("Rifampicin", "34");
  fireEvent.click(screen.getByRole("button", { name: "Save results" }));
  const confirm = await screen.findByRole("button", {
    name: /Acknowledge and save$/,
  });
  fireEvent.click(confirm);
  await waitFor(() =>
    expect(api.saveResults).toHaveBeenLastCalledWith(
      "case-1",
      "20",
      expect.objectContaining({
        components: [
          expect.objectContaining({
            componentId: "primary",
            criticalAcknowledged: true,
          }),
          expect.objectContaining({
            componentId: "rif",
            criticalAcknowledged: false,
          }),
        ],
      }),
    ),
  );
});

// Real workspace + picker + culture tree; HTTP collaborators are stubbed.
// Guards the UI wiring and source selection, not persistence or access policy.
describe("culture test chooser", () => {
  const cultureApi = () => {
    const api = service();
    api.getCultures = vi.fn().mockResolvedValue([
      {
        id: "culture-1",
        sourceSampleItemId: "sample-1",
        containerIdentifier: "PLATE-1",
        mediumName: "Blood agar",
        readings: [],
        extensions: [],
        proposals: [],
      },
    ]);
    api.getCultureOptions = vi.fn().mockResolvedValue({
      sources: detail.samples,
      media: [],
      mediaLinks: [],
      atmospheres: [],
      readings: [],
      quantities: [],
      extensionReasons: [],
      gramStainTest: { id: "30", value: "Follow-up test" },
      gramStainSampleTypeIds: ["type-1"],
    });
    return api;
  };
  const openCulture = async (api) => {
    show(api, {
      ...detail,
      samples: [
        ...detail.samples,
        {
          sampleItemId: "sample-2",
          sampleTypeId: "type-2",
          specimenType: "Blood",
        },
      ],
    });
    await screen.findByText("Two-component assay");
    fireEvent.change(
      screen.getByLabelText(messages["microbiology.testing.scope"]),
      { target: { value: "CULTURE" } },
    );
    await screen.findByRole("heading", { name: /PLATE-1/ });
  };
  test("Gram shortcut visibly selects its catalog test and submits only the culture's source", async () => {
    const api = cultureApi();
    await openCulture(api);
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["microbiology.culture.gramStain"],
      }),
    );
    const chooser = document.getElementById("culture-test-chooser");
    expect(
      within(chooser).getByRole("button", { name: /Sputum.*1/ }),
    ).toBeInTheDocument();
    expect(within(chooser).queryByRole("button", { name: /Blood/ })).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Add selected tests" }));
    await waitFor(() =>
      expect(api.addTests).toHaveBeenCalledWith("case-1", {
        placement: "CULTURE",
        cultureId: "culture-1",
        sampleItemId: "sample-1",
        testIds: ["30"],
        panelIds: [],
      }),
    );
    expect(
      screen.getByRole("button", { name: "Add selected tests" }),
    ).toBeDisabled();
  });
  test("manual selection updates the selected culture picker and becomes saveable", async () => {
    const api = cultureApi();
    await openCulture(api);
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["microbiology.culture.testOnCulture"],
      }),
    );
    const chooser = document.getElementById("culture-test-chooser");
    fireEvent.click(within(chooser).getByRole("button", { name: /Sputum.*0/ }));
    fireEvent.click(
      await within(chooser).findByRole("button", { name: "Follow-up test" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Add selected tests" }));
    await waitFor(() =>
      expect(api.addTests).toHaveBeenCalledWith(
        "case-1",
        expect.objectContaining({
          cultureId: "culture-1",
          sampleItemId: "sample-1",
          testIds: ["30"],
        }),
      ),
    );
  });
});
