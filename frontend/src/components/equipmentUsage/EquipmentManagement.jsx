import React, { useState, useEffect } from "react";
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
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import EquipmentModal from "./modals/EquipmentModal";
import { EquipmentAPI } from "./EquipmentUsageService";

const EquipmentManagement = () => {
  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);

  useEffect(() => {
    loadEquipment();
  }, []);

  const loadEquipment = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await EquipmentAPI.getAll();
      const transformedData = Array.isArray(data)
        ? data.map((item) => ({
            ...item,
            id: String(item.id), // Convert ID to string for DataTable
          }))
        : [];
      setEquipmentList(transformedData);
    } catch (err) {
      setError(err.message || "Failed to load equipment");
      console.error("Error loading equipment:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleNewEquipment = () => {
    setSelectedEquipment(null);
    setIsNew(true);
    setIsModalOpen(true);
  };

  const handleEditEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setIsNew(false);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedEquipment(null);
  };

  const handleEquipmentSaved = async () => {
    handleModalClose();
    await loadEquipment();
  };

  const handleDeleteEquipment = async (id) => {
    if (window.confirm("Are you sure you want to deactivate this equipment?")) {
      try {
        await EquipmentAPI.deactivate(id);
        await loadEquipment();
        setError(null);
      } catch (err) {
        setError("Failed to deactivate equipment: " + err.message);
      }
    }
  };

  if (loading && equipmentList.length === 0) {
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
            <Button
              kind="primary"
              renderIcon={Add}
              onClick={handleNewEquipment}
            >
              <FormattedMessage
                id="equipment.management.button.add"
                defaultMessage="Add Equipment"
              />
            </Button>
          </Stack>
        </Column>
      </Grid>

      <DataTable
        rows={equipmentList}
        headers={[
          { key: "name", header: "Equipment Name" },
          { key: "serialNumber", header: "Serial Number" },
          { key: "department", header: "Department" },
          { key: "manufacturer", header: "Manufacturer" },
          { key: "modelNumber", header: "Model" },
          { key: "isActive", header: "Status" },
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
                  {row.cells.map((cell) => {
                    let cellContent;
                    if (cell.info.header === "Status") {
                      cellContent =
                        row.original.isActive === "Y" ? "Active" : "Inactive";
                    } else if (cell.info.header === "actions") {
                      cellContent = renderActions(row.original);
                    } else if (
                      typeof cell.value === "object" &&
                      cell.value !== null
                    ) {
                      // Handle any remaining object values (shouldn't happen, but defensive)
                      cellContent = "—";
                    } else {
                      cellContent = cell.value;
                    }
                    return <TableCell key={cell.id}>{cellContent}</TableCell>;
                  })}
                </TableRow>
              ))}
            </TableBody>
          </table>
        )}
      />

      <EquipmentModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        equipment={selectedEquipment}
        isNew={isNew}
        onSubmit={handleEquipmentSaved}
      />
    </>
  );

  function renderActions(equipment) {
    if (!equipment || !equipment.id) {
      return <span>—</span>;
    }

    return (
      <OverflowMenu flipped ariaLabel="Equipment actions">
        <OverflowMenuItem
          itemText="Edit"
          onClick={() => handleEditEquipment(equipment)}
        />
        <OverflowMenuItem
          itemText={equipment.isActive === "Y" ? "Deactivate" : "Activate"}
          onClick={() => handleDeleteEquipment(equipment.id)}
          isDelete={equipment.isActive === "Y"}
        />
      </OverflowMenu>
    );
  }
};

export default EquipmentManagement;
