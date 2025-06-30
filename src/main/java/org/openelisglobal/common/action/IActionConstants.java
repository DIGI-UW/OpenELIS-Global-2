/**
 * /* The contents of this file are subject to the Mozilla Public License Version 1.1 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.action;

/**
 * IActionConstants.java
 *
 * @author diane benz 10/31/2005
 *         <p>
 *         This interface contains the constants for bean and attribute names
 *         used by the struts action classes.
 */
public interface IActionConstants {
    /** The key for the page title in the request scope. */
    String PAGE_TITLE_KEY = "title";
    /** The key for the page subtitle in the request scope. */
    String PAGE_SUBTITLE_KEY = "subtitle";

    String INVALID = "invalid";
    String VALID = "valid";
    String INVALID_TO_LARGE = "invalid_value_to_large";
    String INVALID_TO_SMALL = "invalid_value_to_small";

    String YES = "Y";
    String NO = "N";

    String NOT_APPLICABLE = "N/A";

    String BLANK = "";

    String CHILD_TYPE_NONE = "0";
    String CHILD_TYPE_REFLEX = "1";
    String CHILD_TYPE_LINK = "2";

    String INVALIDSTATUS = "invalidStatus";
    String VALIDSTATUS = "validStatus";
    String MORE_THAN_ONE_ACCESSION_NUMBER = "moreThanOneAccessionNumber";
    String INVALIDOTHERS = "invalidOthers";

    // action forward constants
    String FWD_CLOSE = "close";
    String FWD_FAIL = "fail";
    String FWD_VALIDATION_ERROR = "error";
    String FWD_STALE_DATA_ERROR = "stale";
    String FWD_SUCCESS = "success";
    String FWD_SUCCESS_SEARCH = "searchSuccess";
    String FWD_SUCCESS_INSERT = "insertSuccess";
    String FWD_SUCCESS_DELETE = "deleteSuccess";
    String FWD_FAIL_INSERT = "insertFail";
    String FWD_FAIL_DELETE = "deleteFail";
    String FWD_CANCEL = "cancel";

    String FWD_NEXT = "next";
    String FWD_PREVIOUS = "previous";
    String FWD_SUCCESS_HUMAN = "successHuman";
    String FWD_SUCCESS_ANIMAL = "successAnimal";

    String FWD_SUCCESS_NEWBORN = "successNewborn";
    String FWD_FAIL_HUMAN = "failHuman";
    String FWD_FAIL_ANIMAL = "failAnimal";

    String FWD_SUCCESS_MULTIPLE_SAMPLE_MODE = "successMultipleSampleMode";

    String FWD_SUCCESS_FULL_SCREEN_VIEW_TEST_SECTION = "successFullScreenViewTestSection";
    String FWD_SUCCESS_FULL_SCREEN_VIEW_SAMPLE_SECTION = "successFullScreenViewSampleSection";

    String FWD_SUCCESS_OTHER = "successother";
    String FWD_FAIL_OTHER = "failother";

    String FWD_SUCCESS_QA_EVENTS_ENTRY = "successQaEventsEntry";
    String UNSATISFACTORY_RESULT = "UNSATISFACTORY";

    String ALLOW_EDITS_KEY = "allowEdits";

    String IN_MENU_SELECT_LIST_HEADER_SEARCH = "inMenuSelectListHeaderSearch";
    String MENU_SEARCH_BY_TABLE_COLUMN = "menuSearchByTableColumn";

    String MENU_PAGE_INSTRUCTION = "menuPageInstruction";

    String MENU_OBJECT_TO_ADD = "menuObjectToAdd";

    String FILTER_CHECK_ADMIN = "filterCheckAdmin";

    String FILTER_CHECK_ACTIVE = "filterCheckActive";

    String FILTER_ROLE = "filterRole";

    String SEARCHED_STRING = "searchedString";

    String APPLY_FILTER = "filter";

    String PAGE_SIZE = "pageSize";

    String POPUPFORM_FILTER_BY_TABLE_COLUMN = "popupFormFilterByTableColumn";
    String IN_POPUP_FORM_SEARCH = "inPopupFormSearch";
    String POPUP_FORM_SEARCH_STRING = "popupFormSearchString";

    String ACTION_KEY = "action";

    String FORM_NAME = "formName";

    String DEFAULT = "default";

    String PREVIOUS_DISABLED = "previousDisabled";
    String NEXT_DISABLED = "nextDisabled";
    String DEACTIVATE_DISABLED = "deactivateDisabled";

    String ADD_DISABLED = "addDisabled";
    String EDIT_DISABLED = "editDisabled";
    String SAVE_DISABLED = "saveDisabled";
    String VIEW_DISABLED = "viewDisabled";

    String CANCEL_DISABLED = "cancelDisabled";

    String ADD_DISABLED_ALL_TEST_QAEVENTS_COMPLETED = "addDisabledAllTestQaEventsCompleted";

    String ADD_DISABLED_ALL_SAMPLE_QAEVENTS_COMPLETED = "addDisabledAllSampleQaEventsCompleted";

    String CLOSE = "close";

    String RECORD_FROZEN_EDIT_DISABLED_KEY = "recordFrozenDisableEdits";

    String RESULTS_ENTRY_ROUTING_SWITCH = "resultsEntryRoutingSwitch";
    int RESULTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_VERIFICATION = 1;
    int RESULTS_ENTRY_ROUTING_FROM_QAEVENTS_ENTRY = 2;

    int RESULTS_ENTRY_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING = 3;
    String RESULTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_VERIFICATION_PARAM_TEST_ID = "resultsEntryFromBatchVerificationTestId";
    String RESULTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_VERIFICATION_PARAM_ACCESSION_NUMBER = "resultsEntryFromBatchVerificationAccessionNumber";
    String RESULTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_VERIFICATION_PARAM_TEST_SECTION_ID = "resultsEntryFromBatchVerificationTestSectionId";

    String QA_EVENTS_ENTRY_ROUTING_SWITCH = "qaEventsEntryRoutingSwitch";
    int QA_EVENTS_ENTRY_ROUTING_FROM_TEST_MANAGEMENT = 1;
    int QA_EVENTS_ENTRY_ROUTING_FROM_RESULTS_ENTRY = 2;
    int QA_EVENTS_ENTRY_ROUTING_FROM_SAMPLE_TRACKING = 3;
    int QA_EVENTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_ENTRY = 4;
    int QA_EVENTS_ENTRY_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING = 5;
    String QA_EVENTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_ENTRY_PARAM_TEST_ID = "testIdForUnsatisfactorySamples";
    String QA_EVENTS_ENTRY_ROUTING_FROM_BATCH_RESULTS_ENTRY_PARAM_ACCESSION_NUMBERS = "accessionNumbersForUnsatisfactorySamples";

    String QAEVENTS_ENTRY_PARAM_QAEVENT_CATEGORY_ID = "qaEventsEntryCategoryId";
    String QAEVENTS_ENTRY_PARAM_MULTIPLE_SAMPLE_MODE = "qaEventsEntryMultipleSampleMode";

    String QA_EVENTS_ENTRY_LINELISTING_ROUTING_SWITCH = "qaEventsEntryLineListingRoutingSwitch";
    int QA_EVENTS_ENTRY_LINELISTING_ROUTING_FROM_QAEVENTS_ENTRY = 1;
    String QAEVENTS_ENTRY_LINELISTING_PARAM_QAEVENT_CATEGORY_ID = "qaEventsEntryLineListingCategoryId";

    String QAEVENTS_ENTRY_PARAM_VIEW_MODE = "qaEventsEntryViewMode";
    String QAEVENTS_ENTRY_NORMAL_VIEW = "qaEventsEntryNormalView";
    String QAEVENTS_ENTRY_FULL_SCREEN_VIEW = "qaEventsEntryFullScreenView";
    String QAEVENTS_ENTRY_FULL_SCREEN_VIEW_SECTION = "qaEventsEntryFullScreenViewSection";
    String QAEVENTS_ENTRY_FULL_SCREEN_VIEW_SAMPLE_SECTION = "sampleSection";
    String QAEVENTS_ENTRY_FULL_SCREEN_VIEW_TEST_SECTION = "testSection";

    String TEST_MANAGEMENT_ROUTING_SWITCH = "testManagementRoutingSwitch";
    int TEST_MANAGEMENT_ROUTING_FROM_RESULTS_ENTRY = 1;
    int TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY = 2;

    int TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING = 3;

    String MENU_TOTAL_RECORDS = "totalRecordCount";
    String MENU_FROM_RECORD = "fromRecordCount";
    String MENU_TO_RECORD = "toRecordCount";

    String TRUE = "true";
    String FALSE = "false";

    String OPEN_REPORTS_MODULE = "openreports";

    String NOTES_REFID = "refid";
    String NOTES_REFTABLE = "reftable";

    String NOTES_EXTERNAL_NOTES_DISABLED = "externalNotesDisabled";
    String POPUP_NOTES = "popupNotes";
    String SELECTED_TEST_ID = "selectedTestId";
    String ANALYSIS_ID = "analysisId";
    String ANALYTE_ID = "analyteId";

    String REPORT_TEST_ID_PARAMETER = "Test_Id";
    String REPORT_PROJECT_ID_PARAMETER = "Project_Id";

    String REPORT_TEST_SECTION_ID_PARAMETER = "Test_Section_Id";
    String THE_TREE = "tree";
    String ACCESSION_NUMBER = "accessionNumber";

    String ACCESSION_NUMBER_REQUESTED = "requestedAccessionNumber";

    String AUDIT_TRAIL_DELETE = "D";
    String AUDIT_TRAIL_UPDATE = "U";
    String AUDIT_TRAIL_INSERT = "I";

    String ID = "ID";

    String SAMPLE_TYPE_NOT_GIVEN = "NOT GIVEN";

    // user info
    String USER_SESSION_DATA = "userSessionData";
    String LOGIN_PAGE = "loginPage";
    String HOME_PAGE = "homePage";
    String MAIN_PAGE = "Main";

    String FWD_CHANGE_PASS = "changePassword";
    String LOGIN_FAILED_CNT = "loginFailedCount";
    String ACCOUNT_LOCK_TIME = "lockTime";

    String TEMP_PDF_FILE = "tempPDFFile";

    String LOCAL_CODE_DICT_ENTRY_SEPARATOR_STRING = ": ";

    String RESULTS_REPORT_TYPE_PARAM = "type";
    String RESULTS_REPORT_TYPE_ORIGINAL = "original";
    String RESULTS_REPORT_TYPE_AMENDED = "amended";

    String RESULTS_REPORT_TYPE_PREVIEW = "preview";

    String ASSIGNABLE_TEST_TYPE_TEST = "testType";
    String ASSIGNABLE_TEST_TYPE_PANEL = "panelType";

    String ACCESSION_NUMBERS = "accessionNumbers";

    String NO_LABEL_PRINTING = "NONE";

    String MULTIPLE_SAMPLE_MODE = "multipleSampleMode";

    String PERMITTED_ACTIONS_MAP = "permittedActions";

    String FORM_FIELD_SET_HAITI = "HAITI";
    String FORM_FIELD_SET_LNSP_HAITI = "LNSP_HAITI";
    String FORM_FIELD_SET_LNSP_CI = "LNSP_CI";
    String FORM_FIELD_SET_CDI = "CDI";
    String FORM_FIELD_SET_CI_GENERAL = "CI_GENERAL";
    String FORM_FIELD_SET_KENYA = "KENYA";
    String FORM_FIELD_SET_MAURITIUS = "MAURITIUS";

    String ANALYSIS_TYPE_MANUAL = "MANUAL";
    String ANALYSIS_TYPE_AUTO = "AUTO";

    String STATUS_RULES_HAITI = "HAITI";
    String STATUS_RULES_HAITI_LNSP = "LNSP_HAITI";
    String STATUS_RULES_RETROCI = "RETROCI";
    String SAMPLE_EDIT_WRITABLE = "SampleEditWritable";

    String RESULTS_SESSION_CACHE = "ResultsSessionCache";
    String RESULTS_PAGE_MAPPING_SESSION_CACHE = "ResultsPageMappingSessionCache";

    String DISPLAY_LIST_SESSION_CACHE = "DisplayListSessionCache";
    String DISPLAY_LIST_MAPPING_SESSION_CACHE = "DisplayListPageMappingSessionCache";

    /**
     * The system_module name used to determine if the current user is allowed to
     * edit primary patient IDs (subject number & site subject number).
     */
    String MODULE_ACCESS_PATIENT_SUBJECTNOS_EDIT = "Access.patient.subjectNos.edit";

    String MODULE_ACCESS_SAMPLE_ACCESSIONNO_EDIT = "Access.sample.accessionNo.edit";

    String DISPLAY_PREV_NEXT = "DisplayPrevNext";
}
