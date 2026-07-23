/**
 * Tests for useLatestCallback — a ref-based hook that always returns the
 * most recent callback without changing its own identity across renders.
 *
 * This is the fix for effects whose dependency array would otherwise
 * include an unstable `onChange`-style prop: including it causes the
 * effect to re-fire every time the parent re-renders (because inline
 * arrow functions get a fresh identity), which in turn can cause render
 * loops when the effect's side-effect itself triggers a parent re-render.
 */

import React from "react";
import { render } from "@testing-library/react";
import useLatestCallback from "./useLatestCallback";

describe("useLatestCallback", () => {
  it("returns a ref that carries the most recent callback on each render", () => {
    let currentRef = null;
    function Probe({ cb }) {
      currentRef = useLatestCallback(cb);
      return null;
    }
    const first = vi.fn();
    const second = vi.fn();
    const { rerender } = render(<Probe cb={first} />);
    currentRef.current();
    expect(first).toHaveBeenCalledTimes(1);
    expect(second).toHaveBeenCalledTimes(0);

    rerender(<Probe cb={second} />);
    currentRef.current();
    expect(first).toHaveBeenCalledTimes(1);
    expect(second).toHaveBeenCalledTimes(1);
  });

  it("returns the same ref object across renders (stable identity)", () => {
    const refs = [];
    function Probe({ cb }) {
      refs.push(useLatestCallback(cb));
      return null;
    }
    const { rerender } = render(<Probe cb={() => {}} />);
    rerender(<Probe cb={() => {}} />);
    rerender(<Probe cb={() => {}} />);
    expect(refs).toHaveLength(3);
    expect(refs[0]).toBe(refs[1]);
    expect(refs[1]).toBe(refs[2]);
  });

  it("tolerates a missing callback (undefined)", () => {
    let currentRef = null;
    function Probe({ cb }) {
      currentRef = useLatestCallback(cb);
      return null;
    }
    render(<Probe cb={undefined} />);
    // Calling when undefined should be a no-op, not a throw.
    expect(() => {
      if (currentRef.current) currentRef.current();
    }).not.toThrow();
  });
});
