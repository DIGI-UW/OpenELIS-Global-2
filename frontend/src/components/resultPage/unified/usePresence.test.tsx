import React from "react";
import { act, cleanup, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: { postToOpenElisServerJsonResponse: vi.fn() },
}));

vi.mock("../../utils/Utils", () => utilsMock);

import { usePresence } from "./usePresence";

const Presence = ({ editing }: { editing: string | null }) => {
  const presence = usePresence(editing, ["61", "62"]);
  return <div data-testid="presence">{JSON.stringify(presence)}</div>;
};

describe("usePresence", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
  });

  it("keeps the newest answer when an older one arrives after it", () => {
    const answers: Array<(response: unknown) => void> = [];
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (response: unknown) => void) =>
        answers.push(callback),
    );
    const { rerender } = render(<Presence editing={null} />);
    const older = answers[answers.length - 1];

    rerender(<Presence editing="61" />);
    const newer = answers[answers.length - 1];
    act(() => newer({ "62": "Bob" }));
    act(() => older({ "61": "Alice" }));

    expect(screen.getByTestId("presence")).toHaveTextContent('{"62":"Bob"}');
  });
});
