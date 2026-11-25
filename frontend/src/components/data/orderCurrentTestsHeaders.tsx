import type { IntlShape } from "react-intl";
import type { TableHeaderData } from "./LabTableHeaders";

export const OrderCurrentTestsHeaders = (
  intl: IntlShape,
): TableHeaderData[] => [
  {
    key: "accessionNumber",
    header: intl.formatMessage({ id: "sample.label.labnumber" }),
  },
  {
    key: "sampleType",
    header: intl.formatMessage({ id: "sample.type" }),
  },
  {
    key: "collectionDate",
    header: intl.formatMessage({ id: "sample.collection.date" }),
  },
  {
    key: "collectionTime",
    header: intl.formatMessage({ id: "sample.collection.time" }),
  },
  {
    key: "removeSample",
    header: intl.formatMessage({ id: "sample.remove.action" }),
  },
  {
    key: "testName",
    header: intl.formatMessage({ id: "sample.entry.project.testName" }),
  },
  {
    key: "hasResults",
    header: intl.formatMessage({ id: "header.results.recorded" }),
  },
  {
    key: "canceled",
    header: intl.formatMessage({ id: "header.cancel.test" }),
  },
];
