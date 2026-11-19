import React from "react";
import { vi } from "vitest";
import { act, render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import WorkplanSearchForm from "../WorkplanSearchForm";

const { requests, server } = vi.hoisted(() => ({
  requests: [],
  server: { dropped: false },
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: (url, callback) => {
    requests.push(url);
    const page = new URL(url, "http://x").searchParams.get("page");
    if (!page) {
      server.dropped = false;
      return callback({
        workplanTests: [{ accessionNumber: "ACC-1" }],
        paging: { currentPage: "1", totalPages: "2" },
      });
    }
    if (server.dropped) return callback({ workplanTests: [] });
    callback({
      workplanTests: [{ accessionNumber: `ACC-${page}` }],
      paging: { currentPage: page, totalPages: "2" },
    });
  },
}));

vi.mock("../TestSelectForm", () => ({ default: () => null }));

// OGC-1647: a session keeps a few workplans; the page of one dropped since came
// back empty and the screen said no tests were found.
describe("Workplan search form paging", () => {
  it("runs the search again before reading a page of a workplan the session dropped", async () => {
    const shown = vi.fn();
    let loadPage;
    render(
      <IntlProvider locale="en" messages={messages}>
        <WorkplanSearchForm
          type="test"
          selectedValue={vi.fn()}
          selectedLabel={vi.fn()}
          createTestsList={shown}
          registerPageLoader={(loader) => {
            loadPage = loader;
          }}
          registerPageSource={vi.fn()}
        />
      </IntlProvider>,
    );
    await waitFor(() => expect(loadPage).toBeTruthy());
    requests.length = 0;
    server.dropped = true;

    act(() => loadPage(2));

    await waitFor(() =>
      expect(shown.mock.lastCall[0].workplanTests[0].accessionNumber).toBe(
        "ACC-2",
      ),
    );
    expect(requests).toEqual([
      "/rest/WorkPlanByTest?test_id=&page=2",
      "/rest/WorkPlanByTest?test_id=",
      "/rest/WorkPlanByTest?test_id=&page=2",
    ]);
  });
});
