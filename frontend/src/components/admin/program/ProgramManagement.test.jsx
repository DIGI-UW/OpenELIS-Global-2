import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

const { getFromOpenElisServer, postToOpenElisServerFullResponse, notify } =
  vi.hoisted(() => ({
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    notify: vi.fn(),
  }));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
}));

vi.mock("../../layout/Layout", async () => {
  const ReactModule = await import("react");
  return {
    NotificationContext: ReactModule.createContext({
      notificationVisible: false,
      setNotificationVisible: vi.fn(),
      addNotification: notify,
    }),
  };
});

import ProgramManagement from "./ProgramManagement";

const cytologyQuestionnaire = {
  resourceType: "Questionnaire",
  status: "active",
  item: [
    {
      linkId: "q1",
      text: "Nature of Specimen",
      type: "choice",
      answerOption: [{ valueString: "Conventional smear" }],
    },
  ],
};

const serve = (url, callback) => {
  if (url === "/rest/domains") {
    callback([
      { id: "CLINICAL", labelKey: "label.domain.CLINICAL" },
      { id: "ENVIRONMENTAL", labelKey: "label.domain.ENVIRONMENTAL" },
      { id: "VECTOR", labelKey: "label.domain.VECTOR" },
    ]);
  } else if (url === "/rest/program-list") {
    callback([
      {
        id: "5",
        name: "Cytology",
        code: "CYTO",
        domain: "CLINICAL",
        active: true,
        labUnitIds: ["165"],
      },
      {
        id: "12",
        name: "Water Quality",
        code: "WQ",
        domain: "ENVIRONMENTAL",
        active: false,
        labUnitIds: ["228", "229"],
      },
    ]);
  } else if (url === "/rest/lab-units-management") {
    callback({
      success: true,
      data: [
        {
          id: "165",
          name: "Cytopathology",
          domain: "CLINICAL",
          isActive: true,
        },
        {
          id: "228",
          name: "Kualitas Air",
          domain: "ENVIRONMENTAL",
          isActive: true,
        },
        {
          id: "229",
          name: "Air Quality",
          domain: "ENVIRONMENTAL",
          isActive: true,
        },
      ],
    });
  } else if (url === "/rest/program/5") {
    callback({
      program: {
        id: "5",
        programName: "Cytology",
        code: "CYTO",
        questionnaireUUID: "1dd212a2-5da1-41a5-9304-cad5f900c998",
      },
      domain: "CLINICAL",
      active: true,
      labUnitIds: ["165"],
      testSectionId: "165",
      additionalOrderEntryQuestions: cytologyQuestionnaire,
    });
  } else if (url === "/rest/program/5/orderCount") {
    callback({ programId: "5", count: 0 });
  } else if (url === "/rest/program/12/orderCount") {
    callback({ programId: "12", count: 35 });
  } else {
    callback(undefined);
  }
};

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={["/MasterListsPage/program"]}>
      <IntlProvider locale="en" messages={messages}>
        <ProgramManagement />
      </IntlProvider>
    </MemoryRouter>,
  );

const lastPostedPayload = () => {
  const calls = postToOpenElisServerFullResponse.mock.calls;
  return JSON.parse(calls[calls.length - 1][1]);
};

const rowNamed = (name) =>
  screen.getByText(name, { selector: "td" }).closest("tr");

describe("ProgramManagement", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockReset();
    notify.mockReset();
    getFromOpenElisServer.mockImplementation(serve);
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ status: 200 }),
    );
  });

  it("lists active programs with domain, status and every lab unit, and reveals deactivated ones on demand", async () => {
    renderPage();

    const cytology = (
      await screen.findByText("Cytology", { selector: "td" })
    ).closest("tr");
    expect(within(cytology).getByText("Clinical")).toBeInTheDocument();
    expect(within(cytology).getByText("Active")).toBeInTheDocument();
    expect(within(cytology).getByText("Cytopathology")).toBeInTheDocument();
    expect(
      screen.queryByText("Water Quality", { selector: "td" }),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("switch"));

    const water = (
      await screen.findByText("Water Quality", { selector: "td" })
    ).closest("tr");
    expect(within(water).getByText("Environmental")).toBeInTheDocument();
    expect(within(water).getByText("Inactive")).toBeInTheDocument();
    expect(
      within(water).getByText("Kualitas Air, Air Quality"),
    ).toBeInTheDocument();
  });

  it("stages a domain change of an existing program behind a confirmation and never touches its questionnaire", async () => {
    renderPage();
    await screen.findByText("Cytology", { selector: "td" });

    fireEvent.click(
      within(rowNamed("Cytology")).getByRole("button", {
        name: "Edit program",
      }),
    );
    const question = await screen.findByDisplayValue("Nature of Specimen");
    expect(question).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Environmental"));
    const heading = await screen.findByText("Change Program Domain?");
    expect(heading).toBeInTheDocument();
    expect(screen.getByDisplayValue("Nature of Specimen")).toBeInTheDocument();

    fireEvent.click(
      within(heading.closest(".cds--modal")).getByRole("button", {
        name: "Cancel",
      }),
    );
    await waitFor(() =>
      expect(
        screen.queryByText("Change Program Domain?"),
      ).not.toBeInTheDocument(),
    );
    expect(screen.getByLabelText("Clinical")).toBeChecked();

    fireEvent.click(screen.getByLabelText("Environmental"));
    const reopened = await screen.findByText("Change Program Domain?");
    fireEvent.click(
      within(reopened.closest(".cds--modal")).getByRole("button", {
        name: "Confirm",
      }),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("Environmental")).toBeChecked(),
    );
    expect(screen.getByDisplayValue("Nature of Specimen")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const payload = lastPostedPayload();
    expect(payload.program).toMatchObject({
      id: "5",
      programName: "Cytology",
      code: "CYTO",
      questionnaireUUID: "1dd212a2-5da1-41a5-9304-cad5f900c998",
    });
    expect(payload.domain).toBe("ENVIRONMENTAL");
    expect(payload.active).toBe(true);
    expect(payload.labUnitIds).toEqual(["165"]);
    expect(payload.additionalOrderEntryQuestions.item).toHaveLength(1);
    expect(payload.additionalOrderEntryQuestions.item[0].answerOption).toEqual([
      { valueString: "Conventional smear" },
    ]);
  });

  it("sends a choice question with no answer options as having none, instead of silently restoring the saved ones", async () => {
    renderPage();
    await screen.findByText("Cytology", { selector: "td" });

    fireEvent.click(
      within(rowNamed("Cytology")).getByRole("button", {
        name: "Edit program",
      }),
    );
    await screen.findByDisplayValue("Nature of Specimen");

    fireEvent.click(screen.getByRole("button", { name: "Delete option" }));
    await screen.findByText(
      "No options yet — add at least one so reception can pick a value.",
    );

    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const item = lastPostedPayload().additionalOrderEntryQuestions.item[0];
    expect(item.type).toBe("choice");
    expect(item.answerOption).toBeUndefined();
  });

  it("confirms a deactivation with the order count, sends only the lifecycle flip and says so in the toast", async () => {
    renderPage();
    await screen.findByText("Cytology", { selector: "td" });

    fireEvent.click(
      within(rowNamed("Cytology")).getByRole("button", { name: "Options" }),
    );
    fireEvent.click(await screen.findByText("Deactivate"));

    expect(
      await screen.findByText("Deactivate this Program?"),
    ).toBeInTheDocument();
    expect(
      await screen.findByText(/"Cytology" has no orders/),
    ).toBeInTheDocument();

    const dialog = screen
      .getByText("Deactivate this Program?")
      .closest(".cds--modal");
    fireEvent.click(within(dialog).getByRole("button", { name: /Deactivate/ }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(lastPostedPayload()).toEqual({
      program: { id: "5", programName: "Cytology", code: "CYTO" },
      active: false,
    });
    expect(notify).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Cytology deactivated" }),
    );
    await waitFor(() =>
      expect(
        screen.queryByText("Cytology", { selector: "td" }),
      ).not.toBeInTheDocument(),
    );
  });

  it("saves a new program only once it has a name and a domain, active and with the chosen fields", async () => {
    renderPage();
    await screen.findByText("Cytology", { selector: "td" });

    fireEvent.click(screen.getByRole("button", { name: "Add Program" }));
    const panel = screen.getByText("New Program").closest(".cds--tile");
    const submit = within(panel).getByRole("button", { name: "Submit" });
    expect(submit).toBeDisabled();

    fireEvent.change(within(panel).getByLabelText("Program Name"), {
      target: { value: "Dengue Sentinel" },
    });
    expect(submit).toBeDisabled();
    fireEvent.change(within(panel).getByLabelText("Programme Code"), {
      target: { value: "DENGUE" },
    });
    fireEvent.click(within(panel).getByLabelText("Vector"));
    expect(submit).toBeEnabled();

    fireEvent.click(submit);

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const payload = lastPostedPayload();
    expect(payload.program).toEqual({
      id: "",
      programName: "Dengue Sentinel",
      code: "DENGUE",
    });
    expect(payload.domain).toBe("VECTOR");
    expect(payload.active).toBe(true);
    expect(payload.labUnitIds).toEqual([]);
    expect(payload.additionalOrderEntryQuestions).toEqual({
      resourceType: "Questionnaire",
      item: [],
    });
  });
});
