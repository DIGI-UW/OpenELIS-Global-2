import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import OrderDetailPanel from "../OrderDetailPanel";
import messages from "../../../languages/en.json";

const renderPanel = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderDetailPanel caseId="case-1" orderDetail={null} {...props} />
    </IntlProvider>,
  );

describe("OrderDetailPanel", () => {
  it("saves captured order detail fields", async () => {
    const service = {
      saveOrderDetail: vi.fn().mockResolvedValue({
        orderDetail: {
          patientOrigin: "Emergency department",
          numberOfSets: 2,
          clinicalHistory: "Fever, suspected sepsis",
          antibioticExposure: "",
          criticalNotificationPreference: "",
        },
      }),
    };

    renderPanel({ service });

    fireEvent.change(screen.getByLabelText("Patient origin"), {
      target: { value: "Emergency department" },
    });
    fireEvent.change(screen.getByLabelText("Number of sets"), {
      target: { value: "2" },
    });
    fireEvent.change(screen.getByLabelText("Clinical history"), {
      target: { value: "Fever, suspected sepsis" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save order detail" }));

    await waitFor(() =>
      expect(service.saveOrderDetail).toHaveBeenCalledWith("case-1", {
        patientOrigin: "Emergency department",
        numberOfSets: 2,
        clinicalHistory: "Fever, suspected sepsis",
        antibioticExposure: "",
        criticalNotificationPreference: "",
      }),
    );
  });

  it("prefills fields from an existing captured order detail", () => {
    renderPanel({
      orderDetail: {
        patientOrigin: "Inpatient ward 3",
        numberOfSets: 3,
        clinicalHistory: "",
        antibioticExposure: "",
        criticalNotificationPreference: "Call attending immediately",
      },
      service: { saveOrderDetail: vi.fn() },
    });

    expect(screen.getByLabelText("Patient origin")).toHaveValue(
      "Inpatient ward 3",
    );
    expect(screen.getByLabelText("Number of sets")).toHaveValue(3);
    expect(
      screen.getByLabelText("Critical notification preference"),
    ).toHaveValue("Call attending immediately");
  });
});
