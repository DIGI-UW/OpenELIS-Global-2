import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import AliquotsSection from "./AliquotsSection";

const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;
const postJsonMock = postToOpenElisServerJsonResponse as ReturnType<
  typeof vi.fn
>;

const createAliquot = (
  response: Record<string, unknown>,
  onCreated: () => void,
) => {
  postJsonMock.mockImplementation(
    (_url: string, _body: string, cb: (body: unknown) => void) => cb(response),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <AliquotsSection
        accessionNumber="DEV1"
        sampleItemId="41"
        open={true}
        onToggle={() => {}}
        onCreated={onCreated}
      />
    </IntlProvider>,
  );
  fireEvent.change(screen.getByLabelText("Total quantity to transfer"), {
    target: { value: "1" },
  });
  fireEvent.click(screen.getByTestId("aliquot-create"));
};

describe("AliquotsSection", () => {
  beforeEach(() => {
    getMock.mockReset();
    postJsonMock.mockReset();
  });

  it("tells the panel once aliquots are created, so the parent's quantity reloads", () => {
    const onCreated = vi.fn();

    createAliquot({ aliquots: [{ externalId: "DEV1.1" }] }, onCreated);

    expect(postJsonMock).toHaveBeenCalledTimes(1);
    expect(onCreated).toHaveBeenCalledTimes(1);
  });

  it("does not report a refused create", () => {
    const onCreated = vi.fn();

    createAliquot({ message: "Insufficient quantity" }, onCreated);

    expect(screen.getByText("Insufficient quantity")).toBeInTheDocument();
    expect(onCreated).not.toHaveBeenCalled();
  });
});
