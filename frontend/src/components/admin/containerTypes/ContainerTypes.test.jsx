import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));
vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import ContainerTypes from "./ContainerTypes";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

const SST = {
  id: 6,
  code: "SST",
  name: "Serum separator tube (SST)",
  category: "Blood",
  additive: "Clot activator, gel",
  material: null,
  nominalVolume: null,
  volumeUnit: null,
  capColourHex: "#D4AF37",
  capColourName: "Gold",
  standardCode: null,
  active: true,
  domains: ["CLINICAL"],
  sampleTypes: [{ id: "2", value: "Serum" }],
  samplesUsing: 3,
  testsUsing: 2,
};

const listRequests = [];

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ContainerTypes />
    </IntlProvider>,
  );

describe("Container Types (OGC-1611 P1)", () => {
  beforeEach(() => {
    cleanup();
    listRequests.length = 0;
    vi.mocked(getFromOpenElisServer).mockReset();
    vi.mocked(postToOpenElisServerFullResponse).mockReset();
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) => {
      if (url.startsWith("/rest/container-types/6/usage")) {
        return callback({ tests: [], samples: 3 });
      }
      if (url.startsWith("/rest/container-types")) {
        listRequests.push(url);
        return callback([SST]);
      }
      if (url === "/rest/test-catalog/sample-types") {
        return callback([{ id: "2", name: "Serum", domain: "CLINICAL" }]);
      }
      return callback(undefined);
    });
  });

  it("lists each container type with its domain, cap colour, yields and use", () => {
    renderPage();

    const row = screen.getByTestId("container-type-SST");
    expect(row).toHaveTextContent("Serum separator tube (SST)");
    expect(row).toHaveTextContent("Clinical");
    expect(row).toHaveTextContent("Gold");
    expect(row).toHaveTextContent("Serum");
    expect(row).toHaveTextContent("Tests: 2");
    expect(row).toHaveTextContent("Samples: 3");
    expect(row).toHaveTextContent("Active");
  });

  it("asks the server for the domain, the category and deactivated rows", () => {
    renderPage();

    fireEvent.change(screen.getByLabelText("Domain"), {
      target: { value: "VECTOR" },
    });
    fireEvent.change(screen.getByLabelText("Category"), {
      target: { value: "Blood" },
    });

    expect(listRequests.at(-1)).toBe(
      "/rest/container-types?includeInactive=false&domain=VECTOR&category=Blood",
    );
  });

  it("adds a container type and shows a refusal on the field it names", async () => {
    vi.mocked(postToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) =>
        callback({
          ok: false,
          status: 400,
          json: () => Promise.resolve({ field: "code", code: "codeTaken" }),
        }),
    );
    renderPage();

    fireEvent.click(screen.getByTestId("container-type-add"));
    fireEvent.change(screen.getByLabelText("Name *"), {
      target: { value: "QA vial" },
    });
    fireEvent.change(screen.getByLabelText("Code *"), {
      target: { value: "sst" },
    });
    fireEvent.click(screen.getByTestId("container-type-save"));

    const [url, body] = vi.mocked(postToOpenElisServerFullResponse).mock
      .calls[0];
    expect(url).toBe("/rest/container-types");
    expect(JSON.parse(body)).toMatchObject({
      code: "SST",
      name: "QA vial",
      domains: ["CLINICAL"],
      nominalVolume: null,
      capColourHex: null,
    });
    expect(
      await screen.findByText("Another container type has this code."),
    ).toBeInTheDocument();
  });

  it("warns how many samples use a container type before deactivating it", async () => {
    vi.mocked(postToOpenElisServerFullResponse).mockImplementation(
      (url, body, callback) => callback({ ok: true }),
    );
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    expect(await screen.findByTestId("container-type-usage")).toHaveTextContent(
      "Used by 0 tests and 3 samples. Existing samples keep it; it can no longer be proposed or chosen.",
    );
    fireEvent.click(screen.getByRole("button", { name: "danger Deactivate" }));
    await waitFor(() =>
      expect(vi.mocked(postToOpenElisServerFullResponse).mock.calls[0][0]).toBe(
        "/rest/container-types/6/deactivate",
      ),
    );
  });

  it("names the one test expecting a container type, in the singular", async () => {
    const answer = vi.mocked(getFromOpenElisServer).getMockImplementation();
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) =>
      url.endsWith("/usage")
        ? callback({ tests: ["Creatinine(Serum)"], samples: 1 })
        : answer(url, callback),
    );
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    expect(await screen.findByTestId("container-type-usage")).toHaveTextContent(
      "Used by 1 test (Creatinine(Serum)) and 1 sample. Existing samples keep it; it can no longer be proposed or chosen.",
    );
  });

  it("says it could not count what uses a container type when the count fails", async () => {
    const answer = vi.mocked(getFromOpenElisServer).getMockImplementation();
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) =>
      url.endsWith("/usage") ? callback(undefined) : answer(url, callback),
    );
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    expect(await screen.findByTestId("container-type-usage")).toHaveTextContent(
      "The tests and samples using it could not be counted.",
    );
    expect(screen.getByTestId("container-type-usage")).not.toHaveTextContent(
      "0 samples",
    );
  });

  it("says the list could not be loaded instead of showing it empty", () => {
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) =>
      callback(undefined),
    );
    renderPage();

    expect(screen.getByTestId("container-types-load-failed")).toHaveTextContent(
      "The container types could not be loaded.",
    );
    expect(
      screen.queryByText("No container types match."),
    ).not.toBeInTheDocument();
  });

  it("limits each text field to its column length", () => {
    renderPage();
    fireEvent.click(screen.getByTestId("container-type-add"));

    expect(screen.getByLabelText("Name *")).toHaveAttribute("maxLength", "120");
    expect(screen.getByLabelText("Code *")).toHaveAttribute("maxLength", "40");
  });
});
