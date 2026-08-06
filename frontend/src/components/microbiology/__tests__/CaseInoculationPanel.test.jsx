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
  it("keeps subculture unavailable until parent media exists", async () => {
    const user = userEvent.setup();
    renderPanel();

    expect(
      screen.getByText("No media recorded yet. Start inoculation to begin."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Add subculture" }),
    ).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Start inoculation" }));
    expect(screen.getByLabelText("Bottle or plate ID")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save media" })).toBeDisabled();
  });

  it("records a subculture with its selected parent", async () => {
    const user = userEvent.setup();
    const onRecord = vi.fn().mockResolvedValue({});
    renderPanel({
      inoculations: [
        {
          id: "inoculation-1",
          containerIdentifier: "BOTTLE-001",
          media: "Blood agar",
          incubation: "24h",
        },
      ],
      onRecord,
    });

    await user.click(screen.getByRole("button", { name: "Add subculture" }));
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
  });
});
