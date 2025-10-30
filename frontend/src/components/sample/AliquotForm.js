import React, { useContext, useEffect, useState, useRef } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  Form,
  Button,
  Grid,
  Column,
  Stack,
  Loading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandRow,
  TableExpandHeader,
  TableExpandedRow,
  Select,
  SelectItem,
  NumberInput,
  InlineLoading,
  Tag,
} from "@carbon/react";
import {
  Copy,
  Add,
  Subtract,
  Move,
  Warning,
  Checkmark,
} from "@carbon/icons-react";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import { Formik, Field } from "formik";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { ConfigurationContext } from "../layout/Layout";

function AliquotPage() {
  const [sampleForm, setSampleForm] = useState({
    sampleItems: [],
    accessionNumber: "",
  });
  const [searchBy, setSearchBy] = useState({ type: "", doRange: false });
  const [param, setParam] = useState("&accessionNumber=");

  const setSampleData = (sampleData) => {
    setSampleForm(sampleData);
  };

  return (
    <>
      <SearchSampleForm
        setParam={setParam}
        setSearchBy={setSearchBy}
        setSampleData={setSampleData}
      />
      <SampleItemsDisplay
        sampleData={sampleForm}
        searchBy={searchBy}
        extraParams={param}
        setSampleData={setSampleData}
      />
    </>
  );
}

export function SearchSampleForm(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [loading, setLoading] = useState(false);
  const [searchFormValues, setSearchFormValues] = useState({
    accessionNumber: "",
  });
  const intl = useIntl();

  const querySearch = (values) => {
    setLoading(true);

    let accessionNumber = values.accessionNumber;
    let searchEndPoint = "/rest/SampleItem?accessionNumber=" + accessionNumber;

    getFromOpenElisServer(searchEndPoint, setSampleData);
  };

  const setSampleData = (sampleData) => {
    if (
      sampleData &&
      sampleData.sampleItems &&
      sampleData.sampleItems.length > 0
    ) {
      // Initialize aliquoting state for each sample item
      const sampleItemsWithAliquoting = sampleData.sampleItems.map((item) => ({
        ...item,
        aliquots: [],
        showAliquoting: false,
        remainingQuantity: item.quantity || 0, // Track remaining quantity after aliquoting
      }));

      props.setSampleData?.({
        ...sampleData,
        sampleItems: sampleItemsWithAliquoting,
      });
      setLoading(false);
    } else {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "sample.search.nosample" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      setLoading(false);
    }
  };

  const handleSubmit = (values) => {
    querySearch(values);
  };

  useEffect(() => {
    let accessionNumber = new URLSearchParams(window.location.search).get(
      "accessionNumber",
    );
    if (accessionNumber) {
      let searchValues = {
        ...searchFormValues,
        accessionNumber: accessionNumber,
      };
      setSearchFormValues(searchValues);
      querySearch(searchValues);
    }
  }, []);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <Formik
        initialValues={searchFormValues}
        onSubmit={handleSubmit}
        enableReinitialize={true}
      >
        {({ values, handleChange, setFieldValue, handleSubmit }) => (
          <Form onSubmit={handleSubmit} onChange={handleChange}>
            <Stack gap={2}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <h4>
                    <FormattedMessage id="sample.search.title" />
                  </h4>
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <Field name="accessionNumber">
                    {({ field }) => (
                      <CustomLabNumberInput
                        placeholder="Enter Accession No."
                        name={field.name}
                        id={field.name}
                        data-cy="enterAccession"
                        value={values[field.name]}
                        labelText={
                          <FormattedMessage id="search.label.accession" />
                        }
                        onChange={(e, rawValue) => {
                          setFieldValue(field.name, rawValue);
                        }}
                      />
                    )}
                  </Field>
                </Column>
                <Column lg={2}>
                  <Button
                    style={{ marginTop: "16px" }}
                    type="submit"
                    id="searchSample"
                  >
                    <FormattedMessage id="label.button.search" />
                  </Button>
                </Column>
                <Column lg={8} />
              </Grid>
            </Stack>
          </Form>
        )}
      </Formik>
    </>
  );
}

export function SampleItemsDisplay(props) {
  const { configurationProperties } = useContext(ConfigurationContext);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const [expandedRows, setExpandedRows] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  // Track selected values for each dropdown to prevent auto-selection
  const [selectedAliquotValues, setSelectedAliquotValues] = useState({});

  const toggleRowExpansion = (rowId) => {
    setExpandedRows((prev) => ({
      ...prev,
      [rowId]: !prev[rowId],
    }));
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return "";
    const date = new Date(timestamp);
    return date.toLocaleDateString();
  };

  const getUomDisplay = (sampleItem) => {
    // Check both uom and unitOfMeasure for backward compatibility
    if (sampleItem.uom) {
      return (
        sampleItem.uom.unitOfMeasureName ||
        sampleItem.uom.name ||
        sampleItem.uom.description
      );
    }
    if (sampleItem.unitOfMeasure) {
      return (
        sampleItem.unitOfMeasure.unitOfMeasureName ||
        sampleItem.unitOfMeasure.name ||
        sampleItem.unitOfMeasure.description
      );
    }
    return "";
  };

  const getExternalId = (sampleItem) => {
    return sampleItem.externalId || "";
  };

  const calculateRemainingQuantity = (sampleItem) => {
    const totalAliquotQuantity = sampleItem.aliquots.reduce(
      (total, aliquot) => {
        return total + (parseFloat(aliquot.quantity) || 0);
      },
      0,
    );

    const remaining =
      (parseFloat(sampleItem.quantity) || 0) - totalAliquotQuantity;
    // Prevent negative remaining quantity
    return Math.max(0, remaining);
  };

  const isQuantityBalanced = (sampleItem) => {
    const totalAliquotQuantity = sampleItem.aliquots.reduce(
      (total, aliquot) => {
        return total + (parseFloat(aliquot.quantity) || 0);
      },
      0,
    );

    const originalQuantity = parseFloat(sampleItem.quantity) || 0;
    return Math.abs(totalAliquotQuantity - originalQuantity) < 0.01; // Allow for floating point precision
  };

  const areAllTestsAssigned = (sampleItem) => {
    if (!sampleItem.analysis || sampleItem.analysis.length === 0) {
      return true; // No tests to assign
    }

    const totalAnalysesInAliquots = sampleItem.aliquots.reduce(
      (total, aliquot) => {
        return total + (aliquot.analyses?.length || 0);
      },
      0,
    );

    return totalAnalysesInAliquots === sampleItem.analysis.length;
  };

  const toggleAliquoting = (sampleItemIndex) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    updatedSampleItems[sampleItemIndex].showAliquoting =
      !updatedSampleItems[sampleItemIndex].showAliquoting;

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const addAliquot = (sampleItemIndex) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    const sampleItem = updatedSampleItems[sampleItemIndex];
    const externalId = getExternalId(sampleItem);

    // Generate aliquot ID in format: external_id.{number}
    const aliquotNumber = sampleItem.aliquots.length + 1;
    const aliquotId = externalId
      ? `${externalId}.${aliquotNumber}`
      : `aliquot-${Date.now()}`;

    const newAliquot = {
      id: aliquotId,
      externalId: aliquotId,
      quantity: 0,
      analyses: [],
    };

    sampleItem.aliquots = [...sampleItem.aliquots, newAliquot];

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const removeAliquot = (sampleItemIndex, aliquotIndex) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    const sampleItem = updatedSampleItems[sampleItemIndex];
    const aliquots = [...sampleItem.aliquots];

    // Move any analyses back to parent sample item before removing aliquot
    const aliquotToRemove = aliquots[aliquotIndex];
    if (aliquotToRemove.analyses && aliquotToRemove.analyses.length > 0) {
      sampleItem.analysis = [
        ...sampleItem.analysis,
        ...aliquotToRemove.analyses,
      ];
    }

    aliquots.splice(aliquotIndex, 1);
    sampleItem.aliquots = aliquots;

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const updateAliquotQuantity = (sampleItemIndex, aliquotIndex, quantity) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    const sampleItem = updatedSampleItems[sampleItemIndex];
    const aliquot = sampleItem.aliquots[aliquotIndex];

    const newQuantity = Math.max(0, parseFloat(quantity) || 0); // Prevent negative quantities

    // Calculate current total excluding this aliquot
    const currentTotalExcludingThis = sampleItem.aliquots.reduce(
      (total, alt, idx) => {
        if (idx !== aliquotIndex) {
          return total + (parseFloat(alt.quantity) || 0);
        }
        return total;
      },
      0,
    );

    // Calculate maximum allowed for this aliquot
    const maxAllowed =
      (parseFloat(sampleItem.quantity) || 0) - currentTotalExcludingThis;

    // Ensure aliquot quantity doesn't exceed available quantity
    const finalQuantity = Math.min(newQuantity, maxAllowed);

    aliquot.quantity = finalQuantity;

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const moveAnalysisToAliquot = (
    sampleItemIndex,
    analysisIndex,
    aliquotIndex,
  ) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    const sampleItem = updatedSampleItems[sampleItemIndex];

    // Ensure analyses array exists
    if (!sampleItem.analysis || !sampleItem.analysis[analysisIndex]) {
      return;
    }

    const analysis = sampleItem.analysis[analysisIndex];

    // Remove from parent analyses
    sampleItem.analysis.splice(analysisIndex, 1);

    // Ensure aliquot analyses array exists
    if (!sampleItem.aliquots[aliquotIndex].analyses) {
      sampleItem.aliquots[aliquotIndex].analyses = [];
    }

    // Add to aliquot analyses
    sampleItem.aliquots[aliquotIndex].analyses.push(analysis);

    // Reset the selected value for this dropdown
    const dropdownKey = `${sampleItemIndex}-${analysisIndex}`;
    setSelectedAliquotValues((prev) => ({
      ...prev,
      [dropdownKey]: "",
    }));

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const moveAnalysisFromAliquot = (
    sampleItemIndex,
    aliquotIndex,
    analysisIndex,
  ) => {
    const updatedSampleItems = [...props.sampleData.sampleItems];
    const sampleItem = updatedSampleItems[sampleItemIndex];
    const aliquot = sampleItem.aliquots[aliquotIndex];

    if (!aliquot.analyses || !aliquot.analyses[analysisIndex]) {
      return;
    }

    const analysis = aliquot.analyses[analysisIndex];

    // Remove from aliquot analyses
    aliquot.analyses.splice(analysisIndex, 1);

    // Ensure parent analyses array exists
    if (!sampleItem.analysis) {
      sampleItem.analysis = [];
    }

    // Add back to parent analyses
    sampleItem.analysis.push(analysis);

    props.setSampleData({
      ...props.sampleData,
      sampleItems: updatedSampleItems,
    });
  };

  const handleAliquotSelectChange = (sampleItemIndex, analysisIndex, value) => {
    const dropdownKey = `${sampleItemIndex}-${analysisIndex}`;
    setSelectedAliquotValues((prev) => ({
      ...prev,
      [dropdownKey]: value,
    }));

    if (value !== "") {
      moveAnalysisToAliquot(sampleItemIndex, analysisIndex, parseInt(value));
    }
  };

  const handleSaveAliquoting = async () => {
    if (isSubmitting) return;

    setIsSubmitting(true);

    try {
      // Validate all sample items before saving
      const validationErrors = [];

      props.sampleData.sampleItems.forEach((sampleItem, index) => {
        if (sampleItem.aliquots.length > 0) {
          // Check if quantity is balanced
          if (!isQuantityBalanced(sampleItem)) {
            validationErrors.push(
              intl.formatMessage(
                { id: "aliquot.quantity.not.balanced" },
                { sampleType: sampleItem.typeOfSample },
              ),
            );
          }

          // Check if all tests are assigned
          if (!areAllTestsAssigned(sampleItem)) {
            validationErrors.push(
              intl.formatMessage(
                { id: "aliquot.tests.not.assigned" },
                { sampleType: sampleItem.typeOfSample },
              ),
            );
          }
        }
      });

      if (validationErrors.length > 0) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: (
            <div>
              <FormattedMessage id="aliquot.validation.errors" />
              <ul>
                {validationErrors.map((error, index) => (
                  <li key={index}>{error}</li>
                ))}
              </ul>
            </div>
          ),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        setIsSubmitting(false);
        return;
      }

      const submissionData = {
        accessionNumber: props.sampleData.accessionNumber,
        sampleItems:
          props.sampleData.sampleItems
            ?.filter((item) =>
              item.aliquots?.some((aliquot) => aliquot?.analyses?.length > 0),
            )
            .map((item) => ({
              externalId: item.externalId,
              aliquots:
                item.aliquots
                  ?.filter((aliquot) => aliquot?.analyses?.length > 0)
                  .map((aliquot) => ({
                    externalId: aliquot.externalId,
                    quantity: aliquot.quantity,
                    analyses:
                      aliquot.analyses?.map((analysis) => analysis.id) ?? [],
                  })) ?? [],
            })) ?? [],
      };

      postToOpenElisServer(
        "/rest/Aliquot",
        JSON.stringify(submissionData),
        handlePost,
      );
    } catch (error) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "aliquot.save.error" }),
        kind: NotificationKinds.error,
      });
    } finally {
      setIsSubmitting(false);
      setNotificationVisible(true);
    }
  };

  const handlePost = (status) => {
    setIsSubmitting(false);

    if (status === 200) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "aliquot.save.success" }),
        kind: NotificationKinds.success,
      });
      window.location.reload();
    } else {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "aliquot.save.error" }),
        kind: NotificationKinds.error,
      });
    }
  };

  const headers = [
    {
      key: "sampleItemInfo",
      header: intl.formatMessage({ id: "sample.item.info" }),
    },
    { key: "externalId", header: intl.formatMessage({ id: "external.id" }) },
    { key: "typeOfSample", header: intl.formatMessage({ id: "sample.type" }) },
    {
      key: "collectionDate",
      header: intl.formatMessage({ id: "collection.date" }),
    },
    { key: "collector", header: intl.formatMessage({ id: "collector" }) },
    { key: "quantity", header: intl.formatMessage({ id: "quantity" }) },
    {
      key: "analysisCount",
      header: intl.formatMessage({ id: "analysis.count" }),
    },
    { key: "aliquoting", header: intl.formatMessage({ id: "aliquoting" }) },
  ];

  const sampleItems = props.sampleData?.sampleItems || [];

  return (
    <>
      {sampleItems.length > 0 && (
        <div style={{ marginTop: "2rem" }}>
          <Grid>
            <Column lg={16}>
              <h4>
                <FormattedMessage
                  id="sample.items.for.accession"
                  values={{ accessionNumber: props.sampleData.accessionNumber }}
                />
              </h4>
            </Column>
          </Grid>

          <Table>
            <TableHead>
              <TableRow>
                <TableExpandHeader />
                {headers.map((header) => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {sampleItems.map((sampleItem, index) => {
                const remainingQuantity =
                  calculateRemainingQuantity(sampleItem);
                const uomDisplay = getUomDisplay(sampleItem);
                const externalId = getExternalId(sampleItem);
                const quantityBalanced = isQuantityBalanced(sampleItem);
                const allTestsAssigned = areAllTestsAssigned(sampleItem);

                return (
                  <React.Fragment key={index}>
                    <TableExpandRow
                      onClick={() => toggleRowExpansion(index)}
                      isExpanded={expandedRows[index]}
                    >
                      <TableCell>
                        <div className="sampleInfo">
                          <Button
                            onClick={async () => {
                              if ("clipboard" in navigator) {
                                await navigator.clipboard.writeText(
                                  props.sampleData.accessionNumber,
                                );
                              }
                            }}
                            kind="ghost"
                            iconDescription={intl.formatMessage({
                              id: "instructions.copy.labnum",
                            })}
                            hasIconOnly
                            renderIcon={Copy}
                          />
                          <br />
                          {props.sampleData.accessionNumber}
                        </div>
                      </TableCell>
                      <TableCell>
                        {externalId && (
                          <div>
                            <Tag type="blue" size="sm">
                              {externalId}
                            </Tag>
                          </div>
                        )}
                      </TableCell>
                      <TableCell>{sampleItem.typeOfSample}</TableCell>
                      <TableCell>
                        {formatDate(sampleItem.collectionDate)}
                      </TableCell>
                      <TableCell>
                        {sampleItem.collector ||
                          intl.formatMessage({ id: "not.specified" })}
                      </TableCell>
                      <TableCell>
                        <div>
                          <div>
                            <strong>
                              {sampleItem.quantity} {uomDisplay}
                            </strong>
                          </div>
                          {sampleItem.aliquots.length > 0 && (
                            <div
                              style={{ fontSize: "0.875rem", color: "#6f6f6f" }}
                            >
                              <FormattedMessage
                                id="remaining.quantity"
                                values={{
                                  quantity: remainingQuantity.toFixed(2),
                                  uom: uomDisplay,
                                }}
                              />
                              {!quantityBalanced && (
                                <Tag
                                  type="red"
                                  size="sm"
                                  style={{ marginLeft: "0.5rem" }}
                                >
                                  <Warning size={16} />
                                  <FormattedMessage id="quantity.not.balanced" />
                                </Tag>
                              )}
                              {quantityBalanced && (
                                <Tag
                                  type="green"
                                  size="sm"
                                  style={{ marginLeft: "0.5rem" }}
                                >
                                  <Checkmark size={16} />
                                  <FormattedMessage id="quantity.balanced" />
                                </Tag>
                              )}
                            </div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div>
                          <div>{sampleItem.analysis?.length || 0}</div>
                          {sampleItem.aliquots.length > 0 && (
                            <div style={{ fontSize: "0.875rem" }}>
                              {!allTestsAssigned && (
                                <Tag type="red" size="sm">
                                  <Warning size={16} />
                                  <FormattedMessage id="tests.not.assigned" />
                                </Tag>
                              )}
                              {allTestsAssigned && (
                                <Tag type="green" size="sm">
                                  <Checkmark size={16} />
                                  <FormattedMessage id="all.tests.assigned" />
                                </Tag>
                              )}
                            </div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleAliquoting(index);
                          }}
                          renderIcon={
                            sampleItem.showAliquoting ? Subtract : Add
                          }
                        >
                          {sampleItem.showAliquoting
                            ? intl.formatMessage({ id: "hide.aliquoting" })
                            : intl.formatMessage({ id: "show.aliquoting" })}
                        </Button>
                      </TableCell>
                    </TableExpandRow>
                    {expandedRows[index] && (
                      <TableExpandedRow colSpan={headers.length + 1}>
                        <div style={{ padding: "1rem" }}>
                          {/* Sample Item Details - Simplified */}
                          <Grid style={{ marginBottom: "1rem" }}>
                            <Column lg={12}>
                              <strong>
                                <FormattedMessage id="external.id" />:
                              </strong>{" "}
                              {externalId ||
                                intl.formatMessage({ id: "not.available" })}
                            </Column>
                          </Grid>

                          {/* Analysis Details */}
                          <h5>
                            <FormattedMessage id="analysis.details" />
                          </h5>
                          {sampleItem.analysis &&
                          sampleItem.analysis.length > 0 ? (
                            <Table>
                              <TableHead>
                                <TableRow>
                                  <TableHeader>
                                    <FormattedMessage id="test.name" />
                                  </TableHeader>
                                  <TableHeader>
                                    <FormattedMessage id="test.section" />
                                  </TableHeader>
                                  <TableHeader>
                                    <FormattedMessage id="started.date" />
                                  </TableHeader>
                                  <TableHeader>
                                    <FormattedMessage id="status" />
                                  </TableHeader>
                                  <TableHeader>
                                    <FormattedMessage id="move.to.aliquot" />
                                  </TableHeader>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {sampleItem.analysis.map(
                                  (analysis, analysisIndex) => {
                                    const dropdownKey = `${index}-${analysisIndex}`;
                                    const selectedValue =
                                      selectedAliquotValues[dropdownKey] || "";

                                    return (
                                      <TableRow key={analysisIndex}>
                                        <TableCell>
                                          {analysis.test?.localizedTestName
                                            ?.localizedValue ||
                                            analysis.test?.name ||
                                            intl.formatMessage({
                                              id: "unknown.test",
                                            })}
                                        </TableCell>
                                        <TableCell>
                                          {analysis.testSection?.localization
                                            ?.localizedValue ||
                                            analysis.testSection
                                              ?.testSectionName ||
                                            intl.formatMessage({
                                              id: "unknown.section",
                                            })}
                                        </TableCell>
                                        <TableCell>
                                          {analysis.startedDateForDisplay ||
                                            formatDate(analysis.startedDate)}
                                        </TableCell>
                                        <TableCell>
                                          {analysis.statusId === "4"
                                            ? intl.formatMessage({
                                                id: "status.in.progress",
                                              })
                                            : intl.formatMessage({
                                                id: "status.unknown",
                                              })}
                                        </TableCell>
                                        <TableCell>
                                          {sampleItem.aliquots.length > 0 ? (
                                            <Select
                                              size="sm"
                                              labelText=""
                                              value={selectedValue}
                                              onChange={(e) =>
                                                handleAliquotSelectChange(
                                                  index,
                                                  analysisIndex,
                                                  e.target.value,
                                                )
                                              }
                                            >
                                              <SelectItem
                                                text={intl.formatMessage({
                                                  id: "select.aliquot",
                                                })}
                                                value=""
                                              />
                                              {sampleItem.aliquots.map(
                                                (aliquot, aliquotIndex) => (
                                                  <SelectItem
                                                    key={aliquotIndex}
                                                    text={`${aliquot.externalId || intl.formatMessage({ id: "aliquot" })} ${aliquotIndex + 1} (${aliquot.quantity} ${uomDisplay})`}
                                                    value={aliquotIndex.toString()}
                                                  />
                                                ),
                                              )}
                                            </Select>
                                          ) : (
                                            intl.formatMessage({
                                              id: "no.aliquots",
                                            })
                                          )}
                                        </TableCell>
                                      </TableRow>
                                    );
                                  },
                                )}
                              </TableBody>
                            </Table>
                          ) : (
                            <p>
                              <FormattedMessage id="no.analysis.found" />
                            </p>
                          )}

                          {/* Aliquoting Section */}
                          {sampleItem.showAliquoting && (
                            <div
                              style={{
                                marginTop: "2rem",
                                borderTop: "1px solid #e0e0e0",
                                paddingTop: "1rem",
                              }}
                            >
                              <h5>
                                <FormattedMessage id="aliquoting.section" />
                              </h5>

                              {/* Validation Status */}
                              <div
                                style={{
                                  backgroundColor:
                                    quantityBalanced && allTestsAssigned
                                      ? "#e0f7e9"
                                      : "#fff4e6",
                                  padding: "1rem",
                                  borderRadius: "4px",
                                  marginBottom: "1rem",
                                  border: `1px solid ${quantityBalanced && allTestsAssigned ? "#a3e9b6" : "#ffd8a8"}`,
                                }}
                              >
                                <Grid>
                                  <Column lg={6}>
                                    <div
                                      style={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "0.5rem",
                                      }}
                                    >
                                      {quantityBalanced ? (
                                        <Tag type="green">
                                          <Checkmark size={16} />
                                          <FormattedMessage id="quantity.balanced" />
                                        </Tag>
                                      ) : (
                                        <Tag type="red">
                                          <Warning size={16} />
                                          <FormattedMessage id="quantity.not.balanced" />
                                        </Tag>
                                      )}
                                    </div>
                                  </Column>
                                  <Column lg={6}>
                                    <div
                                      style={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "0.5rem",
                                      }}
                                    >
                                      {allTestsAssigned ? (
                                        <Tag type="green">
                                          <Checkmark size={16} />
                                          <FormattedMessage id="all.tests.assigned" />
                                        </Tag>
                                      ) : (
                                        <Tag type="red">
                                          <Warning size={16} />
                                          <FormattedMessage id="tests.not.assigned" />
                                        </Tag>
                                      )}
                                    </div>
                                  </Column>
                                </Grid>
                              </div>

                              {/* Quantity Summary */}
                              <div
                                style={{
                                  backgroundColor: "#f0f0f0",
                                  padding: "1rem",
                                  borderRadius: "4px",
                                  marginBottom: "1rem",
                                }}
                              >
                                <Grid>
                                  <Column lg={4}>
                                    <strong>
                                      <FormattedMessage id="total.quantity" />:
                                    </strong>{" "}
                                    {sampleItem.quantity} {uomDisplay}
                                  </Column>
                                  <Column lg={4}>
                                    <strong>
                                      <FormattedMessage id="allocated.to.aliquots" />
                                      :
                                    </strong>{" "}
                                    {(
                                      sampleItem.quantity - remainingQuantity
                                    ).toFixed(2)}{" "}
                                    {uomDisplay}
                                  </Column>
                                  <Column lg={4}>
                                    <strong>
                                      <FormattedMessage id="remaining.quantity" />
                                      :
                                    </strong>{" "}
                                    {remainingQuantity.toFixed(2)} {uomDisplay}
                                  </Column>
                                </Grid>
                              </div>

                              {/* Add Aliquot Button */}
                              <Button
                                onClick={() => addAliquot(index)}
                                renderIcon={Add}
                                style={{ marginBottom: "1rem" }}
                              >
                                <FormattedMessage id="add.aliquot" />
                              </Button>

                              {/* Aliquots Display */}
                              {sampleItem.aliquots.map(
                                (aliquot, aliquotIndex) => (
                                  <div
                                    key={aliquot.id}
                                    style={{
                                      border: "1px solid #d1d1d1",
                                      borderRadius: "4px",
                                      padding: "1rem",
                                      marginBottom: "1rem",
                                      backgroundColor: "#f8f8f8",
                                    }}
                                  >
                                    <Grid>
                                      <Column lg={4}>
                                        <div style={{ marginBottom: "0.5rem" }}>
                                          <strong>
                                            <FormattedMessage id="aliquot.id" />
                                            :
                                          </strong>{" "}
                                          {aliquot.externalId}
                                        </div>
                                        <NumberInput
                                          label={intl.formatMessage({
                                            id: "aliquot.quantity",
                                          })}
                                          value={aliquot.quantity}
                                          min={0}
                                          max={sampleItem.quantity}
                                          onChange={(e, { value }) =>
                                            updateAliquotQuantity(
                                              index,
                                              aliquotIndex,
                                              value,
                                            )
                                          }
                                          helperText={`${uomDisplay}`}
                                        />
                                      </Column>
                                      <Column lg={6}>
                                        <h6>
                                          <FormattedMessage
                                            id="aliquot.analyses.count"
                                            values={{
                                              count:
                                                aliquot.analyses?.length || 0,
                                            }}
                                          />
                                        </h6>
                                        {aliquot.analyses &&
                                          aliquot.analyses.length > 0 && (
                                            <ul
                                              style={{
                                                margin: 0,
                                                paddingLeft: "1rem",
                                              }}
                                            >
                                              {aliquot.analyses.map(
                                                (analysis, analysisIndex) => (
                                                  <li
                                                    key={analysisIndex}
                                                    style={{
                                                      display: "flex",
                                                      justifyContent:
                                                        "space-between",
                                                      alignItems: "center",
                                                      marginBottom: "0.5rem",
                                                    }}
                                                  >
                                                    <div>
                                                      {analysis.test
                                                        ?.localizedTestName
                                                        ?.localizedValue ||
                                                        analysis.test?.name}
                                                    </div>
                                                    <Button
                                                      kind="ghost"
                                                      size="sm"
                                                      renderIcon={Move}
                                                      onClick={() =>
                                                        moveAnalysisFromAliquot(
                                                          index,
                                                          aliquotIndex,
                                                          analysisIndex,
                                                        )
                                                      }
                                                    >
                                                      <FormattedMessage id="move.back" />
                                                    </Button>
                                                  </li>
                                                ),
                                              )}
                                            </ul>
                                          )}
                                      </Column>
                                      <Column lg={2}>
                                        <Button
                                          kind="danger"
                                          size="sm"
                                          onClick={() =>
                                            removeAliquot(index, aliquotIndex)
                                          }
                                        >
                                          <FormattedMessage id="remove.aliquot" />
                                        </Button>
                                      </Column>
                                    </Grid>
                                  </div>
                                ),
                              )}
                            </div>
                          )}
                        </div>
                      </TableExpandedRow>
                    )}
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>

          {/* Save Button */}
          {sampleItems.some((item) => item.aliquots.length > 0) && (
            <div style={{ marginTop: "2rem", textAlign: "center" }}>
              <Button
                type="button"
                onClick={handleSaveAliquoting}
                disabled={isSubmitting}
                renderIcon={isSubmitting ? InlineLoading : null}
              >
                {isSubmitting ? (
                  <FormattedMessage id="saving" />
                ) : (
                  <FormattedMessage id="save.aliquot.changes" />
                )}
              </Button>
            </div>
          )}
        </div>
      )}
    </>
  );
}

export default injectIntl(AliquotPage);
