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
  Dropdown,
  DatePicker,
  DatePickerInput,
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
 * VirologyCellCulturePage - Page 3 of the Virology & Vaccine Unit workflow.
 * Grow host cells and track cell line, passage number, growth conditions.
 *
 * Features:
 * - Display samples from previous page (Media Preparation)
 * - Log cell culture parameters: cell line, passage number, growth conditions
 * - Track incubation temperature, CO2 percentage, seeding density
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyCellCulturePage({
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

  // Form data for cell culture modal
  const [cellLine, setCellLine] = useState("");
  const [passageNumber, setPassageNumber] = useState("");
  const [seedingDensity, setSeedingDensity] = useState("");
  const [flaskType, setFlaskType] = useState(null);
  const [mediaVolume, setMediaVolume] = useState("");
  const [incubationTemp, setIncubationTemp] = useState("");
  const [co2Percentage, setCo2Percentage] = useState("");
  const [humidityPercentage, setHumidityPercentage] = useState("");
  const [seedingDate, setSeedingDate] = useState("");
  const [notes, setNotes] = useState("");

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
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Previous page data
              testType: sample.data?.testType,
              source: sample.data?.source,
              mediaName: sample.data?.mediaName,
              batchNumber: sample.data?.batchNumber,
              sterilization: sample.data?.sterilization,
              // Cell culture data
              cellLine: sample.data?.cellLine,
              passageNumber: sample.data?.passageNumber,
              seedingDensity: sample.data?.seedingDensity,
              flaskType: sample.data?.flaskType,
              mediaVolume: sample.data?.mediaVolume,
              incubationTemp: sample.data?.incubationTemp,
              co2Percentage: sample.data?.co2Percentage,
              humidityPercentage: sample.data?.humidityPercentage,
              seedingDate: sample.data?.seedingDate,
              cellCultureNotes: sample.data?.cellCultureNotes,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Flask type options
  const flaskTypeOptions = [
    {
      id: "t25",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.t25",
        defaultMessage: "T25 Flask",
      }),
    },
    {
      id: "t75",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.t75",
        defaultMessage: "T75 Flask",
      }),
    },
    {
      id: "t175",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.t175",
        defaultMessage: "T175 Flask",
      }),
    },
    {
      id: "6-well",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.6well",
        defaultMessage: "6-Well Plate",
      }),
    },
    {
      id: "12-well",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.12well",
        defaultMessage: "12-Well Plate",
      }),
    },
    {
      id: "24-well",
      text: intl.formatMessage({
        id: "virology.cellculture.flask.24well",
        defaultMessage: "24-Well Plate",
      }),
    },
  ];

  // Handle save cell culture data
  const handleSaveCellCulture = useCallback(() => {
    // Validation
    if (!cellLine) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter a cell line",
      });
      return;
    }

    if (!passageNumber) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter a passage number",
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
      cellLine,
      passageNumber: parseInt(passageNumber, 10),
      seedingDensity,
      flaskType: flaskType?.id || flaskType?.text || "",
      mediaVolume: mediaVolume ? parseFloat(mediaVolume) : null,
      incubationTemp: incubationTemp ? parseFloat(incubationTemp) : null,
      co2Percentage: co2Percentage ? parseFloat(co2Percentage) : null,
      humidityPercentage: humidityPercentage
        ? parseFloat(humidityPercentage)
        : null,
      seedingDate,
      notes,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/cell-culture",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response && response.success) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Cell culture data logged successfully. ${samplesUpdated} sample(s) updated.`,
          });

          // Clear form and selection
          setCellLine("");
          setPassageNumber("");
          setSeedingDensity("");
          setFlaskType(null);
          setMediaVolume("");
          setIncubationTemp("");
          setCo2Percentage("");
          setHumidityPercentage("");
          setSeedingDate("");
          setNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorMessage =
            response?.error || response?.message || "Unknown error";
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: errorMessage,
          });
        }
      },
    );
  }, [
    cellLine,
    passageNumber,
    seedingDensity,
    flaskType,
    mediaVolume,
    incubationTemp,
    co2Percentage,
    humidityPercentage,
    seedingDate,
    notes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing cell culture and moving samples to next page (Quality Control)
  const handleCompleteCellCulture = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.cellculture.error.noSelection",
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
          id: "virology.cellculture.error.noPage",
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
                id: "virology.cellculture.success.completed",
                defaultMessage:
                  "Completed cell culture for {count} sample(s). They will proceed to Quality Control.",
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
              id: "virology.cellculture.error.complete",
              defaultMessage:
                "Failed to complete cell culture. Please try again.",
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

  // Custom columns for cell culture display
  const getAdditionalColumns = (intl) => [
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
      key: "cellLine",
      header: intl.formatMessage({
        id: "virology.cellculture.column.cellLine",
        defaultMessage: "Cell Line",
      }),
      render: (value, sample) => {
        const cellLine = value || sample?.cellLine || sample?.data?.cellLine;
        return cellLine || "-";
      },
    },
    {
      key: "passageNumber",
      header: intl.formatMessage({
        id: "virology.cellculture.column.passage",
        defaultMessage: "Passage #",
      }),
      render: (value, sample) => {
        const passage =
          value || sample?.passageNumber || sample?.data?.passageNumber;
        return passage ? String(passage) : "-";
      },
    },
    {
      key: "growthConditions",
      header: intl.formatMessage({
        id: "virology.cellculture.column.conditions",
        defaultMessage: "Growth Conditions",
      }),
      render: (value, sample) => {
        const temp = sample?.incubationTemp || sample?.data?.incubationTemp;
        const co2 = sample?.co2Percentage || sample?.data?.co2Percentage;
        const humidity =
          sample?.humidityPercentage || sample?.data?.humidityPercentage;

        const conditions = [];
        if (temp) conditions.push(`${temp}°C`);
        if (co2) conditions.push(`${co2}% CO₂`);
        if (humidity) conditions.push(`${humidity}% humidity`);

        return conditions.length > 0 ? conditions.join(", ") : "-";
      },
    },
  ];

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  return (
    <div className="virology-cell-culture-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.cellculture.title"
            defaultMessage="Cell Culture - Host Cell Growth"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.cellculture.description"
            defaultMessage="Grow host cells and track cell line, passage number, and growth conditions (temperature, CO₂, humidity)."
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
                  id="virology.cellculture.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="virology.cellculture.pendingInProgress"
                  defaultMessage="Pending / In Progress"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="virology.cellculture.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
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
            id="virology.cellculture.logCulture"
            defaultMessage="Log Cell Culture Data"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteCellCulture}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.cellculture.complete"
            defaultMessage="Complete Cell Culture ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.cellculture.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="virology.cellculture.pendingSamples.description"
              defaultMessage="Samples ready for cell culture. Select samples and log cell culture parameters."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="virology.cellculture.pendingSamples.empty"
                  defaultMessage="No pending samples. Complete media preparation on Page 2 first."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-cell-culture"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.cellculture.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="virology.cellculture.completedSamples.description"
              defaultMessage="Samples with completed cell culture data."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="virology.cellculture.completedSamples.empty"
                  defaultMessage="No completed samples yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="completed-cell-culture"
              samples={completedSamples}
              selectedIds={[]}
              showSelection={false}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Cell Culture Data Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.cellculture.modal.title"
            defaultMessage="Log Cell Culture Data"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.cellculture.modal.subtitle"
            defaultMessage="Track cell line, passage number, and growth conditions"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.cellculture.save"
            defaultMessage="Save Cell Culture Data"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveCellCulture}
        primaryButtonDisabled={loading || !cellLine || !passageNumber}
        size="lg"
      >
        <Grid fullWidth>
          {/* Cell Line */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="cell-line"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.cellculture.cellLine"
                    defaultMessage="Cell Line"
                  />
                  {" *"}
                </span>
              }
              value={cellLine}
              onChange={(e) => setCellLine(e.target.value)}
              placeholder="e.g., Vero, MDCK, HEK293"
              invalid={!cellLine}
              invalidText="Required"
            />
          </Column>

          {/* Passage Number */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="passage-number"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.cellculture.passageNumber"
                    defaultMessage="Passage Number"
                  />
                  {" *"}
                </span>
              }
              value={passageNumber}
              onChange={(e) => setPassageNumber(e.target.value)}
              placeholder="e.g., 15"
              type="number"
              invalid={!passageNumber}
              invalidText="Required"
            />
          </Column>

          {/* Seeding Density */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="seeding-density"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.seedingDensity"
                  defaultMessage="Seeding Density"
                />
              }
              value={seedingDensity}
              onChange={(e) => setSeedingDensity(e.target.value)}
              placeholder="e.g., 1x10^6 cells/mL"
            />
          </Column>

          {/* Flask Type */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="flask-type"
              titleText={
                <FormattedMessage
                  id="virology.cellculture.flaskType"
                  defaultMessage="Flask/Plate Type"
                />
              }
              label="Select flask or plate..."
              items={flaskTypeOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={flaskType}
              onChange={({ selectedItem }) => setFlaskType(selectedItem)}
            />
          </Column>

          {/* Media Volume */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="media-volume"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.mediaVolume"
                  defaultMessage="Media Volume (mL)"
                />
              }
              value={mediaVolume}
              onChange={(e) => setMediaVolume(e.target.value)}
              placeholder="e.g., 10"
              type="number"
            />
          </Column>

          {/* Incubation Temperature */}
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="incubation-temp"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.temperature"
                  defaultMessage="Temperature (°C)"
                />
              }
              value={incubationTemp}
              onChange={(e) => setIncubationTemp(e.target.value)}
              placeholder="e.g., 37"
              type="number"
            />
          </Column>

          {/* CO2 Percentage */}
          <Column lg={3} md={4} sm={4}>
            <TextInput
              id="co2-percentage"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.co2"
                  defaultMessage="CO₂ (%)"
                />
              }
              value={co2Percentage}
              onChange={(e) => setCo2Percentage(e.target.value)}
              placeholder="e.g., 5"
              type="number"
            />
          </Column>

          {/* Humidity Percentage */}
          <Column lg={3} md={4} sm={4}>
            <TextInput
              id="humidity-percentage"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.humidity"
                  defaultMessage="Humidity (%)"
                />
              }
              value={humidityPercentage}
              onChange={(e) => setHumidityPercentage(e.target.value)}
              placeholder="e.g., 95"
              type="number"
            />
          </Column>

          {/* Seeding Date */}
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={seedingDate}
              onChange={(dates) =>
                setSeedingDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="seeding-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <FormattedMessage
                    id="virology.cellculture.seedingDate"
                    defaultMessage="Seeding Date"
                  />
                }
              />
            </DatePicker>
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="cell-culture-notes"
              labelText={
                <FormattedMessage
                  id="virology.cellculture.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Add any observations about cell morphology, confluence, or other details..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={4}
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

export default VirologyCellCulturePage;
