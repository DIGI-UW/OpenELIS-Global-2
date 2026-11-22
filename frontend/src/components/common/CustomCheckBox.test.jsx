import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import CustomCheckBox from "./CustomCheckBox";

// UserManagement refetches its list whenever `filters` changes, and that
// effect's cleanup drops the page for a spinner before the new render, so the
// checkbox is unmounted and remounted on every toggle. `gated` reproduces that.
const Screen = ({ gated, controlled }) => {
  const [filters, setFilters] = useState([]);
  if (gated) {
    return <output data-testid="filters">{filters.join(",")}</output>;
  }
  return (
    <>
      <CustomCheckBox
        id="only-active"
        label="Only Active"
        onChange={(isChecked) => setFilters(isChecked ? ["isActive"] : [])}
        {...(controlled ? { checked: filters.includes("isActive") } : {})}
      />
      <output data-testid="filters">{filters.join(",")}</output>
    </>
  );
};

describe("CustomCheckBox", () => {
  it("a controlled box comes back checked after the screen remounts it", async () => {
    const { container, rerender } = render(<Screen gated={false} controlled />);

    await userEvent.click(container.querySelector("#only-active"));
    expect(screen.getByTestId("filters")).toHaveTextContent("isActive");

    rerender(<Screen gated controlled />);
    rerender(<Screen gated={false} controlled />);

    expect(container.querySelector("#only-active")).toBeChecked();
  });

  it("an uncontrolled box loses the choice the caller still holds", async () => {
    const { container, rerender } = render(
      <Screen gated={false} controlled={false} />,
    );

    await userEvent.click(container.querySelector("#only-active"));

    rerender(<Screen gated controlled={false} />);
    rerender(<Screen gated={false} controlled={false} />);

    expect(container.querySelector("#only-active")).not.toBeChecked();
  });

  it("still reports changes when the caller does not own the value", async () => {
    const onChange = vi.fn();
    const { container } = render(
      <CustomCheckBox
        id="uncontrolled"
        label="Only Active"
        onChange={onChange}
      />,
    );

    await userEvent.click(container.querySelector("#uncontrolled"));

    expect(onChange).toHaveBeenCalledWith(true);
  });
});
