import React, { useState, useEffect } from "react";
import { Button, Tag, Grid, Column } from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const STATUS_TAG_MAP = {
  DRAFT: "gray",
  PREPARED: "blue",
  SHIPPED: "teal",
  COMPLETED: "green",
};

const DistributionDetail = ({ distributionId, onBack }) => {
  const intl = useIntl();
  const [distribution, setDistribution] = useState(null);

  useEffect(() => {
    if (distributionId) {
      getFromOpenElisServer(
        `/rest/eqa/distributions/${distributionId}`,
        (data) => {
          if (data) {
            setDistribution(data);
          }
        },
      );
    }
  }, [distributionId]);

  const handleAdvanceStatus = () => {
    putToOpenElisServer(
      `/rest/eqa/distributions/${distributionId}/status`,
      null,
      (status) => {
        if (status === 200) {
          getFromOpenElisServer(
            `/rest/eqa/distributions/${distributionId}`,
            (data) => {
              if (data) setDistribution(data);
            },
          );
        }
      },
    );
  };

  const handleGenerateBarcodes = () => {
    postToOpenElisServerJsonResponse(
      `/rest/eqa/distributions/${distributionId}/barcodes`,
      "{}",
      () => {},
    );
  };

  if (!distribution) {
    return null;
  }

  const canAdvance = distribution.status !== "COMPLETED";

  return (
    <div>
      <h3>{intl.formatMessage({ id: "eqa.distribution.detail.title" })}</h3>
      <Grid condensed>
        <Column lg={8} md={8} sm={4}>
          <p>
            <strong>
              {intl.formatMessage({ id: "eqa.distribution.name" })}:
            </strong>{" "}
            {distribution.distributionName}
          </p>
          <p>
            <strong>
              {intl.formatMessage({ id: "eqa.distribution.program" })}:
            </strong>{" "}
            {distribution.programName}
          </p>
          <p>
            <strong>
              {intl.formatMessage({ id: "eqa.distribution.deadline" })}:
            </strong>{" "}
            {distribution.deadline
              ? new Date(distribution.deadline).toLocaleDateString()
              : ""}
          </p>
          <p>
            <strong>
              {intl.formatMessage({ id: "alerts.table.status" })}:
            </strong>{" "}
            <Tag type={STATUS_TAG_MAP[distribution.status] || "gray"} size="sm">
              {intl.formatMessage({
                id: `eqa.distribution.status.${(distribution.status || "").toLowerCase()}`,
                defaultMessage: distribution.status,
              })}
            </Tag>
          </p>
        </Column>
        <Column lg={8} md={8} sm={4}>
          {canAdvance && (
            <Button
              onClick={handleAdvanceStatus}
              style={{ marginRight: "0.5rem" }}
            >
              {intl.formatMessage({ id: "eqa.distribution.advance" })}
            </Button>
          )}
          <Button kind="secondary" onClick={handleGenerateBarcodes}>
            {intl.formatMessage({ id: "eqa.distribution.barcodes" })}
          </Button>
          {onBack && (
            <Button
              kind="ghost"
              onClick={onBack}
              style={{ marginLeft: "0.5rem" }}
            >
              Back
            </Button>
          )}
        </Column>
      </Grid>
    </div>
  );
};

export default DistributionDetail;
