import React from "react";
import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../../utils/Utils";
// eslint-disable-next-line import/first
import { useGetManyObstreeData } from "./useObstreeData";

const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

/**
 * The tree the server sends already carries the reference range chosen for this
 * patient, specimen and component; the client must not replace it with the raw
 * numeric bounds, and must still assess each observation against them.
 */
const Probe = () => {
  const { roots } = useGetManyObstreeData("7");
  const leaf = roots?.[0]?.subSets?.[0]?.subSets?.[0];
  if (!leaf) {
    return null;
  }
  return (
    <>
      <span data-testid="range">{leaf.range}</span>
      <span data-testid="flat-name">{leaf.flatName}</span>
      <span data-testid="interpretation">{leaf.obs[0].interpretation}</span>
    </>
  );
};

const tree = (leaf: Record<string, unknown>) => [
  {
    display: "Haematology",
    subSets: [{ display: "Serum", subSets: [leaf] }],
  },
];

const givenTree = (leaf: Record<string, unknown>) => {
  getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
    cb(tree(leaf));
  });
};

describe("useGetManyObstreeData", () => {
  beforeEach(() => {
    getMock.mockReset();
  });

  it("keeps the reference range resolved by the server", () => {
    givenTree({
      display: "Blood Pressure — Systolic",
      range: "90 - 120",
      lowNormal: 90,
      hiNormal: 120,
      obs: [{ obsDatetime: "2026-08-01 09:00:00.0", value: "130" }],
    });

    render(<Probe />);

    expect(screen.getByTestId("range")).toHaveTextContent("90 - 120");
    expect(screen.getByTestId("interpretation")).toHaveTextContent("HIGH");
    expect(screen.getByTestId("flat-name")).toHaveTextContent(
      "Haematology-Serum-Blood Pressure — Systolic",
    );
  });

  it("falls back to the raw bounds when the server sends no range text", () => {
    givenTree({
      display: "Glucose",
      lowNormal: 4,
      hiNormal: 9,
      obs: [{ obsDatetime: "2026-08-01 09:00:00.0", value: "5" }],
    });

    render(<Probe />);

    expect(screen.getByTestId("range")).toHaveTextContent("4 – 9");
    expect(screen.getByTestId("interpretation")).toHaveTextContent("NORMAL");
  });

  it("assesses a critical value against its own critical bound", () => {
    givenTree({
      display: "Potassium",
      range: "3.5 - 5.1",
      lowNormal: 3.5,
      hiNormal: 5.1,
      hiCritical: 6.5,
      obs: [{ obsDatetime: "2026-08-01 09:00:00.0", value: "7.2" }],
    });

    render(<Probe />);

    expect(screen.getByTestId("interpretation")).toHaveTextContent(
      "CRITICALLY_HIGH",
    );
  });
});
