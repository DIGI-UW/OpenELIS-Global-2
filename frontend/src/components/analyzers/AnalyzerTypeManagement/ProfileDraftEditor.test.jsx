import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import "@testing-library/jest-dom";
import messages from "../../../languages/en.json";
import {
  getAnalyzerTypeDraft,
  updateAnalyzerTypeDraft,
  getAnalyzerTypeControlRecognition,
  updateAnalyzerTypeControlRecognition,
  publishAnalyzerTypeDraft,
} from "../../../services/analyzerService";
import AnalyzerTypeLifecycleModals from "./AnalyzerTypeLifecycleModals";
import newFileProfile from "./__fixtures__/new-file-profile.json";
import ProfileDraftEditor from "./ProfileDraftEditor";
import fileProfile from "./__fixtures__/fluorocycler-xt-v3.json";
import astmProfile from "./__fixtures__/genexpert-astm-v5.json";

vi.mock("../../../services/analyzerService", () => ({
  publishAnalyzerTypeDraft: vi.fn(),
  getAnalyzerTypeDraft: vi.fn(),
  updateAnalyzerTypeDraft: vi.fn(),
  getAnalyzerTypeControlRecognition: vi.fn(),
  updateAnalyzerTypeControlRecognition: vi.fn(),
}));
const clone = (value) => JSON.parse(JSON.stringify(value));
let stored;
const recognition = () => ({
  draftId: stored.draftId,
  validationIssues: stored.validationIssues,
  recognition: {
    mode: "RULES",
    affirmedNoControlResults: false,
    conditions: [
      {
        key: "control",
        kind: "SPECIMEN_ID_STARTS_WITH",
        value: "QC-",
        editable: true,
      },
    ],
    availableSources: [],
  },
});
const mount = (profile = fileProfile) => {
  stored = {
    draftId: "draft-file",
    kind: "DUPLICATE",
    profile: clone(profile),
    validationIssues: [],
  };
  delete stored.profile.catalog;
  const onStateChange = vi.fn();
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <ProfileDraftEditor draft={clone(stored)} onStateChange={onStateChange} />
    </IntlProvider>,
  );
  return { ...view, onStateChange };
};
const replace = async (name, value) => {
  const input = screen.getByRole("textbox", { name });
  await userEvent.clear(input);
  await userEvent.type(input, value.replaceAll("{", "{{"));
};
const save = async () =>
  userEvent.click(
    screen.getByRole("button", { name: "Save and validate profile settings" }),
  );

beforeEach(() => {
  vi.clearAllMocks();
  getAnalyzerTypeDraft.mockImplementation((id, callback) =>
    callback(clone(stored)),
  );
  updateAnalyzerTypeDraft.mockImplementation((id, profile, callback) => {
    stored = { ...stored, profile: clone(profile) };
    callback(clone(stored));
  });
  getAnalyzerTypeControlRecognition.mockImplementation((id, callback) =>
    callback(recognition()),
  );
});

it.each([
  [
    "FluoroCycler FILE",
    fileProfile,
    "Filename pattern",
    "*.{ods,ODS,xlsx,XLSX,xls,XLS,csv}",
    ["configDefaults", "filePattern"],
  ],
  [
    "GeneXpert ASTM",
    astmProfile,
    "Protocol version",
    "E-1394-97-site",
    ["protocol", "version"],
  ],
])(
  "preserves the unabridged %s profile when editing one setting",
  async (_, original, label, value, path) => {
    const authored = clone(original);
    delete authored.catalog;
    const { onStateChange, unmount } = mount(authored);
    await replace(label, value);
    expect(onStateChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ dirty: true, publishable: false }),
    );
    await save();
    const expected = clone(authored);
    expected[path[0]][path[1]] = value;
    expect(updateAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-file",
      expected,
      expect.any(Function),
    );
    await screen.findByText("Profile settings saved");
    expect(onStateChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ dirty: false, publishable: true }),
    );
    const saved = clone(stored.profile);
    unmount();
    mount(saved);
    expect(screen.getByRole("textbox", { name: label })).toHaveValue(value);
  },
);

it("retains edits after a save failure and retries the same complete profile", async () => {
  const { onStateChange } = mount();
  updateAnalyzerTypeDraft.mockImplementationOnce((id, profile, callback) =>
    callback({ status: 503 }),
  );
  await replace("Filename pattern", "*.{ods,xlsx,xls}");
  await save();
  expect(await screen.findByText(/Could not save the draft/)).toBeVisible();
  expect(screen.getByRole("textbox", { name: "Filename pattern" })).toHaveValue(
    "*.{ods,xlsx,xls}",
  );
  expect(onStateChange).toHaveBeenLastCalledWith(
    expect.objectContaining({ publishable: false }),
  );
  await save();
  expect(updateAnalyzerTypeDraft).toHaveBeenCalledTimes(2);
  expect(updateAnalyzerTypeDraft.mock.calls[1][1]).toEqual(
    updateAnalyzerTypeDraft.mock.calls[0][1],
  );
  expect(await screen.findByText("Profile settings saved")).toBeVisible();
});

it("shows Bridge validation failures after saving and keeps publication blocked", async () => {
  const { onStateChange } = mount();
  updateAnalyzerTypeDraft.mockImplementation((id, profile, callback) => {
    stored = {
      ...stored,
      profile,
      validationIssues: ["Filename pattern does not match .xlsx"],
    };
    callback(clone(stored));
  });
  await replace("Filename pattern", "*.csv");
  await save();
  expect(updateAnalyzerTypeDraft).toHaveBeenCalledTimes(1);
  expect(
    await screen.findByText("Filename pattern does not match .xlsx"),
  ).toBeVisible();
  expect(onStateChange).toHaveBeenLastCalledWith(
    expect.objectContaining({ dirty: false, publishable: false }),
  );
});

it("does not overwrite a draft changed by another editor", async () => {
  const { onStateChange } = mount();
  await replace("Filename pattern", "*.csv");
  stored.profile.controlResultRecognition.rules["new-control"] = {
    ruleType: "SPECIMEN_ID_PREFIX",
    operand: "CONTROL-",
  };
  await save();
  expect(
    await screen.findByText(/The draft changed since it was opened/),
  ).toBeVisible();
  expect(updateAnalyzerTypeDraft).not.toHaveBeenCalled();
  expect(screen.getByRole("textbox", { name: "Filename pattern" })).toHaveValue(
    "*.csv",
  );
  expect(onStateChange).toHaveBeenLastCalledWith(
    expect.objectContaining({ publishable: false }),
  );
  await userEvent.click(
    screen.getByRole("button", {
      name: "Reload saved draft and discard local edits",
    }),
  );
  expect(screen.getByRole("textbox", { name: "Filename pattern" })).toHaveValue(
    fileProfile.configDefaults.filePattern,
  );
});

it("does not collapse duplicate file column names into a silently truncated mapping", async () => {
  mount();
  await userEvent.click(screen.getByRole("button", { name: "Add column" }));
  const rows = screen.getAllByRole("group", { name: /File column \d+/ });
  const added = within(rows[rows.length - 1]);
  await userEvent.type(
    added.getByRole("textbox", { name: "Column name in the file" }),
    "Sample ID",
  );
  await userEvent.selectOptions(
    added.getByLabelText("Meaning of the column"),
    "testCode",
  );
  expect(
    screen.getByText("Each column needs a unique name and a selected meaning."),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Save and validate profile settings" }),
  ).toBeDisabled();
  expect(updateAnalyzerTypeDraft).not.toHaveBeenCalled();
});

it("reloads saved recognition before a subsequent full-profile edit", async () => {
  mount();
  updateAnalyzerTypeControlRecognition.mockImplementation(
    (id, update, callback) => {
      stored.profile.controlResultRecognition = {
        mode: "RULES",
        rules: {
          "new-prefix": { ruleType: "SPECIMEN_ID_PREFIX", operand: "CONTROL-" },
        },
      };
      callback(recognition());
    },
  );
  await replace("Specimen ID prefix", "CONTROL-");
  expect(
    screen.getByRole("textbox", { name: "Filename pattern" }),
  ).toBeDisabled();
  await userEvent.click(
    screen.getByRole("button", { name: "Save control recognition" }),
  );
  await waitFor(() =>
    expect(
      screen.getByRole("textbox", { name: "Filename pattern" }),
    ).toBeEnabled(),
  );
  await replace("Filename pattern", "*.{ods,xlsx,xls}");
  await save();
  expect(
    updateAnalyzerTypeDraft.mock.calls[0][1].controlResultRecognition,
  ).toEqual({
    mode: "RULES",
    rules: {
      "new-prefix": { ruleType: "SPECIMEN_ID_PREFIX", operand: "CONTROL-" },
    },
  });
});

it("creates, saves, reopens, recognizes controls and explicitly publishes a new FILE profile", async () => {
  const empty = {
    $schema: newFileProfile.$schema,
    schemaVersion: "1.0",
    profileMeta: {
      id: "site.synthetic-file",
      displayName: "Synthetic CSV analyzer",
    },
  };
  stored = {
    draftId: "new-file",
    kind: "CREATE",
    profile: empty,
    validationIssues: ["Profile settings are incomplete"],
  };
  getAnalyzerTypeControlRecognition.mockImplementation((id, callback) =>
    callback({
      draftId: id,
      validationIssues: stored.validationIssues,
      recognition: {
        mode: stored.profile.controlResultRecognition?.mode,
        conditions: stored.profile.controlResultRecognition
          ? [
              {
                key: "specimen-prefix",
                kind: "SPECIMEN_ID_STARTS_WITH",
                value: "QC-",
                editable: true,
              },
            ]
          : [],
        availableSources: [],
      },
    }),
  );
  updateAnalyzerTypeDraft.mockImplementation((id, profile, callback) => {
    stored = {
      ...stored,
      profile: clone(profile),
      validationIssues: ["Control recognition is required"],
    };
    callback(clone(stored));
  });
  updateAnalyzerTypeControlRecognition.mockImplementation(
    (id, update, callback) => {
      expect(update).toEqual({
        mode: "RULES",
        affirmedNoControlResults: false,
        conditions: [
          {
            key: null,
            kind: "SPECIMEN_ID_STARTS_WITH",
            sourceKey: null,
            value: "QC-",
            controlLevel: null,
            controlType: null,
          },
        ],
      });
      stored.profile.controlResultRecognition = clone(
        newFileProfile.controlResultRecognition,
      );
      stored.validationIssues = [];
      getAnalyzerTypeControlRecognition(id, callback);
    },
  );
  publishAnalyzerTypeDraft.mockImplementation((id, callback) =>
    callback({
      profile: {
        ...clone(stored.profile),
        catalog: { revision: 1, source: "SITE", status: "ACTIVE" },
      },
    }),
  );
  const onSuccess = vi.fn();
  const editor = () => (
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerTypeLifecycleModals
        action="create"
        types={[]}
        draftId="new-file"
        onClose={vi.fn()}
        onError={vi.fn()}
        onSuccess={onSuccess}
        onDraftCreated={vi.fn()}
      />
    </IntlProvider>
  );
  const view = render(editor());
  const choose = async (name, value) =>
    userEvent.selectOptions(screen.getByRole("combobox", { name }), value);
  await replace("Profile version", "1.0");
  await choose("Evidence confidence", "LOW");
  await choose("Laboratory discipline", "MOLECULAR");
  await choose("Protocol", "FILE");
  await choose("Receives analyzer results", "true");
  await choose("Sends orders to the analyzer", "false");
  await choose("Supports a connection test", "true");
  await choose("Profile file format", "CSV");
  await choose("Default file format", "CSV");
  await replace("Filename pattern", "*.csv");
  await choose("First row contains column names", "true");
  await replace("Column delimiter", ",");
  const extensions = within(
    screen.getByRole("group", { name: "Supported file extensions" }),
  );
  await userEvent.click(extensions.getByRole("button", { name: "Add value" }));
  await userEvent.type(extensions.getByRole("textbox"), ".csv");
  for (const [source, field] of Object.entries(newFileProfile.column_mapping)) {
    await userEvent.click(screen.getByRole("button", { name: "Add column" }));
    const rows = screen.getAllByRole("group", { name: /File column \d+/ });
    const row = within(rows[rows.length - 1]);
    await userEvent.type(
      row.getByRole("textbox", { name: "Column name in the file" }),
      source,
    );
    await userEvent.selectOptions(
      row.getByLabelText("Meaning of the column"),
      field,
    );
  }
  const preference = within(
    screen.getByRole("group", {
      name: "Result value preference (first available value wins)",
    }),
  );
  await userEvent.click(preference.getByRole("button", { name: "Add value" }));
  await userEvent.selectOptions(preference.getByRole("combobox"), "result");
  await userEvent.click(
    screen.getByRole("button", { name: "Add connection field" }),
  );
  const field = within(
    screen.getByRole("group", { name: "Connection field 1" }),
  );
  await userEvent.type(
    field.getByRole("textbox", { name: "Setting name" }),
    "directory",
  );
  await userEvent.type(
    field.getByRole("textbox", { name: "Label translation key" }),
    "analyzer.connection.field.directory",
  );
  await userEvent.selectOptions(
    field.getByLabelText("Input type"),
    "FILE_PATH",
  );
  await userEvent.selectOptions(
    field.getByLabelText("Required during connection setup"),
    "true",
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Add analyzer test" }),
  );
  const testRow = within(
    screen.getByRole("group", { name: "Analyzer test 1" }),
  );
  for (const [label, value] of [
    ["Analyzer test code", "TEST_VL"],
    ["Suggested test name", "Synthetic viral load"],
    ["LOINC code", "20447-9"],
    ["Reported units", "copies/mL"],
  ]) {
    await userEvent.type(testRow.getByRole("textbox", { name: label }), value);
  }
  await userEvent.selectOptions(
    testRow.getByRole("combobox", { name: "Reported value type" }),
    "quantitative",
  );
  expect(
    screen.getByRole("button", { name: "Publish Profile" }),
  ).toBeDisabled();
  await save();
  const settings = clone(newFileProfile);
  delete settings.controlResultRecognition;
  expect(updateAnalyzerTypeDraft).toHaveBeenCalledWith(
    "new-file",
    settings,
    expect.any(Function),
  );
  expect(
    await screen.findByText("Control recognition is required"),
  ).toBeVisible();
  expect(publishAnalyzerTypeDraft).not.toHaveBeenCalled();
  view.unmount();
  render(editor());
  expect(screen.getByRole("textbox", { name: "Filename pattern" })).toHaveValue(
    "*.csv",
  );
  expect(
    screen.getAllByRole("group", { name: /File column \d+/ }),
  ).toHaveLength(4);
  await userEvent.click(
    screen.getByRole("radio", {
      name: messages["analyzerType.recognition.mode.rules"],
    }),
  );
  await userEvent.selectOptions(
    screen.getByLabelText(
      messages["analyzerType.recognition.condition.select"],
    ),
    "SPECIMEN_ID_STARTS_WITH|",
  );
  await userEvent.click(
    screen.getByRole("button", {
      name: messages["analyzerType.recognition.condition.add"],
    }),
  );
  await replace("Specimen ID prefix", "QC-");
  await userEvent.click(
    screen.getByRole("button", { name: "Save control recognition" }),
  );
  expect(stored.profile).toEqual(newFileProfile);
  const publish = screen.getByRole("button", { name: "Publish Profile" });
  await waitFor(() => expect(publish).toBeEnabled());
  await userEvent.click(publish);
  expect(publishAnalyzerTypeDraft).toHaveBeenCalledExactlyOnceWith(
    "new-file",
    expect.any(Function),
  );
  expect(onSuccess).toHaveBeenCalledWith("create");
});

it("edits test definitions without losing aliases, named results or unrelated profile behavior", async () => {
  const authored = clone(astmProfile);
  delete authored.catalog;
  authored.default_test_mappings[0].aliases = ["MTB", "MTB_ALT"];
  mount(authored);
  const row = within(screen.getByRole("group", { name: "Analyzer test 1" }));
  const name = row.getByRole("textbox", { name: "Suggested test name" });
  await userEvent.type(name, "Site tuberculosis assay");
  const namedValues = within(
    row.getByRole("group", { name: "Result values reported by this test" }),
  );
  await userEvent.click(namedValues.getByRole("button", { name: "Add value" }));
  const inputs = namedValues.getAllByRole("textbox");
  await userEvent.type(inputs[inputs.length - 1], "SITE REVIEW REQUIRED");
  await save();
  const expected = clone(authored);
  expected.default_test_mappings[0].test_name_hint = "Site tuberculosis assay";
  expected.default_test_mappings[0].values.push("SITE REVIEW REQUIRED");
  expect(updateAnalyzerTypeDraft).toHaveBeenCalledWith(
    "draft-file",
    expected,
    expect.any(Function),
  );
});
