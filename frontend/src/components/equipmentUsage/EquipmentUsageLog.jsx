import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Grid,
  Column,
  Loading,
  Stack,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import { Add, Edit, TrashCan, SendAlt } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EquipmentUsageModal from "./modals/EquipmentUsageModal";
import EditUsageEntryModal from "./modals/EditUsageEntryModal";
import EquipmentUsageService from "./EquipmentUsageService";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

const EquipmentUsageLog = ({ onEntrySubmitted }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [isMounted, setIsMounted] = useState(true);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  useEffect(() => {
    setIsMounted(true);
    loadDrafts();

    return () => {
      setIsMounted(false);
    };
  }, []);

  const loadDrafts = async () => {
    if (!isMounted) return;

    setLoading(true);
    try {
      const allEntries = await EquipmentUsageService.getAllUsageEntries();
      const userDrafts = allEntries
        .filter((entry) => entry.entryStatus === "DRAFT")
        .map((entry) => ({
          ...entry,
          id: String(entry.id),
        }));

      if (isMounted) {
        setEntries(userDrafts);
      }
    } catch (err) {
      console.error("Error loading drafts:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: err.message || intl.formatMessage({
            id: "equipment.usage.load.error",
            defaultMessage: "Failed to load usage entries"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    } finally {
      if (isMounted) {
        setLoading(false);
      }
    }
  };

  const handleNewEntry = () => {
    setSelectedEntry(null);
    setIsNew(true);
    setIsModalOpen(true);
  };

  const handleEditEntry = (entry) => {
    setSelectedEntry(entry);
    setIsEditModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedEntry(null);
  };

  const handleEditModalClose = () => {
    setIsEditModalOpen(false);
    setSelectedEntry(null);
  };

  const handleEntrySubmitted = async (entry) => {
    if (!isMounted) return;

    try {
      if (isNew) {
        await EquipmentUsageService.createUsageEntry(entry);
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
          message: intl.formatMessage({
            id: "equipment.usage.create.success",
            defaultMessage: "Usage entry created successfully"
          }),
          kind: NotificationKinds.success,
        });
      } else {
        await EquipmentUsageService.saveDraft(entry.id, entry);
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
          message: intl.formatMessage({
            id: "equipment.usage.save.success",
            defaultMessage: "Usage entry saved successfully"
          }),
          kind: NotificationKinds.success,
        });
      }

      if (isMounted) {
        setNotificationVisible(true);
        handleModalClose();
        loadDrafts();
        if (onEntrySubmitted) {
          onEntrySubmitted();
        }
      }
    } catch (err) {
      console.error("Error submitting entry:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: err.message || intl.formatMessage({
            id: "equipment.usage.submit.error",
            defaultMessage: "Failed to submit usage entry"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    }
  };

  const handleEditEntrySubmitted = async (entry) => {
    if (!isMounted) return;

    try {
      await EquipmentUsageService.saveDraft(entry.id, entry);

      addNotification({
        title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
        message: intl.formatMessage({
          id: "equipment.usage.update.success",
          defaultMessage: "Usage entry updated successfully"
        }),
        kind: NotificationKinds.success,
      });

      if (isMounted) {
        setNotificationVisible(true);
        handleEditModalClose();
        loadDrafts();
        if (onEntrySubmitted) {
          onEntrySubmitted();
        }
      }
    } catch (err) {
      console.error("Error updating entry:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: err.message || intl.formatMessage({
            id: "equipment.usage.update.error",
            defaultMessage: "Failed to update usage entry"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    }
  };

  const handleSubmitForApproval = async (id) => {
    if (!isMounted) return;

    try {
      await EquipmentUsageService.submitForApproval(id);

      addNotification({
        title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
        message: intl.formatMessage({
          id: "equipment.usage.submit.approval.success",
          defaultMessage: "Usage entry submitted for approval successfully"
        }),
        kind: NotificationKinds.success,
      });

      if (isMounted) {
        setNotificationVisible(true);
        loadDrafts();
        if (onEntrySubmitted) {
          onEntrySubmitted();
        }
      }
    } catch (err) {
      console.error("Error submitting for approval:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: err.message || intl.formatMessage({
            id: "equipment.usage.submit.approval.error",
            defaultMessage: "Failed to submit usage entry for approval"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    }
  };

  const handleDeleteEntry = async (id) => {
    if (!isMounted) return;

    if (window.confirm("Are you sure you want to delete this entry?")) {
      try {
        // Implement delete functionality
        if (isMounted) {
          loadDrafts();
        }
      } catch (err) {
        console.error("Error deleting entry:", err);
        if (isMounted) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
            message: err.message || intl.formatMessage({
              id: "equipment.usage.delete.error",
              defaultMessage: "Failed to delete usage entry"
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      }
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}

      <Grid fullWidth={true} style={{ marginBottom: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <Stack orientation="horizontal" gap={4}>
            <Button kind="primary" renderIcon={Add} onClick={handleNewEntry}>
              <FormattedMessage
                id="equipment.usage.button.new"
                defaultMessage="New Entry"
              />
            </Button>
          </Stack>
        </Column>
      </Grid>

      <DataTable
        rows={entries}
        headers={[
          { key: "equipment", header: "Equipment" },
          { key: "loginTime", header: "Date/Time" },
          { key: "activitiesDone", header: "Activities" },
          { key: "equipmentStatus", header: "Status" },
          { key: "entryStatus", header: "Entry Status" },
          { key: "actions", header: "Actions" },
        ]}
      >
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getTableContainerProps,
        }) => (
          <TableContainer {...getTableContainerProps()}>
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
                {rows.map((row, rowIndex) => {
                  const entry = entries[rowIndex];
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => {
                        if (cell.info.header === "equipment") {
                          return (
                            <TableCell key={cell.id}>
                              {entry?.equipment?.name || "—"}
                            </TableCell>
                          );
                        }

                        if (cell.info.header === "loginTime") {
                          return (
                            <TableCell key={cell.id}>
                              {entry?.loginTime
                                ? new Date(entry.loginTime).toLocaleString()
                                : "—"}
                            </TableCell>
                          );
                        }

                        if (cell.info.header === "actions") {
                          return (
                            <TableCell key={cell.id}>
                              <OverflowMenu
                                flipped
                                ariaLabel="Equipment usage entry actions"
                              >
                                <OverflowMenuItem
                                  itemText="Edit"
                                  onClick={() => handleEditEntry(entry)}
                                />
                                <OverflowMenuItem
                                  itemText="Submit for Approval"
                                  onClick={() =>
                                    handleSubmitForApproval(entry.id)
                                  }
                                />
                                <OverflowMenuItem
                                  itemText="Delete"
                                  onClick={() => handleDeleteEntry(entry.id)}
                                  isDelete
                                />
                              </OverflowMenu>
                            </TableCell>
                          );
                        }

                        if (typeof cell.value === "object" && cell.value !== null) {
                          return (
                            <TableCell key={cell.id}>
                              —
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

      <EquipmentUsageModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        entry={selectedEntry}
        isNew={isNew}
        onSubmit={handleEntrySubmitted}
      />

      <EditUsageEntryModal
        isOpen={isEditModalOpen}
        onClose={handleEditModalClose}
        entry={selectedEntry}
        onSubmit={handleEditEntrySubmitted}
      />
    </>
  );
};

export default EquipmentUsageLog;
