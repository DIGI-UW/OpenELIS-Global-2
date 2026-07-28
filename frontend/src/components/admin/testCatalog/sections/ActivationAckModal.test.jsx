/**
 * ActivationAckModal — OGC-949 M7 / OGC-973.
 *
 * The coverage-gap acknowledgment modal (the H-03 safety gate). Verifies that
 * gaps — which arrive in DAYS from the backend — render in human-readable units
 * (the bug this guards against: rendering raw day counts as years), and that
 * acknowledging fires the parent callback.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ActivationAckModal from "./ActivationAckModal";
import messages from "../../../../languages/en.json";

const renderModal = (report, handlers = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ActivationAckModal
        open
        report={report}
        onAcknowledge={handlers.onAcknowledge || (() => {})}
        onCancel={handlers.onCancel || (() => {})}
      />
    </IntlProvider>,
  );

describe("ActivationAckModal", () => {
  it("renders neonatal day-granular gaps in days, not years", () => {
    renderModal({
      male: {
        sex: "M",
        status: "GAP",
        gaps: [{ fromAge: 1, toAge: 3 }], // days 1–3, the bilirubin window
        overlaps: [],
      },
      female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
    });
    expect(
      screen.getByText(messages["label.testCatalog.ranges.ackModal.warning"]),
    ).toBeInTheDocument();
    // 1–3 days must read in days, not "0 years".
    expect(screen.getByText("1 days – 3 days")).toBeInTheDocument();
  });

  it("renders a large gap in years", () => {
    renderModal({
      male: {
        sex: "M",
        status: "GAP",
        gaps: [{ fromAge: 0, toAge: 6570 }], // 0 to 18 years (in days)
        overlaps: [],
      },
      female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
    });
    expect(screen.getByText("0 days – 18 years")).toBeInTheDocument();
  });

  it("fires onAcknowledge when the confirm button is clicked", () => {
    const onAcknowledge = vi.fn();
    renderModal(
      {
        male: {
          sex: "M",
          status: "GAP",
          gaps: [{ fromAge: 1, toAge: 3 }],
          overlaps: [],
        },
        female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
      },
      { onAcknowledge },
    );
    fireEvent.click(
      screen.getByText(messages["label.testCatalog.ranges.ackModal.confirm"]),
    );
    expect(onAcknowledge).toHaveBeenCalledTimes(1);
  });
});
