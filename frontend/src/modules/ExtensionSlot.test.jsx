import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ExtensionSlot from "./ExtensionSlot";
import UserSessionDetailsContext from "../UserSessionDetailsContext";

const hostValue = { slots: {} };

vi.mock("./ModuleHostProvider", () => ({
  useModuleHost: () => hostValue,
}));

vi.mock("./LazyRemote", () => ({
  default: ({ moduleId, expose, slot, patientId }) => (
    <div data-testid={`remote-${moduleId}-${expose}`}>
      {slot} for {patientId}
    </div>
  ),
}));

function mount(permissions, extra = {}) {
  return render(
    <UserSessionDetailsContext.Provider
      value={{
        userSessionDetails: { authenticated: true, permissions, roles: [] },
      }}
    >
      <ExtensionSlot
        name="patient.header.actions"
        context={{ patientId: "83" }}
        {...extra}
      />
    </UserSessionDetailsContext.Provider>,
  );
}

describe("ExtensionSlot", () => {
  it("renders every contribution the user may see, in order, with the slot context", () => {
    hostValue.slots = {
      "patient.header.actions": [
        {
          moduleId: "beta",
          remoteName: "beta",
          expose: "./Later",
          order: 50,
          permission: null,
        },
        {
          moduleId: "alpha",
          remoteName: "alpha",
          expose: "./Card",
          order: 10,
          permission: "mod.alpha.view",
        },
        {
          moduleId: "gamma",
          remoteName: "gamma",
          expose: "./Hidden",
          order: 20,
          permission: "mod.gamma.view",
        },
      ],
    };
    const { container } = mount(["mod.alpha.view"]);

    expect(screen.getByTestId("remote-alpha-./Card")).toHaveTextContent(
      "patient.header.actions for 83",
    );
    expect(screen.getByTestId("remote-beta-./Later")).toBeInTheDocument();
    expect(screen.queryByTestId("remote-gamma-./Hidden")).toBeNull();
    expect(container.textContent.indexOf("for 83")).toBeGreaterThanOrEqual(0);
  });

  it("renders nothing when no module contributes to the slot", () => {
    hostValue.slots = {};
    const { container } = mount({});

    expect(container.innerHTML).toBe("");
  });

  it("wraps the contributions when the page asks for it", () => {
    hostValue.slots = {
      "patient.header.actions": [
        { moduleId: "alpha", remoteName: "alpha", expose: "./Card", order: 1 },
      ],
    };
    mount(
      {},
      {
        wrapper: ({ children }) => <ul data-testid="slot-list">{children}</ul>,
      },
    );

    expect(screen.getByTestId("slot-list")).toContainElement(
      screen.getByTestId("remote-alpha-./Card"),
    );
  });
});
