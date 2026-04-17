import { render } from "@testing-library/react";
import App from "./App";

// Mock react-router-dom
jest.mock("react-router-dom", () => ({
  BrowserRouter: ({ children }) => children,
  Routes: ({ children }) => children,
  Route: ({ element }) => element,
  Navigate: () => null,
  useNavigate: () => jest.fn(),
  useLocation: () => ({ pathname: "/" }),
  useParams: () => ({}),
}));

test("renders App component without errors", () => {
  // Just verify the App component renders without throwing errors
  const { container } = render(<App />);
  expect(container).toBeTruthy();
});
