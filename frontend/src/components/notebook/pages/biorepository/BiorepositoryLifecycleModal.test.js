import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import BiorepositoryLifecycleModal from "./BiorepositoryLifecycleModal";

describe("BiorepositoryLifecycleModal", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test("shows awaiting re-storage lifecycle state banner", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => ({
        sampleItemId: 101,
        currentState: {
          workflowStatus: "PENDING_STORAGE",
          awaitingRestorage: true,
          isPhysicallyInStorage: false,
        },
        events: [
          {
            custodyAction: "RETURN_RECEIVED",
            eventTimestamp: "2026-04-20T08:00:00Z",
          },
        ],
      }),
    });

    render(
      <IntlProvider locale="en">
        <BiorepositoryLifecycleModal
          open
          onClose={() => {}}
          sampleItemId={101}
          sampleLabel="SAMPLE-101"
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByText("Returned - Awaiting Re-storage"),
    ).toBeTruthy();
    expect(screen.getByText("RETURN_RECEIVED")).toBeTruthy();
  });
});
