import React from "react";
import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));
// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

import Avatar, {
  avatarColor,
  avatarInitials,
  clearAvatarPhotoCache,
} from "./Avatar";

/**
 * OGC-811 gallery parity — patient avatar on clinical rows: photo when the
 * patient has one, initials chip fallback otherwise (same contract as the
 * legacy pages' AsyncAvatar).
 */
const respondWithPhoto = (data: string) =>
  getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
    if (typeof url !== "string" || typeof cb !== "function") {
      return;
    }
    if (url.includes("/rest/patient-photos/")) {
      cb({ data });
    }
  });

describe("Avatar", () => {
  beforeEach(() => {
    getMock.mockReset();
    clearAvatarPhotoCache();
  });

  it("derives initials from comma-separated name parts", () => {
    expect(avatarInitials("Doe, John")).toBe("DJ");
    expect(avatarInitials("Sarah , Sarah")).toBe("SS");
    expect(avatarInitials("Single")).toBe("S");
  });

  it("color is deterministic for the same seed and from the fixed palette", () => {
    expect(avatarColor("42")).toBe(avatarColor("42"));
    expect(avatarColor("42")).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it("shows the patient photo when a thumbnail exists", () => {
    respondWithPhoto("AAAA");
    render(<Avatar name="Doe, John" id="42" />);
    const photo = screen.getByTestId("row-avatar-photo");
    expect(photo).toHaveAttribute("src", "data:image/jpeg;base64,AAAA");
    expect(getMock).toHaveBeenCalledWith(
      "/rest/patient-photos/42/true",
      expect.any(Function),
    );
  });

  it("falls back to the initials chip when the patient has no photo", () => {
    respondWithPhoto("");
    render(<Avatar name="Doe, John" id="42" />);
    const chip = screen.getByTestId("row-avatar");
    expect(chip).toHaveTextContent("DJ");
    expect(chip).toHaveStyle({ background: avatarColor("42") });
    expect(screen.queryByTestId("row-avatar-photo")).not.toBeInTheDocument();
  });

  it("renders initials without fetching when there is no patient id", () => {
    render(<Avatar name="Doe, John" />);
    expect(screen.getByTestId("row-avatar")).toHaveTextContent("DJ");
    expect(getMock).not.toHaveBeenCalledWith(
      expect.stringContaining("/rest/patient-photos/"),
      expect.any(Function),
    );
  });

  it("fetches a patient's thumbnail once across many rows (cache)", () => {
    respondWithPhoto("AAAA");
    render(
      <>
        <Avatar name="Doe, John" id="42" />
        <Avatar name="Doe, John" id="42" />
        <Avatar name="Doe, John" id="42" />
      </>,
    );
    const photoCalls = getMock.mock.calls.filter(
      (call) =>
        typeof call[0] === "string" &&
        call[0].includes("/rest/patient-photos/"),
    );
    expect(photoCalls.length).toBe(1);
    expect(screen.getAllByTestId("row-avatar-photo").length).toBe(3);
  });

  it("renders nothing without a name", () => {
    const { container } = render(<Avatar id="42" />);
    expect(container.firstChild).toBeNull();
  });
});
