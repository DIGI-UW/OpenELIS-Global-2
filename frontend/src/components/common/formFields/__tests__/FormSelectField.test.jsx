import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { Formik, Form } from "formik";
import { IntlProvider } from "react-intl";
import FormSelectField from "../FormSelectField";

// Mock messages with test labels
const messages = {
  "test.select": "Test Select",
  "select.default": "Select an option",
  "error.required": "This field is required",
};

const renderWithFormik = (
  component,
  initialValues = {},
  validationSchema = null,
) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <Formik
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={() => {}}
      >
        <Form>{component}</Form>
      </Formik>
    </IntlProvider>,
  );
};

const mockOptions = [
  { value: "option1", label: "Option 1" },
  { value: "option2", label: "Option 2" },
  { value: "option3", label: "Option 3" },
];

describe("FormSelectField", () => {
  test("should render with label text", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
      />,
      { testSelect: "" },
    );

    expect(screen.getByLabelText("Test Select")).toBeInTheDocument();
  });

  test("should render all options", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
      />,
      { testSelect: "" },
    );

    const select = screen.getByLabelText("Test Select");
    expect(select.querySelector('option[value="option1"]')).toBeInTheDocument();
    expect(select.querySelector('option[value="option2"]')).toBeInTheDocument();
    expect(select.querySelector('option[value="option3"]')).toBeInTheDocument();
  });

  test("should include empty option by default", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
      />,
      { testSelect: "" },
    );

    const select = screen.getByLabelText("Test Select");
    const options = select.querySelectorAll("option");
    expect(options[0]).toHaveValue("");
    expect(options[0].textContent).toBe("Select an option");
  });

  test("should bind to Formik field value", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
      />,
      { testSelect: "option2" },
    );

    const select = screen.getByLabelText("Test Select");
    expect(select).toHaveValue("option2");
  });

  test("should update Formik value on selection change", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
      />,
      { testSelect: "" },
    );

    const select = screen.getByLabelText("Test Select");
    fireEvent.change(select, { target: { value: "option3" } });

    expect(select).toHaveValue("option3");
  });

  test("should render as disabled when disabled prop is true", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
        disabled={true}
      />,
      { testSelect: "" },
    );

    const select = screen.getByLabelText("Test Select");
    expect(select).toBeDisabled();
  });

  test("should show required indicator when required prop is true", () => {
    renderWithFormik(
      <FormSelectField
        name="testSelect"
        label="test.select"
        options={mockOptions}
        required={true}
      />,
      { testSelect: "" },
    );

    const select = screen.getByLabelText("Test Select");
    expect(select).toBeRequired();
  });
});
