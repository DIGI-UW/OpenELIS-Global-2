vi.mock("../../common/textMacro/TextMacroService", () => ({
  bulkAdminMacros: vi.fn(),
  exportAdminMacros: vi.fn(),
  getAdminMacroPage: vi.fn(),
  getAdminMacro: vi.fn(),
  saveAdminMacro: vi.fn(),
}));
vi.mock("../../utils/downloadAttachment", () => ({
  downloadAttachment: vi.fn(),
}));

import React from "react";
import { waitFor, within } from "@testing-library/dom";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import {
  bulkAdminMacros,
  exportAdminMacros,
  getAdminMacro,
  getAdminMacroPage,
  saveAdminMacro,
} from "../../common/textMacro/TextMacroService";
import { downloadAttachment } from "../../utils/downloadAttachment";
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
      {
        id: "ng24",
        code: ".ng24",
        expansionText: "No growth at 24 hours",
        contexts: ["MICROBIOLOGY_CULTURE_ACTIVITY"],
        active: true,
        provenance: "PACKAGE",
        sourceKey: "reviewed-defaults",
        sourceVersion: "2026.08",
      },
    ],
    page: 1,
    pageSize: 20,
    total: 2,
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
  bulkAdminMacros.mockResolvedValue({
    action: "DEACTIVATE",
    affectedCount: 1,
    affectedCodes: [".gpc"],
  });
  exportAdminMacros.mockResolvedValue({
    blob: new Blob(["code\n.gpc"], { type: "text/csv" }),
    filename: "openelis-text-macros.csv",
  });
});

describe("MacroLibraryPage", () => {
  it("renders a Carbon administration table and writes create state to the URL", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: "Phrase actions" }),
    ).toHaveLength(2);
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

  it("confirms a Carbon bulk action with selected codes before applying it", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    await user.click(screen.getByRole("checkbox", { name: "Select .gpc" }));
    await user.click(screen.getByRole("button", { name: "Deactivate" }));

    const confirmation = screen.getByRole("dialog", {
      name: "Deactivate 1 phrase?",
    });
    expect(confirmation).toBeInTheDocument();
    expect(within(confirmation).getByText(".gpc")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Deactivate phrases" }),
    );

    expect(bulkAdminMacros).toHaveBeenCalledWith({
      action: "DEACTIVATE",
      ids: ["gpc"],
    });
    await waitFor(() => expect(getAdminMacroPage).toHaveBeenCalledTimes(2));
    expect(window.location.search).toContain("status=active");
  });

  it("exports through the shared attachment helper", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Export phrases" }));

    expect(exportAdminMacros).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(downloadAttachment).toHaveBeenCalledWith(
        expect.any(Blob),
        "openelis-text-macros.csv",
      ),
    );
  });
});
