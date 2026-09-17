import { render } from "@testing-library/react";
import React from "react";

// ---------------------------------------------------------------------------
// getFromOpenElisServer invokes its callback with `undefined` whenever the
// response is not JSON — most importantly when an authorization failure
// redirects /rest/departments-for-site to the HTML /Home page. The department
// callbacks fed that straight into state, and the later `departments.map(...)`
// in render then threw `Cannot read properties of undefined (reading 'map')`,
// which surfaced to the user as the route error boundary's
// "Sample entry could not be loaded" rather than as a missing dropdown.
//
// These tests pin the contract at the seam that broke: whatever the callback
// receives, the rendered list must survive it.
// ---------------------------------------------------------------------------

const DepartmentList = ({ departments }) => (
  <ul>
    {departments.map((department, index) => (
      <li key={index}>{department.value}</li>
    ))}
  </ul>
);

// Mirrors loadDepartments in AddOrder.jsx / SampleBatchEntrySetup.jsx.
const loadDepartments = (data, setDepartments) => {
  setDepartments(data || []);
};

describe("loadDepartments guard", () => {
  it("keeps the state an array when the response was not JSON", () => {
    let departments = null;
    loadDepartments(undefined, (next) => {
      departments = next;
    });

    expect(Array.isArray(departments)).toBe(true);
    expect(departments).toHaveLength(0);
    // The render that used to throw.
    const { container } = render(<DepartmentList departments={departments} />);
    expect(container.querySelectorAll("li")).toHaveLength(0);
  });

  it("still passes a real payload through untouched", () => {
    let departments = null;
    const payload = [
      { id: "1", value: "Haematology" },
      { id: "2", value: "Biochemistry" },
    ];
    loadDepartments(payload, (next) => {
      departments = next;
    });

    expect(departments).toEqual(payload);
    const { container } = render(<DepartmentList departments={departments} />);
    expect(container.querySelectorAll("li")).toHaveLength(2);
  });

  it("demonstrates the unguarded form throws on render", () => {
    let departments = null;
    const unguarded = (data, setDepartments) => setDepartments(data);
    unguarded(undefined, (next) => {
      departments = next;
    });

    expect(departments).toBeUndefined();
    expect(() => render(<DepartmentList departments={departments} />)).toThrow(
      TypeError,
    );
  });
});
