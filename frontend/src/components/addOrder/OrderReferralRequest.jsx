import React, { useContext, useEffect } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CustomTextInput from "../common/CustomTextInput";
import CustomSelect from "../common/CustomSelect";
import CustomDatePicker from "../common/CustomDatePicker";
import { useIntl } from "react-intl";

function requiredSymbol(value) {
  return (
    <>
      {" "}
      {value} <span style={{ color: "red" }}>*</span>
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
      key: "name",
      header: requiredSymbol(intl.formatMessage({ id: "search.label.test" })),
    },
  ];

  // Keep referralRequests aligned with selectedTests by index whenever the
  // selected tests change. Rebuilt (not appended) on every run so this stays
  // idempotent — appending here previously accumulated a duplicate entry per
  // render and left only a single entry for a multi-test sample, which the
  // payload builder then collapsed into comma-joined ids such as "4,4" —
  // values the server rejects.
  const syncReferralRequests = () => {
    if (selectedTests.length === 0) {
      return;
    }
    const updateReferralRequest = selectedTests.map((test) => {
      return (
        referralRequests.find((r) => r && r.testId === test.id) || {
          referralRequestObject: referralReasons[0].id,
          referrer:
            userSessionDetails.firstName + " " + userSessionDetails.lastName,
          institute: null,
          sentDate: "",
          testId: test.id,
        }
      );
    });
    setReferralRequests(updateReferralRequest);
  };

  useEffect(() => {
    syncReferralRequests();
  }, [selectedTests]);

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
            {selectedTests.map((test, i) => {
              const id = index + "_" + test.id;
              const obj = referralRequests[i] || {};
              const testValue = { id: test.id, value: test.name };
              const defaultSelect = { id: "", value: "" };
              return (
                <TableRow key={test.id}>
                  <TableCell key="reason">
                    <CustomSelect
                      id={"referralReasonId_" + id}
                      options={referralReasons}
                      value={obj.reasonForReferral || null}
                      onChange={(e) => handleReasonForReferral(e, i)}
                    />
                  </TableCell>
                  <TableCell key="referrer">
                    <CustomTextInput
                      id={"referrer_" + id}
                      defaultValue={obj.referrer}
                      onChange={(value) => handleReferrer(value, i)}
                      labelText={""}
                    />
                  </TableCell>
                  <TableCell key="institute">
                    <CustomSelect
                      id={"referredInstituteId_" + id}
                      options={referralOrganizations}
                      value={obj.institute || null}
                      onChange={(e) => handleInstituteSelect(e, i)}
                      defaultSelect={defaultSelect}
                    />
                  </TableCell>
                  <TableCell key="sentDate">
                    <CustomDatePicker
                      id={"sendDate_" + id}
                      autofillDate={true}
                      className="orderReferralSentDate"
                      value={obj.sentDate || null}
                      onChange={(date) => handleSentDatePicker(date, i)}
                      labelText={""}
                    />
                  </TableCell>
                  <TableCell key="testName">
                    <CustomSelect
                      id={"shadowReferredTest_" + id}
                      defaultSelect={testValue}
                      value={test.id}
                      disabled={true}
                    />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </>
    </>
  );
};

export default OrderReferralRequest;
