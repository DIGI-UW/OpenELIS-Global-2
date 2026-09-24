import React from "react";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { Analytics } from "@carbon/icons-react";
import QAEmptyState, { QASparseList } from "../QAEmptyState";
import { renderQa } from "../../testUtils";

describe("QAEmptyState", () => {
  test("renders the state-naming headline and subhead from real i18n keys", () => {
    renderQa(
      <QAEmptyState
        titleKey="qa.empty.tat.title"
        subheadKey="qa.empty.tat.subhead"
      />,
    );
    expect(screen.getByText("No results in this window")).toBeInTheDocument();
    expect(
      screen.getByText(/Turnaround times appear here/),
    ).toBeInTheDocument();
  });

  test("renders without a subhead when none is given, and honors a custom icon", () => {
    const { container } = renderQa(
      <QAEmptyState titleKey="qa.empty.amendment.title" icon={Analytics} />,
    );
    expect(
      screen.getByText("No amendments in this window"),
    ).toBeInTheDocument();
    expect(container.querySelectorAll(".qa-empty__subhead")).toHaveLength(0);
    // custom icon renders an svg
    expect(container.querySelector(".qa-empty__icon")).toBeInTheDocument();
  });

  test("inline size applies the inline modifier class", () => {
    const { container } = renderQa(
      <QAEmptyState size="inline" titleKey="qa.empty.attention.title" />,
    );
    expect(container.querySelector(".qa-empty--inline")).toBeInTheDocument();
  });
});

describe("QASparseList", () => {
  test("renders a count headline and one row per item", () => {
    renderQa(
      <QASparseList
        headlineKey="qa.empty.sparse.labUnits"
        headlineValues={{ count: 2 }}
        items={[
          { label: "Chemistry", value: "12h" },
          { label: "Microbiology", value: "61h" },
        ]}
      />,
    );
    expect(screen.getByText("2 lab units this period")).toBeInTheDocument();
    expect(screen.getByText("Chemistry")).toBeInTheDocument();
    expect(screen.getByText("12h")).toBeInTheDocument();
    expect(screen.getByText("Microbiology")).toBeInTheDocument();
    expect(screen.getByText("61h")).toBeInTheDocument();
  });

  test("pluralizes the headline for a single category", () => {
    renderQa(
      <QASparseList
        headlineKey="qa.empty.sparse.labUnits"
        headlineValues={{ count: 1 }}
        items={[{ label: "Chemistry", value: "12h" }]}
      />,
    );
    expect(screen.getByText("1 lab unit this period")).toBeInTheDocument();
  });
});
