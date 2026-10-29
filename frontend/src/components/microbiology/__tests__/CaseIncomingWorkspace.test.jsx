import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseIncomingWorkspace from "../CaseIncomingWorkspace";

const components = [
  { componentId: "one", value: "90" },
  { componentId: "two", value: "-3" },
  { componentId: "three", value: "12" },
];
const refusal = {
  response: {
    status: 422,
    code: "ACKNOWLEDGEMENT_REQUIRED",
    acknowledgementRequired: [
      { kind: "CRITICAL", componentId: "one", value: "90" },
      { kind: "INVALID", componentId: "two", value: "-3" },
    ],
  },
};
function show() {
  const service = {
    getIncoming: vi.fn().mockResolvedValue([
      {
        id: "incoming",
        testName: "Culture measurement",
        state: "WAITING",
        sampleItemId: "sample",
        receivedAt: "2026-10-22T12:00:00Z",
        result: { components, note: "Instrument note" },
      },
    ]),
    placeIncoming: vi.fn().mockRejectedValueOnce(refusal).mockResolvedValue({}),
  };
  const onChanged = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseIncomingWorkspace
        detail={{
          id: "case",
          canWrite: true,
          samples: [{ sampleItemId: "sample", label: "Tube 1" }],
        }}
        service={service}
        onChanged={onChanged}
      />
    </IntlProvider>,
  );
  return { service, onChanged };
}
it("confirms only the refused components and preserves the incoming values and note", async () => {
  const { service, onChanged } = show();
  fireEvent.click(
    await screen.findByRole("button", {
      name: messages["microbiology.incoming.review"],
    }),
  );
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.incoming.place"],
    }),
  );
  expect(await screen.findByTestId("result-alert-modal")).toBeInTheDocument();
  expect(onChanged).not.toHaveBeenCalled();
  fireEvent.click(
    screen.getByRole("button", {
      name: /Acknowledge and save/,
    }),
  );
  await waitFor(() => expect(onChanged).toHaveBeenCalledOnce());
  expect(service.placeIncoming).toHaveBeenLastCalledWith("case", "incoming", {
    sampleItemId: "sample",
    targetCaseId: "case",
    reason: "",
    result: {
      note: "Instrument note",
      components: [
        {
          ...components[0],
          criticalAcknowledged: true,
          invalidResultConfirmed: false,
        },
        {
          ...components[1],
          criticalAcknowledged: false,
          invalidResultConfirmed: true,
        },
        {
          ...components[2],
          criticalAcknowledged: false,
          invalidResultConfirmed: false,
        },
      ],
    },
  });
});
it("keeps the item open without retrying when acknowledgement is declined", async () => {
  const { service, onChanged } = show();
  fireEvent.click(
    await screen.findByRole("button", {
      name: messages["microbiology.incoming.review"],
    }),
  );
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.incoming.place"],
    }),
  );
  await screen.findByTestId("result-alert-modal");
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["label.results.alert.correct"],
    }),
  );
  expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
  expect(service.placeIncoming).toHaveBeenCalledTimes(1);
  expect(onChanged).not.toHaveBeenCalled();
  expect(
    screen.getByRole("button", {
      name: messages["microbiology.incoming.place"],
    }),
  ).toBeEnabled();
});
