/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
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
package org.openelisglobal.common.provider.query.workerObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.externalLinks.IExternalPatientSearch;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;

public class PatientSearchLocalAndExternalWorker extends PatientSearchWorker {

    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected PatientIdentityService patientIdentityService = SpringContext.getBean(PatientIdentityService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);
    private AddressPartService addressPartService = SpringContext.getBean(AddressPartService.class);
    private PersonAddressService personAddressService = SpringContext.getBean(PersonAddressService.class);

    private final String sysUserId;

    public PatientSearchLocalAndExternalWorker(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    /**
     * @see org.openelisglobal.common.provider.query.workerObjects.PatientSearchWorker#createSearchResultXML(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.StringBuilder)
     */
    @Override
    public String createSearchResultXML(String lastName, String firstName, String STNumber, String subjectNumber,
            String nationalID, String patientID, String guid, String dateOfBirth, String gender, StringBuilder xml) {

        // just to make the name shorter
        ConfigurationProperties config = ConfigurationProperties.getInstance();

        String success = IActionConstants.VALID;

        if (GenericValidator.isBlankOrNull(lastName) && GenericValidator.isBlankOrNull(firstName)
                && GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalID) && GenericValidator.isBlankOrNull(patientID)
                && GenericValidator.isBlankOrNull(guid)) {

            xml.append("No search terms were entered");
            return IActionConstants.INVALID;
        }

        List<PatientSearchResults> allResults = new ArrayList<>();

        List<IExternalPatientSearch> externalSearches = new ArrayList<>();

        List<PatientSearchResults> localResults = new ArrayList<>();
        localResults = searchResultsService.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID,
                guid, patientID, guid, "", "");
        localResults.forEach(e -> e.setDataSourceName(MessageUtil.getMessage("patient.local.source")));
        allResults.addAll(localResults);

        if (config.getPropertyValue(Property.INFO_HIGHWAY_ENABLED).equals("true")) {
            IExternalPatientSearch externalSearch = (IExternalPatientSearch) SpringContext.getBean("InfoHighwaySearch");
            externalSearch.setSearchCriteria(lastName, firstName, STNumber, subjectNumber, nationalID, guid);
            externalSearch.setConnectionCredentials(config.getPropertyValue(Property.INFO_HIGHWAY_ADDRESS),
                    config.getPropertyValue(Property.INFO_HIGHWAY_USERNAME),
                    config.getPropertyValue(Property.INFO_HIGHWAY_PASSWORD));

            externalSearches.add(externalSearch);
        }

        if (config.getPropertyValue(Property.PatientSearchEnabled).equals("true")) {
            IExternalPatientSearch externalSearch = SpringContext.getBean(IExternalPatientSearch.class);
            externalSearch.setSearchCriteria(lastName, firstName, STNumber, subjectNumber, nationalID, guid);
            externalSearch.setConnectionCredentials(config.getPropertyValue(Property.PatientSearchURL),
                    config.getPropertyValue(Property.PatientSearchUserName),
                    config.getPropertyValue(Property.PatientSearchPassword));

            externalSearches.add(externalSearch);
        }

        List<ExtendedPatientSearchResults> externalResults = null;
        List<ExtendedPatientSearchResults> newPatientsFromExternalSearch = new ArrayList<>();

        for (IExternalPatientSearch externalSearch : externalSearches) {
            try {
                Future<Integer> futureExternalSearchResult = externalSearch.runExternalSearch();
                Integer externalSearchResult = futureExternalSearchResult.get(externalSearch.getTimeout(),
                        TimeUnit.MILLISECONDS);

                if (externalSearchResult == 200) {
                    externalResults = externalSearch.getSearchResults();
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "createSearchResultXML",
                            "could not get external search results from " + externalSearch.getConnectionString()
                                    + " - failed response");
                }
            } catch (InterruptedException | ExecutionException | TimeoutException | IllegalStateException e) {
                LogEvent.logError(e);
            }

            findNewPatients(localResults, externalResults, newPatientsFromExternalSearch);
            insertNewPatients(newPatientsFromExternalSearch);
            newPatientsFromExternalSearch
                    .forEach(e -> e.setDataSourceName(MessageUtil.getMessage("patient.imported.source")));
            allResults.addAll(newPatientsFromExternalSearch);
        }
        sortPatients(allResults);

        if (allResults != null && allResults.size() > 0) {
            for (PatientSearchResults singleResult : allResults) {
                appendSearchResultRow(singleResult, xml);
            }
        } else {
            success = IActionConstants.INVALID;

            xml.append("No results were found for search.  Check spelling or remove some of the fields");
        }

        return success;
    }

    /**
     * @see org.openelisglobal.common.provider.query.workerObjects.PatientSearchWorker#createSearchResultXML(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.StringBuilder)
     */
    @Override
    public List<PatientSearchResults> getPatientSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String patientID, String guid, String dateOfBirth, String gender) {

        // just to make the name shorter
        ConfigurationProperties config = ConfigurationProperties.getInstance();

        String success = IActionConstants.VALID;

        if (GenericValidator.isBlankOrNull(lastName) && GenericValidator.isBlankOrNull(firstName)
                && GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalID) && GenericValidator.isBlankOrNull(patientID)
                && GenericValidator.isBlankOrNull(guid)) {

            return new ArrayList<>();
        }

        List<PatientSearchResults> allResults = new ArrayList<>();

        List<IExternalPatientSearch> externalSearches = new ArrayList<>();

        List<PatientSearchResults> localResults = new ArrayList<>();
        localResults = searchResultsService.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID,
                guid, patientID, guid, "", "");
        localResults.forEach(e -> e.setDataSourceName(MessageUtil.getMessage("patient.local.source")));
        allResults.addAll(localResults);

        if (config.getPropertyValue(Property.INFO_HIGHWAY_ENABLED).equals("true")) {
            IExternalPatientSearch externalSearch = (IExternalPatientSearch) SpringContext.getBean("InfoHighwaySearch");
            externalSearch.setSearchCriteria(lastName, firstName, STNumber, subjectNumber, nationalID, guid);
            externalSearch.setConnectionCredentials(config.getPropertyValue(Property.INFO_HIGHWAY_ADDRESS),
                    config.getPropertyValue(Property.INFO_HIGHWAY_USERNAME),
                    config.getPropertyValue(Property.INFO_HIGHWAY_PASSWORD));

            externalSearches.add(externalSearch);
        }

        if (config.getPropertyValue(Property.PatientSearchEnabled).equals("true")) {
            IExternalPatientSearch externalSearch = SpringContext.getBean(IExternalPatientSearch.class);
            externalSearch.setSearchCriteria(lastName, firstName, STNumber, subjectNumber, nationalID, guid);
            externalSearch.setConnectionCredentials(config.getPropertyValue(Property.PatientSearchURL),
                    config.getPropertyValue(Property.PatientSearchUserName),
                    config.getPropertyValue(Property.PatientSearchPassword));

            externalSearches.add(externalSearch);
        }

        List<ExtendedPatientSearchResults> externalResults = null;
        List<ExtendedPatientSearchResults> newPatientsFromExternalSearch = new ArrayList<>();

        for (IExternalPatientSearch externalSearch : externalSearches) {
            try {
                Future<Integer> futureExternalSearchResult = externalSearch.runExternalSearch();
                Integer externalSearchResult = futureExternalSearchResult.get(externalSearch.getTimeout(),
                        TimeUnit.MILLISECONDS);

                if (externalSearchResult == 200) {
                    externalResults = externalSearch.getSearchResults();
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "createSearchResultXML",
                            "could not get external search results from " + externalSearch.getConnectionString()
                                    + " - failed response");
                }
            } catch (InterruptedException | ExecutionException | TimeoutException | IllegalStateException e) {
                LogEvent.logError(e);
            }

            findNewPatients(localResults, externalResults, newPatientsFromExternalSearch);
            insertNewPatients(newPatientsFromExternalSearch);
            newPatientsFromExternalSearch
                    .forEach(e -> e.setDataSourceName(MessageUtil.getMessage("patient.imported.source")));
            allResults.addAll(newPatientsFromExternalSearch);
        }
        sortPatients(allResults);

        return allResults;
    }

    private void insertNewPatients(List<ExtendedPatientSearchResults> newPatientsFromClinic) {
        try {
            for (ExtendedPatientSearchResults results : newPatientsFromClinic) {
                insertNewPatients(results);
            }
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }
    }

    private void insertNewPatients(ExtendedPatientSearchResults results) {
        Patient patient = new Patient();
        Person person = new Person();

        patient.setBirthDateForDisplay(results.getBirthdate());
        patient.setGender(results.getGender());
        patient.setNationalId(results.getNationalId());
        patient.setSysUserId(sysUserId);

        person.setLastName(results.getLastName());
        person.setFirstName(results.getFirstName());
        person.setStreetAddress(results.getStreetAddress());
        person.setZipCode(results.getPostalCode());
        person.setSysUserId(sysUserId);

        personService.insert(person);
        patient.setPerson(person);
        patientService.insert(patient);

        persistIdentityType(patientIdentityService, results.getStNumber(), "ST", patient.getId());
        persistIdentityType(patientIdentityService, results.getSubjectNumber(), "SUBJECT", patient.getId());
        persistIdentityType(patientIdentityService, results.getMothersName(), "MOTHER", patient.getId());
        persistIdentityType(patientIdentityService, results.getGUID(), "GUID", patient.getId());
        persistIdentityType(patientIdentityService, results.getDataSourceId(), "ORG_SITE", patient.getId());

        String ADDRESS_PART_COMMUNE_ID = "";
        String ADDRESS_PART_VILLAGE_ID = "";

        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart addressPart : partList) {
            if ("commune".equals(addressPart.getPartName())) {
                ADDRESS_PART_COMMUNE_ID = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                ADDRESS_PART_VILLAGE_ID = addressPart.getId();
            }
        }

        patientService.insertNewPatientAddressInfo(ADDRESS_PART_COMMUNE_ID, results.getCampCommune(), "T", patient,
                sysUserId);
        patientService.insertNewPatientAddressInfo(ADDRESS_PART_VILLAGE_ID, results.getTown(), "T", patient, sysUserId);

        results.setPatientID(patient.getId());
    }

    public void persistIdentityType(PatientIdentityService patientIdentityService, String paramValue, String type,
            String patientId) throws LIMSRuntimeException {

        if (!GenericValidator.isBlankOrNull(paramValue)) {

            String typeID = PatientIdentityTypeMap.getInstance().getIDForType(type);

            PatientIdentity patientIdentity = new PatientIdentity();
            patientIdentity.setPatientId(patientId);
            patientIdentity.setIdentityTypeId(typeID);
            patientIdentity.setSysUserId(sysUserId);
            patientIdentity.setIdentityData(paramValue);
            patientIdentity.setLastupdatedFields();
            patientIdentityService.insert(patientIdentity);
        }
    }

    /*
     * This will check to see if the clinic results are in OpenELIS. If they are not
     * then they will be
     */
    private void findNewPatients(List<PatientSearchResults> results, List<ExtendedPatientSearchResults> externalResults,
            List<ExtendedPatientSearchResults> newPatientsFromExternalSearch) {

        if (externalResults != null) {
            List<String> currentGuids = new ArrayList<>();
            List<String> currentNationalIds = new ArrayList<>();

            for (PatientSearchResults result : results) {
                if (!GenericValidator.isBlankOrNull(result.getGUID())) {
                    currentGuids.add(result.getGUID());
                }
                if (!GenericValidator.isBlankOrNull(result.getNationalId())) {
                    currentNationalIds.add(result.getNationalId());
                }
            }

            for (ExtendedPatientSearchResults externalResult : externalResults) {
                if (!currentGuids.contains(externalResult.getGUID())
                        && !currentNationalIds.contains(externalResult.getNationalId())) {
                    newPatientsFromExternalSearch.add(externalResult);
                }
            }
        }
    }
}
