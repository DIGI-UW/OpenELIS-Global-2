import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQAOrderForm from "../EQAOrderForm";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});
vi.mock("../../common/CustomDatePicker", () => ({
  default: function MockDatePicker() {
    return <div data-testid="datepicker" />;
  },
}));

const PROGRAMS = [
  { id: 7, programName: "CPHL National HIV Viral Load EQA" },
  { id: 8, programName: "CPHL National HIV Serology EQA" },
];
const CYCLES = [
  {
    id: 12,
    cycleName: "Round 1",
    schemeName: "CPHL National HIV Viral Load EQA",
  },
  {
    id: 13,
    cycleName: "Round 2",
    schemeName: "CPHL National HIV Serology EQA",
  },
];

const Harness = () => {
  const [orderFormValues, setOrderFormValues] = useState({
    sampleOrderItems: {},
  });
  return (
    <EQAOrderForm
      orderFormValues={orderFormValues}
      setOrderFormValues={setOrderFormValues}
    />
  );
};

const renderForm = (search) => {
  window.history.pushState({}, "", `/SamplePatientEntry${search}`);
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/eqa/my-programs")) cb(PROGRAMS);
    else if (url.startsWith("/rest/eqa/cycles/mine")) cb(CYCLES);
  });
  return render(
    <IntlProvider locale="en" messages={messages}>
      <Harness />
    </IntlProvider>,
  );
};

const cycleSelect = () => screen.findByLabelText(messages["eqa.order.cycle"]);
const programmeSelect = () =>
  screen.findByLabelText(messages["eqa.order.programme"]);

describe("EQAOrderForm deep link from My Cycles", () => {
  test("preselects the linked cycle and the enrollment whose programme matches its scheme", async () => {
    renderForm("?isEQA=true&cycleId=12");
    expect(await cycleSelect()).toHaveValue("12");
    expect(await programmeSelect()).toHaveValue("7");
  });

  test("an explicit enrollmentId on the link wins over the name match", async () => {
    renderForm("?isEQA=true&cycleId=12&enrollmentId=8");
    expect(await cycleSelect()).toHaveValue("12");
    expect(await programmeSelect()).toHaveValue("8");
  });

  test("without a linked cycle both selects start empty", async () => {
    renderForm("?isEQA=true");
    expect(await cycleSelect()).toHaveValue("");
    expect(await programmeSelect()).toHaveValue("");
  });
});
