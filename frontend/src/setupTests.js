// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom";

// Mock window.scrollTo since jsdom doesn't implement it
Object.defineProperty(window, "scrollTo", {
  value: jest.fn(),
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
const { TextEncoder, TextDecoder } = require("util");
if (!global.TextEncoder) global.TextEncoder = TextEncoder;
if (!global.TextDecoder) global.TextDecoder = TextDecoder;

// Polyfill fetch for jsdom (Jest 29 jsdom doesn't include it)
if (!global.fetch) {
  global.fetch = jest.fn(() =>
    Promise.resolve({ ok: true, json: () => Promise.resolve({}) }),
  );
}

// Mock window.matchMedia for Carbon v11+ responsive breakpoints (jsdom doesn't implement it)
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: jest.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock Element.scrollIntoView for Carbon Dropdown/ComboBox (jsdom doesn't implement it)
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = jest.fn();
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
      postMessage: jest.fn(),
      start: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      onmessage: null,
    };
    this.port2 = {
      postMessage: jest.fn(),
      start: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      onmessage: null,
    };
  }
};
