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
import { FormattedMessage } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EquipmentUsageService from "./EquipmentUsageService";

const EquipmentUsageApproval = ({ onApprovalSubmitted }) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
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
    setError(null);
    try {
      const data = await EquipmentUsageService.getPendingApproval();

      if (isMounted) {
        setEntries(data || []);
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
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
      await EquipmentUsageService.approveEntry(selectedEntry.id, userSessionDetails.userId);

      if (isMounted) {
        setShowApprovalModal(false);
        setSelectedEntry(null);
        loadPendingApproval();
        if (onApprovalSubmitted) {
          onApprovalSubmitted();
        }
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
      }
    }
  };

  const handleReject = async (id) => {
    if (!isMounted) return;

    if (window.confirm("Are you sure you want to reject this entry?")) {
      try {
        await EquipmentUsageService.rejectEntry(id);

        if (isMounted) {
          loadPendingApproval();
          if (onApprovalSubmitted) {
            onApprovalSubmitted();
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(err.message);
        }
      }
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      {error && (
        <InlineNotification
          title="Error"
          subtitle={error}
          kind="error"
          onClose={() => setError(null)}
        />
      )}

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
                        ? row.original.equipment?.name
                        : cell.info.header === "actions"
                        ? renderApprovalActions(row.original)
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
              <strong>Login Time:</strong> {new Date(selectedEntry.loginTime).toLocaleString()}
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
