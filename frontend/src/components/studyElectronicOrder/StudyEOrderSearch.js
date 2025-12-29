import {
  Button,
  Column,
  Link,
  Select,
  SelectItem,
  TextInput,
  Loading,
} from "@carbon/react";
import React, { useEffect, useState, useContext } from "react";
import CustomDatePicker from "../common/CustomDatePicker";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds, AlertDialog } from "../common/CustomNotification";

const StudyEOrderSearch = ({ setEOrders = () => {}, eOrderRef }) => {
  const intl = useIntl();

  const [hasEOrders, setHasEOrders] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [statusId, setStatusId] = useState("");
  const [statusOptions, setStatusOptions] = useState([]);
  const [organizationId, setOrganizationId] = useState("");
  const [organizationOptions, setOrganizationOptions] = useState([]);
  const [searchCompleted, setSearchCompleted] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [loading, setLoading] = useState(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders",
      handleElectronicOrders,
    );
    getFromOpenElisServer(
      "/rest/displayList/ELECTRONIC_ORDER_STATUSES",
      handleOrderStatus,
    );
  }, []);

  const handleElectronicOrders = (response) => {
    console.log("Initial response:", response);
    if (response && response.organizationList) {
      setOrganizationOptions(response.organizationList);
    }
  };

  const handleOrderStatus = (response) => {
    setStatusOptions(response || []);
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  };

  function searchByIdentifier() {
    const params = new URLSearchParams({
      searchType: "IDENTIFIER",
      searchValue: searchValue,
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  function searchByFacility() {
    const params = new URLSearchParams({
      searchType: "FACILITY",
      organizationId: organizationId,
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  function searchByDateAndStatus() {
    const params = new URLSearchParams({
      searchType: "DATE_STATUS",
      startDate: startDate,
      endDate: endDate,
      statusId: statusId,
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  const parseEOrders = (response) => {
    console.log("parseEOrders response:", response);
    setSearchCompleted(true);
    setLoading(false);

    // Check if response is valid
    if (!response) {
      console.error("Response is null or undefined");
      setEOrders([]);
      setHasEOrders(false);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Failed to fetch electronic orders",
      });
      setNotificationVisible(true);
      return;
    }

    // Handle pagination
    if (response.paging) {
      const { totalPages, currentPage } = response.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        if (parseInt(currentPage) < parseInt(totalPages)) {
          setNextPage(parseInt(currentPage) + 1);
        } else {
          setNextPage(null);
        }

        if (parseInt(currentPage) > 1) {
          setPreviousPage(parseInt(currentPage) - 1);
        } else {
          setPreviousPage(null);
        }
      } else {
        setNextPage(null);
        setPreviousPage(null);
        setPagination(false);
      }
    }

    // Handle eOrders array
    const eOrdersArray = response.eOrders || [];
    setHasEOrders(eOrdersArray.length > 0);

    setEOrders(
      eOrdersArray.map((item) => {
        return { ...item, id: item.electronicOrderId };
      }),
    );

    if (eOrderRef?.current) {
      window.scrollTo({
        top: eOrderRef.current.offsetTop - 50,
        left: 0,
        behavior: "smooth",
      });
    }

    if (eOrdersArray.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "eorder.search.noresults",
        }),
      });
      setNotificationVisible(true);
    }
  };

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?page=" + nextPage,
      parseEOrders,
    );
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/StudyElectronicOrders?page=" + previousPage,
      parseEOrders,
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <Column lg={16} md={8} sm={4}>
        <hr />
        <FormattedMessage
          id="study.eorder.search.patient_facility.title"
          defaultMessage="Search by Patient Code or Facility"
        />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <br />
      </Column>
      <Column lg={9} md={4} sm={4}>
        <TextInput
          id="searchValue"
          labelText={intl.formatMessage({
            id: "study.eorder.patient.code",
            defaultMessage: "Patient Code",
          })}
          value={searchValue}
          onChange={(e) => {
            setSearchValue(e.target.value);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              searchByIdentifier();
            }
          }}
        />
      </Column>
      <Column lg={1}></Column>
      <Column lg={6}>
        <Button onClick={searchByIdentifier}>
          <FormattedMessage id="label.button.search" />
        </Button>
      </Column>

      <Column lg={16} md={8} sm={4}>
        <hr />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <FormattedMessage
          id="study.eorder.search.date_range.title"
          defaultMessage="Search by Date Range and Status"
        />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <br />
      </Column>
      <Column lg={3} md={2} sm={2}>
        <CustomDatePicker
          id={"studyEOrder_startDate"}
          labelText={intl.formatMessage({
            id: "study.eorder.search.date.start",
            defaultMessage: "Start Date",
          })}
          value={startDate}
          className="inputDate"
          onChange={(date) => setStartDate(date)}
        />
      </Column>
      <Column lg={3} md={2} sm={2}>
        <CustomDatePicker
          id={"studyEOrder_endDate"}
          labelText={intl.formatMessage({
            id: "study.eorder.search.date.end",
            defaultMessage: "End Date",
          })}
          value={endDate}
          className="inputDate"
          onChange={(date) => setEndDate(date)}
        />
      </Column>
      <Column lg={3} md={2} sm={2}>
        <Select
          id="statusId"
          labelText={intl.formatMessage({
            id: "eorder.status",
            defaultMessage: "Status",
          })}
          value={statusId}
          onChange={(e) => {
            setStatusId(e.target.value);
          }}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "study.eorder.all_status",
              defaultMessage: "All Statuses",
            })}
          />
          {statusOptions.map((statusOption, index) => {
            return (
              <SelectItem
                key={index}
                value={statusOption.id}
                text={statusOption.value}
              />
            );
          })}
        </Select>
      </Column>
      <Column lg={1}></Column>
      <Column lg={6} md={4} sm={2}>
        <Button onClick={searchByDateAndStatus}>
          <FormattedMessage id="label.button.search" />
        </Button>
      </Column>

      {searchCompleted && !hasEOrders && (
        <Column lg={16} md={8} sm={4}>
          <FormattedMessage id="eorder.search.noresults" />
        </Column>
      )}
      <Column lg={16} md={8} sm={4}>
        {loading && <Loading description="Loading Orders..." small={true} />}
      </Column>

      <>
        <Column lg={14} />
        <Column
          lg={2}
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "10px",
            width: "110%",
          }}
        >
          {pagination && (
            <>
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="loadpreviousresults"
                  onClick={loadPreviousResultsPage}
                  disabled={previousPage != null ? false : true}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                ></Button>
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={loadNextResultsPage}
                  disabled={nextPage != null ? false : true}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                ></Button>
              </div>
            </>
          )}
        </Column>
      </>
    </>
  );
};

export default StudyEOrderSearch;
