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
  Tag,
  Modal,
  TextInput,
  TextArea,
  Dropdown,
  ComboBox,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { Add, TrashCan, Save, Chemistry, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyMediaPreparationPage - Page 2 of the Virology & Vaccine Unit workflow.
 * Displays samples and allows logging materials used in media preparation.
 *
 * Features:
 * - Display and select samples from the notebook (verified from Page 1)
 * - Open dialog to log media preparation materials
 * - Select media, reagents, equipment with full traceability
 * - Attach selected samples to the media preparation batch
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyMediaPreparationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);

  // Inventory data
  const [availableMedia, setAvailableMedia] = useState([]);
  const [availableReagents, setAvailableReagents] = useState([]);
  const [availableEquipment, setAvailableEquipment] = useState([]);

  // Selected items (for modal form)
  const [selectedMedia, setSelectedMedia] = useState(null);
  const [reagentList, setReagentList] = useState([]);
  const [selectedEquipment, setSelectedEquipment] = useState(null);

  // Form data (for modal form)
  const [preparationNotes, setPreparationNotes] = useState("");
  const [batchNumber, setBatchNumber] = useState("");

  // Sterilization modal state
  const [sterilizationModalOpen, setSterilizationModalOpen] = useState(false);
  const [sterilizationType, setSterilizationType] = useState(null);
  const [sterilizationTemp, setSterilizationTemp] = useState("");
  const [sterilizationTime, setSterilizationTime] = useState("");
  const [sterilizationPressure, setSterilizationPressure] = useState("");
  const [sterilizationNotes, setSterilizationNotes] = useState("");

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadInventory();
    loadEquipment();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        console.log(
          "Loaded samples for page",
          pageData.id,
          ":",
          JSON.stringify(response, null, 2),
        );
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              console.log(
                "Sample",
                sample.id,
                "data:",
                JSON.stringify(sample.data, null, 2),
              );
              return {
                id: String(sample.id || sample.sampleItemId),
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || sample.status || "PENDING",
                // Virology-specific metadata
                sampleId: sample.data?.sampleId,
                source: sample.data?.source,
                testType: sample.data?.testType,
                projectStudyAssociation: sample.data?.projectStudyAssociation,
                // Media preparation data
                mediaName: sample.data?.mediaName,
                reagents: sample.data?.reagents,
                batchNumber: sample.data?.batchNumber,
                // Sterilization data
                sterilization: sample.data?.sterilization,
                // Keep full data for column rendering
                data: sample.data,
              };
            });
            console.log(
              "Transformed samples:",
              JSON.stringify(transformedSamples, null, 2),
            );
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const loadInventory = useCallback(() => {
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          // All inventory items for media
          const media = response.map((item) => ({
            ...item,
            text: item.name,
          }));
          setAvailableMedia(media);

          // All inventory items for reagents
          const reagents = response.map((item) => ({
            ...item,
            id: item.itemId,
            text: item.name,
            category: item.category,
            lotNumber: item.lotNumber,
            expirationDate: item.expirationDate,
          }));
          setAvailableReagents(reagents);
        }
      },
    );
  }, []);

  const loadEquipment = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(`/rest/notebook/${entryId}`, (response) => {
      if (componentMounted.current && response?.analyzers) {
        const equipment = response.analyzers.map((item) => ({
          id: item.id,
          text: item.value || item.name || item.analyzerName || "",
          name: item.value || item.name || item.analyzerName || "",
          value: item.value,
          serialNumber: item.serialNumber || item.modelNumber || "",
        }));
        setAvailableEquipment(equipment);
      }
    });
  }, [entryId]);

  const handleAddReagent = () => {
    setReagentList([
      ...reagentList,
      {
        id: Date.now(),
        reagentId: null,
        reagentName: "",
        lotNumber: "",
        expiryDate: "",
        category: "",
      },
    ]);
  };

  const handleRemoveReagent = (id) => {
    setReagentList(reagentList.filter((r) => r.id !== id));
  };

  const handleReagentChange = (id, field, value) => {
    setReagentList(
      reagentList.map((r) => {
        if (r.id === id) {
          if (field === "reagent") {
            // When selecting reagent, auto-fill lot and expiry if available
            return {
              ...r,
              reagentId: value.id,
              reagentName: value.text,
              category: value.category || "",
              lotNumber: value.lotNumber || "",
              expiryDate: value.expirationDate || "",
            };
          }
          return { ...r, [field]: value };
        }
        return r;
      }),
    );
  };

  const handleSave = useCallback(() => {
    // Validation
    if (!selectedMedia) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please select a media type",
      });
      return;
    }

    if (reagentList.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please add at least one reagent",
      });
      return;
    }

    // Check all reagents have required fields
    const incompleteReagents = reagentList.filter(
      (r) => !r.reagentName || !r.lotNumber || !r.expiryDate,
    );
    if (incompleteReagents.length > 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "All reagents must have name, lot number, and expiry date",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      mediaId: selectedMedia.itemId,
      mediaName: selectedMedia.name,
      mediaLotNumber: selectedMedia.lotNumber,
      mediaExpiryDate: selectedMedia.expirationDate,
      reagents: reagentList.map((r) => ({
        reagentId: r.reagentId,
        name: r.reagentName,
        category: r.category,
        lotNumber: r.lotNumber,
        expiryDate: r.expiryDate,
      })),
      equipmentId: selectedEquipment?.id,
      equipmentName: selectedEquipment?.text,
      equipmentSerialNumber: selectedEquipment?.serialNumber,
      batchNumber,
      preparationNotes,
    };

    console.log("Media preparation payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/media-preparation",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success || response.mediaPreparationId) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Media preparation logged successfully. ${samplesUpdated} sample(s) updated.`,
          });
          console.log("Backend response:", response);

          // Clear form and selection
          setSelectedMedia(null);
          setReagentList([]);
          setSelectedEquipment(null);
          setBatchNumber("");
          setPreparationNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples to show updated media/reagent data
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorMessage =
            response.error || response.message || "Unknown error";
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: errorMessage,
          });
        }
      },
    );
  }, [
    selectedMedia,
    reagentList,
    selectedSampleIds,
    selectedEquipment,
    batchNumber,
    preparationNotes,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Check if all reagents are complete
  const allReagentsComplete = reagentList.every(
    (r) => r.reagentName && r.lotNumber && r.expiryDate,
  );

  // Sterilization type options
  const sterilizationTypeOptions = [
    {
      id: "autoclaving",
      text: intl.formatMessage({
        id: "virology.sterilization.type.autoclaving",
        defaultMessage: "Autoclaving",
      }),
    },
    {
      id: "filtration",
      text: intl.formatMessage({
        id: "virology.sterilization.type.filtration",
        defaultMessage: "Filtration",
      }),
    },
  ];

  // Handle sterilization save
  const handleSaveSterilization = useCallback(() => {
    // Validation
    if (!sterilizationType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please select a sterilization method",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    // Validate required parameters based on sterilization type
    if (sterilizationType.id === "autoclaving") {
      if (!sterilizationTemp || !sterilizationTime || !sterilizationPressure) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle:
            "Autoclaving requires temperature, time, and pressure values",
        });
        return;
      }
    } else if (sterilizationType.id === "filtration") {
      if (!sterilizationTime) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Filtration requires time duration",
        });
        return;
      }
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      sterilizationType: sterilizationType.id,
      sterilizationMethod: sterilizationType.text,
      temperature: sterilizationTemp,
      time: sterilizationTime,
      pressure: sterilizationPressure,
      notes: sterilizationNotes,
    };

    console.log("Sterilization payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/sterilization",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);
        console.log("Sterilization save response:", response);

        if (response.success) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Sterilization logged successfully. ${samplesUpdated} sample(s) updated.`,
          });

          // Clear form and selection
          setSterilizationType(null);
          setSterilizationTemp("");
          setSterilizationTime("");
          setSterilizationPressure("");
          setSterilizationNotes("");
          setSelectedSampleIds([]);
          setSterilizationModalOpen(false);

          // Reload samples to show updated data
          console.log("Reloading samples after sterilization save...");
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorMessage =
            response.error || response.message || "Unknown error";
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: errorMessage,
          });
        }
      },
    );
  }, [
    sterilizationType,
    sterilizationTemp,
    sterilizationTime,
    sterilizationPressure,
    sterilizationNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing media preparation and moving samples to next page (Cell Culture)
  const handleCompleteMediaPreparation = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.media.error.noSelection",
          defaultMessage: "Please select at least one sample to complete.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "virology.media.error.noPage",
          defaultMessage:
            "Cannot complete samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.media.success.completed",
                defaultMessage:
                  "Completed media preparation for {count} sample(s). They will proceed to Cell Culture.",
              },
              { count: selectedSampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.media.error.complete",
              defaultMessage:
                "Failed to complete media preparation. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Custom columns for virology sample display
  const getAdditionalColumns = (intl) => [
    {
      key: "source",
      header: intl.formatMessage({
        id: "notebook.sample.source",
        defaultMessage: "Source",
      }),
      render: (value, sample) => {
        const source = value || sample?.source || sample?.data?.source;
        if (!source) return "-";
        if (typeof source === "string") return source;
        if (typeof source === "object") {
          return source.value || source.name || source.description || "-";
        }
        return String(source);
      },
    },
    {
      key: "testType",
      header: intl.formatMessage({
        id: "notebook.sample.testType",
        defaultMessage: "Test Type",
      }),
      render: (value, sample) => {
        const testType = value || sample?.testType || sample?.data?.testType;
        if (!testType) return "-";

        let testTypeStr;
        if (typeof testType === "string") {
          testTypeStr = testType;
        } else if (typeof testType === "object") {
          testTypeStr =
            testType.value ||
            testType.name ||
            testType.description ||
            JSON.stringify(testType);
        } else {
          testTypeStr = String(testType);
        }

        const tagType =
          testTypeStr.toLowerCase() === "viral"
            ? "purple"
            : testTypeStr.toLowerCase() === "bacterial"
              ? "teal"
              : "gray";
        return (
          <Tag type={tagType} size="sm">
            {testTypeStr}
          </Tag>
        );
      },
    },
    {
      key: "mediaName",
      header: intl.formatMessage({
        id: "virology.media.column.media",
        defaultMessage: "Media Used",
      }),
      render: (value, sample) => {
        const media = value || sample?.mediaName || sample?.data?.mediaName;
        if (!media) return "-";
        if (typeof media === "string") return media;
        if (typeof media === "object") {
          return media.value || media.name || media.description || "-";
        }
        return String(media);
      },
    },
    {
      key: "reagents",
      header: intl.formatMessage({
        id: "virology.media.column.reagents",
        defaultMessage: "Reagents",
      }),
      render: (value, sample) => {
        const reagents = value || sample?.reagents || sample?.data?.reagents;
        if (!reagents) return "-";

        // If reagents is an array, join the names
        if (Array.isArray(reagents)) {
          if (reagents.length === 0) return "-";
          const reagentNames = reagents
            .map((r) => {
              if (typeof r === "string") return r;
              if (typeof r === "object") {
                return r.name || r.reagentName || r.value || r.text || "-";
              }
              return String(r);
            })
            .filter((name) => name !== "-");

          if (reagentNames.length === 0) return "-";

          // Show first 2 reagents, then count if more
          if (reagentNames.length <= 2) {
            return reagentNames.join(", ");
          } else {
            return `${reagentNames.slice(0, 2).join(", ")} +${reagentNames.length - 2}`;
          }
        }

        // If reagents is a string, return it
        if (typeof reagents === "string") return reagents;

        // If reagents is an object, try to extract a name
        if (typeof reagents === "object") {
          return reagents.value || reagents.name || reagents.description || "-";
        }

        return String(reagents);
      },
    },
    {
      key: "batchNumber",
      header: intl.formatMessage({
        id: "virology.media.column.batch",
        defaultMessage: "Batch #",
      }),
      render: (value, sample) => {
        const batch = value || sample?.batchNumber || sample?.data?.batchNumber;
        if (!batch) return "-";
        if (typeof batch === "string") return batch;
        if (typeof batch === "number") return String(batch);
        if (typeof batch === "object") {
          return batch.value || batch.name || batch.id || "-";
        }
        return String(batch);
      },
    },
    {
      key: "sterilization",
      header: intl.formatMessage({
        id: "virology.sterilization.column",
        defaultMessage: "Sterilization",
      }),
      render: (value, sample) => {
        console.log(
          "Rendering sterilization column for sample:",
          sample?.id,
          "value:",
          value,
          "sterilization:",
          sample?.sterilization,
          "data.sterilization:",
          sample?.data?.sterilization,
        );
        const sterilization =
          value || sample?.sterilization || sample?.data?.sterilization;
        if (!sterilization) return "-";

        const method =
          sterilization.method ||
          sterilization.sterilizationMethod ||
          sterilization.type ||
          "";
        const temp = sterilization.temperature;
        const time = sterilization.time;
        const pressure = sterilization.pressure;

        // Build a compact display
        let display = method;
        const params = [];
        if (temp) params.push(`${temp}°C`);
        if (time) params.push(`${time} min`);
        if (pressure) params.push(`${pressure} psi`);

        if (params.length > 0) {
          display += ` (${params.join(", ")})`;
        }

        const tagType = method.toLowerCase().includes("autocla")
          ? "red"
          : "cyan";
        console.log(
          "Sterilization render output - display:",
          display,
          "tagType:",
          tagType,
        );
        return (
          <Tag type={tagType} size="sm">
            {display || "-"}
          </Tag>
        );
      },
    },
  ];

  return (
    <div className="virology-media-preparation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.media.preparation.title"
            defaultMessage="Media Preparation - Material Logging"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.media.preparation.page.description"
            defaultMessage="Select samples and log materials used in media preparation with full traceability."
          />
        </p>
      </div>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="md"
          renderIcon={Save}
          onClick={() => setModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="virology.media.preparation.open"
            defaultMessage="Log Media Preparation"
          />
        </Button>
        <Button
          kind="secondary"
          size="md"
          renderIcon={Chemistry}
          onClick={() => setSterilizationModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.sterilization.open"
            defaultMessage="Log Sterilization"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteMediaPreparation}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.media.complete"
            defaultMessage="Complete Media Preparation ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.media.samples.title"
              defaultMessage="Samples for Media Preparation"
            />
            {selectedSampleIds.length > 0 && (
              <Tag type="blue" size="sm" className="count-tag">
                {selectedSampleIds.length} selected
              </Tag>
            )}
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="virology.media.samples.description"
              defaultMessage="Select samples to attach to the media preparation batch. Click 'Log Media Preparation' to record materials used."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && samples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="virology.media.samples.empty"
                  defaultMessage="No samples available. Complete sample reception on Page 1 first."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="media-prep-samples"
              samples={samples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Material Logging Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.media.preparation.modal.title"
            defaultMessage="Log Media Preparation Materials"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.media.preparation.modal.subtitle"
            defaultMessage="Record all materials and equipment used with full traceability"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.media.save"
            defaultMessage="Save Material Log"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSave}
        primaryButtonDisabled={
          loading ||
          !selectedMedia ||
          reagentList.length === 0 ||
          !allReagentsComplete
        }
        size="lg"
      >
        <Grid fullWidth>
          {/* Batch Number */}
          <Column lg={8}>
            <TextInput
              id="batch-number"
              labelText={
                <FormattedMessage
                  id="virology.media.batchNumber"
                  defaultMessage="Batch Number"
                />
              }
              value={batchNumber}
              onChange={(e) => setBatchNumber(e.target.value)}
              placeholder="e.g., MP-2026-001"
            />
          </Column>

          {/* Media Selection */}
          <Column lg={8}>
            <Dropdown
              id="media-select"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.media.select"
                    defaultMessage="Select Media"
                  />
                  {" *"}
                </span>
              }
              label="Select media from inventory..."
              items={availableMedia}
              itemToString={(item) =>
                item ? item.text || item.name || "" : ""
              }
              selectedItem={selectedMedia}
              onChange={({ selectedItem }) => setSelectedMedia(selectedItem)}
              disabled={loading}
            />
            {selectedMedia?.lotNumber && (
              <div style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
                <strong>Lot:</strong> {selectedMedia.lotNumber}
                {selectedMedia.expirationDate && (
                  <>
                    {" | "}
                    <strong>Expiry:</strong> {selectedMedia.expirationDate}
                  </>
                )}
              </div>
            )}
          </Column>

          {/* Equipment Selection */}
          <Column lg={8}>
            <Dropdown
              id="equipment-select"
              titleText={
                <FormattedMessage
                  id="virology.equipment.select"
                  defaultMessage="Equipment Used"
                />
              }
              label="Select equipment..."
              items={availableEquipment}
              itemToString={(item) =>
                item ? item.text || item.name || "" : ""
              }
              selectedItem={selectedEquipment}
              onChange={({ selectedItem }) =>
                setSelectedEquipment(selectedItem)
              }
              disabled={loading}
            />
            {selectedEquipment?.serialNumber && (
              <div style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
                <strong>Serial #:</strong> {selectedEquipment.serialNumber}
              </div>
            )}
          </Column>

          {/* Preparation Notes */}
          <Column lg={16}>
            <TextArea
              id="preparation-notes"
              labelText={
                <FormattedMessage
                  id="virology.media.notes"
                  defaultMessage="Preparation Notes"
                />
              }
              placeholder="Add any additional observations or notes..."
              value={preparationNotes}
              onChange={(e) => setPreparationNotes(e.target.value)}
              rows={4}
            />
          </Column>

          {/* Reagents Section */}
          <Column lg={16}>
            <Tile style={{ marginTop: "1rem", marginBottom: "3rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "1rem",
                }}
              >
                <h4>
                  <FormattedMessage
                    id="virology.reagents.title"
                    defaultMessage="Reagents Used"
                  />
                  {" *"}
                </h4>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Add}
                  onClick={handleAddReagent}
                  disabled={loading}
                >
                  <FormattedMessage
                    id="virology.reagent.add"
                    defaultMessage="Add Reagent"
                  />
                </Button>
              </div>

              {reagentList.length === 0 ? (
                <p style={{ color: "#6f6f6f", fontStyle: "italic" }}>
                  <FormattedMessage
                    id="virology.reagents.empty"
                    defaultMessage="No reagents added yet. Click 'Add Reagent' to start."
                  />
                </p>
              ) : (
                <div style={{ width: "100%" }}>
                  {reagentList.map((reagent) => (
                    <div
                      key={reagent.id}
                      style={{
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        padding: "1rem",
                        marginBottom: "1rem",
                        backgroundColor: "#ffffff",
                      }}
                    >
                      <Grid narrow>
                        <Column lg={6} md={4} sm={4}>
                          <ComboBox
                            id={`reagent-${reagent.id}`}
                            titleText={
                              <span>
                                {intl.formatMessage({
                                  id: "virology.reagent.name",
                                  defaultMessage: "Reagent",
                                })}
                                {" *"}
                              </span>
                            }
                            placeholder="Select reagent..."
                            items={availableReagents}
                            itemToString={(item) => (item ? item.text : "")}
                            selectedItem={
                              availableReagents.find(
                                (r) => r.text === reagent.reagentName,
                              ) || null
                            }
                            onChange={({ selectedItem }) =>
                              handleReagentChange(
                                reagent.id,
                                "reagent",
                                selectedItem,
                              )
                            }
                            size="sm"
                            invalid={!reagent.reagentName}
                            invalidText="Required"
                          />
                        </Column>
                        <Column lg={4} md={4} sm={4}>
                          <TextInput
                            id={`category-${reagent.id}`}
                            labelText={intl.formatMessage({
                              id: "virology.reagent.category",
                              defaultMessage: "Category",
                            })}
                            value={reagent.category}
                            readOnly
                            size="sm"
                          />
                        </Column>
                        <Column lg={4} md={4} sm={4}>
                          <TextInput
                            id={`lot-${reagent.id}`}
                            labelText={
                              <span>
                                {intl.formatMessage({
                                  id: "virology.reagent.lot",
                                  defaultMessage: "Lot Number",
                                })}
                                {" *"}
                              </span>
                            }
                            value={reagent.lotNumber}
                            onChange={(e) =>
                              handleReagentChange(
                                reagent.id,
                                "lotNumber",
                                e.target.value,
                              )
                            }
                            placeholder="Lot #"
                            size="sm"
                            invalid={!reagent.lotNumber}
                            invalidText="Required"
                          />
                        </Column>
                        <Column lg={4} md={4} sm={4}>
                          <DatePicker
                            datePickerType="single"
                            dateFormat="Y-m-d"
                            value={reagent.expiryDate}
                            onChange={(dates) =>
                              handleReagentChange(
                                reagent.id,
                                "expiryDate",
                                dates[0]
                                  ? dates[0].toISOString().split("T")[0]
                                  : "",
                              )
                            }
                          >
                            <DatePickerInput
                              id={`expiry-${reagent.id}`}
                              placeholder="YYYY-MM-DD"
                              labelText={
                                <span>
                                  {intl.formatMessage({
                                    id: "virology.reagent.expiry",
                                    defaultMessage: "Expiry Date",
                                  })}
                                  {" *"}
                                </span>
                              }
                              size="sm"
                              invalid={!reagent.expiryDate}
                              invalidText="Required"
                            />
                          </DatePicker>
                        </Column>
                        <Column lg={2} md={2} sm={2}>
                          <div style={{ marginTop: "1.5rem" }}>
                            <Button
                              kind="danger--ghost"
                              size="sm"
                              renderIcon={TrashCan}
                              iconDescription="Remove"
                              onClick={() => handleRemoveReagent(reagent.id)}
                            >
                              <FormattedMessage
                                id="virology.reagent.remove"
                                defaultMessage="Remove"
                              />
                            </Button>
                          </div>
                        </Column>
                      </Grid>
                    </div>
                  ))}
                </div>
              )}
            </Tile>
          </Column>
        </Grid>
      </Modal>

      {/* Sterilization Modal */}
      <Modal
        open={sterilizationModalOpen}
        onRequestClose={() => setSterilizationModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.sterilization.modal.title"
            defaultMessage="Log Sterilization"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.sterilization.modal.subtitle"
            defaultMessage="Record sterilization parameters for selected samples"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.sterilization.save"
            defaultMessage="Save Sterilization Log"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveSterilization}
        primaryButtonDisabled={loading || !sterilizationType}
        size="md"
      >
        <Grid fullWidth>
          {/* Sterilization Type Selection */}
          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="sterilization-type"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.type"
                    defaultMessage="Sterilization Method"
                  />
                  {" *"}
                </span>
              }
              label="Select sterilization method..."
              items={sterilizationTypeOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={sterilizationType}
              onChange={({ selectedItem }) =>
                setSterilizationType(selectedItem)
              }
              disabled={loading}
            />
          </Column>

          {/* Temperature (for Autoclaving) */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="sterilization-temp"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.temperature"
                    defaultMessage="Temperature (°C)"
                  />
                  {sterilizationType?.id === "autoclaving" && " *"}
                </span>
              }
              value={sterilizationTemp}
              onChange={(e) => setSterilizationTemp(e.target.value)}
              placeholder="e.g., 121"
              type="number"
              disabled={loading}
              invalid={
                sterilizationType?.id === "autoclaving" && !sterilizationTemp
              }
              invalidText="Required for autoclaving"
            />
          </Column>

          {/* Time */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="sterilization-time"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.time"
                    defaultMessage="Time (minutes)"
                  />
                  {" *"}
                </span>
              }
              value={sterilizationTime}
              onChange={(e) => setSterilizationTime(e.target.value)}
              placeholder="e.g., 15"
              type="number"
              disabled={loading}
              invalid={!sterilizationTime && sterilizationType}
              invalidText="Required"
            />
          </Column>

          {/* Pressure (for Autoclaving) */}
          <Column lg={6} md={4} sm={4}>
            <TextInput
              id="sterilization-pressure"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.sterilization.pressure"
                    defaultMessage="Pressure (psi)"
                  />
                  {sterilizationType?.id === "autoclaving" && " *"}
                </span>
              }
              value={sterilizationPressure}
              onChange={(e) => setSterilizationPressure(e.target.value)}
              placeholder="e.g., 15"
              type="number"
              disabled={loading || sterilizationType?.id === "filtration"}
              invalid={
                sterilizationType?.id === "autoclaving" &&
                !sterilizationPressure
              }
              invalidText="Required for autoclaving"
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="sterilization-notes"
              labelText={
                <FormattedMessage
                  id="virology.sterilization.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Add any additional observations or notes..."
              value={sterilizationNotes}
              onChange={(e) => setSterilizationNotes(e.target.value)}
              rows={3}
              disabled={loading}
            />
          </Column>

          {/* Info about selected samples */}
          <Column lg={16} md={8} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <Tag type="blue">
                {selectedSampleIds.length} sample(s) selected
              </Tag>
            </div>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default VirologyMediaPreparationPage;
