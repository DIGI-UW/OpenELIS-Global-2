import React, { useState, useEffect, useCallback } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  Button,
  Modal,
  TextInput,
  Select,
  SelectItem,
  InlineNotification,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  FileUploader,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Add, Upload, TrashCan, Edit, Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";

/**
 * RetentionPolicySection - Manages retention policy configuration
 * for the biorepository. Policies can be defined by project, sample type, or both.
 *
 * Priority rule: Project-specific policies take precedence over sample type policies.
 */
function RetentionPolicySection() {
  const intl = useIntl();

  // State
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Modal state
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedPolicy, setSelectedPolicy] = useState(null);

  // Form state
  const [formData, setFormData] = useState({
    policyName: "",
    projectName: "",
    sampleTypeName: "",
    periodValue: "",
    periodUnit: "YEARS",
    description: "",
  });
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // CSV import state
  const [csvFile, setCsvFile] = useState(null);
  const [importing, setImporting] = useState(false);

  // Load policies on mount
  useEffect(() => {
    loadPolicies();
  }, []);

  const loadPolicies = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      "/rest/biorepository/retention-policies",
      (response) => {
        setLoading(false);
        if (Array.isArray(response)) {
          setPolicies(response);
        } else {
          setError("Failed to load retention policies");
        }
      },
    );
  }, []);

  const resetForm = () => {
    setFormData({
      policyName: "",
      projectName: "",
      sampleTypeName: "",
      periodValue: "",
      periodUnit: "YEARS",
      description: "",
    });
    setFormErrors({});
  };

  const validateForm = () => {
    const errors = {};

    if (!formData.policyName.trim()) {
      errors.policyName = intl.formatMessage({
        id: "biorepository.retention.error.policyNameRequired",
        defaultMessage: "Policy name is required",
      });
    }

    if (!formData.periodValue || parseInt(formData.periodValue) <= 0) {
      errors.periodValue = intl.formatMessage({
        id: "biorepository.retention.error.periodValueRequired",
        defaultMessage: "Period must be a positive number",
      });
    }

    if (!formData.projectName.trim() && !formData.sampleTypeName.trim()) {
      errors.scope = intl.formatMessage({
        id: "biorepository.retention.error.scopeRequired",
        defaultMessage: "Either project or sample type must be specified",
      });
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleAddPolicy = () => {
    resetForm();
    setAddModalOpen(true);
  };

  const handleEditPolicy = (policy) => {
    setSelectedPolicy(policy);
    setFormData({
      policyName: policy.policyName || "",
      projectName: policy.projectName || "",
      sampleTypeName: policy.sampleTypeName || "",
      periodValue: policy.periodValue?.toString() || "",
      periodUnit: policy.periodUnit || "YEARS",
      description: policy.description || "",
    });
    setFormErrors({});
    setEditModalOpen(true);
  };

  const handleDeletePolicy = (policy) => {
    setSelectedPolicy(policy);
    setDeleteModalOpen(true);
  };

  const handleSavePolicy = async (isEdit = false) => {
    if (!validateForm()) return;

    setSaving(true);
    setError(null);

    const payload = {
      policyName: formData.policyName.trim(),
      projectName: formData.projectName.trim() || null,
      sampleTypeName: formData.sampleTypeName.trim() || null,
      periodValue: parseInt(formData.periodValue),
      periodUnit: formData.periodUnit,
      description: formData.description.trim() || null,
    };

    const url = isEdit
      ? `${config.serverBaseUrl}/rest/biorepository/retention-policies/${selectedPolicy.id}`
      : `${config.serverBaseUrl}/rest/biorepository/retention-policies`;

    const method = isEdit ? "PUT" : "POST";

    try {
      const response = await fetch(url, {
        method,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();
      setSaving(false);

      if (data.success) {
        setSuccessMessage(
          isEdit
            ? intl.formatMessage({
                id: "biorepository.retention.success.updated",
                defaultMessage: "Policy updated successfully",
              })
            : intl.formatMessage({
                id: "biorepository.retention.success.created",
                defaultMessage: "Policy created successfully",
              }),
        );
        setAddModalOpen(false);
        setEditModalOpen(false);
        loadPolicies();
        setTimeout(() => setSuccessMessage(null), 5000);
      } else {
        setError(data.error || "Failed to save policy");
      }
    } catch (err) {
      setSaving(false);
      setError(err.message || "Failed to save policy");
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedPolicy) return;

    setSaving(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/biorepository/retention-policies/${selectedPolicy.id}`,
        {
          method: "DELETE",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      const data = await response.json();
      setSaving(false);

      if (data.success) {
        setSuccessMessage(
          intl.formatMessage({
            id: "biorepository.retention.success.deleted",
            defaultMessage: "Policy deactivated successfully",
          }),
        );
        setDeleteModalOpen(false);
        setSelectedPolicy(null);
        loadPolicies();
        setTimeout(() => setSuccessMessage(null), 5000);
      } else {
        setError(data.error || "Failed to delete policy");
      }
    } catch (err) {
      setSaving(false);
      setError(err.message || "Failed to delete policy");
    }
  };

  const handleCsvImport = async () => {
    if (!csvFile) return;

    setImporting(true);
    setError(null);

    try {
      // Use FormData for multipart file upload
      const formData = new FormData();
      formData.append("file", csvFile);

      const response = await fetch(
        `${config.serverBaseUrl}/rest/biorepository/retention-policies/import`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: formData,
        },
      );

      const data = await response.json();
      setImporting(false);

      if (data.success) {
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "biorepository.retention.success.imported",
              defaultMessage: "{count} policies imported successfully",
            },
            { count: data.imported },
          ),
        );
        setImportModalOpen(false);
        setCsvFile(null);
        loadPolicies();
        setTimeout(() => setSuccessMessage(null), 5000);
      } else {
        setError(data.error || "Failed to import policies");
      }
    } catch (err) {
      setImporting(false);
      setError(err.message || "Failed to import policies");
    }
  };

  const downloadTemplate = async () => {
    try {
      const response = await fetch(
        config.serverBaseUrl +
          "/rest/biorepository/retention-policies/template",
        {
          method: "GET",
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error("Failed to download template");
      }

      const csvContent = await response.text();

      // Create a blob and trigger download
      const blob = new Blob([csvContent], { type: "text/csv" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "retention_policy_template.csv";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || "Failed to download template");
    }
  };

  const headers = [
    {
      key: "policyName",
      header: intl.formatMessage({
        id: "biorepository.retention.column.policyName",
        defaultMessage: "Policy Name",
      }),
    },
    {
      key: "scope",
      header: intl.formatMessage({
        id: "biorepository.retention.column.scope",
        defaultMessage: "Scope",
      }),
    },
    {
      key: "period",
      header: intl.formatMessage({
        id: "biorepository.retention.column.period",
        defaultMessage: "Retention Period",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "biorepository.retention.column.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const rows = policies.map((policy) => ({
    id: policy.id.toString(),
    policyName: policy.policyName,
    scope: policy.projectName
      ? `Project: ${policy.projectName}`
      : policy.sampleTypeName
        ? `Sample Type: ${policy.sampleTypeName}`
        : "-",
    period:
      policy.periodDisplay ||
      `${policy.periodValue} ${policy.periodUnit?.toLowerCase()}`,
    actions: policy,
  }));

  return (
    <div style={{ marginTop: "2rem" }}>
      <Accordion>
        <AccordionItem
          title={
            <span style={{ fontWeight: 600 }}>
              <FormattedMessage
                id="biorepository.retention.policy.title"
                defaultMessage="Retention Policy Configuration"
              />
            </span>
          }
        >
          <div style={{ padding: "1rem 0" }}>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="biorepository.retention.description"
                defaultMessage="Define how long samples should be retained. Project-specific policies take precedence over sample type policies."
              />
            </p>

            {error && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "biorepository.retention.error.title",
                  defaultMessage: "Error",
                })}
                subtitle={error}
                lowContrast
                onCloseButtonClick={() => setError(null)}
                style={{ marginBottom: "1rem" }}
              />
            )}

            {successMessage && (
              <InlineNotification
                kind="success"
                title={intl.formatMessage({
                  id: "biorepository.retention.success.title",
                  defaultMessage: "Success",
                })}
                subtitle={successMessage}
                lowContrast
                onCloseButtonClick={() => setSuccessMessage(null)}
                style={{ marginBottom: "1rem" }}
              />
            )}

            {loading ? (
              <p>
                <FormattedMessage
                  id="biorepository.retention.loading"
                  defaultMessage="Loading policies..."
                />
              </p>
            ) : (
              <DataTable rows={rows} headers={headers}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <TableToolbar>
                      <TableToolbarContent>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={Download}
                          onClick={downloadTemplate}
                        >
                          <FormattedMessage
                            id="biorepository.retention.downloadTemplate"
                            defaultMessage="Download Template"
                          />
                        </Button>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={Upload}
                          onClick={() => setImportModalOpen(true)}
                        >
                          <FormattedMessage
                            id="biorepository.retention.importCsv"
                            defaultMessage="Import CSV"
                          />
                        </Button>
                        <Button
                          kind="primary"
                          size="sm"
                          renderIcon={Add}
                          onClick={handleAddPolicy}
                        >
                          <FormattedMessage
                            id="biorepository.retention.addPolicy"
                            defaultMessage="Add Policy"
                          />
                        </Button>
                      </TableToolbarContent>
                    </TableToolbar>
                    <Table {...getTableProps()} size="lg">
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader
                              {...getHeaderProps({ header })}
                              key={header.key}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={headers.length}>
                              <p
                                style={{
                                  textAlign: "center",
                                  padding: "1rem",
                                  color: "#525252",
                                }}
                              >
                                <FormattedMessage
                                  id="biorepository.retention.noPolicies"
                                  defaultMessage="No retention policies configured. Add a policy or import from CSV."
                                />
                              </p>
                            </TableCell>
                          </TableRow>
                        ) : (
                          rows.map((row) => {
                            const policy = policies.find(
                              (p) => p.id.toString() === row.id,
                            );
                            return (
                              <TableRow {...getRowProps({ row })} key={row.id}>
                                {row.cells.map((cell) => {
                                  if (cell.info.header === "scope") {
                                    const isProject = policy?.projectName;
                                    return (
                                      <TableCell key={cell.id}>
                                        <Tag type={isProject ? "blue" : "teal"}>
                                          {cell.value}
                                        </Tag>
                                      </TableCell>
                                    );
                                  }
                                  if (cell.info.header === "actions") {
                                    return (
                                      <TableCell key={cell.id}>
                                        <OverflowMenu
                                          flipped
                                          ariaLabel={intl.formatMessage({
                                            id: "biorepository.retention.actionsMenu",
                                            defaultMessage: "Actions",
                                          })}
                                        >
                                          <OverflowMenuItem
                                            itemText={intl.formatMessage({
                                              id: "biorepository.retention.action.edit",
                                              defaultMessage: "Edit",
                                            })}
                                            onClick={() =>
                                              handleEditPolicy(policy)
                                            }
                                          />
                                          <OverflowMenuItem
                                            itemText={intl.formatMessage({
                                              id: "biorepository.retention.action.delete",
                                              defaultMessage: "Deactivate",
                                            })}
                                            isDelete
                                            onClick={() =>
                                              handleDeletePolicy(policy)
                                            }
                                          />
                                        </OverflowMenu>
                                      </TableCell>
                                    );
                                  }
                                  return (
                                    <TableCell key={cell.id}>
                                      {cell.value}
                                    </TableCell>
                                  );
                                })}
                              </TableRow>
                            );
                          })
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
          </div>
        </AccordionItem>
      </Accordion>

      {/* Add/Edit Policy Modal */}
      <Modal
        open={addModalOpen || editModalOpen}
        onRequestClose={() => {
          setAddModalOpen(false);
          setEditModalOpen(false);
          resetForm();
        }}
        onRequestSubmit={() => handleSavePolicy(editModalOpen)}
        modalHeading={
          editModalOpen
            ? intl.formatMessage({
                id: "biorepository.retention.modal.editTitle",
                defaultMessage: "Edit Retention Policy",
              })
            : intl.formatMessage({
                id: "biorepository.retention.modal.addTitle",
                defaultMessage: "Add Retention Policy",
              })
        }
        primaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={saving}
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <TextInput
            id="policyName"
            labelText={intl.formatMessage({
              id: "biorepository.retention.form.policyName",
              defaultMessage: "Policy Name",
            })}
            value={formData.policyName}
            onChange={(e) =>
              setFormData({ ...formData, policyName: e.target.value })
            }
            invalid={!!formErrors.policyName}
            invalidText={formErrors.policyName}
            required
          />

          <TextInput
            id="projectName"
            labelText={intl.formatMessage({
              id: "biorepository.retention.form.projectName",
              defaultMessage: "Project Name (optional)",
            })}
            value={formData.projectName}
            onChange={(e) =>
              setFormData({ ...formData, projectName: e.target.value })
            }
            helperText={intl.formatMessage({
              id: "biorepository.retention.form.projectName.help",
              defaultMessage: "Leave empty to apply to all projects",
            })}
          />

          <TextInput
            id="sampleTypeName"
            labelText={intl.formatMessage({
              id: "biorepository.retention.form.sampleTypeName",
              defaultMessage: "Sample Type Name (optional)",
            })}
            value={formData.sampleTypeName}
            onChange={(e) =>
              setFormData({ ...formData, sampleTypeName: e.target.value })
            }
            helperText={intl.formatMessage({
              id: "biorepository.retention.form.sampleTypeName.help",
              defaultMessage: "Leave empty to apply to all sample types",
            })}
          />

          {formErrors.scope && (
            <InlineNotification
              kind="error"
              title=""
              subtitle={formErrors.scope}
              lowContrast
              hideCloseButton
            />
          )}

          <div style={{ display: "flex", gap: "1rem" }}>
            <TextInput
              id="periodValue"
              labelText={intl.formatMessage({
                id: "biorepository.retention.form.periodValue",
                defaultMessage: "Retention Period",
              })}
              type="number"
              min="1"
              value={formData.periodValue}
              onChange={(e) =>
                setFormData({ ...formData, periodValue: e.target.value })
              }
              invalid={!!formErrors.periodValue}
              invalidText={formErrors.periodValue}
              required
              style={{ flex: 1 }}
            />

            <Select
              id="periodUnit"
              labelText={intl.formatMessage({
                id: "biorepository.retention.form.periodUnit",
                defaultMessage: "Unit",
              })}
              value={formData.periodUnit}
              onChange={(e) =>
                setFormData({ ...formData, periodUnit: e.target.value })
              }
              style={{ flex: 1 }}
            >
              <SelectItem
                value="DAYS"
                text={intl.formatMessage({
                  id: "biorepository.retention.unit.days",
                  defaultMessage: "Days",
                })}
              />
              <SelectItem
                value="MONTHS"
                text={intl.formatMessage({
                  id: "biorepository.retention.unit.months",
                  defaultMessage: "Months",
                })}
              />
              <SelectItem
                value="YEARS"
                text={intl.formatMessage({
                  id: "biorepository.retention.unit.years",
                  defaultMessage: "Years",
                })}
              />
            </Select>
          </div>

          <TextInput
            id="description"
            labelText={intl.formatMessage({
              id: "biorepository.retention.form.description",
              defaultMessage: "Description (optional)",
            })}
            value={formData.description}
            onChange={(e) =>
              setFormData({ ...formData, description: e.target.value })
            }
          />
        </div>
      </Modal>

      {/* CSV Import Modal */}
      <Modal
        open={importModalOpen}
        onRequestClose={() => {
          setImportModalOpen(false);
          setCsvFile(null);
        }}
        onRequestSubmit={handleCsvImport}
        modalHeading={intl.formatMessage({
          id: "biorepository.retention.modal.importTitle",
          defaultMessage: "Import Retention Policies from CSV",
        })}
        primaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.import",
          defaultMessage: "Import",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={!csvFile || importing}
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p>
            <FormattedMessage
              id="biorepository.retention.import.description"
              defaultMessage="Upload a CSV file with columns: Policy Name, Project, Sample Type, Period"
            />
          </p>

          <p style={{ fontSize: "0.875rem", color: "#525252" }}>
            <FormattedMessage
              id="biorepository.retention.import.formatHint"
              defaultMessage='Period format examples: "5 years", "18 months", "30 days", or just "5" (defaults to years)'
            />
          </p>

          <FileUploader
            accept={[".csv"]}
            buttonLabel={intl.formatMessage({
              id: "biorepository.retention.import.selectFile",
              defaultMessage: "Select CSV file",
            })}
            iconDescription={intl.formatMessage({
              id: "biorepository.retention.import.iconDescription",
              defaultMessage: "Delete file",
            })}
            filenameStatus="edit"
            labelDescription={intl.formatMessage({
              id: "biorepository.retention.import.fileTypes",
              defaultMessage: "Only .csv files are accepted",
            })}
            labelTitle={intl.formatMessage({
              id: "biorepository.retention.import.uploadTitle",
              defaultMessage: "Upload CSV",
            })}
            onChange={(e) => {
              if (e.target.files && e.target.files[0]) {
                setCsvFile(e.target.files[0]);
              }
            }}
            onDelete={() => setCsvFile(null)}
          />

          <Button
            kind="ghost"
            size="sm"
            renderIcon={Download}
            onClick={downloadTemplate}
          >
            <FormattedMessage
              id="biorepository.retention.import.downloadTemplate"
              defaultMessage="Download CSV template"
            />
          </Button>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteModalOpen}
        onRequestClose={() => {
          setDeleteModalOpen(false);
          setSelectedPolicy(null);
        }}
        onRequestSubmit={handleConfirmDelete}
        modalHeading={intl.formatMessage({
          id: "biorepository.retention.modal.deleteTitle",
          defaultMessage: "Deactivate Retention Policy",
        })}
        primaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.confirmDelete",
          defaultMessage: "Deactivate",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.retention.modal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        size="sm"
      >
        <p>
          <FormattedMessage
            id="biorepository.retention.delete.confirmation"
            defaultMessage='Are you sure you want to deactivate the policy "{policyName}"? This will not affect samples already assigned this policy.'
            values={{ policyName: selectedPolicy?.policyName }}
          />
        </p>
      </Modal>
    </div>
  );
}

export default RetentionPolicySection;
