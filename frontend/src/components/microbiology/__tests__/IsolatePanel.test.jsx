import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import IsolatePanel from "../IsolatePanel";
import messages from "../../../languages/en.json";

const renderPanel = (onCreateIsolate) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <IsolatePanel
        caseId="case-1"
        isolates={[]}
        onCreateIsolate={onCreateIsolate}
        saving={false}
      />
    </IntlProvider>,
  );

describe("IsolatePanel", () => {
  it("submits isolate creation details", async () => {
    const onCreateIsolate = vi.fn();
    renderPanel(onCreateIsolate);

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
});
