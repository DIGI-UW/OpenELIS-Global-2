import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
vi.mock("../../../common/useDomains", () => ({
  default: () => [{ id: "CLINICAL", labelKey: "label.domain.CLINICAL" }],
}));

// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../../utils/Utils";
// eslint-disable-next-line import/first
import AssignedTestsSection from "./AssignedTestsSection";

/**
 * OGC-1459 review: a bulk reassign may move tests into an inactive unit
 * (decision D4), so the dialog marks inactive destinations and says what moving
 * there means, instead of promising new orders route there.
 */
describe("AssignedTestsSection reassign dialog", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.endsWith("/tests")) {
        cb([{ id: "t1", name: "Glucose", domain: "CLINICAL", active: true }]);
      } else if (url.endsWith("/lab-units-management")) {
        cb({
          success: true,
          data: [
            { id: "5", name: "Biochemistry", isActive: true },
            { id: "8", name: "Chemistry", isActive: true },
            { id: "9", name: "Parasitology", isActive: false },
          ],
        });
      } else {
        cb([]);
      }
    });
  });

  it("marks an inactive destination and warns that its tests cannot be ordered", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <AssignedTestsSection labUnitId="5" />
      </IntlProvider>,
    );
    fireEvent.click(await screen.findByText("Glucose"));
    fireEvent.click(document.getElementById("lu-test-select-t1"));
    fireEvent.click(screen.getByRole("button", { name: /Reassign selected/ }));

    const options = Array.from(
      document.querySelectorAll("#lu-reassign-destination option"),
    ).map((option) => option.textContent);
    expect(options).toContain("Chemistry");
    expect(options).toContain("Parasitology (inactive)");

    fireEvent.change(document.getElementById("lu-reassign-destination"), {
      target: { value: "8" },
    });
    expect(
      screen.getByText(messages["label.labUnit.tests.reassign.explain"]),
    ).toBeInTheDocument();

    fireEvent.change(document.getElementById("lu-reassign-destination"), {
      target: { value: "9" },
    });
    expect(
      screen.getByText(
        messages["label.labUnit.tests.reassign.inactiveDestination"],
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["label.labUnit.tests.reassign.explain"]),
    ).toBeNull();
  });
});
