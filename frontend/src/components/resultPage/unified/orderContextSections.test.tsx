/**
 * useOrderContext fetches the order once per accession. A panel that closes
 * before the order arrives (the validator released the row) must not be
 * updated afterwards.
 */
import React from "react";
import { vi } from "vitest";
import { act, render } from "@testing-library/react";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));
// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../utils/Utils";
// eslint-disable-next-line import/first
import { useOrderContext } from "./orderContextSections";

const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

const Probe: React.FC<{ accession: string }> = ({ accession }) => {
  const order = useOrderContext(accession);
  return <span data-testid="loaded">{String(order.loaded)}</span>;
};

describe("useOrderContext", () => {
  it("fills in the order when it arrives", () => {
    getMock.mockReset();
    const { getByTestId } = render(<Probe accession="ACC1" />);
    expect(getByTestId("loaded").textContent).toBe("false");

    const [, callback] = getMock.mock.calls[0];
    act(() => {
      callback({ sampleOrderItems: {} });
    });

    expect(getByTestId("loaded").textContent).toBe("true");
  });

  it("ignores an order that arrives after the panel closed", () => {
    getMock.mockReset();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<Probe accession="ACC2" />);
    const [, callback] = getMock.mock.calls[0];
    unmount();

    callback({ sampleOrderItems: {} });

    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes("unmounted component"),
      ),
    ).toBe(false);
    errors.mockRestore();
  });
});
