import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { Formik, Form } from "formik";
import { IntlProvider } from "react-intl";
import FormCheckboxField from "../FormCheckboxField";

const messages = {
  "test.checkbox": "Test Checkbox",
};

const renderWithFormik = (component, initialValues = {}) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <Formik initialValues={initialValues} onSubmit={() => {}}>
        <Form>{component}</Form>
      </Formik>
    </IntlProvider>,
  );
};

describe("FormCheckboxField", () => {
  test("should render with label text", () => {
    renderWithFormik(
      <FormCheckboxField name="testCheckbox" label="test.checkbox" />,
      { testCheckbox: false },
    );

    expect(screen.getByLabelText(/Test Checkbox/i)).toBeInTheDocument();
  });

  test("should bind to Formik checked value", () => {
    renderWithFormik(
      <FormCheckboxField name="testCheckbox" label="test.checkbox" />,
      { testCheckbox: true },
    );

    const checkbox = screen.getByLabelText(/Test Checkbox/i);
    expect(checkbox).toBeChecked();
  });

  test("should be unchecked when Formik value is false", () => {
    renderWithFormik(
      <FormCheckboxField name="testCheckbox" label="test.checkbox" />,
      { testCheckbox: false },
    );

    const checkbox = screen.getByLabelText(/Test Checkbox/i);
    expect(checkbox).not.toBeChecked();
  });

  test("should render as disabled when disabled prop is true", () => {
    renderWithFormik(
      <FormCheckboxField
        name="testCheckbox"
        label="test.checkbox"
        disabled={true}
      />,
      { testCheckbox: false },
    );

    const checkbox = screen.getByLabelText(/Test Checkbox/i);
    expect(checkbox).toBeDisabled();
  });
});
