import React, { useState } from "react";
import { FileUploader, Button } from "@carbon/react";
import { useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

const BatchImport = ({ distributionId, onImportComplete }) => {
  const intl = useIntl();
  const [parsedRows, setParsedRows] = useState([]);
  const [importResult, setImportResult] = useState(null);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target.result;
      const lines = text.split("\n").filter((line) => line.trim());
      const headers = lines[0].split(",").map((h) => h.trim());

      const rows = lines.slice(1).map((line) => {
        const values = line.split(",").map((v) => v.trim());
        const row = {};
        headers.forEach((h, i) => {
          row[h] = values[i];
        });
        return {
          organizationId: Number(row.organizationId || row.orgId),
          testId: Number(row.testId),
          resultValue: Number(row.resultValue || row.value),
        };
      });

      setParsedRows(rows);
    };
    reader.readAsText(file);
  };

  const handleImport = () => {
    postToOpenElisServerJsonResponse(
      `/rest/eqa/distributions/${distributionId}/results/import`,
      JSON.stringify(parsedRows),
      (response) => {
        setImportResult(response);
        if (onImportComplete) onImportComplete(response);
      },
    );
  };

  return (
    <div>
      <h4>{intl.formatMessage({ id: "eqa.results.batch.import" })}</h4>
      <FileUploader
        accept={[".csv"]}
        buttonLabel={intl.formatMessage({ id: "eqa.results.import.upload" })}
        filenameStatus="edit"
        labelDescription="CSV format: organizationId,testId,resultValue"
        labelTitle=""
        onChange={handleFileChange}
      />
      {parsedRows.length > 0 && (
        <div style={{ marginTop: "1rem" }}>
          <p>{parsedRows.length} rows parsed</p>
          <Button onClick={handleImport}>
            {intl.formatMessage({ id: "eqa.results.import.confirm" })}
          </Button>
        </div>
      )}
      {importResult && (
        <div style={{ marginTop: "1rem" }}>
          <p>
            {intl.formatMessage(
              { id: "eqa.results.import.success" },
              { count: importResult.successCount },
            )}
          </p>
          {importResult.errorCount > 0 && (
            <p style={{ color: "#da1e28" }}>
              {intl.formatMessage(
                { id: "eqa.results.import.errors" },
                { count: importResult.errorCount },
              )}
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export default BatchImport;
