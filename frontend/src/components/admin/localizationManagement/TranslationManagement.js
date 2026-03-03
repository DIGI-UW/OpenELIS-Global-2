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
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Loading,
  ProgressBar,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tag,
  Pagination,
} from "@carbon/react";
import { Edit, Download, Upload, WarningAlt } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServerFullResponse } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const TranslationManagement = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [locales, setLocales] = useState([]);
  const [localizations, setLocalizations] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedLocale, setSelectedLocale] = useState("");
  const [searchText, setSearchText] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [editValues, setEditValues] = useState({});
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [filterMissing, setFilterMissing] = useState(false);

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
    getFromOpenElisServer("/rest/supportedlocales/active", (response) => {
      if (response) {
        setLocales(response);
        if (response.length > 0) {
          // Default to first non-fallback locale, or first locale
          const defaultLocale =
            response.find((l) => !l.fallback) || response[0];
          setSelectedLocale(defaultLocale.localeCode);
        }
      }
    });
  };

  const fetchStats = () => {
    getFromOpenElisServer("/rest/localizations/stats", (response) => {
      if (response) {
        setStats(response);
      }
    });
  };

  const fetchLocalizations = () => {
    setLoading(true);
    const url = filterMissing
      ? `/rest/localizations/missing/${selectedLocale}`
      : "/rest/localizations";

    getFromOpenElisServer(url, (response) => {
      if (response) {
        setLocalizations(response);
      }
      setLoading(false);
    });
  };

  const handleEdit = (item) => {
    setEditingItem(item);
    setEditValues(item.translations || {});
    setIsEditModalOpen(true);
  };

  const handleSave = () => {
    if (!editingItem) return;

    putToOpenElisServerFullResponse(
      `/rest/localizations/${editingItem.id}/translations`,
      JSON.stringify(editValues),
      (response) => {
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
      }
    );
  };

  const handleExport = () => {
    if (!selectedLocale) return;

    getFromOpenElisServer(
      `/rest/localizations/export/${selectedLocale}`,
      (response) => {
        if (response) {
          // Convert to CSV
          const headers = ["ID", "Description", "Fallback Value", "Translation"];
          const rows = response.map((item) => [
            item.id,
            item.description || "",
            item.fallbackValue || "",
            item.translatedValue || "",
          ]);

          const csv = [
            headers.join(","),
            ...rows.map((row) =>
              row.map((cell) => `"${(cell || "").replace(/"/g, '""')}"`).join(",")
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
      }
    );
  };

  const filteredLocalizations = localizations.filter((item) => {
    if (!searchText) return true;
    const search = searchText.toLowerCase();
    return (
      (item.description && item.description.toLowerCase().includes(search)) ||
      (item.id && item.id.includes(search)) ||
      Object.values(item.translations || {}).some(
        (val) => val && val.toLowerCase().includes(search)
      )
    );
  });

  const paginatedLocalizations = filteredLocalizations.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
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
                {Object.entries(stats.localeStats || {}).map(
                  ([code, stat]) => (
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
                  )
                )}
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
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                  onInputChange,
                }) => (
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
                              {item.translations?.en || "-"}
                            </TableCell>
                            <TableCell>
                              {item.translations?.[selectedLocale] ? (
                                item.translations[selectedLocale]
                              ) : (
                                <Tag type="red">
                                  <FormattedMessage
                                    id="translation.missing"
                                    defaultMessage="Missing"
                                  />
                                </Tag>
                              )}
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

            {locales.map((locale) => (
              <TextArea
                key={locale.localeCode}
                id={`translation-${locale.localeCode}`}
                labelText={`${locale.displayName} (${locale.localeCode})`}
                value={editValues[locale.localeCode] || ""}
                onChange={(e) =>
                  setEditValues({
                    ...editValues,
                    [locale.localeCode]: e.target.value,
                  })
                }
                rows={2}
                style={{ marginBottom: "1rem" }}
              />
            ))}
          </>
        )}
      </Modal>
    </>
  );
};

export default TranslationManagement;
