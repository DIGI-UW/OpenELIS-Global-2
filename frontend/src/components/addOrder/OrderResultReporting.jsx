import React, { useEffect, useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import CustomCheckBox from "../common/CustomCheckBox";
import { useIntl } from "react-intl";
const PatientEmailAndSMS = (testId, index, handlePatientEmailAndSMS) => {
  function handlePatientSMS(checked) {
    handlePatientEmailAndSMS("PatientSMS", index, checked, testId);
  }

  function handlePatientEmail(checked) {
    handlePatientEmailAndSMS("PatientEmail", index, checked, testId);
  }

  return (
    <>
      <CustomCheckBox
        id={"patientEmail_" + index + "_" + testId}
        onChange={(checked) => handlePatientEmail(checked)}
        label="Email"
      />
      <CustomCheckBox
        id={"patientSMS_" + index + "_" + testId}
        onChange={(checked) => handlePatientSMS(checked)}
        label="SMS"
      />
    </>
  );
};

const RequesterEmailAndSMS = (testId, index, handleRequesterEmailAndSMS) => {
  const intl = useIntl();

  function handleProviderEmail(checked) {
    handleRequesterEmailAndSMS("RequesterEmail", index, checked, testId);
  }

  function handleProviderSMS(checked) {
    handleRequesterEmailAndSMS("RequesterSMS", index, checked, testId);
  }

  return (
    <>
      <CustomCheckBox
        id={"providerEmail_" + index + "_" + testId}
        onChange={(checked) => handleProviderEmail(checked)}
        label={intl.formatMessage({ id: "provider.email" })}
      />
      <CustomCheckBox
        id={"providerSMS_" + index + "_" + testId}
        onChange={(checked) => handleProviderSMS(checked)}
        label="SMS"
      />
    </>
  );
};

const OrderResultReporting = (props) => {
  const [notificationTestIds, setNotificationTestIds] = useState({
    patientEmailNotificationTestIds: [],
    patientSMSNotificationTestIds: [],
    providerSMSNotificationTestIds: [],
    providerEmailNotificationTestIds: [],
  });
  const intl = useIntl();
  const headers = [
    "",
    intl.formatMessage({ id: "patient.label" }),
    intl.formatMessage({ id: "requester.label" }),
  ];
  let rows = [];
  props.selectedTests.map((test, index) => {
    const testId = test.id;
    rows.push({
      id: test.id,
      testName: <p key={index}>{test.name}</p>,
      patientEmailAndSMS: PatientEmailAndSMS(
        testId,
        index,
        handlePatientEmailAndSMS,
      ),
      requesterEmailAndSMS: RequesterEmailAndSMS(
        testId,
        index,
        handleRequesterEmailAndSMS,
      ),
    });
  });

  function handlePatientEmailAndSMS(type, index, checked, testId) {
    switch (type) {
      case "PatientSMS":
        if (checked) {
          setNotificationTestIds({
            ...notificationTestIds,
            patientSMSNotificationTestIds: [
              ...notificationTestIds.patientSMSNotificationTestIds,
              Number(testId),
            ],
          });
        } else {
          const updatedSMSNotificationIds =
            notificationTestIds.patientSMSNotificationTestIds.filter(
              (test) => test !== testId,
            );
          setNotificationTestIds({
            ...notificationTestIds,
            patientSMSNotificationTestIds: updatedSMSNotificationIds,
          });
        }
        break;
      case "PatientEmail":
        if (checked) {
          setNotificationTestIds({
            ...notificationTestIds,
            patientEmailNotificationTestIds: [
              ...notificationTestIds.patientEmailNotificationTestIds,
              Number(testId),
            ],
          });
        } else {
          const updatedEmailNotificationIds =
            notificationTestIds.patientEmailNotificationTestIds.filter(
              (test) => test !== testId,
            );
          setNotificationTestIds({
            ...notificationTestIds,
            patientEmailNotificationTestIds: updatedEmailNotificationIds,
          });
        }
        break;
    }
  }

  function handleRequesterEmailAndSMS(type, index, checked, testId) {
    switch (type) {
      case "RequesterSMS":
        if (checked) {
          setNotificationTestIds({
            ...notificationTestIds,
            providerSMSNotificationTestIds: [
              ...notificationTestIds.providerSMSNotificationTestIds,
              Number(testId),
            ],
          });
        } else {
          const updatedSMSNotificationIds =
            notificationTestIds.providerSMSNotificationTestIds.filter(
              (test) => test !== testId,
            );
          setNotificationTestIds({
            ...notificationTestIds,
            providerSMSNotificationTestIds: updatedSMSNotificationIds,
          });
        }
        break;
      case "RequesterEmail":
        if (checked) {
          setNotificationTestIds({
            ...notificationTestIds,
            providerEmailNotificationTestIds: [
              ...notificationTestIds.providerEmailNotificationTestIds,
              Number(testId),
            ],
          });
        } else {
          const updatedEmailNotificationIds =
            notificationTestIds.providerEmailNotificationTestIds.filter(
              (test) => test !== testId,
            );
          setNotificationTestIds({
            ...notificationTestIds,
            providerEmailNotificationTestIds: updatedEmailNotificationIds,
          });
        }
        break;
    }
  }

  useEffect(() => {
    props.reportingNotifications(notificationTestIds);
  }, [notificationTestIds]);

  return (
    <>
      <Table size="sm" useZebraStyles={false}>
        <TableHead>
          <TableRow>
            {headers.map((header) => (
              <TableHeader id={header.key} key={header}>
                {header}
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              {Object.keys(row)
                .filter((key) => key !== "id")
                .map((key) => {
                  return <TableCell key={key}>{row[key]}</TableCell>;
                })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </>
  );
};

export default OrderResultReporting;
