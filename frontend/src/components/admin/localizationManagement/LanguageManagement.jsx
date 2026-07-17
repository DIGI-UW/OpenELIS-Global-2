import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Modal,
  TextInput,
  Toggle,
  NumberInput,
  Loading,
  InlineNotification,
  Tag,
} from "@carbon/react";
import { Add, Edit, TrashCan, Star, StarFilled } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
  deleteFromOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const LanguageManagement = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [locales, setLocales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingLocale, setEditingLocale] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [formData, setFormData] = useState({
    localeCode: "",
    displayName: "",
    active: true,
    fallback: false,
    sortOrder: 0,
  });
  const [formErrors, setFormErrors] = useState({});

  useEffect(() => {
    fetchLocales();
  }, []);

  const fetchLocales = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/supportedlocales", (response) => {
      if (response) {
        setLocales(response);
      }
      setLoading(false);
    });
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.localeCode || formData.localeCode.trim() === "") {
      errors.localeCode = intl.formatMessage({
        id: "error.field.required",
        defaultMessage: "This field is required",
      });
    } else if (!/^[a-z]{2}(-[A-Z]{2})?$/.test(formData.localeCode)) {
      errors.localeCode = intl.formatMessage({
        id: "error.locale.format",
        defaultMessage:
          "Invalid format. Use: en, fr, es, or en-US, fr-CA, etc.",
      });
    }
    if (!formData.displayName || formData.displayName.trim() === "") {
      errors.displayName = intl.formatMessage({
        id: "error.field.required",
        defaultMessage: "This field is required",
      });
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleAdd = () => {
    setEditingLocale(null);
    setFormData({
      localeCode: "",
      displayName: "",
      active: true,
      fallback: false,
      sortOrder: locales.length + 1,
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleEdit = (locale) => {
    setEditingLocale(locale);
    setFormData({
      localeCode: locale.localeCode,
      displayName: locale.displayName,
      active: locale.active,
      fallback: locale.fallback,
      sortOrder: locale.sortOrder,
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleDelete = (locale) => {
    if (locale.fallback) {
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "locale.delete.fallback.error",
          defaultMessage:
            "Cannot delete the fallback language. Set another language as fallback first.",
        }),
      });
      return;
    }
    setDeleteTarget(locale);
    setIsDeleteModalOpen(true);
  };

  const confirmDelete = () => {
    if (!deleteTarget) return;

    deleteFromOpenElisServerFullResponse(
      `/rest/supportedlocales/${deleteTarget.id}`,
      (response) => {
        setIsDeleteModalOpen(false);
        setDeleteTarget(null);
        if (response.ok) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "locale.delete.success",
              defaultMessage: "Language deleted successfully",
            }),
          });
          fetchLocales();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "locale.delete.error",
              defaultMessage: "Failed to delete language",
            }),
          });
        }
      },
    );
  };

  const handleSetFallback = (locale) => {
    postToOpenElisServerFullResponse(
      `/rest/supportedlocales/${locale.id}/setFallback`,
      null,
      (response) => {
        if (response.ok) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "locale.fallback.success",
              defaultMessage: "Fallback language updated",
            }),
          });
          fetchLocales();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "locale.fallback.error",
              defaultMessage: "Failed to set fallback language",
            }),
          });
        }
      },
    );
  };

  const handleSave = () => {
    if (!validateForm()) return;

    const url = editingLocale
      ? `/rest/supportedlocales/${editingLocale.id}`
      : "/rest/supportedlocales";

    const callback = (response) => {
      if (response.ok) {
        addNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          message: editingLocale
            ? intl.formatMessage({
                id: "locale.update.success",
                defaultMessage: "Language updated successfully",
              })
            : intl.formatMessage({
                id: "locale.create.success",
                defaultMessage: "Language created successfully",
              }),
        });
        setIsModalOpen(false);
        fetchLocales();
      } else {
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "locale.save.error",
            defaultMessage: "Failed to save language",
          }),
        });
      }
    };

    if (editingLocale) {
      putToOpenElisServerFullResponse(url, JSON.stringify(formData), callback);
    } else {
      postToOpenElisServerFullResponse(url, JSON.stringify(formData), callback);
    }
  };

  const headers = [
    {
      key: "localeCode",
      header: intl.formatMessage({
        id: "locale.code",
        defaultMessage: "Locale Code",
      }),
    },
    {
      key: "displayName",
      header: intl.formatMessage({
        id: "locale.displayName",
        defaultMessage: "Display Name",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "locale.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "sortOrder",
      header: intl.formatMessage({
        id: "locale.sortOrder",
        defaultMessage: "Sort Order",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "admin.label", link: "/admin" },
    { label: "locale.management.title", link: "/admin/languageManagement" },
  ];

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="adminPageContent">
        <div className="orderLegendBody">
          <h2>
            <FormattedMessage
              id="locale.management.title"
              defaultMessage="Language Management"
            />
          </h2>
          <p>
            <FormattedMessage
              id="locale.management.description"
              defaultMessage="Manage supported languages for metadata translations."
            />
          </p>

          <div style={{ marginBottom: "1rem" }}>
            <Button
              kind="primary"
              renderIcon={Add}
              onClick={handleAdd}
              size="sm"
            >
              <FormattedMessage id="locale.add" defaultMessage="Add Language" />
            </Button>
          </div>

          <DataTable rows={locales} headers={headers}>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
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
                    {locales.map((locale) => (
                      <TableRow key={locale.id}>
                        <TableCell>
                          {locale.localeCode}
                          {locale.fallback && (
                            <Tag type="blue" style={{ marginLeft: "0.5rem" }}>
                              <FormattedMessage
                                id="locale.fallback"
                                defaultMessage="Fallback"
                              />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>{locale.displayName}</TableCell>
                        <TableCell>
                          {locale.active ? (
                            <Tag type="green">
                              <FormattedMessage
                                id="label.active"
                                defaultMessage="Active"
                              />
                            </Tag>
                          ) : (
                            <Tag type="gray">
                              <FormattedMessage
                                id="label.inactive"
                                defaultMessage="Inactive"
                              />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>{locale.sortOrder}</TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={Edit}
                            iconDescription={intl.formatMessage({
                              id: "label.edit",
                              defaultMessage: "Edit",
                            })}
                            onClick={() => handleEdit(locale)}
                          />
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={locale.fallback ? StarFilled : Star}
                            iconDescription={intl.formatMessage({
                              id: "locale.setFallback",
                              defaultMessage: "Set as Fallback",
                            })}
                            onClick={() => handleSetFallback(locale)}
                            disabled={locale.fallback}
                          />
                          <Button
                            kind="danger--ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={TrashCan}
                            iconDescription={intl.formatMessage({
                              id: "label.delete",
                              defaultMessage: "Delete",
                            })}
                            onClick={() => handleDelete(locale)}
                            disabled={locale.fallback}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </div>
      </div>

      {/* Add/Edit Modal */}
      <Modal
        open={isModalOpen}
        modalHeading={
          editingLocale
            ? intl.formatMessage({
                id: "locale.edit.title",
                defaultMessage: "Edit Language",
              })
            : intl.formatMessage({
                id: "locale.add.title",
                defaultMessage: "Add Language",
              })
        }
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setIsModalOpen(false)}
        onRequestSubmit={handleSave}
      >
        <TextInput
          id="localeCode"
          labelText={intl.formatMessage({
            id: "locale.code",
            defaultMessage: "Locale Code",
          })}
          helperText={intl.formatMessage({
            id: "locale.code.helper",
            defaultMessage: "e.g., en, fr, es, pt",
          })}
          value={formData.localeCode}
          onChange={(e) =>
            setFormData({ ...formData, localeCode: e.target.value })
          }
          invalid={!!formErrors.localeCode}
          invalidText={formErrors.localeCode}
          disabled={!!editingLocale}
        />
        <TextInput
          id="displayName"
          labelText={intl.formatMessage({
            id: "locale.displayName",
            defaultMessage: "Display Name",
          })}
          helperText={intl.formatMessage({
            id: "locale.displayName.helper",
            defaultMessage: "e.g., English, Francais, Espanol",
          })}
          value={formData.displayName}
          onChange={(e) =>
            setFormData({ ...formData, displayName: e.target.value })
          }
          invalid={!!formErrors.displayName}
          invalidText={formErrors.displayName}
          style={{ marginTop: "1rem" }}
        />
        <NumberInput
          id="sortOrder"
          label={intl.formatMessage({
            id: "locale.sortOrder",
            defaultMessage: "Sort Order",
          })}
          value={formData.sortOrder}
          onChange={(e, { value }) =>
            setFormData({ ...formData, sortOrder: value })
          }
          min={0}
          style={{ marginTop: "1rem" }}
        />
        <Toggle
          id="active"
          labelText={intl.formatMessage({
            id: "locale.active",
            defaultMessage: "Active",
          })}
          labelA={intl.formatMessage({
            id: "label.no",
            defaultMessage: "No",
          })}
          labelB={intl.formatMessage({
            id: "label.yes",
            defaultMessage: "Yes",
          })}
          toggled={formData.active}
          onToggle={(checked) => setFormData({ ...formData, active: checked })}
          style={{ marginTop: "1rem" }}
        />
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={isDeleteModalOpen}
        danger
        modalHeading={intl.formatMessage({
          id: "locale.delete.confirm.title",
          defaultMessage: "Delete Language",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.delete",
          defaultMessage: "Delete",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setIsDeleteModalOpen(false)}
        onRequestSubmit={confirmDelete}
      >
        <p>
          <FormattedMessage
            id="locale.delete.confirm.message"
            defaultMessage="Are you sure you want to delete this language? This action cannot be undone."
          />
        </p>
        {deleteTarget && (
          <p style={{ marginTop: "1rem" }}>
            <strong>
              {deleteTarget.displayName} ({deleteTarget.localeCode})
            </strong>
          </p>
        )}
      </Modal>
    </>
  );
};

export default LanguageManagement;
