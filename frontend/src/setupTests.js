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
