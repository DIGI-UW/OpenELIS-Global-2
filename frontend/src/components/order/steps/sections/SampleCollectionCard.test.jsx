import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";

// Brings its own Notification/Configuration context needs; exercised in its
// own suite, not here.
vi.mock("../../../addOrder/GpsCoordinatesCapture", () => ({
  default: () => <div data-testid="gps-capture" />,
}));

import SampleCollectionCard from "./SampleCollectionCard";

describe("SampleCollectionCard", () => {
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
          collectionDate: "2026-08-13",
          collectionTime: "10:00",
          receivedDate: "2026-08-13",
          receivedTime: "10:00",
        }),
      ),
    );
  });

  it("waits for the server clock before defaulting collection and receipt", () => {
    const onUpdate = vi.fn();
    const props = {
      sampleIndex: 0,
      sampleTypes: [],
      unitOfMeasures: [],
      onUpdate,
      onRemove: vi.fn(),
      onPrintLabels: vi.fn(),
      isReadOnly: false,
      canRemove: false,
      sample: {
        sampleTypeId: "5",
        collectionDate: "",
        collectionTime: "",
        receivedDate: "",
        receivedTime: "",
        tests: [],
        panels: [],
      },
    };
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard
          {...props}
          serverReceivedDate=""
          serverReceivedTime=""
        />
      </IntlProvider>,
    );
    expect(onUpdate).not.toHaveBeenCalled();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard
          {...props}
          serverReceivedDate="2026-09-26"
          serverReceivedTime="09:00"
        />
      </IntlProvider>,
    );

    expect(onUpdate).toHaveBeenCalledWith(0, {
      collectionDate: "2026-09-26",
      collectionTime: "09:00",
      receivedDate: "2026-09-26",
      receivedTime: "09:00",
    });
  });

  it("fills the defaults again when the order reloads the sample without them", () => {
    const onUpdate = vi.fn();
    const props = {
      sampleIndex: 0,
      sampleTypes: [],
      unitOfMeasures: [],
      serverReceivedDate: "2026-09-26",
      serverReceivedTime: "09:00",
      onUpdate,
      onRemove: vi.fn(),
      onPrintLabels: vi.fn(),
      isReadOnly: false,
      canRemove: false,
    };
    const filled = {
      sampleTypeRequestId: "20",
      sampleTypeId: "5",
      collectionDate: "2026-09-26",
      collectionTime: "09:00",
      receivedDate: "2026-09-26",
      receivedTime: "09:00",
      tests: [],
      panels: [],
    };
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard {...props} sample={filled} />
      </IntlProvider>,
    );
    expect(onUpdate).not.toHaveBeenCalled();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <SampleCollectionCard
          {...props}
          sample={{
            ...filled,
            collectionDate: "",
            collectionTime: "",
            receivedDate: "",
            receivedTime: "",
          }}
        />
      </IntlProvider>,
    );

    expect(onUpdate).toHaveBeenCalledWith(0, {
      collectionDate: "2026-09-26",
      collectionTime: "09:00",
      receivedDate: "2026-09-26",
      receivedTime: "09:00",
    });
  });
});

describe("SampleCollectionCard print labels while the order loads (OGC-1423)", () => {
  const renderCard = (props = {}) =>
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
              tests: [],
              panels: [],
            }}
            sampleIndex={0}
            sampleTypes={[]}
            unitOfMeasures={[]}
            serverReceivedDate="2026-08-13"
            serverReceivedTime="10:00"
            onUpdate={vi.fn()}
            onRemove={vi.fn()}
            onPrintLabels={vi.fn()}
            isReadOnly={false}
            canRemove={false}
            {...props}
          />
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

  it("disables Print Labels while printDisabled is set", () => {
    renderCard({ printDisabled: true });
    expect(
      screen.getByRole("button", {
        name: messages["collect.sample.printLabels"],
      }),
    ).toBeDisabled();
  });

  it("offers Print Labels once the order has loaded", () => {
    renderCard({ printDisabled: false });
    expect(
      screen.getByRole("button", {
        name: messages["collect.sample.printLabels"],
      }),
    ).toBeEnabled();
  });
});
