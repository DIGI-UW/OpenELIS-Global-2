import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import ContainerTypeField from "./ContainerTypeField";
import messages from "../../../../languages/en.json";

const SST = { id: 11, name: "SST 5 mL", capColourHex: "#FFD700" };
const CITRATE = { id: 12, name: "Citrate 2.7 mL", capColourHex: "#87CEEB" };

const renderField = (sample, onChange = vi.fn()) => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <ContainerTypeField
        sample={{ containerChoices: [SST, CITRATE], ...sample }}
        sampleIndex={0}
        isReadOnly={false}
        onChange={onChange}
      />
    </IntlProvider>,
  );
  return onChange;
};

describe("ContainerTypeField (OGC-1611 FR-B24, B26, G8)", () => {
  it("lists the containers yielding the sample's type and reports a choice", () => {
    const onChange = renderField({ containerTypeId: "" });

    const select = screen.getByLabelText("Container");
    expect(
      [...select.querySelectorAll("option")].map((option) => option.text),
    ).toEqual(["Choose a container", "SST 5 mL", "Citrate 2.7 mL"]);
    fireEvent.change(select, { target: { value: "12" } });
    expect(onChange).toHaveBeenCalledWith("12");
  });

  it("marks the container required and says why when the catalog names containers", () => {
    renderField({ containerTypeId: "", containerRequired: true });

    expect(screen.getByLabelText("Container *")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Choose a container. The catalog names the containers for the tests on this sample.",
      ),
    ).toBeInTheDocument();
  });

  it("does not require a container the catalog does not ask for", () => {
    renderField({ containerTypeId: "", containerRequired: false });

    expect(screen.getByLabelText("Container")).toBeInTheDocument();
    expect(
      screen.queryByText(/The catalog names the containers/),
    ).not.toBeInTheDocument();
  });

  it("tags a proposed sample with the tests it was proposed for and the count to draw", () => {
    renderField({
      containerTypeId: "12",
      proposed: true,
      proposedFor: ["PT/INR", "APTT"],
      proposedCount: 2,
    });

    expect(screen.getByTestId("container-proposed-0")).toHaveAttribute(
      "aria-label",
      "Proposed for PT/INR, APTT",
    );
    expect(screen.getByTestId("container-draw-0")).toHaveTextContent("Draw 2");
    expect(screen.getByTestId("container-swatch-0")).toHaveStyle({
      backgroundColor: "#87CEEB",
    });
    expect(screen.queryByTestId("container-edited-0")).not.toBeInTheDocument();
  });

  it("tags an edited sample Edited instead of Proposed until it is saved", () => {
    renderField({
      containerTypeId: "11",
      proposed: true,
      proposedCount: 2,
      containerEdited: true,
    });

    expect(screen.getByTestId("container-edited-0")).toHaveTextContent(
      "Edited",
    );
    expect(
      screen.queryByTestId("container-proposed-0"),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId("container-draw-0")).not.toBeInTheDocument();
  });

  it("shows no proposal tags on a saved sample", () => {
    renderField({
      sampleItemId: "55",
      containerTypeId: "11",
      proposed: true,
      containerEdited: true,
    });

    expect(
      screen.queryByTestId("container-proposed-0"),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId("container-edited-0")).not.toBeInTheDocument();
  });

  it("keeps a saved sample's retired container, named as no longer in use", () => {
    renderField({
      sampleItemId: "55",
      containerTypeId: "40",
      containerTypeName: "Old tube",
    });

    const select = screen.getByLabelText("Container");
    expect(select).toHaveValue("40");
    expect(
      screen.getByRole("option", { name: "Old tube (no longer in use)" }),
    ).toBeInTheDocument();
  });
});
