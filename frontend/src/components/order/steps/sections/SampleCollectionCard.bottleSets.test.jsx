import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import SampleCollectionCard from "./SampleCollectionCard";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback) =>
    callback(
      url.startsWith("/rest/sample-type-tests")
        ? { tests: [{ id: "42", collectedInSets: true }] }
        : [],
    ),
}));
vi.mock("../../../addOrder/GpsCoordinatesCapture", () => ({
  default: () => null,
}));

function renderBottle(cultureSetNumber, isReadOnly = false) {
  const onUpdate = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleCollectionCard
        sample={{
          sampleTypeRequestId: "81",
          sampleTypeId: "5",
          tests: [{ id: "42", name: "Blood culture" }],
          cultureSetNumber,
        }}
        sampleIndex={0}
        sampleTypes={[]}
        unitOfMeasures={[]}
        onUpdate={onUpdate}
        onRemove={vi.fn()}
        onPrintLabels={vi.fn()}
        isReadOnly={isReadOnly}
        canRemove={false}
      />
    </IntlProvider>,
  );
  return onUpdate;
}

it("shows the stored set and sends an explicit collection correction", async () => {
  const onUpdate = renderBottle(2);
  const input = await screen.findByRole("spinbutton", { name: "Set number" });
  expect(input).toHaveValue(2);
  fireEvent.change(input, { target: { value: "3" } });
  expect(onUpdate).toHaveBeenCalledWith(0, { cultureSetNumber: "3" });
});

it("resolves a reloaded test's catalog flag without inventing a missing set", async () => {
  const onUpdate = renderBottle(null);
  const input = await screen.findByRole("spinbutton", { name: "Set number" });
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(input).toHaveValue(null);
  expect(onUpdate).not.toHaveBeenCalled();
});

it("prevents read-only collection edits", async () => {
  renderBottle(2, true);
  expect(
    await screen.findByRole("spinbutton", { name: "Set number" }),
  ).toBeDisabled();
});

it("lets collection record a body site and container without altering the set", async () => {
  const onUpdate = renderBottle(2);
  fireEvent.change(await screen.findByRole("textbox", { name: "Body site" }), {
    target: { value: "Left arm" },
  });
  expect(onUpdate).toHaveBeenLastCalledWith(0, { bodySite: "Left arm" });
  fireEvent.change(screen.getByRole("textbox", { name: "Container type" }), {
    target: { value: "Aerobic" },
  });
  expect(onUpdate).toHaveBeenLastCalledWith(0, { container: "Aerobic" });
  expect(screen.getByRole("spinbutton", { name: "Set number" })).toHaveValue(2);
});
