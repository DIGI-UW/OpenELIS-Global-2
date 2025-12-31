import React, { useState, useEffect } from "react";
import { FormattedMessage } from "react-intl";
import { EquipmentUsageEntryAPI } from "./EquipmentUsageService";
import "./EquipmentUsagePrintableForm.css";

const EquipmentUsagePrintableForm = ({ entries = [], equipment = null, onClose = null }) => {
  const [filteredEntries, setFilteredEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // If entries are passed in, use them; otherwise load from API
    if (entries && entries.length > 0) {
      setFilteredEntries(entries);
    }
  }, [entries]);

  // Format date to YYYY-MM-DD
  const formatDate = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  // Format time to HH:MM
  const formatTime = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  // Get today's date for effective date field
  const getTodayDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${day}/${month}/${year}`;
  };

  // Get unique equipment name from first entry if not provided
  const getEquipmentName = () => {
    if (equipment && equipment.name) return equipment.name;
    if (filteredEntries.length > 0 && filteredEntries[0].equipment) {
      return filteredEntries[0].equipment.name;
    }
    return "____________________________";
  };

  // Get unique serial number from first entry if not provided
  const getSerialNumber = () => {
    if (equipment && equipment.serialNumber) return equipment.serialNumber;
    if (filteredEntries.length > 0 && filteredEntries[0].equipment) {
      return filteredEntries[0].equipment.serialNumber;
    }
    return "____________________________";
  };

  // Get unique department from first entry if not provided
  const getDepartment = () => {
    if (equipment && equipment.department) return equipment.department;
    if (filteredEntries.length > 0 && filteredEntries[0].department) {
      return filteredEntries[0].department;
    }
    return "____________________________";
  };

  // Generate empty rows for printing (total rows per page: 15)
  const ROWS_PER_PAGE = 15;
  const totalRows = Math.max(
    filteredEntries.length,
    ROWS_PER_PAGE
  );
  const rows = Array.from({ length: totalRows }, (_, index) => {
    return filteredEntries[index] || null;
  });

  // Split rows into pages
  const pages = [];
  for (let i = 0; i < rows.length; i += ROWS_PER_PAGE) {
    pages.push(rows.slice(i, i + ROWS_PER_PAGE));
  }

  return (
    <div className="printable-form-container">
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {pages.map((pageRows, pageIndex) => (
        <div key={pageIndex} className="form-page">
          {/* Header Section */}
          <div className="form-header">
            <div className="header-title">
              <h2>ARMAUER HANSEN'S RESEARCH INSTITUTE</h2>
              <p className="subtitle">Equipment Usage Log</p>
              <p className="document-code">Document No: AHRI/AU/FS3-003</p>
            </div>
            <div className="header-info">
              <div className="info-row">
                <span className="label">Effective Date:</span>
                <span className="value">{getTodayDate()}</span>
              </div>
              <div className="info-row">
                <span className="label">Version:</span>
                <span className="value">1.0</span>
              </div>
            </div>
          </div>

          {/* Form Table */}
          <table className="usage-table">
            <thead>
              <tr>
                <th className="col-date">Date</th>
                <th className="col-operator">Operator Name</th>
                <th className="col-login-time">Login Time</th>
                <th className="col-activities">Activities Done</th>
                <th className="col-equipment-status">Equipment Status</th>
                <th className="col-logout-time">Logout Time</th>
                <th className="col-signature">Signature</th>
              </tr>
            </thead>
            <tbody>
              {pageRows.map((entry, rowIndex) => (
                <tr key={rowIndex} className={entry ? "data-row" : "empty-row"}>
                  <td className="col-date">
                    {entry ? formatDate(entry.loginTime) : ""}
                  </td>
                  <td className="col-operator">
                    {entry ? entry.operatorName : ""}
                  </td>
                  <td className="col-login-time">
                    {entry ? formatTime(entry.loginTime) : ""}
                  </td>
                  <td className="col-activities">
                    {entry ? entry.activitiesDone : ""}
                  </td>
                  <td className="col-equipment-status">
                    {entry ? entry.equipmentStatus : ""}
                  </td>
                  <td className="col-logout-time">
                    {entry ? formatTime(entry.logoutTime) : ""}
                  </td>
                  <td className="col-signature">
                    {entry && entry.approvalSignature ? entry.approvalSignature : ""}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Footer Section */}
          <div className="form-footer">
            <div className="footer-row">
              <div className="footer-field">
                <label>Equipment Name:</label>
                <span className="field-value">{getEquipmentName()}</span>
              </div>
              <div className="footer-field">
                <label>Equipment Serial No.:</label>
                <span className="field-value">{getSerialNumber()}</span>
              </div>
            </div>
            <div className="footer-row">
              <div className="footer-field full-width">
                <label>Department:</label>
                <span className="field-value">{getDepartment()}</span>
              </div>
            </div>
            <div className="footer-disclaimer">
              <p>A controlled copy for internal use only</p>
            </div>
          </div>

          {/* Page Break (not visible on last page) */}
          {pageIndex < pages.length - 1 && (
            <div className="page-break"></div>
          )}
        </div>
      ))}

      {/* Print and Close buttons (hidden when printing) */}
      <div className="print-controls no-print">
        <button
          className="btn-print"
          onClick={() => window.print()}
        >
          <FormattedMessage
            id="equipment.usage.printable.button.print"
            defaultMessage="Print"
          />
        </button>
        {onClose && (
          <button
            className="btn-close"
            onClick={onClose}
          >
            <FormattedMessage
              id="equipment.usage.printable.button.close"
              defaultMessage="Close"
            />
          </button>
        )}
      </div>
    </div>
  );
};

export default EquipmentUsagePrintableForm;
