import React, { useState, useEffect, useContext } from "react";
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
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  validateEntireForm,
  formatValidationErrors,
} from "./utils/formValidation";
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

    // Project Data (tests and study-specific fields)
    projectData: {
      // ARV specific
      ARVcenterName: "",
      ARVcenterCode: "",
      doctor: "",

      // EID specific
      EIDsiteName: "",
      EIDsiteCode: "",
      dbsInfantNumber: "",
      dbsSiteInfantNumber: "",
      eidWhichPCR: "",
      eidSecondPCRReason: "",
      requester: "",

      // RTN specific
      RTNsiteName: "",
      RTNsiteCode: "",
      rtnReferenceNumber: "",
      rtnNotes: "",

      // Indeterminate specific
      INDsiteName: "",
      INDsiteCode: "",
      indeterminateContext: "",

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
      WB1Test: false,
      WB2Test: false,
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

  const loadDoubleData = async () => {
    setLoading(true);
    try {
      const response = await getFromOpenElisServer(
        "/rest/SampleEntryByProject?type=verify",
      );

      if (response) {
        // Set current date
        const currentDate = new Date()
          .toISOString()
          .split("T")[0]
          .split("-")
          .reverse()
          .join("/");
        setFormData((prev) => ({
          ...prev,
          currentDate: currentDate,
          receivedDateForDisplay: currentDate,
          interviewDate: currentDate,
        }));

        // Set display lists
        setDisplayLists({
          genders: response.genders || [],
          organizationTypeLists: response.organizationTypeLists || {},
          dictionaryLists: response.dictionaryLists || {},
          formLists: response.formLists || {},
        });
      }
    } catch (error) {
      console.error("Error loading verify data:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "error.loading.data",
          defaultMessage: "Error loading form data",
        }),
      });
      setNotificationVisible(true);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleProjectDataChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      projectData: {
        ...prev.projectData,
        [field]: value,
      },
    }));
  };

  const handleProjectChange = (projectId) => {
    setSelectedProject(projectId);
    handleInputChange("project", projectId);
  };

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

    setSaving(true);
    try {
      const response = await postToOpenElisServer(
        "/rest/SampleEntryByProject",
        JSON.stringify(formData),
      );

      if (response) {
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
            {/* Patient Information Section */}
            <Column lg={16} md={8} sm={4}>
              <PatientInfoSection
                formData={formData}
                onInputChange={handleInputChange}
                genders={displayLists.genders}
              />
            </Column>

            {/* Sample Information Section */}
            <Column lg={16} md={8} sm={4}>
              <SampleInfoSection
                formData={formData}
                onInputChange={handleInputChange}
              />
            </Column>

            {/* Project Selection Section */}
            <Column lg={16} md={8} sm={4}>
              <ProjectSelectionSection
                selectedProject={selectedProject}
                onProjectChange={handleProjectChange}
                formLists={displayLists.formLists}
              />
            </Column>

            {/* Test Selection Section */}
            <Column lg={16} md={8} sm={4}>
              <TestSelectionSection
                projectData={formData.projectData}
                onTestChange={handleProjectDataChange}
              />
            </Column>

            {/* Study-Specific Sections */}
            {selectedProject && (
              <>
                {/* ARV Section */}
                {(selectedProject.includes("ARV") ||
                  selectedProject.includes("Double") ||
                  selectedProject.includes("Follow")) && (
                  <Column lg={16} md={8} sm={4}>
                    <ARVSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                      organizationLists={displayLists.organizationTypeLists}
                    />
                  </Column>
                )}

                {/* EID Section */}
                {selectedProject.includes("EID") && (
                  <Column lg={16} md={8} sm={4}>
                    <EIDSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {/* Special Request Section */}
                {selectedProject.includes("Special") && (
                  <Column lg={16} md={8} sm={4}>
                    <SpecialRequestSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {/* RTN Section */}
                {selectedProject.includes("RTN") && (
                  <Column lg={16} md={8} sm={4}>
                    <RTNSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}

                {/* HPV Section */}
                {selectedProject.includes("HPV") && (
                  <Column lg={16} md={8} sm={4}>
                    <HPVSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                    />
                  </Column>
                )}

                {/* Indeterminate Section */}
                {selectedProject.includes("Indeterminate") && (
                  <Column lg={16} md={8} sm={4}>
                    <IndeterminateSection
                      projectData={formData.projectData}
                      onInputChange={handleProjectDataChange}
                      organizationLists={displayLists.organizationTypeLists}
                      dictionaryLists={displayLists.dictionaryLists}
                    />
                  </Column>
                )}
              </>
            )}

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
                  <Button type="submit" disabled={saving} onClick={handleSave}>
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
                      (window.location.href = "/StudyDoubleEntry?type=verify")
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
          </Grid>
        </Form>
      </div>

      {notificationVisible && <AlertDialog />}
    </>
  );
};

export default StudyDoubleEntry;
