import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import LabelPresetFieldsEditor from "./LabelPresetFieldsEditor";
import { normalizeFields } from "./labelFieldCatalog";
import messages from "../../../languages/en.json";

const renderEditor = (props = {}) => {
  const onChange = vi.fn();
  const fields =
    props.fields ||
    normalizeFields([
      { fieldKey: "PATIENT_NAME", displayOrder: 2 },
      { fieldKey: "TESTS", displayOrder: 3, isRequired: true },
    ]);
  render(
    <IntlProvider locale="en" messages={messages}>
      <LabelPresetFieldsEditor
        fields={fields}
        heightMm={props.heightMm ?? 25}
        onChange={onChange}
        disabled={props.disabled ?? false}
      />
    </IntlProvider>,
  );
  return { onChange, fields };
};

const row = (key) => screen.getByTestId(`label-field-row-${key}`);
const keys = (fields) => fields.map((f) => f.fieldKey);

describe("LabelPresetFieldsEditor (OGC-1218)", () => {
  test("shows the fields by name in order, with Lab Number locked first", () => {
    renderEditor();
    const labNumber = row("LAB_NUMBER");
    expect(within(labNumber).getByText("Lab Number")).toBeInTheDocument();
    expect(within(labNumber).getByText("Position 1")).toBeInTheDocument();
    expect(
      within(labNumber).getByText(messages["admin.labelPresets.fields.locked"]),
    ).toBeInTheDocument();
    expect(within(labNumber).getByRole("checkbox")).toBeChecked();
    expect(within(labNumber).getByRole("checkbox")).toBeDisabled();
    expect(within(labNumber).queryByRole("button")).toBeNull();

    expect(
      within(row("PATIENT_NAME")).getByText("Patient Name"),
    ).toBeInTheDocument();
    expect(
      within(row("PATIENT_NAME")).getByText("Position 2"),
    ).toBeInTheDocument();
    expect(within(row("TESTS")).getByText("Position 3")).toBeInTheDocument();
    expect(within(row("TESTS")).getByRole("checkbox")).toBeChecked();
  });

  test("adds a chosen field at the next position", () => {
    const { onChange } = renderEditor();
    fireEvent.change(
      screen.getByLabelText(messages["admin.labelPresets.fields.add.label"]),
      {
        target: { value: "PATIENT_DOB" },
      },
    );
    fireEvent.click(screen.getByTestId("label-field-add-button"));

    const next = onChange.mock.calls[0][0];
    expect(keys(next)).toEqual([
      "LAB_NUMBER",
      "PATIENT_NAME",
      "TESTS",
      "PATIENT_DOB",
    ]);
    expect(next[3].displayOrder).toBe(4);
    expect(next[3].isRequired).toBe(false);
  });

  test("the picker offers only fields not yet on the label", () => {
    renderEditor();
    const options = within(
      screen.getByLabelText(messages["admin.labelPresets.fields.add.label"]),
    )
      .getAllByRole("option")
      .map((o) => o.textContent);
    expect(options).not.toContain("Patient Name");
    expect(options).not.toContain("Tests");
    expect(options).not.toContain("Lab Number");
    expect(options).toContain("Patient ID");
    expect(options).toHaveLength(1 + 15 - 2);
  });

  test("moves a field up or down with buttons the keyboard can reach, and stops at the edges", () => {
    const { onChange } = renderEditor();
    expect(screen.getByTestId("label-field-up-PATIENT_NAME")).toBeDisabled();
    expect(screen.getByTestId("label-field-down-TESTS")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Move Tests up" })).toBeEnabled();

    fireEvent.click(screen.getByTestId("label-field-up-TESTS"));
    const next = onChange.mock.calls[0][0];
    expect(keys(next)).toEqual(["LAB_NUMBER", "TESTS", "PATIENT_NAME"]);
    expect(next.map((f) => f.displayOrder)).toEqual([1, 2, 3]);
  });

  test("removes a field and toggles required", () => {
    const { onChange } = renderEditor();
    fireEvent.click(
      screen.getByRole("button", { name: "Remove Patient Name" }),
    );
    expect(keys(onChange.mock.calls[0][0])).toEqual(["LAB_NUMBER", "TESTS"]);

    fireEvent.click(within(row("PATIENT_NAME")).getByRole("checkbox"));
    const toggled = onChange.mock.calls[1][0];
    expect(toggled.find((f) => f.fieldKey === "PATIENT_NAME").isRequired).toBe(
      true,
    );
  });

  test("warns softly when the fields are unlikely to fit the height, without blocking", () => {
    renderEditor({
      heightMm: 12,
      fields: normalizeFields(
        ["PATIENT_NAME", "PATIENT_ID", "PATIENT_DOB", "TESTS", "SITE_ID"].map(
          (fieldKey, index) => ({ fieldKey, displayOrder: index + 2 }),
        ),
      ),
    });
    const hint = screen.getByTestId("label-fields-fit-hint");
    expect(hint).toHaveTextContent(
      "The 5 selected fields may not fit a 12 mm label; about 2 rows fit above the barcode.",
    );
    expect(hint).toHaveTextContent("You can still save.");
  });

  test("the shipped Specimen default opens without a fit warning at 25 mm", () => {
    renderEditor({
      heightMm: 25,
      fields: normalizeFields(
        [
          "PATIENT_NAME",
          "PATIENT_DOB",
          "PATIENT_ID",
          "PATIENT_SEX",
          "COLLECTION_DATETIME",
          "COLLECTED_BY",
          "TESTS",
        ].map((fieldKey, index) => ({ fieldKey, displayOrder: index + 2 })),
      ),
    });
    expect(screen.queryByTestId("label-fields-fit-hint")).toBeNull();
  });

  test("shows no hint for a short list", () => {
    renderEditor();
    expect(screen.queryByTestId("label-fields-fit-hint")).toBeNull();
  });

  test("says so when every field is on the label", () => {
    renderEditor({
      fields: normalizeFields(
        [
          "PATIENT_NAME",
          "PATIENT_ID",
          "PATIENT_DOB",
          "PATIENT_SEX",
          "SITE_ID",
          "COLLECTION_DATETIME",
          "COLLECTED_BY",
          "TESTS",
          "SPECIMEN_TYPE",
          "BLOCK_ID",
          "SLIDE_ID",
          "STAIN_TYPE",
          "CASE_NUMBER",
          "STORAGE_LOCATION",
          "EXPIRY_DATE",
        ].map((fieldKey, index) => ({ fieldKey, displayOrder: index + 2 })),
      ),
      heightMm: 200,
    });
    expect(
      screen.getByText(messages["admin.labelPresets.fields.allAdded"]),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("label-field-add-button")).toBeNull();
  });

  test("disabled leaves every control inert", () => {
    renderEditor({ disabled: true });
    expect(screen.getByTestId("label-field-add-button")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Remove Tests" })).toBeDisabled();
    expect(within(row("TESTS")).getByRole("checkbox")).toBeDisabled();
  });
});
