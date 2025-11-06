package org.openelisglobal.common.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.valueholder.BaseObject;

public interface BaseObjectService<T extends BaseObject<PK>, PK extends Serializable> {

    /**
     * @param id
     * @return the baseObject corresponding with the id or a new object s
     */
    T get(PK id);

    Optional<T> getMatch(String propertyName, Object propertyValue);

    Optional<T> getMatch(Map<String, Object> propertyValues);

    /**
     * @return all data type for the baseObject type
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
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results sorted by id. If length is 1 more than page size,
     *         this signifies there is a next page
     */
    List<T> getPage(int startingRecNo);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param startingRecNo  0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo);

    /**
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getOrderedPage(String orderProperty, boolean descending, int startingRecNo);

    /**
     * @param orderProperties the properties to order by, starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @param startingRecNo   0 indexed page number to get results from
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
     * @param startingRecNo 0 indexed page number to get results from
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
     * @param startingRecNo   0 indexed page number to get results from
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
     * @param startingRecNo  0 indexed page number to get results from
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
     * @param startingRecNo   0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, List<String> orderProperties, boolean descending,
            int startingRecNo);

    /**
     * @param baseObject the data to insert
     * @return the id of the inserted baseObject
     */
    PK insert(T baseObject);

    /**
     * @param baseObjects the data to insert
     * @return the ids of the inserted baseObjects
     */
    List<PK> insertAll(List<T> baseObjects);

    /**
     * @param baseObject the new data to update the database with. Will insert if it
     *                   doesn't already exist
     * @return the baseObject as it was saved to the database
     */
    T save(T baseObject);

    /**
     * @param baseObjects the new data to update the database with. Will insert if
     *                    it doesn't already exist
     * @return the baseObjects as they were saved to the database
     */
    List<T> saveAll(List<T> baseObjects);

    /**
     * @param baseObject the new data to update the database with. Must have an id
     *                   parameter
     * @return the baseObject as it was saved to the database
     */
    T update(T baseObject);

    /**
     * @param baseObjects the new data to update the database with. Must have an id
     *                    parameter
     * @return the baseObjects as they were saved to the database
     */
    List<T> updateAll(List<T> baseObjects);

    /**
     * @param baseObject the data to delete from the database. Must have primary key
     *                   fields filled in
     */
    void delete(T baseObject);

    void delete(PK id, String sysUserId);

    /**
     * @param baseObjects List of all baseObjects to delete from the database. Must
     *                    have primary key fields filled in
     */
    void deleteAll(List<T> baseObjects);

    void deleteAll(List<PK> ids, String sysUserId);

    /**
     * @return the number of rows
     */
    Integer getCount();

    Integer getCountMatching(String propertyName, Object propertyValue);

    Integer getCountMatching(Map<String, Object> propertyValues);

    Integer getCountLike(String propertyName, String propertyValue);

    Integer getCountLike(Map<String, String> propertyValues);

    /**
     * @param id the id to start from
     * @return list of the baseObject corresponding to the next two ids ( if they
     *         exist)
     */
    public T getNext(String id);

    /**
     * @param id the id to start from
     * @return list of the baseObject corresponding to the previous two ids ( if
     *         they exist)
     */
    public T getPrevious(String id);

    /**
     * @param id the id to start from
     * @return check if baseObject has a next baseObject in the database
     */
    public boolean hasNext(String id);

    /**
     * @param id the id to start from
     * @return check if baseObject has a previous baseObject in the database
     */
    public boolean hasPrevious(String id);
}
