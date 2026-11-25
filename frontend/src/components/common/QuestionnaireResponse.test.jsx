import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { vi } from "vitest";
import QuestionnaireResponse from "./QuestionnaireResponse";

describe("QuestionnaireResponse", () => {
  it("lists every answer of a multi-answer question as keyed items", () => {
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <QuestionnaireResponse
        questionnaireResponse={{
          item: [
            {
              text: "Specimen site",
              answer: [{ valueString: "Left" }, { valueString: "Right" }],
            },
          ],
        }}
      />,
    );

    expect(screen.getByText("Specimen site")).toBeInTheDocument();
    expect(screen.getByText("Left")).toBeInTheDocument();
    expect(screen.getByText("Right")).toBeInTheDocument();
    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes('unique "key"'),
      ),
    ).toBe(false);
    errors.mockRestore();
  });
});
