import React, { useEffect } from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import Workplan from "../Workplan";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";

vi.mock("../WorkplanSearchForm", () => ({
  default: function SearchFormStub({ createTestsList }) {
    useEffect(() => {
      createTestsList({
        workplanTests: [
          {
            accessionNumber: "DEV0100000000000001",
            testName: "Haemoglobin",
            receivedDate: "2026-09-01",
          },
        ],
      });
    }, []);
    return null;
  },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

describe("Workplan print buttons", () => {
  it("offers Print Workplan above and below the list under distinct ids", async () => {
    const { container } = render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <Workplan type="test" />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

    const buttons = await screen.findAllByRole("button", {
      name: "Print Workplan",
    });
    expect(buttons).toHaveLength(2);
    const ids = buttons.map((b) => b.id);
    expect(ids.every(Boolean)).toBe(true);
    expect(new Set(ids).size).toBe(2);
    ids.forEach((id) =>
      expect(container.querySelectorAll(`[id="${id}"]`)).toHaveLength(1),
    );
  });
});
