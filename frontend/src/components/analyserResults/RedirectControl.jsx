import React, { useState } from "react";
import { Button, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * Lets the reviewer place a grouping's results on another existing order, for a
 * specimen ID the instrument misread. The reason is required: the server keeps
 * the grouping staged until it has one.
 */
const RedirectControl = ({ row, onChange }) => {
  const intl = useIntl();
  const [open, setOpen] = useState(Boolean(row.redirectAccession));
  const [accession, setAccession] = useState(row.redirectAccession || "");
  const [reason, setReason] = useState(row.redirectReason || "");

  if (!open) {
    return (
      <Button kind="ghost" size="sm" onClick={() => setOpen(true)}>
        {intl.formatMessage({ id: "analyzer.placement.redirect.open" })}
      </Button>
    );
  }
  const update = (field, value, set) => {
    set(value);
    onChange(field, value, row.id);
  };
  return (
    <div className="redirectControl" data-testid={`redirect-${row.id}`}>
      <TextInput
        id={`resultList${row.id}.redirectAccession`}
        size="sm"
        labelText={intl.formatMessage({
          id: "analyzer.placement.redirect.accession",
        })}
        helperText={intl.formatMessage({
          id: "analyzer.placement.redirect.help",
        })}
        value={accession}
        onChange={(event) =>
          update("redirectAccession", event.target.value, setAccession)
        }
      />
      <TextArea
        id={`resultList${row.id}.redirectReason`}
        rows={2}
        labelText={intl.formatMessage({
          id: "analyzer.placement.redirect.reason",
        })}
        invalid={Boolean(accession.trim()) && !reason.trim()}
        invalidText={intl.formatMessage({
          id: "analyzer.placement.redirect.reasonRequired",
        })}
        value={reason}
        onChange={(event) =>
          update("redirectReason", event.target.value, setReason)
        }
      />
    </div>
  );
};

export default RedirectControl;
