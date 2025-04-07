import { render, screen, fireEvent } from "@testing-library/react";
import Workplan from "../Workplan";

describe("Workplan Component", () => {
  test("renders Workplan component", () => {
    render(<Workplan />);
    expect(screen.getByText(/Workplan/i)).toBeInTheDocument();
  });

  test("displays a loading message when data is fetching", async () => {
    render(<Workplan />);
    expect(screen.getByText(/Loading.../i)).toBeInTheDocument();
  });

  test("handles form submission correctly", () => {
    render(<Workplan />);
    const submitButton = screen.getByText(/Submit/i);
    fireEvent.click(submitButton);
    expect(screen.getByText(/Processing request.../i)).toBeInTheDocument();
  });
});
