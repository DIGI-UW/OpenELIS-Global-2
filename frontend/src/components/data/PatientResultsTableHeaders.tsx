import { FormattedMessage } from "react-intl";
import React from "react";

interface TableHeader {
  key: string;
  header: React.ReactNode;
}

export const patientSearchHeaderData: TableHeader[] = [
  {
    key: "lastName",
    header: <FormattedMessage id="patient.last.name" />,
  },
  {
    key: "firstName",
    header: <FormattedMessage id="patient.first.name" />,
  },
  {
    key: "gender",
    header: <FormattedMessage id="patient.gender" />,
  },
  {
    key: "dob",
    header: <FormattedMessage id="patient.dob" />,
  },
  {
    key: "subjectNumber",
    header: <FormattedMessage id="patient.subject.number" />,
  },
  {
    key: "nationalId",
    header: <FormattedMessage id="patient.natioanalid" />,
  },
  {
    key: "dataSourceName",
    header: <FormattedMessage id="patient.dataSourceName" />,
  },
];
