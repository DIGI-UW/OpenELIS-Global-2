import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { Formik, Form } from "formik";
import { IntlProvider } from "react-intl";
import FormTextField from "../FormTextField";

// Mock messages with test labels
const messages = {
  "test.label": "Test Label",
  "email.label": "Email",
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

describe("FormTextField", () => {
  test("should render with label text", () => {
    renderWithFormik(<FormTextField name="testField" label="test.label" />, {
      testField: "",
    });

    expect(screen.getByLabelText("Test Label")).toBeInTheDocument();
  });

  test("should bind to Formik field value", () => {
    renderWithFormik(<FormTextField name="testField" label="test.label" />, {
      testField: "initial value",
    });

    const input = screen.getByLabelText("Test Label");
    expect(input).toHaveValue("initial value");
  });

  test("should update Formik value on user input", () => {
    renderWithFormik(<FormTextField name="testField" label="test.label" />, {
      testField: "",
    });

    const input = screen.getByLabelText("Test Label");
    fireEvent.change(input, { target: { value: "new value" } });

    expect(input).toHaveValue("new value");
  });

  test("should render as disabled when disabled prop is true", () => {
    renderWithFormik(
      <FormTextField name="testField" label="test.label" disabled={true} />,
      { testField: "" },
    );

    const input = screen.getByLabelText("Test Label");
    expect(input).toBeDisabled();
  });

  test("should support different input types", () => {
    renderWithFormik(
      <FormTextField name="email" label="email.label" type="email" />,
      { email: "" },
    );

    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("type", "email");
  });

  test("should show required indicator when required prop is true", () => {
    renderWithFormik(
      <FormTextField name="testField" label="test.label" required={true} />,
      { testField: "" },
    );

    const input = screen.getByLabelText("Test Label");
    expect(input).toBeRequired();
  });
});
