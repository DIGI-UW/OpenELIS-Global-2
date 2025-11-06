import { FormattedMessage } from "react-intl";
export const sampleTypesTableHeader = [
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
    header: <FormattedMessage id="sample.collection.date" /> + "(dd/mm/yyyy) ",
  },
  {
    key: "collectionTime",
    header: <FormattedMessage id="sample.collection.time" /> + "(hh:mm) ",
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
