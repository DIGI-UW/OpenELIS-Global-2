import React from "react";
import {
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const enabled = (value) => value === true || value === "true";

const CaseInfoSummary = ({ orderDetail = {} }) => {
  const intl = useIntl();
  const display = (value) =>
    value || intl.formatMessage({ id: "microbiology.caseInfo.notProvided" });
  const booleanDisplay = (value) =>
    intl.formatMessage({
      id: enabled(value)
        ? "microbiology.caseInfo.yes"
        : "microbiology.caseInfo.no",
    });

  return (
    <StructuredListWrapper
      isCondensed
      isFlush
      ariaLabel={intl.formatMessage({ id: "microbiology.caseInfo.summary" })}
    >
      <StructuredListBody>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.clinicalHistory",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {display(orderDetail.clinicalHistory)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.patientOrigin",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {display(formatMicrobiologyEnum(orderDetail.patientOrigin))}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.numberOfSets",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {display(orderDetail.numberOfSets)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.antibioticExposure",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {booleanDisplay(orderDetail.antibioticExposure)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.criticalNotificationPreference",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {booleanDisplay(orderDetail.criticalNotificationPreference)}
          </StructuredListCell>
        </StructuredListRow>
      </StructuredListBody>
    </StructuredListWrapper>
  );
};

export default CaseInfoSummary;
