import React from "react";
import { FormattedMessage } from "react-intl";
import type { TableHeaderData } from "./LabTableHeaders";

export const sampleTypesTableHeader: TableHeaderData[] = [
  {
    key: "select_checkBox",
    header: "",
  },
  {
    key: "_id",
    header: <FormattedMessage id="header.ID" />,
  },
  {
    key: "sampleType",
    header: <FormattedMessage id="sample.type" />,
  },
  {
    key: "collectionDate",
    header:
      String(<FormattedMessage id="sample.collection.date" />) +
      "(dd/mm/yyyy) ",
  },
  {
    key: "collectionTime",
    header:
      String(<FormattedMessage id="sample.collection.time" />) + "(hh:mm) ",
  },
  {
    key: "collector",
    header: <FormattedMessage id="collector.label" />,
  },
  {
    key: "reject",
    header: <FormattedMessage id="header.reject" />,
  },
  {
    key: "rejectReason",
    header: <FormattedMessage id="header.rejection.reason" />,
  },
  {
    key: "removeSample",
    header: "",
  },
];
