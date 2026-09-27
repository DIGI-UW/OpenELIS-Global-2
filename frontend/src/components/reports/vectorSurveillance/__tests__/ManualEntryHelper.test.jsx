import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ManualEntryHelper from "../ManualEntryHelper";

const MOCK_VIEW = {
  periodStart: "2026-02-02",
  periodEnd: "2026-02-08",
  siteId: null,
  rows: [
    {
      metricKey: "POOLS_TESTED",
      label: "Pools tested",
      portalTag: "PT",
      value: "12",
      lowConfidence: false,
    },
    {
      metricKey: "POOLS_POSITIVE",
      label: "Pools positive",
      portalTag: null,
      value: "3",
      lowConfidence: false,
    },
    {
      metricKey: "SPOROZOITE_RATE",
      label: "Sporozoite rate",
      portalTag: null,
      value: "1.20",
      lowConfidence: true,
    },
  ],
};

const submitMock = vi.fn((url, payload, callback) => {
  callback({ status: 201 });
});

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    callback(MOCK_VIEW);
  }),
  postToOpenElisServerFullResponse: (url, payload, callback) =>
    submitMock(url, payload, callback),
}));

vi.mock("../../../../config.json", () => ({
  default: { serverBaseUrl: "http://localhost:8080" },
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

beforeEach(() => {
  submitMock.mockClear();
  Object.assign(navigator, {
    clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
  });
});

describe("ManualEntryHelper", () => {
  it("renders the helper title and a single genus-agnostic metric list", () => {
    renderWithIntl(<ManualEntryHelper />);
    expect(screen.getByText(/Manual Entry Helper/i)).toBeInTheDocument();
    // The decorative species tabs were removed (no per-genus data); the metrics
    // render as one list.
    expect(screen.queryByRole("tab")).not.toBeInTheDocument();
    expect(screen.getByText("Pools tested")).toBeInTheDocument();
  });

  it("renders metric tiles with values and portal tag", () => {
    renderWithIntl(<ManualEntryHelper />);
    expect(screen.getAllByText("Pools tested").length).toBeGreaterThan(0);
    expect(screen.getAllByText("12").length).toBeGreaterThan(0);
    expect(screen.getAllByText("PT").length).toBeGreaterThan(0);
  });

  it("shows the sporozoite value with a low-confidence caveat (not withheld)", () => {
    renderWithIntl(<ManualEntryHelper />);
    // The rate is shown...
    expect(screen.getAllByText("1.20").length).toBeGreaterThan(0);
    // ...accompanied by the caveat (superseding the old "withheld" behavior),
    // and the literal null is never rendered.
    expect(screen.getAllByText(/Confidence reduced/i).length).toBeGreaterThan(0);
    expect(screen.queryByText("null")).not.toBeInTheDocument();
  });

  it("copies a metric value to the clipboard", () => {
    renderWithIntl(<ManualEntryHelper />);
    const copyButtons = screen.getAllByRole("button", { name: /Copy value/i });
    fireEvent.click(copyButtons[0]);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("12");
  });

  it("keeps the low-confidence sporozoite row copyable (copy button not hidden)", () => {
    renderWithIntl(<ManualEntryHelper />);
    const copyButtons = screen.getAllByRole("button", { name: /Copy value/i });
    // All three rows expose a copy button now — including the low-confidence
    // sporozoite row (row 2), which the old code hid while it was gated. A
    // regression to conditional rendering would drop this to 2 buttons.
    expect(copyButtons.length).toBe(3);
    fireEvent.click(copyButtons[2]);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("1.20");
  });

  it("POSTs a snapshot when the week is marked submitted", async () => {
    renderWithIntl(<ManualEntryHelper />);

    // Pick ISO week 2026-W06 (Mon 2026-02-02 .. Sun 2026-02-08).
    fireEvent.change(screen.getByLabelText(/Reporting week/i), {
      target: { value: "2026-W06" },
    });

    fireEvent.click(
      screen.getByRole("button", { name: /Mark week submitted/i }),
    );
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /^Confirm$/i }));
    });

    expect(submitMock).toHaveBeenCalledTimes(1);
    const [url, payload] = submitMock.mock.calls[0];
    expect(url).toMatch(/manual-entry\/submit$/);
    const body = JSON.parse(payload);
    expect(body.periodStart).toBe("2026-02-02");
    expect(body.periodEnd).toBe("2026-02-08");
    // snapshot carries the actual figures, including the low-confidence sporozoite value
    expect(body.valueSnapshot.POOLS_TESTED).toBe("12");
    expect(body.valueSnapshot.SPOROZOITE_RATE).toBe("1.20");
    expect(screen.getByText(/Week marked submitted/i)).toBeInTheDocument();
  });
});
