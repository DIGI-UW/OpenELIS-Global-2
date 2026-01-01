import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Grid,
  Column,
  Loading,
  InlineNotification,
  Stack,
  OverflowMenu,
  OverflowMenuItem,
  Modal,
} from "@carbon/react";
import { Checkmark, Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EquipmentUsageService from "./EquipmentUsageService";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

const EquipmentUsageApproval = ({ onApprovalSubmitted }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [showApprovalModal, setShowApprovalModal] = useState(false);
  const [isMounted, setIsMounted] = useState(true);

  useEffect(() => {
    setIsMounted(true);
    loadPendingApproval();

    // Cleanup function to prevent memory leaks
    return () => {
      setIsMounted(false);
    };
  }, []);

  const loadPendingApproval = async () => {
    if (!isMounted) return;

    setLoading(true);
    try {
      const data = await EquipmentUsageService.getPendingApproval();
      const transformedData = Array.isArray(data)
        ? data.map((item) => ({
            ...item,
            id: String(item.id), // Convert ID to string for DataTable
          }))
        : [];

      if (isMounted) {
        setEntries(transformedData);
      }
    } catch (err) {
      console.error("Error loading pending approvals:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message:
            err.message ||
            intl.formatMessage({
              id: "equipment.approval.load.error",
              defaultMessage: "Failed to load pending approvals",
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

  const handleApproveClick = (entry) => {
    setSelectedEntry(entry);
    setShowApprovalModal(true);
  };

  const handleApprove = async () => {
    if (!isMounted || !selectedEntry || !userSessionDetails?.userId) return;

    try {
      await EquipmentUsageService.approveEntry(
        selectedEntry.id,
        userSessionDetails.userId,
      );

      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Success",
        }),
        message: intl.formatMessage({
          id: "equipment.approval.approve.success",
          defaultMessage: "Entry approved successfully",
        }),
        kind: NotificationKinds.success,
      });

      if (isMounted) {
        setNotificationVisible(true);
        setShowApprovalModal(false);
        setSelectedEntry(null);
        loadPendingApproval();
        if (onApprovalSubmitted) {
          onApprovalSubmitted();
        }
      }
    } catch (err) {
      console.error("Error approving entry:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message:
            err.message ||
            intl.formatMessage({
              id: "equipment.approval.approve.error",
              defaultMessage: "Failed to approve entry",
            }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    }
  };

  const handleReject = async (id) => {
    if (!isMounted) return;

    if (window.confirm("Are you sure you want to reject this entry?")) {
      try {
        await EquipmentUsageService.rejectEntry(id);

        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "equipment.approval.reject.success",
            defaultMessage: "Entry rejected successfully",
          }),
          kind: NotificationKinds.success,
        });

        if (isMounted) {
          setNotificationVisible(true);
          loadPendingApproval();
          if (onApprovalSubmitted) {
            onApprovalSubmitted();
          }
        }
      } catch (err) {
        console.error("Error rejecting entry:", err);
        if (isMounted) {
          addNotification({
            title: intl.formatMessage({
              id: "notification.title",
              defaultMessage: "Error",
            }),
            message:
              err.message ||
              intl.formatMessage({
                id: "equipment.approval.reject.error",
                defaultMessage: "Failed to reject entry",
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

      {entries.length === 0 && !loading && (
        <InlineNotification
          title="No Pending Approvals"
          subtitle="There are no entries waiting for approval."
          kind="info"
        />
      )}

      <DataTable
        rows={entries}
        headers={[
          { key: "id", header: "ID" },
          { key: "equipment", header: "Equipment" },
          { key: "operatorName", header: "Operator" },
          { key: "loginTime", header: "Date/Time" },
          { key: "activitiesDone", header: "Activities" },
          { key: "equipmentStatus", header: "Equipment Status" },
          { key: "actions", header: "Actions" },
        ]}
        render={({ rows, headers, getHeaderProps, getRowProps }) => (
          <table className="cds--data-table cds--data-table--zebra">
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell) => (
                    <TableCell key={cell.id}>
                      {cell.info.header === "Equipment"
                        ? row.original.equipment?.name || "—"
                        : cell.info.header === "Actions"
                          ? renderApprovalActions(row.original)
                          : cell.info.header === "Date/Time"
                            ? row.original.loginTime
                              ? new Date(
                                  row.original.loginTime,
                                ).toLocaleString()
                              : "—"
                            : typeof cell.value === "object" &&
                                cell.value !== null
                              ? "—"
                              : cell.value}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </table>
        )}
      />

      <Modal
        open={showApprovalModal}
        modalHeading="Approve Equipment Usage Entry"
        primaryButtonText="Approve"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleApprove}
        onRequestClose={() => {
          setShowApprovalModal(false);
          setSelectedEntry(null);
        }}
      >
        {selectedEntry && (
          <div>
            <p>
              <strong>Equipment:</strong> {selectedEntry.equipment?.name}
            </p>
            <p>
              <strong>Operator:</strong> {selectedEntry.operatorName}
            </p>
            <p>
              <strong>Login Time:</strong>{" "}
              {new Date(selectedEntry.loginTime).toLocaleString()}
            </p>
            <p>
              <strong>Activities:</strong> {selectedEntry.activitiesDone}
            </p>
            <p>
              <strong>Equipment Status:</strong> {selectedEntry.equipmentStatus}
            </p>
          </div>
        )}
      </Modal>
    </>
  );

  function renderApprovalActions(entry) {
    if (!entry || !entry.id) {
      return <span>—</span>;
    }

    return (
      <OverflowMenu flipped ariaLabel="Approval actions">
        <OverflowMenuItem
          itemText="Approve"
          onClick={() => handleApproveClick(entry)}
        />
        <OverflowMenuItem
          itemText="Reject"
          onClick={() => handleReject(entry.id)}
          isDelete
        />
      </OverflowMenu>
    );
  }
};

export default EquipmentUsageApproval;
