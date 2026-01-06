import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  Tag,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Checkbox,
  Accordion,
  AccordionItem,
  RadioButtonGroup,
  RadioButton,
  TimePicker,
  TimePickerSelect,
} from "@carbon/react";
import {
  Package,
  CheckmarkFilled,
  WarningFilled,
  Delivery,
  Temperature,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext, ConfigurationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import "../workflow/NotebookWorkflow.css";

/**
 * TransportPackagingPage - Sample Transport & Packaging Tracking
 * Ensures compliance with biosafety and transport standards (IATA PI650).
 *
 * Used by: Courier, Receiving staff, Quality officer
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TransportPackagingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  // State for samples
  const [pendingSamples, setPendingSamples] = useState([]);
  const [packagedSamples, setPackagedSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state
  const [packagingModalOpen, setPackagingModalOpen] = useState(false);
  const [bulkPackagingModalOpen, setBulkPackagingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [selectedRows, setSelectedRows] = useState([]);

  // Packaging form state
  const [packagingForm, setPackagingForm] = useState({
    // Primary Packaging
    containerType: "",
    sealStatus: "intact",
    barcodePresent: true,
    absorbentPresent: true,
    // Secondary Packaging
    secondaryPackageType: "",
    secondaryIntegrity: "intact",
    watertightPressureResistant: true,
    primaryContainerCount: 1,
    inspectorInitials: "",
    inspectionTimestamp: "",
    // Tertiary/Outer Packaging
    transportBoxType: "",
    biohazardLabelPresent: true,
    orientationArrowsPresent: true,
    temperatureLoggerId: "",
    courierCompany: "",
    trackingNumber: "",
    conditionOnArrival: "intact",
    iataCompliant: true,
    // Transport Status
    transportStatus: "on_time",
    notes: "",
  });

  // Container type options
  const containerTypes = [
    { id: "vacutainer", label: "Vacutainer" },
    { id: "cryovial", label: "Cryovial" },
    { id: "urine_cup", label: "Urine Cup" },
    { id: "stool_jar", label: "Stool Jar" },
    { id: "swab_tube", label: "Swab Tube" },
  ];

  // Secondary packaging types
  const secondaryPackageTypes = [
    { id: "biohazard_bag", label: "Biohazard Bag" },
    { id: "canister", label: "Canister" },
    { id: "sealed_pouch", label: "Sealed Pouch" },
  ];

  // Transport box types
  const transportBoxTypes = [
    { id: "styrofoam", label: "Styrofoam Box" },
    { id: "cardboard", label: "Cardboard Box" },
    { id: "insulated", label: "Insulated Container" },
    { id: "cold_chain", label: "Cold Chain Box" },
  ];

  // Integrity status options
  const integrityOptions = [
    { id: "intact", label: "Intact" },
    { id: "leaking", label: "Leaking" },
    { id: "damaged", label: "Damaged" },
    { id: "torn", label: "Torn" },
    { id: "wet", label: "Wet" },
  ];

  // Condition on arrival options
  const arrivalConditionOptions = [
    { id: "intact", label: "Intact" },
    { id: "damaged", label: "Damaged" },
  ];

  // Transport status options
  const transportStatusOptions = [
    { id: "on_time", label: "On Time" },
    { id: "delayed", label: "Delayed" },
  ];

  // Load samples for packaging - only collected samples
  const loadSamplesForPackaging = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Use the dedicated endpoint that returns only collected samples
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-transport`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Separate pending from packaged samples based on pageStatus
            const pending = response.filter(
              (s) =>
                s.pageStatus === "PENDING" || s.pageStatus === "IN_PROGRESS",
            );
            const packaged = response.filter(
              (s) => s.pageStatus === "COMPLETED" || s.pageStatus === "SKIPPED",
            );
            setPendingSamples(pending);
            setPackagedSamples(packaged);
          } else {
            setPendingSamples([]);
            setPackagedSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForPackaging();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, loadSamplesForPackaging]);

  // Open packaging modal for single sample
  const handleOpenPackagingModal = useCallback((sample) => {
    setSelectedSample(sample);
    setPackagingForm({
      containerType: "",
      sealStatus: "intact",
      barcodePresent: true,
      absorbentPresent: true,
      secondaryPackageType: "",
      secondaryIntegrity: "intact",
      watertightPressureResistant: true,
      primaryContainerCount: 1,
      inspectorInitials: "",
      inspectionTimestamp: new Date().toISOString(),
      transportBoxType: "",
      biohazardLabelPresent: true,
      orientationArrowsPresent: true,
      temperatureLoggerId: "",
      courierCompany: "",
      trackingNumber: "",
      conditionOnArrival: "intact",
      iataCompliant: true,
      transportStatus: "on_time",
      notes: "",
    });
    setPackagingModalOpen(true);
  }, []);

  // Open bulk packaging modal
  const handleOpenBulkPackagingModal = useCallback((selectedRowIds) => {
    setSelectedRows(selectedRowIds);
    setPackagingForm({
      containerType: "",
      sealStatus: "intact",
      barcodePresent: true,
      absorbentPresent: true,
      secondaryPackageType: "",
      secondaryIntegrity: "intact",
      watertightPressureResistant: true,
      primaryContainerCount: selectedRowIds.length,
      inspectorInitials: "",
      inspectionTimestamp: new Date().toISOString(),
      transportBoxType: "",
      biohazardLabelPresent: true,
      orientationArrowsPresent: true,
      temperatureLoggerId: "",
      courierCompany: "",
      trackingNumber: "",
      conditionOnArrival: "intact",
      iataCompliant: true,
      transportStatus: "on_time",
      notes: "",
    });
    setBulkPackagingModalOpen(true);
  }, []);

  // Check if form has non-compliance issues
  const hasNonCompliance = useCallback(() => {
    return (
      packagingForm.sealStatus !== "intact" ||
      !packagingForm.barcodePresent ||
      !packagingForm.absorbentPresent ||
      packagingForm.secondaryIntegrity !== "intact" ||
      !packagingForm.watertightPressureResistant ||
      !packagingForm.biohazardLabelPresent ||
      !packagingForm.orientationArrowsPresent ||
      packagingForm.conditionOnArrival !== "intact" ||
      !packagingForm.iataCompliant
    );
  }, [packagingForm]);

  // Submit packaging data for single sample
  const handleSubmitPackaging = useCallback(() => {
    if (!selectedSample) return;

    const packagingData = {
      sampleIds: [parseInt(selectedSample.id, 10)],
      status: hasNonCompliance() ? "SKIPPED" : "COMPLETED",
      ...packagingForm,
      nonCompliant: hasNonCompliance(),
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify(packagingData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.transport.packaging.success",
              defaultMessage: "Packaging information recorded",
            }),
            kind: hasNonCompliance()
              ? NotificationKinds.warning
              : NotificationKinds.success,
          });
          setNotificationVisible(true);
          setPackagingModalOpen(false);
          loadSamplesForPackaging();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.transport.packaging.error",
              defaultMessage: "Error recording packaging information",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedSample,
    packagingForm,
    pageData,
    hasNonCompliance,
    intl,
    addNotification,
    setNotificationVisible,
    loadSamplesForPackaging,
    onProgressUpdate,
  ]);

  // Submit bulk packaging data
  const handleSubmitBulkPackaging = useCallback(() => {
    if (selectedRows.length === 0) return;

    const packagingData = {
      sampleIds: selectedRows.map((id) => parseInt(id, 10)),
      status: hasNonCompliance() ? "SKIPPED" : "COMPLETED",
      ...packagingForm,
      nonCompliant: hasNonCompliance(),
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify(packagingData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              {
                id: "medlab.transport.bulk.packaging.success",
                defaultMessage: "Packaging recorded for {count} samples",
              },
              { count: selectedRows.length },
            ),
            kind: hasNonCompliance()
              ? NotificationKinds.warning
              : NotificationKinds.success,
          });
          setNotificationVisible(true);
          setBulkPackagingModalOpen(false);
          setSelectedRows([]);
          loadSamplesForPackaging();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.transport.bulk.packaging.error",
              defaultMessage: "Error recording bulk packaging information",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedRows,
    packagingForm,
    pageData,
    hasNonCompliance,
    intl,
    addNotification,
    setNotificationVisible,
    loadSamplesForPackaging,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = pendingSamples.length + packagedSamples.length;
  const compliantCount = packagedSamples.filter(
    (s) => s.pageStatus === "COMPLETED",
  ).length;
  const nonCompliantCount = packagedSamples.filter(
    (s) => s.pageStatus === "SKIPPED",
  ).length;
  const pendingCount = pendingSamples.length;

  // Table headers
  const pendingHeaders = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "sample.externalId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "sample.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({
        id: "medlab.collection.date",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.actions" }),
    },
  ];

  const processedHeaders = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "sample.externalId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "sample.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "transportStatus",
      header: intl.formatMessage({
        id: "medlab.transport.status",
        defaultMessage: "Transport Status",
      }),
    },
    {
      key: "compliance",
      header: intl.formatMessage({
        id: "medlab.transport.compliance",
        defaultMessage: "Compliance",
      }),
    },
  ];

  // Packaging form component (used in both modals)
  const renderPackagingForm = () => (
    <Accordion>
      {/* Primary Packaging Section */}
      <AccordionItem
        title={intl.formatMessage({
          id: "medlab.transport.primary",
          defaultMessage: "Primary Packaging (Specimen Level)",
        })}
        open
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="container-type"
              labelText={intl.formatMessage({
                id: "medlab.transport.containerType",
                defaultMessage: "Container Type",
              })}
              value={packagingForm.containerType}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  containerType: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select container type" />
              {containerTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="seal-status"
              labelText={intl.formatMessage({
                id: "medlab.transport.sealStatus",
                defaultMessage: "Seal Status",
              })}
              value={packagingForm.sealStatus}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  sealStatus: e.target.value,
                })
              }
              warn={packagingForm.sealStatus !== "intact"}
              warnText="Non-compliant: Seal not intact"
            >
              {integrityOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="barcode-present"
              labelText={intl.formatMessage({
                id: "medlab.transport.barcodePresent",
                defaultMessage: "Barcode Present",
              })}
              checked={packagingForm.barcodePresent}
              onChange={(e, { checked }) =>
                setPackagingForm({ ...packagingForm, barcodePresent: checked })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="absorbent-present"
              labelText={intl.formatMessage({
                id: "medlab.transport.absorbentPresent",
                defaultMessage: "Absorbent Material Present",
              })}
              checked={packagingForm.absorbentPresent}
              onChange={(e, { checked }) =>
                setPackagingForm({
                  ...packagingForm,
                  absorbentPresent: checked,
                })
              }
            />
          </Column>
        </Grid>
      </AccordionItem>

      {/* Secondary Packaging Section */}
      <AccordionItem
        title={intl.formatMessage({
          id: "medlab.transport.secondary",
          defaultMessage: "Secondary Packaging",
        })}
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="secondary-package-type"
              labelText={intl.formatMessage({
                id: "medlab.transport.secondaryPackageType",
                defaultMessage: "Packaging Type",
              })}
              value={packagingForm.secondaryPackageType}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  secondaryPackageType: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select packaging type" />
              {secondaryPackageTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="secondary-integrity"
              labelText={intl.formatMessage({
                id: "medlab.transport.integrity",
                defaultMessage: "Integrity",
              })}
              value={packagingForm.secondaryIntegrity}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  secondaryIntegrity: e.target.value,
                })
              }
              warn={packagingForm.secondaryIntegrity !== "intact"}
              warnText="Non-compliant: Secondary packaging not intact"
            >
              {integrityOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="watertight"
              labelText={intl.formatMessage({
                id: "medlab.transport.watertight",
                defaultMessage: "Watertight & Pressure Resistant",
              })}
              checked={packagingForm.watertightPressureResistant}
              onChange={(e, { checked }) =>
                setPackagingForm({
                  ...packagingForm,
                  watertightPressureResistant: checked,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="primary-container-count"
              labelText={intl.formatMessage({
                id: "medlab.transport.primaryContainerCount",
                defaultMessage: "Number of Primary Containers",
              })}
              type="number"
              min={1}
              value={packagingForm.primaryContainerCount}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  primaryContainerCount: parseInt(e.target.value, 10) || 1,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="inspector-initials"
              labelText={intl.formatMessage({
                id: "medlab.transport.inspectorInitials",
                defaultMessage: "Inspector Initials",
              })}
              value={packagingForm.inspectorInitials}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  inspectorInitials: e.target.value,
                })
              }
            />
          </Column>
        </Grid>
      </AccordionItem>

      {/* Tertiary/Outer Packaging Section */}
      <AccordionItem
        title={intl.formatMessage({
          id: "medlab.transport.tertiary",
          defaultMessage: "Tertiary / Outer Packaging",
        })}
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="transport-box-type"
              labelText={intl.formatMessage({
                id: "medlab.transport.transportBoxType",
                defaultMessage: "Transport Box Type",
              })}
              value={packagingForm.transportBoxType}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  transportBoxType: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select box type" />
              {transportBoxTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="temperature-logger-id"
              labelText={intl.formatMessage({
                id: "medlab.transport.temperatureLoggerId",
                defaultMessage: "Temperature Logger ID (if applicable)",
              })}
              value={packagingForm.temperatureLoggerId}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  temperatureLoggerId: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="biohazard-label"
              labelText={intl.formatMessage({
                id: "medlab.transport.biohazardLabel",
                defaultMessage: "Biohazard Labeling Present",
              })}
              checked={packagingForm.biohazardLabelPresent}
              onChange={(e, { checked }) =>
                setPackagingForm({
                  ...packagingForm,
                  biohazardLabelPresent: checked,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="orientation-arrows"
              labelText={intl.formatMessage({
                id: "medlab.transport.orientationArrows",
                defaultMessage: "Orientation Arrows Present",
              })}
              checked={packagingForm.orientationArrowsPresent}
              onChange={(e, { checked }) =>
                setPackagingForm({
                  ...packagingForm,
                  orientationArrowsPresent: checked,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="courier-company"
              labelText={intl.formatMessage({
                id: "medlab.transport.courierCompany",
                defaultMessage: "Courier Company",
              })}
              value={packagingForm.courierCompany}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  courierCompany: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tracking-number"
              labelText={intl.formatMessage({
                id: "medlab.transport.trackingNumber",
                defaultMessage: "Tracking Number",
              })}
              value={packagingForm.trackingNumber}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  trackingNumber: e.target.value,
                })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="condition-arrival"
              labelText={intl.formatMessage({
                id: "medlab.transport.conditionOnArrival",
                defaultMessage: "Condition on Arrival",
              })}
              value={packagingForm.conditionOnArrival}
              onChange={(e) =>
                setPackagingForm({
                  ...packagingForm,
                  conditionOnArrival: e.target.value,
                })
              }
              warn={packagingForm.conditionOnArrival !== "intact"}
              warnText="Non-compliant: Package arrived damaged"
            >
              {arrivalConditionOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="iata-compliant"
              labelText={intl.formatMessage({
                id: "medlab.transport.iataCompliant",
                defaultMessage: "IATA PI650 Compliant",
              })}
              checked={packagingForm.iataCompliant}
              onChange={(e, { checked }) =>
                setPackagingForm({ ...packagingForm, iataCompliant: checked })
              }
            />
          </Column>
        </Grid>
      </AccordionItem>

      {/* Transport Status Section */}
      <AccordionItem
        title={intl.formatMessage({
          id: "medlab.transport.transportStatus",
          defaultMessage: "Transport Status",
        })}
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "medlab.transport.deliveryStatus",
                defaultMessage: "Delivery Status",
              })}
              name="transport-status"
              valueSelected={packagingForm.transportStatus}
              onChange={(value) =>
                setPackagingForm({ ...packagingForm, transportStatus: value })
              }
            >
              {transportStatusOptions.map((opt) => (
                <RadioButton
                  key={opt.id}
                  id={`transport-${opt.id}`}
                  labelText={opt.label}
                  value={opt.id}
                />
              ))}
            </RadioButtonGroup>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="transport-notes"
              labelText={intl.formatMessage({
                id: "medlab.transport.notes",
                defaultMessage: "Notes / Chain of Custody Comments",
              })}
              value={packagingForm.notes}
              onChange={(e) =>
                setPackagingForm({ ...packagingForm, notes: e.target.value })
              }
              rows={3}
            />
          </Column>
        </Grid>
      </AccordionItem>

      {/* Non-compliance Warning */}
      {hasNonCompliance() && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "medlab.transport.nonCompliance.title",
            defaultMessage: "Non-Compliance Detected",
          })}
          subtitle={intl.formatMessage({
            id: "medlab.transport.nonCompliance.subtitle",
            defaultMessage:
              "One or more packaging requirements are not met. This will be flagged in the chain of custody log.",
          })}
          hideCloseButton
          lowContrast
          style={{ marginTop: "1rem" }}
        />
      )}
    </Accordion>
  );

  return (
    <div className="transport-packaging-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.transportPackaging.title"
            defaultMessage="Sample Transport & Packaging Tracking"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.transportPackaging.description"
            defaultMessage="Ensure compliance with biosafety and transport standards (IATA PI650). Record packaging details for chain of custody."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.transportPackaging.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.transportPackaging.compliant"
                  defaultMessage="Compliant"
                />
              </span>
              <span className="progress-value">{compliantCount}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.transportPackaging.nonCompliant"
                  defaultMessage="Non-Compliant"
                />
              </span>
              <span className="progress-value">{nonCompliantCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.transportPackaging.pending"
                  defaultMessage="Pending Packaging"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Loading State */}
      {loading && <Loading />}

      {/* Pending Samples Table */}
      {!loading && pendingSamples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.transportPackaging.pendingSamples"
              defaultMessage="Samples Pending Packaging Verification"
            />
          </h5>
          <DataTable
            rows={pendingSamples.map((s) => ({
              ...s,
              id: String(s.id),
            }))}
            headers={pendingHeaders}
            isSortable
          >
            {({
              rows,
              headers,
              getHeaderProps,
              getTableProps,
              getSelectionProps,
              getRowProps,
              selectedRows,
              getBatchActionProps,
            }) => {
              const batchActionProps = getBatchActionProps();
              return (
                <TableContainer>
                  <TableToolbar>
                    <TableBatchActions {...batchActionProps}>
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={Package}
                        onClick={() =>
                          handleOpenBulkPackagingModal(
                            selectedRows.map((r) => r.id),
                          )
                        }
                      >
                        <FormattedMessage
                          id="medlab.transport.bulk.package"
                          defaultMessage="Record Packaging"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        <FormattedMessage
                          id="medlab.transport.bulk.hint"
                          defaultMessage="Select samples to record bulk packaging information"
                        />
                      </p>
                    </TableToolbarContent>
                  </TableToolbar>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = pendingSamples.find(
                          (s) => String(s.id) === row.id,
                        );
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "actions" ? (
                                  <Button
                                    kind="primary"
                                    size="sm"
                                    renderIcon={Package}
                                    onClick={() =>
                                      handleOpenPackagingModal(sample)
                                    }
                                  >
                                    <FormattedMessage
                                      id="medlab.transport.recordPackaging"
                                      defaultMessage="Record Packaging"
                                    />
                                  </Button>
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          </DataTable>
        </div>
      )}

      {/* Processed Samples Table */}
      {!loading && packagedSamples.length > 0 && (
        <div className="orders-section" style={{ marginTop: "2rem" }}>
          <h5>
            <FormattedMessage
              id="medlab.page.transportPackaging.processedSamples"
              defaultMessage="Packaged Samples"
            />
          </h5>
          <DataTable
            rows={packagedSamples.map((s) => ({
              ...s,
              id: String(s.id),
              transportStatus: s.data?.transportStatus || "on_time",
              compliance:
                s.pageStatus === "COMPLETED" ? "COMPLIANT" : "NON_COMPLIANT",
            }))}
            headers={processedHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "transportStatus" ? (
                              <Tag
                                type={
                                  cell.value === "on_time" ? "green" : "yellow"
                                }
                              >
                                {cell.value === "on_time"
                                  ? "On Time"
                                  : "Delayed"}
                              </Tag>
                            ) : cell.info.header === "compliance" ? (
                              <Tag
                                type={
                                  cell.value === "COMPLIANT" ? "green" : "red"
                                }
                                renderIcon={
                                  cell.value === "COMPLIANT"
                                    ? CheckmarkFilled
                                    : WarningFilled
                                }
                              >
                                {cell.value === "COMPLIANT"
                                  ? "Compliant"
                                  : "Non-Compliant"}
                              </Tag>
                            ) : (
                              cell.value || "-"
                            )}
                          </TableCell>
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

      {/* Empty state */}
      {!loading && totalSamples === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.transportPackaging.empty"
              defaultMessage="No collected samples available for packaging verification. Samples must be collected first on the Sample Collection page."
            />
          </p>
        </div>
      )}

      {/* Single Sample Packaging Modal */}
      <Modal
        open={packagingModalOpen}
        onRequestClose={() => setPackagingModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.transport.packagingModal.title",
          defaultMessage: "Record Transport Packaging",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.transport.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitPackaging}
        size="lg"
      >
        {selectedSample && (
          <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
            <strong>Sample ID:</strong> {selectedSample.externalId || "-"}
            <br />
            <strong>Accession #:</strong>{" "}
            {selectedSample.accessionNumber || "-"}
            <br />
            <strong>Type:</strong> {selectedSample.sampleType || "-"}
          </Tile>
        )}
        {renderPackagingForm()}
      </Modal>

      {/* Bulk Packaging Modal */}
      <Modal
        open={bulkPackagingModalOpen}
        onRequestClose={() => setBulkPackagingModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.transport.bulkPackagingModal.title",
          defaultMessage: "Bulk Transport Packaging",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.transport.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitBulkPackaging}
        size="lg"
      >
        <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
          <Tag type="blue">
            <FormattedMessage
              id="medlab.transport.bulk.selectedCount"
              defaultMessage="{count} samples selected"
              values={{ count: selectedRows.length }}
            />
          </Tag>
        </Tile>
        {renderPackagingForm()}
      </Modal>
    </div>
  );
}

export default TransportPackagingPage;
