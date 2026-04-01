import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import NotebookSampleOrder from "./NotebookSampleOrder";

const mockGenericSampleOrder = jest.fn();

jest.mock("../genericSample/GenericSampleOrder", () => (props) => {
  mockGenericSampleOrder(props);
  return null;
});

jest.mock("react-router-dom", () => ({
  useParams: () => ({ notebookId: "15", notebookEntryId: "99" }),
  useHistory: () => ({ push: jest.fn() }),
}));

describe("NotebookSampleOrder rollout", () => {
  test("reuses GenericSampleOrder shared workflow foundation", () => {
    render(<NotebookSampleOrder />);

    expect(mockGenericSampleOrder).toHaveBeenCalledWith(
      expect.objectContaining({
        showNotebookSelection: false,
        saveEndpoint: "/rest/GenericSampleOrder",
      }),
    );
  });
});
