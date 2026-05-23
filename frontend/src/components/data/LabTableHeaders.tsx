import React from "react";
import { FormattedMessage } from "react-intl";
import type { ReactNode } from "react";

interface TableHeader {
  key: string;
  header: ReactNode;
}

export const labHeaderData: TableHeader[] = [
  {
    key: "formName",
    header: <FormattedMessage id="header.formname" />,
  },
  {
    key: "firstName",
    header: <FormattedMessage id="patient.first.name" />,
  },
  {
    key: "lastName",
    header: <FormattedMessage id="patient.last.name" />,
  },
];
