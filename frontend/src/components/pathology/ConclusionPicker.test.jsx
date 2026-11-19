/**
 * The coded conclusion picker of the Findings section (FR-13.2, design table
 * P1 to P11).
 *
 * The picker asks the server for the dictionary's diagnoses by name or code
 * and keeps the chosen ones as chips; these tests pin when it asks, what it
 * offers, what it says when there is nothing to offer, and where focus goes
 * as chips come and go.
 */
import React, { useState } from "react";
import { vi } from "vitest";
import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ConclusionPicker from "./ConclusionPicker";
import { fetchFromOpenElisServer } from "../utils/Utils";

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

const SEARCH = "/rest/pathology/conclusionEntries?q=";

const adeno = { id: "9101", value: "Adenocarcinoma", code: "C80.1" };
const squamous = {
  id: "9102",
  value: "Squamous cell carcinoma",
  code: "C44.9",
};
const benign = { id: "9103", value: "Benign", code: null };

const answer = (items) => Promise.resolve({ items, total: items.length });

const deferred = () => {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
};

const queriesAsked = () =>
  fetchFromOpenElisServer.mock.calls.map(([url]) =>
    decodeURIComponent(url.slice(SEARCH.length)),
  );

const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const Harness = ({ initial = [], onChangeSpy = () => {}, ...props }) => {
  const [value, setValue] = useState(initial);
  return (
    <ConclusionPicker
      value={value}
      onChange={(next) => {
        onChangeSpy(next);
        setValue(next);
      }}
      {...props}
    />
  );
};

const renderPicker = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Harness readOnly={false} canAddDiagnosis={false} {...props} />
    </IntlProvider>,
  );

const box = () => screen.getByRole("combobox", { name: /Coded conclusion/ });

const chipTexts = () =>
  Array.from(
    screen
      .getByTestId("findings-conclusion-chips")
      .querySelectorAll(".cds--tag__label"),
  ).map((label) => label.textContent);

beforeEach(() => {
  fetchFromOpenElisServer.mockReset();
});

describe("ConclusionPicker", () => {
  it("asks once with an empty query when it opens editable (P1)", async () => {
    fetchFromOpenElisServer.mockImplementation(() =>
      answer([adeno, squamous, benign]),
    );
    renderPicker();

    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());
    expect(fetchFromOpenElisServer).toHaveBeenCalledWith(
      SEARCH,
      expect.any(AbortSignal),
    );

    await userEvent.click(box());
    expect(
      await screen.findByRole("option", { name: "C80.1 Adenocarcinoma" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "C44.9 Squamous cell carcinoma" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Benign" })).toBeInTheDocument();
    await pause(300);
    expect(queriesAsked()).toEqual([""]);
  });

  it("an empty dictionary disables the box and says so (P2)", async () => {
    fetchFromOpenElisServer.mockImplementation(() => answer([]));
    renderPicker();

    expect(
      await screen.findByText("No diagnosis is in the dictionary yet."),
    ).toBeInTheDocument();
    expect(box()).toBeDisabled();
  });

  it("asks once per pause in typing and ignores an answer for an older query (P3)", async () => {
    const stale = deferred();
    const fresh = deferred();
    fetchFromOpenElisServer.mockImplementation((url) => {
      const query = decodeURIComponent(url.slice(SEARCH.length));
      if (query === "") return answer([adeno, squamous]);
      if (query === "ade") return stale.promise;
      return fresh.promise;
    });
    renderPicker();
    await waitFor(() => expect(queriesAsked()).toEqual([""]));

    await userEvent.type(box(), "ade");
    await waitFor(() => expect(queriesAsked()).toEqual(["", "ade"]));
    await pause(400);
    expect(queriesAsked()).toEqual(["", "ade"]);
    // Until the new answer arrives the previous one stays listed.
    expect(
      screen.getByRole("option", { name: "C44.9 Squamous cell carcinoma" }),
    ).toBeInTheDocument();

    await userEvent.type(box(), "n");
    await waitFor(() => expect(queriesAsked()).toEqual(["", "ade", "aden"]));
    fresh.resolve({ items: [adeno], total: 1 });
    await waitFor(() =>
      expect(
        screen.queryByRole("option", {
          name: "C44.9 Squamous cell carcinoma",
        }),
      ).not.toBeInTheDocument(),
    );
    stale.resolve({ items: [benign], total: 1 });
    await pause(50);

    expect(
      screen.getByRole("option", { name: "C80.1 Adenocarcinoma" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Benign" }),
    ).not.toBeInTheDocument();
  });

  it('lists "code name" and leaves out what is already chosen (P4)', async () => {
    fetchFromOpenElisServer.mockImplementation(() =>
      answer([adeno, squamous, benign]),
    );
    renderPicker({ initial: [adeno] });
    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());

    await userEvent.click(box());
    expect(
      await screen.findByRole("option", {
        name: "C44.9 Squamous cell carcinoma",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Benign" })).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "C80.1 Adenocarcinoma" }),
    ).not.toBeInTheDocument();
    expect(chipTexts()).toEqual(["C80.1 Adenocarcinoma"]);
  });

  it("says when nothing matches, quoting the text (P5)", async () => {
    fetchFromOpenElisServer.mockImplementation((url) =>
      url === SEARCH ? answer([adeno]) : answer([]),
    );
    renderPicker();
    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());

    await userEvent.type(box(), "zzz");

    expect(
      await screen.findByText('No diagnosis matches "zzz".'),
    ).toBeInTheDocument();
    expect(box()).toBeEnabled();
  });

  it("says when every match is already a chip, quoting the text, and not that nothing matches (P5)", async () => {
    fetchFromOpenElisServer.mockImplementation((url) =>
      url === SEARCH ? answer([adeno, benign]) : answer([adeno]),
    );
    renderPicker({ initial: [adeno] });
    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());

    await userEvent.type(box(), "aden");

    const status = await screen.findByRole("status");
    await waitFor(() =>
      expect(status).toHaveTextContent(
        'Every diagnosis that matches "aden" is already on this case.',
      ),
    );
    expect(screen.queryByText(/No diagnosis matches/)).not.toBeInTheDocument();
    expect(chipTexts()).toHaveLength(1);
  });

  it("says when the list could not be loaded, and keeps the chips (P6)", async () => {
    fetchFromOpenElisServer.mockImplementation(() =>
      Promise.reject(new Error("Request failed (500)")),
    );
    renderPicker({ initial: [adeno] });

    expect(
      await screen.findByText("The diagnosis list could not be loaded."),
    ).toBeInTheDocument();
    expect(chipTexts()).toEqual(["C80.1 Adenocarcinoma"]);
    expect(
      screen.getByRole("button", { name: "Remove C80.1 Adenocarcinoma" }),
    ).toBeEnabled();
  });

  it("a pick adds a chip, clears the box and keeps focus in it (P7)", async () => {
    fetchFromOpenElisServer.mockImplementation(() =>
      answer([adeno, squamous, benign]),
    );
    const onChangeSpy = vi.fn();
    renderPicker({ initial: [adeno], onChangeSpy });
    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());

    await userEvent.type(box(), "squ");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "C44.9 Squamous cell carcinoma",
      }),
    );

    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith([adeno, squamous]);
    expect(chipTexts()).toEqual([
      "C80.1 Adenocarcinoma",
      "C44.9 Squamous cell carcinoma",
    ]);
    await waitFor(() => expect(box()).toHaveValue(""));
    expect(box()).toHaveFocus();
  });

  it("removing a chip moves focus to the next chip or the box (P8)", async () => {
    fetchFromOpenElisServer.mockImplementation(() => answer([benign]));
    const onChangeSpy = vi.fn();
    renderPicker({ initial: [adeno, squamous], onChangeSpy });
    await waitFor(() => expect(fetchFromOpenElisServer).toHaveBeenCalled());

    await userEvent.click(
      screen.getByRole("button", { name: "Remove C80.1 Adenocarcinoma" }),
    );
    expect(onChangeSpy).toHaveBeenLastCalledWith([squamous]);
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: "Remove C44.9 Squamous cell carcinoma",
        }),
      ).toHaveFocus(),
    );

    await userEvent.click(
      screen.getByRole("button", {
        name: "Remove C44.9 Squamous cell carcinoma",
      }),
    );
    expect(onChangeSpy).toHaveBeenLastCalledWith([]);
    await waitFor(() => expect(box()).toHaveFocus());
  });

  it("read-only shows plain tags and asks nothing (P9)", async () => {
    fetchFromOpenElisServer.mockImplementation(() => answer([benign]));
    renderPicker({ initial: [adeno, benign], readOnly: true });

    await pause(300);
    expect(fetchFromOpenElisServer).not.toHaveBeenCalled();
    expect(chipTexts()).toEqual(["C80.1 Adenocarcinoma", "Benign"]);
    expect(
      within(screen.getByTestId("findings-conclusion-chips")).queryAllByRole(
        "button",
      ),
    ).toHaveLength(0);
    expect(box()).toBeDisabled();
  });

  it("the add-diagnosis control is a new-tab link for an administrator and a disabled button with its reason for anyone else on an editable picker, and absent when read-only (P10, P11)", async () => {
    fetchFromOpenElisServer.mockImplementation(() => answer([adeno]));
    const reasonText =
      "Only an administrator adds a diagnosis to the dictionary.";

    for (const canAddDiagnosis of [true, false]) {
      const locked = renderPicker({ canAddDiagnosis, readOnly: true });
      expect(
        screen.queryByTestId("findings-add-diagnosis"),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("Add a diagnosis to the dictionary"),
      ).not.toBeInTheDocument();
      locked.unmount();
    }

    const admin = renderPicker({ canAddDiagnosis: true });
    const place = screen.getByTestId("findings-add-diagnosis");
    const link = within(place).getByRole("link", {
      name: /Add a diagnosis to the dictionary/,
    });
    expect(link.getAttribute("href")).toMatch(
      /\/MasterListsPage\/DictionaryMenu$/,
    );
    expect(link).toHaveAttribute("target", "_blank");
    expect(link.getAttribute("rel")).toContain("noopener");
    expect(link.getAttribute("rel")).toContain("noreferrer");
    expect(screen.queryByText(reasonText)).not.toBeInTheDocument();
    admin.unmount();

    const other = renderPicker({ canAddDiagnosis: false });
    const otherPlace = screen.getByTestId("findings-add-diagnosis");
    expect(within(otherPlace).queryByRole("link")).not.toBeInTheDocument();
    const button = within(otherPlace).getByRole("button", {
      name: "Add a diagnosis to the dictionary",
    });
    expect(button).toBeDisabled();
    const reason = within(otherPlace).getByText(reasonText);
    expect(reason).toBeVisible();
    expect(button).toHaveAttribute("aria-describedby", reason.id);
    other.unmount();
  });

  it("while the dictionary is empty it asks again when the window has focus back, and not once it holds an entry (P2)", async () => {
    fetchFromOpenElisServer
      .mockImplementationOnce(() => answer([]))
      .mockImplementation(() => answer([adeno]));
    renderPicker();
    await waitFor(() => expect(box()).toBeDisabled());
    expect(queriesAsked()).toEqual([""]);
    await pause(50);

    act(() => {
      window.dispatchEvent(new FocusEvent("focus"));
    });

    await waitFor(() => expect(queriesAsked()).toEqual(["", ""]));
    await waitFor(() => expect(box()).toBeEnabled());
    expect(queriesAsked()).toEqual(["", ""]);

    await pause(50);
    act(() => {
      window.dispatchEvent(new FocusEvent("focus"));
    });
    await pause(50);
    expect(queriesAsked()).toEqual(["", ""]);
  });

  it("asks again for the same text after a failed request (P6)", async () => {
    let failOnce = true;
    fetchFromOpenElisServer.mockImplementation((url) => {
      if (url.endsWith("ade") && failOnce) {
        failOnce = false;
        return Promise.reject(new Error("Request failed (500)"));
      }
      return answer([adeno]);
    });
    renderPicker();
    await waitFor(() => expect(queriesAsked()).toEqual([""]));

    await userEvent.type(box(), "ade");
    expect(
      await screen.findByText("The diagnosis list could not be loaded."),
    ).toBeInTheDocument();

    await userEvent.type(box(), "x{Backspace}");

    await waitFor(() => expect(queriesAsked()).toEqual(["", "ade", "ade"]));
    await waitFor(() =>
      expect(
        screen.queryByText("The diagnosis list could not be loaded."),
      ).not.toBeInTheDocument(),
    );
  });
});
