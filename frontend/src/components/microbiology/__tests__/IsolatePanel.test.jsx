import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import IsolatePanel from "../IsolatePanel";
import messages from "../../../languages/en.json";

const renderPanel = ({
  isolates = [],
  onCreateIsolate = vi.fn(),
  onUpdateIdentification = vi.fn(),
  readOnly = false,
  service = { getOrganisms: vi.fn().mockResolvedValue([]) },
} = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <IsolatePanel
        caseId="case-1"
        isolates={isolates}
        onCreateIsolate={onCreateIsolate}
        onUpdateIdentification={onUpdateIdentification}
        readOnly={readOnly}
        service={service}
        saving={false}
      />
    </IntlProvider>,
  );

describe("IsolatePanel", () => {
  it("submits isolate creation details", async () => {
    const onCreateIsolate = vi.fn();
    renderPanel({ onCreateIsolate });

    fireEvent.change(screen.getByLabelText("Preliminary organism"), {
      target: { value: "Escherichia coli" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(onCreateIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        preliminaryOrganismText: "Escherichia coli",
        significance: "CLINICALLY_SIGNIFICANT",
      }),
    );
  });

  it("updates isolate identification from reusable organism data", async () => {
    const onUpdateIdentification = vi.fn();
    renderPanel({
      isolates: [
        {
          id: "isolate-1",
          isolateLabel: "ISO-1",
          preliminaryOrganismText: "Gram negative rod",
          significance: "UNKNOWN",
          identificationStatus: "PRELIMINARY",
        },
      ],
      onUpdateIdentification,
      service: {
        getOrganisms: vi
          .fn()
          .mockResolvedValue([{ id: "organism-1", label: "Escherichia coli" }]),
      },
    });

    fireEvent.click(
      await screen.findByRole("button", { name: "Update identification" }),
    );
    fireEvent.change(screen.getByLabelText("Organism"), {
      target: { value: "organism-1" },
    });
    fireEvent.change(screen.getByLabelText("Identification status"), {
      target: { value: "CONFIRMED" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Save identification" }),
    );

    expect(onUpdateIdentification).toHaveBeenCalledWith("isolate-1", {
      organismId: "organism-1",
      preliminaryOrganismText: "Gram negative rod",
      significance: "UNKNOWN",
      identificationStatus: "CONFIRMED",
    });
  });

  it("disables isolate mutation controls for a final case", async () => {
    renderPanel({
      isolates: [
        {
          id: "isolate-1",
          isolateLabel: "ISO-1",
          preliminaryOrganismText: "Escherichia coli",
          significance: "CLINICALLY_SIGNIFICANT",
          identificationStatus: "CONFIRMED",
        },
      ],
      readOnly: true,
    });

    expect(
      await screen.findByRole("button", { name: "Update identification" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Create isolate" }),
    ).toBeDisabled();
  });
});
