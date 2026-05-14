import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import NotebookSampleOrder from "./NotebookSampleOrder";

const mockGenericSampleOrder = vi.fn();

vi.mock("../genericSample/GenericSampleOrder", () => ({
  default: (props) => {
    mockGenericSampleOrder(props);
    return null;
  },
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useParams: () => ({ notebookId: "15", notebookEntryId: "99" }),
    useHistory: () => ({ push: vi.fn() }),
  };
});

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
