import { Checkmark, Document, Scan, TrashCan } from "@carbon/icons-react";
import {
  Button,
  Checkbox,
  Column,
  DataTable,
  Dropdown,
  Grid,
  InlineNotification,
  Loading,
  Modal,
  NumberInput,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextArea,
  TextInput,
  Tile,
  Toggle,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { AlertDialog } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServer,
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../utils/Utils";
import "./BoxCreation.css";
import ShipmentNavigation from "./ShipmentNavigation";

const BoxCreation = () => {
  const intl = useIntl();
  const history = useHistory();
  const { addNotification } = useContext(NotificationContext);

  // Auto-generated box number
  const [boxNumber, setBoxNumber] = useState("");
  const [boxLabelPrefix, setBoxLabelPrefix] = useState("BOX");
  const [loading, setLoading] = useState(false);
  const [facilities, setFacilities] = useState([]);

  // Form state
  const [selectedFacility, setSelectedFacility] = useState(null);
  const [selectedTemperature, setSelectedTemperature] = useState(null);
  const [capacity, setCapacity] = useState(25);
  const [notes, setNotes] = useState("");

  // Sample management
  const [sampleSearchTerm, setSampleSearchTerm] = useState("");
  const [addedSamples, setAddedSamples] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [selectedResultIds, setSelectedResultIds] = useState([]);
  const [searching, setSearching] = useState(false);

  // Reject sample modal
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [sampleToReject, setSampleToReject] = useState(null);
  const [rejectionReason, setRejectionReason] = useState(null);

  // Validation messages
  const [validationMessages, setValidationMessages] = useState([]);

  // Scan existing box ID mode
  const [useExistingBoxId, setUseExistingBoxId] = useState(false);
  const [manualBoxId, setManualBoxId] = useState("");

  // Temperature options
  const temperatureOptions = [
    {
      id: "AMBIENT",
      label: intl.formatMessage({ id: "shipment.temperature.ambient" }),
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({ id: "shipment.temperature.refrigerated" }),
    },
    {
      id: "FROZEN",
      label: intl.formatMessage({ id: "shipment.temperature.frozen" }),
    },
    {
      id: "DEEP_FROZEN",
      label: intl.formatMessage({ id: "shipment.temperature.deepFrozen" }),
    },
    {
      id: "DRY_ICE",
      label: intl.formatMessage({ id: "shipment.temperature.dryIce" }),
    },
  ];

  // Rejection reasons - loaded from backend
  const [rejectionReasons, setRejectionReasons] = useState([]);

  useEffect(() => {
    fetchFacilities();
    fetchRejectionReasons();
    fetchBoxLabelPrefix();
    generateBoxNumber();
  }, []);

  useEffect(() => {
    validateForm();
  }, [selectedFacility, addedSamples]);

  // Handle capacity changes - remove excess samples
  useEffect(() => {
    if (addedSamples.length > capacity) {
      const samplesToKeep = addedSamples.slice(0, capacity);
      setAddedSamples(samplesToKeep);
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage(
          { id: "shipment.notification.samplesRemovedDueToCapacity" },
          { removed: addedSamples.length - capacity },
        ),
      });
    }
  }, [capacity]);

  const fetchBoxLabelPrefix = () => {
    getFromOpenElisServer("/rest/shipping-box/box-label-prefix", (response) => {
      if (response) {
        setBoxLabelPrefix(response);
      }
    });
  };

  const generateBoxNumber = () => {
    getFromOpenElisServer(
      "/rest/shipping-box/generate-box-number",
      (response) => {
        if (response) {
          setBoxNumber(response);
        }
      },
    );
  };

  const fetchFacilities = () => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (response) {
          setFacilities(response);
        }
      },
    );
  };

  const fetchRejectionReasons = () => {
    getFromOpenElisServer("/rest/displayList/REFERRAL_REASONS", (response) => {
      if (response) {
        setRejectionReasons(response);
      }
    });
  };

  const validateForm = () => {
    const messages = [];

    if (!selectedFacility) {
      messages.push(
        intl.formatMessage({ id: "shipment.validation.selectDestination" }),
      );
    }

    if (addedSamples.length === 0) {
      messages.push(
        intl.formatMessage({ id: "shipment.validation.addSample" }),
      );
    }

    setValidationMessages(messages);
    return messages.length === 0;
  };

  // Map a SampleItemDTO to the shape stored in addedSamples
  const buildSampleToAdd = (item) => ({
    id: item.sampleItemId,
    sampleItemId: item.sampleItemId,
    accessionNumber: item.accessionNumber,
    sampleType: item.typeOfSample || "-",
    tests: item.referralTests ? item.referralTests.map((t) => t.testName) : [],
  });

  // Add a single resolved sample item, with dedup + capacity checks
  const addResolvedSample = (item) => {
    const sampleToAdd = buildSampleToAdd(item);

    if (
      addedSamples.some(
        (s) => s.accessionNumber === sampleToAdd.accessionNumber,
      )
    ) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage({
          id: "shipment.error.sampleAlreadyInBox",
        }),
      });
      return;
    }

    if (capacity - addedSamples.length <= 0) {
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.boxFull" }),
      });
      return;
    }

    setAddedSamples([...addedSamples, sampleToAdd]);
    setSearchResults([]);
    setSelectedResultIds([]);
    setSampleSearchTerm("");

    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage(
        { id: "shipment.notification.sampleAdded" },
        {
          accessionNumber: sampleToAdd.accessionNumber,
          testCount: sampleToAdd.tests.length,
        },
      ),
    });
  };

  const handleSearchSample = async () => {
    if (!sampleSearchTerm.trim()) {
      return;
    }

    // Check if box is full
    if (addedSamples.length >= capacity) {
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.boxFull" }),
      });
      return;
    }

    // Clear any chooser from a previous search
    setSearchResults([]);
    setSelectedResultIds([]);

    // A scanned/pasted sample item ID may carry a "-<sortOrder>" suffix (e.g.
    // DEV...005-6). The accession number stored on the server has no suffix, so
    // search by the base accession and resolve the specific item from results.
    const term = sampleSearchTerm.trim();
    const suffixMatch = term.match(/^(.*)-(\d+)$/);
    const baseAccession = suffixMatch ? suffixMatch[1] : term;

    setSearching(true);

    try {
      // Search for sample items by accession number using new SampleItem-based API
      const data = await getFromOpenElisServerV2(
        `/rest/unassigned-sample/items/search?accessionNumber=${encodeURIComponent(baseAccession)}`,
      );

      setSearching(false);

      // Backend returns SampleItemDTO array
      if (!data || !Array.isArray(data) || data.length === 0) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({ id: "shipment.error.sampleNotFound" }),
        });
        return;
      }

      // Full ID with suffix: resolve to the exact matching item
      if (suffixMatch) {
        const exact = data.find((d) => d.accessionNumber === term);
        if (exact) {
          addResolvedSample(exact);
          return;
        }
        // Suffix didn't match a display accession; add directly only if
        // unambiguous, otherwise fall through to the chooser
        if (data.length === 1) {
          addResolvedSample(data[0]);
          return;
        }
      } else if (data.length === 1) {
        addResolvedSample(data[0]);
        return;
      }

      // Multiple matches — let the user pick which item(s) to add
      setSearchResults(data);
    } catch (error) {
      console.error("Error searching sample:", error);
      setSearching(false);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.searchFailed" }),
      });
    }
  };

  const toggleResultSelection = (sampleItemId) => {
    setSelectedResultIds((prev) =>
      prev.includes(sampleItemId)
        ? prev.filter((id) => id !== sampleItemId)
        : [...prev, sampleItemId],
    );
  };

  const handleAddSelected = () => {
    const toAdd = searchResults.filter(
      (item) =>
        selectedResultIds.includes(item.sampleItemId) &&
        !addedSamples.some((s) => s.accessionNumber === item.accessionNumber),
    );

    if (toAdd.length === 0) {
      return;
    }

    if (toAdd.length > capacity - addedSamples.length) {
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.boxFull" }),
      });
      return;
    }

    setAddedSamples([...addedSamples, ...toAdd.map(buildSampleToAdd)]);
    setSearchResults([]);
    setSelectedResultIds([]);
    setSampleSearchTerm("");

    addNotification({
      kind: "success",
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage(
        { id: "shipment.notification.samplesAdded" },
        { count: toAdd.length },
      ),
    });
  };

  const handleRemoveSample = (sample) => {
    setAddedSamples(
      addedSamples.filter((s) => s.accessionNumber !== sample.accessionNumber),
    );
  };

  const handleRemoveTest = (sample, testToRemove) => {
    const updatedSamples = addedSamples.map((s) => {
      if (s.accessionNumber === sample.accessionNumber) {
        const updatedTests = s.tests.filter((test) => test !== testToRemove);
        // If no tests left, remove the entire sample
        if (updatedTests.length === 0) {
          return null;
        }
        return { ...s, tests: updatedTests };
      }
      return s;
    });

    // Filter out null entries (samples with no tests)
    setAddedSamples(updatedSamples.filter((s) => s !== null));
  };

  const handleRejectSample = (sample) => {
    setSampleToReject(sample);
    setShowRejectModal(true);
  };

  const confirmRejectSample = () => {
    if (sampleToReject && rejectionReason) {
      handleRemoveSample(sampleToReject);

      addNotification({
        kind: "info",
        title: intl.formatMessage({ id: "notification.info" }),
        message: intl.formatMessage(
          { id: "shipment.notification.sampleRejected" },
          {
            accession: sampleToReject.accessionNumber,
            reason: rejectionReason.value || rejectionReason.label,
          },
        ),
      });

      setShowRejectModal(false);
      setSampleToReject(null);
      setRejectionReason(null);
    }
  };

  const handleSaveBox = () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);

    const boxData = {
      boxId:
        useExistingBoxId && manualBoxId.trim() ? manualBoxId.trim() : boxNumber,
      destinationFacilityId: Number.parseInt(selectedFacility.id),
      temperatureRequirement: selectedTemperature?.id || "AMBIENT",
      capacity: capacity,
      actualSampleCount: addedSamples.length,
      notes: notes,
      state: "DRAFT",
    };

    postToOpenElisServerJsonResponse(
      "/rest/shipping-box",
      JSON.stringify(boxData),
      (response) => {
        if (response.error || response.status >= 400) {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({ id: "shipment.error.createBox" }),
          });
          setLoading(false);
          return;
        }

        if (response && response.id) {
          // Add samples sequentially using Promise wrappers
          const addSamplesAndNavigate = async () => {
            let errors = 0;
            for (const sample of addedSamples) {
              const result = await new Promise((resolve) => {
                postToOpenElisServerJsonResponse(
                  "/rest/box-sample/items",
                  JSON.stringify({
                    shippingBoxId: response.id,
                    sampleItemId: sample.sampleItemId || sample.id,
                  }),
                  (res) => {
                    if (res.error || res.status >= 400) {
                      console.error("Error adding sample to box:", res);
                      resolve(false);
                    } else {
                      resolve(true);
                    }
                  },
                );
              });
              if (!result) {
                errors++;
              }
            }

            addNotification({
              kind: errors > 0 ? "warning" : "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage({
                id: "shipment.notification.boxCreated",
              }),
            });
            setLoading(false);
            history.push(`/SampleShipment/box/${response.id}`);
          };

          addSamplesAndNavigate();
        } else {
          setLoading(false);
        }
      },
    );
  };

  const handleSaveAndMarkReady = () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);

    const boxData = {
      boxId:
        useExistingBoxId && manualBoxId.trim() ? manualBoxId.trim() : boxNumber,
      destinationFacilityId: Number.parseInt(selectedFacility.id),
      temperatureRequirement: selectedTemperature?.id || "AMBIENT",
      capacity: capacity,
      actualSampleCount: addedSamples.length,
      notes: notes,
      state: "DRAFT",
    };

    postToOpenElisServerJsonResponse(
      "/rest/shipping-box",
      JSON.stringify(boxData),
      (response) => {
        if (response.error || response.status >= 400) {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({ id: "shipment.error.createBox" }),
          });
          setLoading(false);
          return;
        }

        if (response && response.id) {
          const addSamplesAndMarkReady = async () => {
            // Add all samples in parallel using Promise.all
            if (addedSamples.length > 0) {
              await Promise.all(
                addedSamples.map(
                  (sample) =>
                    new Promise((resolve) => {
                      postToOpenElisServerJsonResponse(
                        "/rest/box-sample/items",
                        JSON.stringify({
                          shippingBoxId: response.id,
                          sampleItemId: sample.sampleItemId || sample.id,
                        }),
                        () => {
                          resolve();
                        },
                      );
                    }),
                ),
              );
            }

            // Mark as ready after all samples added
            putToOpenElisServer(
              `/rest/shipping-box/${response.id}/state?newState=READY_TO_SEND`,
              null,
              () => {
                addNotification({
                  kind: "success",
                  title: intl.formatMessage({ id: "notification.success" }),
                  message: intl.formatMessage({
                    id: "shipment.notification.boxReadyToSend",
                  }),
                });
                setLoading(false);
                history.push(`/SampleShipment/box/${response.id}`);
              },
            );
          };

          addSamplesAndMarkReady();
        } else {
          setLoading(false);
        }
      },
    );
  };

  const handleCancel = () => {
    history.push("/SampleShipment/boxes");
  };

  // Calculate remaining capacity
  const remainingCapacity = capacity - addedSamples.length;
  const isFormValid = validationMessages.length === 0;

  // Sample table headers
  const sampleHeaders = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({ id: "shipment.sample.type" }),
    },
    {
      key: "referralTest",
      header: intl.formatMessage({ id: "shipment.label.referralTest" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  const renderSampleRows = () => {
    return addedSamples.map((sample, index) => ({
      id: `${sample.id}-${sample.accessionNumber}-${index}`,
      accessionNumber: sample.accessionNumber,
      sampleType: sample.sampleType,
      referralTest: (
        <div className="test-tags">
          {sample.tests.map((test, testIndex) => (
            <Tag
              key={`${sample.accessionNumber}-test-${testIndex}`}
              type="blue"
              filter
              onClose={() => handleRemoveTest(sample, test)}
            >
              {test}
            </Tag>
          ))}
        </div>
      ),
      actions: (
        <div className="sample-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={TrashCan}
            iconDescription={intl.formatMessage({
              id: "shipment.sample.remove",
            })}
            onClick={() => handleRemoveSample(sample)}
            hasIconOnly
          />
          <Button
            kind="danger--ghost"
            size="sm"
            onClick={() => handleRejectSample(sample)}
          >
            <FormattedMessage id="shipment.action.rejectSample" />
          </Button>
        </div>
      ),
    }));
  };

  if (loading) {
    return (
      <div className="loading-container">
        <Loading />
      </div>
    );
  }

  return (
    <div className="box-creation">
      <AlertDialog />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          { label: "shipment.box.create", link: "/SampleShipment/create-box" },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2 className="page-title">
            <FormattedMessage id="shipment.box.create" />
          </h2>
          <p className="page-description">
            <FormattedMessage id="shipment.box.createDescription" />
          </p>
        </Column>
      </Grid>

      <Grid fullWidth className="box-creation-content">
        {/* Left Column - Form */}
        <Column lg={10} md={5} sm={4}>
          {/* Validation Messages */}
          {validationMessages.length > 0 && (
            <div className="validation-messages">
              {validationMessages.map((message, index) => (
                <InlineNotification
                  key={index}
                  kind="warning"
                  title={intl.formatMessage({ id: "label.warning" })}
                  subtitle={message}
                  lowContrast
                  hideCloseButton
                />
              ))}
            </div>
          )}

          {/* Box Details Form */}
          <div className="form-section">
            <Toggle
              id="useExistingBoxId"
              labelText={intl.formatMessage({
                id: "shipment.box.useExistingId",
              })}
              labelA={intl.formatMessage({
                id: "shipment.box.autoGenerate",
              })}
              labelB={intl.formatMessage({
                id: "shipment.box.scanExisting",
              })}
              toggled={useExistingBoxId}
              onToggle={(checked) => setUseExistingBoxId(checked)}
              size="sm"
            />

            {!useExistingBoxId && (
              <>
                <TextInput
                  id="labelPrefix"
                  labelText={intl.formatMessage({
                    id: "shipment.box.labelPrefix",
                  })}
                  value={boxLabelPrefix}
                  readOnly
                  disabled
                  helperText={intl.formatMessage({
                    id: "shipment.box.labelPrefixHelper",
                  })}
                />

                <TextInput
                  id="boxNumber"
                  labelText={intl.formatMessage({
                    id: "shipment.box.number",
                  })}
                  value={boxNumber}
                  readOnly
                  disabled
                />
              </>
            )}

            {useExistingBoxId && (
              <TextInput
                id="manualBoxId"
                labelText={intl.formatMessage({
                  id: "shipment.box.scanExistingId",
                })}
                placeholder={intl.formatMessage({
                  id: "shipment.box.scanExistingIdPlaceholder",
                })}
                value={manualBoxId}
                onChange={(e) => setManualBoxId(e.target.value)}
              />
            )}

            <NumberInput
              id="capacity"
              label={intl.formatMessage({ id: "shipment.box.capacity" })}
              value={capacity}
              onChange={(e, { value }) => setCapacity(value)}
              min={1}
              max={100}
              step={1}
            />

            <Dropdown
              id="destination"
              titleText={intl.formatMessage({ id: "shipment.box.destination" })}
              label={intl.formatMessage({ id: "label.select" })}
              items={facilities}
              itemToString={(item) => (item ? item.value : "")}
              selectedItem={selectedFacility}
              onChange={({ selectedItem }) => setSelectedFacility(selectedItem)}
            />

            <Dropdown
              id="temperature"
              titleText={intl.formatMessage({ id: "shipment.box.temperature" })}
              label={intl.formatMessage({ id: "label.select" })}
              items={temperatureOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedTemperature}
              onChange={({ selectedItem }) =>
                setSelectedTemperature(selectedItem)
              }
            />

            <TextArea
              id="notes"
              labelText={intl.formatMessage({ id: "shipment.box.notes" })}
              placeholder={intl.formatMessage({
                id: "shipment.box.notesPlaceholder",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>

          {/* Sample Search Section */}
          <div className="sample-search-section">
            <h4>
              <FormattedMessage id="shipment.box.addSamples" />
            </h4>
            <p className="sample-progress">
              <FormattedMessage
                id="shipment.box.sampleProgress"
                values={{
                  added: addedSamples.length,
                  capacity,
                  remaining: remainingCapacity,
                }}
              />
            </p>

            <div className="sample-search-input">
              <TextInput
                id="sampleSearch"
                labelText={intl.formatMessage({ id: "shipment.sample.search" })}
                placeholder={intl.formatMessage({
                  id: "shipment.sample.searchPlaceholder",
                })}
                value={sampleSearchTerm}
                onChange={(e) => setSampleSearchTerm(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearchSample()}
                disabled={searching || addedSamples.length >= capacity}
              />
              <Button
                renderIcon={Scan}
                onClick={handleSearchSample}
                disabled={
                  searching ||
                  !sampleSearchTerm.trim() ||
                  addedSamples.length >= capacity
                }
              >
                <FormattedMessage id="shipment.action.scanSample" />
              </Button>
            </div>

            {searchResults.length > 0 && (
              <div className="sample-search-results">
                <p className="sample-search-results-heading">
                  <FormattedMessage id="shipment.sample.multipleMatches" />
                </p>
                {searchResults.map((item) => {
                  const alreadyAdded = addedSamples.some(
                    (s) => s.accessionNumber === item.accessionNumber,
                  );
                  const tests =
                    item.referralTests && item.referralTests.length > 0
                      ? item.referralTests.map((t) => t.testName).join(", ")
                      : "";
                  return (
                    <Checkbox
                      key={item.sampleItemId}
                      id={`search-result-${item.sampleItemId}`}
                      labelText={
                        `${item.accessionNumber} · ${item.typeOfSample || "-"}` +
                        (tests ? ` · ${tests}` : "")
                      }
                      checked={selectedResultIds.includes(item.sampleItemId)}
                      disabled={alreadyAdded}
                      onChange={() => toggleResultSelection(item.sampleItemId)}
                    />
                  );
                })}
                <div className="sample-search-results-actions">
                  <Button
                    size="sm"
                    onClick={handleAddSelected}
                    disabled={selectedResultIds.length === 0}
                  >
                    <FormattedMessage id="shipment.action.addSelected" />
                  </Button>
                  <Button
                    size="sm"
                    kind="ghost"
                    onClick={() => {
                      setSearchResults([]);
                      setSelectedResultIds([]);
                    }}
                  >
                    <FormattedMessage id="label.cancel" />
                  </Button>
                </div>
              </div>
            )}
          </div>

          {/* Samples Table */}
          {addedSamples.length > 0 && (
            <div className="samples-table-section">
              <DataTable rows={renderSampleRows()} headers={sampleHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader
                              {...getHeaderProps({ header })}
                              key={header.key}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow {...getRowProps({ row })} key={row.id}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            </div>
          )}
        </Column>

        {/* Right Column - Summary Panel */}
        <Column lg={6} md={3} sm={4}>
          <Tile className="summary-panel">
            <h4 className="summary-title">
              <FormattedMessage id="shipment.box.summary" />
            </h4>

            <div className="summary-details">
              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.number" />:
                </span>
                <span className="summary-value">{boxNumber}</span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.capacity" />:
                </span>
                <span className="summary-value">
                  <FormattedMessage
                    id="shipment.box.capacityValue"
                    values={{ capacity }}
                  />
                </span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.samplesAdded" />:
                </span>
                <span className="summary-value">{addedSamples.length}</span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.destination" />:
                </span>
                <span className="summary-value">
                  {selectedFacility?.value || selectedFacility?.label || "-"}
                </span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.temperature" />:
                </span>
                <span className="summary-value">
                  {selectedTemperature?.label ||
                    intl.formatMessage({
                      id: "shipment.temperature.ambient",
                    })}
                </span>
              </div>
            </div>

            <div className="summary-actions">
              <Button
                kind="primary"
                renderIcon={Checkmark}
                onClick={handleSaveAndMarkReady}
                disabled={!isFormValid || loading}
              >
                <FormattedMessage id="shipment.action.saveAndMarkReady" />
              </Button>

              <Button
                kind="secondary"
                renderIcon={Document}
                onClick={handleSaveBox}
                disabled={!isFormValid || loading}
              >
                <FormattedMessage id="shipment.action.saveDraft" />
              </Button>

              <Button kind="ghost" onClick={handleCancel}>
                <FormattedMessage id="label.cancel" />
              </Button>
            </div>
          </Tile>
        </Column>
      </Grid>

      {/* Reject Sample Modal */}
      <Modal
        open={showRejectModal}
        size="lg"
        modalHeading={intl.formatMessage({ id: "shipment.modal.rejectSample" })}
        primaryButtonText={intl.formatMessage({
          id: "shipment.action.confirmRejection",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={confirmRejectSample}
        onRequestClose={() => {
          setShowRejectModal(false);
          setSampleToReject(null);
          setRejectionReason(null);
        }}
        primaryButtonDisabled={!rejectionReason}
      >
        <p>
          <FormattedMessage
            id="shipment.modal.rejectSampleMessage"
            values={{ accession: sampleToReject?.accessionNumber }}
          />
        </p>

        <Dropdown
          id="rejectionReason"
          titleText={intl.formatMessage({ id: "shipment.rejection.reason" })}
          label={intl.formatMessage({ id: "label.select" })}
          items={rejectionReasons}
          itemToString={(item) => (item ? item.value || item.label : "")}
          selectedItem={rejectionReason}
          onChange={({ selectedItem }) => setRejectionReason(selectedItem)}
        />
      </Modal>
    </div>
  );
};

export default BoxCreation;
