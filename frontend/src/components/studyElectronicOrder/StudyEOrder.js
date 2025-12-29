import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  TableContainer,
  Modal,
  Select,
  SelectItem,
  TextArea,
  TextInput,
  InlineLoading,
} from "@carbon/react";
import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormData,
} from "../utils/Utils";
import { AlertDialog } from "../common/CustomNotification";

const StudyEOrder = ({ eOrderRef, eOrders, setEOrders }) => {
  const intl = useIntl();
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [qaEvents, setQaEvents] = useState([]);
  const [rejectReason, setRejectReason] = useState("");
  const [rejectNote, setRejectNote] = useState("");
  const [rejectAuthorizer, setRejectAuthorizer] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState("");
  const [notificationKind, setNotificationKind] = useState("success");

  useEffect(() => {
    // Fetch QA events (rejection reasons)
    getFromOpenElisServer("/rest/StudyElectronicOrders", (response) => {
      if (response.qaEvents) {
        setQaEvents(response.qaEvents);
      }
    });
  }, []);

  const headers = [
    {
      key: "no",
      header: intl.formatMessage({
        id: "study.eorder.no",
        defaultMessage: "No.",
      }),
    },
    {
      key: "requestingFacility",
      header: intl.formatMessage({
        id: "study.eorder.requester.facility",
        defaultMessage: "Requesting Facility",
      }),
    },
    {
      key: "patientNationalId",
      header: intl.formatMessage({
        id: "study.eorder.patient.code",
        defaultMessage: "Patient Code",
      }),
    },
    {
      key: "patientUpid",
      header: intl.formatMessage({
        id: "study.eorder.patient.upid",
        defaultMessage: "Patient UPID",
      }),
    },
    {
      key: "gender",
      header: intl.formatMessage({
        id: "study.eorder.patient.gender",
        defaultMessage: "Gender",
      }),
    },
    {
      key: "birthDate",
      header: intl.formatMessage({
        id: "study.eorder.patient.birth_date",
        defaultMessage: "Birth Date",
      }),
    },
    {
      key: "requestDateDisplay",
      header: intl.formatMessage({
        id: "study.eorder.request.date",
        defaultMessage: "Request Date",
      }),
    },
    {
      key: "collectionDateDisplay",
      header: intl.formatMessage({
        id: "study.eorder.collection.date",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "study.eorder.request.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "testName",
      header: intl.formatMessage({
        id: "study.eorder.request.test_name",
        defaultMessage: "Test Name",
      }),
    },
    {
      key: "labNumber",
      header: intl.formatMessage({
        id: "study.eorder.lab_number",
        defaultMessage: "Lab Number",
      }),
    },
    {
      key: "electronicOrderId",
      header: "",
    },
    {
      key: "externalOrderId",
      header: "",
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "study.eorder.action.title",
        defaultMessage: "Actions",
      }),
    },
  ];

  const handleEdit = (externalOrderId) => {
    // Redirect to React order entry page with external order ID
    window.location.href = `/SamplePatientEntry?ID=${externalOrderId}`;
  };

  const handleReject = (electronicOrderId) => {
    setSelectedOrderId(electronicOrderId);
    setRejectReason("");
    setRejectNote("");
    setRejectAuthorizer("");
    setRejectModalOpen(true);
  };

  const handleRejectSubmit = () => {
    if (!rejectReason) {
      setNotificationMessage(
        intl.formatMessage({
          id: "eorder.reject.error.reason.required",
          defaultMessage: "Rejection reason is required",
        }),
      );
      setNotificationKind("error");
      setNotificationVisible(true);
      return;
    }

    setIsSubmitting(true);

    const formData = new FormData();
    formData.append("electronicOrderId", selectedOrderId);
    formData.append("qaEventId", rejectReason);
    formData.append("qaNote", rejectNote);
    formData.append("qaAuthorizer", rejectAuthorizer);

    postToOpenElisServerFormData(
      "/rest/rejectStudyElectronicOrder",
      formData,
      (status) => {
        setIsSubmitting(false);
        if (status === 200) {
          setRejectModalOpen(false);
          setNotificationMessage(
            intl.formatMessage({
              id: "eorder.reject.success",
              defaultMessage: "Electronic order rejected successfully",
            }),
          );
          setNotificationKind("success");
          setNotificationVisible(true);

          // Remove the rejected order from the list
          setEOrders((prevOrders) =>
            prevOrders.filter(
              (order) => order.electronicOrderId !== selectedOrderId,
            ),
          );
        } else {
          setNotificationMessage(
            intl.formatMessage({
              id: "eorder.reject.error",
              defaultMessage: "Error rejecting order. Status: " + status,
            }),
          );
          setNotificationKind("error");
          setNotificationVisible(true);
        }
      },
    );
  };

  const rowsWithIndex = eOrders.map((row, index) => ({
    ...row,
    no: index + 1,
  }));

  return (
    <div ref={eOrderRef}>
      <AlertDialog
        isOpen={notificationVisible}
        kind={notificationKind}
        message={notificationMessage}
        onClose={() => setNotificationVisible(false)}
      />

      <Modal
        open={rejectModalOpen}
        onRequestClose={() => !isSubmitting && setRejectModalOpen(false)}
        onRequestSubmit={handleRejectSubmit}
        modalHeading={intl.formatMessage({
          id: "eorder.reject.modal.title",
          defaultMessage: "Reject Electronic Order",
        })}
        primaryButtonText={
          isSubmitting ? (
            <InlineLoading
              description={intl.formatMessage({
                id: "eorder.reject.submitting",
                defaultMessage: "Rejecting...",
              })}
            />
          ) : (
            intl.formatMessage({
              id: "eorder.reject.button",
              defaultMessage: "Reject",
            })
          )
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isSubmitting || !rejectReason}
      >
        <Select
          id="reject-reason"
          labelText={intl.formatMessage({
            id: "eorder.reject.reason",
            defaultMessage: "Rejection Reason *",
          })}
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          disabled={isSubmitting}
        >
          <SelectItem value="" text="Select a reason" />
          {qaEvents.map((event) => (
            <SelectItem key={event.id} value={event.id} text={event.value} />
          ))}
        </Select>

        <br />

        <TextInput
          id="reject-authorizer"
          labelText={intl.formatMessage({
            id: "eorder.reject.authorizer",
            defaultMessage: "Authorizer",
          })}
          value={rejectAuthorizer}
          onChange={(e) => setRejectAuthorizer(e.target.value)}
          disabled={isSubmitting}
        />

        <br />

        <TextArea
          id="reject-note"
          labelText={intl.formatMessage({
            id: "eorder.reject.note",
            defaultMessage: "Notes",
          })}
          value={rejectNote}
          onChange={(e) => setRejectNote(e.target.value)}
          disabled={isSubmitting}
          rows={4}
        />
      </Modal>

      {eOrders.length > 0 && (
        <>
          <DataTable rows={rowsWithIndex} headers={headers}>
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({
                  id: "eorder.search.result.title",
                  defaultMessage: "Test Requests Matching Search",
                })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header, index) => (
                        <TableHeader
                          key={index}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const statusCell = row.cells.find(
                        (cell) => cell.info.header === "status",
                      );
                      const isCancelled =
                        statusCell?.value === "Cancelled" ||
                        statusCell?.value === "Canceled";
                      const isDisabled =
                        row.cells.find(
                          (cell) => cell.info.header === "labNumber",
                        )?.value ||
                        row.cells.find(
                          (cell) => cell.info.header === "qaEventId",
                        )?.value ||
                        isCancelled;

                      return (
                        <TableRow key={row.id}>
                          {row.cells.map((cell) => {
                            if (cell.info.header === "electronicOrderId") {
                              return null; // Hidden column
                            }
                            if (cell.info.header === "externalOrderId") {
                              return null; // Hidden column
                            }
                            if (cell.info.header === "actions") {
                              return (
                                <TableCell key={cell.id}>
                                  <div style={{ display: "flex", gap: "8px" }}>
                                    <Button
                                      size="sm"
                                      disabled={isDisabled}
                                      onClick={() =>
                                        handleEdit(
                                          row.cells.find(
                                            (c) =>
                                              c.info.header ===
                                              "externalOrderId",
                                          )?.value,
                                        )
                                      }
                                    >
                                      <FormattedMessage
                                        id="study.eorder.action.edit"
                                        defaultMessage="Edit"
                                      />
                                    </Button>
                                    <Button
                                      kind="danger"
                                      size="sm"
                                      disabled={isDisabled}
                                      onClick={() =>
                                        handleReject(
                                          row.cells.find(
                                            (c) =>
                                              c.info.header ===
                                              "electronicOrderId",
                                          )?.value,
                                        )
                                      }
                                    >
                                      {isCancelled ? (
                                        <FormattedMessage
                                          id="study.eorder.action.rejected"
                                          defaultMessage="Rejected"
                                        />
                                      ) : (
                                        <FormattedMessage
                                          id="study.eorder.action.reject"
                                          defaultMessage="Reject"
                                        />
                                      )}
                                    </Button>
                                  </div>
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
        </>
      )}
    </div>
  );
};

export default StudyEOrder;
