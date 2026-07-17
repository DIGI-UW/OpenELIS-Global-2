// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Explicit DOM cleanup between tests. @testing-library/react's auto-cleanup
// relies on process.env.VITEST being set; registering afterEach(cleanup)
// here guarantees isolation across tests that render multiple components
// (e.g. alert/notification flows where stale alerts would violate
// getByRole("alert") singleton expectations).
afterEach(() => {
  cleanup();
});

// Mock window.scrollTo since jsdom doesn't implement it
Object.defineProperty(window, "scrollTo", {
  value: vi.fn(),
  writable: true,
});

// jsdom does not implement pseudo-element computed styles. Some accessibility
// queries (via dom-accessibility-api) call getComputedStyle with a pseudoElement
// argument. Ignore the pseudoElement to prevent noisy test failures.
const originalGetComputedStyle = window.getComputedStyle;
window.getComputedStyle = (element, pseudoElement) => {
  if (pseudoElement) {
    return originalGetComputedStyle(element);
  }
  return originalGetComputedStyle(element);
};

// Polyfill TextEncoder/TextDecoder for jsdom (required by jspdf 4.x)
import { TextEncoder, TextDecoder } from "util";
if (!global.TextEncoder) global.TextEncoder = TextEncoder;
if (!global.TextDecoder) global.TextDecoder = TextDecoder;

// Polyfill fetch for jsdom (Jest 29 jsdom doesn't include it)
if (!global.fetch) {
  global.fetch = vi.fn(() =>
    Promise.resolve({ ok: true, json: () => Promise.resolve({}) }),
  );
}

// Mock window.matchMedia for Carbon v11+ responsive breakpoints (jsdom doesn't implement it)
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock Element.scrollIntoView for Carbon Dropdown/ComboBox (jsdom doesn't implement it)
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = vi.fn();
}

// Mock ResizeObserver for Carbon components and other UI elements
global.ResizeObserver = class ResizeObserver {
  constructor(callback) {
    this.callback = callback;
  }
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Mock MessageChannel for react-idle-timer (used in SecureRoute)
global.MessageChannel = class MessageChannel {
  constructor() {
    this.port1 = {
      postMessage: vi.fn(),
      start: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      onmessage: null,
    };
    this.port2 = {
      postMessage: vi.fn(),
      start: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      onmessage: null,
    };
  }
};

// Vitest React 17 Polyfill (handles missing jsx-runtime in non-TSX test files)
import React from "react";
global.React = React;

// Vitest Flatpickr Isolation (bypasses JSDOM dynamic date parse crashes)
vi.mock("flatpickr", () => {
  return {
    default: vi.fn().mockImplementation(() => ({
      destroy: vi.fn(),
      setDate: vi.fn(),
      set: vi.fn(),
      clear: vi.fn(),
      parseDate: vi.fn(),
      formatDate: vi.fn(),
      redraw: vi.fn(),
    })),
  };
});
