import React, { useContext, useState, useEffect, useRef } from "react";
import type { ChangeEvent } from "react";
import {
  Form,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AutoComplete from "../../common/AutoComplete";

interface OrganizationType {
  id: string;
  name: string;
  description: string;
  disabled?: boolean;
}

interface ParentOrganization {
  id?: string;
  isActive?: string | boolean;
  lastupdated?: string;
  mlsSentinelLabFlag?: string | boolean;
  organizationName?: string;
  organizationTypes?: OrganizationType[];
  shortName?: string;
  parentOrganizationName?: string;
}

interface OrganizationResponse {
  id?: string;
  organizationName?: string;
  shortName?: string;
  isActive?: string | boolean;
  internetAddress?: string;
  selectedTypes: string[];
  cliaNum?: string;
  streetAddress?: string;
  city?: string;
  orgTypes: OrganizationType[];
  organization?: ParentOrganization;
  lastupdated?: string;
  commune?: string;
  village?: string;
  department?: string;
  formName?: string;
  formMethod?: string;
  cancelAction?: string;
  submitOnCancel?: boolean;
  cancelMethod?: string;
  mlsSentinelLabFlag?: string | boolean;
  parentOrgName?: string;
  state?: string;
}

interface OrganizationFormData extends ParentOrganization {
  internetAddress?: string;
  selectedTypes?: string[];
  cliaNum?: string;
  streetAddress?: string;
  city?: string;
  organization?: ParentOrganization;
  [key: string]: unknown;
}

interface CarbonTableCell {
  id: string;
  value: string | number | boolean;
  info: { header: string };
}

interface CarbonTableRow {
  id: string;
}

interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  addNotification: (notification: {
    kind: string;
    title: string;
    message: string;
  }) => void;
}

interface ConfigurationContextValue {
  configurationProperties: Record<string, string>;
}

// eslint-disable-next-line prefer-const -- preserve the original JavaScript runtime declaration
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "organization.main.title",
    link: "/MasterListsPage/organizationManagement",
  },
];

function OrganizationAddModify() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;
  const { configurationProperties } = useContext(
    ConfigurationContext,
  ) as ConfigurationContextValue;

  const componentMounted = useRef(false);
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- preserve the original state tuple
  const [page, setPage] = useState(1);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- preserve the original state tuple
  const [pageSize, setPageSize] = useState(20);
  const [selectedRowIds, setSelectedRowIds] = useState<string[]>([]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- write-only legacy state preserved during migration
  const [orgSelectedTypeOfActivity, setOrgSelectedTypeOfActivity] = useState<
    Array<{ id: string }>
  >([]);
  const [parentOrgList, setParentOrgList] = useState<ParentOrganization[]>([]);
  const [parentOrgId, setParentOrgId] = useState("");
  const [parentOrg, setParentOrg] = useState<ParentOrganization>({});
  const [parentOrgPost, setParentOrgPost] = useState<ParentOrganization>({});
  const [orgInfo, setOrgInfo] = useState<OrganizationFormData>({});
  const [orgInfoPost, setOrgInfoPost] = useState<OrganizationFormData>({});
  const [saveButton, setSaveButton] = useState(true);
  const [typeOfActivity, setTypeOfActivity] = useState<OrganizationResponse>();
  const [typeOfActivityShow, setTypeOfActivityShow] = useState<
    OrganizationType[]
  >([]);

  const location = useLocation();
  const ID = (() => {
    const search = location.search;
    if (search) {
      const urlParams = new URLSearchParams(search);
      return urlParams.get("ID");
    }
    return "0";
  })();

  // eslint-disable-next-line local/no-useeffect-timer-leaks -- preserve the original redirect behavior
  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    if (ID) {
      getFromOpenElisServer(
        `/rest/Organization?ID=${ID}&startingRecNo=1`,
        handleMenuItems,
      );
    } else {
      setTimeout(() => {
        window.location.assign("/MasterListsPage/organizationManagement");
      }, 1000);
    }
    return () => {
      componentMounted.current = false;
    };
  }, [ID]);

  const handleMenuItems = (res?: OrganizationResponse) => {
    if (!res) {
      setLoading(true);
    } else {
      setTypeOfActivity(res);
    }
  };

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/displayList/ACTIVE_ORG_LIST`,
      handleParentOrgList,
    );
  }, []);

  const handleParentOrgList = (res?: ParentOrganization[]) => {
    if (!res) {
      setLoading(true);
    } else {
      setParentOrgList(res);
    }
  };

  useEffect(() => {
    if (typeOfActivity) {
      const newOrganizationsManagementList = typeOfActivity.orgTypes.map(
        (item) => {
          return {
            id: item.id,
            name: item.name,
            description: item.description,
          };
        },
      );
      const newOrganizationsManagementListArray = Object.values(
        newOrganizationsManagementList,
      );
      setTypeOfActivityShow(newOrganizationsManagementListArray);

      const organizationsManagementIdInfo = {
        id: typeOfActivity.id,
        organizationName: typeOfActivity.organizationName,
        shortName: typeOfActivity.shortName,
        isActive: typeOfActivity.isActive,
        internetAddress: typeOfActivity.internetAddress,
        selectedTypes: typeOfActivity.selectedTypes,
        cliaNum: typeOfActivity.cliaNum,
        streetAddress: typeOfActivity.streetAddress,
        city: typeOfActivity.city,
      };

      const organizationsManagementIdInfoPost = {
        id: typeOfActivity.id,
        organizationName: typeOfActivity.organizationName,
        shortName: typeOfActivity.shortName,
        isActive: typeOfActivity.isActive,
        lastupdated: typeOfActivity.lastupdated,
        commune: typeOfActivity.commune,
        village: typeOfActivity.village,
        department: typeOfActivity.department,
        formName: typeOfActivity.formName,
        formMethod: typeOfActivity.formMethod,
        cancelAction: typeOfActivity.cancelAction,
        submitOnCancel: typeOfActivity.submitOnCancel,
        cancelMethod: typeOfActivity.cancelMethod,
        mlsSentinelLabFlag: typeOfActivity.mlsSentinelLabFlag,
        parentOrgName: typeOfActivity.parentOrgName,
        state: typeOfActivity.state,
        internetAddress: typeOfActivity.internetAddress,
        selectedTypes: typeOfActivity.selectedTypes,
        organization: typeOfActivity.organization,
        cliaNum: typeOfActivity.cliaNum,
        streetAddress: typeOfActivity.streetAddress,
        city: typeOfActivity.city,
      };
      setOrgInfo(organizationsManagementIdInfo);
      setOrgInfoPost(organizationsManagementIdInfoPost);
      setSelectedRowIds(typeOfActivity.selectedTypes);

      if (ID !== "0") {
        const organizationSelectedTypeOfActivity =
          typeOfActivity.selectedTypes.map((item) => {
            return {
              id: item,
            };
          });
        const organizationSelectedTypeOfActivityListArray = Object.values(
          organizationSelectedTypeOfActivity,
        );
        setOrgSelectedTypeOfActivity(
          organizationSelectedTypeOfActivityListArray,
        );
      } else {
        setOrgSelectedTypeOfActivity([]);
      }
    }
  }, [typeOfActivity, ID]);

  useEffect(() => {
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      selectedTypes: selectedRowIds,
    }));
  }, [selectedRowIds, orgInfo]);

  // const handlePageChange = ({ page, pageSize }) => {
  //   setPage(page);
  //   setPageSize(pageSize);
  //   setSelectedRowIds([]);
  // };

  function handleOrgNameChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      organizationName: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      organizationName: e.target.value,
    }));
  }

  function handleOrgPrefixChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      shortName: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      shortName: e.target.value,
    }));
  }

  function handleStreetAddressChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      streetAddress: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      streetAddress: e.target.value,
    }));
  }

  function handleCityChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      city: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      city: e.target.value,
    }));
  }

  function handleCliaNumberChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      cliaNum: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      cliaNum: e.target.value,
    }));
  }

  function handleIsActiveChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      isActive: e.target.value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      isActive: e.target.value,
    }));
  }

  function handleInternetAddressChange(e: ChangeEvent<HTMLInputElement>) {
    setSaveButton(false);
    const value = e.target.value.trim();
    const urlPattern =
      /^(https?:\/\/)?(www\.)?[\w-]+\.[a-z]{2,}(\.[a-z]{2,})?$/i;

    if (value && !urlPattern.test(value)) {
      if (!notificationVisible) {
        setNotificationVisible(true);
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
          }),
          message: intl.formatMessage({
            id: "notification.organization.post.internetAddress",
          }),
          kind: NotificationKinds.info,
        });
      }
    } else {
      setNotificationVisible(false);
    }

    setOrgInfoPost((prevOrgInfoPost) => ({
      ...prevOrgInfoPost,
      internetAddress: value,
    }));
    setOrgInfo((prevOrgInfo) => ({
      ...prevOrgInfo,
      internetAddress: value,
    }));
  }

  function handleParentOrganizationName(e: ChangeEvent<HTMLInputElement>) {
    setParentOrgPost({
      ...parentOrgPost,
      parentOrganizationName: e.target.value,
    });
    setSaveButton(false);
  }

  function handleAutoCompleteParentOrganizationNames(parentOrgId: string) {
    setParentOrgId(parentOrgId);
    setSaveButton(false);
  }

  const handleParentOrgPost = (res?: ParentOrganization) => {
    if (!res) {
      setLoading(true);
    } else {
      setParentOrg(res);
    }
  };

  useEffect(() => {
    if (parentOrgId) {
      getFromOpenElisServer(
        `/rest/organization/${parentOrgId}`,
        handleParentOrgPost,
      );
    }
  }, [parentOrgId]);

  useEffect(() => {
    if (parentOrg) {
      const parentOrgPost = {
        id: parentOrg.id,
        isActive: parentOrg.isActive,
        lastupdated: parentOrg.lastupdated,
        mlsSentinelLabFlag: parentOrg.mlsSentinelLabFlag,
        organizationName: parentOrg.organizationName,
        organizationTypes: parentOrg.organizationTypes,
        shortName: parentOrg.shortName,
      };
      setParentOrgPost(parentOrgPost);
      setOrgInfoPost((prevOrgInfo) => ({
        ...prevOrgInfo,
        organization: parentOrgPost,
      }));
    }
  }, [parentOrg]);

  function submitAddUpdatedOrgInfo() {
    setLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/Organization?ID=${ID}&startingRecNo=1`,
      JSON.stringify(orgInfoPost),
      () => {
        submitAddUpdatedOrgInfoCallback();
      },
    );
  }

  const submitAddUpdatedOrgInfoCallback = () => {
    setLoading(false);
    addNotification({
      title: intl.formatMessage({
        id: "notification.title",
      }),
      message: intl.formatMessage({
        id: "notification.organization.post.success",
      }),
      kind: NotificationKinds.success,
    });
    setTimeout(() => {
      window.location.assign("/MasterListsPage/organizationManagement");
    }, 200);
    setNotificationVisible(true);
  };

  const renderCell = (cell: CarbonTableCell, row: CarbonTableRow) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          key={cell.id}
          id={cell.id}
          checked={selectedRowIds.includes(row.id)}
          name="selectRowCheckbox"
          ariaLabel="selectRows"
          onSelect={() => {
            setSaveButton(false);
            if (selectedRowIds.includes(row.id)) {
              setSelectedRowIds(selectedRowIds.filter((id) => id !== row.id));
            } else {
              setSelectedRowIds([...selectedRowIds, row.id]);
            }
          }}
        />
      );
    } else if (cell.info.header === "active") {
      return <TableCell key={cell.id}>{cell.value.toString()}</TableCell>;
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  if (!loading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                {ID === "0" ? (
                  <FormattedMessage id="organization.add.title" />
                ) : (
                  <FormattedMessage id="organization.edit.title" />
                )}
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <div className="orderLegendBody">
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <Form
              // onSubmit={handleSubmit}
              // onChange={setSaveButton(false)}
              // onBlur={handleBlur}
              >
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.organizationName" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-name"
                      className="defalut"
                      type="text"
                      labelText=""
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder",
                      })}
                      required={true}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      value={
                        orgInfo && orgInfo.organizationName
                          ? orgInfo.organizationName
                          : ""
                      }
                      onChange={(e) => handleOrgNameChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.short.CI" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-prefix"
                      className="defalut"
                      type="text"
                      labelText=""
                      maxLength={15}
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder",
                      })}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      required={true}
                      value={
                        orgInfo && orgInfo.shortName ? orgInfo.shortName : ""
                      }
                      onChange={(e) => handleOrgPrefixChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.isActive" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="is-active"
                      className="defalut"
                      type="text"
                      labelText=""
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder.active",
                      })}
                      required={true}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      value={
                        orgInfo && orgInfo.isActive ? orgInfo.isActive : ""
                      }
                      onChange={(e) => handleIsActiveChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.internetaddress" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-internet-address"
                      className="defalut"
                      type="text"
                      labelText=""
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder.internetAddress",
                      })}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      value={
                        orgInfo && orgInfo.internetAddress
                          ? orgInfo.internetAddress
                          : ""
                      }
                      onChange={(e) => handleInternetAddressChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.streetAddress" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-street-address"
                      className="defalut"
                      type="text"
                      labelText=""
                      maxLength={15}
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder",
                      })}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      // required={true}
                      value={
                        orgInfo && orgInfo.streetAddress
                          ? orgInfo.streetAddress
                          : ""
                      }
                      onChange={(e) => handleStreetAddressChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.city" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-city"
                      className="defalut"
                      type="text"
                      labelText=""
                      maxLength={15}
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder",
                      })}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      // required={true}
                      value={orgInfo && orgInfo.city ? orgInfo.city : ""}
                      onChange={(e) => handleCityChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.clia.number" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="org-clia-number"
                      className="defalut"
                      type="text"
                      placeholder={intl.formatMessage({
                        id: "organization.add.placeholder",
                      })}
                      // invalid={errors.order && touched.order}
                      // invalidText={errors.order}
                      // required={true}
                      value={orgInfo && orgInfo.cliaNum ? orgInfo.cliaNum : ""}
                      onChange={(e) => handleCliaNumberChange(e)}
                    />
                  </Column>
                </Grid>
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="organization.parent" /> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <AutoComplete
                      name="parentOrgName"
                      id="parentOrgName"
                      allowFreeText={
                        !(
                          configurationProperties.restrictFreeTextRefSiteEntry ===
                          "true"
                        )
                      }
                      value={
                        typeOfActivity &&
                        typeOfActivity.organization &&
                        typeOfActivity.organization.organizationName != ""
                          ? typeOfActivity.organization.organizationName
                          : ""
                      }
                      onChange={handleParentOrganizationName}
                      onSelect={handleAutoCompleteParentOrganizationNames}
                      label={
                        <>
                          <FormattedMessage id="organization.search.parent.name" />{" "}
                          <span className="requiredlabel">*</span>
                        </>
                      }
                      style={{ width: "!important 100%" }}
                      suggestions={
                        parentOrgList.length > 0 ? parentOrgList : []
                      }
                      required
                    />
                  </Column>
                </Grid>
              </Form>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <>
                        <FormattedMessage id="organization.type.CI" />
                        <span className="requiredlabel">*</span>
                      </>
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <br />
              <DataTable
                rows={typeOfActivityShow.slice(
                  (page - 1) * pageSize,
                  page * pageSize,
                )}
                headers={[
                  {
                    key: "select",
                    header: intl.formatMessage({
                      id: "organization.type.CI.select",
                    }),
                  },
                  {
                    key: "name",
                    header: intl.formatMessage({
                      id: "organization.type.CI.name",
                    }),
                  },

                  {
                    key: "description",
                    header: intl.formatMessage({
                      id: "organization.type.CI.description",
                    }),
                  },
                ]}
              >
                {({
                  rows,
                  headers,
                  getHeaderProps,
                  getTableProps,
                  getSelectionProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          <TableSelectAll
                            id="table-select-all"
                            {...getSelectionProps()}
                            checked={typeOfActivityShow
                              .slice((page - 1) * pageSize, page * pageSize)
                              .filter((row) => !row.disabled)
                              .every((row) => selectedRowIds.includes(row.id))}
                            indeterminate={
                              selectedRowIds.length > 0 &&
                              selectedRowIds.length <
                                typeOfActivityShow
                                  .slice((page - 1) * pageSize, page * pageSize)
                                  .filter((row) => !row.disabled).length
                            }
                            onSelect={() => {
                              setSaveButton(false);
                              const currentPageIds = typeOfActivityShow
                                .slice((page - 1) * pageSize, page * pageSize)
                                .filter((row) => !row.disabled)
                                .map((row) => row.id);
                              if (
                                selectedRowIds.length === pageSize &&
                                currentPageIds.every((id) =>
                                  selectedRowIds.includes(id),
                                )
                              ) {
                                setSelectedRowIds([]);
                              } else {
                                setSelectedRowIds(
                                  currentPageIds.filter(
                                    (id) => !selectedRowIds.includes(id),
                                  ),
                                );
                              }
                            }}
                          />
                          {headers.map(
                            (header) =>
                              header.key !== "select" && (
                                <TableHeader
                                  key={header.key}
                                  {...getHeaderProps({ header })}
                                >
                                  {header.header}
                                </TableHeader>
                              ),
                          )}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <>
                          {rows.map((row) => (
                            <TableRow
                              key={row.id}
                              onClick={() => {
                                const id = row.id;
                                const isSelected = selectedRowIds.includes(id);
                                if (isSelected) {
                                  setSelectedRowIds(
                                    selectedRowIds.filter(
                                      (selectedId) => selectedId !== id,
                                    ),
                                  );
                                } else {
                                  setSelectedRowIds([...selectedRowIds, id]);
                                }
                              }}
                            >
                              {row.cells.map((cell) => renderCell(cell, row))}
                            </TableRow>
                          ))}
                        </>
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
              <br />
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                id="saveButton"
                disabled={saveButton}
                onClick={submitAddUpdatedOrgInfo}
                type="button"
              >
                Save
              </Button>{" "}
              <Button
                onClick={() =>
                  window.location.assign(
                    "/MasterListsPage/organizationManagement",
                  )
                }
                kind="tertiary"
                type="button"
              >
                Exit
              </Button>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(OrganizationAddModify);
