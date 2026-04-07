/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.js"],
  transform: {
    "^.+\\.[jt]sx?$": "babel-jest",
  },
  transformIgnorePatterns: [
    "node_modules/(?!(lodash-es|jsonpath-plus|jspdf|fflate|fast-png|iobuffer)/)",
  ],
  moduleNameMapper: {
    "\\.(css|less|scss|sass)$": "<rootDir>/src/__mocks__/styleMock.js",
    "^react-confirm-alert/src/react-confirm-alert\\.css$":
      "<rootDir>/src/__mocks__/styleMock.js",
    "\\.(png|jpg|jpeg|gif|svg|webp|ico)$":
      "<rootDir>/src/__mocks__/fileMock.js",
  },
  testMatch: [
    "<rootDir>/src/**/*.test.{js,jsx,ts,tsx}",
    "<rootDir>/src/**/__tests__/**/*.{js,jsx,ts,tsx}",
  ],
  moduleFileExtensions: ["js", "jsx", "ts", "tsx", "json"],
};
