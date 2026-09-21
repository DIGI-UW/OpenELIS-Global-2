import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, it, expect, vi } from "vitest";
import messages from "../../languages/en.json";
import PatientHeader from "./PatientHeader";

// The avatar fetches the patient's photo from the server. Nothing here is
// about photos, so it is the one thing stubbed; every other child renders for
// real.
vi.mock("../patient/photoManagement/photoAvatar/AyncAvatar", () => ({
  default: ({ patientName }) => <div data-testid="avatar">{patientName}</div>,
}));

const renderHeader = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PatientHeader
        id="42"
        firstName="Ada"
        lastName="Lovelace"
        gender="F"
        dob="12/10/1815"
        {...props}
      />
    </IntlProvider>,
  );

describe("PatientHeader", () => {
  it("renders the status node it is handed, whatever that node is", () => {
    renderHeader({
      statusTag: <span data-testid="case-status">Ready</span>,
    });

    expect(screen.getByTestId("case-status")).toHaveTextContent("Ready");
  });

  it("names each assigned member of staff with their role and their name", () => {
    renderHeader({
      assignedStaff: [
        { roleKey: "assigned.pathologist.label", name: "Dr Okello" },
        { roleKey: "assigned.technician.label", name: "Mary N." },
      ],
    });

    const staffLines = screen.getAllByTestId("case-assigned-staff");

    expect(staffLines).toHaveLength(2);
    expect(staffLines[0]).toHaveTextContent(
      `${messages["assigned.pathologist.label"]}: Dr Okello`,
    );
    expect(staffLines[1]).toHaveTextContent(
      `${messages["assigned.technician.label"]}: Mary N.`,
    );
  });

  // A nullable list on a server DTO drills straight into this prop, and a
  // destructuring default does not cover an explicit null. Reading its length
  // would throw and take the whole patient band down with it.
  it("survives an explicit null for the assigned staff", () => {
    const { container } = renderHeader({ assignedStaff: null });

    expect(container.querySelector(".patient-name")).toHaveTextContent(
      "Lovelace Ada",
    );
    expect(screen.queryAllByTestId("case-assigned-staff")).toHaveLength(0);
  });

  // This component is rendered by six screens — the pathology, cytology and
  // immunohistochemistry case views, Modify Order, the results viewer and the
  // generic program case view — so the two new props have to be inert for all
  // of them: with neither prop the markup must be what it was before they
  // existed, the details column still spanning the full width beside the
  // avatar and no third column present at all.
  it("leaves the layout untouched for the screens that pass neither new prop", () => {
    const { container } = renderHeader();

    expect(
      container.querySelector(".cds--lg\\:col-span-15"),
    ).toBeInTheDocument();
    expect(container.querySelector(".cds--lg\\:col-span-11")).toBeNull();
    expect(container.querySelector(".cds--lg\\:col-span-4")).toBeNull();
  });

  // The inversion of the guard above: the same query must report a different
  // layout once the props are given, otherwise the guard could pass on a
  // component that had stopped laying anything out at all.
  it("narrows the details column and adds a third one once a status is given", () => {
    const { container } = renderHeader({
      statusTag: <span data-testid="case-status">Ready</span>,
    });

    expect(
      container.querySelector(".cds--lg\\:col-span-11"),
    ).toBeInTheDocument();
    expect(
      container.querySelector(".cds--lg\\:col-span-4"),
    ).toBeInTheDocument();
    expect(container.querySelector(".cds--lg\\:col-span-15")).toBeNull();
  });

  // A malformed entry is dropped rather than rendered, because this band sits
  // above a patient's identity on every screen that shows it.
  it("renders no line, and so no dangling separator, for an entry that carries no name", () => {
    const { container } = renderHeader({
      assignedStaff: [
        { roleKey: "assigned.pathologist.label" },
        { roleKey: "assigned.technician.label", name: "Mary N." },
      ],
    });

    const staffLines = screen.getAllByTestId("case-assigned-staff");

    expect(staffLines).toHaveLength(1);
    expect(staffLines[0]).toHaveTextContent(
      `${messages["assigned.technician.label"]}: Mary N.`,
    );
    expect(container).not.toHaveTextContent(
      messages["assigned.pathologist.label"],
    );
  });

  // Same guard, the other missing field: without a roleKey the line would
  // reach formatMessage with an undefined id and print the literal string
  // "undefined" next to the patient's name.
  it("renders no line, and so no literal undefined, for an entry that carries no roleKey", () => {
    const { container } = renderHeader({
      assignedStaff: [
        { name: "Dr Okello" },
        { roleKey: "assigned.technician.label", name: "Mary N." },
      ],
    });

    const staffLines = screen.getAllByTestId("case-assigned-staff");

    expect(staffLines).toHaveLength(1);
    expect(staffLines[0]).toHaveTextContent(
      `${messages["assigned.technician.label"]}: Mary N.`,
    );
    expect(container).not.toHaveTextContent("undefined");
  });

  it("still says there is no patient when there is no patient, whatever the new props say", () => {
    const { container } = render(
      <IntlProvider locale="en" messages={messages}>
        <PatientHeader
          statusTag={<span data-testid="case-status">Ready</span>}
          assignedStaff={[
            { roleKey: "assigned.pathologist.label", name: "Dr Okello" },
          ]}
        />
      </IntlProvider>,
    );

    expect(
      screen.getByText(messages["patient.label.nopatientid"]),
    ).toBeInTheDocument();
    expect(container).not.toHaveTextContent("Dr Okello");
    expect(screen.queryByTestId("case-status")).toBeNull();
  });
});
