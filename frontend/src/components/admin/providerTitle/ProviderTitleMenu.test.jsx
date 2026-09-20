/**
 * Provider Titles admin page (OGC-1223).
 *
 * - lists titles with their in-use count and status;
 * - filters by status and by text over title and abbreviation;
 * - deactivates rather than deletes, naming how many providers keep the title;
 * - reports the server's duplicate message instead of silently failing.
 */

const mockHistory = {
  push: vi.fn(),
  replace: vi.fn(),
  location: { search: "" },
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useHistory: () => mockHistory };
});

const {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import ProviderTitleMenu, {
  filterTitles,
  plainMessage,
} from "./ProviderTitleMenu";
import messages from "../../../languages/en.json";

const TITLES = [
  {
    id: "1",
    title: "Doctor",
    abbreviation: "Dr",
    sortOrder: 10,
    active: true,
    inUse: 4,
  },
  {
    id: "2",
    title: "Health Extension Officer",
    abbreviation: "HEO",
    sortOrder: 20,
    active: false,
    inUse: 2,
  },
];

const serve = (titles = TITLES) =>
  getFromOpenElisServer.mockImplementation((url, cb) =>
    cb(url.startsWith("/rest/admin/provider-titles") ? titles : []),
  );

const wrap = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <ProviderTitleMenu />
      </IntlProvider>
    </BrowserRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  mockHistory.location = { search: "" };
});

describe("ProviderTitleMenu (OGC-1223)", () => {
  it("lists a title with its abbreviation and in-use count", async () => {
    serve();
    wrap();

    expect(await screen.findByText("Doctor")).toBeInTheDocument();
    expect(screen.getByText("Dr")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("shows only active titles until the filter is widened", async () => {
    serve();
    wrap();

    // The default view is the titles a provider can be given now.
    expect(await screen.findByText("Doctor")).toBeInTheDocument();
    expect(
      screen.queryByText("Health Extension Officer"),
    ).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "all" },
    });
    expect(screen.getByText("Health Extension Officer")).toBeInTheDocument();
  });

  it("summarises how many titles there are", async () => {
    serve();
    wrap();

    expect(
      await screen.findByTestId("provider-title-summary"),
    ).toHaveTextContent("2 title(s): 1 active, 1 inactive.");
  });

  it("deactivating names the providers that keep the title, and never deletes", async () => {
    serve();
    wrap();
    await screen.findByText("Doctor");

    fireEvent.click(screen.getByText("Deactivate"));

    expect(screen.getByText(/Deactivate "Doctor"\?/)).toBeInTheDocument();
    expect(
      screen.getByText(/The 4 provider\(s\) already carrying it keep it\./),
    ).toBeInTheDocument();
    // No delete is offered anywhere on the page.
    expect(screen.queryByText("Delete")).not.toBeInTheDocument();
  });

  it("sends the deactivation as a status change", async () => {
    serve();
    wrap();
    await screen.findByText("Doctor");

    fireEvent.click(screen.getByText("Deactivate"));
    // Scoped to the confirmation's footer: the row carries the same label.
    const confirm = document.querySelector(
      ".cds--modal-footer button.cds--btn--danger",
    );
    fireEvent.click(confirm);

    await waitFor(() =>
      expect(patchToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        "/rest/admin/provider-titles/1/active?active=false",
        null,
        expect.any(Function),
      ),
    );
  });

  // The server answers a rejected save with the reason as a JSON string, so the
  // body arrives quoted. A mock that hands the callback a bare string agrees
  // with any implementation and hid this from the suite until the page was
  // driven in a browser.
  it("surfaces the server's duplicate message rather than failing silently", async () => {
    serve();
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({
        ok: false,
        status: 422,
        text: () => Promise.resolve('"\\"Doctor\\" is already in the list"'),
      }),
    );
    wrap();
    await screen.findByText("Doctor");

    fireEvent.click(screen.getByTestId("provider-title-add"));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "Doctor" },
    });
    fireEvent.change(screen.getByLabelText("Abbreviation"), {
      target: { value: "Dr" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByText('"Doctor" is already in the list'),
    ).toBeInTheDocument();
  });

  it("keeps the form open on a rejected save so it can be corrected", async () => {
    serve();
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({
        ok: false,
        status: 422,
        text: () => Promise.resolve('"\\"Doctor\\" is already in the list"'),
      }),
    );
    wrap();
    await screen.findByText("Doctor");

    fireEvent.click(screen.getByTestId("provider-title-add"));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "Doctor" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await screen.findByText('"Doctor" is already in the list');
    expect(screen.getByLabelText("Title")).toHaveValue("Doctor");
  });

  it("closes the form and reloads the list once the save is accepted", async () => {
    serve();
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ ok: true, status: 201, text: () => Promise.resolve("") }),
    );
    wrap();
    await screen.findByText("Doctor");
    const loadsBefore = getFromOpenElisServer.mock.calls.length;

    fireEvent.click(screen.getByTestId("provider-title-add"));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "Registrar" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() =>
      expect(getFromOpenElisServer.mock.calls.length).toBeGreaterThan(
        loadsBefore,
      ),
    );
    expect(screen.queryByLabelText("Title")).not.toBeInTheDocument();
  });
});

describe("plainMessage (pure)", () => {
  it("unwraps the quoted string the server sends", () => {
    expect(plainMessage('"\\"Doctor\\" is already in the list"')).toBe(
      '"Doctor" is already in the list',
    );
  });

  it("passes a plain-text body through untouched", () => {
    expect(plainMessage("Something went wrong")).toBe("Something went wrong");
  });

  it("leaves a JSON object body alone", () => {
    expect(plainMessage('{"error":"nope"}')).toBe('{"error":"nope"}');
  });
});

describe("filterTitles (pure)", () => {
  it("matches title and abbreviation case-insensitively", () => {
    expect(
      filterTitles(TITLES, { search: "doct", status: "all" }),
    ).toHaveLength(1);
    expect(filterTitles(TITLES, { search: "heo", status: "all" })).toHaveLength(
      1,
    );
  });

  it("filters by status", () => {
    expect(filterTitles(TITLES, { search: "", status: "active" })).toHaveLength(
      1,
    );
    expect(
      filterTitles(TITLES, { search: "", status: "inactive" }),
    ).toHaveLength(1);
    expect(filterTitles(TITLES, { search: "", status: "all" })).toHaveLength(2);
  });
});
