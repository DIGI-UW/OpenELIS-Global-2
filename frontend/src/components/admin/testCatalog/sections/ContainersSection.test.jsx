/**
 * ContainersSection: OGC-1611 P2 (FR-G5 to G6b). A test names its expected
 * containers, one Preferred and the rest Alternate, and a container yielding
 * another sample type is an Alternate used as the test's own sample type.
 */

const { notify } = vi.hoisted(() => ({
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return { NotificationContext: React.createContext(notify) };
});

import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ContainersSection from "./ContainersSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const SERUM = { id: "2", value: "Serum" };
const PLASMA = { id: "3", value: "Plasma" };
const SST = {
  id: 6,
  name: "Serum separator tube (SST)",
  capColourHex: "#D4AF37",
  active: true,
  sampleTypes: [SERUM],
};
const RED = {
  id: 5,
  name: "Plain serum tube",
  capColourHex: "#D32F2F",
  active: true,
  sampleTypes: [SERUM],
};
const PST = {
  id: 8,
  name: "Plasma separator tube (PST)",
  capColourHex: "#90EE90",
  active: true,
  sampleTypes: [PLASMA],
};

const view = (overrides = {}) => ({
  testId: "42",
  domain: "CLINICAL",
  needsOwnContainer: false,
  containers: [],
  primarySampleTypes: [SERUM],
  secondarySampleTypes: [],
  yieldingChoices: [SST, RED],
  otherChoices: [PST],
  ...overrides,
});

const renderSection = (loaded) => {
  getFromOpenElisServer.mockImplementation((url, callback) => callback(loaded));
  render(
    <IntlProvider locale="en" messages={messages}>
      <ContainersSection testId="42" />
    </IntlProvider>,
  );
};

const addContainer = (id) =>
  fireEvent.change(screen.getByLabelText("Add a container"), {
    target: { value: String(id) },
  });

const sentBody = () =>
  JSON.parse(putToOpenElisServerJsonResponse.mock.calls[0][1]);

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ContainersSection", () => {
  it("renders once the server answers after the first render, as a real fetch does", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      setTimeout(() => callback(view()), 0),
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <ContainersSection testId="42" />
      </IntlProvider>,
    );

    expect(await screen.findByTestId("containers-section")).toBeInTheDocument();
    expect(screen.getByLabelText("Add a container")).toBeInTheDocument();
  });

  it("offers the containers yielding the test's sample type first and those yielding another apart", () => {
    renderSection(view());

    const picker = screen.getByLabelText("Add a container");
    const groups = picker.querySelectorAll("optgroup");
    expect(groups[0]).toHaveAttribute("label", "Yields Serum");
    expect(
      within(groups[0])
        .getAllByRole("option")
        .map((o) => o.textContent),
    ).toEqual(["Serum separator tube (SST)", "Plain serum tube"]);
    expect(within(groups[1]).getByRole("option")).toHaveTextContent(
      "Plasma separator tube (PST) (Plasma)",
    );
    expect(screen.getByText(/No expected containers/)).toBeInTheDocument();
  });

  it("makes the first container Preferred, the next Alternate, and saves them in order with the counts and volumes", () => {
    renderSection(view());
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback(view({ containers: [] })),
    );

    addContainer(6);
    addContainer(5);
    fireEvent.change(document.getElementById("container-count-6"), {
      target: { value: "2" },
    });
    fireEvent.change(document.getElementById("container-volume-6"), {
      target: { value: "1.5" },
    });
    fireEvent.click(screen.getByLabelText("Needs its own container"));
    fireEvent.click(screen.getByTestId("containers-save"));

    expect(putToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/test-catalog/tests/42/containers",
    );
    expect(sentBody()).toEqual({
      needsOwnContainer: true,
      containers: [
        {
          containerTypeId: 6,
          role: "PREFERRED",
          count: 2,
          minVolume: 1.5,
          volumeUnit: "mL",
          usedAsSampleTypeId: null,
        },
        {
          containerTypeId: 5,
          role: "ALTERNATE",
          count: 1,
          minVolume: null,
          volumeUnit: null,
          usedAsSampleTypeId: null,
        },
      ],
    });
  });

  it("reads a comma as the decimal point of a minimum volume", () => {
    renderSection(view());
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback(view()),
    );
    addContainer(6);

    fireEvent.change(document.getElementById("container-volume-6"), {
      target: { value: "1,5" },
    });
    fireEvent.click(screen.getByTestId("containers-save"));

    expect(sentBody().containers[0]).toMatchObject({
      minVolume: 1.5,
      volumeUnit: "mL",
    });
  });

  it("refuses a minimum volume or a count that is not a number before saving", () => {
    renderSection(view());
    addContainer(6);
    const save = screen.getByTestId("containers-save");

    fireEvent.change(document.getElementById("container-volume-6"), {
      target: { value: "abc" },
    });
    expect(
      document.getElementById("container-volume-6"),
    ).toHaveAccessibleDescription(
      "Enter the minimum volume as a number, for example 1.5.",
    );
    expect(save).toBeDisabled();

    fireEvent.change(document.getElementById("container-volume-6"), {
      target: { value: "0" },
    });
    expect(
      document.getElementById("container-volume-6"),
    ).toHaveAccessibleDescription("The minimum volume must be more than zero.");
    expect(save).toBeDisabled();

    fireEvent.change(document.getElementById("container-volume-6"), {
      target: { value: "" },
    });
    expect(save).toBeEnabled();

    fireEvent.change(document.getElementById("container-count-6"), {
      target: { value: "" },
    });
    expect(screen.getByText("Enter a count from 1 to 99.")).toBeInTheDocument();
    expect(save).toBeDisabled();
    fireEvent.click(save);
    expect(putToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it("adds a container of another sample type as an Alternate used as the test's own, never Preferred", () => {
    renderSection(view());
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback(view()),
    );

    addContainer(6);
    addContainer(8);

    const role = document.getElementById("container-role-8");
    expect(role).toHaveValue("ALTERNATE");
    expect(
      within(role).getByRole("option", { name: "Preferred" }),
    ).toBeDisabled();
    expect(document.getElementById("container-used-as-8")).toHaveValue("2");
    expect(
      screen.getByLabelText("Plasma separator tube (PST) yields Plasma"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("containers-save"));
    expect(sentBody().containers[1]).toMatchObject({
      containerTypeId: 8,
      role: "ALTERNATE",
      usedAsSampleTypeId: "2",
    });
  });

  it("keeps a single Preferred container when another is made Preferred", () => {
    renderSection(view());
    addContainer(6);
    addContainer(5);

    fireEvent.change(document.getElementById("container-role-5"), {
      target: { value: "PREFERRED" },
    });

    expect(document.getElementById("container-role-5")).toHaveValue(
      "PREFERRED",
    );
    expect(document.getElementById("container-role-6")).toHaveValue(
      "ALTERNATE",
    );
  });

  it("asks for a Preferred container before saving a list that has none", () => {
    renderSection(
      view({
        containers: [
          {
            containerTypeId: 5,
            name: "Plain serum tube",
            active: true,
            role: "ALTERNATE",
            count: 1,
            yields: [SERUM],
          },
        ],
      }),
    );

    expect(
      screen.getByTestId("containers-preferred-required"),
    ).toHaveTextContent("Choose one Preferred container.");
    expect(screen.getByTestId("containers-save")).toBeDisabled();
  });

  it("shows the test's secondary sample types and a deactivated container it still lists", () => {
    renderSection(
      view({
        containers: [
          {
            containerTypeId: 6,
            name: "Serum separator tube (SST)",
            active: false,
            role: "PREFERRED",
            count: 1,
            yields: [SERUM],
          },
        ],
        secondarySampleTypes: [
          {
            sampleTypeId: "3",
            sampleTypeName: "Plasma",
            containerName: "Plasma separator tube (PST)",
            usedAsSampleTypeName: "Serum",
          },
        ],
      }),
    );

    expect(screen.getByTestId("containers-secondary")).toHaveTextContent(
      "Plasma, via Plasma separator tube (PST), used as Serum",
    );
    expect(
      within(screen.getByTestId("container-row-6")).getByText("Deactivated"),
    ).toBeInTheDocument();
  });

  it("names the refused row and keeps the list as typed", () => {
    renderSection(view());
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ status: 400, field: "count", code: "countInvalid", row: 0 }),
    );
    addContainer(6);

    fireEvent.click(screen.getByTestId("containers-save"));

    expect(screen.getByTestId("container-row-error-6")).toHaveTextContent(
      "Enter a count from 1 to 99.",
    );
    expect(screen.getByTestId("container-row-6")).toBeInTheDocument();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
  });
});
