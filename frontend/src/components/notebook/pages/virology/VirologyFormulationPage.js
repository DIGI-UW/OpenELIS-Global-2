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
 * VirologyFormulationPage - Page 7 of the Virology & Vaccine Unit workflow.
 * Formulation: Prepare viral product while recording formulation details.
 *
 * WORKFLOW: Virus Culture (Page 5) → Dark Room Imaging (Page 6) → Formulation (Page 7)
 *
 * Features:
 * - Display samples from previous page (Dark Room Imaging - Page 6)
 * - Document formulation details (stabilizers, preservatives, concentrations)
 * - Record batch information and buffer composition
 * - Track formulation notes
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyFormulationPage({
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

  // Form data for formulation modal
  const [batchNumber, setBatchNumber] = useState("");
  const [stabilizers, setStabilizers] = useState("");
  const [preservatives, setPreservatives] = useState("");
  const [virusConcentration, setVirusConcentration] = useState("");
  const [bufferComposition, setBufferComposition] = useState("");
  const [formulationNotes, setFormulationNotes] = useState("");

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

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
      (fetchedSamples) => {
        if (!componentMounted.current) {
          return;
        }
        setSamples(fetchedSamples || []);
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  const resetForm = useCallback(() => {
    setBatchNumber("");
    setStabilizers("");
    setPreservatives("");
    setVirusConcentration("");
    setBufferComposition("");
    setFormulationNotes("");
  }, []);

  const handleSaveFormulation = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.formulation.error.noSelection",
          defaultMessage:
            "Please select at least one sample to save formulation data.",
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
          id: "virology.formulation.error.noPage",
          defaultMessage:
            "Cannot save formulation data until the page is saved. Please save the page first.",
        }),
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
    };

    if (batchNumber && batchNumber.trim() !== "") {
      payload.batchNumber = batchNumber.trim();
    }

    if (stabilizers && stabilizers.trim() !== "") {
      payload.stabilizers = stabilizers.trim();
    }

    if (preservatives && preservatives.trim() !== "") {
      payload.preservatives = preservatives.trim();
    }

    if (virusConcentration && virusConcentration.trim() !== "") {
      payload.virusConcentration = virusConcentration.trim();
    }

    if (bufferComposition && bufferComposition.trim() !== "") {
      payload.bufferComposition = bufferComposition.trim();
    }

    if (formulationNotes && formulationNotes.trim() !== "") {
      payload.formulationNotes = formulationNotes.trim();
    }

    postToOpenElisServerJsonResponse(
      "/rest/virology/formulation/save",
      JSON.stringify(payload),
      (response) => {
        if (!componentMounted.current) {
          return;
        }

        setLoading(false);
        setModalOpen(false);
        resetForm();
        setSelectedSampleIds([]);

        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          subtitle: intl.formatMessage({
            id: "virology.formulation.save.success",
            defaultMessage: "Formulation data saved successfully.",
          }),
        });

        loadPageSamples();
        onProgressUpdate?.();
      },
      (error) => {
        if (!componentMounted.current) {
          return;
        }

        setLoading(false);
        console.error("Error saving formulation data:", error);

        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          }),
          subtitle: intl.formatMessage({
            id: "virology.formulation.save.error",
            defaultMessage:
              "Failed to save formulation data. Please try again.",
          }),
        });
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    batchNumber,
    stabilizers,
    preservatives,
    virusConcentration,
    bufferComposition,
    formulationNotes,
    notify,
    intl,
    loadPageSamples,
    resetForm,
    onProgressUpdate,
  ]);

  const handleCompleteFormulation = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.formulation.error.noSelection",
          defaultMessage:
            "Please select at least one sample to complete formulation.",
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
          id: "virology.formulation.error.noPage",
          defaultMessage:
            "Cannot complete formulation until the page is saved. Please save the page first.",
        }),
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/formulation/complete",
      JSON.stringify(payload),
      (response) => {
        if (!componentMounted.current) {
          return;
        }

        setLoading(false);
        setSelectedSampleIds([]);

        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          subtitle: intl.formatMessage({
            id: "virology.formulation.complete.success",
            defaultMessage: "Formulation completed successfully.",
          }),
        });

        loadPageSamples();
        onProgressUpdate?.();
      },
      (error) => {
        if (!componentMounted.current) {
          return;
        }

        setLoading(false);
        console.error("Error completing formulation:", error);

        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          }),
          subtitle: intl.formatMessage({
            id: "virology.formulation.complete.error",
            defaultMessage: "Failed to complete formulation. Please try again.",
          }),
        });
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    notify,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const additionalColumns = useMemo(
    () => [
      {
        key: "batchNumber",
        header: intl.formatMessage({
          id: "virology.formulation.batchNumber",
          defaultMessage: "Batch Number",
        }),
        render: (value, sample) => {
          const batchNum =
            value || sample?.batchNumber || sample?.data?.batchNumber;
          return batchNum || "-";
        },
      },
      {
        key: "stabilizers",
        header: intl.formatMessage({
          id: "virology.formulation.stabilizers",
          defaultMessage: "Stabilizers",
        }),
        render: (value, sample) => {
          const stab =
            value || sample?.stabilizers || sample?.data?.stabilizers;
          return stab || "-";
        },
      },
      {
        key: "preservatives",
        header: intl.formatMessage({
          id: "virology.formulation.preservatives",
          defaultMessage: "Preservatives",
        }),
        render: (value, sample) => {
          const pres =
            value || sample?.preservatives || sample?.data?.preservatives;
          return pres || "-";
        },
      },
      {
        key: "virusConcentration",
        header: intl.formatMessage({
          id: "virology.formulation.virusConcentration",
          defaultMessage: "Virus Concentration",
        }),
        render: (value, sample) => {
          const conc =
            value ||
            sample?.virusConcentration ||
            sample?.data?.virusConcentration;
          return conc || "-";
        },
      },
      {
        key: "bufferComposition",
        header: intl.formatMessage({
          id: "virology.formulation.bufferComposition",
          defaultMessage: "Buffer Composition",
        }),
        render: (value, sample) => {
          const buffer =
            value ||
            sample?.bufferComposition ||
            sample?.data?.bufferComposition;
          return buffer || "-";
        },
      },
    ],
    [intl],
  );

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (sample) =>
          sample.pageStatus === "PENDING" ||
          sample.pageStatus === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((sample) => sample.pageStatus === "COMPLETED"),
    [samples],
  );

  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  if (loading) {
    return (
      <div className="notebook-page-loading">
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="notebook-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="notebook-page-header">
            <h3>
              <FormattedMessage
                id="virology.formulation.title"
                defaultMessage="Formulation - Prepare Viral Product"
              />
            </h3>
            <p className="notebook-page-description">
              <FormattedMessage
                id="virology.formulation.description"
                defaultMessage="Document formulation details including stabilizers, preservatives, and concentrations for viral product preparation."
              />
            </p>
          </Tile>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <div className="notebook-actions-bar">
            <Button
              kind="primary"
              renderIcon={Save}
              onClick={() => setModalOpen(true)}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="virology.formulation.logData"
                defaultMessage="Log Formulation Data"
              />
            </Button>
            <Button
              kind="secondary"
              renderIcon={Checkmark}
              onClick={handleCompleteFormulation}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="virology.formulation.complete"
                defaultMessage="Complete Formulation"
              />
            </Button>
            {selectedSampleIds.length > 0 && (
              <Tag type="blue">
                <FormattedMessage
                  id="virology.formulation.selectedCount"
                  defaultMessage="{count} sample(s) selected"
                  values={{ count: selectedSampleIds.length }}
                />
              </Tag>
            )}
          </div>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <div className="sample-table-section">
            <div className="table-section-header">
              <h5>
                <FormattedMessage
                  id="virology.formulation.pendingSamples.title"
                  defaultMessage="Pending / In Progress"
                />
                <Tag type="gray" size="sm" className="count-tag">
                  {pendingCount}
                </Tag>
              </h5>
            </div>
            <SampleGrid
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              additionalColumns={additionalColumns}
              pageType="formulation"
            />
          </div>

          <div className="sample-table-section">
            <div className="table-section-header">
              <h5>
                <FormattedMessage
                  id="virology.formulation.completedSamples.title"
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
        </Column>
      </Grid>

      <Modal
        open={modalOpen}
        onRequestClose={() => {
          setModalOpen(false);
          resetForm();
        }}
        onRequestSubmit={handleSaveFormulation}
        modalHeading={intl.formatMessage({
          id: "virology.formulation.modal.title",
          defaultMessage: "Log Formulation Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "button.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        size="md"
      >
        <div className="formulation-modal-content">
          <TextInput
            id="batchNumber"
            labelText={intl.formatMessage({
              id: "virology.formulation.batchNumber",
              defaultMessage: "Batch Number",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.batchNumber.placeholder",
              defaultMessage: "Enter batch number",
            })}
            value={batchNumber}
            onChange={(e) => setBatchNumber(e.target.value)}
          />

          <TextInput
            id="stabilizers"
            labelText={intl.formatMessage({
              id: "virology.formulation.stabilizers",
              defaultMessage: "Stabilizers",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.stabilizers.placeholder",
              defaultMessage: "Enter stabilizers used",
            })}
            value={stabilizers}
            onChange={(e) => setStabilizers(e.target.value)}
          />

          <TextInput
            id="preservatives"
            labelText={intl.formatMessage({
              id: "virology.formulation.preservatives",
              defaultMessage: "Preservatives",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.preservatives.placeholder",
              defaultMessage: "Enter preservatives used",
            })}
            value={preservatives}
            onChange={(e) => setPreservatives(e.target.value)}
          />

          <TextInput
            id="virusConcentration"
            labelText={intl.formatMessage({
              id: "virology.formulation.virusConcentration",
              defaultMessage: "Virus Concentration",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.virusConcentration.placeholder",
              defaultMessage:
                "Enter virus concentration (e.g., 10^6 TCID50/mL)",
            })}
            value={virusConcentration}
            onChange={(e) => setVirusConcentration(e.target.value)}
          />

          <TextInput
            id="bufferComposition"
            labelText={intl.formatMessage({
              id: "virology.formulation.bufferComposition",
              defaultMessage: "Buffer Composition",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.bufferComposition.placeholder",
              defaultMessage: "Enter buffer composition details",
            })}
            value={bufferComposition}
            onChange={(e) => setBufferComposition(e.target.value)}
          />

          <TextArea
            id="formulationNotes"
            labelText={intl.formatMessage({
              id: "virology.formulation.notes",
              defaultMessage: "Formulation Notes",
            })}
            placeholder={intl.formatMessage({
              id: "virology.formulation.notes.placeholder",
              defaultMessage: "Enter any additional formulation notes",
            })}
            value={formulationNotes}
            onChange={(e) => setFormulationNotes(e.target.value)}
            rows={4}
          />
        </div>
      </Modal>
    </div>
  );
}

export default VirologyFormulationPage;
