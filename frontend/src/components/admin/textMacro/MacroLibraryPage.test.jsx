vi.mock("../../common/textMacro/TextMacroService", () => ({
  getAdminMacroPage: vi.fn(),
  getAdminMacro: vi.fn(),
  saveAdminMacro: vi.fn(),
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import {
  getAdminMacro,
  getAdminMacroPage,
  saveAdminMacro,
} from "../../common/textMacro/TextMacroService";
import MacroLibraryPage from "./MacroLibraryPage";

const renderPage = (url = "/admin/MacroLibrary") => {
  window.history.pushState({}, "", url);
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <MacroLibraryPage />
      </IntlProvider>
    </BrowserRouter>,
  );
};

beforeEach(() => {
  vi.clearAllMocks();
  getAdminMacroPage.mockResolvedValue({
    items: [
      {
        id: "gpc",
        code: ".gpc",
        expansionText: "Gram-positive cocci",
        contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
        active: true,
        provenance: "LOCAL",
      },
    ],
    page: 1,
    pageSize: 20,
    total: 1,
  });
  getAdminMacro.mockResolvedValue({
    id: "gpc",
    code: ".gpc",
    expansionText: "Gram-positive cocci",
    contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
    active: true,
    provenance: "LOCAL",
  });
  saveAdminMacro.mockResolvedValue({ id: "new-macro" });
});

describe("MacroLibraryPage", () => {
  it("renders a Carbon administration table and writes create state to the URL", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Phrase actions" }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Add phrase" }));

    expect(window.location.search).toContain("edit=new");
    expect(
      screen.getByRole("dialog", { name: "Add phrase" }),
    ).toBeInTheDocument();
  });

  it("loads exact edit state from a bookmark and saves without an actor field", async () => {
    const user = userEvent.setup();
    renderPage(
      "/admin/MacroLibrary?q=gpc&context=all&status=active&sort=code%3Aasc&page=1&pageSize=20&edit=gpc",
    );

    expect(
      await screen.findByRole("dialog", { name: "Edit phrase" }),
    ).toBeInTheDocument();
    await user.clear(screen.getByRole("textbox", { name: "Phrase text" }));
    await user.type(
      screen.getByRole("textbox", { name: "Phrase text" }),
      "Gram-positive cocci in clusters",
    );
    await user.click(screen.getByRole("button", { name: "Save phrase" }));

    expect(saveAdminMacro).toHaveBeenCalledWith(
      expect.not.objectContaining({ actor: expect.anything() }),
    );
    expect(window.location.search).not.toContain("edit=");
  });
});
