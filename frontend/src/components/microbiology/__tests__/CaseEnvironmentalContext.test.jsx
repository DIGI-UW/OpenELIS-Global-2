import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import CaseViewShell from "../CaseViewShell";
import messages from "../../../languages/en.json";
vi.mock("../CaseTestingWorkspace", () => ({ default: () => null }));
const detail = {
  id: "environmental-case",
  domain: "ENVIRONMENTAL",
  accessionNumber: "ENV-001",
  status: "ACTIVE",
  siteName: "Water station",
  siteCode: "WATER-01",
  siteLocation: "North building",
  latitude: "1.2",
  longitude: "3.4",
  patientName: "Must not display",
  gender: "F",
  birthDate: "2000-01-01",
  culturePurpose: "ROUTINE_MONITORING",
  purposeOptions: ["ROUTINE_MONITORING", "OUTBREAK_INVESTIGATION"],
  replicateCount: 2,
  orderCaseCount: 3,
  canWrite: true,
  samples: [{ id: "sample", label: "ENV-001-1", samplingPoint: "Tap 2" }],
  pendingSamples: [],
  relatedCases: [],
};
const mount = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <CaseViewShell caseId={detail.id} service={service} />
      </MemoryRouter>
    </IntlProvider>,
  );
test("environmental shell shows site and sampling point without patient labels", async () => {
  mount({ getCase: vi.fn().mockResolvedValue(detail) });
  await screen.findByText(/Water station/);
  expect(screen.getByText("Tap 2")).toBeTruthy();
  expect(
    screen.getByText("Patient-dependent rules: Skipped (Environmental)"),
  ).toBeTruthy();
  expect(screen.getByText("Applies to all 3 cases on this order")).toBeTruthy();
  expect(screen.queryByText("Must not display")).toBeNull();
  expect(screen.queryByText(/Date of birth/)).toBeNull();
  expect(screen.queryByText(/^Sex/)).toBeNull();
  expect(screen.queryByRole("option", { name: "Diagnostic" })).toBeNull();
});
test("saves case purpose and order replicates and disables writes for read-only access", async () => {
  const service = {
    getCase: vi.fn().mockResolvedValue(detail),
    saveContext: vi.fn().mockResolvedValue({
      ...detail,
      canWrite: false,
      culturePurpose: "OUTBREAK_INVESTIGATION",
    }),
  };
  mount(service);
  fireEvent.change(await screen.findByLabelText("Culture purpose"), {
    target: { value: "OUTBREAK_INVESTIGATION" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(service.saveContext).toHaveBeenCalledWith(detail.id, {
      culturePurpose: "OUTBREAK_INVESTIGATION",
      replicateCount: 2,
      programId: "",
    }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Culture purpose").disabled).toBe(true),
  );
  expect(screen.queryByRole("button", { name: "Save" })).toBeNull();
});
