import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { Formik, Form } from "formik";
import { IntlProvider } from "react-intl";
import FormTextAreaField from "../FormTextAreaField";

const messages = {
  "test.textarea": "Test TextArea",
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

describe("FormTextAreaField", () => {
  test("should render with label text", () => {
    renderWithFormik(
      <FormTextAreaField name="testArea" label="test.textarea" />,
      { testArea: "" },
    );

    expect(screen.getByLabelText("Test TextArea")).toBeInTheDocument();
  });

  test("should bind to Formik field value", () => {
    renderWithFormik(
      <FormTextAreaField name="testArea" label="test.textarea" />,
      { testArea: "initial text" },
    );

    const textarea = screen.getByLabelText("Test TextArea");
    expect(textarea).toHaveValue("initial text");
  });

  test("should update Formik value on user input", () => {
    renderWithFormik(
      <FormTextAreaField name="testArea" label="test.textarea" />,
      { testArea: "" },
    );

    const textarea = screen.getByLabelText("Test TextArea");
    fireEvent.change(textarea, { target: { value: "new text content" } });

    expect(textarea).toHaveValue("new text content");
  });

  test("should render as disabled when disabled prop is true", () => {
    renderWithFormik(
      <FormTextAreaField
        name="testArea"
        label="test.textarea"
        disabled={true}
      />,
      { testArea: "" },
    );

    const textarea = screen.getByLabelText("Test TextArea");
    expect(textarea).toBeDisabled();
  });

  test("should show required indicator when required prop is true", () => {
    renderWithFormik(
      <FormTextAreaField
        name="testArea"
        label="test.textarea"
        required={true}
      />,
      { testArea: "" },
    );

    const textarea = screen.getByLabelText("Test TextArea");
    expect(textarea).toBeRequired();
  });

  test("should use custom rows value", () => {
    renderWithFormik(
      <FormTextAreaField name="testArea" label="test.textarea" rows={10} />,
      { testArea: "" },
    );

    const textarea = screen.getByLabelText("Test TextArea");
    expect(textarea).toHaveAttribute("rows", "10");
  });
});
