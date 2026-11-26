import React from "react";
import { render, fireEvent, waitForElement } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi, describe, it, expect } from "vitest";
import { NotificationContext } from "../layout/Layout";
import { VectorDeconvolutionAPI } from "./VectorIdentificationService";
import VectorInlineDeconPanel from "./VectorInlineDeconPanel";
import messages from "../../languages/en.json";

vi.mock("../layout/Layout", async () => {
  const React = await import("react");
  return { NotificationContext: React.createContext({}) };
});
vi.mock("./VectorIdentificationService", () => ({
  VectorSamplingSiteAPI: { getAll: vi.fn().mockResolvedValue([]) },
  VectorDeconvolutionAPI: {
    previewReflexes: vi.fn().mockResolvedValue({ poolTests: [] }),
    getAvailablePanelTests: vi.fn().mockResolvedValue([]),
    initiate: vi.fn(),
  },
}));

describe("pool-count validation", () => {
  it("renders the server counts with the active translation, preserving the unsaved split", async () => {
    const addNotification = vi.fn();
    const onInitiated = vi.fn();
    const error = new Error("Do not display this fallback");
    error.payload = {
      messageKey: "vectorDec.error.poolCountExceedsMembers",
      messageArgs: { max: "2", requested: "3" },
    };
    VectorDeconvolutionAPI.initiate.mockRejectedValue(error);
    const view = render(
      <IntlProvider
        locale="fr"
        messages={{
          ...messages,
          "vectorDec.error.poolCountExceedsMembers":
            "Demande : {requested}. Maximum : {max}.",
        }}
      >
        <NotificationContext.Provider
          value={{ addNotification, setNotificationVisible: vi.fn() }}
        >
          <VectorInlineDeconPanel
            vectorPoolId={700}
            accessionNumber="TEST"
            specimens={Array.from({ length: 6 }, (_, i) => ({
              sampleItemId: i + 1,
            }))}
            onInitiated={onInitiated}
          />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    fireEvent.click(
      view.getByText(messages["vectorDec.button.previewGrouping"], {
        exact: false,
      }),
    );
    fireEvent.click(view.getByText(messages["vectorDec.button.savePools"]));
    await waitForElement(() => {
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          message: "Demande : 3. Maximum : 2.",
        }),
      );
      return view.getByText(messages["vectorDec.button.savePools"]);
    });
    expect(onInitiated).not.toHaveBeenCalled();
    expect(VectorDeconvolutionAPI.initiate).toHaveBeenCalledWith(
      expect.objectContaining({ poolCount: 3 }),
    );
  });
});
