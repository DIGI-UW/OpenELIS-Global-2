/**
 * The generic sample order's notebook picker offers "no notebook" once, as its
 * empty choice, and every notebook the server lists after it.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import GenericSampleOrder from "./GenericSampleOrder";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) => {
      if (url === "/rest/notebook/list") {
        callback([{ id: "7", title: "Water survey" }]);
      } else if (url === "/rest/uom") {
        callback([
          { id: "1", value: "mL" },
          { id: "2", value: "uL" },
        ]);
      } else if (url === "/rest/user-sample-types") {
        callback([
          { id: "2", value: "Sérum" },
          { id: "4", value: "Eau de surface" },
        ]);
      } else {
        callback([]);
      }
    }),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

describe("GenericSampleOrder — notebook picker", () => {
  it("offers 'no notebook' once as the empty choice, then the notebooks", () => {
    const { container } = render(
      <MemoryRouter>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <NotificationContext.Provider
            value={{
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <IntlProvider locale="en" messages={messages}>
              <GenericSampleOrder />
            </IntlProvider>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </MemoryRouter>,
    );

    const select = container.querySelector("#notebookSelect");
    expect(select).not.toBeNull();
    const options = Array.from(select.options).map((o) => [o.value, o.text]);
    expect(options).toEqual([
      ["", "None - default fields only"],
      ["7", "Water survey"],
    ]);

    fireEvent.change(select, { target: { value: "7" } });
    expect(select.value).toBe("7");
  });
});

const renderOrder = () =>
  render(
    <MemoryRouter>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
      >
        <NotificationContext.Provider
          value={{
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <GenericSampleOrder />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

const optionsOf = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

/**
 * The unit list comes from /rest/uom, which every signed-in user may read;
 * the admin-only /rest/UomCreate left non-admin users with no units. The
 * units and the sample types are searched by typing, ignoring case and
 * accents (OGC-1515).
 */
describe("GenericSampleOrder — sample type and unit of measure", () => {
  it("offers the units /rest/uom lists and narrows them to what is typed", async () => {
    renderOrder();
    const units = screen.getByRole("combobox", {
      name: messages["sample.uom.label"],
    });

    await userEvent.click(units);
    expect(optionsOf(units)).toEqual(["mL", "uL"]);
    await userEvent.type(units, "ML");
    expect(optionsOf(units)).toEqual(["mL"]);
    await userEvent.click(screen.getByRole("option", { name: "mL" }));
    expect(units).toHaveValue("mL");
  });

  it("narrows the sample types to what is typed, ignoring accents", async () => {
    renderOrder();
    const sampleType = screen.getByRole("combobox", {
      name: messages["sample.type"],
    });

    await userEvent.type(sampleType, "serum");
    expect(optionsOf(sampleType)).toEqual(["Sérum"]);
    await userEvent.click(screen.getByRole("option", { name: "Sérum" }));
    expect(sampleType).toHaveValue("Sérum");
  });
});
