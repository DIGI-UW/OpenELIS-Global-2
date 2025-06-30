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
package org.openelisglobal.common.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.valueholder.BaseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Caleb
 * @param <T>
 */
@Component
@DependsOn({ "defaultConfigurationProperties", "springContext" })
@Transactional
public abstract class BaseDAOImpl<T extends BaseObject<PK>, PK extends Serializable>
        implements BaseDAO<T, PK>, IActionConstants {

    private enum DBComparison {
        EQ, LIKE, IN
    }

    private final Class<T> classType;

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    public BaseDAOImpl(Class<T> clazz) {
        classType = clazz;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> get(PK id) {
        try {
            T object = entityManager.find(classType, id);
            return Optional.ofNullable(object);
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "get", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> get(List<PK> ids) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.classType);
        Root<T> root = criteriaQuery.from(this.classType);
        criteriaQuery.select(root);
        List<PropertyValueComparison> whereComparisonOperations = new ArrayList<>();
        whereComparisonOperations.add(new PropertyValueComparison("id", ids, DBComparison.IN));
        this.addWhere(criteriaBuilder, criteriaQuery, root, whereComparisonOperations);

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAll() {
        return getAllMatchingOrdered(new HashMap<>(), new ArrayList<>(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatching(String propertyName, Object propertyValue) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getAllMatching(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatching(Map<String, Object> propertyValues) {
        return getAllMatchingOrdered(propertyValues, new ArrayList<>(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLike(String propertyName, String propertyValue) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getAllLike(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLike(Map<String, String> propertyValues) {
        return getAllLikeOrdered(propertyValues, new ArrayList<>(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllOrdered(String orderProperty, boolean descending) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getAllOrdered(orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllOrdered(List<String> orderProperties, boolean descending) {
        return getAllMatchingOrdered(new HashMap<>(), orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, String orderProperty,
            boolean descending) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getAllMatchingOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getAllMatchingOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty, boolean descending) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getAllMatchingOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, List<String> orderProperties,
            boolean descending) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.classType);
            Root<T> root = criteriaQuery.from(this.classType);
            criteriaQuery.select(root);
            List<PropertyValueComparison> whereComparisonOperations = new ArrayList<>();
            for (Entry<String, Object> entrySet : propertyValues.entrySet()) {
                whereComparisonOperations
                        .add(new PropertyValueComparison(entrySet.getKey(), entrySet.getValue(), DBComparison.EQ));
            }
            this.addWhere(criteriaBuilder, criteriaQuery, root, whereComparisonOperations);

            Map<String, Boolean> orderByMap = new HashMap<>();
            for (String orderProperty : orderProperties) {
                orderByMap.put(orderProperty, descending);
            }
            this.addOrder(criteriaBuilder, criteriaQuery, root, orderByMap);

            return entityManager.createQuery(criteriaQuery).getResultList();

            // Map<String, String> aliases = new HashMap<>();
            // Session session = entityManager.unwrap(Session.class);
            // Criteria criteria = session.createCriteria(classType);
            // for (Entry<String, Object> entrySet : propertyValues.entrySet()) {
            // addRestriction(criteria, entrySet.getKey(), entrySet.getValue(), aliases);
            // }
            // for (String orderProperty : orderProperties) {
            // addOrder(criteria, orderProperty, descending, aliases);
            // }
            // return criteria.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in " + this.getClass().getSimpleName() + " " + "getAllMatchingOrdered", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLikeOrdered(String propertyName, String propertyValue, String orderProperty,
            boolean descending) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getAllLikeOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLikeOrdered(String propertyName, String propertyValue, List<String> orderProperties,
            boolean descending) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getAllLikeOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLikeOrdered(Map<String, String> propertyValues, String orderProperty, boolean descending) {
        List<String> orderProperties = new ArrayList<>();

        return getAllLikeOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllLikeOrdered(Map<String, String> propertyValues, List<String> orderProperties,
            boolean descending) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(classType);
            Root<T> root = criteriaQuery.from(classType);
            criteriaQuery.select(root);

            List<PropertyValueComparison> whereComparisonOperations = new ArrayList<>();
            for (Entry<String, String> entrySet : propertyValues.entrySet()) {
                whereComparisonOperations
                        .add(new PropertyValueComparison(entrySet.getKey(), entrySet.getValue(), DBComparison.LIKE));
            }
            addWhere(criteriaBuilder, criteriaQuery, root, whereComparisonOperations);

            Map<String, Boolean> orderByMap = new HashMap<>();
            for (String orderProperty : orderProperties) {
                orderByMap.put(orderProperty, descending);
            }
            this.addOrder(criteriaBuilder, criteriaQuery, root, orderByMap);

            return entityManager.createQuery(criteriaQuery).getResultList();

            // Map<String, String> aliases = new HashMap<>();
            // Session session = entityManager.unwrap(Session.class);
            // Criteria criteria = session.createCriteria(classType);
            // for (Entry<String, String> entrySet : propertyValues.entrySet()) {
            // addLikeRestriction(criteria, entrySet.getKey(), entrySet.getValue(),
            // aliases);
            // }
            // for (String orderProperty : orderProperties) {
            // addOrder(criteria, orderProperty, descending, aliases);
            // }
            // return criteria.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "getAllLikeOrdered",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getPage(int startingRecNo) {
        return getOrderedPage(new ArrayList<>(), false, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        return getMatchingPage(propertyValues, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo) {
        return getMatchingOrderedPage(propertyValues, new ArrayList<>(), false, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikePage(String propertyName, String propertyValue, int startingRecNo) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        return getLikePage(propertyValues, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikePage(Map<String, String> propertyValues, int startingRecNo) {
        return getLikeOrderedPage(propertyValues, new ArrayList<>(), false, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getOrderedPage(String orderProperty, boolean descending, int startingRecNo) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getOrderedPage(orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getOrderedPage(List<String> orderProperties, boolean descending, int startingRecNo) {
        return getMatchingOrderedPage(new HashMap<>(), orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, String orderProperty,
            boolean descending, int startingRecNo) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getMatchingOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending, int startingRecNo) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getMatchingOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty, boolean descending,
            int startingRecNo) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getMatchingOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, List<String> orderProperties,
            boolean descending, int startingRecNo) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.classType);
            Root<T> root = criteriaQuery.from(this.classType);
            criteriaQuery.select(root);

            List<PropertyValueComparison> whereComparisonOperations = new ArrayList<>();
            for (Entry<String, Object> entrySet : propertyValues.entrySet()) {
                whereComparisonOperations
                        .add(new PropertyValueComparison(entrySet.getKey(), entrySet.getValue(), DBComparison.EQ));
            }
            addWhere(criteriaBuilder, criteriaQuery, root, whereComparisonOperations);

            Map<String, Boolean> orderByMap = new HashMap<>();
            for (String orderProperty : orderProperties) {
                orderByMap.put(orderProperty, descending);
            }
            this.addOrder(criteriaBuilder, criteriaQuery, root, orderByMap);

            TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);
            typedQuery.setFirstResult(startingRecNo - 1);
            typedQuery.setMaxResults(
                    Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            return typedQuery.getResultList();

            // Map<String, String> aliases = new HashMap<>();
            // Session session = entityManager.unwrap(Session.class);
            // Criteria criteria = session.createCriteria(classType);
            // for (Entry<String, Object> entrySet : propertyValues.entrySet()) {
            // addRestriction(criteria, entrySet.getKey(), entrySet.getValue(), aliases);
            // }
            // for (String orderProperty : orderProperties) {
            // addOrder(criteria, orderProperty, descending, aliases);
            // }
            // criteria.setFirstResult(startingRecNo - 1);
            // criteria.setMaxResults(Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
            // + 1);
            // return criteria.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in " + this.getClass().getSimpleName() + " " + "getMatchingOrderedPage", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikeOrderedPage(String propertyName, String propertyValue, String orderProperty,
            boolean descending, int startingRecNo) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getLikeOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikeOrderedPage(String propertyName, String propertyValue, List<String> orderProperties,
            boolean descending, int startingRecNo) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);

        return getLikeOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikeOrderedPage(Map<String, String> propertyValues, String orderProperty, boolean descending,
            int startingRecNo) {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add(orderProperty);

        return getLikeOrderedPage(propertyValues, orderProperties, descending, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getLikeOrderedPage(Map<String, String> propertyValues, List<String> orderProperties,
            boolean descending, int startingRecNo) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.classType);
            Root<T> root = criteriaQuery.from(this.classType);
            criteriaQuery.select(root);

            List<PropertyValueComparison> whereComparisonOperations = new ArrayList<>();
            for (Entry<String, String> entrySet : propertyValues.entrySet()) {
                whereComparisonOperations
                        .add(new PropertyValueComparison(entrySet.getKey(), entrySet.getValue(), DBComparison.LIKE));
            }
            addWhere(criteriaBuilder, criteriaQuery, root, whereComparisonOperations);

            Map<String, Boolean> orderByMap = new HashMap<>();
            for (String orderProperty : orderProperties) {
                orderByMap.put(orderProperty, descending);
            }
            this.addOrder(criteriaBuilder, criteriaQuery, root, orderByMap);

            TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);
            typedQuery.setFirstResult(startingRecNo - 1);
            typedQuery.setMaxResults(
                    Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            return typedQuery.getResultList();

            // Map<String, String> aliases = new HashMap<>();
            // Session session = entityManager.unwrap(Session.class);
            // Criteria criteria = session.createCriteria(classType);
            // for (Entry<String, String> entrySet : propertyValues.entrySet()) {
            // addLikeRestriction(criteria, entrySet.getKey(), entrySet.getValue(),
            // aliases);
            // }
            // for (String orderProperty : orderProperties) {
            // addOrder(criteria, orderProperty, descending, aliases);
            // }
            // criteria.setFirstResult(startingRecNo - 1);
            // criteria.setMaxResults(Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
            // + 1);
            // return criteria.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "getLikeOrderedPage",
                    e);
        }
    }

    @Override
    public PK insert(T object) {
        try {
            entityManager.persist(object);
            entityManager.flush();
            return object.getId();
            // Session session = entityManager.unwrap(Session.class);
            // PK id = (PK) session.save(object);
            // session.flush();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "insert", e);
        }
    }

    @Override
    public T update(T object) {
        try {
            T dbObject = entityManager.merge(object);
            entityManager.flush();
            return dbObject;
            // Session session = entityManager.unwrap(Session.class);
            // T dbObject = (T) session.merge(object);
            // session.flush();
            // return dbObject;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "save", e);
        }
    }

    @Override
    public void delete(T object) {
        try {
            entityManager.remove(object);
            entityManager.flush();
            // Session session = entityManager.unwrap(Session.class);
            // session.delete(object);
            // session.flush();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "delete", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCount() {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
            criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(this.classType)));
            return entityManager.createQuery(criteriaQuery).getSingleResult().intValue();

            // Integer rowCount = 0;
            // Session session = entityManager.unwrap(Session.class);
            // Criteria criteria = session.createCriteria(classType);
            // criteria.setProjection(Projections.rowCount());
            // List<Long> results = criteria.list();
            //
            // if (results != null) {
            // rowCount = results.get(0).intValue();
            // } else {
            // LogEvent.logError(this.getClass().getSimpleName(), "getCount()",
            // "could not count number of objects for class" + classType.getName());
            // rowCount = -1;
            // }
            // return rowCount;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "getCount", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> getNext(String id) {
        int start = (Integer.valueOf(id)).intValue();

        List<T> list;
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(classType);
            Root<T> root = criteriaQuery.from(classType);
            criteriaQuery.select(root);

            Predicate greaterThanPredicate = criteriaBuilder.greaterThan(root.get("id"), start);
            criteriaQuery.where(greaterThanPredicate);

            TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
            query.setFirstResult(0);
            query.setMaxResults(1);
            list = query.getResultList();
            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(list.get(0));
            }
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "getNext", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> getPrevious(String id) {
        int start = (Integer.valueOf(id)).intValue();

        List<T> list;
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(classType);
            Root<T> root = criteriaQuery.from(classType);
            criteriaQuery.select(root);

            Predicate greaterThanPredicate = criteriaBuilder.lessThan(root.get("id"), start);
            criteriaQuery.where(greaterThanPredicate);

            TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
            query.setFirstResult(0);
            query.setMaxResults(1);
            list = query.getResultList();
            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(list.get(0));
            }
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + "getPrevious", e);
        }
    }

    /**
     * @return object name that Hibernate uses
     */
    protected String getObjectName() {
        return classType.getSimpleName();
    }

    @Override
    @Transactional(readOnly = true)
    // TODO standardize table names and use that same standardization to generate
    // the table name from the object instead of querying the metadata;
    // ImplicitNamingStrategyJpaCompliantImpl
    public String getTableName() {
        AbstractEntityPersister persister = (AbstractEntityPersister) entityManager.unwrap(Session.class)
                .getSessionFactory().getClassMetadata(classType);
        String tableName = persister.getTableName();
        return tableName.substring(tableName.indexOf('.') + 1);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addWhere(CriteriaBuilder criteriaBuilder, CriteriaQuery<T> criteriaQuery, Root<T> root,
            List<PropertyValueComparison> whereComparisonOperations) {
        List<Predicate> wherePredicates = new ArrayList<>();
        for (PropertyValueComparison comparisonOperation : whereComparisonOperations) {
            String propertyName = comparisonOperation.getPropertyName();
            Object propertyValue = comparisonOperation.getPropertyValue();
            Path pathToProperty = getPathToProperty(root, propertyName);
            if ((propertyName.endsWith("id") || propertyName.endsWith("Id")) && propertyValue instanceof String
                    && org.apache.commons.validator.GenericValidator.isInt((String) propertyValue)) {
                propertyValue = Integer.valueOf((String) propertyValue);
            }
            Predicate predicate;
            switch (comparisonOperation.getComparison()) {
            case EQ:
                predicate = criteriaBuilder.equal(pathToProperty, propertyValue);
                break;
            case LIKE:
                predicate = criteriaBuilder.like(criteriaBuilder.lower(pathToProperty),
                        "%" + ((String) propertyValue).toLowerCase() + "%");
                break;
            case IN:
                In<String> inClause = criteriaBuilder.in(root.get(propertyName));
                for (String id : (List<String>) propertyValue) {
                    inClause.value(id);
                }
                predicate = inClause;
                break;
            default:
                throw new UnsupportedOperationException();
            }
            wherePredicates.add(predicate);
        }
        criteriaQuery.where(wherePredicates.toArray(new Predicate[wherePredicates.size()]));
    }

    @SuppressWarnings("rawtypes")
    protected void addOrder(CriteriaBuilder criteriaBuilder, CriteriaQuery<T> criteriaQuery, Root<T> root,
            Map<String, Boolean> orderByMap) {
        List<Order> orderByList = new ArrayList<>();
        for (Entry<String, Boolean> entry : orderByMap.entrySet()) {
            String propertyName = entry.getKey();
            Boolean descending = entry.getValue();
            Path pathToProperty = getPathToProperty(root, propertyName);
            if (descending) {
                orderByList.add(criteriaBuilder.desc(pathToProperty));
            } else {
                orderByList.add(criteriaBuilder.asc(pathToProperty));
            }
        }
        criteriaQuery.orderBy(orderByList);
    }

    private Path getPathToProperty(Root<T> root, String propertyName) {
        Path path;
        if (this.needsJoins(propertyName)) {
            // the path leading to the final property
            String dottedPropertyPath = propertyName.substring(0, propertyName.lastIndexOf('.'));
            Join finalJoin = this.createJoinsOnDot(root, dottedPropertyPath);
            String finalPropertyName = this.getFinalPropertyName(propertyName);
            path = finalJoin.get(finalPropertyName);
        } else {
            path = root.get(propertyName);
        }
        return path;
    }

    private boolean needsJoins(String propertyName) {
        return propertyName.contains(".");
    }

    @SuppressWarnings("rawtypes")
    private Join createJoinsOnDot(From from, String dottedPropertyPath) {
        String[] propertyPaths = dottedPropertyPath.split("\\.");
        Join join = null;
        for (int i = 0; i < propertyPaths.length; ++i) {
            String propertyPath = propertyPaths[i];
            join = from.join(propertyPath, JoinType.LEFT);
        }
        return join;
    }

    private String getFinalPropertyName(String propertyName) {
        int endIndex = propertyName.lastIndexOf('.');
        if (endIndex > 0) {
            return propertyName.substring(endIndex + 1);
        } else {
            return propertyName;
        }
    }

    // end of new methods, below this point are legacy methods

    // bugzilla 1427
    public String enquote(String sql) {

        // bugzilla 2316, take care of ' symbol in the sql
        if (sql.indexOf("'") != -1) {
            sql = sql.replaceAll("'", "''");
        }
        return "'" + sql + "'";
    }

    protected void handleException(Exception e, String method) throws LIMSRuntimeException {
        LogEvent.logError(e);
        throw new LIMSRuntimeException("Error in " + this.getClass().getSimpleName() + " " + method, e);
    }

    private class PropertyValueComparison {
        private final String propertyName;
        private final Object propertyValue;
        private final DBComparison comparison;

        private PropertyValueComparison(String propertyName, Object propertyValue, DBComparison comparison) {
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
            this.comparison = comparison;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object getPropertyValue() {
            return propertyValue;
        }

        public DBComparison getComparison() {
            return comparison;
        }
    }

    protected boolean columnNameIsInjectionSafe(String columnName) {
        return columnName.matches("[a-zA-Z0-9 _]+");
    }

    @Override
    public void evict(T baseObject) {
        entityManager.unwrap(Session.class).evict(baseObject);
    }

    // private static final int RANDOM_ALIAS_LENGTH = 5;
    // private static final String MULTI_NESTED_MARKING = ",";
    // private void addRestriction(Criteria criteria, String propertyName, Object
    // propertyValue,
    // Map<String, String> aliases) {
    // String aliasedProperty = createAliasIfNeeded(criteria, propertyName,
    // aliases);
    // criteria.add(Restrictions.eq(aliasedProperty, propertyValue));
    // }
    //
    // private void addLikeRestriction(Criteria criteria, String propertyName,
    // String propertyValue,
    // Map<String, String> aliases) {
    // String aliasedProperty = createAliasIfNeeded(criteria, propertyName,
    // aliases);
    // criteria.add(Restrictions.ilike(aliasedProperty, propertyValue,
    // MatchMode.ANYWHERE));
    // }
    //
    // private void addOrder(Criteria criteria, String orderProperty, boolean
    // descending, Map<String, String> aliases) {
    // String aliasedProperty = createAliasIfNeeded(criteria, orderProperty,
    // aliases);
    // if (descending) {
    // criteria.addOrder(Order.desc(aliasedProperty));
    // } else {
    // criteria.addOrder(Order.asc(aliasedProperty));
    // }
    //
    // }
    //
    // private String createAliasIfNeeded(Criteria criteria, String propertyName,
    // Map<String, String> aliases) {
    // int dotCount = StringUtils.countMatches(propertyName, '.');
    // if (dotCount > 2 || (dotCount == 2 && !propertyName.endsWith(".id"))) {
    // // multi nesting detected, need special consideration to generate multiple
    // // aliases unless there are only 2 levels and the third property is the id
    // field
    // return createMultiNestedAliases(criteria, propertyName, aliases);
    // } else if (dotCount == 1 || (dotCount == 2 && propertyName.endsWith(".id")))
    // {
    // // simple aliasing is required
    // return createSingleNestedAlias(criteria, propertyName, aliases);
    // } else {
    // // no aliasing needed, use the property name as is
    // return propertyName;
    // }
    // }
    //
    // private Optional<String> createAlias(String nestedProperty, Map<String,
    // String> aliases) {
    // return createAlias(nestedProperty, 1, aliases);
    // }
    //
    // private Optional<String> createAlias(String nestedProperty, int i,
    // Map<String, String> aliases) {
    // String alias;
    // if (aliases.containsKey(nestedProperty)) {
    // // signal alias does not need to be created, it already exists
    // return Optional.empty();
    // } else {
    // if (nestedProperty.length() > i) {
    // // use substring of property as alias
    // alias = nestedProperty.substring(0, i);
    // } else {
    // // reached max length for the property without finding a unique alias, using
    // // random alias
    // LogEvent.logWarn(this.getClass().getSimpleName(), "createAlias()",
    // "this alias is going to be a poorly named alias as the string length has been
    // reached.");
    // alias = createRandomAlias();
    // }
    // if (aliases.containsValue(alias)) {
    // // recurse to try and find an unused alias,
    // return createAlias(nestedProperty, ++i, aliases);
    // } else {
    // // unique alias found, end recursion
    // return Optional.of(alias);
    // }
    // }
    //
    // }
    //
    // private String createRandomAlias() {
    // return RandomStringUtils.randomAlphabetic(RANDOM_ALIAS_LENGTH);
    // }
    //
    // private String createSingleNestedAlias(Criteria criteria, String
    // propertyName, Map<String, String> aliases) {
    // String nestedProperty = propertyName.substring(0, propertyName.indexOf('.'));
    // ClassMetadata metadata = sessionFactory.getClassMetadata(classType);
    // // check if composite-id, which doesn't require aliasing
    // if (nestedProperty.equals(metadata.getIdentifierPropertyName())) {
    // return propertyName;
    // }
    // Optional<String> alias = createAlias(nestedProperty, aliases);
    // // alias is new and needs to be added
    // if (alias.isPresent()) {
    // criteria.createAlias(nestedProperty, alias.get());
    // aliases.put(nestedProperty, alias.get());
    // }
    // return aliases.get(nestedProperty) +
    // propertyName.substring(propertyName.indexOf('.'));
    // }
    //
    // private String createMultiNestedAliases(Criteria criteria, String
    // propertyName, Map<String, String> aliases) {
    // String alias;
    // String newPropertyName = propertyName;
    // while (true) {
    // String[] properties = newPropertyName.split("\\.");
    // // create alias for one level of nesting
    // alias = createMarkedAlias(criteria, properties[0] + "." + properties[1],
    // aliases);
    // // mark nesting level that has just been completed
    // newPropertyName = newPropertyName.replaceFirst("\\.", MULTI_NESTED_MARKING);
    // // check if there is another level of nesting
    // if (newPropertyName.contains(".")) {
    // // replace the property name with it's alias
    // newPropertyName = alias.replaceFirst("\\.", MULTI_NESTED_MARKING)
    // + newPropertyName.substring(newPropertyName.indexOf('.'));
    // } else {
    // break;
    // }
    // }
    // return alias;
    // }
    //
    // private String createMarkedAlias(Criteria criteria, String propertyName,
    // Map<String, String> aliases) {
    // String nestedProperty = propertyName.substring(0, propertyName.indexOf('.'));
    // // replace properties that have been marked as done with "." for properly
    // adding
    // // the criteria
    // nestedProperty = nestedProperty.replace(MULTI_NESTED_MARKING, ".");
    // Optional<String> alias = createMarkedAlias(nestedProperty, aliases);
    // // alias is new and needs to be added
    // if (alias.isPresent()) {
    // criteria.createAlias(nestedProperty, alias.get());
    // aliases.put(nestedProperty, alias.get());
    // }
    // return aliases.get(nestedProperty) +
    // propertyName.substring(propertyName.indexOf('.'));
    //
    // }
    //
    // private Optional<String> createMarkedAlias(String nestedProperty, Map<String,
    // String> aliases) {
    // return createMarkedAlias(nestedProperty, 1, aliases);
    // }
    //
    // private Optional<String> createMarkedAlias(String nestedProperty, int i,
    // Map<String, String> aliases) {
    // String alias;
    // if (aliases.containsKey(nestedProperty)) {
    // // signal alias does not need to be created, it already exists
    // return Optional.empty();
    // } else {
    // if (nestedProperty.length() > i) {
    // if (nestedProperty.contains(".")) {
    // // use substring of second property as alias (property after the first ".")
    // alias = nestedProperty.substring(nestedProperty.indexOf('.') + 1,
    // nestedProperty.indexOf('.') + 1 + i);
    // } else {
    // // use substring of property as alias
    // alias = nestedProperty.substring(0, i);
    // }
    // } else {
    // // reached max length for the property without finding a unique alias, using
    // // random alias
    // LogEvent.logWarn(this.getClass().getSimpleName(), "createMultiAlias()",
    // "this alias is going to be a poor alias as the string length has been
    // reached.");
    // alias = createRandomAlias();
    //
    // }
    // if (aliases.containsValue(alias)) {
    // // recurse to try and find an unused alias,
    // return createAlias(nestedProperty, ++i, aliases);
    // } else {
    // // unique alias found, end recursion
    // return Optional.of(alias);
    // }
    // }
    // }
}
