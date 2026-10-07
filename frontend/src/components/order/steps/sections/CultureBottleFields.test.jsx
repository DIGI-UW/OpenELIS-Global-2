import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import CultureBottleFields from "./CultureBottleFields";
import messages from "../../../../languages/en.json";

it("edits each recorded detail independently and preserves lab-local date/time", () => {
  const onChange = vi.fn();
  const sample = {
    container: "Aerobic",
    bodySite: "Left arm",
    collectionDate: "2026-10-07",
    collectionTime: "07:25",
  };
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <CultureBottleFields
        sample={sample}
        sampleIndex={0}
        onChange={onChange}
        isReadOnly={false}
      />
    </IntlProvider>,
  );
  expect(screen.getByLabelText("Collection Date")).toHaveValue("2026-10-07");
  expect(screen.getByLabelText("Collection Time")).toHaveValue("07:25");
  fireEvent.change(screen.getByLabelText("Collection Time"), {
    target: { value: "08:10" },
  });
  expect(onChange).toHaveBeenCalledWith("collectionTime", "08:10");
  expect(screen.getByLabelText("Body site")).toHaveAttribute("maxlength", "40");
  view.rerender(
    <IntlProvider locale="en" messages={messages}>
      <CultureBottleFields
        sample={sample}
        sampleIndex={0}
        onChange={onChange}
        isReadOnly
      />
    </IntlProvider>,
  );
  expect(screen.getByLabelText("Body site")).toBeDisabled();
  expect(screen.getByLabelText("Container type")).toBeDisabled();
  expect(screen.getByLabelText("Collection Time")).toBeDisabled();
});
