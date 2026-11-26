// LabelMakerServlet reads type=specimen with a bare accession as "print one
// label for every sample item", so a case with N sample items would print N
// labels per request. Addressing each item as labNo.<sortOrder> prints exactly
// one label per item. Mirrors BarcodeWorkflowPrintServiceImpl on the backend.
export const buildSpecimenPrintLabels = (
  accessionNumber,
  sampleItemSortOrders,
) =>
  (Array.isArray(sampleItemSortOrders) ? sampleItemSortOrders : [])
    .filter((sortOrder) => sortOrder !== null && sortOrder !== "")
    .filter((sortOrder) => sortOrder !== undefined)
    .map((sortOrder) => {
      const sampleNumber = Number(sortOrder);
      return {
        labelType: "specimen",
        sampleNumber: Number.isInteger(sampleNumber) ? sampleNumber : null,
        quantity: 1,
        printUrl: `/LabelMakerServlet?labNo=${encodeURIComponent(
          `${accessionNumber}.${sortOrder}`,
        )}&type=specimen&quantity=1`,
      };
    });
