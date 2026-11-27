import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { Callout } from "@carbon/react";
import { useOrderContext, SaveStatus } from "./OrderContext";
import { searchCases } from "../microbiology/CaseWorkspaceService";
import { getMicrobiologyCaseUrl } from "../microbiology/MicrobiologyRoutes";

const MicroCasesOpened = () => {
  const intl = useIntl();
  const history = useHistory();
  const { saveStatus, isDirty, labNumber } = useOrderContext();
  const [cases, setCases] = useState({ labNumber: null, rows: [] });

  useEffect(() => {
    let current = true;
    setCases({ labNumber: null, rows: [] });
    if (saveStatus === SaveStatus.SAVED && !isDirty && labNumber) {
      searchCases(
        new URLSearchParams({
          accessionNumber: labNumber,
          pageSize: "20",
        }).toString(),
      ).then(
        (result) => {
          if (current) {
            setCases({ labNumber, rows: result?.rows || [] });
          }
        },
        () => {},
      );
    }
    return () => {
      current = false;
    };
  }, [saveStatus, isDirty, labNumber]);

  if (
    saveStatus !== SaveStatus.SAVED ||
    isDirty ||
    !labNumber ||
    cases.labNumber !== labNumber ||
    cases.rows.length === 0
  ) {
    return null;
  }

  return cases.rows.map((row) => (
    <Callout
      key={row.id}
      lowContrast
      kind="info"
      titleId={`micro-case-opened-${row.id}`}
      statusIconDescription={intl.formatMessage({
        id: "notification.info",
      })}
      title={intl.formatMessage(
        { id: "order.microPreview.opened" },
        { unit: row.labUnit },
      )}
      actionButtonLabel={intl.formatMessage({
        id: "common.openCase",
      })}
      onActionButtonClick={() => history.push(getMicrobiologyCaseUrl(row.id))}
    />
  ));
};

export default MicroCasesOpened;
