package org.openelisglobal.common.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.valueholder.BaseObject;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <b>Authorization.</b> Every method here carries a {@code @PreAuthorize} that
 * delegates to {@link org.openelisglobal.common.security.CrudGate}, which reads
 * the descendant interface's
 * {@link org.openelisglobal.common.security.CrudPrivileges} (falling back to
 * the descendant interface's type-level gate, else open). This is the one place
 * inherited CRUD is gated: a descendant interface must <em>not</em> redeclare
 * these methods with its own {@code @PreAuthorize} — such a gate is resolved
 * against the most specific method, which for an inherited implementation lives
 * in {@code BaseObjectServiceImpl} and never sees it.
 */
public interface BaseObjectService<T extends BaseObject<PK>, PK extends Serializable> {

    /**
     * @param id
     * @return the baseObject corresponding with the id or a new object s
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    T get(PK id);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Optional<T> getMatch(String propertyName, Object propertyValue);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Optional<T> getMatch(Map<String, Object> propertyValues);

    /**
     * @return all data type for the baseObject type
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAll();

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @return List of all matching entries
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAllMatching(String propertyName, Object propertyValue);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @return List of all matching entries
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAllMatching(Map<String, Object> propertyValues);

    /**
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @return List of all ordered entries
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAllOrdered(String orderProperty, boolean descending);

    /**
     * @param orderProperties the properties to order by starting with the first
     *                        entry
     * @param descending      Set to true to order by descending, false for order by
     *                        ascending
     * @return List of all ordered entries
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAllOrdered(List<String> orderProperties, boolean descending);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @return List of all ordered matching entries
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, List<String> orderProperties, boolean descending);

    /**
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results sorted by id. If length is 1 more than page size,
     *         this signifies there is a next page
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getPage(int startingRecNo);

    /**
     * @param propertyName  the property that must match
     * @param propertyValue the value the property must equal
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo);

    /**
     * @param propertyValues Key Value pairs where key is the property name and
     *                       value is the value it must match
     * @param startingRecNo  0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo);

    /**
     * @param orderProperty the property to order by
     * @param descending    Set to true to order by descending, false for order by
     *                      ascending
     * @param startingRecNo 0 indexed page number to get results from
     * @return A page of results. If length is 1 more than page size, this signifies
     *         there is a next page
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
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
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, List<String> orderProperties, boolean descending,
            int startingRecNo);

    /**
     * @param baseObject the data to insert
     * @return the id of the inserted baseObject
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    PK insert(T baseObject);

    /**
     * @param baseObjects the data to insert
     * @return the ids of the inserted baseObjects
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    List<PK> insertAll(List<T> baseObjects);

    /**
     * @param baseObject the new data to update the database with. Will insert if it
     *                   doesn't already exist
     * @return the baseObject as it was saved to the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    T save(T baseObject);

    /**
     * @param baseObjects the new data to update the database with. Will insert if
     *                    it doesn't already exist
     * @return the baseObjects as they were saved to the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    List<T> saveAll(List<T> baseObjects);

    /**
     * @param baseObject the new data to update the database with. Must have an id
     *                   parameter
     * @return the baseObject as it was saved to the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    T update(T baseObject);

    /**
     * @param baseObjects the new data to update the database with. Must have an id
     *                    parameter
     * @return the baseObjects as they were saved to the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    List<T> updateAll(List<T> baseObjects);

    /**
     * @param baseObject the data to delete from the database. Must have primary key
     *                   fields filled in
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    void delete(T baseObject);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    void delete(PK id, String sysUserId);

    /**
     * @param baseObjects List of all baseObjects to delete from the database. Must
     *                    have primary key fields filled in
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    void deleteAll(List<T> baseObjects);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).write(#root)")
    void deleteAll(List<PK> ids, String sysUserId);

    /**
     * @return the number of rows
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Integer getCount();

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Integer getCountMatching(String propertyName, Object propertyValue);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Integer getCountMatching(Map<String, Object> propertyValues);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Integer getCountLike(String propertyName, String propertyValue);

    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    Integer getCountLike(Map<String, String> propertyValues);

    /**
     * @param id the id to start from
     * @return list of the baseObject corresponding to the next two ids ( if they
     *         exist)
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    public T getNext(String id);

    /**
     * @param id the id to start from
     * @return list of the baseObject corresponding to the previous two ids ( if
     *         they exist)
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    public T getPrevious(String id);

    /**
     * @param id the id to start from
     * @return check if baseObject has a next baseObject in the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    public boolean hasNext(String id);

    /**
     * @param id the id to start from
     * @return check if baseObject has a previous baseObject in the database
     */
    @PreAuthorize("T(org.openelisglobal.common.security.CrudGate).read(#root)")
    public boolean hasPrevious(String id);
}
