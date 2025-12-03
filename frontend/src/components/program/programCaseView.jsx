import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom"; // Changed from useNavigate
import { getFromOpenElisServer } from "../utils/Utils";
import {
  Button,
  Loading,
  Modal,
  Grid,
  Column,
  Row,
  Stack,
  Section,
  Tag,
} from "@carbon/react";

import "./programCaseView.css";
import Questionnaire from "../common/Questionnaire";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import PatientHeader from "../common/PatientHeader";

const ProgramCaseView = () => {
  const { programSampleId } = useParams();
  const history = useHistory(); // Changed from useNavigate
  const [programSampleData, setProgramSampleData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [questionnaireAnswers, setQuestionnaireAnswers] = useState({});
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showSaveModal, setShowSaveModal] = useState(false);

  const fetchProgramSampleData = () => {
    if (!programSampleId) {
      setError("No program sample ID provided");
      setLoading(false);
      return;
    }

    const url = `/rest/programSample/${programSampleId}`;
    
    getFromOpenElisServer(url, (response) => {
      if (response) {
        setProgramSampleData(response);
        
        // Initialize answers from questionnaire response
        if (response.programQuestionnaireResponse?.item) {
          const initialAnswers = {};
          response.programQuestionnaireResponse.item.forEach((item) => {
            if (item.answer && item.answer.length > 0) {
              if (item.answer[0].valueString !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueString;
              } else if (item.answer[0].valueBoolean !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueBoolean.toString();
              } else if (item.answer[0].valueInteger !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueInteger.toString();
              } else if (item.answer[0].valueDecimal !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueDecimal.toString();
              } else if (item.answer[0].valueDate !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueDate;
              } else if (item.answer[0].valueTime !== undefined) {
                initialAnswers[item.linkId] = item.answer[0].valueTime;
              }
            }
          });
          setQuestionnaireAnswers(initialAnswers);
        }
      } else {
        setError("Failed to fetch program sample data");
      }
      setLoading(false);
    }, (error) => {
      setError(`Error fetching data: ${error.message || error}`);
      setLoading(false);
    });
  };

  useEffect(() => {
    fetchProgramSampleData();
  }, [programSampleId]);

  const formatDate = (timestamp) => {
    if (!timestamp) return "";
    try {
      const date = new Date(Number(timestamp));
      return date.toLocaleDateString();
    } catch (e) {
      return "Invalid date";
    }
  };

  const formatDateTime = (timestamp) => {
    if (!timestamp) return "";
    try {
      const date = new Date(Number(timestamp));
      return date.toLocaleString();
    } catch (e) {
      return "Invalid date";
    }
  };

  const handleAnswerChange = (e) => {
    const { id, value } = e.target;
    setQuestionnaireAnswers(prev => ({
      ...prev,
      [id]: value
    }));
  };

  const getAnswer = (linkId) => {
    return questionnaireAnswers[linkId] || "";
  };

  const handleEditQuestionnaire = () => {
    setEditMode(true);
  };

  const handleCancelEdit = () => {
    setShowCancelModal(true);
  };

  const handleConfirmCancel = () => {
    setEditMode(false);
    setShowCancelModal(false);
    if (programSampleData?.programQuestionnaireResponse?.item) {
      const initialAnswers = {};
      programSampleData.programQuestionnaireResponse.item.forEach((item) => {
        if (item.answer && item.answer.length > 0) {
          if (item.answer[0].valueString !== undefined) {
            initialAnswers[item.linkId] = item.answer[0].valueString;
          } else if (item.answer[0].valueBoolean !== undefined) {
            initialAnswers[item.linkId] = item.answer[0].valueBoolean.toString();
          }
        }
      });
      setQuestionnaireAnswers(initialAnswers);
    }
  };

  const handleSaveQuestionnaire = () => {
    setShowSaveModal(true);
  };

  const handleConfirmSave = async () => {
    try {
      // Prepare questionnaire response data for saving
      const responseData = {
        resourceType: "QuestionnaireResponse",
        questionnaire: programSampleData.programQuestionnaire.questionnaire,
        status: "completed",
        subject: {
          reference: `Patient/${programSampleData.id}`
        },
        item: Object.keys(questionnaireAnswers).map(linkId => {
          const item = programSampleData.programQuestionnaire.item.find(
            q => q.linkId === linkId
          );
          const value = questionnaireAnswers[linkId];
          
          let answer = {};
          if (item.type === "boolean") {
            answer.valueBoolean = value === "true";
          } else if (item.type === "integer") {
            answer.valueInteger = parseInt(value);
          } else if (item.type === "decimal") {
            answer.valueDecimal = parseFloat(value);
          } else if (item.type === "date") {
            answer.valueDate = value;
          } else if (item.type === "time") {
            answer.valueTime = value;
          } else {
            answer.valueString = value;
          }
          
          return {
            linkId: linkId,
            text: item?.text || "",
            answer: [answer]
          };
        })
      };

      // Save to server
      const saveUrl = `/rest/programSample/${programSampleId}/questionnaire`;
      await fetch(saveUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(responseData),
      });

      // Refresh data
      fetchProgramSampleData();
      setEditMode(false);
      setShowSaveModal(false);
    } catch (error) {
      console.error("Error saving questionnaire:", error);
      alert("Failed to save questionnaire");
    }
  };

  const handleBack = () => {
    history.goBack(); // Changed from navigate(-1)
  };

  if (loading) {
    return (
      <Grid fullWidth className="program-case-loading">
        <Column lg={16} md={8} sm={4}>
          <Stack gap={6} alignItems="center" justifyContent="center" style={{ minHeight: "400px" }}>
            <Loading withOverlay={false} description="Loading program case..." />
            <p>Loading program case details...</p>
          </Stack>
        </Column>
      </Grid>
    );
  }

  if (error) {
    return (
      <Grid fullWidth className="program-case-error">
        <Column lg={16} md={8} sm={4}>
          <Stack gap={6} alignItems="center" justifyContent="center" style={{ minHeight: "400px" }}>
            <h3>Error</h3>
            <p>{error}</p>
            <Button onClick={handleBack}>Go Back</Button>
          </Stack>
        </Column>
      </Grid>
    );
  }

  if (!programSampleData) {
    return (
      <Grid fullWidth className="program-case-no-data">
        <Column lg={16} md={8} sm={4}>
          <Stack gap={6} alignItems="center" justifyContent="center" style={{ minHeight: "400px" }}>
            <h3>No Data Found</h3>
            <p>No program sample data found for ID: {programSampleId}</p>
            <Button onClick={handleBack}>Go Back</Button>
          </Stack>
        </Column>
      </Grid>
    );
  }

  // Prepare props for PatientHeader
  const patientHeaderProps = {
    id: programSampleData.programSampleId?.toString(),
    firstName: programSampleData.firstName,
    lastName: programSampleData.lastName,
    gender: programSampleData.gender,
    age: programSampleData.age,
    accesionNumber: programSampleData.accessionNumber,
    orderDate: formatDate(programSampleData.receivedDate),
  };

  return (
    <Grid fullWidth className="program-case-view">
      <Column lg={16} md={8} sm={4}>
        {/* Header Section */}
        <Row className="program-case-header">
          <Column lg={16} md={8} sm={4}>
            <Stack orientation="horizontal" gap={3}>
              <Button kind="secondary" onClick={handleBack}>
                Back to Dashboard
              </Button>
              <h2>Program Case Details</h2>
            </Stack>
          </Column>
        </Row>

        {/* Patient Header Section */}
        <Row className="patient-header-section">
          <Column lg={16} md={8} sm={4}>
            <PatientHeader {...patientHeaderProps} />
          </Column>
        </Row>

        {/* Program Information Section */}
        <Row className="program-info-section">
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <h3 className="section-title">Program Information</h3>
                <Grid condensed>
                  <Row>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Program Name</strong>
                        <span>{programSampleData.programName}</span>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Program Code</strong>
                        <span>{programSampleData.programCode}</span>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Status</strong>
                        <Tag
                          type={programSampleData.questionnaireStatus === "COMPLETED" ? "green" : "red"}
                          size="sm"
                        >
                          {programSampleData.questionnaireStatus}
                        </Tag>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Received Date</strong>
                        <span>{formatDateTime(programSampleData.receivedDate)}</span>
                      </div>
                    </Column>
                  </Row>
                  <Row>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Accession Number</strong>
                        <span>{programSampleData.accessionNumber}</span>
                      </div>
                    </Column>
                    <Column lg={4} md={4} sm={2}>
                      <div className="info-item">
                        <strong>Sample ID</strong>
                        <span>{programSampleData.programSampleId}</span>
                      </div>
                    </Column>
                  </Row>
                </Grid>
              </Section>
            </Section>
          </Column>
        </Row>

        {/* Questionnaire Section */}
        <Row className="questionnaire-section">
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Stack orientation="horizontal" gap={3} className="questionnaire-header">
                  <h3 className="section-title">Questionnaire</h3>
                  {!editMode && programSampleData.questionnaireStatus === "IN-PROGRESS" && (
                    <Button onClick={handleEditQuestionnaire}>
                      Continue Questionnaire
                    </Button>
                  )}
                  {!editMode && programSampleData.questionnaireStatus === "COMPLETED" && (
                    <Button onClick={handleEditQuestionnaire}>
                      Edit Questionnaire
                    </Button>
                  )}
                </Stack>

                {editMode ? (
                  <div className="questionnaire-edit-mode">
                    <Grid condensed>
                      <Questionnaire
                        questionnaire={programSampleData.programQuestionnaire}
                        onAnswerChange={handleAnswerChange}
                        getAnswer={getAnswer}
                      />
                    </Grid>
                    <Stack orientation="horizontal" gap={3} className="questionnaire-edit-actions">
                      <Button kind="danger" onClick={handleCancelEdit}>
                        Cancel
                      </Button>
                      <Button onClick={handleSaveQuestionnaire}>
                        Save Questionnaire
                      </Button>
                    </Stack>
                  </div>
                ) : (
                  programSampleData.programQuestionnaireResponse && (
                    <div className="questionnaire-view-mode">
                      <Grid condensed>
                        <QuestionnaireResponse
                          questionnaireResponse={programSampleData.programQuestionnaireResponse}
                        />
                      </Grid>
                    </div>
                  )
                )}
              </Section>
            </Section>
          </Column>
        </Row>
      </Column>

      {/* Cancel Confirmation Modal */}
      <Modal
        open={showCancelModal}
        modalHeading="Cancel Changes"
        modalLabel="Confirmation"
        primaryButtonText="Yes, Cancel Changes"
        secondaryButtonText="Continue Editing"
        onRequestClose={() => setShowCancelModal(false)}
        onRequestSubmit={handleConfirmCancel}
        danger
      >
        <p>Are you sure you want to cancel your changes? All unsaved changes will be lost.</p>
      </Modal>

      {/* Save Confirmation Modal */}
      <Modal
        open={showSaveModal}
        modalHeading="Save Questionnaire"
        modalLabel="Confirmation"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        onRequestClose={() => setShowSaveModal(false)}
        onRequestSubmit={handleConfirmSave}
      >
        <p>Are you sure you want to save the questionnaire responses?</p>
      </Modal>
    </Grid>
  );
};

export default ProgramCaseView;