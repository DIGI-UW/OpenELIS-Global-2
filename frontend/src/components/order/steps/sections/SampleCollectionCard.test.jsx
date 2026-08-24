import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";
import SampleCollectionCard from "./SampleCollectionCard";

describe("SampleCollectionCard admission-date validation", () => {
  it("shows an inline error when collection predates admission", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <SampleCollectionCard
            sample={{
              sampleItemId: "1",
              sampleTypeId: "5",
              sampleTypeName: "Blood",
              collectionDate: "2026-08-02",
              tests: [],
              panels: [],
            }}
            sampleIndex={0}
            sampleTypes={[]}
            unitOfMeasures={[]}
            serverReceivedDate="2026-08-13"
            serverReceivedTime="10:00"
            admissionDate="2026-08-03"
            onUpdate={vi.fn()}
            onRemove={vi.fn()}
            onPrintLabels={vi.fn()}
            isReadOnly={false}
            canRemove={false}
          />
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

    expect(screen.getByLabelText(/Collection Date/)).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(
      screen.getByText("Collection date cannot be before date of admission."),
    ).toBeInTheDocument();
  });

  it("does not restore the default after the user clears a collection date", async () => {
    const ControlledCard = () => {
      const [sample, setSample] = React.useState({
        sampleTypeId: "5",
        sampleTypeName: "Blood",
        collectionDate: "2026-08-13",
        collectionTime: "10:00",
        receivedDate: "2026-08-13",
        receivedTime: "10:00",
        tests: [],
        panels: [],
      });
      return (
        <SampleCollectionCard
          sample={sample}
          sampleIndex={0}
          sampleTypes={[]}
          unitOfMeasures={[]}
          serverReceivedDate="2026-08-13"
          serverReceivedTime="10:00"
          onUpdate={(_, updates) =>
            setSample((previous) => ({ ...previous, ...updates }))
          }
          onRemove={vi.fn()}
          onPrintLabels={vi.fn()}
          isReadOnly={false}
          canRemove={false}
        />
      );
    };

    render(
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <ControlledCard />
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

    const collectionDate = screen.getByLabelText(/Collection Date/);
    await userEvent.setup().clear(collectionDate);

    expect(collectionDate).toHaveValue("");
  });

  it("initializes defaults when a different pending request replaces the card", async () => {
    const onUpdate = vi.fn();
    const props = {
      sampleIndex: 0,
      sampleTypes: [],
      unitOfMeasures: [],
      serverReceivedDate: "2026-08-13",
      serverReceivedTime: "10:00",
      onUpdate,
      onRemove: vi.fn(),
      onPrintLabels: vi.fn(),
      isReadOnly: false,
      canRemove: false,
    };
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard
          {...props}
          sample={{
            sampleTypeRequestId: "request-old",
            sampleTypeId: "5",
            collectionDate: "2026-08-12",
            collectionTime: "09:00",
            receivedDate: "2026-08-12",
            receivedTime: "09:05",
            tests: [],
            panels: [],
          }}
        />
      </IntlProvider>,
    );
    onUpdate.mockClear();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard
          {...props}
          sample={{
            sampleTypeRequestId: "request-new",
            sampleTypeId: "5",
            collectionDate: "",
            collectionTime: "",
            receivedDate: "",
            receivedTime: "",
            tests: [],
            panels: [],
          }}
        />
      </IntlProvider>,
    );

    await waitFor(() =>
      expect(onUpdate).toHaveBeenCalledWith(
        0,
        expect.objectContaining({
          collectionDate: expect.any(String),
          collectionTime: expect.any(String),
          receivedDate: "2026-08-13",
          receivedTime: "10:00",
        }),
      ),
    );
  });
});
