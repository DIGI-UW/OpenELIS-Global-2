import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import CaseCultureTransitionPanel from "../CaseCultureTransitionPanel";
import MicrobiologyService from "../MicrobiologyService";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

it("retains the selected specimen after rejection and completes only after a successful retry", async () => {
  const user = userEvent.setup();
  const onComplete = vi.fn();
  const savedCase = { id: "case-owned", stage: "POSITIVE_SIGNAL" };
  postToOpenElisServerJsonResponse
    .mockImplementationOnce((url, body, callback) => callback({ status: 403 }))
    .mockImplementationOnce((url, body, callback) => callback(savedCase));
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseCultureTransitionPanel
        action="mark-positive"
        caseId="case-owned"
        specimens={[{ sampleItemId: "sample-owned", label: "OWN-SAMPLE" }]}
        readOnly={false}
        service={MicrobiologyService}
        onComplete={onComplete}
        onCancel={vi.fn()}
      />
    </IntlProvider>,
  );
  await user.selectOptions(screen.getByLabelText("Sample"), "sample-owned");
  await user.click(
    screen.getByRole("button", { name: "Confirm positive signal" }),
  );
  expect(
    await screen.findByText(messages["microbiology.cultureAction.error"]),
  ).toBeInTheDocument();
  expect(onComplete).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Sample")).toHaveValue("sample-owned");
  await user.click(
    screen.getByRole("button", { name: "Confirm positive signal" }),
  );
  await waitFor(() =>
    expect(onComplete).toHaveBeenCalledExactlyOnceWith(savedCase),
  );
  expect(
    JSON.parse(postToOpenElisServerJsonResponse.mock.calls[1][1]),
  ).toMatchObject({
    sourceSampleItemId: "sample-owned",
    nextStage: "POSITIVE_SIGNAL",
  });
});
