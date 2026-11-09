import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  clearMethodChoices,
  methodOptions,
  useMethodChoices,
} from "./methodChoices";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const ALL = [
  { id: "41", value: "STAIN" },
  { id: "42", value: "CULTURE" },
  { id: "48", value: "AUTO" },
];

const Choices = ({ testId }: { testId?: string }) => {
  const choices = useMethodChoices(testId);
  return (
    <div data-testid="choices">
      {choices ? choices.map((m) => m.value).join(",") : "loading"}
    </div>
  );
};

describe("a result's Method list (OGC-1614)", () => {
  beforeEach(() => {
    clearMethodChoices();
    vi.mocked(getFromOpenElisServer).mockReset();
  });

  it("loads the test's own methods once per test", async () => {
    vi.mocked(getFromOpenElisServer).mockImplementation(
      (url: string, callback: (r: unknown) => void) =>
        callback({ methods: [{ id: "48", value: "AUTO" }] }),
    );

    const first = render(<Choices testId="4" />);
    await waitFor(() =>
      expect(screen.getByTestId("choices").textContent).toBe("AUTO"),
    );
    first.unmount();
    render(<Choices testId="4" />);

    expect(screen.getByTestId("choices").textContent).toBe("AUTO");
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(vi.mocked(getFromOpenElisServer).mock.calls[0][0]).toBe(
      "/rest/methods-for-test/4",
    );
  });

  it("offers the test's choices, and keeps a method the row already records", () => {
    const linked = [{ id: "48", value: "AUTO" }];

    expect(methodOptions(linked, ALL, "")).toEqual(linked);
    expect(methodOptions(linked, ALL, "48")).toEqual(linked);
    expect(methodOptions(linked, ALL, "42")).toEqual([
      { id: "42", value: "CULTURE" },
      ...linked,
    ]);
    expect(methodOptions(null, ALL, "")).toEqual(ALL);
  });
});
