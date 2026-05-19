import { render } from "@testing-library/react";
import App from "./App";

jest.mock(
  "./components/patient/resultsViewer/results-viewer.tsx",
  () => () => null,
);

test("renders App component without errors", () => {
  const { container } = render(<App />);
  expect(container).toBeTruthy();
});
