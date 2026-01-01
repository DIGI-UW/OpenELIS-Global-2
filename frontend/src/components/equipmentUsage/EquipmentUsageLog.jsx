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
} from "@carbon/react";
import { Add, Edit, TrashCan, SendAlt } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EquipmentUsageModal from "./modals/EquipmentUsageModal";
import EquipmentUsageService from "./EquipmentUsageService";

const EquipmentUsageLog = ({ onEntrySubmitted }) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [isMounted, setIsMounted] = useState(true);

  useEffect(() => {
    setIsMounted(true);
    loadDrafts();

    // Cleanup function to prevent memory leaks
    return () => {
      setIsMounted(false);
    };
  }, []);

  const loadDrafts = async () => {
    if (!isMounted) return;

    setLoading(true);
    setError(null);
    try {
      const allEntries = await EquipmentUsageService.getAllUsageEntries();
      // Filter to show only DRAFT entries for current user (simplified)
      const userDrafts = allEntries.filter((entry) => entry.entryStatus === "DRAFT");

      if (isMounted) {
        setEntries(userDrafts);
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

  const handleNewEntry = () => {
    setSelectedEntry(null);
    setIsNew(true);
    setIsModalOpen(true);
  };

  const handleEditEntry = (entry) => {
    setSelectedEntry(entry);
    setIsNew(false);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedEntry(null);
  };

  const handleEntrySubmitted = async (entry) => {
    if (!isMounted) return;

    try {
      if (isNew) {
        await EquipmentUsageService.createUsageEntry(entry);
      } else {
        await EquipmentUsageService.saveDraft(entry.id, entry);
      }

      if (isMounted) {
        handleModalClose();
        loadDrafts();
        if (onEntrySubmitted) {
          onEntrySubmitted();
        }
      }
    } catch (err) {
      // Handle error appropriately
      console.error('Error submitting entry:', err);
    }
  };

  const handleSubmitForApproval = async (id) => {
    if (!isMounted) return;

    try {
      await EquipmentUsageService.submitForApproval(id);

      if (isMounted) {
        loadDrafts();
        if (onEntrySubmitted) {
          onEntrySubmitted();
        }
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
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
                        ? renderActions(row.original)
                        : cell.value}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </table>
        )}
      />

      <EquipmentUsageModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        entry={selectedEntry}
        isNew={isNew}
        onSubmit={handleEntrySubmitted}
      />
    </>
  );

  function renderActions(entry) {
    return (
      <OverflowMenu flipped ariaLabel="Equipment usage entry actions">
        <OverflowMenuItem
          itemText="Edit"
          onClick={() => handleEditEntry(entry)}
        />
        <OverflowMenuItem
          itemText="Submit for Approval"
          onClick={() => handleSubmitForApproval(entry.id)}
        />
        <OverflowMenuItem
          itemText="Delete"
          onClick={() => handleDeleteEntry(entry.id)}
          isDelete
        />
      </OverflowMenu>
    );
  }
};

export default EquipmentUsageLog;
