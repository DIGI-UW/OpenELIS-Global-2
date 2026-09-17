import React, { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import RequesterSection from "./RequesterSection";
import { ConfigurationContext } from "../../../layout/Layout";

const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
}));

function renderSection({
  workflowType = "clinical",
  orderData = { sampleOrderItems: {} },
  setOrderData = vi.fn(),
  isReadOnly = false,
  configurationProperties = {},
} = {}) {
  return render(
    <ConfigurationContext.Provider value={{ configurationProperties }}>
      <IntlProvider locale="en" messages={messages}>
        <RequesterSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly}
          workflowType={workflowType}
        />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );
}

// Wraps RequesterSection with real React state, the way its actual parent
// (OrderContext) does — needed to reproduce bugs that only appear across a
// setOrderData -> re-render cycle, like the "typing locks the field" bug.
function StatefulSection({ workflowType, initialOrderData }) {
  const [orderData, setOrderData] = useState(initialOrderData);
  return (
    <IntlProvider locale="en" messages={messages}>
      <RequesterSection
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
        workflowType={workflowType}
      />
    </IntlProvider>
  );
}

function renderControlledRequester(initialOrderData) {
  let latestOrderData = initialOrderData;

  function ControlledRequester() {
    const [orderData, setOrderData] = useState(initialOrderData);
    latestOrderData = orderData;
    return (
      <RequesterSection
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
        workflowType="clinical"
      />
    );
  }

  render(
    <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
      <IntlProvider locale="en" messages={messages}>
        <ControlledRequester />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );

  return () => latestOrderData;
}

describe("RequesterSection", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
  });

  // Vector previously had the entire organization/site search block
  // hidden via `workflowType !== "vector"`, forcing it through Provider only.
  it("renders the Requesting Organization search for vector orders (no longer hidden)", () => {
    renderSection({ workflowType: "vector" });

    expect(
      screen.getByText("Requesting Organization Search"),
    ).toBeInTheDocument();
  });

  it("renders the Requesting Organization search for environmental orders", () => {
    renderSection({ workflowType: "environmental" });

    expect(
      screen.getByText("Requesting Organization Search"),
    ).toBeInTheDocument();
  });

  it("labels the section 'Site Search' for clinical orders", () => {
    renderSection({ workflowType: "clinical" });

    expect(screen.getByText("Site Search")).toBeInTheDocument();
    expect(
      screen.queryByText("Requesting Organization Search"),
    ).not.toBeInTheDocument();
  });

  // Provider is Clinical-only; env/vector use Requestor instead.
  it("shows Provider search for clinical orders and hides Requestor search", () => {
    renderSection({ workflowType: "clinical" });

    expect(screen.getByText("Provider Search")).toBeInTheDocument();
    expect(screen.queryByText("Requestor Search")).not.toBeInTheDocument();
  });

  it("shows Requestor search for environmental/vector orders and hides Provider search", () => {
    renderSection({ workflowType: "environmental" });

    expect(screen.getByText("Requestor Search")).toBeInTheDocument();
    expect(screen.queryByText("Provider Search")).not.toBeInTheDocument();
  });

  // Clinical Provider must surface Fax and Email (v1 dropped them).
  it("renders Provider Fax and Email inputs for clinical orders", () => {
    renderSection({ workflowType: "clinical" });

    expect(screen.getByLabelText("Provider Fax")).toBeInTheDocument();
    expect(screen.getByLabelText("Provider Email")).toBeInTheDocument();
  });

  it("calls setOrderData with providerFax/providerEmail on input", () => {
    const setOrderData = vi.fn();
    renderSection({ workflowType: "clinical", setOrderData });

    fireEvent.change(screen.getByLabelText("Provider Fax"), {
      target: { value: "555-1234" },
    });

    expect(setOrderData).toHaveBeenCalled();
    const updater = setOrderData.mock.calls[0][0];
    const result = updater({ sampleOrderItems: {} });
    expect(result.sampleOrderItems.providerFax).toBe("555-1234");
  });

  // Search & store — Requestor uses the same type-ahead mechanism
  // as Organization/Provider, against /rest/requestor/search.
  it("searches /rest/requestor/search and lets the user select a result", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/requestor/search")) {
        callback({
          requestors: [
            {
              id: "77",
              personId: "77",
              firstName: "Jane",
              lastName: "Doe",
              phone: "555-0000",
              email: "jane@example.com",
              department: "Water Quality",
            },
          ],
        });
      }
    });

    const setOrderData = vi.fn();
    renderSection({ workflowType: "environmental", setOrderData });

    fireEvent.change(screen.getByLabelText("Requestor Name"), {
      target: { value: "Jane" },
    });

    await waitFor(() =>
      expect(getFromOpenElisServerMock).toHaveBeenCalledWith(
        expect.stringContaining("/rest/requestor/search?search=Jane"),
        expect.any(Function),
      ),
    );

    await waitFor(() => screen.getByText("Select"));
    fireEvent.click(screen.getByText("Select"));

    const updater =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1][0];
    const result = updater({ sampleOrderItems: {} });
    expect(result.sampleOrderItems.requestorPersonId).toBe("77");
    expect(result.sampleOrderItems.requestorFirstName).toBe("Jane");
    expect(result.sampleOrderItems.requestorDepartment).toBe("Water Quality");
  });

  it("clears Requestor selection back to empty fields", () => {
    const setOrderData = vi.fn();
    renderSection({
      workflowType: "vector",
      setOrderData,
      orderData: {
        sampleOrderItems: {
          requestorPersonId: "77",
          requestorFirstName: "Jane",
          requestorLastName: "Doe",
        },
      },
    });

    // Selected-card "Clear" links render as <Link> text; the Requestor one is
    // the only Clear affordance rendered once a Requestor is pre-selected in
    // this vector-only render (no site/provider selection in this fixture).
    const clearLinks = screen.getAllByText("Clear");
    fireEvent.click(clearLinks[clearLinks.length - 1]);

    const lastCall =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1];
    const result = lastCall[0]({
      sampleOrderItems: {
        requestorPersonId: "77",
        requestorFirstName: "Jane",
        requestorLastName: "Doe",
      },
    });
    expect(result.sampleOrderItems.requestorPersonId).toBe("");
    expect(result.sampleOrderItems.requestorFirstName).toBe("");
  });

  // Requestor's data fields (First/Last/Phone/Fax/Email/Department) used to
  // disable once a Requestor was selected/loaded, matching only the search
  // box — but Organization's equivalent contact fields were always editable.
  // Aligning Requestor with that so phone/email/etc. can be corrected without
  // clearing and re-searching.
  // A Requestor loaded from an existing order (via
  // requestorPersonId) now starts read-only/locked — "Edit details" unlocks
  // it in place. Previously (pre-task-#5) these fields were always editable
  // once loaded; that behavior moved to the "Edit details" flow instead.
  it("starts Requestor fields locked/read-only after a Requestor is selected/loaded, until Edit details is clicked", () => {
    renderSection({
      workflowType: "environmental",
      orderData: {
        sampleOrderItems: {
          requestorPersonId: "77",
          requestorFirstName: "Jane",
          requestorLastName: "Doe",
          requestorPhone: "555-0000",
        },
      },
    });

    expect(screen.getByLabelText("First Name")).toBeDisabled();
    expect(screen.getByLabelText("Last Name")).toBeDisabled();
    expect(screen.getByLabelText("Phone")).toBeDisabled();
    expect(screen.getByLabelText("Fax")).toBeDisabled();
    expect(screen.getByLabelText("Email")).toBeDisabled();
    expect(screen.getByLabelText("Department")).toBeDisabled();

    fireEvent.click(screen.getByText("Edit details"));

    expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    expect(screen.getByLabelText("Last Name")).not.toBeDisabled();
    expect(screen.getByLabelText("Phone")).not.toBeDisabled();
    expect(screen.getByLabelText("Fax")).not.toBeDisabled();
    expect(screen.getByLabelText("Email")).not.toBeDisabled();
    expect(screen.getByLabelText("Department")).not.toBeDisabled();
  });

  // Bug: the requestorPersonId hydration effect used to also watch
  // requestorFirstName/requestorLastName. Typing into the free-text First
  // Name field set requestorFirstName, which the effect mistook for
  // "requestor data arrived from elsewhere" and immediately locked the field
  // via setSelectedRequestor — after exactly one keystroke, since every input
  // in this subsection is disabled once selectedRequestor is truthy.
  it("typing into Requestor First Name does not lock the field after one keystroke", () => {
    render(
      <StatefulSection
        workflowType="environmental"
        initialOrderData={{ sampleOrderItems: {} }}
      />,
    );

    const firstNameInput = screen.getByLabelText("First Name");
    fireEvent.change(firstNameInput, { target: { value: "J" } });

    expect(screen.getByLabelText("First Name")).not.toBeDisabled();

    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Ja" },
    });
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Jan" },
    });
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Jane" },
    });

    expect(screen.getByLabelText("First Name")).toHaveValue("Jane");
    expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    // No "Selected"/"Clear" card should have appeared — the user is still
    // composing a brand-new Requestor, nothing has been "selected".
    expect(screen.queryByText("Selected")).not.toBeInTheDocument();
  });

  // Requesting Organization needs its own phone/fax/email, separate
  // from any Requestor contact person.
  it("renders Organization contact fields for environmental/vector orders", () => {
    renderSection({ workflowType: "environmental" });

    expect(screen.getByLabelText("Organization Phone")).toBeInTheDocument();
    expect(screen.getByLabelText("Organization Fax")).toBeInTheDocument();
    expect(screen.getByLabelText("Organization Email")).toBeInTheDocument();
  });

  // Bug: typing a brand-new org name with no search match previously left
  // referringSiteName/newRequesterName unset, so the org-or-requestor backend
  // validation rejected the order even though the user had filled in an
  // organization — there was no way to actually create one on the fly.
  it("offers '+ Add new organization' when no search results match, and sets newRequesterName on click", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/organization/search")) {
        callback({ organizations: [] });
      }
    });

    const setOrderData = vi.fn();
    renderSection({ workflowType: "clinical", setOrderData });

    fireEvent.change(screen.getByLabelText(/Site Name/), {
      target: { value: "Brand New Clinic" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new organization "Brand New Clinic"'),
      ).toBeInTheDocument(),
    );

    fireEvent.click(
      screen.getByText('+ Add new organization "Brand New Clinic"'),
    );

    const updater =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1][0];
    const result = updater({ sampleOrderItems: {} });
    expect(result.sampleOrderItems.newRequesterName).toBe("Brand New Clinic");
    expect(result.sampleOrderItems.referringSiteId).toBe("");
  });

  // Provider previously had no way to create a brand-new provider on the
  // fly — the search box was pure local state that never reached orderData.
  // Mirrors the Organization "+ Add new" affordance: promote the typed
  // search text into providerFirstName/providerLastName (split on the last
  // space) so the backend's existing new-Provider creation path picks it up.
  it("offers '+ Add new provider' when no search results match, and splits the name on click", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/provider/search")) {
        callback({ providers: [] });
      }
    });

    const setOrderData = vi.fn();
    renderSection({ workflowType: "clinical", setOrderData });

    fireEvent.change(screen.getByLabelText("Provider Name"), {
      target: { value: "Jane Doe" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new provider "Jane Doe"'),
      ).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByText('+ Add new provider "Jane Doe"'));

    const updater =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1][0];
    const result = updater({ sampleOrderItems: {} });
    expect(result.sampleOrderItems.providerFirstName).toBe("Jane");
    expect(result.sampleOrderItems.providerLastName).toBe("Doe");
    expect(result.sampleOrderItems.providerPersonId).toBe("");
  });

  // Requestor previously had no "no match" affordance at all — unlike
  // Organization/Provider, there was no way to tell the user their typed
  // name didn't match anything, or to explicitly promote it into a new
  // Requestor. Mirrors the Provider "+ Add new" flow exactly.
  it("offers '+ Add new requestor' when no search results match, and splits the name on click", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/requestor/search")) {
        callback({ requestors: [] });
      }
    });

    const setOrderData = vi.fn();
    renderSection({ workflowType: "environmental", setOrderData });

    fireEvent.change(screen.getByLabelText("Requestor Name"), {
      target: { value: "Jane Doe" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new requestor "Jane Doe"'),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByText("No matching requestor found."),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText('+ Add new requestor "Jane Doe"'));

    const updater =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1][0];
    const result = updater({ sampleOrderItems: {} });
    expect(result.sampleOrderItems.requestorFirstName).toBe("Jane");
    expect(result.sampleOrderItems.requestorLastName).toBe("Doe");
    expect(result.sampleOrderItems.requestorPersonId).toBe("");
  });

  it("leaves a brand-new requestor's fields editable immediately, tagged 'New', with no Edit details link", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/requestor/search")) {
        callback({ requestors: [] });
      }
    });

    renderSection({ workflowType: "environmental" });

    fireEvent.change(screen.getByLabelText("Requestor Name"), {
      target: { value: "Jane Doe" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new requestor "Jane Doe"'),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText('+ Add new requestor "Jane Doe"'));

    expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    expect(screen.getByText("New")).toBeInTheDocument();
  });

  it("disables '+ Add new requestor' and shows a message when restrictFreeTextRequestorEntry is true", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/requestor/search")) {
        callback({ requestors: [] });
      }
    });

    renderSection({
      workflowType: "environmental",
      configurationProperties: { restrictFreeTextRequestorEntry: "true" },
    });

    fireEvent.change(screen.getByLabelText("Requestor Name"), {
      target: { value: "Jane Doe" },
    });

    await waitFor(() =>
      expect(screen.getByText('+ Add new requestor "Jane Doe"')).toBeDisabled(),
    );
    expect(
      screen.getByText(
        "Adding a new requestor has been disabled by your administrator.",
      ),
    ).toBeInTheDocument();
  });

  it("renders editable Provider First Name/Last Name/Phone fields", () => {
    renderSection({ workflowType: "clinical" });

    expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    expect(screen.getByLabelText("Last Name")).not.toBeDisabled();
    expect(screen.getByLabelText("Phone")).not.toBeDisabled();
  });

  // Same class of bug as the earlier Requestor fix: the hydration effect
  // must not react to providerFirstName/providerLastName alone, or typing
  // into the (now editable) name fields would lock them after one keystroke.
  it("typing into Provider First Name does not lock the field after one keystroke", () => {
    render(
      <StatefulSection
        workflowType="clinical"
        initialOrderData={{ sampleOrderItems: {} }}
      />,
    );

    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "J" },
    });

    expect(screen.getByLabelText("First Name")).not.toBeDisabled();

    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Jane" },
    });

    expect(screen.getByLabelText("First Name")).toHaveValue("Jane");
    expect(screen.getByLabelText("First Name")).not.toBeDisabled();
  });

  // Admin-gated "+ Add new" affordances. All three must
  // stay VISIBLE when restricted (never silently hidden) but disabled, with
  // an explanatory message.
  describe("admin-gated add-new affordances", () => {
    it("disables '+ Add new organization' and shows a message when restrictFreeTextRefSiteEntry is true", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/organization/search")) {
          callback({ organizations: [] });
        }
      });

      renderSection({
        workflowType: "clinical",
        configurationProperties: { restrictFreeTextRefSiteEntry: "true" },
      });

      fireEvent.change(screen.getByLabelText(/Site Name/), {
        target: { value: "Brand New Clinic" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new organization "Brand New Clinic"'),
        ).toBeDisabled(),
      );
      expect(
        screen.getByText(
          "Adding a new organization has been disabled by your administrator.",
        ),
      ).toBeInTheDocument();
    });

    it("keeps '+ Add new organization' enabled when restrictFreeTextRefSiteEntry is false", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/organization/search")) {
          callback({ organizations: [] });
        }
      });

      renderSection({
        workflowType: "clinical",
        configurationProperties: { restrictFreeTextRefSiteEntry: "false" },
      });

      fireEvent.change(screen.getByLabelText(/Site Name/), {
        target: { value: "Brand New Clinic" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new organization "Brand New Clinic"'),
        ).not.toBeDisabled(),
      );
    });

    it("disables '+ Add new provider' and shows a message when restrictFreeTextProviderEntry is true", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/provider/search")) {
          callback({ providers: [] });
        }
      });

      renderSection({
        workflowType: "clinical",
        configurationProperties: { restrictFreeTextProviderEntry: "true" },
      });

      fireEvent.change(screen.getByLabelText("Provider Name"), {
        target: { value: "Jane Doe" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new provider "Jane Doe"'),
        ).toBeDisabled(),
      );
      expect(
        screen.getByText(
          "Adding a new provider has been disabled by your administrator.",
        ),
      ).toBeInTheDocument();
    });

    it("disables new-Requestor fields and shows a message when restrictFreeTextRequestorEntry is true, but does not affect an already-selected Requestor", () => {
      const { rerender } = render(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              restrictFreeTextRequestorEntry: "true",
            },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{ sampleOrderItems: {} }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="environmental"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      expect(screen.getByLabelText("First Name")).toBeDisabled();
      expect(
        screen.getByText(
          "Adding a new requestor has been disabled by your administrator. Search for an existing requestor above.",
        ),
      ).toBeInTheDocument();

      // Now simulate an already-selected Requestor (e.g. loaded from an
      // existing order) — the restriction no longer applies since this isn't
      // "creating new" anymore. It's disabled instead by the
      // edit-lock (a loaded record starts read-only); "Edit details"
      // unlocks it, and unlocking must succeed despite the restriction flag,
      // since that flag only ever governs free-text creation of new records.
      rerender(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              restrictFreeTextRequestorEntry: "true",
            },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{
                sampleOrderItems: {
                  requestorPersonId: "77",
                  requestorFirstName: "Jane",
                  requestorLastName: "Doe",
                },
              }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="environmental"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      expect(screen.getByLabelText("First Name")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    });

    it("keeps new-Requestor fields enabled when restrictFreeTextRequestorEntry is false", () => {
      renderSection({
        workflowType: "environmental",
        configurationProperties: { restrictFreeTextRequestorEntry: "false" },
      });

      expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    });

    it("disables new-Organization contact fields and shows a message when restrictFreeTextRefSiteEntry is true, but does not affect an already-selected organization", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/organization/5")) {
          callback({
            id: "5",
            organizationName: "Existing Clinic",
          });
        }
      });

      const { rerender } = render(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: { restrictFreeTextRefSiteEntry: "true" },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{ sampleOrderItems: {} }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="clinical"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      expect(screen.getByLabelText("Organization Phone")).toBeDisabled();
      expect(
        screen.getByText(
          "Entering contact info for a new organization has been disabled by your administrator. Search for an existing organization above.",
        ),
      ).toBeInTheDocument();

      // Simulate an already-selected organization (e.g. loaded from an
      // existing order) — the restriction no longer applies (not "creating
      // new"), but the edit-lock now disables it instead
      // until "Edit details" is clicked; unlocking must succeed despite the
      // restriction flag, since it only ever governs new-record creation.
      rerender(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: { restrictFreeTextRefSiteEntry: "true" },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{
                sampleOrderItems: {
                  referringSiteId: "5",
                  referringSiteName: "Existing Clinic",
                },
              }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="clinical"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      await waitFor(() =>
        expect(screen.getByLabelText("Organization Phone")).toBeDisabled(),
      );

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("Organization Phone")).not.toBeDisabled();
    });

    it("keeps new-Organization contact fields enabled when restrictFreeTextRefSiteEntry is false", () => {
      renderSection({
        workflowType: "clinical",
        configurationProperties: { restrictFreeTextRefSiteEntry: "false" },
      });

      expect(screen.getByLabelText("Organization Phone")).not.toBeDisabled();
    });

    it("disables new-Provider contact fields and shows a message when restrictFreeTextProviderEntry is true, but does not affect an already-selected provider", () => {
      const { rerender } = render(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              restrictFreeTextProviderEntry: "true",
            },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{ sampleOrderItems: {} }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="clinical"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      expect(screen.getByLabelText("Provider Fax")).toBeDisabled();
      expect(
        screen.getByText(
          "Entering contact info for a new provider has been disabled by your administrator. Search for an existing provider above.",
        ),
      ).toBeInTheDocument();

      // Simulate an already-selected provider (e.g. loaded from an existing
      // order) — the restriction no longer applies (not "creating new"),
      // but the edit-lock now disables it instead until
      // "Edit details" is clicked; unlocking must succeed despite the
      // restriction flag, since it only ever governs new-record creation.
      rerender(
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              restrictFreeTextProviderEntry: "true",
            },
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <RequesterSection
              orderData={{
                sampleOrderItems: {
                  providerPersonId: "12",
                  providerFirstName: "Jane",
                  providerLastName: "Doe",
                },
              }}
              setOrderData={vi.fn()}
              isReadOnly={false}
              workflowType="clinical"
            />
          </IntlProvider>
        </ConfigurationContext.Provider>,
      );

      expect(screen.getByLabelText("Provider Fax")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("Provider Fax")).not.toBeDisabled();
    });

    it("keeps new-Provider contact fields enabled when restrictFreeTextProviderEntry is false", () => {
      renderSection({
        workflowType: "clinical",
        configurationProperties: { restrictFreeTextProviderEntry: "false" },
      });

      expect(screen.getByLabelText("Provider Fax")).not.toBeDisabled();
    });
  });

  // Edit-lock. A record pulled in from search starts
  // read-only; "Edit details" unlocks it in place. A brand-new record
  // (via "+ Add new X") starts editable immediately, since there's no
  // prior saved state to protect.
  describe("edit-lock behavior", () => {
    it("locks Organization contact fields after selecting a search result, and unlocks via Edit details", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/organization/search")) {
          callback({
            organizations: [
              { id: "5", organizationName: "Existing Clinic", city: "Kigali" },
            ],
          });
        }
      });

      renderSection({ workflowType: "clinical" });

      fireEvent.change(screen.getByLabelText(/Site Name/), {
        target: { value: "Existing" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("Organization Phone")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("Organization Phone")).not.toBeDisabled();
    });

    it("leaves a brand-new organization's contact fields editable immediately, with no Edit details link", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/organization/search")) {
          callback({ organizations: [] });
        }
      });

      renderSection({ workflowType: "clinical" });

      fireEvent.change(screen.getByLabelText(/Site Name/), {
        target: { value: "Brand New Clinic" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new organization "Brand New Clinic"'),
        ).toBeInTheDocument(),
      );
      fireEvent.click(
        screen.getByText('+ Add new organization "Brand New Clinic"'),
      );

      expect(screen.getByLabelText("Organization Phone")).not.toBeDisabled();
      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });

    it("locks Provider contact fields after selecting a search result, and unlocks via Edit details", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/provider/search")) {
          callback({
            providers: [
              {
                id: "9",
                personId: "9",
                firstName: "Jane",
                lastName: "Doe",
                phone: "555-0000",
              },
            ],
          });
        }
      });

      renderSection({ workflowType: "clinical" });

      fireEvent.change(screen.getByLabelText("Provider Name"), {
        target: { value: "Jane" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("Provider Fax")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("Provider Fax")).not.toBeDisabled();
    });

    it("leaves a brand-new provider's contact fields editable immediately, with no Edit details link", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/provider/search")) {
          callback({ providers: [] });
        }
      });

      renderSection({ workflowType: "clinical" });

      fireEvent.change(screen.getByLabelText("Provider Name"), {
        target: { value: "Jane Doe" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new provider "Jane Doe"'),
        ).toBeInTheDocument(),
      );
      fireEvent.click(screen.getByText('+ Add new provider "Jane Doe"'));

      expect(screen.getByLabelText("Provider Fax")).not.toBeDisabled();
      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });

    it("locks Requestor fields after selecting a search result, and unlocks via Edit details", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/requestor/search")) {
          callback({
            requestors: [
              {
                id: "77",
                personId: "77",
                firstName: "Jane",
                lastName: "Doe",
                phone: "555-0000",
              },
            ],
          });
        }
      });

      renderSection({ workflowType: "environmental" });

      fireEvent.change(screen.getByLabelText("Requestor Name"), {
        target: { value: "Jane" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("First Name")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("First Name")).not.toBeDisabled();
    });

    it("leaves the Clear affordance available while locked, resetting the lock on clear", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/requestor/search")) {
          callback({
            requestors: [
              { id: "77", personId: "77", firstName: "Jane", lastName: "Doe" },
            ],
          });
        }
      });

      renderSection({ workflowType: "environmental" });

      fireEvent.change(screen.getByLabelText("Requestor Name"), {
        target: { value: "Jane" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("First Name")).toBeDisabled();

      const clearLinks = screen.getAllByText("Clear");
      fireEvent.click(clearLinks[clearLinks.length - 1]);

      // Back to the pristine "no selection" state — fields are the
      // always-editable "new Requestor" inputs again, not locked.
      expect(screen.getByLabelText("First Name")).not.toBeDisabled();
      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });

    // Bug (2026-07-07): handleClearProvider reset providerSearch to
    // { lastName, firstName, phone } — but providerSearch's actual shape
    // (declared via useState({ name: "", phone: "" })) uses `name`, not
    // firstName/lastName. Clicking "Clear" on a selected provider left
    // providerSearch.name as undefined, and the very next render crashed on
    // `providerSearch.name.trim()` in the "+ Add new provider" gate below —
    // "clearing data points breaks the page".
    it("clears a selected Provider without crashing, and re-shows the search inputs", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/provider/search")) {
          callback({
            providers: [
              {
                id: "9",
                personId: "9",
                firstName: "Jane",
                lastName: "Doe",
                phone: "555-0000",
              },
            ],
          });
        }
      });

      renderSection({ workflowType: "clinical" });

      fireEvent.change(screen.getByLabelText("Provider Name"), {
        target: { value: "Jane" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      const clearLinks = screen.getAllByText("Clear");
      fireEvent.click(clearLinks[clearLinks.length - 1]);

      expect(screen.getByLabelText("Provider Name")).toHaveValue("");
      expect(screen.getByLabelText("Provider Name")).not.toBeDisabled();
      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });

    it("loads the selected facility departments and stores the selected unit", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url === "/rest/organization/10") {
          callback({
            id: "10",
            organizationName: "Central Hospital",
            shortName: "CENTRAL",
          });
        }
        if (url === "/rest/departments-for-site?refferingSiteId=10") {
          callback([
            { id: "27", value: "Intensive Care Unit" },
            { id: "28", value: "Medical Ward" },
          ]);
        }
      });
      const getLatestOrderData = renderControlledRequester({
        sampleOrderItems: {
          referringSiteId: "10",
          referringSiteDepartmentId: "27",
          referringSiteDepartmentName: "Intensive Care Unit",
        },
      });

      expect(await screen.findByText("Central Hospital")).toBeInTheDocument();
      const department = screen.getByLabelText("Department / Ward / Unit");
      await waitFor(() => expect(department).toBeEnabled());
      expect(department).toHaveValue("27");

      const user = userEvent.setup();
      await user.selectOptions(department, "28");

      expect(getLatestOrderData().sampleOrderItems).toEqual(
        expect.objectContaining({
          referringSiteDepartmentId: "28",
          referringSiteDepartmentName: "Medical Ward",
        }),
      );
    });

    it("keeps the department control disabled without a facility or subunit", async () => {
      const emptyFacility = renderSection({ workflowType: "clinical" });

      expect(screen.getByLabelText("Department / Ward / Unit")).toBeDisabled();
      expect(screen.getByText("Select facility first...")).toBeInTheDocument();
      emptyFacility.unmount();

      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url === "/rest/organization/11") {
          callback({ id: "11", organizationName: "Clinic" });
        }
        if (url === "/rest/departments-for-site?refferingSiteId=11") {
          callback([]);
        }
      });
      renderSection({
        workflowType: "clinical",
        orderData: { sampleOrderItems: { referringSiteId: "11" } },
      });

      expect(await screen.findByText("Clinic")).toBeInTheDocument();
      expect(screen.getByText("No subunits available")).toBeInTheDocument();
      expect(screen.getByLabelText("Department / Ward / Unit")).toBeDisabled();
    });
  });
});
