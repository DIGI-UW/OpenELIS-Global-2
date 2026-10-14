import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import IsolatePanel from "../IsolatePanel";
import service from "../CaseWorkspaceService";
import messages from "../../../languages/en.json";

vi.mock("../CaseWorkspaceService", () => ({ default: { getCase: vi.fn() } }));
vi.mock("../CaseIsolateWorkspace", () => ({
  default: ({ detail, onChanged }) => (
    <>
      <output>
        {detail.canWrite ? "editable workspace" : "read-only workspace"}
      </output>
      <button onClick={onChanged}>Save isolate</button>
    </>
  ),
}));
const show = (onChanged = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <IsolatePanel caseId="case-1" onChanged={onChanged} />
    </IntlProvider>,
  );
it("loads current case permissions before opening the shared workspace", async () => {
  service.getCase.mockResolvedValue({ id: "case-1", canWrite: false });
  show();
  expect(await screen.findByText("read-only workspace")).toBeInTheDocument();
  expect(screen.queryByText("editable workspace")).not.toBeInTheDocument();
});
it("refreshes its host after the shared workspace saves", async () => {
  service.getCase.mockResolvedValue({ id: "case-1", canWrite: true });
  const changed = vi.fn();
  show(changed);
  fireEvent.click(await screen.findByRole("button", { name: "Save isolate" }));
  expect(changed).toHaveBeenCalledTimes(1);
});
it("shows a load error instead of opening a workspace with assumed rights", async () => {
  service.getCase.mockRejectedValue(new Error("denied"));
  show();
  expect(
    await screen.findByText(messages["microbiology.case.loadError"]),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Save isolate" }),
  ).not.toBeInTheDocument();
});
