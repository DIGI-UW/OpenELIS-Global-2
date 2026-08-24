import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";
import ProviderSchemeList from "../ProviderSchemeList";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

/** Exactly the shape GET /rest/eqa/provider/schemes answers with. */
const SCHEMES = [
  {
    id: 3,
    name: "National HIV VL PT",
    provider: "This lab",
    schemeType: "REGIONAL_PT",
    enrolledParticipantCount: 4,
    cycles: [
      {
        id: 7,
        cycleNumber: 2,
        cycleName: "2026 Round 2",
        status: "PREP_IN_PROGRESS",
        participantCount: 3,
        panelCount: 1,
      },
    ],
  },
];

const renderList = (schemes = SCHEMES) => {
  getFromOpenElisServer.mockImplementation((url, cb) =>
    url === "/rest/eqa/provider/schemes" ? cb(schemes) : cb([]),
  );
  const history = [];
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/schemes"]}>
        <Route path="/qa/eqa/provider/schemes" exact>
          <ProviderSchemeList />
        </Route>
        <Route
          path="/qa/eqa/provider/schemes/:schemeId/cycles/new"
          render={({ match }) => {
            history.push(match.url);
            return <div>wizard</div>;
          }}
        />
      </MemoryRouter>
    </IntlProvider>,
  );
  return { ...view, history };
};

describe("ProviderSchemeList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("lists each provided scheme with its enrollment and cycle counts", () => {
    renderList();

    expect(screen.getByText("National HIV VL PT")).toBeInTheDocument();
    expect(screen.getByText("Regional PT")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  test("expanding a scheme reveals its cycles", () => {
    renderList();
    // Carbon keeps the expanded row mounted and hides it with CSS, so the
    // contract to assert is the expand state, not absence from the DOM.
    const expander = screen.getByLabelText("Show cycles");
    expect(expander).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(expander);

    expect(expander).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("2026 Round 2")).toBeInTheDocument();
    // The count comes from the cycle's own roster, not the scheme's enrollment.
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("Prep in progress")).toBeInTheDocument();
  });

  test("an expanded cycle links into the prep and shipping workbench", () => {
    renderList();
    fireEvent.click(screen.getByLabelText("Show cycles"));

    expect(screen.getByRole("link", { name: "2026 Round 2" })).toHaveAttribute(
      "href",
      "/qa/eqa/provider/cycles/7/workbench",
    );
  });

  test("New cycle opens the wizard for that scheme", () => {
    const { history } = renderList();

    fireEvent.click(screen.getByRole("button", { name: "New cycle" }));

    expect(history).toContain("/qa/eqa/provider/schemes/3/cycles/new");
  });

  test("a lab that provides nothing is told why the list is empty", () => {
    renderList([]);

    expect(screen.getByText("No schemes to provide yet")).toBeInTheDocument();
  });

  test("a scheme with no cycle yet says so instead of rendering an empty table", () => {
    renderList([{ ...SCHEMES[0], cycles: [] }]);
    fireEvent.click(screen.getByLabelText("Show cycles"));

    expect(
      screen.getByText(
        "No cycles yet. Start one to define its panel and participants.",
      ),
    ).toBeInTheDocument();
  });
});
