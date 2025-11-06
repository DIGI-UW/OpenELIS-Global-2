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
package org.openelisglobal.common.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * @author caleb
 * @param <T> the type of data object that this returns
 */
public interface BaseDAO<T extends BaseObject<PK>, PK extends Serializable> {

    /**
     * @param id
     * @return the object corresponding with the id
     */
    Optional<T> get(PK id);

    List<T> get(List<PK> ids);

    /**
     * @return all data type for the object type
     */
    List<T> getAll();

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @return List of all matching entries
     */
    List<T> getAllMatching(String propertyName, Object propertyValue);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @return List of all matching entries
     */
    List<T> getAllMatching(Map<String, Object> propertyValues);

    List<T> getAllLike(Map<String, String> propertyValues);

    List<T> getAllLike(String propertyName, String propertyValue);

    /**
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @return List of all ordered entries
     */
    List<T> getAllOrdered(String orderProperty, boolean descending);

    /**
     * @param orderProperties the properties to order by starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered entries
     */
    List<T> getAllOrdered(List<String> orderProperties, boolean descending);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, String orderProperty, boolean descending);

    /**
     * @param propertyName    the property that must match
     * @param propertyValue   the value the property must equal
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param orderProperty  the property to order by
     * @param descending     Set to true to order by descending, false for order by
     *                       ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty, boolean descending);

    /**
     * @param propertyValues  Key Value pairs where key is the property name and
     *                        value is the value it must match
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, List<String> orderProperties, boolean descending);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllLikeOrdered(String propertyName, String propertyValue, String orderProperty, boolean descending);

    /**
     * @param propertyName    the property that must match
     * @param propertyValue   the value the property must equal
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllLikeOrdered(String propertyName, String propertyValue, List<String> orderProperties,
            boolean descending);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param orderProperty  the property to order by
     * @param descending     Set to true to order by descending, false for order by
     *                       ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllLikeOrdered(Map<String, String> propertyValues, String orderProperty, boolean descending);

    /**
     * @param propertyValues  Key Value pairs where key is the property name and
     *                        value is the value it must match
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered matching entries
     */
    List<T> getAllLikeOrdered(Map<String, String> propertyValues, List<String> orderProperties, boolean descending);

    /**
     * @param startingRecNo the rec number to start from for this page
     * @return A page of results sorted by id. If length is 1 more than page size,
     *         this signifies there is a next page
     */
    List<T> getPage(int startingRecNo);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param startingRecNo the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param startingRecNo  the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo);

    /**
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @param startingRecNo the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getOrderedPage(String orderProperty, boolean descending, int startingRecNo);

    /**
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @param startingRecNo   the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getOrderedPage(List<String> orderProperties, boolean descending, int startingRecNo);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @param startingRecNo the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, String orderProperty, boolean descending,
            int startingRecNo);

    /**
     * @param propertyName    the property that must match
     * @param propertyValue   the value the property must equal
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @param startingRecNo   the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending, int startingRecNo);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param orderProperty  the property to order by
     * @param descending     Set to true to order by descending, false for order by
     *                       ascending
     * @param startingRecNo  the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty, boolean descending,
            int startingRecNo);

    /**
     * @param propertyValues  Key Value pairs where key is the property name and
     *                        value is the value it must match
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @param startingRecNo   the rec number to start from for this page
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, List<String> orderProperties, boolean descending,
            int startingRecNo);

    /**
     * @param object the data to insert
     * @return the id of the inserted object
     */
    PK insert(T object);

    /**
     * @param object the new data to update the database with.
     * @return the object as it was saved to the database
     */
    T update(T baseObject);

    /**
     * @param object the data to delete from the database. Must have primary key
     *               fields filled in
     */
    void delete(T object);

    /**
     * @return the number of rows
     */
    Integer getCount();

    /**
     * @param id the id to start from
     */
    Optional<T> getNext(String id);

    /**
     * @param id the id to start from
     */
    Optional<T> getPrevious(String id);

    /**
     * @return get table name in database for the object
     */
    String getTableName();

    // bugzilla 1411

    List<T> getLikePage(String propertyName, String propertyValue, int startingRecNo);

    List<T> getLikePage(Map<String, String> propertyValues, int startingRecNo);

    List<T> getLikeOrderedPage(String propertyName, String propertyValue, String orderProperty, boolean descending,
            int startingRecNo);

    List<T> getLikeOrderedPage(String propertyName, String propertyValue, List<String> orderProperties,
            boolean descending, int startingRecNo);

    List<T> getLikeOrderedPage(Map<String, String> propertyValues, String orderProperty, boolean descending,
            int startingRecNo);

    List<T> getLikeOrderedPage(Map<String, String> propertyValues, List<String> orderProperties, boolean descending,
            int startingRecNo);

    void evict(T oldObject);
}
