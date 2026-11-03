import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

import { getFromOpenElisServer } from "../utils/Utils";
import PrePrint from "./PrePrint";

describe("PrePrint", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-sample-types") {
        callback([{ id: "2", value: "Serum" }]);
      } else if (url.startsWith("/rest/sample-type-tests")) {
        callback({ tests: [{ id: "4", name: "Creatinine" }], panels: [] });
      }
    });
  });

  test("pre-printed labels load from the label servlet, not from an app page", async () => {
    const { container } = render(
      <IntlProvider locale="en" messages={messages}>
        <PrePrint />
      </IntlProvider>,
    );

    fireEvent.change(container.querySelector("#selectSampleType"), {
      target: { value: "2" },
    });
    fireEvent.click(await screen.findByLabelText("Creatinine"));
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["barcode.print.preprint.button"],
      }),
    );

    await waitFor(() =>
      expect(container.querySelector("iframe")).toBeInTheDocument(),
    );
    const source = container.querySelector("iframe").getAttribute("src");
    expect(source).toMatch(/^\/api\/OpenELIS-Global\/LabelMakerServlet\?/);
    expect(source).toContain("prePrinting=true");
    expect(source).toContain("testIds=4");
  });
});
