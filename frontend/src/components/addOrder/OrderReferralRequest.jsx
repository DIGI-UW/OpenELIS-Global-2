import React, { useContext, useEffect } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CustomTextInput from "../common/CustomTextInput";
import CustomSelect from "../common/CustomSelect";
import CustomDatePicker from "../common/CustomDatePicker";
import { useIntl } from "react-intl";
import { RequiredMarker, requiredProps } from "../common/RequiredMarker";

function requiredSymbol(value) {
  return (
    <>
      {" "}
      {value}
      <RequiredMarker />
    </>
  );
}

const OrderReferralRequest = ({
  index,
  selectedTests,
  referralReasons,
  referralOrganizations,
  referralRequests,
  setReferralRequests,
}) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  function handleReferrer(referrer, index) {
    const update = [...referralRequests];
    update[index].referrer = referrer;
    setReferralRequests(update);
  }

  function handleReasonForReferral(reasonId, index) {
    const update = [...referralRequests];
    update[index].reasonForReferral = reasonId;
    setReferralRequests(update);
  }

  function handleInstituteSelect(instituteId, index) {
    const update = [...referralRequests];
    update[index].institute = instituteId;
    setReferralRequests(update);
  }

  function handleSentDatePicker(date, index) {
    if (date != null) {
      const update = [...referralRequests];
      if (update[index]) {
        update[index].sentDate = date;
      }
      setReferralRequests(update);
    }
  }

  // Transport details are written straight onto the referral row.
  function handleRowField(field, value, index) {
    const update = [...referralRequests];
    if (update[index]) {
      update[index][field] = value;
    }
    setReferralRequests(update);
  }

  const transportModes = [
    {
      id: "CAR",
      value: intl.formatMessage({ id: "referral.transportMode.car" }),
    },
    {
      id: "MOTORCYCLE",
      value: intl.formatMessage({ id: "referral.transportMode.motorcycle" }),
    },
    {
      id: "COURIER",
      value: intl.formatMessage({ id: "referral.transportMode.courier" }),
    },
    {
      id: "HAND_CARRIED",
      value: intl.formatMessage({ id: "referral.transportMode.handCarried" }),
    },
    {
      id: "OTHER",
      value: intl.formatMessage({ id: "referral.transportMode.other" }),
    },
  ];

  const header = [
    {
      key: "reason",
      header: requiredSymbol(
        intl.formatMessage({ id: "referral.label.reason" }),
      ),
    },
    { key: "referrer", header: intl.formatMessage({ id: "referrer.label" }) },
    {
      key: "institute",
      header: requiredSymbol(
        intl.formatMessage({ id: "referral.label.institute" }),
      ),
    },
    {
      key: "",
      header:
        intl.formatMessage({ id: "referral.label.sentdate" }) +
        "\n" +
        "(dd/mm/yyyy)",
    },
    {
      key: "sentTime",
      header:
        intl.formatMessage({ id: "referral.label.senttime" }) +
        "\n" +
        "(hh:mm)",
    },
    {
      key: "transportMode",
      header: intl.formatMessage({ id: "referral.label.transportMode" }),
    },
    {
      key: "vehicleRegistration",
      header: intl.formatMessage({ id: "referral.label.vehicleRegistration" }),
    },
    {
      key: "driverName",
      header: intl.formatMessage({ id: "referral.label.driverName" }),
    },
    {
      key: "name",
      header: requiredSymbol(intl.formatMessage({ id: "search.label.test" })),
    },
  ];

  // One referral entry per selected test, rebuilt (not appended) whenever the
  // selection changes so the entries stay aligned with selectedTests by index.
  // Appending here previously accumulated a duplicate entry per render and left
  // only a single entry for a multi-test sample, which the payload builder then
  // collapsed into comma-joined ids such as "4,4" — values the server rejects.
  const defaultReferralRequest = (test) => ({
    reasonForReferral: referralReasons[0]?.id ?? "",
    referrer: userSessionDetails.firstName + " " + userSessionDetails.lastName,
    institute: referralOrganizations[0]?.id ?? null,
    sentDate: "",
    sentTime: "",
    transportMode: "",
    vehicleRegistration: "",
    driverName: "",
    testId: test.id,
  });

  useEffect(() => {
    if (selectedTests.length === 0) {
      setReferralRequests([]);
      return;
    }
    setReferralRequests(
      selectedTests.map(
        (test) =>
          referralRequests.find((r) => r && r.testId === test.id) ||
          defaultReferralRequest(test),
      ),
    );
  }, [selectedTests]);

  // The rows are built from the current requests on every render, so each
  // select shows what the user picked rather than the value it was created with.
  const referralRows = selectedTests.map((test, i) => {
    const id = index + "_" + test.id;
    const request = referralRequests[i];
    return {
      reason: (
        <CustomSelect
          id={"referralReasonId_" + id}
          aria-label={intl.formatMessage({ id: "referral.label.reason" })}
          {...requiredProps()}
          options={referralReasons}
          value={request?.reasonForReferral ? request.reasonForReferral : null}
          onChange={(e) => handleReasonForReferral(e, i)}
        />
      ),
      referrer: (
        <CustomTextInput
          id={"referrer_" + id}
          defaultValue={
            request?.referrer
              ? request.referrer
              : defaultReferralRequest(test).referrer
          }
          onChange={(value) => handleReferrer(value, i)}
          labelText={""}
        />
      ),
      institute: (
        <CustomSelect
          id={"referredInstituteId_" + id}
          aria-label={intl.formatMessage({ id: "referral.label.institute" })}
          {...requiredProps()}
          options={referralOrganizations}
          value={request?.institute ? request.institute : null}
          onChange={(e) => handleInstituteSelect(e, i)}
          defaultSelect={{ id: "", value: "" }}
        />
      ),
      sentDate: (
        <CustomDatePicker
          id={"sendDate_" + id}
          autofillDate={true}
          className="orderReferralSentDate"
          value={request?.sentDate ? request.sentDate : null}
          onChange={(date) => handleSentDatePicker(date, i)}
          labelText={""}
        />
      ),
      sentTime: (
        <TextInput
          id={"sendTime_" + id}
          type="time"
          labelText={""}
          aria-label={intl.formatMessage({ id: "referral.label.senttime" })}
          value={request?.sentTime ?? ""}
          onChange={(e) => handleRowField("sentTime", e.target.value, i)}
        />
      ),
      transportMode: (
        <CustomSelect
          id={"transportMode_" + id}
          aria-label={intl.formatMessage({
            id: "referral.label.transportMode",
          })}
          options={transportModes}
          value={request?.transportMode ? request.transportMode : null}
          onChange={(e) => handleRowField("transportMode", e, i)}
          defaultSelect={{ id: "", value: "" }}
        />
      ),
      vehicleRegistration: (
        <CustomTextInput
          id={"vehicleRegistration_" + id}
          defaultValue={request?.vehicleRegistration ?? ""}
          onChange={(value) => handleRowField("vehicleRegistration", value, i)}
          labelText={""}
        />
      ),
      driverName: (
        <CustomTextInput
          id={"driverName_" + id}
          defaultValue={request?.driverName ?? ""}
          onChange={(value) => handleRowField("driverName", value, i)}
          labelText={""}
        />
      ),
      testName: (
        <CustomSelect
          id={"shadowReferredTest_" + id}
          defaultSelect={{ id: test.id, value: test.name }}
          value={test.id}
          disabled={true}
        />
      ),
    };
  });

  return (
    <>
      <>
        <Table useZebraStyles={false} id={`referralRequestTable_` + index}>
          <TableHead>
            <TableRow>
              {header.map((header, header_index) => (
                <TableHeader id={header.key} key={header_index}>
                  {header.header}
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {referralRows.length > 0 &&
              referralRows.map((row, td_index) => (
                <TableRow key={td_index}>
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
    </>
  );
};

export default OrderReferralRequest;
