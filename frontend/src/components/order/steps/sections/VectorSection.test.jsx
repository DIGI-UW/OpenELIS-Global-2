import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import VectorSection from "./VectorSection";
import { ConfigurationContext } from "../../../layout/Layout";

const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
}));

vi.mock("../../OrderContext", () => ({
  useOrderContext: () => ({ samples: [{}], setSamples: vi.fn() }),
}));

function renderSection({
  workflowType = "environmental",
  orderData = { sampleOrderItems: { environmentalFields: {} } },
  setOrderData = vi.fn(),
  isReadOnly = false,
  configurationProperties = {},
} = {}) {
  return render(
    <ConfigurationContext.Provider value={{ configurationProperties }}>
      <IntlProvider locale="en" messages={messages}>
        <VectorSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly}
          workflowType={workflowType}
        />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );
}

describe("VectorSection — Sampling Site", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([{ id: "1", dictEntry: "Water Source" }]);
      }
    });
  });

  it("does not fetch the full active-sites list on mount (live search only)", () => {
    renderSection();

    expect(
      getFromOpenElisServerMock.mock.calls.some(([url]) =>
        url.startsWith("/rest/admin/vector/sampling-sites/active"),
      ),
    ).toBe(false);
  });

  it("debounces and calls the live search endpoint once ≥2 characters are typed", async () => {
    vi.useFakeTimers();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
      if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
        callback([{ id: "9", code: "WS-1", name: "Well One" }]);
      }
    });

    renderSection();

    fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
      target: { value: "We" },
    });

    vi.advanceTimersByTime(300);
    vi.useRealTimers();

    await waitFor(() =>
      expect(
        getFromOpenElisServerMock.mock.calls.some(([url]) =>
          url.startsWith("/rest/admin/vector/sampling-sites/search?search=We"),
        ),
      ).toBe(true),
    );
  });

  it("offers '+ Add new site' when no search results match, and defers creation (no id set)", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
      if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
        callback([]);
      }
    });

    const setOrderData = vi.fn();
    renderSection({ setOrderData });

    fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
      target: { value: "Brand New Site" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new site "Brand New Site"'),
      ).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByText('+ Add new site "Brand New Site"'));

    const updater =
      setOrderData.mock.calls[setOrderData.mock.calls.length - 1][0];
    const result = updater({ sampleOrderItems: { environmentalFields: {} } });
    expect(result.sampleOrderItems.environmentalFields.samplingSiteName).toBe(
      "Brand New Site",
    );
    expect(result.sampleOrderItems.environmentalFields.samplingSiteId).toBe("");
    expect(
      result.sampleOrderItems.environmentalFields.samplingSiteCode,
    ).toBeTruthy();

    // Selected card shows the deferred-creation tag/helper, and no eager
    // POST to the sites endpoint happened.
    expect(screen.getByText("New")).toBeInTheDocument();
    expect(
      getFromOpenElisServerMock.mock.calls.some(
        ([url]) => url === "/rest/admin/vector/sampling-sites",
      ),
    ).toBe(false);
  });

  it("disables '+ Add new site' and shows a message when restrictFreeTextSampSiteEntry is true", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
      if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
        callback([]);
      }
    });

    renderSection({
      configurationProperties: { restrictFreeTextSampSiteEntry: "true" },
    });

    fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
      target: { value: "Brand New Site" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new site "Brand New Site"'),
      ).toBeInTheDocument(),
    );

    expect(screen.getByText('+ Add new site "Brand New Site"')).toBeDisabled();
    expect(
      screen.getByText(
        "Adding a new sampling site has been disabled by your administrator.",
      ),
    ).toBeInTheDocument();
  });

  it("keeps '+ Add new site' enabled when restrictFreeTextSampSiteEntry is false", async () => {
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
      if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
        callback([]);
      }
    });

    renderSection({
      configurationProperties: { restrictFreeTextSampSiteEntry: "false" },
    });

    fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
      target: { value: "Brand New Site" },
    });

    await waitFor(() =>
      expect(
        screen.getByText('+ Add new site "Brand New Site"'),
      ).toBeInTheDocument(),
    );

    expect(
      screen.getByText('+ Add new site "Brand New Site"'),
    ).not.toBeDisabled();
  });

  describe("edit-lock behavior", () => {
    it("locks the site name/code/type fields after selecting a search result, and unlocks via Edit details", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
          callback([]);
        }
        if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
          callback([
            { id: "5", code: "WS-1", name: "Existing Well", type: "Water" },
          ]);
        }
      });

      renderSection();

      fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
        target: { value: "Existing" },
      });

      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("Site Name")).toBeDisabled();
      expect(screen.getByLabelText("Site Code")).toBeDisabled();

      fireEvent.click(screen.getByText("Edit details"));

      expect(screen.getByLabelText("Site Name")).not.toBeDisabled();
      expect(screen.getByLabelText("Site Code")).not.toBeDisabled();

      fireEvent.change(screen.getByLabelText("Site Name"), {
        target: { value: "Renamed Well" },
      });
      expect(screen.getByLabelText("Site Name")).toHaveValue("Renamed Well");
    });

    it("leaves a brand-new site's fields editable immediately, tagged 'New', with no Edit details link", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
          callback([]);
        }
        if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
          callback([]);
        }
      });

      renderSection();

      fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
        target: { value: "Brand New Site" },
      });

      await waitFor(() =>
        expect(
          screen.getByText('+ Add new site "Brand New Site"'),
        ).toBeInTheDocument(),
      );
      fireEvent.click(screen.getByText('+ Add new site "Brand New Site"'));

      expect(screen.getByLabelText("Site Name")).not.toBeDisabled();
      expect(screen.getByLabelText("Site Name")).toHaveValue("Brand New Site");
      expect(screen.getByLabelText("Site Code")).not.toBeDisabled();
      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });

    it("resets lock state on Clear", async () => {
      getFromOpenElisServerMock.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
          callback([]);
        }
        if (url.startsWith("/rest/admin/vector/sampling-sites/search")) {
          callback([
            { id: "5", code: "WS-1", name: "Existing Well", type: "Water" },
          ]);
        }
      });

      renderSection();

      fireEvent.change(screen.getByPlaceholderText(/Search by site name/), {
        target: { value: "Existing" },
      });
      await waitFor(() => screen.getByText("Select"));
      fireEvent.click(screen.getByText("Select"));

      expect(screen.getByLabelText("Site Code")).toBeDisabled();

      fireEvent.click(screen.getByText("Clear"));

      expect(screen.queryByText("Edit details")).not.toBeInTheDocument();
    });
  });
});
