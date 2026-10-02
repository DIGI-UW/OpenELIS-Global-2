import React, { useState } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQAOrderForm, { eqaReceiptNoteMissing } from "../EQAOrderForm";
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
      status: "PLANNED",
      schemeName: "CPHL National HIV Viral Load EQA",
    },
    {
      id: 13,
      cycleName: "Round 2",
      status: "PLANNED",
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

describe("EQAOrderForm cycle picker", () => {
  const CYCLES = [
    {
      id: 21,
      cycleName: "Awaiting panel",
      status: "PLANNED",
      schemeType: "INTERNATIONAL_PT",
      plannedEndDate: "2026-10-15",
    },
    {
      id: 22,
      cycleName: "Testing",
      status: "TESTING",
      schemeType: "REGIONAL_PT",
      plannedEndDate: "2026-11-02",
    },
    {
      id: 23,
      cycleName: "Submitted",
      status: "SUBMITTED",
      schemeType: "INTERNATIONAL_PT",
    },
    {
      id: 24,
      cycleName: "Closed",
      status: "CLOSED",
      schemeType: "INTERNATIONAL_PT",
    },
    {
      id: 25,
      cycleName: "Bench blind",
      status: "PLANNED",
      schemeType: "IN_HOUSE",
    },
    {
      id: 26,
      cycleName: "Provider round",
      status: "SUBMISSIONS_OPEN",
      schemeType: "REGIONAL_PT",
    },
  ];

  let latestOrder = null;
  const Harness = ({ initial = {} }) => {
    const [orderFormValues, setOrderFormValues] = useState({
      sampleOrderItems: initial,
    });
    latestOrder = orderFormValues.sampleOrderItems;
    return (
      <EQAOrderForm
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
      />
    );
  };

  const renderForm = (search = "?isEQA=true", initial) => {
    window.history.pushState({}, "", `/SamplePatientEntry${search}`);
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/eqa/my-programs")) cb([]);
      else if (url.startsWith("/rest/eqa/cycles/mine")) cb(CYCLES);
    });
    return render(
      <IntlProvider locale="en" messages={messages}>
        <Harness initial={initial} />
      </IntlProvider>,
    );
  };

  const cycleOptions = async () => {
    const select = await screen.findByLabelText(messages["eqa.order.cycle"]);
    return Array.from(select.querySelectorAll("option")).map(
      (o) => o.textContent,
    );
  };

  test("lists only external cycles still open for results", async () => {
    renderForm();
    await screen.findByRole("option", { name: "Testing" });
    expect(await cycleOptions()).toEqual(["", "Awaiting panel", "Testing"]);
  });

  test("picking a cycle fills the result deadline from that cycle", async () => {
    renderForm();
    const select = await screen.findByLabelText(messages["eqa.order.cycle"]);
    await screen.findByRole("option", { name: "Testing" });

    fireEvent.change(select, { target: { value: "22" } });
    expect(latestOrder.eqaDeadline).toBe("11/02/2026");

    fireEvent.change(select, { target: { value: "21" } });
    expect(latestOrder.eqaDeadline).toBe("10/15/2026");
  });

  test("the My Cycles deep link fills the deadline too", async () => {
    renderForm("?isEQA=true&cycleId=21");
    expect(
      await screen.findByLabelText(messages["eqa.order.cycle"]),
    ).toHaveValue("21");
    expect(latestOrder.eqaDeadline).toBe("10/15/2026");
  });

  test("a deadline already on the order survives the deep link", async () => {
    renderForm("?isEQA=true&cycleId=21", { eqaDeadline: "10/01/2026" });
    expect(
      await screen.findByLabelText(messages["eqa.order.cycle"]),
    ).toHaveValue("21");
    expect(latestOrder.eqaDeadline).toBe("10/01/2026");
  });
});

describe("EQAOrderForm inbound consignment", () => {
  const PROGRAMS = [{ id: 7, programName: "CPHL National HIV Viral Load EQA" }];
  const CYCLES = [
    {
      id: 12,
      cycleName: "Round 1",
      status: "PLANNED",
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

describe("EQAOrderForm panel integrity", () => {
  const PROGRAMS = [{ id: 7, programName: "CPHL National HIV Viral Load EQA" }];
  const CYCLES = [
    {
      id: 12,
      cycleName: "Round 1",
      status: "PLANNED",
      schemeName: "CPHL National HIV Viral Load EQA",
    },
  ];

  const Harness = () => {
    const [orderFormValues, setOrderFormValues] = useState({
      sampleOrderItems: {
        isEQASample: true,
        eqaProgramId: "7",
        eqaCycleId: "12",
      },
    });
    return (
      <EQAOrderForm
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
      />
    );
  };

  const renderForm = () => {
    window.history.pushState({}, "", "/SamplePatientEntry?isEQA=true");
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/eqa/my-programs")) cb(PROGRAMS);
      else if (url.startsWith("/rest/eqa/cycles/mine")) cb(CYCLES);
      else if (url.startsWith("/rest/shipping-box/by-state/IN_TRANSIT")) cb([]);
    });
    return render(
      <IntlProvider locale="en" messages={messages}>
        <Harness />
      </IntlProvider>,
    );
  };

  // The one receipt an accreditation record needs prose on is the
  // one saying the material arrived compromised.
  test("unticking intact demands the note, and filling it clears the alert", async () => {
    renderForm();
    const alert = messages["eqa.order.receipt.notes.required"];

    expect(await screen.findByLabelText("Integrity notes")).toBeInTheDocument();
    expect(screen.queryByText(alert)).toBeNull();

    fireEvent.click(screen.getByLabelText("Panel arrived intact"));
    expect(screen.getByText(alert)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Integrity notes"), {
      target: { value: "Two vials cracked in transit" },
    });
    expect(screen.queryByText(alert)).toBeNull();

    // Whitespace is not an explanation.
    fireEvent.change(screen.getByLabelText("Integrity notes"), {
      target: { value: "   " },
    });
    expect(screen.getByText(alert)).toBeInTheDocument();
  });
});

describe("eqaReceiptNoteMissing", () => {
  const order = (sampleOrderItems) => ({ sampleOrderItems });

  test("holds only an EQA receipt that says not-intact with no note", () => {
    expect(
      eqaReceiptNoteMissing(
        order({ isEQASample: true, eqaIntegrityOk: false }),
      ),
    ).toBe(true);
    expect(
      eqaReceiptNoteMissing(
        order({
          isEQASample: true,
          eqaIntegrityOk: false,
          eqaIntegrityNotes: "Cracked",
        }),
      ),
    ).toBe(false);
    // Intact, unanswered, and non-EQA orders are none of this rule's business.
    expect(
      eqaReceiptNoteMissing(order({ isEQASample: true, eqaIntegrityOk: true })),
    ).toBe(false);
    expect(eqaReceiptNoteMissing(order({ isEQASample: true }))).toBe(false);
    expect(eqaReceiptNoteMissing(order({ eqaIntegrityOk: false }))).toBe(false);
    expect(eqaReceiptNoteMissing(undefined)).toBe(false);
  });
});
