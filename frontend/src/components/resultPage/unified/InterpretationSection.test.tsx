import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));
// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../utils/Utils";
// eslint-disable-next-line import/first
import InterpretationSection from "./InterpretationSection";

const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

describe("InterpretationSection component rules", () => {
  it("shows a rule for the matching component", async () => {
    getMock.mockReset();
    getMock.mockImplementation((_url: string, cb: (body: unknown) => void) =>
      cb([
        {
          id: "rule-a",
          componentId: "A",
          valueMatch: ">10",
          text: "Suggestion for A",
          severity: "NORMAL",
          displayOrder: 1,
        },
      ]),
    );

    render(
      <IntlProvider locale="en" messages={messages}>
        <InterpretationSection
          testId="7"
          componentId="A"
          draft={null}
          onDraftChange={vi.fn()}
          editable
          openOverride={true}
          onToggle={vi.fn()}
        />
      </IntlProvider>,
    );
    const suggestion = await screen.findByText("Suggestion for A");
    expect(suggestion).toBeInTheDocument();
  });

  it("does not show another component's rule", async () => {
    getMock.mockReset();
    getMock.mockImplementation((_url: string, cb: (body: unknown) => void) =>
      cb([
        {
          id: "rule-a",
          componentId: "A",
          valueMatch: ">10",
          text: "Suggestion for A",
          severity: "NORMAL",
          displayOrder: 1,
        },
      ]),
    );

    render(
      <IntlProvider locale="en" messages={messages}>
        <InterpretationSection
          testId="7"
          componentId="B"
          draft={null}
          onDraftChange={vi.fn()}
          editable
          openOverride={true}
          onToggle={vi.fn()}
        />
      </IntlProvider>,
    );

    await screen.findByRole("textbox");
    expect(screen.queryByText("Suggestion for A")).not.toBeInTheDocument();
  });

  it("shows all rules when the row has no component ID", async () => {
    getMock.mockReset();
    getMock.mockImplementation((_url: string, cb: (body: unknown) => void) =>
      cb([
        {
          id: "rule-a",
          componentId: "A",
          valueMatch: ">10",
          text: "Suggestion for A",
        },
        {
          id: "rule-b",
          componentId: "B",
          valueMatch: "<5",
          text: "Suggestion for B",
        },
      ]),
    );

    render(
      <IntlProvider locale="en" messages={messages}>
        <InterpretationSection
          testId="7"
          draft={null}
          onDraftChange={vi.fn()}
          editable
          openOverride={true}
          onToggle={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(await screen.findByText("Suggestion for A")).toBeInTheDocument();
    expect(screen.getByText("Suggestion for B")).toBeInTheDocument();
  });

  it("matches a numeric rule ID to a string row ID", async () => {
    getMock.mockReset();
    getMock.mockImplementation((_url: string, cb: (body: unknown) => void) =>
      cb([
        {
          id: "rule-1",
          componentId: 1,
          valueMatch: ">10",
          text: "Suggestion for component 1",
        },
      ]),
    );

    render(
      <IntlProvider locale="en" messages={messages}>
        <InterpretationSection
          testId="7"
          componentId="1"
          draft={null}
          onDraftChange={vi.fn()}
          editable
          openOverride={true}
          onToggle={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByText("Suggestion for component 1"),
    ).toBeInTheDocument();
  });
});

/**
 * OGC-1458: a select-list result and a bucket's match are dictionary ids. The
 * tech reads the option text the admin configured, never the id.
 */
describe("InterpretationSection on a select-list result", () => {
  beforeEach(() => {
    getMock.mockReset();
    getMock.mockImplementation((_url: string, cb: (body: unknown) => void) =>
      cb([
        {
          id: "b1",
          valueMatch: "1958",
          text: "Grade one",
          severity: "ABNORMAL",
          displayOrder: 1,
        },
      ]),
    );
  });

  it("names the option in the banner and the bucket, not its dictionary id", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <InterpretationSection
          testId="7"
          resultValue="1958"
          dictionaryOptions={[
            { id: "1958", value: "1" },
            { id: "1959", value: "2" },
          ]}
          draft={null}
          onDraftChange={vi.fn()}
          editable
          openOverride={true}
          onToggle={vi.fn()}
        />
      </IntlProvider>,
    );

    const banner = await screen.findByTestId("interpretation-rule");
    expect(banner).toHaveTextContent("the entered value 1 falls in 1.");
    expect(banner).not.toHaveTextContent("1958");
    expect(screen.getByTestId("bucket-1958")).not.toHaveTextContent("1958");
  });
});
