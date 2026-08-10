import React from "react";
import { render, screen } from "@testing-library/react";
import Avatar, { avatarColor, avatarInitials } from "./Avatar";

/** OGC-811 gallery parity — patient initials chip on clinical rows. */
describe("Avatar", () => {
  it("derives initials from comma-separated name parts", () => {
    expect(avatarInitials("Doe, John")).toBe("DJ");
    expect(avatarInitials("Sarah , Sarah")).toBe("SS");
    expect(avatarInitials("Single")).toBe("S");
  });

  it("color is deterministic for the same seed and from the fixed palette", () => {
    expect(avatarColor("42")).toBe(avatarColor("42"));
    expect(avatarColor("42")).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it("renders the chip with initials, seeded by id", () => {
    render(<Avatar name="Doe, John" id="42" />);
    const chip = screen.getByTestId("row-avatar");
    expect(chip).toHaveTextContent("DJ");
    expect(chip).toHaveStyle({ background: avatarColor("42") });
  });

  it("renders nothing without a name", () => {
    const { container } = render(<Avatar id="42" />);
    expect(container.firstChild).toBeNull();
  });
});
