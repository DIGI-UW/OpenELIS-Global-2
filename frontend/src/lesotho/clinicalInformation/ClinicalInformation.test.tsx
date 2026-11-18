import React, { useState } from "react";
import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ClinicalInformation, {
  type ClinicalInformationValues,
  formsForSex,
  type RequestForm,
} from "./ClinicalInformation";

// Holds the values like Add Order does, so typing re-renders. `forms` stands
// in for the forms Add Order works out from the order's tests.
const Harness = ({
  sex = "",
  forms = [],
  initial = {},
  onValues = () => {},
}: {
  sex?: string;
  forms?: RequestForm[];
  initial?: ClinicalInformationValues;
  onValues?: (v: ClinicalInformationValues) => void;
}) => {
  const [values, setValues] = useState<ClinicalInformationValues>(initial);
  return (
    <IntlProvider locale="en" messages={messages}>
      <ClinicalInformation
        values={values}
        forms={forms}
        patientSex={sex}
        onChange={(next) => {
          setValues(next);
          onValues(next);
        }}
      />
    </IntlProvider>
  );
};

const tick = (label: string) => fireEvent.click(screen.getByLabelText(label));

describe("ClinicalInformation", () => {
  it("shows only the shared fields when no form applies, with no form checkboxes", () => {
    render(<Harness />);
    expect(screen.getByLabelText("Clinical history")).toBeInTheDocument();
    expect(screen.getByLabelText("Current treatment")).toBeInTheDocument();
    expect(screen.getByLabelText("Previous treatment")).toBeInTheDocument();
    expect(screen.queryAllByRole("checkbox")).toHaveLength(0);
    expect(
      screen.queryByTestId("clinical-info-viral-load"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("clinical-info-transfusion"),
    ).not.toBeInTheDocument();
  });

  it("no longer asks for the clinician's name or signature", () => {
    render(<Harness forms={["DNA_PCR"]} />);
    expect(screen.queryByLabelText("Clinician's name")).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Request signed by the clinician"),
    ).not.toBeInTheDocument();
  });

  it("shows only the fields of the forms it is given, and keeps typed values when a form goes away", () => {
    let latest: ClinicalInformationValues = {};
    const { rerender } = render(
      <Harness forms={["DNA_PCR"]} onValues={(v) => (latest = v)} />,
    );
    expect(screen.getByTestId("clinical-info-pcr")).toBeInTheDocument();
    expect(
      screen.queryByTestId("clinical-info-viral-load"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("clinical-info-transfusion"),
    ).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Under 5 clinic number"), {
      target: { value: "U5-123" },
    });
    rerender(<Harness forms={[]} onValues={(v) => (latest = v)} />);
    expect(screen.queryByTestId("clinical-info-pcr")).not.toBeInTheDocument();
    expect(latest.pcr?.under5Number).toBe("U5-123");
  });

  it("offers cervical cytology and pregnancies only when the patient is not male", () => {
    expect(formsForSex("M")).not.toContain("CERVICAL_CYTOLOGY");
    expect(formsForSex("F")).toContain("CERVICAL_CYTOLOGY");
    const both: RequestForm[] = ["CERVICAL_CYTOLOGY", "BLOOD_TRANSFUSION"];
    const { unmount } = render(<Harness sex="M" forms={both} />);
    expect(
      screen.queryByTestId("clinical-info-cervical"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Number of pregnancies"),
    ).not.toBeInTheDocument();
    unmount();
    render(<Harness sex="F" forms={both} />);
    expect(screen.getByTestId("clinical-info-cervical")).toBeInTheDocument();
    expect(screen.getByLabelText("Number of pregnancies")).toBeInTheDocument();
  });

  it("asks for the reaction only after a previous transfusion, and its type only after a reaction", () => {
    render(<Harness forms={["BLOOD_TRANSFUSION"]} />);
    expect(screen.queryByText("Any reaction?")).not.toBeInTheDocument();
    fireEvent.click(screen.getByLabelText("Yes"));
    expect(screen.getByText("Any reaction?")).toBeInTheDocument();
    expect(screen.queryByLabelText("Type of reaction")).not.toBeInTheDocument();
    fireEvent.click(screen.getAllByLabelText("Yes")[1]);
    expect(screen.getByLabelText("Type of reaction")).toBeInTheDocument();
  });

  it("clears the reaction when the previous-transfusion answer changes to No", () => {
    let latest: ClinicalInformationValues = {};
    render(
      <Harness
        forms={["BLOOD_TRANSFUSION"]}
        initial={{
          transfusion: {
            previousTransfusion: "Y",
            reaction: "Y",
            reactionType: "Fever",
          },
        }}
        onValues={(v) => (latest = v)}
      />,
    );
    fireEvent.click(screen.getAllByLabelText("No")[0]);
    expect(latest.transfusion).toMatchObject({
      previousTransfusion: "N",
      reaction: "",
      reactionType: "",
    });
    expect(screen.queryByText("Any reaction?")).not.toBeInTheDocument();
  });

  it("asks for the units and time of each blood component that is ticked", () => {
    render(<Harness forms={["BLOOD_TRANSFUSION"]} />);
    expect(
      screen.queryByTestId("clinical-info-component-PACKED_CELLS"),
    ).not.toBeInTheDocument();
    tick("Packed cells");
    expect(
      screen.getByTestId("clinical-info-component-PACKED_CELLS"),
    ).toBeInTheDocument();
    tick("Other");
    expect(
      screen.getByLabelText("Other component (specify)"),
    ).toBeInTheDocument();
  });

  it("asks what to specify only for contraception and therapy", () => {
    render(<Harness sex="F" forms={["CERVICAL_CYTOLOGY"]} />);
    expect(
      screen.queryByLabelText("Therapy (specify)"),
    ).not.toBeInTheDocument();
    tick("Therapy");
    expect(screen.getByLabelText("Therapy (specify)")).toBeInTheDocument();
  });

  it("asks about pregnancy and breastfeeding on a viral load request only when the patient is not male", () => {
    const { unmount } = render(<Harness sex="M" forms={["VIRAL_LOAD"]} />);
    expect(screen.getByLabelText("ART number")).toBeInTheDocument();
    expect(screen.queryByText("Currently pregnant?")).not.toBeInTheDocument();
    unmount();
    render(<Harness sex="F" forms={["VIRAL_LOAD"]} />);
    expect(screen.getByText("Currently pregnant?")).toBeInTheDocument();
    expect(screen.getByText("Currently breastfeeding?")).toBeInTheDocument();
  });

  it("stores the reason for viral load picked", () => {
    let latest: ClinicalInformationValues = {};
    render(<Harness forms={["VIRAL_LOAD"]} onValues={(v) => (latest = v)} />);
    fireEvent.click(screen.getByLabelText("Repeat after EAC"));
    expect(latest.viralLoad?.reason).toBe("REPEAT_AFTER_EAC");
  });

  it("shows the DNA-PCR fields, and asks about breastfeeding now only for a baby ever breastfed", () => {
    render(<Harness forms={["DNA_PCR"]} />);
    expect(screen.getByTestId("clinical-info-pcr")).toBeInTheDocument();
    expect(screen.getByLabelText("Confirmatory test")).toBeInTheDocument();
    expect(screen.getByLabelText("Mother's ART number")).toBeInTheDocument();
    expect(
      screen.queryByText("Is the baby breastfeeding now?"),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByLabelText("Yes", {
        selector: "#clinical-info-everBreastfed-Y",
      }),
    );
    expect(
      screen.getByText("Is the baby breastfeeding now?"),
    ).toBeInTheDocument();
    expect(
      screen.queryByLabelText("Date of last breastfeed"),
    ).not.toBeInTheDocument();
    fireEvent.click(
      screen.getByLabelText("No", {
        selector: "#clinical-info-breastfeedingNow-N",
      }),
    );
    expect(
      screen.getByLabelText("Date of last breastfeed"),
    ).toBeInTheDocument();
  });

  it("clears the breastfeeding answers when ever-breastfed changes away from Yes", () => {
    let latest: ClinicalInformationValues = {};
    render(
      <Harness
        forms={["DNA_PCR"]}
        initial={{
          pcr: {
            everBreastfed: "Y",
            breastfeedingNow: "N",
            lastBreastfeedDate: "01/09/2026",
          },
        }}
        onValues={(v) => (latest = v)}
      />,
    );
    fireEvent.click(
      screen.getByLabelText("No", {
        selector: "#clinical-info-everBreastfed-N",
      }),
    );
    expect(latest.pcr).toMatchObject({
      everBreastfed: "N",
      breastfeedingNow: "",
      lastBreastfeedDate: "",
    });
    expect(
      screen.queryByLabelText("Date of last breastfeed"),
    ).not.toBeInTheDocument();
  });

  it("shows the TB programme fields when the TB form applies", () => {
    render(<Harness forms={["TB"]} />);
    expect(screen.getByLabelText("Patient TB number")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Diagnostic (not on TB treatment)"),
    ).toBeInTheDocument();
  });
});
