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
package org.openelisglobal.person.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Vector;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.person.dao.PersonDAO;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class PersonDAOImpl extends BaseDAOImpl<Person, String> implements PersonDAO {

    public PersonDAOImpl() {
        super(Person.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Person person) throws LIMSRuntimeException {
        try {
            Person pers = entityManager.unwrap(Session.class).get(Person.class, person.getId());
            if (pers != null) {
                PropertyUtils.copyProperties(person, pers);
            } else {
                person.setId(null);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Person getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> getAllPersons() throws LIMSRuntimeException {
        List<Person> list = new Vector<>();
        try {
            String sql = "from Person";
            Query<Person> query = entityManager.unwrap(Session.class).createQuery(sql, Person.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Person getAllPersons()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> getPageOfPersons(int startingRecNo) throws LIMSRuntimeException {
        List<Person> list = new Vector<>();
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Person t order by t.id";
            Query<Person> query = entityManager.unwrap(Session.class).createQuery(sql, Person.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Person getPageOfPersons()", e);
        }

        return list;
    }

    public Person readPerson(String idString) {
        Person person = null;
        try {
            person = entityManager.unwrap(Session.class).get(Person.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Person readPerson()", e);
        }

        return person;
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonByLastName(String lastName) throws LIMSRuntimeException {
        List<Person> list = null;
        try {
            String sql = "from Person p where p.lastName = :lastName";
            Query<Person> query = entityManager.unwrap(Session.class).createQuery(sql, Person.class);
            query.setParameter("lastName", lastName);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Person getPersonByLastName()", e);
        }

        if (list.size() > 0) {
            return list.get(0);
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonById(String personId) throws LIMSRuntimeException {
        String sql = "From Person p where id = :personId";
        try {
            Query<Person> query = entityManager.unwrap(Session.class).createQuery(sql, Person.class);
            query.setParameter("personId", Integer.parseInt(personId));
            Person person = query.uniqueResult();
            return person;
        } catch (HibernateException e) {
            handleException(e, "getPersonById");
        }
        return null;
    }
}
