import React, { useState } from "react";
import { fireEvent, render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import OrderReferralRequest from "./OrderReferralRequest";

// ---------------------------------------------------------------------------
// A referral's reason and institute selects show what the user picked. The
// rows used to be built once per test selection and kept in state, so each
// select kept the value it was created with and snapped back after a change,
// while the order still saved the user's choice.
// ---------------------------------------------------------------------------

vi.mock("../common/CustomTextInput", () => ({
  default: () => <div />,
}));
vi.mock("../common/CustomDatePicker", () => ({
  default: () => <div />,
}));

const REASONS = [
  { id: "1", value: "Test not performed" },
  { id: "2", value: "Confirmation requested" },
];
const ORGS = [
  { id: "4", value: "Kiruddu" },
  { id: "36", value: "Central Reference Lab" },
];
const TESTS = [{ id: "6", name: "Hemoglobin" }];

let latestRequests;

const Harness = () => {
  const [referralRequests, setReferralRequests] = useState([
    { testId: "6", reasonForReferral: "1", institute: null, referrer: "" },
  ]);
  latestRequests = referralRequests;
  return (
    <OrderReferralRequest
      index={0}
      selectedTests={TESTS}
      referralReasons={REASONS}
      referralOrganizations={ORGS}
      referralRequests={referralRequests}
      setReferralRequests={setReferralRequests}
    />
  );
};

describe("OrderReferralRequest selects", () => {
  it("show the reason and institute the user picked", () => {
    const { container } = render(
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider
          value={{
            userSessionDetails: { firstName: "Test", lastName: "User" },
          }}
        >
          <Harness />
        </UserSessionDetailsContext.Provider>
      </IntlProvider>,
    );

    const reason = container.querySelector("#referralReasonId_0_6");
    const institute = container.querySelector("#referredInstituteId_0_6");
    expect(reason.value).toBe("1");

    fireEvent.change(reason, { target: { value: "2" } });
    fireEvent.change(institute, { target: { value: "36" } });

    expect(container.querySelector("#referralReasonId_0_6").value).toBe("2");
    expect(container.querySelector("#referredInstituteId_0_6").value).toBe(
      "36",
    );
    expect(latestRequests[0].reasonForReferral).toBe("2");
    expect(latestRequests[0].institute).toBe("36");
  });
});
