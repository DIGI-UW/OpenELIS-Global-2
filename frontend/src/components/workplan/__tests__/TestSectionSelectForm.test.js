import { render, screen, fireEvent } from "@testing-library/react";
import TestSectionSelectForm from "../TestSectionSelectForm";

describe("TestSectionSelectForm Component", () => {
  test("renders the form correctly", () => {
    render(<TestSectionSelectForm />);
    expect(screen.getByLabelText(/Select Section/i)).toBeInTheDocument();
  });

  test("handles section selection", () => {
    render(<TestSectionSelectForm />);
    const selectElement = screen.getByLabelText(/Select Section/i);
    fireEvent.change(selectElement, { target: { value: "Section A" } });
    expect(selectElement.value).toBe("Section A");
  });
});
