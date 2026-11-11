import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import ReagentUsageHistory from "../ReagentUsageHistory";

describe("ReagentUsageHistory", () => {
  it("keeps a consumed historical lot visible with its bench context", () => {
    render(
      <IntlProvider locale="en" messages={messages} timeZone="UTC">
        <ReagentUsageHistory
          usages={[
            {
              id: "usage-1",
              usageContext: "CULTURE_SETUP",
              actionId: "activity-1",
              reagentName: "Blood agar",
              lotNumber: "MEDIA-CONSUMED",
              quantityUsed: 1,
              quantityUnit: "plate",
              usageDate: "2026-08-04T10:00:00Z",
              currentLotStatus: "CONSUMED",
              currentQcStatus: "PASSED",
            },
          ]}
        />
      </IntlProvider>,
    );

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByText("MEDIA-CONSUMED")).toBeInTheDocument();
    expect(screen.getByText("Culture setup")).toBeInTheDocument();
    expect(screen.getByText("Consumed")).toBeInTheDocument();
  });
});
