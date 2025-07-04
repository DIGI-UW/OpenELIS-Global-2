import {
  Button,
  Checkbox,
  Column,
  Link,
  Grid,
  Select,
  SelectItem,
  TextInput,
  Loading,
} from "@carbon/react";
import { React, useEffect, useState, useContext } from "react";
import CustomDatePicker from "../common/CustomDatePicker";
import { Minimize, Maximize, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
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
    if (response.eOrders.length == 0) {
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
      <Column lg={16} md={8} sm={4}>
        <FormattedMessage id="eorder.search1.text" />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <br></br>
      </Column>
      <Column lg={9} md={4} sm={4}>
        <TextInput
          id="searchValue"
          labelText={intl.formatMessage({ id: "eorder.searchValue" })}
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
      <Column lg={2}>
        <div className="bottomAlign">
          <Checkbox
            id="allInfo1"
            labelText={intl.formatMessage({ id: "eorder.allInfo" })}
            checked={allInfo}
            onChange={(e) => {
              setAllInfo(e.currentTarget.checked);
            }}
          />
        </div>
      </Column>
      <Column lg={1}></Column>
      <Column lg={4}>
        <Button onClick={searchByIdentifier}>
          <FormattedMessage id="label.button.search" />
        </Button>
      </Column>

      <Column lg={16} md={8} sm={4}>
        <hr></hr>
      </Column>
      <Column lg={16} md={8} sm={4}>
        <FormattedMessage id="eorder.search2.text" />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <br></br>
      </Column>
      <Column lg={3} md={2} sm={2}>
        <CustomDatePicker
          id={"eOrder_startDate"}
          labelText={intl.formatMessage({ id: "eorder.date.start" })}
          value={startDate}
          className="inputDate"
          onChange={(date) => setStartDate(date)}
        />
      </Column>
      <Column lg={3} md={2} sm={2}>
        <CustomDatePicker
          id={"eOrder_startDate"}
          labelText={intl.formatMessage({ id: "eorder.date.end" })}
          value={startDate}
          className="inputDate"
          onChange={(date) => setEndDate(date)}
        />
      </Column>
      <Column lg={3} md={2} sm={2}>
        <Select
          id="statusId"
          labelText={intl.formatMessage({ id: "eorder.status" })}
          value={statusId}
          onChange={(e) => {
            setStatusId(e.target.value);
          }}
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
      <Column lg={2}>
        <div className="bottomAlign">
          <Checkbox
            id="allInfo2"
            labelText={intl.formatMessage({ id: "eorder.allInfo" })}
            checked={allInfo2}
            onChange={(e) => {
              setAllInfo2(e.currentTarget.checked);
            }}
          />
        </div>
      </Column>
      <Column lg={1}></Column>
      <Column lg={4} md={4} sm={2}>
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

export default EOrderSearch;
