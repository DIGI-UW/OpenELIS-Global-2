import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import GenericSampleOrderImport from "./GenericSampleOrderImport";

vi.mock("../common/PageBreadCrumb", () => ({ default: () => null }));

const refusal = {
  rowNumber: 2,
  field: "sampleQuantity",
  messageKey: "import.error.sampleQuantityLimit",
  messageValues: { row: 2, quantity: 501, maximum: 500 },
};

beforeEach(() => {
  vi.stubGlobal("fetch", vi.fn());
});
afterEach(() => vi.unstubAllGlobals());

async function uploadAndValidate(response) {
  fetch.mockResolvedValueOnce({ json: () => Promise.resolve(response) });
  const { container } = render(
    <IntlProvider
      locale="fr"
      messages={{
        ...messages,
        "import.error.sampleQuantityLimit":
          "Ligne {row} : {quantity} dépasse la limite de {maximum}.",
      }}
    >
      <GenericSampleOrderImport showBreadcrumbs={false} />
    </IntlProvider>,
  );
  fireEvent.change(container.querySelector('input[type="file"]'), {
    target: {
      files: [
        new File(["SampleQuantity\n501"], "limits.csv", { type: "text/csv" }),
      ],
    },
  });
  fireEvent.click(
    screen.getByRole("button", { name: messages["label.button.validate"] }),
  );
}

it("translates preview refusals and prevents import", async () => {
  await uploadAndValidate({
    valid: false,
    totalRows: 1,
    validRows: 0,
    invalidRows: 1,
    totalSamplesToCreate: 0,
    previewRows: [],
    errors: [refusal],
  });
  expect(
    await screen.findByText("Ligne 2 : 501 dépasse la limite de 500."),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: messages["label.button.import"] }),
  ).toBeDisabled();
});

it("translates a direct import refusal as well as preview errors", async () => {
  await uploadAndValidate({
    valid: true,
    totalRows: 1,
    validRows: 1,
    invalidRows: 0,
    totalSamplesToCreate: 1,
    previewRows: [],
    errors: [],
  });
  await screen.findByText(messages["import.status.valid"]);
  fetch.mockResolvedValueOnce({
    json: () =>
      Promise.resolve({
        success: true,
        totalCreated: 0,
        totalFailed: 1,
        errors: [refusal],
        createdAccessionNumbers: [],
      }),
  });
  fireEvent.click(
    screen.getByRole("button", { name: messages["label.button.import"] }),
  );
  expect(
    await screen.findByText("Ligne 2 : 501 dépasse la limite de 500."),
  ).toBeInTheDocument();
});
