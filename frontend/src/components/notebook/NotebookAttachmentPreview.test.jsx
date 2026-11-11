import React from "react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import NotebookAttachmentPreview from "./NotebookAttachmentPreview";
import messages from "../../languages/en.json";

beforeEach(() => {
  vi.stubGlobal("URL", {
    createObjectURL: vi.fn(() => "blob:notebook"),
    revokeObjectURL: vi.fn(),
  });
  vi.spyOn(window, "open").mockImplementation(() => null);
});
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
const show = (file) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotebookAttachmentPreview file={file} />
    </IntlProvider>,
  );
it("previews an unsaved upload from its data URL as inert text", () => {
  show({
    base64File:
      "data:text/plain;base64," + btoa("<script>not executed</script>"),
    fileType: "text/html",
  });
  fireEvent.click(screen.getByRole("button"));
  expect(URL.createObjectURL.mock.calls[0][0].type).toBe("text/plain");
  expect(window.open).toHaveBeenCalledWith(
    "blob:notebook",
    "_blank",
    "noopener,noreferrer",
  );
  expect(document.querySelector("script")).toBeNull();
});
it("supports the historical raw MIME spelling for stored supported files", () => {
  show({ fileType: "image/png", fileData: btoa("stored bytes") });
  fireEvent.click(screen.getByRole("button"));
  expect(URL.createObjectURL.mock.calls[0][0].type).toBe("image/png");
});
it("does not open legacy executable metadata", () => {
  show({
    fileType: 'image/png" onload="alert(1)',
    fileData: btoa("stored bytes"),
  });
  fireEvent.click(screen.getByRole("button"));
  expect(window.open).not.toHaveBeenCalled();
  expect(
    screen.getByText(messages["clinicalAttachment.previewUnavailable"]),
  ).toBeTruthy();
});
