import React from "react";
import {
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  formatCulturePurpose,
  formatMicrobiologyEnum,
} from "./MicrobiologyLabels";

const enabled = (value) => value === true || value === "true";

export const CaseInfoCompactSummary = ({
  accessionNumber,
  requestingLocation,
  orderDetail = {},
}) => {
  const intl = useIntl();
  const detail = orderDetail || {};
  const exposure =
    detail.antibioticExposure == null
      ? intl.formatMessage({ id: "microbiology.caseInfo.notProvided" })
      : intl.formatMessage({
          id: enabled(detail.antibioticExposure)
            ? "microbiology.caseInfo.yes"
            : "microbiology.caseInfo.no",
        });
  const values = [
    accessionNumber &&
      `${intl.formatMessage({
        id: "microbiology.case.accessionNumber",
      })}: ${accessionNumber}`,
    detail.patientOrigin &&
      `${intl.formatMessage({
        id: "microbiology.orderDetail.patientOrigin",
      })}: ${formatMicrobiologyEnum(detail.patientOrigin, intl)}`,
    `${intl.formatMessage({
      id: "microbiology.culturePurpose.label",
    })}: ${formatCulturePurpose(intl, detail.culturePurpose)}`,
    requestingLocation &&
      `${intl.formatMessage({
        id: "microbiology.case.requestingLocation",
      })}: ${requestingLocation}`,
    detail.numberOfSets != null &&
      `${intl.formatMessage({
        id: "microbiology.orderDetail.numberOfSets",
      })}: ${detail.numberOfSets}`,
    `${intl.formatMessage({
      id: "microbiology.orderDetail.antibioticExposure",
    })}: ${exposure}`,
  ].filter(Boolean);

  return (
    <span className="microbiology-case-info__compact">
      {values.join(" | ")}
    </span>
  );
};

const CaseInfoSummary = ({
  accessionNumber,
  requestingLocation,
  orderDetail = {},
}) => {
  const intl = useIntl();
  const detail = orderDetail || {};
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
      aria-label={intl.formatMessage({ id: "microbiology.caseInfo.summary" })}
    >
      <StructuredListBody>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.culturePurpose.label",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {formatCulturePurpose(intl, detail.culturePurpose)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.clinicalHistory",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {display(detail.clinicalHistory)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.orderDetail.admissionDate",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>
            {detail.admissionDate
              ? intl.formatDate(new Date(`${detail.admissionDate}T00:00:00`))
              : display(null)}
          </StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.case.accessionNumber",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>{display(accessionNumber)}</StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>
            <strong>
              {intl.formatMessage({
                id: "microbiology.case.requestingLocation",
              })}
            </strong>
          </StructuredListCell>
          <StructuredListCell>{display(requestingLocation)}</StructuredListCell>
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
            {display(formatMicrobiologyEnum(detail.patientOrigin, intl))}
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
            {display(detail.numberOfSets)}
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
            {detail.antibioticExposure == null
              ? display(null)
              : booleanDisplay(detail.antibioticExposure)}
          </StructuredListCell>
        </StructuredListRow>
      </StructuredListBody>
    </StructuredListWrapper>
  );
};

export default CaseInfoSummary;
