import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  TextInput,
  NumberInput,
  TextArea,
  Loading,
} from "@carbon/react";
import { Save, Checkmark } from "@carbon/react/icons";
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
 * VirologyVirusCulturePage - Page 5 of the Virology & Vaccine Unit workflow.
 * Inoculate cells with virus and track culture conditions.
 *
 * Features:
 * - Display samples from previous page (Quality Control)
 * - Log virus culture data: virus strain, temperature, CO₂, duration
 * - Track culture observations and notes
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyVirusCulturePage({
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

  // Form data for virus culture modal
  const [virusStrain, setVirusStrain] = useState("");
  const [temperature, setTemperature] = useState("");
  const [co2Percentage, setCo2Percentage] = useState("");
  const [durationHours, setDurationHours] = useState("");
  const [cultureNotes, setCultureNotes] = useState("");

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

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    console.log("VirologyVirusCulturePage - loadPageSamples called", {
      pageDataId: pageData?.id,
      pageData,
    });

    if (!pageData?.id) {
      console.log("VirologyVirusCulturePage - No pageData.id, skipping load");
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      console.log("VirologyVirusCulturePage - Default page, skipping load");
      setLoading(false);
      return;
    }

    console.log(
      "VirologyVirusCulturePage - Fetching samples for pageId:",
      pageData.id,
    );
    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (fetchedSamples) => {
        console.log(
          "VirologyVirusCulturePage - Received samples:",
          fetchedSamples,
        );
        if (fetchedSamples && fetchedSamples.length > 0) {
          console.log(
            "VirologyVirusCulturePage - First sample structure:",
            JSON.stringify(fetchedSamples[0], null, 2),
          );
          console.log(
            "VirologyVirusCulturePage - First sample status:",
            fetchedSamples[0].status,
          );
        }
        if (!componentMounted.current) {
          console.log(
            "VirologyVirusCulturePage - Component unmounted, ignoring",
          );
          return;
        }
        setSamples(fetchedSamples || []);
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  const resetForm = useCallback(() => {
    setVirusStrain("");
    setTemperature("");
    setCo2Percentage("");
    setDurationHours("");
    setCultureNotes("");
  }, []);

  const handleSaveVirusCulture = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.culture.error.noSelection",
          defaultMessage:
            "Please select at least one sample to save virus culture data.",
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
          id: "virology.culture.error.noPage",
          defaultMessage:
            "Cannot save virus culture data until the page is saved. Please save the page first.",
        }),
      });
      return;
    }

    if (!virusStrain || virusStrain.trim() === "") {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.culture.error.virusStrainRequired",
          defaultMessage: "Please enter the virus strain.",
        }),
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      virusStrain: virusStrain,
    };

    if (temperature && temperature !== "") {
      payload.temperature = parseFloat(temperature);
    }

    if (co2Percentage && co2Percentage !== "") {
      payload.co2Percentage = parseFloat(co2Percentage);
    }

    if (durationHours && durationHours !== "") {
      payload.durationHours = parseInt(durationHours, 10);
    }

    if (cultureNotes && cultureNotes.trim() !== "") {
      payload.notes = cultureNotes;
    }

    postToOpenElisServerJsonResponse(
      "/rest/virology/virus-culture",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.culture.success.saved",
                defaultMessage:
                  "Virus culture data saved for {count} sample(s).",
              },
              { count: response.samplesUpdated || selectedSampleIds.length },
            ),
          });

          setModalOpen(false);
          resetForm();
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
            subtitle: intl.formatMessage(
              {
                id: "virology.culture.error.saveFailed",
                defaultMessage:
                  "Failed to save virus culture data: {error}. Please try again.",
              },
              { error: response.error || "Unknown error" },
            ),
          });
        }
      },
    );
  }, [
    virusStrain,
    temperature,
    co2Percentage,
    durationHours,
    cultureNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    resetForm,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const handleCompleteVirusCulture = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.culture.error.noSelectionComplete",
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
          id: "virology.culture.error.noPageComplete",
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
                id: "virology.culture.success.completed",
                defaultMessage:
                  "Completed virus culture for {count} sample(s). They will proceed to the next stage.",
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
              id: "virology.culture.error.complete",
              defaultMessage:
                "Failed to complete virus culture. Please try again.",
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

  // Custom columns for virus culture display
  const getAdditionalColumns = (intl) => [
    {
      key: "virusStrain",
      header: intl.formatMessage({
        id: "virology.culture.column.virusStrain",
        defaultMessage: "Virus Strain",
      }),
      render: (value, sample) => {
        const strain =
          value || sample?.virusStrain || sample?.data?.virusStrain;
        return strain || "-";
      },
    },
    {
      key: "temperature",
      header: intl.formatMessage({
        id: "virology.culture.column.temperature",
        defaultMessage: "Temperature (°C)",
      }),
      render: (value, sample) => {
        const temp = value || sample?.temperature || sample?.data?.temperature;
        if (!temp) return "-";
        return `${temp}°C`;
      },
    },
    {
      key: "co2Percentage",
      header: intl.formatMessage({
        id: "virology.culture.column.co2",
        defaultMessage: "CO₂ (%)",
      }),
      render: (value, sample) => {
        const co2 =
          value || sample?.co2Percentage || sample?.data?.co2Percentage;
        if (!co2) return "-";
        return `${co2}%`;
      },
    },
    {
      key: "durationHours",
      header: intl.formatMessage({
        id: "virology.culture.column.duration",
        defaultMessage: "Duration (hrs)",
      }),
      render: (value, sample) => {
        const duration =
          value || sample?.durationHours || sample?.data?.durationHours;
        if (!duration) return "-";
        return `${duration} hrs`;
      },
    },
  ];

  const additionalColumns = useMemo(() => getAdditionalColumns(intl), [intl]);

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(() => {
    const filtered = samples.filter(
      (sample) =>
        sample.status === "PENDING" || sample.status === "IN_PROGRESS",
    );
    console.log("VirologyVirusCulturePage - pendingSamples:", filtered);
    return filtered;
  }, [samples]);

  const completedSamples = useMemo(() => {
    const filtered = samples.filter((sample) => sample.status === "COMPLETED");
    console.log("VirologyVirusCulturePage - completedSamples:", filtered);
    return filtered;
  }, [samples]);

  const pendingCount = useMemo(() => {
    console.log(
      "VirologyVirusCulturePage - pendingCount:",
      pendingSamples.length,
    );
    return pendingSamples.length;
  }, [pendingSamples]);

  const completedCount = useMemo(() => {
    console.log(
      "VirologyVirusCulturePage - completedCount:",
      completedSamples.length,
    );
    return completedSamples.length;
  }, [completedSamples]);

  return (
    <div className="virology-virus-culture-page">
      {loading && <Loading />}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <Tile className="instructions-tile">
              <h4>
                <FormattedMessage
                  id="virology.culture.instructions.title"
                  defaultMessage="Virus Culture Instructions"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="virology.culture.instructions.body"
                  defaultMessage="Inoculate cells with virus. Record virus strain (required), culture conditions (temperature, CO₂ percentage, duration), and any observations during the culture period."
                />
              </p>
            </Tile>
          </div>
        </Column>
      </Grid>

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
            id="virology.culture.logCulture"
            defaultMessage="Log Virus Culture Data"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteVirusCulture}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.culture.complete"
            defaultMessage="Complete Virus Culture ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.culture.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
        </div>
        <SampleGrid
          samples={pendingSamples}
          onSelectionChange={setSelectedSampleIds}
          selectedIds={selectedSampleIds}
          additionalColumns={additionalColumns}
        />
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.culture.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
        </div>
        <SampleGrid
          samples={completedSamples}
          additionalColumns={additionalColumns}
          readOnly
        />
      </div>

      {/* Virus Culture Data Entry Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => {
          setModalOpen(false);
          resetForm();
        }}
        modalHeading={intl.formatMessage({
          id: "virology.culture.modal.heading",
          defaultMessage: "Log Virus Culture Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.culture.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "virology.culture.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveVirusCulture}
        size="md"
      >
        <div className="modal-form-content">
          <p className="modal-selected-count">
            <FormattedMessage
              id="virology.culture.modal.selectedCount"
              defaultMessage="Selected samples: {count}"
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <TextInput
            id="virusStrain"
            labelText={
              <span>
                <FormattedMessage
                  id="virology.culture.modal.virusStrain"
                  defaultMessage="Virus Strain"
                />
                <span style={{ color: "red" }}> *</span>
              </span>
            }
            placeholder={intl.formatMessage({
              id: "virology.culture.modal.virusStrain.placeholder",
              defaultMessage: "e.g., SARS-CoV-2, H1N1, etc.",
            })}
            value={virusStrain}
            onChange={(e) => setVirusStrain(e.target.value)}
            required
          />

          <NumberInput
            id="temperature"
            label={intl.formatMessage({
              id: "virology.culture.modal.temperature",
              defaultMessage: "Temperature (°C)",
            })}
            value={temperature}
            onChange={(e) => setTemperature(e.target.value)}
            min={-80}
            max={50}
            step={0.1}
            placeholder="37"
          />

          <NumberInput
            id="co2Percentage"
            label={intl.formatMessage({
              id: "virology.culture.modal.co2",
              defaultMessage: "CO₂ Percentage (%)",
            })}
            value={co2Percentage}
            onChange={(e) => setCo2Percentage(e.target.value)}
            min={0}
            max={100}
            step={0.1}
            placeholder="5"
          />

          <NumberInput
            id="durationHours"
            label={intl.formatMessage({
              id: "virology.culture.modal.duration",
              defaultMessage: "Duration (hours)",
            })}
            value={durationHours}
            onChange={(e) => setDurationHours(e.target.value)}
            min={0}
            step={1}
            placeholder="24"
          />

          <TextArea
            id="cultureNotes"
            labelText={intl.formatMessage({
              id: "virology.culture.modal.notes",
              defaultMessage: "Culture Notes",
            })}
            placeholder={intl.formatMessage({
              id: "virology.culture.modal.notes.placeholder",
              defaultMessage: "Any observations during culture period...",
            })}
            value={cultureNotes}
            onChange={(e) => setCultureNotes(e.target.value)}
            rows={3}
          />
        </div>
      </Modal>
    </div>
  );
}

export default VirologyVirusCulturePage;
