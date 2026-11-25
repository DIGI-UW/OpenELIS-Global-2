import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import { ConfigurationContext } from "../../layout/Layout";
import ReferralAction, { emptyReferralDraft } from "./ReferralAction";
import { todayForReferral } from "./ExpandedPanel";

/**
 * The referral date is typed text that the server reads in the lab's date
 * format. It defaulted to dd/MM/yyyy everywhere, so on a month-first site the
 * 15th of October was saved as 10 March of the next year.
 */
const renderAction = (dateLocale: string) =>
  render(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: dateLocale } }}
    >
      <IntlProvider locale="en" messages={messages}>
        <ReferralAction
          rowKey="row-1"
          organizations={[]}
          reasons={[]}
          draft={emptyReferralDraft(todayForReferral(dateLocale))}
          onDraftChange={() => undefined}
          onCancel={() => undefined}
        />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );

describe("referral date", () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date(2026, 9, 15, 10, 0));
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it("defaults to today month first on a month-first site and says so", () => {
    renderAction("en-US");

    const date = screen.getByLabelText("Referral date");
    expect(date).toHaveValue("10/15/2026");
    expect(date).toHaveAttribute("placeholder", "mm/dd/yyyy");
  });

  it("defaults to today day first on a day-first site and says so", () => {
    renderAction("fr-FR");

    const date = screen.getByLabelText("Referral date");
    expect(date).toHaveValue("15/10/2026");
    expect(date).toHaveAttribute("placeholder", "dd/mm/yyyy");
  });
});
