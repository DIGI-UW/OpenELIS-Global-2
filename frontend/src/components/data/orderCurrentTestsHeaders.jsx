import { FormattedMessage } from "react-intl";
import React from "react";
export const OrderCurrentTestsHeaders = [
  {
    key: "accessionNumber",
    header: <FormattedMessage id="sample.label.labnumber" />,
  },
  {
    key: "sampleType",
    header: <FormattedMessage id="sample.type" />,
  },
  {
    key: "collectionDate",
    header: <FormattedMessage id="sample.collection.date" />,
  },
  {
    key: "collectionTime",
    header: <FormattedMessage id="sample.collection.time" />,
  },
  {
    key: "removeSample",
    header: <FormattedMessage id="sample.remove.action" />,
  },
  {
    key: "testName",
    header: <FormattedMessage id="sample.entry.project.testName" />,
  },
  {
    key: "hasResults",
    header: <FormattedMessage id="header.results.recorded" />,
  },
  {
    key: "canceled",
    header: <FormattedMessage id="header.cancel.test" />,
  },
];

export const OrderPossibleTestsHeaders = [
  {
    key: "accessionNumber",
    header: <FormattedMessage id="sample.label.labnumber" />,
  },
  {
    key: "sampleType",
    header: <FormattedMessage id="sample.type" />,
  },
  {
    key: "testName",
    header: <FormattedMessage id="sample.entry.project.testName" />,
  },
  {
    key: "add",
    header: <FormattedMessage id="header.assign" />,
  },
];
