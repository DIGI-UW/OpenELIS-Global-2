import React from "react";
import {
  Tile,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * OrderSummaryCard - Displays order context summary on all workflow steps.
 *
 * Shows key order information:
 * - Lab/Accession Number
 * - Patient Name
 * - Sample Type
 * - Collection Date
 * - Read-only status indicator
 */

const OrderSummaryCard = ({ className = "" }) => {
  const intl = useIntl();
  const { labNumber, orderData, samples, isReadOnly } = useOrderContext();

  // Don't render if no order is loaded
  if (!labNumber && !orderData?.sampleOrderItems?.labNo) {
    return null;
  }

  const displayLabNumber = labNumber || orderData?.sampleOrderItems?.labNo;
  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.firstName || ""} ${orderData.patientProperties.lastName || ""}`.trim()
    : "";
  const sampleTypes = samples
    ?.map((s) => s.sampleTypeName || s.name)
    .filter(Boolean)
    .join(", ");
  const collectionDate =
    samples?.[0]?.sampleXML?.collectionDate ||
    orderData?.sampleOrderItems?.collectionDate;

  return (
    <Tile className={`order-summary-card ${className}`}>
      <div className="order-summary-header">
        <h4>
          <FormattedMessage id="order.summary.title" />
        </h4>
        {isReadOnly && (
          <Tag type="blue" size="sm">
            <FormattedMessage id="label.readonly" defaultMessage="Read Only" />
          </Tag>
        )}
      </div>
      <StructuredListWrapper isCondensed>
        <StructuredListBody>
          {displayLabNumber && (
            <StructuredListRow>
              <StructuredListCell>
                <strong>
                  <FormattedMessage id="order.summary.accessionNumber" />
                </strong>
              </StructuredListCell>
              <StructuredListCell>{displayLabNumber}</StructuredListCell>
            </StructuredListRow>
          )}
          {patientName && (
            <StructuredListRow>
              <StructuredListCell>
                <strong>
                  <FormattedMessage id="order.summary.patientName" />
                </strong>
              </StructuredListCell>
              <StructuredListCell>{patientName}</StructuredListCell>
            </StructuredListRow>
          )}
          {sampleTypes && (
            <StructuredListRow>
              <StructuredListCell>
                <strong>
                  <FormattedMessage id="order.summary.sampleType" />
                </strong>
              </StructuredListCell>
              <StructuredListCell>{sampleTypes}</StructuredListCell>
            </StructuredListRow>
          )}
          {collectionDate && (
            <StructuredListRow>
              <StructuredListCell>
                <strong>
                  <FormattedMessage id="order.summary.collectionDate" />
                </strong>
              </StructuredListCell>
              <StructuredListCell>{collectionDate}</StructuredListCell>
            </StructuredListRow>
          )}
        </StructuredListBody>
      </StructuredListWrapper>
    </Tile>
  );
};

export default OrderSummaryCard;
