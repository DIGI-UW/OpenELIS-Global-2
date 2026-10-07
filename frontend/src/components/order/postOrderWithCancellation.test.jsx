import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import { postOrderWithCancellation } from "./postOrderWithCancellation";
import { postToOpenElisServerFullResponse } from "../utils/Utils";
import CaseCancellationPrompt from "./CaseCancellationPrompt";
import messages from "../../languages/en.json";
vi.mock("../utils/Utils", () => ({
  postToOpenElisServerFullResponse: vi.fn(),
}));

const conflict = () => ({
  status: 409,
  clone: () => ({
    json: async () => ({
      code: "MICRO_CASE_CANCELLATION_REQUIRED",
      cases: [{ caseId: "c1", labUnit: "Microbiology", hasResults: true }],
    }),
  }),
});

it("resubmits the exact order with case-specific consent after server rollback", async () => {
  const saved = { ok: true };
  const callback = vi.fn();
  const ask = vi
    .fn()
    .mockResolvedValue({ caseIds: ["c1"], reason: "Duplicate" });
  postToOpenElisServerFullResponse
    .mockImplementationOnce((url, body, cb) => cb(conflict()))
    .mockImplementationOnce((url, body, cb) => cb(saved));
  postOrderWithCancellation(
    "/save",
    JSON.stringify({ requestedSampleTypes: [], other: "unchanged" }),
    callback,
    ask,
  );
  await vi.waitFor(() => expect(callback).toHaveBeenCalledWith(saved));
  expect(
    JSON.parse(postToOpenElisServerFullResponse.mock.calls.at(-1)[1]),
  ).toEqual({
    requestedSampleTypes: [],
    other: "unchanged",
    microCaseCancellationIds: ["c1"],
    microCaseCancellationReason: "Duplicate",
  });
});

it("does not resubmit when the user keeps editing", async () => {
  postToOpenElisServerFullResponse.mockClear();
  postToOpenElisServerFullResponse.mockImplementationOnce((url, body, cb) =>
    cb(conflict()),
  );
  const callback = vi.fn();
  postOrderWithCancellation("/save", "{}", callback, async () => null);
  await vi.waitFor(() => expect(callback).toHaveBeenCalled());
  expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
});

it("requires a reason for recorded results and returns the explicitly confirmed cases", async () => {
  const user = userEvent.setup();
  const decision = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseCancellationPrompt
        cases={[{ caseId: "c1", labUnit: "Microbiology", hasResults: true }]}
        onDecision={decision}
      />
    </IntlProvider>,
  );
  const confirm = await screen.findByRole("button", {
    name: /Cancel cases and save$/,
  });
  expect(confirm).toBeDisabled();
  await user.type(screen.getByRole("textbox"), "Duplicate order");
  await user.click(confirm);
  expect(decision).toHaveBeenCalledWith({
    caseIds: ["c1"],
    reason: "Duplicate order",
  });
});
