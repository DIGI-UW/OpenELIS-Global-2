import type { MockInstance } from "vitest";

declare global {
  const afterEach: typeof import("vitest").afterEach;
  const beforeEach: typeof import("vitest").beforeEach;
  const describe: typeof import("vitest").describe;
  const expect: typeof import("vitest").expect;
  const it: typeof import("vitest").it;
  const test: typeof import("vitest").test;
  const vi: typeof import("vitest").vi;

  interface HTMLElement {
    checked?: boolean;
    disabled?: boolean;
    value?: string;
  }
}

export type MockedFunction<T extends (...args: never[]) => unknown> =
  MockInstance<T> & T;
