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
  FlexGrid,
  Row,
  Tile
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

  const [hasEOrders, setHasEOrders] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [statusId, setStatusId] = useState("");
  const [statusOptions, setStatusOptions] = useState([]);
  const [allInfo, setAllInfo] = useState(false);
  const [allInfo2, setAllInfo2] = useState(false);
  const [searchCompleted, setSearchCompleted] = useState(false);
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);

  useEffect(() => {
    getFromOpenElisServer("/rest/ElectronicOrders", handleElectronicOrders);
    getFromOpenElisServer(
      "/rest/displayList/ELECTRONIC_ORDER_STATUSES",
      handleOrderStatus,
    );
    
    const handleResize = () => {
      setWindowWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
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

  const getColumnSizes = () => {
    if (windowWidth < 672) { 
      return { 
        main: { sm: 4 },
        half: { sm: 4 },
        third: { sm: 4 },
        quarter: { sm: 4 }
      };
    } else if (windowWidth < 1056) {
      return { 
        main: { md: 8 },
        half: { md: 4 },
        third: { md: 2 },
        quarter: { md: 2 }
      };
    } else {
      return { 
        main: { lg: 16 },
        half: { lg: 8 },
        third: { lg: 4 },
        quarter: { lg: 2 }
      };
    }
  };
  
  const colSizes = getColumnSizes();

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid narrow>
        <Column {...colSizes.main}>
          <Section>
            <Heading>
              <FormattedMessage id="eorder.header" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      
      <Tile>
        <Stack gap={5}>
          {/* Search by Identifier Section */}
          <Stack gap={4}>
            <Column {...colSizes.main}>
              <FormattedMessage id="eorder.search1.text" />
            </Column>
            
            <Grid narrow>
              <Column {...colSizes.main}>
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
            </Grid>
            
            <Grid narrow>
              <Column {...(windowWidth > 672 ? colSizes.half : colSizes.main)}>
                <Checkbox
                  id="allInfo1"
                  labelText={intl.formatMessage({ id: "eorder.allInfo" })}
                  checked={allInfo}
                  onChange={(e) => {
                    setAllInfo(e.currentTarget.checked);
                  }}
                />
              </Column>
              
              <Column {...(windowWidth > 672 ? colSizes.half : colSizes.main)}>
                <Button onClick={searchByIdentifier} size={windowWidth < 672 ? "md" : "default"}>
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
            </Grid>
          </Stack>
          
          <div style={{ width: "100%", borderBottom: "1px solid #e0e0e0", margin: "1rem 0" }}></div>
          
          {/* Search by Date and Status Section */}
          <Stack gap={4}>
            <Column {...colSizes.main}>
              <FormattedMessage id="eorder.search2.text" />
            </Column>
            
            <Grid narrow>
              <Column {...(windowWidth < 672 ? colSizes.main : (windowWidth < 1056 ? { md: 4 } : { lg: 4 }))}>
                <CustomDatePicker
                  id="eOrder_startDate"
                  labelText={intl.formatMessage({ id: "eorder.date.start" })}
                  value={startDate}
                  onChange={(date) => setStartDate(date)}
                />
              </Column>
              
              <Column {...(windowWidth < 672 ? colSizes.main : (windowWidth < 1056 ? { md: 4 } : { lg: 4 }))}>
                <CustomDatePicker
                  id="eOrder_endDate"
                  labelText={intl.formatMessage({ id: "eorder.date.end" })}
                  value={endDate}
                  onChange={(date) => setEndDate(date)}
                />
              </Column>
            </Grid>
            
            <Grid narrow>
              <Column {...(windowWidth < 672 ? colSizes.main : colSizes.half)}>
                <Select
                  id="statusId"
                  labelText={intl.formatMessage({ id: "eorder.status" })}
                  value={statusId}
                  onChange={(e) => {
                    setStatusId(e.target.value);
                  }}
                >
                  <SelectItem value="" text="All Statuses" />
                  {statusOptions.map((statusOption, index) => (
                    <SelectItem
                      key={index}
                      value={statusOption.id}
                      text={statusOption.value}
                    />
                  ))}
                </Select>
              </Column>
              
              <Column {...(windowWidth < 672 ? colSizes.main : colSizes.half)}>
                <Stack>
                  <Checkbox
                    id="allInfo2"
                    labelText={intl.formatMessage({ id: "eorder.allInfo" })}
                    checked={allInfo2}
                    onChange={(e) => {
                      setAllInfo2(e.currentTarget.checked);
                    }}
                  />
                </Stack>
              </Column>
            </Grid>
            
            <Grid narrow>
              <Column {...colSizes.main}>
                <Button 
                  onClick={searchByDateAndStatus} 
                  size={windowWidth < 672 ? "md" : "default"}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
            </Grid>
          </Stack>
          
          {/* No Results Message */}
          {searchCompleted && !hasEOrders && (
            <Grid narrow>
              <Column {...colSizes.main}>
                <div style={{ marginTop: "1rem" }}>
                  <FormattedMessage id="eorder.search.noresults" />
                </div>
              </Column>
            </Grid>
          )}
        </Stack>
      </Tile>
    </>
  );
};

export default EOrderSearch;