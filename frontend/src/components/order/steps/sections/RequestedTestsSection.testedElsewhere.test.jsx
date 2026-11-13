import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { serverGet, listMock, markMock, unmarkMock } = vi.hoisted(() => ({
  serverGet: vi.fn(),
  listMock: vi.fn(),
  markMock: vi.fn(),
  unmarkMock: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback) => callback(serverGet(url)),
}));
vi.mock("../../api/orderEntryCleanupApi", () => ({
  listTestedElsewhere: (...args) => listMock(...args),
  markTestedElsewhere: (...args) => markMock(...args),
  unmarkTestedElsewhere: (...args) => unmarkMock(...args),
}));
vi.mock("./TestAssignmentModal", () => ({ default: () => null }));

import RequestedTestsSection from "./RequestedTestsSection";

const LAB = "DEV01260000000001201";
const samples = [
  {
    sampleTypeId: "2",
    sampleTypeName: "Whole Blood",
    tests: [{ id: "7", name: "Haemoglobin" }],
    panels: [{ id: "30", name: "Liver Function Panel", testIds: "8,9" }],
  },
];

const renderSection = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RequestedTestsSection
        samples={samples}
        setSamples={() => {}}
        assignTestToSample={() => {}}
        sampleTypes={[{ id: "2", value: "Whole Blood" }]}
        isReadOnly={false}
        labNumber={LAB}
        referringSite={{ id: "55", name: "Gerehu Clinic" }}
        {...props}
      />
    </IntlProvider>,
  );

const openMenu = (testId) =>
  fireEvent.click(screen.getByTestId(`test-row-actions-${testId}`));

describe("Mark tested elsewhere (OGC-1424, FR-B18, FR-B20)", () => {
  beforeEach(() => {
    serverGet.mockReset();
    serverGet.mockReturnValue({ tests: [], panels: [] });
    listMock.mockReset();
    listMock.mockResolvedValue([]);
    markMock.mockReset();
    unmarkMock.mockReset();
  });

  it("offers it in the test row's More actions menu, not as a column, and not on panels", async () => {
    renderSection();

    await waitFor(() => expect(listMock).toHaveBeenCalledWith(LAB));
    const headers = screen
      .getAllByRole("columnheader")
      .map((h) => h.textContent);
    expect(headers.join("|")).not.toMatch(/Tested elsewhere|Paid/);
    expect(screen.getByTestId("test-row-actions-7")).toBeInTheDocument();
    expect(screen.queryByTestId("test-row-actions-30")).toBeNull();

    openMenu("7");
    expect(screen.getByText("Mark tested elsewhere")).toBeInTheDocument();
  });

  it("marking a test saves it with the referring facility, shows a purple tag and the value and laboratory fields", async () => {
    markMock.mockResolvedValue({
      testId: "7",
      performingLabId: "55",
      performingLabName: "Gerehu Clinic",
      reportedValue: "",
    });
    renderSection();
    await waitFor(() => expect(listMock).toHaveBeenCalled());

    openMenu("7");
    fireEvent.click(screen.getByText("Mark tested elsewhere"));

    expect(markMock).toHaveBeenCalledWith({
      labNumber: LAB,
      testId: "7",
      performingLabId: "55",
      reportedValue: "",
    });
    const tag = await screen.findByTestId("tested-elsewhere-tag-7");
    expect(tag).toHaveTextContent("Tested elsewhere: Gerehu Clinic");
    expect(tag.closest(".cds--tag")).toHaveClass("cds--tag--purple");
    expect(screen.getByLabelText("Reported value")).toBeInTheDocument();
    expect(
      screen.getByText("Result reported by another laboratory"),
    ).toBeInTheDocument();

    openMenu("7");
    expect(screen.getByText("Not tested elsewhere")).toBeInTheDocument();
  });

  it("a reported value is saved when the field is left", async () => {
    listMock.mockResolvedValue([
      {
        testId: "7",
        performingLabId: "55",
        performingLabName: "Gerehu Clinic",
        reportedValue: "",
      },
    ]);
    markMock.mockResolvedValue({
      testId: "7",
      performingLabId: "55",
      performingLabName: "Gerehu Clinic",
      reportedValue: "13.2 g/dL",
    });
    renderSection();

    const value = await screen.findByLabelText("Reported value");
    fireEvent.change(value, { target: { value: "13.2 g/dL" } });
    fireEvent.focusOut(value);
    expect(markMock).toHaveBeenCalledWith({
      labNumber: LAB,
      testId: "7",
      performingLabId: "55",
      reportedValue: "13.2 g/dL",
    });
  });

  it("Not tested elsewhere removes the mark", async () => {
    listMock.mockResolvedValue([
      {
        testId: "7",
        performingLabId: "55",
        performingLabName: "Gerehu Clinic",
      },
    ]);
    unmarkMock.mockResolvedValue({ removed: true });
    renderSection();
    await screen.findByTestId("tested-elsewhere-tag-7");

    openMenu("7");
    fireEvent.click(screen.getByText("Not tested elsewhere"));

    expect(unmarkMock).toHaveBeenCalledWith(LAB, "7");
    await waitFor(() =>
      expect(screen.queryByTestId("tested-elsewhere-tag-7")).toBeNull(),
    );
  });

  it("a refused save says why", async () => {
    markMock.mockRejectedValue(new Error("the test is not on this order"));
    renderSection();
    await waitFor(() => expect(listMock).toHaveBeenCalled());

    openMenu("7");
    fireEvent.click(screen.getByText("Mark tested elsewhere"));

    expect(
      await screen.findByTestId("tested-elsewhere-error"),
    ).toHaveTextContent(
      "Tested elsewhere was not saved: the test is not on this order",
    );
    expect(screen.queryByTestId("tested-elsewhere-tag-7")).toBeNull();
  });

  it("is not offered before the order has a lab number or when read-only", () => {
    renderSection({ labNumber: "" });
    expect(screen.queryByTestId("test-row-actions-7")).toBeNull();
    expect(listMock).not.toHaveBeenCalled();
  });
});
