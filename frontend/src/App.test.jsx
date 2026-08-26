import { render } from "@testing-library/react";
import App, { ANALYZER_RESULTS_ROLES } from "./App";
import { Roles } from "./components/utils/Utils";

test("renders App component without errors", () => {
  // Just verify the App component renders without throwing errors
  const { container } = render(<App />);
  expect(container).toBeTruthy();
});

test("allows analyzer operators and global administrators into Analyzer Results", () => {
  expect(ANALYZER_RESULTS_ROLES).toEqual([
    Roles.GLOBAL_ADMIN,
    Roles.ANALYSER_IMPORT,
  ]);
});
