import React, { useState, useContext } from "react";
import {
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  FileUploader,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";

function CsvImportPreview({ year, onClose, onImportComplete }) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [parsedRows, setParsedRows] = useState([]);
  const [file, setFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [result, setResult] = useState(null);

  const handleFileChange = (event) => {
    const uploadedFile = event.target.files?.[0];
    if (!uploadedFile) return;
    setFile(uploadedFile);

    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target.result;
      const lines = text.split("\n").filter((l) => l.trim());
      const rows = [];
      // Skip header
      for (let i = 1; i < lines.length; i++) {
        const parts = lines[i].split(",");
        if (parts.length >= 2) {
          rows.push({
            id: String(i),
            date: parts[0]?.trim(),
            name: parts[1]?.trim().replace(/^"|"$/g, ""),
            recurring: parts[2]?.trim() === "true" ? "Yes" : "No",
          });
        }
      }
      setParsedRows(rows);
    };
    reader.readAsText(uploadedFile);
  };

  const handleImport = async () => {
    if (!file) return;
    setImporting(true);
    const formData = new FormData();
    formData.append("file", file);

    try {
      // Note: postToOpenElisServerFormData only returns status code, not response
      // body. We need the JSON body (imported/skipped/errors) for user feedback,
      // so we use fetch directly here with the standard CSRF/credentials pattern.
      const response = await fetch(
        `${config.serverBaseUrl}/rest/calendar/holidays/import?year=${year}`,
        {
          method: "POST",
          credentials: "include",
          headers: { "X-CSRF-Token": localStorage.getItem("CSRF") },
          body: formData,
        },
      );
      const data = await response.json();
      setResult(data);
      if (data.imported > 0) {
        addNotification({
          kind: "success",
          title: intl.formatMessage(
            { id: "calendar.management.imported" },
            { count: data.imported },
          ),
        });
        setNotificationVisible(true);
        onImportComplete();
      }
    } catch {
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "calendar.management.saveError" }),
      });
      setNotificationVisible(true);
    }
    setImporting(false);
  };

  const headers = [
    {
      key: "date",
      header: intl.formatMessage({ id: "calendar.management.column.csvDate" }),
    },
    {
      key: "name",
      header: intl.formatMessage({ id: "calendar.management.column.csvName" }),
    },
    {
      key: "recurring",
      header: intl.formatMessage({
        id: "calendar.management.column.csvRecurring",
      }),
    },
  ];

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: "calendar.management.importPreview",
      })}
      primaryButtonText={
        importing
          ? intl.formatMessage({ id: "calendar.management.importing" })
          : intl.formatMessage({ id: "calendar.management.importCsv" })
      }
      secondaryButtonText={intl.formatMessage({
        id: "calendar.management.cancel",
      })}
      onRequestSubmit={handleImport}
      onRequestClose={onClose}
      primaryButtonDisabled={parsedRows.length === 0 || importing}
      size="lg"
    >
      {!file && (
        <FileUploader
          accept={[".csv"]}
          buttonLabel={intl.formatMessage({
            id: "calendar.management.chooseFile",
          })}
          filenameStatus="edit"
          labelDescription={intl.formatMessage({
            id: "calendar.management.csvColumns",
          })}
          onChange={handleFileChange}
        />
      )}

      {parsedRows.length > 0 && (
        <>
          <p style={{ marginBottom: "0.5rem" }}>
            {intl.formatMessage(
              { id: "calendar.management.rowsFound" },
              { count: parsedRows.length },
            )}
          </p>
          <DataTable rows={parsedRows} headers={headers}>
            {({ rows, headers: hdrs, getTableProps }) => (
              <TableContainer>
                <Table {...getTableProps()} size="sm">
                  <TableHead>
                    <TableRow>
                      {hdrs.map((h) => (
                        <TableHeader key={h.key}>{h.header}</TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </>
      )}

      {result && result.errors && result.errors.length > 0 && (
        <div style={{ marginTop: "1rem" }}>
          {result.errors.map((err, i) => (
            <InlineNotification
              key={i}
              kind="warning"
              title={`Row ${err.row}: ${err.reason}`}
              lowContrast
              hideCloseButton
            />
          ))}
        </div>
      )}
    </Modal>
  );
}

export default CsvImportPreview;
