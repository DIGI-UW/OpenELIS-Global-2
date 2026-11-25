import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * OGC-1488: an EQA sample that fails intake is rejected, as a vector catch is,
 * because PT material cannot be recollected. The rejection needs one of the
 * lab's rejection reasons; the comment is optional.
 */

const { apiMock, notificationMock, sessionValue } = vi.hoisted(() => ({
  apiMock: {
    getSampleItemEvaluation: undefined,
    rejectSampleItem: undefined,
  },
  notificationMock: {
    addNotification: undefined,
    setNotificationVisible: undefined,
  },
  sessionValue: { userSessionDetails: { roles: ["Reception"] } },
}));

vi.mock("../../api/sampleAcceptanceApi", () => ({
  ANSWER: { PASS: "PASS", FAIL: "FAIL", NA: "NA" },
  getSampleItemEvaluation: (...args) =>
    apiMock.getSampleItemEvaluation(...args),
  recordAssessment: vi.fn(),
  resampleSample: vi.fn(),
  rejectPool: vi.fn(),
  rejectSampleItem: (...args) => apiMock.rejectSampleItem(...args),
}));

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) => {
    callback(
      url === "/rest/displayList/REJECTION_REASONS"
        ? [
            { id: "7", value: "Hemolyzed" },
            { id: "8", value: "Leaking container" },
          ]
        : [],
    );
  }),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext(notificationMock),
}));

vi.mock("../../../common/CustomNotification", () => ({
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../../../../UserSessionDetailsContext", () => ({
  default: React.createContext(sessionValue),
}));

vi.mock("../../../nonconform/common/InlineNceForm", () => ({
  default: () => <div data-testid="nce-form" />,
}));

import SampleAcceptanceChecklist from "./SampleAcceptanceChecklist";

const evaluation = (overrides = {}) => ({
  sampleItemId: "42",
  domain: "CLINICAL",
  enforcement: "OPTIONAL",
  overallStatus: "REVIEW",
  blocked: false,
  items: [
    {
      itemKey: "container_intact",
      label: "Container intact",
      localizedName: "Container intact",
      displayOrder: 0,
    },
  ],
  latest: {
    answers: [{ itemKey: "container_intact", answer: "FAIL", note: "" }],
  },
  resample: { resampledToSampleId: null, resampledFromSampleId: null },
  ...overrides,
});

const renderChecklist = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleAcceptanceChecklist sampleItemId="42" labNumber="EQA-26-0001" />
    </IntlProvider>,
  );

beforeEach(() => {
  apiMock.getSampleItemEvaluation = vi.fn(() =>
    Promise.resolve(evaluation({ eqa: true })),
  );
  apiMock.rejectSampleItem = vi.fn(() => Promise.resolve({ rejected: true }));
  notificationMock.addNotification = vi.fn();
  notificationMock.setNotificationVisible = vi.fn();
});

describe("SampleAcceptanceChecklist, EQA sample (OGC-1488)", () => {
  test("a failed EQA sample offers Reject, not Resample", async () => {
    renderChecklist();

    expect(
      await screen.findByRole("button", { name: /Reject/ }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Resample/ }),
    ).not.toBeInTheDocument();
  });

  test("rejecting without a reason names the field and sends nothing", async () => {
    renderChecklist();
    fireEvent.click(await screen.findByRole("button", { name: /Reject/ }));

    fireEvent.click(
      await screen.findByRole("button", {
        name: /Commit reject$/,
        hidden: true,
      }),
    );

    expect(
      await screen.findByText(messages["sample.reject.reasonRequired"]),
    ).toBeInTheDocument();
    expect(apiMock.rejectSampleItem).not.toHaveBeenCalled();
  });

  test("a chosen reason rejects with an optional comment", async () => {
    renderChecklist();
    fireEvent.click(await screen.findByRole("button", { name: /Reject/ }));

    fireEvent.change(
      await screen.findByLabelText(messages["sample.reject.reason.label"]),
      { target: { value: "7" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["sampleAcceptance.reject.comment.label"]),
      { target: { value: "" } },
    );
    fireEvent.click(
      await screen.findByRole("button", {
        name: /Commit reject$/,
        hidden: true,
      }),
    );

    await waitFor(() =>
      expect(apiMock.rejectSampleItem).toHaveBeenCalledWith("42", "", "7"),
    );
  });

  test("a routine clinical sample still offers Resample", async () => {
    apiMock.getSampleItemEvaluation = vi.fn(() =>
      Promise.resolve(evaluation()),
    );
    renderChecklist();

    expect(
      await screen.findByRole("button", { name: /Resample/ }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Reject/ }),
    ).not.toBeInTheDocument();
  });
});
