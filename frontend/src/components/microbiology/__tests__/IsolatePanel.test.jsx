import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
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
    const user = userEvent.setup();
    const onCreateIsolate = vi.fn();
    renderPanel({ onCreateIsolate });

    await user.type(screen.getByLabelText("Gram stain"), "Gram negative rod");
    await user.type(
      screen.getByLabelText("Colony morphology"),
      "Lactose fermenting",
    );
    await user.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(onCreateIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        gramStain: "Gram negative rod",
        colonyMorphology: "Lactose fermenting",
        significance: "CLINICALLY_SIGNIFICANT",
      }),
    );
  });

  it("updates isolate identification from reusable organism data", async () => {
    const user = userEvent.setup();
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

    await user.click(
      await screen.findByRole("button", { name: "Update identification" }),
    );
    await user.selectOptions(screen.getByLabelText("Organism"), "organism-1");
    await user.type(screen.getByLabelText("ID method"), "MALDI-TOF");
    await user.type(screen.getByLabelText("ID confidence (%)"), "98");
    await user.click(
      screen.getByRole("button", { name: "Save identification" }),
    );

    expect(onUpdateIdentification).toHaveBeenCalledWith("isolate-1", {
      organismId: "organism-1",
      preliminaryOrganismText: "Gram negative rod",
      significance: "UNKNOWN",
      identificationStatus: "CONFIRMED",
      identificationMethod: "MALDI-TOF",
      identificationConfidence: "98",
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
