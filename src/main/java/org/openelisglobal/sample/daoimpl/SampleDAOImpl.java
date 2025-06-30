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
package org.openelisglobal.sample.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class SampleDAOImpl extends BaseDAOImpl<Sample, String> implements SampleDAO {

    public SampleDAOImpl() {
        super(Sample.class);
    }

    private static final String ACC_NUMBER_SEQ_BEGIN = "000001";

    @Override
    @Transactional(readOnly = true)
    public void getData(Sample sample) throws LIMSRuntimeException {
        try {
            Sample samp = entityManager.unwrap(Session.class).get(Sample.class, sample.getId());

            if (samp != null) {

                // set sample projects
                String sql = "from SampleProject sp where samp_id = :sampleId";
                Query<SampleProject> query = entityManager.unwrap(Session.class).createQuery(sql, SampleProject.class);
                query.setParameter("sampleId", Integer.parseInt(samp.getId()));
                List<SampleProject> list = query.list();

                samp.setSampleProjects(list);

                PropertyUtils.copyProperties(sample, samp);
            } else {
                sample.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getPageOfSamples(int startingRecNo) throws LIMSRuntimeException {
        List<Sample> samples;
        try {

            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Sample s order by s.id";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            samples = query.list();
            Sample samp;

            // set the display dates for STARTED_DATE, COMPLETED_DATE
            for (int i = 0; i < samples.size(); i++) {
                samp = samples.get(i);
                samp.setEnteredDateForDisplay(DateUtil.convertSqlDateToStringDate(samp.getEnteredDate()));
                samp.setReceivedDateForDisplay(DateUtil.convertSqlDateToStringDate(samp.getReceivedDate()));
                samp.setCollectionDateForDisplay(DateUtil.convertTimestampToStringDate(samp.getCollectionDate()));
                samp.setTransmissionDateForDisplay(DateUtil.convertSqlDateToStringDate(samp.getTransmissionDate()));
                samp.setReleasedDateForDisplay(DateUtil.convertSqlDateToStringDate(samp.getReleasedDate()));
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getPageOfSamples()", e);
        }

        return samples;
    }

    @Override
    @Transactional(readOnly = true)
    public void getSampleByAccessionNumber(Sample sample) throws LIMSRuntimeException {
        try {
            String sql = "from Sample s where s.accessionNumber = :param";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("param", sample.getAccessionNumber());
            List<Sample> list = query.list();
            Sample samp = null;
            if (list.size() > 0) {
                samp = list.get(0);
            }

            if (samp != null) {
                // set sample projects
                sql = "from SampleProject sp where samp_id = :param";
                Query<SampleProject> query2 = entityManager.unwrap(Session.class).createQuery(sql, SampleProject.class);
                query2.setParameter("param", Integer.parseInt(samp.getId()));
                List<SampleProject> sp = query2.list();
                samp.setSampleProjects(sp);

                PropertyUtils.copyProperties(sample, samp);
            } else {
                sample.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getDataByAccessionNumber()", e);
        }
    }

    public Sample readSample(String idString) {
        Sample samp = null;
        try {
            samp = entityManager.unwrap(Session.class).get(Sample.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample readSample()", e);
        }
        return samp;
    }

    @Override
    @Transactional(readOnly = true)
    public String getNextAccessionNumber() {
        String accessionNumber = null;
        String lastAccessionNumber = null;

        // get the current year
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        try {
            String sql = "select max(s.accessionNumber) from Sample s";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);

            List<String> reports = query.list();
            if (reports != null && reports.get(0) != null) {
                if (reports.get(0) != null) {
                    lastAccessionNumber = reports.get(0);
                    if (lastAccessionNumber != null) {
                        String lastAccessionNumberYear = lastAccessionNumber.substring(0, 4);
                        if (lastAccessionNumberYear.equals(String.valueOf(currentYear))) {
                            String seqNumber = lastAccessionNumber.substring(4);
                            int sequence = Integer.parseInt(seqNumber);
                            sequence++;
                            String stringSequenceShort = String.valueOf(sequence);
                            String stringSequenceLong = "";
                            if (stringSequenceShort.length() < 6) {
                                int zeroPaddingLength = 6 - stringSequenceShort.length();
                                StringBuffer zeros = new StringBuffer();
                                for (int i = 0; i < zeroPaddingLength; i++) {
                                    zeros.append("0");
                                }
                                stringSequenceLong = zeros + stringSequenceShort;
                            }
                            if (stringSequenceShort.length() == 6) {
                                stringSequenceLong = stringSequenceShort;
                            }
                            if (stringSequenceShort.length() > 6) { // we
                                // are
                                // over
                                // the
                                // limit
                                // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Error in
                                // Sample
                                // getNextAccessionNumber() max sequence
                                // number reached");
                                throw new LIMSRuntimeException(
                                        "Error in Sample getNextAccessionNumber() max sequence number reached");
                            }
                            accessionNumber = currentYear + stringSequenceLong;
                        } else {
                            // start with new sequence - new year
                            accessionNumber = currentYear + ACC_NUMBER_SEQ_BEGIN;
                        }
                    } else {
                        // nothing retrieved (start from scratch)
                        accessionNumber = currentYear + ACC_NUMBER_SEQ_BEGIN;
                    }
                } else {
                    // nothing retrieved (start from scratch)
                    accessionNumber = currentYear + ACC_NUMBER_SEQ_BEGIN;
                }
            } else {
                // fixed bug - don't throw an exception - if no samples in database start from
                // scratch
                // nothing retrieved (start from scratch)
                accessionNumber = currentYear + ACC_NUMBER_SEQ_BEGIN;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getNextAccessionNumber()", e);
        }
        return accessionNumber;
    }

    /**
     * Get the Sample for the specified accession number.
     *
     * @param accessionNumber The accession number of the Sample being sought.
     * @return Sample The Sample for the specified accession number, or null if the
     *         accession number does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    public Sample getSampleByAccessionNumber(String accessionNumber) throws LIMSRuntimeException {
        Sample sample = null;
        try {
            String sql = "from Sample s where accession_number = :param";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);

            query.setParameter("param", accessionNumber);
            List<Sample> list = query.list();
            if ((list != null) && !list.isEmpty()) {
                sample = list.get(0);
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Exception occurred in getSampleForAccessionNumber", e);
        }
        return sample;
    }
    // ==============================================================

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByStatusAndDomain(List<String> statuses, String domain) throws LIMSRuntimeException {
        List<Sample> list;
        try {
            String sql = "from Sample s where status in (:param1) and domain = :param2";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameterList("param1", statuses);
            query.setParameter("param2", domain);
            list = query.list();
        } catch (RuntimeException e) {

            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getAllSampleByStatusAndDomain()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesWithPendingQaEvents(Sample sample, boolean filterByQaEventCategory,
            String qaEventCategoryId, boolean filterByDomain) throws LIMSRuntimeException {
        List<Sample> list;

        try {

            String sql = "";

            if (filterByDomain) {
                if (filterByQaEventCategory) {
                    sql = "from Sample s where s.id IN (select sqe.sample.id from SampleQaEvent sqe where"
                            + " sqe.completedDate is null and sqe.qaEvent.category = :param2)  or s.id IN"
                            + " (select aqe.analysis.sampleItem.sample.id from AnalysisQaEvent aqe where"
                            + " aqe.completedDate is null and aqe.qaEvent.category = :param2 and " +
                            // bugzilla 2300 exclude canceled tests
                            "aqe.analysis.status NOT IN (:param3) and" +
                            // make sure we only pick the max revision analyses that have qa events pending
                            "(aqe.analysis.sampleItem.id, aqe.analysis.test.id, aqe.analysis.revision) IN"
                            + " (select b.sampleItem.id, b.test.id, max(b.revision) from Analysis b  group by"
                            + " b.sampleItem.id, b.test.id)) and s.domain = :param order by s.accessionNumber";
                } else {
                    sql = "from Sample s where s.id IN (select sqe.sample.id from SampleQaEvent sqe where"
                            + " sqe.completedDate is null)  or s.id IN (select"
                            + " aqe.analysis.sampleItem.sample.id from AnalysisQaEvent aqe where"
                            + " aqe.completedDate is null and " +
                            // bugzilla 2300 exclude canceled tests
                            "aqe.analysis.status NOT IN (:param3) and " +
                            // make sure we only pick the max revision analyses that have qa events pending
                            "(aqe.analysis.sampleItem.id, aqe.analysis.test.id, aqe.analysis.revision) IN"
                            + " (select b.sampleItem.id, b.test.id, max(b.revision) from Analysis b  group by"
                            + " b.sampleItem.id, b.test.id)) and s.domain = :param order by s.accessionNumber";
                }
            } else {
                if (filterByQaEventCategory) {
                    sql = "from Sample s where s.id IN (select sqe.sample.id from SampleQaEvent sqe where"
                            + " sqe.completedDate is null and sqe.qaEvent.category = :param2)  or s.id IN"
                            + " (select aqe.analysis.sampleItem.sample.id from AnalysisQaEvent aqe where"
                            + " aqe.completedDate is null and aqe.qaEvent.category = :param2 and " +
                            // bugzilla 2300 exclude canceled tests
                            "aqe.analysis.status NOT IN (:param3) and" +
                            // make sure we only pick the max revision analyses that have qa events pending
                            "(aqe.analysis.sampleItem.id, aqe.analysis.test.id, aqe.analysis.revision) IN"
                            + " (select b.sampleItem.id, b.test.id, max(b.revision) from Analysis b  group by"
                            + " b.sampleItem.id, b.test.id)) order by s.accessionNumber";
                } else {
                    sql = "from Sample s where s.id IN (select sqe.sample.id from SampleQaEvent sqe where"
                            + " sqe.completedDate is null)  or s.id IN (select"
                            + " aqe.analysis.sampleItem.sample.id from AnalysisQaEvent aqe where"
                            + " aqe.completedDate is null and " +
                            // bugzilla 2300 exclude canceled tests
                            "aqe.analysis.status NOT IN (:param3) and " +
                            // make sure we only pick the max revision analyses that have qa events pending
                            "(aqe.analysis.sampleItem.id, aqe.analysis.test.id, aqe.analysis.revision) IN"
                            + " (select b.sampleItem.id, b.test.id, max(b.revision) from Analysis b  group by"
                            + " b.sampleItem.id, b.test.id)) order by s.accessionNumber";
                }
            }

            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            if (filterByDomain) {
                query.setParameter("param", sample.getDomain());
            }
            if (filterByQaEventCategory) {
                query.setParameter("param2", qaEventCategoryId);
            }

            List<String> statusesToExclude = new ArrayList<>();
            statusesToExclude.add(ConfigurationProperties.getInstance().getPropertyValue("analysis.status.canceled"));
            query.setParameterList("param3", statusesToExclude);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getSamplesWithPendingQaEvents()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesReceivedOn(String receivedDate) throws LIMSRuntimeException {
        // covers full day so handles time stamps
        return getSamplesReceivedInDateRange(receivedDate, receivedDate);
    }

    /**
     * @see org.openelisglobal.sample.dao.SampleDAO#getSamplesReceivedInDateRange(String,
     *      String) (java.lang.String, java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesReceivedInDateRange(String receivedDateStart, String receivedDateEnd)
            throws LIMSRuntimeException {
        List<Sample> list;

        Calendar start = getCalendarForDateString(receivedDateStart);
        if (GenericValidator.isBlankOrNull(receivedDateEnd)) {
            receivedDateEnd = receivedDateStart;
        }
        Calendar end = getCalendarForDateString(receivedDateEnd);
        // worried about time stamps including time information, so might be missed
        // comparing to midnight (00:00:00.00) on the last day of range.
        end.add(Calendar.DAY_OF_YEAR, 1);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        try {
            String sql = "from Sample as s where s.receivedTimestamp >= :start AND s.receivedTimestamp < :end";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("start", start.getTime());
            query.setParameter("end", end.getTime());
            list = query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getSamplesReceivedInDateRange()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesCollectedOn(String collectionDate) throws LIMSRuntimeException {
        List<Sample> list = null;

        Calendar calendar = getCalendarForDateString(collectionDate);

        try {
            String sql = "from Sample as sample where sample.collectionDate = :date";
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("date", calendar.getTime());
            list = query.list();

        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Sample getSamplesRecievedOn()", e);
        }

        return list;
    }

    private Calendar getCalendarForDateString(String recievedDate) {
        String localeName = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        Locale locale = new Locale(localeName);
        Calendar calendar = Calendar.getInstance(locale);

        Date date = DateUtil.convertStringDateToSqlDate(recievedDate, localeName);
        calendar.setTime(date);
        return calendar;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(List<Integer> inclusiveProjectIdList,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession) throws LIMSRuntimeException {

        String sql = "from Sample s where s.statusId in (:statusList) and s.accessionNumber >= :minAccess and"
                + " s.accessionNumber <= :maxAccess and s.id in (select sp.sample.id from SampleProject"
                + " sp where sp.project.id in (:projectId))";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameterList("statusList", inclusiveStatusIdList);
            query.setParameterList("projectId", inclusiveProjectIdList);
            query.setParameter("minAccess", minAccession);
            query.setParameter("maxAccess", maxAccession);

            List<Sample> sampleList = query.list();

            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesByProjectAndStatusIDAndAccessionRange");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByProjectAndStatusIDAndAccessionRange(String projectId,
            List<Integer> inclusiveStatusIdList, String minAccession, String maxAccession) throws LIMSRuntimeException {

        String sql = "from Sample s where s.statusId in (:statusList) and s.accessionNumber >= :minAccess and"
                + " s.accessionNumber <= :maxAccess and s.id in (select sp.sample.id from SampleProject"
                + " sp where sp.project.id = :projectId)";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameterList("statusList", inclusiveStatusIdList);
            query.setParameter("projectId", Integer.parseInt(projectId));
            query.setParameter("minAccess", minAccession);
            query.setParameter("maxAccess", maxAccession);

            List<Sample> sampleList = query.list();

            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesByProjectAndStatusIDAndAccessionRange");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByAccessionRange(String minAccession, String maxAccession)
            throws LIMSRuntimeException {

        String sql = "from Sample s where s.accessionNumber >= :minAccess and s.accessionNumber <= :maxAccess";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("minAccess", minAccession);
            query.setParameter("maxAccess", maxAccession);

            List<Sample> sampleList = query.list();
            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesByAccessionRange");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumber() throws LIMSRuntimeException {
        String greatestAccessionNumber = null;

        try {
            String sql = "select max(s.accessionNumber) from Sample s";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);

            greatestAccessionNumber = query.uniqueResult();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Exception occurred in SampleDAOImpl.getLargestAccessionNumber", e);
        }

        return greatestAccessionNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumberWithPrefix(String prefix) throws LIMSRuntimeException {
        String greatestAccessionNumber = null;

        try {
            String sql = "select max(s.accessionNumber) from Sample s where s.accessionNumber like :prefix";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("prefix", prefix + "%");
            greatestAccessionNumber = query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Exception occurred in SampleNumberDAOImpl.getLargestAccessionNumberWithPrefix", e);
        }

        return greatestAccessionNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public String getLargestAccessionNumberMatchingPattern(String startingWith, int accessionSize)
            throws LIMSRuntimeException {
        String greatestAccessionNumber = null;

        try {
            String sql = "select max(s.accessionNumber) from Sample s where s.accessionNumber LIKE :starts and"
                    + " length(s.accessionNumber) = :numberSize";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("starts", startingWith + "%");
            query.setParameter("numberSize", accessionSize);
            greatestAccessionNumber = query.uniqueResult();
        } catch (RuntimeException e) {
            handleException(e, "getLargestAccessionNumberMatchingPattern");
        }

        return greatestAccessionNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesWithPendingQaEventsByService(String serviceId) throws LIMSRuntimeException {
        String sql = "Select sqa.sample From SampleQaEvent sqa where sqa.sample.id IN (select sa.sample.id from"
                + " SampleOrganization sa where sa.organization.id = :serviceId) ";

        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("serviceId", Integer.parseInt(serviceId));
            List<Sample> samples = query.list();
            return samples;
        } catch (HibernateException e) {
            handleException(e, "getSamplesWithPendingQaEventsByService");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getConfirmationSamplesReceivedInDateRange(Date receivedDateStart, Date receivedDateEnd)
            throws LIMSRuntimeException {
        String sql = "from Sample s where s.isConfirmation = true and s.receivedTimestamp BETWEEN :lowDate AND"
                + " :highDate";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("lowDate", receivedDateStart);
            query.setParameter("highDate", receivedDateEnd);

            List<Sample> list = query.list();
            return list;
        } catch (HibernateException e) {
            handleException(e, "getResultsInDateRange");
        }

        return null;
    }

    @Transactional(readOnly = true)
    public List<Sample> getSamplesBySampleItem(Integer sampleitemId) throws LIMSRuntimeException {

        String sql = "from Sample s where s.id in (select si.sample.id from SampleItem si where si.id ="
                + " :sampleitemId)";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("sampleitemId", sampleitemId);
            List<Sample> sampleList = query.list();
            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesBySampleItem");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Sample getSampleByReferringId(String referringId) throws LIMSRuntimeException {

        String sql = "from Sample s where s.referringId = :referringId";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("referringId", referringId);
            Sample sample = query.uniqueResult();
            return sample;
        } catch (HibernateException e) {
            handleException(e, "getSampleByReferringId");
        }

        return null;
    }

    @Override
    public List<Sample> getAllMissingFhirUuid() {
        String sql = "from Sample s where s.fhirUuid is NULL";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            List<Sample> sampleList = query.list();
            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesBySampleItem");
        }
        return new ArrayList<>();
    }

    @Override
    public List<Sample> getSamplesByAnalysisIds(List<String> analysisIds) {
        String hql = "FROM Sample s WHERE s.id IN (SELECT si.sample.id FROM SampleItem si WHERE si.id IN (SELECT"
                + " a.sampleItem.id FROM Analysis a WHERE a.id IN (:analysisIds)))";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(hql, Sample.class);
            query.setParameter("analysisIds",
                    analysisIds.stream().map(e -> Integer.parseInt(e)).collect(Collectors.toList()));
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getSamplesBySampleItem");
        }
        return new ArrayList<>();
    }

    @Override
    public List<Sample> getSamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        String hql = "FROM Sample s WHERE s.enteredDate BETWEEN :lowerDate AND :upperDate AND s.id IN (SELECT"
                + " sr.sampleId FROM SampleRequester sr WHERE sr.requesterId = :requesterId AND"
                + " sr.requesterTypeId = (SELECT rt.id FROM RequesterType rt WHERE rt.requesterType ="
                + " 'organization' ))";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(hql, Sample.class);
            query.setParameter("requesterId", Integer.parseInt(referringSiteId));
            query.setParameter("lowerDate", lowerDate.atStartOfDay());
            query.setParameter("upperDate", upperDate.atTime(LocalTime.MAX));
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getSamplesForSiteBetweenOrderDates");
        }
        return new ArrayList<>();
    }

    @Override
    public List<Sample> getStudySamplesForSiteBetweenOrderDates(String referringSiteId, LocalDate lowerDate,
            LocalDate upperDate) {
        String hql = "FROM Sample s WHERE s.enteredDate BETWEEN :lowerDate AND :upperDate AND s.id IN (SELECT"
                + " so.sample.id FROM SampleOrganization so WHERE so.organization.id = :requesterId )";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(hql, Sample.class);
            query.setParameter("requesterId", Integer.parseInt(referringSiteId));
            query.setParameter("lowerDate", lowerDate.atStartOfDay());
            query.setParameter("upperDate", upperDate.atTime(LocalTime.MAX));
            return query.list();
        } catch (HibernateException e) {
            handleException(e, "getSamplesForSiteBetweenOrderDates");
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sample> getSamplesByPriority(OrderPriority priority) throws LIMSRuntimeException {
        String sql = "from Sample s where s.priority = :oderpriority";
        try {
            Query<Sample> query = entityManager.unwrap(Session.class).createQuery(sql, Sample.class);
            query.setParameter("oderpriority", priority.name());
            List<Sample> sampleList = query.list();
            return sampleList;
        } catch (HibernateException e) {
            handleException(e, "getSamplesByPriority");
        }

        return null;
    }
}
