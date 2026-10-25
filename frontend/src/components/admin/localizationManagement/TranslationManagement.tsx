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
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Modal,
  TextArea,
  Select,
  SelectItem,
  Loading,
  ProgressBar,
  Tag,
  Pagination,
} from "@carbon/react";
import { Edit, Download, WarningAlt } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import PageBreadCrumb from "../../common/PageBreadCrumb";

interface SupportedLocale {
  localeCode: string;
  displayName: string;
  fallback: boolean;
}

type LabelOrigin = "SHIPPED" | "SITE_OVERRIDE" | "SITE_VALUE";

interface LocalizationItem {
  id: string;
  description?: string;
  fallbackValue?: string;
  translatedValue?: string;
  translations?: Record<string, string>;
  // Set on a value OpenELIS ships with a display key (OGC-1507).
  displayKey?: string;
  origins?: Record<string, LabelOrigin>;
  shippedLabels?: Record<string, string>;
  labels?: Record<string, string>;
}

const ORIGIN_TAG: Record<LabelOrigin, { type: string; id: string }> = {
  SHIPPED: { type: "blue", id: "translation.origin.shipped" },
  SITE_OVERRIDE: { type: "purple", id: "translation.origin.siteOverride" },
  SITE_VALUE: { type: "gray", id: "translation.origin.siteValue" },
};

/**
 * The label a language shows: for a shipped value what the server resolves (the
 * lab's override, the shipped translation, else the lab's English), or the
 * stored label of the site's own value.
 */
const effectiveLabel = (item: LocalizationItem, locale: string): string => {
  if (item.displayKey) {
    if (item.labels?.[locale] !== undefined) {
      return item.labels[locale];
    }
    if (item.origins?.[locale] !== "SITE_OVERRIDE") {
      return item.shippedLabels?.[locale] || "";
    }
  }
  return item.translations?.[locale] || "";
};

const effectiveLabels = (
  item: LocalizationItem,
  localeCodes: string[],
): Record<string, string> =>
  Object.fromEntries(
    localeCodes.map((code) => [code, effectiveLabel(item, code)]),
  );

/** A shipped value with no translation in this language shows its English. */
const isUntranslated = (item: LocalizationItem, locale: string): boolean => {
  if (!item.displayKey) {
    return !item.translations?.[locale];
  }
  return (
    locale !== "en" &&
    item.origins?.[locale] !== "SITE_OVERRIDE" &&
    (item.shippedLabels?.[locale] || "") === (item.shippedLabels?.en || "")
  );
};

interface LocaleTranslationStats {
  displayName: string;
  translated: number;
  missing: number;
  percentage: number;
}

interface TranslationStats {
  totalEntries: number;
  localeStats?: Record<string, LocaleTranslationStats>;
}

const OriginTag = ({ origin }: { origin: LabelOrigin }) => (
  <Tag type={ORIGIN_TAG[origin].type as "blue"} size="sm">
    <FormattedMessage id={ORIGIN_TAG[origin].id} />
  </Tag>
);

const TranslationManagement = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [locales, setLocales] = useState<SupportedLocale[]>([]);
  const [localizations, setLocalizations] = useState<LocalizationItem[]>([]);
  const [stats, setStats] = useState<TranslationStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedLocale, setSelectedLocale] = useState("");
  const [searchText, setSearchText] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<LocalizationItem | null>(null);
  const [editValues, setEditValues] = useState<Record<string, string>>({});
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [filterMissing, setFilterMissing] = useState(false);
  const [confirmingReset, setConfirmingReset] = useState("");

  useEffect(() => {
    fetchLocales();
    fetchStats();
  }, []);

  useEffect(() => {
    if (selectedLocale) {
      fetchLocalizations();
    }
  }, [selectedLocale, filterMissing]);

  const fetchLocales = () => {
    getFromOpenElisServer(
      "/rest/supportedlocales/active",
      (response?: SupportedLocale[]) => {
        if (response) {
          setLocales(response);
          if (response.length > 0) {
            // Default to first non-fallback locale, or first locale
            const defaultLocale =
              response.find((locale) => !locale.fallback) || response[0];
            setSelectedLocale(defaultLocale.localeCode);
          }
        }
      },
    );
  };

  const fetchStats = () => {
    getFromOpenElisServer(
      "/rest/localizations/stats",
      (response?: TranslationStats) => {
        if (response) {
          setStats(response);
        }
      },
    );
  };

  const fetchLocalizations = () => {
    setLoading(true);
    const url = filterMissing
      ? `/rest/localizations/missing/${selectedLocale}`
      : "/rest/localizations";

    getFromOpenElisServer(url, (response?: LocalizationItem[]) => {
      if (response) {
        setLocalizations(response);
      }
      setLoading(false);
    });
  };

  const localeCodes = locales.map((locale) => locale.localeCode);

  const startingValues = (item: LocalizationItem) =>
    item.displayKey
      ? effectiveLabels(item, localeCodes)
      : item.translations || {};

  const handleEdit = (item: LocalizationItem) => {
    setEditingItem(item);
    setEditValues(startingValues(item));
    setConfirmingReset("");
    setIsEditModalOpen(true);
  };

  const blankOverride = (locale: string) =>
    Boolean(editingItem?.displayKey) &&
    (editValues[locale] || "").trim() === "";

  const handleReset = (locale: string) => {
    if (!editingItem) return;
    postToOpenElisServerFullResponse(
      `/rest/localizations/${editingItem.id}/translations/${locale}/reset`,
      "{}",
      async (response?: Response) => {
        setConfirmingReset("");
        if (response?.ok) {
          const updated: LocalizationItem = await response.json();
          setEditingItem(updated);
          setEditValues(startingValues(updated));
          setLocalizations((items) =>
            items.map((item) => (item.id === updated.id ? updated : item)),
          );
          addNotification({
            kind: "success",
            title: intl.formatMessage({ id: "notification.success" }),
            message: intl.formatMessage({ id: "translation.reset.success" }),
          });
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({ id: "translation.reset.error" }),
          });
        }
      },
    );
  };

  const handleSave = () => {
    if (!editingItem) return;
    if (localeCodes.some(blankOverride)) return;

    const stored = startingValues(editingItem);
    const changed = Object.fromEntries(
      Object.entries(editValues).filter(
        ([locale, value]) => value !== (stored[locale] || ""),
      ),
    );
    putToOpenElisServerFullResponse(
      `/rest/localizations/${editingItem.id}/translations`,
      JSON.stringify(changed),
      (response: Response) => {
        if (response.ok) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "translation.update.success",
              defaultMessage: "Translation updated successfully",
            }),
          });
          setIsEditModalOpen(false);
          fetchLocalizations();
          fetchStats();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "translation.update.error",
              defaultMessage: "Failed to update translation",
            }),
          });
        }
      },
    );
  };

  const handleExport = () => {
    if (!selectedLocale) return;

    getFromOpenElisServer(
      `/rest/localizations/export/${selectedLocale}`,
      (response?: LocalizationItem[]) => {
        if (response) {
          // Convert to CSV
          const headers = [
            "ID",
            "Description",
            "Fallback Value",
            "Translation",
          ];
          const rows = response.map((item) => [
            item.id,
            item.description || "",
            item.fallbackValue || "",
            item.translatedValue || "",
          ]);

          const csv = [
            headers.join(","),
            ...rows.map((row) =>
              row
                .map((cell) => `"${(cell || "").replace(/"/g, '""')}"`)
                .join(","),
            ),
          ].join("\n");

          const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
          const link = document.createElement("a");
          link.href = URL.createObjectURL(blob);
          link.download = `translations_${selectedLocale}.csv`;
          link.click();

          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "translation.export.success",
              defaultMessage: "Translations exported successfully",
            }),
          });
        }
      },
    );
  };

  const filteredLocalizations = localizations.filter((item) => {
    if (!searchText) return true;
    const search = searchText.toLowerCase();
    return (
      (item.description && item.description.toLowerCase().includes(search)) ||
      (item.id && item.id.includes(search)) ||
      Object.values(item.translations || {}).some(
        (val) => val && val.toLowerCase().includes(search),
      )
    );
  });

  const paginatedLocalizations = filteredLocalizations.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "label.id", defaultMessage: "ID" }),
    },
    {
      key: "description",
      header: intl.formatMessage({
        id: "label.description",
        defaultMessage: "Description",
      }),
    },
    {
      key: "fallback",
      header: intl.formatMessage({
        id: "translation.fallback",
        defaultMessage: "Fallback (English)",
      }),
    },
    {
      key: "translation",
      header: intl.formatMessage({
        id: "translation.value",
        defaultMessage: "Translation",
      }),
    },
    {
      key: "origin",
      header: intl.formatMessage({ id: "translation.origin" }),
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
    {
      label: "translation.management.title",
      link: "/admin/translationManagement",
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="adminPageContent">
        <div className="orderLegendBody">
          <h2>
            <FormattedMessage
              id="translation.management.title"
              defaultMessage="Translation Management"
            />
          </h2>
          <p>
            <FormattedMessage
              id="translation.management.description"
              defaultMessage="View and edit translations for metadata."
            />
          </p>

          {/* Stats Section */}
          {stats && (
            <div style={{ marginBottom: "2rem" }}>
              <h4>
                <FormattedMessage
                  id="translation.stats.title"
                  defaultMessage="Translation Progress"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="translation.stats.total"
                  defaultMessage="Total entries: {count}"
                  values={{ count: stats.totalEntries }}
                />
              </p>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
                  gap: "1rem",
                  marginTop: "1rem",
                }}
              >
                {Object.entries(stats.localeStats || {}).map(([code, stat]) => (
                  <div
                    key={code}
                    style={{
                      padding: "1rem",
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                    }}
                  >
                    <strong>
                      {stat.displayName} ({code})
                    </strong>
                    <ProgressBar
                      label={`${stat.displayName} (${code})`}
                      hideLabel
                      value={stat.percentage}
                      max={100}
                      helperText={`${stat.translated}/${stats.totalEntries} (${stat.percentage.toFixed(1)}%)`}
                      style={{ marginTop: "0.5rem" }}
                    />
                    {stat.missing > 0 && (
                      <Tag type="red" style={{ marginTop: "0.5rem" }}>
                        {stat.missing}{" "}
                        <FormattedMessage
                          id="translation.missing"
                          defaultMessage="missing"
                        />
                      </Tag>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Controls */}
          <div
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1rem",
              alignItems: "flex-end",
            }}
          >
            <Select
              id="locale-select"
              labelText={intl.formatMessage({
                id: "translation.selectLocale",
                defaultMessage: "Select Language",
              })}
              value={selectedLocale}
              onChange={(e) => setSelectedLocale(e.target.value)}
              style={{ width: "200px" }}
            >
              {locales.map((locale) => (
                <SelectItem
                  key={locale.localeCode}
                  value={locale.localeCode}
                  text={`${locale.displayName} (${locale.localeCode})`}
                />
              ))}
            </Select>

            <Button
              kind={filterMissing ? "primary" : "tertiary"}
              size="md"
              renderIcon={WarningAlt}
              onClick={() => setFilterMissing(!filterMissing)}
            >
              <FormattedMessage
                id="translation.showMissing"
                defaultMessage="Show Missing Only"
              />
            </Button>

            <Button
              kind="tertiary"
              size="md"
              renderIcon={Download}
              onClick={handleExport}
            >
              <FormattedMessage
                id="translation.export"
                defaultMessage="Export CSV"
              />
            </Button>
          </div>

          {/* Table */}
          {loading ? (
            <Loading />
          ) : (
            <>
              <DataTable rows={paginatedLocalizations} headers={headers}>
                {({ headers, getTableProps, getHeaderProps }) => (
                  <TableContainer>
                    <TableToolbar>
                      <TableToolbarContent>
                        <TableToolbarSearch
                          onChange={(e) => setSearchText(e.target.value)}
                          placeholder={intl.formatMessage({
                            id: "translation.search",
                            defaultMessage: "Search translations...",
                          })}
                        />
                      </TableToolbarContent>
                    </TableToolbar>
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
                        {paginatedLocalizations.map((item) => (
                          <TableRow key={item.id}>
                            <TableCell>{item.id}</TableCell>
                            <TableCell>{item.description}</TableCell>
                            <TableCell>
                              {effectiveLabel(item, "en") || "-"}
                            </TableCell>
                            <TableCell>
                              {item.displayKey &&
                                effectiveLabel(item, selectedLocale)}
                              {!item.displayKey &&
                                item.translations?.[selectedLocale]}
                              {isUntranslated(item, selectedLocale) && (
                                <Tag type="red">
                                  <FormattedMessage
                                    id="translation.missing"
                                    defaultMessage="Missing"
                                  />
                                </Tag>
                              )}
                            </TableCell>
                            <TableCell>
                              <OriginTag
                                origin={
                                  item.origins?.[selectedLocale] || "SITE_VALUE"
                                }
                              />
                            </TableCell>
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
                                onClick={() => handleEdit(item)}
                              />
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>

              <Pagination
                totalItems={filteredLocalizations.length}
                pageSize={pageSize}
                pageSizes={[10, 25, 50, 100]}
                page={currentPage}
                onChange={({ page, pageSize: newPageSize }) => {
                  setCurrentPage(page);
                  setPageSize(newPageSize);
                }}
              />
            </>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      <Modal
        open={isEditModalOpen}
        modalHeading={intl.formatMessage({
          id: "translation.edit.title",
          defaultMessage: "Edit Translations",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setIsEditModalOpen(false)}
        onRequestSubmit={handleSave}
        primaryButtonDisabled={localeCodes.some(blankOverride)}
        size="lg"
      >
        {editingItem && (
          <>
            <p style={{ marginBottom: "1rem" }}>
              <strong>ID:</strong> {editingItem.id}
            </p>
            <p style={{ marginBottom: "1rem" }}>
              <strong>
                <FormattedMessage
                  id="label.description"
                  defaultMessage="Description"
                />
                :
              </strong>{" "}
              {editingItem.description}
            </p>

            {locales.map((locale) => {
              const code = locale.localeCode;
              const origin = editingItem.origins?.[code] || "SITE_VALUE";
              return (
                <div
                  key={code}
                  data-testid={`translation-row-${code}`}
                  style={{ marginBottom: "1rem" }}
                >
                  <TextArea
                    id={`translation-${code}`}
                    labelText={`${locale.displayName} (${code})`}
                    value={editValues[code] || ""}
                    onChange={(e) =>
                      setEditValues({ ...editValues, [code]: e.target.value })
                    }
                    invalid={blankOverride(code)}
                    invalidText={intl.formatMessage({
                      id: "localization.error.overrideBlank",
                    })}
                    helperText={
                      editingItem.displayKey
                        ? intl.formatMessage(
                            { id: "translation.shippedLabel" },
                            { label: editingItem.shippedLabels?.[code] || "" },
                          )
                        : undefined
                    }
                    rows={2}
                  />
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                      marginTop: "0.25rem",
                    }}
                  >
                    <OriginTag origin={origin} />
                    {origin === "SITE_OVERRIDE" && confirmingReset !== code && (
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => setConfirmingReset(code)}
                      >
                        <FormattedMessage id="translation.reset" />
                      </Button>
                    )}
                    {confirmingReset === code && (
                      <>
                        <span>
                          <FormattedMessage
                            id="translation.reset.confirm"
                            values={{
                              label: editingItem.shippedLabels?.[code] || "",
                            }}
                          />
                        </span>
                        <Button
                          kind="tertiary"
                          size="sm"
                          onClick={() => handleReset(code)}
                        >
                          <FormattedMessage id="translation.reset.confirmButton" />
                        </Button>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => setConfirmingReset("")}
                        >
                          <FormattedMessage id="label.cancel" />
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </>
        )}
      </Modal>
    </>
  );
};

export default TranslationManagement;
