import React, { useEffect, useState } from "react";
import { Button, Tag } from "@carbon/react";
import { Phone } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import CriticalCallbackModal from "../CriticalCallbackModal";

interface CallbackRow {
  resultId?: string | null;
  testName?: string;
  resultValue?: string;
  unitsOfMeasure?: string;
  accessionNumber?: string;
}

/**
 * OGC-1417 / OGC-714 — documents the phone call for a saved critical result
 * from the unified Results page, as the legacy page does. A callback is
 * logged against a persisted result, so the action only renders once the
 * critical value is saved.
 */
const CriticalCallbackAction: React.FC<{ row: CallbackRow }> = ({ row }) => {
  const intl = useIntl();
  const [open, setOpen] = useState(false);
  const [logged, setLogged] = useState(false);
  const resultId = row.resultId ? String(row.resultId) : "";

  useEffect(() => {
    if (!resultId) {
      return;
    }
    let mounted = true;
    getFromOpenElisServer(
      `/rest/critical-callback/logged-results?resultIds=${resultId}`,
      (ids: unknown) => {
        if (mounted && Array.isArray(ids)) {
          setLogged(ids.map(String).includes(resultId));
        }
      },
    );
    return () => {
      mounted = false;
    };
  }, [resultId]);

  if (!resultId) {
    return null;
  }
  return (
    <div className="unifiedCriticalCallback">
      <Button
        kind="danger--tertiary"
        size="sm"
        renderIcon={Phone}
        data-testid="unified-log-callback-button"
        onClick={() => setOpen(true)}
      >
        {intl.formatMessage({ id: "qa.qi.callback.button" })}
      </Button>
      {logged && (
        <Tag type="green" size="sm" data-testid="unified-callback-logged">
          <FormattedMessage id="label.results.callback.logged" />
        </Tag>
      )}
      <CriticalCallbackModal
        open={open}
        resultRow={row}
        onClose={() => setOpen(false)}
        onLogged={() => setLogged(true)}
      />
    </div>
  );
};

export default CriticalCallbackAction;
