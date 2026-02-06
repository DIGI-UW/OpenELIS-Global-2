// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom/extend-expect";

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
