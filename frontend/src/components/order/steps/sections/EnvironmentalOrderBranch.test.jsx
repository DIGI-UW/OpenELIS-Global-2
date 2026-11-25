import React, { useState } from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import EnvironmentalOrderBranch from "./EnvironmentalOrderBranch";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

let latest;
const Harness = ({ fields }) => {
  const [orderData, setOrderData] = useState({
    sampleOrderItems: { environmentalFields: fields },
  });
  latest = orderData.sampleOrderItems.environmentalFields;
  return (
    <IntlProvider locale="en" messages={messages}>
      <EnvironmentalOrderBranch
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
      />
    </IntlProvider>
  );
};

const tile = (name) => screen.getByRole("radio", { name: new RegExp(name) });

describe("Environmental order type (OGC-768)", () => {
  it("starts a new order ad-hoc and keeps the regulatory reference with it", () => {
    render(<Harness fields={{}} />);

    expect(tile("Ad-hoc")).toBeChecked();
    fireEvent.change(screen.getByLabelText("Regulatory Reference"), {
      target: { value: "Permit 12/2026" },
    });

    expect(latest.regulatoryReference).toBe("Permit 12/2026");
    expect(latest.orderBranch).toBe("AD_HOC");
  });

  it("opens an order that names a standard as regulation-driven, and asks before switching it to ad-hoc", () => {
    render(<Harness fields={{ complianceStandards: '["9761"]' }} />);
    expect(tile("Regulation-driven")).toBeChecked();
    expect(screen.queryByLabelText("Regulatory Reference")).toBeNull();

    fireEvent.click(tile("Ad-hoc"));
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveTextContent(
      "Switching to Ad-hoc clears the selected compliance standards",
    );
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(latest.complianceStandards).toBe('["9761"]');
    expect(tile("Regulation-driven")).toBeChecked();

    fireEvent.click(tile("Ad-hoc"));
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "Switch",
      }),
    );

    expect(latest.complianceStandards).toBe("[]");
    expect(latest.orderBranch).toBe("AD_HOC");
    expect(tile("Ad-hoc")).toBeChecked();
  });

  it("switches an ad-hoc order with nothing to lose without asking, and clears the reference when it has one", () => {
    const { unmount } = render(<Harness fields={{ orderBranch: "AD_HOC" }} />);
    fireEvent.click(tile("Regulation-driven"));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(latest.orderBranch).toBe("REGULATION_DRIVEN");
    unmount();

    render(
      <Harness
        fields={{
          orderBranch: "AD_HOC",
          regulatoryReference: "Permit 12/2026",
        }}
      />,
    );
    fireEvent.click(tile("Regulation-driven"));
    expect(screen.getByRole("dialog")).toHaveTextContent(
      "Switching to Regulation-driven clears the regulatory reference.",
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "Switch",
      }),
    );
    expect(latest.regulatoryReference).toBe("");
    expect(latest.orderBranch).toBe("REGULATION_DRIVEN");
  });
});
