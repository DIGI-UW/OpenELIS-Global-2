import { useState, useEffect, useRef, useCallback } from "react";
import { Tabs, TabList, Tab, TabPanels, TabPanel } from "@carbon/react";
import {
  Bacteria,
  Microscope,
  Chemistry,
  Medication,
} from "@carbon/react/icons";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import CultureResultsPanel from "./panels/CultureResultsPanel";
import SmearMicroscopyPanel from "./panels/SmearMicroscopyPanel";
import GeneXpertPanel from "./panels/GeneXpertPanel";
import DSTPanel from "./panels/DSTPanel";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBTestExecutionPage - Page 5 of the TB workflow.
 *
 * Provides tabbed interface for all TB diagnostic test types:
 * - Smear Microscopy (AFB): Record acid-fast bacilli smear results
 * - Culture Results: View finalized culture results from Incubation Monitoring
 * - GeneXpert: Molecular PCR testing (coming soon)
 * - DST: Drug Susceptibility Testing (coming soon)
 */
function TBTestExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const componentMounted = useRef(true);
  const [selectedTabIndex, setSelectedTabIndex] = useState(0);
  const [cultureSamples, setCultureSamples] = useState([]);
  const [cultureSamplesLoading, setCultureSamplesLoading] = useState(true);

  // Load culture samples with results (shared between Culture Results and Smear tabs)
  const loadCultureSamples = useCallback(() => {
    if (!entryId) {
      setCultureSamples([]);
      setCultureSamplesLoading(false);
      return;
    }

    setCultureSamplesLoading(true);
    const resultTypes = ["POSITIVE", "NEGATIVE", "CONTAMINATED"];
    const allResults = [];
    let completedRequests = 0;

    const transformSample = (sample) => ({
      id: String(sample.id),
      sampleItemId: String(sample.sampleItem?.id || sample.sampleItemId),
      externalId: sample.sampleItem?.externalId,
      accessionNumber: sample.sampleItem?.sample?.accessionNumber,
      cultureMethod: sample.cultureMethod || "LJ",
      cultureResult: sample.cultureResult,
      positiveWeek: sample.positiveWeek,
      finalResultDate: sample.finalResultDate,
      inoculationDate: sample.inoculationDate,
    });

    resultTypes.forEach((resultType) => {
      getFromOpenElisServer(
        `/rest/tb/incubation/samples/by-result/${resultType}?entryId=${entryId}`,
        (response) => {
          if (componentMounted.current) {
            if (response && Array.isArray(response)) {
              response.forEach((sample) => {
                allResults.push(transformSample(sample));
              });
            }
            completedRequests++;
            if (completedRequests === resultTypes.length) {
              const uniqueResults = Array.from(
                new Map(allResults.map((s) => [s.id, s])).values(),
              );
              setCultureSamples(uniqueResults);
              setCultureSamplesLoading(false);
            }
          }
        },
      );
    });
  }, [entryId]);

  useEffect(() => {
    componentMounted.current = true;
    loadCultureSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [loadCultureSamples]);

  return (
    <div className="tb-test-execution-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.testExecution.title"
            defaultMessage="Assay/Test Execution"
          />
          <span
            style={{
              fontSize: "0.875rem",
              fontWeight: "normal",
              color: "#525252",
              marginLeft: "1rem",
            }}
          >
            {progress?.completed || 0}/{progress?.total || 0}{" "}
            <FormattedMessage
              id="notebook.page.tb.samplesCompleted"
              defaultMessage="samples completed"
            />
          </span>
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.testExecution.description"
            defaultMessage="Execute requested tests including culture, smear microscopy, GeneXpert, identification, and drug susceptibility testing. Record test results and methods used."
          />
        </p>
      </div>

      {/* Tabbed Interface */}
      <Tabs
        selectedIndex={selectedTabIndex}
        onChange={({ selectedIndex }) => setSelectedTabIndex(selectedIndex)}
      >
        <TabList aria-label="TB Test Types" contained>
          <Tab renderIcon={Bacteria}>
            <FormattedMessage
              id="notebook.page.tb.tab.culture"
              defaultMessage="Culture Results"
            />
          </Tab>
          <Tab renderIcon={Microscope}>
            <FormattedMessage
              id="notebook.page.tb.tab.smear"
              defaultMessage="Smear Microscopy"
            />
          </Tab>
          <Tab renderIcon={Chemistry}>
            <FormattedMessage
              id="notebook.page.tb.tab.genexpert"
              defaultMessage="GeneXpert"
            />
          </Tab>
          <Tab renderIcon={Medication}>
            <FormattedMessage
              id="notebook.page.tb.tab.dst"
              defaultMessage="DST"
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Culture Results Panel */}
          <TabPanel>
            <CultureResultsPanel
              cultureSamples={cultureSamples}
              loading={cultureSamplesLoading}
              onRefresh={loadCultureSamples}
            />
          </TabPanel>

          {/* Smear Microscopy Panel */}
          <TabPanel>
            <SmearMicroscopyPanel
              pageData={pageData}
              onProgressUpdate={onProgressUpdate}
              cultureSamples={cultureSamples}
            />
          </TabPanel>

          {/* GeneXpert Panel */}
          <TabPanel>
            <GeneXpertPanel
              pageData={pageData}
              onProgressUpdate={onProgressUpdate}
              cultureSamples={cultureSamples}
            />
          </TabPanel>

          {/* DST Panel */}
          <TabPanel>
            <DSTPanel
              pageData={pageData}
              onProgressUpdate={onProgressUpdate}
              cultureSamples={cultureSamples}
            />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
}

export default TBTestExecutionPage;
