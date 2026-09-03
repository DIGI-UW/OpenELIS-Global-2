import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
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

describe("EQAOrderForm deep link from My Cycles", () => {
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

describe("EQAOrderForm inbound consignment", () => {
  const PROGRAMS = [{ id: 7, programName: "CPHL National HIV Viral Load EQA" }];
  const CYCLES = [
    {
      id: 12,
      cycleName: "Round 1",
      schemeName: "CPHL National HIV Viral Load EQA",
    },
  ];
  const INBOUND = [
    {
      id: 4,
      boxId: "REF-9",
      eqaCycleId: null,
      destinationFacilityName: "Test LIMS",
    },
    {
      id: 3,
      boxId: "PCYC-1",
      eqaCycleId: 12,
      destinationFacilityName: "Test LIMS",
    },
  ];

  let latestOrder = null;
  const Harness = () => {
    const [orderFormValues, setOrderFormValues] = useState({
      sampleOrderItems: { eqaProgramId: "7", eqaCycleId: "12" },
    });
    latestOrder = orderFormValues.sampleOrderItems;
    return (
      <EQAOrderForm
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
      />
    );
  };

  const renderForm = ({ receipt } = {}) => {
    // The deep-link suite above leaves a cycleId in the query string, and the
    // form reads it on mount, so clear it before rendering this one.
    window.history.pushState({}, "", "/SamplePatientEntry?isEQA=true");
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/eqa/my-programs")) cb(PROGRAMS);
      else if (url.startsWith("/rest/eqa/cycles/mine")) cb(CYCLES);
      else if (url.startsWith("/rest/shipping-box/by-state/IN_TRANSIT"))
        cb(INBOUND);
      else if (url.startsWith("/rest/eqa/cycles/12/receipt") && receipt)
        cb(receipt);
    });
    return render(
      <IntlProvider locale="en" messages={messages}>
        <Harness />
      </IntlProvider>,
    );
  };

  test("offers the imported boxes, this cycle's first, and records the chosen one with its code", async () => {
    renderForm();
    const select = await screen.findByLabelText(
      messages["eqa.order.receipt.consignment"],
    );
    const options = Array.from(select.querySelectorAll("option")).map(
      (o) => o.textContent,
    );
    expect(options).toEqual([
      messages["eqa.order.receipt.consignment.none"],
      "PCYC-1 · this cycle — Test LIMS",
      "REF-9 — Test LIMS",
    ]);
    expect(
      screen.queryByLabelText(messages["eqa.order.receipt.reference"]),
    ).toBeNull();

    fireEvent.change(select, { target: { value: "3" } });

    expect(latestOrder.eqaShippingBoxId).toBe("3");
    expect(
      screen.getByLabelText(messages["eqa.order.receipt.reference"]),
    ).toHaveValue("PCYC-1");
  });

  test("a receipt already on file shows the consignment it took delivery of", async () => {
    renderForm({
      receipt: {
        id: 55,
        cycleId: 12,
        receivedDate: "2026-09-03",
        boxCode: "PCYC-1",
      },
    });
    expect(
      await screen.findByText(
        (_, element) =>
          element.tagName === "P" &&
          /Consignment PCYC-1 received/.test(element.textContent),
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByLabelText(messages["eqa.order.receipt.consignment"]),
    ).toBeNull();
  });
});
