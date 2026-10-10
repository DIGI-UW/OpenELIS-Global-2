import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CaseInoculationPanel from "../CaseInoculationPanel";
import messages from "../../../languages/en.json";

const renderPanel = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseInoculationPanel
        onRecord={vi.fn().mockResolvedValue({})}
        {...props}
      />
    </IntlProvider>,
  );

describe("CaseInoculationPanel", () => {
  it("routes primary setup, moves focus into it, and restores focus after cancel", async () => {
    const user = userEvent.setup();
    const onInoculationAction = vi.fn();
    const { rerender } = renderPanel({ onInoculationAction });

    const trigger = screen.getByRole("button", { name: "Start inoculation" });
    trigger.focus();
    await user.keyboard("{Enter}");

    expect(onInoculationAction).toHaveBeenCalledWith("start-inoculation");
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel
          action="start-inoculation"
          onRecord={vi.fn().mockResolvedValue({})}
          onInoculationAction={onInoculationAction}
        />
      </IntlProvider>,
    );

    expect(screen.getByLabelText("Bottle or plate ID")).toHaveFocus();
    expect(screen.getByText("Inoculation form expanded")).toHaveAttribute(
      "role",
      "status",
    );
    await user.keyboard("{Escape}");
    expect(onInoculationAction).toHaveBeenLastCalledWith("");
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel
          onRecord={vi.fn().mockResolvedValue({})}
          onInoculationAction={onInoculationAction}
        />
      </IntlProvider>,
    );
    expect(trigger).toHaveFocus();
  });

  it("keeps subculture unavailable until parent media exists", async () => {
    const user = userEvent.setup();
    const onInoculationAction = vi.fn();
    const { rerender } = renderPanel({ onInoculationAction });

    expect(
      screen.getByText("No media recorded yet. Start inoculation to begin."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Add subculture" }),
    ).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Start inoculation" }));
    expect(onInoculationAction).toHaveBeenCalledWith("start-inoculation");
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel
          action="start-inoculation"
          onRecord={vi.fn().mockResolvedValue({})}
          onInoculationAction={onInoculationAction}
        />
      </IntlProvider>,
    );
    expect(screen.getByLabelText("Bottle or plate ID")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save media" })).toBeDisabled();
  });

  it("records a subculture with its selected parent", async () => {
    const user = userEvent.setup();
    const onRecord = vi.fn().mockResolvedValue({});
    const onInoculationAction = vi.fn();
    const panelProps = {
      inoculations: [
        {
          id: "inoculation-1",
          containerIdentifier: "BOTTLE-001",
          media: "Blood agar",
          incubation: "24h",
        },
      ],
      onRecord,
      onInoculationAction,
    };
    const { rerender } = renderPanel(panelProps);

    const trigger = screen.getByRole("button", { name: "Add subculture" });
    trigger.focus();
    await user.keyboard("{Enter}");
    expect(onInoculationAction).toHaveBeenCalledWith("add-subculture");
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel {...panelProps} action="add-subculture" />
      </IntlProvider>,
    );

    expect(screen.getByLabelText("Parent media")).toHaveFocus();
    expect(screen.getByText("Subculture form expanded")).toHaveAttribute(
      "role",
      "status",
    );
    await user.selectOptions(
      screen.getByLabelText("Parent media"),
      "inoculation-1",
    );
    await user.type(screen.getByLabelText("Bottle or plate ID"), "PLATE-002");
    await user.type(screen.getByLabelText("Media or bottle"), "MacConkey agar");
    await user.click(screen.getByRole("button", { name: "Save media" }));

    expect(onRecord).toHaveBeenCalledWith({
      sourceInoculationId: "inoculation-1",
      containerIdentifier: "PLATE-002",
      media: "MacConkey agar",
      incubation: "",
      atmosphere: "",
      lotSelections: [],
    });
    expect(onInoculationAction).toHaveBeenLastCalledWith("");
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel {...panelProps} />
      </IntlProvider>,
    );
    expect(
      screen.getByRole("button", { name: "Add subculture" }),
    ).toHaveFocus();
  });

  it("offers culture outcome actions only while the case is incubating", async () => {
    const user = userEvent.setup();
    const onCultureAction = vi.fn();
    const { rerender } = renderPanel({
      stage: "INCUBATING",
      inoculations: [
        {
          id: "inoculation-1",
          containerIdentifier: "BOTTLE-001",
          media: "Blood culture bottle",
        },
      ],
      onCultureAction,
    });

    await user.click(screen.getByRole("button", { name: "Mark positive" }));
    await user.click(screen.getByRole("button", { name: "Mark no growth" }));

    expect(onCultureAction).toHaveBeenNthCalledWith(1, "mark-positive");
    expect(onCultureAction).toHaveBeenNthCalledWith(2, "mark-no-growth");

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <CaseInoculationPanel
          stage="POSITIVE_SIGNAL"
          inoculations={[
            {
              id: "inoculation-1",
              containerIdentifier: "BOTTLE-001",
              media: "Blood culture bottle",
            },
          ]}
          onRecord={vi.fn().mockResolvedValue({})}
          onCultureAction={onCultureAction}
        />
      </IntlProvider>,
    );

    expect(
      screen.queryByRole("button", { name: "Mark positive" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Mark no growth" }),
    ).not.toBeInTheDocument();
  });
});
