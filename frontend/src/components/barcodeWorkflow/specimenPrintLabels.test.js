import { buildSpecimenPrintLabels } from "./specimenPrintLabels";

describe("buildSpecimenPrintLabels", () => {
  it("returns one specimen label per sample item, addressed as labNo.sortOrder", () => {
    const labels = buildSpecimenPrintLabels("CY-100", ["1", "2", "3"]);

    expect(labels).toHaveLength(3);
    expect(labels.map((label) => label.printUrl)).toEqual([
      "/LabelMakerServlet?labNo=CY-100.1&type=specimen&quantity=1",
      "/LabelMakerServlet?labNo=CY-100.2&type=specimen&quantity=1",
      "/LabelMakerServlet?labNo=CY-100.3&type=specimen&quantity=1",
    ]);
    labels.forEach((label) => {
      expect(label.labelType).toBe("specimen");
      expect(label.quantity).toBe(1);
    });
  });

  it("never emits a bare labNo, which the servlet reads as every sample item", () => {
    const labels = buildSpecimenPrintLabels("CY-100", ["1", "2"]);

    labels.forEach((label) => {
      expect(label.printUrl).not.toContain("labNo=CY-100&");
    });
  });

  it("URL-encodes the accession number", () => {
    const [label] = buildSpecimenPrintLabels("AB 12/3", ["1"]);

    expect(label.printUrl).toBe(
      "/LabelMakerServlet?labNo=AB%2012%2F3.1&type=specimen&quantity=1",
    );
  });

  it("returns the sort order as the numeric sampleNumber when it is a number", () => {
    const labels = buildSpecimenPrintLabels("CY-100", ["2", "5"]);

    expect(labels.map((label) => label.sampleNumber)).toEqual([2, 5]);
  });

  it.each([[undefined], [null], [[]]])(
    "returns no specimen label when the case has no sample items (%p)",
    (sortOrders) => {
      expect(buildSpecimenPrintLabels("CY-100", sortOrders)).toEqual([]);
    },
  );

  it("skips blank sort orders instead of printing for the whole accession", () => {
    const labels = buildSpecimenPrintLabels("CY-100", ["1", "", null, "2"]);

    expect(labels.map((label) => label.printUrl)).toEqual([
      "/LabelMakerServlet?labNo=CY-100.1&type=specimen&quantity=1",
      "/LabelMakerServlet?labNo=CY-100.2&type=specimen&quantity=1",
    ]);
  });
});
