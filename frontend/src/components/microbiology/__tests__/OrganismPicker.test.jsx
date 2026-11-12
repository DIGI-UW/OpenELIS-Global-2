import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import OrganismPicker, { OrganismName } from "../OrganismPicker";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer: vi.fn() }));

const klebsiella = { id: "k", label: "Klebsiella sp.", code: "kl-" };
const ecoli = { id: "e", label: "Escherichia coli", code: "eco" };
const kluyveromyces = {
  id: "m",
  label: "Kluyveromyces marxianus",
  code: "kma",
  formerName: "Candida kefyr",
};
const URL = "/rest/microbiology/reference/organisms";

const show = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    const query = new URLSearchParams(url.split("?")[1]);
    if (query.has("id")) {
      callback([klebsiella, ecoli].filter((o) => o.id === query.get("id")));
    } else {
      const typed = query.get("q");
      callback(
        typed === "kleb"
          ? [klebsiella]
          : typed === "kefyr"
            ? [kluyveromyces]
            : [ecoli],
      );
    }
  });
});

it("offers the server's matches to what was typed and reports the organism chosen", async () => {
  const onChange = vi.fn();
  show(
    <OrganismPicker
      id="picker"
      titleText="Organism"
      value=""
      onChange={onChange}
    />,
  );
  fireEvent.change(screen.getByRole("combobox", { name: "Organism" }), {
    target: { value: "kleb" },
  });
  const row = await screen.findByRole("option", { name: /Klebsiella sp\./ });
  expect(row).toHaveTextContent("kl-");
  fireEvent.click(row);
  expect(getFromOpenElisServer).toHaveBeenCalledWith(
    `${URL}?q=kleb`,
    expect.any(Function),
  );
  expect(onChange).toHaveBeenCalledWith("k");
  expect(screen.getByRole("combobox", { name: "Organism" })).toHaveValue(
    "Klebsiella sp. (kl-)",
  );
});

it("says which older name found an organism", async () => {
  show(
    <OrganismPicker
      id="picker"
      titleText="Organism"
      value=""
      onChange={vi.fn()}
    />,
  );
  fireEvent.change(screen.getByRole("combobox", { name: "Organism" }), {
    target: { value: "kefyr" },
  });
  expect(
    await screen.findByRole("option", { name: /Kluyveromyces marxianus/ }),
  ).toHaveTextContent("Formerly Candida kefyr");
});

it("withdraws the earlier rows as soon as the text changes", async () => {
  show(
    <OrganismPicker
      id="picker"
      titleText="Organism"
      value=""
      onChange={vi.fn()}
    />,
  );
  const input = screen.getByRole("combobox", { name: "Organism" });
  fireEvent.click(input);
  expect(
    await screen.findByRole("option", { name: /Escherichia coli/ }),
  ).toBeVisible();
  fireEvent.change(input, { target: { value: "kleb" } });
  expect(screen.queryByRole("option", { name: /Escherichia coli/ })).toBeNull();
  expect(
    await screen.findByRole("option", { name: /Klebsiella sp\./ }),
  ).toBeVisible();
});

it("offers the common organisms again when the text is erased", async () => {
  show(
    <OrganismPicker
      id="picker"
      titleText="Organism"
      value=""
      onChange={vi.fn()}
    />,
  );
  const input = screen.getByRole("combobox", { name: "Organism" });
  fireEvent.change(input, { target: { value: "kleb" } });
  expect(
    await screen.findByRole("option", { name: /Klebsiella sp\./ }),
  ).toBeVisible();
  fireEvent.change(input, { target: { value: "" } });
  expect(
    await screen.findByRole("option", { name: /Escherichia coli/ }),
  ).toBeVisible();
  expect(screen.queryByRole("option", { name: /Klebsiella sp\./ })).toBeNull();
});

it("does not report a change when its value is cleared from outside", async () => {
  const onChange = vi.fn();
  const picker = (value) => (
    <IntlProvider locale="en" messages={messages}>
      <OrganismPicker
        id="picker"
        titleText="Organism"
        value={value}
        onChange={onChange}
      />
    </IntlProvider>
  );
  const { rerender } = render(picker("e"));
  const input = screen.getByRole("combobox", { name: "Organism" });
  await waitFor(() => expect(input).toHaveValue("Escherichia coli (eco)"));
  rerender(picker(""));
  await waitFor(() => expect(input).toHaveValue(""));
  expect(onChange).not.toHaveBeenCalled();
});

it("shows an organism it was given only by id", async () => {
  show(
    <OrganismPicker
      id="picker"
      titleText="Organism"
      value="e"
      onChange={vi.fn()}
    />,
  );
  await waitFor(() =>
    expect(screen.getByRole("combobox", { name: "Organism" })).toHaveValue(
      "Escherichia coli (eco)",
    ),
  );
  expect(getFromOpenElisServer).toHaveBeenCalledWith(
    `${URL}?id=e`,
    expect.any(Function),
  );
});

it("names an organism by id and falls back when there is none", async () => {
  const { rerender } = show(<OrganismName id="k" fallback="Not available" />);
  expect(await screen.findByText("Klebsiella sp.")).toBeVisible();
  rerender(<OrganismName id="" fallback="Not available" />);
  expect(await screen.findByText("Not available")).toBeVisible();
});
