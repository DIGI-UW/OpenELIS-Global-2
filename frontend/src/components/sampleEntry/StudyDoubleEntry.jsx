import React, { useState, useEffect, useContext, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Form,
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
} from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  validateEntireForm,
  formatValidationErrors,
} from "./utils/formValidation";
import { normalizeTimeTo24Hour } from "./utils/formHelpers";
import PatientInfoSection from "./sections/PatientInfoSection";
import SampleInfoSection from "./sections/SampleInfoSection";
import ProjectSelectionSection from "./sections/ProjectSelectionSection";
import TestSelectionSection from "./sections/TestSelectionSection";
import ARVSection from "./sections/ARVSection";
import EIDSection from "./sections/EIDSection";
import SpecialRequestSection from "./sections/SpecialRequestSection";
import RTNSection from "./sections/RTNSection";
import HPVSection from "./sections/HPVSection";
import IndeterminateSection from "./sections/IndeterminateSection";
import RecencySection from "./sections/RecencySection";
import VLSection from "./sections/VLSection";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.sampleCreate", link: "#" },
  { label: "banner.menu.createDouble", link: "/StudyDoubleEntry" },
];

const StudyDoubleEntry = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    // Patient Information
    subjectNumber: "",
    siteSubjectNumber: "",
    upidCode: "",
    gender: "",
    birthDateForDisplay: "",
    patientPK: "",
    patientFhirUuid: "",

    // Sample Information
    labNo: "",
    receivedDateForDisplay: "",
    receivedTimeForDisplay: "",
    interviewDate: "",
    interviewTime: "",
    currentDate: "",

    // Project Selection
    project: "",

    // Observations (patient observations like HIV status, under investigation, etc.)
    observations: {
      projectFormName: "",
      nameOfDoctor: "",
      hivStatus: "",
      underInvestigation: "",

      // Special Request specific observations
      reasonForRequest: "",

      // EID specific observations
      whichPCR: "",
      reasonForSecondPCRTest: "",
      nameOfRequestor: "",
      nameOfSampler: "",
      eidInfantPTME: "",
      eidTypeOfClinic: "",
      eidTypeOfClinicOther: "",
      eidHowChildFed: "",
      eidStoppedBreastfeeding: "",
      eidInfantSymptomatic: "",
      eidInfantsARV: "",
      eidInfantCotrimoxazole: "",
      eidMothersHIVStatus: "",
      eidMothersARV: "",

      // Indeterminate specific observations
      indFirstTestDate: "",
      indFirstTestName: "",
      indFirstTestResult: "",
      indSecondTestDate: "",
      indSecondTestName: "",
      indSecondTestResult: "",
      indSiteFinalResult: "",

      // Recency Testing specific observations
      vlPregnancy: "",
      vlSuckle: "",

      // HPV specific observations
      hpvSamplingMethod: "",

      // VL (Viral Load) specific observations
      currentARVTreatment: "",
      arvTreatmentInitDate: "",
      arvTreatmentRegime: "",
      currentARVTreatmentINNsList: [],
      vlReasonForRequest: "",
      vlOtherReasonForRequest: "",
      initcd4Count: "",
      initcd4Percent: "",
      initcd4Date: "",
      demandcd4Count: "",
      demandcd4Percent: "",
      demandcd4Date: "",
      vlBenefit: "",
      priorVLLab: "",
      priorVLValue: "",
      priorVLDate: "",
    },

    // Project Data (tests and study-specific fields)
    projectData: {
      // ARV specific
      arvcenterName: "",
      arvcenterCode: "",
      doctor: "",

      // EID specific
      eidsiteName: "",
      eidsiteCode: "",
      dbsInfantNumber: "",
      dbsSiteInfantNumber: "",
      eidWhichPCR: "",
      eidSecondPCRReason: "",
      requester: "",

      // Indeterminate specific
      indsiteName: "",
      indsiteCode: "",

      // Test selections
      dryTubeTaken: false,
      edtaTubeTaken: false,
      dbsTaken: false,
      dbsvlTaken: false,
      pscvlTaken: false,
      plasmaTaken: false,
      serumTaken: false,

      // HIV Tests
      serologyHIVTest: false,
      murexTest: false,
      integralTest: false,
      genscreenTest: false,
      genieIITest: false,
      vironostikaTest: false,
      genieII100Test: false,
      genieII10Test: false,
      wb1Test: false,
      wb2Test: false,
      p24AgTest: false,
      innoliaTest: false,

      // Other Tests
      cd4cd8Test: false,
      cd4CountTest: false,
      cd3CountTest: false,
      viralLoadTest: false,
      genotypingTest: false,
      dnaPCR: false,

      // Chemistry Tests
      glycemiaTest: false,
      creatinineTest: false,
      transaminaseTest: false,
      transaminaseALTLTest: false,
      transaminaseASTLTest: false,

      // Hematology Tests
      nfsTest: false,
      gbTest: false,
      neutTest: false,
      lymphTest: false,
      monoTest: false,
      eoTest: false,
      basoTest: false,
      grTest: false,
      hbTest: false,
      hctTest: false,
      vgmTest: false,
      tcmhTest: false,
      ccmhTest: false,
      plqTest: false,

      // HPV specific
      hpvTest: false,
      preservCytTaken: false,
      abbottOrRocheAnalysis: false,
      geneXpertAnalysis: false,
      selfCollection: false,
      collectionDoneByHealthWorker: false,

      // Special Request
      address: "",
      phoneNumber: "",
      faxNumber: "",
      email: "",
      reasonForRequest: "",

      // Under Investigation Note
      underInvestigationNote: "",
    },
  });

  const [displayLists, setDisplayLists] = useState({
    genders: [],
    organizationTypeLists: {},
    dictionaryLists: {},
    formLists: {},
  });

  const [selectedProject, setSelectedProject] = useState("");

  useEffect(() => {
    loadDoubleData();
  }, []);

  const loadDoubleData = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/SampleEntryByProject?type=verify",
      (response) => {
        if (response) {
          const currentDate =
            response.currentDate ||
            response.receivedDateForDisplay ||
            response.interviewDate ||
            "";
          setFormData((prev) => ({
            ...prev,
            currentDate: currentDate,
            receivedDateForDisplay:
              response.receivedDateForDisplay || currentDate,
            interviewDate: response.interviewDate || currentDate,
          }));

          // Set display lists
          setDisplayLists({
            genders: response.genders || [],
            organizationTypeLists: response.organizationTypeLists || {},
            dictionaryLists: response.dictionaryLists || {},
            formLists: response.formLists || {},
          });
        } else {
          // Handle error case
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "error.loading.data",
              defaultMessage: "Error loading form data",
            }),
          });
          setNotificationVisible(true);
        }
        setLoading(false);
      },
    );
  };

  const handleInputChange = useCallback((field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  }, []);

  const handleProjectDataChange = useCallback((field, value) => {
    setFormData((prev) => ({
      ...prev,
      projectData: {
        ...prev.projectData,
        [field]: value,
      },
    }));
  }, []);

  const handleObservationChange = useCallback((field, value) => {
    setFormData((prev) => ({
      ...prev,
      observations: {
        ...prev.observations,
        [field]: value,
      },
    }));
  }, []);

  const handleProjectChange = useCallback(
    (projectId) => {
      const projectFormNameMap = {
        ARV_INITIAL: "InitialARV_Id",
        ARV_FOLLOWUP: "FollowUpARV_Id",
        ARV_VIRAL_LOAD: "VL_Id",
        RTN: "RTN_Id",
        EID: "EID_Id",
        INDETERMINATE: "Indeterminate_Id",
        SPECIAL_REQUEST: "Special_Request_Id",
        RECENCY_TESTING: "Recency_Id",
        HPV_TESTING: "HPV_Id",
      };
      const projectFormName = projectFormNameMap[projectId] || "";
      setSelectedProject(projectId);
      setFormData((prev) => ({
        ...prev,
        project: projectId,
        observations: {
          ...prev.observations,
          projectFormName,
        },
      }));
    },
    [setFormData],
  );

  const validateForm = () => {
    // Use comprehensive validation utility
    const validationErrors = validateEntireForm(formData, selectedProject);

    if (validationErrors.length > 0) {
      // Format errors for display
      return validationErrors.map((error) => error.message);
    }

    return [];
  };

  const handleSave = async (event) => {
    event.preventDefault();
    if (saving) {
      return;
    }

    const errors = validateForm();
    if (errors.length > 0) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: errors.join(", "),
      });
      setNotificationVisible(true);
      return;
    }

    const projectPrefixMap = {
      ARV_INITIAL: "LARC",
      ARV_FOLLOWUP: "LARC",
      ARV_VIRAL_LOAD: "LARC",
      RTN: "LRTN",
      EID: "LDBS",
      INDETERMINATE: "LIND",
      SPECIAL_REQUEST: "LSPE",
      RECENCY_TESTING: "RTRI",
      HPV_TESTING: "HPVT",
    };
    const projectKey = formData.project || selectedProject;
    const prefix = projectPrefixMap[projectKey] || "";
    const trimmedLabNo = (formData.labNo || "").trim();
    let normalizedLabNo = trimmedLabNo.toUpperCase();
    if (prefix) {
      const programPattern = new RegExp(`^${prefix}\\d{5}$`);
      if (programPattern.test(normalizedLabNo)) {
        // already valid format
      } else if (/^\d{5}$/.test(trimmedLabNo)) {
        normalizedLabNo = `${prefix}${trimmedLabNo}`;
      } else {
        const digitsOnly = trimmedLabNo.replace(/\D/g, "");
        if (digitsOnly.length >= 5) {
          normalizedLabNo = `${prefix}${digitsOnly.slice(-5)}`;
        }
      }
    }

    setSaving(true);
    try {
      const normalizedReceivedTime = normalizeTimeTo24Hour(
        formData.receivedTimeForDisplay,
      );
      const normalizedInterviewTime = normalizeTimeTo24Hour(
        formData.interviewTime,
      );
      const rawResponse = await fetch(
        "/api/OpenELIS-Global/rest/SampleEntryByProject?type=verify",
        {
          credentials: "include",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify({
            ...formData,
            labNo: normalizedLabNo,
            receivedTimeForDisplay: normalizedReceivedTime,
            interviewTime: normalizedInterviewTime,
          }),
        },
      );

      const responseBody = await rawResponse.json().catch(() => ({}));

      if (rawResponse.ok && responseBody.success !== false) {
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "success.save.study.entry",
            defaultMessage: "Study entry saved successfully",
          }),
        });
        setNotificationVisible(true);

        // Redirect or reset form
        setTimeout(() => {
          window.location.href = "/StudyDoubleEntry?type=verify";
        }, 2000);
      } else {
        const serverMessage =
          responseBody.message ||
          intl.formatMessage({
            id: "error.save.study.entry",
            defaultMessage: "Error saving study entry",
          });
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: serverMessage,
        });
        setNotificationVisible(true);
      }
    } catch (error) {
      console.error("Error saving study entry:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "error.save.study.entry",
          defaultMessage: "Error saving study entry",
        }),
      });
      setNotificationVisible(true);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="sample.entry.study.verify.title"
                defaultMessage="Study - Double Entry"
              />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Form onSubmit={handleSave}>
          <Grid fullWidth={true}>
            {/* Project Selection Section - Always visible first */}
            <Column lg={16} md={8} sm={4}>
              <ProjectSelectionSection
                selectedProject={selectedProject}
                onProjectChange={handleProjectChange}
                formLists={displayLists.formLists}
              />
            </Column>

            {/* Show form fields only after project is selected */}
            {selectedProject && (
              <>
                {/* ORDER MATCHES OLD JSP: 1. Project-specific fields, 2. Sample info, 3. Patient info, 4. Tests */}

                {/* STEP 1: Project-Specific Information (ARV Center, EID Site, etc.) */}
                {(selectedProject.includes("ARV") ||
                  selectedProject.includes("Initial") ||
                  selectedProject.includes("Follow")) &&
                  !selectedProject.includes("VL") &&
                  selectedProject !== "ARV_VIRAL_LOAD" && (
                    <Column lg={16} md={8} sm={4}>
                      <ARVSection
                        projectData={formData.projectData}
                        observations={formData.observations}
                        onInputChange={handleProjectDataChange}
                        onObservationChange={handleObservationChange}
                        organizationLists={displayLists.organizationTypeLists}
                        dictionaryLists={displayLists.dictionaryLists}
                        birthDate={formData.birthDateForDisplay}
                        selectedProject={selectedProject}
                      />
                    </Column>
                  )}

                {selectedProject.includes("EID") && (
                  <Column lg={16} md={8} sm={4}>
                    <EIDSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {selectedProject.includes("RTN") && (
                  <Column lg={16} md={8} sm={4}>
                    <RTNSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {(selectedProject.includes("Indeterminate") ||
                  selectedProject === "INDETERMINATE") && (
                  <Column lg={16} md={8} sm={4}>
                    <IndeterminateSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {(selectedProject.includes("HPV") ||
                  selectedProject === "HPV_TESTING") && (
                  <Column lg={16} md={8} sm={4}>
                    <HPVSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {(selectedProject.includes("Recency") ||
                  selectedProject === "RECENCY_TESTING") && (
                  <Column lg={16} md={8} sm={4}>
                    <RecencySection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                      gender={formData.gender}
                    />
                  </Column>
                )}

                {(selectedProject.includes("VL") ||
                  selectedProject === "ARV_VIRAL_LOAD") && (
                  <Column lg={16} md={8} sm={4}>
                    <VLSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                      gender={formData.gender}
                    />
                  </Column>
                )}

                {(selectedProject.includes("Special") ||
                  selectedProject === "SPECIAL_REQUEST") && (
                  <Column lg={16} md={8} sm={4}>
                    <SpecialRequestSection
                      projectData={formData.projectData}
                      observations={formData.observations}
                      onInputChange={handleProjectDataChange}
                      onObservationChange={handleObservationChange}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {/* STEP 2: Sample Information (Received Date/Time, Interview Date/Time) */}
                <Column lg={16} md={8} sm={4}>
                  <SampleInfoSection
                    formData={formData}
                    onInputChange={handleInputChange}
                  />
                </Column>

                {/* STEP 3: Patient Information (Subject Number, Lab No, Gender, DOB, Age) */}
                <Column lg={16} md={8} sm={4}>
                  <PatientInfoSection
                    formData={formData}
                    onInputChange={handleInputChange}
                    genders={displayLists.genders}
                    selectedProject={selectedProject}
                  />
                </Column>

                {/* STEP 4: Test Selection (Sample Types and Tests) */}
                <Column lg={16} md={8} sm={4}>
                  <TestSelectionSection
                    projectData={formData.projectData}
                    onTestChange={handleProjectDataChange}
                    selectedProject={selectedProject}
                  />
                </Column>

                {/* Action Buttons */}
                <Column lg={16} md={8} sm={4}>
                  <Section>
                    <div
                      style={{
                        display: "flex",
                        gap: "1rem",
                        marginTop: "2rem",
                      }}
                    >
                      <Button type="submit" disabled={saving}>
                        {saving ? (
                          <FormattedMessage
                            id="label.button.saving"
                            defaultMessage="Saving..."
                          />
                        ) : (
                          <FormattedMessage
                            id="label.button.save"
                            defaultMessage="Save"
                          />
                        )}
                      </Button>
                      <Button
                        kind="secondary"
                        onClick={() =>
                          (window.location.href =
                            "/StudyDoubleEntry?type=verify")
                        }
                      >
                        <FormattedMessage
                          id="label.button.cancel"
                          defaultMessage="Cancel"
                        />
                      </Button>
                    </div>
                  </Section>
                </Column>
              </>
            )}
          </Grid>
        </Form>
      </div>

      {notificationVisible && <AlertDialog />}
    </>
  );
};

export default StudyDoubleEntry;
