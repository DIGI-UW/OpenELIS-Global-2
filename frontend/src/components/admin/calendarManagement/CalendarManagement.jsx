import React, { useState, useEffect, useContext, useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  Dropdown,
  Tag,
  InlineNotification,
  DataTableSkeleton,
  Modal,
  DatePicker,
  DatePickerInput,
  TextInput,
  Checkbox,
} from "@carbon/react";
import { Add, Edit, TrashCan, Upload, Download } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../utils/Utils";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import WeekendConfig from "./WeekendConfig";
import CsvImportPreview from "./CsvImportPreview";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  id: String(currentYear - 1 + i),
  text: String(currentYear - 1 + i),
}));

function CalendarManagement() {
  const intl = useIntl();

  const headers = [
    {
      key: "date",
      header: intl.formatMessage({ id: "calendar.management.column.date" }),
    },
    {
      key: "name",
      header: intl.formatMessage({
        id: "calendar.management.column.holidayName",
      }),
    },
    {
      key: "recurring",
      header: intl.formatMessage({
        id: "calendar.management.column.recurring",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "calendar.management.column.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "calendar.management.column.actions" }),
    },
  ];
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [holidays, setHolidays] = useState([]);
  const [loading, setLoading] = useState(true);
  const [year, setYear] = useState(String(currentYear));
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({
    date: "",
    name: "",
    recurring: false,
  });
  const [isAdding, setIsAdding] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(null);
  const [showImportModal, setShowImportModal] = useState(false);
  const [error, setError] = useState(null);

  const fetchHolidays = useCallback(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      `/rest/calendar/holidays?year=${year}&includeInactive=true`,
      (res) => {
        if (res) {
          setHolidays(res.holidays || []);
        } else {
          setError(intl.formatMessage({ id: "calendar.management.loadError" }));
        }
        setLoading(false);
      },
    );
  }, [year, intl]);

  useEffect(() => {
    fetchHolidays();
  }, [fetchHolidays]);

  const handleAdd = () => {
    setIsAdding(true);
    setEditForm({ date: "", name: "", recurring: false });
    setEditingId(null);
  };

  const handleEdit = (holiday) => {
    setEditingId(holiday.id);
    setEditForm({
      date: holiday.date,
      name: holiday.name,
      recurring: holiday.isRecurring,
    });
    setIsAdding(false);
  };

  const handleSave = () => {
    const body = {
      date: editForm.date,
      name: editForm.name,
      isRecurring: editForm.recurring,
    };

    if (isAdding) {
      postToOpenElisServerJsonResponse(
        "/rest/calendar/holidays",
        JSON.stringify(body),
        (res) => {
          if (res && !res.error) {
            fetchHolidays();
            setIsAdding(false);
            setEditForm({ date: "", name: "", recurring: false });
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({
                id: "calendar.management.saveError",
              }),
              message: res?.error || "",
            });
            setNotificationVisible(true);
          }
        },
      );
    } else {
      putToOpenElisServer(
        `/rest/calendar/holidays/${editingId}`,
        JSON.stringify(body),
        (status) => {
          if (status === 200) {
            fetchHolidays();
            setEditingId(null);
            setEditForm({ date: "", name: "", recurring: false });
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({
                id: "calendar.management.saveError",
              }),
            });
            setNotificationVisible(true);
          }
        },
      );
    }
  };

  const handleDelete = (id) => {
    deleteFromOpenElisServer(`/rest/calendar/holidays/${id}`, (status) => {
      if (status === 204 || status === 200) {
        fetchHolidays();
        setShowDeleteModal(null);
      } else {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "calendar.management.saveError" }),
        });
        setNotificationVisible(true);
      }
    });
  };

  const handleCancel = () => {
    setIsAdding(false);
    setEditingId(null);
    setEditForm({ date: "", name: "", recurring: false });
  };

  const canSave = editForm.date && editForm.name.trim();
  const isEditing = isAdding || editingId !== null;

  const rows = holidays.map((h) => ({
    id: String(h.id),
    date: h.date,
    name: h.name,
    recurring: h.isRecurring,
    isActive: h.isActive,
    isWeekendDay: h.isWeekendDay,
    dayOfWeek: h.dayOfWeek,
  }));

  const breadcrumb = [
    { label: intl.formatMessage({ id: "home.label" }), link: "/" },
    {
      label: intl.formatMessage({ id: "breadcrums.admin.managment" }),
      link: "/MasterListsPage",
    },
    {
      label: intl.formatMessage({ id: "calendar.management.title" }),
      link: "/MasterListsPage/calendarManagement",
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumb} />
      <div className="adminPageContent">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <div>
            <h2>
              <FormattedMessage id="calendar.management.title" />
            </h2>
            <p style={{ color: "var(--cds-text-secondary)", fontSize: "14px" }}>
              <FormattedMessage id="calendar.management.description" />
            </p>
          </div>
          <Button
            renderIcon={Add}
            onClick={handleAdd}
            disabled={isEditing}
            data-testid="add-holiday-button"
          >
            <FormattedMessage id="calendar.management.addHoliday" />
          </Button>
        </div>

        <div
          style={{
            display: "flex",
            gap: "1rem",
            alignItems: "flex-end",
            marginBottom: "1rem",
          }}
        >
          <Dropdown
            id="year-dropdown"
            data-testid="year-dropdown"
            titleText={intl.formatMessage({ id: "calendar.management.year" })}
            items={yearOptions}
            selectedItem={yearOptions.find((y) => y.id === year)}
            onChange={({ selectedItem }) => setYear(selectedItem.id)}
            style={{ width: "120px" }}
          />
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Upload}
              onClick={() => setShowImportModal(true)}
              disabled={isEditing}
              data-testid="import-csv-button"
            >
              <FormattedMessage id="calendar.management.importCsv" />
            </Button>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Download}
              onClick={() =>
                window.open(
                  `${config.serverBaseUrl}/rest/calendar/holidays/export?year=${year}`,
                  "_blank",
                )
              }
              disabled={isEditing}
              data-testid="export-csv-button"
            >
              <FormattedMessage id="calendar.management.exportCsv" />
            </Button>
          </div>
        </div>

        <WeekendConfig />

        {error && (
          <InlineNotification
            kind="error"
            title={error}
            onClose={() => setError(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {loading ? (
          <DataTableSkeleton headers={headers} rowCount={5} />
        ) : (
          <DataTable rows={rows} headers={headers} data-testid="holiday-table">
            {({ rows: tableRows, headers: tableHeaders, getTableProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {tableHeaders.map((header) => (
                        <TableHeader key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {isAdding && (
                      <TableRow data-testid="holiday-inline-row">
                        <TableCell>
                          <DatePicker
                            datePickerType="single"
                            onChange={([date]) =>
                              setEditForm((f) => ({
                                ...f,
                                date: date
                                  ? date.toISOString().split("T")[0]
                                  : "",
                              }))
                            }
                          >
                            <DatePickerInput
                              id="new-holiday-date"
                              placeholder="yyyy-mm-dd"
                              size="sm"
                              autoFocus
                            />
                          </DatePicker>
                        </TableCell>
                        <TableCell>
                          <TextInput
                            id="new-holiday-name"
                            value={editForm.name}
                            onChange={(e) =>
                              setEditForm((f) => ({
                                ...f,
                                name: e.target.value,
                              }))
                            }
                            placeholder={intl.formatMessage({
                              id: "calendar.management.holidayName",
                            })}
                            size="sm"
                            maxLength={100}
                          />
                        </TableCell>
                        <TableCell>
                          <Checkbox
                            id="new-holiday-recurring"
                            labelText={intl.formatMessage({
                              id: "calendar.management.annual",
                            })}
                            checked={editForm.recurring}
                            onChange={(_, { checked }) =>
                              setEditForm((f) => ({
                                ...f,
                                recurring: checked,
                              }))
                            }
                          />
                        </TableCell>
                        <TableCell>
                          <Tag type="green">
                            <FormattedMessage id="calendar.management.active" />
                          </Tag>
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            onClick={handleSave}
                            disabled={!canSave}
                            data-testid="save-holiday-button"
                          >
                            <FormattedMessage id="calendar.management.save" />
                          </Button>
                          <Button
                            size="sm"
                            kind="ghost"
                            onClick={handleCancel}
                            data-testid="cancel-holiday-button"
                          >
                            <FormattedMessage id="calendar.management.cancel" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    )}
                    {tableRows.map((row) => {
                      const holiday = holidays.find(
                        (h) => String(h.id) === row.id,
                      );
                      if (!holiday) return null;

                      if (editingId === holiday.id) {
                        return (
                          <TableRow
                            key={row.id}
                            data-testid="holiday-inline-row"
                          >
                            <TableCell>
                              <DatePicker
                                datePickerType="single"
                                value={editForm.date}
                                onChange={([date]) =>
                                  setEditForm((f) => ({
                                    ...f,
                                    date: date
                                      ? date.toISOString().split("T")[0]
                                      : "",
                                  }))
                                }
                              >
                                <DatePickerInput
                                  id="edit-holiday-date"
                                  placeholder="yyyy-mm-dd"
                                  size="sm"
                                />
                              </DatePicker>
                            </TableCell>
                            <TableCell>
                              <TextInput
                                id="edit-holiday-name"
                                value={editForm.name}
                                onChange={(e) =>
                                  setEditForm((f) => ({
                                    ...f,
                                    name: e.target.value,
                                  }))
                                }
                                size="sm"
                                maxLength={100}
                                autoFocus
                              />
                            </TableCell>
                            <TableCell>
                              <Checkbox
                                id="edit-holiday-recurring"
                                labelText={intl.formatMessage({
                                  id: "calendar.management.annual",
                                })}
                                checked={editForm.recurring}
                                onChange={(_, { checked }) =>
                                  setEditForm((f) => ({
                                    ...f,
                                    recurring: checked,
                                  }))
                                }
                              />
                            </TableCell>
                            <TableCell>
                              <Tag type={holiday.isActive ? "green" : "gray"}>
                                <FormattedMessage
                                  id={
                                    holiday.isActive
                                      ? "calendar.management.active"
                                      : "calendar.management.inactive"
                                  }
                                />
                              </Tag>
                            </TableCell>
                            <TableCell>
                              <Button
                                size="sm"
                                onClick={handleSave}
                                disabled={!canSave}
                                data-testid="save-holiday-button"
                              >
                                <FormattedMessage id="calendar.management.save" />
                              </Button>
                              <Button
                                size="sm"
                                kind="ghost"
                                onClick={handleCancel}
                                data-testid="cancel-holiday-button"
                              >
                                <FormattedMessage id="calendar.management.cancel" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        );
                      }

                      return (
                        <TableRow
                          key={row.id}
                          style={{
                            opacity: holiday.isActive ? 1 : 0.5,
                          }}
                        >
                          <TableCell>
                            <div>
                              <span style={{ fontWeight: 500 }}>
                                {holiday.date}
                              </span>
                              <br />
                              <span
                                style={{
                                  fontSize: "12px",
                                  color: holiday.isWeekendDay
                                    ? "var(--cds-support-warning)"
                                    : "var(--cds-text-helper)",
                                }}
                              >
                                {holiday.dayOfWeek}
                                {holiday.isWeekendDay && (
                                  <>
                                    {" "}
                                    (
                                    <FormattedMessage id="calendar.management.weekend" />
                                    )
                                  </>
                                )}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>{holiday.name}</TableCell>
                          <TableCell>
                            <Tag type={holiday.isRecurring ? "teal" : "gray"}>
                              <FormattedMessage
                                id={
                                  holiday.isRecurring
                                    ? "calendar.management.annual"
                                    : "calendar.management.oneTime"
                                }
                              />
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <Tag type={holiday.isActive ? "green" : "gray"}>
                              <FormattedMessage
                                id={
                                  holiday.isActive
                                    ? "calendar.management.active"
                                    : "calendar.management.inactive"
                                }
                              />
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              hasIconOnly
                              renderIcon={Edit}
                              iconDescription={intl.formatMessage({
                                id: "calendar.management.editHoliday",
                              })}
                              onClick={() => handleEdit(holiday)}
                              disabled={isEditing}
                            />
                            <Button
                              kind="ghost"
                              size="sm"
                              hasIconOnly
                              renderIcon={TrashCan}
                              iconDescription={intl.formatMessage({
                                id: "label.delete",
                              })}
                              onClick={() => setShowDeleteModal(holiday.id)}
                              disabled={isEditing}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                    {holidays.length === 0 && !isAdding && (
                      <TableRow>
                        <TableCell colSpan={5} style={{ textAlign: "center" }}>
                          <FormattedMessage
                            id="calendar.management.noHolidays"
                            values={{ year }}
                          />
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}

        <div
          style={{
            padding: "0.75rem 1rem",
            fontSize: "12px",
            color: "var(--cds-text-helper)",
          }}
          data-testid="holiday-count-footer"
        >
          <FormattedMessage
            id="calendar.management.holidayCount"
            values={{ count: holidays.length, year }}
          />
        </div>
      </div>

      <Modal
        open={showDeleteModal !== null}
        modalHeading={intl.formatMessage({
          id: "calendar.management.deleteConfirm",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.delete" })}
        secondaryButtonText={intl.formatMessage({
          id: "calendar.management.cancel",
        })}
        onRequestSubmit={() => handleDelete(showDeleteModal)}
        onRequestClose={() => setShowDeleteModal(null)}
        danger
      />

      {showImportModal && (
        <CsvImportPreview
          year={year}
          onClose={() => setShowImportModal(false)}
          onImportComplete={() => {
            setShowImportModal(false);
            fetchHolidays();
          }}
        />
      )}
    </>
  );
}

export default CalendarManagement;
