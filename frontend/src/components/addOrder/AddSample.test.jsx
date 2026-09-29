/**
 * The Add Sample step must carry exactly what it shows (OGC-1387, OGC-1388):
 * an unticked test, a replaced or cleared sample type and a stale test search
 * never leave tests in the order, and Back never brings them back.
 */
import React, { useState } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

const { utilsMock, pending } = vi.hoisted(() => ({
  utilsMock: { getFromOpenElisServer: vi.fn() },
  pending: {},
}));

vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({ configurationProperties: {} }),
}));

vi.mock("../common/CustomDatePicker", () => ({ default: () => <div /> }));
vi.mock("../common/CustomTimePicker", () => ({ default: () => <div /> }));
vi.mock("../storage/LocationPicker/LocationPickerInline", () => ({
  default: () => <div />,
}));
vi.mock("../barcodeWorkflow/LabelsSection", () => ({ default: () => <div /> }));
vi.mock("./GpsCoordinatesCapture", () => ({ default: () => <div /> }));

import AddSample from "./AddSample";

const SAMPLE_TYPES = [
  { id: "4", value: "Whole Blood" },
  { id: "106", value: "Vaginal Fluid" },
];

const TESTS_BY_TYPE = {
  4: {
    panels: [],
    tests: [
      { id: "18", name: "TMCH" },
      { id: "19", name: "CMCH" },
    ],
  },
  106: { panels: [], tests: [{ id: "500", name: "KOH prep" }] },
};

const emptySample = () => ({
  index: 0,
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId: "",
  sampleXML: null,
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
});

let latestSamples;

function Harness({ initialSamples, showStep = true, allowReferral }) {
  const [samples, setSamples] = useState(initialSamples);
  latestSamples = samples;
  return (
    <IntlProvider locale="en" messages={messages}>
      {showStep && (
        <AddSample
          samples={samples}
          setSamples={setSamples}
          error={() => null}
          domain="C"
          allowReferral={allowReferral}
        />
      )}
    </IntlProvider>
  );
}

const answerImmediately = (url, callback) => {
  if (url === "/rest/user-sample-types") {
    callback(SAMPLE_TYPES);
    return;
  }
  const match = url.match(/sample-type-tests\?sampleType=(\d+)/);
  if (match) {
    callback(TESTS_BY_TYPE[match[1]]);
    return;
  }
  if (url === "/rest/UomCreate") {
    callback({ existingUomList: [] });
    return;
  }
  callback([]);
};

const chooseSampleType = (value, index = 0) =>
  act(() => {
    fireEvent.change(document.getElementById("sampleId_" + index), {
      target: { value },
    });
  });

const searchTests = (text, index = 0) =>
  act(() => {
    fireEvent.change(document.getElementById("tests_search_" + index), {
      target: { value: text },
    });
  });

const toggleTest = (id, index = 0) =>
  act(() => {
    fireEvent.click(document.getElementById(`test_${index}_${id}`));
  });

beforeEach(() => {
  window.scrollTo = vi.fn();
  latestSamples = undefined;
  Object.keys(pending).forEach((key) => delete pending[key]);
  utilsMock.getFromOpenElisServer.mockReset();
  utilsMock.getFromOpenElisServer.mockImplementation(answerImmediately);
});

describe("AddSample test search (OGC-1387)", () => {
  test("changing the sample type clears the search and its results", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    searchTests("ch");
    expect(screen.getAllByRole("menuitem").map((li) => li.textContent)).toEqual(
      ["TMCH", "CMCH"],
    );

    chooseSampleType("106");

    expect(document.getElementById("tests_search_0")).toHaveValue("");
    expect(screen.queryAllByRole("menuitem")).toHaveLength(0);
    expect(screen.queryByLabelText("TMCH")).not.toBeInTheDocument();
    expect(screen.getByLabelText("KOH prep")).toBeInTheDocument();
  });

  test("a late answer for the previous sample type is ignored", () => {
    utilsMock.getFromOpenElisServer.mockImplementation((url, callback) => {
      const match = url.match(/sample-type-tests\?sampleType=(\d+)/);
      if (match) {
        pending[match[1]] = () => callback(TESTS_BY_TYPE[match[1]]);
        return;
      }
      answerImmediately(url, callback);
    });
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    chooseSampleType("106");

    act(() => pending["106"]());
    act(() => pending["4"]());

    expect(screen.getByLabelText("KOH prep")).toBeInTheDocument();
    expect(screen.queryByLabelText("TMCH")).not.toBeInTheDocument();
  });

  test("Select sample type clears the stored type and its test list", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("106");
    toggleTest("500");

    chooseSampleType("");

    expect(document.getElementById("sampleId_0")).toHaveValue("");
    expect(latestSamples[0].sampleTypeId).toBe("");
    expect(latestSamples[0].tests).toEqual([]);
    expect(screen.queryByLabelText("KOH prep")).not.toBeInTheDocument();
  });
});

describe("AddSample carries what the step shows (OGC-1388)", () => {
  test("unticking the only test leaves the sample with no tests", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    toggleTest("18");
    expect(latestSamples[0].tests.map((t) => t.id)).toEqual(["18"]);

    toggleTest("18");

    expect(latestSamples[0].tests).toEqual([]);
  });

  test("changing the sample type drops the previous type's tests", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    toggleTest("18");

    chooseSampleType("106");

    expect(latestSamples[0].sampleTypeId).toBe("106");
    expect(latestSamples[0].tests).toEqual([]);
  });

  test("Next then Back does not bring an unticked test back", () => {
    const { rerender } = render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    toggleTest("18");
    toggleTest("18");

    rerender(<Harness initialSamples={[emptySample()]} showStep={false} />);
    rerender(<Harness initialSamples={[emptySample()]} showStep />);

    expect(document.getElementById("sampleId_0")).toHaveValue("4");
    expect(document.getElementById("test_0_18")).not.toBeChecked();
    expect(latestSamples[0].tests).toEqual([]);
  });

  test("Next then Back keeps the tests that are still ticked", () => {
    const { rerender } = render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    toggleTest("18");
    toggleTest("19");
    toggleTest("18");

    rerender(<Harness initialSamples={[emptySample()]} showStep={false} />);
    rerender(<Harness initialSamples={[emptySample()]} showStep />);

    expect(document.getElementById("test_0_18")).not.toBeChecked();
    expect(document.getElementById("test_0_19")).toBeChecked();
    expect(latestSamples[0].tests.map((t) => t.id)).toEqual(["19"]);
  });

  test("removing the first sample keeps the second sample's ticks", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    chooseSampleType("4");
    toggleTest("18");
    act(() => {
      fireEvent.click(screen.getByRole("button", { name: /Add Sample/i }));
    });
    chooseSampleType("106", 1);
    toggleTest("500", 1);

    act(() => {
      fireEvent.click(screen.getAllByText("Remove Sample")[0]);
    });

    expect(latestSamples).toHaveLength(1);
    expect(latestSamples[0].sampleTypeId).toBe("106");
    expect(document.getElementById("sampleId_0")).toHaveValue("106");
    expect(document.getElementById("test_0_500")).toBeChecked();
  });

  test("the sample is recorded as rejected only while Reject is ticked", () => {
    render(<Harness initialSamples={[emptySample()]} />);
    expect(latestSamples[0].sampleRejected).toBe(false);

    act(() => {
      fireEvent.click(document.getElementById("reject_0"));
    });
    expect(latestSamples[0].sampleRejected).toBe(true);

    act(() => {
      fireEvent.click(document.getElementById("reject_0"));
    });
    expect(latestSamples[0].sampleRejected).toBe(false);
  });

  test("the reference-lab referral is offered unless the screen turns it off", () => {
    const { unmount } = render(<Harness initialSamples={[emptySample()]} />);
    expect(document.getElementById("useReferral_0")).toBeInTheDocument();
    unmount();

    render(<Harness initialSamples={[emptySample()]} allowReferral={false} />);
    expect(document.getElementById("useReferral_0")).not.toBeInTheDocument();
  });

  test("the samples it starts from are never modified", () => {
    const initial = emptySample();
    render(<Harness initialSamples={[initial]} />);
    chooseSampleType("4");
    toggleTest("18");

    expect(initial).toEqual(emptySample());
  });
});
