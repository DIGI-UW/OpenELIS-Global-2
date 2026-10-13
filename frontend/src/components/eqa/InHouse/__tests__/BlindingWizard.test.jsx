import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import BlindingWizard from "../BlindingWizard";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  formatDateOnly: (value) => value,
}));

vi.mock("../inHouseApi", () => ({
  downloadLabelSheet: vi.fn(),
  fetchAnalysts: (_schemeId, callback) => callback([]),
  fetchInHouseSchemes: (callback) =>
    callback([
      { id: "3", name: "Malaria scheme" },
      { id: "4", name: "HIV scheme" },
      { id: "5", name: "Empty scheme" },
    ]),
  fetchLabUsers: (callback) => callback([]),
  saveAnalystRoster: vi.fn(),
  sealAndDistribute: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const SCHEME_TESTS = {
  3: [
    { id: 1, testId: 55, isActive: true },
    { id: 2, testId: 56, isActive: true },
    { id: 3, testId: 57, isActive: false },
  ],
  4: [{ id: 4, testId: 55, isActive: true }],
  5: [],
};

const renderWizard = () => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    const schemeTests = url.match(/^\/rest\/eqa\/programs\/(\d+)\/tests$/);
    if (schemeTests) cb(SCHEME_TESTS[schemeTests[1]]);
    else if (url === "/rest/eqa/testable-tests") cb(["55", "56", "57", "58"]);
    else if (url === "/rest/displayList/ALL_TESTS")
      cb([
        { id: "55", value: "HIV Viral Load" },
        { id: "56", value: "Malaria smear" },
        { id: "57", value: "CD4 count" },
        { id: "58", value: "Glucose" },
      ]);
    else cb([]);
  });
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <BlindingWizard />
      </MemoryRouter>
    </IntlProvider>,
  );
};

const SCHEME_NAMES = {
  3: "Malaria scheme",
  4: "HIV scheme",
  5: "Empty scheme",
};

const schemeSelector = () =>
  screen.getByRole("combobox", { name: "In-house scheme" });

const chooseScheme = (schemeId) => {
  fireEvent.click(schemeSelector());
  fireEvent.click(screen.getByRole("option", { name: SCHEME_NAMES[schemeId] }));
};

const pickScheme = (schemeId) => {
  chooseScheme(schemeId);
  fireEvent.change(
    screen.getByLabelText("Unblind date (submission deadline)"),
    { target: { value: "2026-10-31" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Next" }));
};

const offeredTests = (container) =>
  [...container.querySelectorAll("#test-S01 option")]
    .map((option) => option.textContent)
    .filter(Boolean);

describe("BlindingWizard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("the test picker offers only the scheme's active tests", () => {
    const { container } = renderWizard();
    pickScheme("3");

    expect(offeredTests(container)).toEqual([
      "HIV Viral Load",
      "Malaria smear",
    ]);
  });

  test("same-named tests are told apart by their sample type", () => {
    const { container } = renderWizard();
    const reply = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, cb) =>
      url === "/rest/eqa/programs/3/tests"
        ? cb([
            { id: 1, testId: 55, isActive: true, label: "Glucose (Serum)" },
            { id: 2, testId: 56, isActive: true, label: "Glucose (Plasma)" },
          ])
        : reply(url, cb),
    );
    pickScheme("3");

    expect(offeredTests(container)).toEqual([
      "Glucose (Serum)",
      "Glucose (Plasma)",
    ]);
  });

  test("a picked test outside a newly chosen scheme is cleared", () => {
    const { container } = renderWizard();
    pickScheme("3");
    fireEvent.change(container.querySelector("#test-S01"), {
      target: { value: "56" },
    });
    expect(container.querySelector("#test-S01")).toHaveValue("56");

    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    pickScheme("4");

    expect(offeredTests(container)).toEqual(["HIV Viral Load"]);

    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    pickScheme("3");
    expect(container.querySelector("#test-S01")).toHaveValue("");
  });

  test("keeps the chosen scheme's tests when an earlier scheme's reply lands late", () => {
    const { container } = renderWizard();
    const replies = {};
    const serve = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      const schemeTests = url.match(/^\/rest\/eqa\/programs\/(\d+)\/tests$/);
      if (schemeTests) replies[schemeTests[1]] = () => serve(url, cb);
      else serve(url, cb);
    });

    chooseScheme("3");
    chooseScheme("4");
    act(() => replies["4"]());
    act(() => replies["3"]());
    pickScheme("4");

    expect(offeredTests(container)).toEqual(["HIV Viral Load"]);
  });

  test("a scheme with no tests offers none and says so", () => {
    const { container } = renderWizard();
    pickScheme("5");

    expect(offeredTests(container)).toEqual([]);
    expect(
      screen.getByText(/This scheme has no tests a panel can use/),
    ).toBeInTheDocument();
  });

  test("the picker waits for the scheme's tests instead of saying it has none", () => {
    const { container } = renderWizard();
    const reply = getFromOpenElisServer.getMockImplementation();
    let lateCatalog;
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/displayList/ALL_TESTS") {
        lateCatalog = () => reply(url, cb);
      } else {
        reply(url, cb);
      }
    });
    pickScheme("3");

    expect(
      screen.queryByText(/This scheme has no tests a panel can use/),
    ).toBeNull();
    act(() => lateCatalog());
    expect(offeredTests(container)).toEqual([
      "HIV Viral Load",
      "Malaria smear",
    ]);
  });

  test("a failed read says so instead of claiming the scheme has no tests", () => {
    renderWizard();
    const reply = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, cb) =>
      url === "/rest/eqa/testable-tests" ? cb(undefined) : reply(url, cb),
    );
    pickScheme("3");

    expect(
      screen.getByText(/The scheme's tests could not be loaded/),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/This scheme has no tests a panel can use/),
    ).toBeNull();
  });
});
