import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";
import CycleWizard from "../CycleWizard";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  resolveApiErrorMessage: (_intl, payload, fallbackId) =>
    payload?.error || fallbackId,
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const ENROLLMENTS = [
  { organizationId: 100, organizationName: "District Lab A", status: "Active" },
  { organizationId: 101, organizationName: "District Lab B", status: "Active" },
  { organizationId: 102, organizationName: "District Lab C", status: "Active" },
  { organizationId: 103, organizationName: "Lapsed Lab", status: "Withdrawn" },
];

const renderWizard = () => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/eqa/programs/3")
      cb({ id: 3, name: "National HIV VL PT" });
    else if (url === "/rest/eqa/programs/3/enrollments") cb(ENROLLMENTS);
    // fetchTests: testable-tests narrows the whole catalog to the tests a
    // participant could order.
    else if (url === "/rest/eqa/testable-tests") cb(["55", "56"]);
    else if (url === "/rest/displayList/ALL_TESTS")
      cb([
        { id: "55", name: "HIV Viral Load" },
        { id: "56", name: "HIV EID" },
        { id: "99", name: "Test with no analyte" },
      ]);
    else cb([]);
  });
  const landedOn = [];
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/schemes/3/cycles/new"]}>
        <Route path="/qa/eqa/provider/schemes/:schemeId/cycles/new" exact>
          <CycleWizard />
        </Route>
        <Route
          path="/qa/eqa/provider/cycles/:cycleId/workbench"
          render={({ match }) => {
            landedOn.push(match.url);
            return <div>workbench</div>;
          }}
        />
      </MemoryRouter>
    </IntlProvider>,
  );
  return { ...view, landedOn };
};

const next = () =>
  fireEvent.click(screen.getByRole("button", { name: "Next" }));

/** Steps 0 → 2, with the panel step completed on the way through. */
const throughPanelStep = () => {
  next();
  fireEvent.change(screen.getByLabelText("Panel name"), {
    target: { value: "HIV VL panel" },
  });
  fireEvent.change(screen.getByLabelText("Sample code"), {
    target: { value: "PS-1" },
  });
  fireEvent.change(screen.getByLabelText("Test"), {
    target: { value: "55" },
  });
  fireEvent.change(screen.getByLabelText("Storage temperature"), {
    target: { value: "DRY_ICE" },
  });
  fireEvent.change(screen.getByLabelText("Material expiry"), {
    target: { value: "2027-01-31" },
  });
};

describe("CycleWizard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("only tests that carry an analyte are offered", () => {
    renderWizard();
    next();

    // A panel target is stored against an analyte, so a test without one is a
    // dead end the wizard must not offer (T-21's rule, same seam).
    expect(screen.getByText("HIV Viral Load")).toBeInTheDocument();
    expect(screen.queryByText("Test with no analyte")).not.toBeInTheDocument();
  });

  test("the panel step cannot be left until a panel name and a full sample exist", () => {
    renderWizard();
    next();

    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Panel name"), {
      target: { value: "HIV VL panel" },
    });
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Sample code"), {
      target: { value: "PS-1" },
    });
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Test"), {
      target: { value: "55" },
    });
    expect(screen.getByRole("button", { name: "Next" })).toBeEnabled();
  });

  test("vendor-sourced material demands the vendor before the step will advance", () => {
    renderWizard();
    throughPanelStep();

    fireEvent.change(screen.getByLabelText("Material source"), {
      target: { value: "VENDOR_SOURCED" },
    });
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Vendor name"), {
      target: { value: "NHLS" },
    });
    expect(screen.getByRole("button", { name: "Next" })).toBeEnabled();
  });

  test("only actively enrolled labs are offered as participants", async () => {
    renderWizard();
    throughPanelStep();
    next();

    await userEvent.click(screen.getByRole("combobox"));

    expect(screen.getByText("District Lab A")).toBeInTheDocument();
    expect(screen.getByText("District Lab B")).toBeInTheDocument();
    expect(screen.queryByText("Lapsed Lab")).not.toBeInTheDocument();
  });

  test("step 3 preselects exactly the active enrollments", () => {
    // FR-V2.5-02 step 3: default = all active, still editable — a cycle must
    // not be one forgotten click from shipping to nobody.
    renderWizard();
    throughPanelStep();
    next();

    expect(screen.getByRole("button", { name: "Next" })).toBeEnabled();

    // The confirm step names each preselected lab — the withdrawn one is not
    // among them.
    next();
    next();
    expect(
      screen.getByText("District Lab A, District Lab B, District Lab C"),
    ).toBeInTheDocument();
  });

  test("deselecting every lab disables Next", async () => {
    renderWizard();
    throughPanelStep();
    next();

    await userEvent.click(screen.getByRole("combobox"));
    await userEvent.click(screen.getByText("District Lab A"));
    await userEvent.click(screen.getByText("District Lab B"));
    await userEvent.click(screen.getByText("District Lab C"));

    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  test("the whole cycle is one POST, and it lands on the new cycle's workbench", async () => {
    const { landedOn } = renderWizard();
    postToOpenElisServerFullResponse.mockImplementation((_url, _payload, cb) =>
      cb({ ok: true, json: () => Promise.resolve({ id: 42 }) }),
    );

    fireEvent.change(screen.getByLabelText("Cycle name"), {
      target: { value: "2026 Round 2" },
    });
    throughPanelStep();
    next();
    // Step 3 preselects all active labs; the POST carries that default.
    next();
    fireEvent.click(screen.getByLabelText("CSV export"));
    next();

    fireEvent.click(
      screen.getByRole("button", { name: "Create cycle and begin prep" }),
    );

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
    const [url, payload] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(url).toBe("/rest/eqa/provider/cycles");
    const body = JSON.parse(payload);
    expect(body.schemeId).toBe(3);
    expect(body.cycleName).toBe("2026 Round 2");
    expect(body.panelName).toBe("HIV VL panel");
    // Collected on the panel step now, and still reaching the same payload field.
    expect(body.storageTemp).toBe("DRY_ICE");
    expect(body.distributionMethod).toBe("CSV");
    // testId, not analyteId: the server resolves the analyte behind the test.
    expect(body.samples).toEqual([
      expect.objectContaining({ sampleCode: "PS-1", testId: "55" }),
    ]);
    expect(body.participantOrganizationIds).toEqual([100, 101, 102]);
    // Nothing may be written before the last step.
    expect(body.aliquotsReserved).toBeUndefined();

    expect(await screen.findByText("workbench")).toBeInTheDocument();
    expect(landedOn).toContain("/qa/eqa/provider/cycles/42/workbench");
  });

  test("a refusal shows the server's own reason and stays on the wizard", async () => {
    renderWizard();
    postToOpenElisServerFullResponse.mockImplementation((_url, _payload, cb) =>
      cb({
        ok: false,
        json: () =>
          Promise.resolve({ error: "Sample code PS-1 is used twice" }),
      }),
    );

    throughPanelStep();
    next();
    next();
    next();
    fireEvent.click(
      screen.getByRole("button", { name: "Create cycle and begin prep" }),
    );

    expect(
      await screen.findByText("Sample code PS-1 is used twice"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Create cycle and begin prep" }),
    ).toBeInTheDocument();
  });

  test("the distribution method defaults to FHIR and is sent with the cycle", async () => {
    renderWizard();
    postToOpenElisServerFullResponse.mockImplementation((_url, _payload, cb) =>
      cb({ ok: true, json: () => Promise.resolve({ id: 43 }) }),
    );

    throughPanelStep();
    next();
    next();

    // Step 4 is the method, not the cold chain: that moved to the panel step.
    expect(screen.getByLabelText("FHIR")).toBeChecked();
    expect(
      screen.queryByLabelText("Storage temperature"),
    ).not.toBeInTheDocument();

    next();
    fireEvent.click(
      screen.getByRole("button", { name: "Create cycle and begin prep" }),
    );

    const [, payload] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(JSON.parse(payload).distributionMethod).toBe("FHIR");
  });
});
