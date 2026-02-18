import {
  Button,
  Checkbox,
  Column,
  Link,
  Select,
  SelectItem,
  TextInput,
  Loading,
} from "@carbon/react";
import { React, useEffect, useState, useContext } from "react";
import CustomDatePicker from "../common/CustomDatePicker";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds, AlertDialog } from "../common/CustomNotification";

const EOrderSearch = ({
  setEOrders = (eOrders) => {
    console.debug("set EOrders default");
  },
  eOrderRef,
}) => {
  const intl = useIntl();

  const [hasEOrders, setHasEOrders] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [statusId, setStatusId] = useState("");
  const [statusOptions, setStatusOptions] = useState([]);
  const [allInfo, setAllInfo] = useState(false);
  const [allInfo2, setAllInfo2] = useState(false);
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
    getFromOpenElisServer("/rest/ElectronicOrders", handleElectronicOrders);
    getFromOpenElisServer(
      "/rest/displayList/ELECTRONIC_ORDER_STATUSES",
      handleOrderStatus,
    );
  }, []);

  const handleElectronicOrders = (response) => {
    console.log(response);
  };

  const handleOrderStatus = (response) => {
    setStatusOptions(response);
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  };

  function searchByIdentifier() {
    const params = new URLSearchParams({
      searchType: "IDENTIFIER",
      searchValue: searchValue,
      useAllInfo: allInfo,
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/ElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  function searchByDateAndStatus() {
    const params = new URLSearchParams({
      searchType: "DATE_STATUS",
      startDate: startDate,
      endDate: endDate,
      statusId: statusId,
      useAllInfo: allInfo2,
    });
    setLoading(true);
    getFromOpenElisServer(
      "/rest/ElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  const parseEOrders = (response) => {
    setSearchCompleted(true);
    if (response && response.paging) {
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
    setHasEOrders(
      response.eOrders instanceof Array && response.eOrders.length > 0,
    );
    setEOrders(
      response.eOrders.map((item) => {
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
    if (response.eOrders.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "eorder.search.noresults",
        }),
      });
      setNotificationVisible(true);
    }
    setLoading(false);
  };

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/ElectronicOrders?page=" + nextPage,
      parseEOrders,
    );
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/ElectronicOrders?page=" + previousPage,
      parseEOrders,
    );
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}

      {/* SECTION 1: SEARCH BY IDENTIFIER */}
      <Column lg={16} md={8} sm={4}>
        <div style={{ marginBottom: "1rem", marginTop: "1rem" }}>
          <FormattedMessage id="eorder.search1.text" />
        </div>
      </Column>

      <Column lg={9} md={8} sm={4} style={{ marginBottom: "1rem" }}>
        <TextInput
          id="searchValue"
          labelText={intl.formatMessage({ id: "eorder.searchValue" })}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              searchByIdentifier();
            }
          }}
        />
      </Column>

      <Column lg={3} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            alignItems: "flex-end",
            height: "100%",
            paddingBottom: "0.5rem",
          }}
        >
          <Checkbox
            id="allInfo1"
            labelText={intl.formatMessage({ id: "eorder.allInfo" })}
            checked={allInfo}
            onChange={(e) => setAllInfo(e.currentTarget.checked)}
          />
        </div>
      </Column>

      <Column lg={4} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <div
          style={{ display: "flex", alignItems: "flex-end", height: "100%" }}
        >
          <Button onClick={searchByIdentifier}>
            <FormattedMessage id="label.button.search" />
          </Button>
        </div>
      </Column>

      {/* DIVIDER */}
      <Column lg={16} md={8} sm={4}>
        <hr style={{ margin: "2rem 0" }} />
      </Column>

      {/* SECTION 2: SEARCH BY DATE AND STATUS */}
      <Column lg={16} md={8} sm={4}>
        <div style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="eorder.search2.text" />
        </div>
      </Column>

      <Column lg={3} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <CustomDatePicker
          id={"eOrder_startDate"}
          labelText={intl.formatMessage({ id: "eorder.date.start" })}
          value={startDate}
          className="inputDate"
          onChange={(date) => setStartDate(date)}
        />
      </Column>

      <Column lg={3} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <CustomDatePicker
          id={"eOrder_endDate"}
          labelText={intl.formatMessage({ id: "eorder.date.end" })}
          value={endDate}
          className="inputDate"
          onChange={(date) => setEndDate(date)}
        />
      </Column>

      <Column lg={4} md={8} sm={4} style={{ marginBottom: "1rem" }}>
        <Select
          id="statusId"
          labelText={intl.formatMessage({ id: "eorder.status" })}
          value={statusId}
          onChange={(e) => setStatusId(e.target.value)}
        >
          <SelectItem value="" text="All Statuses" />
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

      <Column lg={3} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            alignItems: "flex-end",
            height: "100%",
            paddingBottom: "0.5rem",
          }}
        >
          <Checkbox
            id="allInfo2"
            labelText={intl.formatMessage({ id: "eorder.allInfo" })}
            checked={allInfo2}
            onChange={(e) => setAllInfo2(e.currentTarget.checked)}
          />
        </div>
      </Column>

      <Column lg={3} md={4} sm={4} style={{ marginBottom: "1rem" }}>
        <div
          style={{ display: "flex", alignItems: "flex-end", height: "100%" }}
        >
          <Button onClick={searchByDateAndStatus}>
            <FormattedMessage id="label.button.search" />
          </Button>
        </div>
      </Column>

      {/* RESULTS / LOADING / PAGINATION */}
      {searchCompleted && !hasEOrders && (
        <Column lg={16} md={8} sm={4}>
          <div style={{ marginTop: "1rem" }}>
            <FormattedMessage id="eorder.search.noresults" />
          </div>
        </Column>
      )}

      <Column lg={16} md={8} sm={4}>
        {loading && <Loading description="Loading Orders..." small={true} />}
      </Column>

      <>
        <Column lg={14} md={6} sm={2} />
        <Column
          lg={2}
          md={2}
          sm={2}
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "10px",
            marginTop: "1rem",
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
                  disabled={previousPage == null}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                />
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={loadNextResultsPage}
                  disabled={nextPage == null}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                />
              </div>
            </>
          )}
        </Column>
      </>
    </>
  );
};

export default EOrderSearch;
