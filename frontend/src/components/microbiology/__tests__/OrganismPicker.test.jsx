import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import OrganismPicker, { OrganismName } from "../OrganismPicker";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer: vi.fn() }));

const klebsiella = {
  id: "k",
  label: "Klebsiella",
  code: "kl-",
  whonetName: "Klebsiella sp.",
};
const ecoli = {
  id: "e",
  label: "Escherichia coli",
  code: "eco",
  whonetName: "Escherichia coli",
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
      callback(query.get("q") === "kleb" ? [klebsiella] : [ecoli]);
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
  fireEvent.click(
    await screen.findByRole("option", {
      name: "Klebsiella (Klebsiella sp., kl-)",
    }),
  );
  expect(getFromOpenElisServer).toHaveBeenCalledWith(
    `${URL}?q=kleb`,
    expect.any(Function),
  );
  expect(onChange).toHaveBeenCalledWith("k");
  expect(screen.getByRole("combobox", { name: "Organism" })).toHaveValue(
    "Klebsiella (Klebsiella sp., kl-)",
  );
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
  expect(await screen.findByText("Klebsiella")).toBeVisible();
  rerender(<OrganismName id="" fallback="Not available" />);
  expect(await screen.findByText("Not available")).toBeVisible();
});
