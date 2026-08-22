import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getAnalyzerTypeControlRecognition,
  updateAnalyzerTypeControlRecognition,
} from "../../../services/analyzerService";
import ControlRecognitionDraftEditor from "./ControlRecognitionDraftEditor";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerTypeControlRecognition: vi.fn(),
  updateAnalyzerTypeControlRecognition: vi.fn(),
}));

const rulesDraft = {
  draftId: "draft-1",
  kind: "DUPLICATE",
  displayName: "Site GeneXpert",
  updatedBy: "17",
  updatedAt: "2026-08-22T12:00:00Z",
  validationIssues: [],
  recognition: {
    mode: "RULES",
    affirmedNoControlResults: false,
    description: "Any listed condition identifies a control result.",
    conditions: [
      {
        key: "order-action-control",
        kind: "FIELD_VALUE_EQUALS",
        sourceKey: "source-safe-1",
        sourceLabel: "Order field 12",
        description: "Order field 12 equals Q",
        value: "Q",
        editable: true,
        controlLevel: null,
        controlType: null,
      },
      {
        key: "configured-control-pattern",
        kind: "CONFIGURED_SPECIMEN_ID_PATTERN",
        sourceKey: null,
        sourceLabel: "Specimen ID",
        description: "Specimen ID matches a configured pattern",
        value: null,
        editable: false,
        controlLevel: null,
        controlType: null,
      },
    ],
    availableSources: [{ key: "source-safe-1", label: "Order field 12" }],
  },
};

const noneDraft = {
  ...rulesDraft,
  updatedAt: "2026-08-22T12:05:00Z",
  recognition: {
    ...rulesDraft.recognition,
    mode: "NONE",
    affirmedNoControlResults: true,
    description: "This analyzer interface transports no control results.",
    conditions: [],
  },
};

const renderEditor = (props = {}) => {
  const onStateChange = vi.fn();
  const onError = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <ControlRecognitionDraftEditor
        draftId="draft-1"
        onStateChange={onStateChange}
        onError={onError}
        {...props}
      />
    </IntlProvider>,
  );
  return { onError, onStateChange };
};

describe("ControlRecognitionDraftEditor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerTypeControlRecognition.mockImplementation((draftId, callback) =>
      callback(rulesDraft),
    );
    updateAnalyzerTypeControlRecognition.mockImplementation(
      (draftId, update, callback) =>
        callback(
          update.mode === "NONE"
            ? noneDraft
            : {
                ...rulesDraft,
                recognition: {
                  ...rulesDraft.recognition,
                  conditions: update.conditions.map((condition, index) => ({
                    ...condition,
                    key: condition.key || `condition-${index + 1}`,
                    sourceLabel:
                      condition.kind === "SPECIMEN_ID_STARTS_WITH"
                        ? "Specimen ID"
                        : "Order field 12",
                    description:
                      condition.kind === "SPECIMEN_ID_STARTS_WITH"
                        ? `Specimen ID starts with ${condition.value}`
                        : `Order field 12 equals ${condition.value}`,
                    editable: true,
                  })),
                },
              },
        ),
    );
  });

  it("edits plain-language RULES without exposing raw fields or patterns", async () => {
    const { onError, onStateChange } = renderEditor();

    expect(
      await screen.findByRole("heading", {
        name: "Control result recognition",
      }),
    ).toBeVisible();
    expect(screen.getByText("Order field 12 equals Q")).toBeVisible();
    expect(
      screen.getByText("Specimen ID matches a configured pattern"),
    ).toBeVisible();
    expect(document.body).not.toHaveTextContent("O.12");
    expect(document.body).not.toHaveTextContent("^(CNEG|NTC)");

    const value = screen.getByRole("textbox", {
      name: "Order field 12 value",
    });
    await userEvent.clear(value);
    await userEvent.type(value, "CONTROL");
    const save = screen.getByRole("button", {
      name: "Save control recognition",
    });
    expect(save).toBeEnabled();
    expect(onStateChange).toHaveBeenLastCalledWith(
      expect.objectContaining({
        loaded: true,
        dirty: true,
        publishable: false,
      }),
    );

    await userEvent.click(save);

    expect(updateAnalyzerTypeControlRecognition).toHaveBeenCalledWith(
      "draft-1",
      {
        mode: "RULES",
        affirmedNoControlResults: false,
        conditions: [
          {
            key: "order-action-control",
            kind: "FIELD_VALUE_EQUALS",
            sourceKey: "source-safe-1",
            value: "CONTROL",
            controlLevel: null,
            controlType: null,
          },
          {
            key: "configured-control-pattern",
            kind: "CONFIGURED_SPECIMEN_ID_PATTERN",
            sourceKey: null,
            value: null,
            controlLevel: null,
            controlType: null,
          },
        ],
      },
      expect.any(Function),
    );
    expect(await screen.findByText("Control recognition saved")).toBeVisible();
    expect(onStateChange).toHaveBeenLastCalledWith(
      expect.objectContaining({
        loaded: true,
        dirty: false,
        publishable: true,
      }),
    );
    expect(onError).not.toHaveBeenCalled();
  });

  it("adds an OR condition using only Bridge-provided sources", async () => {
    renderEditor();
    await screen.findByText("Order field 12 equals Q");

    await userEvent.selectOptions(
      screen.getByRole("combobox", { name: "New condition" }),
      "SPECIMEN_ID_STARTS_WITH|",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Add condition" }),
    );
    const conditionGroup = screen.getByRole("group", {
      name: "New recognition condition",
    });
    await userEvent.type(
      within(conditionGroup).getByRole("textbox", {
        name: "Specimen ID prefix",
      }),
      "QC-",
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Save control recognition" }),
    );

    expect(updateAnalyzerTypeControlRecognition).toHaveBeenCalledWith(
      "draft-1",
      expect.objectContaining({
        mode: "RULES",
        conditions: expect.arrayContaining([
          expect.objectContaining({
            key: null,
            kind: "SPECIMEN_ID_STARTS_WITH",
            sourceKey: null,
            value: "QC-",
          }),
        ]),
      }),
      expect.any(Function),
    );
  });

  it("requires explicit affirmation before saving NONE", async () => {
    renderEditor();
    await screen.findByText("Order field 12 equals Q");

    await userEvent.click(
      screen.getByRole("radio", {
        name: "This interface does not transmit control results",
      }),
    );
    const affirmation = screen.getByRole("checkbox", {
      name: "I confirm this interface does not transmit control results",
    });
    const save = screen.getByRole("button", {
      name: "Save control recognition",
    });
    expect(affirmation).not.toBeChecked();
    expect(save).toBeDisabled();

    await userEvent.click(affirmation);
    expect(save).toBeEnabled();
    await userEvent.click(save);

    expect(updateAnalyzerTypeControlRecognition).toHaveBeenCalledWith(
      "draft-1",
      {
        mode: "NONE",
        affirmedNoControlResults: true,
        conditions: [],
      },
      expect.any(Function),
    );
    expect(
      await screen.findByText(
        "This analyzer interface transports no control results.",
      ),
    ).toBeVisible();
  });
});
