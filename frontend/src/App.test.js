/* eslint-disable no-redeclare */
/* eslint-env jest */
// Or individual globals:
/* global test, expect */

import { render, screen } from "@testing-library/react";
import App from "./App";

test("renders learn react link", () => {
  render(<App />);
  const linkElement = screen.getByText(/learn react/i);
  expect(linkElement).toBeInTheDocument();
});
