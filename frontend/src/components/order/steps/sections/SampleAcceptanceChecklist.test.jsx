import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

// Hoisted mutable mocks so each test can program the API responses and assert
// against stable spy instances.
const { apiMock, notificationMock, sessionValue } = vi.hoisted(() => ({
  apiMock: {
    getSampleItemEvaluation: undefined,
    recordAssessment: undefined,
    resampleSample: undefined,
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
  recordAssessment: (...args) => apiMock.recordAssessment(...args),
  resampleSample: (...args) => apiMock.resampleSample(...args),
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

// Stub the heavy NCE form + resample dialog — we only assert on the props the
// checklist hands them.
vi.mock("../../../nonconform/common/InlineNceForm", () => ({
  default: ({ initialDescription }) => (
    <div data-testid="nce-form" data-desc={initialDescription} />
  ),
}));
vi.mock("./ResampleDialog", () => ({
  default: ({ initialReason, onSuccess }) => (
    <div data-testid="resample-dialog" data-reason={initialReason}>
      <button
        type="button"
        onClick={() =>
          onSuccess({
            newAccessionNumber: "ENV-2026-00231-R1",
            newSampleId: 99,
          })
        }
      >
        commit-mock
      </button>
    </div>
  ),
}));

import SampleAcceptanceChecklist from "./SampleAcceptanceChecklist";

const ITEMS = [
  {
    itemKey: "container_intact",
    label: "Container intact",
    localizedName: "Container intact",
    displayOrder: 0,
  },
  {
    itemKey: "label_legible",
    label: "Label legible",
    localizedName: "Label legible",
    displayOrder: 1,
  },
];

const evaluation = (overrides = {}) => ({
  sampleItemId: "42",
  domain: "ENVIRONMENTAL",
  enforcement: "OPTIONAL",
  overallStatus: "PENDING",
  blocked: false,
  items: ITEMS,
  latest: null,
  resample: { resampledToSampleId: null, resampledFromSampleId: null },
  ...overrides,
});

const renderChecklist = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleAcceptanceChecklist
        sampleItemId="42"
        labNumber="ENV-2026-00231"
        {...props}
      />
    </IntlProvider>,
  );

beforeEach(() => {
  apiMock.getSampleItemEvaluation = vi.fn(() => Promise.resolve(evaluation()));
  apiMock.recordAssessment = vi.fn((id, answers) =>
    Promise.resolve(
      evaluation({ overallStatus: "ACCEPTED", latest: { answers } }),
    ),
  );
  apiMock.resampleSample = vi.fn();
  notificationMock.addNotification = vi.fn();
  notificationMock.setNotificationVisible = vi.fn();
  sessionValue.userSessionDetails = { roles: ["Reception"] };
});

describe("SampleAcceptanceChecklist", () => {
  test("renders the resolved checklist items for the sample's domain", async () => {
    renderChecklist();
    expect(await screen.findByText("Container intact")).toBeInTheDocument();
    expect(screen.getByText("Label legible")).toBeInTheDocument();
    expect(apiMock.getSampleItemEvaluation).toHaveBeenCalledWith("42");
  });

  test("Accept is disabled until every item is Pass/NA, and enabled then", async () => {
    renderChecklist();
    await screen.findByText("Container intact");

    const accept = screen.getByRole("button", { name: "Accept sample" });
    expect(accept).toBeDisabled(); // all unanswered → Pending

    screen.getAllByRole("radio", { name: "Pass" }).forEach((r) => {
      fireEvent.click(r);
    });
    await waitFor(() => expect(accept).toBeEnabled());
  });

  test("a Fail disables Accept, reveals a note field, and shows Review status", async () => {
    renderChecklist();
    await screen.findByText("Container intact");

    fireEvent.click(screen.getAllByRole("radio", { name: "Pass" })[1]); // item 2 pass
    fireEvent.click(screen.getAllByRole("radio", { name: "Fail" })[0]); // item 1 fail

    await waitFor(() =>
      expect(
        screen.getByPlaceholderText("Optional — note any observed condition"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByRole("button", { name: "Accept sample" }),
    ).toBeDisabled();
    expect(screen.getByText("Review")).toBeInTheDocument();
  });

  test("Accept POSTs the exact answers snapshot", async () => {
    renderChecklist();
    await screen.findByText("Container intact");

    screen.getAllByRole("radio", { name: "Pass" }).forEach((r) => {
      fireEvent.click(r);
    });
    const accept = screen.getByRole("button", { name: "Accept sample" });
    await waitFor(() => expect(accept).toBeEnabled());
    fireEvent.click(accept);

    await waitFor(() =>
      expect(apiMock.recordAssessment).toHaveBeenCalledWith("42", [
        {
          itemKey: "container_intact",
          label: "Container intact",
          answer: "PASS",
          note: "",
        },
        {
          itemKey: "label_legible",
          label: "Label legible",
          answer: "PASS",
          note: "",
        },
      ]),
    );
    expect(notificationMock.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });

  test("Report NCE pre-fills the description from the failed item + note", async () => {
    renderChecklist();
    await screen.findByText("Container intact");

    fireEvent.click(screen.getAllByRole("radio", { name: "Fail" })[0]);
    const note = await screen.findByPlaceholderText(
      "Optional — note any observed condition",
    );
    fireEvent.change(note, { target: { value: "cooler at 14°C" } });

    fireEvent.click(screen.getByRole("button", { name: /Report NCE/i }));

    const nce = await screen.findByTestId("nce-form");
    expect(nce).toHaveAttribute(
      "data-desc",
      "Container intact: FAIL — cooler at 14°C",
    );
  });

  test("Resample opens the dialog and a successful commit shows the cross-reference banner", async () => {
    renderChecklist();
    await screen.findByText("Container intact");

    // After a successful resample the reloaded evaluation carries the link.
    apiMock.getSampleItemEvaluation = vi.fn(() =>
      Promise.resolve(evaluation({ resample: { resampledToSampleId: 99 } })),
    );

    fireEvent.click(screen.getByRole("button", { name: /Resample/i }));
    fireEvent.click(await screen.findByText("commit-mock"));

    expect(
      await screen.findByText(/Replacement order: ENV-2026-00231-R1/i),
    ).toBeInTheDocument();
  });

  test("hides Resample when the user lacks an authorising role", async () => {
    sessionValue.userSessionDetails = { roles: ["Reports"] };
    renderChecklist();
    await screen.findByText("Container intact");
    expect(
      screen.queryByRole("button", { name: /Resample/i }),
    ).not.toBeInTheDocument();
  });

  test("FR-08: lifts blocked=true under Mandatory until accepted, then false", async () => {
    const onBlockedChange = vi.fn();
    apiMock.getSampleItemEvaluation = vi.fn(() =>
      Promise.resolve(evaluation({ enforcement: "MANDATORY" })),
    );
    renderChecklist({ onBlockedChange });
    await screen.findByText("Container intact");

    await waitFor(() => expect(onBlockedChange).toHaveBeenLastCalledWith(true));

    screen.getAllByRole("radio", { name: "Pass" }).forEach((r) => {
      fireEvent.click(r);
    });
    await waitFor(() =>
      expect(onBlockedChange).toHaveBeenLastCalledWith(false),
    );
  });

  test("renders the empty state when the domain has no checklist", async () => {
    apiMock.getSampleItemEvaluation = vi.fn(() =>
      Promise.resolve(evaluation({ items: [] })),
    );
    renderChecklist();
    expect(
      await screen.findByText(
        "No acceptance checklist is configured for this domain.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Accept sample" }),
    ).not.toBeInTheDocument();
  });

  test("a rejected specimen is read-only: shows Rejected + the resample banner and hides actions", async () => {
    apiMock.getSampleItemEvaluation = vi.fn(() =>
      Promise.resolve(evaluation({ resample: { resampledToSampleId: 99 } })),
    );
    renderChecklist({ rejected: true });
    await screen.findByText("Container intact");

    // Rejected badge + the resample cross-reference (#99), without an in-place resample.
    expect(screen.getByText("Rejected")).toBeInTheDocument();
    expect(screen.getByText(/Replacement order: #99/i)).toBeInTheDocument();

    // Read-only: inputs disabled, no Accept / Resample actions.
    expect(screen.getAllByRole("radio", { name: "Pass" })[0]).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Accept sample" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Resample/i }),
    ).not.toBeInTheDocument();
  });
});
