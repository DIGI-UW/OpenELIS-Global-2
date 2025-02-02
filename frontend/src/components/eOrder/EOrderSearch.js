import {
  Button,
  Checkbox,
  Column,
  Form,
  Grid,
  Heading,
  Section,
  Select,
  SelectItem,
  TextInput,
  Stack,
} from "@carbon/react";
import { React, useEffect, useState } from "react";
import CustomDatePicker from "../common/CustomDatePicker";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const EOrderSearch = ({
  setEOrders = (eOrders) => {
    console.debug("set EOrders default");
  },
  eOrderRef,
}) => {
  const intl = useIntl();

  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [hasEOrders, setHasEOrders] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [statusId, setStatusId] = useState("");
  const [statusOptions, setStatusOptions] = useState([]);
  const [allInfo, setAllInfo] = useState(false);
  const [allInfo2, setAllInfo2] = useState(false);
  const [searchCompleted, setSearchCompleted] = useState(false);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

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
  };

  function searchByIdentifier() {
    const params = new URLSearchParams({
      searchType: "IDENTIFIER",
      searchValue: searchValue,
      useAllInfo: allInfo,
    });

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
    getFromOpenElisServer(
      "/rest/ElectronicOrders?" + params.toString(),
      parseEOrders,
    );
  }

  const parseEOrders = (response) => {
    setSearchCompleted(true);
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
  };

  return (
    <div className="eorder-search-container">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid narrow={isMobile} className="header-grid">
        <Column sm={4} md={8} lg={16}>
          <Section>
            <Heading>
              <FormattedMessage id="eorder.header" />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Grid narrow={isMobile}>
          {/* Search by Identifier Section */}
          <Column sm={4} md={8} lg={16} className="search-section">
            <Stack gap={5}>
              <FormattedMessage id="eorder.search1.text" />

              <div className="search-row">
                <Column sm={4} md={6} lg={8} className="search-input">
                  <TextInput
                    id="searchValue"
                    labelText={intl.formatMessage({ id: "eorder.searchValue" })}
                    value={searchValue}
                    onChange={(e) => setSearchValue(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") searchByIdentifier();
                    }}
                  />
                </Column>

                <Column sm={2} md={2} lg={2} className="checkbox-column">
                  <Checkbox
                    id="allInfo1"
                    labelText={intl.formatMessage({ id: "eorder.allInfo" })}
                    checked={allInfo}
                    onChange={(e) => setAllInfo(e.currentTarget.checked)}
                  />
                </Column>

                <Column sm={2} md={2} lg={4} className="button-column">
                  <Button
                    onClick={searchByIdentifier}
                    className="search-button"
                  >
                    <FormattedMessage id="label.button.search" />
                  </Button>
                </Column>
              </div>
            </Stack>
          </Column>

          <Column sm={4} md={8} lg={16}>
            <hr className="section-divider" />
          </Column>

          {/* Search by Date and Status Section */}
          <Column sm={4} md={8} lg={16} className="search-section">
            <Stack gap={5}>
              <FormattedMessage id="eorder.search2.text" />

              <div className="date-status-row">
                <Column sm={4} md={2} lg={2} className="date-picker">
                  <CustomDatePicker
                    id="eOrder_startDate"
                    labelText={intl.formatMessage({ id: "eorder.date.start" })}
                    value={startDate}
                    onChange={setStartDate}
                    className="inputDate"
                  />
                </Column>

                <Column sm={4} md={2} lg={2} className="date-picker">
                  <CustomDatePicker
                    id="eOrder_endDate"
                    labelText={intl.formatMessage({ id: "eorder.date.end" })}
                    value={endDate}
                    onChange={setEndDate}
                    className="inputDate"
                  />
                </Column>

                <Column sm={4} md={4} lg={4} className="status-select">
                  <Select
                    id="statusId"
                    labelText={intl.formatMessage({ id: "eorder.status" })}
                    value={statusId}
                    onChange={(e) => setStatusId(e.target.value)}
                  >
                    <SelectItem value="" text="All Statuses" />
                    {statusOptions.map((option, index) => (
                      <SelectItem
                        key={index}
                        value={option.id}
                        text={option.value}
                      />
                    ))}
                  </Select>
                </Column>

                <Column sm={2} md={2} lg={2} className="checkbox-column">
                  <Checkbox
                    id="allInfo2"
                    labelText={intl.formatMessage({ id: "eorder.allInfo" })}
                    checked={allInfo2}
                    onChange={(e) => setAllInfo2(e.currentTarget.checked)}
                  />
                </Column>

                <Column sm={2} md={2} lg={4} className="button-column">
                  <Button
                    onClick={searchByDateAndStatus}
                    className="search-button"
                  >
                    <FormattedMessage id="label.button.search" />
                  </Button>
                </Column>
              </div>
            </Stack>
          </Column>

          {searchCompleted && !hasEOrders && (
            <Column sm={4} md={8} lg={16} className="no-results">
              <FormattedMessage id="eorder.search.noresults" />
            </Column>
          )}
        </Grid>
      </div>

      <style>
        {`
          .eorder-search-container {
            max-width: 100%;
            padding: 1rem;
          }

          .header-grid {
            margin-bottom: 2rem;
          }

          .search-section {
            margin-bottom: ${isMobile ? "2rem" : "1.5rem"};
          }

          .search-row,
          .date-status-row {
            display: flex;
            flex-wrap: wrap;
            gap: 1rem;
            width: 100%;
          }

          .search-input {
            flex: 1 1 300px;
          }

          .checkbox-column {
            flex: 0 1 auto;
            display: flex;
            align-items: flex-end;
            margin-bottom: ${isMobile ? "1rem" : "0"};
          }

          .button-column {
            flex: 0 1 auto;
            display: flex;
            align-items: flex-end;
          }

          .search-button {
            width: ${isMobile ? "100%" : "auto"};
          }

          .date-picker {
            flex: 1 1 200px;
          }

          .status-select {
            flex: 1 1 250px;
          }

          .section-divider {
            margin: 2rem 0;
            border: none;
            border-top: 1px solid #e0e0e0;
          }

          .no-results {
            margin-top: 2rem;
            padding: 1rem;
            background-color: #f4f4f4;
            border-radius: 4px;
          }

          @media (max-width: 768px) {
            .date-status-row {
              flex-direction: column;
            }

            .checkbox-column,
            .button-column {
              width: 100%;
              justify-content: flex-start;
            }

            .inputDate {
              width: 100%;
            }
          }

          @media (max-width: 480px) {
            .eorder-search-container {
              padding: 0.5rem;
            }
          }
        `}
      </style>
    </div>
  );
};

export default EOrderSearch;
