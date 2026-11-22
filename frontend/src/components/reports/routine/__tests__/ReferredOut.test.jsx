import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ReferredOut from "../ReferredOut";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

vi.mock("../../../common/CustomDatePicker", () => ({
  default: ({ id, onChange }) => (
    <button type="button" onClick={() => onChange("2026-11-03")}>
      {"set " + id}
    </button>
  ),
}));

vi.mock("../../../common/CustomNotification", () => ({
  AlertDialog: () => null,
}));

const optionNames = () =>
  screen.queryAllByRole("option").map((option) => option.textContent);

/**
 * The referral laboratory of the Referred Out report is searched by typing
 * (OGC-1515), and the report, which has nothing to show without one, is only
 * opened once a laboratory is chosen.
 */
describe("Referred Out report laboratory", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/displayList/REFERRAL_ORGANIZATIONS") {
        callback([
          { id: "7", value: "Laboratoire National de Référence" },
          { id: "8", value: "CDC Atlanta" },
          { id: "9", value: "Institut Pasteur" },
        ]);
      } else {
        callback([]);
      }
    });
    vi.spyOn(window, "open").mockImplementation(() => ({}));
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
  });

  const renderReport = async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ReferredOut />
      </IntlProvider>,
    );
    await userEvent.click(screen.getByRole("button", { name: "set startDate" }));
    await userEvent.click(screen.getByRole("button", { name: "set endDate" }));
    return screen.getByRole("combobox", {
      name: "Referral Center or Laboratory",
    });
  };

  it("narrows the laboratories to what is typed, ignoring accents, and reports on the chosen one", async () => {
    const box = await renderReport();

    await userEvent.type(box, "reference");
    expect(optionNames()).toEqual(["Laboratoire National de Référence"]);
    await userEvent.keyboard("{ArrowDown}{Enter}");
    await userEvent.click(
      screen.getByRole("button", { name: "Generate Printable Version" }),
    );

    expect(window.open).toHaveBeenCalledTimes(1);
    const opened = new URL(window.open.mock.calls[0][0], "https://lab.test");
    expect(opened.searchParams.get("report")).toBe("referredOut");
    expect(opened.searchParams.get("locationCode")).toBe("7");
  });

  it("asks for a laboratory instead of opening a report without one", async () => {
    await renderReport();

    await userEvent.click(
      screen.getByRole("button", { name: "Generate Printable Version" }),
    );

    expect(window.open).not.toHaveBeenCalled();
    expect(
      screen.getByText("Choose the referral center or laboratory to report on."),
    ).toBeInTheDocument();
  });
});
