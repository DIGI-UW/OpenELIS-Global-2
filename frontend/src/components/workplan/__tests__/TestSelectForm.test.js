import { render, screen, fireEvent } from "@testing-library/react";
import TestSelectForm from "../TestSelectForm";

describe("TestSelectForm Component", () => {
  test("renders the form correctly", () => {
    render(<TestSelectForm />);
    expect(screen.getByLabelText(/Select Test/i)).toBeInTheDocument();
  });

  test("handles test selection", () => {
    render(<TestSelectForm />);
    const selectElement = screen.getByLabelText(/Select Test/i);
    fireEvent.change(selectElement, { target: { value: "Test1" } });
    expect(selectElement.value).toBe("Test1");
  });
});
