import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import { IntlProvider } from "react-intl";
import "@testing-library/jest-dom";
import messages from "../../../languages/en.json";
import ClinicalAttachmentPreview from "./ClinicalAttachmentPreview";
const show = (attachment, allowText = false) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ClinicalAttachmentPreview
        attachment={attachment}
        allowText={allowText}
      />
    </IntlProvider>,
  );
beforeEach(() => {
  vi.stubGlobal("URL", {
    createObjectURL: vi.fn(() => "blob:synthetic-attachment"),
    revokeObjectURL: vi.fn(),
  });
  vi.spyOn(window, "open").mockImplementation(() => null);
});
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
describe("clinical attachment previews", () => {
  it.each(["image/png", "image/jpeg", "application/pdf"])(
    "opens %s as a typed file without an opener or HTML document",
    (type) => {
      const write = vi.spyOn(document, "write");
      const view = show({
        fileType: `data:${type}`,
        image: btoa("file bytes"),
      });
      fireEvent.click(screen.getByRole("button"));
      expect(URL.createObjectURL).toHaveBeenCalledOnce();
      const blob = URL.createObjectURL.mock.calls[0][0];
      expect(blob.type).toBe(type);
      expect(blob.size).toBe(10);
      expect(window.open).toHaveBeenCalledWith(
        "blob:synthetic-attachment",
        "_blank",
        "noopener,noreferrer",
      );
      expect(write).not.toHaveBeenCalled();
      act(() => view.unmount());
      expect(URL.revokeObjectURL).toHaveBeenCalledWith(
        "blob:synthetic-attachment",
      );
    },
  );
  it.each([
    "data:application/pdf;charset=utf-8",
    "data:APPLICATION/PDF; charset=UTF-8",
  ])(
    "previews historical parameterized PDF %s with a fixed media type",
    (fileType) => {
      show({ fileType, image: btoa("%PDF-synthetic") });
      fireEvent.click(screen.getByRole("button"));
      expect(URL.createObjectURL.mock.calls[0][0].type).toBe("application/pdf");
      expect(window.open).toHaveBeenCalledWith(
        "blob:synthetic-attachment",
        "_blank",
        "noopener,noreferrer",
      );
    },
  );
  it.each([
    "data:text/html;charset=utf-8",
    "data:image/svg+xml;charset=utf-8",
    "data:text/html",
    "data:image/svg+xml",
    'data:image/png"><script>window.opener.alert(1)</script>',
    "javascript:alert(1)",
    "__proto__",
    "toString",
  ])("refuses stored unsafe metadata: %s", (fileType) => {
    show({ fileType, image: btoa("untrusted content") });
    fireEvent.click(screen.getByRole("button"));
    expect(window.open).not.toHaveBeenCalled();
    expect(URL.createObjectURL).not.toHaveBeenCalled();
    expect(
      screen.getByText(messages["clinicalAttachment.previewUnavailable"]),
    ).toBeInTheDocument();
    expect(document.querySelector("script")).toBeNull();
  });
  it("opens notebook markup-looking text as inert plain text", () => {
    show(
      { fileType: "data:text/plain", image: btoa("<script>alert(1)</script>") },
      true,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(URL.createObjectURL.mock.calls[0][0].type).toBe("text/plain");
    expect(window.open).toHaveBeenCalledWith(
      "blob:synthetic-attachment",
      "_blank",
      "noopener,noreferrer",
    );
    expect(document.querySelector("script")).toBeNull();
  });
  it("does not broaden clinical uploads to text", () => {
    show({ fileType: "data:text/plain", image: btoa("text") });
    fireEvent.click(screen.getByRole("button"));
    expect(window.open).not.toHaveBeenCalled();
  });
  it("shows an error for a malformed stored file", () => {
    show({ fileType: "data:image/png", image: "???" });
    fireEvent.click(screen.getByRole("button"));
    expect(window.open).not.toHaveBeenCalled();
    expect(
      screen.getByText(messages["clinicalAttachment.previewUnavailable"]),
    ).toBeInTheDocument();
  });
});
