import React, { useContext, useState } from "react";
import { useHistory } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
} from "@carbon/react";
import { Printer, Checkmark } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

/**
 * OrderLabel - Step 3: Label & Store
 *
 * Handles barcode label printing and storage location assignment.
 * Shows list of samples with their labels and storage status.
 */

const OrderLabel = () => {
  const intl = useIntl();
  const history = useHistory();
  const {
    orderData,
    samples,
    setSamples,
    saveOrder,
    setCurrentStep,
    labNumber,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [printedLabels, setPrintedLabels] = useState(new Set());
  const [assignedStorage, setAssignedStorage] = useState(new Set());

  const handlePrintLabel = (sampleIndex) => {
    // TODO: Integrate with actual label printing service
    setPrintedLabels((prev) => new Set([...prev, sampleIndex]));

    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "label.print.success",
        defaultMessage: "Label printed successfully",
      }),
    });
    setNotificationVisible(true);
  };

  const handleAssignStorage = (sampleIndex) => {
    // TODO: Open storage location selector modal
    setAssignedStorage((prev) => new Set([...prev, sampleIndex]));

    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "storage.assign.success",
        defaultMessage: "Storage location assigned",
      }),
    });
    setNotificationVisible(true);
  };

  // All samples should have labels printed to proceed
  const canProceed = samples.every((_, index) => printedLabels.has(index));

  const handleSave = async () => {
    try {
      await saveOrder();
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSaveAndNext = async () => {
    try {
      await saveOrder();
      setCurrentStep(3);
      history.push("/order/qa");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const headers = [
    {
      key: "sampleId",
      header: intl.formatMessage({
        id: "sample.label.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "tests",
      header: intl.formatMessage({ id: "label.test", defaultMessage: "Tests" }),
    },
    {
      key: "labelStatus",
      header: intl.formatMessage({
        id: "label.status",
        defaultMessage: "Label Status",
      }),
    },
    {
      key: "storageStatus",
      header: intl.formatMessage({
        id: "storage.status",
        defaultMessage: "Storage Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.action",
        defaultMessage: "Actions",
      }),
    },
  ];

  const rows = samples.map((sample, index) => ({
    id: `sample-${index}`,
    sampleId: `${labNumber || orderData?.sampleOrderItems?.labNo || "---"}-${index + 1}`,
    sampleType: sample.name || sample.sampleTypeName || "---",
    tests: sample.tests?.map((t) => t.name).join(", ") || "---",
    labelStatus: printedLabels.has(index) ? (
      <Tag type="green" size="sm">
        <Checkmark size={12} />{" "}
        <FormattedMessage id="label.printed" defaultMessage="Printed" />
      </Tag>
    ) : (
      <Tag type="gray" size="sm">
        <FormattedMessage id="label.pending" defaultMessage="Pending" />
      </Tag>
    ),
    storageStatus: assignedStorage.has(index) ? (
      <Tag type="green" size="sm">
        <Checkmark size={12} />{" "}
        <FormattedMessage id="storage.assigned" defaultMessage="Assigned" />
      </Tag>
    ) : (
      <Tag type="gray" size="sm">
        <FormattedMessage id="storage.unassigned" defaultMessage="Unassigned" />
      </Tag>
    ),
    actions: (
      <div className="label-actions">
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Printer}
          onClick={() => handlePrintLabel(index)}
          disabled={printedLabels.has(index)}
        >
          <FormattedMessage id="label.print" defaultMessage="Print" />
        </Button>
        <Button
          kind="ghost"
          size="sm"
          onClick={() => handleAssignStorage(index)}
          disabled={assignedStorage.has(index)}
        >
          <FormattedMessage
            id="storage.assign"
            defaultMessage="Assign Storage"
          />
        </Button>
      </div>
    ),
  }));

  return (
    <OrderWorkflowLayout
      currentStep={2}
      title="order.step.label"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
    >
      {notificationVisible && <AlertDialog />}

      <Tile className="label-store-tile">
        <h4>
          <FormattedMessage
            id="label.store.instructions"
            defaultMessage="Print labels for each sample and assign storage locations"
          />
        </h4>

        {samples && samples.length > 0 ? (
          <DataTable rows={rows} headers={headers}>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
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
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        ) : (
          <p>
            <FormattedMessage
              id="label.store.noSamples"
              defaultMessage="No samples to label. Please go back and add samples."
            />
          </p>
        )}
      </Tile>
    </OrderWorkflowLayout>
  );
};

export default OrderLabel;
