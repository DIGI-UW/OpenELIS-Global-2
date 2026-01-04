import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import { Formik, Form } from "formik";
import { IntlProvider } from "react-intl";
import FormRadioField from "../FormRadioField";

const messages = {
  "test.radio.legend": "Test Radio Group",
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

const mockOptions = [
  { value: "radio1", label: "Radio 1" },
  { value: "radio2", label: "Radio 2" },
  { value: "radio3", label: "Radio 3" },
];

describe("FormRadioField", () => {
  test("should render with legend text", () => {
    renderWithFormik(
      <FormRadioField
        name="testRadio"
        legendText="test.radio.legend"
        options={mockOptions}
      />,
      { testRadio: "" },
    );

    expect(screen.getByText("Test Radio Group")).toBeInTheDocument();
  });

  test("should render all radio options", () => {
    renderWithFormik(
      <FormRadioField
        name="testRadio"
        legendText="test.radio.legend"
        options={mockOptions}
      />,
      { testRadio: "" },
    );

    expect(screen.getByLabelText("Radio 1")).toBeInTheDocument();
    expect(screen.getByLabelText("Radio 2")).toBeInTheDocument();
    expect(screen.getByLabelText("Radio 3")).toBeInTheDocument();
  });

  test("should bind to Formik selected value", () => {
    renderWithFormik(
      <FormRadioField
        name="testRadio"
        legendText="test.radio.legend"
        options={mockOptions}
      />,
      { testRadio: "radio2" },
    );

    const radio2 = screen.getByLabelText("Radio 2");
    expect(radio2).toBeChecked();
  });

  test("should update Formik value on selection change", () => {
    renderWithFormik(
      <FormRadioField
        name="testRadio"
        legendText="test.radio.legend"
        options={mockOptions}
      />,
      { testRadio: "radio1" },
    );

    const radio3 = screen.getByLabelText("Radio 3");
    fireEvent.click(radio3);

    expect(radio3).toBeChecked();
  });

  test("should render all options as disabled when disabled prop is true", () => {
    renderWithFormik(
      <FormRadioField
        name="testRadio"
        legendText="test.radio.legend"
        options={mockOptions}
        disabled={true}
      />,
      { testRadio: "" },
    );

    expect(screen.getByLabelText("Radio 1")).toBeDisabled();
    expect(screen.getByLabelText("Radio 2")).toBeDisabled();
    expect(screen.getByLabelText("Radio 3")).toBeDisabled();
  });
});
