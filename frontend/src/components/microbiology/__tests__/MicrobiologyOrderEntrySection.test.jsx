import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import MicrobiologyOrderEntrySection from "../MicrobiologyOrderEntrySection";
import messages from "../../../languages/en.json";

const renderSection = (samples, orderFormValues, setOrderFormValues) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MicrobiologyOrderEntrySection
        samples={samples}
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
      />
    </IntlProvider>,
  );

const baseForm = {
  microbiologyOrderDetail: {
    patientOrigin: "",
    numberOfSets: "",
    clinicalHistory: "",
    antibioticExposure: "",
    criticalNotificationPreference: "",
  },
};

describe("MicrobiologyOrderEntrySection", () => {
  it("stays hidden when no selected test starts a culture workflow", () => {
    renderSection(
      [{ tests: [{ id: "1", name: "Complete blood count" }] }],
      baseForm,
      vi.fn(),
    );

    expect(
      screen.queryByRole("heading", { name: "Microbiology order details" }),
    ).not.toBeInTheDocument();
  });

  it("captures order context when a selected test starts bacteriology", () => {
    const setOrderFormValues = vi.fn();
    renderSection(
      [
        {
          tests: [
            {
              id: "2",
              name: "Blood culture",
              cultureWorkflowType: "BACTERIOLOGY",
            },
          ],
        },
      ],
      baseForm,
      setOrderFormValues,
    );

    expect(
      screen.getByRole("heading", { name: "Microbiology order details" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Bacteriology")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Patient origin"), {
      target: { value: "Emergency department" },
    });

    expect(setOrderFormValues).toHaveBeenCalledWith({
      ...baseForm,
      microbiologyOrderDetail: {
        ...baseForm.microbiologyOrderDetail,
        patientOrigin: "Emergency department",
      },
    });
  });
});
