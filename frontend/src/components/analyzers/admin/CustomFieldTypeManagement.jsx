import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Search,
  Button,
  OverflowMenu,
  OverflowMenuItem,
  Tag,
  Modal,
  TextInput,
  NumberInput,
  Toggle,
  Form,
  FormGroup,
  InlineNotification,
  Grid,
  Column,
} from "@carbon/react";
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getCustomFieldTypes,
  createCustomFieldType,
  updateCustomFieldType,
  deleteCustomFieldType,
} from "../../../services/analyzerService";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./CustomFieldTypeManagement.css";

/**
 * CustomFieldTypeManagement Component
 *
 * Admin-only page for managing custom field types (FR-018)
 */
const CustomFieldTypeManagement = () => {
  const intl = useIntl();
  const searchTimeoutRef = useRef(null);

  // State
  const [customFieldTypes, setCustomFieldTypes] = useState([]);
  const [filteredTypes, setFilteredTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editingType, setEditingType] = useState(null);
  const [notification, setNotification] = useState(null);
  const [formData, setFormData] = useState({
    typeName: "",
    displayName: "",
    validationPattern: "",
    valueRangeMin: "",
    valueRangeMax: "",
    allowedCharacters: "",
    isActive: true,
  });
  const [formErrors, setFormErrors] = useState({});
  const [deleteModal, setDeleteModal] = useState({ open: false, type: null });

  // Load custom field types
  const loadCustomFieldTypes = useCallback(() => {
    setLoading(true);
    getCustomFieldTypes((data) => {
      if (Array.isArray(data)) {
        setCustomFieldTypes(data);
        setFilteredTypes(data);
      } else {
        setCustomFieldTypes([]);
        setFilteredTypes([]);
      }
      setLoading(false);
    }, null);
  }, []);

  // Initial load
  useEffect(() => {
    loadCustomFieldTypes();
  }, [loadCustomFieldTypes]);

  // Search handler with debounce
  const handleSearch = (value) => {
    setSearchTerm(value);

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = setTimeout(() => {
      if (!value.trim()) {
        setFilteredTypes(customFieldTypes);
        return;
      }

      const filtered = customFieldTypes.filter(
        (type) =>
          type.typeName?.toLowerCase().includes(value.toLowerCase()) ||
          type.displayName?.toLowerCase().includes(value.toLowerCase()),
      );
      setFilteredTypes(filtered);
    }, 300);
  };

  // Validate regex pattern
  const validateRegexPattern = (pattern) => {
    if (!pattern || !pattern.trim()) {
      return true; // Optional field
    }
    try {
      new RegExp(pattern);
      return true;
    } catch (e) {
      return false;
    }
  };

  // Validate form
  const validateForm = () => {
    const errors = {};

    if (!formData.typeName || !formData.typeName.trim()) {
      errors.typeName = intl.formatMessage({
        id: "customFieldType.error.typeNameRequired",
        defaultMessage: "Type name is required",
      });
    }

    if (!formData.displayName || !formData.displayName.trim()) {
      errors.displayName = intl.formatMessage({
        id: "customFieldType.error.displayNameRequired",
        defaultMessage: "Display name is required",
      });
    }

    if (
      formData.validationPattern &&
      !validateRegexPattern(formData.validationPattern)
    ) {
      errors.validationPattern = intl.formatMessage({
        id: "customFieldType.error.invalidRegex",
        defaultMessage: "Invalid regex pattern",
      });
    }

    if (formData.valueRangeMin && formData.valueRangeMax) {
      const min = parseFloat(formData.valueRangeMin);
      const max = parseFloat(formData.valueRangeMax);
      if (!isNaN(min) && !isNaN(max) && min > max) {
        errors.valueRangeMax = intl.formatMessage({
          id: "customFieldType.error.rangeInvalid",
          defaultMessage: "Minimum cannot be greater than maximum",
        });
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle form submit
  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }

    const submitData = {
      typeName: formData.typeName.trim(),
      displayName: formData.displayName.trim(),
      validationPattern: formData.validationPattern?.trim() || null,
      valueRangeMin: formData.valueRangeMin?.trim() || null,
      valueRangeMax: formData.valueRangeMax?.trim() || null,
      allowedCharacters: formData.allowedCharacters?.trim() || null,
      isActive: formData.isActive,
    };

    const callback = (response, extraParams) => {
      if (response && !extraParams?.error) {
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "customFieldType.success.saved",
            defaultMessage: "Custom field type saved successfully",
          }),
        });
        setFormOpen(false);
        resetForm();
        loadCustomFieldTypes();
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "customFieldType.error.saveFailed",
            defaultMessage: "Failed to save custom field type",
          }),
          subtitle: extraParams?.error || "Unknown error",
        });
      }
    };

    if (editingType) {
      updateCustomFieldType(editingType.id, submitData, callback, null);
    } else {
      createCustomFieldType(submitData, callback, null);
    }
  };

  // Reset form
  const resetForm = () => {
    setFormData({
      typeName: "",
      displayName: "",
      validationPattern: "",
      valueRangeMin: "",
      valueRangeMax: "",
      allowedCharacters: "",
      isActive: true,
    });
    setFormErrors({});
    setEditingType(null);
  };

  // Open form for editing
  const handleEdit = (type) => {
    setEditingType(type);
    setFormData({
      typeName: type.typeName || "",
      displayName: type.displayName || "",
      validationPattern: type.validationPattern || "",
      valueRangeMin: type.valueRangeMin?.toString() || "",
      valueRangeMax: type.valueRangeMax?.toString() || "",
      allowedCharacters: type.allowedCharacters || "",
      isActive: type.isActive !== undefined ? type.isActive : true,
    });
    setFormOpen(true);
  };

  // Handle delete
  const handleDelete = () => {
    if (!deleteModal.type) {
      return;
    }

    deleteCustomFieldType(
      deleteModal.type.id,
      (success, error) => {
        if (success) {
          setNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "customFieldType.success.deleted",
              defaultMessage: "Custom field type deleted successfully",
            }),
          });
          loadCustomFieldTypes();
        } else {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "customFieldType.error.deleteFailed",
              defaultMessage: "Failed to delete custom field type",
            }),
            subtitle: error?.error || "Unknown error",
          });
        }
        setDeleteModal({ open: false, type: null });
      },
      null,
    );
  };

  // Table headers
  const headers = [
    {
      key: "typeName",
      header: intl.formatMessage({
        id: "customFieldType.table.header.typeName",
        defaultMessage: "Type Name",
      }),
    },
    {
      key: "displayName",
      header: intl.formatMessage({
        id: "customFieldType.table.header.displayName",
        defaultMessage: "Display Name",
      }),
    },
    {
      key: "validationPattern",
      header: intl.formatMessage({
        id: "customFieldType.table.header.validationPattern",
        defaultMessage: "Validation Pattern",
      }),
    },
    {
      key: "valueRange",
      header: intl.formatMessage({
        id: "customFieldType.table.header.valueRange",
        defaultMessage: "Value Range",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "customFieldType.table.header.status",
        defaultMessage: "Status",
      }),
    },
    { key: "actions", header: "" },
  ];

  // Table rows
  const rows = filteredTypes.map((type) => {
    const valueRange =
      type.valueRangeMin && type.valueRangeMax
        ? `${type.valueRangeMin} - ${type.valueRangeMax}`
        : "-";

    return {
      id: type.id,
      typeName: type.typeName || "-",
      displayName: type.displayName || "-",
      validationPattern: type.validationPattern || "-",
      valueRange: valueRange,
      status: type.isActive ? "Active" : "Inactive",
      _type: type, // Store full type object for actions
    };
  });

  return (
    <div
      className="custom-field-type-management"
      data-testid="custom-field-type-management"
    >
      {/* Header */}
      <div className="custom-field-type-management-header">
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({
                id: "customFieldType.page.hierarchy.root",
                defaultMessage: "System Administration",
              }),
            },
            {
              label: intl.formatMessage({
                id: "customFieldType.page.hierarchy.management",
                defaultMessage: "Custom Field Types",
              }),
            },
          ]}
          subtitle={intl.formatMessage({
            id: "customFieldType.page.subtitle",
            defaultMessage:
              "Manage custom field types for analyzer field mapping",
          })}
        />
        <Button
          kind="primary"
          renderIcon={Add}
          data-testid="add-custom-field-type-button"
          onClick={() => {
            resetForm();
            setFormOpen(true);
          }}
        >
          {intl.formatMessage({
            id: "customFieldType.action.add",
            defaultMessage: "Add Custom Field Type",
          })}
        </Button>
      </div>

      {/* Notification */}
      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.title}
          subtitle={notification.subtitle}
          onClose={() => setNotification(null)}
          lowContrast
        />
      )}

      {/* Search */}
      <div className="custom-field-type-management-search">
        <Search
          labelText={intl.formatMessage({
            id: "customFieldType.search.label",
            defaultMessage: "Search custom field types",
          })}
          placeholder={intl.formatMessage({
            id: "customFieldType.search.placeholder",
            defaultMessage: "Search by type name or display name...",
          })}
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
          size="lg"
        />
      </div>

      {/* Table */}
      <DataTable
        rows={rows}
        headers={headers}
        isSortable
        useZebraStyles
        size="lg"
        data-testid="custom-field-type-table"
      >
        {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const type =
                    row._type || customFieldTypes.find((t) => t.id === row.id);
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "actions") {
                          return (
                            <TableCell key={cell.id}>
                              <OverflowMenu
                                flipped
                                ariaLabel={intl.formatMessage({
                                  id: "customFieldType.actions.ariaLabel",
                                  defaultMessage: "Actions",
                                })}
                              >
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "customFieldType.action.edit",
                                    defaultMessage: "Edit",
                                  })}
                                  onClick={() => handleEdit(type)}
                                />
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "customFieldType.action.delete",
                                    defaultMessage: "Delete",
                                  })}
                                  onClick={() =>
                                    setDeleteModal({ open: true, type })
                                  }
                                  isDelete
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }
                        if (cell.info.header === "status") {
                          return (
                            <TableCell key={cell.id}>
                              <Tag
                                type={
                                  cell.value === "Active" ? "green" : "gray"
                                }
                                size="sm"
                              >
                                {cell.value}
                              </Tag>
                            </TableCell>
                          );
                        }
                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      {/* Add/Edit Form Modal */}
      <Modal
        open={formOpen}
        modalHeading={
          editingType
            ? intl.formatMessage({
                id: "customFieldType.modal.editTitle",
                defaultMessage: "Edit Custom Field Type",
              })
            : intl.formatMessage({
                id: "customFieldType.modal.addTitle",
                defaultMessage: "Add Custom Field Type",
              })
        }
        primaryButtonText={intl.formatMessage({
          id: "customFieldType.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "customFieldType.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmit}
        onRequestClose={() => {
          setFormOpen(false);
          resetForm();
        }}
        size="sm"
      >
        <Form>
          <FormGroup legendText="">
            <TextInput
              id="typeName"
              labelText={intl.formatMessage({
                id: "customFieldType.form.typeName",
                defaultMessage: "Type Name",
              })}
              placeholder={intl.formatMessage({
                id: "customFieldType.form.typeNamePlaceholder",
                defaultMessage: "e.g., CUSTOM_NUMERIC",
              })}
              value={formData.typeName}
              onChange={(e) =>
                setFormData({ ...formData, typeName: e.target.value })
              }
              invalid={!!formErrors.typeName}
              invalidText={formErrors.typeName}
              required
            />
            <TextInput
              id="displayName"
              labelText={intl.formatMessage({
                id: "customFieldType.form.displayName",
                defaultMessage: "Display Name",
              })}
              placeholder={intl.formatMessage({
                id: "customFieldType.form.displayNamePlaceholder",
                defaultMessage: "e.g., Custom Numeric Field",
              })}
              value={formData.displayName}
              onChange={(e) =>
                setFormData({ ...formData, displayName: e.target.value })
              }
              invalid={!!formErrors.displayName}
              invalidText={formErrors.displayName}
              required
            />
            <TextInput
              id="validationPattern"
              labelText={intl.formatMessage({
                id: "customFieldType.form.validationPattern",
                defaultMessage: "Validation Pattern (Regex)",
              })}
              placeholder={intl.formatMessage({
                id: "customFieldType.form.validationPatternPlaceholder",
                defaultMessage: "e.g., ^[0-9]+$",
              })}
              value={formData.validationPattern}
              onChange={(e) =>
                setFormData({ ...formData, validationPattern: e.target.value })
              }
              invalid={!!formErrors.validationPattern}
              invalidText={formErrors.validationPattern}
              helperText={intl.formatMessage({
                id: "customFieldType.form.validationPatternHelper",
                defaultMessage:
                  "Optional: Regular expression pattern for validation",
              })}
            />
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="valueRangeMin"
                  label={intl.formatMessage({
                    id: "customFieldType.form.valueRangeMin",
                    defaultMessage: "Value Range Min",
                  })}
                  placeholder="0"
                  value={formData.valueRangeMin}
                  onChange={(e) =>
                    setFormData({ ...formData, valueRangeMin: e.target.value })
                  }
                  helperText={intl.formatMessage({
                    id: "customFieldType.form.valueRangeMinHelper",
                    defaultMessage: "Optional: Minimum value for numeric types",
                  })}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="valueRangeMax"
                  label={intl.formatMessage({
                    id: "customFieldType.form.valueRangeMax",
                    defaultMessage: "Value Range Max",
                  })}
                  placeholder="100"
                  value={formData.valueRangeMax}
                  onChange={(e) =>
                    setFormData({ ...formData, valueRangeMax: e.target.value })
                  }
                  invalid={!!formErrors.valueRangeMax}
                  invalidText={formErrors.valueRangeMax}
                  helperText={intl.formatMessage({
                    id: "customFieldType.form.valueRangeMaxHelper",
                    defaultMessage: "Optional: Maximum value for numeric types",
                  })}
                />
              </Column>
            </Grid>
            <TextInput
              id="allowedCharacters"
              labelText={intl.formatMessage({
                id: "customFieldType.form.allowedCharacters",
                defaultMessage: "Allowed Characters",
              })}
              placeholder={intl.formatMessage({
                id: "customFieldType.form.allowedCharactersPlaceholder",
                defaultMessage: "e.g., ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
              })}
              value={formData.allowedCharacters}
              onChange={(e) =>
                setFormData({ ...formData, allowedCharacters: e.target.value })
              }
              helperText={intl.formatMessage({
                id: "customFieldType.form.allowedCharactersHelper",
                defaultMessage: "Optional: Character set restriction",
              })}
            />
            <Toggle
              id="isActive"
              labelText={intl.formatMessage({
                id: "customFieldType.form.isActive",
                defaultMessage: "Active Status",
              })}
              toggled={formData.isActive}
              onToggle={(checked) =>
                setFormData({ ...formData, isActive: checked })
              }
            />
          </FormGroup>
        </Form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteModal.open}
        modalHeading={intl.formatMessage({
          id: "customFieldType.modal.deleteTitle",
          defaultMessage: "Delete Custom Field Type",
        })}
        primaryButtonText={intl.formatMessage({
          id: "customFieldType.modal.delete",
          defaultMessage: "Delete",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "customFieldType.modal.cancel",
          defaultMessage: "Cancel",
        })}
        danger
        onRequestSubmit={handleDelete}
        onRequestClose={() => setDeleteModal({ open: false, type: null })}
      >
        <p>
          {intl.formatMessage(
            {
              id: "customFieldType.modal.deleteMessage",
              defaultMessage:
                "Are you sure you want to delete {typeName}? This action cannot be undone.",
            },
            {
              typeName:
                deleteModal.type?.displayName || deleteModal.type?.typeName,
            },
          )}
        </p>
      </Modal>
    </div>
  );
};

export default CustomFieldTypeManagement;
