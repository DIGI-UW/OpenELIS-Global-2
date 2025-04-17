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
 */
package org.openelisglobal.citystatezip.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.citystatezip.dao.CityStateZipDAO;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.openelisglobal.citystatezip.valueholder.CityView;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
/**
 * @author benzd1
 */
@Component
@Transactional
public class CityStateZipDAOImpl extends BaseDAOImpl<CityStateZip, String> implements CityStateZipDAO {

    public CityStateZipDAOImpl() {
        super(CityStateZip.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getCities(String filter) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();

        try {
            String sql = "from CityView csz where upper(csz.city) like upper(:param) order by upper(csz.city)";

            Query<CityView> query = entityManager.unwrap(Session.class).createQuery(sql, CityView.class);
            query.setParameter("param", filter + "%");
            // for performance
            query.setMaxResults(100);
            List<CityView> list = query.list();

            for (int i = 0; i < list.size(); i++) {
                CityStateZip csz = new CityStateZip();
                csz.setId(String.valueOf(i));
                CityView c = new CityView();
                c = list.get(i);
                csz.setCity(c.getCity());
                cityStateZips.add(csz);
            }

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getCities(String filter)", e);
        }

        return cityStateZips;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getZipCodesByCity(CityStateZip cityStateZip) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        try {

            String sql = "select distinct csz.zipCode, csz.city from CityStateZip csz where upper(csz.city) ="
                    + " :param";

            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());

            List<?> list = query.list();

            // different behavior using select distinct...we need to put data
            // into valueholder manually and load up the list to return
            for (int i = 0; i < list.size(); i++) {
                Object[] obj = (Object[]) list.get(i);
                String zipCode = ((String) obj[0]).trim();
                String city = ((String) obj[1]).trim();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                String id = String.valueOf(i);
                CityStateZip csz = new CityStateZip();
                csz.setId(id);
                csz.setCity(city);
                csz.setZipCode(zipCode);
                cityStateZips.add(csz);
            }

            return cityStateZips;

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getZipCodesByCity()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getCitiesByZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        try {
            String sql = "select distinct csz.city, csz.zipCode from CityStateZip csz where csz.zipCode = :param";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("param", cityStateZip.getZipCode().trim());

            List<?> list = query.list();
            // different behavior using select distinct...we need to put data
            // into valueholder manually and load up the list to return
            for (int i = 0; i < list.size(); i++) {
                Object[] obj = (Object[]) list.get(i);
                String city = ((String) obj[0]).trim();
                String zipCode = ((String) obj[1]).trim();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                String id = String.valueOf(i);
                CityStateZip csz = new CityStateZip();
                csz.setId(id);
                csz.setCity(city);
                csz.setZipCode(zipCode);
                cityStateZips.add(csz);
            }
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCitiesByZipCode()", e);
        }

        return cityStateZips;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getAllStateCodes() throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        try {
            // bugzilla 1908 postgres error on order by
            String sql = "select distinct upper(csz.state) from CityStateZip csz order by upper(csz.state)";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            List list = query.list();

            for (int i = 0; i < list.size(); i++) {
                String state = ((String) list.get(i)).trim();
                CityStateZip cityStateZip = new CityStateZip();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                cityStateZip.setId(String.valueOf(i));
                cityStateZip.setState(state);
                cityStateZips.add(cityStateZip);
            }
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getAllStateCodes()", e);
        }

        return cityStateZips;
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getCityStateZipByCityAndZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException {
        CityStateZip csz = null;
        try {
            String sql = "from CityStateZip csz where upper(csz.city) = :param and csz.zipCode = :param2";
            Query<CityStateZip> query = entityManager.unwrap(Session.class).createQuery(sql, CityStateZip.class);
            query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
            query.setParameter("param2", cityStateZip.getZipCode());

            List<CityStateZip> list = query.list();

            if (list != null && list.size() > 0) {
                csz = list.get(0);
            }

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCityStateZipByCityAndZipCode()", e);
        }

        return csz;
    }

    @Override
    @Transactional(readOnly = true)
    public CityStateZip getState(CityStateZip cityStateZip) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        CityStateZip csz = null;
        try {
            String sql = "select distinct csz.state from CityStateZip csz where upper(csz.state) = :param";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);

            query.setParameter("param", cityStateZip.getState().trim().toUpperCase());
            List<?> list = query.list();

            for (int i = 0; i < list.size(); i++) {
                String state = ((String) list.get(i)).trim();
                CityStateZip cszip = new CityStateZip();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                cszip.setId(String.valueOf(i));
                cszip.setState(state);
                cityStateZips.add(cszip);
            }

            if (!cityStateZips.isEmpty()) {
                csz = cityStateZips.get(0);
            }

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getState()", e);
        }

        return csz;
    }

    // bugzilla 1765
    @Override
    @Transactional(readOnly = true)
    public CityStateZip getCity(CityStateZip cityStateZip) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        CityStateZip csz = null;
        try {
            String sql = "select distinct csz.city from CityStateZip csz where upper(csz.city) = :param";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);

            query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
            List<?> list = query.list();

            for (int i = 0; i < list.size(); i++) {
                String city = ((String) list.get(i)).trim();
                CityStateZip cszip = new CityStateZip();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                cszip.setId(String.valueOf(i));
                cszip.setCity(city);
                cityStateZips.add(cszip);
            }

            if (!cityStateZips.isEmpty()) {
                csz = cityStateZips.get(0);
            }

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getCity()", e);
        }

        return csz;
    }

    // bugzilla 1765
    @Override
    @Transactional(readOnly = true)
    public CityStateZip getZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException {
        List<CityStateZip> cityStateZips = new ArrayList<>();
        CityStateZip csz = null;
        try {
            String sql = "select distinct csz.zipCode from CityStateZip csz where upper(csz.zipCode) = :param";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);

            query.setParameter("param", cityStateZip.getZipCode().trim().toUpperCase());
            List<?> list = query.list();

            for (int i = 0; i < list.size(); i++) {
                String zipCode = ((String) list.get(i)).trim();
                CityStateZip cszip = new CityStateZip();
                // give the cityStateZip object an artificial id needed for
                // autocomplete but not stored anywhere
                cszip.setId(String.valueOf(i));
                cszip.setZipCode(zipCode);
                cityStateZips.add(cszip);
            }

            if (!cityStateZips.isEmpty()) {
                csz = cityStateZips.get(0);
            }

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CityStateZip getZip()", e);
        }

        return csz;
    }

    // bugzilla 1765 - validate city state zip combination
    @Override
    public boolean isCityStateZipComboValid(CityStateZip cityStateZip) throws LIMSRuntimeException {
        boolean isValid = false;
        try {
            String sql = null;
            String state = cityStateZip.getState();
            String city = cityStateZip.getCity();
            String zipCode = cityStateZip.getZipCode();
            List<?> list = new ArrayList<>();

            if (!StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.zipCode, csz.city, csz.state from CityStateZip csz where"
                        + " upper(csz.city) = :param and csz.zipCode = :param2 and upper(csz.state) =" + " :param3";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                query.setParameter("param2", cityStateZip.getZipCode().trim().toUpperCase());
                query.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                list = query.list();
            } else if (!StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.city, csz.state from CityStateZip csz where upper(csz.city) ="
                        + " :param and upper(csz.state) = :param3";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                query.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                list = query.list();
            } else if (StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.zipCode, csz.city from CityStateZip csz where upper(csz.city) ="
                        + " :param and csz.zipCode = :param2";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                query.setParameter("param2", cityStateZip.getZipCode().trim().toUpperCase());
                list = query.list();
            } else if (!StringUtil.isNullorNill(state) && StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.zipCode, csz.state from CityStateZip csz where csz.zipCode ="
                        + " :param2 and upper(csz.state) = :param3";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param2", cityStateZip.getZipCode().trim().toUpperCase());
                query.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                list = query.list();
            } else if (StringUtil.isNullorNill(state) && StringUtil.isNullorNill(zipCode)
                    && !StringUtil.isNullorNill(city)) {
                sql = "select distinct csz.city from CityStateZip csz where upper(csz.city) = :param";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                list = query.list();
            } else if (StringUtil.isNullorNill(state) && StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.zipCode from CityStateZip csz where csz.zipCode = :param2";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param2", cityStateZip.getZipCode().trim().toUpperCase());
                list = query.list();
            } else if (StringUtil.isNullorNill(city) && StringUtil.isNullorNill(zipCode)
                    && !StringUtil.isNullorNill(state)) {
                sql = "select distinct csz.state from CityStateZip csz where upper(csz.state) = :param3";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                list = query.list();
            } else if (StringUtil.isNullorNill(state) && StringUtil.isNullorNill(city)
                    && StringUtil.isNullorNill(zipCode)) {
                isValid = true;
            }

            if (!list.isEmpty()) {
                isValid = true;
            }

            return isValid;

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in isCityStateZipValidForHumanSampleEntry()", e);
        }
    }

    // bugzilla 1765 - validate city state zip combination
    @Override
    @Transactional(readOnly = true)
    public List<CityStateZip> getValidCityStateZipCombosForHumanSampleEntry(CityStateZip cityStateZip)
            throws LIMSRuntimeException {

        List<CityStateZip> cityStateZips = new ArrayList<>();
        try {
            String sql = null;
            String state = cityStateZip.getState();
            String city = cityStateZip.getCity();
            String zipCode = cityStateZip.getZipCode();
            List<?> listByCity;
            List<?> listByZip;
            List<?> list;

            // 1) ALL THREE PARAMETERS NEED TO BE SEARCHED (CITY, STATE, ZIP)
            if (!StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                // first get all where city matches (and state)
                sql = "select distinct csz.zipCode, csz.city, csz.state from CityStateZip csz where"
                        + " upper(csz.city) = :param and upper(csz.state) = :param3 order by csz.zipCode";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                query.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                listByCity = query.list();
                for (int i = 0; i < listByCity.size(); i++) {
                    Object[] obj = (Object[]) listByCity.get(i);
                    String z = ((String) obj[0]).trim();
                    String c = ((String) obj[1]).trim();
                    String s = ((String) obj[2]).trim();
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }
                // now get all where zip matches (and state)
                sql = "select distinct csz.zipCode, csz.city, csz.state from CityStateZip csz where"
                        + " csz.zipCode = :param2 and upper(csz.state) = :param3 order by csz.city";
                Query<?> query2 = entityManager.unwrap(Session.class).createQuery(sql);
                query2.setParameter("param2", cityStateZip.getZipCode().trim());
                query2.setParameter("param3", cityStateZip.getState().trim().toUpperCase());
                listByZip = query2.list();
                int j = cityStateZips.size();
                for (int i = 0; i < listByZip.size(); i++) {
                    Object[] obj = (Object[]) listByZip.get(i);
                    String z = ((String) obj[0]).trim();
                    String c = ((String) obj[1]).trim();
                    String s = ((String) obj[2]).trim();
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(j + i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }

                // for case that state entered is incorrect for both city and zip we should list
                // all matches for city and zip regardless of state
                if (cityStateZips.size() == 0) {
                    sql = "select distinct csz.zipCode, csz.city, csz.state from CityStateZip csz where"
                            + " upper(csz.city) = :param order by csz.zipCode";
                    Query<?> query3 = entityManager.unwrap(Session.class).createQuery(sql);
                    query3.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                    listByCity = query3.list();
                    for (int i = 0; i < listByCity.size(); i++) {
                        Object[] obj = (Object[]) listByCity.get(i);
                        String z = ((String) obj[0]).trim();
                        String c = ((String) obj[1]).trim();
                        String s = ((String) obj[2]).trim();
                        // give the cityStateZip object an artificial id needed for
                        // autocomplete but not stored anywhere
                        String id = String.valueOf(i);
                        CityStateZip csz = new CityStateZip();
                        csz.setId(id);
                        setCityStateZipValueholder(csz, c, s, z);
                        cityStateZips.add(csz);
                    }
                    // now get all where zip matches
                    sql = "select distinct csz.zipCode, csz.city, csz.state from CityStateZip csz where"
                            + " csz.zipCode = :param2 order by csz.city";
                    Query<?> query4 = entityManager.unwrap(Session.class).createQuery(sql);
                    query4.setParameter("param2", cityStateZip.getZipCode().trim());
                    listByZip = query4.list();
                    int k = cityStateZips.size();
                    for (int i = 0; i < listByZip.size(); i++) {
                        Object[] obj = (Object[]) listByZip.get(i);
                        String z = ((String) obj[0]).trim();
                        String c = ((String) obj[1]).trim();
                        String s = ((String) obj[2]).trim();
                        // give the cityStateZip object an artificial id needed for
                        // autocomplete but not stored anywhere
                        String id = String.valueOf(k + i);
                        CityStateZip csz = new CityStateZip();
                        csz.setId(id);
                        setCityStateZipValueholder(csz, c, s, z);
                        cityStateZips.add(csz);
                    }
                }
            } else if (!StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.city, csz.state from CityStateZip csz where upper(csz.city) ="
                        + " :param order by csz.state";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                list = query.list();
                for (int i = 0; i < list.size(); i++) {
                    Object[] obj = (Object[]) list.get(i);
                    String c = ((String) obj[0]).trim();
                    String s = ((String) obj[1]).trim();
                    String z = null;
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }
            } else if (StringUtil.isNullorNill(state) && !StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.zipCode, csz.city from CityStateZip csz where upper(csz.city) ="
                        + " :param order by csz.zipCode";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param", cityStateZip.getCity().trim().toUpperCase());
                listByCity = query.list();
                for (int i = 0; i < listByCity.size(); i++) {
                    Object[] obj = (Object[]) listByCity.get(i);
                    String z = ((String) obj[0]).trim();
                    String c = ((String) obj[1]).trim();
                    String s = null;
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }
                // now get all where zip matches
                sql = "select distinct csz.zipCode, csz.city from CityStateZip csz where csz.zipCode ="
                        + " :param2 order by csz.city";
                Query<?> query2 = entityManager.unwrap(Session.class).createQuery(sql);
                query2.setParameter("param2", cityStateZip.getZipCode().trim());
                listByZip = query2.list();
                int k = cityStateZips.size();
                for (int i = 0; i < listByZip.size(); i++) {
                    Object[] obj = (Object[]) listByZip.get(i);
                    String z = ((String) obj[0]).trim();
                    String c = ((String) obj[1]).trim();
                    String s = null;
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(k + i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }
            } else if (!StringUtil.isNullorNill(state) && StringUtil.isNullorNill(city)
                    && !StringUtil.isNullorNill(zipCode)) {
                sql = "select distinct csz.state, csz.zipCode from CityStateZip csz where csz.zipCode ="
                        + " :param2 order by csz.state";
                Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
                query.setParameter("param2", cityStateZip.getZipCode().trim());
                list = query.list();
                for (int i = 0; i < list.size(); i++) {
                    Object[] obj = (Object[]) list.get(i);
                    String s = ((String) obj[1]).trim();
                    String z = ((String) obj[2]).trim();
                    String c = null;
                    // give the cityStateZip object an artificial id needed for
                    // autocomplete but not stored anywhere
                    String id = String.valueOf(i);
                    CityStateZip csz = new CityStateZip();
                    csz.setId(id);
                    setCityStateZipValueholder(csz, c, s, z);
                    cityStateZips.add(csz);
                }
            }

            return cityStateZips;

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getValidCityStateZipCombosForHumanSampleEntry()", e);
        }
    }

    // make sure values are not null but blank if non-existant
    private CityStateZip setCityStateZipValueholder(CityStateZip csz, String city, String state, String zip) {
        if (StringUtil.isNullorNill(city)) {
            city = "";
        }
        if (StringUtil.isNullorNill(state)) {
            state = "";
        }
        if (StringUtil.isNullorNill(zip)) {
            zip = "";
        }

        csz.setState(state);
        csz.setCity(city);
        csz.setZipCode(zip);
        return csz;
    }

    // bugzilla 2393
    @Override
    @Transactional(readOnly = true)
    public String getCountyCodeByStateAndZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException {
        String countyCode = null;
        try {
            String hql = "SELECT csz.countyFips FROM CityStateZip csz WHERE upper(csz.state) = :param AND csz.zipCode = :param2";

            List<?> list = entityManager.unwrap(Session.class).createQuery(hql)
                    .setParameter("param", cityStateZip.getState().trim().toUpperCase())
                    .setParameter("param2", cityStateZip.getZipCode().trim()).list();

            if (list != null && !list.isEmpty()) {
                countyCode = (String) list.get(0);
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCountyCodeByStateAndZipCode()", e);
        }

        return countyCode;
    }
}
