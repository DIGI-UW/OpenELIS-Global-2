import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
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

  it("has a message for every bench context and lot status", () => {
    const onError = vi.fn();
    const usage = {
      reagentName: "Blood agar",
      lotNumber: "MEDIA-1",
      quantityUsed: 1,
      usageContext: "CULTURE_SETUP",
      currentLotStatus: "ACTIVE",
    };
    // react-intl reports a fallback message only when the locale is not the default, as for an en-US browser
    render(
      <IntlProvider
        locale="en-US"
        defaultLocale="en"
        messages={messages}
        timeZone="UTC"
        onError={onError}
      >
        <ReagentUsageHistory
          usages={[
            ...["CULTURE_SETUP", "AST_SETUP"].map((usageContext) => ({
              ...usage,
              id: usageContext,
              usageContext,
            })),
            ...[
              "ACTIVE",
              "IN_USE",
              "EXPIRED",
              "CONSUMED",
              "DISPOSED",
              "QUARANTINED",
            ].map((currentLotStatus) => ({
              ...usage,
              id: currentLotStatus,
              currentLotStatus,
            })),
          ]}
        />
      </IntlProvider>,
    );

    expect(onError.mock.calls.map(([error]) => error.message)).toEqual([]);
  });
});
