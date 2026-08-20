import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import SchemesPage from "../SchemesPage";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const SCHEMES = [
  {
    id: 3,
    name: "National HIV VL PT",
    provider: "This lab",
    schemeType: "REGIONAL_PT",
    participantCount: 2,
  },
  {
    id: 5,
    name: "Awaiting participants",
    provider: "This lab",
    schemeType: "INTER_LAB_SPLIT",
    participantCount: 0,
  },
  { id: 9, name: "Internal round", schemeType: "IN_HOUSE" },
];

const CYCLES = [
  {
    id: 41,
    schemeId: 3,
    cycleNumber: 1,
    cycleName: "2026 Round 1",
    status: "PREP_IN_PROGRESS",
    panelCount: 1,
    distributionMethod: "FHIR",
  },
  {
    id: 42,
    schemeId: 99,
    cycleNumber: 1,
    cycleName: "Someone else's round",
    status: "SHIPPED",
    panelCount: 1,
  },
];

const goodReads = (url, cb) => {
  if (url === "/rest/eqa/programs") cb(SCHEMES);
  else if (url === "/rest/eqa/provider/cycles") cb(CYCLES);
  else cb([]);
};

const renderPage = (reads = goodReads) => {
  getFromOpenElisServer.mockImplementation(reads);
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <SchemesPage />
      </MemoryRouter>
    </IntlProvider>,
  );
};

describe("SchemesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("lists the schemes this lab provides and leaves in-house to its own page", () => {
    renderPage();

    expect(screen.getByText("National HIV VL PT")).toBeInTheDocument();
    expect(screen.getByText("Awaiting participants")).toBeInTheDocument();
    expect(screen.queryByText("Internal round")).not.toBeInTheDocument();
  });

  test("a scheme with no participants offers enrolment instead of a new cycle", () => {
    renderPage();

    // The enrolled scheme can start a cycle; the empty one cannot.
    expect(screen.getAllByText("New cycle")).toHaveLength(1);
    expect(screen.getByText("Enrol participants")).toBeInTheDocument();
  });

  test("expanding a scheme shows only its own cycles", () => {
    renderPage();

    fireEvent.click(screen.getAllByLabelText("Cycles")[0]);

    expect(screen.getByText("2026 Round 1")).toBeInTheDocument();
    expect(screen.getByText("Prep in progress")).toBeInTheDocument();
    expect(screen.getByText("FHIR")).toBeInTheDocument();
    // A cycle of another scheme must not appear under this one.
    expect(screen.queryByText("Someone else's round")).not.toBeInTheDocument();
  });

  test("a scheme with no cycles says so rather than showing an empty table", () => {
    renderPage();

    fireEvent.click(screen.getAllByLabelText("Cycles")[1]);

    expect(
      screen.getByText("No cycles yet for this scheme."),
    ).toBeInTheDocument();
  });

  test("a refused read leaves an empty page instead of crashing on the error body", () => {
    renderPage((url, cb) => cb({ error: "403" }));

    expect(screen.getByText("No schemes yet")).toBeInTheDocument();
  });
});
