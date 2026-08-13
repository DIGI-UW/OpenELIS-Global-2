import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

import RequesterSection from "./RequesterSection";

const renderRequester = (initialOrderData) => {
  let latestOrderData = initialOrderData;

  const ControlledRequester = () => {
    const [orderData, setOrderData] = React.useState(initialOrderData);
    latestOrderData = orderData;
    return (
      <RequesterSection
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
      />
    );
  };

  render(
    <IntlProvider locale="en" messages={messages}>
      <ControlledRequester />
    </IntlProvider>,
  );

  return () => latestOrderData;
};

describe("RequesterSection department selection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  it("loads the selected facility departments and stores the selected unit", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/organization/10") {
        callback({
          id: "10",
          organizationName: "Central Hospital",
          shortName: "CENTRAL",
        });
      }
      if (url === "/rest/departments-for-site?refferingSiteId=10") {
        callback([
          { id: "27", value: "Intensive Care Unit" },
          { id: "28", value: "Medical Ward" },
        ]);
      }
    });
    const getLatestOrderData = renderRequester({
      sampleOrderItems: {
        referringSiteId: "10",
        referringSiteDepartmentId: "27",
        referringSiteDepartmentName: "Intensive Care Unit",
      },
    });

    expect(await screen.findByText("Central Hospital")).toBeInTheDocument();
    const department = screen.getByLabelText("Department / Ward / Unit");
    await waitFor(() => expect(department).toBeEnabled());
    expect(department).toHaveValue("27");

    await userEvent.setup().selectOptions(department, "28");

    expect(getLatestOrderData().sampleOrderItems).toEqual(
      expect.objectContaining({
        referringSiteDepartmentId: "28",
        referringSiteDepartmentName: "Medical Ward",
      }),
    );
  });

  it("keeps the control disabled when no facility or subunit is available", async () => {
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <RequesterSection
          orderData={{ sampleOrderItems: {} }}
          setOrderData={vi.fn()}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    expect(screen.getByLabelText("Department / Ward / Unit")).toBeDisabled();
    expect(screen.getByText("Select facility first...")).toBeInTheDocument();

    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/organization/11") {
        callback({ id: "11", organizationName: "Clinic" });
      }
      if (url === "/rest/departments-for-site?refferingSiteId=11") {
        callback([]);
      }
    });
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <RequesterSection
          orderData={{ sampleOrderItems: { referringSiteId: "11" } }}
          setOrderData={vi.fn()}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    expect(await screen.findByText("Clinic")).toBeInTheDocument();
    expect(screen.getByText("No subunits available")).toBeInTheDocument();
    expect(screen.getByLabelText("Department / Ward / Unit")).toBeDisabled();
  });
});
