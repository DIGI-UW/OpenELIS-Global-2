import React, { useEffect, useState } from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Grid,
  Column,
  Toggle,
  FilterableMultiSelect,
  Tabs,
  TabList,
  Tab,
  Tag,
  TabPanels,
  TabPanel,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Section,
  Heading,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { FormattedMessage, useIntl } from "react-intl";
// TODO OGC-746: This LabelsTab import is a transitional shim. Once OGC-746
// ships the dedicated Test Editor modal, the Tabs below should be removed and
// LabelsTab mounted inside the per-test editor panel instead.
// See https://jira.itech.uw.edu/browse/OGC-746
import LabelsTab from "./labelsTab/LabelsTab";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "sidenav.label.admin.testmgt.ViewtestCatalog",
    link: "/MasterListsPage/TestCatalog",
  },
];

const TestCatalog = () => {
  const [showGuide, setShowGuide] = useState(false);
  const [testSectionList, setTestSectionList] = useState([]);
  const [data, setData] = useState([]);
  const intl = useIntl();

  useEffect(() => {
    getFromOpenElisServer(`/rest/TestCatalog`, handleCatalog);
  }, []);

  const handleCatalog = (res) => {
    setTestSectionList(res.testSectionList);
    setData(res.testCatalogList);
  };

  const handleToggle = () => {
    setShowGuide(!showGuide);
  };

  const [selectedSections, setSelectedSections] = useState([]);

  const handleSectionChange = (selectedItems) => {
    setSelectedSections(selectedItems.selectedItems);
  };

  const rows = [
    {
      id: "name",
      field: intl.formatMessage({ id: "field.name" }),
      description: <FormattedMessage id="description.name" />,
    },
    {
      id: "reportName",
      field: intl.formatMessage({ id: "field.reportName" }),
      description: <FormattedMessage id="description.reportName" />,
    },
    {
      id: "active",
      field: intl.formatMessage({ id: "field.active" }),
      description: <FormattedMessage id="description.active" />,
    },
    {
      id: "orderable",
      field: intl.formatMessage({ id: "field.orderable" }),
      description: <FormattedMessage id="description.orderable" />,
    },
    {
      id: "testUnit",
      field: intl.formatMessage({ id: "field.testUnit" }),
      description: <FormattedMessage id="description.testUnit" />,
    },
    {
      id: "sampleType",
      field: intl.formatMessage({ id: "field.sampleType" }),
      description: <FormattedMessage id="description.sampleType" />,
    },
    {
      id: "panel",
      field: intl.formatMessage({ id: "field.panel" }),
      description: <FormattedMessage id="description.panel" />,
    },
    {
      id: "resultType",
      field: intl.formatMessage({ id: "field.resultType" }),
      description: (
        <>
          <p>
            <FormattedMessage id="description.resultType.kind" />
          </p>
          <ul>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.numeric" />
              </strong>
              <FormattedMessage id="description.resultType.numericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.alphanumeric" />
              </strong>
              <FormattedMessage id="description.resultType.alphanumericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.textArea" />
              </strong>
              <FormattedMessage id="description.resultType.textAreaDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.selectList" />
              </strong>
              <FormattedMessage id="description.resultType.selectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.multiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.multiSelectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.cascadingMultiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.cascadingMultiSelectListDesc" />
            </li>
          </ul>
        </>
      ),
    },
    {
      id: "uom",
      field: intl.formatMessage({ id: "field.uom" }),
      description: <FormattedMessage id="description.uom" />,
    },
    {
      id: "significantDigits",
      field: intl.formatMessage({ id: "field.significantDigits" }),
      description: <FormattedMessage id="description.significantDigits" />,
    },
    {
      id: "selectValues",
      field: intl.formatMessage({ id: "field.selectValues" }),
      description: <FormattedMessage id="description.selectValues" />,
    },
    {
      id: "referenceValue",
      field: intl.formatMessage({ id: "field.referenceValue" }),
      description: <FormattedMessage id="description.referenceValue" />,
    },
    {
      id: "resultLimits",
      field: intl.formatMessage({ id: "field.resultLimits" }),
      description: <FormattedMessage id="description.resultLimits" />,
    },
    {
      id: "sex",
      field: intl.formatMessage({ id: "field.sex" }),
      description: <FormattedMessage id="description.sex" />,
    },
    {
      id: "ageRange",
      field: intl.formatMessage({ id: "field.ageRange" }),
      description: <FormattedMessage id="description.ageRange" />,
    },
    {
      id: "normalRange",
      field: intl.formatMessage({ id: "field.normalRange" }),
      description: <FormattedMessage id="description.normalRange" />,
    },
    {
      id: "validRange",
      field: intl.formatMessage({ id: "field.validRange" }),
      description: <FormattedMessage id="description.validRange" />,
    },
    {
      id: "note",
      field: intl.formatMessage({ id: "field.note" }),
      description: <FormattedMessage id="description.note" />,
    },
  ];

  const DataTableComponent = ({ item }) => {
    const headers = [
      {
        key: "field",
        header: intl.formatMessage({ id: "testCatalog.header.field" }),
      },
      {
        key: "value",
        header: intl.formatMessage({ id: "testCatalog.header.value" }),
      },
    ];

    const rows = [
      {
        id: `${item.id}-name`,
        field: intl.formatMessage({ id: "field.name" }),
        value: `en: ${item.localization.english},  fr: ${item.localization.french}`,
      },
      {
        id: `${item.id}-reportName`,
        field: intl.formatMessage({ id: "field.reportName" }),
        value: `en: ${item.reportLocalization.english},  fr: ${item.reportLocalization.french}`,
      },
      {
        id: `${item.id}-testUnit`,
        field: intl.formatMessage({ id: "field.testUnit" }),
        value: item.testUnit,
      },
      {
        id: `${item.id}-sampleType`,
        field: intl.formatMessage({ id: "field.sampleType" }),
        value: item.sampleType,
      },
      {
        id: `${item.id}-panel`,
        field: intl.formatMessage({ id: "field.panel" }),
        value: item.panel,
      },
      {
        id: `${item.id}-resultType`,
        field: intl.formatMessage({ id: "field.resultType" }),
        value: item.resultType,
      },
      {
        id: `${item.id}-uom`,
        field: intl.formatMessage({ id: "field.uom" }),
        value: item.uom,
      },
      item.resultType == "N" && {
        id: `${item.id}-significantDigits`,
        field: intl.formatMessage({ id: "field.significantDigits" }),
        value: item.significantDigits,
      },
      item.loinc && {
        id: `${item.id}-loinc`,
        field: intl.formatMessage({ id: "field.loinc" }),
        value: item.loinc,
      },
      {
        id: `${item.id}-active`,
        field: intl.formatMessage({ id: "field.active" }),
        value: item.active,
      },
      {
        id: `${item.id}-orderable`,
        field: intl.formatMessage({ id: "field.orderable" }),
        value: item.orderable,
      },
      item.hasDictionaryValues && {
        id: `${item.id}-dictionaryValues`,
        field: intl.formatMessage({ id: "field.dictionaryValues" }),
        value: item.dictionaryValues.join(", "),
      },
    ].filter(Boolean);

    const limitHeaders = [
      {
        key: "gender",
        header: intl.formatMessage({ id: "testCatalog.header.gender" }),
      },
      {
        key: "ageRange",
        header: intl.formatMessage({ id: "testCatalog.header.ageRange" }),
      },
      {
        key: "normalRange",
        header: intl.formatMessage({ id: "testCatalog.header.normalRange" }),
      },
      {
        key: "validRange",
        header: intl.formatMessage({ id: "testCatalog.header.validRange" }),
      },
      {
        key: "reportingRange",
        header: intl.formatMessage({ id: "testCatalog.header.reportingRange" }),
      },
      {
        key: "criticalRange",
        header: intl.formatMessage({ id: "testCatalog.header.criticalRange" }),
      },
    ];

    // TODO OGC-746: The <Tabs> below is a transitional shim so the LabelsTab
    // is reachable from the existing Test Catalog view. Once OGC-746 ships the
    // dedicated Test Editor modal, remove this Tabs wrapper and mount LabelsTab
    // inside the per-test editor instead.
    return (
      <Tabs>
        <TabList
          aria-label={intl.formatMessage({
            id: "admin.testCatalog.tabs.ariaLabel",
          })}
        >
          <Tab>
            <FormattedMessage id="admin.testCatalog.tabs.details" />
          </Tab>
          <Tab>
            <FormattedMessage id="admin.testCatalog.tabs.labels" />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader key={header.key}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell>{row.field}</TableCell>
                      <TableCell>{row.value}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              {item.hasLimitValues && (
                <Table>
                  <TableHead>
                    <TableRow>
                      {limitHeaders.map((header) => (
                        <TableHeader key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {item.resultLimits.map((limit, limitIndex) => (
                      <TableRow key={limitIndex}>
                        <TableCell>{limit.gender}</TableCell>
                        <TableCell>{limit.ageRange}</TableCell>
                        <TableCell>{limit.normalRange}</TableCell>
                        <TableCell>{limit.validRange}</TableCell>
                        <TableCell>{limit.reportingRange}</TableCell>
                        <TableCell>{limit.criticalRange}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </TableContainer>
          </TabPanel>
          <TabPanel>
            <LabelsTab testId={String(item.id)} />
          </TabPanel>
        </TabPanels>
      </Tabs>
    );
  };

  return (
    <div className="adminPageContent">
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <br />
      <div className="orderLegendBody" style={{ minHeight: "80vh" }}>
        <Grid fullWidth={true}>
          <Column lg={12} md={6} sm={3}>
            <h1>
              {" "}
              <FormattedMessage id="label.viewtestCatalog" />
            </h1>
          </Column>
          <Column lg={12} md={6} sm={3}>
            <Toggle
              id="toggle"
              labelText={<FormattedMessage id="test.show.guide" />}
              onClick={handleToggle}
            />
          </Column>
        </Grid>
        <hr />
        <Grid>
          <Column lg={11} md={6} sm={3}>
            <br />
            <h5>
              <FormattedMessage id="label.selectTestSectionCatalog" />
            </h5>
          </Column>
          <Column lg={5} md={2} sm={1}>
            <FilterableMultiSelect
              type="inline"
              id="carbon-multiselect-example-3"
              items={
                Array.isArray(testSectionList)
                  ? [...testSectionList, "All"]
                  : ["All"]
              }
              itemToString={(item) => (typeof item === "string" ? item : "")}
              onChange={handleSectionChange}
              selectedItems={selectedSections}
              selectionFeedback="top-after-reopen"
              placeholder={intl.formatMessage({
                id: "field.select.testSection",
              })}
            />
          </Column>
        </Grid>

        {selectedSections.length > 0 && (
          <>
            {selectedSections.map((section, index) => (
              <Tag key={index} type="cyan">
                {section}
              </Tag>
            ))}
          </>
        )}
        {showGuide && (
          <>
            <hr />
            <StructuredListWrapper ariaLabel="Structured list">
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>
                    <FormattedMessage id="label.button.field" />
                  </StructuredListCell>
                  <StructuredListCell head>
                    <FormattedMessage id="label.button.descritpion" />
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                {rows.map((row) => (
                  <StructuredListRow key={row.id}>
                    <StructuredListCell>{row.field}</StructuredListCell>
                    <StructuredListCell>{row.description}</StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
            <hr />
            <br />
          </>
        )}

        {selectedSections.length > 0 && (
          <>
            <br />
            <br />
            <Tabs>
              <TabList aria-label="List of tabs">
                {selectedSections
                  .sort((a, b) => (a === "All" ? -1 : b === "All" ? 1 : 0))
                  .map((section) => (
                    <Tab key={section}>{section}</Tab>
                  ))}
              </TabList>
              <TabPanels>
                {selectedSections
                  .sort((a, b) => (a === "All" ? -1 : b === "All" ? 1 : 0))
                  .map((section) => (
                    <TabPanel key={section}>
                      {section === "All"
                        ? data.map((item) => (
                            <div key={item.id}>
                              <DataTableComponent item={item} />
                              <br />
                            </div>
                          ))
                        : data
                            .filter((item) => item.testUnit === section)
                            .map((item) => (
                              <div key={item.id}>
                                <DataTableComponent item={item} />
                                <br />
                              </div>
                            ))}
                    </TabPanel>
                  ))}
              </TabPanels>
            </Tabs>
          </>
        )}
      </div>
    </div>
  );
};

export default TestCatalog;
