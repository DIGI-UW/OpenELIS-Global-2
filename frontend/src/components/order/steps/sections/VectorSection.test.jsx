import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { addDays, format } from "date-fns";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import VectorSection from "./VectorSection";
import { ConfigurationContext } from "../../../layout/Layout";

const { getFromOpenElisServerMock, orderContextMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
  orderContextMock: {
    samples: [{}],
    setSamples: vi.fn(),
    hydrateOrderData: vi.fn(),
    hydrateSamples: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
}));

vi.mock("../../OrderContext", () => ({
  useOrderContext: () => orderContextMock,
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

// ---------------------------------------------------------------------------
// OGC-1192 — reopening a saved environmental order. The lot Collection Date
// was fixed at mount, so it showed today instead of the saved date, and the
// site lookup and the default-date fill marked the untouched order as having
// unsaved changes (the page then prompted before every navigation).
// ---------------------------------------------------------------------------
describe("VectorSection — reopening a saved order (OGC-1192)", () => {
  const savedSite = {
    id: "3",
    code: "IDN-BKS-01",
    name: "Bekasi Riverside",
    type: "Water Source",
  };

  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
      if (url.startsWith("/rest/admin/vector/sampling-sites/3")) {
        callback(savedSite);
      }
    });
    orderContextMock.samples = [{}];
    orderContextMock.setSamples = vi.fn();
    orderContextMock.hydrateOrderData = vi.fn();
    orderContextMock.hydrateSamples = vi.fn();
  });

  const savedOrder = {
    sampleOrderItems: {
      environmentalFields: {
        samplingSiteId: "3",
        samplingSiteName: "Bekasi Riverside",
      },
    },
  };

  test("shows the saved collection date once the order's samples load", () => {
    const view = renderSection();
    expect(document.getElementById("vec-collection-date").value).not.toBe(
      "09/27/2026",
    );

    orderContextMock.samples = [{ collectionDate: "2026-09-27" }];
    view.rerender(
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <IntlProvider locale="en" messages={messages}>
          <VectorSection
            orderData={savedOrder}
            setOrderData={vi.fn()}
            isReadOnly={false}
            workflowType="environmental"
          />
        </IntlProvider>
      </ConfigurationContext.Provider>,
    );

    expect(document.getElementById("vec-collection-date").value).toBe(
      "09/27/2026",
    );
  });

  test("restoring the saved site does not mark the order as edited", async () => {
    const setOrderData = vi.fn();
    orderContextMock.samples = [{ collectionDate: "2026-09-27" }];

    renderSection({ orderData: savedOrder, setOrderData });

    await waitFor(() =>
      expect(orderContextMock.hydrateOrderData).toHaveBeenCalled(),
    );
    expect(setOrderData).not.toHaveBeenCalled();
    expect(orderContextMock.setSamples).not.toHaveBeenCalled();
  });

  test("defaulting a blank sample's collection date does not mark it edited", () => {
    renderSection();

    expect(orderContextMock.hydrateSamples).toHaveBeenCalled();
    expect(orderContextMock.setSamples).not.toHaveBeenCalled();
  });
});

// OGC-1657 — the lot Collection Date hard-coded YYYY-MM-DD while every other
// date followed the site date format.
// ---------------------------------------------------------------------------
describe("VectorSection — Collection Date follows the site date format (OGC-1657)", () => {
  const dayFirst = { DEFAULT_DATE_LOCALE: "fr-FR" };
  const monthFirst = { DEFAULT_DATE_LOCALE: "en-US" };
  const savedOrder = (workflowType) => ({
    sampleOrderItems: {
      environmentalFields: {
        [workflowType === "vector" ? "vecCollectionDate" : "envCollectionDate"]:
          "2026-03-05",
      },
    },
  });
  const field = () => document.getElementById("vec-collection-date");
  const stored = (setOrderData) =>
    setOrderData.mock.calls
      .map(([update]) =>
        update({ sampleOrderItems: { environmentalFields: {} } }),
      )
      .map((next) => next.sampleOrderItems.environmentalFields)
      .pop();

  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/vector/dictionary/sampling-site-types")) {
        callback([]);
      }
    });
    orderContextMock.samples = [{}];
    orderContextMock.setSamples = vi.fn();
    orderContextMock.hydrateOrderData = vi.fn();
    orderContextMock.hydrateSamples = vi.fn();
  });

  test.each(["environmental", "vector"])(
    "%s: a dd/MM/yyyy site shows the saved date and placeholder day first",
    (workflowType) => {
      renderSection({
        workflowType,
        orderData: savedOrder(workflowType),
        configurationProperties: dayFirst,
      });

      expect(field()).toHaveAttribute("placeholder", "dd/mm/yyyy");
      expect(field().value).toBe("05/03/2026");
    },
  );

  test("a MM/dd/yyyy site shows the saved date and placeholder month first", () => {
    renderSection({
      orderData: savedOrder("environmental"),
      configurationProperties: monthFirst,
    });

    expect(field()).toHaveAttribute("placeholder", "mm/dd/yyyy");
    expect(field().value).toBe("03/05/2026");
  });

  test.each([
    ["environmental", dayFirst, "05/03/2026", "envCollectionDate"],
    ["vector", dayFirst, "05/03/2026", "vecCollectionDate"],
    ["environmental", monthFirst, "03/05/2026", "envCollectionDate"],
  ])(
    "%s: a date typed in the site format is stored as that day",
    async (workflowType, configurationProperties, typed, key) => {
      const setOrderData = vi.fn();
      renderSection({ workflowType, setOrderData, configurationProperties });
      const user = userEvent.setup();

      await user.clear(field());
      await user.type(field(), `${typed}{Enter}`);

      await waitFor(() =>
        expect(stored(setOrderData)?.[key]).toBe("2026-03-05"),
      );
      expect(orderContextMock.setSamples).toHaveBeenLastCalledWith([
        expect.objectContaining({ collectionDate: "2026-03-05" }),
      ]);
    },
  );

  test("a future date is refused on a dd/MM/yyyy site", async () => {
    const setOrderData = vi.fn();
    renderSection({ setOrderData, configurationProperties: dayFirst });
    const user = userEvent.setup();

    await user.clear(field());
    await user.type(
      field(),
      `${format(addDays(new Date(), 2), "dd/MM/yyyy")}{Enter}`,
    );
    await user.click(document.body);

    expect(setOrderData).not.toHaveBeenCalled();
  });
});
