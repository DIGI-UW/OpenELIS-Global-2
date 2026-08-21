import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import OrderReferralRequest from "./OrderReferralRequest";

// ---------------------------------------------------------------------------
// The referral rows must stay one-per-selected-test and must not accumulate
// across renders. They previously appended a single trailing entry per render,
// so a two-test sample ended up with duplicate rows carrying the same
// organization. Index.jsx then collapsed those into comma-joined ids
// ("referredInstituteId": "4,4"), and the server rejected the order with
// `For input string: "4,4"` while resolving the organization id.
// ---------------------------------------------------------------------------

vi.mock("../common/CustomTextInput", () => ({
  default: () => <div />,
}));
vi.mock("../common/CustomSelect", () => ({
  default: () => <div />,
}));
vi.mock("../common/CustomDatePicker", () => ({
  default: () => <div />,
}));

const REASONS = [{ id: "1", value: "Test not performed" }];
const ORGS = [{ id: "4", value: "Kiruddu" }];
const TESTS = [
  { id: "6", name: "Albumin" },
  { id: "10", name: "Beta HCG" },
];

function renderWith(referralRequests, setReferralRequests, selectedTests) {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { firstName: "Test", lastName: "User" } }}
      >
        <OrderReferralRequest
          index={0}
          selectedTests={selectedTests}
          referralReasons={REASONS}
          referralOrganizations={ORGS}
          referralRequests={referralRequests}
          setReferralRequests={setReferralRequests}
        />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
}

describe("OrderReferralRequest referral rows", () => {
  it("emits exactly one row per selected test", () => {
    const setReferralRequests = vi.fn();
    renderWith([], setReferralRequests, TESTS);

    expect(setReferralRequests).toHaveBeenCalled();
    const rows = setReferralRequests.mock.calls.at(-1)[0];
    expect(rows).toHaveLength(2);
    expect(rows.map((r) => r.testId)).toEqual(["6", "10"]);
  });

  it("does not accumulate duplicate rows when re-rendered", () => {
    const setReferralRequests = vi.fn();
    const existing = [
      { testId: "6", institute: "4", referrer: "", sentDate: "" },
      { testId: "10", institute: "4", referrer: "", sentDate: "" },
    ];
    renderWith(existing, setReferralRequests, TESTS);

    const rows = setReferralRequests.mock.calls.at(-1)[0];
    expect(rows).toHaveLength(2);
    expect(rows.map((r) => r.testId)).toEqual(["6", "10"]);
  });

  it("preserves values already captured for a test", () => {
    const setReferralRequests = vi.fn();
    const existing = [
      {
        testId: "10",
        institute: "4",
        referrer: "Dr Who",
        sentDate: "01/01/2026",
      },
    ];
    renderWith(existing, setReferralRequests, TESTS);

    const rows = setReferralRequests.mock.calls.at(-1)[0];
    expect(rows).toHaveLength(2);
    const kept = rows.find((r) => r.testId === "10");
    expect(kept.institute).toBe("4");
    expect(kept.referrer).toBe("Dr Who");
    expect(kept.sentDate).toBe("01/01/2026");
  });
});
