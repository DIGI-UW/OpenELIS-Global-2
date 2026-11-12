vi.mock("../../common/textMacro/TextMacroService", () => ({
  bulkAdminMacros: vi.fn(),
  exportAdminMacros: vi.fn(),
  getAdminMacroPage: vi.fn(),
  getAdminMacro: vi.fn(),
  saveAdminMacro: vi.fn(),
}));
vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  downloadBlob: vi.fn(),
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
import { downloadBlob } from "../../utils/Utils";
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
        category: "GRAM_STAIN",
        contexts: ["MICROBIOLOGY_CULTURE_READING"],
        active: true,
        provenance: "LOCAL",
      },
      {
        id: "ng24",
        code: ".ng24",
        expansionText: "No growth at 24 hours",
        category: "CULTURE",
        contexts: ["MICROBIOLOGY_CULTURE_READING", "MICROBIOLOGY_CASE_NOTE"],
        active: true,
        provenance: "PACKAGED",
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
    category: "GRAM_STAIN",
    contexts: ["MICROBIOLOGY_CULTURE_READING"],
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

const phrase = (id, active = true) => ({
  id,
  code: `.${id}`,
  expansionText: `Phrase ${id}`,
  category: "CULTURE",
  contexts: ["MICROBIOLOGY_CULTURE_READING"],
  active,
  provenance: "LOCAL",
});
const pageOf = (...items) => ({
  items,
  page: 1,
  pageSize: 20,
  total: items.length,
});

describe("MacroLibraryPage", () => {
  it("keeps rendering when a filter leaves fewer rows", async () => {
    const user = userEvent.setup();
    getAdminMacroPage.mockImplementation((query) =>
      Promise.resolve(
        query.includes("category=AST")
          ? pageOf(phrase("beta"))
          : pageOf(phrase("alpha"), phrase("beta"), phrase("gamma")),
      ),
    );
    renderPage();

    expect(await screen.findByText(".gamma")).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Category"), "AST");

    expect(await screen.findByText(".beta")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText(".alpha")).toBeNull());
    expect(screen.queryByText(".gamma")).toBeNull();
    expect(
      screen.getByRole("checkbox", { name: "Select .beta" }),
    ).toBeInTheDocument();
  });

  it("keeps rendering after a bulk deactivation empties part of the Active view", async () => {
    const user = userEvent.setup();
    getAdminMacroPage
      .mockResolvedValueOnce(pageOf(phrase("alpha"), phrase("beta")))
      .mockResolvedValue(pageOf(phrase("beta")));
    renderPage();

    expect(await screen.findByText(".alpha")).toBeInTheDocument();
    await user.click(screen.getByRole("checkbox", { name: "Select .alpha" }));
    await user.click(screen.getByRole("button", { name: "Deactivate" }));
    await user.click(
      screen.getByRole("button", { name: "Deactivate phrases" }),
    );

    await waitFor(() => expect(screen.queryByText(".alpha")).toBeNull());
    expect(screen.getByText(".beta")).toBeInTheDocument();
    expect(bulkAdminMacros).toHaveBeenCalledWith({
      action: "DEACTIVATE",
      ids: ["alpha"],
    });
  });

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
    await user.clear(screen.getByLabelText("Phrase text"));
    await user.type(
      screen.getByLabelText("Phrase text"),
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
    expect(
      screen.queryByRole("button", { name: "Activate", hidden: true }),
    ).not.toBeInTheDocument();
    await user.click(screen.getByRole("checkbox", { name: "Select .gpc" }));
    expect(
      screen.getByRole("button", { name: "Activate" }),
    ).toBeInTheDocument();
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

  it("shows category, field contexts, and source for each phrase", async () => {
    renderPage();

    const row = (await screen.findByText(".ng24")).closest("tr");
    expect(within(row).getByText("Culture")).toBeInTheDocument();
    expect(
      within(row).getByText("Culture reading notes, Case notes"),
    ).toBeInTheDocument();
    expect(within(row).getByText("OpenELIS package")).toBeInTheDocument();
    const local = screen.getByText(".gpc").closest("tr");
    expect(within(local).getByText("Gram stain")).toBeInTheDocument();
    expect(within(local).getByText("Local")).toBeInTheDocument();
  });

  it("filters by category and source through the bookmarkable URL", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Category"), "AST");
    await user.selectOptions(screen.getByLabelText("Source"), "local");

    expect(window.location.search).toContain("category=AST");
    expect(window.location.search).toContain("source=local");
    await waitFor(() =>
      expect(getAdminMacroPage).toHaveBeenLastCalledWith(
        expect.stringContaining("category=AST&source=local"),
        expect.any(AbortSignal),
      ),
    );
  });

  it("keeps the editor open and marks a reserved code on the code field", async () => {
    const user = userEvent.setup();
    const rejection = new Error("This code is reserved by the system");
    rejection.payload = { error: "MACRO_CODE_RESERVED" };
    saveAdminMacro.mockRejectedValue(rejection);
    renderPage("/admin/MacroLibrary?edit=new");

    const dialog = await screen.findByRole("dialog", { name: "Add phrase" });
    await user.type(within(dialog).getByLabelText("Shortcut code"), ".help");
    await user.selectOptions(
      within(dialog).getByLabelText("Category"),
      "CLINICAL",
    );
    await user.type(within(dialog).getByLabelText("Phrase text"), "Help text");
    await user.click(within(dialog).getByLabelText("Clinical history"));
    await user.click(
      within(dialog).getByRole("button", { name: "Save phrase" }),
    );

    expect(
      await within(dialog).findByText("This code is reserved by the system."),
    ).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Shortcut code")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(window.location.search).toContain("edit=new");
  });

  it("lists the codes a local removal deletes before it runs", async () => {
    const user = userEvent.setup();
    bulkAdminMacros.mockResolvedValue({
      action: "DELETE_LOCAL",
      affectedCount: 1,
      affectedCodes: [".gpc"],
    });
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    await user.click(screen.getByRole("checkbox", { name: "Select .ng24" }));
    expect(
      screen.getByRole("button", {
        name: "Remove local phrases is unavailable while packaged phrases are selected",
      }),
    ).toBeDisabled();
    await user.click(screen.getByRole("checkbox", { name: "Select .ng24" }));
    await user.click(screen.getByRole("checkbox", { name: "Select .gpc" }));
    await user.click(
      screen.getByRole("button", { name: "Remove local phrases" }),
    );

    const confirmation = screen.getByRole("dialog", {
      name: "Remove 1 local phrase?",
    });
    expect(within(confirmation).getAllByRole("listitem")).toHaveLength(1);
    expect(within(confirmation).getByText(".gpc")).toBeInTheDocument();
    expect(bulkAdminMacros).not.toHaveBeenCalled();
    await user.click(screen.getByRole("button", { name: /Remove phrases$/ }));

    expect(bulkAdminMacros).toHaveBeenCalledWith({
      action: "DELETE_LOCAL",
      ids: ["gpc"],
    });
  });

  it("exports through the shared download helper", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Gram-positive cocci")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Export phrases" }));

    expect(exportAdminMacros).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(downloadBlob).toHaveBeenCalledWith(
        expect.any(Blob),
        "openelis-text-macros.csv",
      ),
    );
  });
});
