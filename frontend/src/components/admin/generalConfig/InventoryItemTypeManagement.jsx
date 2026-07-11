import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Loading,
  Modal,
  NumberInput,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  TextInput,
  Tile,
  Toggle,
} from "@carbon/react";
import { Add, Close, Edit, Save } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import { getFromOpenElisServer } from "../../utils/Utils";
import { InventoryItemTypeAPI } from "../../inventory/InventoryService";
import "../../Style.css";

const DEFAULT_LOCALE = { localeCode: "en", displayName: "English" };

const StatusTag = ({ active }) =>
  active ? (
    <Tag type="green">
      <FormattedMessage id="label.active" defaultMessage="Active" />
    </Tag>
  ) : (
    <Tag type="gray">
      <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
    </Tag>
  );

function EditPanel({
  row,
  isNew,
  activeLocale,
  saving,
  onSave,
  onCancel,
  onDeactivate,
}) {
  const intl = useIntl();
  const [draft, setDraft] = useState({
    ...row,
    nameInLocale: row.localized?.[activeLocale.localeCode] || row.name || "",
  });

  const codeHint = isNew
    ? intl.formatMessage({
        id: "inventoryItemType.hint.code.new",
        defaultMessage:
          "Stable identifier used by integrations. Leave blank and we'll generate one from the name.",
      })
    : intl.formatMessage({
        id: "inventoryItemType.hint.code.locked",
        defaultMessage:
          "Code is locked once saved so integrations and existing items keep working.",
      });

  const nameHint = row.seeded
    ? intl.formatMessage({
        id: "inventoryItemType.hint.name.seeded",
        defaultMessage:
          "You can rename this seeded type for the current locale. Translations for other languages are managed separately.",
      })
    : intl.formatMessage({
        id: "inventoryItemType.hint.name.custom",
        defaultMessage:
          "Saves as the name in your active locale. Other locales fall back to this value until separately translated.",
      });

  return (
    <Tile style={{ padding: "1.25rem" }}>
      <Grid narrow>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id={`itemtype-code-${row.id}`}
            labelText={intl.formatMessage({
              id: "inventoryItemType.label.code",
              defaultMessage: "Code",
            })}
            value={draft.code}
            disabled={!isNew}
            placeholder={
              isNew
                ? intl.formatMessage({
                    id: "inventoryItemType.placeholder.code",
                    defaultMessage: "Leave blank to auto-generate from name",
                  })
                : ""
            }
            helperText={codeHint}
            onChange={(e) =>
              setDraft({ ...draft, code: e.target.value.toUpperCase() })
            }
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id={`itemtype-name-${row.id}`}
            labelText={`${intl.formatMessage({
              id: "inventoryItemType.label.name",
              defaultMessage: "Name",
            })} · ${intl.formatMessage({
              id: "inventoryItemType.label.editingIn",
              defaultMessage: "Editing in",
            })} ${activeLocale.displayName}`}
            value={draft.nameInLocale}
            onChange={(e) =>
              setDraft({ ...draft, nameInLocale: e.target.value })
            }
            helperText={nameHint}
          />
        </Column>
        <Column
          lg={isNew ? 4 : 8}
          md={isNew ? 2 : 4}
          sm={isNew ? 2 : 4}
          style={{ marginTop: "1rem" }}
        >
          <NumberInput
            id={`itemtype-sortorder-${row.id}`}
            label={intl.formatMessage({
              id: "inventoryItemType.label.sortOrder",
              defaultMessage: "Sort order",
            })}
            value={draft.sortOrder}
            min={0}
            step={10}
            onChange={(e, { value }) =>
              setDraft({ ...draft, sortOrder: value })
            }
          />
        </Column>
        {isNew && (
          <Column lg={4} md={2} sm={2} style={{ marginTop: "1rem" }}>
            <Toggle
              id={`itemtype-active-${row.id}`}
              labelText={intl.formatMessage({
                id: "inventoryItemType.label.status",
                defaultMessage: "Status",
              })}
              labelA={intl.formatMessage({
                id: "label.inactive",
                defaultMessage: "Inactive",
              })}
              labelB={intl.formatMessage({
                id: "label.active",
                defaultMessage: "Active",
              })}
              toggled={draft.active}
              onToggle={(checked) => setDraft({ ...draft, active: checked })}
            />
          </Column>
        )}
      </Grid>

      <Stack
        orientation="horizontal"
        gap={3}
        style={{
          marginTop: "1.25rem",
          paddingTop: "1.25rem",
          borderTop: "1px solid var(--cds-border-subtle)",
        }}
      >
        <Button
          kind="primary"
          size="md"
          renderIcon={Save}
          disabled={saving || !draft.nameInLocale?.trim()}
          onClick={() => onSave(draft)}
        >
          {isNew
            ? intl.formatMessage({
                id: "inventoryItemType.button.addItemType",
                defaultMessage: "Add item type",
              })
            : intl.formatMessage({
                id: "label.button.save",
                defaultMessage: "Save changes",
              })}
        </Button>
        <Button kind="ghost" size="md" onClick={onCancel} disabled={saving}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <div style={{ flex: 1 }} />
        {!isNew && draft.active && (
          <Button
            kind="danger--tertiary"
            size="md"
            disabled={saving}
            onClick={() => onDeactivate(draft)}
          >
            <FormattedMessage
              id="inventoryItemType.button.deactivate"
              defaultMessage="Deactivate"
            />
          </Button>
        )}
      </Stack>
    </Tile>
  );
}

function InventoryItemTypeManagement() {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [rows, setRows] = useState([]);
  const [locales, setLocales] = useState([DEFAULT_LOCALE]);
  const [activeLocale, setActiveLocale] = useState(DEFAULT_LOCALE);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const [adding, setAdding] = useState(false);
  const [search, setSearch] = useState("");
  const [confirmDeactivate, setConfirmDeactivate] = useState(null);
  const [saving, setSaving] = useState(false);

  const notify = (kind, title, message) => {
    setNotificationVisible(true);
    addNotification({ kind, title, message });
  };

  const fetchItemTypes = () => {
    InventoryItemTypeAPI.getAll()
      .then((data) => {
        if (componentMounted.current) {
          setRows(Array.isArray(data) ? data : []);
        }
      })
      .catch(() => {
        if (componentMounted.current) {
          notify(
            NotificationKinds.error,
            intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            intl.formatMessage({
              id: "inventoryItemType.notify.loadError",
              defaultMessage: "Failed to load inventory item types",
            }),
          );
        }
      });
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/supportedlocales", (localeRes) => {
      if (!componentMounted.current) {
        return;
      }
      const activeLocales = (Array.isArray(localeRes) ? localeRes : []).filter(
        (l) => l.active,
      );
      activeLocales.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
      const resolvedLocales = activeLocales.length
        ? activeLocales
        : [DEFAULT_LOCALE];
      setLocales(resolvedLocales);
      setActiveLocale(
        resolvedLocales.find((l) => l.fallback) || resolvedLocales[0],
      );
      fetchItemTypes();
      setLoading(false);
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const nameFor = (row) =>
    (activeLocale && row.localized?.[activeLocale.localeCode]) || row.name;

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter(
      (r) =>
        r.code.toLowerCase().includes(q) ||
        nameFor(r).toLowerCase().includes(q),
    );
  }, [rows, search, activeLocale]);

  if (loading) {
    return <Loading />;
  }

  const blankRow = {
    id: "new",
    code: "",
    name: "",
    localized: {},
    active: true,
    sortOrder: (rows.length + 1) * 10,
    seeded: false,
  };

  const handleSaveNew = (draft) => {
    setSaving(true);
    InventoryItemTypeAPI.create({
      code: draft.code?.trim() || null,
      name: draft.nameInLocale.trim(),
      locale: activeLocale.localeCode,
      sortOrder: draft.sortOrder,
      active: draft.active,
    })
      .then((created) => {
        setAdding(false);
        fetchItemTypes();
        notify(
          NotificationKinds.success,
          intl.formatMessage({
            id: "inventoryItemType.notify.added.title",
            defaultMessage: "Item type added",
          }),
          `${created.name} (${created.code})`,
        );
      })
      .catch((err) => {
        // err.errorCode (OGC-658 C8) is an en.json message id set by
        // InventoryItemTypeRestController for validation failures
        // (duplicate/malformed code, missing name) — prefer it over
        // err.message, which is the untranslated backend fallback string.
        notify(
          NotificationKinds.error,
          intl.formatMessage({
            id: "inventoryItemType.notify.saveError",
            defaultMessage: "Failed to save item type",
          }),
          err.errorCode
            ? intl.formatMessage({ id: err.errorCode }, err.params)
            : err.message,
        );
      })
      .finally(() => setSaving(false));
  };

  const handleSaveEdit = (draft) => {
    setSaving(true);
    InventoryItemTypeAPI.update(draft.id, {
      name: draft.nameInLocale.trim(),
      locale: activeLocale.localeCode,
      sortOrder: draft.sortOrder,
    })
      .then(() => {
        setExpandedId(null);
        fetchItemTypes();
        notify(
          NotificationKinds.success,
          intl.formatMessage({
            id: "inventoryItemType.notify.saved.title",
            defaultMessage: "Changes saved",
          }),
          `${draft.nameInLocale} · ${activeLocale.displayName}`,
        );
      })
      .catch((err) => {
        notify(
          NotificationKinds.error,
          intl.formatMessage({
            id: "inventoryItemType.notify.saveError",
            defaultMessage: "Failed to save item type",
          }),
          err.message,
        );
      })
      .finally(() => setSaving(false));
  };

  const confirmDeactivation = () => {
    if (!confirmDeactivate) {
      return;
    }
    setSaving(true);
    InventoryItemTypeAPI.deactivate(confirmDeactivate.id)
      .then(() => {
        setConfirmDeactivate(null);
        setExpandedId(null);
        fetchItemTypes();
        notify(
          NotificationKinds.info,
          intl.formatMessage({
            id: "inventoryItemType.notify.deactivated.title",
            defaultMessage: "Item type deactivated",
          }),
          intl.formatMessage({
            id: "inventoryItemType.notify.deactivated.body",
            defaultMessage:
              "Existing inventory items keep this type. It will no longer appear for new items.",
          }),
        );
      })
      .catch((err) => {
        notify(
          NotificationKinds.error,
          intl.formatMessage({
            id: "inventoryItemType.notify.saveError",
            defaultMessage: "Failed to save item type",
          }),
          err.message,
        );
      })
      .finally(() => setSaving(false));
  };

  const headers = [
    {
      key: "code",
      header: intl.formatMessage({
        id: "inventoryItemType.col.code",
        defaultMessage: "Code",
      }),
    },
    {
      key: "name",
      header: `${intl.formatMessage({ id: "inventoryItemType.col.name", defaultMessage: "Name" })} (${activeLocale.displayName})`,
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "inventoryItemType.col.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "sortOrder",
      header: intl.formatMessage({
        id: "inventoryItemType.col.sortOrder",
        defaultMessage: "Sort order",
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

  return (
    <div className="adminPageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "admin.label", link: "/admin" },
          {
            label: "inventoryItemType.page.title",
            link: "/admin/InventoryItemTypeManagement",
          },
        ]}
      />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2 style={{ margin: "0.5rem 0 0.25rem" }}>
            <FormattedMessage
              id="inventoryItemType.page.title"
              defaultMessage="Inventory Item Types"
            />
          </h2>
          <p
            style={{
              color: "var(--cds-text-secondary)",
              fontSize: "0.875rem",
              marginBottom: "1.5rem",
              maxWidth: "70ch",
            }}
          >
            <FormattedMessage
              id="inventoryItemType.page.help"
              defaultMessage="Manage the list of types available when creating an inventory item. Adding, renaming, or deactivating a type takes effect on the Inventory Catalog form immediately, with no code change or redeploy required."
            />
          </p>

          <TableContainer
            title={intl.formatMessage({
              id: "inventoryItemType.table.title",
              defaultMessage: "Item types",
            })}
            description={`${filtered.length} ${intl.formatMessage({ id: "of", defaultMessage: "of" })} ${rows.length}`}
          >
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  placeholder={intl.formatMessage({
                    id: "inventoryItemType.toolbar.search.placeholder",
                    defaultMessage: "Search by code or name",
                  })}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                <Select
                  id="inventory-item-type-active-locale"
                  labelText=""
                  hideLabel
                  inline
                  size="md"
                  value={activeLocale.localeCode}
                  onChange={(e) => {
                    const next = locales.find(
                      (l) => l.localeCode === e.target.value,
                    );
                    if (next) {
                      setActiveLocale(next);
                    }
                  }}
                >
                  {locales.map((loc) => (
                    <SelectItem
                      key={loc.localeCode}
                      value={loc.localeCode}
                      text={`${intl.formatMessage({
                        id: "inventoryItemType.toolbar.locale.label",
                        defaultMessage: "Editing in:",
                      })} ${loc.displayName}`}
                    />
                  ))}
                </Select>
                <Button
                  kind="primary"
                  renderIcon={Add}
                  onClick={() => {
                    setAdding(true);
                    setExpandedId(null);
                  }}
                  disabled={adding}
                >
                  <FormattedMessage
                    id="inventoryItemType.button.addItemType"
                    defaultMessage="Add item type"
                  />
                </Button>
              </TableToolbarContent>
            </TableToolbar>

            <Table>
              <TableHead>
                <TableRow>
                  {headers.map((h) => (
                    <TableHeader key={h.key}>{h.header}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {adding && (
                  <>
                    <TableRow
                      style={{ background: "var(--cds-layer-accent-01)" }}
                    >
                      <TableCell colSpan={headers.length}>
                        <FormattedMessage
                          id="inventoryItemType.table.newRow"
                          defaultMessage="New inventory item type"
                        />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell
                        colSpan={headers.length}
                        style={{ padding: 0 }}
                      >
                        <EditPanel
                          row={blankRow}
                          isNew
                          saving={saving}
                          activeLocale={activeLocale}
                          onSave={handleSaveNew}
                          onCancel={() => setAdding(false)}
                        />
                      </TableCell>
                    </TableRow>
                  </>
                )}
                {filtered.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableRow>
                      <TableCell>
                        <code>{row.code}</code>
                      </TableCell>
                      <TableCell>{nameFor(row)}</TableCell>
                      <TableCell>
                        <StatusTag active={row.active} />
                      </TableCell>
                      <TableCell>{row.sortOrder}</TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={expandedId === row.id ? Close : Edit}
                          onClick={() => {
                            setExpandedId(
                              expandedId === row.id ? null : row.id,
                            );
                            setAdding(false);
                          }}
                        >
                          {expandedId === row.id
                            ? intl.formatMessage({
                                id: "label.button.close",
                                defaultMessage: "Close",
                              })
                            : intl.formatMessage({
                                id: "label.edit",
                                defaultMessage: "Edit",
                              })}
                        </Button>
                      </TableCell>
                    </TableRow>
                    {expandedId === row.id && (
                      <TableRow>
                        <TableCell
                          colSpan={headers.length}
                          style={{ padding: 0 }}
                        >
                          <EditPanel
                            row={row}
                            isNew={false}
                            saving={saving}
                            activeLocale={activeLocale}
                            onSave={handleSaveEdit}
                            onCancel={() => setExpandedId(null)}
                            onDeactivate={setConfirmDeactivate}
                          />
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Column>
      </Grid>

      {confirmDeactivate && (
        <Modal
          open
          danger
          modalHeading={intl.formatMessage({
            id: "inventoryItemType.modal.deactivate.title",
            defaultMessage: "Deactivate item type?",
          })}
          modalLabel={`${confirmDeactivate.code} — ${confirmDeactivate.name}`}
          primaryButtonText={intl.formatMessage({
            id: "inventoryItemType.button.deactivateConfirm",
            defaultMessage: "Yes, deactivate",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
            defaultMessage: "Cancel",
          })}
          primaryButtonDisabled={saving}
          onRequestClose={() => setConfirmDeactivate(null)}
          onRequestSubmit={confirmDeactivation}
        >
          <p>
            <FormattedMessage
              id="inventoryItemType.modal.deactivate.body"
              defaultMessage="Deactivating this type will hide it from the Inventory Catalog dropdown for new items. Existing inventory items already using this type keep their assignment."
            />
          </p>
        </Modal>
      )}
    </div>
  );
}

export default InventoryItemTypeManagement;
