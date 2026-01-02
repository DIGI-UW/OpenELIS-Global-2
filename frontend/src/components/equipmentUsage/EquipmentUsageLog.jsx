import React, { useState, useEffect } from "react";
import {
  Button,
  Grid,
  Column,
  TextInput,
  ButtonSet,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Modal,
  Loading,
  InlineNotification,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  Dropdown,
  DropdownItem,
  TimePicker,
  TimePickerSelect,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CartridgeUsageAPI from "./EquipmentUsageService";
import ChooseEquipmentModal from "./modals/ChooseEquipment";
import "./EquipmentUsage.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.inventory.management",
    link: "/inventory",
    defaultMessage: "Inventory Management",
  },
  {
    label: "equipment.usage.title",
    link: "/cartridge-usage",
    defaultMessage: "Equipment Usage",
  },
];

/**
 * EquipmentUsageLog Component
 *
 * Main form for tracking cartridge (equipment) usage in the MNTD laboratory.
 * Displays:
 * - Equipment selection dropdown (filtered to CARTRIDGE type from inventory)
 * - Equipment details (name, serial number, department)
 * - Dynamic table of usage log entries with fields for:
 *   - Date
 *   - Operator Name
 *   - Login Time
 *   - Activities
 *   - Equipment Status
 *   - Logout Time
 *   - Signature
 *
 * Actions supported:
 * - Add/Remove rows from usage log
 * - Save as draft
 * - Submit for approval
 * - Load saved entry
 * - View history
 * - Print report
 */
const EquipmentUsageLog = () => {
  const intl = useIntl();

  // Equipment Selection State
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [loadingEquipment, setLoadingEquipment] = useState(true);
  const [equipmentError, setEquipmentError] = useState(null);

  // Modal State
  const [showChooseEquipmentModal, setShowChooseEquipmentModal] = useState(false);

  // Usage Log Table State
  const [usageRows, setUsageRows] = useState([
    {
      id: 1,
      date: "",
      operatorName: "",
      loginTime: "",
      activities: "",
      equipmentStatus: "Functional",
      logoutTime: "",
      signature: "",
    },
  ]);

  // Form State
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [saveMessage, setSaveMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  // Load Equipment (Cartridges) on Mount
  useEffect(() => {
    const fetchEquipment = async () => {
      setLoadingEquipment(true);
      setEquipmentError(null);
      try {
        // Fetch cartridges from inventory
        CartridgeUsageAPI.getCartridges(
          (data) => {
            if (data && Array.isArray(data)) {
              setEquipment(data);
            } else {
              setEquipmentError(
                intl.formatMessage({ id: "equipment.error.loadFailed" })
              );
            }
            setLoadingEquipment(false);
          },
          (error) => {
            console.error("Error loading equipment:", error);
            setEquipmentError(
              intl.formatMessage({ id: "equipment.error.loadFailed" })
            );
            setLoadingEquipment(false);
          }
        );
      } catch (error) {
        console.error("Error fetching equipment:", error);
        setEquipmentError(
          intl.formatMessage({ id: "equipment.error.loadFailed" })
        );
        setLoadingEquipment(false);
      }
    };

    fetchEquipment();
  }, [intl]);

  // Handle Equipment Selection
  const handleSelectEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setShowChooseEquipmentModal(false);
    setErrorMessage(null);
  };

  // Add new row to usage log
  const handleAddRow = () => {
    const newRow = {
      id: Math.max(...usageRows.map((r) => r.id), 0) + 1,
      date: "",
      operatorName: "",
      loginTime: "",
      activities: "",
      equipmentStatus: "Functional",
      logoutTime: "",
      signature: "",
    };
    setUsageRows([...usageRows, newRow]);
  };

  // Remove row from usage log
  const handleRemoveRow = (rowId) => {
    if (usageRows.length > 1) {
      setUsageRows(usageRows.filter((row) => row.id !== rowId));
    }
  };

  // Update row field
  const handleRowChange = (rowId, field, value) => {
    setUsageRows(
      usageRows.map((row) => (row.id === rowId ? { ...row, [field]: value } : row))
    );
  };

  // Save as Draft
  const handleSaveDraft = () => {
    if (!selectedEquipment) {
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.selectEquipment" })
      );
      return;
    }

    setIsSaving(true);
    try {
      // Save to localStorage for now (draft functionality)
      const draftData = {
        equipment: selectedEquipment,
        rows: usageRows,
        savedAt: new Date().toISOString(),
      };
      localStorage.setItem("equipmentUsageDraft", JSON.stringify(draftData));
      setSaveMessage(
        intl.formatMessage({ id: "equipment.usage.message.saveDraft" })
      );
      setTimeout(() => setSaveMessage(null), 3000);
    } catch (error) {
      console.error("Error saving draft:", error);
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.saveFailed" })
      );
    } finally {
      setIsSaving(false);
    }
  };

  // Submit to Server (Record Usage)
  const handleSubmit = () => {
    if (!selectedEquipment) {
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.selectEquipment" })
      );
      return;
    }

    if (usageRows.length === 0) {
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.noRows" })
      );
      return;
    }

    setIsSubmitting(true);
    try {
      // Record usage via inventory API
      const consumeRequest = {
        itemId: selectedEquipment.id,
        quantity: usageRows.length, // Number of usage entries as quantity
      };

      CartridgeUsageAPI.recordUsage(consumeRequest, (response) => {
        console.log("Usage recorded:", response);
        setSaveMessage(
          intl.formatMessage({ id: "equipment.usage.message.submitted" })
        );
        // Clear draft
        localStorage.removeItem("equipmentUsageDraft");
        // Reset form
        setUsageRows([
          {
            id: 1,
            date: "",
            operatorName: "",
            loginTime: "",
            activities: "",
            equipmentStatus: "Functional",
            logoutTime: "",
            signature: "",
          },
        ]);
        setTimeout(() => setSaveMessage(null), 3000);
        setIsSubmitting(false);
      });
    } catch (error) {
      console.error("Error submitting usage:", error);
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.submitFailed" })
      );
      setIsSubmitting(false);
    }
  };

  // Load Saved Draft
  const handleLoadSaved = () => {
    try {
      const draftData = localStorage.getItem("equipmentUsageDraft");
      if (draftData) {
        const { equipment: savedEquipment, rows: savedRows } = JSON.parse(draftData);
        setSelectedEquipment(savedEquipment);
        setUsageRows(savedRows);
        setSaveMessage(
          intl.formatMessage({ id: "equipment.usage.message.loadedDraft" })
        );
        setTimeout(() => setSaveMessage(null), 3000);
      } else {
        setErrorMessage(
          intl.formatMessage({ id: "equipment.usage.error.noDraft" })
        );
      }
    } catch (error) {
      console.error("Error loading draft:", error);
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.loadFailed" })
      );
    }
  };

  // Clear Form
  const handleClear = () => {
    setSelectedEquipment(null);
    setUsageRows([
      {
        id: 1,
        date: "",
        operatorName: "",
        loginTime: "",
        activities: "",
        equipmentStatus: "Functional",
        logoutTime: "",
        signature: "",
      },
    ]);
    setErrorMessage(null);
    setSaveMessage(null);
  };

  // Print Report
  const handlePrint = () => {
    if (!selectedEquipment) {
      setErrorMessage(
        intl.formatMessage({ id: "equipment.usage.error.selectEquipment" })
      );
      return;
    }
    window.print();
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageContainer">
            {/* Title */}
            <h2>
              <FormattedMessage id="equipment.usage.title" />
            </h2>

            {/* Notifications */}
            {errorMessage && (
              <InlineNotification
                kind="error"
                title="Error"
                subtitle={errorMessage}
                onClose={() => setErrorMessage(null)}
              />
            )}
            {saveMessage && (
              <InlineNotification
                kind="success"
                title="Success"
                subtitle={saveMessage}
                onClose={() => setSaveMessage(null)}
              />
            )}

            {/* Action Buttons - Top */}
            <div className="equipmentUsageActions">
              <Button kind="secondary" size="sm">
                <FormattedMessage id="equipment.usage.addRow" />
              </Button>
              <Button kind="secondary" size="sm">
                <FormattedMessage id="equipment.usage.removeRow" />
              </Button>
              <Button kind="secondary" size="sm" onClick={handleLoadSaved}>
                <FormattedMessage id="equipment.usage.loadSaved" />
              </Button>
              <Button kind="secondary" size="sm" onClick={handleClear}>
                <FormattedMessage id="common.clear" />
              </Button>
              <Button kind="secondary" size="sm" onClick={handlePrint}>
                <FormattedMessage id="common.print" />
              </Button>
              <Button kind="primary" size="sm" onClick={handleSaveDraft}>
                <FormattedMessage id="equipment.usage.saveDraft" />
              </Button>
              <Button kind="primary" size="sm" onClick={handleSubmit}>
                <FormattedMessage id="equipment.usage.submit" />
              </Button>
              <Button kind="ghost" size="sm">
                <FormattedMessage id="equipment.usage.history" />
              </Button>
            </div>

            {/* Equipment Selection Section */}
            {loadingEquipment ? (
              <Loading description="Loading equipment..." />
            ) : (
              <div className="equipmentSelectionSection">
                <h3>
                  <FormattedMessage id="equipment.usage.selectedEquipment" />
                </h3>
                {selectedEquipment ? (
                  <div className="equipmentListSection">
                    <div className="equipmentItem">
                      <div className="equipmentItemContent">
                        <span className="equipmentName">{selectedEquipment.name}</span>
                        <span className="equipmentSerial">
                          {selectedEquipment.catalogNumber || "No serial"}
                        </span>
                        <Button
                          kind="ghost"
                          size="sm"
                          className="removeEquipmentBtn"
                          onClick={() => handleSelectEquipment(null)}
                        >
                          <FormattedMessage id="common.remove" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <Button
                    kind="primary"
                    size="sm"
                    onClick={() => setShowChooseEquipmentModal(true)}
                  >
                    <FormattedMessage id="equipment.usage.chooseEquipment" />
                  </Button>
                )}
              </div>
            )}

            {/* Equipment Details Section */}
            {selectedEquipment && (
              <div className="equipmentDetailsSection">
                <div className="detailsRow">
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.name" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.name}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.serialNumber" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.catalogNumber || ""}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.department" />
                    </label>
                    <input type="text" value="MNTD" readOnly className="detailsInput" />
                  </div>
                </div>
              </div>
            )}

            {/* Usage Log Table */}
            <div className="usageLogSection">
              <h3>
                <FormattedMessage id="equipment.usage.logTable" />
              </h3>
              <div className="tableWrapper">
                <table className="usageLogTable">
                  <thead>
                    <tr>
                      <th>
                        <FormattedMessage id="equipment.usage.table.date" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.operatorName" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.loginTime" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.activities" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.equipmentStatus" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.logoutTime" />
                      </th>
                      <th>
                        <FormattedMessage id="equipment.usage.table.signature" />
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {usageRows.map((row) => (
                      <tr key={row.id}>
                        <td>
                          <DatePickerInput
                            kind="single"
                            dateFormat="mm/dd/yyyy"
                            placeholder="mm/dd/yyyy"
                            value={row.date}
                            onChange={(e) =>
                              handleRowChange(row.id, "date", e.target.value)
                            }
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.operatorName}
                            onChange={(e) =>
                              handleRowChange(row.id, "operatorName", e.target.value)
                            }
                            placeholder="Operator name"
                            className="tableInput"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.loginTime}
                            onChange={(e) =>
                              handleRowChange(row.id, "loginTime", e.target.value)
                            }
                            placeholder="--:-- --"
                            className="tableInput"
                          />
                        </td>
                        <td>
                          <textarea
                            value={row.activities}
                            onChange={(e) =>
                              handleRowChange(row.id, "activities", e.target.value)
                            }
                            placeholder="Activities"
                            className="tableTextarea"
                          />
                        </td>
                        <td>
                          <select
                            value={row.equipmentStatus}
                            onChange={(e) =>
                              handleRowChange(row.id, "equipmentStatus", e.target.value)
                            }
                            className="tableSelect"
                          >
                            <option value="Functional">Functional</option>
                            <option value="Non-functional">Non-functional</option>
                            <option value="Maintenance">Maintenance</option>
                          </select>
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.logoutTime}
                            onChange={(e) =>
                              handleRowChange(row.id, "logoutTime", e.target.value)
                            }
                            placeholder="--:-- --"
                            className="tableInput"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.signature}
                            onChange={(e) =>
                              handleRowChange(row.id, "signature", e.target.value)
                            }
                            placeholder="Type name or upload"
                            className="tableInput signatureInput"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Action Buttons - Bottom */}
            <div className="equipmentUsageActionsBottom">
              <ButtonSet>
                <Button kind="secondary" onClick={handleAddRow}>
                  <FormattedMessage id="equipment.usage.addRow" />
                </Button>
                {usageRows.length > 1 && (
                  <Button
                    kind="danger"
                    onClick={() => handleRemoveRow(usageRows[usageRows.length - 1].id)}
                  >
                    <FormattedMessage id="equipment.usage.removeRow" />
                  </Button>
                )}
              </ButtonSet>
            </div>
          </div>
        </Column>
      </Grid>

      {/* Choose Equipment Modal */}
      <ChooseEquipmentModal
        open={showChooseEquipmentModal}
        onClose={() => setShowChooseEquipmentModal(false)}
        equipment={equipment}
        onSelectEquipment={handleSelectEquipment}
      />
    </>
  );
};

export default EquipmentUsageLog;
