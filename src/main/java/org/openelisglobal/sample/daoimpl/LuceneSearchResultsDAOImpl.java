package org.openelisglobal.sample.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.sample.dao.SearchResultsDAO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class LuceneSearchResultsDAOImpl implements SearchResultsDAO {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException {

        SearchSession searchSession = Search.session(entityManager);

        boolean queryFirstName = !GenericValidator.isBlankOrNull(firstName);
        boolean queryLastName = !GenericValidator.isBlankOrNull(lastName);
        boolean queryNationalId = !GenericValidator.isBlankOrNull(nationalID);
        boolean querySTNumber = !GenericValidator.isBlankOrNull(STNumber);
        boolean querySubjectNumber = !GenericValidator.isBlankOrNull(subjectNumber);
        boolean queryExternalId = !GenericValidator.isBlankOrNull(externalID);
        boolean queryPatientID = !GenericValidator.isBlankOrNull(patientID);
        boolean queryGuid = !GenericValidator.isBlankOrNull(guid);
        boolean queryDateOfBirth = !GenericValidator.isBlankOrNull(dateOfBirth);
        boolean queryGender = !GenericValidator.isBlankOrNull(gender);

        // fields searched from DB
        nationalID = '%' + nationalID + '%';
        STNumber = '%' + STNumber + '%';
        subjectNumber = '%' + subjectNumber + '%';
        externalID = '%' + externalID + '%';

        List<String> hits = searchSession.search(Patient.class).select(f -> f.id(String.class)).where(f -> f.bool(b -> {
            if (queryPatientID) {
                b.must(f.match().field("id").matching(patientID));
            }
            if (queryGender) {
                b.must(f.match().field("gender").matching(gender));
            }
            if (queryDateOfBirth) {
                b.must(f.bool().should(f.match().field("birthDateForDisplay").matching(dateOfBirth))
                        .should(f.match().field("birthDateForDisplay").matching(getFormatedDOB(dateOfBirth))));
            }
            if (queryFirstName && queryLastName) {
                b.must(f.nested().objectField("person")
                        .nest(f.bool().must(f.match().field("person.firstName").matching(firstName).fuzzy())
                                .must(f.match().field("person.lastName").matching(lastName).fuzzy())));
            } else {
                if (queryFirstName) {
                    b.must(f.match().field("person.firstName").matching(firstName).fuzzy());
                }
                if (queryLastName) {
                    b.must(f.match().field("person.lastName").matching(lastName).fuzzy());
                }
            }
        })).fetchAllHits();

        List<Long> longHits = hits.stream().map(Long::parseLong).collect(Collectors.toList());
        // 'IN' predicate requires the list to contain at least one value
        longHits.add(-1L);
        int hitsCount = longHits.size();

        String sqlString = buildQueryString(queryNationalId, queryExternalId, querySTNumber, querySubjectNumber,
                queryGuid, hitsCount);
        Query query = entityManager.unwrap(Session.class).createNativeQuery(sqlString);
        query.setParameter(ID_TYPE_FOR_ST, Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("ST")));
        query.setParameter(ID_TYPE_FOR_SUBJECT_NUMBER,
                Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("SUBJECT")));
        query.setParameter(ID_TYPE_FOR_GUID,
                Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("GUID")));

        if (queryNationalId) {
            query.setParameter(NATIONAL_ID_PARAM, nationalID);
        }
        if (queryExternalId) {
            query.setParameter(EXTERNAL_ID_PARAM, nationalID);
        }
        if (querySTNumber) {
            query.setParameter(ST_NUMBER_PARAM, STNumber);
        }
        if (querySubjectNumber) {
            query.setParameter(SUBJECT_NUMBER_PARAM, subjectNumber);
        }
        if (queryGuid) {
            query.setParameter(GUID, guid);
        }
        if (hitsCount > 1) {
            query.setParameter(ID_LIST, longHits);
        }

        List<Object[]> queryResults = query.list();

        List<PatientSearchResults> patientSearchResultsList = new ArrayList<>();

        for (Object[] tuple : queryResults) {

            PatientSearchResults patientSearchResults = new PatientSearchResults((BigDecimal) tuple[0],
                    (String) tuple[1], (String) tuple[2], (String) tuple[3], (String) tuple[4], (String) tuple[5],
                    (String) tuple[6], (String) tuple[7], (String) tuple[8], (String) tuple[9], null);
            patientSearchResultsList.add(patientSearchResults);
        }

        return patientSearchResultsList;
    }

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResultsByGUID(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException {

        List<PatientSearchResults> patientSearchResultsList = new ArrayList<>();
        return patientSearchResultsList;
    }

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResultsExact(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException {

        SearchSession searchSession = Search.session(entityManager);

        boolean queryFirstName = !GenericValidator.isBlankOrNull(firstName);
        boolean queryLastName = !GenericValidator.isBlankOrNull(lastName);
        boolean queryNationalId = !GenericValidator.isBlankOrNull(nationalID);
        boolean querySTNumber = !GenericValidator.isBlankOrNull(STNumber);
        boolean querySubjectNumber = !GenericValidator.isBlankOrNull(subjectNumber);
        boolean queryExternalId = !GenericValidator.isBlankOrNull(externalID);
        boolean queryPatientID = !GenericValidator.isBlankOrNull(patientID);
        boolean queryGuid = !GenericValidator.isBlankOrNull(guid);
        boolean queryDateOfBirth = !GenericValidator.isBlankOrNull(dateOfBirth);
        boolean queryGender = !GenericValidator.isBlankOrNull(gender);

        List<String> hits = searchSession.search(Patient.class).select(f -> f.id(String.class)).where(f -> f.bool(b -> {
            if (queryPatientID) {
                b.must(f.match().field("id").matching(patientID));
            }
            if (queryGender) {
                b.must(f.match().field("gender").matching(gender));
            }
            if (queryDateOfBirth) {
                b.must(f.bool().should(f.match().field("birthDateForDisplay").matching(dateOfBirth))
                        .should(f.match().field("birthDateForDisplay").matching(getFormatedDOB(dateOfBirth))));
            }
            if (queryFirstName && queryLastName) {
                b.must(f.nested().objectField("person")
                        .nest(f.bool().must(f.match().field("person.firstName").matching(firstName))
                                .must(f.match().field("person.lastName").matching(lastName))));
            } else {
                if (queryFirstName) {
                    b.must(f.match().field("person.firstName").matching(firstName));
                }
                if (queryLastName) {
                    b.must(f.match().field("person.lastName").matching(lastName));
                }
            }
        })).fetchAllHits();

        List<Long> longHits = hits.stream().map(Long::parseLong).collect(Collectors.toList());
        // 'IN' predicate requires the list to contain at least one value
        longHits.add(-1L);
        int hitsCount = longHits.size();

        String sqlString = buildQueryString(queryNationalId, queryExternalId, querySTNumber, querySubjectNumber,
                queryGuid, hitsCount);
        Query query = entityManager.unwrap(Session.class).createNativeQuery(sqlString);
        query.setParameter(ID_TYPE_FOR_ST, Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("ST")));
        query.setParameter(ID_TYPE_FOR_SUBJECT_NUMBER,
                Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("SUBJECT")));
        query.setParameter(ID_TYPE_FOR_GUID,
                Integer.valueOf(PatientIdentityTypeMap.getInstance().getIDForType("GUID")));

        if (queryNationalId) {
            query.setParameter(NATIONAL_ID_PARAM, nationalID);
        }
        if (queryExternalId) {
            query.setParameter(EXTERNAL_ID_PARAM, nationalID);
        }
        if (querySTNumber) {
            query.setParameter(ST_NUMBER_PARAM, STNumber);
        }
        if (querySubjectNumber) {
            query.setParameter(SUBJECT_NUMBER_PARAM, subjectNumber);
        }
        if (queryGuid) {
            query.setParameter(GUID, guid);
        }
        if (hitsCount > 1) {
            query.setParameter(ID_LIST, longHits);
        }

        List<Object[]> queryResults = query.list();

        List<PatientSearchResults> patientSearchResultsList = new ArrayList<>();

        for (Object[] tuple : queryResults) {

            PatientSearchResults patientSearchResults = new PatientSearchResults((BigDecimal) tuple[0],
                    (String) tuple[1], (String) tuple[2], (String) tuple[3], (String) tuple[4], (String) tuple[5],
                    (String) tuple[6], (String) tuple[7], (String) tuple[8], (String) tuple[9], null);
            patientSearchResultsList.add(patientSearchResults);
        }

        return patientSearchResultsList;
    }

    private String buildQueryString(Boolean queryNationalID, Boolean queryExternalID, Boolean querySTNumber,
            Boolean querySubjectNumber, Boolean queryGuid, int hitsCount) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select p.id, pr.first_name, pr.last_name, p.gender, p.entered_birth_date, p.national_id,"
                + " p.external_id, pi.identity_data as st, piSN.identity_data as subject,"
                + " piGUID.identity_data as guid ");
        queryBuilder.append("from patient p join person pr on p.person_id = pr.id ");
        queryBuilder.append("left join patient_identity pi on pi.patient_id = p.id and pi.identity_type_id = :");
        queryBuilder.append(ID_TYPE_FOR_ST).append(" ");
        queryBuilder.append("left join patient_identity piSN on piSN.patient_id = p.id and piSN.identity_type_id = :");
        queryBuilder.append(ID_TYPE_FOR_SUBJECT_NUMBER).append(" ");
        queryBuilder.append(
                "left join patient_identity piGUID on piGUID.patient_id = p.id and piGUID.identity_type_id" + " = :");
        queryBuilder.append(ID_TYPE_FOR_GUID).append(" ");
        queryBuilder.append("where ");

        queryBuilder.append("( false or ");
        if (querySubjectNumber) {
            queryBuilder.append("piSN.identity_data ilike :");
            queryBuilder.append(SUBJECT_NUMBER_PARAM).append(" or ");
        }

        if (queryNationalID) {
            queryBuilder.append("p.national_id ilike :");
            queryBuilder.append(NATIONAL_ID_PARAM).append(" or ");
        }

        if (queryExternalID) {
            queryBuilder.append("p.external_id ilike :");
            queryBuilder.append(EXTERNAL_ID_PARAM).append(" or ");
        }

        if (querySTNumber) {
            queryBuilder.append("pi.identity_data ilike :");
            queryBuilder.append(ST_NUMBER_PARAM).append(" and ");
        }

        // Need to close paren before dangling AND/OR.
        int lastAndIndex = queryBuilder.lastIndexOf("and");
        int lastOrIndex = queryBuilder.lastIndexOf("or");

        if (lastAndIndex > lastOrIndex) {
            queryBuilder.delete(lastAndIndex, queryBuilder.length());
            queryBuilder.append(") and ");
        } else if (lastOrIndex > lastAndIndex) {
            queryBuilder.delete(lastOrIndex, queryBuilder.length());
            queryBuilder.append(") or ");
        }

        if (queryGuid) {
            queryBuilder.append("piGUID.identity_data = :");
            queryBuilder.append(GUID).append(" and ");
        }

        // idList contains patient IDs from Lucene search results matching the fields
        // id, person.firstName, person.lastName, birthDateForDisplay and gender
        if (hitsCount > 1) {
            queryBuilder.append("p.id in :");
            queryBuilder.append(ID_LIST).append(" and ");
        }

        // No matter which was added last there is one dangling AND to remove.
        lastAndIndex = queryBuilder.lastIndexOf("and");
        lastOrIndex = queryBuilder.lastIndexOf("or");

        if (lastAndIndex > lastOrIndex) {
            queryBuilder.delete(lastAndIndex, queryBuilder.length());
        } else if (lastOrIndex > lastAndIndex) {
            queryBuilder.delete(lastOrIndex, queryBuilder.length());
        }
        return queryBuilder.toString();
    }

    private String getFormatedDOB(String dob) {
        String format1 = "dd/MM/yyyy";
        String format2 = "MM/dd/yyyy";
        String dobFormated = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE)
                .equals("fr-FR") ? DateUtil.formatStringDate(dob, format2) : DateUtil.formatStringDate(dob, format1);

        return dobFormated;
    }
}
