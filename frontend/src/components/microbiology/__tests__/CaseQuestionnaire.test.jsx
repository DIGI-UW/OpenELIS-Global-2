import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import CaseQuestionnaire from "../CaseQuestionnaire";
import messages from "../../../languages/en.json";

const questionnaire = {
  resourceType: "Questionnaire",
  item: [
    {
      linkId: "exposure",
      text: "Recent antibiotic exposure",
      type: "boolean",
      required: true,
    },
    {
      linkId: "drug",
      text: "Antibiotic name",
      type: "string",
      required: true,
      enableWhen: [
        { question: "exposure", operator: "=", answerBoolean: true },
      ],
    },
  ],
};
const show = (canWrite = true, definition = questionnaire, response = null) => {
  const data = {
    programId: "p",
    questionnaireJson: JSON.stringify(definition),
    version: "revision-1",
    responseJson: response ? JSON.stringify(response) : null,
  };
  const service = {
    getQuestionnaire: vi.fn().mockResolvedValue(data),
    saveQuestionnaire: vi
      .fn()
      .mockResolvedValue({ ...data, version: "revision-2" }),
  };
  const onSaved = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseQuestionnaire
        detail={{ id: "c", programId: "p", canWrite }}
        service={service}
        onSaved={onSaved}
      />
    </IntlProvider>,
  );
  return { service, onSaved };
};
it("saves conditional answers with the loaded revision and preserves false as an answer", async () => {
  const { service, onSaved } = show();
  const exposure = await screen.findByLabelText(/Recent antibiotic exposure/);
  expect(screen.queryByLabelText(/Antibiotic name/)).not.toBeInTheDocument();
  fireEvent.change(exposure, { target: { value: "true" } });
  fireEvent.change(screen.getByLabelText(/Antibiotic name/), {
    target: { value: "Ampicillin" },
  });
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  );
  await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
  const first = service.saveQuestionnaire.mock.calls[0][1];
  expect(first.version).toBe("revision-1");
  expect(JSON.parse(first.responseJson).item).toEqual([
    {
      linkId: "exposure",
      text: "Recent antibiotic exposure",
      answer: [{ valueBoolean: true }],
    },
    {
      linkId: "drug",
      text: "Antibiotic name",
      answer: [{ valueString: "Ampicillin" }],
    },
  ]);
  fireEvent.change(exposure, { target: { value: "false" } });
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  );
  await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(2));
  const next = service.saveQuestionnaire.mock.calls[1][1];
  expect(next.version).toBe("revision-2");
  expect(JSON.parse(next.responseJson).item).toEqual([
    {
      linkId: "exposure",
      text: "Recent antibiotic exposure",
      answer: [{ valueBoolean: false }],
    },
  ]);
});
it("keeps unsaved answers after a stale-response conflict", async () => {
  const { service, onSaved } = show();
  service.saveQuestionnaire.mockRejectedValue(
    new Error("MICROBIOLOGY_QUESTIONNAIRE_CHANGED"),
  );
  fireEvent.change(await screen.findByLabelText(/Recent antibiotic exposure/), {
    target: { value: "false" },
  });
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  );
  expect(
    await screen.findByText(messages["microbiology.case.questionSaveError"]),
  ).toBeInTheDocument();
  expect(screen.getByLabelText(/Recent antibiotic exposure/)).toHaveValue(
    "false",
  );
  expect(onSaved).not.toHaveBeenCalled();
});
it("outside-unit readers cannot edit or submit answers", async () => {
  show(false);
  expect(
    await screen.findByLabelText(/Recent antibiotic exposure/),
  ).toBeDisabled();
  expect(
    screen.queryByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  ).not.toBeInTheDocument();
});

it("places nested questions under their parent answer in the FHIR response", async () => {
  const { service } = show(true, {
    resourceType: "Questionnaire",
    item: [
      {
        linkId: "parent",
        text: "Parent",
        type: "boolean",
        item: [{ linkId: "child", text: "Child", type: "string" }],
      },
    ],
  });
  fireEvent.change(await screen.findByLabelText("Parent"), {
    target: { value: "true" },
  });
  fireEvent.change(screen.getByLabelText("Child"), {
    target: { value: "nested answer" },
  });
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  );
  await waitFor(() =>
    expect(service.saveQuestionnaire).toHaveBeenCalledTimes(1),
  );
  const item = JSON.parse(
    service.saveQuestionnaire.mock.calls[0][1].responseJson,
  ).item[0];
  expect(item.item).toBeUndefined();
  expect(item.answer[0].item).toEqual([
    {
      linkId: "child",
      text: "Child",
      answer: [{ valueString: "nested answer" }],
    },
  ]);
});

it("does not overwrite repeated group responses with a flattened form", async () => {
  const { service } = show(true, {
    resourceType: "Questionnaire",
    item: [
      {
        linkId: "group",
        text: "Repeated group",
        type: "group",
        repeats: true,
        item: [{ linkId: "child", text: "Child", type: "string" }],
      },
    ],
  });
  await screen.findByLabelText("Child");
  expect(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  ).toBeDisabled();
  expect(service.saveQuestionnaire).not.toHaveBeenCalled();
});

const coding = {
  system: "urn:specimen",
  code: "known",
  display: "Known source",
};
const openChoice = (repeats = false) => ({
  resourceType: "Questionnaire",
  item: [
    {
      linkId: "source",
      text: "Source",
      type: "open-choice",
      repeats,
      answerOption: [{ valueCoding: coding }],
    },
  ],
});
const submitQuestions = () =>
  fireEvent.click(
    screen.getByRole("button", {
      name: messages["microbiology.case.saveQuestions"],
    }),
  );
it("saves free text and predefined coding for an open-choice question", async () => {
  const { service } = show(true, openChoice());
  fireEvent.change(await screen.findByLabelText("Source"), {
    target: { value: "custom" },
  });
  fireEvent.change(
    screen.getByLabelText(messages["microbiology.case.otherAnswer"]),
    { target: { value: "Different source" } },
  );
  submitQuestions();
  await waitFor(() =>
    expect(service.saveQuestionnaire).toHaveBeenCalledTimes(1),
  );
  expect(
    JSON.parse(service.saveQuestionnaire.mock.calls[0][1].responseJson).item[0]
      .answer,
  ).toEqual([{ valueString: "Different source" }]);
  fireEvent.change(screen.getByLabelText("Source"), { target: { value: "0" } });
  submitQuestions();
  await waitFor(() =>
    expect(service.saveQuestionnaire).toHaveBeenCalledTimes(2),
  );
  expect(
    JSON.parse(service.saveQuestionnaire.mock.calls[1][1].responseJson).item[0]
      .answer,
  ).toEqual([{ valueCoding: coding }]);
});
it("preserves coded selections while editing repeated free-text answers", async () => {
  const { service } = show(true, openChoice(true), {
    resourceType: "QuestionnaireResponse",
    item: [
      {
        linkId: "source",
        answer: [{ valueCoding: coding }, { valueString: "First source" }],
      },
    ],
  });
  const other = await screen.findByLabelText(
    messages["microbiology.case.otherAnswers"],
  );
  expect(other).toHaveValue("First source");
  fireEvent.change(other, { target: { value: "First source\n" } });
  expect(other).toHaveValue("First source\n");
  fireEvent.change(other, {
    target: { value: "First source\nSecond source\n" },
  });
  submitQuestions();
  await waitFor(() => expect(service.saveQuestionnaire).toHaveBeenCalled());
  expect(
    JSON.parse(service.saveQuestionnaire.mock.calls[0][1].responseJson).item[0]
      .answer,
  ).toEqual([
    { valueCoding: coding },
    { valueString: "First source" },
    { valueString: "Second source" },
  ]);
});
it("shows saved free text read-only outside the owning unit", async () => {
  show(false, openChoice(), {
    resourceType: "QuestionnaireResponse",
    item: [{ linkId: "source", answer: [{ valueString: "Saved source" }] }],
  });
  const other = await screen.findByLabelText(
    messages["microbiology.case.otherAnswer"],
  );
  expect(other).toHaveValue("Saved source");
  expect(other).toBeDisabled();
});
it("accepts open-choice text when no predefined answers exist", async () => {
  const definition = openChoice();
  definition.item[0].answerOption = [];
  const { service } = show(true, definition);
  fireEvent.change(await screen.findByLabelText("Source"), {
    target: { value: "Unlisted" },
  });
  submitQuestions();
  await waitFor(() => expect(service.saveQuestionnaire).toHaveBeenCalled());
  expect(
    JSON.parse(service.saveQuestionnaire.mock.calls[0][1].responseJson).item[0]
      .answer,
  ).toEqual([{ valueString: "Unlisted" }]);
});
