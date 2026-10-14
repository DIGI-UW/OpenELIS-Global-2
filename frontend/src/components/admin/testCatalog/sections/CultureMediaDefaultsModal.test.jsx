import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import CultureMediaDefaultsModal from "./CultureMediaDefaultsModal";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));
test("saves incubation defaults without discarding existing reagent usage; failed saves preserve inputs", async () => {
  getFromOpenElisServer.mockImplementation((url, callback) =>
    callback({ atmospheres: [{ id: "air", value: "Aerobic" }] }),
  );
  putToOpenElisServer.mockImplementation((url, body, callback) =>
    callback(400),
  );
  const onSaved = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureMediaDefaultsModal
        testId="12"
        link={{
          reagentId: 10,
          usageType: "PRIMARY",
          quantityPerTest: 2,
          quantityUnit: "mL",
        }}
        onClose={vi.fn()}
        onSaved={onSaved}
      />
    </IntlProvider>,
  );
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.culture.duration"]),
    { target: { value: "56" } },
  );
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.culture.durationUnit"]),
    { target: { value: "DAYS" } },
  );
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(putToOpenElisServer.mock.calls[0][0]).toBe(
    "/rest/test-catalog/12/reagents/10",
  );
  expect(JSON.parse(putToOpenElisServer.mock.calls[0][1])).toEqual(
    expect.objectContaining({
      cultureDuration: 56,
      cultureDurationUnit: "DAYS",
      cultureDefaultsChanged: true,
      usageType: "PRIMARY",
      quantityPerTest: 2,
      quantityUnit: "mL",
    }),
  );
  expect(
    await screen.findByText(messages["microbiology.culture.saveError"]),
  ).toBeInTheDocument();
  expect(
    screen.getByLabelText(messages["microbiology.culture.duration"]),
  ).toHaveValue(56);
  expect(onSaved).not.toHaveBeenCalled();
});

test("selects and clears specimen-specific media defaults through Carbon controls", async () => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    callback(
      url === "/rest/test-catalog/sample-types"
        ? [
            { id: "serum", name: "Serum" },
            { id: "plasma", name: "Plasma" },
          ]
        : { atmospheres: [] },
    );
  });
  putToOpenElisServer.mockClear();
  putToOpenElisServer.mockImplementation((url, body, callback) =>
    callback(200),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureMediaDefaultsModal
        testId="12"
        link={{
          reagentId: 10,
          sampleTypeId: "serum",
          usageType: "PRIMARY",
          quantityUnit: "mL",
        }}
        onClose={vi.fn()}
        onSaved={vi.fn()}
      />
    </IntlProvider>,
  );
  const specimen = await screen.findByLabelText("Specimen type");
  expect(specimen).toHaveValue("serum");
  fireEvent.change(specimen, { target: { value: "plasma" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(JSON.parse(putToOpenElisServer.mock.calls[0][1])).toEqual(
    expect.objectContaining({
      sampleTypeId: "plasma",
      cultureDefaultsChanged: true,
      usageType: "PRIMARY",
      quantityUnit: "mL",
    }),
  );
  fireEvent.change(specimen, { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(
    JSON.parse(putToOpenElisServer.mock.calls[1][1]).sampleTypeId,
  ).toBeNull();
});
