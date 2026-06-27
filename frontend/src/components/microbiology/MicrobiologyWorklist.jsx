import React, { useEffect, useState } from "react";
import { Button, Loading, Stack, Tag, Tile } from "@carbon/react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";

const MicrobiologyWorklist = ({ service = MicrobiologyService }) => {
  const intl = useIntl();
  const history = useHistory();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    service.getWorklistRows().then((worklistRows) => {
      setRows(Array.isArray(worklistRows) ? worklistRows : []);
      setLoading(false);
    });
  }, [service]);

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  return (
    <main data-testid="microbiology-worklist">
      <Stack gap={5}>
        <h2>{intl.formatMessage({ id: "microbiology.worklist.title" })}</h2>
        {rows.map((row) => (
          <Tile
            key={row.caseId}
            data-testid={`microbiology-worklist-row-${row.caseId}`}
          >
            <Stack gap={3}>
              <div>
                <strong>{row.sampleItemId}</strong>
                <div>{row.workflowType}</div>
              </div>
              <div>
                <Tag type={row.urgency === "HIGH" ? "red" : "gray"}>
                  {row.urgency}
                </Tag>
                <Tag type={row.needsAstReview ? "purple" : "blue"}>
                  {row.dueAction}
                </Tag>
                {row.hasOpenCriticalCommunication && (
                  <Tag type="magenta">
                    {intl.formatMessage({
                      id: "microbiology.worklist.critical",
                    })}
                  </Tag>
                )}
              </div>
              {row.siblingWorkflows.length > 0 && (
                <p data-testid="microbiology-worklist-siblings">
                  {intl.formatMessage({
                    id: "microbiology.worklist.siblings",
                  })}
                  : {row.siblingWorkflows.join(", ")}
                </p>
              )}
              <Button
                kind="secondary"
                size="sm"
                onClick={() =>
                  history.push(`/MicrobiologyCaseView/${row.caseId}`)
                }
              >
                {intl.formatMessage({ id: "microbiology.worklist.openCase" })}
              </Button>
            </Stack>
          </Tile>
        ))}
      </Stack>
    </main>
  );
};

export default MicrobiologyWorklist;
